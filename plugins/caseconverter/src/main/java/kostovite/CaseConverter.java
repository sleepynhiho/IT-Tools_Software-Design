package kostovite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

public class CaseConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    // Pattern to find word boundaries and special characters
    private static final Pattern WORD_BOUNDARY_PATTERN = Pattern.compile("[\\s_\\-./\\\\]+|(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
    private static final Random RANDOM = new Random();

    @Override
    public String getName() {
        return "CaseConverter";
    }

    @Override
    public void execute() {
        System.out.println("CaseConverter Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test with a simple string
            params.put("stringInput", "lorem ipsum dolor sit amet");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Simple string): " + result1);

            // Test with a mixed case string with special chars
            params.put("stringInput", "Hello World! This-is_a.test/string\\with special-chars");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Mixed case with special chars): " + result2);

            // Test with camelCase already
            params.put("stringInput", "thisIsCamelCase");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (camelCase input): " + result3);

            // Test with snake_case already
            params.put("stringInput", "this_is_snake_case");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (snake_case input): " + result4);

            // Test with multiple spaces and empty parts
            params.put("stringInput", "  multiple   spaces   between   words  ");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Multiple spaces): " + result5);

            // Test with empty input
            params.put("stringInput", "");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Empty input): " + result6);

            // Test with numbers and symbols
            params.put("stringInput", "user123@example.com - 42 tests & examples!");
            Map<String, Object> result7 = process(params);
            System.out.println("Test 7 (Numbers and symbols): " + result7);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "CaseConverter");
        metadata.put("name", "Case converter");
        metadata.put("description", "Transform the case of a string and choose between different formats");
        metadata.put("icon", "TextFields");
        metadata.put("category", "Converter");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Case Converter ---
        Map<String, Object> caseConverterSection = new HashMap<>();
        caseConverterSection.put("id", "caseConverterSection");
        caseConverterSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "stringInput"),
                Map.entry("label", "Your string:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "Enter your string"),
                Map.entry("required", true),
                Map.entry("default", "lorem ipsum dolor sit amet"),
                Map.entry("containerId", "input"),
                Map.entry("width", 250),
                Map.entry("height", 36)
        ));
        caseConverterSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();

        // Add all the case format outputs
        outputs.add(createOutputField("camelOutput", "camelCase"));
        outputs.add(createOutputField("capitalizedOutput", "Capitalized"));
        outputs.add(createOutputField("constantOutput", "CONSTANT_CASE"));
        outputs.add(createOutputField("dotOutput", "dot.case"));
        outputs.add(createOutputField("headerOutput", "Header-Case"));
        outputs.add(createOutputField("lowercaseOutput", "lowercase"));
        outputs.add(createOutputField("mockingOutput", "Mocking Case"));
        outputs.add(createOutputField("nocaseOutput", "no case"));
        outputs.add(createOutputField("paramOutput", "param-case"));
        outputs.add(createOutputField("pascalOutput", "PascalCase"));
        outputs.add(createOutputField("pathOutput", "path/case"));
        outputs.add(createOutputField("sentenceOutput", "Sentence case"));
        outputs.add(createOutputField("snakeOutput", "snake_case"));
        outputs.add(createOutputField("titleOutput", "Title Case"));
        outputs.add(createOutputField("uppercaseOutput", "UPPERCASE"));

        caseConverterSection.put("outputs", outputs);

        sections.add(caseConverterSection);

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
                Map.entry("monospace", true),
                Map.entry("containerId", "output"),
                Map.entry("width", 250),
                Map.entry("height", 36),
                Map.entry("multiline", false),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside"))
        );
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get input parameter
            String stringInput = getStringParam(input, "stringInput", null);

            // --- Validation ---
            if (stringInput == null) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input string is required.");
            }

            // Trim the input
            stringInput = stringInput.trim();

            // Split into words for processing
            List<String> words = splitIntoWords(stringInput);

            if (words.isEmpty()) {
                // Return empty result for all formats if input is empty
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("camelOutput", "");
                result.put("capitalizedOutput", "");
                result.put("constantOutput", "");
                result.put("dotOutput", "");
                result.put("headerOutput", "");
                result.put("lowercaseOutput", "");
                result.put("mockingOutput", "");
                result.put("nocaseOutput", "");
                result.put("paramOutput", "");
                result.put("pascalOutput", "");
                result.put("pathOutput", "");
                result.put("sentenceOutput", "");
                result.put("snakeOutput", "");
                result.put("titleOutput", "");
                result.put("uppercaseOutput", "");
                return result;
            }

            // Convert to all the different case formats
            String camelCase = toCamelCase(words);
            String capitalized = toCapitalized(words);
            String constantCase = toConstantCase(words);
            String dotCase = toDotCase(words);
            String headerCase = toHeaderCase(words);
            String lowercase = toLowercase(words);
            String mockingCase = toMockingCase(stringInput);
            String noCase = toNoCase(words);
            String paramCase = toParamCase(words);
            String pascalCase = toPascalCase(words);
            String pathCase = toPathCase(words);
            String sentenceCase = toSentenceCase(words);
            String snakeCase = toSnakeCase(words);
            String titleCase = toTitleCase(words);
            String uppercase = toUppercase(words);

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("camelOutput", camelCase);
            result.put("capitalizedOutput", capitalized);
            result.put("constantOutput", constantCase);
            result.put("dotOutput", dotCase);
            result.put("headerOutput", headerCase);
            result.put("lowercaseOutput", lowercase);
            result.put("mockingOutput", mockingCase);
            result.put("nocaseOutput", noCase);
            result.put("paramOutput", paramCase);
            result.put("pascalOutput", pascalCase);
            result.put("pathOutput", pathCase);
            result.put("sentenceOutput", sentenceCase);
            result.put("snakeOutput", snakeCase);
            result.put("titleOutput", titleCase);
            result.put("uppercaseOutput", uppercase);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing case conversion: " + e.getMessage());
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
     * Split a string into words based on common delimiters and case changes
     */
    private List<String> splitIntoWords(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }

        // Split on spaces, underscores, hyphens, dots, slashes, backslashes,
        // and camelCase boundaries (e.g., "camelCase" -> "camel", "Case")
        String[] parts = WORD_BOUNDARY_PATTERN.split(input);

        List<String> words = new ArrayList<>();
        for (String part : parts) {
            // Skip empty parts
            if (!part.isEmpty()) {
                // Remove any remaining non-alphanumeric chars (except underscore) for cleaner words
                // Keep digits since they may be relevant (e.g., "user123")
                words.add(part);
            }
        }

        return words;
    }

    /**
     * Convert to camelCase
     */
    private String toCamelCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder(words.get(0).toLowerCase());

        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * Convert to Capitalized (first letter of string capitalized)
     */
    private String toCapitalized(List<String> words) {
        if (words.isEmpty()) return "";

        String firstWord = words.get(0);
        if (firstWord.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(firstWord.charAt(0)))
                .append(firstWord.substring(1).toLowerCase());

        for (int i = 1; i < words.size(); i++) {
            result.append(" ").append(words.get(i).toLowerCase());
        }

        return result.toString();
    }

    /**
     * Convert to CONSTANT_CASE
     */
    private String toConstantCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toUpperCase());
            if (i < words.size() - 1) {
                result.append("_");
            }
        }

        return result.toString();
    }

    /**
     * Convert to dot.case
     */
    private String toDotCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append(".");
            }
        }

        return result.toString();
    }

    /**
     * Convert to Header-Case
     */
    private String toHeaderCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
            if (i < words.size() - 1) {
                result.append("-");
            }
        }

        return result.toString();
    }

    /**
     * Convert to lowercase
     */
    private String toLowercase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Convert to MoCkInG cAsE (randomly alternating case)
     */
    private String toMockingCase(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            // 50% chance of uppercase, 50% chance of lowercase
            if (RANDOM.nextBoolean()) {
                result.append(Character.toUpperCase(c));
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Convert to no case (lowercase with spaces)
     */
    private String toNoCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Convert to param-case (kebab-case)
     */
    private String toParamCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append("-");
            }
        }

        return result.toString();
    }

    /**
     * Convert to PascalCase
     */
    private String toPascalCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * Convert to path/case
     */
    private String toPathCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append("/");
            }
        }

        return result.toString();
    }

    /**
     * Convert to Sentence case
     */
    private String toSentenceCase(List<String> words) {
        if (words.isEmpty()) return "";

        String firstWord = words.get(0);
        if (firstWord.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(firstWord.charAt(0)))
                .append(firstWord.substring(1).toLowerCase());

        for (int i = 1; i < words.size(); i++) {
            result.append(" ").append(words.get(i).toLowerCase());
        }

        return result.toString();
    }

    /**
     * Convert to snake_case
     */
    private String toSnakeCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toLowerCase());
            if (i < words.size() - 1) {
                result.append("_");
            }
        }

        return result.toString();
    }

    /**
     * Convert to Title Case (capitalize each word)
     */
    private String toTitleCase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
            if (i < words.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Convert to UPPERCASE
     */
    private String toUppercase(List<String> words) {
        if (words.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            result.append(words.get(i).toUpperCase());
            if (i < words.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }
}