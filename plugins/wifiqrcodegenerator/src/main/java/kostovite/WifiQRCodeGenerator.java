package kostovite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException; // Import IOException
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher; // Import Matcher
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

// Assuming PluginInterface is standard
public class WifiQRCodeGenerator implements PluginInterface {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    // Updated patterns to handle potential alpha in hex and RGB/RGBA formats
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*\\)$");
    private static final Pattern RGBA_COLOR_PATTERN = Pattern.compile("^rgba\\(\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*(\\d+%?)\\s*,\\s*([0-9]*\\.?[0-9]+)\\s*\\)$");

    // Define standard encryption types for WiFi QR format
    private static final Map<String, String> SUPPORTED_ENCRYPTION_TYPES = Map.of(
            "WPA", "WPA/WPA2/WPA3", // Consolidate common WPA types
            "WEP", "WEP (Legacy - Insecure)",
            "WPA2-EAP", "WPA2-Enterprise",
            "NONE", "No Security (Open Network)"
    );

    public WifiQRCodeGenerator() {
        // Create upload directory
        try {
            // Consider making configurable
            String uploadDir = "wifi-qrcodes";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created WiFi QR code directory: " + directory.getAbsolutePath());
                } else {
                    System.err.println("Failed to create WiFi QR code directory: " + directory.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating WiFi QR code directory: " + e.getMessage());
        }
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "WifiQRCodeGenerator";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("WiFi QR Code Generator Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("ssid", "MyHomeNetwork"); // Use new ID
            params.put("password", "verySecret!123"); // Use new ID
            params.put("encryptionType", "WPA"); // Use new ID
            params.put("isHidden", false); // Use new ID
            params.put("foregroundColor", "#004D40"); // Teal color
            params.put("backgroundColor", "#E0F2F1"); // Light teal background
            params.put("errorCorrection", "Q"); // Use new ID
            params.put("size", 400); // Use new ID
            params.put("fileName", "my_wifi_qr"); // Use new ID

            Map<String, Object> result = process(params);
            if(result.get("success") == Boolean.TRUE) {
                System.out.println("Sample WiFi QR code generated successfully. Check file: " + result.get("generatedFileName"));
            } else {
                System.err.println("Sample WiFi QR code generation failed: " + result.get("errorMessage"));
            }
        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "WifiQRCodeGenerator"); // ID matches class name
        metadata.put("name", "WiFi QR Code Generator"); // User-facing name
        metadata.put("description", "Easily share your WiFi network credentials by generating a QR code.");
        metadata.put("icon", "Wifi");
        metadata.put("category", "Network");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: WiFi Network Details ---
        Map<String, Object> networkSection = new HashMap<>();
        networkSection.put("id", "networkDetails");
        networkSection.put("label", "WiFi Network Details");

        List<Map<String, Object>> networkInputs = new ArrayList<>();
        networkInputs.add(Map.ofEntries(
                Map.entry("id", "ssid"),
                Map.entry("label", "Network Name (SSID):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "Your Network Name"),
                Map.entry("required", true),
                Map.entry("helperText", "The exact name of the WiFi network.")
        ));
        networkInputs.add(Map.ofEntries(
                Map.entry("id", "isHidden"),
                Map.entry("label", "Network is Hidden"),
                Map.entry("type", "switch"),
                Map.entry("default", false),
                Map.entry("helperText", "Check if the SSID is not broadcast.")
        ));
        List<Map<String, String>> encryptionOptions = new ArrayList<>();
        SUPPORTED_ENCRYPTION_TYPES.forEach((key, label) ->
                encryptionOptions.add(Map.of("value", key, "label", label))
        );
        networkInputs.add(Map.ofEntries(
                Map.entry("id", "encryptionType"),
                Map.entry("label", "Security Type:"),
                Map.entry("type", "select"),
                Map.entry("options", encryptionOptions),
                Map.entry("default", "WPA"),
                Map.entry("required", true), // Technically required for format
                Map.entry("helperText", "Select the network security (WPA recommended).")
        ));
        networkInputs.add(Map.ofEntries(
                Map.entry("id", "password"),
                Map.entry("label", "Password:"),
                Map.entry("type", "password"), // Use password type
                Map.entry("placeholder", "Network Password"),
                Map.entry("required", false), // Not required for 'NONE' type
                // Condition could hide/show based on encryption type, if frontend supports it
                Map.entry("condition", "encryptionType !== 'NONE'"),
                Map.entry("helperText", "Required for WEP/WPA/WPA2/WPA3 networks.")
        ));

        networkSection.put("inputs", networkInputs);
        sections.add(networkSection);

        // --- Section 2: QR Code Style & Settings ---
        Map<String, Object> styleSection = new HashMap<>();
        styleSection.put("id", "styleSettings");
        styleSection.put("label", "QR Code Style & Settings");

        List<Map<String, Object>> styleInputs = new ArrayList<>();

        styleInputs.add(Map.ofEntries(
                Map.entry("id", "size"),
                Map.entry("label", "Size (pixels):"),
                Map.entry("type", "slider"),
                Map.entry("min", 150), // Slightly increased min size for WiFi
                Map.entry("max", 1000),
                Map.entry("step", 50),
                Map.entry("default", 300)
        ));
        styleInputs.add(Map.ofEntries(
                Map.entry("id", "foregroundColor"),
                Map.entry("label", "Code Color:"),
                Map.entry("type", "color"),
                Map.entry("default", "#000000")
        ));
        styleInputs.add(Map.ofEntries(
                Map.entry("id", "backgroundColor"),
                Map.entry("label", "Background Color:"),
                Map.entry("type", "color"),
                Map.entry("default", "#FFFFFF")
        ));
        styleInputs.add(Map.ofEntries(
                Map.entry("id", "errorCorrection"),
                Map.entry("label", "Error Correction:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "L", "label", "Low (~7%)"),
                        Map.of("value", "M", "label", "Medium (~15%)"),
                        Map.of("value", "Q", "label", "Quartile (~25%)"),
                        Map.of("value", "H", "label", "High (~30%)")
                )),
                Map.entry("default", "M"), // Medium is often sufficient
                Map.entry("helperText", "Higher robustness against damage.")
        ));
        styleInputs.add(Map.ofEntries(
                Map.entry("id", "fileName"),
                Map.entry("label", "Filename (Optional):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "my_wifi_qr"),
                Map.entry("required", false),
                Map.entry("helperText", "Custom filename ('.png' added automatically).")
        ));

        styleSection.put("inputs", styleInputs);
        sections.add(styleSection);

        // --- Section 3: Generated QR Code ---
        Map<String, Object> outputSection = new HashMap<>();
        outputSection.put("id", "output");
        outputSection.put("label", "Generated WiFi QR Code");
        outputSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> outputs = new ArrayList<>();

        // QR Code Image Output
        Map<String, Object> qrImageOutput = createOutputField("qrImage", "", "image", null);
        qrImageOutput.put("buttons", List.of("download"));
        qrImageOutput.put("downloadFilenameKey", "generatedFileName"); // Key for download filename
        outputs.add(qrImageOutput);

        // Network Info Display
        outputs.add(createOutputField("networkInfo", "Network", "text", null));
        // Security Info Display
        outputs.add(createOutputField("securityInfo", "Security", "text", null));
        // Raw WiFi String Display (for debugging/info)
        outputs.add(createOutputField("wifiString", "Raw QR Data", "text", null));


        outputSection.put("outputs", outputs);
        sections.add(outputSection);

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

    // Helper to create output field definitions
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
        if ("text".equals(type) && (id.toLowerCase().contains("string") || id.toLowerCase().contains("info"))) {
            field.put("monospace", true);
        }
        if ("image".equals(type)){
            field.put("maxWidth", 350); // Default max width for QR code
            field.put("maxHeight", 350);
        }
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format)
     * to generate a WiFi QR code.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage";
        // No uiOperation needed as there's only one action: generate

        try {
            // Get parameters using NEW IDs
            String ssid = getStringParam(input, "ssid", null); // Required
            String password = getStringParam(input, "password", ""); // Optional, default empty
            String encryptionType = getStringParam(input, "encryptionType", "WPA").toUpperCase(); // Use new ID
            boolean isHidden = getBooleanParam(input); // Use new ID
            String foregroundColorStr = getStringParam(input, "foregroundColor", "#000000");
            String backgroundColorStr = getStringParam(input, "backgroundColor", "#FFFFFF");
            String errorCorrectionStr = getStringParam(input, "errorCorrection", "M"); // Default M
            int size = getIntParam(input); // Default 300
            String requestedFileName = getStringParam(input, "fileName", null); // Optional

            // --- Input Validation ---
            if (!SUPPORTED_ENCRYPTION_TYPES.containsKey(encryptionType)) {
                throw new IllegalArgumentException("Invalid Encryption Type selected: " + encryptionType);
            }
            // Require password if encryption is not NONE
            if (!"NONE".equals(encryptionType) && password.isEmpty()) {
                throw new IllegalArgumentException("Password is required for encryption type " + encryptionType);
            }
            if (size < 50 || size > 2000) throw new IllegalArgumentException("Size must be between 50 and 2000 pixels.");

            // Determine final filename
            // Sanitize
            String finalFileNameBase = !requestedFileName.isBlank()
                    ? requestedFileName.replaceAll("[^a-zA-Z0-9_.-]", "_") // Sanitize
                    : "wifi_" + ssid.replaceAll("[^a-zA-Z0-9_-]", "") + "_" + LocalDateTime.now().format(formatter); // Sanitize SSID for filename
            String finalFileName = finalFileNameBase.endsWith(".png") ? finalFileNameBase : finalFileNameBase + ".png";


            // --- Core QR Code Generation Logic ---
            String wifiString = generateWifiNetworkString(ssid, password, encryptionType, isHidden);

            Color foregroundColor = parseColor(foregroundColorStr);
            Color backgroundColor = parseColor(backgroundColorStr);
            ErrorCorrectionLevel errorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionStr);

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(wifiString, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage qrImage = renderQrCodeImage(bitMatrix, size, foregroundColor, backgroundColor);

            // --- Prepare Output ---
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(qrImage, "png", baos)) {
                throw new IOException("Failed to write QR code image to byte stream.");
            }
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            String dataUri = "data:image/png;base64," + base64Image;

            // Build result map matching NEW output IDs
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("qrImage", dataUri);             // Matches output ID "qrImage"
            result.put("networkInfo", ssid + (isHidden ? " (Hidden)" : "")); // Matches output ID
            result.put("securityInfo", SUPPORTED_ENCRYPTION_TYPES.getOrDefault(encryptionType, encryptionType)); // Matches output ID
            result.put("wifiString", wifiString);       // Matches output ID
            result.put("generatedFileName", finalFileName); // Matches output ID

            // Optional: Save image to file if backend storage is needed
            // saveImageToFile(qrImage, finalFileName);

            return result;

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (WriterException e) {
            System.err.println("Error generating QR code matrix: " + e.getMessage());
            return Map.of("success", false, errorOutputId, "Error encoding data into QR code. Data might be too complex or long.");
        } catch (Exception e) { // Catch other unexpected errors
            System.err.println("Error processing WiFi QR Code request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /** Generates the standard WiFi QR code string. */
    private String generateWifiNetworkString(String ssid, String password, String encryptionType, boolean isHidden) {
        // Input validation happens in process()
        StringBuilder sb = new StringBuilder("WIFI:");
        sb.append("S:").append(escapeWifiSpecialChars(ssid)).append(";");
        sb.append("T:").append(encryptionType).append(";"); // Already validated
        if (password != null && !password.isEmpty() && !"NONE".equals(encryptionType)) {
            sb.append("P:").append(escapeWifiSpecialChars(password)).append(";");
        }
        if (isHidden) {
            sb.append("H:true;");
        }
        sb.append(";"); // Final terminator
        return sb.toString();
    }

    /** Escapes characters reserved in the WiFi QR string format. */
    private String escapeWifiSpecialChars(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace("\"", "\\\"")
                .replace(":", "\\:")
                .replace(",", "\\,");
        // No need to escape $ % ` according to most specs
    }

    /** Renders the QR code BitMatrix to a BufferedImage. */
    private BufferedImage renderQrCodeImage(BitMatrix bitMatrix, int size, Color foreground, Color background) {
        BufferedImage qrImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = qrImage.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, size, size);
            graphics.setColor(foreground);
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
        return qrImage;
    }

    /** Parses color string (hex, rgb). */
    private Color parseColor(String colorStr) {
        // Reusing parseColor logic from previous refactors (ensure it handles hex/rgb)
        if (colorStr == null || colorStr.trim().isEmpty()) return Color.BLACK;
        String trimmed = colorStr.trim().toLowerCase();
        try {
            if (HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
                if (trimmed.length() == 4) {
                    char r=trimmed.charAt(1); char g=trimmed.charAt(2); char b=trimmed.charAt(3);
                    trimmed = "#" + r + r + g + g + b + b;
                }
                if (trimmed.length() > 7) trimmed = trimmed.substring(0, 7); // Ignore alpha
                return Color.decode(trimmed);
            }
            Matcher rgbMatcher = RGB_COLOR_PATTERN.matcher(trimmed);
            if (rgbMatcher.matches()) {
                int r = parseColorComponent(rgbMatcher.group(1));
                int g = parseColorComponent(rgbMatcher.group(2));
                int b = parseColorComponent(rgbMatcher.group(3));
                return new Color(r, g, b);
            }
            Matcher rgbaMatcher = RGBA_COLOR_PATTERN.matcher(trimmed); // Handle RGBA if needed
            if (rgbaMatcher.matches()) {
                int r = parseColorComponent(rgbaMatcher.group(1));
                int g = parseColorComponent(rgbaMatcher.group(2));
                int b = parseColorComponent(rgbaMatcher.group(3));
                // Alpha ignored for QR code background/foreground usually
                return new Color(r, g, b);
            }
        } catch (Exception e) { System.err.println("Color parse error: "+e.getMessage()); }
        return colorStr.contains("background") ? Color.WHITE : Color.BLACK; // Guess default
    }

    /** Parses color component (number or percentage). */
    private int parseColorComponent(String component) {
        component = component.trim(); int value;
        if (component.endsWith("%")) { value = (int) ((Double.parseDouble(component.substring(0, component.length() - 1)) / 100.0) * 255.0); }
        else { value = Integer.parseInt(component); }
        return Math.max(0, Math.min(255, value));
    }

    /** Gets ZXing ErrorCorrectionLevel. */
    private ErrorCorrectionLevel getErrorCorrectionLevel(String level) {
        if (level == null) return ErrorCorrectionLevel.M;
        return switch (level.toUpperCase()) {
            case "L" -> ErrorCorrectionLevel.L; case "Q" -> ErrorCorrectionLevel.Q; case "H" -> ErrorCorrectionLevel.H;
            default -> ErrorCorrectionLevel.M;
        };
    }

    // --- Parameter Parsing Helpers ---

    private int getIntParam(Map<String, Object> input) throws IllegalArgumentException {
        Object value = input.get("size");
        if (value == null || value.toString().trim().isEmpty()) {
            return 300;
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            if (Math.abs(dValue - Math.round(dValue)) < 0.00001) return (int) Math.round(dValue);
            else throw new IllegalArgumentException("Non-integer numeric value for integer parameter '" + "size" + "': " + value);
        }
        else { try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid integer value for parameter '" + "size" + "': " + value); }
        }
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString(); // Don't trim SSID/Password initially
        // Check if required and empty AFTER potential trim if logic needs it
        if (strValue.isEmpty() && defaultValue == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return strValue.isEmpty() ? defaultValue : strValue;
    }

    private boolean getBooleanParam(Map<String, Object> input) {
        Object value = input.get("isHidden");
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return "true".equalsIgnoreCase(value.toString());
        return false;
    }
}