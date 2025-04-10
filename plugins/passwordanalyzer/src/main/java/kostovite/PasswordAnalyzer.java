package kostovite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PasswordAnalyzer implements ExtendedPluginInterface {
    // Character set patterns
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    // Character set sizes
    private static final int LOWERCASE_SIZE = 26;
    private static final int UPPERCASE_SIZE = 26;
    private static final int DIGITS_SIZE = 10;
    private static final int SPECIAL_SIZE = 33; // Common special characters

    // Average cracking speeds (passwords per second) for different attack scenarios
    private static final long ONLINE_THROTTLED = 100L; // Online service with throttling
    private static final long ONLINE_UNTHROTTLED = 10_000L; // Online service without throttling
    private static final long OFFLINE_SLOW_HASH = 1_000_000L; // Offline attack, slow hash function
    private static final long OFFLINE_FAST_HASH = 1_000_000_000L; // Offline attack, fast hash function
    private static final long OFFLINE_GPU_CLUSTER = 1_000_000_000_000L; // Large GPU cluster

    // Time units in seconds
    private static final long SECOND = 1;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;
    private static final long CENTURY = 100 * YEAR;

    @Override
    public String getName() {
        return "PasswordAnalyzer";
    }

    @Override
    public void execute() {
        System.out.println("PasswordAnalyzer Plugin executed");

        // Demonstrate basic usage
        try {
            Map<String, Object> analyzeParams = new HashMap<>();
            analyzeParams.put("password", "P@ssw0rd123");

            Map<String, Object> result = process(analyzeParams);
            System.out.println("Sample analysis: " + result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Analyzes password strength and security");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Analyze operation
        Map<String, Object> analyzeOperation = new HashMap<>();
        analyzeOperation.put("description", "Analyze password strength");

        Map<String, Object> analyzeInputs = new HashMap<>();
        analyzeInputs.put("password", "Password to analyze");
        analyzeInputs.put("scenario", "Attack scenario (optional): online-throttled, online-unthrottled, offline-slow-hash, offline-fast-hash, offline-gpu-cluster");

        analyzeOperation.put("inputs", analyzeInputs);
        operations.put("analyze", analyzeOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "analyze");

            if ("analyze".equalsIgnoreCase(operation)) {
                String password = (String) input.get("password");

                if (password == null || password.isEmpty()) {
                    result.put("error", "Password cannot be empty");
                    return result;
                }

                // Get attack scenario if provided
                String scenario = (String) input.getOrDefault("scenario", "offline-fast-hash");

                // Perform analysis
                return analyzePasswordStrength(password, scenario);
            } else {
                result.put("error", "Unsupported operation: " + operation);
            }

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> analyzePasswordStrength(String password, String scenario) {
        Map<String, Object> result = new HashMap<>();

        // Basic metrics
        int length = password.length();
        boolean hasLowercase = LOWERCASE.matcher(password).find();
        boolean hasUppercase = UPPERCASE.matcher(password).find();
        boolean hasDigits = DIGITS.matcher(password).find();
        boolean hasSpecial = SPECIAL.matcher(password).find();

        // Calculate character set size
        int charSetSize = calculateCharacterSetSize(hasLowercase, hasUppercase, hasDigits, hasSpecial);

        // Calculate entropy (bits)
        double entropy = calculateEntropy(length, charSetSize);

        // Calculate time to crack based on scenario
        long crackingSpeed = getCrackingSpeed(scenario);
        Map<String, Object> crackingTime = calculateCrackingTime(length, charSetSize, crackingSpeed);

        // Calculate score (0-100)
        int score = calculateScore(length, charSetSize, hasLowercase, hasUppercase, hasDigits, hasSpecial);

        // Build the response
        result.put("password", maskPassword(password));
        result.put("length", length);
        result.put("entropy", roundToTwoDecimalPlaces(entropy));
        result.put("characterSetSize", charSetSize);
        result.put("crackingTime", crackingTime);
        result.put("score", score);

        // Add character set details
        Map<String, Boolean> charSets = new HashMap<>();
        charSets.put("lowercase", hasLowercase);
        charSets.put("uppercase", hasUppercase);
        charSets.put("digits", hasDigits);
        charSets.put("special", hasSpecial);
        result.put("characterSets", charSets);

        // Provide strength assessment
        result.put("strength", getStrengthLabel(score));

        // Add recommendations for weak passwords
        if (score < 60) {
            result.put("recommendations", generateRecommendations(password, hasLowercase, hasUppercase, hasDigits, hasSpecial));
        }

        result.put("success", true);
        return result;
    }

    private int calculateCharacterSetSize(boolean hasLowercase, boolean hasUppercase, boolean hasDigits, boolean hasSpecial) {
        int size = 0;
        if (hasLowercase) size += LOWERCASE_SIZE;
        if (hasUppercase) size += UPPERCASE_SIZE;
        if (hasDigits) size += DIGITS_SIZE;
        if (hasSpecial) size += SPECIAL_SIZE;
        return Math.max(size, 1); // Ensure we don't return 0
    }

    private double calculateEntropy(int length, int charSetSize) {
        return length * (Math.log(charSetSize) / Math.log(2));
    }

    private long getCrackingSpeed(String scenario) {
        switch (scenario.toLowerCase()) {
            case "online-throttled":
                return ONLINE_THROTTLED;
            case "online-unthrottled":
                return ONLINE_UNTHROTTLED;
            case "offline-slow-hash":
                return OFFLINE_SLOW_HASH;
            case "offline-gpu-cluster":
                return OFFLINE_GPU_CLUSTER;
            case "offline-fast-hash":
            default:
                return OFFLINE_FAST_HASH;
        }
    }

    private Map<String, Object> calculateCrackingTime(int length, int charSetSize, long crackingSpeed) {
        Map<String, Object> result = new HashMap<>();

        // Calculate the number of possible combinations
        BigDecimal combinations = BigDecimal.valueOf(charSetSize).pow(length);

        // Calculate average time to crack (in seconds)
        // Average case is trying half of all possibilities
        BigDecimal averageAttempts = combinations.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        BigDecimal secondsToCrack = averageAttempts.divide(BigDecimal.valueOf(crackingSpeed), RoundingMode.HALF_UP);

        // Convert to appropriate time unit
        Map<String, Object> formattedTime = formatTimeUnit(secondsToCrack);
        result.put("formatted", formattedTime.get("formatted"));
        result.put("value", formattedTime.get("value"));
        result.put("unit", formattedTime.get("unit"));
        result.put("seconds", secondsToCrack.doubleValue());

        return result;
    }

    private Map<String, Object> formatTimeUnit(BigDecimal seconds) {
        Map<String, Object> result = new HashMap<>();
        double value;
        String unit;
        String formatted;

        if (seconds.compareTo(BigDecimal.valueOf(CENTURY)) >= 0) {
            BigDecimal centuries = seconds.divide(BigDecimal.valueOf(CENTURY), 1, RoundingMode.HALF_UP);
            if (centuries.compareTo(BigDecimal.valueOf(1000)) > 0) {
                result.put("value", "many");
                result.put("unit", "centuries");
                result.put("formatted", "many centuries");
                return result;
            }
            value = centuries.doubleValue();
            unit = value == 1.0 ? "century" : "centuries";
        } else if (seconds.compareTo(BigDecimal.valueOf(YEAR)) >= 0) {
            value = seconds.divide(BigDecimal.valueOf(YEAR), 1, RoundingMode.HALF_UP).doubleValue();
            unit = value == 1.0 ? "year" : "years";
        } else if (seconds.compareTo(BigDecimal.valueOf(MONTH)) >= 0) {
            value = seconds.divide(BigDecimal.valueOf(MONTH), 1, RoundingMode.HALF_UP).doubleValue();
            unit = value == 1.0 ? "month" : "months";
        } else if (seconds.compareTo(BigDecimal.valueOf(DAY)) >= 0) {
            value = seconds.divide(BigDecimal.valueOf(DAY), 1, RoundingMode.HALF_UP).doubleValue();
            unit = value == 1.0 ? "day" : "days";
        } else if (seconds.compareTo(BigDecimal.valueOf(HOUR)) >= 0) {
            value = seconds.divide(BigDecimal.valueOf(HOUR), 1, RoundingMode.HALF_UP).doubleValue();
            unit = value == 1.0 ? "hour" : "hours";
        } else if (seconds.compareTo(BigDecimal.valueOf(MINUTE)) >= 0) {
            value = seconds.divide(BigDecimal.valueOf(MINUTE), 1, RoundingMode.HALF_UP).doubleValue();
            unit = value == 1.0 ? "minute" : "minutes";
        } else {
            value = seconds.doubleValue();
            unit = value == 1.0 ? "second" : "seconds";
        }

        // Round to one decimal place for readable format
        if (value == Math.floor(value)) {
            formatted = String.format("%.0f %s", value, unit);
        } else {
            formatted = String.format("%.1f %s", value, unit);
        }

        result.put("value", value);
        result.put("unit", unit);
        result.put("formatted", formatted);

        return result;
    }

    private int calculateScore(int length, int charSetSize, boolean hasLowercase, boolean hasUppercase,
                               boolean hasDigits, boolean hasSpecial) {
        // Base score calculation
        int score = 0;

        // Length score (up to 50 points)
        score += Math.min(length * 4, 50);

        // Character set diversity score (up to 25 points)
        int diversityCount = 0;
        if (hasLowercase) diversityCount++;
        if (hasUppercase) diversityCount++;
        if (hasDigits) diversityCount++;
        if (hasSpecial) diversityCount++;

        score += diversityCount * 6;

        // Bonus for mixing character types (up to 15 points)
        if (diversityCount >= 3) score += 10;
        if (diversityCount == 4) score += 5;

        // Entropy bonus (up to 10 points)
        double entropy = calculateEntropy(length, charSetSize);
        if (entropy > 100) score += 10;
        else if (entropy > 80) score += 8;
        else if (entropy > 60) score += 6;
        else if (entropy > 40) score += 4;
        else if (entropy > 28) score += 2;

        // Capping score at 100
        return Math.min(score, 100);
    }

    private String getStrengthLabel(int score) {
        if (score >= 90) return "Very Strong";
        if (score >= 70) return "Strong";
        if (score >= 50) return "Moderate";
        if (score >= 30) return "Weak";
        return "Very Weak";
    }

    private String[] generateRecommendations(String password, boolean hasLowercase, boolean hasUppercase,
                                             boolean hasDigits, boolean hasSpecial) {

        int length = password.length();
        int recommendations = 0;
        String[] tips = new String[5]; // Max 5 recommendations

        // Length recommendation
        if (length < 12) {
            tips[recommendations++] = "Increase password length to at least 12 characters";
        }

        // Character types recommendations
        if (!hasLowercase) {
            tips[recommendations++] = "Add lowercase letters (a-z)";
        }

        if (!hasUppercase) {
            tips[recommendations++] = "Add uppercase letters (A-Z)";
        }

        if (!hasDigits) {
            tips[recommendations++] = "Add numbers (0-9)";
        }

        if (!hasSpecial) {
            tips[recommendations++] = "Add special characters (!@#$%^&*...)";
        }

        // Pattern recommendations (if we have space)
        if (recommendations < 5 && detectCommonPatterns(password)) {
            tips[recommendations++] = "Avoid common patterns and dictionary words";
        }

        // Remove null entries
        String[] finalTips = new String[recommendations];
        System.arraycopy(tips, 0, finalTips, 0, recommendations);

        return finalTips;
    }

    private boolean detectCommonPatterns(String password) {
        // Very simplified pattern detection
        String lower = password.toLowerCase();

        // Check for sequential characters
        String[] sequences = {"qwerty", "asdfgh", "zxcvbn", "1234", "abcd"};
        for (String seq : sequences) {
            if (lower.contains(seq)) return true;
        }

        // Check for repeated characters
        for (int i = 0; i < lower.length() - 2; i++) {
            if (lower.charAt(i) == lower.charAt(i + 1) && lower.charAt(i) == lower.charAt(i + 2)) {
                return true;
            }
        }

        // Very common replacements
        if (lower.contains("@")) {
            String withA = lower.replace("@", "a");
            if (isCommonDictionaryWord(withA)) return true;
        }

        if (lower.contains("0")) {
            String withO = lower.replace("0", "o");
            if (isCommonDictionaryWord(withO)) return true;
        }

        return false;
    }

    private boolean isCommonDictionaryWord(String word) {
        // Very simplified check for common words
        // In a real implementation, this would use a dictionary lookup
        String[] commonWords = {"password", "admin", "welcome", "secret", "123456", "qwerty"};
        for (String common : commonWords) {
            if (word.contains(common)) return true;
        }
        return false;
    }

    private String maskPassword(String password) {
        // Show first and last character, mask the rest
        if (password.length() <= 2) {
            return "*".repeat(password.length());
        }
        return password.charAt(0) + "*".repeat(password.length() - 2) + password.charAt(password.length() - 1);
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}