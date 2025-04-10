package kostovite;

import java.security.SecureRandom;
import java.util.HashMap;
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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Generates secure random tokens with configurable character sets");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        Map<String, Object> generateOperation = new HashMap<>();
        generateOperation.put("description", "Generate a random token");

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("useUppercase", "Include uppercase letters (true/false)");
        inputs.put("useLowercase", "Include lowercase letters (true/false)");
        inputs.put("useNumbers", "Include numbers (true/false)");
        inputs.put("useSpecial", "Include special characters (true/false)");
        inputs.put("length", "Token length (positive integer)");

        generateOperation.put("inputs", inputs);
        operations.put("generate", generateOperation);

        metadata.put("operations", operations);
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
                int length = getIntParam(input, "length", 16);

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

    private int getIntParam(Map<String, Object> input, String key, int defaultValue) {
        Object value = input.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return defaultValue;
    }
}