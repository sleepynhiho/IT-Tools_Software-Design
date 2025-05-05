package kostovite;

import java.util.*;
import java.io.StringReader;
import java.io.StringWriter;

public class JSONPrettify implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int DEFAULT_INDENT_SIZE = 3;
    private static final boolean DEFAULT_SORT_KEYS = true;

    @Override
    public String getName() {
        return "JSONPrettify";
    }

    @Override
    public void execute() {
        System.out.println("JSONPrettify Plugin executed (standalone test)");
        try {
            // Test with simple JSON
            Map<String, Object> params = new HashMap<>();
            params.put("rawJson", "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}");
            params.put("indentSize", 4);
            params.put("sortKeys", true);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Simple JSON): " + result1);

            // Test with complex nested JSON
            params.put("rawJson", "{\"employees\":[{\"name\":\"Bob\",\"age\":25,\"department\":\"Engineering\"},{\"name\":\"Alice\",\"age\":28,\"department\":\"Marketing\"}],\"company\":\"ACME Inc.\",\"founded\":1985}");
            params.put("indentSize", 2);
            params.put("sortKeys", false);
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Complex JSON): " + result2);

            // Test with invalid JSON
            params.put("rawJson", "{name:John,age:30}");  // Missing quotes
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Invalid JSON): " + result3);

            // Test with empty JSON
            params.put("rawJson", "{}");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Empty JSON): " + result4);

            // Test with JSON array
            params.put("rawJson", "[1,2,3,4,5]");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (JSON Array): " + result5);

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
        metadata.put("name", "JSON prettify and format");
        metadata.put("icon", "FormatListNumbered");
        metadata.put("description", "Prettify your JSON string into a friendly, human-readable format.");
        metadata.put("id", "JSONPrettify");
        metadata.put("category", "Development");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: JSON Prettify ---
        Map<String, Object> jsonPrettifySection = new HashMap<>();
        jsonPrettifySection.put("id", "jsonPrettify");
        jsonPrettifySection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // Sort Keys switch
        inputs.add(Map.ofEntries(
                Map.entry("label", "Sort keys"),
                Map.entry("id", "sortKeys"),
                Map.entry("type", "switch"),
                Map.entry("default", true),
                Map.entry("containerId", "input1")
        ));

        // Indent Size input
        inputs.add(Map.ofEntries(
                Map.entry("required", true),
                Map.entry("default", "3"),
                Map.entry("label", "Indent size"),
                Map.entry("id", "indentSize"),
                Map.entry("type", "number"),
                Map.entry("placeholder", "e.g., 3"),
                Map.entry("button", List.of("minus", "plus")),
                Map.entry("width", 100),
                Map.entry("height", 40),
                Map.entry("containerId", "input1")
        ));

        // Raw JSON input
        inputs.add(Map.ofEntries(
                Map.entry("label", "Your raw JSON"),
                Map.entry("placeholder", "Paste your raw JSON here..."),
                Map.entry("required", true),
                Map.entry("multiline", true),
                Map.entry("containerId", "input2"),
                Map.entry("id", "rawJson"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 420)
        ));

        jsonPrettifySection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("label", "Prettified version of your JSON"),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "formattedJson"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));
        jsonPrettifySection.put("outputs", outputs);

        sections.add(jsonPrettifySection);

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
            String rawJson = getStringParam(input, "rawJson", "");
            int indentSize = getIntParam(input, "indentSize", DEFAULT_INDENT_SIZE);
            boolean sortKeys = getBooleanParam(input, "sortKeys", DEFAULT_SORT_KEYS);

            // Validate inputs
            if (rawJson.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Raw JSON input is required.");
            }

            if (indentSize < 0 || indentSize > 8) {
                indentSize = DEFAULT_INDENT_SIZE; // Reset to default if out of reasonable range
            }

            // Format JSON
            String formattedJson = prettifyJson(rawJson, indentSize, sortKeys);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("formattedJson", formattedJson);

            return result;

        } catch (JsonException e) {
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing JSON prettification: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Format JSON string with specified indent and sorting options
    private String prettifyJson(String jsonString, int indentSize, boolean sortKeys) {
        try {
            // First, validate JSON by parsing it
            Object parsedJson = parseJson(jsonString);

            // Convert to properly formatted JSON
            StringBuilder sb = new StringBuilder();
            if (parsedJson instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonMap = (Map<String, Object>) parsedJson;

                // Sort if needed
                if (sortKeys) {
                    jsonMap = sortJsonObject(jsonMap);
                }

                formatJsonObject(sb, jsonMap, 0, indentSize);
            } else if (parsedJson instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> jsonList = (List<Object>) parsedJson;
                formatJsonArray(sb, jsonList, 0, indentSize, sortKeys);
            } else {
                // Should never happen if JSON is valid
                throw new JsonException("Invalid JSON structure");
            }

            return sb.toString();

        } catch (JsonException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonException("Error formatting JSON: " + e.getMessage());
        }
    }

    // Parse JSON string into Map/List structure
    private Object parseJson(String jsonString) {
        jsonString = jsonString.trim();

        if (jsonString.isEmpty()) {
            throw new JsonException("Empty JSON string");
        }

        // Determine if it's an object or array
        if (jsonString.startsWith("{")) {
            return parseJsonObject(jsonString);
        } else if (jsonString.startsWith("[")) {
            return parseJsonArray(jsonString);
        } else {
            throw new JsonException("JSON must start with { or [");
        }
    }

    // Parse JSON object
    private Map<String, Object> parseJsonObject(String jsonString) {
        // Simple validation
        if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
            throw new JsonException("Invalid JSON object format");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // Remove outer braces
        jsonString = jsonString.substring(1, jsonString.length() - 1).trim();

        // Empty object
        if (jsonString.isEmpty()) {
            return result;
        }

        // Parse key-value pairs
        int pos = 0;
        while (pos < jsonString.length()) {
            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Check if we've reached the end
            if (pos >= jsonString.length()) {
                break;
            }

            // Parse key (must be in quotes)
            if (jsonString.charAt(pos) != '"') {
                throw new JsonException("Invalid JSON format: keys must be quoted");
            }

            int keyStart = pos + 1;
            pos = findEndOfString(jsonString, keyStart);
            String key = jsonString.substring(keyStart, pos);
            pos++; // Skip closing quote

            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Expect colon
            if (pos >= jsonString.length() || jsonString.charAt(pos) != ':') {
                throw new JsonException("Invalid JSON format: missing colon after key");
            }
            pos++; // Skip colon

            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Parse value
            ValueResult valueResult = parseValue(jsonString, pos);
            result.put(key, valueResult.value);
            pos = valueResult.endPos;

            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Check for comma or end
            if (pos < jsonString.length()) {
                if (jsonString.charAt(pos) == ',') {
                    pos++; // Skip comma
                } else if (jsonString.charAt(pos) != '}') {
                    throw new JsonException("Invalid JSON format: expected comma or closing brace");
                }
            }
        }

        return result;
    }

    // Parse JSON array
    private List<Object> parseJsonArray(String jsonString) {
        // Simple validation
        if (!jsonString.startsWith("[") || !jsonString.endsWith("]")) {
            throw new JsonException("Invalid JSON array format");
        }

        List<Object> result = new ArrayList<>();

        // Remove outer brackets
        jsonString = jsonString.substring(1, jsonString.length() - 1).trim();

        // Empty array
        if (jsonString.isEmpty()) {
            return result;
        }

        // Parse values
        int pos = 0;
        while (pos < jsonString.length()) {
            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Check if we've reached the end
            if (pos >= jsonString.length()) {
                break;
            }

            // Parse value
            ValueResult valueResult = parseValue(jsonString, pos);
            result.add(valueResult.value);
            pos = valueResult.endPos;

            // Skip whitespace
            while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
                pos++;
            }

            // Check for comma or end
            if (pos < jsonString.length()) {
                if (jsonString.charAt(pos) == ',') {
                    pos++; // Skip comma
                } else if (jsonString.charAt(pos) != ']') {
                    throw new JsonException("Invalid JSON format: expected comma or closing bracket");
                }
            }
        }

        return result;
    }

    // Parse a JSON value (object, array, string, number, boolean, null)
    private ValueResult parseValue(String jsonString, int startPos) {
        int pos = startPos;

        // Skip whitespace
        while (pos < jsonString.length() && Character.isWhitespace(jsonString.charAt(pos))) {
            pos++;
        }

        if (pos >= jsonString.length()) {
            throw new JsonException("Unexpected end of JSON string");
        }

        char c = jsonString.charAt(pos);

        // Object
        if (c == '{') {
            int endPos = findMatchingBrace(jsonString, pos, '{', '}');
            String objectStr = jsonString.substring(pos, endPos + 1);
            return new ValueResult(parseJsonObject(objectStr), endPos + 1);
        }

        // Array
        else if (c == '[') {
            int endPos = findMatchingBrace(jsonString, pos, '[', ']');
            String arrayStr = jsonString.substring(pos, endPos + 1);
            return new ValueResult(parseJsonArray(arrayStr), endPos + 1);
        }

        // String
        else if (c == '"') {
            int endPos = findEndOfString(jsonString, pos + 1);
            String value = jsonString.substring(pos + 1, endPos);
            return new ValueResult(value, endPos + 1);
        }

        // Number, boolean, or null
        else {
            int endPos = pos;
            while (endPos < jsonString.length() &&
                    !Character.isWhitespace(jsonString.charAt(endPos)) &&
                    jsonString.charAt(endPos) != ',' &&
                    jsonString.charAt(endPos) != '}' &&
                    jsonString.charAt(endPos) != ']') {
                endPos++;
            }

            String valueStr = jsonString.substring(pos, endPos);

            // Try to parse as a number first
            try {
                if (valueStr.contains(".")) {
                    return new ValueResult(Double.parseDouble(valueStr), endPos);
                } else {
                    return new ValueResult(Long.parseLong(valueStr), endPos);
                }
            } catch (NumberFormatException e) {
                // Not a number, check for boolean or null
                if ("true".equals(valueStr)) {
                    return new ValueResult(Boolean.TRUE, endPos);
                } else if ("false".equals(valueStr)) {
                    return new ValueResult(Boolean.FALSE, endPos);
                } else if ("null".equals(valueStr)) {
                    return new ValueResult(null, endPos);
                } else {
                    throw new JsonException("Invalid JSON value: " + valueStr);
                }
            }
        }
    }

    // Find the end of a string, considering escapes
    private int findEndOfString(String jsonString, int startPos) {
        int pos = startPos;
        boolean escaped = false;

        while (pos < jsonString.length()) {
            char c = jsonString.charAt(pos);

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return pos;
            }

            pos++;
        }

        throw new JsonException("Unterminated string in JSON");
    }

    // Find matching closing brace/bracket
    private int findMatchingBrace(String jsonString, int startPos, char openBrace, char closeBrace) {
        int pos = startPos;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        while (pos < jsonString.length()) {
            char c = jsonString.charAt(pos);

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
                } else if (c == openBrace) {
                    depth++;
                } else if (c == closeBrace) {
                    depth--;
                    if (depth == 0) {
                        return pos;
                    }
                }
            }

            pos++;
        }

        throw new JsonException("Unmatched " + openBrace + " in JSON");
    }

    // Format a JSON object with indentation
    private void formatJsonObject(StringBuilder sb, Map<String, Object> jsonObject, int level, int indentSize) {
        if (jsonObject.isEmpty()) {
            sb.append("{}");
            return;
        }

        sb.append("{\n");

        String indent = " ".repeat((level + 1) * indentSize);
        boolean first = true;

        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;

            sb.append(indent).append("\"").append(escapeJsonString(entry.getKey())).append("\": ");
            formatValue(sb, entry.getValue(), level + 1, indentSize);
        }

        sb.append("\n").append(" ".repeat(level * indentSize)).append("}");
    }

    // Format a JSON array with indentation
    private void formatJsonArray(StringBuilder sb, List<Object> jsonArray, int level, int indentSize, boolean sortKeys) {
        if (jsonArray.isEmpty()) {
            sb.append("[]");
            return;
        }

        sb.append("[\n");

        String indent = " ".repeat((level + 1) * indentSize);
        boolean first = true;

        for (Object value : jsonArray) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;

            sb.append(indent);
            formatValue(sb, value, level + 1, indentSize);
        }

        sb.append("\n").append(" ".repeat(level * indentSize)).append("]");
    }

    // Format a JSON value with indentation
    private void formatValue(StringBuilder sb, Object value, int level, int indentSize) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJsonString((String) value)).append("\"");
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            formatJsonObject(sb, map, level, indentSize);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            formatJsonArray(sb, list, level, indentSize, false);
        } else {
            sb.append("\"").append(escapeJsonString(value.toString())).append("\"");
        }
    }

    // Sort a JSON object by keys
    private Map<String, Object> sortJsonObject(Map<String, Object> jsonObject) {
        Map<String, Object> sortedMap = new TreeMap<>();

        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            Object value = entry.getValue();

            // Recursively sort nested objects
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                value = sortJsonObject(nestedMap);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) value;
                value = sortJsonArray(nestedList);
            }

            sortedMap.put(entry.getKey(), value);
        }

        return sortedMap;
    }

    // Sort objects inside a JSON array
    private List<Object> sortJsonArray(List<Object> jsonArray) {
        List<Object> sortedList = new ArrayList<>(jsonArray.size());

        for (Object value : jsonArray) {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                sortedList.add(sortJsonObject(nestedMap));
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) value;
                sortedList.add(sortJsonArray(nestedList));
            } else {
                sortedList.add(value);
            }
        }

        return sortedList;
    }

    // Escape special characters in JSON strings
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        result.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            result.append('0');
                        }
                        result.append(hex);
                    } else {
                        result.append(c);
                    }
            }
        }
        return result.toString();
    }

    // Helper class to hold parsed value and end position
    private static class ValueResult {
        final Object value;
        final int endPos;

        ValueResult(Object value, int endPos) {
            this.value = value;
            this.endPos = endPos;
        }
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

    // Custom JsonException for error handling
    private static class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }
}