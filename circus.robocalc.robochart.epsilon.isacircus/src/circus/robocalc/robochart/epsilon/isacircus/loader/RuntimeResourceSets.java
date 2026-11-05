package circus.robocalc.robochart.epsilon.isacircus.loader;

import java.util.function.Supplier;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import circus.robocalc.robochart.textual.RoboChartStandaloneSetup;

/**
 * Resource sets in which RoboChart models are linked by the runtime (non-UI) Xtext services, so that
 * cross-file references are resolved against the resources of the set only (generator issue C).
 *
 * In RoboTool the .rct resource factory registered globally comes from RoboChart's UI injector. There the
 * default IResourceDescriptions is DirtyStateAwareResourceDescriptions (bound in org.eclipse.xtext.ui.shared):
 * the workspace builder index plus open editors. Files not in the index (e.g. with Build Automatically off)
 * are invisible, and RctLoader's references stay unresolved even if all files are loaded into the set.
 *
 * Here a separate runtime injector (RoboChartStandaloneSetup.createInjector(), without EMF registration, so
 * the global registries and the editors are untouched) supplies the resource sets: each is an
 * XtextResourceSet with .rct mapped, in its own factory map, to the runtime resource factory. Its default
 * IResourceDescriptions is ResourceSetBasedResourceDescriptions, as in the command-line tools.
 */
public class RuntimeResourceSets implements Supplier<ResourceSet> {

	private final Injector injector;

	public RuntimeResourceSets() {
		this(new RoboChartStandaloneSetup().createInjector());
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
}
