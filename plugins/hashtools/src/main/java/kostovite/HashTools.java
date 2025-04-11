package kostovite;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Tools for calculating various hashes"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // SHA-256 operation
        Map<String, Object> sha256Operation = new HashMap<>();
        sha256Operation.put("description", "Calculate SHA-256 hash");
        Map<String, Object> sha256Inputs = new HashMap<>();
        sha256Inputs.put("text", Map.of("type", "string", "description", "String to hash", "required", true));
        sha256Operation.put("inputs", sha256Inputs);
        operations.put("sha256", sha256Operation);

        // MD5 operation
        Map<String, Object> md5Operation = new HashMap<>();
        md5Operation.put("description", "Calculate MD5 hash");
        Map<String, Object> md5Inputs = new HashMap<>();
        md5Inputs.put("text", Map.of("type", "string", "description", "String to hash", "required", true));
        md5Operation.put("inputs", md5Inputs);
        operations.put("md5", md5Operation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "HashTools"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Fingerprint"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Crypto"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Hash Configuration
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Hash Configuration");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Hash algorithm selection field
        Map<String, Object> algorithmField = new HashMap<>();
        algorithmField.put("name", "operation");
        algorithmField.put("label", "Hash Algorithm:");
        algorithmField.put("type", "select");
        List<Map<String, String>> algorithmOptions = new ArrayList<>();
        algorithmOptions.add(Map.of("value", "sha256", "label", "SHA-256 (Secure Hash Algorithm 256-bit)"));
        algorithmOptions.add(Map.of("value", "md5", "label", "MD5 (Message Digest 5)"));
        algorithmField.put("options", algorithmOptions);
        algorithmField.put("default", "sha256");
        algorithmField.put("required", true);
        section1Fields.add(algorithmField);

        // Input text field
        Map<String, Object> textField = new HashMap<>();
        textField.put("name", "text");
        textField.put("label", "Text to Hash:");
        textField.put("type", "text");
        textField.put("multiline", true);
        textField.put("rows", 5);
        textField.put("placeholder", "Enter text to hash...");
        textField.put("required", true);
        section1Fields.add(textField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Advanced Options (for future expansion)
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Advanced Options");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Encoding option field
        Map<String, Object> encodingField = new HashMap<>();
        encodingField.put("name", "encoding");
        encodingField.put("label", "Input Encoding:");
        encodingField.put("type", "select");
        List<Map<String, String>> encodingOptions = new ArrayList<>();
        encodingOptions.add(Map.of("value", "utf8", "label", "UTF-8"));
        encodingOptions.add(Map.of("value", "ascii", "label", "ASCII"));
        encodingField.put("options", encodingOptions);
        encodingField.put("default", "utf8");
        encodingField.put("required", false);
        section2Fields.add(encodingField);

        // Output format field
        Map<String, Object> outputFormatField = new HashMap<>();
        outputFormatField.put("name", "outputFormat");
        outputFormatField.put("label", "Output Format:");
        outputFormatField.put("type", "select");
        List<Map<String, String>> formatOptions = new ArrayList<>();
        formatOptions.add(Map.of("value", "hex", "label", "Hexadecimal (lowercase)"));
        formatOptions.add(Map.of("value", "hexUpper", "label", "Hexadecimal (UPPERCASE)"));
        formatOptions.add(Map.of("value", "base64", "label", "Base64"));
        outputFormatField.put("options", formatOptions);
        outputFormatField.put("default", "hex");
        outputFormatField.put("required", false);
        section2Fields.add(outputFormatField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Hash Result
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Hash Result");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Input summary
        Map<String, Object> inputSummaryOutput = new HashMap<>();
        inputSummaryOutput.put("title", "Input");
        inputSummaryOutput.put("name", "input");
        inputSummaryOutput.put("type", "text");
        section1OutputFields.add(inputSummaryOutput);

        // Algorithm used
        Map<String, Object> algorithmOutput = new HashMap<>();
        algorithmOutput.put("title", "Algorithm");
        algorithmOutput.put("name", "algorithm");
        algorithmOutput.put("type", "text");
        section1OutputFields.add(algorithmOutput);

        // Hash result
        Map<String, Object> hashOutput = new HashMap<>();
        hashOutput.put("title", "Hash");
        hashOutput.put("name", "hash");
        hashOutput.put("type", "text");
        hashOutput.put("monospace", true); // For better readability of hashes
        hashOutput.put("buttons", List.of("copy")); // Add copy button
        section1OutputFields.add(hashOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Error Display (conditional)
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Error Information");
        outputSection2.put("condition", "error");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section2OutputFields.add(errorOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

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