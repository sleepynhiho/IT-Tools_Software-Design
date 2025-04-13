package kostovite;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneOffsetTransition; // Import specific class
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap; // Use TreeMap for sorted map
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Locale; // Import Locale

// Assuming PluginInterface is standard
public class WorldClock implements PluginInterface {

    // Common formatters
    private static final DateTimeFormatter INPUT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    // private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Included in DISPLAY_DATETIME_FORMAT
    private static final DateTimeFormatter DISPLAY_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    // Cache for country display names
    private static final Map<String, String> COUNTRY_DISPLAY_NAMES_CACHE = new TreeMap<>(); // TreeMap for sorted keys
    static {
        for (String regionCode : ZoneId.getAvailableZoneIds()) { // Iterate zones to get potential region codes
            if (regionCode.contains("/") && !regionCode.toLowerCase().startsWith("etc/")) {
                String code = regionCode.substring(regionCode.lastIndexOf('/') + 1).replace('_', ' ');
                // Simple heuristic to map Zone ID city to potential country code (not always accurate)
                // A better approach might involve a dedicated mapping library if needed.
                // For UI options, we primarily need the Zone ID itself.
                // This cache isn't strictly necessary for the refactored metadata.
            }
            // Add Zone ID itself for display/selection
            if (!regionCode.toLowerCase().startsWith("etc/") && regionCode.contains("/")) { // Filter out Etc/* and non-regional zones
                COUNTRY_DISPLAY_NAMES_CACHE.put(regionCode, regionCode.replace('_', ' ')); // Use ZoneID as key and formatted name as value
            }
        }
        // Add UTC explicitly
        COUNTRY_DISPLAY_NAMES_CACHE.put("UTC", "UTC (Coordinated Universal Time)");
        COUNTRY_DISPLAY_NAMES_CACHE.put("GMT", "GMT (Greenwich Mean Time)");
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "WorldClock";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("World Clock Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uiOperation", "convertTime"); // Use new ID
            params.put("dateTimeInput", "2024-07-15 10:00:00"); // Use new ID
            params.put("sourceTimeZoneInput", "America/New_York"); // Use new ID
            params.put("targetTimeZoneInput", "Asia/Tokyo"); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Convert Time Result: " + result);

            params.clear();
            params.put("uiOperation", "getCurrentTime");
            params.put("filter", "Europe");
            result = process(params);
            System.out.println("Get Current Time Result (Europe): ");
            if(result.get("success") == Boolean.TRUE && result.containsKey("timeZoneTable")){
                System.out.println(((List<?>)result.get("timeZoneTable")).size() + " zones found.");
                // System.out.println(result.get("timeZoneTable")); // Print first few maybe
            } else {
                System.out.println(result);
            }


            params.clear();
            params.put("uiOperation", "getTimeZoneDetails");
            params.put("timeZoneDetailInput", "Australia/Sydney"); // Use new ID
            result = process(params);
            System.out.println("Get Time Zone Details Result: " + result);


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
        metadata.put("id", "WorldClock"); // ID matches class name
        metadata.put("name", "World Clock & Time Converter"); // User-facing name
        metadata.put("description", "View current time across different zones or convert a specific time.");
        metadata.put("icon", "Public"); // Material Icon name
        metadata.put("category", "Utilities");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Operation Selection ---
        Map<String, Object> operationSection = new HashMap<>();
        operationSection.put("id", "operationSelection");
        operationSection.put("label", "Select Action");

        List<Map<String, Object>> operationInputs = new ArrayList<>();
        operationInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "convertTime", "label", "Convert Specific Time"),
                        Map.of("value", "getCurrentTime", "label", "Show Current Times"),
                        Map.of("value", "getTimeZoneDetails", "label", "Get Time Zone Details")
                        // Removed listTimeZones as getCurrentTime can filter
                )),
                Map.entry("default", "convertTime"),
                Map.entry("required", true)
        ));
        operationSection.put("inputs", operationInputs);
        sections.add(operationSection);


        // --- Section 2: Input Parameters (Conditional) ---
        Map<String, Object> paramsSection = new HashMap<>();
        paramsSection.put("id", "parameters");
        paramsSection.put("label", "Input Parameters");

        List<Map<String, Object>> paramInputs = new ArrayList<>();

        // == Inputs for Convert Time ==
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "dateTimeInput"),
                Map.entry("label", "Date & Time to Convert:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "YYYY-MM-DD HH:MM:SS"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'convertTime'")
        ));
        paramInputs.add(createZoneIdInput("sourceTimeZoneInput", "From Time Zone:", "convertTime", "UTC", true));
        paramInputs.add(createZoneIdInput("targetTimeZoneInput", "To Time Zone (Optional):", "convertTime", ZoneId.systemDefault().getId(), false)); // Optional


        // == Inputs for Get Current Time ==
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "filter"),
                Map.entry("label", "Filter Zones (Optional):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe, London, EST, GMT"),
                Map.entry("required", false),
                Map.entry("condition", "uiOperation === 'getCurrentTime'"),
                Map.entry("helperText", "Enter region, city, or abbreviation.")
        ));
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "limit"),
                Map.entry("label", "Max Zones (Optional):"),
                Map.entry("type", "number"),
                Map.entry("default", 50), // Default limit
                Map.entry("min", 1),
                Map.entry("max", 500), // Max limit
                Map.entry("required", false),
                Map.entry("condition", "uiOperation === 'getCurrentTime'")
        ));

        // == Inputs for Get Zone Details ==
        paramInputs.add(createZoneIdInput("timeZoneDetailInput", "Time Zone:", "getTimeZoneDetails", "Europe/London", true));


        paramsSection.put("inputs", paramInputs);
        sections.add(paramsSection);


        // --- Section 3: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Output");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // == Outputs for Convert Time ==
        resultOutputs.add(createOutputField("convert_sourceInfo", "Source Time", "text", "uiOperation === 'convertTime'"));
        resultOutputs.add(createOutputField("convert_targetInfo", "Target Time", "text", "uiOperation === 'convertTime' && typeof convert_targetInfo !== 'undefined'")); // Specific target
        resultOutputs.add(createOutputField("convert_targetOffset", "Target Offset", "text", "uiOperation === 'convertTime' && typeof convert_targetInfo !== 'undefined'"));
        resultOutputs.add(createOutputField("convert_targetIsDST", "Target DST", "boolean", "uiOperation === 'convertTime' && typeof convert_targetInfo !== 'undefined'"));

        // Table for all conversions (when target is empty)
        Map<String, Object> allConversionsTable = createOutputField("convert_allZonesTable", "All Zone Conversions", "table", "uiOperation === 'convertTime' && typeof convert_allZonesTable !== 'undefined'");
        allConversionsTable.put("columns", List.of(
                Map.of("header", "Zone ID", "field", "zoneName"),
                Map.of("header", "Converted Time", "field", "formattedTime"),
                Map.of("header", "Offset", "field", "offset"),
                Map.of("header", "DST Active", "field", "isDST")
        ));
        resultOutputs.add(allConversionsTable);


        // == Outputs for Get Current Time ==
        resultOutputs.add(createOutputField("currentTime_utc", "Current UTC Time", "text", "uiOperation === 'getCurrentTime'"));
        // Table for time zones
        Map<String, Object> currentTimeTable = createOutputField("timeZoneTable", "Current Times by Zone", "table", "uiOperation === 'getCurrentTime' && typeof timeZoneTable !== 'undefined'");
        currentTimeTable.put("columns", List.of(
                Map.of("header", "Zone ID", "field", "zoneName"),
                Map.of("header", "Current Time", "field", "formattedTime"),
                Map.of("header", "Offset", "field", "offset"),
                Map.of("header", "DST Active", "field", "isDST")
        ));
        resultOutputs.add(currentTimeTable);


        // == Outputs for Get Time Zone Details ==
        resultOutputs.add(createOutputField("details_id", "Zone ID", "text", "uiOperation === 'getTimeZoneDetails'"));
        resultOutputs.add(createOutputField("details_displayName", "Display Name", "text", "uiOperation === 'getTimeZoneDetails'"));
        resultOutputs.add(createOutputField("details_currentTime", "Current Time", "text", "uiOperation === 'getTimeZoneDetails'"));
        resultOutputs.add(createOutputField("details_currentOffset", "Current Offset", "text", "uiOperation === 'getTimeZoneDetails'"));
        resultOutputs.add(createOutputField("details_standardOffset", "Standard Offset", "text", "uiOperation === 'getTimeZoneDetails'"));
        resultOutputs.add(createOutputField("details_isDSTActive", "DST Currently Active", "boolean", "uiOperation === 'getTimeZoneDetails'"));
        // Transition info might be complex, show as JSON or separate text fields
        resultOutputs.add(createOutputField("details_nextTransition", "Next DST Transition", "json", "uiOperation === 'getTimeZoneDetails' && typeof details_nextTransition !== 'undefined'"));
        resultOutputs.add(createOutputField("details_prevTransition", "Previous DST Transition", "json", "uiOperation === 'getTimeZoneDetails' && typeof details_prevTransition !== 'undefined'"));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 4: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        errorOutputs.add(createOutputField("errorType", "Error Type", "text", "typeof errorType !== 'undefined'")); // Specific parse error type


        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create Time Zone input fields (Text or Select)
    private Map<String, Object> createZoneIdInput(String id, String label, String condition, String defaultValue, boolean required) {
        // For simplicity, using text input. A select with all zones can be very long.
        // Frontend could potentially implement an autocomplete select.
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("label", label),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe/Paris, America/Los_Angeles, UTC"),
                Map.entry("required", required),
                Map.entry("condition", condition),
                Map.entry("default", defaultValue)
        );
    }

    // Helper to create output field definitions
    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", type);
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error");
        }
        if ("text".equals(type) && (id.toLowerCase().contains("time") || id.toLowerCase().contains("offset") || id.toLowerCase().contains("date"))) {
            field.put("monospace", true); // Monospace for times/offsets
            field.put("buttons", List.of("copy")); // Add copy button
        }
        if ("json".equals(type)) {
            field.put("buttons", List.of("copy"));
        }
        // Handle boolean type for frontend checkmark rendering
        // Handle table type (requires columns definition added where used)
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format).
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String uiOperation = getStringParam(input, "uiOperation", null); // Operation is required
        String errorOutputId = "errorMessage";

        Map<String, Object> processingInput = new HashMap<>(input);

        try {
            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "getcurrenttime" -> result = getCurrentTime(processingInput);
                case "converttime" -> result = convertTime(processingInput);
                // case "listtimezones" -> result = listTimeZones(processingInput); // Merged into getCurrentTime with filter
                case "gettimezonedetails" -> result = getTimeZoneDetails(processingInput);
                default -> {
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
                }
            }

            // Add uiOperation to success result for context
            if (result.get("success") == Boolean.TRUE) {
                Map<String, Object> finalResult = new HashMap<>(result);
                finalResult.put("uiOperation", uiOperation);
                return finalResult;
            } else {
                // Ensure error key consistency
                if (result.containsKey("error") && !result.containsKey(errorOutputId)) {
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put(errorOutputId, result.get("error"));
                    finalResult.remove("error");
                    // Add errorType if present
                    if(result.containsKey("errorType")) {
                        finalResult.put("errorType", result.get("errorType"));
                        finalResult.remove("errorType");
                    }
                    return finalResult;
                }
                return result;
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (DateTimeParseException e) {
            return Map.of("success", false, errorOutputId, "Invalid date/time format. Use YYYY-MM-DD HH:MM:SS.");
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing WorldClock request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Action Methods (Updated for new IDs and result keys)
    // ========================================================================

    /** Get current time in specified/filtered time zones. */
    private Map<String, Object> getCurrentTime(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            String filter = getStringParam(input, "filter", ""); // Optional filter
            int limit = getIntParam(input); // Default limit, use Integer

            ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
            String utcTimeFormatted = utcNow.format(DISPLAY_DATETIME_FORMAT);

            result.put("success", true);
            result.put("currentTime_utc", utcTimeFormatted + " UTC"); // Matches output ID

            List<Map<String, Object>> timeZoneList = new ArrayList<>();
            Set<String> zoneIds = ZoneId.getAvailableZoneIds();
            final String filterLower = filter.toLowerCase();

            // Filter zones
            Set<String> filteredZoneIds = zoneIds.stream()
                    .filter(zoneId -> !zoneId.toLowerCase().startsWith("etc/")) // Exclude Etc/*
                    .filter(zoneId -> filter.isEmpty() || zoneId.toLowerCase().contains(filterLower) || getZoneDescription(zoneId).toLowerCase().contains(filterLower))
                    .collect(Collectors.toSet());


            // Process and collect zones
            for (String zoneId : new TreeSet<>(filteredZoneIds)) { // Sort alphabetically
                if (timeZoneList.size() >= limit) break;
                try {
                    ZoneId zone = ZoneId.of(zoneId);
                    ZonedDateTime zonedTime = utcNow.withZoneSameInstant(zone);
                    ZoneRules rules = zone.getRules();
                    Instant nowInstant = zonedTime.toInstant(); // Use consistent instant
                    boolean isDst = rules.isDaylightSavings(nowInstant);
                    ZoneOffset offset = rules.getOffset(nowInstant);

                    timeZoneList.add(Map.of(
                            "zoneName", zone.getId(), // Field for table column
                            "formattedTime", zonedTime.format(DISPLAY_DATETIME_FORMAT), // Field for table
                            "offset", formatOffset(offset), // Field for table
                            "isDST", isDst, // Field for table (boolean)
                            "offsetSeconds", offset.getTotalSeconds() // For sorting
                    ));
                } catch (Exception e) { /* Skip invalid zones */ }
            }

            // Sort by offset, then by name
            timeZoneList.sort(Comparator.<Map<String, Object>, Integer>comparing(tz -> (Integer)tz.get("offsetSeconds"))
                    .thenComparing(tz -> (String)tz.get("zoneName")));

            result.put("timeZoneTable", timeZoneList); // Matches output ID

        } catch (Exception e) {
            System.err.println("Error in getCurrentTime: " + e.getMessage());
            result.put("success", false); result.put(errorOutputId, "Error retrieving current times.");
        }
        return result;
    }


    /** Convert time between time zones. */
    private Map<String, Object> convertTime(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            String dateTimeStr = getStringParam(input, "dateTimeInput", null); // Required
            String sourceTimeZoneId = getStringParam(input, "sourceTimeZoneInput", null); // Required
            String targetTimeZoneId = getStringParam(input, "targetTimeZoneInput", ""); // Optional

            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, INPUT_DATETIME_FORMAT);
            ZoneId sourceZone = ZoneId.of(sourceTimeZoneId); // Throws if invalid
            ZonedDateTime sourceZonedDateTime = ZonedDateTime.of(localDateTime, sourceZone);

            result.put("success", true);
            result.put("convert_sourceInfo", sourceZonedDateTime.format(DISPLAY_DATETIME_FORMAT) + " " + sourceZone.getId()); // Matches output ID

            // Convert to specific target or all zones
            if (!targetTimeZoneId.isEmpty()) {
                ZoneId targetZone = ZoneId.of(targetTimeZoneId); // Throws if invalid
                ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);
                ZoneRules targetRules = targetZone.getRules();
                Instant targetInstant = targetZonedDateTime.toInstant();

                result.put("convert_targetInfo", targetZonedDateTime.format(DISPLAY_DATETIME_FORMAT) + " " + targetZone.getId()); // Matches output ID
                result.put("convert_targetOffset", formatOffset(targetRules.getOffset(targetInstant))); // Matches output ID
                result.put("convert_targetIsDST", targetRules.isDaylightSavings(targetInstant)); // Matches output ID (boolean)

            } else {
                // Convert to all zones (similar to getCurrentTime but starting from sourceZonedDateTime)
                List<Map<String, Object>> conversions = new ArrayList<>();
                Instant sourceInstant = sourceZonedDateTime.toInstant();

                for (String zoneId : new TreeSet<>(ZoneId.getAvailableZoneIds())) {
                    if (zoneId.toLowerCase().startsWith("etc/")) continue; // Skip Etc
                    try {
                        ZoneId targetZone = ZoneId.of(zoneId);
                        ZonedDateTime targetZonedDateTime = sourceInstant.atZone(targetZone);
                        ZoneRules rules = targetZone.getRules();
                        boolean isDst = rules.isDaylightSavings(sourceInstant);
                        ZoneOffset offset = rules.getOffset(sourceInstant);

                        conversions.add(Map.of(
                                "zoneName", zoneId,
                                "formattedTime", targetZonedDateTime.format(DISPLAY_DATETIME_FORMAT),
                                "offset", formatOffset(offset),
                                "isDST", isDst, // boolean
                                "offsetSeconds", offset.getTotalSeconds()
                        ));
                    } catch (Exception e) { /* Skip invalid zones */ }
                }
                conversions.sort(Comparator.<Map<String, Object>, Integer>comparing(tz -> (Integer)tz.get("offsetSeconds"))
                        .thenComparing(tz -> (String)tz.get("zoneName")));
                result.put("convert_allZonesTable", conversions); // Matches output ID
            }

        } catch (IllegalArgumentException | DateTimeParseException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error converting time: " + e.getMessage());
            result.put("success", false); result.put(errorOutputId, "Conversion failed. Check time format and zone IDs.");
        }
        return result;
    }


    /** Get details for a specific time zone. */
    private Map<String, Object> getTimeZoneDetails(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            String timeZoneId = getStringParam(input, "timeZoneDetailInput", null); // Required

            ZoneId zone = ZoneId.of(timeZoneId); // Throws if invalid
            ZoneRules rules = zone.getRules();
            Instant nowInstant = Instant.now();
            ZonedDateTime nowInZone = nowInstant.atZone(zone);

            ZoneOffset currentOffset = rules.getOffset(nowInstant);
            boolean isCurrentlyDst = rules.isDaylightSavings(nowInstant);
            ZoneOffset standardOffset = rules.getStandardOffset(nowInstant);
            ZoneOffsetTransition nextTransition = rules.nextTransition(nowInstant);
            ZoneOffsetTransition prevTransition = rules.previousTransition(nowInstant);

            result.put("success", true);
            result.put("details_id", zone.getId());
            result.put("details_displayName", zone.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH));
            result.put("details_currentTime", nowInZone.format(DISPLAY_DATETIME_FORMAT));
            result.put("details_currentOffset", formatOffset(currentOffset));
            result.put("details_standardOffset", formatOffset(standardOffset));
            result.put("details_isDSTActive", isCurrentlyDst); // boolean

            if (nextTransition != null) {
                result.put("details_nextTransition", formatTransition(nextTransition, zone)); // JSON object
            }
            if (prevTransition != null) {
                result.put("details_prevTransition", formatTransition(prevTransition, zone)); // JSON object
            }

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error getting time zone details: " + e.getMessage());
            result.put("success", false); result.put(errorOutputId, "Failed to get details for zone ID.");
        }
        return result;
    }

    // Helper to format transition info into a map for JSON output
    private Map<String, Object> formatTransition(ZoneOffsetTransition transition, ZoneId zone) {
        if (transition == null) return null;
        Map<String, Object> info = new HashMap<>();
        info.put("transitionTimeUTC", transition.getInstant().toString());
        info.put("transitionTimeLocal", transition.getDateTimeBefore().format(DISPLAY_DATETIME_FORMAT) + " -> " + transition.getDateTimeAfter().format(DISPLAY_DATETIME_FORMAT) );
        info.put("offsetBefore", formatOffset(transition.getOffsetBefore()));
        info.put("offsetAfter", formatOffset(transition.getOffsetAfter()));
        // isGap() is true when clocks spring forward (gap), isOverlap() when they fall back (overlap)
        info.put("type", transition.isGap() ? "Spring Forward (DST Start)" : "Fall Back (DST End)");
        return info;
    }


    // Format ZoneOffset to "+HH:MM" or "Z"
    private String formatOffset(ZoneOffset offset) {
        if (offset == null) return "N/A";
        String formatted = offset.getId();
        return "Z".equals(formatted) ? "+00:00" : formatted; // Replace Z with UTC offset
    }

    // Get Zone ID description (City/Region)
    private String getZoneDescription(String zoneId) {
        return zoneId.substring(zoneId.lastIndexOf('/') + 1).replace('_', ' ');
    }

    // Format PhoneNumberType enum to readable string
    private String formatNumberType(PhoneNumberUtil.PhoneNumberType type) {
        if (type == null) return "Unknown";
        // ... (implementation from previous PhoneParser example) ...
        return switch (type) {
            case FIXED_LINE -> "Fixed Line";
            case MOBILE -> "Mobile";
            case FIXED_LINE_OR_MOBILE -> "Fixed Line or Mobile";
            case TOLL_FREE -> "Toll Free";
            case PREMIUM_RATE -> "Premium Rate";
            case SHARED_COST -> "Shared Cost";
            case VOIP -> "VoIP";
            case PERSONAL_NUMBER -> "Personal Number";
            case PAGER -> "Pager";
            case UAN -> "UAN";
            case VOICEMAIL -> "Voicemail";
            default -> type.name(); // Fallback
        };
    }

    // --- Parameter Parsing Helpers ---

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

    // Null default indicates required
    private Integer getIntParam(Map<String, Object> input) throws IllegalArgumentException {
        Object value = input.get("limit");
        if (value == null || value.toString().trim().isEmpty()) {
            return 500;
            // Only throw if truly required (defaultValue is null)
            // For limit, we have a default, so this won't be hit if key is missing
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            if (Math.abs(dValue - Math.round(dValue)) < 0.00001) return (int) Math.round(dValue);
            else throw new IllegalArgumentException("Non-integer numeric value for integer parameter '" + "limit" + "': " + value);
        }
        else {
            try { return Integer.parseInt(value.toString()); }
            catch (NumberFormatException e) {
                return 500; // Allow fallback if optional
            }
        }
    }

}