package kostovite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoTools implements ExtendedPluginInterface {
    // Supported algorithms
    private static final String[] SUPPORTED_ALGORITHMS = {
            "AES", "DES", "TripleDES", "Blowfish", "RC4"
    };

    // IV size for GCM mode (in bytes)
    private static final int GCM_IV_LENGTH = 12;
    // Tag size for GCM mode (in bits)
    private static final int GCM_TAG_LENGTH = 128;

    @Override
    public String getName() {
        return "CryptoTools";
    }

    @Override
    public void execute() {
        System.out.println("CryptoTools Plugin executed");

        // Demonstrate basic usage
        try {
            Map<String, Object> encryptParams = new HashMap<>();
            encryptParams.put("operation", "encrypt");
            encryptParams.put("algorithm", "AES");
            encryptParams.put("text", "Hello, World!");
            encryptParams.put("key", "MySuperSecretKey");

            Map<String, Object> encryptResult = process(encryptParams);
            System.out.println("Sample encryption: " + encryptResult.get("encryptedText"));

            Map<String, Object> decryptParams = new HashMap<>();
            decryptParams.put("operation", "decrypt");
            decryptParams.put("algorithm", "AES");
            decryptParams.put("encryptedText", encryptResult.get("encryptedText"));
            decryptParams.put("key", "MySuperSecretKey");

            Map<String, Object> decryptResult = process(decryptParams);
            System.out.println("Sample decryption: " + decryptResult.get("decryptedText"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Provides encryption and decryption functionality with various algorithms"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Encrypt operation
        Map<String, Object> encryptOperation = new HashMap<>();
        encryptOperation.put("description", "Encrypt text with a secret key");
        Map<String, Object> encryptInputs = new HashMap<>();
        encryptInputs.put("text", Map.of("type", "string", "description", "Text to encrypt", "required", true));
        encryptInputs.put("key", Map.of("type", "string", "description", "Secret key for encryption", "required", true));
        encryptInputs.put("algorithm", Map.of("type", "string", "description", "Encryption algorithm (AES, DES, TripleDES, Blowfish, RC4)", "required", true));
        encryptOperation.put("inputs", encryptInputs);
        operations.put("encrypt", encryptOperation);

        // Decrypt operation
        Map<String, Object> decryptOperation = new HashMap<>();
        decryptOperation.put("description", "Decrypt encrypted text with a secret key");
        Map<String, Object> decryptInputs = new HashMap<>();
        decryptInputs.put("encryptedText", Map.of("type", "string", "description", "Text to decrypt (Base64 encoded)", "required", true));
        decryptInputs.put("key", Map.of("type", "string", "description", "Secret key for decryption (must match the encryption key)", "required", true));
        decryptInputs.put("algorithm", Map.of("type", "string", "description", "Decryption algorithm (must match the encryption algorithm)", "required", true));
        decryptOperation.put("inputs", decryptInputs);
        operations.put("decrypt", decryptOperation);

        // List algorithms operation
        Map<String, Object> listAlgorithmsOperation = new HashMap<>();
        listAlgorithmsOperation.put("description", "List supported encryption/decryption algorithms");
        operations.put("listAlgorithms", listAlgorithmsOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "CryptoTools"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "EnhancedEncryption"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Crypto"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Main Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Encryption/Decryption Settings");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "uiOperation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "encrypt", "label", "Encrypt"));
        operationOptions.add(Map.of("value", "decrypt", "label", "Decrypt"));
        operationField.put("options", operationOptions);
        operationField.put("default", "encrypt");
        operationField.put("required", true);
        section1Fields.add(operationField);

        // Algorithm selection field
        Map<String, Object> algorithmField = new HashMap<>();
        algorithmField.put("name", "algorithm");
        algorithmField.put("label", "Algorithm:");
        algorithmField.put("type", "select");
        List<Map<String, String>> algorithmOptions = new ArrayList<>();
        algorithmOptions.add(Map.of("value", "AES", "label", "AES (Advanced Encryption Standard)"));
        algorithmOptions.add(Map.of("value", "DES", "label", "DES (Data Encryption Standard)"));
        algorithmOptions.add(Map.of("value", "TripleDES", "label", "Triple DES"));
        algorithmOptions.add(Map.of("value", "Blowfish", "label", "Blowfish"));
        algorithmOptions.add(Map.of("value", "RC4", "label", "RC4 (Rivest Cipher 4)"));
        algorithmField.put("options", algorithmOptions);
        algorithmField.put("default", "AES");
        algorithmField.put("required", true);
        section1Fields.add(algorithmField);

        // Secret key field
        Map<String, Object> keyField = new HashMap<>();
        keyField.put("name", "key");
        keyField.put("label", "Secret Key:");
        keyField.put("type", "password"); // Use password type for better security
        keyField.put("default", "");
        keyField.put("required", true);
        section1Fields.add(keyField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Encrypt Inputs (conditional)
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Text to Encrypt");
        inputSection2.put("condition", "uiOperation === 'encrypt'");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Text to encrypt field
        Map<String, Object> textField = new HashMap<>();
        textField.put("name", "text");
        textField.put("label", "Text to Encrypt:");
        textField.put("type", "text");
        textField.put("multiline", true); // For longer text
        textField.put("rows", 5); // Suggest multi-line input
        textField.put("default", "");
        textField.put("required", true);
        section2Fields.add(textField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Decrypt Inputs (conditional)
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Text to Decrypt");
        inputSection3.put("condition", "uiOperation === 'decrypt'");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Text to decrypt field
        Map<String, Object> encryptedTextField = new HashMap<>();
        encryptedTextField.put("name", "encryptedText");
        encryptedTextField.put("label", "Encrypted Text (Base64):");
        encryptedTextField.put("type", "text");
        encryptedTextField.put("multiline", true); // For longer text
        encryptedTextField.put("rows", 5); // Suggest multi-line input
        encryptedTextField.put("default", "");
        encryptedTextField.put("required", true);
        section3Fields.add(encryptedTextField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Encryption Result
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Encryption Result");
        outputSection1.put("condition", "uiOperation === 'encrypt'");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Original text
        Map<String, Object> originalTextOutput = new HashMap<>();
        originalTextOutput.put("title", "Original Text");
        originalTextOutput.put("name", "originalText");
        originalTextOutput.put("type", "text");
        section1OutputFields.add(originalTextOutput);

        // Encrypted text
        Map<String, Object> encryptedTextOutput = new HashMap<>();
        encryptedTextOutput.put("title", "Encrypted Text (Base64)");
        encryptedTextOutput.put("name", "encryptedText");
        encryptedTextOutput.put("type", "text");
        encryptedTextOutput.put("buttons", List.of("copy")); // Add copy button
        section1OutputFields.add(encryptedTextOutput);

        // Algorithm used
        Map<String, Object> encryptAlgorithmOutput = new HashMap<>();
        encryptAlgorithmOutput.put("title", "Algorithm Used");
        encryptAlgorithmOutput.put("name", "algorithm");
        encryptAlgorithmOutput.put("type", "text");
        section1OutputFields.add(encryptAlgorithmOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Decryption Result
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Decryption Result");
        outputSection2.put("condition", "uiOperation === 'decrypt'");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Original encrypted text
        Map<String, Object> originalEncryptedOutput = new HashMap<>();
        originalEncryptedOutput.put("title", "Original Encrypted Text");
        originalEncryptedOutput.put("name", "encryptedText");
        originalEncryptedOutput.put("type", "text");
        section2OutputFields.add(originalEncryptedOutput);

        // Decrypted text
        Map<String, Object> decryptedTextOutput = new HashMap<>();
        decryptedTextOutput.put("title", "Decrypted Text");
        decryptedTextOutput.put("name", "decryptedText");
        decryptedTextOutput.put("type", "text");
        decryptedTextOutput.put("buttons", List.of("copy")); // Add copy button
        section2OutputFields.add(decryptedTextOutput);

        // Algorithm used
        Map<String, Object> decryptAlgorithmOutput = new HashMap<>();
        decryptAlgorithmOutput.put("title", "Algorithm Used");
        decryptAlgorithmOutput.put("name", "algorithm");
        decryptAlgorithmOutput.put("type", "text");
        section2OutputFields.add(decryptAlgorithmOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display (conditional)
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "error");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section3OutputFields.add(errorOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "");

            switch (operation.toLowerCase()) {
                case "encrypt":
                    return encrypt(input);
                case "decrypt":
                    return decrypt(input);
                case "listalgorithms":
                    result.put("algorithms", SUPPORTED_ALGORITHMS);
                    result.put("success", true);
                    break;
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

    private Map<String, Object> encrypt(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get parameters
            String text = (String) input.get("text");
            String key = (String) input.get("key");
            String algorithm = ((String) input.getOrDefault("algorithm", "AES")).toUpperCase();

            // Validation
            if (text == null || text.isEmpty()) {
                result.put("error", "Text to encrypt cannot be empty");
                return result;
            }

            if (key == null || key.isEmpty()) {
                result.put("error", "Encryption key cannot be empty");
                return result;
            }

            // Check if algorithm is supported
            if (!Arrays.asList(SUPPORTED_ALGORITHMS).contains(algorithm)) {
                result.put("error", "Unsupported algorithm: " + algorithm);
                return result;
            }

            // Perform encryption based on algorithm
            String encryptedText;

            switch (algorithm) {
                case "AES":
                    encryptedText = encryptAES(text, key);
                    break;
                case "DES":
                    encryptedText = encryptDES(text, key);
                    break;
                case "TRIPLEDES":
                    encryptedText = encryptTripleDES(text, key);
                    break;
                case "BLOWFISH":
                    encryptedText = encryptBlowfish(text, key);
                    break;
                case "RC4":
                    encryptedText = encryptRC4(text, key);
                    break;
                default:
                    result.put("error", "Unsupported algorithm: " + algorithm);
                    return result;
            }

            // Return results
            result.put("originalText", text);
            result.put("encryptedText", encryptedText);
            result.put("algorithm", algorithm);
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Encryption failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> decrypt(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get parameters
            String encryptedText = (String) input.get("encryptedText");
            String key = (String) input.get("key");
            String algorithm = ((String) input.getOrDefault("algorithm", "AES")).toUpperCase();

            // Validation
            if (encryptedText == null || encryptedText.isEmpty()) {
                result.put("error", "Encrypted text cannot be empty");
                return result;
            }

            if (key == null || key.isEmpty()) {
                result.put("error", "Decryption key cannot be empty");
                return result;
            }

            // Check if algorithm is supported
            if (!Arrays.asList(SUPPORTED_ALGORITHMS).contains(algorithm)) {
                result.put("error", "Unsupported algorithm: " + algorithm);
                return result;
            }

            // Perform decryption based on algorithm
            String decryptedText;

            switch (algorithm) {
                case "AES":
                    decryptedText = decryptAES(encryptedText, key);
                    break;
                case "DES":
                    decryptedText = decryptDES(encryptedText, key);
                    break;
                case "TRIPLEDES":
                    decryptedText = decryptTripleDES(encryptedText, key);
                    break;
                case "BLOWFISH":
                    decryptedText = decryptBlowfish(encryptedText, key);
                    break;
                case "RC4":
                    decryptedText = decryptRC4(encryptedText, key);
                    break;
                default:
                    result.put("error", "Unsupported algorithm: " + algorithm);
                    return result;
            }

            // Return results
            result.put("encryptedText", encryptedText);
            result.put("decryptedText", decryptedText);
            result.put("algorithm", algorithm);
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Decryption failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // AES Implementation (AES/GCM/NoPadding)
    private String encryptAES(String text, String key) throws Exception {
        // Create a secure key from the provided key
        byte[] keyBytes = createKey(key, "AES", 32); // 256-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = getRandomNonce(GCM_IV_LENGTH);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        // Encode as Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decryptAES(String encryptedText, String key) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        // Create a secure key from the provided key
        byte[] keyBytes = createKey(key, "AES", 32); // 256-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // DES Implementation
    private String encryptDES(String text, String key) throws Exception {
        byte[] keyBytes = createKey(key, "DES", 8); // 64-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        byte[] iv = getRandomNonce(8);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        // Encode as Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decryptDES(String encryptedText, String key) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] keyBytes = createKey(key, "DES", 8); // 64-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");

        // Extract IV and encrypted data
        byte[] iv = new byte[8];
        byte[] encryptedBytes = new byte[combined.length - 8];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // TripleDES Implementation
    private String encryptTripleDES(String text, String key) throws Exception {
        byte[] keyBytes = createKey(key, "DESede", 24); // 192-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DESede");

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        byte[] iv = getRandomNonce(8);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        // Encode as Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decryptTripleDES(String encryptedText, String key) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] keyBytes = createKey(key, "DESede", 24); // 192-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DESede");

        // Extract IV and encrypted data
        byte[] iv = new byte[8];
        byte[] encryptedBytes = new byte[combined.length - 8];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Blowfish Implementation
    private String encryptBlowfish(String text, String key) throws Exception {
        byte[] keyBytes = createKey(key, "Blowfish", 16); // 128-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "Blowfish");

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
        byte[] iv = getRandomNonce(8);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        // Encode as Base64
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decryptBlowfish(String encryptedText, String key) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] keyBytes = createKey(key, "Blowfish", 16); // 128-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "Blowfish");

        // Extract IV and encrypted data
        byte[] iv = new byte[8];
        byte[] encryptedBytes = new byte[combined.length - 8];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // RC4 Implementation (Using ARC4 as Java calls it)
    private String encryptRC4(String text, String key) throws Exception {
        byte[] keyBytes = createKey(key, "ARCFOUR", 16); // 128-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "ARCFOUR");

        // Initialize cipher (RC4 doesn't use an IV)
        Cipher cipher = Cipher.getInstance("ARCFOUR");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        // Encode as Base64
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decryptRC4(String encryptedText, String key) throws Exception {
        // Decode from Base64
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

        byte[] keyBytes = createKey(key, "ARCFOUR", 16); // 128-bit key
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "ARCFOUR");

        // Initialize cipher
        Cipher cipher = Cipher.getInstance("ARCFOUR");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Helper method to generate a random nonce (IV)
    private byte[] getRandomNonce(int size) {
        byte[] nonce = new byte[size];
        new java.security.SecureRandom().nextBytes(nonce);
        return nonce;
    }

    // Helper method to create a fixed-length key from a password
    private byte[] createKey(String password, String algorithm, int keyLength) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(key, keyLength);
    }
}