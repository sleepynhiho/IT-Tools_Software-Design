package kostovite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.VersionNumber;
import net.sf.uadetector.service.UADetectorServiceFactory;

public class UserAgentParser implements PluginInterface {

    private final UserAgentStringParser parser;

    // Common browser rendering engines mapping
    private static final Map<String, String> BROWSER_TO_ENGINE = new HashMap<>();
    static {
        BROWSER_TO_ENGINE.put("CHROME", "Blink");
        BROWSER_TO_ENGINE.put("CHROME_MOBILE", "Blink");
        BROWSER_TO_ENGINE.put("EDGE", "Blink");  // Modern Edge
        BROWSER_TO_ENGINE.put("EDGE_MOBILE", "Blink");
        BROWSER_TO_ENGINE.put("OPERA", "Blink");
        BROWSER_TO_ENGINE.put("OPERA_MINI", "Blink");
        BROWSER_TO_ENGINE.put("OPERA_MOBILE", "Blink");
        BROWSER_TO_ENGINE.put("FIREFOX", "Gecko");
        BROWSER_TO_ENGINE.put("FIREFOX_MOBILE", "Gecko");
        BROWSER_TO_ENGINE.put("SAFARI", "WebKit");
        BROWSER_TO_ENGINE.put("MOBILE_SAFARI", "WebKit");
        BROWSER_TO_ENGINE.put("INTERNET_EXPLORER", "Trident");
        BROWSER_TO_ENGINE.put("INTERNET_EXPLORER_MOBILE", "Trident");
        BROWSER_TO_ENGINE.put("SAMSUNG_BROWSER", "Blink");
    }

    // Regular expressions for specific browser details
    private static final Pattern CHROME_VERSION = Pattern.compile("Chrome/([0-9.]+)");
    private static final Pattern EDGE_VERSION = Pattern.compile("Edg(?:e|)/([0-9.]+)");
    private static final Pattern SAFARI_VERSION = Pattern.compile("Safari/([0-9.]+)");
    private static final Pattern FIREFOX_VERSION = Pattern.compile("Firefox/([0-9.]+)");
    private static final Pattern IE_VERSION = Pattern.compile("MSIE ([0-9.]+)|rv:([0-9.]+)");
    private static final Pattern WEBKIT_VERSION = Pattern.compile("AppleWebKit/([0-9.]+)");
    private static final Pattern GECKO_VERSION = Pattern.compile("Gecko/([0-9.]+)");

    public UserAgentParser() {
        // Initialize with cached user agent data
        this.parser = UADetectorServiceFactory.getCachingAndUpdatingParser();
    }

    @Override
    public String getName() {
        return "UserAgentParser";
    }

    @Override
    public void execute() {
        System.out.println("User Agent Parser Plugin executed");

        // Demonstrate basic usage
        try {
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0";

            Map<String, Object> params = new HashMap<>();
            params.put("userAgent", userAgent);

            Map<String, Object> result = process(params);
            System.out.println("User agent parsed successfully: " + result.get("success"));
            System.out.println("Browser: " + result.get("browserName") + " " + result.get("browserVersion"));
            System.out.println("OS: " + result.get("osName") + " " + result.get("osVersion"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse and analyze user agent strings"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Parse operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse a user agent string and extract browser, OS, and device information");
        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("userAgent", Map.of("type", "string", "description", "User agent string to parse", "required", true));
        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "UserAgentParser"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "DeviceHub"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Web Tools"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: User Agent Input
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "User Agent String");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // User agent input field
        Map<String, Object> userAgentField = new HashMap<>();
        userAgentField.put("name", "userAgent");
        userAgentField.put("label", "User Agent:");
        userAgentField.put("type", "text");
        userAgentField.put("multiline", true);
        userAgentField.put("rows", 3);
        userAgentField.put("placeholder", "Enter or paste a user agent string...");
        userAgentField.put("required", true);
        userAgentField.put("helperText", "The browser's user agent string to analyze");
        section1Fields.add(userAgentField);

        // Current user agent button
        Map<String, Object> currentUaButton = new HashMap<>();
        currentUaButton.put("name", "useCurrentUA");
        currentUaButton.put("label", "Use My User Agent");
        currentUaButton.put("type", "button");
        currentUaButton.put("action", "navigator.userAgent && setFieldValue('userAgent', navigator.userAgent)");
        currentUaButton.put("variant", "outlined");
        section1Fields.add(currentUaButton);

        // Example user agents
        Map<String, Object> examplesField = new HashMap<>();
        examplesField.put("name", "examples");
        examplesField.put("label", "Common Examples:");
        examplesField.put("type", "select");
        examplesField.put("helperText", "Select an example user agent to analyze");
        examplesField.put("onChange", "setFieldValue('userAgent', value)");
        List<Map<String, String>> exampleOptions = new ArrayList<>();
        exampleOptions.add(Map.of("value", "", "label", "-- Select an example --"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36", "label", "Chrome on Windows"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15", "label", "Safari on macOS"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1", "label", "Safari on iPhone"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0", "label", "Firefox on Windows"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0", "label", "Edge on Windows"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36", "label", "Chrome on Android"));
        exampleOptions.add(Map.of("value", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)", "label", "Googlebot"));
        examplesField.put("options", exampleOptions);
        examplesField.put("required", false);
        section1Fields.add(examplesField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Browser Information
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Browser Information");
        outputSection1.put("condition", "success");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Browser name and version
        Map<String, Object> browserOutput = new HashMap<>();
        browserOutput.put("title", "Browser");
        browserOutput.put("name", "browserInfo");
        browserOutput.put("type", "text");
        browserOutput.put("formula", "browserName + ' ' + browserVersion");
        browserOutput.put("variant", "bold");
        section1OutputFields.add(browserOutput);

        // Browser type
        Map<String, Object> typeOutput = new HashMap<>();
        typeOutput.put("title", "Type");
        typeOutput.put("name", "browserType");
        typeOutput.put("type", "text");
        section1OutputFields.add(typeOutput);

        // Browser family
        Map<String, Object> familyOutput = new HashMap<>();
        familyOutput.put("title", "Family");
        familyOutput.put("name", "browserFamily");
        familyOutput.put("type", "text");
        section1OutputFields.add(familyOutput);

        // Browser producer
        Map<String, Object> producerOutput = new HashMap<>();
        producerOutput.put("title", "Producer");
        producerOutput.put("name", "browserProducer");
        producerOutput.put("type", "text");
        section1OutputFields.add(producerOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Engine Information
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Rendering Engine");
        outputSection2.put("condition", "success");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Engine name and version
        Map<String, Object> engineOutput = new HashMap<>();
        engineOutput.put("title", "Engine");
        engineOutput.put("name", "engineInfo");
        engineOutput.put("type", "text");
        engineOutput.put("formula", "engineName + ' ' + engineVersion");
        section2OutputFields.add(engineOutput);

        // Engine description
        Map<String, Object> engineDescOutput = new HashMap<>();
        engineDescOutput.put("title", "Description");
        engineDescOutput.put("name", "engineDescription");
        engineDescOutput.put("type", "text");
        engineDescOutput.put("formula",
                "engineName === 'Blink' ? 'Chrome-based rendering engine by Google' : " +
                        "engineName === 'WebKit' ? 'Safari rendering engine by Apple' : " +
                        "engineName === 'Gecko' ? 'Firefox rendering engine by Mozilla' : " +
                        "engineName === 'Trident' ? 'Internet Explorer rendering engine by Microsoft' : " +
                        "'Unknown rendering engine'");
        section2OutputFields.add(engineDescOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: OS and Device Information
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Operating System & Device");
        outputSection3.put("condition", "success");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // OS information
        Map<String, Object> osOutput = new HashMap<>();
        osOutput.put("title", "Operating System");
        osOutput.put("name", "osInfo");
        osOutput.put("type", "text");
        osOutput.put("formula", "osName + ' ' + osVersion");
        osOutput.put("variant", "bold");
        section3OutputFields.add(osOutput);

        // OS family
        Map<String, Object> osFamilyOutput = new HashMap<>();
        osFamilyOutput.put("title", "OS Family");
        osFamilyOutput.put("name", "osFamily");
        osFamilyOutput.put("type", "text");
        section3OutputFields.add(osFamilyOutput);

        // Device type
        Map<String, Object> deviceOutput = new HashMap<>();
        deviceOutput.put("title", "Device Type");
        deviceOutput.put("name", "deviceType");
        deviceOutput.put("type", "text");
        section3OutputFields.add(deviceOutput);

        // Device model (only if available)
        Map<String, Object> deviceModelOutput = new HashMap<>();
        deviceModelOutput.put("title", "Device Model");
        deviceModelOutput.put("name", "deviceModel");
        deviceModelOutput.put("type", "text");
        deviceModelOutput.put("condition", "deviceModel !== 'No device model available'");
        section3OutputFields.add(deviceModelOutput);

        // Device vendor (only if available)
        Map<String, Object> deviceVendorOutput = new HashMap<>();
        deviceVendorOutput.put("title", "Device Vendor");
        deviceVendorOutput.put("name", "deviceVendor");
        deviceVendorOutput.put("type", "text");
        deviceVendorOutput.put("condition", "deviceVendor !== 'No device vendor available'");
        section3OutputFields.add(deviceVendorOutput);

        // CPU architecture
        Map<String, Object> cpuOutput = new HashMap<>();
        cpuOutput.put("title", "CPU Architecture");
        cpuOutput.put("name", "cpuArchitecture");
        cpuOutput.put("type", "text");
        section3OutputFields.add(cpuOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        // Output Section 4: Device Capabilities
        Map<String, Object> outputSection4 = new HashMap<>();
        outputSection4.put("header", "Device Capabilities");
        outputSection4.put("condition", "success");
        List<Map<String, Object>> section4OutputFields = new ArrayList<>();

        // Device type indicators
        Map<String, Object> deviceTypesOutput = new HashMap<>();
        deviceTypesOutput.put("title", "Device Classification");
        deviceTypesOutput.put("name", "deviceTypes");
        deviceTypesOutput.put("type", "chips");
        deviceTypesOutput.put("items", "[" +
                "isMobile ? 'Mobile' : null, " +
                "isTablet ? 'Tablet' : null, " +
                "isDesktop ? 'Desktop' : null, " +
                "isBot ? 'Bot/Crawler' : null" +
                "].filter(Boolean)");
        deviceTypesOutput.put("colors", "[" +
                "isMobile ? 'primary' : null, " +
                "isTablet ? 'primary' : null, " +
                "isDesktop ? 'primary' : null, " +
                "isBot ? 'warning' : null" +
                "].filter(Boolean)");
        section4OutputFields.add(deviceTypesOutput);

        // Device category
        Map<String, Object> deviceCategoryOutput = new HashMap<>();
        deviceCategoryOutput.put("title", "Device Category");
        deviceCategoryOutput.put("name", "deviceCategory");
        deviceCategoryOutput.put("type", "text");
        section4OutputFields.add(deviceCategoryOutput);

        outputSection4.put("fields", section4OutputFields);
        uiOutputs.add(outputSection4);

        // Output Section 5: Raw User Agent
        Map<String, Object> outputSection5 = new HashMap<>();
        outputSection5.put("header", "Raw User Agent String");
        outputSection5.put("condition", "success");
        List<Map<String, Object>> section5OutputFields = new ArrayList<>();

        // Raw user agent
        Map<String, Object> rawUaOutput = new HashMap<>();
        rawUaOutput.put("name", "userAgent");
        rawUaOutput.put("type", "text");
        rawUaOutput.put("multiline", true);
        rawUaOutput.put("monospace", true);
        rawUaOutput.put("buttons", List.of("copy"));
        section5OutputFields.add(rawUaOutput);

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

            if ("parse".equalsIgnoreCase(operation)) {
                String userAgent = (String) input.get("userAgent");

                if (userAgent == null || userAgent.trim().isEmpty()) {
                    result.put("error", "User agent string cannot be empty");
                    return result;
                }

                return parseUserAgent(userAgent);
            } else {
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
     * Parse a user agent string and extract detailed information
     *
     * @param userAgentString The user agent string to parse
     * @return Parsed user agent data
     */
    private Map<String, Object> parseUserAgent(String userAgentString) {
        Map<String, Object> result = new HashMap<>();

        try {
            // First use the UADetector library to get basic information
            ReadableUserAgent userAgent = parser.parse(userAgentString);

            // Get browser information
            String browserName = userAgent.getName();
            String browserType = userAgent.getType().getName();
            VersionNumber browserVersion = userAgent.getVersionNumber();
            String browserVersionStr = formatVersion(browserVersion);

            // Enhanced browser detection using regular expressions for specific browser details
            // This gives more accurate results for modern browsers
            String detectedBrowserName = null;
            String detectedBrowserVersion = null;

            // Check if it's Edge
            Matcher edgeMatcher = EDGE_VERSION.matcher(userAgentString);
            if (edgeMatcher.find()) {
                detectedBrowserName = "Edge";
                detectedBrowserVersion = edgeMatcher.group(1);
            }
            // Chrome (but not Edge which also includes Chrome in UA)
            else if (userAgentString.contains("Chrome/") && !userAgentString.contains("Edg")) {
                Matcher chromeMatcher = CHROME_VERSION.matcher(userAgentString);
                if (chromeMatcher.find()) {
                    detectedBrowserName = "Chrome";
                    detectedBrowserVersion = chromeMatcher.group(1);
                }
            }
            // Firefox
            else if (userAgentString.contains("Firefox/")) {
                Matcher firefoxMatcher = FIREFOX_VERSION.matcher(userAgentString);
                if (firefoxMatcher.find()) {
                    detectedBrowserName = "Firefox";
                    detectedBrowserVersion = firefoxMatcher.group(1);
                }
            }
            // Safari (but not Chrome or Edge which also include Safari in UA)
            else if (userAgentString.contains("Safari/") &&
                    !userAgentString.contains("Chrome/") &&
                    !userAgentString.contains("Edg")) {
                detectedBrowserName = "Safari";
                Matcher safariMatcher = SAFARI_VERSION.matcher(userAgentString);
                if (safariMatcher.find()) {
                    // Safari's version in the UA string isn't its actual version
                    // We would need additional parsing for accurate Safari versions
                    detectedBrowserVersion = safariMatcher.group(1);
                }
            }
            // IE
            else if (userAgentString.contains("MSIE") || userAgentString.contains("Trident/")) {
                detectedBrowserName = "Internet Explorer";
                Matcher ieMatcher = IE_VERSION.matcher(userAgentString);
                if (ieMatcher.find()) {
                    detectedBrowserVersion = ieMatcher.group(1) != null ? ieMatcher.group(1) : ieMatcher.group(2);
                }
            }

            // If we detected a browser manually, use that info
            if (detectedBrowserName != null) {
                browserName = detectedBrowserName;
                browserVersionStr = detectedBrowserVersion;
            }

            // Get operating system information
            String osName = userAgent.getOperatingSystem().getName();
            String osVersion = userAgent.getOperatingSystem().getVersionNumber().toVersionString();

            // Get device information (if available)
            String deviceCategory = userAgent.getDeviceCategory().getName();
            String deviceType = "No device type available";
            String deviceModel = "No device model available";
            String deviceVendor = "No device vendor available";

            // Determine if mobile
            boolean isMobile = browserType.contains("MOBILE") || deviceCategory.contains("SMARTPHONE") ||
                    deviceCategory.contains("TABLET") || userAgentString.contains("Mobile");

            // Determine if tablet
            boolean isTablet = deviceCategory.contains("TABLET");

            // Infer more device details from user agent if possible
            if (userAgentString.contains("iPhone")) {
                deviceType = "Smartphone";
                deviceModel = "iPhone";
                deviceVendor = "Apple";
            } else if (userAgentString.contains("iPad")) {
                deviceType = "Tablet";
                deviceModel = "iPad";
                deviceVendor = "Apple";
            } else if (userAgentString.contains("Android")) {
                deviceVendor = "Unknown Android";

                if (isTablet) {
                    deviceType = "Tablet";
                } else if (isMobile) {
                    deviceType = "Smartphone";
                } else {
                    deviceType = "Device";
                }

                // Try to extract Android device model
                Pattern androidModel = Pattern.compile("; ([^;]+) Build/");
                Matcher androidModelMatcher = androidModel.matcher(userAgentString);
                if (androidModelMatcher.find()) {
                    deviceModel = androidModelMatcher.group(1);
                }
            } else if (!isMobile) {
                deviceType = "Desktop/Laptop";
            }

            // Determine browser engine and version
            String engineName = determineEngine(browserName, userAgentString);
            String engineVersion = determineEngineVersion(engineName, userAgentString);

            // Get CPU architecture information
            String cpuArchitecture = "Unknown";
            if (userAgentString.contains("x64") || userAgentString.contains("x86_64") ||
                    userAgentString.contains("Win64") || userAgentString.contains("amd64")) {
                cpuArchitecture = "amd64";
            } else if (userAgentString.contains("x86") || userAgentString.contains("i686") ||
                    userAgentString.contains("i586") || userAgentString.contains("i386")) {
                cpuArchitecture = "x86";
            } else if (userAgentString.contains("arm") || userAgentString.contains("ARM")) {
                if (userAgentString.contains("arm64") || userAgentString.contains("aarch64")) {
                    cpuArchitecture = "arm64";
                } else {
                    cpuArchitecture = "arm";
                }
            }

            // Build the result
            result.put("success", true);
            result.put("userAgent", userAgentString);

            // Browser information
            result.put("browserName", browserName);
            result.put("browserVersion", browserVersionStr);
            result.put("browserType", browserType);
            result.put("browserFamily", userAgent.getFamily().getName());
            result.put("browserProducer", userAgent.getProducer());

            // OS information
            result.put("osName", osName);
            result.put("osVersion", osVersion);
            result.put("osFamily", userAgent.getOperatingSystem().getFamily());
            result.put("osProducer", userAgent.getOperatingSystem().getProducer());

            // Engine information
            result.put("engineName", engineName);
            result.put("engineVersion", engineVersion);

            // Device information
            result.put("deviceType", deviceType);
            result.put("deviceModel", deviceModel);
            result.put("deviceVendor", deviceVendor);
            result.put("deviceCategory", deviceCategory);

            // CPU information
            result.put("cpuArchitecture", cpuArchitecture);

            // Other flags
            result.put("isMobile", isMobile);
            result.put("isTablet", isTablet);
            result.put("isDesktop", !isMobile && !isTablet);
            result.put("isBot", userAgent.getType().getName().equals("ROBOT"));

        } catch (Exception e) {
            result.put("error", "Error parsing user agent: " + e.getMessage());
            result.put("success", false);
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Format a version number to a string
     *
     * @param version VersionNumber object
     * @return Formatted version string
     */
    private String formatVersion(VersionNumber version) {
        if (version == null) {
            return "Unknown";
        }

        StringBuilder sb = new StringBuilder();

        version.getMajor();
        sb.append(version.getMajor());

        version.getMinor();
        sb.append(".").append(version.getMinor());

        version.getBugfix();
        sb.append(".").append(version.getBugfix());

        if (!version.getExtension().isEmpty()) {
            sb.append(".").append(version.getExtension());
        }

        return sb.toString();
    }

    /**
     * Determine the rendering engine based on browser name and user agent string
     *
     * @param browserName Browser name
     * @param userAgentString User agent string
     * @return Browser engine name
     */
    private String determineEngine(String browserName, String userAgentString) {
        // Use the mapping first
        String engineFromMapping = BROWSER_TO_ENGINE.get(browserName.toUpperCase());
        if (engineFromMapping != null) {
            return engineFromMapping;
        }

        // Fallback to parsing the user agent string
        if (userAgentString.contains("Gecko/") && userAgentString.contains("Firefox/")) {
            return "Gecko";
        } else if (userAgentString.contains("AppleWebKit/") && userAgentString.contains("Chrome/")) {
            return "Blink";
        } else if (userAgentString.contains("AppleWebKit/")) {
            return "WebKit";
        } else if (userAgentString.contains("Trident/") || userAgentString.contains("MSIE")) {
            return "Trident";
        } else if (userAgentString.contains("Presto/")) {
            return "Presto";
        }

        return "Unknown";
    }

    /**
     * Determine the engine version based on engine name and user agent string
     *
     * @param engineName Engine name
     * @param userAgentString User agent string
     * @return Engine version
     */
    private String determineEngineVersion(String engineName, String userAgentString) {
        switch (engineName) {
            case "Blink":
                // Blink uses the same version as Chrome
                Matcher chromeMatcher = CHROME_VERSION.matcher(userAgentString);
                if (chromeMatcher.find()) {
                    return chromeMatcher.group(1);
                }
                break;
            case "WebKit":
                Matcher webkitMatcher = WEBKIT_VERSION.matcher(userAgentString);
                if (webkitMatcher.find()) {
                    return webkitMatcher.group(1);
                }
                break;
            case "Gecko":
                Matcher geckoMatcher = GECKO_VERSION.matcher(userAgentString);
                if (geckoMatcher.find()) {
                    return geckoMatcher.group(1);
                }
                // Modern Firefox doesn't include meaningful Gecko version numbers
                Matcher firefoxMatcher = FIREFOX_VERSION.matcher(userAgentString);
                if (firefoxMatcher.find()) {
                    return firefoxMatcher.group(1);
                }
                break;
            case "Trident":
                Matcher ieMatcher = Pattern.compile("Trident/([0-9.]+)").matcher(userAgentString);
                if (ieMatcher.find()) {
                    return ieMatcher.group(1);
                }
                break;
        }

        return "Unknown";
    }
}