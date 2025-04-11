package kostovite;

import java.time.Instant;
import java.time.LocalDateTime;
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
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "World clock and time zone converter"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        // This is separate from the uiConfig, which describes how the UI should look.
        Map<String, Object> operations = new HashMap<>();
        // Get current time operation
        Map<String, Object> getCurrentTimeOperation = new HashMap<>();
        getCurrentTimeOperation.put("description", "Get current time in different time zones");
        Map<String, Object> getCurrentTimeInputs = new HashMap<>();
        getCurrentTimeInputs.put("filter", Map.of("type", "string", "description", "Filter time zones by region or keyword (optional)"));
        getCurrentTimeInputs.put("limit", Map.of("type", "integer", "description", "Limit the number of time zones returned (optional)"));
        getCurrentTimeOperation.put("inputs", getCurrentTimeInputs);
        operations.put("getCurrentTime", getCurrentTimeOperation); // Use the actual operation name

        // Convert time operation
        Map<String, Object> convertTimeOperation = new HashMap<>();
        convertTimeOperation.put("description", "Convert time between time zones");
        Map<String, Object> convertTimeInputs = new HashMap<>();
        convertTimeInputs.put("dateTime", Map.of("type", "string", "description", "Date and time to convert (format: yyyy-MM-dd HH:mm:ss)", "required", true));
        convertTimeInputs.put("sourceTimeZone", Map.of("type", "string", "description", "Source time zone ID", "required", true));
        convertTimeInputs.put("targetTimeZone", Map.of("type", "string", "description", "Target time zone ID (optional, converts to all zones if not specified)"));
        convertTimeOperation.put("inputs", convertTimeInputs);
        operations.put("convertTime", convertTimeOperation); // Use the actual operation name

        // List time zones operation
        Map<String, Object> listTimeZonesOperation = new HashMap<>();
        listTimeZonesOperation.put("description", "List available time zones");
        Map<String, Object> listTimeZonesInputs = new HashMap<>();
        listTimeZonesInputs.put("filter", Map.of("type", "string", "description", "Filter time zones by region or keyword (optional)"));
        listTimeZonesOperation.put("inputs", listTimeZonesInputs);
        operations.put("listTimeZones", listTimeZonesOperation); // Use the actual operation name

        // Get time zone details operation
        Map<String, Object> getTimeZoneDetailsOperation = new HashMap<>();
        getTimeZoneDetailsOperation.put("description", "Get details about a specific time zone");
        Map<String, Object> getTimeZoneDetailsInputs = new HashMap<>();
        getTimeZoneDetailsInputs.put("timeZone", Map.of("type", "string", "description", "Time zone ID", "required", true));
        getTimeZoneDetailsOperation.put("inputs", getTimeZoneDetailsInputs);
        operations.put("getTimeZoneDetails", getTimeZoneDetailsOperation); // Use the actual operation name
        // --- End of operations definitions ---
        metadata.put("operations", operations); // Keep this for backend/API reference


        // --- Define UI Configuration (matches the HashTools structure) ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "WorldClock"); // Corresponds to ToolMetadata.id
        // uiConfig.put("name", "World Clock"); // Can be derived from metadata.name by FE
        uiConfig.put("icon", "Public"); // Corresponds to ToolMetadata.icon (e.g., Material Icon name)
        uiConfig.put("category", "Utilities"); // Corresponds to ToolMetadata.category
        // uiConfig.put("description", "..."); // Can be derived from metadata.description by FE

        // --- Define UI Inputs ---
        // This list corresponds to uiConfig.inputs in the TypeScript example
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Common Controls
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "World Clock Operation"); // Section header
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "uiOperation"); // Name used in form state/submission
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        // Use value/label pairs for options
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "convertTime", "label", "Convert Time"));
        operationOptions.add(Map.of("value", "getCurrentTime", "label", "Show Current Time"));
        operationOptions.add(Map.of("value", "getTimeZoneDetails", "label", "Get Time Zone Details"));
        operationField.put("options", operationOptions);
        operationField.put("default", "convertTime");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields); // Add fields to the section
        uiInputs.add(inputSection1); // Add section to the inputs list

        // Input Section 2: Conditional Parameters based on Operation
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Operation Parameters"); // Section header
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // --- Fields for "Convert Time" ---
        Map<String, Object> dateTimeField = new HashMap<>();
        dateTimeField.put("name", "dateTime");
        dateTimeField.put("label", "Date and Time (YYYY-MM-DD HH:MM:SS):");
        dateTimeField.put("type", "text");
        try {
            // Requires DEFAULT_DATETIME_FORMAT and ZonedDateTime
            dateTimeField.put("default", ZonedDateTime.now().format(DEFAULT_DATETIME_FORMAT));
        } catch (Exception e) {
            // Fallback if ZonedDateTime fails (e.g., in restricted environment)
            dateTimeField.put("default", "2025-01-01 12:00:00");
        }
        dateTimeField.put("required", true); // Mark as required for this operation
        // Condition for frontend to show/hide this field
        dateTimeField.put("condition", "uiOperation === 'convertTime'");
        section2Fields.add(dateTimeField);

        Map<String, Object> sourceTimeZoneField = new HashMap<>();
        sourceTimeZoneField.put("name", "sourceTimeZone");
        sourceTimeZoneField.put("label", "Source Time Zone (e.g., UTC, America/New_York):");
        sourceTimeZoneField.put("type", "text"); // Consider "timezone" type if FE supports it
        sourceTimeZoneField.put("default", "UTC");
        sourceTimeZoneField.put("required", true);
        sourceTimeZoneField.put("condition", "uiOperation === 'convertTime'");
        section2Fields.add(sourceTimeZoneField);

        Map<String, Object> targetTimeZoneField = new HashMap<>();
        targetTimeZoneField.put("name", "targetTimeZone");
        targetTimeZoneField.put("label", "Target Time Zone (Optional, blank for all):");
        targetTimeZoneField.put("type", "text"); // Consider "timezone" type
        targetTimeZoneField.put("default", "America/New_York");
        targetTimeZoneField.put("required", false); // Optional field
        targetTimeZoneField.put("condition", "uiOperation === 'convertTime'");
        section2Fields.add(targetTimeZoneField);

        // --- Fields for "Show Current Time" ---
        Map<String, Object> filterField = new HashMap<>();
        filterField.put("name", "filter");
        filterField.put("label", "Filter Time Zones (region/keyword, optional):");
        filterField.put("type", "text");
        filterField.put("default", "");
        filterField.put("required", false);
        filterField.put("condition", "uiOperation === 'getCurrentTime'");
        section2Fields.add(filterField);

        Map<String, Object> limitField = new HashMap<>();
        limitField.put("name", "limit");
        limitField.put("label", "Max Zones to Show (optional):");
        limitField.put("type", "number");
        limitField.put("default", 10);
        limitField.put("min", 1);
        limitField.put("max", 100); // Provide reasonable limits
        limitField.put("required", false);
        limitField.put("condition", "uiOperation === 'getCurrentTime'");
        section2Fields.add(limitField);

        // --- Fields for "Get Time Zone Details" ---
        Map<String, Object> timeZoneField = new HashMap<>();
        timeZoneField.put("name", "timeZone");
        timeZoneField.put("label", "Time Zone (e.g., Europe/London):");
        timeZoneField.put("type", "text"); // Consider "timezone" type
        timeZoneField.put("default", "Europe/London");
        timeZoneField.put("required", true);
        timeZoneField.put("condition", "uiOperation === 'getTimeZoneDetails'");
        section2Fields.add(timeZoneField);

        inputSection2.put("fields", section2Fields); // Add fields to the section
        uiInputs.add(inputSection2); // Add section to the inputs list


        uiConfig.put("inputs", uiInputs); // Assign the complete inputs structure

        // --- Define UI Outputs ---
        // This list corresponds to uiConfig.outputs in the TypeScript example
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Conversion Result (Conditional)
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Conversion Result"); // Section header
        // Condition for frontend to show/hide the entire section
        outputSection1.put("condition", "uiOperation === 'convertTime'");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Source Time (Always shown for conversion)
        Map<String, Object> sourceOutput = new HashMap<>();
        sourceOutput.put("title", "Source Time"); // Label for the output field
        sourceOutput.put("name", "sourceDateTime"); // Key in the process() result map
        sourceOutput.put("type", "text");
        section1OutputFields.add(sourceOutput);

        // Target Time (Only shown if a specific target zone was requested)
        Map<String, Object> targetTimeOutput = new HashMap<>();
        targetTimeOutput.put("title", "Target Time");
        // Use dot notation if the result is nested, e.g., result.targetDateTime.formattedTime
        targetTimeOutput.put("name", "targetDateTime.formattedTime");
        targetTimeOutput.put("type", "text");
        targetTimeOutput.put("buttons", List.of("copy")); // Add a copy button
        // Condition for frontend to show/hide this specific field
        targetTimeOutput.put("condition", "targetTimeZone"); // Show if targetTimeZone input was non-empty
        section1OutputFields.add(targetTimeOutput);

        // Target Offset (Only shown for specific target)
        Map<String, Object> targetOffsetOutput = new HashMap<>();
        targetOffsetOutput.put("title", "Target Offset");
        targetOffsetOutput.put("name", "targetDateTime.offset");
        targetOffsetOutput.put("type", "text");
        targetOffsetOutput.put("condition", "targetTimeZone");
        section1OutputFields.add(targetOffsetOutput);

        // Target DST (Only shown for specific target)
        Map<String, Object> targetDstOutput = new HashMap<>();
        targetDstOutput.put("title", "Target DST Active");
        targetDstOutput.put("name", "targetDateTime.isDaylightSavingTime");
        targetDstOutput.put("type", "text"); // Use text for boolean display flexibility
        targetDstOutput.put("condition", "targetTimeZone");
        section1OutputFields.add(targetDstOutput);

        // Table for All Time Zone Conversions (Shown if targetTimeZone was blank)
        Map<String, Object> allConversionsOutput = new HashMap<>();
        allConversionsOutput.put("title", "All Time Zone Conversions"); // Label for the table
        allConversionsOutput.put("name", "conversions"); // Key for the list in process() result
        allConversionsOutput.put("type", "table");
        // Condition: show only if converting time AND targetTimeZone input was empty
        allConversionsOutput.put("condition", "uiOperation === 'convertTime' && !targetTimeZone");
        List<Map<String, Object>> conversionColumns = new ArrayList<>();
        conversionColumns.add(Map.of("header", "Zone", "field", "zoneName")); // Table column definitions
        conversionColumns.add(Map.of("header", "Time", "field", "formattedTime"));
        conversionColumns.add(Map.of("header", "Offset", "field", "offset"));
        conversionColumns.add(Map.of("header", "DST", "field", "isDaylightSavingTime"));
        allConversionsOutput.put("columns", conversionColumns);
        section1OutputFields.add(allConversionsOutput); // Add the table definition as a field

        outputSection1.put("fields", section1OutputFields); // Add fields to the section
        uiOutputs.add(outputSection1); // Add section to the outputs list


        // Output Section 2: Current Time Zones (Conditional)
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Current Time Zones");
        outputSection2.put("condition", "uiOperation === 'getCurrentTime'");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Table for current times
        Map<String, Object> timeZonesTableOutput = new HashMap<>();
        // title is optional if header is sufficient
        timeZonesTableOutput.put("name", "timeZones"); // Key for the list in process() result
        timeZonesTableOutput.put("type", "table");
        List<Map<String, Object>> currentTimeColumns = new ArrayList<>();
        currentTimeColumns.add(Map.of("header", "Zone", "field", "zoneName"));
        currentTimeColumns.add(Map.of("header", "Time", "field", "formattedTime"));
        currentTimeColumns.add(Map.of("header", "Offset", "field", "offset"));
        currentTimeColumns.add(Map.of("header", "DST", "field", "isDaylightSavingTime"));
        timeZonesTableOutput.put("columns", currentTimeColumns);
        section2OutputFields.add(timeZonesTableOutput); // Add table definition as a field

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);


        // Output Section 3: Time Zone Details (Conditional)
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Time Zone Details");
        outputSection3.put("condition", "uiOperation === 'getTimeZoneDetails'");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Individual detail fields
        section3OutputFields.add(Map.of("title", "Zone ID", "name", "id", "type", "text"));
        section3OutputFields.add(Map.of("title", "Display Name", "name", "displayName", "type", "text"));
        section3OutputFields.add(Map.of("title", "Current Time", "name", "currentTime", "type", "text", "buttons", List.of("copy")));
        section3OutputFields.add(Map.of("title", "Current Offset", "name", "currentOffset", "type", "text"));
        section3OutputFields.add(Map.of("title", "Standard Offset", "name", "standardOffset", "type", "text"));
        section3OutputFields.add(Map.of("title", "DST Active Now", "name", "isDaylightSavingsActive", "type", "text")); // Or boolean

        // Fields for nested transition data - display as JSON or specific component
        section3OutputFields.add(Map.of("title", "Next DST Transition", "name", "nextTransition", "type", "json"));
        section3OutputFields.add(Map.of("title", "Previous DST Transition", "name", "previousTransition", "type", "json"));

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);


        uiConfig.put("outputs", uiOutputs); // Assign the complete outputs structure

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

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