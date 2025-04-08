package kostovite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Component
public class ManualPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(ManualPluginLoader.class);
    private final List<PluginInterface> loadedPlugins = new ArrayList<>();

    public List<PluginInterface> loadPlugins(Path pluginsDir) {
        loadedPlugins.clear();

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
                }

            } catch (Exception e) {
                log.error("Error loading plugin from file: {}", file.getName(), e);
            }
        }

        return loadedPlugins;
    }

    public List<PluginInterface> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }
}