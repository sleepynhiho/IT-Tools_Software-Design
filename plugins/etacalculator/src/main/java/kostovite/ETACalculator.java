package kostovite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ETACalculator implements PluginInterface {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");

    @Override
    public String getName() {
        return "ETACalculator";
    }

    @Override
    public void execute() {
        System.out.println("ETA Calculator Plugin executed");

        // Demonstrate basic usage
        try {
            // Example calculation
            Map<String, Object> params = new HashMap<>();
            params.put("totalItems", 500);
            params.put("startTime", "2025-04-11 09:00:00");
            params.put("itemsProcessed", 5);
            params.put("timeElapsed", 3);
            params.put("timeUnit", "minutes");

            Map<String, Object> result = process(params);
            System.out.println("Total duration: " + result.get("formattedDuration"));
            System.out.println("ETA: " + result.get("formattedEndTime"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Calculate estimated time of arrival/completion"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Calculate ETA operation
        Map<String, Object> calculateOperation = new HashMap<>();
        calculateOperation.put("description", "Calculate ETA based on processing rate");
        Map<String, Object> calculateInputs = new HashMap<>();
        calculateInputs.put("totalItems", Map.of("type", "integer", "description", "Total number of items to process", "required", true));
        calculateInputs.put("itemsProcessed", Map.of("type", "integer", "description", "Number of items processed in the sample time period", "required", true));
        calculateInputs.put("timeElapsed", Map.of("type", "integer", "description", "Amount of time elapsed for the sample", "required", true));
        calculateInputs.put("timeUnit", Map.of("type", "string", "description", "Time unit for timeElapsed (seconds, minutes, hours)", "required", false));
        calculateInputs.put("startTime", Map.of("type", "string", "description", "Start time in format YYYY-MM-DD HH:MM:SS", "required", true));
        calculateInputs.put("timezone", Map.of("type", "string", "description", "Timezone for calculations (optional, defaults to UTC)", "required", false));
        calculateOperation.put("inputs", calculateInputs);
        operations.put("calculate", calculateOperation);

        // Calculate progress operation
        Map<String, Object> progressOperation = new HashMap<>();
        progressOperation.put("description", "Calculate progress information for an ongoing task");
        Map<String, Object> progressInputs = new HashMap<>();
        progressInputs.put("totalItems", Map.of("type", "integer", "description", "Total number of items to process", "required", true));
        progressInputs.put("itemsCompleted", Map.of("type", "integer", "description", "Number of items already processed", "required", true));
        progressInputs.put("startTime", Map.of("type", "string", "description", "Start time in format YYYY-MM-DD HH:MM:SS", "required", true));
        progressInputs.put("timezone", Map.of("type", "string", "description", "Timezone for calculations (optional, defaults to UTC)", "required", false));
        progressOperation.put("inputs", progressInputs);
        operations.put("progress", progressOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "ETACalculator"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Schedule"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Utilities"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Calculation Type");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "uiOperation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "calculate", "label", "Calculate ETA"));
        operationOptions.add(Map.of("value", "progress", "label", "Calculate Progress"));
        operationField.put("options", operationOptions);
        operationField.put("default", "calculate");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Common Parameters
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Common Parameters");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Total items field
        Map<String, Object> totalItemsField = new HashMap<>();
        totalItemsField.put("name", "totalItems");
        totalItemsField.put("label", "Total Items to Process:");
        totalItemsField.put("type", "number");
        totalItemsField.put("default", 100);
        totalItemsField.put("min", 1);
        totalItemsField.put("required", true);
        section2Fields.add(totalItemsField);

        // Start time field
        Map<String, Object> startTimeField = new HashMap<>();
        startTimeField.put("name", "startTime");
        startTimeField.put("label", "Start Time (YYYY-MM-DD HH:MM:SS):");
        startTimeField.put("type", "text");
        startTimeField.put("default", LocalDateTime.now().format(DATETIME_FORMATTER));
        startTimeField.put("required", true);
        section2Fields.add(startTimeField);

        // Timezone field
        Map<String, Object> timezoneField = new HashMap<>();
        timezoneField.put("name", "timezone");
        timezoneField.put("label", "Timezone:");
        timezoneField.put("type", "text");
        timezoneField.put("default", "UTC");
        timezoneField.put("required", false);
        section2Fields.add(timezoneField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: ETA Calculation (conditional)
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "ETA Calculation Parameters");
        inputSection3.put("condition", "uiOperation === 'calculate'");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Items processed field
        Map<String, Object> itemsProcessedField = new HashMap<>();
        itemsProcessedField.put("name", "itemsProcessed");
        itemsProcessedField.put("label", "Items Processed in Sample:");
        itemsProcessedField.put("type", "number");
        itemsProcessedField.put("default", 10);
        itemsProcessedField.put("min", 1);
        itemsProcessedField.put("required", true);
        section3Fields.add(itemsProcessedField);

        // Time elapsed field
        Map<String, Object> timeElapsedField = new HashMap<>();
        timeElapsedField.put("name", "timeElapsed");
        timeElapsedField.put("label", "Time Elapsed for Sample:");
        timeElapsedField.put("type", "number");
        timeElapsedField.put("default", 5);
        timeElapsedField.put("min", 1);
        timeElapsedField.put("required", true);
        section3Fields.add(timeElapsedField);

        // Time unit field
        Map<String, Object> timeUnitField = new HashMap<>();
        timeUnitField.put("name", "timeUnit");
        timeUnitField.put("label", "Time Unit:");
        timeUnitField.put("type", "select");
        List<Map<String, String>> timeUnitOptions = new ArrayList<>();
        timeUnitOptions.add(Map.of("value", "seconds", "label", "Seconds"));
        timeUnitOptions.add(Map.of("value", "minutes", "label", "Minutes"));
        timeUnitOptions.add(Map.of("value", "hours", "label", "Hours"));
        timeUnitField.put("options", timeUnitOptions);
        timeUnitField.put("default", "minutes");
        timeUnitField.put("required", false);
        section3Fields.add(timeUnitField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        // Input Section 4: Progress Calculation (conditional)
        Map<String, Object> inputSection4 = new HashMap<>();
        inputSection4.put("header", "Progress Calculation Parameters");
        inputSection4.put("condition", "uiOperation === 'progress'");
        List<Map<String, Object>> section4Fields = new ArrayList<>();

        // Items completed field
        Map<String, Object> itemsCompletedField = new HashMap<>();
        itemsCompletedField.put("name", "itemsCompleted");
        itemsCompletedField.put("label", "Items Already Completed:");
        itemsCompletedField.put("type", "number");
        itemsCompletedField.put("default", 25);
        itemsCompletedField.put("min", 0);
        itemsCompletedField.put("required", true);
        section4Fields.add(itemsCompletedField);

        inputSection4.put("fields", section4Fields);
        uiInputs.add(inputSection4);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: ETA Results (conditional)
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "ETA Calculation Results");
        outputSection1.put("condition", "uiOperation === 'calculate'");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Processing Rate
        Map<String, Object> processingRateOutput = new HashMap<>();
        processingRateOutput.put("title", "Processing Rate");
        processingRateOutput.put("name", "processingRateFormatted");
        processingRateOutput.put("type", "text");
        section1OutputFields.add(processingRateOutput);

        // Total Duration
        Map<String, Object> totalDurationOutput = new HashMap<>();
        totalDurationOutput.put("title", "Total Duration");
        totalDurationOutput.put("name", "formattedDuration");
        totalDurationOutput.put("type", "text");
        section1OutputFields.add(totalDurationOutput);

        // Will End At
        Map<String, Object> endTimeOutput = new HashMap<>();
        endTimeOutput.put("title", "Will End At");
        endTimeOutput.put("name", "formattedEndTime");
        endTimeOutput.put("type", "text");
        section1OutputFields.add(endTimeOutput);

        // Remaining Time
        Map<String, Object> remainingTimeOutput = new HashMap<>();
        remainingTimeOutput.put("title", "Remaining Time");
        remainingTimeOutput.put("name", "formattedRemainingTime");
        remainingTimeOutput.put("type", "text");
        section1OutputFields.add(remainingTimeOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Progress Results (conditional)
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Progress Information");
        outputSection2.put("condition", "uiOperation === 'progress'");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Progress Percentage
        Map<String, Object> progressOutput = new HashMap<>();
        progressOutput.put("title", "Progress");
        progressOutput.put("name", "formattedProgress");
        progressOutput.put("type", "text");
        section2OutputFields.add(progressOutput);

        // Items Completed/Remaining
        Map<String, Object> itemsStatusOutput = new HashMap<>();
        itemsStatusOutput.put("title", "Items Completed/Remaining");
        itemsStatusOutput.put("name", "itemsStatus");
        itemsStatusOutput.put("type", "text");
        itemsStatusOutput.put("formula", "itemsCompleted + ' of ' + totalItems + ' (' + remainingItems + ' remaining)'");
        section2OutputFields.add(itemsStatusOutput);

        // Processing Rate
        Map<String, Object> progressRateOutput = new HashMap<>();
        progressRateOutput.put("title", "Processing Rate");
        progressRateOutput.put("name", "processingRateFormatted");
        progressRateOutput.put("type", "text");
        section2OutputFields.add(progressRateOutput);

        // Elapsed Time
        Map<String, Object> elapsedTimeOutput = new HashMap<>();
        elapsedTimeOutput.put("title", "Elapsed Time");
        elapsedTimeOutput.put("name", "formattedElapsedTime");
        elapsedTimeOutput.put("type", "text");
        section2OutputFields.add(elapsedTimeOutput);

        // Estimated Remaining Time
        Map<String, Object> estRemainingTimeOutput = new HashMap<>();
        estRemainingTimeOutput.put("title", "Estimated Remaining");
        estRemainingTimeOutput.put("name", "formattedRemainingTime");
        estRemainingTimeOutput.put("type", "text");
        section2OutputFields.add(estRemainingTimeOutput);

        // Estimated End Time
        Map<String, Object> estEndTimeOutput = new HashMap<>();
        estEndTimeOutput.put("title", "Estimated End Time");
        estEndTimeOutput.put("name", "formattedEndTime");
        estEndTimeOutput.put("type", "text");
        section2OutputFields.add(estEndTimeOutput);

        // Total Duration
        Map<String, Object> totalTimeOutput = new HashMap<>();
        totalTimeOutput.put("title", "Total Duration");
        totalTimeOutput.put("name", "formattedTotalTime");
        totalTimeOutput.put("type", "text");
        section2OutputFields.add(totalTimeOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display (conditional)
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "error");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section3OutputFields.add(errorOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "calculate");

            switch (operation.toLowerCase()) {
                case "calculate":
                    // Get parameters
                    Integer totalItems = getIntegerParameter(input, "totalItems");
                    Integer itemsProcessed = getIntegerParameter(input, "itemsProcessed");
                    Integer timeElapsed = getIntegerParameter(input, "timeElapsed");
                    String timeUnit = (String) input.getOrDefault("timeUnit", "minutes");
                    String startTimeStr = (String) input.get("startTime");
                    String timezone = (String) input.getOrDefault("timezone", "UTC");

                    // Validate required parameters
                    if (totalItems == null || itemsProcessed == null ||
                            timeElapsed == null || startTimeStr == null) {
                        result.put("error", "Missing required parameters");
                        return result;
                    }

                    return calculateETA(totalItems, itemsProcessed, timeElapsed,
                            timeUnit, startTimeStr, timezone);

                case "progress":
                    // Get parameters
                    Integer progressTotalItems = getIntegerParameter(input, "totalItems");
                    Integer itemsCompleted = getIntegerParameter(input, "itemsCompleted");
                    String progressStartTimeStr = (String) input.get("startTime");
                    String progressTimezone = (String) input.getOrDefault("timezone", "UTC");

                    // Validate required parameters
                    if (progressTotalItems == null || itemsCompleted == null || progressStartTimeStr == null) {
                        result.put("error", "Missing required parameters");
                        return result;
                    }

                    return calculateProgress(progressTotalItems, itemsCompleted,
                            progressStartTimeStr, progressTimezone);

                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Helper to get integer parameter from input
     *
     * @param input Input map
     * @param paramName Parameter name
     * @return Integer value or null
     */
    private Integer getIntegerParameter(Map<String, Object> input, String paramName) {
        Object value = input.get(paramName);
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Calculate ETA based on processing rate
     *
     * @param totalItems Total number of items to process
     * @param itemsProcessed Number of items processed in sample period
     * @param timeElapsed Time elapsed for sample period
     * @param timeUnit Time unit (seconds, minutes, hours)
     * @param startTimeStr Start time string
     * @param timezone Timezone for calculations
     * @return ETA calculation results
     */
    private Map<String, Object> calculateETA(Integer totalItems, Integer itemsProcessed,
                                             Integer timeElapsed, String timeUnit, String startTimeStr, String timezone) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Parse start time
            LocalDateTime startTime = parseDateTime(startTimeStr);
            if (startTime == null) {
                result.put("error", "Invalid start time format. Use YYYY-MM-DD HH:MM:SS");
                return result;
            }

            // Get current time
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime currentZonedDateTime = ZonedDateTime.now(zoneId);
            LocalDateTime currentTime = currentZonedDateTime.toLocalDateTime();

            // Convert time unit to seconds
            long timeElapsedSeconds = convertToSeconds(timeElapsed, timeUnit);

            // Calculate processing rate (items per second)
            double processingRate = (double) itemsProcessed / timeElapsedSeconds;

            // Calculate remaining items and time
            int remainingItems = totalItems;
            long estimatedSecondsTotal = (long) Math.ceil(remainingItems / processingRate);

            // Calculate ETA
            LocalDateTime endTime = startTime.plusSeconds(estimatedSecondsTotal);
            Duration totalDuration = Duration.between(startTime, endTime);
            Duration remainingDuration = Duration.between(currentTime, endTime);

            // Format durations
            String formattedTotalDuration = formatDuration(totalDuration);
            String formattedRemainingDuration = formatDuration(remainingDuration);

            // Format end time
            String formattedEndTime = formatEndTime(endTime, currentZonedDateTime);

            // Build result
            result.put("success", true);
            result.put("totalItems", totalItems);
            result.put("processingRate", processingRate);
            result.put("processingRateFormatted", String.format("%.2f items/second", processingRate));
            result.put("processingRatePerMinute", processingRate * 60);
            result.put("processingRatePerHour", processingRate * 3600);
            result.put("startTime", startTime.format(DATETIME_FORMATTER));
            result.put("endTime", endTime.format(DATETIME_FORMATTER));
            result.put("formattedEndTime", formattedEndTime);
            result.put("durationSeconds", totalDuration.getSeconds());
            result.put("formattedDuration", formattedTotalDuration);
            result.put("remainingTimeSeconds", remainingDuration.getSeconds());
            result.put("formattedRemainingTime", formattedRemainingDuration);
            result.put("currentTime", currentTime.format(DATETIME_FORMATTER));
            result.put("timezone", timezone);

        } catch (Exception e) {
            result.put("error", "Error calculating ETA: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Calculate progress information for an ongoing task
     *
     * @param totalItems Total number of items
     * @param itemsCompleted Number of items completed so far
     * @param startTimeStr Start time string
     * @param timezone Timezone for calculations
     * @return Progress information
     */
    private Map<String, Object> calculateProgress(Integer totalItems, Integer itemsCompleted,
                                                  String startTimeStr, String timezone) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Parse start time
            LocalDateTime startTime = parseDateTime(startTimeStr);
            if (startTime == null) {
                result.put("error", "Invalid start time format. Use YYYY-MM-DD HH:MM:SS");
                return result;
            }

            // Get current time
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime currentZonedDateTime = ZonedDateTime.now(zoneId);
            LocalDateTime currentTime = currentZonedDateTime.toLocalDateTime();

            // Calculate elapsed time
            Duration elapsedDuration = Duration.between(startTime, currentTime);
            long elapsedSeconds = elapsedDuration.getSeconds();

            // Calculate progress percentage
            double progressPercentage = (double) itemsCompleted / totalItems * 100.0;

            // Calculate processing rate (items per second)
            double processingRate = elapsedSeconds > 0 ?
                    (double) itemsCompleted / elapsedSeconds : 0;

            // Calculate ETA
            int remainingItems = totalItems - itemsCompleted;
            long estimatedRemainingSeconds = processingRate > 0 ?
                    (long) Math.ceil(remainingItems / processingRate) : 0;

            LocalDateTime estimatedEndTime = currentTime.plusSeconds(estimatedRemainingSeconds);
            Duration remainingDuration = Duration.ofSeconds(estimatedRemainingSeconds);
            Duration totalDuration = Duration.ofSeconds(elapsedSeconds + estimatedRemainingSeconds);

            // Format durations and times
            String formattedElapsedTime = formatDuration(elapsedDuration);
            String formattedRemainingTime = formatDuration(remainingDuration);
            String formattedTotalTime = formatDuration(totalDuration);
            String formattedEndTime = formatEndTime(estimatedEndTime, currentZonedDateTime);

            // Build result
            result.put("success", true);
            result.put("totalItems", totalItems);
            result.put("itemsCompleted", itemsCompleted);
            result.put("remainingItems", remainingItems);
            result.put("progressPercentage", progressPercentage);
            result.put("formattedProgress", String.format("%.1f%%", progressPercentage));
            result.put("processingRate", processingRate);
            result.put("processingRateFormatted", String.format("%.2f items/second", processingRate));
            result.put("processingRatePerMinute", processingRate * 60);
            result.put("processingRatePerHour", processingRate * 3600);
            result.put("startTime", startTime.format(DATETIME_FORMATTER));
            result.put("currentTime", currentTime.format(DATETIME_FORMATTER));
            result.put("estimatedEndTime", estimatedEndTime.format(DATETIME_FORMATTER));
            result.put("formattedEndTime", formattedEndTime);
            result.put("elapsedSeconds", elapsedSeconds);
            result.put("formattedElapsedTime", formattedElapsedTime);
            result.put("estimatedRemainingSeconds", estimatedRemainingSeconds);
            result.put("formattedRemainingTime", formattedRemainingTime);
            result.put("estimatedTotalSeconds", elapsedSeconds + estimatedRemainingSeconds);
            result.put("formattedTotalTime", formattedTotalTime);
            result.put("timezone", timezone);

        } catch (Exception e) {
            result.put("error", "Error calculating progress: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Parse a date time string
     *
     * @param dateTimeStr Date time string
     * @return LocalDateTime object or null if parsing fails
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try other common formats
            try {
                // Try to parse as date only and use current time
                LocalDateTime now = LocalDateTime.now();
                return LocalDateTime.parse(dateTimeStr + " " +
                        now.format(TIME_ONLY_FORMATTER), DATETIME_FORMATTER);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /**
     * Convert a time value to seconds based on the unit
     *
     * @param time Time value
     * @param unit Time unit (seconds, minutes, hours)
     * @return Time in seconds
     */
    private long convertToSeconds(int time, String unit) {
        switch (unit.toLowerCase()) {
            case "hours":
            case "hour":
            case "h":
                return time * 3600L;
            case "minutes":
            case "minute":
            case "min":
            case "m":
                return time * 60L;
            case "seconds":
            case "second":
            case "sec":
            case "s":
            default:
                return time;
        }
    }

    /**
     * Format a duration into a human-readable string
     *
     * @param duration Duration object
     * @return Formatted duration string
     */
    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "Unknown";
        }

        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            if (minutes > 0 || remainingSeconds > 0) {
                sb.append(" ");
            }
        }

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            if (remainingSeconds > 0) {
                sb.append(" ");
            }
        }

        if (remainingSeconds > 0 && hours == 0) { // Only show seconds if less than an hour
            sb.append(remainingSeconds).append(remainingSeconds == 1 ? " second" : " seconds");
        }

        return sb.length() > 0 ? sb.toString() : "less than a second";
    }

    /**
     * Format an end time into a human-readable string
     *
     * @param endTime End time
     * @param currentZonedDateTime Current time with timezone
     * @return Formatted end time string
     */
    private String formatEndTime(LocalDateTime endTime, ZonedDateTime currentZonedDateTime) {
        LocalDateTime currentTime = currentZonedDateTime.toLocalDateTime();

        // Check if it's today
        if (endTime.toLocalDate().equals(currentTime.toLocalDate())) {
            return "today at " + endTime.format(TIME_ONLY_FORMATTER);
        }

        // Check if it's tomorrow
        if (endTime.toLocalDate().equals(currentTime.toLocalDate().plusDays(1))) {
            return "tomorrow at " + endTime.format(TIME_ONLY_FORMATTER);
        }

        // Check if it's within a week
        if (endTime.toLocalDate().isBefore(currentTime.toLocalDate().plusDays(7))) {
            return endTime.getDayOfWeek().toString().toLowerCase() + " at " +
                    endTime.format(TIME_ONLY_FORMATTER);
        }

        // Otherwise include the date
        return endTime.format(DAY_MONTH_FORMATTER) + " at " + endTime.format(TIME_ONLY_FORMATTER);
    }
}