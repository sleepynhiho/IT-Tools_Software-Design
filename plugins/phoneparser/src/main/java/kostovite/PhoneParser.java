package kostovite;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.util.*;
import java.util.stream.Collectors; // For stream operations

// Assuming PluginInterface is standard
public class PhoneParser implements PluginInterface {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
    private final PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();

    // Cache for country display names
    private static final Map<String, String> COUNTRY_DISPLAY_NAMES_CACHE = new TreeMap<>(); // TreeMap for sorted keys
    static {
        // Pre-populate cache with supported regions
        for (String regionCode : PhoneNumberUtil.getInstance().getSupportedRegions()) {
            Locale locale = new Locale("", regionCode); // Get locale for the region code
            COUNTRY_DISPLAY_NAMES_CACHE.put(regionCode, locale.getDisplayCountry(Locale.ENGLISH)); // Use English display name
        }
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "PhoneParser";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("Phone Parser Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uiOperation", "parse"); // Use new ID
            params.put("phoneNumberInput", "+44 20 7123 4567"); // Use new ID
            params.put("defaultRegionCode", "GB"); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Parse Result: " + result);

            params.clear();
            params.put("uiOperation", "getSupportedRegions");
            result = process(params);
            System.out.println("Get Regions Result (Count): " + (result.containsKey("regions") ? ((List<?>)result.get("regions")).size() : "Error"));

            params.clear();
            params.put("uiOperation", "getExampleNumber");
            params.put("exampleRegionCode", "DE"); // Use new ID
            params.put("exampleNumberType", "MOBILE"); // Use new ID
            result = process(params);
            System.out.println("Get Example Result: " + result);

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
        metadata.put("id", "PhoneParser"); // ID matches class name
        metadata.put("name", "Phone Number Parser"); // User-facing name
        metadata.put("description", "Parse, validate, format phone numbers, get country info and examples using Google's libphonenumber.");
        metadata.put("icon", "Phone");
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
                        Map.of("value", "parse", "label", "Parse & Validate Number"),
                        // Map.of("value", "format", "label", "Format Number"), // Format is part of Parse now
                        Map.of("value", "getSupportedRegions", "label", "List Supported Countries"),
                        Map.of("value", "getExampleNumber", "label", "Get Example Number")
                )),
                Map.entry("default", "parse"),
                Map.entry("required", true)
        ));
        operationSection.put("inputs", operationInputs);
        sections.add(operationSection);

        // --- Section 2: Input Parameters (Conditional) ---
        Map<String, Object> paramsSection = new HashMap<>();
        paramsSection.put("id", "parameters");
        paramsSection.put("label", "Input Parameters");
        // No top-level condition, handled by fields

        List<Map<String, Object>> paramInputs = new ArrayList<>();

        // Phone Number Input (for parse)
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "phoneNumberInput"),
                Map.entry("label", "Phone Number:"),
                Map.entry("type", "text"), // Use text for flexibility
                Map.entry("placeholder", "e.g., +1 415 555 2671, 020 7123 4567"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'parse'"),
                Map.entry("helperText", "Enter full number, include country code if known.")
        ));

        // Default Region Select (for parse)
        List<Map<String, String>> regionOptions = COUNTRY_DISPLAY_NAMES_CACHE.entrySet().stream()
                .map(entry -> Map.of("value", entry.getKey(), "label", entry.getValue() + " (" + entry.getKey() + ")"))
                .sorted(Comparator.comparing(option -> option.get("label"))) // Sort alphabetically by display name
                .toList();

        paramInputs.add(Map.ofEntries(
                Map.entry("id", "defaultRegionCode"),
                Map.entry("label", "Default Country (if code missing):"),
                Map.entry("type", "select"),
                Map.entry("options", regionOptions),
                Map.entry("default", Locale.getDefault().getCountry()), // Sensible default
                Map.entry("required", false), // Not strictly required, library can guess
                Map.entry("condition", "uiOperation === 'parse'"),
                Map.entry("helperText", "Helps parse numbers without a country code.")
        ));

        // Region Select (for getExampleNumber)
        paramInputs.add(Map.ofEntries(
                Map.entry("id", "exampleRegionCode"),
                Map.entry("label", "Country:"),
                Map.entry("type", "select"),
                Map.entry("options", regionOptions), // Reuse region options
                Map.entry("default", Locale.getDefault().getCountry()),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'getExampleNumber'")
        ));

        // Example Number Type Select (for getExampleNumber)
        List<Map<String, String>> typeOptions = Arrays.stream(PhoneNumberType.values())
                .filter(type -> type != PhoneNumberType.UNKNOWN) // Exclude UNKNOWN type
                .map(type -> Map.of("value", type.name(), "label", formatNumberType(type)))
                .sorted(Comparator.comparing(option -> option.get("label")))
                .toList();

        paramInputs.add(Map.ofEntries(
                Map.entry("id", "exampleNumberType"),
                Map.entry("label", "Number Type (Optional):"),
                Map.entry("type", "select"),
                Map.entry("options", typeOptions),
                Map.entry("default", "MOBILE"), // Common default
                Map.entry("required", false),
                Map.entry("condition", "uiOperation === 'getExampleNumber'")
        ));


        paramsSection.put("inputs", paramInputs);
        sections.add(paramsSection);


        // --- Section 3: Parse Results ---
        Map<String, Object> parseResultsSection = new HashMap<>();
        parseResultsSection.put("id", "parseResults");
        parseResultsSection.put("label", "Parsed Number Details");
        parseResultsSection.put("condition", "success === true && uiOperation === 'parse'"); // Show only on successful parse

        List<Map<String, Object>> parseOutputs = new ArrayList<>();

        parseOutputs.add(createOutputField("parse_isValid", "Is Valid Number?", "boolean", null)); // Frontend renders check/cross
        parseOutputs.add(createOutputField("parse_isPossible", "Is Possible Number?", "boolean", null));
        parseOutputs.add(createOutputField("parse_country", "Country / Region", "text", null));
        parseOutputs.add(createOutputField("parse_numberType", "Number Type", "text", null));
        parseOutputs.add(createOutputField("parse_location", "Location (Approx)", "text", "typeof parse_location !== 'undefined' && parse_location !== ''")); // Optional
        parseOutputs.add(createOutputField("parse_carrier", "Carrier (Approx)", "text", "typeof parse_carrier !== 'undefined' && parse_carrier !== ''")); // Optional
        parseOutputs.add(createOutputField("parse_e164", "E.164 Format", "text", null));
        parseOutputs.add(createOutputField("parse_international", "International Format", "text", null));
        parseOutputs.add(createOutputField("parse_national", "National Format", "text", null));
        parseOutputs.add(createOutputField("parse_rfc3966", "RFC3966 URI", "text", null));
        parseOutputs.add(createOutputField("parse_extension", "Extension", "text", "typeof parse_extension !== 'undefined' && parse_extension !== ''")); // Optional

        parseResultsSection.put("outputs", parseOutputs);
        sections.add(parseResultsSection);


        // --- Section 4: Regions List ---
        Map<String, Object> regionsSection = new HashMap<>();
        regionsSection.put("id", "regionsList");
        regionsSection.put("label", "Supported Countries");
        regionsSection.put("condition", "success === true && uiOperation === 'getSupportedRegions'");

        List<Map<String, Object>> regionOutputs = new ArrayList<>();
        regionOutputs.add(createOutputField("regions_count", "Total Count", "text", null));
        // Regions Table
        Map<String, Object> regionTable = createOutputField("regions_list", "", "table", null); // Label handled by section
        regionTable.put("columns", List.of(
                Map.of("header", "Code", "field", "code"),
                Map.of("header", "Country Name", "field", "name"),
                Map.of("header", "Calling Code", "field", "callingCode")
        ));
        regionOutputs.add(regionTable);

        regionsSection.put("outputs", regionOutputs);
        sections.add(regionsSection);

        // --- Section 5: Example Number Result ---
        Map<String, Object> exampleSection = new HashMap<>();
        exampleSection.put("id", "exampleResult");
        exampleSection.put("label", "Example Number");
        exampleSection.put("condition", "success === true && uiOperation === 'getExampleNumber'");

        List<Map<String, Object>> exampleOutputs = new ArrayList<>();
        exampleOutputs.add(createOutputField("example_country", "Country", "text", null));
        exampleOutputs.add(createOutputField("example_type", "Number Type", "text", null));
        exampleOutputs.add(createOutputField("example_international", "International Format", "text", null));
        exampleOutputs.add(createOutputField("example_national", "National Format", "text", null));
        exampleOutputs.add(createOutputField("example_e164", "E.164 Format", "text", null));

        exampleSection.put("outputs", exampleOutputs);
        sections.add(exampleSection);


        // --- Section 6: Error Display ---
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
        if ("text".equals(type) && (id.toLowerCase().contains("format") || id.toLowerCase().contains("e164"))) {
            field.put("monospace", true); // Monospace for formatted numbers
            field.put("buttons", List.of("copy")); // Add copy button
        }
        // No specific attributes needed, frontend renders check/cross
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format).
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String uiOperation = getStringParam(input, "uiOperation", null); // Operation is required
        String errorOutputId = "errorMessage";

        Map<String, Object> processingInput = new HashMap<>(input); // Copy

        try {
            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "parse" -> result = parsePhoneNumber(processingInput);
                // Format operation removed, included within parse
                // case "format" -> result = formatPhoneNumber(processingInput);
                case "getsupportedregions" -> result = getSupportedRegions();
                case "getexamplenumber" -> result = getExampleNumber(processingInput);
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
                    return finalResult;
                }
                return result;
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing phone number request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Action Methods (Updated for new IDs)
    // ========================================================================

    private Map<String, Object> parsePhoneNumber(Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>(); // Preserve order
        String errorOutputId = "errorMessage";

        try {
            String phoneNumberStr = getStringParam(input, "phoneNumberInput", null); // Use new ID, required
            String defaultRegion = getStringParam(input, "defaultRegionCode", Locale.getDefault().getCountry()); // Use new ID

            PhoneNumber parsedNumber;
            try {
                parsedNumber = phoneUtil.parse(phoneNumberStr, defaultRegion.toUpperCase());
            } catch (NumberParseException e) {
                // Provide detailed error info for NumberParseException
                result.put("success", false);
                result.put(errorOutputId, "Parsing failed: " + e.getMessage());
                result.put("errorType", e.getErrorType().toString()); // Matches output ID
                return result;
            }

            // Basic Info
            boolean isValid = phoneUtil.isValidNumber(parsedNumber);
            boolean isPossible = phoneUtil.isPossibleNumber(parsedNumber);
            String regionCode = phoneUtil.getRegionCodeForNumber(parsedNumber);
            String countryName = getCountryDisplayName(regionCode);
            int callingCode = parsedNumber.getCountryCode();
            PhoneNumberType numberType = phoneUtil.getNumberType(parsedNumber);
            String location = geocoder.getDescriptionForNumber(parsedNumber, Locale.ENGLISH);
            String carrier = carrierMapper.getNameForNumber(parsedNumber, Locale.ENGLISH);
            String extension = parsedNumber.hasExtension() ? parsedNumber.getExtension() : null;

            // Build success result map using NEW output IDs
            result.put("success", true);
            result.put("parse_isValid", isValid); // boolean
            result.put("parse_isPossible", isPossible); // boolean
            result.put("parse_country", countryName + " (" + regionCode + ", +" + callingCode + ")");
            result.put("parse_numberType", formatNumberType(numberType));

            // Optional fields
            if (location != null && !location.isEmpty()) result.put("parse_location", location);
            if (carrier != null && !carrier.isEmpty()) result.put("parse_carrier", carrier);
            if (extension != null && !extension.isEmpty()) result.put("parse_extension", extension);

            // Formatted numbers
            result.put("parse_e164", phoneUtil.format(parsedNumber, PhoneNumberFormat.E164));
            result.put("parse_international", phoneUtil.format(parsedNumber, PhoneNumberFormat.INTERNATIONAL));
            result.put("parse_national", phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL));
            result.put("parse_rfc3966", phoneUtil.format(parsedNumber, PhoneNumberFormat.RFC3966));

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors during lib calls
            System.err.println("Unexpected error during phone parsing: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put(errorOutputId, "Internal error during parsing.");
        }
        return result;
    }

    private Map<String, Object> getSupportedRegions() {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            List<Map<String, Object>> regionList = COUNTRY_DISPLAY_NAMES_CACHE.entrySet().stream()
                    .map(entry -> {
                        String code = entry.getKey();
                        String name = entry.getValue();
                        int callingCode = phoneUtil.getCountryCodeForRegion(code);
                        return Map.<String, Object>of(
                                "code", code,
                                "name", name,
                                "callingCode", "+" + callingCode // Add '+' for display
                        );
                    })
                    .sorted(Comparator.comparing(map -> (String) map.get("name"))) // Sort by name
                    .collect(Collectors.toList());

            result.put("success", true);
            result.put("regions_list", regionList); // Matches table output ID
            result.put("regions_count", regionList.size()); // Matches text output ID

        } catch (Exception e) {
            System.err.println("Error getting supported regions: " + e.getMessage());
            result.put("success", false);
            result.put(errorOutputId, "Could not retrieve supported regions list.");
        }
        return result;
    }

    private Map<String, Object> getExampleNumber(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            String region = getStringParam(input, "exampleRegionCode", null); // Required
            String typeStr = getStringParam(input, "exampleNumberType", "MOBILE"); // Optional

            PhoneNumberType numberType = PhoneNumberType.MOBILE; // Default
            try {
                numberType = PhoneNumberType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) { /* Ignore, use default */ }

            PhoneNumber exampleNumber = phoneUtil.getExampleNumberForType(region.toUpperCase(), numberType);
            if (exampleNumber == null) { // Try general example if type specific fails
                exampleNumber = phoneUtil.getExampleNumber(region.toUpperCase());
            }

            if (exampleNumber == null) {
                throw new IllegalArgumentException("No example number found for " + region + " (Type: " + numberType + ")");
            }

            result.put("success", true);
            result.put("example_country", getCountryDisplayName(region));
            result.put("example_type", formatNumberType(numberType));
            result.put("example_international", phoneUtil.format(exampleNumber, PhoneNumberFormat.INTERNATIONAL));
            result.put("example_national", phoneUtil.format(exampleNumber, PhoneNumberFormat.NATIONAL));
            result.put("example_e164", phoneUtil.format(exampleNumber, PhoneNumberFormat.E164));

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error getting example number: " + e.getMessage());
            result.put("success", false);
            result.put(errorOutputId, "Could not retrieve example number.");
        }
        return result;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String formatNumberType(PhoneNumberType type) {
        // Reuse existing switch, make sure it covers all enum values if needed
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
            // case UNKNOWN -> "Unknown"; // Excluded from options usually
            default -> type.name(); // Fallback to enum name
        };
    }

    private String getCountryDisplayName(String regionCode) {
        return regionCode == null ? "Unknown" : COUNTRY_DISPLAY_NAMES_CACHE.getOrDefault(regionCode.toUpperCase(), regionCode);
    }

    // Null default indicates required
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString().trim(); // Trim region codes and phone numbers
        if (strValue.isEmpty()) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return strValue;
    }

    // Null default indicates required
    private boolean getBooleanParam(Map<String, Object> input, String key, boolean defaultValue) {
        Object value = input.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return "true".equalsIgnoreCase(value.toString());
        return defaultValue;
    }
}