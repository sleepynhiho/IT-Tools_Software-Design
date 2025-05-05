package kostovite;

import java.util.*;

public class NumeronymGenerator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "NumeronymGenerator";
    }

    @Override
    public void execute() {
        System.out.println("NumeronymGenerator Plugin executed (standalone test)");
        try {
            // Test with simple word
            Map<String, Object> params = new HashMap<>();
            params.put("inputText", "internationalization");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (internationalization): " + result1);

            // Test with another word
            params.put("inputText", "accessibility");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (accessibility): " + result2);

            // Test with multiple words
            params.put("inputText", "software engineering");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (software engineering): " + result3);

            // Test with short word
            params.put("inputText", "dog");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (dog): " + result4);

            // Test with very short word
            params.put("inputText", "hi");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (hi): " + result5);

            // Test with empty input
            params.put("inputText", "");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (empty input): " + result6);

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
        metadata.put("name", "Numeronym generator");
        metadata.put("icon", "TextFields");
        metadata.put("description", "A numeronym is a word where a number is used to form an abbreviation. For example, \"i18n\" is a numeronym of \"internationalization\" where 18 stands for the number of letters between the first i and the last n in the word.");
        metadata.put("id", "NumeronymGenerator");
        metadata.put("category", "Text");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Numeronym Generator ---
        Map<String, Object> numeronymSection = new HashMap<>();
        numeronymSection.put("id", "numeronym");
        numeronymSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("placeholder", "Enter a word, e.g., internationalization"),
                Map.entry("required", true),
                Map.entry("multiline", false),
                Map.entry("containerId", "input"),
                Map.entry("id", "inputText"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 36)
        ));
        numeronymSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "numeronymOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));
        numeronymSection.put("outputs", outputs);

        sections.add(numeronymSection);

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
            // Get the input text
            String inputText = getStringParam(input, "inputText", "");

            // Validation
            if (inputText.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Please enter a word or phrase to generate a numeronym.");
            }

            // Generate numeronym(s)
            String numeronym = generateNumeronym(inputText);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("numeronymOutput", numeronym);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating numeronym: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Generate a numeronym from the input text
    private String generateNumeronym(String input) {
        // Handle multi-word input
        if (input.contains(" ")) {
            String[] words = input.split("\\s+");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    result.append(" ");
                }
                result.append(generateSingleWordNumeronym(words[i]));
            }

            return result.toString();
        } else {
            return generateSingleWordNumeronym(input);
        }
    }

    // Generate a numeronym for a single word
    private String generateSingleWordNumeronym(String word) {
        int length = word.length();

        // Words with 3 or fewer characters don't need numeronyms
        if (length <= 3) {
            return word;
        }

        // Create numeronym: first letter + number of omitted letters + last letter
        char firstChar = word.charAt(0);
        char lastChar = word.charAt(length - 1);
        int middleLength = length - 2;

        return firstChar + String.valueOf(middleLength) + lastChar;
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