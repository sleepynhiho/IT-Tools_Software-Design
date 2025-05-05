package kostovite;

import java.util.*;

public class RomanNumeralConverter implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "RomanNumeralConverter";
    }

    @Override
    public void execute() {
        System.out.println("RomanNumeralConverter Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test Arabic to Roman conversion
            params.put("arabicInputNumber", "42");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Arabic 42): " + result1);

            // Test larger number
            params.put("arabicInputNumber", "1984");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Arabic 1984): " + result2);

            // Test Roman to Arabic conversion
            params.clear();
            params.put("romanInputNumber", "XLII");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Roman XLII): " + result3);

            // Test larger Roman numeral
            params.put("romanInputNumber", "MCMLXXXIV");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Roman MCMLXXXIV): " + result4);

            // Test invalid Arabic input
            params.clear();
            params.put("arabicInputNumber", "abc");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Invalid Arabic): " + result5);

            // Test invalid Roman input
            params.clear();
            params.put("romanInputNumber", "XYZ");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Invalid Roman): " + result6);

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
        metadata.put("name", "Roman numeral converter");
        metadata.put("icon", "Numbers");
        metadata.put("description", "Convert Roman numerals to numbers and convert numbers to Roman numerals.");
        metadata.put("id", "RomanNumeralConverter");
        metadata.put("category", "Converter");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Arabic to Roman ---
        Map<String, Object> arabicToRomanSection = new HashMap<>();
        arabicToRomanSection.put("id", "arabicToRoman");
        arabicToRomanSection.put("label", "Arabic to Roman");

        // --- Arabic to Roman Inputs ---
        List<Map<String, Object>> arabicInputs = new ArrayList<>();
        arabicInputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("id", "arabicInputNumber"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "42"),
                Map.entry("containerId", "main"),
                Map.entry("width", 200),
                Map.entry("height", 40)
        ));
        arabicToRomanSection.put("inputs", arabicInputs);

        // --- Arabic to Roman Outputs ---
        List<Map<String, Object>> arabicOutputs = new ArrayList<>();
        arabicOutputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("id", "romanOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "main")
        ));
        arabicToRomanSection.put("outputs", arabicOutputs);

        sections.add(arabicToRomanSection);

        // --- Section 2: Roman to Arabic ---
        Map<String, Object> romanToArabicSection = new HashMap<>();
        romanToArabicSection.put("id", "romanToArabic");
        romanToArabicSection.put("label", "Roman to Arabic");

        // --- Roman to Arabic Inputs ---
        List<Map<String, Object>> romanInputs = new ArrayList<>();
        romanInputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("id", "romanInputNumber"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("default", "XLII"),
                Map.entry("containerId", "main"),
                Map.entry("width", 200),
                Map.entry("height", 40)
        ));
        romanToArabicSection.put("inputs", romanInputs);

        // --- Roman to Arabic Outputs ---
        List<Map<String, Object>> romanOutputs = new ArrayList<>();
        romanOutputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "outside")),
                Map.entry("id", "arabicOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "main")
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
            // Check which input is provided (Arabic or Roman)
            if (input.containsKey("arabicInputNumber")) {
                // Convert Arabic to Roman
                return processArabicToRoman(input);
            } else if (input.containsKey("romanInputNumber")) {
                // Convert Roman to Arabic
                return processRomanToArabic(input);
            } else {
                return Map.of("success", false, ERROR_OUTPUT_ID, "No input provided.");
            }
        } catch (Exception e) {
            System.err.println("Error processing Roman numeral conversion: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    private Map<String, Object> processArabicToRoman(Map<String, Object> input) {
        String arabicStr = getStringParam(input, "arabicInputNumber", null);

        // Validation
        if (arabicStr == null || arabicStr.trim().isEmpty()) {
            return Map.of("success", false, ERROR_OUTPUT_ID, "Input number is required.");
        }

        arabicStr = arabicStr.trim();

        try {
            int arabicNumber = Integer.parseInt(arabicStr);

            // Check range (Roman numerals typically 1-3999)
            if (arabicNumber < 1 || arabicNumber > 3999) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Arabic number must be between 1 and 3999 for standard Roman numeral conversion.");
            }

            // Convert to Roman numeral
            String romanNumeral = convertToRoman(arabicNumber);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("romanOutput", romanNumeral);

            return result;

        } catch (NumberFormatException e) {
            return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid Arabic number format. Please enter a valid integer.");
        }
    }

    private Map<String, Object> processRomanToArabic(Map<String, Object> input) {
        String romanStr = getStringParam(input, "romanInputNumber", null);

        // Validation
        if (romanStr == null || romanStr.trim().isEmpty()) {
            return Map.of("success", false, ERROR_OUTPUT_ID, "Input Roman numeral is required.");
        }

        romanStr = romanStr.trim().toUpperCase();

        // Validate Roman numeral format
        if (!romanStr.matches("^[MDCLXVI]+$")) {
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "Invalid Roman numeral. Only M, D, C, L, X, V, I characters are allowed.");
        }

        try {
            // Convert to Arabic number
            int arabicNumber = convertToArabic(romanStr);

            // Validate if it's a valid Roman numeral structure
            String backToRoman = convertToRoman(arabicNumber);
            if (!romanStr.equals(backToRoman)) {
                return Map.of("success", false, ERROR_OUTPUT_ID,
                        "Invalid Roman numeral structure. Please check your input.");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("arabicOutput", String.valueOf(arabicNumber));

            return result;

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, ERROR_OUTPUT_ID, e.getMessage());
        }
    }

    // Convert Arabic number to Roman numeral
    private String convertToRoman(int number) {
        TreeMap<Integer, String> romanMap = new TreeMap<>(Collections.reverseOrder());
        romanMap.put(1000, "M");
        romanMap.put(900, "CM");
        romanMap.put(500, "D");
        romanMap.put(400, "CD");
        romanMap.put(100, "C");
        romanMap.put(90, "XC");
        romanMap.put(50, "L");
        romanMap.put(40, "XL");
        romanMap.put(10, "X");
        romanMap.put(9, "IX");
        romanMap.put(5, "V");
        romanMap.put(4, "IV");
        romanMap.put(1, "I");

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, String> entry : romanMap.entrySet()) {
            int value = entry.getKey();
            String symbol = entry.getValue();
            while (number >= value) {
                result.append(symbol);
                number -= value;
            }
        }
        return result.toString();
    }

    // Convert Roman numeral to Arabic number
    private int convertToArabic(String roman) {
        Map<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);

        int result = 0;
        int prevValue = 0;

        // Process from right to left
        for (int i = roman.length() - 1; i >= 0; i--) {
            int currentValue = romanMap.get(roman.charAt(i));

            // If current value is greater than or equal to previous value,
            // add it; otherwise, subtract it
            if (currentValue >= prevValue) {
                result += currentValue;
            } else {
                result -= currentValue;
            }

            prevValue = currentValue;
        }

        return result;
    }

    // Helper method to get string parameters
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) {
                return null;
            }
            return defaultValue;
        }
        return value.toString();
    }
}