package kostovite;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// Assuming PluginInterface is standard
public class ETACalculator implements PluginInterface {

    // Formatting
    private static final DateTimeFormatter INPUT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter OUTPUT_DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");
    // private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#,##0.##"); // Unused now
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#,##0.##'%'");
    private static final Locale DEFAULT_LOCALE = Locale.US; // Keep for formatting consistency if needed

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "ETACalculator";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("ETA Calculator Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uiOperation", "calculate"); // Use new ID
            params.put("totalItems", 500.0);     // Use new ID
            params.put("startTime", LocalDateTime.now().minusMinutes(10).format(INPUT_DATETIME_FORMATTER)); // Use new ID
            params.put("itemsProcessedSample", 5.0); // Use new ID
            params.put("timeElapsedSample", 3.0);  // Use new ID
            params.put("timeUnitSample", "minutes"); // Use new ID
            params.put("timezone", "Europe/London"); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("calculateETA Result: " + result);

            params.clear();
            params.put("uiOperation", "progress");
            params.put("totalItems", 1000.0);
            params.put("itemsCompleted", 250.0); // Use new ID
            params.put("startTime", LocalDateTime.now().minusHours(1).format(INPUT_DATETIME_FORMATTER));
            params.put("timezone", "America/New_York");

            result = process(params);
            System.out.println("calculateProgress Result: " + result);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.)
     * ONLY for ETA and Progress calculations.
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "ETACalculator");
        metadata.put("name", "ETA Calculator");
        metadata.put("description", "Estimate task completion time or calculate progress.");
        metadata.put("icon", "ScheduleSend");
        metadata.put("category", "Utilities");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Calculation Type & Common Inputs ---
        Map<String, Object> commonSection = new HashMap<>();
        commonSection.put("id", "commonConfig");
        commonSection.put("label", "Calculation Setup");

        List<Map<String, Object>> commonInputs = new ArrayList<>();

        // Operation Selection
        commonInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Calculation Type:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "calculate", "label", "Estimate Total Time (from sample rate)"),
                        Map.of("value", "progress", "label", "Calculate Current Progress & ETA")
                )),
                Map.entry("default", "calculate"),
                Map.entry("required", true)
        ));

        // Total Items (Common)
        commonInputs.add(Map.ofEntries(
                Map.entry("id", "totalItems"),
                Map.entry("label", "Total Units/Items to Process:"),
                Map.entry("type", "number"),
                Map.entry("default", 100.0),
                Map.entry("min", 1),
                Map.entry("required", true),
                Map.entry("helperText", "The total size of the task.")
        ));

        // Start Time (Common)
        commonInputs.add(Map.ofEntries(
                Map.entry("id", "startTime"),
                Map.entry("label", "Task Start Time:"),
                Map.entry("type", "text"), // Keep as text, requires specific format
                Map.entry("default", LocalDateTime.now().format(INPUT_DATETIME_FORMATTER)),
                Map.entry("required", true),
                Map.entry("placeholder", "YYYY-MM-DD HH:MM:SS"),
                Map.entry("helperText", "When the task processing began.")
        ));

        // Timezone (Common)
        commonInputs.add(Map.ofEntries(
                Map.entry("id", "timezone"),
                Map.entry("label", "Timezone (Optional):"),
                Map.entry("type", "text"),
                Map.entry("default", ZoneId.systemDefault().getId()),
                Map.entry("required", false),
                Map.entry("placeholder", "e.g., America/New_York, UTC"),
                Map.entry("helperText", "Used for time calculations.")
        ));

        commonSection.put("inputs", commonInputs);
        sections.add(commonSection);


        // --- Section 2: Calculation Mode Specific Inputs ---
        Map<String, Object> specificSection = new HashMap<>();
        specificSection.put("id", "specificInputs");
        specificSection.put("label", "Calculation Specific Inputs");

        List<Map<String, Object>> specificInputs = new ArrayList<>();

        // Fields for 'calculate' mode (Estimate from Sample)
        specificInputs.add(Map.ofEntries(
                Map.entry("id", "itemsProcessedSample"),
                Map.entry("label", "Units Processed (in sample):"),
                Map.entry("type", "number"),
                Map.entry("default", 10.0),
                Map.entry("min", 1),
                Map.entry("required", true), // Logic handled in process
                Map.entry("condition", "uiOperation === 'calculate'"),
                Map.entry("helperText", "How many units finished during the sample time.")
        ));
        specificInputs.add(Map.ofEntries(
                Map.entry("id", "timeElapsedSample"),
                Map.entry("label", "Time Taken for Sample:"),
                Map.entry("type", "number"),
                Map.entry("default", 5.0),
                Map.entry("min", 0.001), // Allow small fractions
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'calculate'")
        ));
        specificInputs.add(Map.ofEntries(
                Map.entry("id", "timeUnitSample"),
                Map.entry("label", "Time Unit for Sample:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "seconds", "label", "Seconds"),
                        Map.of("value", "minutes", "label", "Minutes"),
                        Map.of("value", "hours", "label", "Hours")
                )),
                Map.entry("default", "minutes"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'calculate'")
        ));

        // Field for 'progress' mode
        specificInputs.add(Map.ofEntries(
                Map.entry("id", "itemsCompleted"),
                Map.entry("label", "Units Already Completed:"),
                Map.entry("type", "number"),
                Map.entry("default", 25.0),
                Map.entry("min", 0),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'progress'"),
                Map.entry("helperText", "How many units are finished right now.")
        ));

        specificSection.put("inputs", specificInputs);
        sections.add(specificSection);


        // --- Section 3: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Calculation Results");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Outputs for 'calculate' mode
        resultOutputs.add(createOutputField("calc_processingRate", "Processing Rate (Sample)", "text", "uiOperation === 'calculate'"));
        resultOutputs.add(createOutputField("calc_totalDuration", "Estimated Total Duration", "text", "uiOperation === 'calculate'"));
        resultOutputs.add(createOutputField("calc_endTime", "Estimated End Time", "text", "uiOperation === 'calculate'"));

        // Outputs for 'progress' mode
        resultOutputs.add(createOutputField("prog_progressPercent", "Progress", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_itemsStatus", "Items Status", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_elapsedTime", "Elapsed Time", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_processingRate", "Avg. Rate (Overall)", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_remainingTime", "Est. Remaining Time", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_endTime", "Est. End Time", "text", "uiOperation === 'progress'"));
        resultOutputs.add(createOutputField("prog_totalTime", "Est. Total Duration", "text", "uiOperation === 'progress'"));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 4: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create output field definitions more easily
    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        field.put("label", label);
        field.put("type", type); // Use the passed type
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if ("errorMessage".equals(id)) { // Special handling for the error field
            field.put("style", "error"); // Add style hint
        }
        // Fix: Check the type parameter, not the literal "text"
        if ("text".equals(type)) { // Default text outputs to monospace if time/rate/percent related
            if (id.toLowerCase().contains("time") || id.toLowerCase().contains("duration") || id.toLowerCase().contains("rate") || id.toLowerCase().contains("percent")) {
                field.put("monospace", true);
            }
        }
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format)
     * to perform ETA calculations.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        // Read the operation using the ID defined in UI metadata
        String uiOperation = (String) input.get("uiOperation");
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        if (uiOperation == null || uiOperation.isBlank()) {
            return Map.of("success", false, errorOutputId, "No operation specified.");
        }

        // Include the operation in the input map for context
        Map<String, Object> processingInput = new HashMap<>(input);

        try {
            // Default timezone
            String timezoneStr = getStringParam(processingInput, "timezone", ZoneId.systemDefault().getId());
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(timezoneStr);
            } catch (Exception e) {
                System.err.println("Invalid timezone: '" + timezoneStr + "'. Defaulting to system default.");
                zoneId = ZoneId.systemDefault();
            }

            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "calculate" -> result = calculateETA(processingInput, zoneId);
                case "progress" -> result = calculateProgress(processingInput, zoneId);
                default -> {
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
                }
            }

            // Add uiOperation to success result for context if needed by complex conditions
            if (result.get("success") == Boolean.TRUE) {
                Map<String, Object> finalResult = new HashMap<>(result);
                finalResult.put("uiOperation", uiOperation); // Add operation context
                return finalResult;
            } else {
                // Ensure error key consistency
                if (result.containsKey("error") && !result.containsKey(errorOutputId)) {
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put(errorOutputId, result.get("error"));
                    finalResult.remove("error");
                    return finalResult;
                }
                return result; // Return error as is
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (DateTimeParseException e) {
            return Map.of("success", false, errorOutputId, "Invalid date/time format. Use YYYY-MM-DD HH:MM:SS.");
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing ETA request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Calculation Methods (Updated for new IDs)
    // ========================================================================

    private Map<String, Object> calculateETA(Map<String, Object> input, ZoneId zoneId) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            // Get parameters using NEW IDs
            double totalItems = parseDoubleParam(input, "totalItems"); // Required
            double itemsProcessed = parseDoubleParam(input, "itemsProcessedSample"); // Required
            double timeElapsed = parseDoubleParam(input, "timeElapsedSample"); // Required
            String timeUnit = getStringParam(input, "timeUnitSample", null); // Required
            String startTimeStr = getStringParam(input, "startTime", null); // Required

            if (itemsProcessed <= 0) throw new IllegalArgumentException("Items processed in sample must be positive.");
            if (timeElapsed <= 0) throw new IllegalArgumentException("Time elapsed for sample must be positive.");
            if (totalItems <= 0) throw new IllegalArgumentException("Total items must be positive.");


            LocalDateTime startLocalDateTime = LocalDateTime.parse(startTimeStr, INPUT_DATETIME_FORMATTER);
            ZonedDateTime startZonedDateTime = startLocalDateTime.atZone(zoneId);

            long timeElapsedSeconds = convertToSeconds(timeElapsed, timeUnit);
            if (timeElapsedSeconds <= 0) throw new IllegalArgumentException("Sample duration must be positive.");

            double processingRate = itemsProcessed / timeElapsedSeconds;
            if (processingRate <= 0) throw new IllegalArgumentException("Processing rate must be positive.");

            long estimatedTotalSeconds = (long) Math.ceil(totalItems / processingRate);
            Duration totalDuration = Duration.ofSeconds(estimatedTotalSeconds);
            ZonedDateTime estimatedEndZonedDateTime = startZonedDateTime.plus(totalDuration);

            // Format results for NEW output IDs
            String formattedTotalDuration = formatDuration(totalDuration);
            String formattedEndTime = formatEndTime(estimatedEndZonedDateTime);
            String formattedProcessingRate = String.format(Locale.US, "%.2f items/sec", processingRate);
            if (processingRate * 60 > 0.1) {
                formattedProcessingRate += String.format(Locale.US, " (%.1f items/min)", processingRate * 60);
            }

            result.put("success", true);
            result.put("calc_processingRate", formattedProcessingRate); // Matches output ID
            result.put("calc_totalDuration", formattedTotalDuration);  // Matches output ID
            result.put("calc_endTime", formattedEndTime);             // Matches output ID

        } catch (IllegalArgumentException | DateTimeParseException e) {
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put(errorOutputId, "Calculation failed: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> calculateProgress(Map<String, Object> input, ZoneId zoneId) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            // Get parameters using NEW IDs
            double totalItems = parseDoubleParam(input, "totalItems"); // Required
            double itemsCompleted = parseDoubleParam(input, "itemsCompleted"); // Required
            String startTimeStr = getStringParam(input, "startTime", null); // Required

            if (itemsCompleted < 0) throw new IllegalArgumentException("Items completed cannot be negative.");
            if (totalItems <= 0) throw new IllegalArgumentException("Total items must be positive.");
            if (itemsCompleted > totalItems) throw new IllegalArgumentException("Items completed cannot exceed total items.");


            LocalDateTime startLocalDateTime = LocalDateTime.parse(startTimeStr, INPUT_DATETIME_FORMATTER);
            ZonedDateTime startZonedDateTime = startLocalDateTime.atZone(zoneId);
            ZonedDateTime currentZonedDateTime = ZonedDateTime.now(zoneId);

            if (currentZonedDateTime.isBefore(startZonedDateTime)) {
                throw new IllegalArgumentException("Current time cannot be before start time.");
            }

            Duration elapsedDuration = Duration.between(startZonedDateTime, currentZonedDateTime);
            long elapsedSeconds = Math.max(0, elapsedDuration.getSeconds()); // Ensure non-negative

            double progressPercentage = (totalItems > 0) ? (itemsCompleted / totalItems * 100.0) : 100.0;

            double processingRate = (elapsedSeconds > 0 && itemsCompleted > 0) ?
                    (itemsCompleted / elapsedSeconds) : 0;

            long remainingItems = (long) (totalItems - itemsCompleted); // Use long for safety
            long estimatedRemainingSeconds = 0;
            ZonedDateTime estimatedEndZonedDateTime = currentZonedDateTime;
            Duration estimatedTotalDuration = elapsedDuration;

            if (remainingItems > 0 && processingRate > 0) {
                estimatedRemainingSeconds = (long) Math.ceil(remainingItems / processingRate);
                estimatedEndZonedDateTime = currentZonedDateTime.plusSeconds(estimatedRemainingSeconds);
                estimatedTotalDuration = Duration.ofSeconds(elapsedSeconds + estimatedRemainingSeconds);
            } else if (remainingItems <= 0) { // Task completed
                processingRate = (elapsedSeconds > 0 && totalItems > 0) ? (totalItems / elapsedSeconds) : 0; // Recalculate rate based on total
                // Actual duration
            }

            // Format results for NEW output IDs
            String formattedProgress = String.format(Locale.US, "%.1f%%", progressPercentage);
            String itemsStatus = String.format(Locale.US, "%,.0f of %,.0f completed (%,.0f remaining)", itemsCompleted, totalItems, (double)remainingItems);
            String formattedElapsedTime = formatDuration(elapsedDuration);
            String formattedProcessingRate = "N/A";
            if (processingRate > 0) {
                formattedProcessingRate = String.format(Locale.US, "%.2f items/sec", processingRate);
                if (processingRate * 60 > 0.1) formattedProcessingRate += String.format(Locale.US, " (%.1f items/min)", processingRate * 60);
            } else if (itemsCompleted > 0 && elapsedSeconds == 0) formattedProcessingRate = "Rate N/A (instantaneous?)";
            else if (itemsCompleted == 0) formattedProcessingRate = "Rate N/A (0 completed)";

            String formattedRemainingTime = (remainingItems <= 0) ? "Completed" : formatDuration(Duration.ofSeconds(estimatedRemainingSeconds));
            String formattedEndTime = (remainingItems <= 0) ? "Completed at/before " + formatEndTime(currentZonedDateTime) : formatEndTime(estimatedEndZonedDateTime);
            String formattedTotalTime = (remainingItems <= 0 && elapsedSeconds > 0) ? formattedElapsedTime + " (Actual)" : formatDuration(estimatedTotalDuration);

            result.put("success", true);
            result.put("prog_progressPercent", formattedProgress); // Matches output ID
            result.put("prog_itemsStatus", itemsStatus);           // Matches output ID
            result.put("prog_elapsedTime", formattedElapsedTime); // Matches output ID
            result.put("prog_processingRate", formattedProcessingRate); // Matches output ID
            result.put("prog_remainingTime", formattedRemainingTime); // Matches output ID
            result.put("prog_endTime", formattedEndTime);           // Matches output ID
            result.put("prog_totalTime", formattedTotalTime);         // Matches output ID

        } catch (IllegalArgumentException | DateTimeParseException e) {
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put(errorOutputId, "Calculation failed: " + e.getMessage());
        }
        return result;
    }

    // --- Helper Methods ---

    private long convertToSeconds(double time, String unit) throws IllegalArgumentException {
        if (time < 0) throw new IllegalArgumentException("Time value cannot be negative.");
        if (unit == null || unit.isBlank()) throw new IllegalArgumentException("Time unit cannot be empty.");

        return switch (unit.toLowerCase()) {
            case "hours", "hour", "h" -> (long) (time * 3600.0);
            case "minutes", "minute", "min", "m" -> (long) (time * 60.0);
            case "seconds", "second", "sec", "s" -> (long) Math.ceil(time); // Use ceil for seconds precision
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) return "0 seconds";
        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long remainingSeconds = totalSeconds % 60;
        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + (days == 1 ? " day" : " days"));
        if (hours > 0) parts.add(hours + (hours == 1 ? " hour" : " hours"));
        if (minutes > 0) parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        if (remainingSeconds > 0 && totalSeconds < 60 || (remainingSeconds > 0 && parts.isEmpty())) {
            parts.add(remainingSeconds + (remainingSeconds == 1 ? " second" : " seconds"));
        } else if (parts.isEmpty()) return "0 seconds";
        return String.join(" ", parts);
    }

    private String formatEndTime(ZonedDateTime endZonedDateTime) {
        if (endZonedDateTime == null) return "N/A";
        ZonedDateTime now = ZonedDateTime.now(endZonedDateTime.getZone());
        if (endZonedDateTime.toLocalDate().equals(now.toLocalDate())) {
            return "today at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
        }
        if (endZonedDateTime.toLocalDate().equals(now.toLocalDate().plusDays(1))) {
            return "tomorrow at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
        }
        if (!endZonedDateTime.isBefore(now) && endZonedDateTime.isBefore(now.plusDays(7))) {
            String dayName = endZonedDateTime.getDayOfWeek().toString();
            return dayName.substring(0, 1).toUpperCase() + dayName.substring(1).toLowerCase() + " at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
        }
        if (endZonedDateTime.isBefore(now)) {
            if (endZonedDateTime.toLocalDate().equals(now.toLocalDate())) return "earlier today at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
            if (endZonedDateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) return "yesterday at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
            return "on " + endZonedDateTime.format(OUTPUT_DAY_MONTH_FORMATTER) + " at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
        }
        return "on " + endZonedDateTime.format(OUTPUT_DAY_MONTH_FORMATTER) + " at " + endZonedDateTime.format(OUTPUT_TIME_FORMATTER);
    }

    // Null default indicates required
    private double parseDoubleParam(Map<String, Object> input, String key) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required numeric parameter: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            try { return Double.parseDouble(value.toString().replace(',', '.')); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric value for parameter '" + key + "': " + value);
            }
        }
    }

    // Null default indicates required
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return strValue;
    }
}