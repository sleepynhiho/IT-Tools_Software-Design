package kostovite.controllers;

import kostovite.CustomPluginManager;
import org.pf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Map<String, Object> manualLoad() {
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

            // Create a fresh DefaultPluginManager to test loading
            DefaultPluginManager testManager = new DefaultPluginManager(pluginsDir);
            testManager.loadPlugins();

            List<Map<String, Object>> loadedPlugins = new ArrayList<>();
            for (PluginWrapper plugin : testManager.getPlugins()) {
                Map<String, Object> pluginInfo = new HashMap<>();
                pluginInfo.put("id", plugin.getPluginId());
                pluginInfo.put("version", plugin.getDescriptor().getVersion());
                pluginInfo.put("state", plugin.getPluginState().toString());

                try {
                    testManager.startPlugin(plugin.getPluginId());
                    pluginInfo.put("startResult", "Started successfully");
                    pluginInfo.put("stateAfterStart", plugin.getPluginState().toString());
                } catch (Exception e) {
                    pluginInfo.put("startError", e.toString());
                }

                List<String> extensions = (List<String>) testManager.getExtensions(plugin.getPluginId())
                        .stream()
                        .map(ext -> ext.getClass().getName())
                        .collect(Collectors.toList());
                pluginInfo.put("extensions", extensions);

                loadedPlugins.add(pluginInfo);
            }

            result.put("loadedPlugins", loadedPlugins);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }

        return result;
    }
}