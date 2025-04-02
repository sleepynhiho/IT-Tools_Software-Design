package kostovite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import kostovite.PluginInterface;
import kostovite.CustomPluginManager;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {
    private final CustomPluginManager customPluginManager;

    @Autowired
    public PluginController(CustomPluginManager customPluginManager) {
        this.customPluginManager = customPluginManager;
    }

    @GetMapping
    public List<String> getPlugins() {
        return customPluginManager.getPlugins().stream()
                .map(PluginInterface::getName)
                .collect(Collectors.toList());
    }

    @GetMapping("/{pluginName}/execute")
    public void executePlugin(@PathVariable String pluginName) {
        customPluginManager.executePlugin(pluginName);
    }

    @PostMapping("/load")
    public void loadPlugin(@RequestParam String pluginPath) {
        customPluginManager.loadPlugin(pluginPath);
    }

    @PostMapping("/unload")
    public void unloadPlugin(@RequestParam String pluginId) {
        customPluginManager.unloadPlugin(pluginId);
    }
}