// src/services/pluginService.ts
import { useCallback, useMemo } from 'react';
import { fetchWithAuth } from '../utils/fetchWithAuth';
import { useAuth } from '../context/AuthContext';

// Tool Interface
interface Tool {
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin';
    description?: string;
    sections?: any[]; // Or specific type
    uiConfig?: any; // Or specific type
}

const usePluginService = () => {
    // Still get currentUser and getIdToken for OTHER methods that might need them
    const { getIdToken, currentUser } = useAuth();

    const getAvailablePlugins = useCallback(async (): Promise<Tool[]> => {
        console.log("[pluginService] Attempting to fetch available plugins...");

        // --- CHANGE: REMOVE THE BLOCKING CHECK for !currentUser ---
        /*
        if (!currentUser) {
            console.warn("[pluginService] No current user found. Cannot fetch authenticated plugins.");
            return []; // REMOVED THIS BLOCK
        }
        */
        // We proceed regardless of currentUser status for this specific endpoint.

        // The getIdToken function itself might be null if context isn't ready,
        // but fetchWithAuth can handle a null token function if needed, or we rely on it being ready.
        // Let's assume AuthProvider ensures getIdToken is available when !isAuthLoading
        if (!getIdToken) {
             console.error("[pluginService] getIdToken function potentially unavailable (AuthContext issue?).");
             // Depending on strictness, either throw or try to proceed without token capability for fetchWithAuth
             // throw new Error("Authentication context error: getIdToken unavailable.");
        }

        try {
            // Call fetchWithAuth. It will use getIdToken IF needed/available,
            // otherwise it will proceed without sending an Authorization header,
            // which is correct for anonymous requests to a permitAll endpoint.
            console.log("[pluginService] Calling fetchWithAuth for GET /api/plugins (User:", currentUser?.uid ?? "anonymous", ")");
            const response = await fetchWithAuth(
                '/api/plugins', // Target endpoint (BE allows anonymous GET)
                { method: 'GET' },
                getIdToken // Pass the function; fetchWithAuth will call it (and it handles null currentUser)
            );

            if (!response.ok) {
                const errorBody = await response.text();
                console.error(`[pluginService] API Error ${response.status}: ${errorBody}`);
                 // Handle specific errors if needed (401/403 shouldn't happen for anonymous on permitAll GET)
                throw new Error(`Failed to fetch plugins: ${response.status} (${response.statusText})`);
            }

            const data: Tool[] = await response.json();

             // --- Add Check: Ensure data is an array ---
             if (!Array.isArray(data)) {
                 console.error("[pluginService] Received non-array data:", data);
                 throw new Error("Invalid data format received from server.");
             }

            console.log(`[pluginService] Successfully fetched ${data.length} plugins (User: ${currentUser?.uid ?? "anonymous"})`);
            return data; // Return the array (potentially empty if backend filtered all)

        } catch (error) {
            console.error('[pluginService] Error in getAvailablePlugins:', error);
            throw error; // Re-throw for AllToolsContext to handle
        }
        // --- CHANGE: Adjust dependencies ---
        // Since we removed the direct currentUser check inside, we might only depend on
        // getIdToken if other parts of the service *always* require it.
        // However, keeping currentUser ensures this specific callback instance is
        // consistent with the auth state, which is usually safer.
    }, [getIdToken, currentUser]);

    // --- Other methods (getPluginMetadata, processPlugin) still require currentUser ---
    const getPluginMetadata = useCallback(async (pluginId: string): Promise<any> => {
        console.log(`[pluginService] Attempting to fetch metadata for: ${pluginId}`);
        // These methods *should* check for currentUser
        if (!currentUser || !getIdToken) {
             console.error("[pluginService] getPluginMetadata requires authenticated user.");
             throw new Error("User not authenticated or auth context error.");
        }
        const response = await fetchWithAuth( /* ... */ );
        // ...
    }, [getIdToken, currentUser]);

    const processPlugin = useCallback(async (pluginId: string, inputData: any): Promise<any> => {
        console.log(`[pluginService] Attempting to process plugin: ${pluginId}`);
         // These methods *should* check for currentUser
         if (!currentUser || !getIdToken) {
             console.error("[pluginService] processPlugin requires authenticated user.");
             throw new Error("User not authenticated or auth context error.");
         }
        const response = await fetchWithAuth( /* ... */ );
        // ...
    }, [getIdToken, currentUser]);

    // --- Memoize the returned object ---
    return useMemo(() => ({
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
    }), [getAvailablePlugins, getPluginMetadata, processPlugin]);
};

export default usePluginService;