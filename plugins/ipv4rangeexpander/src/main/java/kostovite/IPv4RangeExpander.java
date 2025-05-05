package kostovite;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class IPv4RangeExpander implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final String DEFAULT_START_IP = "192.168.1.1";
    private static final String DEFAULT_END_IP = "192.168.6.255";
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    @Override
    public String getName() {
        return "IPv4RangeExpander";
    }

    @Override
    public void execute() {
        System.out.println("IPv4RangeExpander Plugin executed (standalone test)");
        try {
            // Test with default IPs
            Map<String, Object> params = new HashMap<>();
            params.put("startAddress", DEFAULT_START_IP);
            params.put("endAddress", DEFAULT_END_IP);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default range): " + result1);

            // Test with smaller range
            params.put("startAddress", "10.0.0.1");
            params.put("endAddress", "10.0.0.10");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Small range): " + result2);

            // Test with large range
            params.put("startAddress", "172.16.0.0");
            params.put("endAddress", "172.31.255.255");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Large range): " + result3);

            // Test with invalid start IP
            params.put("startAddress", "256.256.256.256");
            params.put("endAddress", "10.0.0.10");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid start IP): " + result4);

            // Test with invalid end IP
            params.put("startAddress", "10.0.0.1");
            params.put("endAddress", "300.400.500.600");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Invalid end IP): " + result5);

            // Test with start > end
            params.put("startAddress", "192.168.5.10");
            params.put("endAddress", "192.168.1.5");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Start > End): " + result6);

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
        metadata.put("name", "IPv4 range expander");
        metadata.put("icon", "Dns");
        metadata.put("description", "Given a start and an end IPv4 address, this tool calculates a valid IPv4 subnet along with its CIDR notation.");
        metadata.put("id", "IPv4RangeExpander");
        metadata.put("category", "Network");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: IPv4 Range Expander ---
        Map<String, Object> ipv4Section = new HashMap<>();
        ipv4Section.put("id", "ipv4RangeExpander");
        ipv4Section.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // Start address input
        inputs.add(Map.ofEntries(
                Map.entry("monospace", true),
                Map.entry("label", "Start address"),
                Map.entry("placeholder", "Start IPv4 address"),
                Map.entry("required", true),
                Map.entry("multiline", false),
                Map.entry("id", "startAddress"),
                Map.entry("type", "text"),
                Map.entry("default", DEFAULT_START_IP),
                Map.entry("width", 300),
                Map.entry("height", 36),
                Map.entry("containerId", "input1")
        ));

        // End address input
        inputs.add(Map.ofEntries(
                Map.entry("monospace", true),
                Map.entry("label", "End address"),
                Map.entry("placeholder", "End IPv4 address"),
                Map.entry("required", true),
                Map.entry("multiline", false),
                Map.entry("id", "endAddress"), // Fixed from "startAddress" to "endAddress"
                Map.entry("type", "text"),
                Map.entry("default", DEFAULT_END_IP),
                Map.entry("width", 300),
                Map.entry("height", 36),
                Map.entry("containerId", "input2")
        ));

        ipv4Section.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();

        // Result table
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.ofEntries(
                Map.entry("id", "start_old"),
                Map.entry("description", "Start address (Old)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "start_new"),
                Map.entry("description", "Start address (New)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "end_old"),
                Map.entry("description", "End address (Old)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "end_new"),
                Map.entry("description", "End address (New)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "range_old"),
                Map.entry("description", "Addresses in range (Old)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "range_new"),
                Map.entry("description", "Addresses in range (New)"),
                Map.entry("value", "")
        ));
        rows.add(Map.ofEntries(
                Map.entry("id", "cidr_new"),
                Map.entry("description", "CIDR"),
                Map.entry("value", "")
        ));

        outputs.add(Map.ofEntries(
                Map.entry("id", "result"),
                Map.entry("type", "table"),
                Map.entry("label", ""),
                Map.entry("rows", rows)
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
            // Get the IPv4 addresses from input
            String startAddress = getStringParam(input, "startAddress", "");
            String endAddress = getStringParam(input, "endAddress", "");

            // Validation
            if (startAddress.trim().isEmpty() || endAddress.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Both start and end IPv4 addresses are required.");
            }

            // Validate IPv4 formats
            if (!isValidIPv4(startAddress)) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid start IPv4 address format. Please use the format xxx.xxx.xxx.xxx where each octet is between 0-255.");
            }

            if (!isValidIPv4(endAddress)) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid end IPv4 address format. Please use the format xxx.xxx.xxx.xxx where each octet is between 0-255.");
            }

            // Convert to integers for comparison
            long startIp = ipToLong(startAddress);
            long endIp = ipToLong(endAddress);

            // Ensure start address is less than or equal to end address
            if (startIp > endIp) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Start address must be less than or equal to end address.");
            }

            // Calculate subnet information
            long rangeOld = endIp - startIp + 1;

            // Find the smallest power of 2 that can hold the range
            int cidrBits = 32 - (int) Math.ceil(Math.log(rangeOld) / Math.log(2));
            if (cidrBits < 0) {
                cidrBits = 0; // Handle very large ranges
            }

            // Calculate network mask
            long mask = 0xFFFFFFFF << (32 - cidrBits);

            // Calculate network address (subnet start)
            long networkAddress = startIp & mask;

            // Calculate broadcast address (subnet end)
            long broadcastAddress = networkAddress | ~mask & 0xFFFFFFFFL;

            // Calculate the new range size
            long rangeNew = broadcastAddress - networkAddress + 1;

            // Convert back to IP address strings
            String startNew = longToIp(networkAddress);
            String endNew = longToIp(broadcastAddress);

            // Format the CIDR notation
            String cidrNotation = startNew + "/" + cidrBits;

            // Create result map with table rows
            Map<String, String> resultRows = new HashMap<>();
            resultRows.put("start_old", startAddress);
            resultRows.put("start_new", startNew);
            resultRows.put("end_old", endAddress);
            resultRows.put("end_new", endNew);
            resultRows.put("range_old", String.valueOf(rangeOld));
            resultRows.put("range_new", String.valueOf(rangeNew));
            resultRows.put("cidr_new", cidrNotation);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("result", resultRows);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing IPv4 range expansion: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Validate IPv4 address format
    private boolean isValidIPv4(String ip) {
        Matcher matcher = IPV4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // Convert IP address to long integer
    private long ipToLong(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        long result = 0;

        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(octets[i]);
        }

        return result;
    }

    // Convert long integer to IP address
    private String longToIp(long ip) {
        StringBuilder result = new StringBuilder();

        for (int i = 3; i >= 0; i--) {
            result.append((ip >> (i * 8)) & 0xFF);
            if (i > 0) {
                result.append(".");
            }
        }

        return result.toString();
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