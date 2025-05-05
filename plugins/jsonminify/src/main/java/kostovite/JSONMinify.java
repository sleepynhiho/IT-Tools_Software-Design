package kostovite;

import java.util.*;

public class JSONMinify implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "JSONMinify";
    }

    @Override
    public void execute() {
        System.out.println("JSONMinify Plugin executed (standalone test)");
        try {
            // Test with formatted JSON
            Map<String, Object> params = new HashMap<>();
            params.put("inputXML", "{\n  \"name\": \"John\",\n  \"age\": 30,\n  \"city\": \"New York\"\n}");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Simple JSON): " + result1);

            // Test with already minified JSON
            params.put("inputXML", "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Already minified): " + result2);

            // Test with complex nested JSON
            params.put("inputXML", "{\n  \"employees\": [\n    {\n      \"name\": \"Bob\",\n      \"age\": 25,\n      \"department\": \"Engineering\"\n    },\n    {\n      \"name\": \"Alice\",\n      \"age\": 28,\n      \"department\": \"Marketing\"\n    }\n  ],\n  \"company\": \"ACME Inc.\",\n  \"founded\": 1985\n}");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Complex nested JSON): " + result3);

            // Test with invalid JSON
            params.put("inputXML", "{\n  name: \"John\",\n  age: 30\n}"); // Missing quotes
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid JSON): " + result4);

            // Test with empty input
            params.put("inputXML", "");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Empty input): " + result5);

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
        metadata.put("name", "JSON minify");
        metadata.put("icon", "FormatListNumbered");
        metadata.put("description", "Minify and compress your JSON by removing unnecessary whitespace.");
        metadata.put("id", "JSONMinify");
        metadata.put("category", "Development");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: JSON Minify ---
        Map<String, Object> jsonMinifySection = new HashMap<>();
        jsonMinifySection.put("id", "jsonMinify");
        jsonMinifySection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("label", "Your raw JSON"),
                Map.entry("placeholder", "Paste your raw JSON here..."),
                Map.entry("required", true),
                Map.entry("multiline", true),
                Map.entry("containerId", "input"),
                Map.entry("id", "inputXML"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 420),
                Map.entry("default", "{\n  \"name\": \"John\",\n  \"age\": 30,\n  \"city\": \"New York\"\n}")
        ));
        jsonMinifySection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("label", "Minified version of your JSON"),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "jsonOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));
        jsonMinifySection.put("outputs", outputs);

        sections.add(jsonMinifySection);

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
            // Get the JSON string from input
            String jsonString = getStringParam(input, "inputXML", "");

            // Validation
            if (jsonString.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "JSON input is required.");
            }

            // Minify JSON
            String minifiedJson = minifyJson(jsonString);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("jsonOutput", minifiedJson);

            return result;

        } catch (JsonException e) {
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing JSON minification: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Minify JSON string by removing unnecessary whitespace
    private String minifyJson(String jsonString) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        // Validate the JSON first
        try {
            validateJson(jsonString);
        } catch (JsonException e) {
            throw e;
        }

        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);

            // Handle string literals
            if (inString) {
                result.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            }
            // Handle non-string content
            else {
                if (c == '"') {
                    inString = true;
                    result.append(c);
                } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    // Skip whitespace
                    continue;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    // Validate JSON without using external libraries
    private void validateJson(String jsonString) {
        jsonString = jsonString.trim();

        if (jsonString.isEmpty()) {
            throw new JsonException("Empty JSON string");
        }

        // Simple structure validation
        char firstChar = jsonString.charAt(0);
        char lastChar = jsonString.charAt(jsonString.length() - 1);

        if ((firstChar == '{' && lastChar != '}') ||
                (firstChar == '[' && lastChar != ']') ||
                (firstChar != '{' && firstChar != '[')) {
            throw new JsonException("Invalid JSON structure");
        }

        // Check for balanced braces and brackets
        int curlyBraceCount = 0;
        int squareBracketCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    curlyBraceCount++;
                } else if (c == '}') {
                    curlyBraceCount--;
                    if (curlyBraceCount < 0) {
                        throw new JsonException("Unbalanced curly braces in JSON");
                    }
                } else if (c == '[') {
                    squareBracketCount++;
                } else if (c == ']') {
                    squareBracketCount--;
                    if (squareBracketCount < 0) {
                        throw new JsonException("Unbalanced square brackets in JSON");
                    }
                }
            }
        }

        if (curlyBraceCount != 0 || squareBracketCount != 0) {
            throw new JsonException("Unbalanced braces or brackets in JSON");
        }

        if (inString) {
            throw new JsonException("Unterminated string in JSON");
        }
    }

    // Helper method to get string parameters
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    // Custom JsonException for error handling
    private static class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }
}