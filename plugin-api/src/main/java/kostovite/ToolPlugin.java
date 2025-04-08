package kostovite;

import java.util.Map;

/**
 * Extended interface for tools with categorization
 */
public interface ToolPlugin extends ExtendedPluginInterface {
    /**
     * Get the category this tool belongs to
     * @return Category name
     */
    String getCategory();

    /**
     * Get a short description of this tool
     * @return Tool description
     */
    String getDescription();
}