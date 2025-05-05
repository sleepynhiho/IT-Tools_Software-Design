import { useCallback } from 'react';
import { fetchWithAuth } from '../utils/fetchWithAuth'; // <<<--- IMPORT THE HELPER
import { useAuth } from '../context/AuthContext'; // To get the token function

// Define or import the Tool interface (ensure it includes accessLevel if needed by frontend)
interface Tool {
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin'; // Optional here if backend filters completely
    description?: string;
    // Other properties...
}

// Structure as a custom hook to use useAuth()
const usePluginService = () => {
    const { getIdToken, currentUser } = useAuth(); // Get the token function and user status

    // --- Method to get the filtered list of plugins for the current user ---
    const getAvailablePlugins = useCallback(async (): Promise<Tool[]> => {
        console.log("[pluginService] Attempting to fetch available plugins...");

        // Check if user is potentially logged in before trying to get token
        if (!currentUser) {
            console.warn("[pluginService] No current user found. Cannot fetch authenticated plugins.");
            // Return empty or throw, depending on desired behavior for logged-out users
             return [];
            // throw new Error("User not logged in.");
        }

        // Ensure getIdToken is available (should be if currentUser exists)
        if (!getIdToken) {
            console.error("[pluginService] getIdToken function unavailable.");
            throw new Error("Authentication context error.");
        }

        try {
            // Use fetchWithAuth for the backend-filtered endpoint
            const response = await fetchWithAuth(
                '/api/plugins', // <<<--- Endpoint handled by PluginController.getPluginsForCurrentUser
                { method: 'GET' },
                getIdToken // Pass the function from useAuth
            );

            if (!response.ok) {
                const errorBody = await response.text(); // Read body for details
                console.error(`[pluginService] API Error ${response.status}: ${errorBody}`);
                 if (response.status === 401 || response.status === 403) {
                     throw new Error(`Failed to fetch plugins: ${response.status} (Authentication/Authorization Error - Check Token/Roles)`);
                 }
                 throw new Error(`Failed to fetch plugins: ${response.status} (Server Error)`);
            }

            // Expecting an array of Tool objects (metadata) directly from backend
            const data: Tool[] = await response.json();
            console.log(`[pluginService] Successfully fetched ${data.length} plugins for user.`);
            return Array.isArray(data) ? data : [];

        } catch (error) {
            console.error('[pluginService] Error in getAvailablePlugins:', error);
            throw error; // Re-throw for the context to handle
        }
    }, [getIdToken, currentUser]); // Depend on getIdToken and currentUser

    // --- Method to get specific metadata (if still needed separately) ---
    const getPluginMetadata = useCallback(async (pluginId: string): Promise<any> => {
         console.log(`[pluginService] Attempting to fetch metadata for: ${pluginId}`);
         if (!currentUser || !getIdToken) throw new Error("User not authenticated or auth context error.");

         const response = await fetchWithAuth(
             `/api/plugins/universal/${pluginId}/metadata`, // Use the correct metadata endpoint
             { method: 'GET' },
             getIdToken
         );
         if (!response.ok) { /* Handle error */ throw new Error(`Metadata fetch failed: ${response.status}`); }
         return await response.json();
    }, [getIdToken, currentUser]);

    // --- Method to process plugin data ---
    const processPlugin = useCallback(async (pluginId: string, inputData: any): Promise<any> => {
         console.log(`[pluginService] Attempting to process plugin: ${pluginId}`);
          if (!currentUser || !getIdToken) throw new Error("User not authenticated or auth context error.");

         const response = await fetchWithAuth(
             // Use the universal process endpoint or the debug one? Choose one.
             `/api/plugins/universal/${pluginId}`, // Assuming POST to universal path
             {
                 method: 'POST',
                 body: JSON.stringify(inputData)
                 // fetchWithAuth adds Headers automatically
             },
             getIdToken
         );
          if (!response.ok) { /* Handle error */ throw new Error(`Processing failed: ${response.status}`); }
         return await response.json();
    }, [getIdToken, currentUser]);

    // Return the service methods
    return {
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
    };
};

export default usePluginService;