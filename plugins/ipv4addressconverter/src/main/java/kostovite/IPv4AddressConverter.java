package kostovite;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IPv4AddressConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final String DEFAULT_IP = "192.168.1.1";
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    @Override
    public String getName() {
        return "IPv4AddressConverter";
    }

    @Override
    public void execute() {
        System.out.println("IPv4AddressConverter Plugin executed (standalone test)");
        try {
            // Test with default IP
            Map<String, Object> params = new HashMap<>();
            params.put("ipv4Address", DEFAULT_IP);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default IP 192.168.1.1): " + result1);

            // Test with loopback address
            params.put("ipv4Address", "127.0.0.1");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Loopback 127.0.0.1): " + result2);

            // Test with Google DNS
            params.put("ipv4Address", "8.8.8.8");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Google DNS 8.8.8.8): " + result3);

            // Test with invalid IP
            params.put("ipv4Address", "256.256.256.256");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid IP 256.256.256.256): " + result4);

            // Test with malformed IP
            params.put("ipv4Address", "192.168.1");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Malformed IP 192.168.1): " + result5);

            // Test with empty input
            params.put("ipv4Address", "");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Empty input): " + result6);

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
        metadata.put("name", "IPv4 address converter");
        metadata.put("icon", "Dns");
        metadata.put("description", "Convert an IP address into decimal, binary, hexadecimal, or even an IPv6 representation of it.");
        metadata.put("id", "IPv4AddressConverter");
        metadata.put("category", "Network");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: IPv4 Address Converter ---
        Map<String, Object> ipv4Section = new HashMap<>();
        ipv4Section.put("id", "ipv4AddressConverter");
        ipv4Section.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("label", "The ipv4 address:"),
                Map.entry("id", "ipv4Address"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", DEFAULT_IP),
                Map.entry("containerId", "input"),
                Map.entry("width", 600),
                Map.entry("height", 36)
        ));
        ipv4Section.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();

        // Decimal output
        outputs.add(Map.ofEntries(
                Map.entry("label", "Decimal"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "decimalOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));

        // Hexadecimal output
        outputs.add(Map.ofEntries(
                Map.entry("label", "Hexadecimal"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "hexadecimalOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));

        // Binary output
        outputs.add(Map.ofEntries(
                Map.entry("label", "Binary"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "binaryOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));

        // IPv6 output
        outputs.add(Map.ofEntries(
                Map.entry("label", "IPv6"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "ipv6Output"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));

        // IPv6 short output
        outputs.add(Map.ofEntries(
                Map.entry("label", "IPv6 (short)"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "ipv6ShortOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));

        ipv4Section.put("outputs", outputs);

        sections.add(ipv4Section);

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
            // Get the IPv4 address from input
            String ipv4Address = getStringParam(input, "ipv4Address", "");

            // Validation
            if (ipv4Address.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "IPv4 address is required.");
            }

            // Validate IPv4 format
            if (!isValidIPv4(ipv4Address)) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid IPv4 address format. Please use the format xxx.xxx.xxx.xxx where each octet is between 0-255.");
            }

            // Convert to various formats
            long decimalValue = convertToDecimal(ipv4Address);
            String hexadecimalValue = convertToHexadecimal(decimalValue);
            String binaryValue = convertToBinary(ipv4Address);
            String ipv6Value = convertToIPv6(ipv4Address);
            String ipv6ShortValue = shortenIPv6(ipv6Value);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("decimalOutput", String.valueOf(decimalValue));
            result.put("hexadecimalOutput", hexadecimalValue);
            result.put("binaryOutput", binaryValue);
            result.put("ipv6Output", ipv6Value);
            result.put("ipv6ShortOutput", ipv6ShortValue);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing IPv4 conversion: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Validate IPv4 address format
    private boolean isValidIPv4(String ip) {
        Matcher matcher = IPV4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // Convert IPv4 to decimal representation
    private long convertToDecimal(String ipv4) {
        String[] octets = ipv4.split("\\.");
        long result = 0;

        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(octets[i]);
        }

        return result;
    }

    // Convert decimal to hexadecimal representation
    private String convertToHexadecimal(long decimal) {
        return "0x" + Long.toHexString(decimal).toUpperCase();
    }

    // Convert IPv4 to binary representation
    private String convertToBinary(String ipv4) {
        String[] octets = ipv4.split("\\.");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            String binary = Integer.toBinaryString(Integer.parseInt(octets[i]));
            // Pad to 8 bits
            while (binary.length() < 8) {
                binary = "0" + binary;
            }

            if (i > 0) {
                result.append(".");
            }
            result.append(binary);
        }

        return result.toString();
    }

    // Convert IPv4 to IPv6 representation (mapped address)
    private String convertToIPv6(String ipv4) {
        String[] octets = ipv4.split("\\.");
        StringBuilder result = new StringBuilder("::ffff:");

        // Format first two octets as a hex pair
        String hex1 = Integer.toHexString(Integer.parseInt(octets[0]));
        if (hex1.length() == 1) hex1 = "0" + hex1;
        String hex2 = Integer.toHexString(Integer.parseInt(octets[1]));
        if (hex2.length() == 1) hex2 = "0" + hex2;
        result.append(hex1).append(hex2).append(":");

        // Format second two octets as a hex pair
        String hex3 = Integer.toHexString(Integer.parseInt(octets[2]));
        if (hex3.length() == 1) hex3 = "0" + hex3;
        String hex4 = Integer.toHexString(Integer.parseInt(octets[3]));
        if (hex4.length() == 1) hex4 = "0" + hex4;
        result.append(hex3).append(hex4);

        return result.toString();
    }

    // Create a shortened IPv6 representation
    private String shortenIPv6(String ipv6) {
        // For IPv4-mapped IPv6 addresses, we can shorten to ::ffff:a.b.c.d format
        if (ipv6.startsWith("::ffff:")) {
            String hexPart = ipv6.substring(7); // e.g., "c0a8:0101"

            // Extract the hex values and convert back to decimal
            String[] hexParts = hexPart.split(":");

            if (hexParts.length == 2) {
                // Convert first hex pair to decimals (e.g., "c0a8" -> 192.168)
                int dec1 = Integer.parseInt(hexParts[0].substring(0, 2), 16);
                int dec2 = Integer.parseInt(hexParts[0].substring(2, 4), 16);

                // Convert second hex pair to decimals (e.g., "0101" -> 1.1)
                int dec3 = Integer.parseInt(hexParts[1].substring(0, 2), 16);
                int dec4 = Integer.parseInt(hexParts[1].substring(2, 4), 16);

                return "::ffff:" + dec1 + "." + dec2 + "." + dec3 + "." + dec4;
            }
        }

        return ipv6; // If not in the expected format, return as is
    }

    // Helper method to get string parameters
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
}