package kostovite.controllers;

import kostovite.services.PluginService;
import kostovite.services.PluginService.PluginDisabledException; // Import custom exception
import kostovite.services.PluginService.PluginStatusCheckResult; // Import status result
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
    // No @PreAuthorize, allows anonymous access to this endpoint.
    // Access control is handled within the service.
    public ResponseEntity<List<Map<String, Object>>> getPluginsForCurrentUser(Authentication authentication) {
        String userIdentifier = (authentication != null ? authentication.getName() : "anonymous");
        log.info("Request received for GET /api/plugins by user/identity: {}", userIdentifier);

        try {
            // pluginService.getAccessiblePluginMetadata should now:
            // 1. Get all plugins.
            // 2. For each plugin, check its enabled/disabled status (e.g., from Firestore via isPluginEnabled).
            // 3. If enabled, then check if the current user (anonymous or authenticated) can access it based on userType vs pluginAccessLevel.
            // 4. Return metadata of only enabled AND accessible plugins.
            List<Map<String, Object>> accessiblePlugins = pluginService.getAccessiblePluginMetadata(authentication);
            log.info("Returning {} accessible and enabled plugins for user/identity: {}", accessiblePlugins.size(), userIdentifier);
            return ResponseEntity.ok(accessiblePlugins);
        } catch (Exception e) {
            log.error("Error retrieving accessible plugins for user/identity {}: {}", userIdentifier, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    private ResponseEntity<Map<String, Object>> handleProcessRequest(
            String pluginName, Map<String, Object> input, Authentication authentication, boolean isDebug) {

        String userIdentifier = (authentication != null ? authentication.getName() : "anonymous");
        String logPrefix = isDebug ? "Debug processing" : "Processing";
        log.info("{} request for plugin: {} by user/identity: {}", logPrefix, pluginName, userIdentifier);

        try {
            // The pluginService.processPlugin method should internally call isPluginEnabled first.
            // If disabled, it will throw PluginDisabledException.
            // Then it checks user access level. If denied, it throws AccessDeniedException.
            Map<String, Object> result = pluginService.processPlugin(pluginName, input, authentication);

            if (isDebug) {
                result.put("debug_request_info", Map.of(
                        "plugin", pluginName,
                        "user", userIdentifier,
                        "timestamp", System.currentTimeMillis()
                ));
            }
            return ResponseEntity.ok(result);
        } catch (PluginDisabledException e) {
            log.warn("{} failed - Plugin '{}' is disabled. Reason: {}", logPrefix, pluginName, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // 503 might be more appropriate for "disabled"
                    .body(Map.of("success", false, "error", "Plugin disabled by administrator.", "message", e.getMessage()));
        } catch (IllegalArgumentException e) { // Typically for plugin not found
            log.warn("{} failed - Plugin not found: {}", logPrefix, pluginName, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", e.getMessage()));
        } catch (AccessDeniedException e) { // For user not having access to an *enabled* plugin
            log.warn("{} failed - Access denied for plugin {}: {}", logPrefix, pluginName, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "error", "No access permission for this plugin.", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error in {} with plugin {}: {}", logPrefix, pluginName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", "Processing failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{pluginName}/process")
    public ResponseEntity<Map<String, Object>> processPlugin(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {
        return handleProcessRequest(pluginName, input, authentication, false);
    }

    @PostMapping("/debug/{pluginName}/process")
    public ResponseEntity<Map<String, Object>> processPluginDebug(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {
        return handleProcessRequest(pluginName, input, authentication, true);
    }

    @GetMapping("/universal/{pluginName}/metadata")
    @PreAuthorize("isAuthenticated()") // Keep this if only logged-in users can get specific metadata
    public ResponseEntity<Map<String, Object>> getPluginMetadata(
            @PathVariable String pluginName,
            Authentication authentication) {

        log.info("Metadata request for plugin: {} by user {}", pluginName, authentication.getName());

        // Check general enabled status first
        PluginStatusCheckResult statusResult = pluginService.isPluginEnabled(pluginName);
        if (!statusResult.isEnabled()) {
            log.warn("Metadata request for disabled plugin '{}'. Reason: {}", pluginName, statusResult.message());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "error", "Plugin disabled by administrator.", "message", statusResult.message()));
        }

        try {
            PluginInterface plugin = manualPluginLoader.getPluginByName(pluginName);
            if (plugin == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("success", false, "error", "Plugin not found: " + pluginName));
            }

            String userType = pluginService.extractUserType(authentication);
            Map<String, Object> metadata = plugin.getMetadata();
            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();

            if (!pluginService.canUserAccess(userType, pluginAccessLevel)) {
                log.warn("Metadata access denied for plugin {} to user type {}", pluginName, userType);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "No access permission for this plugin.", "message", "User does not have sufficient access for this plugin's metadata."));
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
            List<Map<String, Object>> accessiblePlugins = pluginService.getAccessiblePluginMetadata(authentication);
            Map<String, Object> response = new HashMap<>();
            // The getAccessiblePluginMetadata returns List<Map<String,Object>>, not List<String>
            response.put("loadedPlugins", accessiblePlugins.stream().map(p -> p.get("name")).collect(Collectors.toList())); // Extract names if that's what FE expects
            response.put("userType", pluginService.extractUserType(authentication));
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving loaded plugins: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to retrieve loaded plugins: " + e.getMessage()));
        }
    }

    @PostMapping("/universal/{pluginName}/process")
    public ResponseEntity<Map<String, Object>> processUniversalPlugin(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) {
        // Delegate to the common handler
        return handleProcessRequest(pluginName, input, authentication, false);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadPlugin(@RequestParam("file") MultipartFile file, Authentication authentication) {
        log.info("Plugin upload attempt by user: {}", authentication.getName());
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Plugin file is empty"));
            }

            // --- IMPORTANT: Sanitize filename and add more checks ---
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Plugin filename cannot be empty."));
            }

            // Basic sanitization: prevent path traversal, allow only alphanumeric, hyphens, underscores, dots.
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
            if (!sanitizedFilename.toLowerCase().endsWith(".jar")) { // Check sanitized name
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Only JAR files are supported (filename must end with .jar). Invalid or sanitized filename: " + sanitizedFilename));
            }
            // --- END Filename Sanitization ---

            Path targetPath = pluginsDirectory.resolve(sanitizedFilename); // Use sanitized filename

            // --- Check for directory traversal attempt after resolving ---
            if (!targetPath.normalize().startsWith(pluginsDirectory.normalize())) {
                log.error("Directory traversal attempt detected in plugin upload: {} (resolved to {})", sanitizedFilename, targetPath);
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid plugin filename (potential path traversal)."));
            }
            // --- END Directory Traversal Check ---

            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Plugin JAR '{}' (original: '{}') uploaded successfully to: {}", sanitizedFilename, originalFilename, targetPath);

            // After uploading, trigger a reload of plugins
            Path pluginsPath = Paths.get(System.getProperty("user.dir"), "plugins-deploy").toAbsolutePath();
            List<PluginInterface> plugins = manualPluginLoader.loadPlugins(pluginsPath);
            log.info("Reloaded plugins after new JAR upload, {} plugins loaded", plugins.size());

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Plugin '" + sanitizedFilename + "' uploaded and plugins reloaded successfully by admin.");
            response.put("pluginFilename", sanitizedFilename); // Return the sanitized name it was saved as
            response.put("pluginPath", targetPath.toString());
            response.put("loadedCount", String.valueOf(plugins.size()));
            return ResponseEntity.ok(response);
        } catch (IOException e) { // Catch specific IOException for file operations
            log.error("IOException during plugin upload by user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to save uploaded plugin file: " + e.getMessage()));
        } catch (Exception e) { // Catch other potential exceptions
            log.error("Generic exception during plugin upload by user {}: {}", authentication.getName(), e.getMessage(), e);
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
        log.warn("Execute endpoint called for '{}' by user '{}'", extensionName, authentication.getName());

        // Check general enabled status first
        PluginStatusCheckResult statusResult = pluginService.isPluginEnabled(extensionName);
        if (!statusResult.isEnabled()) {
            log.warn("Execute request for disabled plugin '{}'. Reason: {}", extensionName, statusResult.message());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "error", "message", statusResult.message()));
        }

        // Get plugin
        PluginInterface plugin = manualPluginLoader.getPluginByName(extensionName);
        if (plugin == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("status", "error", "message", "Plugin not found: " + extensionName));
        }

        try {
            // Get user type
            String userType = pluginService.extractUserType(authentication);

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
        try {
            List<Map<String, Object>> accessiblePlugins = pluginService.getAccessiblePluginMetadata(authentication);
            // The current code maps to basicInfo. Adjust if getAccessiblePluginMetadata already returns the desired format.
            List<Map<String, Object>> filteredExtensions = accessiblePlugins.stream()
                    .map(metadata -> {
                        Map<String, Object> basicInfo = new HashMap<>();
                        basicInfo.put("name", metadata.get("name"));
                        basicInfo.put("id", metadata.getOrDefault("id", metadata.get("name")));
                        basicInfo.put("description", metadata.getOrDefault("description", ""));
                        basicInfo.put("category", metadata.getOrDefault("category", "Other"));
                        return basicInfo;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(filteredExtensions);
        } catch (Exception e) {
            log.error("Error retrieving extensions for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @DeleteMapping("/{pluginId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deletePluginWithId(@PathVariable String pluginId, Authentication authentication) {
        String adminUsername = (authentication != null && authentication.getName() != null) ? authentication.getName() : "UnknownAdmin";
        log.info("Plugin delete (with ID endpoint) attempt for '{}' by user: {}",
                pluginId, adminUsername);

        try {
            // First, try to find the plugin by ID to verify it exists
            // Assuming getPluginByName can also take an ID if they are the same, or you have getPluginById
            PluginInterface plugin = manualPluginLoader.getPluginByName(pluginId); // Or manualPluginLoader.getPluginById(pluginId)
            if (plugin == null) {
                log.warn("Delete request for non-existent plugin ID: '{}' by user: {}",
                        pluginId, adminUsername);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "error",
                                "message", "Plugin not found with ID: " + pluginId
                        ));
            }

            // Proceed with deletion
            boolean deleted = manualPluginLoader.deletePlugin(pluginId); // Pass the ID/name that deletePlugin expects

            if (!deleted) {
                log.warn("Failed to delete plugin with ID: '{}' by user: {}",
                        pluginId, adminUsername);
                // Assuming plugin was unloaded but JAR deletion might be pending
                Path pluginsPath = pluginsDirectory.toAbsolutePath();
                manualPluginLoader.loadPlugins(pluginsPath); // Refresh list
                return ResponseEntity.status(HttpStatus.ACCEPTED) // Or INTERNAL_SERVER_ERROR if immediate delete is critical
                        .body(Map.of(
                                "status", "pending_or_failed_immediate_delete",
                                "message", "Plugin unloaded. JAR deletion failed immediately (may be locked) or was scheduled for JVM exit. Plugins reloaded."
                        ));
            }

            // Reload plugins after successful deletion
            Path pluginsPath = pluginsDirectory.toAbsolutePath();
            manualPluginLoader.loadPlugins(pluginsPath);

            log.info("Successfully deleted plugin with ID: '{}' by user: {}",
                    pluginId, adminUsername);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Plugin deleted successfully by admin."
            ));
        } catch (Exception e) {
            log.error("Exception during plugin deletion for ID '{}' by user: {}: {}",
                    pluginId, adminUsername, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to delete plugin: " + e.getMessage()
                    ));
        }
    }
}