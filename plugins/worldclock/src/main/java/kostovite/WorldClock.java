package kostovite;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class WorldClock implements PluginInterface {

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ISO_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    @Override
    public String getName() {
        return "WorldClock";
    }

    @Override
    public void execute() {
        System.out.println("World Clock Plugin executed");

        // Demonstrate basic usage
        try {
            // Get current time in different time zones
            Map<String, Object> params = new HashMap<>();
            params.put("operation", "getCurrentTime");

            Map<String, Object> result = process(params);
            System.out.println("Current time in UTC: " + result.get("utcTime"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> timeZones = (List<Map<String, Object>>) result.get("timeZones");
            if (timeZones != null && !timeZones.isEmpty()) {
                System.out.println("Sample time zone: " + timeZones.get(0).get("zoneName") +
                        " - " + timeZones.get(0).get("formattedTime"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "World clock and time zone converter");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Get current time operation
        Map<String, Object> getCurrentTimeOperation = new HashMap<>();
        getCurrentTimeOperation.put("description", "Get current time in different time zones");

        Map<String, Object> getCurrentTimeInputs = new HashMap<>();
        getCurrentTimeInputs.put("filter", "Filter time zones by region or keyword (optional)");
        getCurrentTimeInputs.put("limit", "Limit the number of time zones returned (optional)");

        getCurrentTimeOperation.put("inputs", getCurrentTimeInputs);
        operations.put("getCurrentTime", getCurrentTimeOperation);

        // Convert time operation
        Map<String, Object> convertTimeOperation = new HashMap<>();
        convertTimeOperation.put("description", "Convert time between time zones");

        Map<String, Object> convertTimeInputs = new HashMap<>();
        convertTimeInputs.put("dateTime", "Date and time to convert (format: yyyy-MM-dd HH:mm:ss)");
        convertTimeInputs.put("sourceTimeZone", "Source time zone ID");
        convertTimeInputs.put("targetTimeZone", "Target time zone ID (optional, converts to all zones if not specified)");

        convertTimeOperation.put("inputs", convertTimeInputs);
        operations.put("convertTime", convertTimeOperation);

        // List time zones operation
        Map<String, Object> listTimeZonesOperation = new HashMap<>();
        listTimeZonesOperation.put("description", "List available time zones");

        Map<String, Object> listTimeZonesInputs = new HashMap<>();
        listTimeZonesInputs.put("filter", "Filter time zones by region or keyword (optional)");

        listTimeZonesOperation.put("inputs", listTimeZonesInputs);
        operations.put("listTimeZones", listTimeZonesOperation);

        // Get time zone details operation
        Map<String, Object> getTimeZoneDetailsOperation = new HashMap<>();
        getTimeZoneDetailsOperation.put("description", "Get details about a specific time zone");

        Map<String, Object> getTimeZoneDetailsInputs = new HashMap<>();
        getTimeZoneDetailsInputs.put("timeZone", "Time zone ID");

        getTimeZoneDetailsOperation.put("inputs", getTimeZoneDetailsInputs);
        operations.put("getTimeZoneDetails", getTimeZoneDetailsOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "getCurrentTime");

            switch (operation.toLowerCase()) {
                case "getcurrenttime":
                    return getCurrentTime(input);
                case "converttime":
                    return convertTime(input);
                case "listtimezones":
                    return listTimeZones(input);
                case "gettimezonedetails":
                    return getTimeZoneDetails(input);
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
     * Get the current time in different time zones
     *
     * @param input Input parameters
     * @return Current time information
     */
    private Map<String, Object> getCurrentTime(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String filter = (String) input.getOrDefault("filter", "");
            int limit = input.containsKey("limit") ?
                    Integer.parseInt(input.get("limit").toString()) : Integer.MAX_VALUE;

            // Get current time in UTC
            ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);

            // Format UTC time
            String utcTime = utcNow.format(DEFAULT_DATETIME_FORMAT);

            result.put("success", true);
            result.put("utcTime", utcTime);
            result.put("utcIsoTime", utcNow.format(ISO_DATETIME_FORMAT));
            result.put("utcTimestamp", utcNow.toEpochSecond());

            // Get all available time zones
            List<Map<String, Object>> timeZones = new ArrayList<>();

            Set<String> zoneIds = ZoneId.getAvailableZoneIds();

            // Apply filter if provided
            if (filter != null && !filter.isEmpty()) {
                final String filterLower = filter.toLowerCase();
                zoneIds = zoneIds.stream()
                        .filter(zoneId -> zoneId.toLowerCase().contains(filterLower))
                        .collect(Collectors.toSet());
            }

            // Convert time to each zone
            for (String zoneId : new TreeSet<>(zoneIds)) {
                if (timeZones.size() >= limit) {
                    break;
                }

                try {
                    ZoneId zone = ZoneId.of(zoneId);
                    ZonedDateTime zonedTime = utcNow.withZoneSameInstant(zone);

                    // Get zone details
                    ZoneRules rules = zone.getRules();
                    boolean isDst = rules.isDaylightSavings(zonedTime.toInstant());
                    ZoneOffset offset = rules.getOffset(zonedTime.toInstant());
                    String offsetString = formatOffset(offset);

                    Map<String, Object> timeZoneInfo = new HashMap<>();
                    timeZoneInfo.put("zoneName", zone.getId());
                    timeZoneInfo.put("formattedTime", zonedTime.format(DISPLAY_DATETIME_FORMAT));
                    timeZoneInfo.put("formattedDate", zonedTime.format(DATE_FORMAT));
                    timeZoneInfo.put("formattedDay", zonedTime.format(DAY_FORMAT));
                    timeZoneInfo.put("formattedTimeOnly", zonedTime.format(TIME_FORMAT));
                    timeZoneInfo.put("offset", offsetString);
                    timeZoneInfo.put("offsetTotalSeconds", offset.getTotalSeconds());
                    timeZoneInfo.put("isDaylightSavingTime", isDst);
                    timeZoneInfo.put("isDaylightSavingActive", rules.isDaylightSavings(Instant.now()));

                    timeZones.add(timeZoneInfo);
                } catch (Exception e) {
                    // Skip invalid time zones
                }
            }

            // Sort time zones by offset
            Collections.sort(timeZones, Comparator.comparingInt(tz -> (Integer)tz.get("offsetTotalSeconds")));

            result.put("timeZones", timeZones);
            result.put("count", timeZones.size());

        } catch (Exception e) {
            result.put("error", "Error getting current time: " + e.getMessage());
        }

        return result;
    }

    /**
     * Convert time between time zones
     *
     * @param input Input parameters
     * @return Converted time information
     */
    private Map<String, Object> convertTime(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String dateTimeStr = (String) input.get("dateTime");
            String sourceTimeZoneId = (String) input.get("sourceTimeZone");
            String targetTimeZoneId = (String) input.get("targetTimeZone");

            if (dateTimeStr == null || sourceTimeZoneId == null) {
                result.put("error", "DateTime and sourceTimeZone are required");
                return result;
            }

            // Parse the input date-time
            LocalDateTime localDateTime;
            try {
                localDateTime = LocalDateTime.parse(dateTimeStr, DEFAULT_DATETIME_FORMAT);
            } catch (DateTimeParseException e) {
                result.put("error", "Invalid date-time format. Use yyyy-MM-dd HH:mm:ss");
                return result;
            }

            // Parse the source time zone
            ZoneId sourceZone;
            try {
                sourceZone = ZoneId.of(sourceTimeZoneId);
            } catch (Exception e) {
                result.put("error", "Invalid source time zone: " + sourceTimeZoneId);
                return result;
            }

            // Create a ZonedDateTime using the source time zone
            ZonedDateTime sourceZonedDateTime = ZonedDateTime.of(localDateTime, sourceZone);

            result.put("success", true);
            result.put("sourceDateTime", dateTimeStr);
            result.put("sourceTimeZone", sourceTimeZoneId);
            result.put("sourceOffset", formatOffset(sourceZone.getRules().getOffset(sourceZonedDateTime.toInstant())));
            result.put("sourceIsDST", sourceZone.getRules().isDaylightSavings(sourceZonedDateTime.toInstant()));

            // If a target time zone is specified, only convert to that
            if (targetTimeZoneId != null && !targetTimeZoneId.isEmpty()) {
                try {
                    ZoneId targetZone = ZoneId.of(targetTimeZoneId);
                    ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);

                    Map<String, Object> targetInfo = new HashMap<>();
                    targetInfo.put("zoneName", targetZone.getId());
                    targetInfo.put("formattedTime", targetZonedDateTime.format(DISPLAY_DATETIME_FORMAT));
                    targetInfo.put("formattedDate", targetZonedDateTime.format(DATE_FORMAT));
                    targetInfo.put("formattedDay", targetZonedDateTime.format(DAY_FORMAT));
                    targetInfo.put("formattedTimeOnly", targetZonedDateTime.format(TIME_FORMAT));
                    targetInfo.put("offset", formatOffset(targetZone.getRules().getOffset(targetZonedDateTime.toInstant())));
                    targetInfo.put("isDaylightSavingTime", targetZone.getRules().isDaylightSavings(targetZonedDateTime.toInstant()));

                    result.put("targetTimeZone", targetTimeZoneId);
                    result.put("targetDateTime", targetInfo);

                } catch (Exception e) {
                    result.put("error", "Invalid target time zone: " + targetTimeZoneId);
                    return result;
                }
            }
            // Otherwise, convert to all time zones
            else {
                List<Map<String, Object>> conversions = new ArrayList<>();

                for (String zoneId : new TreeSet<>(ZoneId.getAvailableZoneIds())) {
                    try {
                        ZoneId targetZone = ZoneId.of(zoneId);
                        ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);

                        ZoneRules rules = targetZone.getRules();
                        boolean isDst = rules.isDaylightSavings(targetZonedDateTime.toInstant());
                        ZoneOffset offset = rules.getOffset(targetZonedDateTime.toInstant());
                        String offsetString = formatOffset(offset);

                        Map<String, Object> conversionInfo = new HashMap<>();
                        conversionInfo.put("zoneName", targetZone.getId());
                        conversionInfo.put("formattedTime", targetZonedDateTime.format(DISPLAY_DATETIME_FORMAT));
                        conversionInfo.put("formattedDate", targetZonedDateTime.format(DATE_FORMAT));
                        conversionInfo.put("formattedDay", targetZonedDateTime.format(DAY_FORMAT));
                        conversionInfo.put("formattedTimeOnly", targetZonedDateTime.format(TIME_FORMAT));
                        conversionInfo.put("offset", offsetString);
                        conversionInfo.put("offsetTotalSeconds", offset.getTotalSeconds());
                        conversionInfo.put("isDaylightSavingTime", isDst);

                        conversions.add(conversionInfo);
                    } catch (Exception e) {
                        // Skip invalid time zones
                    }
                }

                // Sort conversions by offset
                Collections.sort(conversions, Comparator.comparingInt(tz -> (Integer)tz.get("offsetTotalSeconds")));

                result.put("conversions", conversions);
                result.put("count", conversions.size());
            }

        } catch (Exception e) {
            result.put("error", "Error converting time: " + e.getMessage());
        }

        return result;
    }

    /**
     * List available time zones
     *
     * @param input Input parameters
     * @return Time zone information
     */
    private Map<String, Object> listTimeZones(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String filter = (String) input.getOrDefault("filter", "");

            // Get all available time zones
            Set<String> zoneIds = ZoneId.getAvailableZoneIds();

            // Apply filter if provided
            if (filter != null && !filter.isEmpty()) {
                final String filterLower = filter.toLowerCase();
                zoneIds = zoneIds.stream()
                        .filter(zoneId -> zoneId.toLowerCase().contains(filterLower))
                        .collect(Collectors.toSet());
            }

            // Create a sorted list of time zones with details
            List<Map<String, Object>> timeZones = new ArrayList<>();

            for (String zoneId : new TreeSet<>(zoneIds)) {
                try {
                    ZoneId zone = ZoneId.of(zoneId);
                    ZoneRules rules = zone.getRules();

                    // Get current offset
                    Instant now = Instant.now();
                    ZoneOffset offset = rules.getOffset(now);
                    String offsetString = formatOffset(offset);

                    // Check if this zone uses DST
                    // Check with a 6 month interval to detect DST changes
                    Instant winterTime = now.minus(6, ChronoUnit.MONTHS);
                    Instant summerTime = now.plus(6, ChronoUnit.MONTHS);
                    boolean usesDst = rules.isDaylightSavings(winterTime) != rules.isDaylightSavings(summerTime);

                    Map<String, Object> timeZoneInfo = new HashMap<>();
                    timeZoneInfo.put("id", zone.getId());
                    timeZoneInfo.put("displayName", zone.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
                    timeZoneInfo.put("offset", offsetString);
                    timeZoneInfo.put("offsetTotalSeconds", offset.getTotalSeconds());
                    timeZoneInfo.put("usesDaylightSavings", usesDst);
                    timeZoneInfo.put("isDaylightSavingActive", rules.isDaylightSavings(now));

                    // Region information (derived from ID)
                    String[] parts = zone.getId().split("/");
                    timeZoneInfo.put("region", parts.length > 0 ? parts[0] : "");
                    timeZoneInfo.put("city", parts.length > 1 ? parts[parts.length - 1].replace("_", " ") : "");

                    timeZones.add(timeZoneInfo);
                } catch (Exception e) {
                    // Skip invalid time zones
                }
            }

            // Sort time zones by offset
            Collections.sort(timeZones, Comparator.comparingInt(tz -> (Integer)tz.get("offsetTotalSeconds")));

            result.put("success", true);
            result.put("timeZones", timeZones);
            result.put("count", timeZones.size());

        } catch (Exception e) {
            result.put("error", "Error listing time zones: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get details about a specific time zone
     *
     * @param input Input parameters
     * @return Time zone details
     */
    private Map<String, Object> getTimeZoneDetails(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String timeZoneId = (String) input.get("timeZone");

            if (timeZoneId == null || timeZoneId.isEmpty()) {
                result.put("error", "Time zone ID is required");
                return result;
            }

            try {
                ZoneId zone = ZoneId.of(timeZoneId);
                ZoneRules rules = zone.getRules();

                // Current time in this zone
                ZonedDateTime now = ZonedDateTime.now(zone);
                Instant nowInstant = now.toInstant();

                // Current offset
                ZoneOffset currentOffset = rules.getOffset(nowInstant);
                String currentOffsetString = formatOffset(currentOffset);

                // Check if currently in DST
                boolean isCurrentlyDst = rules.isDaylightSavings(nowInstant);

                // Standard offset (non-DST)
                ZoneOffset standardOffset = rules.getStandardOffset(nowInstant);
                String standardOffsetString = formatOffset(standardOffset);

                // Find next DST transition
                java.time.zone.ZoneOffsetTransition nextTransition = rules.nextTransition(nowInstant);
                Map<String, Object> nextTransitionInfo = null;

                if (nextTransition != null) {
                    nextTransitionInfo = new HashMap<>();
                    nextTransitionInfo.put("instant", nextTransition.getInstant().toString());
                    nextTransitionInfo.put("localDateTime", LocalDateTime.ofInstant(
                            nextTransition.getInstant(), zone).format(DEFAULT_DATETIME_FORMAT));
                    nextTransitionInfo.put("offsetBefore", formatOffset(nextTransition.getOffsetBefore()));
                    nextTransitionInfo.put("offsetAfter", formatOffset(nextTransition.getOffsetAfter()));
                    nextTransitionInfo.put("isDaylightSavings", nextTransition.isGap() ? "Start" : "End");
                }

                // Find previous DST transition
                java.time.zone.ZoneOffsetTransition prevTransition = rules.previousTransition(nowInstant);
                Map<String, Object> prevTransitionInfo = null;

                if (prevTransition != null) {
                    prevTransitionInfo = new HashMap<>();
                    prevTransitionInfo.put("instant", prevTransition.getInstant().toString());
                    prevTransitionInfo.put("localDateTime", LocalDateTime.ofInstant(
                            prevTransition.getInstant(), zone).format(DEFAULT_DATETIME_FORMAT));
                    prevTransitionInfo.put("offsetBefore", formatOffset(prevTransition.getOffsetBefore()));
                    prevTransitionInfo.put("offsetAfter", formatOffset(prevTransition.getOffsetAfter()));
                    prevTransitionInfo.put("isDaylightSavings", prevTransition.isGap() ? "Start" : "End");
                }

                // Build the result
                result.put("success", true);
                result.put("id", zone.getId());
                result.put("displayName", zone.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
                result.put("currentTime", now.format(DISPLAY_DATETIME_FORMAT));
                result.put("currentOffset", currentOffsetString);
                result.put("standardOffset", standardOffsetString);
                result.put("isDaylightSavingsActive", isCurrentlyDst);

                if (nextTransitionInfo != null) {
                    result.put("nextTransition", nextTransitionInfo);
                }

                if (prevTransitionInfo != null) {
                    result.put("previousTransition", prevTransitionInfo);
                }

                // Region information (derived from ID)
                String[] parts = zone.getId().split("/");
                result.put("region", parts.length > 0 ? parts[0] : "");
                result.put("city", parts.length > 1 ? parts[parts.length - 1].replace("_", " ") : "");

            } catch (Exception e) {
                result.put("error", "Invalid time zone: " + timeZoneId);
                return result;
            }

        } catch (Exception e) {
            result.put("error", "Error getting time zone details: " + e.getMessage());
        }

        return result;
    }

    /**
     * Format a ZoneOffset to a string (e.g., "+09:00")
     *
     * @param offset ZoneOffset to format
     * @return Formatted offset string
     */
    private String formatOffset(ZoneOffset offset) {
        String result = offset.getId();

        // Handle special case for zero offset
        if (result.equals("Z")) {
            return "+00:00";
        }

        // Ensure format is consistent (e.g. +09:00 not +9:00)
        if (result.length() == 5 && (result.charAt(0) == '+' || result.charAt(0) == '-')) {
            return result.substring(0, 1) + "0" + result.substring(1);
        }

        return result;
    }
}