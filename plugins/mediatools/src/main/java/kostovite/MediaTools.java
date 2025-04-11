package kostovite;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaTools implements PluginInterface {

    @Override
    public String getName() {
        return "MediaTools";
    }

    @Override
    public void execute() {
        System.out.println("MediaTools Plugin executed");

        // Demo functionality can be added here if needed
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Image processing and media utilities"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Resize image operation
        Map<String, Object> resizeOperation = new HashMap<>();
        resizeOperation.put("description", "Resize an image to specified dimensions");
        Map<String, Object> resizeInputs = new HashMap<>();
        resizeInputs.put("imageData", Map.of("type", "binary", "description", "Binary image data", "required", true));
        resizeInputs.put("width", Map.of("type", "integer", "description", "Target width in pixels", "required", true));
        resizeInputs.put("height", Map.of("type", "integer", "description", "Target height in pixels", "required", true));
        resizeInputs.put("format", Map.of("type", "string", "description", "Output format (jpg, png, etc.)", "required", true));
        resizeOperation.put("inputs", resizeInputs);
        operations.put("resize", resizeOperation);

        // Convert format operation
        Map<String, Object> convertOperation = new HashMap<>();
        convertOperation.put("description", "Convert an image to a different format");
        Map<String, Object> convertInputs = new HashMap<>();
        convertInputs.put("imageData", Map.of("type", "binary", "description", "Binary image data", "required", true));
        convertInputs.put("targetFormat", Map.of("type", "string", "description", "Target format (jpg, png, etc.)", "required", true));
        convertOperation.put("inputs", convertInputs);
        operations.put("convert", convertOperation);

        // Apply filter operation
        Map<String, Object> filterOperation = new HashMap<>();
        filterOperation.put("description", "Apply a filter to an image");
        Map<String, Object> filterInputs = new HashMap<>();
        filterInputs.put("imageData", Map.of("type", "binary", "description", "Binary image data", "required", true));
        filterInputs.put("filter", Map.of("type", "string", "description", "Filter type (grayscale, invert, sepia)", "required", true));
        filterOperation.put("inputs", filterInputs);
        operations.put("filter", filterOperation);

        // Get image info operation
        Map<String, Object> infoOperation = new HashMap<>();
        infoOperation.put("description", "Get information about an image");
        Map<String, Object> infoInputs = new HashMap<>();
        infoInputs.put("imageData", Map.of("type", "binary", "description", "Binary image data", "required", true));
        infoOperation.put("inputs", infoInputs);
        operations.put("info", infoOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "MediaTools"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Image"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Media"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Image Operation");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "operation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "resize", "label", "Resize Image"));
        operationOptions.add(Map.of("value", "convert", "label", "Convert Format"));
        operationOptions.add(Map.of("value", "filter", "label", "Apply Filter"));
        operationOptions.add(Map.of("value", "info", "label", "Image Information"));
        operationField.put("options", operationOptions);
        operationField.put("default", "resize");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Image Input
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Image Input");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Image upload field
        Map<String, Object> imageField = new HashMap<>();
        imageField.put("name", "imageUpload");
        imageField.put("label", "Upload Image:");
        imageField.put("type", "file");
        imageField.put("accept", "image/*");
        imageField.put("required", true);
        section2Fields.add(imageField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Resize Parameters (conditional)
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Resize Parameters");
        inputSection3.put("condition", "operation === 'resize'");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Width field
        Map<String, Object> widthField = new HashMap<>();
        widthField.put("name", "width");
        widthField.put("label", "Width (pixels):");
        widthField.put("type", "number");
        widthField.put("min", 1);
        widthField.put("max", 5000);
        widthField.put("default", 800);
        widthField.put("required", true);
        section3Fields.add(widthField);

        // Height field
        Map<String, Object> heightField = new HashMap<>();
        heightField.put("name", "height");
        heightField.put("label", "Height (pixels):");
        heightField.put("type", "number");
        heightField.put("min", 1);
        heightField.put("max", 5000);
        heightField.put("default", 600);
        heightField.put("required", true);
        section3Fields.add(heightField);

        // Format field
        Map<String, Object> formatField = new HashMap<>();
        formatField.put("name", "format");
        formatField.put("label", "Output Format:");
        formatField.put("type", "select");
        List<Map<String, String>> formatOptions = new ArrayList<>();
        formatOptions.add(Map.of("value", "png", "label", "PNG"));
        formatOptions.add(Map.of("value", "jpg", "label", "JPEG"));
        formatOptions.add(Map.of("value", "gif", "label", "GIF"));
        formatOptions.add(Map.of("value", "bmp", "label", "BMP"));
        formatField.put("options", formatOptions);
        formatField.put("default", "png");
        formatField.put("required", true);
        section3Fields.add(formatField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        // Input Section 4: Convert Format Parameters (conditional)
        Map<String, Object> inputSection4 = new HashMap<>();
        inputSection4.put("header", "Convert Format Parameters");
        inputSection4.put("condition", "operation === 'convert'");
        List<Map<String, Object>> section4Fields = new ArrayList<>();

        // Target format field
        Map<String, Object> targetFormatField = new HashMap<>();
        targetFormatField.put("name", "targetFormat");
        targetFormatField.put("label", "Target Format:");
        targetFormatField.put("type", "select");
        targetFormatField.put("options", formatOptions); // Reuse the same options
        targetFormatField.put("default", "png");
        targetFormatField.put("required", true);
        section4Fields.add(targetFormatField);

        inputSection4.put("fields", section4Fields);
        uiInputs.add(inputSection4);

        // Input Section 5: Filter Parameters (conditional)
        Map<String, Object> inputSection5 = new HashMap<>();
        inputSection5.put("header", "Filter Parameters");
        inputSection5.put("condition", "operation === 'filter'");
        List<Map<String, Object>> section5Fields = new ArrayList<>();

        // Filter selection field
        Map<String, Object> filterField = new HashMap<>();
        filterField.put("name", "filter");
        filterField.put("label", "Filter Type:");
        filterField.put("type", "select");
        List<Map<String, String>> filterOptions = new ArrayList<>();
        filterOptions.add(Map.of("value", "grayscale", "label", "Grayscale"));
        filterOptions.add(Map.of("value", "sepia", "label", "Sepia Tone"));
        filterOptions.add(Map.of("value", "invert", "label", "Invert Colors"));
        filterField.put("options", filterOptions);
        filterField.put("default", "grayscale");
        filterField.put("required", true);
        section5Fields.add(filterField);

        inputSection5.put("fields", section5Fields);
        uiInputs.add(inputSection5);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Processed Image
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Processed Image");
        outputSection1.put("condition", "success && (operation === 'resize' || operation === 'convert' || operation === 'filter')");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Image display
        Map<String, Object> resultImageOutput = new HashMap<>();
        resultImageOutput.put("title", "Result");
        resultImageOutput.put("name", "resultImage");
        resultImageOutput.put("type", "image");
        resultImageOutput.put("formula", "'data:image/' + (format || targetFormat || 'png') + ';base64,' + base64");
        resultImageOutput.put("buttons", List.of("download"));
        section1OutputFields.add(resultImageOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Image Information
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Image Information");
        outputSection2.put("condition", "success && operation === 'info'");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Width
        Map<String, Object> widthOutput = new HashMap<>();
        widthOutput.put("title", "Width");
        widthOutput.put("name", "width");
        widthOutput.put("type", "text");
        widthOutput.put("formula", "width + ' pixels'");
        section2OutputFields.add(widthOutput);

        // Height
        Map<String, Object> heightOutput = new HashMap<>();
        heightOutput.put("title", "Height");
        heightOutput.put("name", "height");
        heightOutput.put("type", "text");
        heightOutput.put("formula", "height + ' pixels'");
        section2OutputFields.add(heightOutput);

        // Type
        Map<String, Object> typeOutput = new HashMap<>();
        typeOutput.put("title", "Image Type");
        typeOutput.put("name", "type");
        typeOutput.put("type", "text");
        section2OutputFields.add(typeOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "error");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section3OutputFields.add(errorOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "info");
            byte[] imageData = getImageData(input);

            if (imageData == null || imageData.length == 0) {
                result.put("error", "No image data provided");
                return result;
            }

            switch (operation.toLowerCase()) {
                case "resize":
                    int width = Integer.parseInt(input.get("width").toString());
                    int height = Integer.parseInt(input.get("height").toString());
                    String format = (String) input.getOrDefault("format", "png");
                    result = resizeImage(imageData, width, height, format);
                    result.put("success", !result.containsKey("error"));
                    break;

                case "convert":
                    String targetFormat = (String) input.getOrDefault("targetFormat", "png");
                    result = convertImageFormat(imageData, targetFormat);
                    result.put("success", !result.containsKey("error"));
                    break;

                case "filter":
                    String filter = (String) input.getOrDefault("filter", "grayscale");
                    result = applyFilter(imageData, filter);
                    result.put("success", !result.containsKey("error"));
                    break;

                case "info":
                    result = getImageInfo(imageData);
                    result.put("success", !result.containsKey("error"));
                    break;

                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

        } catch (Exception e) {
            result.put("error", "Error processing image: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Helper method to extract image data from various input formats
     */
    private byte[] getImageData(Map<String, Object> input) {
        try {
            // Handle direct binary data
            if (input.containsKey("imageData")) {
                return (byte[]) input.get("imageData");
            }

            // Handle base64 encoded data
            if (input.containsKey("imageBase64")) {
                String base64Data = (String) input.get("imageBase64");
                // Remove header if present
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                return Base64.getDecoder().decode(base64Data);
            }

            // Handle file upload data (assuming it's already been converted to bytes)
            if (input.containsKey("imageUpload")) {
                return (byte[]) input.get("imageUpload");
            }
        } catch (Exception e) {
            System.err.println("Error processing image data: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Resize an image to the specified dimensions
     */
    public Map<String, Object> resizeImage(byte[] imageData, int width, int height, String format) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Create resized image using imgscalr library for better quality
        BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height);

        // Convert back to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, outputStream);
        byte[] resizedData = outputStream.toByteArray();

        // Return result
        result.put("data", resizedData);
        result.put("base64", Base64.getEncoder().encodeToString(resizedData));
        result.put("format", format);
        result.put("width", width);
        result.put("height", height);

        return result;
    }

    /**
     * Convert an image to a different format
     */
    public Map<String, Object> convertImageFormat(byte[] imageData, String targetFormat) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Convert to target format
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, targetFormat, outputStream);
        byte[] convertedData = outputStream.toByteArray();

        // Return result
        result.put("data", convertedData);
        result.put("base64", Base64.getEncoder().encodeToString(convertedData));
        result.put("format", targetFormat);
        result.put("width", image.getWidth());
        result.put("height", image.getHeight());

        return result;
    }

    /**
     * Apply a filter to an image
     */
    public Map<String, Object> applyFilter(byte[] imageData, String filter) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Apply filter
        BufferedImage filteredImage;

        switch (filter.toLowerCase()) {
            case "grayscale":
                filteredImage = toGrayscale(originalImage);
                break;
            case "invert":
                filteredImage = invertColors(originalImage);
                break;
            case "sepia":
                filteredImage = toSepia(originalImage);
                break;
            default:
                result.put("error", "Unknown filter: " + filter);
                return result;
        }

        // Convert back to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(filteredImage, "png", outputStream);
        byte[] filteredData = outputStream.toByteArray();

        // Return result
        result.put("data", filteredData);
        result.put("base64", Base64.getEncoder().encodeToString(filteredData));
        result.put("filter", filter);
        result.put("width", filteredImage.getWidth());
        result.put("height", filteredImage.getHeight());

        return result;
    }

    /**
     * Get information about an image
     */
    public Map<String, Object> getImageInfo(byte[] imageData) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Get image info
        result.put("width", image.getWidth());
        result.put("height", image.getHeight());
        result.put("type", getImageType(image.getType()));
        result.put("aspectRatio", (double) image.getWidth() / image.getHeight());
        result.put("pixelCount", image.getWidth() * image.getHeight());

        return result;
    }

    // Helper methods for filter effects

    private BufferedImage toGrayscale(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        grayscaleImage.getGraphics().drawImage(original, 0, 0, null);
        return grayscaleImage;
    }

    private BufferedImage invertColors(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage invertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = 255 - ((rgb >> 16) & 0xff);
                int g = 255 - ((rgb >> 8) & 0xff);
                int b = 255 - (rgb & 0xff);
                int newRgb = (r << 16) | (g << 8) | b;
                invertedImage.setRGB(x, y, newRgb);
            }
        }

        return invertedImage;
    }

    private BufferedImage toSepia(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage sepiaImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Sepia formula
                int newRed = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                int newGreen = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                int newBlue = (int) (0.272 * r + 0.534 * g + 0.131 * b);

                // Clamp values
                newRed = Math.min(255, newRed);
                newGreen = Math.min(255, newGreen);
                newBlue = Math.min(255, newBlue);

                int newRgb = (newRed << 16) | (newGreen << 8) | newBlue;
                sepiaImage.setRGB(x, y, newRgb);
            }
        }

        return sepiaImage;
    }

    private String getImageType(int type) {
        return switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR -> "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY -> "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY -> "BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED -> "BYTE_INDEXED";
            case BufferedImage.TYPE_INT_ARGB -> "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR -> "INT_BGR";
            case BufferedImage.TYPE_INT_RGB -> "INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY -> "USHORT_GRAY";
            default -> "CUSTOM";
        };
    }
}