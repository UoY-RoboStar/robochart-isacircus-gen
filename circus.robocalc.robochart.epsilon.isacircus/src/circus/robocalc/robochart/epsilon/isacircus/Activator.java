package circus.robocalc.robochart.epsilon.isacircus;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "circus.robocalc.robochart.epsilon.isacircus";
    private static Activator plugin;

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Cleanup Isabelle session and server on Eclipse shutdown
        try {
            circus.robocalc.robochart.epsilon.isacircus.ui.handlers.RunIsabelleHandlerR0.shutdown();
        } catch (Exception e) {
            // ignore errors during shutdown
        }
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }
}