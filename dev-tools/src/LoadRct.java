import java.io.File;
import java.util.*;
import org.eclipse.emf.ecore.*;
import org.eclipse.xtext.resource.XtextResourceSet;
import circus.robocalc.robochart.textual.RoboChartStandaloneSetup;
import circus.robocalc.robochart.epsilon.isacircus.loader.RctLoader;

/**
 * Loads a .rct with RctLoader and reports how the files were chosen, the event references of the
 * selected file, and the ambiguity warnings.
 * usage: LoadRct <file.rct> <project root> [closure|all] [ide|ide-fix]
 * ide: simulates the plugin inside RoboTool: plain ResourceSetImpl, and an injector whose default
 * IResourceDescriptions is empty, like the workspace index of the IDE (DirtyStateAwareResourceDescriptions,
 * bound in org.eclipse.xtext.ui.shared) when the files are not indexed (Build Automatically off).
 * ide-fix: the same global registration, but the resource sets come from RuntimeResourceSets (the plugin's fix).
 */
public class LoadRct {
  public static void main(String[] a) throws Exception {
    List<String> opts = Arrays.asList(a);
    RctLoader loader;
    if (opts.contains("ide") || opts.contains("ide-fix")) {
      // global registration as in RoboTool: the factory of an injector whose default index does not contain the
      // files (empty), with a container manager that works on any index
      new RoboChartStandaloneSetup() {
        @Override public com.google.inject.Injector createInjector() {
          return com.google.inject.Guice.createInjector(com.google.inject.util.Modules
            .override(new circus.robocalc.robochart.textual.RoboChartRuntimeModule())
            .with(b -> {
              b.bind(org.eclipse.xtext.resource.IResourceDescriptions.class).toInstance(
                new org.eclipse.xtext.resource.impl.ResourceDescriptionsData(Collections.emptyList()));
              b.bind(org.eclipse.xtext.resource.IContainer.Manager.class)
                .to(org.eclipse.xtext.resource.impl.SimpleResourceDescriptionsBasedContainerManager.class);
            }));
        }
      }.createInjectorAndDoEMFRegistration();
      if (opts.contains("ide")) {
        loader = new RctLoader(org.eclipse.emf.ecore.resource.impl.ResourceSetImpl::new, false);
        System.out.println("simulating the IDE: plain ResourceSetImpl, global factory with an empty index");
      } else {
        loader = new RctLoader(new circus.robocalc.robochart.epsilon.isacircus.loader.RuntimeResourceSets(), false);
        System.out.println("simulating the IDE with the fix: RuntimeResourceSets (own runtime injector)");
      }
    } else {
      com.google.inject.Injector inj = new RoboChartStandaloneSetup().createInjectorAndDoEMFRegistration();
      loader = new RctLoader(() -> inj.getInstance(XtextResourceSet.class), false);
    }
    RctLoader.Mode mode = Arrays.asList(a).contains("all") ? RctLoader.Mode.ALL : RctLoader.Mode.CLOSURE;
    RctLoader.Result r = loader.load(new File(a[0]), new File(a[1]), mode);
    for (String l : r.log) if (!l.startsWith("reference ")) System.out.println(l);
    System.out.println("files: " + r.loadedFiles.stream().map(File::getName).reduce((x, y) -> x + " " + y).orElse(""));
    for (Iterator<EObject> it = r.resource.getAllContents(); it.hasNext();) {
      EObject o = it.next();
      EStructuralFeature ev = o.eClass().getEStructuralFeature("event");
      if (ev == null || !o.eClass().getName().equals("Communication")) continue;
      EObject e = (EObject) o.eGet(ev, true);
      Object name = e == null ? null : e.eGet(e.eClass().getEStructuralFeature("name"));
      System.out.println("  event " + (e != null && e.eIsProxy() ? "PROXY" : "ok") + " " + name);
    }
    for (String w : r.warnings) System.out.println("WARNING " + w);
  }
}
