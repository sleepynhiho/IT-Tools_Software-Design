package kostovite;

import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CustomPluginManager {
    private static final Logger log = LoggerFactory.getLogger(CustomPluginManager.class);
    private final PluginManager pluginManager;
    private final Path pluginsPath;
    private final WatchService watchService;
    private final ScheduledExecutorService executorService;
    private volatile boolean watching = true;

    public CustomPluginManager() throws IOException {
        // Get the current working directory
        super();
        String workingDir = System.getProperty("user.dir");
        log.info("Working directory: {}", workingDir);

        // Define the plugin directory path - use ABSOLUTE path to avoid confusion
        String pluginsPathStr = workingDir + File.separator + "plugins-deploy";
        this.pluginsPath = Paths.get(pluginsPathStr).toAbsolutePath().normalize();

        log.info("Plugin directory path: {}", pluginsPath);

        // Create the directory if it doesn't exist
        if (!Files.exists(pluginsPath)) {
            Files.createDirectories(pluginsPath);
            log.info("Created plugins directory at: {}", pluginsPath);
        }

        // Log the contents of the plugin directory
        File pluginsDir = pluginsPath.toFile();
        File[] files = pluginsDir.listFiles();
        if (files != null) {
            log.info("Found {} files in plugins directory", files.length);
            for (File file : files) {
                log.info("File in plugins directory: {} (size: {} bytes)", file.getName(), file.length());
            }
        } else {
            log.warn("No files found in plugins directory or directory cannot be accessed");
        }

        // Initialize plugin manager with the plugins directory
        pluginManager = new DefaultPluginManager(pluginsPath) {
            @Override
            protected PluginLoader createPluginLoader() {
                return new DefaultPluginLoader(this) {
                    @Override
                    protected PluginClassLoader createPluginClassLoader(Path pluginPath, PluginDescriptor pluginDescriptor) {
                        PluginClassLoader pluginClassLoader = new PluginClassLoader((PluginManager) CustomPluginManager.this, pluginDescriptor, getClass().getClassLoader());

                        // Add plugin JAR to classpath
                        pluginClassLoader.addFile(pluginPath.toFile());

                        return pluginClassLoader;
                    }
                };
            }
        };

        // Setup file system watcher
        this.watchService = FileSystems.getDefault().newWatchService();
        pluginsPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

        // Initialize the executor service for polling the watch service
        this.executorService = Executors.newSingleThreadScheduledExecutor();

        // Load and start existing plugins
        loadAndStartPlugins();
    }

    private void loadAndStartPlugins() {
        log.info("Loading plugins from: {}", pluginsPath);

        // Load plugins from the directory
        pluginManager.loadPlugins();

        // Log loaded plugins
        List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();
        log.info("Loaded {} plugins", loadedPlugins.size());
        for (PluginWrapper plugin : loadedPlugins) {
            log.info("Loaded plugin: {} ({})", plugin.getPluginId(), plugin.getDescriptor().getVersion());
        }

        // Start the plugins
        log.info("Starting plugins...");
        pluginManager.startPlugins();

        // Log started plugins and their states
        for (PluginWrapper plugin : loadedPlugins) {
            log.info("Plugin {} state: {}", plugin.getPluginId(), plugin.getPluginState());
        }

        // Log available extensions
        List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class);
        log.info("Found {} extensions implementing PluginInterface", extensions.size());
        for (PluginInterface extension : extensions) {
            log.info("Found extension: {}", extension.getName());
        }
    }

    @PostConstruct
    public void startWatching() {
        // Start watching for changes in the plugins directory
        executorService.scheduleAtFixedRate(() -> {
            if (!watching) return;

            WatchKey key;
            try {
                key = watchService.poll();
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = (Path) event.context();

                        log.info("Detected file system event: {} on file: {}", kind, fileName);

                        // Only process jar files
                        if (!fileName.toString().toLowerCase().endsWith(".jar")) {
                            continue;
                        }

                        Path fullPath = pluginsPath.resolve(fileName);

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            handlePluginCreated(fullPath);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            handlePluginDeleted(fileName.toString());
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            handlePluginModified(fullPath);
                        }
                    }

                    // Reset the key to receive further events
                    boolean valid = key.reset();
                    if (!valid) {
                        log.warn("Watch key is no longer valid");
                        watching = false;
                    }
                }
            } catch (Exception e) {
                log.error("Error while watching plugins directory", e);
            }
        }, 0, 2, TimeUnit.SECONDS);

        log.info("Started watching plugins directory for changes");

        // Log the current state of things right after starting
        logPluginStatus();
    }

    private void logPluginStatus() {
        // Log plugin directory contents
        File dir = pluginsPath.toFile();
        File[] files = dir.listFiles();
        if (files != null) {
            log.info("Plugin directory contains {} files", files.length);
            for (File file : files) {
                log.info("File in plugin directory: {} ({}KB)",
                        file.getName(), file.length() / 1024);
            }
        } else {
            log.warn("Could not list files in plugin directory");
        }

        // Log loaded plugins
        List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();
        log.info("Currently loaded plugins: {}", loadedPlugins.size());
        for (PluginWrapper plugin : loadedPlugins) {
            log.info("Plugin: {} ({}) - State: {}",
                    plugin.getPluginId(),
                    plugin.getDescriptor().getVersion(),
                    plugin.getPluginState());
        }

        // Log available extensions
        List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class);
        log.info("Available extensions: {}", extensions.size());
        for (PluginInterface extension : extensions) {
            log.info("Extension: {}", extension.getName());
        }
    }

    private void handlePluginCreated(Path pluginPath) {
        log.info("New plugin detected: {}", pluginPath);
        String pluginId = pluginManager.loadPlugin(pluginPath);
        if (pluginId != null) {
            pluginManager.startPlugin(pluginId);
            log.info("Successfully loaded and started plugin: {}", pluginId);

            // Log extensions from this plugin
            List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class, pluginId);
            log.info("Plugin {} provides {} extensions", pluginId, extensions.size());
            for (PluginInterface extension : extensions) {
                log.info("Extension from plugin {}: {}", pluginId, extension.getName());
            }
        } else {
            log.error("Failed to load plugin from path: {}", pluginPath);
        }
    }

    private void handlePluginDeleted(String fileName) {
        // Find plugin ID by file name
        for (PluginWrapper plugin : pluginManager.getPlugins()) {
            if (plugin.getPluginPath().getFileName().toString().equals(fileName)) {
                String pluginId = plugin.getPluginId();
                log.info("Plugin removed: {}", pluginId);
                pluginManager.stopPlugin(pluginId);
                pluginManager.unloadPlugin(pluginId);
                return;
            }
        }
        log.warn("Could not find plugin ID for deleted file: {}", fileName);
    }

    private void handlePluginModified(Path pluginPath) {
        String fileName = pluginPath.getFileName().toString();
        // Find plugin ID by file name
        for (PluginWrapper plugin : pluginManager.getPlugins()) {
            if (plugin.getPluginPath().getFileName().toString().equals(fileName)) {
                String pluginId = plugin.getPluginId();
                log.info("Plugin modified: {}", pluginId);

                // Stop and unload the old plugin
                pluginManager.stopPlugin(pluginId);
                pluginManager.unloadPlugin(pluginId);

                // Load and start the new version
                String newPluginId = pluginManager.loadPlugin(pluginPath);
                if (newPluginId != null) {
                    pluginManager.startPlugin(newPluginId);
                    log.info("Successfully reloaded plugin: {}", newPluginId);

                    // Log extensions from this plugin
                    List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class, newPluginId);
                    log.info("Reloaded plugin {} provides {} extensions", newPluginId, extensions.size());
                    for (PluginInterface extension : extensions) {
                        log.info("Extension from reloaded plugin {}: {}", newPluginId, extension.getName());
                    }
                } else {
                    log.error("Failed to reload plugin from path: {}", pluginPath);
                }
                return;
            }
        }
    }

    public List<PluginInterface> getPlugins() {
        List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class);
        log.debug("Retrieved {} extensions", extensions.size());
        return extensions;
    }

    public List<PluginWrapper> getPluginWrappers() {
        return pluginManager.getPlugins();
    }

    public void executePlugin(String pluginName) {
        List<PluginInterface> extensions = getPlugins();
        log.info("Executing plugin with name '{}'. Available extensions: {}",
                pluginName, extensions.size());

        for (PluginInterface plugin : extensions) {
            log.info("Checking extension: {}", plugin.getName());
            if (plugin.getName().equals(pluginName)) {
                log.info("Executing plugin: {}", pluginName);
                plugin.execute();
                return;
            }
        }
        log.error("Plugin not found: {}", pluginName);
        throw new IllegalArgumentException("Plugin not found: " + pluginName);
    }

    public String loadPlugin(Path pluginPath) {
        log.info("Explicitly loading plugin from: {}", pluginPath);
        String pluginId = pluginManager.loadPlugin(pluginPath);
        if (pluginId != null) {
            log.info("Plugin loaded with ID: {}", pluginId);
            pluginManager.startPlugin(pluginId);
            log.info("Plugin started: {}", pluginId);
        } else {
            log.error("Failed to load plugin from: {}", pluginPath);
        }
        return pluginId;
    }

    public void unloadPlugin(String pluginId) {
        log.info("Unloading plugin: {}", pluginId);
        pluginManager.stopPlugin(pluginId);
        pluginManager.unloadPlugin(pluginId);
    }

    // Method for debugging plugin issues
    public String debugPluginInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Plugin Directory: ").append(pluginsPath).append("\n");

        // Check directory contents
        File dir = pluginsPath.toFile();
        File[] files = dir.listFiles();
        info.append("Files in directory: ").append(files != null ? files.length : "directory not accessible").append("\n");
        if (files != null) {
            for (File file : files) {
                info.append("  ").append(file.getName()).append(" (").append(file.length()).append(" bytes)\n");
            }
        }

        // Check loaded plugins
        List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();
        info.append("Loaded plugins: ").append(loadedPlugins.size()).append("\n");
        for (PluginWrapper plugin : loadedPlugins) {
            info.append("  ").append(plugin.getPluginId())
                    .append(" (").append(plugin.getDescriptor().getVersion()).append(")")
                    .append(" - State: ").append(plugin.getPluginState())
                    .append(" - Path: ").append(plugin.getPluginPath())
                    .append("\n");
        }

        // Check extensions
        List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class);
        info.append("Available extensions: ").append(extensions.size()).append("\n");
        for (PluginInterface extension : extensions) {
            info.append("  ").append(extension.getName())
                    .append(" (").append(extension.getClass().getName()).append(")")
                    .append("\n");
        }

        return info.toString();
    }

    @PreDestroy
    public void shutdown() {
        watching = false;
        executorService.shutdownNow();
        try {
            watchService.close();
        } catch (IOException e) {
            log.error("Error closing watch service", e);
        }

        // Stop all plugins
        pluginManager.stopPlugins();
        log.info("CustomPluginManager shut down");
    }

    // Add this method
    public void reloadAllPlugins() {
        log.info("Reloading all plugins from: {}", pluginsPath);

        // Stop and unload all existing plugins
        List<PluginWrapper> plugins = new ArrayList<>(pluginManager.getPlugins());
        for (PluginWrapper plugin : plugins) {
            pluginManager.stopPlugin(plugin.getPluginId());
            pluginManager.unloadPlugin(plugin.getPluginId());
        }

        // Load and start all plugins in the directory
        try {
            Files.list(pluginsPath)
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    .forEach(path -> {
                        try {
                            log.info("Loading plugin: {}", path);
                            String pluginId = pluginManager.loadPlugin(path);
                            if (pluginId != null) {
                                log.info("Starting plugin: {}", pluginId);
                                pluginManager.startPlugin(pluginId);

                                // Check for extensions
                                List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class, pluginId);
                                log.info("Plugin {} provides {} extensions", pluginId, extensions.size());
                                for (PluginInterface extension : extensions) {
                                    log.info("Found extension: {} from plugin {}", extension.getName(), pluginId);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error loading plugin: {}", path, e);
                        }
                    });
        } catch (Exception e) {
            log.error("Error reloading plugins", e);
        }
    }

    // Add to your CustomPluginManager class
    public void startPlugin(String pluginId) {
        try {
            log.info("Starting plugin: {}", pluginId);
            pluginManager.startPlugin(pluginId);

            // Check plugin state
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin != null) {
                log.info("Plugin state after starting: {}", plugin.getPluginState());

                // Check extensions
                List<PluginInterface> extensions = pluginManager.getExtensions(PluginInterface.class, pluginId);
                log.info("Found {} extensions from plugin {}", extensions.size(), pluginId);
                for (PluginInterface extension : extensions) {
                    log.info("Registered extension: {}", extension.getName());
                }
            } else {
                log.warn("Plugin with ID {} not found after loading", pluginId);
            }
        } catch (Exception e) {
            log.error("Error starting plugin: {}", pluginId, e);
        }
    }
}