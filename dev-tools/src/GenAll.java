import java.io.File;
import java.nio.file.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.*;
import org.eclipse.epsilon.emc.emf.*;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.egl.*;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.xtext.resource.XtextResourceSet;
import circus.robocalc.robochart.textual.RoboChartStandaloneSetup;
import circus.robocalc.robochart.epsilon.isacircus.loader.RctLoader;

/**
 * Generates the HOL-CSP theory of a .rct without RoboTool: RctLoader (cross-file references resolved),
 * RC2Z.eol, then circusM2T_HOLCSP.egl, as the plugin does.
 * usage: GenAll <file.rct> <project root> <erules dir> <circus_metamodel.ecore> <out dir> <tag> [closure|all|single]
 * closure (default): RctLoader with fallback; all: all source files of the project; single: the selected file
 * alone, without RctLoader (the plugin's former behaviour, to test the defensive check of RC2Z.eol).
 * Writes <out>/<base>_circus.model and <out>/HOLCSP_<base>_<tag>.thy (theory HOLCSP_<base>_<tag>).
 */
public class GenAll {
  public static void main(String[] a) throws Exception {
    com.google.inject.Injector inj = new RoboChartStandaloneSetup().createInjectorAndDoEMFRegistration();
    RctLoader loader = new RctLoader(() -> inj.getInstance(XtextResourceSet.class), false);
    String modeArg = a.length > 6 ? a[6] : "closure";
    File rct = new File(a[0]).getAbsoluteFile();
    Resource selected;
    if (modeArg.equals("single")) {
      XtextResourceSet rs = inj.getInstance(XtextResourceSet.class);
      Resource r = rs.getResource(URI.createFileURI(rct.getPath()), true);
      org.eclipse.emf.ecore.util.EcoreUtil.resolveAll(rs);
      ResourceSet alone = new ResourceSetImpl();
      selected = new org.eclipse.emf.ecore.resource.impl.ResourceImpl(URI.createURI("memory:/" + rct.getName()));
      alone.getResources().add(selected);
      selected.getContents().addAll(r.getContents());
      System.out.println("single: " + rct.getName() + " alone");
    } else {
      RctLoader.Result lr = loader.load(rct, new File(a[1]), modeArg.equals("all") ? RctLoader.Mode.ALL : RctLoader.Mode.CLOSURE);
      for (String l : lr.log) if (!l.startsWith("reference ")) System.out.println(l);
      for (String w : lr.warnings) System.out.println("WARNING " + w);
      selected = lr.resource;
    }
    String base = rct.getName().replace(".rct", "");

    ResourceSet mrs = new ResourceSetImpl();
    mrs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
    EPackage pkg = (EPackage) mrs.getResource(URI.createFileURI(new File(a[3]).getAbsolutePath()), true).getContents().get(0);
    EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("model", new XMIResourceFactoryImpl());

    InMemoryEmfModel rc = new InMemoryEmfModel("RC", selected);
    rc.setModelFileUri(URI.createFileURI(rct.getPath())); // RC2Z.eol reads RC.modelFile for the base name
    File out = new File(a[4]); out.mkdirs();
    String zPath = new File(out, base + "_circus.model").getAbsolutePath();
    EmfModel z = new EmfModel();
    z.setName("Z"); z.setMetamodelUri(pkg.getNsURI()); z.setModelFile(zPath);
    z.setReadOnLoad(false); z.setStoredOnDisposal(true); z.setCachingEnabled(false); z.load();
    EolModule eol = new EolModule();
    if (!eol.parse(new File(a[2], "RC2Z.eol"))) { System.out.println(eol.getParseProblems()); System.exit(1); }
    eol.getContext().getModelRepository().addModel(rc);
    eol.getContext().getModelRepository().addModel(z);
    eol.getContext().setOutputStream(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
    eol.execute();
    z.dispose();

    EmfModel zr = new EmfModel();
    zr.setName("Z"); zr.setMetamodelUri(pkg.getNsURI()); zr.setModelFile(zPath); zr.setReadOnLoad(true); zr.setStoredOnDisposal(false); zr.load();
    EglTemplateFactoryModuleAdapter mod = new EglTemplateFactoryModuleAdapter(new EglTemplateFactory());
    if (!mod.parse(new File(a[2], "circusM2T_HOLCSP.egl"))) { System.out.println(mod.getParseProblems()); System.exit(1); }
    mod.getContext().getModelRepository().addModel(zr);
    mod.getContext().getFrameStack().put(Variable.createReadOnlyVariable("z", zr.getAllOfType("Spec").iterator().next()));
    mod.getContext().getFrameStack().put(Variable.createReadOnlyVariable("baseName", base));
    mod.getContext().getFrameStack().put(Variable.createReadOnlyVariable("ts", a[5]));
    mod.getContext().setOutputStream(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
    Path thy = new File(out, "HOLCSP_" + base + "_" + a[5] + ".thy").toPath();
    Files.writeString(thy, mod.execute().toString());
    System.out.println("wrote " + zPath + "\nwrote " + thy);
  }
}
