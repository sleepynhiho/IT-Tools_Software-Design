package kostovite;

import java.util.Map;

public interface PluginInterface {
    /**
     * Get the name of the plugin
     */
    String getName();

    /**
     * Execute the default plugin behavior
     */
    void execute();

    /**
     * Get metadata about the plugin including available operations
     * @return Map containing plugin metadata
     */
    Map<String, Object> getMetadata();

    /**
     * Process data universally without needing to know specific plugin implementation
     * @param input Map containing input data
     * @return Map containing processed output data
     */
    Map<String, Object> process(Map<String, Object> input);
}