package kostovite;

import java.util.*;

public class StringObfuscator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final String DEFAULT_STRING = "Lorem ipsum dolor sit amet";
    private static final int DEFAULT_KEEP_FIRST = 4;
    private static final int DEFAULT_KEEP_LAST = 4;
    private static final boolean DEFAULT_KEEP_SPACES = true;
    private static final char MASK_CHAR = '*';

    @Override
    public String getName() {
        return "StringObfuscator";
    }

    @Override
    public void execute() {
        System.out.println("StringObfuscator Plugin executed (standalone test)");
        try {
            // Test with default settings
            Map<String, Object> params = new HashMap<>();
            params.put("paragraphs", DEFAULT_STRING);
            params.put("keepFirst", DEFAULT_KEEP_FIRST);
            params.put("keepLast", DEFAULT_KEEP_LAST);
            params.put("keepSpaces", DEFAULT_KEEP_SPACES);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default settings): " + result1);

            // Test with different string
            params.put("paragraphs", "confidential@email.com");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Email address): " + result2);

            // Test with credit card number
            params.put("paragraphs", "4111 1111 1111 1111");
            params.put("keepFirst", 4);
            params.put("keepLast", 4);
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Credit card): " + result3);

            // Test with API token
            params.put("paragraphs", "ghp_12345678901234567890ABCDEFGHIJKLMNOPQR");
            params.put("keepFirst", 6);
            params.put("keepLast", 0);
            params.put("keepSpaces", false);
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (API token): " + result4);

            // Test with short string
            params.put("paragraphs", "abc");
            params.put("keepFirst", 4);
            params.put("keepLast", 4);
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Short string): " + result5);

            // Test with empty string
            params.put("paragraphs", "");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Empty string): " + result6);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("customUI", false);
        metadata.put("name", "String obfuscator");
        metadata.put("description", "Obfuscate a string (like a secret, an IBAN, or a token) to make it shareable and identifiable without revealing its content.");
        metadata.put("icon", "TextFields");
        metadata.put("id", "StringObfuscator");
        metadata.put("category", "Text");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: String Obfuscator ---
        Map<String, Object> obfuscatorSection = new HashMap<>();
        obfuscatorSection.put("id", "stringObfuscator");
        obfuscatorSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // String to obfuscate input
        inputs.add(Map.ofEntries(
                Map.entry("label", "String to obfuscate:"),
                Map.entry("id", "paragraphs"),
                Map.entry("default", DEFAULT_STRING),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 40),
                Map.entry("containerId", "input1")
        ));

        // Keep first input
        inputs.add(Map.ofEntries(
                Map.entry("required", true),
                Map.entry("default", "4"),
                Map.entry("label", "Keep first:"),
                Map.entry("id", "keepFirst"),
                Map.entry("type", "number"),
                Map.entry("placeholder", "e.g., 4"),
                Map.entry("button", List.of("minus", "plus")),
                Map.entry("width", 200),
                Map.entry("height", 36),
                Map.entry("containerId", "input2")
        ));

        // Keep last input
        inputs.add(Map.ofEntries(
                Map.entry("label", "Keep last:"),
                Map.entry("id", "keepLast"),
                Map.entry("default", 4),
                Map.entry("type", "number"),
                Map.entry("placeholder", "e.g., 4"),
                Map.entry("button", List.of("minus", "plus")),
                Map.entry("width", 200),
                Map.entry("height", 36),
                Map.entry("containerId", "input3")
        ));

        // Keep spaces switch
        inputs.add(Map.ofEntries(
                Map.entry("label", "Keep spaces:"),
                Map.entry("id", "keepSpaces"),
                Map.entry("type", "switch"),
                Map.entry("default", true),
                Map.entry("containerId", "input5")
        ));

        obfuscatorSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 600),
                Map.entry("id", "obfuscated"),
                Map.entry("label", ""),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80),
                Map.entry("multiline", true),
                Map.entry("monospace", true)
        ));
        obfuscatorSection.put("outputs", outputs);

        sections.add(obfuscatorSection);

        // --- Error Section ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", ERROR_OUTPUT_ID),
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error")
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);

        metadata.put("sections", sections);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get parameters
            String inputString = getStringParam(input, "paragraphs", DEFAULT_STRING);
            int keepFirst = getIntParam(input, "keepFirst", DEFAULT_KEEP_FIRST);
            int keepLast = getIntParam(input, "keepLast", DEFAULT_KEEP_LAST);
            boolean keepSpaces = getBooleanParam(input, "keepSpaces", DEFAULT_KEEP_SPACES);

            // Validate inputs
            if (keepFirst < 0) {
                keepFirst = 0;
            }

            if (keepLast < 0) {
                keepLast = 0;
            }

            // Obfuscate the string
            String obfuscatedString = obfuscateString(inputString, keepFirst, keepLast, keepSpaces);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("obfuscated", obfuscatedString);

            return result;

        } catch (Exception e) {
            System.err.println("Error obfuscating string: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Obfuscate a string by replacing characters with mask character
    private String obfuscateString(String input, int keepFirst, int keepLast, boolean keepSpaces) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // If the string is shorter than the sum of keepFirst and keepLast,
        // just return the original string
        int length = input.length();
        if (keepFirst + keepLast >= length) {
            return input;
        }

        StringBuilder result = new StringBuilder();

        // Keep the first N characters
        result.append(input.substring(0, keepFirst));

        // Obfuscate the middle part
        for (int i = keepFirst; i < length - keepLast; i++) {
            char c = input.charAt(i);
            if (keepSpaces && c == ' ') {
                result.append(' ');
            } else {
                result.append(MASK_CHAR);
            }
        }

        // Keep the last N characters
        if (keepLast > 0) {
            result.append(input.substring(length - keepLast));
        }

        return result.toString();
    }

    // Helper methods to get parameters with different types
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private int getIntParam(Map<String, Object> input, String key, int defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String strValue = value.toString().toLowerCase();
        if ("true".equals(strValue) || "yes".equals(strValue) || "1".equals(strValue)) {
            return true;
        } else if ("false".equals(strValue) || "no".equals(strValue) || "0".equals(strValue)) {
            return false;
        }

        return defaultValue;
    }
}