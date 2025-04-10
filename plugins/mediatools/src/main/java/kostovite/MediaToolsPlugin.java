package kostovite;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * MediaTools Plugin that implements both PluginInterface and ExtendedPluginInterface
 */
public class MediaToolsPlugin implements PluginInterface, ExtendedPluginInterface {

    public MediaToolsPlugin() {
        // Constructor
        System.out.println("MediaTools Plugin instantiated");
    }

    @Override
    public String getName() {
        return "MediaTools";
    }

    @Override
    public void execute() {
        System.out.println("MediaTools Plugin executed");
    }

    // This method is probably in ExtendedPluginInterface
    public boolean supportsUniversalInterface() {
        return true;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("category", "Media Processing");
        metadata.put("description", "Tools for processing images and videos");

        // List available operations
        Map<String, Object> operations = new HashMap<>();
        operations.put("resizeImage", "Resize an image to specified dimensions");
        operations.put("convertFormat", "Convert image between formats");
        operations.put("applyFilter", "Apply filter to image");
        operations.put("getImageInfo", "Get information about an image");
        operations.put("testimage", "Test with a predefined image");
        metadata.put("operations", operations);

        // Supported formats
        Map<String, Object> supportedFormats = new HashMap<>();
        supportedFormats.put("images", new String[]{"jpg", "png", "gif", "bmp"});
        metadata.put("supportedFormats", supportedFormats);

        // Supported filters
        metadata.put("filters", new String[]{"grayscale", "invert", "sepia"});

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.get("operation");
            if (operation == null) {
                result.put("error", "No operation specified");
                return result;
            }

            // Handle operations that don't need image data
            if ("testimage".equals(operation) || "getSupportedOperations".equals(operation) || "getMetadata".equals(operation)) {
                if ("testimage".equals(operation)) {
                    result.put("status", "success");
                    result.put("message", "Test image processed successfully");
                    result.put("dimensions", "1x1");
                    result.put("format", "PNG");
                    result.put("size", "67 bytes");
                } else {
                    return getMetadata();
                }
                return result;
            }

            // Get input image data for other operations
            byte[] imageData = getImageData(input);
            if (imageData == null) {
                result.put("error", "No image data provided or invalid format");
                return result;
            }

            switch (operation) {
                case "resizeImage":
                    int width = getIntParam(input, "width", 0);
                    int height = getIntParam(input, "height", 0);
                    String format = getStringParam(input, "format", "png");

                    if (width <= 0 || height <= 0) {
                        result.put("error", "Invalid dimensions");
                        return result;
                    }

                    return MediaTools.resizeImage(imageData, width, height, format);

                case "convertFormat":
                    String targetFormat = getStringParam(input, "format", "png");
                    return MediaTools.convertImageFormat(imageData, targetFormat);

                case "applyFilter":
                    String filter = getStringParam(input, "filter", "grayscale");
                    return MediaTools.applyFilter(imageData, filter);

                case "getImageInfo":
                    return MediaTools.getImageInfo(imageData);

                default:
                    result.put("error", "Unknown operation: " + operation);
                    return result;
            }
        } catch (Exception e) {
            System.out.println("Error in MediaTools.process(): " + e.getMessage());
            e.printStackTrace();
            result.put("error", "Error processing media: " + e.getMessage());
            return result;
        }
    }

    // Helper methods

    private byte[] getImageData(Map<String, Object> input) {
        Object imageInput = input.get("image");

        if (imageInput == null) {
            System.out.println("DEBUG: Image input is null");
            return null;
        }

        if (imageInput instanceof byte[]) {
            System.out.println("DEBUG: Image input is byte array of length: " + ((byte[])imageInput).length);
            return (byte[]) imageInput;
        } else if (imageInput instanceof String) {
            try {
                System.out.println("DEBUG: Image input is string with length: " + ((String)imageInput).length());
                byte[] decodedBytes = Base64.getDecoder().decode((String) imageInput);
                System.out.println("DEBUG: Decoded " + decodedBytes.length + " bytes");
                return decodedBytes;
            } catch (IllegalArgumentException e) {
                System.out.println("DEBUG: Base64 decoding error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        System.out.println("DEBUG: Image input is of unexpected type: " + imageInput.getClass().getName());
        return null;
    }

    private int getIntParam(Map<String, Object> input, String key, int defaultValue) {
        Object value = input.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
}