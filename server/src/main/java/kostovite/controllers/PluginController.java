package kostovite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {
    private final ManualPluginLoader manualPluginLoader;
    private final Path pluginsDirectory = Paths.get("plugins-deploy");

    @Autowired
    public PluginController(ManualPluginLoader manualPluginLoader) {
        this.manualPluginLoader = manualPluginLoader;

        // Ensure plugins directory exists
        try {
            if (!Files.exists(pluginsDirectory)) {
                Files.createDirectories(pluginsDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create plugins directory", e);
        }
    }

    @GetMapping
    public List<Map<String, Object>> getPlugins() {
        List<PluginInterface> plugins = manualPluginLoader.getLoadedPlugins();
        return plugins.stream()
                .map(plugin -> {
                    Map<String, Object> pluginInfo = new HashMap<>();
                    pluginInfo.put("name", plugin.getName());
                    // You could add more details from the plugin here
                    return pluginInfo;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/extensions")
    public List<Map<String, Object>> getExtensions() {
        List<PluginInterface> plugins = manualPluginLoader.getLoadedPlugins();
        return plugins.stream()
                .map(plugin -> {
                    Map<String, Object> extensionInfo = new HashMap<>();
                    extensionInfo.put("name", plugin.getName());
                    return extensionInfo;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/extensions/{extensionName}/execute")
    public ResponseEntity<Map<String, String>> executePlugin(@PathVariable String extensionName) {
        try {
            List<PluginInterface> plugins = manualPluginLoader.getLoadedPlugins();
            for (PluginInterface plugin : plugins) {
                if (plugin.getName().equals(extensionName)) {
                    plugin.execute();
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Extension '" + extensionName + "' executed successfully");
                    return ResponseEntity.ok(response);
                }
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Extension not found: " + extensionName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPlugin(@RequestParam("file") MultipartFile file) {
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Plugin file is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if file is a JAR
            if (!file.getOriginalFilename().endsWith(".jar")) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Only JAR files are supported");
                return ResponseEntity.badRequest().body(response);
            }

            // Save the file to the plugins directory
            Path targetPath = pluginsDirectory.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Reload all plugins using the manual loader
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            manualPluginLoader.loadPlugins(pluginsPath);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin uploaded and loaded successfully");
            response.put("pluginPath", targetPath.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to upload plugin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{pluginName}")
    public ResponseEntity<Map<String, String>> deletePlugin(@PathVariable String pluginName) {
        try {
            // Find plugin by name
            List<PluginInterface> plugins = manualPluginLoader.getLoadedPlugins();
            PluginInterface targetPlugin = null;

            for (PluginInterface plugin : plugins) {
                if (plugin.getName().equals(pluginName)) {
                    targetPlugin = plugin;
                    break;
                }
            }

            if (targetPlugin == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Plugin not found: " + pluginName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Find and delete the plugin JAR file
            boolean deleted = manualPluginLoader.deletePlugin(pluginName);

            if (!deleted) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to delete plugin file");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // Reload plugins
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            manualPluginLoader.loadPlugins(pluginsPath);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin deleted and unloaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete plugin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}