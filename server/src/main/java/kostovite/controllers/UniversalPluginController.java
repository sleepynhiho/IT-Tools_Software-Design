package kostovite.controllers;

import kostovite.ExtendedPluginInterface;
import kostovite.ManualPluginLoader;
import kostovite.PluginInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level; // Import Level
import java.util.logging.Logger; // Import Logger
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plugins/universal")
public class UniversalPluginController {

    private final ManualPluginLoader pluginLoader;
    // Use java.util.logging matching the examples provided
    private static final Logger logger = Logger.getLogger(UniversalPluginController.class.getName());

    // Remove redundant Autowired field if constructor injection is used

    @Autowired
    public UniversalPluginController(ManualPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    // --- Helper Method to Find Plugin (Refactored) ---
    private PluginInterface findPluginByName(String pluginName) {
        for (PluginInterface p : pluginLoader.getLoadedPlugins()) {
            // Use equalsIgnoreCase for robustness
            if (p.getName().equalsIgnoreCase(pluginName)) {
                return p;
            }
        }
        logger.warning("Plugin not found by name: " + pluginName);
        return null;
    }

    // --- Helper Method for Not Found Response ---
    private ResponseEntity<Map<String, Object>> buildPluginNotFoundResponse(String pluginName) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false); // Consistent error structure
        errorResponse.put("errorMessage", "Plugin not found: " + pluginName);
        // Return 404 Not Found status
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // --- Helper Method for Unsupported Plugin Type ---
    private ResponseEntity<Map<String, Object>> buildUnsupportedPluginResponse(String pluginName, String operation) {
        logger.warning("Plugin " + pluginName + " does not support " + operation);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorMessage", "Plugin '" + pluginName + "' is not an ExtendedPluginInterface and does not support " + operation);
        // Return 400 Bad Request status, as the client tried an operation the plugin type doesn't support
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // --- Helper method to guess field name based on mime type (simple version) ---
    private String guessDataFieldName(String contentType) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) return "capturedImageData";
            if (contentType.startsWith("video/")) return "capturedVideoData";
        }
        logger.warning("Could not reliably guess data field name for content type: " + contentType);
        return null; // Cannot guess reliably
    }

    // --- Existing Endpoints (Minor adjustments for consistency) ---

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPlugins() {
        Map<String, Object> response = new HashMap<>();
        List<PluginInterface> plugins = pluginLoader.getLoadedPlugins();

        response.put("count", plugins.size());
        // Use equalsIgnoreCase safe stream processing if needed, assumed names are consistent casing
        response.put("plugins", plugins.stream().map(PluginInterface::getName).toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Existing endpoint for processing data sent as JSON in the request body.
     * Assumes data (like images/video) is already Base64 encoded within the input map.
     */
    @PostMapping("/{pluginName}")
    public ResponseEntity<Map<String, Object>> processPluginJsonData(
            @PathVariable String pluginName,
            @RequestBody Map<String, Object> input) {
        logger.info("Processing JSON request for plugin: " + pluginName);
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        try {
            if (plugin instanceof ExtendedPluginInterface extendedPlugin) { // Use pattern variable binding
                // Process using the ExtendedPluginInterface
                Map<String, Object> result = extendedPlugin.process(input);
                logger.info("Plugin " + pluginName + " processed JSON data successfully.");
                return ResponseEntity.ok(result);
            } else {
                // Fallback for basic plugins (that don't process input maps)
                plugin.execute();
                logger.info("Executed basic plugin (no data processing): " + pluginName);
                // Create a basic success response indicating execution
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("success", true); // Indicate success
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

    // --- NEW: Endpoint for Multipart File Upload ---
    /**
     * Handles file uploads via multipart/form-data.
     * Converts the uploaded file to Base64 and places it in the input map
     * before passing it to the plugin's process method.
     * Requires 'dataFieldName' parameter to specify the key for the Base64 data in the map.
     */
    @PostMapping(value = "/{pluginName}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> processPluginMultipartData(
            @PathVariable String pluginName,
            @RequestParam("file") MultipartFile file, // The uploaded file
            @RequestParam Map<String, String> allParams // Catch all other form fields
    ) {
        long startTime = System.currentTimeMillis();
        logger.info(String.format("Received multipart upload for plugin '%s': file='%s', size=%d, params=%s",
                pluginName, file.getOriginalFilename(), file.getSize(), allParams.keySet()));

        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        // Check if the plugin can actually process data
        if (!(plugin instanceof ExtendedPluginInterface extendedPlugin)) {
            return buildUnsupportedPluginResponse(pluginName, "data processing via multipart upload");
        }

        try {
            // --- Data Mapping Logic ---
            Map<String, Object> input = new HashMap<>();

            // 1. Handle the file upload -> Base64 encode
            if (file.isEmpty()) {
                logger.warning("Received empty file for multipart upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Uploaded file is empty."));
            }
            byte[] fileBytes = file.getBytes();
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            logger.fine("Encoded multipart file to Base64 (length: " + base64Data.length() + ")");


            // 2. Determine where to put the base64 data and mime type in the input map
            // Convention: Expect 'dataFieldName' and optional 'mimeTypeFieldName' in the params
            String dataFieldName = allParams.get("dataFieldName");
            // Try guessing if not provided (useful for simple webcam uploads)
            if (dataFieldName == null || dataFieldName.isBlank()) {
                dataFieldName = guessDataFieldName(file.getContentType());
                if(dataFieldName != null) {
                    logger.info("Guessed 'dataFieldName' as '" + dataFieldName + "' based on Content-Type: " + file.getContentType());
                }
            }

            // dataFieldName is essential for placing the data correctly
            if (dataFieldName == null || dataFieldName.isBlank()){
                logger.warning("Missing required 'dataFieldName' parameter for multipart upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Missing required form parameter: dataFieldName (e.g., 'capturedImageData' or 'capturedVideoData')"));
            }

            input.put(dataFieldName, base64Data);
            logger.fine("Putting Base64 data into input map field: " + dataFieldName);

            // Optionally add mime type if field name is provided
            String mimeTypeFieldName = allParams.get("mimeTypeFieldName");
            if (mimeTypeFieldName != null && !mimeTypeFieldName.isBlank() && file.getContentType() != null) {
                input.put(mimeTypeFieldName, file.getContentType());
                logger.fine("Putting MimeType ("+ file.getContentType() +") into input map field: " + mimeTypeFieldName);
            }

            // 3. Add all other parameters from the form data to the input map
            allParams.forEach((key, value) -> {
                // Avoid overwriting the file/mime data fields or the convention fields
                if (!key.equals("file") && !key.equals("dataFieldName") && !key.equals("mimeTypeFieldName") && !input.containsKey(key)) {
                    input.put(key, value);
                    logger.fine("Adding parameter to input map: " + key + "=" + value);
                }
            });
            logger.info("Constructed input map for plugin " + pluginName + ": " + input.keySet());


            // --- Process with Plugin ---
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


    // --- NEW: Endpoint for Binary Data Upload ---
    /**
     * Handles file uploads as raw binary data in the request body.
     * Converts the binary data to Base64 and places it in the input map.
     * Requires 'X-Data-Field-Name' header to specify the key for the Base64 data.
     */
    @PostMapping(value = "/{pluginName}/binary", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, Object>> processPluginBinaryData(
            @PathVariable String pluginName,
            @RequestBody byte[] fileData, // The raw binary data
            @RequestHeader("Content-Type") String contentType, // Standard Content-Type header
            // Custom headers for mapping data into the plugin's expected input map
            @RequestHeader(value = "X-Data-Field-Name", required = false) String dataFieldNameHeader,
            @RequestHeader(value = "X-Mime-Type-Field-Name", required = false) String mimeTypeFieldNameHeader,
            // Capture other optional headers that might be parameters for the plugin
            @RequestHeader(value = "X-File-Name", required = false) String fileNameHeader
    ) {
        long startTime = System.currentTimeMillis();
        logger.info(String.format("Received binary upload for plugin '%s': size=%d, Content-Type=%s, X-Data-Field-Name=%s, X-File-Name=%s",
                pluginName, fileData.length, contentType, dataFieldNameHeader, fileNameHeader));

        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        // Check if the plugin can actually process data
        if (!(plugin instanceof ExtendedPluginInterface extendedPlugin)) {
            return buildUnsupportedPluginResponse(pluginName, "data processing via binary upload");
        }

        try {
            // --- Data Mapping Logic ---
            Map<String, Object> input = new HashMap<>();

            // 1. Convert binary data to Base64
            if (fileData.length == 0) {
                logger.warning("Received empty binary data for plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Received empty binary data payload."));
            }
            String base64Data = Base64.getEncoder().encodeToString(fileData);
            logger.fine("Encoded binary data to Base64 (length: " + base64Data.length() + ")");

            // 2. Determine where to put the base64 data and mime type
            // Use header X-Data-Field-Name, fallback to guessing
            String dataFieldName = dataFieldNameHeader;
            if (dataFieldName == null || dataFieldName.isBlank()) {
                dataFieldName = guessDataFieldName(contentType);
                if(dataFieldName != null) {
                    logger.info("Guessed 'dataFieldName' as '" + dataFieldName + "' based on Content-Type: " + contentType);
                }
            }

            if (dataFieldName == null || dataFieldName.isBlank()){
                logger.warning("Missing required 'X-Data-Field-Name' header for binary upload to plugin: " + pluginName);
                return ResponseEntity.badRequest().body(Map.of("success", false, "errorMessage", "Missing required header: X-Data-Field-Name (e.g., 'capturedImageData' or 'capturedVideoData')"));
            }

            input.put(dataFieldName, base64Data);
            logger.fine("Putting Base64 data into input map field: " + dataFieldName);

            // Optionally add mime type if field name header is provided
            String mimeTypeFieldName = mimeTypeFieldNameHeader;
            if (mimeTypeFieldName != null && !mimeTypeFieldName.isBlank() && contentType != null) {
                input.put(mimeTypeFieldName, contentType);
                logger.fine("Putting MimeType ("+ contentType +") into input map field: " + mimeTypeFieldName);
            }

            // 3. Add optional headers like filename to the input map
            // Convention: Use the header name directly as the key in the map if it's relevant
            if (fileNameHeader != null && !fileNameHeader.isBlank()) {
                // Assume the plugin expects it under a key like 'outputFileName' or similar
                // Adjust this key based on plugin expectations or add another header for the target key name
                input.put("outputFileName", fileNameHeader); // Adjust key if needed
                logger.fine("Adding X-File-Name to input map as 'outputFileName': " + fileNameHeader);
            }
            // Could add more headers here if needed

            logger.info("Constructed input map for plugin " + pluginName + ": " + input.keySet());

            // --- Process with Plugin ---
            Map<String, Object> result = extendedPlugin.process(input);
            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("Plugin '%s' processed binary data successfully. Duration: %d ms", pluginName, duration));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing binary upload for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false,"errorMessage", "Error processing binary data: " + e.getMessage()));
        }
    }


    // --- Plugin Management Endpoints (Unchanged) ---

    @GetMapping("/manual-load")
    public ResponseEntity<Map<String, Object>> manualLoad() {
        // ... (implementation unchanged)
        Map<String, Object> response = new HashMap<>();
        try {
            String workingDir = System.getProperty("user.dir");
            Path pluginsPath = Paths.get(workingDir, "plugins-deploy").toAbsolutePath();
            List<PluginInterface> plugins = pluginLoader.loadPlugins(pluginsPath); // Use injected loader
            List<String> pluginNames = plugins.stream()
                    .map(PluginInterface::getName)
                    .collect(Collectors.toList());
            response.put("loadedPlugins", pluginNames);
            response.put("status", "success");
            logger.info("Manually loaded plugins from: " + pluginsPath + ", Found: " + pluginNames);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during manual plugin loading", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unload/{pluginName}")
    public ResponseEntity<Map<String, Object>> unloadPlugin(@PathVariable String pluginName) {
        // ... (implementation unchanged)
        Map<String, Object> response = new HashMap<>();
        boolean success = pluginLoader.unloadPlugin(pluginName); // Use injected loader
        if (success) {
            response.put("status", "success");
            response.put("message", "Plugin '" + pluginName + "' unloaded successfully");
            logger.info("Unloaded plugin: " + pluginName);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to unload plugin '" + pluginName + "' (not found or error).");
            logger.warning("Failed attempt to unload plugin: " + pluginName);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unload-all")
    public ResponseEntity<Map<String, Object>> unloadAllPlugins() {
        // ... (implementation unchanged)
        Map<String, Object> response = new HashMap<>();
        int count = pluginLoader.unloadAllPlugins(); // Use injected loader
        response.put("status", "success");
        response.put("message", count + " plugins unloaded successfully");
        logger.info("Unloaded all " + count + " plugins.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{pluginName}/metadata")
    public ResponseEntity<Map<String, Object>> getPluginMetadata(@PathVariable String pluginName) {
        // ... (implementation unchanged)
        logger.fine("Requesting metadata for plugin: " + pluginName);
        PluginInterface plugin = findPluginByName(pluginName);
        if (plugin == null) {
            return buildPluginNotFoundResponse(pluginName);
        }

        try {
            Map<String, Object> metadata = plugin.getMetadata();
            logger.fine("Returning metadata for plugin: " + pluginName);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving metadata for plugin " + pluginName, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false,"errorMessage", "Error retrieving plugin metadata: " + e.getMessage()));
        }
    }
}