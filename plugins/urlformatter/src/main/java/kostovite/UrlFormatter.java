package kostovite;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Removed unused SimpleDateFormat, ObjectMapper, Pattern

// Assuming PluginInterface is standard
public class UrlFormatter implements PluginInterface {

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "UrlFormatter";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("UrlFormatter Plugin executed (standalone test)");
        try {
            // Example encoding
            Map<String, Object> encodeParams = new HashMap<>();
            encodeParams.put("uiOperation", "encode"); // Use new ID
            encodeParams.put("inputText", "hello world?&="); // Use new ID
            Map<String, Object> encodeResult = process(encodeParams);
            System.out.println("Encode Result: " + encodeResult);

            // Example decoding
            Map<String, Object> decodeParams = new HashMap<>();
            decodeParams.put("uiOperation", "decode"); // Use new ID
            decodeParams.put("inputText", encodeResult.get("outputText")); // Use result from encode
            Map<String, Object> decodeResult = process(decodeParams);
            System.out.println("Decode Result: " + decodeResult);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "UrlFormatter"); // ID matches class name
        metadata.put("name", "URL Encoder / Decoder"); // User-facing name
        metadata.put("description", "Encode strings for safe use in URLs or decode URL-encoded strings.");
        metadata.put("icon", "Link");
        metadata.put("category", "Web Tools");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", true); // Enable dynamic updates

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input & Operation ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "inputConfig");
        inputSection.put("label", "Input & Operation");

        List<Map<String, Object>> inputs = new ArrayList<>();

        // Operation Selection
        inputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "encode", "label", "URL Encode (Percent-Encode)"),
                        Map.of("value", "decode", "label", "URL Decode (Percent-Decode)")
                )),
                Map.entry("default", "encode"),
                Map.entry("required", true)
        ));

        // Text Input
        inputs.add(Map.ofEntries(
                Map.entry("id", "inputText"),
                Map.entry("label", "Text:"), // Label is static now
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 6),
                Map.entry("placeholder", "Enter text to encode or decode..."), // Static placeholder
                Map.entry("required", true), // Allow empty string, handled in process
                Map.entry("helperText", "Encodes/decodes based on RFC 3986 (UTF-8).") // Static helper
        ));

        inputSection.put("inputs", inputs);
        sections.add(inputSection);


        // --- Section 2: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Output");
        // Condition: Show only on success AND when output text is actually present
        resultsSection.put("condition", "success === true && typeof outputText !== 'undefined'");

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Result Output
        Map<String, Object> outputField = createOutputField("outputText", "Result", null);
        outputField.put("multiline", true);
        outputField.put("rows", 6);
        outputField.put("monospace", true);
        outputField.put("buttons", List.of("copy"));
        resultOutputs.add(outputField);

        // Length info
        resultOutputs.add(createOutputField("outputLength", "Output Length", null));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 3: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", null)); // style handled by helper
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create output field definitions
    private Map<String, Object> createOutputField(String id, String label, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", "text");
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error");
        }
        if ("text".equals("text") && id.toLowerCase().contains("length")) {
            field.put("monospace", true); // Monospace for length
        }
        return field;
    }

    /**
     * Processes the input text (using IDs from the new format)
     * to perform URL encoding or decoding.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String uiOperation = getStringParam(input, "uiOperation", "encode"); // Default operation
        String inputText = getStringParam(input, "inputText", ""); // Allow empty string input
        String errorOutputId = "errorMessage";

        Map<String, Object> result = new HashMap<>();

        try {
            String outputText;
            switch (uiOperation.toLowerCase()) {
                case "encode":
                    outputText = urlEncode(inputText);
                    break;
                case "decode":
                    // Add specific catch for IllegalArgumentException during decode
                    try {
                        outputText = urlDecode(inputText);
                    } catch (IllegalArgumentException iae) {
                        System.err.println("URL Decode Error: " + iae.getMessage());
                        // Provide a more specific error message for common decode issues
                        String specificError = "Invalid URL encoding sequence";
                        if (iae.getMessage() != null && iae.getMessage().contains("unterminated")) {
                            specificError = "Invalid URL encoding (unterminated % escape sequence)";
                        } else if (iae.getMessage() != null && iae.getMessage().contains("Illegal hex characters")) {
                            specificError = "Invalid URL encoding (illegal hex characters in % sequence)";
                        }
                        throw new IllegalArgumentException(specificError, iae); // Re-throw with better message
                    }
                    break;
                default:
                    // Should not happen if frontend uses select options, but good defense
                    throw new IllegalArgumentException("Unsupported operation: " + uiOperation);
            }

            // Build success result map matching NEW output field IDs
            result.put("success", true);
            result.put("outputText", outputText); // Matches output ID
            result.put("outputLength", outputText.length() + " characters"); // Matches output ID

            // Optionally include original input for context if needed by frontend
            // result.put("originalInput", inputText);

        } catch (IllegalArgumentException e) { // Catch our specific validation/decode errors
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing URL format request: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put(errorOutputId, "Unexpected error: " + e.getMessage());
        }
        return result;
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * URL encodes a string using UTF-8 and replaces '+' with '%20'.
     */
    private String urlEncode(String input) {
        if (input == null) return "";
        // URLEncoder encodes space as '+', replace it with %20 for stricter RFC 3986 compatibility
        return URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * URL decodes a string using UTF-8.
     * Throws IllegalArgumentException on invalid sequences.
     */
    private String urlDecode(String input) throws IllegalArgumentException {
        if (input == null) return "";
        // URLDecoder throws IllegalArgumentException for invalid sequences
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    // Null default indicates required (or empty string allowed if defaultValue="")
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString(); // Don't trim input text
        // Only throw if required (defaultValue is null) and value is truly empty
        if (strValue.isEmpty() && defaultValue == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return strValue;
    }
}