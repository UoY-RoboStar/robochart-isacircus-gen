import java.io.File;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.EglTemplateFactory;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.module.IModule;

/** Parses .eol and .egl files with the Epsilon engine and prints the parse problems.
 *  usage: ParseCheck <file>...   (exit code 1 if a file has problems) */
public class ParseCheck {
  public static void main(String[] a) throws Exception {
    boolean failed = false;
    for (String f : a) {
      IModule m = f.endsWith(".eol") ? new EolModule() : new EglTemplateFactoryModuleAdapter(new EglTemplateFactory());
      boolean ok = m.parse(new File(f)) && m.getParseProblems().isEmpty();
      System.out.println((ok ? "OK   " : "FAIL ") + f);
      for (ParseProblem p : m.getParseProblems()) System.out.println("   " + p);
      failed |= !ok;
    }
    System.exit(failed ? 1 : 0);
  }
}
