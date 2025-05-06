// src/services/pluginService.ts
import { useCallback, useMemo } from 'react'; // <<<--- ADD useMemo
import { fetchWithAuth } from '../utils/fetchWithAuth';
import { useAuth } from '../context/AuthContext';

// Define or import the Tool interface
interface Tool {
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin';
    description?: string;
    // Other properties...
}

const usePluginService = () => {
    const { getIdToken, currentUser } = useAuth();

    const getAvailablePlugins = useCallback(async (): Promise<Tool[]> => {
        console.log("[pluginService] Attempting to fetch available plugins...");
        if (!currentUser) {
            console.warn("[pluginService] No current user found. Cannot fetch authenticated plugins.");
            return [];
        }
        if (!getIdToken) {
            console.error("[pluginService] getIdToken function unavailable.");
            throw new Error("Authentication context error.");
        }
        try {
            const response = await fetchWithAuth(
                '/api/plugins',
                { method: 'GET' },
                getIdToken
            );
            if (!response.ok) {
                const errorBody = await response.text();
                console.error(`[pluginService] API Error ${response.status}: ${errorBody}`);
                if (response.status === 401 || response.status === 403) {
                    throw new Error(`Failed to fetch plugins: ${response.status} (Authentication/Authorization Error - Check Token/Roles)`);
                }
                throw new Error(`Failed to fetch plugins: ${response.status} (Server Error)`);
            }
            const data: Tool[] = await response.json();
            console.log(`[pluginService] Successfully fetched ${data.length} plugins for user.`);
            return Array.isArray(data) ? data : [];
        } catch (error) {
            console.error('[pluginService] Error in getAvailablePlugins:', error);
            throw error;
        }
    }, [getIdToken, currentUser]);

    const getPluginMetadata = useCallback(async (pluginId: string): Promise<any> => {
        console.log(`[pluginService] Attempting to fetch metadata for: ${pluginId}`);
        if (!currentUser || !getIdToken) throw new Error("User not authenticated or auth context error.");
        const response = await fetchWithAuth(
            `/api/plugins/universal/${pluginId}/metadata`,
            { method: 'GET' },
            getIdToken
        );
        if (!response.ok) { throw new Error(`Metadata fetch failed: ${response.status}`); }
        return await response.json();
    }, [getIdToken, currentUser]);

    const processPlugin = useCallback(async (pluginId: string, inputData: any): Promise<any> => {
        console.log(`[pluginService] Attempting to process plugin: ${pluginId}`);
        if (!currentUser || !getIdToken) throw new Error("User not authenticated or auth context error.");
        const response = await fetchWithAuth(
            `/api/plugins/universal/${pluginId}`,
            {
                method: 'POST',
                body: JSON.stringify(inputData)
            },
            getIdToken
        );
        if (!response.ok) { throw new Error(`Processing failed: ${response.status}`); }
        return await response.json();
    }, [getIdToken, currentUser]);

    // --- Return a memoized object containing these callbacks ---
    return useMemo(() => ({
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
    }), [getAvailablePlugins, getPluginMetadata, processPlugin]); // Dependencies are the stable callbacks
};

export default usePluginService;