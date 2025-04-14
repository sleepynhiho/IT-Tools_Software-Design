/**
 * API functions to interact with the plugin backend system
 */

/**
 * Fetch plugin metadata from backend
 */
export const fetchPluginMetadata = async (): Promise<any[]> => {
  try {
    // Fetch the list of plugin names first
    const pluginListRes = await fetch(`/api/plugins/universal/manual-load`);
    
    if (!pluginListRes.ok) {
      throw new Error(`Failed to load plugin list: ${pluginListRes.status} ${pluginListRes.statusText}`);
    }
    
    const pluginListData = await pluginListRes.json();
    const { loadedPlugins } = pluginListData;
    
    if (!Array.isArray(loadedPlugins)) {
      throw new Error("Invalid plugin list format received from server.");
    }
    
    // Fetch metadata for each plugin
    const metadataPromises = loadedPlugins.map(async (pluginName: string) => {
      const url = `/api/plugins/universal/${pluginName}/metadata`;
      const res = await fetch(url);
      
      if (!res.ok) {
        throw new Error(`${res.status} ${res.statusText}`);
      }
      
      return res.json();
    });
    
    const metadataResults = await Promise.all(metadataPromises);
    return metadataResults;
  } catch (error) {
    console.error("Failed to fetch plugin metadata:", error);
    // Return empty array or fallback to mock data
    return [];
  }
};

/**
 * Process plugin requests through backend
 */
export const processPluginRequest = async (
  pluginId: string,
  operation: string,
  inputData: Record<string, any>
): Promise<any> => {
  try {
    const response = await fetch(`/api/plugins/universal/${pluginId}/process`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        uiOperation: operation,
        ...inputData,
      }),
    });

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error(`Plugin '${pluginId}' execution failed:`, error);
    return {
      success: false,
      error: "Backend not available or request failed.",
    };
  }
};