package kostovite;

import org.imgscalr.Scalr; // Import imgscalr

import javax.imageio.ImageIO;
import java.awt.*; // Import missing AWT classes
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

// Assuming PluginInterface is standard
public class MediaTools implements PluginInterface {

    // Consider making upload dir configurable
    // private final String uploadDir = "media-output";

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "MediaTools";
    }

    /**
     * Standalone execution for testing (requires manual image loading).
     */
    @Override
    public void execute() {
        System.out.println("MediaTools Plugin executed (standalone test)");
        System.out.println("Note: Standalone test requires providing image data manually.");
        // Example: You would need to load image bytes first
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "MediaTools"); // ID matches class name
        metadata.put("name", "Image Tools"); // User-facing name
        metadata.put("description", "Resize, convert format, apply filters, or get info about images.");
        metadata.put("icon", "Image");
        metadata.put("category", "Media");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Operation and Image Upload ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "inputSection");
        inputSection.put("label", "Operation & Image");

        List<Map<String, Object>> mainInputs = new ArrayList<>();

        // Operation Selection
        mainInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Select Operation:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "resize", "label", "Resize Image"),
                        Map.of("value", "convert", "label", "Convert Format"),
                        Map.of("value", "filter", "label", "Apply Filter"),
                        Map.of("value", "info", "label", "Get Image Info")
                )),
                Map.entry("default", "info"), // Default to info perhaps?
                Map.entry("required", true)
        ));

        // Image Upload Field Placeholder
        // ** IMPORTANT: Frontend needs custom handling for type: "file" **
        // ** Backend expects data likely as Base64 string under a different ID **
        mainInputs.add(Map.ofEntries(
                Map.entry("id", "imageUpload"), // ID for the UI element
                Map.entry("label", "Upload Image:"),
                Map.entry("type", "file"), // Indicates to frontend to show file input
                Map.entry("accept", "image/png, image/jpeg, image/gif, image/bmp"), // Specify accepted types
                Map.entry("required", true),
                Map.entry("helperText", "Select an image file (PNG, JPG, GIF, BMP).")
        ));

        inputSection.put("inputs", mainInputs);
        sections.add(inputSection);


        // --- Section 2: Operation Specific Parameters ---
        Map<String, Object> paramsSection = new HashMap<>();
        paramsSection.put("id", "paramsSection");
        paramsSection.put("label", "Operation Parameters");
        // No top-level condition, handled by individual fields

        List<Map<String, Object>> paramInputs = new ArrayList<>();

        // == Resize Params ==
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "targetWidth"),
                Map.entry("label", "Width (pixels):"),
                Map.entry("type", "number"),
                Map.entry("min", 1),
                Map.entry("max", 8000), // Increased max size
                Map.entry("default", 800),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'resize'")
        ));
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "targetHeight"),
                Map.entry("label", "Height (pixels):"),
                Map.entry("type", "number"),
                Map.entry("min", 1),
                Map.entry("max", 8000),
                Map.entry("default", 600),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'resize'")
        ));
        List<Map<String, String>> formatOptions = List.of(
                Map.of("value", "png", "label", "PNG"),
                Map.of("value", "jpg", "label", "JPEG"),
                Map.of("value", "gif", "label", "GIF"),
                Map.of("value", "bmp", "label", "BMP")
        );
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "outputFormat"), // Used by resize
                Map.entry("label", "Output Format:"),
                Map.entry("type", "select"),
                Map.entry("options", formatOptions),
                Map.entry("default", "png"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'resize'")
        ));

        // == Convert Params ==
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "targetFormat"), // Used by convert
                Map.entry("label", "Target Format:"),
                Map.entry("type", "select"),
                Map.entry("options", formatOptions), // Reuse format options
                Map.entry("default", "png"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'convert'")
        ));

        // == Filter Params ==
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "filterType"), // Used by filter
                Map.entry("label", "Filter Type:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "grayscale", "label", "Grayscale"),
                        Map.of("value", "sepia", "label", "Sepia Tone"),
                        Map.of("value", "invert", "label", "Invert Colors")
                        // Add more filters here and implement in backend
                )),
                Map.entry("default", "grayscale"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'filter'")
        ));

        paramsSection.put("inputs", paramInputs);
        sections.add(paramsSection);


        // --- Section 3: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Output");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Processed Image Output (for resize, convert, filter)
        Map<String, Object> imageOutput = createOutputField("processedImageBase64", "", "image",
                "(uiOperation === 'resize' || uiOperation === 'convert' || uiOperation === 'filter') && typeof processedImageBase64 !== 'undefined'");
        imageOutput.put("buttons", List.of("download"));
        imageOutput.put("downloadFilenameKey", "outputFileName"); // Key for download filename
        resultOutputs.add(imageOutput);

        // Image Info Outputs (for info operation)
        resultOutputs.add(createOutputField("imageInfoWidth", "Width", "text", "uiOperation === 'info' && typeof imageInfoWidth !== 'undefined'"));
        resultOutputs.add(createOutputField("imageInfoHeight", "Height", "text", "uiOperation === 'info' && typeof imageInfoHeight !== 'undefined'"));
        resultOutputs.add(createOutputField("imageInfoType", "Image Type", "text", "uiOperation === 'info' && typeof imageInfoType !== 'undefined'"));
        resultOutputs.add(createOutputField("imageInfoAspectRatio", "Aspect Ratio", "text", "uiOperation === 'info' && typeof imageInfoAspectRatio !== 'undefined'"));
        resultOutputs.add(createOutputField("imageInfoPixelCount", "Pixel Count", "text", "uiOperation === 'info' && typeof imageInfoPixelCount !== 'undefined'"));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 4: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create output field definitions more easily
    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", type);
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error");
        }
        if ("text".equals(type) && (id.toLowerCase().contains("count") || id.toLowerCase().contains("width") || id.toLowerCase().contains("height"))) {
            field.put("monospace", true); // Monospace for counts/dimensions
        }
        if ("image".equals(type)){
            field.put("maxWidth", 400); // Default max width for image outputs
            field.put("maxHeight", 400); // Default max height
        }
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format)
     * to perform image operations.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage";
        String uiOperation = getStringParam(input, "uiOperation", "info"); // Default operation

        try {
            // --- Get Image Data ---
            // ** CRITICAL: Assumes frontend sends base64 data with this key **
            // ** Modify if frontend sends data differently **
            byte[] imageData = getImageDataFromBase64(input); // Expect base64 key

            if (imageData == null || imageData.length == 0) {
                throw new IllegalArgumentException("No image data provided or failed to decode. Please upload an image.");
            }

            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "resize":
                    int width = getIntParam(input, "targetWidth", 800); // Use new ID
                    int height = getIntParam(input, "targetHeight", 600); // Use new ID
                    String format = getStringParam(input, "outputFormat", "png"); // Use new ID
                    result = resizeImage(imageData, width, height, format);
                    break;

                case "convert":
                    String targetFormat = getStringParam(input, "targetFormat", "png"); // Use new ID
                    result = convertImageFormat(imageData, targetFormat);
                    break;

                case "filter":
                    String filter = getStringParam(input, "filterType", "grayscale"); // Use new ID
                    result = applyFilter(imageData, filter);
                    break;

                case "info":
                    result = getImageInfo(imageData);
                    break;

                default:
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
            }

            Map<String, Object> finalResult = new HashMap<>(result); // Start with specific results
            finalResult.put("success", !result.containsKey("error")); // Determine success
            finalResult.put("uiOperation", uiOperation); // Add operation context

            // Rename result keys to match output IDs
            if (finalResult.containsKey("base64")) {
                finalResult.put("processedImageBase64", "data:image/" + finalResult.getOrDefault("format","png") + ";base64," + finalResult.get("base64"));
                finalResult.remove("base64"); // Remove original key
                // Suggest a filename for download
                finalResult.put("outputFileName", "processed_image." + finalResult.getOrDefault("format","png"));
            }
            if (finalResult.containsKey("error") && !finalResult.containsKey(errorOutputId)) {
                finalResult.put(errorOutputId, finalResult.get("error"));
                finalResult.remove("error");
            }
            // Map info keys with explicit casting for String.format
            if (finalResult.containsKey("width")) {
                // Assuming width is Integer from BufferedImage.getWidth()
                finalResult.put("imageInfoWidth", String.format(Locale.US,"%,d px", (Integer) finalResult.get("width")));
            }
            if (finalResult.containsKey("height")) {
                // Assuming height is Integer from BufferedImage.getHeight()
                finalResult.put("imageInfoHeight", String.format(Locale.US,"%,d px", (Integer) finalResult.get("height")));
            }
            if (finalResult.containsKey("type")) {
                finalResult.put("imageInfoType", finalResult.get("type")); // Type is already String
            }
            if (finalResult.containsKey("aspectRatio")) {
                // *** Cast to Double ***
                finalResult.put("imageInfoAspectRatio", String.format(Locale.US, "%.2f", (Double) finalResult.get("aspectRatio")));
            }
            if (finalResult.containsKey("pixelCount")) {
                // *** Cast to Long ***
                finalResult.put("imageInfoPixelCount", String.format(Locale.US, "%,d", (Long) finalResult.get("pixelCount")));
            }
            // Remove original info keys if mapped (do this AFTER using them)
            finalResult.remove("width");
            finalResult.remove("height");
            finalResult.remove("type");
            finalResult.remove("aspectRatio");
            finalResult.remove("pixelCount");
            finalResult.remove("format");


            return finalResult;

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error processing image: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Failed to read or process image data: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error processing image: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Helper method to extract image data from Base64 string in input.
     * Assumes frontend sends Base64 string with key "imageBase64".
     */
    private byte[] getImageDataFromBase64(Map<String, Object> input) {
        Object data = input.get("imageUploadBase64");
        if (data instanceof String base64Data) {
            try {
                // Remove data URI header if present (e.g., "data:image/png;base64,")
                if (base64Data.startsWith("data:image")) {
                    base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
                }
                return Base64.getDecoder().decode(base64Data);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid Base64 data received for key '" + "imageUploadBase64" + "'");
                throw new IllegalArgumentException("Invalid image data format received (expecting Base64).");
            }
        } else if (data != null) {
            System.err.println("Unexpected data type received for image key '" + "imageUploadBase64" + "': " + data.getClass().getName());
            throw new IllegalArgumentException("Incorrect image data type received.");
        }
        // If the key wasn't present or data was null
        // Depending on requirement, either throw or return null
        // For now, assume required elsewhere if needed.
        return null;
    }


    /**
     * Resize an image using imgscalr.
     */
    public Map<String, Object> resizeImage(byte[] imageData, int width, int height, String format) throws IOException {
        Map<String, Object> result = new HashMap<>();
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) throw new IOException("Could not decode input image data.");

        // Use Scalr for quality resizing
        BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);

        // Convert to bytes in target format
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(resizedImage, format, baos)) {
            throw new IOException("No writer found for format: " + format);
        }
        byte[] resizedData = baos.toByteArray();

        result.put("base64", Base64.getEncoder().encodeToString(resizedData));
        result.put("format", format);
        result.put("width", resizedImage.getWidth()); // Actual width after resize
        result.put("height", resizedImage.getHeight()); // Actual height
        // result.put("data", resizedData); // Usually don't return raw bytes
        return result;
    }

    /**
     * Convert image format.
     */
    public Map<String, Object> convertImageFormat(byte[] imageData, String targetFormat) throws IOException {
        Map<String, Object> result = new HashMap<>();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) throw new IOException("Could not decode input image data.");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Handle formats like JPEG which don't support transparency well
        if ("jpg".equalsIgnoreCase(targetFormat) || "jpeg".equalsIgnoreCase(targetFormat)) {
            // Create a new image with white background if converting from type with alpha
            if (image.getColorModel().hasAlpha()) {
                BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = newImage.createGraphics();
                try {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, image.getWidth(), image.getHeight());
                    g.drawImage(image, 0, 0, null);
                } finally {
                    g.dispose();
                }
                image = newImage; // Use the new image without alpha
            }
        }

        if (!ImageIO.write(image, targetFormat, baos)) {
            throw new IOException("No writer found for format: " + targetFormat);
        }
        byte[] convertedData = baos.toByteArray();

        result.put("base64", Base64.getEncoder().encodeToString(convertedData));
        result.put("format", targetFormat);
        result.put("width", image.getWidth());
        result.put("height", image.getHeight());
        return result;
    }

    /**
     * Apply a filter to an image.
     */
    public Map<String, Object> applyFilter(byte[] imageData, String filter) throws IOException {
        Map<String, Object> result = new HashMap<>();
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) throw new IOException("Could not decode input image data.");

        BufferedImage filteredImage;
        String outputFormat = "png"; // Filters often best saved as PNG

        filteredImage = switch (filter.toLowerCase()) {
            case "grayscale" -> toGrayscale(originalImage);
            case "invert" -> invertColors(originalImage);
            case "sepia" -> toSepia(originalImage);
            default -> throw new IllegalArgumentException("Unknown filter type: " + filter);
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(filteredImage, outputFormat, baos)) {
            throw new IOException("No writer found for format: " + outputFormat);
        }
        byte[] filteredData = baos.toByteArray();

        result.put("base64", Base64.getEncoder().encodeToString(filteredData));
        result.put("format", outputFormat); // Indicate the format saved in
        result.put("filter", filter);
        result.put("width", filteredImage.getWidth());
        result.put("height", filteredImage.getHeight());
        return result;
    }

    /**
     * Get information about an image.
     */
    public Map<String, Object> getImageInfo(byte[] imageData) throws IOException {
        Map<String, Object> result = new HashMap<>();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) throw new IOException("Could not decode input image data.");

        result.put("width", image.getWidth());
        result.put("height", image.getHeight());
        result.put("type", getImageTypeDescription(image.getType())); // Use description
        result.put("aspectRatio", (double) image.getWidth() / image.getHeight());
        result.put("pixelCount", (long) image.getWidth() * image.getHeight()); // Use long for pixel count

        return result;
    }

    // --- Private Filter/Util Methods ---

    private BufferedImage toGrayscale(BufferedImage original) {
        BufferedImage gray = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = gray.getGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return gray;
    }

    private BufferedImage invertColors(BufferedImage original) {
        BufferedImage inverted = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgba = original.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
                inverted.setRGB(x, y, col.getRGB());
            }
        }
        return inverted;
    }

    private BufferedImage toSepia(BufferedImage original) {
        BufferedImage sepia = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgba = original.getRGB(x, y);
                Color col = new Color(rgba, true);
                int r = col.getRed(); int g = col.getGreen(); int b = col.getBlue();
                int tr = (int)(0.393*r + 0.769*g + 0.189*b);
                int tg = (int)(0.349*r + 0.686*g + 0.168*b);
                int tb = (int)(0.272*r + 0.534*g + 0.131*b);
                r = Math.min(255, tr); g = Math.min(255, tg); b = Math.min(255, tb);
                sepia.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return sepia;
    }

    private String getImageTypeDescription(int type) {
        return switch (type) {
            case BufferedImage.TYPE_INT_RGB -> "INT_RGB (Standard Color)";
            case BufferedImage.TYPE_INT_ARGB -> "INT_ARGB (Color with Alpha)";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "INT_ARGB_PRE (Color with Premultiplied Alpha)";
            case BufferedImage.TYPE_INT_BGR -> "INT_BGR (Blue Green Red)";
            case BufferedImage.TYPE_3BYTE_BGR -> "3BYTE_BGR (Blue Green Red)";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4BYTE_ABGR (Alpha Blue Green Red)";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE (Alpha Premultiplied)";
            case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB";
            case BufferedImage.TYPE_BYTE_GRAY -> "BYTE_GRAY (Grayscale)";
            case BufferedImage.TYPE_USHORT_GRAY -> "USHORT_GRAY (Grayscale)";
            case BufferedImage.TYPE_BYTE_BINARY -> "BYTE_BINARY (Black & White)";
            case BufferedImage.TYPE_BYTE_INDEXED -> "BYTE_INDEXED (Palette)";
            default -> "CUSTOM / Unknown (" + type + ")";
        };
    }

    // --- Parameter Parsing Helpers ---

    private int getIntParam(Map<String, Object> input, String key, Integer defaultValue) throws IllegalArgumentException {
        // Reuse helper from previous examples
        Object value = input.get(key);
        // ... (rest of getIntParam implementation) ...
        if (value == null) {
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required integer parameter: " + key);
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            if (dValue == Math.floor(dValue)) return ((Number) value).intValue();
            else throw new IllegalArgumentException("Non-integer numeric value for integer parameter '" + key + "': " + value);
        }
        else {
            try { return Integer.parseInt(value.toString()); }
            catch (NumberFormatException e) {
                if (defaultValue != null) return defaultValue;
                throw new IllegalArgumentException("Invalid integer value for parameter '" + key + "': " + value);
            }
        }
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        // Reuse helper from previous examples
        Object value = input.get(key);
        // ... (rest of getStringParam implementation) ...
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return strValue;
    }

}