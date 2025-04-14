/**
 * Service for interacting with the plugin API
 */
const pluginService = {
    /**
     * Process data with a specific plugin
     * @param {string} pluginId - The ID of the plugin to use
     * @param {Object} inputData - The input data to process
     * @returns {Promise<Object>} - The processing result
     */
    processPlugin: async (pluginId, inputData) => {
      try {
        // Log data sizes for debugging (not sensitive content)
        console.log(`Processing plugin ${pluginId} with inputs:`, 
          Object.keys(inputData).reduce((acc, key) => {
            if (typeof inputData[key] === 'string' && inputData[key].length > 1000) {
              acc[key] = `[${inputData[key].length} characters]`;
            } else {
              acc[key] = inputData[key];
            }
            return acc;
          }, {})
        );
        
        // Check for large data (like images or videos)
        const hasLargeData = Object.values(inputData).some(
          value => typeof value === 'string' && value.length > 100000
        );
        
        // Set up request options
        const options = {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(inputData)
        };
        
        // Use a timeout for large requests
        const timeout = hasLargeData ? 60000 : 30000;
        
        // Make the request
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeout);
        
        const response = await fetch(`/api/plugins/universal/${pluginId}/process`, {
          ...options,
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
          let errorMessage;
          try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorData.error || `Server error (${response.status})`;
          } catch (e) {
            errorMessage = `Server error (${response.status})`;
          }
          throw new Error(errorMessage);
        }
        
        const result = await response.json();
        return result;
      } catch (error) {
        if (error.name === 'AbortError') {
          throw new Error('Request timed out. The image/video may be too large.');
        }
        console.error('Plugin processing error:', error);
        throw error;
      }
    },
    
    /**
     * Get metadata for a specific plugin
     * @param {string} pluginId - The ID of the plugin
     * @returns {Promise<Object>} - The plugin metadata
     */
    getPluginMetadata: async (pluginId) => {
      try {
        const response = await fetch(`/api/plugins/universal/${pluginId}/metadata`);
        
        if (!response.ok) {
          throw new Error(`Failed to fetch plugin metadata: ${response.status}`);
        }
        
        return await response.json();
      } catch (error) {
        console.error(`Error fetching metadata for ${pluginId}:`, error);
        throw error;
      }
    },
    
    /**
     * Get list of available plugins
     * @returns {Promise<Array<string>>} - List of plugin IDs
     */
    getAvailablePlugins: async () => {
      try {
        const response = await fetch('/api/plugins/universal/manual-load');
        
        if (!response.ok) {
          throw new Error(`Failed to fetch plugins: ${response.status}`);
        }
        
        const data = await response.json();
        return data.loadedPlugins || [];
      } catch (error) {
        console.error('Error fetching available plugins:', error);
        throw error;
      }
    }
  };
  
  export default pluginService;