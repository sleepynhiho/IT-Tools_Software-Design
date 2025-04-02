package kostovite;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.nio.file.Paths;
import java.util.List;

public class CustomPluginManager {
    private final PluginManager pluginManager;

    public CustomPluginManager() {
        pluginManager = new DefaultPluginManager(Paths.get("build/plugins-deploy"));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    public List<PluginInterface> getPlugins() {
        return pluginManager.getExtensions(PluginInterface.class);
    }

    public void executePlugin(String pluginName) {
        for (PluginInterface plugin : getPlugins()) {
            if (plugin.getName().equals(pluginName)) {
                plugin.execute();
                return;
            }
        }
        throw new IllegalArgumentException("Plugin not found: " + pluginName);
    }

    public void loadPlugin(String pluginPath) {
        String pluginId = pluginManager.loadPlugin(Paths.get(pluginPath));
        pluginManager.startPlugin(pluginId);
    }

    public void unloadPlugin(String pluginId) {
        pluginManager.stopPlugin(pluginId);
        pluginManager.unloadPlugin(pluginId);
    }
}