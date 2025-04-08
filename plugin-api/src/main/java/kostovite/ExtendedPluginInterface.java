package kostovite;

import java.util.Map;

/**
 * Extended interface for plugins with advanced functionality
 */
public interface ExtendedPluginInterface extends PluginInterface {
    /**
     * Get metadata about the plugin
     * @return Map containing plugin metadata
     */
    Map<String, Object> getMetadata();

    /**
     * Process data universally
     * @param input Map containing input data
     * @return Map containing processed output data
     */
    Map<String, Object> process(Map<String, Object> input);
}