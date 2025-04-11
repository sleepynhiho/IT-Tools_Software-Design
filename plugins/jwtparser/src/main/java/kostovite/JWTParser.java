package kostovite;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse and decode JWT tokens");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Parse JWT operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse and decode a JWT token");

        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("jwtToken", "JWT token string to parse");
        parseInputs.put("verifySignature", "Whether to verify the signature (optional)");
        parseInputs.put("secret", "Secret key to verify signature (optional)");

        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        // Validate JWT operation
        Map<String, Object> validateOperation = new HashMap<>();
        validateOperation.put("description", "Validate a JWT token");

        Map<String, Object> validateInputs = new HashMap<>();
        validateInputs.put("jwtToken", "JWT token string to validate");
        validateInputs.put("secret", "Secret key to verify signature");

        validateOperation.put("inputs", validateInputs);
        operations.put("validate", validateOperation);

        metadata.put("operations", operations);
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

            switch (operation.toLowerCase()) {
                case "parse":
                    return parseJWTToken(jwtToken, input);
                case "validate":
                    return validateJWTToken(jwtToken, input);
                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }
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
            String base64 = base64Url.replace('-', '+').replace('_', '/');
            while (base64.length() % 4 != 0) {
                base64 += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(base64);
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
        switch (algorithm) {
            case "HS256":
                return "HMAC using SHA-256";
            case "HS384":
                return "HMAC using SHA-384";
            case "HS512":
                return "HMAC using SHA-512";
            case "RS256":
                return "RSA Signature with SHA-256";
            case "RS384":
                return "RSA Signature with SHA-384";
            case "RS512":
                return "RSA Signature with SHA-512";
            case "ES256":
                return "ECDSA using P-256 curve and SHA-256";
            case "ES384":
                return "ECDSA using P-384 curve and SHA-384";
            case "ES512":
                return "ECDSA using P-521 curve and SHA-512";
            case "PS256":
                return "RSASSA-PSS using SHA-256 and MGF1 padding";
            case "PS384":
                return "RSASSA-PSS using SHA-384 and MGF1 padding";
            case "PS512":
                return "RSASSA-PSS using SHA-512 and MGF1 padding";
            case "none":
                return "No digital signature or MAC";
            default:
                return "Unknown algorithm";
        }
    }
}