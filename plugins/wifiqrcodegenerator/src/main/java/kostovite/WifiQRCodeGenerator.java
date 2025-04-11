package kostovite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class WifiQRCodeGenerator implements PluginInterface {

    private String uploadDir = "wifi-qrcodes";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generate WiFi QR codes with custom settings");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Generate operation
        Map<String, Object> generateOperation = new HashMap<>();
        generateOperation.put("description", "Generate a WiFi QR code image");

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("ssid", "WiFi network name");
        inputs.put("password", "WiFi password");
        inputs.put("encryptionType", "WiFi encryption type (NONE, WEP, WPA, WPA2-EAP, WPA2-PSK, WPA3)");
        inputs.put("isHidden", "Whether the network is hidden (true/false)");
        inputs.put("foregroundColor", "Foreground color (hex code or RGB)");
        inputs.put("backgroundColor", "Background color (hex code or RGB)");
        inputs.put("errorCorrection", "Error correction level (L, M, Q, H)");
        inputs.put("size", "Size of the QR code in pixels");
        inputs.put("fileName", "Optional filename (without extension)");

        generateOperation.put("inputs", inputs);
        operations.put("generate", generateOperation);

        // Get supported encryption types operation
        Map<String, Object> getSupportedEncryptionOperation = new HashMap<>();
        getSupportedEncryptionOperation.put("description", "Get supported WiFi encryption types");
        operations.put("getSupportedEncryptionTypes", getSupportedEncryptionOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "generate");

            switch (operation.toLowerCase()) {
                case "generate":
                    return generateWifiQRCode(input);
                case "getsupportedencryptiontypes":
                    return getSupportedEncryptionTypes();
                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }
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
            int size = input.containsKey("size") ? parseIntSafe(input.get("size").toString(), 300) : 300;
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

        switch (level.toUpperCase()) {
            case "L": return ErrorCorrectionLevel.L; // Low ~7% error correction
            case "Q": return ErrorCorrectionLevel.Q; // Quartile ~25% error correction
            case "H": return ErrorCorrectionLevel.H; // High ~30% error correction
            case "M":
            default: return ErrorCorrectionLevel.M; // Medium ~15% error correction
        }
    }

    /**
     * Safely parse an integer with default value
     * @param value The string to parse
     * @param defaultValue The default value if parsing fails
     * @return The parsed integer or default value
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}