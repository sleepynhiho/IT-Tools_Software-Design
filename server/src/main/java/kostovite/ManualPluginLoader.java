package kostovite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ManualPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(ManualPluginLoader.class);
    private final List<PluginInterface> loadedPlugins = new ArrayList<>();
    private final Map<String, PluginInterface> pluginsByName = new ConcurrentHashMap<>();

    public List<PluginInterface> loadPlugins(Path pluginsDir) {
        loadedPlugins.clear();
        pluginsByName.clear();

        // Get all jar files in the plugins directory
        File dir = pluginsDir.toFile();
        File[] files = dir.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));

        if (files == null || files.length == 0) {
            log.info("No plugins found in directory: {}", pluginsDir);
            return loadedPlugins;
        }

        // Process each jar file
        for (File file : files) {
            try {
                log.info("Loading plugin from: {}", file.getAbsolutePath());

                // Create a new class loader for this jar
                URL[] urls = new URL[] { file.toURI().toURL() };
                URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

                // Use Java ServiceLoader to find all PluginInterface implementations
                ServiceLoader<PluginInterface> serviceLoader = ServiceLoader.load(PluginInterface.class, classLoader);

                // Add all found plugins to our list
                for (PluginInterface plugin : serviceLoader) {
                    log.info("Found plugin: {}", plugin.getName());
                    loadedPlugins.add(plugin);
                    pluginsByName.put(plugin.getName(), plugin);
                }

            } catch (Exception e) {
                log.error("Error loading plugin from file: {}", file.getName(), e);
            }
        }

        return loadedPlugins;
    }

    /**
     * Deletes a plugin by name
     * @param pluginName The name of the plugin to delete
     * @return true if the plugin was deleted, false otherwise
     */
    public boolean deletePlugin(String pluginName) {
        log.info("Attempting to delete plugin: {}", pluginName);

        // First unload the plugin
        boolean unloaded = unloadPlugin(pluginName);
        if (!unloaded) {
            log.warn("Could not unload plugin: {}", pluginName);
            // Continue anyway, we'll try to delete the file
        }

        try {
            // Get plugins directory
            Path pluginsDirectory = Paths.get(System.getProperty("user.dir"), "plugins-deploy");
            File[] jarFiles = pluginsDirectory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

            if (jarFiles == null) {
                log.error("No JAR files found in plugins directory");
                return false;
            }

            // Try to find and delete the JAR file
            for (File file : jarFiles) {
                try {
                    // Check if this JAR file corresponds to our plugin
                    // This is a simple approach - we're just checking if the file name contains the plugin name
                    // A more robust approach would be to load the JAR and verify the plugin name
                    if (file.getName().toLowerCase().contains(pluginName.toLowerCase())) {
                        log.info("Found matching JAR file for plugin {}: {}", pluginName, file.getName());

                        // Try to delete the file
                        if (file.delete()) {
                            log.info("Successfully deleted plugin file: {}", file.getName());
                            return true;
                        } else {
                            log.warn("Could not delete plugin file: {}", file.getName());

                            // Try force delete on JVM exit
                            file.deleteOnExit();
                            log.info("Scheduled plugin file for deletion on JVM exit: {}", file.getName());
                            return true;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error checking plugin file {}: {}", file.getName(), e.getMessage());
                }
            }

            log.warn("Could not find JAR file for plugin: {}", pluginName);
            return false;
        } catch (Exception e) {
            log.error("Error deleting plugin {}: {}", pluginName, e.getMessage(), e);
            return false;
        }
    }

    public List<PluginInterface> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    /**
     * Find a plugin by name
     * @param name Name of the plugin to find
     * @return The plugin if found, null otherwise
     */
    public PluginInterface getPluginByName(String name) {
        return pluginsByName.get(name);
    }

    /**
     * Get metadata for all loaded plugins
     * @return List of plugin metadata
     */
    public List<Map<String, Object>> getAllPluginMetadata() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PluginInterface plugin : loadedPlugins) {
            try {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", plugin.getName());
                // Add additional metadata if the plugin supports it
                if (plugin instanceof ExtendedPluginInterface) {
                    metadata.putAll(plugin.getMetadata());
                }
                result.add(metadata);
            } catch (Exception e) {
                log.error("Error getting metadata for plugin {}: {}", plugin.getName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * Process data with a specific plugin
     * @param pluginName Name of the plugin to use
     * @param input Input data for the plugin
     * @return Processed output data
     * @throws IllegalArgumentException if plugin not found
     */
    public Map<String, Object> processWithPlugin(String pluginName, Map<String, Object> input) {
        PluginInterface plugin = getPluginByName(pluginName);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginName);
        }

        // If plugin supports extended interface, use it
        if (plugin instanceof ExtendedPluginInterface) {
            return plugin.process(input);
        }

        // Otherwise, return basic info
        Map<String, Object> result = new HashMap<>();
        result.put("pluginName", plugin.getName());
        result.put("success", true);
        result.put("message", "Plugin executed, but doesn't implement advanced processing");
        return result;
    }

    /**
     * Extended interface for plugins with advanced functionality
     */
    public interface ExtendedPluginInterface extends PluginInterface {
        /**
         * Get metadata about the plugin
         * @return Map containing plugin metadata
         */
        Map<String, Object> getMetadata();

        /**
         * Process data universally
         * @param input Map containing input data
         * @return Map containing processed output data
         */
        Map<String, Object> process(Map<String, Object> input);
    }

    /**
     * Unload a specific plugin by name
     * @param pluginName Name of the plugin to unload
     * @return true if the plugin was successfully unloaded, false otherwise
     */
    public boolean unloadPlugin(String pluginName) {
        PluginInterface plugin = pluginsByName.get(pluginName);
        if (plugin == null) {
            log.warn("Cannot unload plugin '{}': Plugin not found", pluginName);
            return false;
        }

        try {
            // Remove from collections
            loadedPlugins.remove(plugin);
            pluginsByName.remove(pluginName);

            // Get the ClassLoader that loaded this plugin
            ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

            // If it's a URLClassLoader, try to close it
            if (pluginClassLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) pluginClassLoader).close();
                    log.info("Closed ClassLoader for plugin: {}", pluginName);
                } catch (IOException e) {
                    log.warn("Could not close ClassLoader for plugin: {}", pluginName, e);
                }
            }

            // Suggest garbage collection (though no guarantee it will run)
            plugin = null;
            System.gc();

            log.info("Successfully unloaded plugin: {}", pluginName);
            return true;
        } catch (Exception e) {
            log.error("Error unloading plugin {}: {}", pluginName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Unload all plugins
     * @return Number of plugins unloaded
     */
    public int unloadAllPlugins() {
        int count = loadedPlugins.size();
        try {
            loadedPlugins.clear();
            pluginsByName.clear();
            log.info("Successfully unloaded {} plugins", count);
            return count;
        } catch (Exception e) {
            log.error("Error unloading all plugins: {}", e.getMessage(), e);
            return 0;
        }
    }
}