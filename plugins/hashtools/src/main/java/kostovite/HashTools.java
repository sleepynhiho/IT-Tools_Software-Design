package kostovite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.*;

// Assuming PluginInterface is standard
public class HashTools implements PluginInterface {
    // Define supported algorithms (using standard Java names)
    // Using LinkedHashMap to maintain display order
    private static final Map<String, String> SUPPORTED_ALGORITHMS = new LinkedHashMap<>();
    static {
        SUPPORTED_ALGORITHMS.put("MD5", "MD5 (Insecure)"); // Mark insecure
        SUPPORTED_ALGORITHMS.put("SHA-1", "SHA-1 (Insecure)"); // Mark insecure
        SUPPORTED_ALGORITHMS.put("SHA-224", "SHA-224");
        SUPPORTED_ALGORITHMS.put("SHA-256", "SHA-256 (Recommended)");
        SUPPORTED_ALGORITHMS.put("SHA-384", "SHA-384");
        SUPPORTED_ALGORITHMS.put("SHA-512", "SHA-512");
        // Add these if Bouncy Castle is included and registered
         SUPPORTED_ALGORITHMS.put("SHA3-224", "SHA3-224");
         SUPPORTED_ALGORITHMS.put("SHA3-256", "SHA3-256");
         SUPPORTED_ALGORITHMS.put("SHA3-384", "SHA3-384");
         SUPPORTED_ALGORITHMS.put("SHA3-512", "SHA3-512");
         SUPPORTED_ALGORITHMS.put("RIPEMD160", "RIPEMD-160");
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "HashTools";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("HashTools Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("algorithm", "SHA-256"); // Use new ID
            params.put("inputText", "Hello World"); // Use new ID
            params.put("outputFormat", "hex"); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Test Hash Result: " + result);

            params.put("algorithm", "MD5");
            result = process(params);
            System.out.println("Test Hash Result (MD5): " + result);

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
        metadata.put("id", "HashTools"); // ID matches class name
        metadata.put("name", "Hash Calculator"); // User-facing name
        metadata.put("description", "Calculate cryptographic hashes (MD5, SHA-1, SHA-2 family, etc.).");
        metadata.put("icon", "Fingerprint");
        metadata.put("category", "Crypto");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input and Configuration ---
        Map<String, Object> configSection = new HashMap<>();
        configSection.put("id", "config");
        configSection.put("label", "Input & Settings");

        List<Map<String, Object>> configInputs = new ArrayList<>();

        // Input Text
        configInputs.add(Map.ofEntries(
                Map.entry("id", "inputText"),
                Map.entry("label", "Text to Hash:"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 5),
                Map.entry("placeholder", "Enter text here..."),
                Map.entry("required", true) // Text is required
        ));

        // Algorithm Selection
        List<Map<String, String>> algoOptions = new ArrayList<>();
        SUPPORTED_ALGORITHMS.forEach((key, label) -> algoOptions.add(Map.of("value", key, "label", label)));
        configInputs.add(Map.ofEntries(
                Map.entry("id", "algorithm"),
                Map.entry("label", "Hash Algorithm:"),
                Map.entry("type", "select"),
                Map.entry("options", algoOptions),
                Map.entry("default", "SHA-256"), // Default to recommended
                Map.entry("required", true),
                Map.entry("helperText", "MD5 and SHA-1 are insecure for many uses.")
        ));

        // Output Format Selection
        configInputs.add(Map.ofEntries(
                Map.entry("id", "outputFormat"),
                Map.entry("label", "Output Format:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "hex", "label", "Hexadecimal (lowercase)"),
                        Map.of("value", "hexUpper", "label", "Hexadecimal (UPPERCASE)"),
                        Map.of("value", "base64", "label", "Base64")
                )),
                Map.entry("default", "hex"),
                Map.entry("required", false) // Default to hex if omitted
        ));

        // Optional: Input Encoding (Defaulting to UTF-8 in backend)

        configSection.put("inputs", configInputs);
        sections.add(configSection);


        // --- Section 2: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Hash Result");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Echo Input Text
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "inputEcho"), // Matches key in response map
                Map.entry("label", "Original Input (Preview)"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 2),
                Map.entry("condition", "typeof inputEcho !== 'undefined'")
        ));

        // Algorithm Used
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "algorithmUsed"), // Matches key in response map
                Map.entry("label", "Algorithm"),
                Map.entry("type", "text"),
                Map.entry("condition", "typeof algorithmUsed !== 'undefined'")
        ));

        // Hash Result
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "hashResult"), // Matches key in response map
                Map.entry("label", "Calculated Hash"),
                Map.entry("type", "text"),
                Map.entry("multiline", true), // Allow wrapping for long hashes
                Map.entry("rows", 3),
                Map.entry("monospace", true), // Essential for hashes
                Map.entry("buttons", List.of("copy")),
                Map.entry("condition", "typeof hashResult !== 'undefined'")
        ));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 3: Error Display ---
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
     * to calculate the selected hash.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        try {
            // Get parameters using NEW IDs
            String algorithm = getStringParam(input, "algorithm", "SHA-256"); // Default if missing
            String text = getStringParam(input, "inputText", null); // Required
            String outputFormat = getStringParam(input, "outputFormat", "hex"); // Default hex
            // String encoding = getStringParam(input, "encoding", "UTF-8"); // Default UTF-8 if input added

            if (text == null) { // getStringParam throws if required and null/empty
                throw new IllegalArgumentException("Input text cannot be empty.");
            }

            // Validate algorithm
            if (!SUPPORTED_ALGORITHMS.containsKey(algorithm)) {
                // Check if it's a BouncyCastle one *if* BC is loaded
                // If not checking for BC:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }

            // Calculate hash
            byte[] hashBytes = calculateHash(text, algorithm);

            // Format output
            String formattedHash = switch (outputFormat.toLowerCase()) {
                case "hexupper" -> bytesToHex(hashBytes, true); // Uppercase hex
                case "base64" -> Base64.getEncoder().encodeToString(hashBytes);
                default -> bytesToHex(hashBytes, false); // Lowercase hex
            };

            // Build success result map matching NEW output field IDs
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("inputEcho", text.length() > 100 ? text.substring(0, 97) + "..." : text); // Preview long text
            result.put("algorithmUsed", algorithm);
            result.put("hashResult", formattedHash);

            return result;

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hashing error: " + e.getMessage());
            return Map.of("success", false, errorOutputId, "Algorithm not available: " + e.getMessage() + ". (Maybe BouncyCastle provider is needed?)");
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing hash request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Calculates the hash of the input string using the specified algorithm.
     *
     * @param input     The string to hash.
     * @param algorithm The standard algorithm name (e.g., "SHA-256", "MD5").
     * @return The raw byte array of the hash.
     * @throws NoSuchAlgorithmException If the algorithm is not supported by the security providers.
     */
    private byte[] calculateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        // Consider selected encoding here if the input field is added
        // Charset charset = Charset.forName(encoding); // e.g., StandardCharsets.UTF_8
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes     The byte array.
     * @param upperCase True for uppercase hex (A-F), false for lowercase (a-f).
     * @return The hexadecimal string representation.
     */
    private String bytesToHex(byte[] bytes, boolean upperCase) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        String format = upperCase ? "%02X" : "%02x";
        for (byte b : bytes) {
            hexString.append(String.format(format, b));
        }
        return hexString.toString();
    }

    // Null default indicates required
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        // Allow empty string for hashing
        // Only throw if required and truly missing (null), not if empty string provided
        return value.toString();
    }
}