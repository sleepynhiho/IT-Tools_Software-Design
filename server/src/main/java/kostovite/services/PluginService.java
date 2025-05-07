// src/main/java/kostovite/services/PluginService.java
package kostovite.services;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import kostovite.ManualPluginLoader;
import kostovite.PluginInterface;
import kostovite.ExtendedPluginInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);
    private final ManualPluginLoader manualPluginLoader;
    private final Firestore firestore;
    private final Path pluginsDirectory = Paths.get("plugins-deploy"); // Added for plugin management

    @Autowired
    public PluginService(ManualPluginLoader manualPluginLoader, Firestore firestore) {
        this.manualPluginLoader = manualPluginLoader;
        this.firestore = firestore;
        log.info("[2025-05-06 18:16:29] Kostovite: PluginService initialized with ManualPluginLoader and Firestore.");
    }

    /**
     * Unload a plugin from memory but keep its JAR file
     * @param pluginName The name of the plugin to unload
     * @return true if successfully unloaded, false otherwise
     */
    public boolean unloadPlugin(String pluginName) {
        log.info("[2025-05-06 18:16:29] Kostovite: Attempting to unload plugin '{}'", pluginName);
        try {
            boolean unloaded = manualPluginLoader.unloadPlugin(pluginName);
            if (unloaded) {
                log.info("[2025-05-06 18:16:29] Kostovite: Successfully unloaded plugin '{}' from memory", pluginName);
            } else {
                log.warn("[2025-05-06 18:16:29] Kostovite: Failed to unload plugin '{}'. It might not be loaded or an error occurred.", pluginName);
            }
            return unloaded;
        } catch (Exception e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Exception during plugin unload for '{}': {}", pluginName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete a plugin JAR file and unload it from memory
     * @param pluginName The name of the plugin to delete
     * @return true if JAR was deleted immediately, false if scheduled for deletion or failed
     */
    public boolean deletePluginJar(String pluginName) {
        log.info("[2025-05-06 18:16:29] Kostovite: Attempting to delete plugin JAR for '{}'", pluginName);
        try {
            // deletePlugin method in ManualPluginLoader handles both unload and file deletion
            boolean deletedImmediately = manualPluginLoader.deletePlugin(pluginName);

            // Ensure plugins list is reloaded to reflect changes
            log.info("[2025-05-06 18:16:29] Kostovite: Reloading plugin list after JAR deletion attempt for '{}'", pluginName);
            manualPluginLoader.loadPlugins(pluginsDirectory.toAbsolutePath());

            if (deletedImmediately) {
                log.info("[2025-05-06 18:16:29] Kostovite: Successfully deleted JAR file for plugin '{}'", pluginName);
            } else {
                log.warn("[2025-05-06 18:16:29] Kostovite: Immediate JAR deletion failed for '{}'. It may be locked or scheduled for deletion on JVM exit.", pluginName);
            }
            return deletedImmediately;
        } catch (Exception e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Exception during plugin JAR deletion for '{}': {}", pluginName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Unload all plugins and reload from plugins directory
     * @return A map containing counts of unloaded and loaded plugins
     */
    public Map<String, Object> refreshPlugins() {
        log.info("[2025-05-06 18:16:29] Kostovite: Refreshing all plugins");
        Map<String, Object> result = new HashMap<>();

        try {
            // Unload all existing plugins
            int unloadedCount = manualPluginLoader.unloadAllPlugins();
            log.info("[2025-05-06 18:16:29] Kostovite: Unloaded {} plugins during refresh", unloadedCount);

            // Load plugins from directory
            List<PluginInterface> loadedPlugins = manualPluginLoader.loadPlugins(pluginsDirectory.toAbsolutePath());
            int loadedCount = loadedPlugins.size();
            log.info("[2025-05-06 18:16:29] Kostovite: Loaded {} plugins during refresh", loadedCount);

            // Prepare result
            result.put("status", "success");
            result.put("unloadedCount", unloadedCount);
            result.put("loadedCount", loadedCount);
            result.put("loadedPlugins", loadedPlugins.stream()
                    .map(PluginInterface::getName)
                    .collect(Collectors.toList()));

            return result;
        } catch (Exception e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Exception during plugin refresh: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Failed to refresh plugins: " + e.getMessage());
            return result;
        }
    }

    public PluginStatusCheckResult isPluginEnabled(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return new PluginStatusCheckResult(false, "Plugin ID cannot be empty.");
        }
        String statusCollectionName = "tools"; // Matching frontend's Firestore collection

        try {
            // Assuming pluginId is the document ID in Firestore 'tools' collection
            DocumentSnapshot statusDoc = firestore.collection(statusCollectionName).document(pluginId).get().get();

            if (statusDoc.exists()) {
                Boolean isEnabled = statusDoc.getBoolean("enabled");
                if (Boolean.FALSE.equals(isEnabled)) {
                    String message = statusDoc.getString("disabledMessage"); // Assuming you might add this field
                    return new PluginStatusCheckResult(false, message != null ? message : "Plugin is disabled by administrator.");
                }
            } else {
                // If no specific status document, assume enabled by default
                log.warn("[2025-05-06 18:16:29] Kostovite: No status document found for plugin ID '{}' in '{}'. Assuming enabled by default.", pluginId, statusCollectionName);
            }
            return new PluginStatusCheckResult(true, null); // Enabled if doc doesn't exist or 'enabled' is not false
        } catch (InterruptedException | ExecutionException e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Error fetching Firestore plugin status for ID '{}': {}", pluginId, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new PluginStatusCheckResult(false, "Error checking plugin status."); // Fail-safe: consider disabled on error
        }
    }

    public List<Map<String, Object>> getAccessiblePluginMetadata(Authentication authentication) {
        String userType = extractUserType(authentication);
        log.info("[2025-05-06 18:16:29] Kostovite: Filtering plugins for effective userType: '{}'", userType);

        List<PluginInterface> allLoadedPlugins = manualPluginLoader.getLoadedPlugins();
        if (allLoadedPlugins == null) {
            return Collections.emptyList();
        }
        log.debug("[2025-05-06 18:16:29] Kostovite: Total plugins loaded from JARs: {}", allLoadedPlugins.size());

        List<Map<String, Object>> pluginsWithStatusAndAccess = allLoadedPlugins.stream()
                .map(plugin -> {
                    Map<String, Object> metadata = getPluginMetadataSafely(plugin);
                    if (metadata == null) {
                        log.warn("[2025-05-06 18:16:29] Kostovite: Could not retrieve metadata for plugin named '{}'. Skipping.", plugin.getName());
                        return null; // Skip if basic metadata retrieval fails
                    }

                    // Ensure 'id' and 'name' are in the metadata for consistency
                    // The plugin's 'id' from its metadata should be the key for Firestore status.
                    String pluginId = String.valueOf(metadata.get("id")); // Assuming 'id' field in metadata
                    if (pluginId == null || pluginId.equals("null") || pluginId.isBlank()){ // "null" string check
                        pluginId = plugin.getName(); // Fallback to name if ID is not good
                        log.warn("[2025-05-06 18:16:29] Kostovite: Plugin '{}' missing 'id' in metadata, using name as identifier for status check: {}", plugin.getName(), pluginId);
                        if (pluginId == null || pluginId.isBlank()) {
                            log.error("[2025-05-06 18:16:29] Kostovite: Plugin from JAR {} has no usable ID or name. Cannot determine status or access.", plugin.getClass().getName());
                            return null;
                        }
                    }
                    metadata.putIfAbsent("id", pluginId); // Ensure ID is there
                    metadata.putIfAbsent("name", plugin.getName()); // Ensure name is there


                    // 1. Get enabled/disabled status from Firestore
                    PluginStatusCheckResult statusResult = isPluginEnabled(pluginId);
                    metadata.put("status", statusResult.isEnabled() ? "enabled" : "disabled");
                    if (!statusResult.isEnabled() && statusResult.message() != null) {
                        metadata.put("disabledMessage", statusResult.message());
                    }

                    // 2. Determine effective access level (considering Firestore override if you implement it)
                    // For now, just using the JAR's accessLevel
                    String pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
                    if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) {
                        pluginAccessLevel = "normal";
                    }
                    metadata.put("effectiveAccessLevel", pluginAccessLevel); // Add for clarity if needed

                    // 3. Check if the current user can access this plugin based on its level
                    boolean hasAccess = canUserAccess(userType, pluginAccessLevel);
                    metadata.put("userHasAccess", hasAccess); // Add flag for frontend

                    // Log the decision process
                    log.debug("[2025-05-06 18:16:29] Kostovite: Plugin '{}' (ID: {}): Firestore enabled={}, Level='{}', UserType='{}', UserHasAccess={}",
                            metadata.get("name"), pluginId, statusResult.isEnabled(), pluginAccessLevel, userType, hasAccess);

                    // The frontend will receive all plugins with their status and access for the current user.
                    // It can then decide how to display them (e.g., grey out inaccessible/disabled ones).
                    return metadata;
                })
                .filter(Objects::nonNull) // Remove plugins that couldn't be processed
                .collect(Collectors.toList());

        log.info("[2025-05-06 18:16:29] Kostovite: Returning {} plugins with status and access information for user type '{}'",
                pluginsWithStatusAndAccess.size(), userType);
        return pluginsWithStatusAndAccess;
    }

    public Map<String, Object> getPluginMetadataSafely(PluginInterface plugin) {
        if (plugin == null) return null;
        try {
            // Return a mutable copy
            return new HashMap<>(plugin.getMetadata());
        } catch (Exception e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Error getting metadata for {}: {}", plugin.getName(), e.getMessage(), e);
            return null;
        }
    }

    public String extractUserType(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) { return "anonymous"; }
        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) { return "normal"; }
        if (hasRole(authentication, "ROLE_ADMIN")) return "admin";
        if (hasRole(authentication, "ROLE_PREMIUM")) return "premium";
        return "normal";
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || authentication.getAuthorities() == null) { return false; }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(roleName::equalsIgnoreCase);
    }

    public boolean canUserAccess(String userType, String pluginAccessLevel) {
        String normalizedUserType = userType != null ? userType.toLowerCase() : "anonymous";
        String normalizedPluginAccessLevel = pluginAccessLevel != null ? pluginAccessLevel.toLowerCase() : "normal";
        return switch (normalizedUserType) {
            case "admin" -> true;
            case "premium" -> "normal".equals(normalizedPluginAccessLevel) || "premium".equals(normalizedPluginAccessLevel);
            case "anonymous", "normal" -> "normal".equals(normalizedPluginAccessLevel);
            default -> false;
        };
    }

    public Map<String, Object> processPlugin(String pluginId, Map<String, Object> inputData, Authentication authentication)
            throws PluginDisabledException, IllegalArgumentException, AccessDeniedException {

        // 1. Check if plugin is globally enabled via Firestore
        PluginStatusCheckResult statusResult = isPluginEnabled(pluginId);
        if (!statusResult.isEnabled()) {
            log.warn("[2025-05-06 18:16:29] Kostovite: Processing attempt failed - Plugin ID '{}' is disabled. Reason: {}",
                    pluginId, statusResult.message());
            throw new PluginDisabledException(statusResult.message() != null ?
                    statusResult.message() : "Plugin is currently disabled.");
        }

        PluginInterface plugin = manualPluginLoader.getPluginByName(pluginId);

        if (plugin == null) {
            log.warn("[2025-05-06 18:16:29] Kostovite: Processing attempt failed - Plugin not found by ID/Name: {}", pluginId);
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }

        // 3. Check if the current user has access to this (now confirmed enabled) plugin
        String userType = extractUserType(authentication);
        Map<String, Object> metadata = getPluginMetadataSafely(plugin);
        String pluginAccessLevel = "normal";

        if (metadata != null) {
            pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
            if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) {
                log.warn("[2025-05-06 18:16:29] Kostovite: Plugin ID '{}' (Name: '{}') has invalid accessLevel '{}'. Treating as 'normal'.",
                        pluginId, plugin.getName(), metadata.get("accessLevel"));
                pluginAccessLevel = "normal";
            }
        } else {
            log.warn("[2025-05-06 18:16:29] Kostovite: Could not retrieve metadata for plugin ID '{}' (Name: '{}'). Assuming 'normal' access.", pluginId, plugin.getName());
        }

        if (!canUserAccess(userType, pluginAccessLevel)) {
            String username = (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) ?
                    authentication.getName() : "anonymous";
            String errorMessage = String.format("User '%s' (type '%s') does not have permission to access plugin '%s' (ID: '%s') requiring '%s' access.",
                    username, userType, plugin.getName(), pluginId, pluginAccessLevel);
            log.warn("[2025-05-06 18:16:29] Kostovite: {}", errorMessage);
            throw new AccessDeniedException(errorMessage);
        }

        log.info("[2025-05-06 18:16:29] Kostovite: Access granted. Processing plugin '{}' (ID: '{}') for user '{}' (type '{}')",
                plugin.getName(), pluginId, (authentication != null ? authentication.getName() : "anonymous"), userType);

        try {
            if (plugin instanceof ExtendedPluginInterface extendedPlugin) {
                return extendedPlugin.process(inputData);
            } else {
                plugin.execute();
                log.warn("[2025-05-06 18:16:29] Kostovite: Plugin '{}' (ID: '{}') is not an ExtendedPluginInterface. Called execute().", plugin.getName(), pluginId);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Basic plugin executed (may not process input or return structured data).");
                return result;
            }
        } catch (Exception e) {
            log.error("[2025-05-06 18:16:29] Kostovite: Error during execution of plugin '{}' (ID: '{}'): {}", plugin.getName(), pluginId, e.getMessage(), e);
            throw new RuntimeException("Error processing plugin '" + plugin.getName() + "': " + e.getMessage(), e);
        }
    }

    public PluginInterface getAccessiblePlugin(String pluginId, Authentication authentication) {
        PluginStatusCheckResult statusResult = isPluginEnabled(pluginId);
        if (!statusResult.isEnabled()) {
            log.debug("[2025-05-06 18:16:29] Kostovite: getAccessiblePlugin: Plugin ID '{}' is disabled (Reason: {}).", pluginId, statusResult.message());
            return null;
        }

        PluginInterface plugin = manualPluginLoader.getPluginByName(pluginId);

        if (plugin == null) { return null; }

        String userType = extractUserType(authentication);
        Map<String, Object> metadata = getPluginMetadataSafely(plugin);
        String pluginAccessLevel = "normal";

        if (metadata != null) {
            pluginAccessLevel = String.valueOf(metadata.getOrDefault("accessLevel", "normal")).toLowerCase();
            if (!List.of("normal", "premium", "admin").contains(pluginAccessLevel)) {
                pluginAccessLevel = "normal";
            }
        } else {
            log.warn("[2025-05-06 18:16:29] Kostovite: Could not retrieve metadata for plugin ID '{}' during accessible check. Assuming 'normal'.", pluginId);
        }

        if (!canUserAccess(userType, pluginAccessLevel)) { return null; }
        return plugin;
    }

    public List<PluginInterface> getAllLoadedPlugins() {
        return manualPluginLoader.getLoadedPlugins();
    }

    public record PluginStatusCheckResult(boolean isEnabled, String message) {}

    public static class PluginDisabledException extends RuntimeException {
        public PluginDisabledException(String message) { super(message); }
    }
}