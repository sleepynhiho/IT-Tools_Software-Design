package kostovite;

import java.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPv4RangeExpander implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "IPv4RangeExpander";
    }

    @Override
    public void execute() {
        System.out.println("IPv4RangeExpander Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startAddress", "192.168.1.1");      // Test start IP
            params.put("endAddress", "192.168.6.255");      // Test end IP

            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Range 192.168.1.1 - 192.168.6.255): " + result1);

            params.put("startAddress", "10.0.0.1");         // Test another range
            params.put("endAddress", "10.0.0.255");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Range 10.0.0.1 - 10.0.0.255): " + result2);

            params.put("startAddress", "172.16.0.0");       // Test another range
            params.put("endAddress", "172.31.255.255");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Range 172.16.0.0 - 172.31.255.255): " + result3);

            params.put("startAddress", "InvalidIP");        // Test with invalid IP
            params.put("endAddress", "192.168.1.100");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid Start IP): " + result4);

            params.put("startAddress", "192.168.1.100");    // Test with end IP < start IP
            params.put("endAddress", "192.168.1.1");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (End IP < Start IP): " + result5);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "IPv4RangeExpander");
        metadata.put("name", "IPv4 range expander");
        metadata.put("description", "Given a start and an end IPv4 address, this tool calculates a valid IPv4 subnet along with its CIDR notation.");
        metadata.put("icon", "Dns");
        metadata.put("category", "Network");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Main IPv4 Range Expander ---
        Map<String, Object> mainSection = new HashMap<>();
        mainSection.put("id", "ipv4RangeExpander");
        mainSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "startAddress"),
                Map.entry("label", "Start address"),
                Map.entry("placeholder", "Start IPv4 address"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("multiline", false),
                Map.entry("default", "192.168.1.1"),
                Map.entry("containerId", "input1"),
                Map.entry("width", 300),
                Map.entry("height", 36),
                Map.entry("monospace", true)
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "endAddress"), // Fixed the ID from the sample
                Map.entry("label", "End address"),
                Map.entry("placeholder", "End IPv4 address"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("multiline", false),
                Map.entry("default", "192.168.6.255"),
                Map.entry("containerId", "input2"),
                Map.entry("width", 300),
                Map.entry("height", 36),
                Map.entry("monospace", true)
        ));
        mainSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(createOutputField("start_new", "Start address (New)"));
        outputs.add(createOutputField("end_new", "End address (New)"));
        outputs.add(createOutputField("range_old", "Addresses in range (Old)"));
        outputs.add(createOutputField("range_new", "Addresses in range (New)"));
        outputs.add(createOutputField("cidr_new", "CIDR"));
        mainSection.put("outputs", outputs);

        sections.add(mainSection);

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
                Map.entry("containerId", "output"),
                Map.entry("width", 600),
                Map.entry("height", 36),
                Map.entry("monospace", true)
        );
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get input parameters
            String startAddressStr = getStringParam(input, "startAddress", null);
            String endAddressStr = getStringParam(input, "endAddress", null);

            // --- Validation ---
            if (startAddressStr == null) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Start address is required.");
            }
            if (endAddressStr == null) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "End address is required.");
            }

            // Validate IP addresses format
            long startIpLong;
            long endIpLong;

            try {
                startIpLong = ipToLong(startAddressStr);
            } catch (UnknownHostException e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid start IP address format.");
            }

            try {
                endIpLong = ipToLong(endAddressStr);
            } catch (UnknownHostException e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid end IP address format.");
            }

            // Ensure start IP is less than or equal to end IP
            if (startIpLong > endIpLong) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Start IP must be less than or equal to end IP.");
            }

            // Calculate the number of addresses in original range
            long originalRangeSize = endIpLong - startIpLong + 1;

            // Calculate CIDR and new subnet
            int cidrPrefix = calculateCidrPrefix(startIpLong, endIpLong);
            long netmask = calculateNetmask(cidrPrefix);

            // Calculate network address (start of subnet)
            long networkAddress = startIpLong & netmask;

            // Calculate broadcast address (end of subnet)
            long broadcastAddress = networkAddress | (~netmask & 0xFFFFFFFFL);

            // Calculate new range size
            long newRangeSize = broadcastAddress - networkAddress + 1;

            // Convert back to string representations
            String newStartAddress = longToIp(networkAddress);
            String newEndAddress = longToIp(broadcastAddress);
            String cidrNotation = newStartAddress + "/" + cidrPrefix;

            // Prepare results
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("start_new", newStartAddress);
            result.put("end_new", newEndAddress);
            result.put("range_old", String.valueOf(originalRangeSize));
            result.put("range_new", String.valueOf(newRangeSize));
            result.put("cidr_new", cidrNotation);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing IPv4 range expansion: " + e.getMessage());
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

    /**
     * Convert an IP address to its long representation
     */
    private long ipToLong(String ipAddress) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(ipAddress).getAddress();
        long result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result & 0xFFFFFFFFL; // Ensure unsigned 32-bit value
    }

    /**
     * Convert a long representation back to an IP address string
     */
    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    /**
     * Calculate the CIDR prefix that covers the IP range
     */
    private int calculateCidrPrefix(long startIp, long endIp) {
        long diff = startIp ^ endIp;
        int leadingZeros = 0;

        // Count leading zeros in the binary difference
        for (int i = 31; i >= 0; i--) {
            if ((diff & (1L << i)) == 0) {
                leadingZeros++;
            } else {
                break;
            }
        }

        // Calculate the smallest subnet that contains both IPs
        int cidrPrefix = 32 - (32 - leadingZeros);

        // Check if the range fits perfectly in the calculated CIDR
        long mask = calculateNetmask(cidrPrefix);
        long networkAddress = startIp & mask;
        long broadcastAddress = networkAddress | (~mask & 0xFFFFFFFFL);

        // If the calculated subnet doesn't fully contain the range, decrease prefix
        while (networkAddress > startIp || broadcastAddress < endIp) {
            cidrPrefix--;
            mask = calculateNetmask(cidrPrefix);
            networkAddress = startIp & mask;
            broadcastAddress = networkAddress | (~mask & 0xFFFFFFFFL);

            // Safety check to prevent infinite loop
            if (cidrPrefix <= 0) {
                break;
            }
        }

        return cidrPrefix;
    }

    /**
     * Calculate the netmask from CIDR prefix
     */
    private long calculateNetmask(int cidrPrefix) {
        if (cidrPrefix == 0) {
            return 0;
        }
        return 0xFFFFFFFFL << (32 - cidrPrefix) & 0xFFFFFFFFL;
    }
}