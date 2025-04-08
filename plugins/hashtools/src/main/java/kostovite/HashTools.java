package kostovite;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HashTools implements ExtendedPluginInterface {
    @Override
    public String getName() {
        return "HashTools";
    }

    @Override
    public void execute() {
        String input = "Hello, World!";
        String hash = calculateSHA256(input);
        System.out.println("Hash of '" + input + "': " + hash);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "HashTools");
        metadata.put("version", "1.0.0");
        metadata.put("description", "Tools for calculating various hashes");

        Map<String, Object> operations = new HashMap<>();
        operations.put("sha256", Map.of(
                "description", "Calculate SHA-256 hash",
                "inputs", Map.of("text", "String to hash")
        ));
        operations.put("md5", Map.of(
                "description", "Calculate MD5 hash",
                "inputs", Map.of("text", "String to hash")
        ));

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        // Extract operation and data
        String operation = (String) input.getOrDefault("operation", "sha256");
        String text = (String) input.getOrDefault("text", "");

        try {
            switch (operation.toLowerCase()) {
                case "sha256":
                    result.put("hash", calculateSHA256(text));
                    result.put("algorithm", "SHA-256");
                    break;
                case "md5":
                    result.put("hash", calculateMD5(text));
                    result.put("algorithm", "MD5");
                    break;
                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

            result.put("input", text);
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating SHA-256 hash: " + e.getMessage(), e);
        }
    }

    public String calculateMD5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] encodedHash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating MD5 hash: " + e.getMessage(), e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}