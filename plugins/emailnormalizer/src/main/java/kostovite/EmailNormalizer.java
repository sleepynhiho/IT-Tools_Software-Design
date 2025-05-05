package kostovite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailNormalizer implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    // Regular expression for validating email format
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Override
    public String getName() {
        return "EmailNormalizer";
    }

    @Override
    public void execute() {
        System.out.println("EmailNormalizer Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test with various email formats
            params.put("rawEmails", "John.Doe@EXAMPLE.com\njohn.doe@example.com\nj.o.h.n.d.o.e@example.com\njohndoe@example.com\nJohn.Doe+newsletter@example.com\njohn.doe@gmail.com\nJOHN.DOE@GMAIL.COM");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Various valid emails): " + result1);

            // Test with an empty input
            params.put("rawEmails", "");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Empty input): " + result2);

            // Test with invalid emails
            params.put("rawEmails", "not-an-email\njohn@\n@example.com");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Invalid emails): " + result3);

            // Test with mixed valid and invalid emails
            params.put("rawEmails", "valid@example.com\nnot-valid\njane.doe@company.org");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Mixed valid and invalid): " + result4);

            // Test with emails having comments and whitespace
            params.put("rawEmails", "  john@example.com  \njanedoe@example.com # personal\n\n\ntest@test.com");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (With comments and whitespace): " + result5);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("id", "EmailNormalizer");
        metadata.put("name", "Email normalizer");
        metadata.put("description", "Normalize email addresses to a standard format for easier comparison. Useful for deduplication and data cleaning.");
        metadata.put("icon", "Email");
        metadata.put("category", "Development");
        metadata.put("customUI", false);

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: Email Normalizer ---
        Map<String, Object> emailNormalizerSection = new HashMap<>();
        emailNormalizerSection.put("id", "emailNormalizer");
        emailNormalizerSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "rawEmails"),
                Map.entry("label", "Raw emails to normalize:"),
                Map.entry("placeholder", "Enter your email addresses"),
                Map.entry("type", "text"),
                Map.entry("required", true),
                Map.entry("multiline", true),
                Map.entry("containerId", "input"),
                Map.entry("width", 600),
                Map.entry("height", 200)
        ));
        emailNormalizerSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();
        outputs.add(Map.ofEntries(
                Map.entry("id", "normalizedEmails"),
                Map.entry("label", "Normalized emails:"),
                Map.entry("type", "text"),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("containerId", "output"),
                Map.entry("width", 600),
                Map.entry("height", 200),
                Map.entry("monospace", true)
        ));
        emailNormalizerSection.put("outputs", outputs);

        sections.add(emailNormalizerSection);

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
            String rawEmails = getStringParam(input, "rawEmails", null);

            // --- Validation ---
            if (rawEmails == null || rawEmails.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Email input is required.");
            }

            // Process emails
            List<String> normalizedList = new ArrayList<>();
            List<String> invalidList = new ArrayList<>();
            int validCount = 0;
            int invalidCount = 0;

            // Split input by newlines and process each line
            String[] emailLines = rawEmails.split("\\r?\\n");
            Set<String> uniqueEmails = new HashSet<>();

            for (String line : emailLines) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Remove comments (anything after #) and trim whitespace
                String cleanLine = line;
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    cleanLine = line.substring(0, commentIndex);
                }
                cleanLine = cleanLine.trim();

                if (isValidEmail(cleanLine)) {
                    String normalized = normalizeEmail(cleanLine);
                    if (uniqueEmails.add(normalized)) {  // Only add if not a duplicate
                        normalizedList.add(normalized);
                        validCount++;
                    }
                } else {
                    invalidList.add(cleanLine + " (invalid format)");
                    invalidCount++;
                }
            }

            // Prepare output
            StringBuilder output = new StringBuilder();

            // Add summary
            output.append("Summary: ")
                    .append(validCount)
                    .append(" valid emails (")
                    .append(normalizedList.size())
                    .append(" unique), ")
                    .append(invalidCount)
                    .append(" invalid\n\n");

            // Add normalized emails
            output.append("Normalized emails:\n");
            for (String email : normalizedList) {
                output.append(email).append("\n");
            }

            // Add invalid emails if any
            if (!invalidList.isEmpty()) {
                output.append("\nInvalid emails:\n");
                for (String invalid : invalidList) {
                    output.append(invalid).append("\n");
                }
            }

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("normalizedEmails", output.toString());

            return result;

        } catch (Exception e) {
            System.err.println("Error processing email normalization: " + e.getMessage());
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
     * Check if a string is a valid email address format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    /**
     * Normalize an email address
     * - Convert to lowercase
     * - Remove dots from Gmail username parts
     * - Remove Gmail's plus addressing
     * - Handle other provider-specific normalization rules
     */
    private String normalizeEmail(String email) {
        // Convert to lowercase
        String normalized = email.toLowerCase();

        // Split into local part and domain
        String[] parts = normalized.split("@", 2);
        String localPart = parts[0];
        String domain = parts[1];

        // Special handling for Gmail
        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            // Remove dots from username (Gmail ignores dots)
            localPart = localPart.replace(".", "");

            // Remove everything after + (Gmail plus addressing)
            int plusIndex = localPart.indexOf('+');
            if (plusIndex > 0) {
                localPart = localPart.substring(0, plusIndex);
            }

            // Standardize domain to gmail.com
            domain = "gmail.com";
        }

        // Special handling for outlook.com, hotmail.com, live.com (Microsoft domains)
        if (domain.equals("outlook.com") || domain.equals("hotmail.com") || domain.equals("live.com")) {
            // Remove everything after + (Microsoft plus addressing)
            int plusIndex = localPart.indexOf('+');
            if (plusIndex > 0) {
                localPart = localPart.substring(0, plusIndex);
            }
        }

        // Special handling for yahoo.com
        if (domain.equals("yahoo.com")) {
            // Remove everything after - (Yahoo disposable addresses)
            int dashIndex = localPart.indexOf('-');
            if (dashIndex > 0) {
                localPart = localPart.substring(0, dashIndex);
            }
        }

        return localPart + "@" + domain;
    }
}