package kostovite;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Locale;

public class WorldClock implements PluginInterface {

    // Common formatters
    private static final DateTimeFormatter INPUT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    // Cache for country display names
    private static final Map<String, String> COUNTRY_DISPLAY_NAMES_CACHE = new TreeMap<>();
    static {
        for (String regionCode : ZoneId.getAvailableZoneIds()) {
            if (regionCode.contains("/") && !regionCode.toLowerCase().startsWith("etc/")) {
                String code = regionCode.substring(regionCode.lastIndexOf('/') + 1).replace('_', ' ');
            }
            if (!regionCode.toLowerCase().startsWith("etc/") && regionCode.contains("/")) {
                COUNTRY_DISPLAY_NAMES_CACHE.put(regionCode, regionCode.replace('_', ' '));
            }
        }
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
            params.put("uiOperation", "convertTime");
            params.put("dateTimeInput", "2024-07-15 10:00:00");
            params.put("sourceTimeZoneInput", "America/New_York");
            params.put("targetTimeZoneInput", "Asia/Tokyo");

            Map<String, Object> result = process(params);
            System.out.println("Convert Time Result: " + result);

            params.clear();
            params.put("uiOperation", "getCurrentTime");
            params.put("filter", "Europe");
            result = process(params);
            System.out.println("Get Current Time Result (Europe): ");
            if(result.get("success") == Boolean.TRUE && result.containsKey("timeZoneTable")){
                System.out.println(((List<?>)result.get("timeZoneTable")).size() + " zones found.");
            } else {
                System.out.println(result);
            }

            params.clear();
            params.put("uiOperation", "getTimeZoneDetails");
            params.put("timeZoneDetailInput", "Australia/Sydney");
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

        // Top Level Attributes
        metadata.put("id", "WorldClock");
        metadata.put("name", "World Clock & Time Converter");
        metadata.put("description", "View current time across different zones or convert a specific time.");
        metadata.put("icon", "Public");
        metadata.put("category", "Utilities");
        metadata.put("customUI", false);

        List<Map<String, Object>> sections = new ArrayList<>();

        // Section 1: Operation Selection (Common for all)
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
                )),
                Map.entry("default", "convertTime"),
                Map.entry("required", true)
        ));
        operationSection.put("inputs", operationInputs);
        sections.add(operationSection);

        // Convert Time Section
        Map<String, Object> convertSection = new HashMap<>();
        convertSection.put("id", "convertTimeSection");
        convertSection.put("label", "Convert Time");
        convertSection.put("condition", "uiOperation === 'convertTime'");

        // Inputs for Convert Time
        List<Map<String, Object>> convertInputs = new ArrayList<>();
        convertInputs.add(Map.ofEntries(
                Map.entry("id", "dateTimeInput"),
                Map.entry("label", "Date & Time to Convert:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "YYYY-MM-DD HH:MM:SS"),
                Map.entry("required", true)
        ));
        convertInputs.add(Map.ofEntries(
                Map.entry("id", "sourceTimeZoneInput"),
                Map.entry("label", "From Time Zone:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe/Paris, America/Los_Angeles, UTC"),
                Map.entry("required", true),
                Map.entry("default", "UTC")
        ));
        convertInputs.add(Map.ofEntries(
                Map.entry("id", "targetTimeZoneInput"),
                Map.entry("label", "To Time Zone (Optional):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe/Paris, America/Los_Angeles, UTC"),
                Map.entry("required", false),
                Map.entry("default", ZoneId.systemDefault().getId())
        ));
        convertSection.put("inputs", convertInputs);

        // Outputs for Convert Time
        List<Map<String, Object>> convertOutputs = new ArrayList<>();
        convertOutputs.add(createSimpleOutputField("convert_sourceInfo", "Source Time", "text"));
        convertOutputs.add(createSimpleOutputField("convert_targetInfo", "Target Time", "text"));
        convertOutputs.add(createSimpleOutputField("convert_targetOffset", "Target Offset", "text"));
        convertOutputs.add(createSimpleOutputField("convert_targetIsDST", "Target DST", "boolean"));

        Map<String, Object> allConversionsTable = createSimpleOutputField("convert_allZonesTable", "All Zone Conversions", "table");
        allConversionsTable.put("columns", List.of(
                Map.of("header", "Zone ID", "field", "zoneName"),
                Map.of("header", "Converted Time", "field", "formattedTime"),
                Map.of("header", "Offset", "field", "offset"),
                Map.of("header", "DST Active", "field", "isDST")
        ));
        convertOutputs.add(allConversionsTable);
        convertSection.put("outputs", convertOutputs);
        sections.add(convertSection);

        // Current Time Section
        Map<String, Object> currentTimeSection = new HashMap<>();
        currentTimeSection.put("id", "currentTimeSection");
        currentTimeSection.put("label", "Current Times");
        currentTimeSection.put("condition", "uiOperation === 'getCurrentTime'");

        // Inputs for Current Time
        List<Map<String, Object>> currentTimeInputs = new ArrayList<>();
        currentTimeInputs.add(Map.ofEntries(
                Map.entry("id", "filter"),
                Map.entry("label", "Filter Zones (Optional):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe, London, EST, GMT"),
                Map.entry("required", false),
                Map.entry("helperText", "Enter region, city, or abbreviation.")
        ));
        currentTimeInputs.add(Map.ofEntries(
                Map.entry("id", "limit"),
                Map.entry("label", "Max Zones (Optional):"),
                Map.entry("type", "number"),
                Map.entry("default", 50),
                Map.entry("min", 1),
                Map.entry("max", 500),
                Map.entry("required", false)
        ));
        currentTimeSection.put("inputs", currentTimeInputs);

        // Outputs for Current Time
        List<Map<String, Object>> currentTimeOutputs = new ArrayList<>();
        currentTimeOutputs.add(createSimpleOutputField("currentTime_utc", "Current UTC Time", "text"));

        Map<String, Object> currentTimeTable = createSimpleOutputField("timeZoneTable", "Current Times by Zone", "table");
        currentTimeTable.put("columns", List.of(
                Map.of("header", "Zone ID", "field", "zoneName"),
                Map.of("header", "Current Time", "field", "formattedTime"),
                Map.of("header", "Offset", "field", "offset"),
                Map.of("header", "DST Active", "field", "isDST")
        ));
        currentTimeOutputs.add(currentTimeTable);
        currentTimeSection.put("outputs", currentTimeOutputs);
        sections.add(currentTimeSection);

        // Time Zone Details Section
        Map<String, Object> detailsSection = new HashMap<>();
        detailsSection.put("id", "timeZoneDetailsSection");
        detailsSection.put("label", "Time Zone Details");
        detailsSection.put("condition", "uiOperation === 'getTimeZoneDetails'");

        // Inputs for Time Zone Details
        List<Map<String, Object>> detailsInputs = new ArrayList<>();
        detailsInputs.add(Map.ofEntries(
                Map.entry("id", "timeZoneDetailInput"),
                Map.entry("label", "Time Zone:"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "e.g., Europe/Paris, America/Los_Angeles, UTC"),
                Map.entry("required", true),
                Map.entry("default", "Europe/London")
        ));
        detailsSection.put("inputs", detailsInputs);

        // Outputs for Time Zone Details
        List<Map<String, Object>> detailsOutputs = new ArrayList<>();
        detailsOutputs.add(createSimpleOutputField("details_id", "Zone ID", "text"));
        detailsOutputs.add(createSimpleOutputField("details_displayName", "Display Name", "text"));
        detailsOutputs.add(createSimpleOutputField("details_currentTime", "Current Time", "text"));
        detailsOutputs.add(createSimpleOutputField("details_currentOffset", "Current Offset", "text"));
        detailsOutputs.add(createSimpleOutputField("details_standardOffset", "Standard Offset", "text"));
        detailsOutputs.add(createSimpleOutputField("details_isDSTActive", "DST Currently Active", "boolean"));
        detailsOutputs.add(createSimpleOutputField("details_nextTransition", "Next DST Transition", "json"));
        detailsOutputs.add(createSimpleOutputField("details_prevTransition", "Previous DST Transition", "json"));
        detailsSection.put("outputs", detailsOutputs);
        sections.add(detailsSection);

        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create simple output field
    private Map<String, Object> createSimpleOutputField(String id, String label, String type) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        field.put("label", label);
        field.put("type", type);

        if ("text".equals(type) && (id.toLowerCase().contains("time") || id.toLowerCase().contains("offset"))) {
            field.put("monospace", true);
            field.put("buttons", List.of("copy"));
        }
        if ("json".equals(type)) {
            field.put("buttons", List.of("copy"));
        }
        return field;
    }

    /**
     * Processes the input parameters.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        try {
            String uiOperation = getStringParam(input, "uiOperation", null);
            result.put("uiOperation", uiOperation);

            switch (uiOperation.toLowerCase()) {
                case "getcurrenttime":
                    processGetCurrentTime(input, result);
                    break;

                case "converttime":
                    processConvertTime(input, result);
                    break;

                case "gettimezonedetails":
                    processGetTimeZoneDetails(input, result);
                    break;

                default:
                    result.put("success", false);
                    result.put("error", "Unsupported operation: " + uiOperation);
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        } catch (DateTimeParseException e) {
            result.put("success", false);
            result.put("error", "Invalid date/time format. Use YYYY-MM-DD HH:MM:SS.");
        } catch (Exception e) {
            System.err.println("Error processing WorldClock request: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "Unexpected error: " + e.getMessage());
        }

        return result;
    }

    // Helper methods that directly modify the result map
    private void processGetCurrentTime(Map<String, Object> input, Map<String, Object> result) {
        String filter = getStringParam(input, "filter", "");
        int limit = getIntParam(input);

        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        String utcTimeFormatted = utcNow.format(DISPLAY_DATETIME_FORMAT);

        result.put("currentTime_utc", utcTimeFormatted + " UTC");

        List<Map<String, Object>> timeZoneList = new ArrayList<>();
        Set<String> zoneIds = ZoneId.getAvailableZoneIds();
        final String filterLower = filter.toLowerCase();

        // Filter zones
        Set<String> filteredZoneIds = zoneIds.stream()
                .filter(zoneId -> !zoneId.toLowerCase().startsWith("etc/"))
                .filter(zoneId -> filter.isEmpty() || zoneId.toLowerCase().contains(filterLower) || getZoneDescription(zoneId).toLowerCase().contains(filterLower))
                .collect(Collectors.toSet());

        // Process and collect zones
        for (String zoneId : new TreeSet<>(filteredZoneIds)) {
            if (timeZoneList.size() >= limit) break;
            try {
                ZoneId zone = ZoneId.of(zoneId);
                ZonedDateTime zonedTime = utcNow.withZoneSameInstant(zone);
                ZoneRules rules = zone.getRules();
                Instant nowInstant = zonedTime.toInstant();
                boolean isDst = rules.isDaylightSavings(nowInstant);
                ZoneOffset offset = rules.getOffset(nowInstant);

                timeZoneList.add(Map.of(
                        "zoneName", zone.getId(),
                        "formattedTime", zonedTime.format(DISPLAY_DATETIME_FORMAT),
                        "offset", formatOffset(offset),
                        "isDST", isDst,
                        "offsetSeconds", offset.getTotalSeconds()
                ));
            } catch (Exception e) { /* Skip invalid zones */ }
        }

        // Sort by offset, then by name
        timeZoneList.sort(Comparator.<Map<String, Object>, Integer>comparing(tz -> (Integer)tz.get("offsetSeconds"))
                .thenComparing(tz -> (String)tz.get("zoneName")));

        result.put("timeZoneTable", timeZoneList);
    }

    private void processConvertTime(Map<String, Object> input, Map<String, Object> result) {
        String dateTimeStr = getStringParam(input, "dateTimeInput", null);
        String sourceTimeZoneId = getStringParam(input, "sourceTimeZoneInput", null);
        String targetTimeZoneId = getStringParam(input, "targetTimeZoneInput", "");

        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, INPUT_DATETIME_FORMAT);
        ZoneId sourceZone = ZoneId.of(sourceTimeZoneId);
        ZonedDateTime sourceZonedDateTime = ZonedDateTime.of(localDateTime, sourceZone);

        result.put("convert_sourceInfo", sourceZonedDateTime.format(DISPLAY_DATETIME_FORMAT) + " " + sourceZone.getId());

        // Convert to specific target or all zones
        if (!targetTimeZoneId.isEmpty()) {
            ZoneId targetZone = ZoneId.of(targetTimeZoneId);
            ZonedDateTime targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZone);
            ZoneRules targetRules = targetZone.getRules();
            Instant targetInstant = targetZonedDateTime.toInstant();

            result.put("convert_targetInfo", targetZonedDateTime.format(DISPLAY_DATETIME_FORMAT) + " " + targetZone.getId());
            result.put("convert_targetOffset", formatOffset(targetRules.getOffset(targetInstant)));
            result.put("convert_targetIsDST", targetRules.isDaylightSavings(targetInstant));
        } else {
            // Convert to all zones
            List<Map<String, Object>> conversions = new ArrayList<>();
            Instant sourceInstant = sourceZonedDateTime.toInstant();

            for (String zoneId : new TreeSet<>(ZoneId.getAvailableZoneIds())) {
                if (zoneId.toLowerCase().startsWith("etc/")) continue;
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
                            "isDST", isDst,
                            "offsetSeconds", offset.getTotalSeconds()
                    ));
                } catch (Exception e) { /* Skip invalid zones */ }
            }
            conversions.sort(Comparator.<Map<String, Object>, Integer>comparing(tz -> (Integer)tz.get("offsetSeconds"))
                    .thenComparing(tz -> (String)tz.get("zoneName")));
            result.put("convert_allZonesTable", conversions);
        }
    }

    private void processGetTimeZoneDetails(Map<String, Object> input, Map<String, Object> result) {
        String timeZoneId = getStringParam(input, "timeZoneDetailInput", null);

        ZoneId zone = ZoneId.of(timeZoneId);
        ZoneRules rules = zone.getRules();
        Instant nowInstant = Instant.now();
        ZonedDateTime nowInZone = nowInstant.atZone(zone);

        ZoneOffset currentOffset = rules.getOffset(nowInstant);
        boolean isCurrentlyDst = rules.isDaylightSavings(nowInstant);
        ZoneOffset standardOffset = rules.getStandardOffset(nowInstant);
        ZoneOffsetTransition nextTransition = rules.nextTransition(nowInstant);
        ZoneOffsetTransition prevTransition = rules.previousTransition(nowInstant);

        result.put("details_id", zone.getId());
        result.put("details_displayName", zone.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH));
        result.put("details_currentTime", nowInZone.format(DISPLAY_DATETIME_FORMAT));
        result.put("details_currentOffset", formatOffset(currentOffset));
        result.put("details_standardOffset", formatOffset(standardOffset));
        result.put("details_isDSTActive", isCurrentlyDst);

        if (nextTransition != null) {
            result.put("details_nextTransition", formatTransition(nextTransition, zone));
        }
        if (prevTransition != null) {
            result.put("details_prevTransition", formatTransition(prevTransition, zone));
        }
    }

    // Helper to format transition info into a map for JSON output
    private Map<String, Object> formatTransition(ZoneOffsetTransition transition, ZoneId zone) {
        if (transition == null) return null;
        Map<String, Object> info = new HashMap<>();
        info.put("transitionTimeUTC", transition.getInstant().toString());
        info.put("transitionTimeLocal", transition.getDateTimeBefore().format(DISPLAY_DATETIME_FORMAT) + " -> " + transition.getDateTimeAfter().format(DISPLAY_DATETIME_FORMAT) );
        info.put("offsetBefore", formatOffset(transition.getOffsetBefore()));
        info.put("offsetAfter", formatOffset(transition.getOffsetAfter()));
        info.put("type", transition.isGap() ? "Spring Forward (DST Start)" : "Fall Back (DST End)");
        return info;
    }

    // Format ZoneOffset to "+HH:MM" or "Z"
    private String formatOffset(ZoneOffset offset) {
        if (offset == null) return "N/A";
        String formatted = offset.getId();
        return "Z".equals(formatted) ? "+00:00" : formatted;
    }

    // Get Zone ID description (City/Region)
    private String getZoneDescription(String zoneId) {
        return zoneId.substring(zoneId.lastIndexOf('/') + 1).replace('_', ' ');
    }

    // Format PhoneNumberType enum to readable string
    private String formatNumberType(PhoneNumberUtil.PhoneNumberType type) {
        if (type == null) return "Unknown";
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
            default -> type.name();
        };
    }

    // Parameter Parsing Helpers
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

    private Integer getIntParam(Map<String, Object> input) throws IllegalArgumentException {
        Object value = input.get("limit");
        if (value == null || value.toString().trim().isEmpty()) {
            return 500;
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            if (Math.abs(dValue - Math.round(dValue)) < 0.00001) return (int) Math.round(dValue);
            else throw new IllegalArgumentException("Non-integer numeric value for integer parameter 'limit': " + value);
        }
        else {
            try { return Integer.parseInt(value.toString()); }
            catch (NumberFormatException e) {
                return 500;
            }
        }
    }
}