package kostovite;

import java.math.BigInteger;
import java.util.*;

public class IntegerBaseConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "IntegerBaseConverter";
    }

    @Override
    public void execute() {
        System.out.println("IntegerBaseConverter Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("integerInputNumber", "255"); // Test với số thập phân
            params.put("integerInputBase", 10);      // Cơ số đầu vào là 10

            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Dec 255): " + result1);

            params.put("integerInputNumber", "FF"); // Test với số Hex
            params.put("integerInputBase", 16);     // Cơ số đầu vào là 16
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Hex FF): " + result2);

            params.put("integerInputNumber", "11111111"); // Test với số Binary
            params.put("integerInputBase", 2);        // Cơ số đầu vào là 2
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Bin 11111111): " + result3);

            params.put("integerInputNumber", "InvalidNum"); // Test lỗi
            params.put("integerInputBase", 10);
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid Input): " + result4);

            params.put("integerInputNumber", "10");
            params.put("integerInputBase", 1); // Test base không hợp lệ
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Invalid Base): " + result5);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "IntegerBaseConverter");
        metadata.put("name", "Integer base converter");
        metadata.put("description", "Convert a number between different bases (decimal, hexadecimal, binary, octal, base64, ...)");
        metadata.put("icon", "SwapHoriz");
        metadata.put("category", "Converter");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Main Converter ---
        Map<String, Object> converterSection = new HashMap<>();
        converterSection.put("id", "integerBaseConverter"); // ID của section
        converterSection.put("label", "");    // Nhãn section trống

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "integerInputNumber"),
                Map.entry("label", "Input number"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "42"),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("multiline", true)
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "integerInputBase"),
                Map.entry("label", "Input base"),
                Map.entry("type", "number"),
                Map.entry("buttons", List.of("minus", "plus")),
                Map.entry("required", true),
                Map.entry("default", 42),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("min", 2),
                Map.entry("max", 64)
        ));
        converterSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(createOutputField("binaryOutput", "Binary(2)"));
        outputs.add(createOutputField("octalOutput", "Octal(8)"));
        outputs.add(createOutputField("decimalOutput", "Decimal(10)"));
        outputs.add(createOutputField("hexadecimalOutput", "Hexadecimal(16)"));
        outputs.add(createOutputField("base64Output", "Base64"));
        converterSection.put("outputs", outputs);

        sections.add(converterSection);

        // --- Error Section ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", ERROR_OUTPUT_ID),
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error")
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);

        metadata.put("sections", sections);
        return metadata;
    }

    private Map<String, Object> createOutputField(String id, String label) {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("label", label),
                Map.entry("type", "text"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("monospace", true)
        );
    }


    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Lấy input number và input base
            String inputNumberStr = getStringParam(input, "integerInputNumber", null);
            int inputBase = getIntParam(input, "integerInputBase", -1);

            // --- Validation ---
            if (inputNumberStr == null) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input number is required.");
            }
            if (inputBase == -1) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input base is required.");
            }
            // Trim whitespace từ input number
            inputNumberStr = inputNumberStr.trim();
            if (inputNumberStr.isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input number cannot be empty.");
            }

            // Validate base range (BigInteger hỗ trợ 2-36 cho constructor chuỗi)
            if (inputBase < 2 || inputBase > 64) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input base must be between 2 and 64.");
            }

            // --- Conversion ---
            BigInteger bigIntValue;
            byte[] inputBytes = null; // Dùng cho Base64

            if (inputBase == 64) {

                try {
                    inputBytes = Base64.getDecoder().decode(inputNumberStr);
                    bigIntValue = new BigInteger(1, inputBytes);
                } catch (IllegalArgumentException e) {
                    return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid Base64 input string.");
                }
            } else if (inputBase <= 36) {
                try {
                    bigIntValue = new BigInteger(inputNumberStr, inputBase);
                } catch (NumberFormatException e) {
                    return Map.of("success", false, ERROR_OUTPUT_ID, "Input number '" + inputNumberStr + "' is not valid for base " + inputBase + ".");
                }
                inputBytes = bigIntValue.toByteArray();
            } else {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Parsing input for base " + inputBase + " is not supported yet.");
            }

            // --- Calculate Outputs ---
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            result.put("binaryOutput", bigIntValue.toString(2));
            result.put("octalOutput", bigIntValue.toString(8));
            result.put("decimalOutput", bigIntValue.toString(10));
            result.put("hexadecimalOutput", bigIntValue.toString(16).toUpperCase());

            if (inputBytes != null) {
                result.put("base64Output", Base64.getEncoder().encodeToString(inputBytes));
            } else {
                result.put("base64Output", "Error converting to bytes");
            }


            return result;

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, ERROR_OUTPUT_ID, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing integer base conversion: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return value.toString();
    }

    private int getIntParam(Map<String, Object> input, String key, int defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == -1) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + key + "' must be a valid integer.");
        }
    }
}