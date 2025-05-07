package kostovite;

import java.util.*;

public class RomanNumeralConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final int MIN_ROMAN_VALUE = 1;
    private static final int MAX_ROMAN_VALUE = 3999;
    private static final String CURRENT_DATE_TIME = "2025-05-06 22:05:10";
    private static final String CURRENT_USER = "hanhihoadd";

    // Roman numeral mapping arrays
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    @Override
    public String getName() {
        return "RomanNumeralConverter";
    }

    @Override
    public void execute() {
        System.out.println(String.format("[%s] [%s] RomanNumeralConverter Plugin executed (standalone test)", 
            CURRENT_DATE_TIME, CURRENT_USER));
        try {
            Map<String, Object> params = new HashMap<>();

            // Test Arabic to Roman conversions
            System.out.println(String.format("[%s] [%s] Testing Arabic to Roman conversions:", 
                CURRENT_DATE_TIME, CURRENT_USER));
            params.put("conversionType", "arabicToRoman");
            
            params.put("arabicInputNumber", "42");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (42): " + result1);

            params.put("arabicInputNumber", "1994");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (1994): " + result2);

            params.put("arabicInputNumber", "3999");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (3999): " + result3);

            // Test with invalid inputs for Arabic
            params.put("arabicInputNumber", "0"); // Below min
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (0 - below min): " + result4);

            params.put("arabicInputNumber", "4000"); // Above max
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (4000 - above max): " + result5);

            params.put("arabicInputNumber", "abc"); // Not a number
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (abc - not a number): " + result6);

            // Test Roman to Arabic conversions
            System.out.println(String.format("[%s] [%s] Testing Roman to Arabic conversions:", 
                CURRENT_DATE_TIME, CURRENT_USER));
            params.put("conversionType", "romanToArabic");
            
            params.put("romanInputNumber", "XLII");
            Map<String, Object> result7 = process(params);
            System.out.println("Test 7 (XLII): " + result7);

            params.put("romanInputNumber", "MCMXCIV");
            Map<String, Object> result8 = process(params);
            System.out.println("Test 8 (MCMXCIV): " + result8);

            params.put("romanInputNumber", "MMMCMXCIX");
            Map<String, Object> result9 = process(params);
            System.out.println("Test 9 (MMMCMXCIX): " + result9);

            // Test with invalid Roman numerals
            params.put("romanInputNumber", "XYZ"); // Invalid symbols
            Map<String, Object> result10 = process(params);
            System.out.println("Test 10 (XYZ - invalid symbols): " + result10);

            params.put("romanInputNumber", "IIII"); // Invalid repetition
            Map<String, Object> result11 = process(params);
            System.out.println("Test 11 (IIII - invalid repetition): " + result11);
            
            params.put("romanInputNumber", ""); // Empty input
            Map<String, Object> result12 = process(params);
            System.out.println("Test 12 (empty input): " + result12);

        } catch (Exception e) {
            System.err.println(String.format("[%s] [%s] Standalone test failed: %s", 
                CURRENT_DATE_TIME, CURRENT_USER, e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "RomanNumeralConverter");
        metadata.put("name", "Roman numeral converter");
        metadata.put("description", "Convert between Roman numerals and Arabic numbers in both directions.");
        metadata.put("icon", "Numbers");
        metadata.put("category", "Converter");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Conversion Type ---
        Map<String, Object> conversionTypeSection = new HashMap<>();
        conversionTypeSection.put("id", "conversionType");
        conversionTypeSection.put("label", "Conversion Type");

        // --- Conversion Type Input ---
        List<Map<String, Object>> conversionTypeInputs = new ArrayList<>();
        conversionTypeInputs.add(Map.ofEntries(
                Map.entry("id", "conversionType"),
                Map.entry("label", "Choose conversion direction"),
                Map.entry("type", "select"),
                Map.entry("required", true),
                Map.entry("default", "arabicToRoman"),
                Map.entry("options", List.of(
                        Map.of("value", "arabicToRoman", "label", "Arabic to Roman"),
                        Map.of("value", "romanToArabic", "label", "Roman to Arabic")
                )),
                Map.entry("containerId", "main")
        ));
        conversionTypeSection.put("inputs", conversionTypeInputs);
        sections.add(conversionTypeSection);

        // --- Section: Arabic to Roman ---
        Map<String, Object> arabicToRomanSection = new HashMap<>();
        arabicToRomanSection.put("id", "arabicToRoman");
        arabicToRomanSection.put("label", "Arabic to Roman");
        arabicToRomanSection.put("condition", "conversionType === 'arabicToRoman'");

        // --- Inputs ---
        List<Map<String, Object>> arabicInputs = new ArrayList<>();
        arabicInputs.add(Map.ofEntries(
                Map.entry("id", "arabicInputNumber"),
                Map.entry("label", "Enter a number (1-3999)"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "42"),
                Map.entry("placeholder", "e.g., 1994"),
                Map.entry("helperText", "Enter a number between 1 and 3999"),
                Map.entry("containerId", "main"),
                Map.entry("width", 200),
                Map.entry("height", 40)
        ));
        arabicToRomanSection.put("inputs", arabicInputs);

        // --- Outputs ---
        List<Map<String, Object>> arabicOutputs = new ArrayList<>();
        arabicOutputs.add(Map.ofEntries(
                Map.entry("id", "romanOutput"),
                Map.entry("label", "Roman Numeral"),
                Map.entry("type", "text"),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36)
        ));
        arabicToRomanSection.put("outputs", arabicOutputs);

        sections.add(arabicToRomanSection);

        // --- Section: Roman to Arabic ---
        Map<String, Object> romanToArabicSection = new HashMap<>();
        romanToArabicSection.put("id", "romanToArabic");
        romanToArabicSection.put("label", "Roman to Arabic");
        romanToArabicSection.put("condition", "conversionType === 'romanToArabic'");

        // --- Inputs ---
        List<Map<String, Object>> romanInputs = new ArrayList<>();
        romanInputs.add(Map.ofEntries(
                Map.entry("id", "romanInputNumber"),
                Map.entry("label", "Enter a Roman numeral"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "XLII"),
                Map.entry("placeholder", "e.g., MCMXCIV"),
                Map.entry("helperText", "Enter a valid Roman numeral (I, V, X, L, C, D, M)"),
                Map.entry("containerId", "main"),
                Map.entry("width", 200),
                Map.entry("height", 40)
        ));
        romanToArabicSection.put("inputs", romanInputs);

        // --- Outputs ---
        List<Map<String, Object>> romanOutputs = new ArrayList<>();
        romanOutputs.add(Map.ofEntries(
                Map.entry("id", "arabicOutput"),
                Map.entry("label", "Arabic Number"),
                Map.entry("type", "text"),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("containerId", "main"),
                Map.entry("width", 440),
                Map.entry("height", 36)
        ));
        romanToArabicSection.put("outputs", romanOutputs);

        sections.add(romanToArabicSection);

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
            // Get conversion type
            String conversionType = getStringParam(input, "conversionType", "arabicToRoman");
            
            if ("arabicToRoman".equals(conversionType)) {
                return processArabicToRoman(input);
            } else if ("romanToArabic".equals(conversionType)) {
                return processRomanToArabic(input);
            } else {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid conversion type: " + conversionType);
            }
        } catch (Exception e) {
            System.err.println(String.format("[%s] [%s] Error processing numeral conversion: %s", 
                CURRENT_DATE_TIME, CURRENT_USER, e.getMessage()));
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Process Arabic to Roman conversion
    private Map<String, Object> processArabicToRoman(Map<String, Object> input) {
        // Get input parameter
        String arabicInputStr = getStringParam(input, "arabicInputNumber", null);
        System.out.println(String.format("[%s] [%s] Processing Arabic to Roman conversion: %s", 
            CURRENT_DATE_TIME, CURRENT_USER, arabicInputStr));

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
        System.out.println(String.format("[%s] [%s] Conversion result: %d -> %s", 
            CURRENT_DATE_TIME, CURRENT_USER, arabicNumber, romanNumeral));

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("romanOutput", romanNumeral);

        return result;
    }

    // Process Roman to Arabic conversion
    private Map<String, Object> processRomanToArabic(Map<String, Object> input) {
        // Get input parameter
        String romanInputStr = getStringParam(input, "romanInputNumber", null);
        System.out.println(String.format("[%s] [%s] Processing Roman to Arabic conversion: %s", 
            CURRENT_DATE_TIME, CURRENT_USER, romanInputStr));

        // --- Validation ---
        if (romanInputStr == null || romanInputStr.trim().isEmpty()) {
            return Map.of("success", false, ERROR_OUTPUT_ID, "Input Roman numeral is required.");
        }

        romanInputStr = romanInputStr.trim().toUpperCase();
        
        // Validate Roman numeral format
        if (!isValidRomanNumeral(romanInputStr)) {
            return Map.of("success", false, ERROR_OUTPUT_ID, 
                "Invalid Roman numeral. Only I, V, X, L, C, D, M symbols are allowed and must follow proper Roman numeral rules.");
        }

        // Convert to Arabic number
        int arabicNumber = convertFromRoman(romanInputStr);
        System.out.println(String.format("[%s] [%s] Conversion result: %s -> %d", 
            CURRENT_DATE_TIME, CURRENT_USER, romanInputStr, arabicNumber));

        // Validate the conversion (should be within range)
        if (arabicNumber < MIN_ROMAN_VALUE || arabicNumber > MAX_ROMAN_VALUE) {
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "The converted value is outside the valid range for Roman numerals (1-3999).");
        }

        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("arabicOutput", Integer.toString(arabicNumber));

        return result;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) {
                System.err.println(String.format("[%s] [%s] Missing required parameter: %s", 
                    CURRENT_DATE_TIME, CURRENT_USER, key));
                return "";
            }
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
     * Validate if the string is a properly formatted Roman numeral
     */
    private boolean isValidRomanNumeral(String roman) {
        // Check that it only contains valid Roman numeral symbols
        if (!roman.matches("^[IVXLCDM]+$")) {
            return false;
        }

        // Check for invalid repetitions (no more than 3 consecutive identical symbols)
        if (roman.contains("IIII") || roman.contains("XXXX") || roman.contains("CCCC") || roman.contains("MMMM")) {
            return false;
        }

        // Check for invalid subtractive combinations
        String[] invalidPairs = {"IL", "IC", "ID", "IM", "VX", "VL", "VC", "VD", "VM", "XD", "XM", "LC", "LD", "LM", "DM"};
        for (String pair : invalidPairs) {
            if (roman.contains(pair)) {
                return false;
            }
        }

        // Check for proper ordering and valid structure by attempting conversion
        try {
            int value = convertFromRoman(roman);
            String reconverted = convertToRoman(value);
            
            // If converting back and forth gives a different result, it wasn't a valid Roman numeral
            return roman.equals(reconverted);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert a Roman numeral to integer
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