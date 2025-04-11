package kostovite;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Encode and decode URL-formatted strings"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Encode operation
        Map<String, Object> encodeOperation = new HashMap<>();
        encodeOperation.put("description", "URL encode a string");
        Map<String, Object> encodeInputs = new HashMap<>();
        encodeInputs.put("input", Map.of("type", "string", "description", "Text string to encode", "required", true));
        encodeOperation.put("inputs", encodeInputs);
        operations.put("encode", encodeOperation);

        // Decode operation
        Map<String, Object> decodeOperation = new HashMap<>();
        decodeOperation.put("description", "URL decode a string");
        Map<String, Object> decodeInputs = new HashMap<>();
        decodeInputs.put("input", Map.of("type", "string", "description", "URL-encoded string to decode", "required", true));
        decodeOperation.put("inputs", decodeInputs);
        operations.put("decode", decodeOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "UrlFormatter"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Link"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Web Tools"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "URL Operation");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "operation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "encode", "label", "URL Encode"));
        operationOptions.add(Map.of("value", "decode", "label", "URL Decode"));
        operationField.put("options", operationOptions);
        operationField.put("default", "encode");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Text Input
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Input Text");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Text input field
        Map<String, Object> inputTextField = new HashMap<>();
        inputTextField.put("name", "input");
        inputTextField.put("label", "Text:");
        inputTextField.put("type", "text");
        inputTextField.put("multiline", true);
        inputTextField.put("rows", 5);
        inputTextField.put("placeholder", "operation === 'encode' ? 'Enter text to encode...' : 'Enter URL-encoded text to decode...'");
        inputTextField.put("required", true);
        inputTextField.put("helperText", "operation === 'encode' ? 'Special characters will be encoded as %XX sequences' : 'URL-encoded text with %XX sequences will be decoded'");
        section2Fields.add(inputTextField);

        // Example hints
        Map<String, Object> exampleField = new HashMap<>();
        exampleField.put("name", "example");
        exampleField.put("label", "Example:");
        exampleField.put("type", "text");
        exampleField.put("disabled", true);
        exampleField.put("formula", "operation === 'encode' ? 'hello world → hello%20world' : 'hello%20world → hello world'");
        exampleField.put("helperText", "operation === 'encode' ? 'Spaces become %20, special chars like & become %26' : '%20 becomes space, %26 becomes &'");
        section2Fields.add(exampleField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Result
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "operation === 'encode' ? 'URL Encoded Result' : 'URL Decoded Result'");
        outputSection1.put("condition", "success");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Result display
        Map<String, Object> resultOutput = new HashMap<>();
        resultOutput.put("title", "operation === 'encode' ? 'Encoded Text' : 'Decoded Text'");
        resultOutput.put("name", "output");
        resultOutput.put("type", "text");
        resultOutput.put("multiline", true);
        resultOutput.put("monospace", true);
        resultOutput.put("buttons", List.of("copy"));
        section1OutputFields.add(resultOutput);

        // Length display
        Map<String, Object> lengthOutput = new HashMap<>();
        lengthOutput.put("title", "Length");
        lengthOutput.put("name", "lengthInfo");
        lengthOutput.put("type", "text");
        lengthOutput.put("formula", "output.length + ' characters'");
        section1OutputFields.add(lengthOutput);

        // Information about the operation
        Map<String, Object> operationInfoOutput = new HashMap<>();
        operationInfoOutput.put("title", "Action");
        operationInfoOutput.put("name", "operationInfo");
        operationInfoOutput.put("type", "text");
        operationInfoOutput.put("formula", "operation === 'encode' ? 'URL encoded using RFC 3986 standard' : 'URL decoded using UTF-8 encoding'");
        section1OutputFields.add(operationInfoOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Character Comparison (when encoding)
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Character Analysis");
        outputSection2.put("condition", "success && operation === 'encode' && input !== output");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Comparison information
        Map<String, Object> comparisonOutput = new HashMap<>();
        comparisonOutput.put("title", "Changes");
        comparisonOutput.put("name", "comparisonInfo");
        comparisonOutput.put("type", "text");
        comparisonOutput.put("formula",
                "const origLen = input.length; " +
                        "const encodedLen = output.length; " +
                        "const diff = encodedLen - origLen; " +
                        "return `Original: ${origLen} chars | Encoded: ${encodedLen} chars | Difference: ${diff > 0 ? '+' + diff : diff} chars`");
        section2OutputFields.add(comparisonOutput);

        // Encoding chart for common characters
        Map<String, Object> chartOutput = new HashMap<>();
        chartOutput.put("title", "Common Encodings");
        chartOutput.put("name", "encodingChart");
        chartOutput.put("type", "table");
        List<Map<String, Object>> chartColumns = new ArrayList<>();
        chartColumns.add(Map.of("header", "Character", "field", "char"));
        chartColumns.add(Map.of("header", "Encoded", "field", "encoded"));
        chartOutput.put("columns", chartColumns);
        chartOutput.put("data",
                "[" +
                        "{ char: 'space', encoded: '%20' }, " +
                        "{ char: '!', encoded: '%21' }, " +
                        "{ char: '#', encoded: '%23' }, " +
                        "{ char: '$', encoded: '%24' }, " +
                        "{ char: '&', encoded: '%26' }, " +
                        "{ char: ''', encoded: '%27' }, " +
                        "{ char: '(', encoded: '%28' }, " +
                        "{ char: ')', encoded: '%29' }, " +
                        "{ char: '*', encoded: '%2A' }, " +
                        "{ char: '+', encoded: '%2B' }, " +
                        "{ char: ',', encoded: '%2C' }, " +
                        "{ char: '/', encoded: '%2F' }, " +
                        "{ char: ':', encoded: '%3A' }, " +
                        "{ char: ';', encoded: '%3B' }, " +
                        "{ char: '=', encoded: '%3D' }, " +
                        "{ char: '?', encoded: '%3F' }, " +
                        "{ char: '@', encoded: '%40' }, " +
                        "{ char: '[', encoded: '%5B' }, " +
                        "{ char: ']', encoded: '%5D' }" +
                        "]");
        section2OutputFields.add(chartOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display
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