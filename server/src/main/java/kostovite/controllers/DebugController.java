package kostovite.controllers;

import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.ManualPluginLoader.ExtendedPluginInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    private static final Logger log = LoggerFactory.getLogger(DebugController.class);

    private final ManualPluginLoader pluginLoader;

    @Autowired
    public DebugController(ManualPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

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

    @GetMapping("/extended-plugins-info")
    public ResponseEntity<Map<String, Object>> extendedPluginsInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<PluginInterface> plugins = pluginLoader.getLoadedPlugins();

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

    @GetMapping("/MediaTools/testimage")
    public ResponseEntity<Map<String, Object>> testMediaToolsImage() {
        try {
            // Find the MediaTools plugin
            PluginInterface plugin = null;
            for (PluginInterface p : pluginLoader.getLoadedPlugins()) {
                if (p.getName().equals("MediaTools")) {
                    plugin = p;
                    break;
                }
            }

            if (plugin == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "MediaTools plugin not found");
                return ResponseEntity.notFound().build();
            }

            // Create a 1x1 pixel test image programmatically
            BufferedImage testImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            testImage.setRGB(0, 0, 0xFF0000); // Red pixel

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(testImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Create test input
            Map<String, Object> testInput = new HashMap<>();
            testInput.put("operation", "getImageInfo");
            testInput.put("image", imageBytes); // Pass raw bytes instead of base64

            // Process with the plugin using reflection
            Map<String, Object> result = new HashMap<>();

            // Check if the plugin has a process method regardless of interface
            try {
                Method processMethod = plugin.getClass().getMethod("process", Map.class);
                Object processResult = processMethod.invoke(plugin, testInput);

                if (processResult instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapResult = (Map<String, Object>) processResult;
                    return ResponseEntity.ok(mapResult);
                } else {
                    result.put("error", "Plugin process method returned unexpected type: " +
                            (processResult != null ? processResult.getClass().getName() : "null"));
                }
            } catch (NoSuchMethodException e) {
                result.put("error", "Plugin does not have a process method");
            } catch (Exception e) {
                result.put("error", "Error invoking process method: " + e.getMessage());
                result.put("stackTrace", e.toString());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error testing MediaTools plugin: " + e.getMessage());
            response.put("stackTrace", e.toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{pluginName}/process")
    public ResponseEntity<Map<String, Object>> processPluginRequest(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input) {

        // Find the plugin
        PluginInterface plugin = null;
        for (PluginInterface p : pluginLoader.getLoadedPlugins()) {
            if (p.getName().equals(pluginName)) {
                plugin = p;
                break;
            }
        }

        if (plugin == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Plugin not found: " + pluginName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Process with the plugin using reflection
        try {
            Method processMethod = plugin.getClass().getMethod("process", Map.class);
            Object processResult = processMethod.invoke(plugin, input);

            if (processResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) processResult;
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Plugin process method returned unexpected type");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing plugin request: " + e.getMessage());
            errorResponse.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}