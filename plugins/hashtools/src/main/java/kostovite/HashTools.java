package kostovite;

import org.pf4j.Extension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Extension
public class HashTools implements PluginInterface {
    @Override
    public String getName() {
        return "HashTools";
    }

    @Override
    public void execute() {
        // Example usage of hashing
        String input = "Hello, World!";
        String hash = hashString(input, "SHA-256");
        System.out.println("Hash of '" + input + "': " + hash);
    }

    public String hashString(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}