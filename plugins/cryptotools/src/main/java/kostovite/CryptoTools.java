package kostovite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; // Use SecureRandom
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException; // More specific exceptions
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException; // More specific exceptions
import java.security.InvalidAlgorithmParameterException;


// Note: Using ExtendedPluginInterface if required by your framework, otherwise use PluginInterface
public class CryptoTools implements PluginInterface { // Assuming standard PluginInterface for now

    // Supported algorithms - Map for easier lookup and clearer names
    private static final Map<String, String> SUPPORTED_ALGORITHMS_MAP = new LinkedHashMap<>(); // Preserve order
    static {
        SUPPORTED_ALGORITHMS_MAP.put("AES", "AES/GCM/NoPadding"); // Recommended: Authenticated Encryption
        SUPPORTED_ALGORITHMS_MAP.put("DES", "DES/CBC/PKCS5Padding"); // Legacy, Weak
        SUPPORTED_ALGORITHMS_MAP.put("TripleDES", "DESede/CBC/PKCS5Padding"); // Legacy
        SUPPORTED_ALGORITHMS_MAP.put("Blowfish", "Blowfish/CBC/PKCS5Padding"); // Okay, but less common
        SUPPORTED_ALGORITHMS_MAP.put("RC4", "ARCFOUR"); // Insecure, Avoid! Included for completeness based on original code.
    }

    // Key lengths in bytes corresponding to map keys above (adjust if needed)
    private static final Map<String, Integer> KEY_LENGTHS = Map.of(
            "AES", 32, // 256-bit
            "DES", 8,  // 64-bit (effectively 56-bit)
            "TripleDES", 24, // 192-bit (effectively 112 or 168-bit)
            "Blowfish", 16, // 128-bit (can range, but 16 is common)
            "RC4", 16 // 128-bit (can range)
    );

    // IV size map (bytes) - DES/3DES/Blowfish use 8, AES/GCM uses 12, RC4 uses none
    private static final Map<String, Integer> IV_LENGTHS = Map.of(
            "AES", 12, // For GCM mode
            "DES", 8,
            "TripleDES", 8,
            "Blowfish", 8,
            "RC4", 0 // RC4 does not use an IV
    );

    private static final int GCM_TAG_LENGTH = 128; // bits

    @Override
    public String getName() {
        return "CryptoTools";
    }

    @Override
    public void execute() {
        // Standalone test execution
        System.out.println("CryptoTools Plugin executed (standalone test)");
        try {
            Map<String, Object> encryptParams = new HashMap<>();
            encryptParams.put("uiOperation", "encrypt"); // Use uiOperation for testing
            encryptParams.put("algorithm", "AES");
            encryptParams.put("inputText", "This is a secret message!"); // Use new input ID
            encryptParams.put("secretKey", "correct horse battery staple"); // Use new input ID

            Map<String, Object> encryptResult = process(encryptParams);
            System.out.println("Encryption Result: " + encryptResult);

            if (encryptResult.get("success") == Boolean.TRUE) {
                Map<String, Object> decryptParams = new HashMap<>();
                decryptParams.put("uiOperation", "decrypt"); // Use uiOperation
                decryptParams.put("algorithm", "AES");
                // Use the correct output key from the encrypt result
                decryptParams.put("encryptedInput", encryptResult.get("encryptedResult")); // Use new input ID
                decryptParams.put("secretKey", "correct horse battery staple"); // Use new input ID

                Map<String, Object> decryptResult = process(decryptParams);
                System.out.println("Decryption Result: " + decryptResult);
            }

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "CryptoTools"); // ID matches class name
        metadata.put("name", "Encryption/Decryption Tools"); // User-facing name
        metadata.put("description", "Encrypt or decrypt text using various algorithms and a secret key.");
        metadata.put("icon", "Lock");
        metadata.put("category", "Security");
        metadata.put("customUI", false);
        metadata.put("accessLevel", "normal");
        // This tool requires explicit action, not dynamic updates on input change
        metadata.put("triggerUpdateOnChange", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Settings and Input ---
        Map<String, Object> settingsSection = new HashMap<>();
        settingsSection.put("id", "settings");
        settingsSection.put("label", "Settings & Input Text");

        List<Map<String, Object>> settingsInputs = new ArrayList<>();

        // Operation (Encrypt/Decrypt)
        settingsInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"), // Use ID
                Map.entry("label", "Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "encrypt", "label", "Encrypt Text"),
                        Map.of("value", "decrypt", "label", "Decrypt Text (Base64)")
                )),
                Map.entry("default", "encrypt"),
                Map.entry("required", true)
        ));

        // Algorithm Selection
        List<Map<String, String>> algoOptions = new ArrayList<>();
        SUPPORTED_ALGORITHMS_MAP.forEach((key, value) -> {
            String label = key;
            if ("DES".equals(key) || "RC4".equals(key)) label += " (Weak - Avoid)";
            else if ("TripleDES".equals(key)) label += " (Legacy - Avoid)";
            else if ("AES".equals(key)) label += " (Recommended)";
            algoOptions.add(Map.of("value", key, "label", label));
        });
        settingsInputs.add(Map.ofEntries(
                Map.entry("id", "algorithm"), // Use ID
                Map.entry("label", "Algorithm:"),
                Map.entry("type", "select"),
                Map.entry("options", algoOptions),
                Map.entry("default", "AES"),
                Map.entry("required", true),
                Map.entry("helperText", "AES (GCM mode) recommended.")
        ));

        // Secret Key Input
        settingsInputs.add(Map.ofEntries(
                Map.entry("id", "secretKey"), // Use ID
                Map.entry("label", "Secret Key / Password:"),
                Map.entry("type", "password"), // Use password input type
                Map.entry("required", true),
                Map.entry("placeholder", "Enter secret key"),
                Map.entry("helperText", "Used to derive the encryption key.")
        ));

        // Plain Text Input (Conditional for Encrypt)
        settingsInputs.add(Map.ofEntries(
                Map.entry("id", "inputText"), // Use ID
                Map.entry("label", "Text to Encrypt:"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 5),
                Map.entry("required", true), // Required only for encrypt, checked in process
                Map.entry("placeholder", "Enter plain text here..."),
                Map.entry("condition", "uiOperation === 'encrypt'") // Condition based on ID
        ));

        // Encrypted Text Input (Conditional for Decrypt)
        settingsInputs.add(Map.ofEntries(
                Map.entry("id", "encryptedInput"), // Use ID
                Map.entry("label", "Text to Decrypt (Base64):"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 5),
                Map.entry("required", true), // Required only for decrypt, checked in process
                Map.entry("placeholder", "Paste Base64 encoded text here..."),
                Map.entry("condition", "uiOperation === 'decrypt'") // Condition based on ID
        ));

        settingsSection.put("inputs", settingsInputs);
        sections.add(settingsSection);


        // --- Section 2: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Output");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Output for Encrypted Result
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "encryptedResult"), // ID matches key in response map
                Map.entry("label", "Encrypted (Base64)"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 4),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("condition", "uiOperation === 'encrypt' && typeof encryptedResult !== 'undefined'") // Show only on successful encrypt
        ));

        // Output for Decrypted Result
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "decryptedResult"), // ID matches key in response map
                Map.entry("label", "Decrypted Text"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 4),
                Map.entry("monospace", false), // Usually not monospace
                Map.entry("buttons", List.of("copy")),
                Map.entry("condition", "uiOperation === 'decrypt' && typeof decryptedResult !== 'undefined'") // Show only on successful decrypt
        ));

        // Output for Algorithm Used
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "algorithmUsed"), // ID matches key in response map
                Map.entry("label", "Algorithm Used"),
                Map.entry("type", "text"),
                // No extra condition needed, shown whenever result section is shown
                Map.entry("condition", "typeof algorithmUsed !== 'undefined'")
        ));

        // Optional: Output for Original Input Text (during encrypt)
        resultOutputs.add(Map.ofEntries(
                Map.entry("id", "originalInputText"), // ID matches key in response map
                Map.entry("label", "Original Input"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 2),
                Map.entry("condition", "uiOperation === 'encrypt' && typeof originalInputText !== 'undefined'")
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

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        // Read the operation using the ID defined in UI metadata
        String uiOperation = (String) input.get("uiOperation");
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        if (uiOperation == null || uiOperation.isBlank()) {
            return Map.of("success", false, errorOutputId, "No operation specified.");
        }

        // Include the operation in the input map for potential use later
        Map<String, Object> processingInput = new HashMap<>(input);

        try {
            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "encrypt" -> result = encrypt(processingInput);
                case "decrypt" -> result = decrypt(processingInput);
                default -> {
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
                }
            }

            // Add uiOperation to success result for context if needed by complex conditions
            if (result.get("success") == Boolean.TRUE) {
                Map<String, Object> finalResult = new HashMap<>(result);
                finalResult.put("uiOperation", uiOperation);
                return finalResult;
            } else {
                // Ensure error key consistency
                if (result.containsKey("error") && !result.containsKey(errorOutputId)) {
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put(errorOutputId, result.get("error"));
                    finalResult.remove("error");
                    return finalResult;
                }
                return result; // Return error as is
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println("Crypto algorithm/padding error: " + e.getMessage());
            return Map.of("success", false, errorOutputId, "Invalid/unsupported algorithm or padding.");
        } catch (InvalidKeyException e) {
            System.err.println("Invalid key error: " + e.getMessage());
            String algo = (String) input.getOrDefault("algorithm", "?");
            String message = "Invalid key for algorithm '" + algo + "'. Check key length/format.";
            return Map.of("success", false, errorOutputId, message);
        } catch (InvalidAlgorithmParameterException e) {
            System.err.println("Invalid IV/Parameter error: " + e.getMessage());
            return Map.of("success", false, errorOutputId, "Invalid crypto parameters (e.g., IV).");
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Decryption padding/block size error: " + e.getMessage());
            return Map.of("success", false, errorOutputId, "Decryption failed (wrong key/data/algo?).");
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing crypto request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // --- Corrected Private Helper Methods ---

    private Map<String, Object> encrypt(Map<String, Object> input) throws Exception {
        // Get parameters using new IDs & Validate using null for required fields
        String text = getStringParameter(input, "inputText"); // Use new ID
        String keyStr = getStringParameter(input, "secretKey"); // Use new ID
        String algorithm = Objects.requireNonNull(getStringParameter(input, "algorithm")).toUpperCase(); // Use new ID

        if (!SUPPORTED_ALGORITHMS_MAP.containsKey(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm specified: " + algorithm);
        }

        String transformation = SUPPORTED_ALGORITHMS_MAP.get(algorithm);
        int keyLength = KEY_LENGTHS.get(algorithm);
        int ivLength = IV_LENGTHS.get(algorithm);

        // Derive key
        byte[] keyBytes = createKey(keyStr, algorithm, keyLength);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, algorithm.equals("RC4") ? "ARCFOUR" : algorithm.split("/")[0]); // Use base algo name for KeySpec

        // Initialize Cipher
        Cipher cipher = Cipher.getInstance(transformation);
        byte[] iv = null;
        if (ivLength > 0) {
            iv = getRandomNonce(ivLength);
            if ("AES".equals(algorithm)) { // GCM mode needs GCMParameterSpec
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            } else { // CBC modes use IvParameterSpec
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            }
        } else { // RC4 has no IV
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        }

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Combine IV (if used) and encrypted data
        byte[] combined = (iv != null) ? combineBytes(iv, encryptedBytes) : encryptedBytes;
        String encryptedBase64 = Base64.getEncoder().encodeToString(combined);

        // Build result map matching NEW output field IDs
        Map<String, Object> result = new HashMap<>();
        result.put("success", true); // ** Indicate success**
        result.put("originalInputText", text);      // Matches output ID "originalInputText"
        result.put("encryptedResult", encryptedBase64); // Matches output ID "encryptedResult"
        result.put("algorithmUsed", algorithm);     // Matches output ID "algorithmUsed"

        return result;
    }

    private Map<String, Object> decrypt(Map<String, Object> input) throws Exception {
        // Get parameters using new IDs & Validate using null for required fields
        String encryptedBase64 = getStringParameter(input, "encryptedInput"); // Use new ID
        String keyStr = getStringParameter(input, "secretKey"); // Use new ID
        String algorithm = Objects.requireNonNull(getStringParameter(input, "algorithm")).toUpperCase(); // Use new ID

        if (!SUPPORTED_ALGORITHMS_MAP.containsKey(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm specified: " + algorithm);
        }

        String transformation = SUPPORTED_ALGORITHMS_MAP.get(algorithm);
        int keyLength = KEY_LENGTHS.get(algorithm);
        int ivLength = IV_LENGTHS.get(algorithm);

        // Decode from Base64
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(encryptedBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 input text.", e);
        }

        // Derive key
        byte[] keyBytes = createKey(keyStr, algorithm, keyLength);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, algorithm.equals("RC4") ? "ARCFOUR" : algorithm.split("/")[0]);

        // Extract IV and Encrypted Data
        byte[] iv = null;
        byte[] encryptedBytes;
        if (ivLength > 0) {
            if (combined.length < ivLength) {
                throw new IllegalArgumentException("Input text too short for IV (Algorithm: " + algorithm + ")");
            }
            iv = Arrays.copyOfRange(combined, 0, ivLength);
            encryptedBytes = Arrays.copyOfRange(combined, ivLength, combined.length);
        } else { // RC4
            encryptedBytes = combined;
        }

        // Initialize Cipher
        Cipher cipher = Cipher.getInstance(transformation);
        if (ivLength > 0) {
            if ("AES".equals(algorithm)) { // GCM mode needs GCMParameterSpec
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            } else { // CBC modes use IvParameterSpec
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            }
        } else { // RC4 has no IV
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
        }

        // Decrypt (This is where BadPaddingException etc. often occur)
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);


        // Build result map matching NEW output field IDs
        Map<String, Object> result = new HashMap<>();
        result.put("success", true); // ** Indicate success**
        // result.put("originalEncryptedInput", encryptedBase64); // Optional: echo original input if needed
        result.put("decryptedResult", decryptedText);   // Matches output ID "decryptedResult"
        result.put("algorithmUsed", algorithm);       // Matches output ID "algorithmUsed"

        return result;
    }


    // Helper to get String parameter and check if required (null default means required)
    private String getStringParameter(Map<String, Object> input, String key) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            // Required? -> Throw error
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        String strValue = value.toString();
        if (strValue.isEmpty()) {
            // Required? -> Throw error
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return strValue; // Return the non-empty string value
    }

    // Helper method to generate a random nonce (IV) using SecureRandom
    private byte[] getRandomNonce(int size) {
        byte[] nonce = new byte[size];
        // Use SecureRandom for cryptographically strong random numbers
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    /** Combines two byte arrays. */
    private byte[] combineBytes(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }


    /** Creates fixed-length key derived via SHA-256 (Basic KDF - see warning). */
    private byte[] createKey(String password, String algorithm, int keyLength) throws NoSuchAlgorithmException {
        // Warning: This is a basic key derivation. For passwords, using PBKDF2 or Argon2 with a salt is strongly recommended.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        // Arrays.fill(hash, (byte) 0); // Optional cleanup
        return Arrays.copyOf(hash, keyLength);
    }
}