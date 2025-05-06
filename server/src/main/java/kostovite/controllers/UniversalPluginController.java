// src/main/java/kostovite/controllers/UniversalPluginController.java
package kostovite.controllers;

import kostovite.ExtendedPluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.PluginInterface;
import kostovite.services.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins/universal")
public class UniversalPluginController {

    private final ManualPluginLoader pluginLoader;
    private final PluginService pluginService;
    private static final Logger logger = Logger.getLogger(UniversalPluginController.class.getName());

    @Autowired
    public UniversalPluginController(ManualPluginLoader pluginLoader, PluginService pluginService) {
        this.pluginLoader = pluginLoader;
        this.pluginService = pluginService;
        logger.info("UniversalPluginController initialized with ManualPluginLoader and PluginService");
    }

    // --- Helper Method to Find Plugin (No access check) ---
    private PluginInterface findPluginByName(String pluginName) {
        for (PluginInterface p : pluginLoader.getLoadedPlugins()) {
            // Using equalsIgnoreCase for robustness
            if (p.getName() != null && p.getName().equalsIgnoreCase(pluginName)) {
                return p;
            }
        }
        logger.warning("Plugin not found by name: " + pluginName);
        return null;
    }

    // --- Helper Method to Find Plugin WITH Access Control (Used for GET info endpoints) ---
    private PluginInterface findAccessiblePlugin(String pluginName, Authentication authentication) {
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return null;
        }

        // If authentication is null (anonymous user), check against "normal" access level
        String userType = pluginService.extractUserType(authentication); // Handles null authentication

        try {
            Map<String, Object> metadata = plugin.getMetadata();
            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();

            // Use the service to check access for the resolved userType (can be "normal" for anonymous)
            if (!pluginService.canUserAccess(userType, pluginAccessLevel)) {
                logger.warning("Access denied: User type '" + userType + "' attempted to access plugin '" +
                        pluginName + "' with required access level '" + pluginAccessLevel + "'");
                return null; // Return null if access denied
            }
            // User has access
            return plugin;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking access for plugin " + pluginName, e);
            return null; // Deny access on error
        }
    }

    // --- Helper Method for Not Found Response ---
    private ResponseEntity<Map<String, Object>> buildPluginNotFoundResponse(String pluginName) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorMessage", "Plugin not found: " + pluginName);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // --- Helper Method for Access Denied Response ---
    private ResponseEntity<Map<String, Object>> buildAccessDeniedResponse(String pluginName) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorMessage", "Access denied to plugin: " + pluginName);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // --- Helper Method for Unsupported Plugin Type ---
    private ResponseEntity<Map<String, Object>> buildUnsupportedPluginResponse(String pluginName, String operation) {
        logger.warning("Plugin " + pluginName + " does not support " + operation);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorMessage", "Plugin '" + pluginName + "' is not an ExtendedPluginInterface and does not support " + operation);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // --- Helper method to guess field name based on mime type ---
    private String guessDataFieldName(String contentType) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) return "capturedImageData";
            if (contentType.startsWith("video/")) return "capturedVideoData";
        }
        logger.warning("Could not reliably guess data field name for content type: " + contentType);
        return null;
    }

    // --- Controller Endpoints ---

    // GET /api/plugins/universal - Allows anonymous access but filters results based on permissions
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPlugins(Authentication authentication) { // Authentication can be null
        String userType = pluginService.extractUserType(authentication); // Will return "normal" if null
        logger.info("Getting all plugins list, checking access for user type: " + userType);

        Map<String, Object> response = new HashMap<>();
        List<PluginInterface> allPlugins = pluginLoader.getLoadedPlugins();

        // Filter plugins based on user access level (uses "normal" for anonymous)
        List<String> accessiblePlugins = allPlugins.stream()
                .filter(plugin -> {
                    try {
                        Map<String, Object> metadata = plugin.getMetadata();
                        String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                        return pluginService.canUserAccess(userType, pluginAccessLevel);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error checking access for plugin " + plugin.getName(), e);
                        return false;
                    }
                })
                .map(PluginInterface::getName)
                .collect(Collectors.toList());

        response.put("count", accessiblePlugins.size());
        response.put("plugins", accessiblePlugins);
        response.put("userType", userType); // Reflects the type used for filtering
        return ResponseEntity.ok(response);
    }

    /**
     * Process data sent as JSON in the request body.
     * Uses findPluginByName (no authentication check) to allow anonymous access.
     */
    @PostMapping("/{pluginName}")
    public ResponseEntity<Map<String, Object>> processPluginJsonData(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input,
            Authentication authentication) { // Authentication can be null

        logger.info("Processing JSON request for plugin: " + pluginName + " by user: " +
                (authentication != null ? authentication.getName() : "anonymous"));

        // Find plugin by name, does not check access level itself
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        // Optional: If you want to check access even for anonymous users
        // Uncomment this block if you want to restrict access based on user type
        /*
        String userType = pluginService.extractUserType(authentication);
        try {
            Map<String, Object> metadata = plugin.getMetadata();
            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
            if (!pluginService.canUserAccess(userType, pluginAccessLevel)) {
                return buildAccessDeniedResponse(pluginName);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking access for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "errorMessage", "Error checking access: " + e.getMessage()
            ));
        }
        */

        try {
            if (plugin instanceof ExtendedPluginInterface extendedPlugin) {
                Map<String, Object> result = extendedPlugin.process(input);
                logger.info("Plugin " + pluginName + " processed JSON data successfully.");
                return ResponseEntity.ok(result);
            } else {
                plugin.execute(); // Handle basic plugins if necessary
                logger.info("Executed basic plugin (no data processing): " + pluginName);
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("success", true);
                fallbackResponse.put("message", "Basic plugin executed successfully (does not process input data).");
                fallbackResponse.put("pluginName", plugin.getName());
                return ResponseEntity.ok(fallbackResponse);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing JSON data for plugin " + pluginName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorMessage", "Error processing data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Handles file uploads via multipart/form-data.
     * Allows anonymous access but you can add access control if needed.
     */
    @PostMapping(value = "/{pluginName}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processPluginMultipartData(
            @PathVariable String pluginName,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> allParams,
            Authentication authentication) { // Authentication can be null

        long startTime = System.currentTimeMillis();
        logger.info(String.format("Received multipart upload for plugin '%s': file='%s', size=%d, params=%s",
                pluginName, file.getOriginalFilename(), file.getSize(), allParams.keySet()));

        // Find plugin by name, does not check access level itself
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        if (!(plugin instanceof ExtendedPluginInterface extendedPlugin)) {
            return buildUnsupportedPluginResponse(pluginName, "data processing via multipart upload");
        }

        try {
            Map<String, Object> input = new HashMap<>();

            if (file.isEmpty()) {
                logger.warning("Received empty file for multipart upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Uploaded file is empty."));
            }
            byte[] fileBytes = file.getBytes();
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            logger.fine("Encoded multipart file to Base64 (length: " + base64Data.length() + ")");

            String dataFieldName = allParams.get("dataFieldName");
            if (dataFieldName == null || dataFieldName.isBlank()) {
                dataFieldName = guessDataFieldName(file.getContentType());
                if(dataFieldName != null) {
                    logger.info("Guessed 'dataFieldName' as '" + dataFieldName + "' based on Content-Type: " + file.getContentType());
                }
            }

            if (dataFieldName == null || dataFieldName.isBlank()){
                logger.warning("Missing required 'dataFieldName' parameter for multipart upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Missing required form parameter: dataFieldName"));
            }

            input.put(dataFieldName, base64Data);
            logger.fine("Putting Base64 data into input map field: " + dataFieldName);

            String mimeTypeFieldName = allParams.get("mimeTypeFieldName");
            if (mimeTypeFieldName != null && !mimeTypeFieldName.isBlank() && file.getContentType() != null) {
                input.put(mimeTypeFieldName, file.getContentType());
                logger.fine("Putting MimeType ("+ file.getContentType() +") into input map field: " + mimeTypeFieldName);
            }

            allParams.forEach((key, value) -> {
                if (!key.equals("file") && !key.equals("dataFieldName") && !key.equals("mimeTypeFieldName") && !input.containsKey(key)) {
                    input.put(key, value);
                    logger.fine("Adding parameter to input map: " + key + "=" + value);
                }
            });
            logger.info("Constructed input map for plugin " + pluginName + ": " + input.keySet());

            Map<String, Object> result = extendedPlugin.process(input);
            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("Plugin '%s' processed multipart data successfully. Duration: %d ms", pluginName, duration));
            return ResponseEntity.ok(result);

        } catch (IOException ioException) {
            logger.log(Level.SEVERE, "IOException processing multipart file for plugin " + pluginName, ioException);
            return ResponseEntity.internalServerError().body(Map.of("success", false,"errorMessage", "Error reading uploaded file: " + ioException.getMessage()));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing multipart upload for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false,"errorMessage", "Error processing multipart data: " + e.getMessage()));
        }
    }

    /**
     * Handles file uploads as raw binary data in the request body.
     * Allows anonymous access but you can add access control if needed.
     */
    @PostMapping(value = "/{pluginName}/binary", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, Object>> processPluginBinaryData(
            @PathVariable String pluginName,
            @RequestBody byte[] fileData,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader(value = "X-Data-Field-Name", required = false) String dataFieldNameHeader,
            @RequestHeader(value = "X-Mime-Type-Field-Name", required = false) String mimeTypeFieldNameHeader,
            @RequestHeader(value = "X-File-Name", required = false) String fileNameHeader,
            Authentication authentication) { // Authentication can be null

        long startTime = System.currentTimeMillis();
        logger.info(String.format("Received binary upload for plugin '%s': size=%d, Content-Type=%s",
                pluginName, fileData.length, contentType));

        // Find plugin by name, does not check access level itself
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        if (!(plugin instanceof ExtendedPluginInterface extendedPlugin)) {
            return buildUnsupportedPluginResponse(pluginName, "data processing via binary upload");
        }

        try {
            Map<String, Object> input = new HashMap<>();

            if (fileData.length == 0) {
                logger.warning("Received empty binary data for plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Received empty binary data payload."));
            }
            String base64Data = Base64.getEncoder().encodeToString(fileData);
            logger.fine("Encoded binary data to Base64 (length: " + base64Data.length() + ")");

            String dataFieldName = dataFieldNameHeader;
            if (dataFieldName == null || dataFieldName.isBlank()) {
                dataFieldName = guessDataFieldName(contentType);
                if(dataFieldName != null) {
                    logger.info("Guessed 'dataFieldName' as '" + dataFieldName + "' based on Content-Type: " + contentType);
                }
            }

            if (dataFieldName == null || dataFieldName.isBlank()){
                logger.warning("Missing required 'X-Data-Field-Name' header for binary upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Missing required header: X-Data-Field-Name"));
            }

            input.put(dataFieldName, base64Data);
            logger.fine("Putting Base64 data into input map field: " + dataFieldName);

            if (mimeTypeFieldNameHeader != null && !mimeTypeFieldNameHeader.isBlank() && contentType != null) {
                input.put(mimeTypeFieldNameHeader, contentType);
                logger.fine("Putting MimeType ("+ contentType +") into input map field: " + mimeTypeFieldNameHeader);
            }

            if (fileNameHeader != null && !fileNameHeader.isBlank()) {
                input.put("outputFileName", fileNameHeader);
                logger.fine("Adding X-File-Name to input map as 'outputFileName': " + fileNameHeader);
            }

            logger.info("Constructed input map for plugin " + pluginName + ": " + input.keySet());

            Map<String, Object> result = extendedPlugin.process(input);
            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("Plugin '%s' processed binary data successfully. Duration: %d ms", pluginName, duration));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing binary upload for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false,"errorMessage", "Error processing binary data: " + e.getMessage()));
        }
    }

    // GET /api/plugins/universal/load-plugins - Allows anonymous access but filters results based on permissions
    @GetMapping("/load-plugins")
    public ResponseEntity<Map<String, Object>> loadPlugins(Authentication authentication) { // Authentication can be null
        String userType = pluginService.extractUserType(authentication);
        logger.info("Loading plugins list, checking access for user type: " + userType);

        Map<String, Object> response = new HashMap<>();
        try {
            String workingDir = System.getProperty("user.dir");
            Path pluginsPath = Paths.get(workingDir, "plugins-deploy").toAbsolutePath();
            List<PluginInterface> allPlugins = pluginLoader.loadPlugins(pluginsPath);

            // Filter based on resolved userType (e.g., "normal" for anonymous)
            List<String> accessiblePlugins = allPlugins.stream()
                    .filter(plugin -> {
                        try {
                            Map<String, Object> metadata = plugin.getMetadata();
                            String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                            return pluginService.canUserAccess(userType, pluginAccessLevel);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error checking access for plugin " + plugin.getName(), e);
                            return false;
                        }
                    })
                    .map(PluginInterface::getName)
                    .collect(Collectors.toList());

            response.put("loadedPlugins", accessiblePlugins);
            response.put("status", "success");
            response.put("userType", userType);
            logger.info("Loaded " + accessiblePlugins.size() + " accessible plugins for user type " + userType);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during plugin loading", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // --- Admin Unload Endpoints (Keep Restricted) ---
    @DeleteMapping("/unload/{pluginName}")
    public ResponseEntity<Map<String, Object>> unloadPlugin(@PathVariable String pluginName, Authentication authentication) {
        // MUST check authentication and role here
        String userType = pluginService.extractUserType(authentication);
        if (!"admin".equals(userType)) {
            logger.warning("Non-admin user (" + userType + ") attempted to unload plugin: " + pluginName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "Only admin users can unload plugins"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        boolean success = pluginLoader.unloadPlugin(pluginName);
        if (success) {
            response.put("status", "success");
            response.put("message", "Plugin '" + pluginName + "' unloaded successfully");
            logger.info("Admin user unloaded plugin: " + pluginName);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to unload plugin '" + pluginName + "' (not found or error).");
            logger.warning("Failed attempt to unload plugin: " + pluginName);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unload-all")
    public ResponseEntity<Map<String, Object>> unloadAllPlugins(Authentication authentication) {
        // MUST check authentication and role here
        String userType = pluginService.extractUserType(authentication);
        if (!"admin".equals(userType)) {
            logger.warning("Non-admin user (" + userType + ") attempted to unload all plugins");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "Only admin users can unload all plugins"
            ));
        }

        Map<String, Object> response = new HashMap<>();
        int count = pluginLoader.unloadAllPlugins();
        response.put("status", "success");
        response.put("message", count + " plugins unloaded successfully");
        logger.info("Admin user unloaded all " + count + " plugins");
        return ResponseEntity.ok(response);
    }

    // GET /api/plugins/universal/{pluginName}/plugin-info - Allows anonymous access but filters results based on permissions
    @GetMapping("/{pluginName}/plugin-info")
    public ResponseEntity<Map<String, Object>> getPluginInfo(
            @PathVariable String pluginName,
            Authentication authentication) { // Authentication can be null

        logger.fine("Requesting plugin info for plugin: " + pluginName);

        // Use findAccessiblePlugin which handles null authentication and checks access
        PluginInterface plugin = findAccessiblePlugin(pluginName, authentication);
        if (plugin == null) {
            // Distinguish between not found and access denied
            if (findPluginByName(pluginName) == null) {
                return buildPluginNotFoundResponse(pluginName);
            } else {
                return buildAccessDeniedResponse(pluginName);
            }
        }

        try {
            Map<String, Object> metadata = plugin.getMetadata();
            // Add user type used for check if needed by frontend
            metadata.put("requestingUserType", pluginService.extractUserType(authentication));
            logger.fine("Returning info for accessible plugin: " + pluginName);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving info for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "errorMessage", "Error retrieving plugin info: " + e.getMessage()
            ));
        }
    }
}