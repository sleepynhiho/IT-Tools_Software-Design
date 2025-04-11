package kostovite;

import java.util.HashMap;
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
    private static final Pattern CHROME_VERSION = Pattern.compile("Chrome/([0-9\\.]+)");
    private static final Pattern EDGE_VERSION = Pattern.compile("Edg(?:e|)/([0-9\\.]+)");
    private static final Pattern SAFARI_VERSION = Pattern.compile("Safari/([0-9\\.]+)");
    private static final Pattern FIREFOX_VERSION = Pattern.compile("Firefox/([0-9\\.]+)");
    private static final Pattern IE_VERSION = Pattern.compile("MSIE ([0-9\\.]+)|rv:([0-9\\.]+)");
    private static final Pattern WEBKIT_VERSION = Pattern.compile("AppleWebKit/([0-9\\.]+)");
    private static final Pattern GECKO_VERSION = Pattern.compile("Gecko/([0-9\\.]+)");

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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Parse and analyze user agent strings");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Parse operation
        Map<String, Object> parseOperation = new HashMap<>();
        parseOperation.put("description", "Parse a user agent string and extract browser, OS, and device information");

        Map<String, Object> parseInputs = new HashMap<>();
        parseInputs.put("userAgent", "User agent string to parse");

        parseOperation.put("inputs", parseInputs);
        operations.put("parse", parseOperation);

        metadata.put("operations", operations);
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

        if (version.getMajor() != null) {
            sb.append(version.getMajor());

            if (version.getMinor() != null) {
                sb.append(".").append(version.getMinor());

                if (version.getBugfix() != null) {
                    sb.append(".").append(version.getBugfix());

                    if (version.getExtension() != null && !version.getExtension().isEmpty()) {
                        sb.append(".").append(version.getExtension());
                    }
                }
            }
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
                Matcher ieMatcher = Pattern.compile("Trident/([0-9\\.]+)").matcher(userAgentString);
                if (ieMatcher.find()) {
                    return ieMatcher.group(1);
                }
                break;
        }

        return "Unknown";
    }
}