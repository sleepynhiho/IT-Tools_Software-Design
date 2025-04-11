package kostovite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRCodeGenerator implements PluginInterface {

    private final String uploadDir = "qrcodes";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)$");

    public QRCodeGenerator() {
        // Create upload directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public String getName() {
        return "QRCodeGenerator";
    }

    @Override
    public void execute() {
        System.out.println("QRCode Generator Plugin executed");

        // Demonstrate basic usage
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("text", "https://example.com");
            params.put("foregroundColor", "#000000");
            params.put("backgroundColor", "#FFFFFF");
            params.put("errorCorrection", "M");
            params.put("size", 250);

            Map<String, Object> result = process(params);
            System.out.println("Sample QR code generated: " + result.get("fileName"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generate QR codes with custom colors and error correction"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Generate operation
        Map<String, Object> generateOperation = new HashMap<>();
        generateOperation.put("description", "Generate a QR code image");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("text", Map.of("type", "string", "description", "Text to encode in the QR code", "required", true));
        inputs.put("foregroundColor", Map.of("type", "string", "description", "Foreground color (hex code or RGB)", "required", false));
        inputs.put("backgroundColor", Map.of("type", "string", "description", "Background color (hex code or RGB)", "required", false));
        inputs.put("errorCorrection", Map.of("type", "string", "description", "Error correction level (L, M, Q, H)", "required", false));
        inputs.put("size", Map.of("type", "integer", "description", "Size of the QR code in pixels", "required", false));
        inputs.put("fileName", Map.of("type", "string", "description", "Optional filename (without extension)", "required", false));
        generateOperation.put("inputs", inputs);
        operations.put("generate", generateOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "QRCodeGenerator"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "QrCode"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Utilities"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Content
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "QR Code Content");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Text content field
        Map<String, Object> textField = new HashMap<>();
        textField.put("name", "text");
        textField.put("label", "Content:");
        textField.put("type", "text");
        textField.put("multiline", true);
        textField.put("rows", 3);
        textField.put("placeholder", "Enter text or URL to encode in the QR code");
        textField.put("required", true);
        textField.put("helperText", "Text, URL, contact info, or any data to encode");
        section1Fields.add(textField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Appearance
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Appearance");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Size field
        Map<String, Object> sizeField = new HashMap<>();
        sizeField.put("name", "size");
        sizeField.put("label", "Size (pixels):");
        sizeField.put("type", "slider");
        sizeField.put("min", 100);
        sizeField.put("max", 1000);
        sizeField.put("step", 50);
        sizeField.put("default", 250);
        sizeField.put("required", false);
        section2Fields.add(sizeField);

        // Foreground color field
        Map<String, Object> fgColorField = new HashMap<>();
        fgColorField.put("name", "foregroundColor");
        fgColorField.put("label", "Foreground Color:");
        fgColorField.put("type", "color");
        fgColorField.put("default", "#000000");
        fgColorField.put("required", false);
        section2Fields.add(fgColorField);

        // Background color field
        Map<String, Object> bgColorField = new HashMap<>();
        bgColorField.put("name", "backgroundColor");
        bgColorField.put("label", "Background Color:");
        bgColorField.put("type", "color");
        bgColorField.put("default", "#FFFFFF");
        bgColorField.put("required", false);
        section2Fields.add(bgColorField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Settings
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Settings");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Error correction field
        Map<String, Object> errorCorrectionField = new HashMap<>();
        errorCorrectionField.put("name", "errorCorrection");
        errorCorrectionField.put("label", "Error Correction:");
        errorCorrectionField.put("type", "select");
        List<Map<String, String>> ecOptions = new ArrayList<>();
        ecOptions.add(Map.of("value", "L", "label", "Low (7%)"));
        ecOptions.add(Map.of("value", "M", "label", "Medium (15%)"));
        ecOptions.add(Map.of("value", "Q", "label", "Quartile (25%)"));
        ecOptions.add(Map.of("value", "H", "label", "High (30%)"));
        errorCorrectionField.put("options", ecOptions);
        errorCorrectionField.put("default", "M");
        errorCorrectionField.put("helperText", "Higher levels make QR code more resistant to damage");
        errorCorrectionField.put("required", false);
        section3Fields.add(errorCorrectionField);

        // File name field
        Map<String, Object> fileNameField = new HashMap<>();
        fileNameField.put("name", "fileName");
        fileNameField.put("label", "File Name:");
        fileNameField.put("type", "text");
        fileNameField.put("placeholder", "qrcode");
        fileNameField.put("helperText", "Optional custom filename (without extension)");
        fileNameField.put("required", false);
        section3Fields.add(fileNameField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Generated QR Code
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Generated QR Code");
        outputSection1.put("condition", "typeof success !== 'undefined' && success"); // FIXED
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // QR Code Image
        Map<String, Object> qrImageOutput = new HashMap<>();
        qrImageOutput.put("title", "");
        qrImageOutput.put("name", "imageBase64");
        qrImageOutput.put("type", "image");
        qrImageOutput.put("maxWidth", 300);
        qrImageOutput.put("buttons", List.of("download"));
        section1OutputFields.add(qrImageOutput);

        // Encoded text
        Map<String, Object> encodedTextOutput = new HashMap<>();
        encodedTextOutput.put("title", "Encoded Content");
        encodedTextOutput.put("name", "text");
        encodedTextOutput.put("type", "text");
        encodedTextOutput.put("multiline", true);
        encodedTextOutput.put("maxLength", 100);
        section1OutputFields.add(encodedTextOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: QR Code Details
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "QR Code Details");
        outputSection2.put("condition", "typeof success !== 'undefined' && success"); // FIXED
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Size
        Map<String, Object> sizeOutput = new HashMap<>();
        sizeOutput.put("title", "Size");
        sizeOutput.put("name", "sizeDisplay");
        sizeOutput.put("type", "text");
        sizeOutput.put("formula", "size + ' × ' + size + ' pixels'");
        section2OutputFields.add(sizeOutput);

        // Colors used
        Map<String, Object> colorsOutput = new HashMap<>();
        colorsOutput.put("title", "Colors");
        colorsOutput.put("name", "colorsDisplay");
        colorsOutput.put("type", "chips");
        colorsOutput.put("items", "['Foreground: ' + foregroundColor, 'Background: ' + backgroundColor]");
        colorsOutput.put("colors", "[foregroundColor, backgroundColor]");
        section2OutputFields.add(colorsOutput);

        // Error correction level
        Map<String, Object> ecLevelOutput = new HashMap<>();
        ecLevelOutput.put("title", "Error Correction");
        ecLevelOutput.put("name", "errorCorrectionDisplay");
        ecLevelOutput.put("type", "text");
        ecLevelOutput.put("formula", "errorCorrection === 'L' ? 'Low (7%)' : errorCorrection === 'M' ? 'Medium (15%)' : errorCorrection === 'Q' ? 'Quartile (25%)' : 'High (30%)'");
        section2OutputFields.add(ecLevelOutput);

        // File info
        Map<String, Object> fileInfoOutput = new HashMap<>();
        fileInfoOutput.put("title", "File Info");
        fileInfoOutput.put("name", "fileInfoDisplay");
        fileInfoOutput.put("type", "text");
        fileInfoOutput.put("formula", "fileName + ' (' + (fileSize / 1024).toFixed(1) + ' KB)'");
        section2OutputFields.add(fileInfoOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "typeof error !== 'undefined' && error"); // FIXED
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
            String operation = (String) input.getOrDefault("operation", "generate");

            if ("generate".equalsIgnoreCase(operation)) {
                return generateQRCode(input);
            } else {
                result.put("error", "Unsupported operation: " + operation);
                return result;
            }
        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> generateQRCode(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get parameters
            String text = (String) input.get("text");
            String foregroundColorStr = (String) input.getOrDefault("foregroundColor", "#000000");
            String backgroundColorStr = (String) input.getOrDefault("backgroundColor", "#FFFFFF");
            String errorCorrectionStr = (String) input.getOrDefault("errorCorrection", "M");
            int size = input.containsKey("size") ? parseIntSafe(input.get("size").toString()) : 250;
            String fileName = (String) input.getOrDefault("fileName",
                    "qrcode_" + LocalDateTime.now().format(formatter));

            // Validate inputs
            if (text == null || text.trim().isEmpty()) {
                result.put("error", "Text cannot be empty");
                return result;
            }

            // Parse colors
            Color foregroundColor = parseColor(foregroundColorStr);
            Color backgroundColor = parseColor(backgroundColorStr);

            // Parse error correction level
            ErrorCorrectionLevel errorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionStr);

            // Set QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.MARGIN, 2);

            // Create QR code writer and encode the text
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            // Create buffered image that will hold the QR code
            BufferedImage qrImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = qrImage.createGraphics();

            // Fill background
            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, size, size);

            // Paint QR code
            graphics.setColor(foregroundColor);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            graphics.dispose();

            // Save image to file
            String fileNameWithExtension = fileName.endsWith(".png") ? fileName : fileName + ".png";
            String filePath = uploadDir + File.separator + fileNameWithExtension;
            File outputFile = new File(filePath);
            ImageIO.write(qrImage, "png", outputFile);

            // Convert image to Base64 for inline display
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", outputStream);
            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            // Build result
            result.put("success", true);
            result.put("text", text);
            result.put("fileName", fileNameWithExtension);
            result.put("filePath", filePath);
            result.put("fileSize", outputFile.length());
            result.put("foregroundColor", colorToHex(foregroundColor));
            result.put("backgroundColor", colorToHex(backgroundColor));
            result.put("errorCorrection", errorCorrectionLevel.toString());
            result.put("size", size);
            result.put("imageBase64", "data:image/png;base64," + base64Image);

        } catch (WriterException e) {
            result.put("error", "Error generating QR code: " + e.getMessage());
        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Parse a color string in hex or RGB format
     * @param colorStr The color string (e.g., "#FF0000" or "rgb(255, 0, 0)")
     * @return The Color object
     */
    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.trim().isEmpty()) {
            return Color.BLACK; // Default to black
        }

        colorStr = colorStr.trim();

        // Check if it's a hex color
        if (HEX_COLOR_PATTERN.matcher(colorStr).matches()) {
            return Color.decode(colorStr);
        }

        // Check if it's an RGB color
        java.util.regex.Matcher rgbMatcher = RGB_COLOR_PATTERN.matcher(colorStr);
        if (rgbMatcher.matches()) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));

            // Clamp values between 0 and 255
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            return new Color(r, g, b);
        }

        // Default color for invalid input
        return colorStr.equalsIgnoreCase("white") ? Color.WHITE : Color.BLACK;
    }

    /**
     * Convert a Color object to hex string
     * @param color The color to convert
     * @return Hex string representation (e.g., "#FF0000")
     */
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Get the error correction level from string
     * @param level The error correction level string
     * @return ErrorCorrectionLevel enum value
     */
    private ErrorCorrectionLevel getErrorCorrectionLevel(String level) {
        if (level == null) return ErrorCorrectionLevel.M; // Default to medium

        return switch (level.toUpperCase()) {
            case "L" -> ErrorCorrectionLevel.L; // Low ~7% error correction
            case "Q" -> ErrorCorrectionLevel.Q; // Quartile ~25% error correction
            case "H" -> ErrorCorrectionLevel.H; // High ~30% error correction
            default -> ErrorCorrectionLevel.M; // Medium ~15% error correction
        };
    }

    /**
     * Safely parse an integer with default value
     *
     * @param value The string to parse
     * @return The parsed integer or default value
     */
    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 250;
        }
    }
}