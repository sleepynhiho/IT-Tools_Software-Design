package kostovite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*; // Import Base64 etc.

// Assuming PluginInterface is standard
public class HashText implements PluginInterface { // Class name should match file name

    // Keep supported algorithms list for backend logic
    private static final List<String> HASH_ALGORITHMS_FOR_PROCESSING = List.of(
            "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512"
            // Add "SHA3-...", "RIPEMD160" if BouncyCastle is available and registered
    );
    // Add specific IDs from the JSON output list that need BouncyCastle
    // Use algo name for check
    private static final List<String> BOUNCY_CASTLE_ALG_NAMES = List.of(
            "SHA3-256", // Example SHA3 variant
            "RIPEMD160"
    );


    /**
     * Internal plugin name - should match the top-level 'id' in metadata
     */
    @Override
    public String getName() {
        return "HashText"; // Match the ID from the target JSON
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("HashText Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            // Use IDs from the NEW metadata format for testing process method
            params.put("inputText", "Hello World");
            params.put("digestEncoding", "base16"); // Maps to hex

            Map<String, Object> result = process(params);
            System.out.println("Test Hash Result: " + result);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id attributes)
     * matching the structure of the provided JSON example for HashText.
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format - matching JSON example) ---
        metadata.put("id", "HashText");
        metadata.put("name", "Hash text");
        metadata.put("description", "Calculate cryptographic hashes (MD5, SHA-1, SHA-2 family, etc.).");
        metadata.put("icon", "Fingerprint");
        metadata.put("category", "Crypto");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", true); // Enable dynamic hashing

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Config (Contains both Inputs and Outputs from JSON example) ---
        Map<String, Object> configSection = new HashMap<>();
        configSection.put("id", "config"); // Section ID from JSON
        configSection.put("label", "");    // Section Label is empty in JSON

        // --- Inputs for the Section ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "inputText"),
                Map.entry("label", "Your text to Hash:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "Your string to hash..."),
                Map.entry("multiline", true),
                Map.entry("rows", 5),
                Map.entry("required", true), // Keep required flag
                Map.entry("containerId", "main") // From JSON
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "digestEncoding"),
                Map.entry("label", "Digest encoding"),
                Map.entry("type", "select"),
                Map.entry("default", "base16"), // Map 'hex' to 'base16' for default value
                Map.entry("required", false),
                Map.entry("containerId", "main"),
                Map.entry("options", List.of( // Options structured for new format
                        Map.of("value", "base2", "label", "Binary (base 2)"),
                        Map.of("value", "base16", "label", "Hexadecimal (base 16)"),
                        Map.of("value", "base64", "label", "Base64 (base 64)"),
                        Map.of("value", "base64url", "label", "Base64url (base 64 with url safe chars)")
                ))
        ));
        configSection.put("inputs", inputs);

        // --- Outputs for the Section ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(createHashOutputFieldNewFormat("md5", "MD5"));
        outputs.add(createHashOutputFieldNewFormat("sha1", "SHA-1"));
        outputs.add(createHashOutputFieldNewFormat("sha256", "SHA-256"));
        outputs.add(createHashOutputFieldNewFormat("sha224", "SHA-224"));
        outputs.add(createHashOutputFieldNewFormat("sha512", "SHA-512"));
        outputs.add(createHashOutputFieldNewFormat("sha384", "SHA-384"));
        outputs.add(createHashOutputFieldNewFormat("sha3", "SHA-3"));
        outputs.add(createHashOutputFieldNewFormat("ripemd160", "RIPEMD-160"));
        configSection.put("outputs", outputs);

        sections.add(configSection);


        // --- Error Section (Standard for New Format) ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Condition to show section

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", "errorMessage"), // ID for the error message field
                Map.entry("label", "Details"),   // Label for the error field
                Map.entry("type", "text"),
                Map.entry("style", "error") // Style hint
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create hash output field definitions for NEW format
    private Map<String, Object> createHashOutputFieldNewFormat(String id, String label) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id); // Use 'id' for new format
        field.put("label", label); // Use 'label' for new format
        field.put("type", "text");
        field.put("monospace", true);
        field.put("buttons", List.of("copy"));
        field.put("buttonPlacement", Map.of("copy", "inside"));
        field.put("containerId", "main");
        // Condition to show only if the value exists in the result map AND is not N/A or Error
        field.put("condition", "typeof " + id + " !== 'undefined' && " + id + " !== 'N/A (Requires BouncyCastle)' && " + id + " !== 'N/A' && " + id + " !== 'Error'");
        return field;
    }


    /**
     * Processes the input text to calculate ALL supported hashes.
     * Reads input keys matching the 'id' attributes from the NEW metadata format.
     * Returns results keyed by the 'id' attributes defined in the output metadata.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        try {
            // Get parameters using NEW metadata 'id' keys
            String text = getStringParam(input, "inputText", ""); // Allow empty string
            String digestEncoding = getStringParam(input, "digestEncoding", "base16"); // Default base16/hex

            String outputFormat; // Map frontend selection to backend format logic
            boolean useUppercaseHex = false; // Flag for hex casing (not currently configurable via UI)

            outputFormat = switch (digestEncoding.toLowerCase()) {
                case "base64" -> "base64";
                case "base64url" -> "base64url";
                case "base2" -> "bin"; // Map to internal 'bin'
                default -> "hex";      // "base16" maps to hex
            };

            Map<String, Object> result = new HashMap<>();
            result.put("success", true); // Assume success unless a specific error occurs later

            List<String> algorithmsToProcess = new ArrayList<>(HASH_ALGORITHMS_FOR_PROCESSING);
            // Add BC algos if needed...
            // boolean bcAvailable = isBouncyCastleAvailable();
            boolean bcAvailable = false; // Assume false without check/dependency

            for(String algorithm : algorithmsToProcess) {
                String outputKey = mapAlgoNameToId(algorithm); // Get the output ID (e.g., "sha256")
                if (outputKey == null) continue;

                try {
                    byte[] hashBytes = calculateHash(text, algorithm);
                    String formattedHash = formatHash(hashBytes, outputFormat, useUppercaseHex);
                    result.put(outputKey, formattedHash); // Use the ID as the key
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Algorithm not supported/available: " + algorithm + " for key " + outputKey);
                    result.put(outputKey, "N/A");
                } catch (Exception e) {
                    System.err.println("Error calculating hash for " + algorithm + ": " + e.getMessage());
                    result.put(outputKey, "Error");
                }
            }

            // --- Handle BC Algos ---
            String sha3OutputKey = "sha3";
            String ripemdOutputKey = "ripemd160";
            if(bcAvailable) {
                try { result.put(sha3OutputKey, formatHash(calculateHash(text, "SHA3-256"), outputFormat, useUppercaseHex)); }
                catch (NoSuchAlgorithmException e) { result.put(sha3OutputKey, "Not Available"); }
                catch (Exception e) { result.put(sha3OutputKey, "Error"); }

                try { result.put(ripemdOutputKey, formatHash(calculateHash(text, "RIPEMD160"), outputFormat, useUppercaseHex)); }
                catch (NoSuchAlgorithmException e) { result.put(ripemdOutputKey, "Not Available"); }
                catch (Exception e) { result.put(ripemdOutputKey, "Error"); }
            } else {
                result.put(sha3OutputKey, "N/A (Requires BouncyCastle)");
                result.put(ripemdOutputKey, "N/A (Requires BouncyCastle)");
            }
            //--- End BC Algos ---

            return result;

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing hash request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Helper Methods (Unchanged from previous refactor)
    // ========================================================================

    private String formatHash(byte[] hashBytes, String format, boolean useUppercaseHex) {
        if (hashBytes == null) return "Error";
        return switch (format) {
            case "base64" -> Base64.getEncoder().encodeToString(hashBytes);
            case "base64url" -> Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
            case "bin" -> bytesToBinary(hashBytes);
            default -> bytesToHex(hashBytes, useUppercaseHex); // "hex"
        };
    }

    private String mapAlgoNameToId(String algorithmName) {
        return switch(algorithmName) {
            case "MD5" -> "md5";
            case "SHA-1" -> "sha1";
            case "SHA-224" -> "sha224";
            case "SHA-256" -> "sha256";
            case "SHA-384" -> "sha384";
            case "SHA-512" -> "sha512";
            case "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512" -> "sha3";
            case "RIPEMD160" -> "ripemd160";
            default -> null;
        };
    }

    private byte[] calculateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes, boolean upperCase) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        String format = upperCase ? "%02X" : "%02x";
        for (byte b : bytes) {
            hexString.append(String.format(format, b));
        }
        return hexString.toString();
    }

    private String bytesToBinary(byte[] bytes) {
        StringBuilder binaryString = new StringBuilder(bytes.length * 9);
        for (int i = 0; i < bytes.length; i++) {
            String bin = Integer.toBinaryString(0xFF & bytes[i]);
            while (bin.length() < 8) { bin = "0" + bin; }
            binaryString.append(bin);
            if (i < bytes.length - 1) binaryString.append(' ');
        }
        return binaryString.toString();
    }

    // Null default indicates required (or empty string allowed if defaultValue="")
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString();
        // Allow empty string for hashing, only throw if required and truly null
        // if (strValue.isEmpty() && defaultValue == null) {
        //      throw new IllegalArgumentException("Missing required parameter: " + key);
        // }
        return strValue;
    }
}