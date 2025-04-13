package kostovite;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException; // Import if needed for specific exceptions
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // For pretty printing

// Assume PluginInterface is standard
public class JWTParser implements PluginInterface {

    // Relax pattern slightly to allow missing signature for parsing
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+(\\.[A-Za-z0-9-_.+/=]*)?$");
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "JWTParser";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("JWT Parser Plugin executed (standalone test)");
        try {
            String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            Map<String, Object> params = new HashMap<>();
            params.put("jwtToken", jwtToken); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Parse Result: " + result);

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
        metadata.put("id", "JWTParser"); // ID matches class name
        metadata.put("name", "JWT Decoder"); // User-facing name
        metadata.put("description", "Decode and inspect JSON Web Tokens (JWT). Optionally verify HMAC signatures.");
        metadata.put("icon", "VpnKey");
        metadata.put("category", "Crypto");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input Token ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "input");
        inputSection.put("label", "Input Token");

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(Map.ofEntries(
                Map.entry("id", "jwtToken"),
                Map.entry("label", "JWT Token:"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 5),
                Map.entry("placeholder", "Paste your JWT here (e.g., xxxxx.yyyyy.zzzzz)"),
                Map.entry("required", true),
                Map.entry("monospace", true) // Good for tokens
        ));

        inputSection.put("inputs", inputs);
        sections.add(inputSection);

        // --- Section 2: Verification Options (Optional) ---
        Map<String, Object> verificationSection = new HashMap<>();
        verificationSection.put("id", "verification");
        verificationSection.put("label", "Signature Verification (Optional - HMAC Only)");

        List<Map<String, Object>> verificationInputs = new ArrayList<>();

        verificationInputs.add(Map.ofEntries(
                Map.entry("id", "verifySignature"),
                Map.entry("label", "Verify Signature?"),
                Map.entry("type", "switch"),
                Map.entry("default", false),
                Map.entry("helperText", "Requires the correct secret key.")
        ));
        verificationInputs.add(Map.ofEntries(
                Map.entry("id", "secret"),
                Map.entry("label", "Secret Key (Base64 Encoded or Text):"),
                Map.entry("type", "password"), // Use password field
                Map.entry("placeholder", "Enter signing secret/key"),
                Map.entry("condition", "verifySignature === true"), // Show only if switch is on
                Map.entry("required", true), // Required if verifySignature is true
                Map.entry("helperText", "The key used for HMAC (HS256/HS384/HS512) signature.")
        ));


        verificationSection.put("inputs", verificationInputs);
        sections.add(verificationSection);


        // --- Section 3: Decoded Output ---
        Map<String, Object> outputSection = new HashMap<>();
        outputSection.put("id", "decodedOutput");
        outputSection.put("label", "Decoded Token");
        outputSection.put("condition", "success === true && typeof headerJson !== 'undefined'"); // Show only on successful parse

        List<Map<String, Object>> outputs = new ArrayList<>();

        // Header Output
        outputs.add(Map.ofEntries(
                Map.entry("id", "headerJson"), // Matches key in response map
                Map.entry("label", "Header (Decoded)"),
                Map.entry("type", "json"), // Use JSON type
                Map.entry("buttons", List.of("copy"))
        ));

        // Payload Output
        outputs.add(Map.ofEntries(
                Map.entry("id", "payloadJson"), // Matches key in response map
                Map.entry("label", "Payload (Decoded)"),
                Map.entry("type", "json"),
                Map.entry("buttons", List.of("copy"))
        ));

        // Signature Output
        outputs.add(Map.ofEntries(
                Map.entry("id", "signature"), // Matches key in response map
                Map.entry("label", "Signature (Raw)"),
                Map.entry("type", "text"),
                Map.entry("monospace", true),
                Map.entry("multiline", true),
                Map.entry("rows", 2),
                Map.entry("buttons", List.of("copy")),
                Map.entry("condition", "typeof signature !== 'undefined' && signature !== ''") // Only show if signature exists
        ));

        // Signature Verification Status Output
        outputs.add(Map.ofEntries(
                Map.entry("id", "signatureStatus"), // Matches key in response map
                Map.entry("label", "Signature Verification"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof signatureStatus !== 'undefined'") // Show if verification was attempted
                // Style could be added dynamically based on status in frontend if needed
        ));

        outputSection.put("outputs", outputs);
        sections.add(outputSection);


        // --- Section 4: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Show only on failure

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", "errorMessage"), // Specific ID for the error message
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error") // Hint for styling
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    /**
     * Processes the input parameters (using IDs from the new format)
     * to parse (and optionally validate) a JWT token.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        try {
            // Get parameters using NEW IDs
            String jwtToken = getStringParam(input, "jwtToken", null); // Required
            boolean verifySignature = getBooleanParam(input);
            String secret = getStringParam(input, "secret", ""); // Not required unless verifySignature is true

            if (verifySignature && secret.isEmpty()) {
                throw new IllegalArgumentException("Secret Key is required when 'Verify Signature' is checked.");
            }

            // Perform parsing and optional validation
            return parseAndValidateJWT(jwtToken, verifySignature, secret);

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing JWT request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error during JWT processing: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Parses and optionally validates the JWT token.
     *
     * @param jwtToken The JWT string.
     * @param verify Whether to attempt signature verification.
     * @param secret The secret key (only used if verify is true).
     * @return A map containing the parsed results or an error.
     */
    private Map<String, Object> parseAndValidateJWT(String jwtToken, boolean verify, String secret) {
        Map<String, Object> result = new LinkedHashMap<>(); // Preserve order
        String errorOutputId = "errorMessage";

        try {
            // Basic format check
            if (!JWT_PATTERN.matcher(jwtToken).matches()) {
                throw new IllegalArgumentException("Invalid JWT format (must have Header.Payload[.Signature])");
            }

            String[] parts = jwtToken.split("\\.", -1); // Include trailing empty strings if signature is empty
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException("Invalid JWT format (expected 2 or 3 parts separated by dots).");
            }
            if (parts[0].isEmpty() || parts[1].isEmpty()) {
                throw new IllegalArgumentException("Invalid JWT format (header or payload part is empty).");
            }


            // Decode Header
            Map<String, Object> header;
            String headerJson;
            try {
                headerJson = decodeBase64Url(parts[0]);
                header = parseJson(headerJson);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to decode or parse JWT header: " + e.getMessage());
            }

            // Decode Payload
            Map<String, Object> payload;
            String payloadJson;
            try {
                payloadJson = decodeBase64Url(parts[1]);
                payload = parseJson(payloadJson);
                processTimestamps(payload); // Add formatted timestamps
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to decode or parse JWT payload: " + e.getMessage());
            }

            // Signature
            String signature = (parts.length == 3) ? parts[2] : "";


            // --- Signature Verification (Optional) ---
            String signatureStatus = "Not Attempted";
            boolean signatureValid; // Assume invalid until proven otherwise

            if (verify) {
                if (secret == null || secret.isEmpty()) {
                    // This case is caught by input validation in process() now
                    signatureStatus = "Verification skipped (Secret key missing)";
                }
                else if (signature.isEmpty()) {
                    signatureStatus = "Verification failed (Token has no signature)";
                }
                else {
                    String algorithm = (String) header.get("alg");
                    if (algorithm == null || algorithm.isEmpty()) {
                        signatureStatus = "Verification failed (Algorithm ('alg') missing in header)";
                    } else {
                        try {
                            String dataToVerify = parts[0] + "." + parts[1];
                            signatureValid = verifySignature(dataToVerify, signature, secret, algorithm);
                            signatureStatus = signatureValid ? "Verified Successfully" : "Verification Failed (Invalid Signature)";
                        } catch (UnsupportedOperationException unsupEx) {
                            signatureStatus = "Verification Failed (" + unsupEx.getMessage() + ")";
                            // Mark as invalid if unsupported
                        } catch (Exception sigEx) {
                            System.err.println("Error during signature verification: " + sigEx.getMessage());
                            signatureStatus = "Verification Failed (Error during check)";
                        }
                    }
                }
            }

            // --- Build Success Result ---
            result.put("success", true);
            // Keys must match output field IDs in getMetadata()
            result.put("headerJson", header); // Return parsed JSON object
            result.put("payloadJson", payload); // Return parsed JSON object
            if (!signature.isEmpty()) {
                result.put("signature", signature); // Include raw signature if present
            }
            result.put("signatureStatus", signatureStatus); // Always include status

            return result;

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) {
            // Catch unexpected issues during parsing/decoding
            System.err.println("Unexpected error during JWT parsing: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Failed to parse token: " + e.getMessage());
        }
    }


    /**
     * Verify HMAC or other signature types (placeholder/example).
     * IMPORTANT: Replace with a robust library like java-jwt, jjwt, or Nimbus JOSE+JWT for production.
     * This basic implementation is NOT secure or complete.
     */
    private boolean verifySignature(String dataToVerify, String signature, String secret, String algorithm) {
        System.out.println("Attempting verification for algo: " + algorithm);
        return switch (algorithm) {
            case "HS256", "HS384", "HS512" -> {
                // TODO: Implement secure HMAC verification using a proper library
                // Example concept (NOT PRODUCTION READY):
                // Mac mac = Mac.getInstance("Hmac" + algorithm.substring(1)); // e.g., HmacSHA256
                // SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "Hmac" + algorithm.substring(1));
                // mac.init(secretKey);
                // byte[] calculatedSignatureBytes = mac.doFinal(dataToVerify.getBytes(StandardCharsets.UTF_8));
                // byte[] providedSignatureBytes = Base64.getUrlDecoder().decode(signature);
                // return MessageDigest.isEqual(calculatedSignatureBytes, providedSignatureBytes);

                // For now, indicate it's not implemented securely
                System.err.println("Warning: Secure HMAC verification for " + algorithm + " requires a dedicated JWT library. Returning false.");
                throw new UnsupportedOperationException("Secure verification for " + algorithm + " not implemented in this basic parser.");
            }
            // return false;

            case "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512" -> {
                // TODO: Implement RSA/ECDSA verification (requires public key and library)
                System.err.println("Warning: Verification for asymmetric algorithm " + algorithm + " requires a public key and library.");
                throw new UnsupportedOperationException("Verification for " + algorithm + " not implemented.");
            }
            // return false;

            case "none" -> signature.isEmpty(); // Valid if signature part is actually empty

            default -> {
                System.err.println("Unsupported algorithm for verification: " + algorithm);
                throw new UnsupportedOperationException("Unsupported algorithm for verification: " + algorithm);
                // return false;
            }
        };
    }


    /** Decodes Base64URL string. */
    private String decodeBase64Url(String base64Url) {
        // Replace URL-safe chars, add padding if needed (Base64.getUrlDecoder handles this)
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Url);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    /** Parses JSON string to Map. */
    private Map<String, Object> parseJson(String jsonStr) {
        try {
            // Use TypeReference to get Map<String, Object>
            return objectMapper.readValue(jsonStr, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            // Throw a more specific runtime exception if parsing fails
            throw new RuntimeException("Invalid JSON structure: " + e.getMessage(), e);
        }
    }

    /** Adds formatted timestamp strings (_formatted) to payload map. */
    private void processTimestamps(Map<String, Object> payload) {
        String[] timeFields = {"iat", "exp", "nbf", "auth_time"};
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // Include timezone indicator
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Assume timestamps are UTC epoch seconds

        for (String field : timeFields) {
            Object value = payload.get(field);
            if (value instanceof Number) { // Check if it's a number
                try {
                    long timestampSeconds = ((Number) value).longValue();
                    Date date = new Date(timestampSeconds * 1000); // Convert seconds to milliseconds
                    payload.put(field + "_formatted", dateFormat.format(date)); // Add formatted version
                } catch (Exception e) {
                    System.err.println("Could not format timestamp for field '" + field + "': " + value);
                    payload.put(field + "_formatted", "Invalid Timestamp");
                }
            }
        }
    }

    // Null default indicates required
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString(); // Allow empty string for secret initially
        if (strValue.isEmpty() && defaultValue == null && !"secret".equals(key)) { // Secret can be empty unless verify=true
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return strValue;
    }

    // Null default indicates required
    private boolean getBooleanParam(Map<String, Object> input) {
        Object value = input.get("verifySignature");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) { // Handle string "true" case insensitive
            return "true".equalsIgnoreCase(value.toString());
        }
        return false;
    }
}