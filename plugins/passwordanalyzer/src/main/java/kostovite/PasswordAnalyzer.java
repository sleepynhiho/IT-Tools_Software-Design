package kostovite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Assuming PluginInterface is standard
public class PasswordAnalyzer implements PluginInterface {

    // Character set patterns
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]"); // Basic special chars

    // Character set sizes for entropy calculation
    private static final int LOWERCASE_SIZE = 26;
    private static final int UPPERCASE_SIZE = 26;
    private static final int DIGITS_SIZE = 10;
    private static final int SPECIAL_SIZE = 33; // OWASP common ~33

    // Cracking speeds (guesses per second) - Rough estimates
    // Using LinkedHashMap to preserve order for dropdown options
    private static final Map<String, Long> CRACKING_SPEEDS = new LinkedHashMap<>();
    static {
        CRACKING_SPEEDS.put("online_throttled", 10L);
        CRACKING_SPEEDS.put("online_unthrottled", 1000L);
        CRACKING_SPEEDS.put("offline_slow_hash", 10_000L);
        CRACKING_SPEEDS.put("offline_fast_hash", 100_000_000_000L); // Default
        CRACKING_SPEEDS.put("offline_gpu_cluster", 100_000_000_000_000L);
    }
    private static final String DEFAULT_SCENARIO = "offline_fast_hash";

    // Time units in seconds
    private static final long SECOND = 1;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long MONTH = 30 * DAY; // Approximation
    private static final long YEAR = 365 * DAY; // Approximation
    private static final long CENTURY = 100 * YEAR;

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "PasswordAnalyzer";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("PasswordAnalyzer Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("passwordInput", "P@ssw0rd!"); // Use new ID
            params.put("attackScenario", DEFAULT_SCENARIO); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Test Analysis Result: " + result);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "PasswordAnalyzer");
        metadata.put("name", "Password Strength Analyzer");
        metadata.put("description", "Analyze password length, complexity, entropy, and estimated cracking time.");
        metadata.put("icon", "Password");
        metadata.put("category", "Security");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", true); // Analyze dynamically

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "inputConfig");
        inputSection.put("label", "Password Input & Scenario");

        List<Map<String, Object>> inputs = new ArrayList<>();

        inputs.add(Map.ofEntries(
                Map.entry("id", "passwordInput"),
                Map.entry("label", "Password to Analyze:"),
                Map.entry("type", "password"), // Use password type for masking
                Map.entry("required", false), // Not technically required, analysis runs on empty string
                Map.entry("placeholder", "Type password here...")
                // Helper text removed, handled by description/section labels
        ));

        // Attack Scenario Select
        List<Map<String, String>> scenarioOptions = new ArrayList<>();
        Pattern wordStartPattern = Pattern.compile("\\b\\w");
        CRACKING_SPEEDS.forEach((key, value) -> {
            String tempLabel = key.replace("_", " ").replace("-", " ");
            Matcher matcher = wordStartPattern.matcher(tempLabel);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group().toUpperCase());
            }
            matcher.appendTail(sb);
            String finalLabel = sb + String.format(Locale.US, " (~%,d g/s)", value);
            scenarioOptions.add(Map.of("value", key, "label", finalLabel));
        });
        inputs.add(Map.ofEntries(
                Map.entry("id", "attackScenario"),
                Map.entry("label", "Assumed Attack Scenario:"),
                Map.entry("type", "select"),
                Map.entry("options", scenarioOptions),
                Map.entry("default", DEFAULT_SCENARIO),
                Map.entry("required", false) // Has default
                // Helper text removed
        ));

        inputSection.put("inputs", inputs);
        sections.add(inputSection);


        // --- Section 2: Strength Overview ---
        Map<String, Object> overviewSection = new HashMap<>();
        overviewSection.put("id", "overview");
        overviewSection.put("label", "Strength Assessment");
        // Condition: Show only when analysis has run successfully and score is available
        overviewSection.put("condition", "success === true && typeof score !== 'undefined'");

        List<Map<String, Object>> overviewOutputs = new ArrayList<>();

        // Score (Represented as text, frontend can use hints for progress bar)
        overviewOutputs.add(Map.ofEntries(
                Map.entry("id", "score"), // ID matches result key
                Map.entry("label", "Overall Score"),
                Map.entry("type", "text"), // Keep as text
                // Hints for frontend ProgressBar component
                Map.entry("min", 0),
                Map.entry("max", 100),
                Map.entry("suffix", " / 100") // Hint for display
                // Frontend would apply color based on the score value
        ));

        // Strength Label
        overviewOutputs.add(createOutputField("strengthLabel", "Assessment", "text", null));

        overviewSection.put("outputs", overviewOutputs);
        sections.add(overviewSection);

        // --- Section 3: Detailed Analysis ---
        Map<String, Object> detailSection = new HashMap<>();
        detailSection.put("id", "details");
        detailSection.put("label", "Detailed Analysis");
        detailSection.put("condition", "success === true && typeof length !== 'undefined' && length > 0"); // Show on success and non-empty password

        List<Map<String, Object>> detailOutputs = new ArrayList<>();

        detailOutputs.add(createOutputField("length", "Length", "text", null));
        detailOutputs.add(createOutputField("entropy", "Entropy (bits)", "text", null));
        detailOutputs.add(createOutputField("crackTimeFormatted", "Est. Time to Crack", "text", null));
        detailOutputs.add(createOutputField("charSetSize", "Character Set Size", "text", null));

        detailSection.put("outputs", detailOutputs);
        sections.add(detailSection);

        // --- Section 4: Character Sets Used ---
        Map<String, Object> charsetSection = new HashMap<>();
        charsetSection.put("id", "charsets");
        charsetSection.put("label", "Character Sets Used");
        charsetSection.put("condition", "success === true && typeof charSetDetails !== 'undefined'"); // Show on success

        List<Map<String, Object>> charsetOutputs = new ArrayList<>();

        // Use type 'boolean'. Frontend needs to render check/cross icon based on value.
        charsetOutputs.add(createOutputField("charSetDetails.lowercase", "Lowercase (a-z)", "boolean", null));
        charsetOutputs.add(createOutputField("charSetDetails.uppercase", "Uppercase (A-Z)", "boolean", null));
        charsetOutputs.add(createOutputField("charSetDetails.digits", "Digits (0-9)", "boolean", null));
        charsetOutputs.add(createOutputField("charSetDetails.special", "Special Chars", "boolean", null));

        charsetSection.put("outputs", charsetOutputs);
        sections.add(charsetSection);

        // --- Section 5: Recommendations ---
        Map<String, Object> recommendationsSection = new HashMap<>();
        recommendationsSection.put("id", "recommendations");
        recommendationsSection.put("label", "Recommendations");
        // Show only if success and recommendations array exists and is not empty
        recommendationsSection.put("condition", "success === true && typeof recommendations !== 'undefined' && recommendations.length > 0");

        List<Map<String, Object>> recommendationOutputs = new ArrayList<>();

        // Use type 'list'. Frontend needs to render the array as a list (e.g., bullet points).
        Map<String, Object> recList = createOutputField("recommendations", "", "list", null); // Label handled by section header
        recList.put("style", "warning"); // Style hint for recommendations
        recommendationOutputs.add(recList);

        recommendationsSection.put("outputs", recommendationOutputs);
        sections.add(recommendationsSection);


        // --- Section 6: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Show only on failure

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create output field definitions
    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id); // Use ID
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", type); // Use specified type
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error"); // Style hint for error field
        }
        // Add monospace hint for relevant text fields
        if ("text".equals(type) && (id.toLowerCase().contains("entropy") || id.toLowerCase().contains("time") || id.toLowerCase().contains("size") || id.toLowerCase().contains("length") || id.toLowerCase().contains("score") )) {
            field.put("monospace", true);
        }
        // Note: Frontend needs to implement rendering for 'boolean' and 'list' types
        return field;
    }

    /**
     * Processes the input password (using IDs from the new format)
     * to analyze its strength.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        try {
            // Get parameters using NEW IDs
            String password = getStringParam(input, "passwordInput", ""); // Allow empty for initial state
            String scenario = getStringParam(input, "attackScenario", DEFAULT_SCENARIO); // Use default

            // Analyze (handle empty password case inside)
            return analyzePasswordStrength(password, scenario);

        } catch (IllegalArgumentException e) { // Catch validation errors from helpers
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing password analysis request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error during analysis.");
        }
    }

    // ========================================================================
    // Private Analysis Methods
    // ========================================================================

    private Map<String, Object> analyzePasswordStrength(String password, String scenario) {
        Map<String, Object> result = new LinkedHashMap<>(); // Preserve result order
        String errorOutputId = "errorMessage"; // Ensure consistency

        // Handle empty password input gracefully
        if (password == null || password.isEmpty()) {
            result.put("success", true); // Process ran, returning empty state
            result.put("isEmpty", true); // Flag for frontend
            result.put("strengthLabel", "Enter a password to analyze"); // Matches output ID
            result.put("score", 0); // Matches output ID
            result.put("length", 0); // Matches output ID
            result.put("entropy", 0.0); // Matches output ID
            result.put("crackTimeFormatted", "N/A"); // Matches output ID
            result.put("charSetSize", 0); // Matches output ID
            // Matches base ID for boolean outputs
            result.put("charSetDetails", Map.of("lowercase", false, "uppercase", false, "digits", false, "special", false));
            // Matches output ID, provide initial prompt
            result.put("recommendations", Collections.emptyList()); // Empty list, no recommendations needed yet
            return result;
        }

        try {
            // --- Calculations ---
            int length = password.length();
            boolean hasLowercase = LOWERCASE.matcher(password).find();
            boolean hasUppercase = UPPERCASE.matcher(password).find();
            boolean hasDigits = DIGITS.matcher(password).find();
            boolean hasSpecial = SPECIAL.matcher(password).find();

            int charSetSize = calculateCharacterSetSize(hasLowercase, hasUppercase, hasDigits, hasSpecial);
            double entropy = calculateEntropy(length, charSetSize);
            long crackingSpeed = getCrackingSpeed(scenario);
            Map<String, Object> crackingTime = calculateCrackingTime(length, charSetSize, crackingSpeed);
            int score = calculateScore(length, entropy); // Use simplified entropy score

            // --- Build Result Map (matching NEW output IDs) ---
            result.put("success", true);
            result.put("isEmpty", false);
            result.put("length", length);
            result.put("entropy", roundToTwoDecimalPlaces(entropy));
            result.put("charSetSize", charSetSize);
            result.put("crackTimeFormatted", crackingTime.get("formatted"));
            result.put("score", score);
            result.put("strengthLabel", getStrengthLabel(score));

            // Character Set Details (structured map for boolean outputs)
            Map<String, Boolean> charSetsUsed = Map.of(
                    "lowercase", hasLowercase,
                    "uppercase", hasUppercase,
                    "digits", hasDigits,
                    "special", hasSpecial
            );
            result.put("charSetDetails", charSetsUsed);

            // Recommendations (List<String>)
            List<String> recommendations = generateRecommendations(length, hasLowercase, hasUppercase, hasDigits, hasSpecial);
            result.put("recommendations", recommendations); // Always include list, might be empty

        } catch (Exception e) {
            System.err.println("Error during password analysis calculation: " + e.getMessage());
            e.printStackTrace();
            result.clear();
            result.put("success", false);
            result.put(errorOutputId, "Failed to analyze password due to an internal error.");
        }
        return result;
    }

    private int calculateCharacterSetSize(boolean hasLowercase, boolean hasUppercase, boolean hasDigits, boolean hasSpecial) {
        int size = 0;
        if (hasLowercase) size += LOWERCASE_SIZE;
        if (hasUppercase) size += UPPERCASE_SIZE;
        if (hasDigits) size += DIGITS_SIZE;
        if (hasSpecial) size += SPECIAL_SIZE;
        return Math.max(size, 1);
    }

    private double calculateEntropy(int length, int charSetSize) {
        if (charSetSize <= 1 || length <= 0) return 0.0;
        return length * (Math.log(charSetSize) / Math.log(2));
    }

    private long getCrackingSpeed(String scenario) {
        return CRACKING_SPEEDS.getOrDefault(scenario.toLowerCase(), CRACKING_SPEEDS.get(DEFAULT_SCENARIO));
    }

    private Map<String, Object> calculateCrackingTime(int length, int charSetSize, long crackingSpeed) {
        Map<String, Object> result = new HashMap<>();
        if (crackingSpeed <= 0) {
            result.put("formatted", "N/A (Invalid speed)");
            result.put("seconds", Double.POSITIVE_INFINITY); return result;
        }
        if (charSetSize <= 0 || length <= 0) {
            result.put("formatted", "N/A"); result.put("seconds", 0.0); return result;
        }
        try {
            BigDecimal combinations = BigDecimal.valueOf(charSetSize).pow(length);
            BigDecimal avgSecondsToCrack = combinations.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(crackingSpeed), 2, RoundingMode.HALF_UP);
            result.put("formatted", formatTimeInterval(avgSecondsToCrack));
            result.put("seconds", avgSecondsToCrack.doubleValue());
        } catch (ArithmeticException e) {
            result.put("formatted", "Effectively Infinite"); result.put("seconds", Double.POSITIVE_INFINITY);
        }
        return result;
    }

    private String formatTimeInterval(BigDecimal seconds) {
        // Reusing the robust formatting from previous example
        if (seconds.compareTo(BigDecimal.valueOf(CENTURY).multiply(BigDecimal.valueOf(1000))) > 0) return "centuries (effectively infinite)";
        if (seconds.compareTo(BigDecimal.valueOf(CENTURY)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(CENTURY), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "century" : "centuries");
        }
        if (seconds.compareTo(BigDecimal.valueOf(YEAR)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(YEAR), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "year" : "years");
        }
        if (seconds.compareTo(BigDecimal.valueOf(MONTH)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(MONTH), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "month" : "months");
        }
        if (seconds.compareTo(BigDecimal.valueOf(DAY)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(DAY), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "day" : "days");
        }
        if (seconds.compareTo(BigDecimal.valueOf(HOUR)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(HOUR), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "hour" : "hours");
        }
        if (seconds.compareTo(BigDecimal.valueOf(MINUTE)) >= 0) {
            BigDecimal val = seconds.divide(BigDecimal.valueOf(MINUTE), 1, RoundingMode.HALF_UP);
            return String.format(Locale.US, "%.1f %s", val, val.compareTo(BigDecimal.ONE) == 0 ? "minute" : "minutes");
        }
        if (seconds.compareTo(BigDecimal.ONE) < 0) {
            if (seconds.compareTo(BigDecimal.valueOf(0.001)) < 0) return "instantaneous";
            return String.format(Locale.US, "%.3f seconds", seconds);
        }
        return String.format(Locale.US, "%.1f %s", seconds, seconds.compareTo(BigDecimal.ONE) == 0 ? "second" : "seconds");
    }


    // Simplified score based primarily on entropy
    private int calculateScore(int length, double entropy) {
        if (length == 0) return 0;
        // Simple scale based on entropy benchmarks
        double weak = 35.0, moderate = 60.0, strong = 85.0, veryStrong = 110.0;
        int score;
        if (entropy < weak) score = (int) Math.max(0, (entropy / weak) * 29.0); // 0-29
        else if (entropy < moderate) score = 30 + (int) Math.max(0, ((entropy - weak) / (moderate - weak)) * 29.0); // 30-59
        else if (entropy < strong) score = 60 + (int) Math.max(0, ((entropy - moderate) / (strong - moderate)) * 29.0); // 60-89
        else score = 90 + (int) Math.min(10.0, ((entropy - strong) / (veryStrong - strong)) * 10.0); // 90-100
        return Math.max(0, Math.min(100, score));
    }

    private String getStrengthLabel(int score) {
        if (score >= 90) return "Very Strong";
        if (score >= 70) return "Strong";
        if (score >= 50) return "Moderate";
        if (score >= 30) return "Weak";
        return "Very Weak";
    }

    private List<String> generateRecommendations(int length, boolean hasLowercase, boolean hasUppercase, boolean hasDigits, boolean hasSpecial) {
        List<String> tips = new ArrayList<>();
        if (length < 12) tips.add("Increase length (12+ recommended).");
        if (!hasLowercase) tips.add("Include lowercase letters (a-z).");
        if (!hasUppercase) tips.add("Include uppercase letters (A-Z).");
        if (!hasDigits) tips.add("Include numbers (0-9).");
        if (!hasSpecial) tips.add("Include symbols (e.g., !@#$%).");
        if (tips.size() < 2 && length >= 8 && length < 16) tips.add("Add more character variety or increase length further.");
        return tips;
    }

    // --- Parameter Parsing Helpers ---

    private double roundToTwoDecimalPlaces(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return value; // Avoid errors on NaN/Infinity
        return Math.round(value * 100.0) / 100.0;
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString();
        // Allow empty password, but throw if other required fields are empty
        if (strValue.isEmpty() && defaultValue == null && !key.equals("passwordInput")) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return strValue;
    }

    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return "true".equalsIgnoreCase(value.toString());
        }
        return defaultValue;
    }
}