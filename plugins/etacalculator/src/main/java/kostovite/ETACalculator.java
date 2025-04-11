package kostovite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Calculate estimated time of arrival/completion");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Calculate ETA operation
        Map<String, Object> calculateOperation = new HashMap<>();
        calculateOperation.put("description", "Calculate ETA based on processing rate");

        Map<String, Object> calculateInputs = new HashMap<>();
        calculateInputs.put("totalItems", "Total number of items to process");
        calculateInputs.put("itemsProcessed", "Number of items processed in the sample time period");
        calculateInputs.put("timeElapsed", "Amount of time elapsed for the sample");
        calculateInputs.put("timeUnit", "Time unit for timeElapsed (seconds, minutes, hours)");
        calculateInputs.put("startTime", "Start time in format YYYY-MM-DD HH:MM:SS");
        calculateInputs.put("timezone", "Timezone for calculations (optional, defaults to UTC)");

        calculateOperation.put("inputs", calculateInputs);
        operations.put("calculate", calculateOperation);

        // Calculate progress operation
        Map<String, Object> progressOperation = new HashMap<>();
        progressOperation.put("description", "Calculate progress information for an ongoing task");

        Map<String, Object> progressInputs = new HashMap<>();
        progressInputs.put("totalItems", "Total number of items to process");
        progressInputs.put("itemsCompleted", "Number of items already processed");
        progressInputs.put("startTime", "Start time in format YYYY-MM-DD HH:MM:SS");
        progressInputs.put("timezone", "Timezone for calculations (optional, defaults to UTC)");

        progressOperation.put("inputs", progressInputs);
        operations.put("progress", progressOperation);

        metadata.put("operations", operations);
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