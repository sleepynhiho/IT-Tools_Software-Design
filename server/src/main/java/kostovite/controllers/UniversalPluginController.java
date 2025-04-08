package kostovite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.ExtendedPluginInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugins/universal")
public class UniversalPluginController {

    private final ManualPluginLoader pluginLoader;

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
}