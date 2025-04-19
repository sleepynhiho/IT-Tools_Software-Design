package kostovite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// For stream operations

import net.sf.uadetector.*; // Import base package
import net.sf.uadetector.service.UADetectorServiceFactory; // Import factory

// Assuming PluginInterface is standard
public class UserAgentParser implements PluginInterface {

    // UADetector parser instance
    private final UserAgentStringParser parser;

    // Regular expressions for more specific browser/engine version details
    private static final Pattern CHROME_VERSION_PATTERN = Pattern.compile("(?:Chrome|CriOS)/([\\d.]+)");
    private static final Pattern EDGE_VERSION_PATTERN = Pattern.compile("Edg(?:e|A|iOS)?/([\\d.]+)");
    private static final Pattern SAFARI_VERSION_PATTERN = Pattern.compile("Version/([\\d.]+)");
    private static final Pattern FIREFOX_VERSION_PATTERN = Pattern.compile("(?:Firefox|FxiOS)/([\\d.]+)");
    private static final Pattern OPERA_VERSION_PATTERN = Pattern.compile("(?:OPR|Opera|OPT)/([\\d.]+)");
    private static final Pattern SAMSUNG_VERSION_PATTERN = Pattern.compile("SamsungBrowser/([\\d.]+)");
    private static final Pattern WEBKIT_VERSION_PATTERN = Pattern.compile("AppleWebKit/([\\d.]+)");
    private static final Pattern GECKO_VERSION_PATTERN = Pattern.compile("Gecko/(\\d{8,})");
    private static final Pattern TRIDENT_VERSION_PATTERN = Pattern.compile("Trident/([\\d.]+)");
    private static final Pattern IE_VERSION_PATTERN = Pattern.compile("(?:MSIE |rv:)([\\d.]+)");


    public UserAgentParser() {
        // Initialize with the standard resource module parser
        this.parser = UADetectorServiceFactory.getResourceModuleParser();
        System.out.println("UserAgentParser initialized with UADetector version: " + parser.getDataVersion());
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "UserAgentParser";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("User Agent Parser Plugin executed (standalone test)");
        try {
            String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1";
            Map<String, Object> params = new HashMap<>();
            params.put("userAgentInput", userAgent); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Test Parse Result: " + result);

            userAgent = "Mozilla/5.0 (Linux; Android 13; SM-S908E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36";
            params.put("userAgentInput", userAgent);
            result = process(params);
            System.out.println("Test Parse Result 2: " + result);


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
        metadata.put("id", "UserAgentParser");
        metadata.put("name", "User Agent Parser");
        metadata.put("description", "Analyze User Agent strings to identify browser, OS, engine, and device type.");
        metadata.put("icon", "Language"); // Alternate icon suggestion
        metadata.put("category", "Web Tools");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", true); // Analyze dynamically

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "input");
        inputSection.put("label", "User Agent Input");

        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "userAgentInput"), // Use ID
                Map.entry("label", "Paste User Agent String:"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 4),
                Map.entry("placeholder", "e.g., Mozilla/5.0 (Windows NT 10.0...)"),
                Map.entry("required", false), // Allow empty string for initial state
                Map.entry("monospace", true),
                Map.entry("helperText", "Results update automatically as you type.")
        ));
        // Omit "Use Current UA" button and Examples select - requires frontend implementation
        inputSection.put("inputs", inputs);
        sections.add(inputSection);


        // --- Section 2: Parsed Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Parsed Information");
        resultsSection.put("condition", "success === true && !isEmpty"); // Show only on success and non-empty input

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Browser Info
        resultOutputs.add(createOutputField("browserName", "Browser Name", "text", null));
        resultOutputs.add(createOutputField("browserVersion", "Browser Version", "text", null));
        resultOutputs.add(createOutputField("browserType", "Browser Type", "text", null));
        resultOutputs.add(createOutputField("browserFamily", "Browser Family", "text", null));
        resultOutputs.add(createOutputField("browserProducer", "Browser Vendor", "text", null));

        // Engine Info
        resultOutputs.add(createOutputField("engineName", "Rendering Engine", "text", null));
        resultOutputs.add(createOutputField("engineVersion", "Engine Version", "text", null));

        // OS Info
        resultOutputs.add(createOutputField("osName", "Operating System", "text", null));
        resultOutputs.add(createOutputField("osVersion", "OS Version", "text", null));
        resultOutputs.add(createOutputField("osFamily", "OS Family", "text", null));
        resultOutputs.add(createOutputField("osProducer", "OS Vendor", "text", null));

        // Device Info
        resultOutputs.add(createOutputField("deviceType", "Device Type", "text", null));
        resultOutputs.add(createOutputField("deviceCategory", "Device Category", "text", null));
        // Device Model/Vendor less reliable from this library, omit specific fields
        resultOutputs.add(createOutputField("cpuArchitecture", "CPU Architecture", "text", null));

        // Capabilities (Boolean flags for frontend checkmark rendering)
        resultOutputs.add(createOutputField("isMobile", "Mobile?", "boolean", null));
        resultOutputs.add(createOutputField("isTablet", "Tablet?", "boolean", null));
        resultOutputs.add(createOutputField("isDesktop", "Desktop?", "boolean", null));
        resultOutputs.add(createOutputField("isBot", "Bot/Crawler?", "boolean", null));

        // Raw UA Echo
        Map<String, Object> rawUaOutput = createOutputField("rawUserAgent", "Full User Agent", "text", null);
        rawUaOutput.put("multiline", true);
        rawUaOutput.put("rows", 2);
        rawUaOutput.put("buttons", List.of("copy"));
        resultOutputs.add(rawUaOutput);


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 3: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Show only on failure

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
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
        if ("text".equals(type) && (id.toLowerCase().contains("version") || id.toLowerCase().contains("agent"))) {
            field.put("monospace", true);
        }
        return field;
    }

    /**
     * Processes the input User Agent string (using IDs from the new format).
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage";
        try {
            String userAgentInput = getStringParam(input); // Use new ID

            // Perform analysis (handle empty string inside)
            return parseUserAgent(userAgentInput);

        } catch (IllegalArgumentException e) {
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing UA parse request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error during parsing.");
        }
    }

    // ========================================================================
    // Private Parsing Methods
    // ========================================================================

    /**
     * Parse a user agent string and extract detailed information.
     * Returns results keyed by the NEW output field IDs.
     */
    private Map<String, Object> parseUserAgent(String userAgentString) {
        Map<String, Object> result = new LinkedHashMap<>(); // Preserve order
        String errorOutputId = "errorMessage";

        if (userAgentString == null || userAgentString.trim().isEmpty()) {
            result.put("success", true);
            result.put("isEmpty", true);
            // Provide default/empty values for output fields
            result.put("browserName", "N/A");
            result.put("browserVersion", "");
            result.put("browserType", "N/A");
            result.put("browserFamily", "N/A");
            result.put("browserProducer", "N/A");
            result.put("engineName", "N/A");
            result.put("engineVersion", "");
            result.put("osName", "N/A");
            result.put("osVersion", "");
            result.put("osFamily", "N/A");
            result.put("osProducer", "N/A");
            result.put("deviceType", "N/A");
            result.put("deviceCategory", "N/A");
            result.put("cpuArchitecture", "N/A");
            result.put("isMobile", false);
            result.put("isTablet", false);
            result.put("isDesktop", false);
            result.put("isBot", false);
            result.put("rawUserAgent", "");
            return result;
        }

        try {
            // --- Parse using UADetector ---
            ReadableUserAgent ua = parser.parse(userAgentString);

            // --- Basic Info from Library ---
            String libBrowserName = ua.getName();
            String libBrowserType = ua.getTypeName(); // Use descriptive name
            String libBrowserFamily = ua.getFamily().getName();
            String libBrowserProducer = ua.getProducer();
            OperatingSystem os = ua.getOperatingSystem(); // Get OS object
            String libOsName = os.getName();
            String libOsFamily = os.getFamilyName();
            String libOsProducer = os.getProducer();
            DeviceCategory deviceCat = (DeviceCategory) ua.getDeviceCategory(); // Get DeviceCategory object
            String libDeviceCategory = deviceCat.getName();

            // --- Refine Browser Info with Regex ---
            String refinedBrowserName = libBrowserName;
            String refinedBrowserVersion = formatVersion(ua.getVersionNumber());

            Matcher edgeMatcher = EDGE_VERSION_PATTERN.matcher(userAgentString);
            Matcher chromeMatcher = CHROME_VERSION_PATTERN.matcher(userAgentString);
            Matcher firefoxMatcher = FIREFOX_VERSION_PATTERN.matcher(userAgentString);
            Matcher safariMatcher = SAFARI_VERSION_PATTERN.matcher(userAgentString);
            Matcher operaMatcher = OPERA_VERSION_PATTERN.matcher(userAgentString);
            Matcher samsungMatcher = SAMSUNG_VERSION_PATTERN.matcher(userAgentString);
            Matcher ieMatcher = IE_VERSION_PATTERN.matcher(userAgentString);

            if (edgeMatcher.find()) { refinedBrowserName = "Edge"; refinedBrowserVersion = edgeMatcher.group(1); }
            else if (chromeMatcher.find()) { refinedBrowserName = "Chrome"; refinedBrowserVersion = chromeMatcher.group(1); }
            else if (firefoxMatcher.find()) { refinedBrowserName = "Firefox"; refinedBrowserVersion = firefoxMatcher.group(1); }
            else if (safariMatcher.find() && userAgentString.contains("Safari/") && !userAgentString.contains("Chrome/") && !userAgentString.contains("CriOS/") && !userAgentString.contains("Edg")) {
                refinedBrowserName = "Safari"; refinedBrowserVersion = safariMatcher.group(1);
            }
            else if (operaMatcher.find()) { refinedBrowserName = "Opera"; refinedBrowserVersion = operaMatcher.group(1); }
            else if (samsungMatcher.find()) { refinedBrowserName = "Samsung Browser"; refinedBrowserVersion = samsungMatcher.group(1); }
            else if (ieMatcher.find()) { refinedBrowserName = "Internet Explorer"; refinedBrowserVersion = ieMatcher.group(1) != null ? ieMatcher.group(1) : ieMatcher.group(2); }

            // --- Refine OS Version ---
            String refinedOsVersion = formatVersion(os.getVersionNumber()); // Use formatted version from library
            // Add specific OS overrides if needed, like for Windows
            if (libOsName.startsWith("Windows")) {
                if (userAgentString.contains("Windows NT 10.0")) refinedOsVersion = "10 / 11";
                else if (userAgentString.contains("Windows NT 6.3")) refinedOsVersion = "8.1";
                else if (userAgentString.contains("Windows NT 6.2")) refinedOsVersion = "8";
                else if (userAgentString.contains("Windows NT 6.1")) refinedOsVersion = "7";
                else if (userAgentString.contains("Windows NT 6.0")) refinedOsVersion = "Vista";
                else if (userAgentString.contains("Windows NT 5.1")) refinedOsVersion = "XP";
                // Add others as necessary
            }
            // Add refinements for macOS, Android, iOS if libphonenumber version is too generic

            // --- Determine Engine ---
            String engineName = determineEngine(refinedBrowserName, libBrowserFamily, userAgentString);
            String engineVersion = determineEngineVersion(engineName, userAgentString);

            // --- Device Info ---
            String deviceType = formatDeviceCategoryName(libDeviceCategory); // Format the category name

            boolean isMobile = deviceType.equals("Smartphone") || deviceType.equals("Tablet") || ua.getType() == UserAgentType.MOBILE_BROWSER;
            boolean isTablet = deviceType.equals("Tablet");
            boolean isDesktop = !isMobile && ua.getType() != UserAgentType.ROBOT;
            boolean isBot = ua.getType() == UserAgentType.ROBOT;

            // --- CPU Architecture ---
            String cpuArchitecture = "Unknown"; // Determine based on common UA string patterns
            String uaLower = userAgentString.toLowerCase();
            if (uaLower.contains("x64") || uaLower.contains("win64") || uaLower.contains("amd64") || uaLower.contains("x86_64")) cpuArchitecture = "x86-64";
            else if (uaLower.contains("x86") || uaLower.contains("i686") || uaLower.contains("wow64")) cpuArchitecture = "x86 (32-bit)";
            else if (uaLower.contains("arm64") || uaLower.contains("aarch64")) cpuArchitecture = "ARM64";
            else if (uaLower.contains("arm")) cpuArchitecture = "ARM (32-bit)";


            // --- Build Result Map (matching NEW output IDs) ---
            result.put("success", true);
            result.put("isEmpty", false);

            result.put("browserName", refinedBrowserName);
            result.put("browserVersion", refinedBrowserVersion);
            result.put("browserType", libBrowserType);
            result.put("browserFamily", libBrowserFamily);
            result.put("browserProducer", libBrowserProducer);

            result.put("engineName", engineName);
            result.put("engineVersion", engineVersion);

            result.put("osName", libOsName);
            result.put("osVersion", refinedOsVersion);
            result.put("osFamily", libOsFamily);
            result.put("osProducer", libOsProducer);

            result.put("deviceType", deviceType);
            result.put("deviceCategory", libDeviceCategory); // Raw category name might be useful too
            // Removed less reliable deviceModel/Vendor fields from output map
            result.put("cpuArchitecture", cpuArchitecture);

            // Boolean capabilities matching output IDs
            result.put("isMobile", isMobile);
            result.put("isTablet", isTablet);
            result.put("isDesktop", isDesktop);
            result.put("isBot", isBot);

            result.put("rawUserAgent", userAgentString);

        } catch (Exception e) {
            System.err.println("Error parsing user agent string: '" + userAgentString + "' - " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put(errorOutputId, "Failed to parse User Agent: " + e.getMessage());
        }
        return result;
    }

    // --- Helper Methods ---

    /** Formats UADetector VersionNumber */
    private String formatVersion(VersionNumber version) {
        // Use the library's recommended formatting method
        return version == null ? "" : version.toVersionString();
    }

    /** Determines rendering engine */
    private String determineEngine(String browserName, String browserFamily, String userAgentString) {
        // This logic remains largely the same, prioritizing known families/names
        if (browserFamily.contains("Chrome") || browserFamily.contains("Chromium") ||
                browserName.equals("Edge") || browserName.equals("Opera") || browserName.equals("Samsung Browser") || browserName.contains("Vivaldi") ) {
            // Check specifically for older Opera Presto engine
            if (userAgentString.contains("Presto/")) return "Presto";
            return "Blink";
        }
        if (browserFamily.contains("Firefox")) return "Gecko";
        if (browserFamily.contains("Safari") || browserName.equals("Safari") || browserName.equals("Mobile Safari")) return "WebKit";
        if (browserFamily.contains("IE") || browserName.contains("Internet Explorer")) return "Trident";
        // Fallbacks based on string patterns
        if (userAgentString.contains("AppleWebKit/")) return "WebKit";
        if (userAgentString.contains("Gecko/")) return "Gecko";
        if (userAgentString.contains("Trident/")) return "Trident";
        if (userAgentString.contains("Presto/")) return "Presto";
        return "Unknown";
    }

    /** Determines engine version using regex */
    private String determineEngineVersion(String engineName, String userAgentString) {
        Matcher matcher = switch (engineName) {
            case "Blink" -> WEBKIT_VERSION_PATTERN.matcher(userAgentString); // Blink version often tracks WebKit closely
            case "WebKit" -> WEBKIT_VERSION_PATTERN.matcher(userAgentString);
            case "Gecko" -> GECKO_VERSION_PATTERN.matcher(userAgentString);
            case "Trident" -> TRIDENT_VERSION_PATTERN.matcher(userAgentString);
            // Add Presto version regex if needed: Pattern.compile("Presto/([\\d.]+)");
            default -> null;
        };
        return (matcher != null && matcher.find()) ? matcher.group(1) : ""; // Return empty string if unknown
    }

    /** Formats device category string for better display */
    private String formatDeviceCategoryName(String category) {
        if (category == null || category.isEmpty() || category.equalsIgnoreCase("Unknown")) return "Unknown";
        // Use Matcher to capitalize first letter of each word correctly
        Pattern wordStartPattern = Pattern.compile("\\b\\w");
        Matcher matcher = wordStartPattern.matcher(category.replace('_', ' ').toLowerCase()); // Replace underscore, lowercase first
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // Null default indicates required (or empty string allowed if defaultValue="")
    private String getStringParam(Map<String, Object> input) throws IllegalArgumentException {
        Object value = input.get("userAgentInput");
        if (value == null) {
            return "";
        }
        // Don't trim UA string
        // Only throw if required and empty, except for userAgentInput which can be empty initially
        return value.toString();
    }
}