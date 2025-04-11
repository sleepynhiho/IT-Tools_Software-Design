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

public class WifiQRCodeGenerator implements PluginInterface {

    private final String uploadDir = "wifi-qrcodes";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)$");

    public WifiQRCodeGenerator() {
        // Create upload directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public String getName() {
        return "WifiQRCodeGenerator";
    }

    @Override
    public void execute() {
        System.out.println("WiFi QR Code Generator Plugin executed");

        // Demonstrate basic usage
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("ssid", "MyWifiNetwork");
            params.put("password", "MySecurePassword123");
            params.put("encryptionType", "WPA");
            params.put("foregroundColor", "#000000");
            params.put("backgroundColor", "#FFFFFF");
            params.put("errorCorrection", "H");
            params.put("size", 300);

            Map<String, Object> result = process(params);
            System.out.println("Sample WiFi QR code generated: " + result.get("fileName"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generate WiFi QR codes with custom settings"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Generate operation
        Map<String, Object> generateOperation = new HashMap<>();
        generateOperation.put("description", "Generate a WiFi QR code image");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("ssid", Map.of("type", "string", "description", "WiFi network name", "required", true));
        inputs.put("password", Map.of("type", "string", "description", "WiFi password", "required", false));
        inputs.put("encryptionType", Map.of("type", "string", "description", "WiFi encryption type (NONE, WEP, WPA, WPA2-EAP, WPA2-PSK, WPA3)", "required", false));
        inputs.put("isHidden", Map.of("type", "boolean", "description", "Whether the network is hidden", "required", false));
        inputs.put("foregroundColor", Map.of("type", "string", "description", "Foreground color (hex code or RGB)", "required", false));
        inputs.put("backgroundColor", Map.of("type", "string", "description", "Background color (hex code or RGB)", "required", false));
        inputs.put("errorCorrection", Map.of("type", "string", "description", "Error correction level (L, M, Q, H)", "required", false));
        inputs.put("size", Map.of("type", "integer", "description", "Size of the QR code in pixels", "required", false));
        inputs.put("fileName", Map.of("type", "string", "description", "Optional filename (without extension)", "required", false));
        generateOperation.put("inputs", inputs);
        operations.put("generate", generateOperation);

        // Get supported encryption types operation
        Map<String, Object> getSupportedEncryptionOperation = new HashMap<>();
        getSupportedEncryptionOperation.put("description", "Get supported WiFi encryption types");
        operations.put("getSupportedEncryptionTypes", getSupportedEncryptionOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "WifiQRCodeGenerator"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Wifi"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Networking"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: WiFi Network Settings
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "WiFi Network Settings");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // SSID field
        Map<String, Object> ssidField = new HashMap<>();
        ssidField.put("name", "ssid");
        ssidField.put("label", "Network Name (SSID):");
        ssidField.put("type", "text");
        ssidField.put("placeholder", "Enter WiFi network name");
        ssidField.put("required", true);
        ssidField.put("helperText", "The name of your WiFi network");
        section1Fields.add(ssidField);

        // Password field
        Map<String, Object> passwordField = new HashMap<>();
        passwordField.put("name", "password");
        passwordField.put("label", "Password:");
        passwordField.put("type", "password");
        passwordField.put("placeholder", "Enter WiFi password");
        passwordField.put("required", false);
        passwordField.put("helperText", "Leave empty for open networks");
        section1Fields.add(passwordField);

        // Encryption type field
        Map<String, Object> encryptionField = new HashMap<>();
        encryptionField.put("name", "encryptionType");
        encryptionField.put("label", "Security Type:");
        encryptionField.put("type", "select");
        List<Map<String, String>> encryptionOptions = new ArrayList<>();
        encryptionOptions.add(Map.of("value", "WPA", "label", "WPA/WPA2 (Most Common)"));
        encryptionOptions.add(Map.of("value", "WPA2-PSK", "label", "WPA2-PSK"));
        encryptionOptions.add(Map.of("value", "WPA3", "label", "WPA3"));
        encryptionOptions.add(Map.of("value", "WEP", "label", "WEP (Legacy)"));
        encryptionOptions.add(Map.of("value", "WPA2-EAP", "label", "WPA2-Enterprise"));
        encryptionOptions.add(Map.of("value", "NONE", "label", "No Security (Open Network)"));
        encryptionField.put("options", encryptionOptions);
        encryptionField.put("default", "WPA");
        encryptionField.put("helperText", "Security/encryption type of the WiFi network");
        section1Fields.add(encryptionField);

        // Hidden network field
        Map<String, Object> hiddenField = new HashMap<>();
        hiddenField.put("name", "isHidden");
        hiddenField.put("label", "Hidden Network");
        hiddenField.put("type", "switch");
        hiddenField.put("default", false);
        hiddenField.put("helperText", "Enable if your network doesn't broadcast its name");
        section1Fields.add(hiddenField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: QR Code Appearance
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "QR Code Appearance");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Size field
        Map<String, Object> sizeField = new HashMap<>();
        sizeField.put("name", "size");
        sizeField.put("label", "Size (pixels):");
        sizeField.put("type", "slider");
        sizeField.put("min", 200);
        sizeField.put("max", 1000);
        sizeField.put("step", 50);
        sizeField.put("default", 300);
        sizeField.put("helperText", "Size of the generated QR code image");
        section2Fields.add(sizeField);

        // Foreground color field
        Map<String, Object> fgColorField = new HashMap<>();
        fgColorField.put("name", "foregroundColor");
        fgColorField.put("label", "QR Code Color:");
        fgColorField.put("type", "color");
        fgColorField.put("default", "#000000");
        fgColorField.put("helperText", "Color of the QR code pattern");
        section2Fields.add(fgColorField);

        // Background color field
        Map<String, Object> bgColorField = new HashMap<>();
        bgColorField.put("name", "backgroundColor");
        bgColorField.put("label", "Background Color:");
        bgColorField.put("type", "color");
        bgColorField.put("default", "#FFFFFF");
        bgColorField.put("helperText", "Color of the QR code background");
        section2Fields.add(bgColorField);

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
        errorCorrectionField.put("default", "H");
        errorCorrectionField.put("helperText", "Higher levels allow QR code to be readable even if partially damaged");
        section2Fields.add(errorCorrectionField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: File Settings
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "File Settings");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Filename field
        Map<String, Object> fileNameField = new HashMap<>();
        fileNameField.put("name", "fileName");
        fileNameField.put("label", "File Name:");
        fileNameField.put("type", "text");
        fileNameField.put("placeholder", "wifi_qrcode");
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
        outputSection1.put("header", "Generated WiFi QR Code");
        outputSection1.put("condition", "success");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // WiFi QR Code Image
        Map<String, Object> qrImageOutput = new HashMap<>();
        qrImageOutput.put("title", "");
        qrImageOutput.put("name", "imageBase64");
        qrImageOutput.put("type", "image");
        qrImageOutput.put("maxWidth", 300);
        qrImageOutput.put("buttons", List.of("download"));
        section1OutputFields.add(qrImageOutput);

        // WiFi Network Info
        Map<String, Object> wifiInfoOutput = new HashMap<>();
        wifiInfoOutput.put("title", "Network Information");
        wifiInfoOutput.put("name", "networkInfo");
        wifiInfoOutput.put("type", "text");
        wifiInfoOutput.put("formula", "ssid + (isHidden ? ' (Hidden)' : '')");
        wifiInfoOutput.put("variant", "bold");
        section1OutputFields.add(wifiInfoOutput);

        // Security info
        Map<String, Object> securityInfoOutput = new HashMap<>();
        securityInfoOutput.put("title", "Security");
        securityInfoOutput.put("name", "securityInfo");
        securityInfoOutput.put("type", "chips");
        securityInfoOutput.put("items", "[encryptionType === 'NONE' ? 'Open Network' : encryptionType]");
        securityInfoOutput.put("colors", "[encryptionType === 'NONE' ? 'warning' : 'primary']");
        section1OutputFields.add(securityInfoOutput);

        // Usage instructions
        Map<String, Object> usageOutput = new HashMap<>();
        usageOutput.put("title", "How to Use");
        usageOutput.put("name", "usageInstructions");
        usageOutput.put("type", "text");
        usageOutput.put("value", "1. Open your phone's camera\n2. Point it at the QR code\n3. Tap the notification to connect");
        usageOutput.put("multiline", true);
        section1OutputFields.add(usageOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: QR Code Details
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Technical Details");
        outputSection2.put("condition", "success");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // QR Code format
        Map<String, Object> formatOutput = new HashMap<>();
        formatOutput.put("title", "QR Format");
        formatOutput.put("name", "wifiString");
        formatOutput.put("type", "text");
        formatOutput.put("multiline", true);
        formatOutput.put("monospace", true);
        formatOutput.put("maxLength", 100);
        section2OutputFields.add(formatOutput);

        // File info
        Map<String, Object> fileInfoOutput = new HashMap<>();
        fileInfoOutput.put("title", "File Info");
        fileInfoOutput.put("name", "fileInfoDisplay");
        fileInfoOutput.put("type", "text");
        fileInfoOutput.put("formula", "fileName + ' (' + (fileSize / 1024).toFixed(1) + ' KB)'");
        section2OutputFields.add(fileInfoOutput);

        // QR Settings
        Map<String, Object> settingsOutput = new HashMap<>();
        settingsOutput.put("title", "QR Settings");
        settingsOutput.put("name", "qrSettings");
        settingsOutput.put("type", "text");
        settingsOutput.put("formula", "size + 'x' + size + ' pixels, ' + errorCorrection + ' error correction'");
        section2OutputFields.add(settingsOutput);

        // Colors used
        Map<String, Object> colorsOutput = new HashMap<>();
        colorsOutput.put("title", "Colors");
        colorsOutput.put("name", "colorsDisplay");
        colorsOutput.put("type", "chips");
        colorsOutput.put("items", "['Foreground: ' + foregroundColor, 'Background: ' + backgroundColor]");
        colorsOutput.put("colors", "[foregroundColor, backgroundColor]");
        section2OutputFields.add(colorsOutput);

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
            String operation = (String) input.getOrDefault("operation", "generate");

            return switch (operation.toLowerCase()) {
                case "generate" -> generateWifiQRCode(input);
                case "getsupportedencryptiontypes" -> getSupportedEncryptionTypes();
                default -> {
                    result.put("error", "Unsupported operation: " + operation);
                    yield result;
                }
            };
        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> generateWifiQRCode(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get parameters
            String ssid = (String) input.get("ssid");
            String password = (String) input.get("password");
            String encryptionType = ((String) input.getOrDefault("encryptionType", "WPA")).toUpperCase();
            boolean isHidden = Boolean.parseBoolean(String.valueOf(input.getOrDefault("isHidden", false)));
            String foregroundColorStr = (String) input.getOrDefault("foregroundColor", "#000000");
            String backgroundColorStr = (String) input.getOrDefault("backgroundColor", "#FFFFFF");
            String errorCorrectionStr = (String) input.getOrDefault("errorCorrection", "M");
            int size = input.containsKey("size") ? parseIntSafe(input.get("size").toString()) : 300;
            String fileName = (String) input.getOrDefault("fileName",
                    "wifi_" + LocalDateTime.now().format(formatter));

            // Validate inputs
            if (ssid == null || ssid.trim().isEmpty()) {
                result.put("error", "SSID cannot be empty");
                return result;
            }

            // Validate encryption type
            Map<String, String> supportedTypes = getSupportedEncryptionTypesMap();
            if (!supportedTypes.containsKey(encryptionType)) {
                result.put("error", "Unsupported encryption type: " + encryptionType +
                        ". Supported types are: " + String.join(", ", supportedTypes.keySet()));
                return result;
            }

            // Generate WiFi Network String
            String wifiString = generateWifiNetworkString(ssid, password, encryptionType, isHidden);

            // Parse colors
            Color foregroundColor = parseColor(foregroundColorStr);
            Color backgroundColor = parseColor(backgroundColorStr);

            // Parse error correction level
            ErrorCorrectionLevel errorCorrectionLevel = getErrorCorrectionLevel(errorCorrectionStr);

            // Set QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.MARGIN, 2);

            // Create QR code writer and encode the WiFi string
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(wifiString, BarcodeFormat.QR_CODE, size, size, hints);

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
            result.put("ssid", ssid);
            result.put("encryptionType", encryptionType);
            result.put("isHidden", isHidden);
            result.put("fileName", fileNameWithExtension);
            result.put("filePath", filePath);
            result.put("fileSize", outputFile.length());
            result.put("foregroundColor", colorToHex(foregroundColor));
            result.put("backgroundColor", colorToHex(backgroundColor));
            result.put("errorCorrection", errorCorrectionLevel.toString());
            result.put("size", size);
            result.put("wifiString", wifiString);
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
     * Generate WiFi network string in the correct format for QR codes
     * Format: WIFI:S:<SSID>;T:<ENCRYPTION>;P:<PASSWORD>;H:<HIDDEN>;
     *
     * @param ssid Network SSID
     * @param password Network password
     * @param encryptionType Encryption type (WEP, WPA, etc.)
     * @param isHidden Whether the network is hidden
     * @return Formatted WiFi network string
     */
    private String generateWifiNetworkString(String ssid, String password, String encryptionType, boolean isHidden) {
        StringBuilder sb = new StringBuilder();
        sb.append("WIFI:");

        // Add SSID - make sure to escape special characters
        sb.append("S:").append(escapeSpecialChars(ssid)).append(";");

        // Add encryption type
        sb.append("T:").append(encryptionType).append(";");

        // Add password - make sure to escape special characters
        if (password != null && !password.isEmpty()) {
            sb.append("P:").append(escapeSpecialChars(password)).append(";");
        }

        // Add hidden flag
        if (isHidden) {
            sb.append("H:true;");
        }

        // End with semicolon
        sb.append(";");

        return sb.toString();
    }

    /**
     * Escape special characters in WiFi strings
     * @param input String to escape
     * @return Escaped string
     */
    private String escapeSpecialChars(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:");
    }

    /**
     * Get supported WiFi encryption types
     * @return Map of encryption types with descriptions
     */
    private Map<String, Object> getSupportedEncryptionTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("encryptionTypes", getSupportedEncryptionTypesMap());
        return result;
    }

    /**
     * Get map of supported encryption types
     * @return Map with encryption type as key and description as value
     */
    private Map<String, String> getSupportedEncryptionTypesMap() {
        Map<String, String> types = new HashMap<>();
        types.put("WEP", "Wired Equivalent Privacy (WEP)");
        types.put("WPA", "Wi-Fi Protected Access (WPA)");
        types.put("WPA2-PSK", "Wi-Fi Protected Access 2 - Pre-Shared Key (WPA2-PSK)");
        types.put("WPA2-EAP", "Wi-Fi Protected Access 2 - Enterprise (WPA2-EAP)");
        types.put("WPA3", "Wi-Fi Protected Access 3 (WPA3)");
        types.put("NONE", "No encryption (Open network)");
        return types;
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
            return 300;
        }
    }
}