package kostovite;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UrlFormatter implements PluginInterface {

    @Override
    public String getName() {
        return "UrlFormatter";
    }

    @Override
    public void execute() {
        System.out.println("UrlFormatter Plugin executed");

        // Demonstrate basic usage
        try {
            // Example encoding
            Map<String, Object> encodeParams = new HashMap<>();
            encodeParams.put("operation", "encode");
            encodeParams.put("input", "hello world");

            Map<String, Object> encodeResult = process(encodeParams);
            System.out.println("Sample URL encoding: " + encodeResult.get("output"));

            // Example decoding
            Map<String, Object> decodeParams = new HashMap<>();
            decodeParams.put("operation", "decode");
            decodeParams.put("input", "hello%20world");

            Map<String, Object> decodeResult = process(decodeParams);
            System.out.println("Sample URL decoding: " + decodeResult.get("output"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Encode and decode URL-formatted strings");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Encode operation
        Map<String, Object> encodeOperation = new HashMap<>();
        encodeOperation.put("description", "URL encode a string");

        Map<String, Object> encodeInputs = new HashMap<>();
        encodeInputs.put("input", "Text string to encode");

        encodeOperation.put("inputs", encodeInputs);
        operations.put("encode", encodeOperation);

        // Decode operation
        Map<String, Object> decodeOperation = new HashMap<>();
        decodeOperation.put("description", "URL decode a string");

        Map<String, Object> decodeInputs = new HashMap<>();
        decodeInputs.put("input", "URL-encoded string to decode");

        decodeOperation.put("inputs", decodeInputs);
        operations.put("decode", decodeOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "encode");
            String inputText = (String) input.get("input");

            // Validation
            if (inputText == null) {
                result.put("error", "Input text cannot be null");
                return result;
            }

            // Process based on the operation
            String output;
            switch (operation.toLowerCase()) {
                case "encode":
                    output = urlEncode(inputText);
                    break;
                case "decode":
                    output = urlDecode(inputText);
                    break;
                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

            // Build the result
            result.put("input", inputText);
            result.put("output", output);
            result.put("operation", operation);
            result.put("success", true);

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * URL encodes a string according to RFC 3986.
     *
     * @param input The string to encode
     * @return The URL encoded string
     */
    private String urlEncode(String input) {
        if (input == null) return "";
        return URLEncoder.encode(input, StandardCharsets.UTF_8)
                .replace("+", "%20"); // Replace space encoding with %20 instead of +
    }

    /**
     * URL decodes a string according to RFC 3986.
     *
     * @param input The URL encoded string to decode
     * @return The decoded string
     */
    private String urlDecode(String input) {
        if (input == null) return "";
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }
}