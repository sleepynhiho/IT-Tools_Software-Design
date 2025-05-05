package kostovite;

import java.util.*;
import java.security.SecureRandom;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MACAddressGenerator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int DEFAULT_QUANTITY = 1;
    private static final String DEFAULT_PREFIX = "64:16:7F";
    private static final String DEFAULT_CASE = "upper";
    private static final String DEFAULT_SEPARATOR = ":";
    private static final Pattern MAC_PREFIX_PATTERN = Pattern.compile(
            "^([0-9A-Fa-f]{2}[:\\-\\.]?){0,5}[0-9A-Fa-f]{0,2}$"
    );

    private final SecureRandom random = new SecureRandom();

    @Override
    public String getName() {
        return "MACAddressGenerator";
    }

    @Override
    public void execute() {
        System.out.println("MACAddressGenerator Plugin executed (standalone test)");
        try {
            // Test with default settings
            Map<String, Object> params = new HashMap<>();
            params.put("quantity", 1);
            params.put("prefix", DEFAULT_PREFIX);
            params.put("case", DEFAULT_CASE);
            params.put("separator", DEFAULT_SEPARATOR);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default settings): " + result1);

            // Test with multiple MAC addresses
            params.put("quantity", 5);
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (5 MAC addresses): " + result2);

            // Test with lowercase
            params.put("quantity", 2);
            params.put("case", "lower");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Lowercase): " + result3);

            // Test with different separator
            params.put("separator", "-");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Different separator): " + result4);

            // Test with custom prefix
            params.put("prefix", "00:1A:2B");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Custom prefix): " + result5);

            // Test with invalid prefix
            params.put("prefix", "ZZ:XX:YY");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Invalid prefix): " + result6);

            // Test with invalid quantity
            params.put("prefix", DEFAULT_PREFIX);
            params.put("quantity", -5);
            Map<String, Object> result7 = process(params);
            System.out.println("Test 7 (Invalid quantity): " + result7);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("customUI", false);
        metadata.put("name", "MAC address generator");
        metadata.put("icon", "DevicesOther");
        metadata.put("description", "Enter the quantity and prefix. MAC addresses will be generated in your chosen case (uppercase or lowercase)");
        metadata.put("id", "MACAddressGenerator");
        metadata.put("category", "Network");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: MAC Address Generator ---
        Map<String, Object> macSection = new HashMap<>();
        macSection.put("id", "macAddressGenerator");
        macSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // Quantity input
        inputs.add(Map.ofEntries(
                Map.entry("required", true),
                Map.entry("default", "1"),
                Map.entry("label", "Quantity:"),
                Map.entry("id", "quantity"),
                Map.entry("type", "number"),
                Map.entry("placeholder", "e.g., 1"),
                Map.entry("button", List.of("minus", "plus")),
                Map.entry("width", 450),
                Map.entry("height", 36),
                Map.entry("containerId", "input1")
        ));

        // Prefix input
        inputs.add(Map.ofEntries(
                Map.entry("label", "MAC address prefix:"),
                Map.entry("id", "prefix"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., 00:1A:2B"),
                Map.entry("default", DEFAULT_PREFIX),
                Map.entry("containerId", "input2"),
                Map.entry("width", 450),
                Map.entry("height", 36)
        ));

        // Case select
        Map<String, Object> caseOption1 = new HashMap<>();
        caseOption1.put("value", "upper");
        caseOption1.put("label", "Uppercase");

        Map<String, Object> caseOption2 = new HashMap<>();
        caseOption2.put("value", "lower");
        caseOption2.put("label", "Lowercase");

        List<Map<String, Object>> caseOptions = List.of(caseOption1, caseOption2);

        inputs.add(Map.ofEntries(
                Map.entry("label", "Case:"),
                Map.entry("id", "case"),
                Map.entry("default", DEFAULT_CASE),
                Map.entry("type", "select"),
                Map.entry("options", caseOptions),
                Map.entry("containerId", "input3")
        ));

        // Separator select
        Map<String, Object> sepOption1 = new HashMap<>();
        sepOption1.put("value", ":");
        sepOption1.put("label", ":");

        Map<String, Object> sepOption2 = new HashMap<>();
        sepOption2.put("value", "-");
        sepOption2.put("label", "-");

        Map<String, Object> sepOption3 = new HashMap<>();
        sepOption3.put("value", ".");
        sepOption3.put("label", ".");

        List<Map<String, Object>> sepOptions = List.of(sepOption1, sepOption2, sepOption3);

        inputs.add(Map.ofEntries(
                Map.entry("label", "Separator:"),
                Map.entry("id", "separator"),
                Map.entry("default", DEFAULT_SEPARATOR),
                Map.entry("type", "select"),
                Map.entry("options", sepOptions),
                Map.entry("containerId", "input4")
        ));

        macSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("id", "macAddress"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 60),
                Map.entry("containerId", "output")
        ));
        macSection.put("outputs", outputs);

        sections.add(macSection);

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

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get parameters
            int quantity = getIntParam(input, "quantity", DEFAULT_QUANTITY);
            String prefix = getStringParam(input, "prefix", DEFAULT_PREFIX);
            String caseFormat = getStringParam(input, "case", DEFAULT_CASE);
            String separator = getStringParam(input, "separator", DEFAULT_SEPARATOR);

            // Validate inputs
            if (quantity <= 0 || quantity > 1000) { // Limit to reasonable amount
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Quantity must be between 1 and 1000.");
            }

            // Clean and validate prefix
            prefix = normalizePrefix(prefix);
            if (!isValidMacPrefix(prefix)) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid MAC address prefix. It should contain only hexadecimal characters (0-9, A-F).");
            }

            // Generate MAC addresses
            List<String> macAddresses = generateMacAddresses(quantity, prefix, caseFormat, separator);

            // Format output
            String output = formatOutput(macAddresses);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("macAddress", output);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating MAC addresses: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Normalize MAC prefix by removing separators and ensuring even length
    private String normalizePrefix(String prefix) {
        // Remove any existing separators
        prefix = prefix.replaceAll("[:\\-\\.]", "").trim();

        // Make sure the length is even (pairs of hex digits)
        if (prefix.length() % 2 != 0) {
            prefix = prefix + "0";
        }

        return prefix;
    }

    // Validate MAC prefix contains only hex characters
    private boolean isValidMacPrefix(String prefix) {
        // Check if prefix contains only hexadecimal characters
        return prefix.matches("[0-9A-Fa-f]*");
    }

    // Generate the specified number of MAC addresses
    private List<String> generateMacAddresses(int quantity, String prefix, String caseFormat, String separator) {
        List<String> macAddresses = new ArrayList<>();

        // Determine how many octets are already defined by the prefix
        int prefixLength = prefix.length();
        int remainingBytes = 6 - (prefixLength / 2);

        for (int i = 0; i < quantity; i++) {
            StringBuilder macBuilder = new StringBuilder();

            // Add the prefix with proper formatting
            for (int j = 0; j < prefixLength; j += 2) {
                if (j > 0) {
                    macBuilder.append(separator);
                }
                macBuilder.append(prefix.substring(j, j + 2));
            }

            // Add separator if we have a prefix and will add more octets
            if (prefixLength > 0 && remainingBytes > 0) {
                macBuilder.append(separator);
            }

            // Add random octets for the remaining part
            for (int j = 0; j < remainingBytes; j++) {
                if (j > 0) {
                    macBuilder.append(separator);
                }

                // Generate random byte
                int randomByte = random.nextInt(256);
                String byteStr = String.format("%02x", randomByte);

                macBuilder.append(byteStr);
            }

            // Apply case formatting
            String macAddress = macBuilder.toString();
            if ("upper".equals(caseFormat)) {
                macAddress = macAddress.toUpperCase();
            } else {
                macAddress = macAddress.toLowerCase();
            }

            macAddresses.add(macAddress);
        }

        return macAddresses;
    }

    // Format the list of MAC addresses for output
    private String formatOutput(List<String> macAddresses) {
        return String.join("\n", macAddresses);
    }

    // Helper methods to get parameters with different types
    private int getIntParam(Map<String, Object> input, String key, int defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
}