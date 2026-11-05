import java.io.File;
import java.util.*;
import org.eclipse.emf.ecore.*;
import org.eclipse.xtext.resource.XtextResourceSet;
import circus.robocalc.robochart.textual.RoboChartStandaloneSetup;
import circus.robocalc.robochart.epsilon.isacircus.loader.RctLoader;

/**
 * Loads a .rct with RctLoader and reports how the files were chosen, the event references of the
 * selected file, and the ambiguity warnings.
 * usage: LoadRct <file.rct> <project root> [closure|all]
 */
public class LoadRct {
  public static void main(String[] a) throws Exception {
    com.google.inject.Injector inj = new RoboChartStandaloneSetup().createInjectorAndDoEMFRegistration();
    RctLoader loader = new RctLoader(() -> inj.getInstance(XtextResourceSet.class), false);
    RctLoader.Mode mode = a.length > 2 && a[2].equals("all") ? RctLoader.Mode.ALL : RctLoader.Mode.CLOSURE;
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
