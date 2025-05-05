package kostovite;

import java.util.*;
import java.security.SecureRandom;

public class RandomPortGenerator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int KNOWN_PORTS_END = 1023;
    private static final int MAX_PORT = 65535;
    private static final int DEFAULT_NUM_PORTS = 5;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String getName() {
        return "RandomPortGenerator";
    }

    @Override
    public void execute() {
        System.out.println("RandomPortGenerator Plugin executed (standalone test)");
        try {
            // Test with default settings
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default settings): " + result1);

            // Test with custom number of ports (10)
            params.put("numberOfPorts", 10);
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (10 ports): " + result2);

            // Test with invalid number of ports
            params.put("numberOfPorts", -5);
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Invalid port count): " + result3);

            // Test with extremely large number of ports
            params.put("numberOfPorts", 1000);
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Large port count): " + result4);

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
        metadata.put("name", "Random port generator");
        metadata.put("icon", "Shuffle");
        metadata.put("description", "Generate random port numbers outside of the range of \"known\" ports (0-1023).");
        metadata.put("id", "RandomPortGenerator");
        metadata.put("category", "Development");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Random Port ---
        Map<String, Object> randomPortSection = new HashMap<>();
        randomPortSection.put("id", "randomPort");
        randomPortSection.put("label", "");

        // --- No inputs, only outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("id", "randomPort"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 150),
                Map.entry("containerId", "output")
        ));
        randomPortSection.put("outputs", outputs);

        sections.add(randomPortSection);

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
            // Get the number of ports to generate (default is 5)
            int numberOfPorts = getIntParam(input, "numberOfPorts", DEFAULT_NUM_PORTS);

            // Validate number of ports
            if (numberOfPorts <= 0) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Number of ports must be positive.");
            }

            // Cap the number of ports at 100 to prevent generating too many
            if (numberOfPorts > 100) {
                numberOfPorts = 100;
            }

            // Generate random ports
            Set<Integer> ports = generateRandomPorts(numberOfPorts);

            // Format output
            String portOutput = formatPortOutput(ports);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("randomPort", portOutput);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating random ports: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Generate a set of unique random ports
    private Set<Integer> generateRandomPorts(int count) {
        Set<Integer> ports = new HashSet<>();

        while (ports.size() < count) {
            // Generate ports in the range KNOWN_PORTS_END+1 to MAX_PORT
            int port = random.nextInt(MAX_PORT - KNOWN_PORTS_END) + KNOWN_PORTS_END + 1;
            ports.add(port);
        }

        return ports;
    }

    // Format the ports for display
    private String formatPortOutput(Set<Integer> ports) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generated ").append(ports.size()).append(" random port");
        if (ports.size() != 1) {
            sb.append("s");
        }
        sb.append(":\n\n");

        List<Integer> sortedPorts = new ArrayList<>(ports);
        Collections.sort(sortedPorts);

        for (Integer port : sortedPorts) {
            sb.append(port).append("\n");
        }

        return sb.toString();
    }

    // Helper method to get integer parameters
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
}