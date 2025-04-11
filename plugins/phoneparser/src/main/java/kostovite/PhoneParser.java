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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse, validate and format phone numbers");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Parse phone number operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse and validate phone numbers");

        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("phoneNumber", "Phone number to parse");
        parseInputs.put("defaultRegion", "Default country code (ISO 3166-1 alpha-2) to use for parsing");

        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        // Format phone number operation
        Map<String, Object> formatOperation = new HashMap<>();
        formatOperation.put("description", "Format phone numbers");

        Map<String, Object> formatInputs = new HashMap<>();
        formatInputs.put("phoneNumber", "Phone number to format");
        formatInputs.put("defaultRegion", "Default country code (ISO 3166-1 alpha-2) to use for parsing");
        formatInputs.put("format", "Format type (INTERNATIONAL, NATIONAL, E164, RFC3966)");

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
        exampleInputs.put("region", "Region code (ISO 3166-1 alpha-2)");
        exampleInputs.put("type", "Phone number type (MOBILE, FIXED_LINE, etc.)");

        exampleOperation.put("inputs", exampleInputs);
        operations.put("getExampleNumber", exampleOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "parse");

            switch (operation.toLowerCase()) {
                case "parse":
                    return parsePhoneNumber(input);
                case "format":
                    return formatPhoneNumber(input);
                case "getsupportedregions":
                    return getSupportedRegions();
                case "getexamplenumber":
                    return getExampleNumber(input);
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
            PhoneNumberFormat phoneNumberFormat;
            switch (format.toUpperCase()) {
                case "NATIONAL":
                    phoneNumberFormat = PhoneNumberFormat.NATIONAL;
                    break;
                case "E164":
                    phoneNumberFormat = PhoneNumberFormat.E164;
                    break;
                case "RFC3966":
                    phoneNumberFormat = PhoneNumberFormat.RFC3966;
                    break;
                case "INTERNATIONAL":
                default:
                    phoneNumberFormat = PhoneNumberFormat.INTERNATIONAL;
                    break;
            }

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
        switch (type) {
            case FIXED_LINE:
                return "Fixed Line";
            case MOBILE:
                return "Mobile";
            case FIXED_LINE_OR_MOBILE:
                return "Fixed Line or Mobile";
            case TOLL_FREE:
                return "Toll Free";
            case PREMIUM_RATE:
                return "Premium Rate";
            case SHARED_COST:
                return "Shared Cost";
            case VOIP:
                return "VoIP";
            case PERSONAL_NUMBER:
                return "Personal Number";
            case PAGER:
                return "Pager";
            case UAN:
                return "UAN (Universal Access Number)";
            case VOICEMAIL:
                return "Voicemail";
            case UNKNOWN:
            default:
                return "Unknown";
        }
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