package kostovite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.ExtendedPluginInterface;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins/universal")
public class UniversalPluginController {

    private final ManualPluginLoader pluginLoader;

    @Autowired
    private ManualPluginLoader manualPluginLoader;

    @Autowired
    public UniversalPluginController(ManualPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPlugins() {
        Map<String, Object> response = new HashMap<>();
        List<PluginInterface> plugins = pluginLoader.getLoadedPlugins();

        response.put("count", plugins.size());
        response.put("plugins", plugins.stream().map(PluginInterface::getName).toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{pluginName}")
    public ResponseEntity<Map<String, Object>> processPluginData(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input) {

        try {
            // Find plugin by name
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
                return ResponseEntity.notFound().build();
            }

            // Check if plugin supports extended interface
            if (plugin instanceof ExtendedPluginInterface) {
                Map<String, Object> result = plugin.process(input);
                return ResponseEntity.ok(result);
            } else {
                // Fallback for basic plugins
                plugin.execute();

                // Create a basic response
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("message", "Plugin executed successfully but doesn't support data processing");
                fallbackResponse.put("pluginName", plugin.getName());
                return ResponseEntity.ok(fallbackResponse);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error processing data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
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

    @DeleteMapping("/unload/{pluginName}")
    public ResponseEntity<Map<String, Object>> unloadPlugin(@PathVariable String pluginName) {
        Map<String, Object> response = new HashMap<>();
        boolean success = manualPluginLoader.unloadPlugin(pluginName);

        if (success) {
            response.put("status", "success");
            response.put("message", "Plugin '" + pluginName + "' unloaded successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to unload plugin '" + pluginName + "'");
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unload-all")
    public ResponseEntity<Map<String, Object>> unloadAllPlugins() {
        Map<String, Object> response = new HashMap<>();
        int count = manualPluginLoader.unloadAllPlugins();

        response.put("status", "success");
        response.put("message", count + " plugins unloaded successfully");

        return ResponseEntity.ok(response);
    }

    // Add this method to your UniversalPluginController class

    @GetMapping("/{pluginName}/metadata")
    public ResponseEntity<Map<String, Object>> getPluginMetadata(@PathVariable String pluginName) {
        try {
            // Find plugin by name
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
                return ResponseEntity.notFound().build();
            }

            // Get and return the plugin's metadata
            Map<String, Object> metadata = plugin.getMetadata();
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error retrieving plugin metadata: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}