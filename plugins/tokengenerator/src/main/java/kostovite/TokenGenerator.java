package kostovite;

import java.security.SecureRandom;
import java.util.*;

// Assuming ExtendedPluginInterface or just PluginInterface based on your framework needs
public class TokenGenerator implements PluginInterface { // Using standard PluginInterface
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBER_CHARS = "0123456789";
    // Reduced special chars for better readability in simple cases, adjust if needed
    private static final String SPECIAL_CHARS = "!@#$%&*-+=?";

    private final SecureRandom random = new SecureRandom();

    /**
     * Internal name for the plugin backend. The 'name' in metadata is user-facing.
     * @return The backend name.
     */
    @Override
    public String getName() {
        return "TokenGenerator";
    }

    /**
     * Executes a standalone test run of the plugin.
     */
    @Override
    public void execute() {
        System.out.println("TokenGenerator Plugin executed (standalone test)");
        // Generate a sample token with default settings matching the new metadata defaults
        Map<String, Object> defaultInput = new HashMap<>();
        defaultInput.put("useUppercase", true);
        defaultInput.put("useLowercase", true);
        defaultInput.put("useNumbers", true);
        defaultInput.put("useSpecial", false);
        defaultInput.put("length", 16); // Default length from new metadata

        try {
            Map<String, Object> result = process(defaultInput);
            System.out.println("Sample Token Generation Result: " + result);
        } catch(Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates the metadata describing the plugin's UI and capabilities
     * in the new specified format.
     *
     * @return A map representing the plugin metadata JSON.
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "TokenGenerator"); // Matches the new format
        metadata.put("name", "Token Generator"); // User-facing name
        metadata.put("description", "Generate random string with the chars you want, uppercase or lowercase letters, numbers and/or symbols.");
        metadata.put("icon", "Key");
        metadata.put("category", "Crypto");
        metadata.put("customUI", false); // As per the example

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Generate Section ---
        Map<String, Object> generateSection = new HashMap<>();
        generateSection.put("id", "generate");
        generateSection.put("label", "Generate Token");

        // --- Inputs for Generate Section ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // useUppercase Input
        Map<String, Object> uppercaseInput = new HashMap<>();
        uppercaseInput.put("id", "useUppercase"); // Matches new format ID
        uppercaseInput.put("label", "Uppercase (ABC...)");
        uppercaseInput.put("type", "switch");
        uppercaseInput.put("default", true);
        uppercaseInput.put("containerId", "main"); // As per the example
        inputs.add(uppercaseInput);

        // useLowercase Input
        Map<String, Object> lowercaseInput = new HashMap<>();
        lowercaseInput.put("id", "useLowercase"); // Matches new format ID
        lowercaseInput.put("label", "Lowercase (abc...)");
        lowercaseInput.put("type", "switch");
        lowercaseInput.put("default", true);
        lowercaseInput.put("containerId", "main");
        inputs.add(lowercaseInput);

        // useNumbers Input
        Map<String, Object> numbersInput = new HashMap<>();
        numbersInput.put("id", "useNumbers"); // Matches new format ID
        numbersInput.put("label", "Numbers (123...)");
        numbersInput.put("type", "switch");
        numbersInput.put("default", true);
        numbersInput.put("containerId", "main");
        inputs.add(numbersInput);

        // useSpecial Input
        Map<String, Object> specialInput = new HashMap<>();
        specialInput.put("id", "useSpecial"); // Matches new format ID
        specialInput.put("label", "Symbols (!-;...)"); // Label from example
        specialInput.put("type", "switch");
        specialInput.put("default", false);
        specialInput.put("containerId", "main");
        inputs.add(specialInput);

        // length Input
        Map<String, Object> lengthInput = new HashMap<>();
        lengthInput.put("id", "length"); // Matches new format ID
        // The label in the example includes the default value, which is unusual.
        // We'll stick to a static label and let the UI handle showing the current value.
        lengthInput.put("label", "Length:");
        lengthInput.put("type", "slider");
        lengthInput.put("default", 16); // Default from example
        lengthInput.put("min", 1);    // Min from example
        lengthInput.put("max", 100);  // Max from example
        // step is not in the example, assume default of 1
        lengthInput.put("containerId", "main");
        inputs.add(lengthInput);

        generateSection.put("inputs", inputs);

        // --- Outputs for Generate Section ---
        List<Map<String, Object>> outputs = new ArrayList<>();

        // token Output
        Map<String, Object> tokenOutput = new HashMap<>();
        tokenOutput.put("id", "token");
        tokenOutput.put("label", "Generated Token");
        tokenOutput.put("type", "text");
        tokenOutput.put("width", 400);
        tokenOutput.put("height", 80); // As per example
        tokenOutput.put("buttons", List.of("copy", "refresh")); // As per example
        // Nested map for button placement
        Map<String, String> buttonPlacement = new HashMap<>();
        buttonPlacement.put("copy", "outside");
        buttonPlacement.put("refresh", "outside");
        tokenOutput.put("buttonPlacement", buttonPlacement); // As per example
        tokenOutput.put("containerId", "main");
        outputs.add(tokenOutput);

        generateSection.put("outputs", outputs);

        // Add the generate section to the list of sections
        sections.add(generateSection);

        // Add sections list to the main metadata map
        metadata.put("sections", sections);

        return metadata;
    }

    /**
     * Processes the input parameters to generate a token.
     *
     * @param input A map containing input parameters based on metadata IDs.
     * @return A map containing the result ('success', 'token' or 'error').
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // No operation selection needed as per the new metadata (only generate)
            // Get parameters using the NEW IDs, with defaults from metadata
            boolean useUppercase = getBooleanParam(input, "useUppercase", true);
            boolean useLowercase = getBooleanParam(input, "useLowercase", true);
            boolean useNumbers = getBooleanParam(input, "useNumbers", true);
            boolean useSpecial = getBooleanParam(input, "useSpecial", false);
            int length = getIntParam(input); // Use new ID and default

            // Validation
            if (length <= 0) {
                // Use standard error structure
                result.put("success", false);
                result.put("error", "Token length must be greater than zero");
                return result;
            }

            if (!useUppercase && !useLowercase && !useNumbers && !useSpecial) {
                // Use standard error structure
                result.put("success", false);
                result.put("error", "At least one character set must be selected");
                return result;
            }

            // Generate the token
            String token = generateToken(useUppercase, useLowercase, useNumbers, useSpecial, length);

            // Return results - ONLY include fields defined in the new metadata output section
            result.put("success", true);
            result.put("token", token); // Matches the output field ID 'token'

            // --- REMOVED other result fields like length, settings etc. ---

        } catch (IllegalArgumentException e) { // Catch specific validation errors
            result.put("success", false);
            result.put("error", e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing token generation request: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "An unexpected error occurred during token generation.");
        }

        return result;
    }

    /**
     * Generates a random token string based on the specified parameters.
     *
     * @param useUppercase Include uppercase letters.
     * @param useLowercase Include lowercase letters.
     * @param useNumbers Include numbers.
     * @param useSpecial Include special characters.
     * @param length The desired length of the token.
     * @return The generated random token string.
     * @throws IllegalArgumentException if no character sets are selected or length is non-positive.
     */
    private String generateToken(boolean useUppercase, boolean useLowercase,
                                 boolean useNumbers, boolean useSpecial, int length) {
        // Build the character set based on selected options
        StringBuilder charSetBuilder = new StringBuilder();
        if (useUppercase) charSetBuilder.append(UPPERCASE_CHARS);
        if (useLowercase) charSetBuilder.append(LOWERCASE_CHARS);
        if (useNumbers) charSetBuilder.append(NUMBER_CHARS);
        if (useSpecial) charSetBuilder.append(SPECIAL_CHARS);

        String charSet = charSetBuilder.toString();

        // This validation is done in process(), but kept here as defense
        if (charSet.isEmpty()) {
            throw new IllegalArgumentException("Internal Error: No character sets available for token generation.");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("Internal Error: Token length must be positive.");
        }

        // Generate the token using SecureRandom
        StringBuilder tokenBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charSet.length());
            tokenBuilder.append(charSet.charAt(randomIndex));
        }

        return tokenBuilder.toString();
    }

    /**
     * Helper method to safely extract a boolean parameter from the input map.
     *
     * @param input The input map.
     * @param key The parameter key (ID).
     * @param defaultValue The default value if the key is missing or has an invalid type.
     * @return The extracted boolean value or the default.
     */
    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        // Optionally handle string "true"/"false" if the frontend might send them
        return defaultValue; // Return default if key missing or wrong type
    }

    /**
     * Helper method to safely extract an integer parameter from the input map.
     *
     * @param input The input map.
     * @return The extracted integer value or the default, ensuring it's at least 1.
     */
    private int getIntParam(Map<String, Object> input) {
        Object value = input.get("length");
        int intVal = 16; // Start with default

        if (value instanceof Integer) {
            intVal = (Integer) value;
        } else if (value instanceof Number) { // Handle Double, Long etc. from JSON
            intVal = ((Number) value).intValue();
        }
        // Optionally handle String representation if needed
        // else if (value instanceof String) {
        //    try {
        //        intVal = Integer.parseInt((String) value);
        //    } catch (NumberFormatException e) { /* Use default */ }
        //}

        // Basic validation - ensure at least 1 for length
        return Math.max(1, intVal);
    }
}