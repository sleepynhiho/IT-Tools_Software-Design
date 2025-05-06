package kostovite.controllers;

import kostovite.services.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import kostovite.PluginInterface;
import kostovite.ManualPluginLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {
    private static final Logger log = LoggerFactory.getLogger(PluginController.class);
    private final PluginService pluginService;
    private final ManualPluginLoader manualPluginLoader;
    private final Path pluginsDirectory = Paths.get("plugins-deploy");

    @Autowired
    public PluginController(PluginService pluginService, ManualPluginLoader manualPluginLoader) {
        this.pluginService = pluginService;
        this.manualPluginLoader = manualPluginLoader;

        try {
            if (!Files.exists(pluginsDirectory)) {
                Files.createDirectories(pluginsDirectory);
            }
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            this.manualPluginLoader.loadPlugins(pluginsPath);
            log.info("PluginController: Initial plugin load attempted.");
        } catch (IOException e) {
            log.error("Failed to create or access plugins directory on startup: {}", pluginsDirectory, e);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getPluginsForCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("Authentication object is null in getPluginsForCurrentUser despite @PreAuthorize");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }
        log.info("Request received for GET /api/plugins by user {}", authentication.getName());
        try {
            List<Map<String, Object>> accessiblePlugins = pluginService.getAccessiblePluginMetadata(authentication);
            return ResponseEntity.ok(accessiblePlugins);
        } catch (Exception e) {
            log.error("Error retrieving accessible plugins for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/{pluginName}/process")
    // Removed @PreAuthorize("isAuthenticated()") to allow anonymous access
    public ResponseEntity<Map<String, Object>> processPlugin(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {

        log.info("Processing request for plugin: {} by user {}", pluginName,
                (authentication != null ? authentication.getName() : "anonymous"));

        try {
            // Pass authentication (which might be null) to the service
            Map<String, Object> result = pluginService.processPlugin(pluginName, input, authentication);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Processing failed - Plugin not found: {}", pluginName, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (AccessDeniedException e) {
            log.warn("Processing failed - Access denied for plugin {}: {}", pluginName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error in processing with plugin {}: {}", pluginName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", "Processing failed: " + e.getMessage()));
        }
    }

    @PostMapping("/debug/{pluginName}/process")
    // Removed @PreAuthorize("isAuthenticated()") to allow anonymous access
    public ResponseEntity<Map<String, Object>> processPluginDebug(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {

        log.info("Debug processing request for plugin: {} by user {}", pluginName,
                (authentication != null ? authentication.getName() : "anonymous"));

        try {
            Map<String, Object> result = pluginService.processPlugin(pluginName, input, authentication);

            result.put("debug_request_info", Map.of(
                    "plugin", pluginName,
                    "user", (authentication != null ? authentication.getName() : "anonymous"),
                    "timestamp", System.currentTimeMillis()
            ));

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Debug processing failed - Plugin not found: {}", pluginName, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (AccessDeniedException e) {
            log.warn("Debug processing failed - Access denied for plugin {}: {}", pluginName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error in debug processing with plugin {}: {}", pluginName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", "Processing failed: " + e.getMessage()));
        }
    }

    @GetMapping("/universal/{pluginName}/metadata")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPluginMetadata(
            @PathVariable String pluginName,
            Authentication authentication) {

        log.info("Metadata request for plugin: {} by user {}", pluginName, authentication.getName());

        try {
            PluginInterface plugin = manualPluginLoader.getPluginByName(pluginName);
            if (plugin == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "error", "Plugin not found: " + pluginName));
            }

            // Check access before returning metadata
            String userType = pluginService.extractUserType(authentication);
            Map<String, Object> metadata = plugin.getMetadata();
            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();

            if (!pluginService.canUserAccess(userType, pluginAccessLevel)) {
                log.warn("Metadata access denied for plugin {} to user type {}", pluginName, userType);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Access denied to plugin metadata: " + pluginName));
            }

            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("Error retrieving metadata for plugin {}: {}", pluginName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to retrieve metadata: " + e.getMessage()));
        }
    }

    @GetMapping("/universal/manual-load")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getLoadedPlugins(Authentication authentication) {
        log.info("Request for loaded plugins list by user {}", authentication.getName());

        try {
            String userType = pluginService.extractUserType(authentication);
            List<PluginInterface> allPlugins = manualPluginLoader.getLoadedPlugins();

            // Filter plugins based on access level
            List<String> accessiblePlugins = allPlugins.stream()
                    .filter(plugin -> {
                        try {
                            Map<String, Object> metadata = plugin.getMetadata();
                            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                            return pluginService.canUserAccess(userType, pluginAccessLevel);
                        } catch (Exception e) {
                            log.warn("Error checking access for plugin {}: {}", plugin.getName(), e.getMessage());
                            return false;
                        }
                    })
                    .map(PluginInterface::getName)
                    .collect(Collectors.toList());

            log.info("Returning {} accessible plugins for user type {}", accessiblePlugins.size(), userType);

            Map<String, Object> response = new HashMap<>();
            response.put("loadedPlugins", accessiblePlugins);
            response.put("userType", userType);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving loaded plugins: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to retrieve loaded plugins: " + e.getMessage()));
        }
    }

    @PostMapping("/universal/{pluginName}/process")
    // Removed @PreAuthorize("isAuthenticated()") to allow anonymous access
    public ResponseEntity<Map<String, Object>> processUniversalPlugin(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {

        log.info("Universal processing request for plugin: {} by user {}", pluginName,
                (authentication != null ? authentication.getName() : "anonymous"));

        try {
            Map<String, Object> result = pluginService.processPlugin(pluginName, input, authentication);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Universal processing failed - Plugin not found: {}", pluginName, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (AccessDeniedException e) {
            log.warn("Universal processing failed - Access denied for plugin {}: {}", pluginName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error in universal processing with plugin {}: {}", pluginName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", "Processing failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadPlugin(@RequestParam("file") MultipartFile file, Authentication authentication) {
        log.info("Plugin upload attempt by user: {}", authentication.getName());
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Plugin file is empty"));
            }
            if (!file.getOriginalFilename().endsWith(".jar")) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Only JAR files are supported"));
            }

            Path targetPath = pluginsDirectory.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            manualPluginLoader.loadPlugins(pluginsPath);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin uploaded and loaded successfully by admin.");
            response.put("pluginPath", targetPath.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed plugin upload by user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to upload plugin: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{pluginName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deletePlugin(@PathVariable String pluginName, Authentication authentication) {
        log.info("Plugin delete attempt for '{}' by user: {}", pluginName, authentication.getName());
        try {
            boolean deleted = manualPluginLoader.deletePlugin(pluginName);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to delete plugin file or plugin not found."));
            }
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            manualPluginLoader.loadPlugins(pluginsPath);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Plugin deleted successfully by admin."));
        } catch (Exception e) {
            log.error("Failed plugin delete for '{}' by user {}: {}", pluginName, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to delete plugin: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshPlugins(Authentication authentication) {
        log.info("Plugin refresh requested by user: {}", authentication.getName());
        try {
            int unloadedCount = manualPluginLoader.unloadAllPlugins();
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            List<PluginInterface> plugins = manualPluginLoader.loadPlugins(pluginsPath);
            Map<String, Object> response = new HashMap<>();
            response.put("unloadedCount", unloadedCount);
            response.put("loadedCount", plugins.size());
            response.put("loadedPlugins", plugins.stream().map(PluginInterface::getName).collect(Collectors.toList()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing plugins by user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to refresh plugins: " + e.getMessage()));
        }
    }

    @GetMapping("/extensions/{extensionName}/execute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> executePlugin(@PathVariable String extensionName, Authentication authentication) {
        log.warn("Execute endpoint called for '{}' by user '{}' - needs access check", extensionName, authentication.getName());

        // Get user type
        String userType = pluginService.extractUserType(authentication);

        // Get plugin
        PluginInterface plugin = manualPluginLoader.getPluginByName(extensionName);
        if (plugin == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("status", "error", "message", "Plugin not found: " + extensionName));
        }

        try {
            // Check access level
            Map<String, Object> metadata = plugin.getMetadata();
            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();

            if (!pluginService.canUserAccess(userType, pluginAccessLevel)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("status", "error", "message", "Access denied to plugin: " + extensionName));
            }

            // Execute is not fully implemented, return appropriate message
            return ResponseEntity.ok(Map.of(
                    "status", "warning",
                    "message", "Plugin execution method not fully implemented yet. Use /api/plugins/universal/" + extensionName + "/process instead."
            ));
        } catch (Exception e) {
            log.error("Error executing plugin {}: {}", extensionName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("status", "error", "message", "Plugin execution failed: " + e.getMessage()));
        }
    }

    @GetMapping("/extensions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getExtensions(Authentication authentication) {
        log.debug("Request for /extensions by user {}", authentication.getName());

        // Get user type to filter plugins
        String userType = pluginService.extractUserType(authentication);

        try {
            // Filter plugins based on access level just like the main endpoint
            List<Map<String, Object>> filteredExtensions = manualPluginLoader.getLoadedPlugins().stream()
                    .filter(plugin -> {
                        try {
                            Map<String, Object> metadata = plugin.getMetadata();
                            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                            return pluginService.canUserAccess(userType, pluginAccessLevel);
                        } catch (Exception e) {
                            log.warn("Error checking access for plugin {}: {}", plugin.getName(), e.getMessage());
                            return false;
                        }
                    })
                    .map(plugin -> {
                        try {
                            Map<String, Object> basicInfo = new HashMap<>();
                            basicInfo.put("name", plugin.getName());

                            // Add minimal metadata if available
                            try {
                                Map<String, Object> metadata = plugin.getMetadata();
                                basicInfo.put("id", metadata.getOrDefault("id", plugin.getName()));
                                basicInfo.put("description", metadata.getOrDefault("description", ""));
                                basicInfo.put("category", metadata.getOrDefault("category", "Other"));
                            } catch (Exception e) {
                                log.warn("Error getting metadata for plugin {}: {}", plugin.getName(), e.getMessage());
                            }

                            return basicInfo;
                        } catch (Exception e) {
                            log.error("Error mapping plugin {}: {}", plugin.getName(), e.getMessage());
                            return Map.<String, Object>of("name", plugin.getName());
                        }
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredExtensions);
        } catch (Exception e) {
            log.error("Error retrieving extensions for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}