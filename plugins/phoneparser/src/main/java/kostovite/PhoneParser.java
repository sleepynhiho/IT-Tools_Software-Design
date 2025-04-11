package kostovite;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PhoneParser implements PluginInterface {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
    private final PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();

    private static final Map<String, String> COUNTRY_DISPLAY_NAMES = new HashMap<>();
    static {
        for (String regionCode : PhoneNumberUtil.getInstance().getSupportedRegions()) {
            Locale locale = new Locale("", regionCode);
            COUNTRY_DISPLAY_NAMES.put(regionCode, locale.getDisplayCountry());
        }
    }

    @Override
    public String getName() {
        return "PhoneParser";
    }

    @Override
    public void execute() {
        System.out.println("Phone Parser Plugin executed");

        // Demonstrate basic usage
        try {
            // Example phone parsing
            Map<String, Object> params = new HashMap<>();
            params.put("phoneNumber", "+1 (555) 123-4567");
            params.put("defaultRegion", "US");

            Map<String, Object> result = process(params);
            System.out.println("Parsed phone number: " + result.get("phoneNumber"));
            System.out.println("Country: " + result.get("countryCode"));
            System.out.println("Is valid: " + result.get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse, validate and format phone numbers"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Parse phone number operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse and validate phone numbers");
        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("phoneNumber", Map.of("type", "string", "description", "Phone number to parse", "required", true));
        parseInputs.put("defaultRegion", Map.of("type", "string", "description", "Default country code (ISO 3166-1 alpha-2) to use for parsing", "required", false));
        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        // Format phone number operation
        Map<String, Object> formatOperation = new HashMap<>();
        formatOperation.put("description", "Format phone numbers");
        Map<String, Object> formatInputs = new HashMap<>();
        formatInputs.put("phoneNumber", Map.of("type", "string", "description", "Phone number to format", "required", true));
        formatInputs.put("defaultRegion", Map.of("type", "string", "description", "Default country code (ISO 3166-1 alpha-2) to use for parsing", "required", false));
        formatInputs.put("format", Map.of("type", "string", "description", "Format type (INTERNATIONAL, NATIONAL, E164, RFC3966)", "required", false));
        formatOperation.put("inputs", formatInputs);
        operations.put("format", formatOperation);

        // Get supported regions operation
        Map<String, Object> regionsOperation = new HashMap<>();
        regionsOperation.put("description", "Get supported regions");
        operations.put("getSupportedRegions", regionsOperation);

        // Get example number operation
        Map<String, Object> exampleOperation = new HashMap<>();
        exampleOperation.put("description", "Get example phone number for region");
        Map<String, Object> exampleInputs = new HashMap<>();
        exampleInputs.put("region", Map.of("type", "string", "description", "Region code (ISO 3166-1 alpha-2)", "required", true));
        exampleInputs.put("type", Map.of("type", "string", "description", "Phone number type (MOBILE, FIXED_LINE, etc.)", "required", false));
        exampleOperation.put("inputs", exampleInputs);
        operations.put("getExampleNumber", exampleOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "PhoneParser"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Phone"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Utilities"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Operation Selection
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Phone Number Operation");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Operation selection field
        Map<String, Object> operationField = new HashMap<>();
        operationField.put("name", "operation");
        operationField.put("label", "Operation:");
        operationField.put("type", "select");
        List<Map<String, String>> operationOptions = new ArrayList<>();
        operationOptions.add(Map.of("value", "parse", "label", "Parse & Validate Phone Number"));
        operationOptions.add(Map.of("value", "format", "label", "Format Phone Number"));
        operationOptions.add(Map.of("value", "getSupportedRegions", "label", "List Supported Countries"));
        operationOptions.add(Map.of("value", "getExampleNumber", "label", "Get Example Number"));
        operationField.put("options", operationOptions);
        operationField.put("default", "parse");
        operationField.put("required", true);
        section1Fields.add(operationField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Phone Number Input (conditional)
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Phone Number");
        inputSection2.put("condition", "operation === 'parse' || operation === 'format'");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Phone number field
        Map<String, Object> phoneField = new HashMap<>();
        phoneField.put("name", "phoneNumber");
        phoneField.put("label", "Phone Number:");
        phoneField.put("type", "text");
        phoneField.put("placeholder", "e.g., +1 (555) 123-4567");
        phoneField.put("required", true);
        phoneField.put("helperText", "Enter a phone number with or without country code");
        section2Fields.add(phoneField);

        // Default region field
        Map<String, Object> regionField = new HashMap<>();
        regionField.put("name", "defaultRegion");
        regionField.put("label", "Default Country:");
        regionField.put("type", "select");
        regionField.put("helperText", "Used when country code is not specified in the phone number");

        // Add common countries first
        List<Map<String, String>> regionOptions = new ArrayList<>();
        regionOptions.add(Map.of("value", "US", "label", "United States (+1)"));
        regionOptions.add(Map.of("value", "CA", "label", "Canada (+1)"));
        regionOptions.add(Map.of("value", "GB", "label", "United Kingdom (+44)"));
        regionOptions.add(Map.of("value", "AU", "label", "Australia (+61)"));
        regionOptions.add(Map.of("value", "DE", "label", "Germany (+49)"));
        regionOptions.add(Map.of("value", "FR", "label", "France (+33)"));
        regionOptions.add(Map.of("value", "JP", "label", "Japan (+81)"));
        regionOptions.add(Map.of("value", "CN", "label", "China (+86)"));
        regionOptions.add(Map.of("value", "IN", "label", "India (+91)"));
        regionOptions.add(Map.of("value", "BR", "label", "Brazil (+55)"));
        regionField.put("options", regionOptions);
        regionField.put("default", "US");
        regionField.put("required", false);
        section2Fields.add(regionField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Format Options (conditional)
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Format Options");
        inputSection3.put("condition", "operation === 'format'");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Format selection field
        Map<String, Object> formatField = new HashMap<>();
        formatField.put("name", "format");
        formatField.put("label", "Format Type:");
        formatField.put("type", "select");
        List<Map<String, String>> formatOptions = new ArrayList<>();
        formatOptions.add(Map.of("value", "INTERNATIONAL", "label", "International (+1 555-123-4567)"));
        formatOptions.add(Map.of("value", "NATIONAL", "label", "National ((555) 123-4567)"));
        formatOptions.add(Map.of("value", "E164", "label", "E.164 (+15551234567)"));
        formatOptions.add(Map.of("value", "RFC3966", "label", "RFC3966 (tel:+1-555-123-4567)"));
        formatField.put("options", formatOptions);
        formatField.put("default", "INTERNATIONAL");
        formatField.put("required", false);
        section3Fields.add(formatField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        // Input Section 4: Example Number Request (conditional)
        Map<String, Object> inputSection4 = new HashMap<>();
        inputSection4.put("header", "Example Number");
        inputSection4.put("condition", "operation === 'getExampleNumber'");
        List<Map<String, Object>> section4Fields = new ArrayList<>();

        // Region selection field
        Map<String, Object> exampleRegionField = new HashMap<>();
        exampleRegionField.put("name", "region");
        exampleRegionField.put("label", "Country:");
        exampleRegionField.put("type", "select");
        exampleRegionField.put("options", regionOptions); // Reuse the same options
        exampleRegionField.put("default", "US");
        exampleRegionField.put("required", true);
        section4Fields.add(exampleRegionField);

        // Phone number type selection field
        Map<String, Object> typeField = new HashMap<>();
        typeField.put("name", "type");
        typeField.put("label", "Number Type:");
        typeField.put("type", "select");
        List<Map<String, String>> typeOptions = new ArrayList<>();
        typeOptions.add(Map.of("value", "MOBILE", "label", "Mobile"));
        typeOptions.add(Map.of("value", "FIXED_LINE", "label", "Fixed Line"));
        typeOptions.add(Map.of("value", "TOLL_FREE", "label", "Toll Free"));
        typeOptions.add(Map.of("value", "PREMIUM_RATE", "label", "Premium Rate"));
        typeOptions.add(Map.of("value", "VOIP", "label", "VoIP"));
        typeField.put("options", typeOptions);
        typeField.put("default", "MOBILE");
        typeField.put("required", false);
        section4Fields.add(typeField);

        inputSection4.put("fields", section4Fields);
        uiInputs.add(inputSection4);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Validation Results
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Validation Results");
        outputSection1.put("condition", "success && operation === 'parse'");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Validity indicators
        Map<String, Object> validityOutput = new HashMap<>();
        validityOutput.put("title", "Valid Number");
        validityOutput.put("name", "isValid");
        validityOutput.put("type", "checkmark");
        section1OutputFields.add(validityOutput);

        // Original number
        Map<String, Object> originalNumberOutput = new HashMap<>();
        originalNumberOutput.put("title", "Input Number");
        originalNumberOutput.put("name", "phoneNumber");
        originalNumberOutput.put("type", "text");
        section1OutputFields.add(originalNumberOutput);

        // Parsed number
        Map<String, Object> parsedNumberOutput = new HashMap<>();
        parsedNumberOutput.put("title", "Parsed Number");
        parsedNumberOutput.put("name", "internationalFormat");
        parsedNumberOutput.put("type", "text");
        parsedNumberOutput.put("style", "isValid ? 'success' : 'warning'");
        parsedNumberOutput.put("buttons", List.of("copy"));
        section1OutputFields.add(parsedNumberOutput);

        // Country information
        Map<String, Object> countryOutput = new HashMap<>();
        countryOutput.put("title", "Country");
        countryOutput.put("name", "countryInfo");
        countryOutput.put("type", "text");
        countryOutput.put("formula", "countryName + ' (' + countryCode + ', +' + callingCode + ')'");
        section1OutputFields.add(countryOutput);

        // Number type
        Map<String, Object> numberTypeOutput = new HashMap<>();
        numberTypeOutput.put("title", "Number Type");
        numberTypeOutput.put("name", "numberTypeName");
        numberTypeOutput.put("type", "text");
        section1OutputFields.add(numberTypeOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Formatting Options
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Formatting Options");
        outputSection2.put("condition", "success && (operation === 'parse' || operation === 'format')");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Format options with copy buttons

        Map<String, Object> internationalFormatOutput = new HashMap<>();
        internationalFormatOutput.put("title", "International");
        internationalFormatOutput.put("name", "internationalFormat");
        internationalFormatOutput.put("type", "text");
        internationalFormatOutput.put("monospace", true);
        internationalFormatOutput.put("buttons", List.of("copy"));
        section2OutputFields.add(internationalFormatOutput);

        Map<String, Object> nationalFormatOutput = new HashMap<>();
        nationalFormatOutput.put("title", "National");
        nationalFormatOutput.put("name", "nationalFormat");
        nationalFormatOutput.put("type", "text");
        nationalFormatOutput.put("monospace", true);
        nationalFormatOutput.put("buttons", List.of("copy"));
        section2OutputFields.add(nationalFormatOutput);

        Map<String, Object> e164FormatOutput = new HashMap<>();
        e164FormatOutput.put("title", "E.164");
        e164FormatOutput.put("name", "e164Format");
        e164FormatOutput.put("type", "text");
        e164FormatOutput.put("monospace", true);
        e164FormatOutput.put("buttons", List.of("copy"));
        section2OutputFields.add(e164FormatOutput);

        Map<String, Object> rfc3966FormatOutput = new HashMap<>();
        rfc3966FormatOutput.put("title", "RFC3966 (URI)");
        rfc3966FormatOutput.put("name", "rfc3966Format");
        rfc3966FormatOutput.put("type", "text");
        rfc3966FormatOutput.put("monospace", true);
        rfc3966FormatOutput.put("buttons", List.of("copy"));
        section2OutputFields.add(rfc3966FormatOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Additional Info
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Additional Information");
        outputSection3.put("condition", "success && operation === 'parse' && (location || carrier || extension)");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Location information
        Map<String, Object> locationOutput = new HashMap<>();
        locationOutput.put("title", "Location");
        locationOutput.put("name", "location");
        locationOutput.put("type", "text");
        locationOutput.put("condition", "location");
        section3OutputFields.add(locationOutput);

        // Carrier information
        Map<String, Object> carrierOutput = new HashMap<>();
        carrierOutput.put("title", "Carrier");
        carrierOutput.put("name", "carrier");
        carrierOutput.put("type", "text");
        carrierOutput.put("condition", "carrier");
        section3OutputFields.add(carrierOutput);

        // Extension information
        Map<String, Object> extensionOutput = new HashMap<>();
        extensionOutput.put("title", "Extension");
        extensionOutput.put("name", "extension");
        extensionOutput.put("type", "text");
        extensionOutput.put("condition", "extension");
        section3OutputFields.add(extensionOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        // Output Section 4: Example Number Result
        Map<String, Object> outputSection4 = new HashMap<>();
        outputSection4.put("header", "Example Number");
        outputSection4.put("condition", "success && operation === 'getExampleNumber'");
        List<Map<String, Object>> section4OutputFields = new ArrayList<>();

        // Country and type information
        Map<String, Object> exampleCountryOutput = new HashMap<>();
        exampleCountryOutput.put("title", "Country");
        exampleCountryOutput.put("name", "countryName");
        exampleCountryOutput.put("type", "text");
        section4OutputFields.add(exampleCountryOutput);

        Map<String, Object> exampleTypeOutput = new HashMap<>();
        exampleTypeOutput.put("title", "Number Type");
        exampleTypeOutput.put("name", "typeName");
        exampleTypeOutput.put("type", "text");
        section4OutputFields.add(exampleTypeOutput);

        // Example number in different formats
        Map<String, Object> exampleInternationalOutput = new HashMap<>();
        exampleInternationalOutput.put("title", "Example Number");
        exampleInternationalOutput.put("name", "internationalFormat");
        exampleInternationalOutput.put("type", "text");
        exampleInternationalOutput.put("buttons", List.of("copy"));
        section4OutputFields.add(exampleInternationalOutput);

        Map<String, Object> exampleNationalOutput = new HashMap<>();
        exampleNationalOutput.put("title", "National Format");
        exampleNationalOutput.put("name", "nationalFormat");
        exampleNationalOutput.put("type", "text");
        exampleNationalOutput.put("buttons", List.of("copy"));
        section4OutputFields.add(exampleNationalOutput);

        outputSection4.put("fields", section4OutputFields);
        uiOutputs.add(outputSection4);

        // Output Section 5: Supported Regions (for getSupportedRegions operation)
        Map<String, Object> outputSection5 = new HashMap<>();
        outputSection5.put("header", "Supported Countries");
        outputSection5.put("condition", "success && operation === 'getSupportedRegions'");
        List<Map<String, Object>> section5OutputFields = new ArrayList<>();

        // Country count
        Map<String, Object> countOutput = new HashMap<>();
        countOutput.put("title", "Total Countries");
        countOutput.put("name", "count");
        countOutput.put("type", "text");
        section5OutputFields.add(countOutput);

        // Country list
        Map<String, Object> regionListOutput = new HashMap<>();
        regionListOutput.put("name", "regions");
        regionListOutput.put("type", "table");
        List<Map<String, Object>> regionColumns = new ArrayList<>();
        regionColumns.add(Map.of("header", "Code", "field", "code"));
        regionColumns.add(Map.of("header", "Country", "field", "name"));
        regionColumns.add(Map.of("header", "Calling Code", "field", "callingCodeDisplay", "formula", "'+' + callingCode"));
        regionListOutput.put("columns", regionColumns);
        section5OutputFields.add(regionListOutput);

        outputSection5.put("fields", section5OutputFields);
        uiOutputs.add(outputSection5);

        // Output Section 6: Error Display
        Map<String, Object> outputSection6 = new HashMap<>();
        outputSection6.put("header", "Error Information");
        outputSection6.put("condition", "error");
        List<Map<String, Object>> section6OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error Message");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section6OutputFields.add(errorOutput);

        // Error type for parsing errors
        Map<String, Object> errorTypeOutput = new HashMap<>();
        errorTypeOutput.put("title", "Error Type");
        errorTypeOutput.put("name", "errorType");
        errorTypeOutput.put("type", "text");
        errorTypeOutput.put("style", "error");
        errorTypeOutput.put("condition", "errorType");
        section6OutputFields.add(errorTypeOutput);

        outputSection6.put("fields", section6OutputFields);
        uiOutputs.add(outputSection6);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "parse");

            return switch (operation.toLowerCase()) {
                case "parse" -> parsePhoneNumber(input);
                case "format" -> formatPhoneNumber(input);
                case "getsupportedregions" -> getSupportedRegions();
                case "getexamplenumber" -> getExampleNumber(input);
                default -> {
                    result.put("error", "Unsupported operation: " + operation);
                    yield result;
                }
            };

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Parse and validate a phone number
     *
     * @param input Input parameters
     * @return Parsing result
     */
    private Map<String, Object> parsePhoneNumber(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String phoneNumberStr = (String) input.get("phoneNumber");
            String defaultRegion = (String) input.getOrDefault("defaultRegion", "US");

            if (phoneNumberStr == null || phoneNumberStr.trim().isEmpty()) {
                result.put("error", "Phone number cannot be empty");
                return result;
            }

            // Try to parse the phone number
            PhoneNumber parsedNumber;
            try {
                parsedNumber = phoneUtil.parse(phoneNumberStr, defaultRegion);
            } catch (NumberParseException e) {
                result.put("error", "Could not parse phone number: " + e.getMessage());
                result.put("errorType", e.getErrorType().toString());

                // Try to provide partial information even though parsing failed
                result.put("phoneNumber", phoneNumberStr);
                result.put("defaultRegion", defaultRegion);
                result.put("isValid", false);
                result.put("isPossible", false);

                return result;
            }

            // Get country information
            String regionCode = phoneUtil.getRegionCodeForNumber(parsedNumber);

            // Check if the number is valid for the specified region
            boolean isValid = phoneUtil.isValidNumber(parsedNumber);
            boolean isPossible = phoneUtil.isPossibleNumber(parsedNumber);

            // Get number type
            PhoneNumberType numberType = phoneUtil.getNumberType(parsedNumber);

            // Format the number in different formats
            String internationalFormat = phoneUtil.format(parsedNumber, PhoneNumberFormat.INTERNATIONAL);
            String nationalFormat = phoneUtil.format(parsedNumber, PhoneNumberFormat.NATIONAL);
            String e164Format = phoneUtil.format(parsedNumber, PhoneNumberFormat.E164);
            String rfc3966Format = phoneUtil.format(parsedNumber, PhoneNumberFormat.RFC3966);

            // Get location information
            String location = geocoder.getDescriptionForNumber(parsedNumber, Locale.ENGLISH);

            // Get carrier information
            String carrier = carrierMapper.getNameForNumber(parsedNumber, Locale.ENGLISH);

            // Build the result
            result.put("success", true);
            result.put("phoneNumber", phoneNumberStr);
            result.put("parsedNumber", String.valueOf(parsedNumber));
            result.put("countryCode", regionCode);
            result.put("countryName", getCountryDisplayName(regionCode));
            result.put("callingCode", parsedNumber.getCountryCode());
            result.put("nationalNumber", parsedNumber.getNationalNumber());
            result.put("isValid", isValid);
            result.put("isPossible", isPossible);
            result.put("numberType", numberType.toString());
            result.put("numberTypeName", formatNumberType(numberType));

            result.put("internationalFormat", internationalFormat);
            result.put("nationalFormat", nationalFormat);
            result.put("e164Format", e164Format);
            result.put("rfc3966Format", rfc3966Format);

            if (location != null && !location.isEmpty()) {
                result.put("location", location);
            }

            if (carrier != null && !carrier.isEmpty()) {
                result.put("carrier", carrier);
            }

            // Additional metadata
            if (parsedNumber.hasExtension() && !parsedNumber.getExtension().isEmpty()) {
                result.put("extension", parsedNumber.getExtension());
            }

        } catch (Exception e) {
            result.put("error", "Error parsing phone number: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Format a phone number in a specific format
     *
     * @param input Input parameters
     * @return Formatted number
     */
    private Map<String, Object> formatPhoneNumber(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String phoneNumberStr = (String) input.get("phoneNumber");
            String defaultRegion = (String) input.getOrDefault("defaultRegion", "US");
            String format = (String) input.getOrDefault("format", "INTERNATIONAL");

            if (phoneNumberStr == null || phoneNumberStr.trim().isEmpty()) {
                result.put("error", "Phone number cannot be empty");
                return result;
            }

            // Try to parse the phone number
            PhoneNumber parsedNumber;
            try {
                parsedNumber = phoneUtil.parse(phoneNumberStr, defaultRegion);
            } catch (NumberParseException e) {
                result.put("error", "Could not parse phone number: " + e.getMessage());
                return result;
            }

            // Determine the format to use
            PhoneNumberFormat phoneNumberFormat = switch (format.toUpperCase()) {
                case "NATIONAL" -> PhoneNumberFormat.NATIONAL;
                case "E164" -> PhoneNumberFormat.E164;
                case "RFC3966" -> PhoneNumberFormat.RFC3966;
                default -> PhoneNumberFormat.INTERNATIONAL;
            };

            // Format the number
            String formattedNumber = phoneUtil.format(parsedNumber, phoneNumberFormat);

            // Build the result
            result.put("success", true);
            result.put("phoneNumber", phoneNumberStr);
            result.put("formattedNumber", formattedNumber);
            result.put("format", format);
            result.put("isValid", phoneUtil.isValidNumber(parsedNumber));

        } catch (Exception e) {
            result.put("error", "Error formatting phone number: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get a list of supported regions with their country codes
     *
     * @return Supported regions
     */
    private Map<String, Object> getSupportedRegions() {
        Map<String, Object> result = new HashMap<>();

        try {
            Set<String> regions = phoneUtil.getSupportedRegions();

            // Create a list of regions with details
            List<Map<String, Object>> regionList = new ArrayList<>();

            for (String regionCode : regions) {
                Map<String, Object> regionInfo = new HashMap<>();
                regionInfo.put("code", regionCode);
                regionInfo.put("name", getCountryDisplayName(regionCode));
                regionInfo.put("callingCode", phoneUtil.getCountryCodeForRegion(regionCode));

                regionList.add(regionInfo);
            }

            // Sort by country name
            regionList.sort((r1, r2) -> {
                String name1 = (String) r1.get("name");
                String name2 = (String) r2.get("name");
                return name1.compareToIgnoreCase(name2);
            });

            // Group regions by calling code
            Map<Integer, List<Map<String, Object>>> regionsByCallingCode = new TreeMap<>();

            for (Map<String, Object> regionInfo : regionList) {
                int callingCode = (Integer) regionInfo.get("callingCode");

                if (!regionsByCallingCode.containsKey(callingCode)) {
                    regionsByCallingCode.put(callingCode, new ArrayList<>());
                }

                regionsByCallingCode.get(callingCode).add(regionInfo);
            }

            result.put("success", true);
            result.put("regions", regionList);
            result.put("regionsByCallingCode", regionsByCallingCode);
            result.put("count", regions.size());

        } catch (Exception e) {
            result.put("error", "Error getting supported regions: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get an example phone number for a region and type
     *
     * @param input Input parameters
     * @return Example phone number
     */
    private Map<String, Object> getExampleNumber(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String region = (String) input.get("region");
            String type = (String) input.getOrDefault("type", "MOBILE");

            if (region == null || region.trim().isEmpty()) {
                result.put("error", "Region code cannot be empty");
                return result;
            }

            // Determine the phone number type
            PhoneNumberType numberType = PhoneNumberType.MOBILE;
            try {
                numberType = PhoneNumberType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to MOBILE if the type is not recognized
            }

            // Get an example number
            PhoneNumber exampleNumber = phoneUtil.getExampleNumberForType(region, numberType);

            if (exampleNumber == null) {
                // Try to get a general example number if type-specific example is not available
                exampleNumber = phoneUtil.getExampleNumber(region);
            }

            if (exampleNumber == null) {
                result.put("error", "No example number available for " + region + " of type " + type);
                return result;
            }

            // Format the number in different formats
            String internationalFormat = phoneUtil.format(exampleNumber, PhoneNumberFormat.INTERNATIONAL);
            String nationalFormat = phoneUtil.format(exampleNumber, PhoneNumberFormat.NATIONAL);
            String e164Format = phoneUtil.format(exampleNumber, PhoneNumberFormat.E164);

            result.put("success", true);
            result.put("region", region);
            result.put("countryName", getCountryDisplayName(region));
            result.put("type", numberType.toString());
            result.put("typeName", formatNumberType(numberType));
            result.put("internationalFormat", internationalFormat);
            result.put("nationalFormat", nationalFormat);
            result.put("e164Format", e164Format);

        } catch (Exception e) {
            result.put("error", "Error getting example number: " + e.getMessage());
        }

        return result;
    }

    /**
     * Format the phone number type to a human-readable string
     *
     * @param type PhoneNumberType
     * @return Formatted type name
     */
    private String formatNumberType(PhoneNumberType type) {
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
            case UAN -> "UAN (Universal Access Number)";
            case VOICEMAIL -> "Voicemail";
            default -> "Unknown";
        };
    }

    /**
     * Get country display name from region code
     *
     * @param regionCode Region code
     * @return Country display name
     */
    private String getCountryDisplayName(String regionCode) {
        if (regionCode == null) {
            return "Unknown";
        }

        return COUNTRY_DISPLAY_NAMES.getOrDefault(regionCode, regionCode);
    }
}