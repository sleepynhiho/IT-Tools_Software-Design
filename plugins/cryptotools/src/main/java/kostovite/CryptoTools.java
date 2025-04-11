package kostovite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Provides encryption and decryption functionality with various algorithms");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Encrypt operation
        Map<String, Object> encryptOperation = new HashMap<>();
        encryptOperation.put("description", "Encrypt text with a secret key");

        Map<String, Object> encryptInputs = new HashMap<>();
        encryptInputs.put("text", "Text to encrypt");
        encryptInputs.put("key", "Secret key for encryption");
        encryptInputs.put("algorithm", "Encryption algorithm (AES, DES, TripleDES, Blowfish, RC4)");

        encryptOperation.put("inputs", encryptInputs);
        operations.put("encrypt", encryptOperation);

        // Decrypt operation
        Map<String, Object> decryptOperation = new HashMap<>();
        decryptOperation.put("description", "Decrypt encrypted text with a secret key");

        Map<String, Object> decryptInputs = new HashMap<>();
        decryptInputs.put("encryptedText", "Text to decrypt (Base64 encoded)");
        decryptInputs.put("key", "Secret key for decryption (must match the encryption key)");
        decryptInputs.put("algorithm", "Decryption algorithm (must match the encryption algorithm)");

        decryptOperation.put("inputs", decryptInputs);
        operations.put("decrypt", decryptOperation);

        // List algorithms operation
        Map<String, Object> listAlgorithmsOperation = new HashMap<>();
        listAlgorithmsOperation.put("description", "List supported encryption/decryption algorithms");
        operations.put("listAlgorithms", listAlgorithmsOperation);

        metadata.put("operations", operations);
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