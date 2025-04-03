package kostovite.controllers;

import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import kostovite.PluginInterface;
import kostovite.CustomPluginManager;

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
    private final CustomPluginManager customPluginManager;
    private final Path pluginsDirectory = Paths.get("plugins-deploy");

    @Autowired
    public PluginController(CustomPluginManager customPluginManager) {
        this.customPluginManager = customPluginManager;

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
        return customPluginManager.getPluginWrappers().stream()
                .map(plugin -> {
                    Map<String, Object> pluginInfo = new HashMap<>();
                    pluginInfo.put("id", plugin.getPluginId());
                    pluginInfo.put("version", plugin.getDescriptor().getVersion());
                    pluginInfo.put("state", plugin.getPluginState().toString());
                    pluginInfo.put("path", plugin.getPluginPath().toString());
                    return pluginInfo;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/extensions")
    public List<Map<String, Object>> getExtensions() {
        return customPluginManager.getPlugins().stream()
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
            customPluginManager.executePlugin(extensionName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Extension '" + extensionName + "' executed successfully");
            return ResponseEntity.ok(response);
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

            // File system watcher should detect the new file and load the plugin automatically
            // But we can also explicitly load it for immediate feedback
            String pluginId = customPluginManager.loadPlugin(targetPath);

            if (pluginId != null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Plugin uploaded and loaded successfully");
                response.put("pluginId", pluginId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to load plugin");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to upload plugin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{pluginId}")
    public ResponseEntity<Map<String, String>> deletePlugin(@PathVariable String pluginId) {
        try {
            // Find plugin by ID
            PluginWrapper pluginWrapper = null;
            for (PluginWrapper pw : customPluginManager.getPluginWrappers()) {
                if (pw.getPluginId().equals(pluginId)) {
                    pluginWrapper = pw;
                    break;
                }
            }

            if (pluginWrapper == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Plugin not found: " + pluginId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Get plugin file path
            Path pluginPath = pluginWrapper.getPluginPath();

            // Unload the plugin
            customPluginManager.unloadPlugin(pluginId);

            // Delete the file
            Files.deleteIfExists(pluginPath);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin unloaded and deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete plugin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}