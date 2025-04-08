package kostovite;

import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomPluginManager extends DefaultPluginManager {
    private static final Logger log = LoggerFactory.getLogger(CustomPluginManager.class);

    public CustomPluginManager() {
        super();
        try {
            // Your existing initialization code
            String workingDir = System.getProperty("user.dir");
            log.info("Working directory: {}", workingDir);

            Path pluginsDir = Paths.get(workingDir, "plugins-deploy");
            log.info("Plugin directory path: {}", pluginsDir);

            // Check if plugins directory exists and log file information
            File pluginsDirFile = pluginsDir.toFile();
            if (pluginsDirFile.exists() && pluginsDirFile.isDirectory()) {
                File[] files = pluginsDirFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (files != null) {
                    log.info("Found {} files in plugins directory", files.length);
                    for (File file : files) {
                        log.info("File in plugins directory: {} (size: {} bytes)", file.getName(), file.length());
                    }
                }
            }

            // Load and start plugins
            loadAndStartPlugins();
        } catch (Exception e) {
            // Log the error but don't let it propagate
            log.error("Error initializing PF4J plugin manager: {}", e.getMessage());
            log.debug("Detailed error", e);
        }
    }

    @Override
    protected PluginLoader createPluginLoader() {
        try {
            return super.createPluginLoader();
        } catch (Exception e) {
            log.error("Error creating plugin loader: {}", e.getMessage());
            // Return a no-op plugin loader that doesn't throw exceptions
            return new DefaultPluginLoader(this) {
                @Override
                public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
                    return getClass().getClassLoader();
                }
            };
        }
    }

    public void loadAndStartPlugins() {
        try {
            // Load plugins from the plugins directory
            Path pluginsDir = Paths.get(System.getProperty("user.dir"), "plugins-deploy");
            log.info("Loading plugins from: {}", pluginsDir);

            // Get all jar files in the plugins directory
            Set<String> pluginPaths = getPluginPaths(pluginsDir);
            pluginPaths.forEach(path -> {
                try {
                    log.info("Attempting to load plugin from: {}", path);
                    PluginWrapper pluginWrapper = loadPluginFromPath(Paths.get(path));
                    if (pluginWrapper != null) {
                        log.info("Loaded plugin with ID: {}", pluginWrapper.getPluginId());
                    }
                } catch (Exception e) {
                    log.error("Error loading plugin from {}: {}", path, e.getMessage());
                }
            });

            // Log loaded plugins
            List<PluginWrapper> loadedPlugins = getPlugins();
            log.info("Loaded {} plugins", loadedPlugins.size());
            for (PluginWrapper plugin : loadedPlugins) {
                log.info("Loaded plugin: {} ({})", plugin.getPluginId(), plugin.getDescriptor().getVersion());
            }

            // Start the plugins safely
            log.info("Starting plugins...");
            try {
                startPluginsSafely();
            } catch (Exception e) {
                log.error("Error starting plugins: {}", e.getMessage());
            }

            // Log plugin states
            for (PluginWrapper plugin : loadedPlugins) {
                log.info("Plugin {} state: {}", plugin.getPluginId(), plugin.getPluginState());
                if (plugin.getPluginState() == PluginState.FAILED && plugin.getFailedException() != null) {
                    log.error("Plugin failed to start: {}", plugin.getFailedException().getMessage());
                }
            }

            // Get and log extensions
            try {
                List<PluginInterface> extensions = getExtensions(PluginInterface.class);
                log.info("Found {} extensions implementing PluginInterface", extensions.size());
            } catch (Exception e) {
                log.error("Error getting extensions: {}", e.getMessage());
            }

            // Start watching for changes
            try {
                log.info("Started watching plugins directory for changes");

                // Additional diagnostics
                File[] files = pluginsDir.toFile().listFiles();
                if (files != null) {
                    log.info("Plugin directory contains {} files", files.length);
                    for (File file : files) {
                        String sizeInKB = String.format("%.0fKB", file.length() / 1024.0);
                        log.info("File in plugin directory: {} ({})", file.getName(), sizeInKB);
                    }
                }

                log.info("Currently loaded plugins: {}", loadedPlugins.size());
                for (PluginWrapper plugin : loadedPlugins) {
                    log.info("Plugin: {} ({}) - State: {}",
                            plugin.getPluginId(),
                            plugin.getDescriptor().getVersion(),
                            plugin.getPluginState());
                }

                try {
                    List<PluginInterface> availableExtensions = getExtensions(PluginInterface.class);
                    log.info("Available extensions: {}", availableExtensions.size());
                } catch (Exception e) {
                    log.info("Available extensions: 0");
                }

            } catch (Exception e) {
                log.error("Error in plugin watcher: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error loading plugins: {}", e.getMessage());
        }
    }

    private Set<String> getPluginPaths(Path pluginsDir) {
        try {
            File dir = pluginsDir.toFile();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));
                if (files != null) {
                    return java.util.Arrays.stream(files)
                            .map(File::getAbsolutePath)
                            .collect(Collectors.toSet());
                }
            }
        } catch (Exception e) {
            log.error("Error getting plugin paths: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    private void startPluginsSafely() {
        List<PluginWrapper> loadedPlugins = getPlugins();
        for (PluginWrapper plugin : loadedPlugins) {
            try {
                if (plugin.getPluginState() != PluginState.STARTED && plugin.getPluginState() != PluginState.DISABLED) {
                    PluginState state = startPlugin(plugin.getPluginId());
                    log.info("Plugin {} state after start attempt: {}", plugin.getPluginId(), state);
                }
            } catch (Exception e) {
                log.error("Error starting plugin {}: {}", plugin.getPluginId(), e.getMessage());
            }
        }
    }

    @Override
    public List<PluginWrapper> getPlugins() {
        try {
            return super.getPlugins();
        } catch (Exception e) {
            log.error("Error getting plugins: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public PluginState startPlugin(String pluginId) {
        try {
            return super.startPlugin(pluginId);
        } catch (Exception e) {
            log.error("Error starting plugin {}: {}", pluginId, e.getMessage());
            return PluginState.FAILED;
        }
    }

    @Override
    public void startPlugins() {
        try {
            super.startPlugins();
        } catch (Exception e) {
            log.error("Error starting plugins: {}", e.getMessage());
        }
    }

    @Override
    public PluginWrapper getPlugin(String pluginId) {
        try {
            return super.getPlugin(pluginId);
        } catch (Exception e) {
            log.error("Error getting plugin {}: {}", pluginId, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type) {
        try {
            return super.getExtensions(type);
        } catch (Exception e) {
            log.error("Error getting extensions of type {}: {}", type.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // ADDITIONAL METHODS REQUIRED BY PLUGINCONTROLLER

    /**
     * Get all plugin wrappers
     */
    public List<PluginWrapper> getPluginWrappers() {
        try {
            return getPlugins();
        } catch (Exception e) {
            log.error("Error getting plugin wrappers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Execute a specific plugin by name
     */
    public void executePlugin(String extensionName) {
        try {
            List<PluginInterface> extensions = getExtensions(PluginInterface.class);
            for (PluginInterface extension : extensions) {
                if (extension.getName().equals(extensionName)) {
                    extension.execute();
                    return;
                }
            }
            throw new IllegalArgumentException("Extension not found: " + extensionName);
        } catch (Exception e) {
            log.error("Error executing plugin {}: {}", extensionName, e.getMessage());
            throw new RuntimeException("Failed to execute plugin: " + e.getMessage(), e);
        }
    }

    /**
     * Load a plugin from a path and return its ID
     */
    public String loadPlugin(Path pluginPath) {
        try {
            PluginWrapper pluginWrapper = loadPluginFromPath(pluginPath);
            if (pluginWrapper == null) {
                log.error("Failed to load plugin from {}", pluginPath);
                return null;
            }

            String pluginId = pluginWrapper.getPluginId();
            PluginState state = startPlugin(pluginId);
            log.info("Loaded and started plugin {} with state {}", pluginId, state);
            return pluginId;
        } catch (Exception e) {
            log.error("Error loading plugin from {}: {}", pluginPath, e.getMessage());
            return null;
        }
    }

    /**
     * Unload a plugin by ID
     */
    public boolean unloadPlugin(String pluginId) {
        try {
            return super.unloadPlugin(pluginId);
        } catch (Exception e) {
            log.error("Error unloading plugin {}: {}", pluginId, e.getMessage());
            return false;
        }
    }
}