package kostovite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher; // Import explicitly
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

// Assuming PluginInterface is standard
public class QRCodeGenerator implements PluginInterface {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"); // Allow alpha hex
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*\\)$");
    private static final Pattern RGBA_COLOR_PATTERN = Pattern.compile("^rgba\\(\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*([0-9]*\\.?[0-9]+)\\s*\\)$");

    public QRCodeGenerator() {
        // Create upload directory if it doesn't exist - Consider error handling
        try {
            // Consider making this configurable or relative to a base path
            String uploadDir = "qrcodes";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created QR code directory: " + directory.getAbsolutePath());
                } else {
                    System.err.println("Failed to create QR code directory: " + directory.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating QR code directory: " + e.getMessage());
        }
    }

    /**
     * Internal name for the plugin backend.
     * @return The backend name.
     */
    @Override
    public String getName() {
        // This name should match the class name for endpoint routing based on previous examples
        return "QRCodeGenerator";
    }

    /**
     * Executes a standalone test run of the plugin.
     */
    @Override
    public void execute() {
        System.out.println("QRCode Generator Plugin executed (standalone test)");
        // Use IDs matching the new metadata format
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("text", "https://react.dev");
            params.put("foregroundColor", "#1E88E5"); // Blue
            params.put("backgroundColor", "#FFFFFF");
            params.put("errorCorrection", "M");
            params.put("size", 300);
            params.put("fileName", "react_qr_test");

            Map<String, Object> result = process(params);
            if(result.get("success") == Boolean.TRUE) {
                System.out.println("Sample QR code generated successfully. Check file: " + result.get("fileName"));
                // System.out.println("Base64 Data URI included in result."); // Optional log
            } else {
                System.err.println("Sample QR code generation failed: " + result.get("error"));
            }
        } catch (Exception e) {
            System.err.println("Standalone test execution error: ");
            e.printStackTrace();
        }
    }

    /**
     * Generates the metadata describing the plugin's UI and capabilities
     * in the NEW specified format (sections, id, etc.).
     *
     * @return A map representing the plugin metadata JSON.
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "QRCodeGenerator"); // ID matching the plugin name/endpoint
        metadata.put("name", "QR Code Generator"); // User-facing name
        metadata.put("description", "Generate QR codes with custom text, colors, size, and error correction.");
        metadata.put("icon", "QrCode2"); // Material Icon name
        metadata.put("category", "Utilities");
        // Assuming endpoint is derived by the framework, or add it if needed:
        // metadata.put("endpoint", "/api/debug/QRCodeGenerator/process");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Content Section ---
        Map<String, Object> contentSection = new HashMap<>();
        contentSection.put("id", "content");
        contentSection.put("label", "QR Code Content");
        List<Map<String, Object>> contentInputs = new ArrayList<>();
        contentInputs.add(Map.of(
                "id", "text", // Use ID
                "label", "Content to Encode:",
                "type", "text",
                "multiline", true,
                "rows", 4,
                "placeholder", "Enter text, URL, contact info, Wi-Fi details, etc.",
                "required", true,
                "helperText", "The data that will be embedded in the QR code.",
                "containerId", "main" // Example containerId
        ));
        contentSection.put("inputs", contentInputs);
        sections.add(contentSection);

        // --- Appearance Section ---
        Map<String, Object> appearanceSection = new HashMap<>();
        appearanceSection.put("id", "appearance");
        appearanceSection.put("label", "Appearance");
        List<Map<String, Object>> appearanceInputs = new ArrayList<>();
        appearanceInputs.add(Map.of(
                "id", "size", // Use ID
                "label", "Size (pixels):",
                "type", "slider",
                "min", 100,
                "max", 1000,
                "step", 50,
                "default", 250,
                "containerId", "main"
        ));
        appearanceInputs.add(Map.of(
                "id", "foregroundColor", // Use ID
                "label", "Foreground Color:",
                "type", "color", // Assuming frontend handles 'color' type input
                "default", "#000000",
                "containerId", "main"
        ));
        appearanceInputs.add(Map.of(
                "id", "backgroundColor", // Use ID
                "label", "Background Color:",
                "type", "color",
                "default", "#FFFFFF",
                "containerId", "main"
        ));
        appearanceSection.put("inputs", appearanceInputs);
        sections.add(appearanceSection);

        // --- Settings Section ---
        Map<String, Object> settingsSection = new HashMap<>();
        settingsSection.put("id", "settings");
        settingsSection.put("label", "Settings");
        List<Map<String, Object>> settingsInputs = new ArrayList<>();
        settingsInputs.add(Map.of(
                "id", "errorCorrection", // Use ID
                "label", "Error Correction Level:",
                "type", "select", // Assuming frontend handles 'select' type
                "options", List.of( // Options for the select dropdown
                        Map.of("value", "L", "label", "Low (~7% correction)"),
                        Map.of("value", "M", "label", "Medium (~15% correction)"),
                        Map.of("value", "Q", "label", "Quartile (~25% correction)"),
                        Map.of("value", "H", "label", "High (~30% correction)")
                ),
                "default", "M",
                "helperText", "Higher levels allow the QR code to be read even if damaged.",
                "containerId", "main"
        ));
        settingsInputs.add(Map.of(
                "id", "fileName", // Use ID
                "label", "Filename (Optional):",
                "type", "text",
                "placeholder", "my-qrcode",
                "helperText", "Custom filename ('.png' will be added).",
                "required", false, // Optional filename
                "containerId", "main"
        ));
        settingsSection.put("inputs", settingsInputs);
        sections.add(settingsSection);


        // --- Define Outputs Section ---
        Map<String, Object> outputSection = new HashMap<>();
        outputSection.put("id", "output");
        outputSection.put("label", "Generated QR Code");
        // No top-level section condition needed if always shown on success
        List<Map<String, Object>> outputs = new ArrayList<>();

        // QR Code Image Output
        Map<String, Object> qrImageOutput = new HashMap<>();
        qrImageOutput.put("id", "qrImage"); // ID for the output image data
        qrImageOutput.put("label", ""); // Label might be handled by layout, or set to "QR Code"
        qrImageOutput.put("type", "image"); // Special type for image rendering
        // Add properties the frontend image renderer might use
        qrImageOutput.put("width", 300); // Suggest max width for display
        qrImageOutput.put("height", 300); // Suggest max height for display
        qrImageOutput.put("buttons", List.of("download")); // Button relevant to image
        // Add button placement if needed by frontend renderer
        // qrImageOutput.put("buttonPlacement", Map.of("download", "outside"));
        qrImageOutput.put("containerId", "main");
        outputs.add(qrImageOutput);

        // Echo Input Text (Optional)
        Map<String, Object> inputTextEcho = new HashMap<>();
        inputTextEcho.put("id", "inputText"); // ID for the echoed text
        inputTextEcho.put("label", "Encoded Text:");
        inputTextEcho.put("type", "text");
        inputTextEcho.put("monospace", true); // Good for URLs/data
        inputTextEcho.put("multiline", true);
        inputTextEcho.put("rows", 2);
        inputTextEcho.put("buttons", List.of("copy"));
        // inputTextEcho.put("buttonPlacement", Map.of("copy", "outside"));
        inputTextEcho.put("containerId", "main");
        outputs.add(inputTextEcho);

        // File Name Output (Optional, useful if generated)
        Map<String, Object> fileNameOutput = new HashMap<>();
        fileNameOutput.put("id", "generatedFileName"); // ID for the echoed text
        fileNameOutput.put("label", "Generated Filename:");
        fileNameOutput.put("type", "text");
        fileNameOutput.put("monospace", true);
        fileNameOutput.put("containerId", "main");
        outputs.add(fileNameOutput);


        outputSection.put("outputs", outputs);
        sections.add(outputSection);

        // --- Error Section (Standard) ---
        // Although not explicitly in the example, it's good practice for error display
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "error");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Assuming frontend understands this convention
        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.of(
                "id", "errorMessage",
                "label", "Error Details:",
                "type", "text",
                "style", "error" // Optional style hint
                // No containerId needed for a simple error message usually
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        // Add sections list to the main metadata map
        metadata.put("sections", sections);

        // Remove old structures if they existed

        return metadata;
    }

    /**
     * Processes the input parameters (using IDs) to generate a QR code.
     *
     * @param input A map containing input parameters based on metadata IDs.
     * @return A map containing the result ('success', 'qrImage', 'inputText', 'generatedFileName', or 'error', 'errorMessage').
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        // No operation check needed if only one action ("generate")

        try {
            // Get parameters using NEW IDs and helper methods
            String text = getStringParam(input, "text", null); // Required
            String foregroundColorStr = getStringParam(input, "foregroundColor", "#000000");
            String backgroundColorStr = getStringParam(input, "backgroundColor", "#FFFFFF");
            String errorCorrectionStr = getStringParam(input, "errorCorrection", "M");
            int size = getIntParam(input);
            String requestedFileName = getStringParam(input, "fileName", null); // Optional

            // --- Input Validation ---
            if (size < 50 || size > 2000) { // Example size validation
                throw new IllegalArgumentException("Size must be between 50 and 2000 pixels.");
            }

            // Determine final filename
            // Sanitize
            String finalFileNameBase = !requestedFileName.isBlank()
                    ? requestedFileName.replaceAll("[^a-zA-Z0-9_.-]", "_") // Sanitize
                    : "qrcode_" + LocalDateTime.now().format(formatter);
            String finalFileName = finalFileNameBase.endsWith(".png") ? finalFileNameBase : finalFileNameBase + ".png";


            // --- Core QR Code Generation Logic ---
            Color foregroundColor = parseColor(foregroundColorStr);
            Color backgroundColor = parseColor(backgroundColorStr);
            ErrorCorrectionLevel errorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionStr);

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class); // Use EnumMap
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.MARGIN, 2); // Standard margin
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()); // Ensure UTF-8

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage qrImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = qrImage.createGraphics();
            try { // Use try-with-resources for Graphics2D if possible, or ensure disposal
                graphics.setColor(backgroundColor);
                graphics.fillRect(0, 0, size, size);
                graphics.setColor(foregroundColor);
                for (int x = 0; x < size; x++) {
                    for (int y = 0; y < size; y++) {
                        if (bitMatrix.get(x, y)) {
                            graphics.fillRect(x, y, 1, 1);
                        }
                    }
                }
            } finally {
                graphics.dispose();
            }

            // --- Prepare Output ---

            // Convert image to Base64 Data URI for inline display
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(qrImage, "png", baos)) {
                throw new IOException("Failed to write QR code image to byte stream.");
            }
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            String dataUri = "data:image/png;base64," + base64Image;

            // --- Build Result Map (matching NEW metadata output IDs) ---
            result.put("success", true);
            result.put("qrImage", dataUri);             // Matches output ID "qrImage"
            result.put("inputText", text);              // Matches output ID "inputText"
            result.put("generatedFileName", finalFileName); // Matches output ID "generatedFileName"
            // Include the filename also for the download button functionality if needed frontend side
            result.put("fileNameForDownload", finalFileName); // Separate key if cleaner

            // Save image to file (optional, if backend needs to store it)
            // Consider if this saving logic is still needed with Base64 output


        } catch (IllegalArgumentException e) { // Catch validation errors
            result.put("success", false);
            result.put("errorMessage", e.getMessage()); // Match error output ID
        } catch (WriterException e) {
            System.err.println("Error generating QR code matrix: " + e.getMessage());
            result.put("success", false);
            result.put("errorMessage", "Error generating QR code. Input might be too long for the chosen size/error correction.");
        } catch (Exception e) { // Catch other unexpected errors
            System.err.println("Error processing QR code request: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("errorMessage", "An unexpected server error occurred: " + e.getClass().getSimpleName());
        }

        return result;
    }

    // ========================================================================
    // Helper Methods (Parsing, Formatting) - Moved to bottom for readability
    // ========================================================================

    /**
     * Parses a color string (hex, rgb, rgba). Defaults to black on failure.
     */
    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.trim().isEmpty()) return Color.BLACK;
        String trimmed = colorStr.trim().toLowerCase();

        try {
            // Hex
            Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(trimmed);
            if (hexMatcher.matches()) {
                // Handle #RGB, #RRGGBB, #RRGGBBAA format needed? Java Color doesn't directly decode alpha hex easily.
                // Color.decode handles #RRGGBB and #RGB (by repeating digits)
                if (trimmed.length() == 4) { // #RGB -> #RRGGBB
                    char r = trimmed.charAt(1);
                    char g = trimmed.charAt(2);
                    char b = trimmed.charAt(3);
                    trimmed = "#" + r + r + g + g + b + b;
                }
                // Ignore alpha for now if present (#RRGGBBAA)
                if (trimmed.length() > 7) trimmed = trimmed.substring(0, 7);

                return Color.decode(trimmed);
            }

            // RGBA
            Matcher rgbaMatcher = RGBA_COLOR_PATTERN.matcher(trimmed);
            if (rgbaMatcher.matches()) {
                int r = parseColorComponent(rgbaMatcher.group(1));
                int g = parseColorComponent(rgbaMatcher.group(2));
                int b = parseColorComponent(rgbaMatcher.group(3));
                int a = (int) (Math.max(0, Math.min(1.0, Double.parseDouble(rgbaMatcher.group(4)))) * 255);
                return new Color(r, g, b, a);
            }

            // RGB
            Matcher rgbMatcher = RGB_COLOR_PATTERN.matcher(trimmed);
            if (rgbMatcher.matches()) {
                int r = parseColorComponent(rgbMatcher.group(1));
                int g = parseColorComponent(rgbMatcher.group(2));
                int b = parseColorComponent(rgbMatcher.group(3));
                return new Color(r, g, b);
            }

            // Try named colors? (Optional)
            // Field field = Color.class.getField(trimmed); return (Color)field.get(null);

        } catch (NumberFormatException | SecurityException e) {
            System.err.println("Failed to parse color '" + colorStr + "': " + e.getMessage());
        }
        System.err.println("Could not parse color '" + colorStr + "', defaulting.");
        // Return default based on common request (white for background, black for foreground)
        return colorStr.contains("background") ? Color.WHITE : Color.BLACK; // Basic guess
    }

    /** Helper to parse R, G, B components (handles plain numbers and percentages) */
    private int parseColorComponent(String component) {
        component = component.trim();
        int value;
        if (component.endsWith("%")) {
            double percent = Double.parseDouble(component.substring(0, component.length() - 1));
            value = (int) ((percent / 100.0) * 255.0);
        } else {
            value = Integer.parseInt(component);
        }
        return Math.max(0, Math.min(255, value)); // Clamp 0-255
    }


    /** Converts Color to #RRGGBB hex string. */
    private String colorToHex(Color color) {
        if (color == null) return "#000000";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** Gets ZXing ErrorCorrectionLevel from string. */
    private ErrorCorrectionLevel getErrorCorrectionLevel(String level) {
        if (level == null) return ErrorCorrectionLevel.M;
        return switch (level.toUpperCase()) {
            case "L" -> ErrorCorrectionLevel.L;
            case "Q" -> ErrorCorrectionLevel.Q;
            case "H" -> ErrorCorrectionLevel.H;
            default -> ErrorCorrectionLevel.M; // Default to Medium
        };
    }

    /** Safely parses integer from object, returns default if invalid/null. */
    private int getIntParam(Map<String, Object> input) {
        Object value = input.get("size");
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) { /* Fall through to default */ }
        }
        return 250;
    }

    /** Safely gets string from object, returns default if null/empty. Null default means required. */
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString(); // Don't trim here, let logic decide if needed
        if (strValue.isEmpty()) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue; // Return default if empty and default allowed
        }
        return strValue;
    }
}