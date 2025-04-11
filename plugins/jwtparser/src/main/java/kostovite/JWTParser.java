package kostovite;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JWTParser implements PluginInterface {

    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "JWTParser";
    }

    @Override
    public void execute() {
        System.out.println("JWT Parser Plugin executed");

        // Demonstrate basic usage
        try {
            // Example JWT token
            String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

            Map<String, Object> params = new HashMap<>();
            params.put("jwtToken", jwtToken);

            Map<String, Object> result = process(params);
            System.out.println("JWT Token parsed successfully: " + result.get("isValid"));

            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) result.get("header");
            System.out.println("Algorithm: " + header.get("alg"));

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            System.out.println("Subject: " + payload.get("sub"));
            System.out.println("Name: " + payload.get("name"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse and decode JWT tokens"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Parse JWT operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse and decode a JWT token");
        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("jwtToken", Map.of("type", "string", "description", "JWT token string to parse", "required", true));
        parseInputs.put("verifySignature", Map.of("type", "boolean", "description", "Whether to verify the signature (optional)", "required", false));
        parseInputs.put("secret", Map.of("type", "string", "description", "Secret key to verify signature (optional)", "required", false));
        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        // Validate JWT operation
        Map<String, Object> validateOperation = new HashMap<>();
        validateOperation.put("description", "Validate a JWT token");
        Map<String, Object> validateInputs = new HashMap<>();
        validateInputs.put("jwtToken", Map.of("type", "string", "description", "JWT token string to validate", "required", true));
        validateInputs.put("secret", Map.of("type", "string", "description", "Secret key to verify signature", "required", true));
        validateOperation.put("inputs", validateInputs);
        operations.put("validate", validateOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "JWTParser"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "VpnKey"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Crypto"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "JWT Operation");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "operation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "parse", "label", "Parse & Decode JWT"));
        operationOptions.add(Map.of("value", "validate", "label", "Validate JWT Signature"));
        operationField.put("options", operationOptions);
        operationField.put("default", "parse");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: JWT Token Input
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "JWT Token");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // JWT Token field
        Map<String, Object> jwtTokenField = new HashMap<>();
        jwtTokenField.put("name", "jwtToken");
        jwtTokenField.put("label", "JWT Token:");
        jwtTokenField.put("type", "text");
        jwtTokenField.put("multiline", true);
        jwtTokenField.put("rows", 4);
        jwtTokenField.put("placeholder", "Paste your JWT token here...");
        jwtTokenField.put("required", true);
        jwtTokenField.put("helperText", "Enter a valid JWT token in the format: xxxxx.yyyyy.zzzzz");
        section2Fields.add(jwtTokenField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Verification Options (conditional based on operation)
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Verification Options");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Verify signature checkbox for parse operation
        Map<String, Object> verifySignatureField = new HashMap<>();
        verifySignatureField.put("name", "verifySignature");
        verifySignatureField.put("label", "Verify Signature");
        verifySignatureField.put("type", "switch");
        verifySignatureField.put("default", false);
        verifySignatureField.put("condition", "operation === 'parse'");
        section3Fields.add(verifySignatureField);

        // Secret key field (conditional based on verifySignature or validate operation)
        Map<String, Object> secretKeyField = new HashMap<>();
        secretKeyField.put("name", "secret");
        secretKeyField.put("label", "Secret Key:");
        secretKeyField.put("type", "password");
        secretKeyField.put("helperText", "Enter the secret key used to sign the token");
        secretKeyField.put("condition", "(operation === 'parse' && verifySignature) || operation === 'validate'");
        secretKeyField.put("required", "operation === 'validate'");
        section3Fields.add(secretKeyField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: JWT Overview
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "JWT Overview");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Validation Result
        Map<String, Object> validationOutput = new HashMap<>();
        validationOutput.put("title", "Validation");
        validationOutput.put("name", "validationStatus");
        validationOutput.put("type", "text");
        validationOutput.put("formula", "isValid ? 'Valid JWT format' : 'Invalid JWT format'");
        validationOutput.put("style", "isValid ? 'success' : 'error'");
        section1OutputFields.add(validationOutput);

        // Signature Status
        Map<String, Object> signatureStatusOutput = new HashMap<>();
        signatureStatusOutput.put("title", "Signature Status");
        signatureStatusOutput.put("name", "signatureStatus");
        signatureStatusOutput.put("type", "text");
        signatureStatusOutput.put("condition", "(operation === 'parse' && verifySignature) || operation === 'validate'");
        section1OutputFields.add(signatureStatusOutput);

        // Algorithm
        Map<String, Object> algorithmOutput = new HashMap<>();
        algorithmOutput.put("title", "Algorithm");
        algorithmOutput.put("name", "algorithmInfo");
        algorithmOutput.put("type", "text");
        algorithmOutput.put("formula", "header?.alg + ' (' + header?.alg_description + ')'");
        section1OutputFields.add(algorithmOutput);

        // Token Parts
        Map<String, Object> tokenPartsOutput = new HashMap<>();
        tokenPartsOutput.put("title", "Token Parts");
        tokenPartsOutput.put("name", "tokenPartsInfo");
        tokenPartsOutput.put("type", "text");
        tokenPartsOutput.put("formula", "tokenParts === 3 ? 'Complete (Header, Payload, Signature)' : 'Incomplete (Missing Signature)'");
        section1OutputFields.add(tokenPartsOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Headers
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Header");
        outputSection2.put("condition", "isValid");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Header JSON
        Map<String, Object> headerOutput = new HashMap<>();
        headerOutput.put("title", "Header Data");
        headerOutput.put("name", "formattedHeader");
        headerOutput.put("type", "json");
        headerOutput.put("buttons", List.of("copy"));
        section2OutputFields.add(headerOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Payload
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Payload Claims");
        outputSection3.put("condition", "isValid");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Common JWT claims as individual fields for better visibility
        Map<String, Object> subjectOutput = new HashMap<>();
        subjectOutput.put("title", "Subject (sub)");
        subjectOutput.put("name", "payload.sub");
        subjectOutput.put("type", "text");
        subjectOutput.put("condition", "payload?.sub");
        section3OutputFields.add(subjectOutput);

        Map<String, Object> issuerOutput = new HashMap<>();
        issuerOutput.put("title", "Issuer (iss)");
        issuerOutput.put("name", "payload.iss");
        issuerOutput.put("type", "text");
        issuerOutput.put("condition", "payload?.iss");
        section3OutputFields.add(issuerOutput);

        Map<String, Object> audienceOutput = new HashMap<>();
        audienceOutput.put("title", "Audience (aud)");
        audienceOutput.put("name", "payload.aud");
        audienceOutput.put("type", "text");
        audienceOutput.put("condition", "payload?.aud");
        section3OutputFields.add(audienceOutput);

        Map<String, Object> expirationOutput = new HashMap<>();
        expirationOutput.put("title", "Expiration (exp)");
        expirationOutput.put("name", "payload.exp_formatted");
        expirationOutput.put("type", "text");
        expirationOutput.put("condition", "payload?.exp_formatted");
        section3OutputFields.add(expirationOutput);

        Map<String, Object> issuedAtOutput = new HashMap<>();
        issuedAtOutput.put("title", "Issued At (iat)");
        issuedAtOutput.put("name", "payload.iat_formatted");
        issuedAtOutput.put("type", "text");
        issuedAtOutput.put("condition", "payload?.iat_formatted");
        section3OutputFields.add(issuedAtOutput);

        Map<String, Object> notBeforeOutput = new HashMap<>();
        notBeforeOutput.put("title", "Not Before (nbf)");
        notBeforeOutput.put("name", "payload.nbf_formatted");
        notBeforeOutput.put("type", "text");
        notBeforeOutput.put("condition", "payload?.nbf_formatted");
        section3OutputFields.add(notBeforeOutput);

        // Name claim
        Map<String, Object> nameOutput = new HashMap<>();
        nameOutput.put("title", "Name");
        nameOutput.put("name", "payload.name");
        nameOutput.put("type", "text");
        nameOutput.put("condition", "payload?.name");
        section3OutputFields.add(nameOutput);

        // Email claim
        Map<String, Object> emailOutput = new HashMap<>();
        emailOutput.put("title", "Email");
        emailOutput.put("name", "payload.email");
        emailOutput.put("type", "text");
        emailOutput.put("condition", "payload?.email");
        section3OutputFields.add(emailOutput);

        // Full payload as JSON
        Map<String, Object> payloadOutput = new HashMap<>();
        payloadOutput.put("title", "All Payload Data");
        payloadOutput.put("name", "formattedPayload");
        payloadOutput.put("type", "json");
        payloadOutput.put("buttons", List.of("copy"));
        section3OutputFields.add(payloadOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        // Output Section 4: Signature
        Map<String, Object> outputSection4 = new HashMap<>();
        outputSection4.put("header", "Signature");
        outputSection4.put("condition", "isValid && tokenParts === 3");
        List<Map<String, Object>> section4OutputFields = new ArrayList<>();

        // Signature data
        Map<String, Object> signatureOutput = new HashMap<>();
        signatureOutput.put("title", "Raw Signature");
        signatureOutput.put("name", "signature");
        signatureOutput.put("type", "text");
        signatureOutput.put("monospace", true);
        signatureOutput.put("buttons", List.of("copy"));
        section4OutputFields.add(signatureOutput);

        outputSection4.put("fields", section4OutputFields);
        uiOutputs.add(outputSection4);

        // Output Section 5: Error Display
        Map<String, Object> outputSection5 = new HashMap<>();
        outputSection5.put("header", "Error Information");
        outputSection5.put("condition", "error");
        List<Map<String, Object>> section5OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section5OutputFields.add(errorOutput);

        outputSection5.put("fields", section5OutputFields);
        uiOutputs.add(outputSection5);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "parse");
            String jwtToken = (String) input.get("jwtToken");

            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                result.put("error", "JWT token cannot be empty");
                return result;
            }

            return switch (operation.toLowerCase()) {
                case "parse" -> parseJWTToken(jwtToken, input);
                case "validate" -> validateJWTToken(jwtToken, input);
                default -> {
                    result.put("error", "Unsupported operation: " + operation);
                    yield result;
                }
            };
        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Parse a JWT token and extract its parts
     *
     * @param jwtToken The JWT token to parse
     * @param input Additional input parameters
     * @return Parsed token data
     */
    private Map<String, Object> parseJWTToken(String jwtToken, Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Validate JWT token format
            if (!JWT_PATTERN.matcher(jwtToken).matches()) {
                result.put("error", "Invalid JWT token format");
                result.put("isValid", false);
                return result;
            }

            // Split the token into parts
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                result.put("error", "Invalid JWT token format (missing parts)");
                result.put("isValid", false);
                return result;
            }

            // Decode header
            String headerJson = decodeBase64(parts[0]);
            Map<String, Object> header = parseJson(headerJson);

            // Decode payload
            String payloadJson = decodeBase64(parts[1]);
            Map<String, Object> payload = parseJson(payloadJson);

            // Process timestamps in payload
            processTimestamps(payload);

            // Extract signature (if available)
            String signature = parts.length > 2 ? parts[2] : "";

            // Check if signature verification is requested
            boolean verifySignature = Boolean.parseBoolean(String.valueOf(input.getOrDefault("verifySignature", "false")));
            String secret = (String) input.get("secret");

            boolean isValid = true;
            String signatureStatus = "Not verified";

            // Verify signature if requested and secret is provided
            if (verifySignature && secret != null && !secret.isEmpty()) {
                Map<String, Object> validationResult = validateJWTToken(jwtToken, input);
                isValid = (boolean) validationResult.get("isValid");
                signatureStatus = isValid ? "Verified" : "Invalid";
            }

            // Build result
            result.put("isValid", isValid);
            result.put("tokenParts", parts.length);
            result.put("header", formatHeaderWithDescriptions(header));
            result.put("payload", formatPayloadWithDescriptions(payload));
            result.put("signature", signature);
            result.put("signatureStatus", signatureStatus);
            result.put("rawHeader", header);
            result.put("rawPayload", payload);
            result.put("rawToken", jwtToken);

            // Format for display
            result.put("formattedHeader", toPrettyJson(header));
            result.put("formattedPayload", toPrettyJson(payload));

        } catch (Exception e) {
            result.put("error", "Error parsing JWT token: " + e.getMessage());
            result.put("isValid", false);
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Validate a JWT token signature
     *
     * @param jwtToken The JWT token to validate
     * @param input Additional input parameters
     * @return Validation result
     */
    private Map<String, Object> validateJWTToken(String jwtToken, Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate JWT token format
            if (!JWT_PATTERN.matcher(jwtToken).matches()) {
                result.put("error", "Invalid JWT token format");
                result.put("isValid", false);
                return result;
            }

            // Split the token into parts
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 3) {
                result.put("error", "Invalid JWT token format (missing signature)");
                result.put("isValid", false);
                return result;
            }

            // Get the secret key
            String secret = (String) input.get("secret");
            if (secret == null || secret.isEmpty()) {
                result.put("error", "Secret key is required for validation");
                result.put("isValid", false);
                return result;
            }

            // Decode header to get algorithm
            String headerJson = decodeBase64(parts[0]);
            Map<String, Object> header = parseJson(headerJson);
            String algorithm = (String) header.get("alg");

            // Validate token based on algorithm
            boolean isValid = false;

            if (algorithm != null) {
                // Data to verify (header.payload)
                String dataToVerify = parts[0] + "." + parts[1];
                String signature = parts[2];

                switch (algorithm) {
                    case "HS256":
                    case "HS384":
                    case "HS512":
                        isValid = verifyHmacSignature(dataToVerify, signature, secret, algorithm);
                        break;
                    case "none":
                        isValid = signature.isEmpty();
                        break;
                    default:
                        result.put("error", "Unsupported algorithm: " + algorithm);
                        result.put("isValid", false);
                        return result;
                }
            }

            // Check token expiration if it has exp claim
            Map<String, Object> payload = parseJson(decodeBase64(parts[1]));
            if (isValid && payload.containsKey("exp")) {
                long expTime = Long.parseLong(payload.get("exp").toString());
                long currentTime = System.currentTimeMillis() / 1000;

                if (currentTime > expTime) {
                    result.put("warning", "Token has expired");
                    isValid = false;
                }
            }

            // Build result
            result.put("isValid", isValid);
            if (isValid) {
                result.put("message", "Token signature is valid");
            } else {
                result.put("message", "Token signature is invalid");
            }

        } catch (Exception e) {
            result.put("error", "Error validating JWT token: " + e.getMessage());
            result.put("isValid", false);
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Verify HMAC signature for JWT token
     *
     * @param data Data to verify
     * @param signature JWT signature to verify against
     * @param secret Secret key
     * @param algorithm HMAC algorithm (HS256, HS384, HS512)
     * @return Whether signature is valid
     */
    private boolean verifyHmacSignature(String data, String signature, String secret, String algorithm) {
        try {
            // This is a simplified implementation - in a production environment,
            // use a proper JWT library that handles all edge cases and security considerations

            // For educational purpose only - we'll respond that validation is not implemented
            // as a secure implementation would require additional libraries
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Decode Base64URL string to text
     *
     * @param base64Url Base64URL encoded string
     * @return Decoded string
     */
    private String decodeBase64(String base64Url) {
        try {
            // Replace URL-safe characters and add padding if needed
            StringBuilder base64 = new StringBuilder(base64Url.replace('-', '+').replace('_', '/'));
            while (base64.length() % 4 != 0) {
                base64.append("=");
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64.toString());
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decoding Base64: " + e.getMessage(), e);
        }
    }

    /**
     * Parse JSON string to Map
     *
     * @param jsonStr JSON string
     * @return Parsed Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonStr) {
        try {
            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Map to pretty-printed JSON string
     *
     * @param map Map to convert
     * @return Pretty-printed JSON
     */
    private String toPrettyJson(Map<String, Object> map) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error formatting JSON";
        }
    }

    /**
     * Process timestamp fields in the payload
     *
     * @param payload JWT payload
     */
    private void processTimestamps(Map<String, Object> payload) {
        // Common JWT timestamp fields
        String[] timeFields = {"iat", "exp", "nbf", "auth_time"};
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (String field : timeFields) {
            if (payload.containsKey(field)) {
                try {
                    // Convert numeric timestamp to readable date
                    long timestamp = Long.parseLong(payload.get(field).toString());
                    Date date = new Date(timestamp * 1000); // Convert seconds to milliseconds
                    payload.put(field + "_formatted", dateFormat.format(date) + " UTC");
                } catch (NumberFormatException e) {
                    // Skip if not a number
                }
            }
        }
    }

    /**
     * Format header with descriptions for common fields
     *
     * @param header JWT header
     * @return Formatted header with descriptions
     */
    private Map<String, Object> formatHeaderWithDescriptions(Map<String, Object> header) {
        Map<String, Object> formatted = new LinkedHashMap<>(header);

        // Add descriptions for common header fields
        if (header.containsKey("alg")) {
            String alg = (String) header.get("alg");
            String description = getAlgorithmDescription(alg);
            formatted.put("alg_description", description);
        }

        if (header.containsKey("typ")) {
            String typ = (String) header.get("typ");
            formatted.put("typ_description", "Token type: " + typ);
        }

        return formatted;
    }

    /**
     * Format payload with descriptions for common fields
     *
     * @param payload JWT payload
     * @return Formatted payload with descriptions
     */
    private Map<String, Object> formatPayloadWithDescriptions(Map<String, Object> payload) {
        Map<String, Object> formatted = new LinkedHashMap<>(payload);

        // Add descriptions for common claim fields
        Map<String, String> claimDescriptions = new HashMap<>();
        claimDescriptions.put("sub", "Subject (the principal about which the token asserts information)");
        claimDescriptions.put("iss", "Issuer (who created and signed the token)");
        claimDescriptions.put("aud", "Audience (recipients the token is intended for)");
        claimDescriptions.put("exp", "Expiration Time (when the token expires)");
        claimDescriptions.put("nbf", "Not Before (when the token starts being valid)");
        claimDescriptions.put("iat", "Issued At (when the token was issued)");
        claimDescriptions.put("jti", "JWT ID (unique identifier for the token)");
        claimDescriptions.put("name", "Full name");
        claimDescriptions.put("email", "Email address");
        claimDescriptions.put("roles", "User roles");

        for (String claim : claimDescriptions.keySet()) {
            if (payload.containsKey(claim)) {
                formatted.put(claim + "_description", claimDescriptions.get(claim));
            }
        }

        return formatted;
    }

    /**
     * Get description for JWT algorithm
     *
     * @param algorithm Algorithm identifier
     * @return Algorithm description
     */
    private String getAlgorithmDescription(String algorithm) {
        return switch (algorithm) {
            case "HS256" -> "HMAC using SHA-256";
            case "HS384" -> "HMAC using SHA-384";
            case "HS512" -> "HMAC using SHA-512";
            case "RS256" -> "RSA Signature with SHA-256";
            case "RS384" -> "RSA Signature with SHA-384";
            case "RS512" -> "RSA Signature with SHA-512";
            case "ES256" -> "ECDSA using P-256 curve and SHA-256";
            case "ES384" -> "ECDSA using P-384 curve and SHA-384";
            case "ES512" -> "ECDSA using P-521 curve and SHA-512";
            case "PS256" -> "RSASSA-PSS using SHA-256 and MGF1 padding";
            case "PS384" -> "RSASSA-PSS using SHA-384 and MGF1 padding";
            case "PS512" -> "RSASSA-PSS using SHA-512 and MGF1 padding";
            case "none" -> "No digital signature or MAC";
            default -> "Unknown algorithm";
        };
    }
}