package circus.robocalc.robochart.epsilon.isacircus.ui.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.epsilon.eol.dt.ExtensionPointToolNativeTypeDelegate;

/**
 * Handler for the "Compile to IsaCircus" right-click menu action in RoboTool.
 *
 * When the user right-clicks a .rct file and selects RoboTool > IsaCircus > Compile,
 * this handler runs two Epsilon transformations in sequence:
 *
 *   Step 1 (EOL): RC2Z.eol
 *     - Reads the RoboChart model (.rct) as input model "RC"
 *     - Writes an intermediate Circus model (_circus.model) to the system temp
 *       directory so it does not appear in the Eclipse project explorer
 *
 *   Step 2 (EGX/EGL): CircusM2T.egx
 *     - Reads the intermediate Circus model as input model "Z"
 *     - Generates .thy text files relative to the directory containing the .rct file:
 *         circus_gen/IsaCircus_enriched_*.thy
 *         hol-csp_gen/HOLCSP_*.thy
 *         hol-csp-abstact_gen/HOLCSP_AbstractInst_*.thy
 */
public class CompileToIsaCircusHandler extends AbstractHandler implements IRunnableWithProgress {

    // The .rct file selected by the user, passed from execute() to run()
    IFile inputFile;

    /**
     * Entry point called by Eclipse when the menu item is clicked.
     * Retrieves the selected file, checks it is a .rct file,
     * then opens a progress dialog and delegates to run().
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);

        if (!(selection.getFirstElement() instanceof IFile)) {
            return null;
        }
        inputFile = (IFile) selection.getFirstElement();

        // Guard: only handle .rct files
        if (!inputFile.getFileExtension().equals("rct")) {
            return null;
        }

        try {
            // Run in a background thread with a progress monitor dialog
            new ProgressMonitorDialog(HandlerUtil.getActiveShell(event)).run(true, true, this);
        } catch (InvocationTargetException e) {
            throw new ExecutionException("IsaCircus compilation failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    /**
     * Main transformation logic, executed in a background thread.
     * Called by the ProgressMonitorDialog.
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Compiling " + inputFile.getName() + " to IsaCircus", 10);
       
        // ── Register the Circus metamodel if not already registered ──────────
        // The Circus metamodel (circus_metamodel.ecore) is bundled inside this plugin.
        // We load it dynamically and register it in EMF's global EPackage registry
        // so that EmfModel can find it by its nsURI "http://www.robocalc.circus/Circus".
        // The check avoids re-registering if the user runs the command multiple times
        // without restarting Eclipse.
        if (!EPackage.Registry.INSTANCE.containsKey("http://www.robocalc.circus/Circus")) {
            try {
                // Register .model extension so EMF knows how to read/write the intermediate model
                Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                    .put("model", new XMIResourceFactoryImpl());

                // Load the ecore file from inside this plugin using platform:/plugin URI
                org.eclipse.emf.common.util.URI ecoreURI = org.eclipse.emf.common.util.URI.createURI(
                    "platform:/plugin/circus.robocalc.robochart.epsilon.isacircus/metamodels/circus_metamodel.ecore");
                ResourceSet rs = new ResourceSetImpl();
                rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("ecore", new EcoreResourceFactoryImpl());
                Resource ecoreResource = rs.getResource(ecoreURI, true);

                // Register the EPackage so EMF can resolve the nsURI globally
                EPackage circusPackage = (EPackage) ecoreResource.getContents().get(0);
                EPackage.Registry.INSTANCE.put(circusPackage.getNsURI(), circusPackage);
            } catch (Exception e) {
            	throw new InvocationTargetException(e, "Metamodel registration failed: " + e.getMessage());
                
            }
        }
        monitor.worked(1);

        // ── Derive file paths from the selected .rct file ────────────────────
        // rctAbsPath:         absolute path to the .rct file
        // baseName:           filename without extension (e.g. "example_robot")
        // circusModelAbsPath: intermediate model written to system temp directory
        //                     so it does not appear in the Eclipse project explorer
        // rctDirAbsPath:      directory containing the .rct file, used as EGX output root
        String rctAbsPath = new java.io.File(inputFile.getLocationURI()).getAbsolutePath().replace("\\", "/");
        String baseName = inputFile.getFullPath().removeFileExtension().lastSegment();
        String tempDir = System.getProperty("java.io.tmpdir");
        String circusModelAbsPath = tempDir + "/" + baseName + "_circus.model";
        String rctDirAbsPath = rctAbsPath.substring(0, rctAbsPath.lastIndexOf("/"));

        
        /*to be used for hiding the console output from eol and egl code:*/
    	java.io.PrintStream nullStream = new java.io.PrintStream(new java.io.OutputStream() {
    	    public void write(int b) {}
    	});
        /**/
        
        // ── Step 1: EOL transformation (RC2Z.eol) ────────────────────────────
        // Input model:  RC  — the RoboChart .rct file, read-only
        // Output model: Z   — the intermediate Circus .model file, written to temp dir
        monitor.subTask("Step 1/2: RoboChart model to Circus model");

        // Load input RoboChart model
        EmfModel rcModel = new EmfModel();
        rcModel.setName("RC");
        rcModel.setMetamodelUri("http://www.robocalc.circus/RoboChart");
        rcModel.setModelFile(rctAbsPath);
        rcModel.setReadOnLoad(true);
        rcModel.setStoredOnDisposal(false);
        try {
            rcModel.load();
        } catch (EolModelLoadingException e) {
        	throw new InvocationTargetException(e, "RC model loading failed: " + e.getMessage());
        }
        monitor.worked(1);

        // Set up output Circus model (empty, will be populated by EOL and saved to temp dir)
        EmfModel circusModel = new EmfModel();
        circusModel.setName("Z");
        circusModel.setMetamodelUri("http://www.robocalc.circus/Circus");
        circusModel.setModelFile(circusModelAbsPath);
        circusModel.setReadOnLoad(false);
        circusModel.setStoredOnDisposal(true); // causes Epsilon to save the model to disk on dispose
        circusModel.setCachingEnabled(false);
        try {
            circusModel.load();
        } catch (EolModelLoadingException e) {
        	throw new InvocationTargetException(e, "Circus model loading failed: " + e.getMessage());
        	
        }
        monitor.worked(1);

        // Parse and execute RC2Z.eol from inside this plugin
        EolModule eolModule = new EolModule();
        try {
            eolModule.parse(new java.net.URI(
                "platform:/plugin/circus.robocalc.robochart.epsilon.isacircus/erules/RC2Z.eol"));
        } catch (Exception e) {
        	throw new InvocationTargetException(e, "EOL parse failed: " + e.getMessage());
        	
        }
        eolModule.getContext().getModelRepository().addModel(rcModel);
        eolModule.getContext().getModelRepository().addModel(circusModel);
        eolModule.getContext().getNativeTypeDelegates().add(
            new org.eclipse.epsilon.eol.dt.ExtensionPointToolNativeTypeDelegate()
        );
        try {
    /*to hide the console output from eol code:*/
  eolModule.getContext().setOutputStream(nullStream);
    /**/
    
    eolModule.execute();
} catch (EolRuntimeException e) {
    // Step 1 (M2M) failed — most likely the RoboChart model uses a feature
    // that is not currently supported by the translation. Show a friendly
    // dialog instead of a technical stack trace, and stop the compilation.
    circusModel.dispose();
    rcModel.dispose();
    monitor.done();
    org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
        org.eclipse.jface.dialogs.MessageDialog.openWarning(
            org.eclipse.swt.widgets.Display.getDefault().getActiveShell(),
            "Unsupported RoboChart Feature",
            "The compilation could not be completed.\n\n"
            + "The RoboChart model appears to contain one or more features "
            + "that are not currently supported by the RoboChart-to-IsaCircus translation.\n\n"
            + "Please modify the model and try again."
        );
    });
    return; // stop here, do not proceed to Step 2
}

        // Disposing circusModel triggers storeOnDisposal, writing _circus.model to temp dir
        circusModel.dispose();
        rcModel.dispose();
        monitor.worked(3);

        // ── Step 2: EGX/EGL transformation (CircusM2T.egx) ───────────────────
        // Input model: Z  — the intermediate Circus model written in Step 1
        // Output:          .thy text files written to subdirectories under rctDirAbsPath:
        //                  circus_gen/, hol-csp_gen/, hol-csp-abstact_gen/
        monitor.subTask("Step 2/2: Circus model to IsaCyPhyCirus/HOL-CSP");

        // Reload the Circus model from temp dir as read-only input for M2T
        EmfModel circusModelForM2T = new EmfModel();
        circusModelForM2T.setName("Z");
        circusModelForM2T.setMetamodelUri("http://www.robocalc.circus/Circus");
        circusModelForM2T.setModelFile(circusModelAbsPath);
        circusModelForM2T.setReadOnLoad(true);
        circusModelForM2T.setStoredOnDisposal(false);
        try {
            circusModelForM2T.load();
        } catch (EolModelLoadingException e) {
        	throw new InvocationTargetException(e, "Circus model (M2T) loading failed: " + e.getMessage());
        	
        }
        monitor.worked(1);

        // Parse and execute CircusM2T.egx
        // The EgxModule constructor takes the base output directory.
        // Target paths in CircusM2T.egx are relative to this directory, e.g.:
        //   "circus_gen/IsaCircus_enriched_*.thy"
        //   "hol-csp_gen/HOLCSP_*.thy"
        //   "hol-csp-abstact_gen/HOLCSP_AbstractInst_*.thy"
        try {
            EgxModule egxModule = new EgxModule(rctDirAbsPath);
            System.out.println("[DEBUG] EGX base dir: " + rctDirAbsPath);
            egxModule.parse(new java.net.URI(
                "platform:/plugin/circus.robocalc.robochart.epsilon.isacircus/erules/CircusM2T.egx"));
            System.out.println("[DEBUG] EGX parsed OK");
            EmfModel rcModelForM2T = new EmfModel();
            rcModelForM2T.setName("RC");
            rcModelForM2T.setMetamodelUri("http://www.robocalc.circus/RoboChart");
            rcModelForM2T.setModelFile(rctAbsPath);
            rcModelForM2T.setReadOnLoad(true);
            rcModelForM2T.setStoredOnDisposal(false);
            try { rcModelForM2T.load(); } catch (EolModelLoadingException e) { e.printStackTrace(); }
            egxModule.getContext().getModelRepository().addModel(circusModelForM2T);
            egxModule.getContext().getModelRepository().addModel(rcModelForM2T);
            egxModule.getContext().getFrameStack().put(
                org.eclipse.epsilon.eol.execute.context.Variable.createReadOnlyVariable("baseName", baseName)
            );
            /*to hide console output from egl code*/
          egxModule.getContext().setOutputStream(nullStream);
            /**/
            egxModule.execute();
            System.out.println("[DEBUG] EGX executed OK");
        } catch (Exception e) {
            System.out.println("[DEBUG] EGX exception: " + e.getMessage());
            throw new InvocationTargetException(e, "EGX execution failed: " + e.getMessage());
        }
        circusModelForM2T.dispose();
        monitor.worked(3);

        // ── Refresh the Eclipse workspace ─────────────────────────────────────
        // Without this, the newly generated .thy files won't appear in the
        // Project Explorer until the user manually refreshes.
        try {
            inputFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        monitor.worked(1);
        System.out.println("IsaCircus .thy files generated successfully for: " + baseName);
        monitor.done();
    }
}