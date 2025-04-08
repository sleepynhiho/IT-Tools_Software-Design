package kostovite.controllers;

import kostovite.CustomPluginManager;
import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.ManualPluginLoader.ExtendedPluginInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    private static final Logger log = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private CustomPluginManager customPluginManager;

    @Autowired
    private ManualPluginLoader manualPluginLoader;

    @GetMapping("/inspect-jar")
    public Map<String, Object> inspectJar() {
        Map<String, Object> result = new HashMap<>();

        try {
            String workingDir = System.getProperty("user.dir");
            Path pluginsDir = Paths.get(workingDir, "plugins-deploy");
            File[] jarFiles = pluginsDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

            if (jarFiles == null || jarFiles.length == 0) {
                result.put("status", "error");
                result.put("message", "No JAR files found in plugins-deploy directory");
                return result;
            }

            File jarFile = jarFiles[0]; // Just inspect the first JAR
            Path jarPath = jarFile.toPath();

            result.put("jarPath", jarPath.toString());
            result.put("jarExists", Files.exists(jarPath));

            try (JarFile jar = new JarFile(jarFile)) {
                // Check manifest
                Manifest manifest = jar.getManifest();
                if (manifest != null) {
                    Map<String, String> manifestAttributes = new HashMap<>();
                    manifest.getMainAttributes().forEach((key, value) ->
                            manifestAttributes.put(key.toString(), value.toString()));
                    result.put("manifest", manifestAttributes);

                    // Check if Plugin-Class exists
                    String pluginClass = manifestAttributes.get("Plugin-Class");
                    if (pluginClass != null) {
                        result.put("pluginClassFound", jarContainsClass(jar, pluginClass));
                        result.put("pluginClassPath", pluginClass.replace('.', '/') + ".class");
                    }
                }

                // List all JAR contents
                List<String> jarContents = new ArrayList<>();
                jar.entries().asIterator().forEachRemaining(entry ->
                        jarContents.add(entry.getName()));
                result.put("jarContents", jarContents);

                // Check for extensions.idx
                JarEntry extensionsEntry = jar.getJarEntry("META-INF/extensions.idx");
                if (extensionsEntry != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(extensionsEntry)))) {
                        StringBuilder extensions = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            extensions.append(line).append("\n");
                        }
                        result.put("extensions", extensions.toString().trim());
                    }
                } else {
                    result.put("extensions", "No extensions.idx found");
                }

                // Try to load the plugin using direct classloading
                try {
                    URLClassLoader classLoader = new URLClassLoader(
                            new URL[]{jarPath.toUri().toURL()},
                            getClass().getClassLoader()
                    );

                    // Try to load the plugin class
                    assert manifest != null;
                    String pluginClass = manifest.getMainAttributes().getValue("Plugin-Class");
                    if (pluginClass != null) {
                        try {
                            Class<?> loadedClass = Class.forName(pluginClass, true, classLoader);
                            result.put("pluginClassLoaded", true);
                            result.put("pluginClassInfo", loadedClass.toString());
                        } catch (ClassNotFoundException e) {
                            result.put("pluginClassLoadError", e.toString());
                        }
                    }

                    // Try to load the extension class
                    JarEntry extensionsEntry2 = jar.getJarEntry("META-INF/extensions.idx");
                    if (extensionsEntry2 != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(extensionsEntry2)))) {
                            String extensionClass = reader.readLine();
                            if (extensionClass != null && !extensionClass.isEmpty()) {
                                try {
                                    Class<?> loadedClass = Class.forName(extensionClass, true, classLoader);
                                    result.put("extensionClassLoaded", true);
                                    result.put("extensionClassInfo", loadedClass.toString());
                                } catch (ClassNotFoundException e) {
                                    result.put("extensionClassLoadError", e.toString());
                                }
                            }
                        }
                    }

                    classLoader.close();
                } catch (Exception e) {
                    result.put("classLoaderError", e.toString());
                }
            }

            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return result;
    }

    private boolean jarContainsClass(JarFile jar, String className) {
        String classPath = className.replace('.', '/') + ".class";
        return jar.getEntry(classPath) != null;
    }

    @GetMapping("/manual-load")
    public ResponseEntity<Map<String, Object>> manualLoad() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get path to plugins directory
            String workingDir = System.getProperty("user.dir");
            Path pluginsPath = Paths.get(workingDir, "plugins-deploy").toAbsolutePath();

            // Load plugins and get list of loaded plugins
            List<PluginInterface> plugins = manualPluginLoader.loadPlugins(pluginsPath);

            List<String> pluginNames = plugins.stream()
                    .map(PluginInterface::getName)
                    .collect(Collectors.toList());

            response.put("loadedPlugins", pluginNames);
            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/extended-plugins-info")
    public ResponseEntity<Map<String, Object>> extendedPluginsInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<PluginInterface> plugins = manualPluginLoader.getLoadedPlugins();

            List<Map<String, Object>> pluginsInfo = new ArrayList<>();
            for (PluginInterface plugin : plugins) {
                Map<String, Object> pluginInfo = new HashMap<>();
                pluginInfo.put("name", plugin.getName());

                // Check if plugin implements the extended interface
                if (plugin instanceof ExtendedPluginInterface) {
                    pluginInfo.put("supportsUniversalInterface", true);
                    pluginInfo.put("metadata", plugin.getMetadata());
                } else {
                    pluginInfo.put("supportsUniversalInterface", false);
                }

                pluginsInfo.add(pluginInfo);
            }

            response.put("plugins", pluginsInfo);
            response.put("status", "success");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}