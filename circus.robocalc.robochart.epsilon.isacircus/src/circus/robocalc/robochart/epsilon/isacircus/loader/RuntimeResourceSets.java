package circus.robocalc.robochart.epsilon.isacircus.loader;

import java.util.function.Supplier;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

/**
 * Resource sets in which RoboChart models are linked by the runtime (non-UI) Xtext services, so that
 * cross-file references are resolved against the resources of the set only (generator issue C).
 *
 * In RoboTool the .rct resource factory registered globally comes from RoboChart's UI injector. There the
 * default IResourceDescriptions is DirtyStateAwareResourceDescriptions (bound in org.eclipse.xtext.ui.shared):
 * the workspace builder index plus open editors. Files not in the index (e.g. with Build Automatically off)
 * are invisible, and RctLoader's references stay unresolved even if all files are loaded into the set
 * (first RoboTool test, 5 Nov 2026).
 *
 * Here a separate runtime injector, created by RoboChartStandaloneSetup.createInjector() (without EMF
 * registration, so the global registries and the editors are untouched), supplies the resource sets: each is
 * an XtextResourceSet with .rct mapped, in its own factory map, to the runtime resource factory. Its default
 * IResourceDescriptions is ResourceSetBasedResourceDescriptions, as in the command-line tools.
 *
 * RUNTIME DEPENDENCY on the bundle circus.robocalc.robochart.textual, NOT declared in MANIFEST.MF:
 * RoboChartStandaloneSetup is loaded by name, at run time. Reason: the Maven/Tycho build resolves bundles only
 * from the update sites in the pom (Epsilon 2.4, Eclipse 2021-12, RoboStar metamodel site), and none of them
 * contains circus.robocalc.robochart.textual, so a Require-Bundle on it makes the build fail. RoboTool always
 * contains the bundle. Outside OSGi (dev-tools) the class is on the classpath. The alternative, an update
 * site with the textual bundle in the pom, is to be discussed with RoboStar.
 */
public class RuntimeResourceSets implements Supplier<ResourceSet> {

	public static final String TEXTUAL_BUNDLE = "circus.robocalc.robochart.textual";
	public static final String SETUP_CLASS = "circus.robocalc.robochart.textual.RoboChartStandaloneSetup";

	private final Injector injector;

	/** Creates the runtime injector; throws IllegalStateException with the reason if that is impossible. */
	public RuntimeResourceSets() {
		this(createRuntimeInjector());
	}

	public RuntimeResourceSets(Injector injector) {
		this.injector = injector;
	}

	@Override
	public ResourceSet get() {
		XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("rct", injector.getInstance(IResourceFactory.class));
		return rs;
	}

	static Injector createRuntimeInjector() {
		Class<?> setupClass = loadSetupClass();
		Object injector;
		try {
			Object setup = setupClass.getDeclaredConstructor().newInstance();
			injector = setupClass.getMethod("createInjector").invoke(setup);
		} catch (ReflectiveOperationException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			throw new IllegalStateException("Could not create the RoboChart runtime injector with " + SETUP_CLASS
				+ ".createInjector() (bundle " + TEXTUAL_BUNDLE + "): " + cause, cause);
		}
		if (!(injector instanceof Injector)) {
			throw new IllegalStateException(SETUP_CLASS + ".createInjector() returned " + describe(injector)
				+ ", which is not the com.google.inject.Injector seen by this plugin (" + describe(Injector.class)
				+ "): the bundles " + TEXTUAL_BUNDLE + " and circus.robocalc.robochart.epsilon.isacircus are wired"
				+ " to different Guice bundles.");
		}
		return (Injector) injector;
	}

	private static Class<?> loadSetupClass() {
		// outside OSGi (dev-tools): the class is on the classpath
		try {
			return Class.forName(SETUP_CLASS, true, RuntimeResourceSets.class.getClassLoader());
		} catch (ClassNotFoundException notOnClasspath) {
			// in RoboTool: load it through the bundle (not a declared dependency of this plugin)
		}
		org.osgi.framework.Bundle bundle;
		try {
			bundle = org.eclipse.core.runtime.Platform.getBundle(TEXTUAL_BUNDLE);
		} catch (RuntimeException | LinkageError e) {
			throw new IllegalStateException("Cannot load " + SETUP_CLASS + ": it is not on the classpath, and the OSGi"
				+ " platform is not available to find the bundle " + TEXTUAL_BUNDLE + " (" + e + ").", e);
		}
		if (bundle == null) {
			throw new IllegalStateException("The bundle " + TEXTUAL_BUNDLE + " (RoboChart textual language) is not"
				+ " installed in this RoboTool; it is needed to load the RoboChart models.");
		}
		try {
			return bundle.loadClass(SETUP_CLASS);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("The bundle " + TEXTUAL_BUNDLE + " " + bundle.getVersion()
				+ " does not provide the class " + SETUP_CLASS + ".", e);
		}
	}

	private static String describe(Object o) {
		if (o == null) return "null";
		Class<?> c = o instanceof Class ? (Class<?>) o : o.getClass();
		return c.getName() + " (class loader " + c.getClassLoader() + ")";
	}
}
