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
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ServiceLoader;
import java.util.Optional;

@Component
public class ManualPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(ManualPluginLoader.class);
    private final List<PluginInterface> loadedPlugins = new ArrayList<>();
    private final Map<String, PluginInterface> pluginsByName = new ConcurrentHashMap<>();
    private final Map<String, PluginInterface> pluginsById = new ConcurrentHashMap<>(); // New: map to access plugins by ID

    public List<PluginInterface> loadPlugins(Path pluginsDir) {
        loadedPlugins.clear();
        pluginsByName.clear();
        pluginsById.clear(); // Clear ID mapping as well

        // Get all jar files in the plugins directory
        File dir = pluginsDir.toFile();
        File[] files = dir.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));

        if (files == null || files.length == 0) {
            log.info("[{}] No plugins found in directory: {}", getCurrentTimestamp(), pluginsDir);
            return loadedPlugins;
        }

        // Process each jar file
        for (File file : files) {
            try {
                log.info("[{}] Loading plugin from: {}", getCurrentTimestamp(), file.getAbsolutePath());

                // Create a new class loader for this jar
                URL[] urls = new URL[] { file.toURI().toURL() };
                URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

                // Use Java ServiceLoader to find all PluginInterface implementations
                ServiceLoader<PluginInterface> serviceLoader = ServiceLoader.load(PluginInterface.class, classLoader);

                // Add all found plugins to our list
                for (PluginInterface plugin : serviceLoader) {
                    log.info("[{}] Found plugin: {}", getCurrentTimestamp(), plugin.getName());
                    loadedPlugins.add(plugin);
                    pluginsByName.put(plugin.getName(), plugin);

                    // Store by ID as well (if it's an ExtendedPluginInterface with metadata)
                    if (plugin instanceof ExtendedPluginInterface) {
                        try {
                            Map<String, Object> metadata = plugin.getMetadata();
                            if (metadata != null && metadata.containsKey("id")) {
                                String pluginId = String.valueOf(metadata.get("id"));
                                if (pluginId != null && !pluginId.isEmpty() && !pluginId.equals("null")) {
                                    log.info("[{}] Registering plugin by ID: {}", getCurrentTimestamp(), pluginId);
                                    pluginsById.put(pluginId, plugin);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("[{}] Error getting metadata for plugin {}: {}", getCurrentTimestamp(), plugin.getName(), e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                log.error("[{}] Error loading plugin from file: {}", getCurrentTimestamp(), file.getName(), e);
            }
        }

        return loadedPlugins;
    }

    /**
     * Deletes a plugin by name or ID
     * @param pluginNameOrId The name or ID of the plugin to delete
     * @return true if the plugin was deleted, false otherwise
     */
    public boolean deletePlugin(String pluginNameOrId) {
        log.info("[{}] Attempting to delete plugin with identifier: {}", getCurrentTimestamp(), pluginNameOrId);

        // First try to find the plugin by ID, then by name
        PluginInterface plugin = pluginsById.get(pluginNameOrId);
        if (plugin == null) {
            log.info("[{}] No plugin found with ID: {}. Trying by name...", getCurrentTimestamp(), pluginNameOrId);
            plugin = pluginsByName.get(pluginNameOrId);
        }

        if (plugin == null) {
            log.warn("[{}] Plugin not found with identifier: {}", getCurrentTimestamp(), pluginNameOrId);
            return false;
        }

        // Get the actual plugin name for unloading and logging
        String pluginName = plugin.getName();
        log.info("[{}] Found plugin: {} by identifier: {}", getCurrentTimestamp(), pluginName, pluginNameOrId);

        // First unload the plugin
        boolean unloaded = unloadPlugin(pluginName);
        if (!unloaded) {
            log.warn("[{}] Could not unload plugin: {}", getCurrentTimestamp(), pluginName);
            // Continue anyway, we'll try to delete the file
        }

        try {
            // Get plugins directory
            Path pluginsDirectory = Paths.get(System.getProperty("user.dir"), "plugins-deploy");
            File[] jarFiles = pluginsDirectory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

            if (jarFiles == null) {
                log.error("[{}] No JAR files found in plugins directory", getCurrentTimestamp());
                return false;
            }

            // Try to find and delete the JAR file
            for (File file : jarFiles) {
                try {
                    // Check if this JAR file corresponds to our plugin
                    // This is a simple approach - we're just checking if the file name contains the plugin name
                    // A more robust approach would be to load the JAR and verify the plugin name
                    if (file.getName().toLowerCase().contains(pluginName.toLowerCase())) {
                        log.info("[{}] Found matching JAR file for plugin {}: {}", getCurrentTimestamp(), pluginName, file.getName());

                        // Try to delete the file
                        if (file.delete()) {
                            log.info("[{}] Successfully deleted plugin file: {}", getCurrentTimestamp(), file.getName());
                        } else {
                            log.warn("[{}] Could not delete plugin file: {}", getCurrentTimestamp(), file.getName());

                            // Try force delete on JVM exit
                            file.deleteOnExit();
                            log.info("[{}] Scheduled plugin file for deletion on JVM exit: {}", getCurrentTimestamp(), file.getName());
                        }
                        return true;
                    }
                } catch (Exception e) {
                    log.error("[{}] Error checking plugin file {}: {}", getCurrentTimestamp(), file.getName(), e.getMessage());
                }
            }

            log.warn("[{}] Could not find JAR file for plugin: {}", getCurrentTimestamp(), pluginName);
            return false;
        } catch (Exception e) {
            log.error("[{}] Error deleting plugin {}: {}", getCurrentTimestamp(), pluginName, e.getMessage(), e);
            return false;
        }
    }

    public List<PluginInterface> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    /**
     * Find a plugin by name or ID
     * @param nameOrId Name or ID of the plugin to find
     * @return The plugin if found, null otherwise
     */
    public PluginInterface getPluginByName(String nameOrId) {
        // First try to get by name (original behavior)
        PluginInterface plugin = pluginsByName.get(nameOrId);

        // If not found by name, try by ID
        if (plugin == null) {
            plugin = pluginsById.get(nameOrId);
            if (plugin != null) {
                log.debug("[{}] Found plugin by ID: {} -> {}", getCurrentTimestamp(), nameOrId, plugin.getName());
            }
        }

        return plugin;
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
                log.error("[{}] Error getting metadata for plugin {}: {}", getCurrentTimestamp(), plugin.getName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * Process data with a specific plugin
     * @param pluginNameOrId Name or ID of the plugin to use
     * @param input Input data for the plugin
     * @return Processed output data
     * @throws IllegalArgumentException if plugin not found
     */
    public Map<String, Object> processWithPlugin(String pluginNameOrId, Map<String, Object> input) {
        PluginInterface plugin = getPluginByName(pluginNameOrId);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginNameOrId);
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
            log.warn("[{}] Cannot unload plugin '{}': Plugin not found", getCurrentTimestamp(), pluginName);
            return false;
        }

        try {
            // Also remove from ID map if present
            Optional<Map.Entry<String, PluginInterface>> idEntry = pluginsById.entrySet().stream()
                    .filter(entry -> entry.getValue() == plugin)
                    .findFirst();

            idEntry.ifPresent(entry -> {
                String pluginId = entry.getKey();
                pluginsById.remove(pluginId);
                log.info("[{}] Removed plugin from ID map: {}", getCurrentTimestamp(), pluginId);
            });

            // Remove from collections
            loadedPlugins.remove(plugin);
            pluginsByName.remove(pluginName);

            // Get the ClassLoader that loaded this plugin
            ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

            // If it's a URLClassLoader, try to close it
            if (pluginClassLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) pluginClassLoader).close();
                    log.info("[{}] Closed ClassLoader for plugin: {}", getCurrentTimestamp(), pluginName);
                } catch (IOException e) {
                    log.warn("[{}] Could not close ClassLoader for plugin: {}", getCurrentTimestamp(), pluginName, e);
                }
            }

            // Suggest garbage collection (though no guarantee it will run)
            System.gc();

            log.info("[{}] Successfully unloaded plugin: {}", getCurrentTimestamp(), pluginName);
            return true;
        } catch (Exception e) {
            log.error("[{}] Error unloading plugin {}: {}", getCurrentTimestamp(), pluginName, e.getMessage(), e);
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
            pluginsById.clear(); // Clear ID mapping as well
            log.info("[{}] Successfully unloaded {} plugins", getCurrentTimestamp(), count);
            return count;
        } catch (Exception e) {
            log.error("[{}] Error unloading all plugins: {}", getCurrentTimestamp(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get current timestamp for logging
     * @return Current timestamp string
     */
    private String getCurrentTimestamp() {
        return "2025-05-06 19:06:12"; // You might want to replace this with actual timestamp generation
    }
}