package kostovite;

import java.util.*;
import java.math.BigInteger;
import java.util.regex.Pattern;

public class IBANValidator implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";
    private static final String DEFAULT_IBAN = "FR7630006000011234567890189";

    // Country formats and lengths based on ISO standard
    private static final Map<String, Integer> COUNTRY_FORMATS = new HashMap<>();
    static {
        COUNTRY_FORMATS.put("AD", 24); // Andorra
        COUNTRY_FORMATS.put("AE", 23); // United Arab Emirates
        COUNTRY_FORMATS.put("AL", 28); // Albania
        COUNTRY_FORMATS.put("AT", 20); // Austria
        COUNTRY_FORMATS.put("AZ", 28); // Azerbaijan
        COUNTRY_FORMATS.put("BA", 20); // Bosnia and Herzegovina
        COUNTRY_FORMATS.put("BE", 16); // Belgium
        COUNTRY_FORMATS.put("BG", 22); // Bulgaria
        COUNTRY_FORMATS.put("BH", 22); // Bahrain
        COUNTRY_FORMATS.put("BR", 29); // Brazil
        COUNTRY_FORMATS.put("BY", 28); // Belarus
        COUNTRY_FORMATS.put("CH", 21); // Switzerland
        COUNTRY_FORMATS.put("CR", 22); // Costa Rica
        COUNTRY_FORMATS.put("CY", 28); // Cyprus
        COUNTRY_FORMATS.put("CZ", 24); // Czech Republic
        COUNTRY_FORMATS.put("DE", 22); // Germany
        COUNTRY_FORMATS.put("DK", 18); // Denmark
        COUNTRY_FORMATS.put("DO", 28); // Dominican Republic
        COUNTRY_FORMATS.put("EE", 20); // Estonia
        COUNTRY_FORMATS.put("ES", 24); // Spain
        COUNTRY_FORMATS.put("FI", 18); // Finland
        COUNTRY_FORMATS.put("FO", 18); // Faroe Islands
        COUNTRY_FORMATS.put("FR", 27); // France
        COUNTRY_FORMATS.put("GB", 22); // United Kingdom
        COUNTRY_FORMATS.put("GE", 22); // Georgia
        COUNTRY_FORMATS.put("GI", 23); // Gibraltar
        COUNTRY_FORMATS.put("GL", 18); // Greenland
        COUNTRY_FORMATS.put("GR", 27); // Greece
        COUNTRY_FORMATS.put("GT", 28); // Guatemala
        COUNTRY_FORMATS.put("HR", 21); // Croatia
        COUNTRY_FORMATS.put("HU", 28); // Hungary
        COUNTRY_FORMATS.put("IE", 22); // Ireland
        COUNTRY_FORMATS.put("IL", 23); // Israel
        COUNTRY_FORMATS.put("IQ", 23); // Iraq
        COUNTRY_FORMATS.put("IS", 26); // Iceland
        COUNTRY_FORMATS.put("IT", 27); // Italy
        COUNTRY_FORMATS.put("JO", 30); // Jordan
        COUNTRY_FORMATS.put("KW", 30); // Kuwait
        COUNTRY_FORMATS.put("KZ", 20); // Kazakhstan
        COUNTRY_FORMATS.put("LB", 28); // Lebanon
        COUNTRY_FORMATS.put("LC", 32); // Saint Lucia
        COUNTRY_FORMATS.put("LI", 21); // Liechtenstein
        COUNTRY_FORMATS.put("LT", 20); // Lithuania
        COUNTRY_FORMATS.put("LU", 20); // Luxembourg
        COUNTRY_FORMATS.put("LV", 21); // Latvia
        COUNTRY_FORMATS.put("MC", 27); // Monaco
        COUNTRY_FORMATS.put("MD", 24); // Moldova
        COUNTRY_FORMATS.put("ME", 22); // Montenegro
        COUNTRY_FORMATS.put("MK", 19); // Macedonia
        COUNTRY_FORMATS.put("MR", 27); // Mauritania
        COUNTRY_FORMATS.put("MT", 31); // Malta
        COUNTRY_FORMATS.put("MU", 30); // Mauritius
        COUNTRY_FORMATS.put("NL", 18); // Netherlands
        COUNTRY_FORMATS.put("NO", 15); // Norway
        COUNTRY_FORMATS.put("PK", 24); // Pakistan
        COUNTRY_FORMATS.put("PL", 28); // Poland
        COUNTRY_FORMATS.put("PS", 29); // Palestinian territories
        COUNTRY_FORMATS.put("PT", 25); // Portugal
        COUNTRY_FORMATS.put("QA", 29); // Qatar
        COUNTRY_FORMATS.put("RO", 24); // Romania
        COUNTRY_FORMATS.put("RS", 22); // Serbia
        COUNTRY_FORMATS.put("SA", 24); // Saudi Arabia
        COUNTRY_FORMATS.put("SC", 31); // Seychelles
        COUNTRY_FORMATS.put("SE", 24); // Sweden
        COUNTRY_FORMATS.put("SI", 19); // Slovenia
        COUNTRY_FORMATS.put("SK", 24); // Slovakia
        COUNTRY_FORMATS.put("SM", 27); // San Marino
        COUNTRY_FORMATS.put("ST", 25); // Sao Tome and Principe
        COUNTRY_FORMATS.put("SV", 28); // El Salvador
        COUNTRY_FORMATS.put("TL", 23); // Timor-Leste
        COUNTRY_FORMATS.put("TN", 24); // Tunisia
        COUNTRY_FORMATS.put("TR", 26); // Turkey
        COUNTRY_FORMATS.put("UA", 29); // Ukraine
        COUNTRY_FORMATS.put("VA", 22); // Vatican City
        COUNTRY_FORMATS.put("VG", 24); // Virgin Islands, British
        COUNTRY_FORMATS.put("XK", 20); // Kosovo
    }

    // IBAN validation pattern - no spaces, only alphanumeric
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$");

    @Override
    public String getName() {
        return "IBANValidator";
    }

    @Override
    public void execute() {
        System.out.println("IBANValidator Plugin executed (standalone test)");
        try {
            // Test with default IBAN (France)
            Map<String, Object> params = new HashMap<>();
            params.put("iban", DEFAULT_IBAN);
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Valid French IBAN): " + result1);

            // Test with German IBAN
            params.put("iban", "DE89370400440532013000");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (Valid German IBAN): " + result2);

            // Test with Swiss QR-IBAN
            params.put("iban", "CH4430000000123456789");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Swiss IBAN): " + result3);

            // Test with invalid IBAN (wrong checksum)
            params.put("iban", "FR7630006000011234567890180");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid checksum): " + result4);

            // Test with invalid IBAN (wrong length)
            params.put("iban", "GB82WEST12345698765432");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Invalid length): " + result5);

            // Test with invalid country code
            params.put("iban", "ZZ89370400440532013000");
            Map<String, Object> result6 = process(params);
            System.out.println("Test 6 (Invalid country code): " + result6);

            // Test with empty input
            params.put("iban", "");
            Map<String, Object> result7 = process(params);
            System.out.println("Test 7 (Empty input): " + result7);

            // Test with formatted IBAN (spaces)
            params.put("iban", "FR76 3000 6000 0112 3456 7890 189");
            Map<String, Object> result8 = process(params);
            System.out.println("Test 8 (Formatted with spaces): " + result8);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("customUI", false);
        metadata.put("name", "IBAN validator and parser");
        metadata.put("icon", "AccountBalance");
        metadata.put("description", "Validate and parse IBAN numbers. Check if an IBAN is valid and get the country, BBAN, if it is a QR-IBAN and the IBAN friendly format.");
        metadata.put("id", "IBANValidator");
        metadata.put("category", "Data");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: IBAN Validator ---
        Map<String, Object> ibanSection = new HashMap<>();
        ibanSection.put("id", "ibanValidator");
        ibanSection.put("label", "");

        // --- Inputs ---
        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("label", ""),
                Map.entry("id", "iban"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "Enter your IBAN"),
                Map.entry("required", true),
                Map.entry("default", DEFAULT_IBAN),
                Map.entry("containerId", "input"),
                Map.entry("width", 600),
                Map.entry("height", 36)
        ));
        ibanSection.put("inputs", inputs);

        // --- Outputs ---
        List<Map<String, Object>> outputs = new ArrayList<>();

        // Is IBAN valid output
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 400),
                Map.entry("id", "isValidIBAN"),
                Map.entry("label", "Is IBAN valid?"),
                Map.entry("monospace", true),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80)
        ));

        // Is QR-IBAN output
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 400),
                Map.entry("id", "isQRIBAN"),
                Map.entry("label", "Is IBAN a QR-IBAN?"),
                Map.entry("monospace", true),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80)
        ));

        // Country code output
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 400),
                Map.entry("id", "country"),
                Map.entry("label", "Country code"),
                Map.entry("monospace", true),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80)
        ));

        // BBAN output
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 400),
                Map.entry("id", "bban"),
                Map.entry("label", "BBAN"),
                Map.entry("monospace", true),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80)
        ));

        // IBAN friendly format output
        outputs.add(Map.ofEntries(
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("buttons", List.of("copy")),
                Map.entry("width", 400),
                Map.entry("id", "ibanFriendlyFormat"),
                Map.entry("label", "IBAN friendly format"),
                Map.entry("monospace", true),
                Map.entry("type", "text"),
                Map.entry("containerId", "output"),
                Map.entry("height", 80)
        ));

        ibanSection.put("outputs", outputs);

        sections.add(ibanSection);

        // --- Error Section ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", ERROR_OUTPUT_ID),
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error")
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);

        metadata.put("sections", sections);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            // Get the IBAN from input
            String iban = getStringParam(input, "iban", "");

            // Validation
            if (iban.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Please enter an IBAN.");
            }

            // Clean and normalize IBAN (remove spaces and convert to uppercase)
            String cleanIban = cleanIban(iban);

            // Parse IBAN
            IbanInfo ibanInfo = parseIban(cleanIban);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("isValidIBAN", ibanInfo.isValid ? "Yes" : "No");
            result.put("isQRIBAN", ibanInfo.isQrIban ? "Yes" : "No");
            result.put("country", ibanInfo.countryCode);
            result.put("bban", ibanInfo.bban);
            result.put("ibanFriendlyFormat", ibanInfo.friendlyFormat);

            return result;

        } catch (Exception e) {
            System.err.println("Error validating IBAN: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Clean and normalize IBAN
    private String cleanIban(String iban) {
        // Remove spaces and convert to uppercase
        return iban.replaceAll("\\s", "").toUpperCase();
    }

    // Parse and validate IBAN
    private IbanInfo parseIban(String iban) {
        IbanInfo info = new IbanInfo();

        // Check if IBAN is empty
        if (iban.isEmpty()) {
            info.isValid = false;
            return info;
        }

        // Check basic format
        if (!IBAN_PATTERN.matcher(iban).matches()) {
            info.isValid = false;
            return info;
        }

        // Extract country code
        info.countryCode = iban.substring(0, 2);

        // Check if country code is supported
        if (!COUNTRY_FORMATS.containsKey(info.countryCode)) {
            info.isValid = false;
            return info;
        }

        // Check length for country
        int expectedLength = COUNTRY_FORMATS.get(info.countryCode);
        if (iban.length() != expectedLength) {
            info.isValid = false;
            return info;
        }

        // Extract check digits and BBAN (Basic Bank Account Number)
        String checkDigits = iban.substring(2, 4);
        info.bban = iban.substring(4);

        // Validate using MOD 97-10 algorithm (ISO 7064)
        info.isValid = validateMod97(iban);

        // Check if it's a QR-IBAN (specific for Switzerland)
        info.isQrIban = isQrIban(iban);

        // Format IBAN in a friendly way (groups of 4)
        info.friendlyFormat = formatIban(iban);

        return info;
    }

    // MOD 97-10 validation (ISO 7064 standard)
    private boolean validateMod97(String iban) {
        // Move first 4 characters to the end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Convert letters to numbers (A=10, B=11, ..., Z=35)
        StringBuilder numericIban = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericIban.append(Character.getNumericValue(c));
            } else {
                numericIban.append(c);
            }
        }

        // Calculate modulo
        try {
            BigInteger ibanNumber = new BigInteger(numericIban.toString());
            return ibanNumber.mod(BigInteger.valueOf(97)).equals(BigInteger.ONE);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Check if IBAN is a QR-IBAN (specific for Switzerland)
    private boolean isQrIban(String iban) {
        // QR-IBANs are specific to Switzerland and have a special pattern
        // in the first 3 positions of the BBAN (positions 5-7 of the IBAN)
        if ("CH".equals(iban.substring(0, 2)) && iban.length() >= 7) {
            // QR-IBANs have values between 30000 and 31999 in these positions
            try {
                int qrValue = Integer.parseInt(iban.substring(4, 9));
                return qrValue >= 30000 && qrValue <= 31999;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    // Format IBAN in a friendly way (groups of 4 characters)
    private String formatIban(String iban) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < iban.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(iban.charAt(i));
        }
        return formatted.toString();
    }

    // Class to hold IBAN information
    private class IbanInfo {
        boolean isValid = false;
        boolean isQrIban = false;
        String countryCode = "";
        String bban = "";
        String friendlyFormat = "";
    }

    // Helper method to get string parameters
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
}