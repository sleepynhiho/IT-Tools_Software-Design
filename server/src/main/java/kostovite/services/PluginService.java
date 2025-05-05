// src/main/java/kostovite/services/PluginService.java
package kostovite.services;

import kostovite.ManualPluginLoader;
import kostovite.PluginInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);
    private final ManualPluginLoader manualPluginLoader;

    // Constructor remains the same
    public PluginService(ManualPluginLoader manualPluginLoader) {
        this.manualPluginLoader = manualPluginLoader;
        log.info("PluginService initialized.");
    }

    // getAccessiblePluginMetadata remains the same
    public List<Map<String, Object>> getAccessiblePluginMetadata(Authentication authentication) {
        String userType = extractUserType(authentication);
        log.info("Filtering plugins for effective userType: '{}' (Derived from authorities: {})",
                userType,
                authentication != null ? authentication.getAuthorities() : "N/A"); // Handle null auth
        List<PluginInterface> allPlugins = manualPluginLoader.getLoadedPlugins();
        if (allPlugins == null) { return Collections.emptyList(); }
        log.debug("Total plugins available before filtering: {}", allPlugins.size());
        // ... (rest of filtering logic from previous correct version) ...
        List<Map<String, Object>> accessibleMetadata = allPlugins.stream()
                .map(this::getPluginMetadataSafely)
                .filter(Objects::nonNull)
                .filter(metadata -> {
                    String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                    if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) { pluginAccessLevel = "normal"; }
                    boolean hasAccess = canUserAccess(userType, pluginAccessLevel);
                    log.debug("Access check for plugin '{}': userType='{}', pluginLevel='{}', result={}", metadata.get("name"), userType, pluginAccessLevel, hasAccess);
                    return hasAccess;
                })
                .collect(Collectors.toList());
        log.info("Found {} accessible plugins for user type {} after filtering", accessibleMetadata.size(), userType);
        return accessibleMetadata;
    }

    // getPluginMetadataSafely remains the same
    public Map<String, Object> getPluginMetadataSafely(PluginInterface plugin) {
        if (plugin == null) return null;
        try { return plugin.getMetadata(); }
        catch (Exception e) { log.error("Error getting metadata for {}: {}", plugin.getName(), e.getMessage(), e); return null; }
    }

    // extractUserType remains the same
    public String extractUserType(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) { return "anonymous"; }
        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) { return "normal"; }
        if (hasRole(authentication, "ROLE_ADMIN")) return "admin";
        if (hasRole(authentication, "ROLE_PREMIUM")) return "premium";
        return "normal";
    }

    // hasRole remains the same
    private boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || authentication.getAuthorities() == null) { return false; }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(roleName::equalsIgnoreCase);
    }

    // canUserAccess remains the same
    public boolean canUserAccess(String userType, String pluginAccessLevel) {
        String normalizedUserType = userType != null ? userType.toLowerCase() : "anonymous";
        String normalizedPluginAccessLevel = pluginAccessLevel != null ? pluginAccessLevel.toLowerCase() : "normal";
        boolean hasAccess = switch (normalizedUserType) {
            case "admin" -> true;
            case "premium" -> "normal".equals(normalizedPluginAccessLevel) || "premium".equals(normalizedPluginAccessLevel);
            case "anonymous", "normal" -> "normal".equals(normalizedPluginAccessLevel); // Grouped normal and anonymous
            default -> false;
        };
        log.debug("Access decision: userType='{}', pluginAccessLevel='{}', hasAccess={}", normalizedUserType, normalizedPluginAccessLevel, hasAccess);
        return hasAccess;
    }

    /**
     * Process a plugin with input data, performing an access control check first.
     * Throws IllegalArgumentException if plugin not found, AccessDeniedException if access denied.
     */
    public Map<String, Object> processPlugin(String pluginName, Map<String, Object> inputData, Authentication authentication) {
        String userType = extractUserType(authentication);
        PluginInterface plugin = manualPluginLoader.getPluginByName(pluginName);

        if (plugin == null) {
            log.warn("Processing attempt failed - Plugin not found: {}", pluginName);
            throw new IllegalArgumentException("Plugin not found: " + pluginName);
        }

        // Check access based on metadata BEFORE processing
        Map<String, Object> metadata = getPluginMetadataSafely(plugin);
        String pluginAccessLevel; // Declare variable

        if (metadata != null) {
            // Metadata exists, try to get the access level
            pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
            // Validate the retrieved access level string
            if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) {
                log.warn("Plugin '{}' has invalid accessLevel '{}' in metadata. Treating as 'normal' for processing check.", pluginName, metadata.get("accessLevel"));
                pluginAccessLevel = "normal"; // Correct invalid value to normal
            }
        } else {
            // --- Metadata is null: Explicitly default to normal ---
            log.warn("Could not retrieve metadata for plugin '{}' during process request. Assuming 'normal' access for check.", pluginName);
            pluginAccessLevel = "normal"; // Explicitly set default when metadata is null
            // ------------------------------------------------------
        }

        // --- Access check using the determined pluginAccessLevel ---
        if (!canUserAccess(userType, pluginAccessLevel)) {
            String username = (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) ? authentication.getName() : "anonymous";
            String errorMessage = String.format("User '%s' (type '%s') does not have permission to access plugin '%s' requiring '%s' access.",
                    username, userType, pluginName, pluginAccessLevel);
            log.warn(errorMessage);
            throw new AccessDeniedException(errorMessage);
        }
        // ----------------------------------------------------------

        log.info("Access granted. Processing plugin '{}' for user '{}' (type '{}')", pluginName, (authentication != null ? authentication.getName() : "anonymous"), userType);
        try {
            Map<String, Object> result = manualPluginLoader.processWithPlugin(pluginName, inputData);
            log.info("Successfully processed plugin '{}'", pluginName);
            return result;
        } catch (Exception e) {
            log.error("Error during execution of plugin '{}': {}", pluginName, e.getMessage(), e);
            throw new RuntimeException("Error processing plugin '" + pluginName + "': " + e.getMessage(), e);
        }
    }

    // getAccessiblePlugin remains the same
    public PluginInterface getAccessiblePlugin(String pluginName, Authentication authentication) {
        String userType = extractUserType(authentication);
        PluginInterface plugin = manualPluginLoader.getPluginByName(pluginName);
        if (plugin == null) { return null; }
        Map<String, Object> metadata = getPluginMetadataSafely(plugin);
        String pluginAccessLevel = "normal";
        if (metadata != null) {
            pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
            if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) { pluginAccessLevel = "normal"; }
        } else {
            log.warn("Could not retrieve metadata for plugin '{}' during accessible check. Assuming 'normal'.", pluginName);
            // pluginAccessLevel is already 'normal'
        }
        if (!canUserAccess(userType, pluginAccessLevel)) { return null; } // Access denied
        return plugin; // Access granted
    }

    // getAllLoadedPlugins remains the same
    public List<PluginInterface> getAllLoadedPlugins() {
        return manualPluginLoader.getLoadedPlugins();
    }
}