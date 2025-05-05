package kostovite;

import java.util.*;

public class RomanNumeralConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int MIN_ROMAN_VALUE = 1;
    private static final int MAX_ROMAN_VALUE = 3999;

    // Roman numeral mapping arrays
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    @Override
    public String getName() {
        return "RomanNumeralConverter";
    }

    @Override
    public void execute() {
        System.out.println("RomanNumeralConverter Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test with various valid inputs
            params.put("arabicInputNumber", "42");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (42): " + result1);

            params.put("arabicInputNumber", "1994");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (1994): " + result2);

            params.put("arabicInputNumber", "3999");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (3999): " + result3);

            // Test with invalid inputs
            params.put("arabicInputNumber", "0"); // Below min
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (0 - below min): " + result4);

            params.put("arabicInputNumber", "4000"); // Above max
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (4000 - above max): " + result5);

            params.put("arabicInputNumber", "abc"); // Not a number
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (abc - not a number): " + result6);

            params.put("arabicInputNumber", "-5"); // Negative number
            Map<String, Object> result7 = process(params);
            System.out.println("Test 7 (-5 - negative number): " + result7);

            params.put("arabicInputNumber", "1.5"); // Decimal number
            Map<String, Object> result8 = process(params);
            System.out.println("Test 8 (1.5 - decimal number): " + result8);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "RomanNumeralConverter");
        metadata.put("name", "Roman numeral converter");
        metadata.put("description", "Convert Roman numerals to numbers and convert numbers to Roman numerals.");
        metadata.put("icon", "Numbers");
        metadata.put("category", "Converter");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Arabic to Roman ---
        Map<String, Object> arabicToRomanSection = new HashMap<>();
        arabicToRomanSection.put("id", "arabicToRoman");
        arabicToRomanSection.put("label", "Arabic to Roman");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "arabicInputNumber"),
                Map.entry("label", ""),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "42"),
                Map.entry("containerId", "main"),
                Map.entry("width", 200),
                Map.entry("height", 40)
        ));
        arabicToRomanSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("id", "romanOutput"),
                Map.entry("label", ""),
                Map.entry("type", "text"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36)
        ));
        arabicToRomanSection.put("outputs", outputs);

        sections.add(arabicToRomanSection);

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
            // Get input parameter
            String arabicInputStr = getStringParam(input, "arabicInputNumber", null);

            // --- Validation ---
            if (arabicInputStr == null || arabicInputStr.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input number is required.");
            }

            // Validate that it's an integer
            int arabicNumber;
            try {
                arabicNumber = Integer.parseInt(arabicInputStr.trim());
            } catch (NumberFormatException e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Input must be a valid integer.");
            }

            // Validate that it's within the acceptable range for Roman numerals
            if (arabicNumber < MIN_ROMAN_VALUE) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Input must be at least " + MIN_ROMAN_VALUE + " (minimum value for Roman numerals).");
            }
            if (arabicNumber > MAX_ROMAN_VALUE) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Input must not exceed " + MAX_ROMAN_VALUE + " (maximum value for standard Roman numerals).");
            }

            // Convert to Roman numeral
            String romanNumeral = convertToRoman(arabicNumber);

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("romanOutput", romanNumeral);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing Roman numeral conversion: " + e.getMessage());
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
     * Convert an integer to Roman numeral representation
     */
    private String convertToRoman(int number) {
        StringBuilder result = new StringBuilder();

        // Process the number using the greedy algorithm approach
        int remaining = number;
        for (int i = 0; i < ROMAN_SYMBOLS.length; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                result.append(ROMAN_SYMBOLS[i]);
                remaining -= ROMAN_VALUES[i];
            }
        }

        return result.toString();
    }

    /**
     * Convert a Roman numeral to integer (not used in this version, but included for completeness)
     */
    private int convertFromRoman(String roman) {
        int result = 0;
        int prevValue = 0;

        // Process from right to left
        for (int i = roman.length() - 1; i >= 0; i--) {
            int currentValue = getRomanSymbolValue(roman.charAt(i));

            // If current value is greater or equal to previous, add it
            // Otherwise subtract it (handles cases like IV = 4)
            if (currentValue >= prevValue) {
                result += currentValue;
            } else {
                result -= currentValue;
            }

            prevValue = currentValue;
        }

        return result;
    }

    /**
     * Get the value of a single Roman numeral symbol
     */
    private int getRomanSymbolValue(char symbol) {
        switch (symbol) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default: return 0;
        }
    }
}