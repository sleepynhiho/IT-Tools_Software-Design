package kostovite;

import java.util.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class JWTParser implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "JWTParser";
    }

    @Override
    public void execute() {
        System.out.println("JWTParser Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test with a valid JWT token
            String validJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            params.put("jwtToken", validJwt);

            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Valid JWT): " + result1);

            // Test with a malformed JWT token (missing parts)
            params.put("jwtToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Malformed JWT - missing signature): " + result2);

            // Test with an invalid JWT token (not a JWT)
            params.put("jwtToken", "not.a.jwt");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Invalid JWT): " + result3);

            // Test with an empty JWT token
            params.put("jwtToken", "");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Empty JWT): " + result4);

            // Test with custom JWT having different payload fields
            String customJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmMxMjMiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJ1c2VyIiwiYWRtaW4iXSwiaWF0IjoxNjE1NDgzMjAwfQ.signature";
            params.put("jwtToken", customJwt);
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Custom JWT with different fields): " + result5);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "JWTParser");
        metadata.put("name", "JWT parser");
        metadata.put("description", "Parse and decode your JSON Web Token (jwt) and display its content.");
        metadata.put("icon", "VpnKey");
        metadata.put("category", "Web");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: JWT Parser ---
        Map<String, Object> jwtSection = new HashMap<>();
        jwtSection.put("id", "jwt");
        jwtSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "jwtToken"),
                Map.entry("label", "JWT to decode"),
                Map.entry("placeholder", "Paste your JWT here (e.g., xxxxx.yyyyy.zzzzz)"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("multiline", true),
                Map.entry("default", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"),
                Map.entry("containerId", "main"),
                Map.entry("width", 498),
                Map.entry("height", 108),
                Map.entry("monospace", true)
        ));
        jwtSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(createOutputField("alg", "alg (Algorithm)"));
        outputs.add(createOutputField("typ", "typ (Type)"));
        outputs.add(createOutputField("payload", "Payload"));
        outputs.add(createOutputField("sub", "sub (Subject)"));
        outputs.add(createOutputField("name", "name (Full name)"));
        outputs.add(createOutputField("iat", "iat (Issued at)"));
        jwtSection.put("outputs", outputs);

        sections.add(jwtSection);

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

    private Map<String, Object> createOutputField(String id, String label) {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("label", label),
                Map.entry("type", "text"),
                Map.entry("containerId", "main"),
                Map.entry("width", 498),
                Map.entry("height", 34),
                Map.entry("monospace", true)
        );
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get input parameters
            String jwtToken = getStringParam(input, "jwtToken", null);

            // --- Validation ---
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "JWT token is required.");
            }

            // Split the JWT into its three parts
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid JWT format. JWT should contain at least header and payload sections separated by dots.");
            }

            // Decode header
            String header;
            Map<String, Object> headerJson;
            try {
                header = decodeBase64(parts[0]);
                headerJson = parseJson(header);
            } catch (Exception e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Failed to decode JWT header: " + e.getMessage());
            }

            // Decode payload
            String payload;
            Map<String, Object> payloadJson;
            try {
                payload = decodeBase64(parts[1]);
                payloadJson = parseJson(payload);
            } catch (Exception e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Failed to decode JWT payload: " + e.getMessage());
            }

            // Extract JWT header fields
            String algorithm = headerJson.containsKey("alg") ? headerJson.get("alg").toString() : "";
            String type = headerJson.containsKey("typ") ? headerJson.get("typ").toString() : "";

            // Extract JWT payload fields
            String subject = payloadJson.containsKey("sub") ? payloadJson.get("sub").toString() : "";
            String name = payloadJson.containsKey("name") ? payloadJson.get("name").toString() : "";
            String issuedAt = payloadJson.containsKey("iat") ? formatIssuedAt(payloadJson.get("iat")) : "";

            // Prepare results
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("alg", algorithm);
            result.put("typ", type);
            result.put("payload", payload); // Return the full decoded payload JSON as a string
            result.put("sub", subject);
            result.put("name", name);
            result.put("iat", issuedAt);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing JWT parsing: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * Decode a Base64 URL-safe encoded string
     */
    private String decodeBase64(String input) {
        // Add padding if necessary
        StringBuilder builder = new StringBuilder(input);
        while (builder.length() % 4 != 0) {
            builder.append('=');
        }
        String paddedInput = builder.toString();

        // Replace URL-safe characters
        paddedInput = paddedInput.replace('-', '+').replace('_', '/');

        byte[] decodedBytes = Base64.getDecoder().decode(paddedInput);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Parse a JSON string into a Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) throws Exception {
        // This is a simplified JSON parser for demonstration purposes
        // In a real implementation, you would use a proper JSON library like Jackson or Gson

        // For this example, we'll implement a very basic parser that can handle simple JSON objects
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new Exception("Invalid JSON format");
        }

        String content = json.substring(1, json.length() - 1);
        Map<String, Object> result = new HashMap<>();

        // Split by commas not inside quotes
        List<String> pairs = new ArrayList<>();
        StringBuilder currentPair = new StringBuilder();
        boolean inQuotes = false;

        for (char c : content.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                pairs.add(currentPair.toString().trim());
                currentPair = new StringBuilder();
                continue;
            }
            currentPair.append(c);
        }

        if (currentPair.length() > 0) {
            pairs.add(currentPair.toString().trim());
        }

        // Process each key-value pair
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim();

                // Handle different value types
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // String value
                    result.put(key, value.substring(1, value.length() - 1));
                } else if (value.equals("true") || value.equals("false")) {
                    // Boolean value
                    result.put(key, Boolean.parseBoolean(value));
                } else if (value.matches("-?\\d+(\\.\\d+)?")) {
                    // Numeric value
                    if (value.contains(".")) {
                        result.put(key, Double.parseDouble(value));
                    } else {
                        try {
                            result.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            // If it's too large for an int, use Long
                            result.put(key, Long.parseLong(value));
                        }
                    }
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    // Array value (simplified implementation)
                    result.put(key, value);
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    // Object value (simplified implementation)
                    result.put(key, value);
                } else {
                    // Unknown type or null
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Format the issued at timestamp
     */
    private String formatIssuedAt(Object iatObj) {
        if (iatObj == null) {
            return "";
        }

        try {
            long timestamp;
            if (iatObj instanceof Number) {
                timestamp = ((Number) iatObj).longValue();
            } else {
                timestamp = Long.parseLong(iatObj.toString());
            }

            // Convert Unix timestamp to human-readable date
            Date date = new Date(timestamp * 1000); // Convert seconds to milliseconds
            return date.toString() + " (Unix timestamp: " + timestamp + ")";
        } catch (NumberFormatException e) {
            return iatObj.toString();
        }
    }
}