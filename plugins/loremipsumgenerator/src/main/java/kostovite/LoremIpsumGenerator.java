package kostovite;

import java.util.*;

public class LoremIpsumGenerator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int DEFAULT_PARAGRAPHS = 1;
    private static final int DEFAULT_SENTENCES = 5;
    private static final int DEFAULT_WORDS = 10;
    private static final boolean DEFAULT_USE_LOREM = true;
    private static final boolean DEFAULT_AS_HTML = false;

    // Lorem ipsum basic words to choose from
    private static final String[] LOREM_WORDS = {
            "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
            "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore",
            "magna", "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud", "exercitation",
            "ullamco", "laboris", "nisi", "aliquip", "ex", "ea", "commodo", "consequat",
            "duis", "aute", "irure", "in", "reprehenderit", "voluptate", "velit", "esse",
            "cillum", "eu", "fugiat", "nulla", "pariatur", "excepteur", "sint", "occaecat",
            "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia", "deserunt",
            "mollit", "anim", "id", "est", "laborum", "perspiciatis", "unde", "omnis", "iste",
            "natus", "error", "accusantium", "doloremque", "laudantium", "totam", "rem", "aperiam",
            "eaque", "ipsa", "quae", "ab", "illo", "inventore", "veritatis", "quasi",
            "architecto", "beatae", "vitae", "dicta", "explicabo", "nemo", "ipsam", "voluptatem",
            "quia", "voluptas", "aspernatur", "aut", "odit", "fugit", "consequuntur", "magni",
            "dolores", "ratione", "sequi", "nesciunt", "neque", "porro", "quisquam", "dolorem",
            "maiores", "alias", "perferendis", "doloribus", "asperiores", "repellat", "maxime"
    };

    // Standard Lorem Ipsum beginning
    private static final String LOREM_START = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";

    private final Random random = new Random();

    @Override
    public String getName() {
        return "LoremIpsumGenerator";
    }

    @Override
    public void execute() {
        System.out.println("LoremIpsumGenerator Plugin executed (standalone test)");
        try {
            // Test with default settings
            Map<String, Object> params = new HashMap<>();
            params.put("paragraphs", DEFAULT_PARAGRAPHS);
            params.put("sentences", DEFAULT_SENTENCES);
            params.put("words", DEFAULT_WORDS);
            params.put("useLorem", DEFAULT_USE_LOREM);
            params.put("asHtml", DEFAULT_AS_HTML);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Default settings): " + result1);

            // Test with multiple paragraphs
            params.put("paragraphs", 3);
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Multiple paragraphs): " + result2);

            // Test with HTML formatting
            params.put("asHtml", true);
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (HTML formatting): " + result3);

            // Test without Lorem start
            params.put("useLorem", false);
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Without Lorem start): " + result4);

            // Test with short sentences
            params.put("sentences", 2);
            params.put("words", 3);
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Short sentences): " + result5);

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
        metadata.put("name", "Lorem ipsum generator");
        metadata.put("icon", "TextFields");
        metadata.put("description", "Lorem ipsum is a placeholder text commonly used to demonstrate the visual form of a document or a typeface without relying on meaningful content");
        metadata.put("id", "LoremIpsumGenerator");
        metadata.put("category", "Text");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Lorem Ipsum Generator ---
        Map<String, Object> loremSection = new HashMap<>();
        loremSection.put("id", "loremIpsum");
        loremSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();

        // Paragraphs slider
        inputs.add(Map.ofEntries(
                Map.entry("label", "Paragraphs"),
                Map.entry("id", "paragraphs"),
                Map.entry("default", 1),
                Map.entry("type", "slider"),
                Map.entry("width", 350),
                Map.entry("height", 20),
                Map.entry("containerId", "main")
        ));

        // Sentences per paragraph slider
        inputs.add(Map.ofEntries(
                Map.entry("label", "Sentences per paragraph"),
                Map.entry("id", "sentences"),
                Map.entry("default", 5),
                Map.entry("type", "slider"),
                Map.entry("width", 350),
                Map.entry("height", 20),
                Map.entry("containerId", "main")
        ));

        // Words per sentence slider
        inputs.add(Map.ofEntries(
                Map.entry("label", "Words per sentence"),
                Map.entry("id", "words"),
                Map.entry("default", 10),
                Map.entry("type", "slider"),
                Map.entry("width", 350),
                Map.entry("height", 20),
                Map.entry("containerId", "main")
        ));

        // Use Lorem switch
        inputs.add(Map.ofEntries(
                Map.entry("default", true),
                Map.entry("id", "useLorem"),
                Map.entry("label", "Start with lorem ipsum ?"),
                Map.entry("type", "switch"),
                Map.entry("containerId", "main")
        ));

        // As HTML switch
        inputs.add(Map.ofEntries(
                Map.entry("default", false),
                Map.entry("id", "asHtml"),
                Map.entry("label", "As html ?"),
                Map.entry("type", "switch"),
                Map.entry("containerId", "main")
        ));

        loremSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("refresh", "outside", "copy", "outside")),
                Map.entry("buttons", List.of("copy", "refresh")),
                Map.entry("width", 600),
                Map.entry("id", "token"),
                Map.entry("label", "Generated Token"),
                Map.entry("type", "text"),
                Map.entry("containerId", "main"),
                Map.entry("height", 80),
                Map.entry("multiline", true),
                Map.entry("monospace", true)
        ));
        loremSection.put("outputs", outputs);

        sections.add(loremSection);

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
            int paragraphs = getIntParam(input, "paragraphs", DEFAULT_PARAGRAPHS);
            int sentences = getIntParam(input, "sentences", DEFAULT_SENTENCES);
            int words = getIntParam(input, "words", DEFAULT_WORDS);
            boolean useLorem = getBooleanParam(input, "useLorem", DEFAULT_USE_LOREM);
            boolean asHtml = getBooleanParam(input, "asHtml", DEFAULT_AS_HTML);

            // Validate inputs
            if (paragraphs <= 0 || paragraphs > 100) {
                paragraphs = DEFAULT_PARAGRAPHS;
            }

            if (sentences <= 0 || sentences > 50) {
                sentences = DEFAULT_SENTENCES;
            }

            if (words <= 0 || words > 50) {
                words = DEFAULT_WORDS;
            }

            // Generate Lorem Ipsum text
            String loremIpsum = generateLoremIpsum(paragraphs, sentences, words, useLorem, asHtml);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", loremIpsum);

            return result;

        } catch (Exception e) {
            System.err.println("Error generating Lorem Ipsum: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Generate Lorem Ipsum text with the specified parameters
    private String generateLoremIpsum(int paragraphs, int sentences, int words, boolean useLorem, boolean asHtml) {
        StringBuilder result = new StringBuilder();

        for (int p = 0; p < paragraphs; p++) {
            StringBuilder paragraph = new StringBuilder();

            for (int s = 0; s < sentences; s++) {
                // If it's the first sentence of the first paragraph and useLorem is true, use the standard Lorem start
                if (p == 0 && s == 0 && useLorem) {
                    paragraph.append(LOREM_START);
                } else {
                    StringBuilder sentence = new StringBuilder();

                    // Generate a random sentence
                    for (int w = 0; w < words; w++) {
                        if (w > 0) {
                            sentence.append(" ");
                        }
                        String word = LOREM_WORDS[random.nextInt(LOREM_WORDS.length)];

                        // Capitalize if it's the first word of the sentence
                        if (w == 0) {
                            word = capitalize(word);
                        }

                        sentence.append(word);
                    }

                    // Add period at the end
                    sentence.append(".");
                    paragraph.append(sentence);
                }

                // Add space between sentences
                if (s < sentences - 1) {
                    paragraph.append(" ");
                }
            }

            // Format paragraph based on HTML setting
            if (asHtml) {
                result.append("<p>").append(paragraph).append("</p>");
            } else {
                result.append(paragraph);
                // Add line breaks between paragraphs
                if (p < paragraphs - 1) {
                    result.append("\n\n");
                }
            }
        }

        return result.toString();
    }

    // Capitalize the first letter of a word
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
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

    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String strValue = value.toString().toLowerCase();
        if ("true".equals(strValue) || "yes".equals(strValue) || "1".equals(strValue)) {
            return true;
        } else if ("false".equals(strValue) || "no".equals(strValue) || "0".equals(strValue)) {
            return false;
        }

        return defaultValue;
    }
}