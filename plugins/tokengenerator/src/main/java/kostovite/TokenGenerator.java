package kostovite;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenGenerator implements ExtendedPluginInterface {
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBER_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String getName() {
        return "TokenGenerator";
    }

    @Override
    public void execute() {
        System.out.println("TokenGenerator Plugin executed");
        // Generate a sample token with default settings
        Map<String, Object> defaultInput = new HashMap<>();
        defaultInput.put("useUppercase", true);
        defaultInput.put("useLowercase", true);
        defaultInput.put("useNumbers", true);
        defaultInput.put("useSpecial", false);
        defaultInput.put("length", 16);

        Map<String, Object> result = process(defaultInput);
        System.out.println("Sample token: " + result.get("token"));
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generates secure random tokens with configurable character sets"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Generate operation
        Map<String, Object> generateOperation = new HashMap<>();
        generateOperation.put("description", "Generate a random token");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("useUppercase", Map.of("type", "boolean", "description", "Include uppercase letters", "required", false));
        inputs.put("useLowercase", Map.of("type", "boolean", "description", "Include lowercase letters", "required", false));
        inputs.put("useNumbers", Map.of("type", "boolean", "description", "Include numbers", "required", false));
        inputs.put("useSpecial", Map.of("type", "boolean", "description", "Include special characters", "required", false));
        inputs.put("length", Map.of("type", "integer", "description", "Token length", "required", false));
        generateOperation.put("inputs", inputs);
        operations.put("generate", generateOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "TokenGenerator"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Key"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Security"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Token Settings
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Token Settings");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Token length
        Map<String, Object> lengthField = new HashMap<>();
        lengthField.put("name", "length");
        lengthField.put("label", "Length:");
        lengthField.put("type", "slider");
        lengthField.put("min", 4);
        lengthField.put("max", 64);
        lengthField.put("step", 1);
        lengthField.put("default", 16);
        lengthField.put("required", true);
        section1Fields.add(lengthField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Character Sets
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Character Sets");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Uppercase letters
        Map<String, Object> uppercaseField = new HashMap<>();
        uppercaseField.put("name", "useUppercase");
        uppercaseField.put("label", "Uppercase Letters (A-Z)");
        uppercaseField.put("type", "switch");
        uppercaseField.put("default", true);
        section2Fields.add(uppercaseField);

        // Lowercase letters
        Map<String, Object> lowercaseField = new HashMap<>();
        lowercaseField.put("name", "useLowercase");
        lowercaseField.put("label", "Lowercase Letters (a-z)");
        lowercaseField.put("type", "switch");
        lowercaseField.put("default", true);
        section2Fields.add(lowercaseField);

        // Numbers
        Map<String, Object> numbersField = new HashMap<>();
        numbersField.put("name", "useNumbers");
        numbersField.put("label", "Numbers (0-9)");
        numbersField.put("type", "switch");
        numbersField.put("default", true);
        section2Fields.add(numbersField);

        // Special characters
        Map<String, Object> specialField = new HashMap<>();
        specialField.put("name", "useSpecial");
        specialField.put("label", "Special Characters (!@#$%^&*...)");
        specialField.put("type", "switch");
        specialField.put("default", false);
        section2Fields.add(specialField);

        // Preview of character set
        Map<String, Object> previewField = new HashMap<>();
        previewField.put("name", "characterPreview");
        previewField.put("label", "Character Set:");
        previewField.put("type", "text");
        previewField.put("disabled", true);
        previewField.put("formula", "((useUppercase ? 'A-Z ' : '') + (useLowercase ? 'a-z ' : '') + (useNumbers ? '0-9 ' : '') + (useSpecial ? '!@#$...' : '')).trim() || 'Please select at least one character set'");
        section2Fields.add(previewField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Generated Token
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Generated Token");
        outputSection1.put("condition", "success");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Token display
        Map<String, Object> tokenOutput = new HashMap<>();
        tokenOutput.put("title", "Token");
        tokenOutput.put("name", "token");
        tokenOutput.put("type", "text");
        tokenOutput.put("monospace", true);
        tokenOutput.put("buttons", List.of("copy"));
        tokenOutput.put("variant", "bold");
        section1OutputFields.add(tokenOutput);

        // Length confirmation
        Map<String, Object> lengthOutput = new HashMap<>();
        lengthOutput.put("title", "Length");
        lengthOutput.put("name", "length");
        lengthOutput.put("type", "text");
        lengthOutput.put("formula", "length + ' characters'");
        section1OutputFields.add(lengthOutput);

        // Generation timestamp
        Map<String, Object> timestampOutput = new HashMap<>();
        timestampOutput.put("title", "Generated On");
        timestampOutput.put("name", "timestamp");
        timestampOutput.put("type", "text");
        timestampOutput.put("formula", "new Date().toISOString()");
        section1OutputFields.add(timestampOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Token Analysis
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Token Analysis");
        outputSection2.put("condition", "success");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Character sets used
        Map<String, Object> usedSetsOutput = new HashMap<>();
        usedSetsOutput.put("title", "Character Sets Used");
        usedSetsOutput.put("name", "usedSets");
        usedSetsOutput.put("type", "chips");
        usedSetsOutput.put("items", "[" +
                "settings.useUppercase ? 'Uppercase' : null, " +
                "settings.useLowercase ? 'Lowercase' : null, " +
                "settings.useNumbers ? 'Numbers' : null, " +
                "settings.useSpecial ? 'Special' : null" +
                "].filter(Boolean)");
        section2OutputFields.add(usedSetsOutput);

        // Entropy calculation (simplified)
        Map<String, Object> entropyOutput = new HashMap<>();
        entropyOutput.put("title", "Estimated Entropy");
        entropyOutput.put("name", "entropy");
        entropyOutput.put("type", "text");
        entropyOutput.put("formula",
                "Math.log2(" +
                        "(settings.useUppercase ? 26 : 0) + " +
                        "(settings.useLowercase ? 26 : 0) + " +
                        "(settings.useNumbers ? 10 : 0) + " +
                        "(settings.useSpecial ? 33 : 0)" +
                        ") * length + ' bits'");
        section2OutputFields.add(entropyOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "error");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
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
            // Extract operation (default to "generate")
            String operation = (String) input.getOrDefault("operation", "generate");

            if ("generate".equalsIgnoreCase(operation)) {
                // Get parameters with defaults
                boolean useUppercase = getBooleanParam(input, "useUppercase", true);
                boolean useLowercase = getBooleanParam(input, "useLowercase", true);
                boolean useNumbers = getBooleanParam(input, "useNumbers", true);
                boolean useSpecial = getBooleanParam(input, "useSpecial", false);
                int length = getIntParam(input);

                // Validation
                if (length <= 0) {
                    result.put("error", "Token length must be greater than zero");
                    return result;
                }

                if (!useUppercase && !useLowercase && !useNumbers && !useSpecial) {
                    result.put("error", "At least one character set must be selected");
                    return result;
                }

                // Generate the token
                String token = generateToken(useUppercase, useLowercase, useNumbers, useSpecial, length);

                // Return results
                result.put("token", token);
                result.put("length", token.length());

                // Include information about what was used
                Map<String, Object> settings = new HashMap<>();
                settings.put("useUppercase", useUppercase);
                settings.put("useLowercase", useLowercase);
                settings.put("useNumbers", useNumbers);
                settings.put("useSpecial", useSpecial);
                settings.put("requestedLength", length);
                result.put("settings", settings);

                result.put("success", true);
            } else {
                result.put("error", "Unsupported operation: " + operation);
                return result;
            }
        } catch (Exception e) {
            result.put("error", "Error generating token: " + e.getMessage());
        }

        return result;
    }

    private String generateToken(boolean useUppercase, boolean useLowercase,
                                 boolean useNumbers, boolean useSpecial, int length) {
        // Build the character set based on selected options
        StringBuilder charSetBuilder = new StringBuilder();

        if (useUppercase) charSetBuilder.append(UPPERCASE_CHARS);
        if (useLowercase) charSetBuilder.append(LOWERCASE_CHARS);
        if (useNumbers) charSetBuilder.append(NUMBER_CHARS);
        if (useSpecial) charSetBuilder.append(SPECIAL_CHARS);

        String charSet = charSetBuilder.toString();

        if (charSet.isEmpty()) {
            throw new IllegalArgumentException("No character sets selected");
        }

        // Generate the token
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charSet.length());
            tokenBuilder.append(charSet.charAt(randomIndex));
        }

        return tokenBuilder.toString();
    }

    // Helper methods for parameter extraction
    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private int getIntParam(Map<String, Object> input) {
        Object value = input.get("length");
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 16;
            }
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 16;
    }
}