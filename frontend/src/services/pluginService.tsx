// src/services/pluginService.ts
import { useCallback, useMemo } from 'react';
import { fetchWithAuth } from '../utils/fetchWithAuth'; // Ensure path is correct
import { useAuth } from '../context/AuthContext';       // Ensure path is correct
// Firestore imports are NOT needed in this file if it only makes API calls
// import { doc, setDoc, collection, query, getDocs, where } from "firebase/firestore";
// import { db } from "../firebaseConfig";


// Helper for logging (if you keep it here or move to a shared util)
const getLogPrefix = (currentUser: any) => `[${new Date().toISOString()}] [User: ${currentUser?.uid ?? (currentUser?.email ?? 'anonymous')}]`;

interface Tool { // Keep this if other methods in this service return/use it
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin';
    status?: 'enabled' | 'disabled';
    description?: string;
    sections?: any[];
    uiConfig?: any;
}


const usePluginService = () => {
    const { getIdToken, currentUser } = useAuth();

    // getAvailablePlugins, getPluginMetadata, processPlugin - keep as they are

    const getAvailablePlugins = useCallback(async (): Promise<Tool[]> => {
        const logP = getLogPrefix(currentUser);
        console.log(`${logP} [pluginService] Attempting to fetch available plugins...`);
        // ... (existing implementation)
        try {
            const response = await fetchWithAuth('/api/plugins', { method: 'GET' }, getIdToken);
            if (!response.ok) { /* ... error handling ... */ }
            const data: Tool[] = await response.json();
            if (!Array.isArray(data)) { /* ... error handling ... */ }
            console.log(`${logP} [pluginService] Successfully fetched ${data.length} plugins.`);
            return data;
        } catch (error) { /* ... error handling ... */ throw error; }
    }, [getIdToken, currentUser]);


    // --- updateToolInFirestore and getToolNameById ARE NO LONGER NEEDED IN THIS SERVICE ---
    // --- if their sole purpose was to support deletePlugin's Firestore interaction. ---
    // --- AllToolsContext will handle its own Firestore interactions. ---
    /*
    const updateToolInFirestore = async (toolName: string, data: any) => { ... };
    const getToolNameById = async (toolId: string): Promise<string | null> => { ... };
    */

    // updateToolStatus & updateToolAccessLevel:
    // These currently call the (now removed) updateToolInFirestore and then a mock API call.
    // If their purpose is to *actually update the backend server's state* for status/access,
    // they should be making real API calls via fetchWithAuth to backend endpoints.
    // If they are ONLY for updating the Firestore 'tools' collection, that logic should be
    // moved entirely into AllToolsContext.tsx.
    // For now, I'll leave them as stubs as they were, but you need to decide their true purpose.

    const updateToolStatus = useCallback(async (toolId: string, status: 'enabled' | 'disabled'): Promise<any> => {
        const logP = getLogPrefix(currentUser);
        console.warn(`${logP} [pluginService] STUB: updateToolStatus called for ${toolId} to ${status}. No backend API endpoint defined for this yet.`);
        // If you create a backend endpoint:
        // return fetchWithAuth(`/api/admin/tools/${toolId}/status`, { method: 'POST', body: JSON.stringify({ status }) }, getIdToken);
        return Promise.resolve({ success: true, message: "Mock status update." });
    }, [getIdToken, currentUser]);

    const updateToolAccessLevel = useCallback(async (toolId: string, accessLevel: 'normal' | 'premium'): Promise<any> => {
        const logP = getLogPrefix(currentUser);
        console.warn(`${logP} [pluginService] STUB: updateToolAccessLevel called for ${toolId} to ${accessLevel}. No backend API endpoint defined for this yet.`);
        // If you create a backend endpoint:
        // return fetchWithAuth(`/api/admin/tools/${toolId}/access`, { method: 'POST', body: JSON.stringify({ accessLevel }) }, getIdToken);
        return Promise.resolve({ success: true, message: "Mock access level update." });
    }, [getIdToken, currentUser]);


    // Method to delete a plugin JAR via backend API
    const deletePlugin = useCallback(async (identifierForBackendPath: string): Promise<any> => {
        const logP = getLogPrefix(currentUser);
        console.log(`${logP} [pluginService] Attempting to call backend to delete plugin: ${identifierForBackendPath}`);

        if (!currentUser || !getIdToken) {
            console.error(`${logP} [pluginService] deletePlugin requires an authenticated admin user.`);
            throw new Error("User not authenticated or auth context error for delete operation.");
        }

        try {
            // --- REMOVED: Firestore update to mark as disabled from here ---
            // const toolName = await getToolNameById(identifierForBackendPath); // This logic might be complex if identifierForBackendPath is ID and toolName is needed
            // if (toolName) {
            //     await updateToolInFirestore(toolName, { enabled: false });
            //     console.log(`${logP} [pluginService] (Removed logic) Marked tool as disabled in Firestore`);
            // }
            // --- END REMOVED ---

            console.log(`${logP} [pluginService] Calling backend DELETE API: /api/plugins/${identifierForBackendPath}/delete`);
            const response = await fetchWithAuth(
                `/api/plugins/${encodeURIComponent(identifierForBackendPath)}/delete`,
                { method: 'DELETE' },
                getIdToken
            );

            if (!response.ok) {
                let errorData;
                try { errorData = await response.json(); }
                catch (e) { errorData = { message: await response.text() || `Failed to delete plugin from backend: ${response.status}` }; }
                console.error(`${logP} [pluginService] API Error deleting plugin ${identifierForBackendPath} from backend: ${response.status}`, errorData);
                throw new Error(errorData.message || `Failed to delete plugin from backend: ${response.status}`);
            }

            try {
                const data = await response.json();
                console.log(`${logP} [pluginService] Backend successfully processed deletion for plugin ${identifierForBackendPath}:`, data);
                return data;
            } catch (e) {
                console.log(`${logP} [pluginService] Backend deletion for ${identifierForBackendPath} successful, no JSON response body.`);
                return { success: true, message: "Plugin deletion successfully initiated with backend." };
            }
        } catch (error) {
            console.error(`${logP} [pluginService] Error in deletePlugin service call for ${identifierForBackendPath}:`, error);
            throw error; // Re-throw for AllToolsContext to handle
        }
    }, [getIdToken, currentUser]); // Removed getAvailablePlugins, updateToolInFirestore from deps if getToolNameById was removed


    // getPluginMetadata, processPlugin - keep as they are
    const getPluginMetadata = useCallback(async (pluginId: string): Promise<any> => { /* ... */ }, [getIdToken, currentUser]);
    const processPlugin = useCallback(async (pluginId: string, inputData: any): Promise<any> => { /* ... */ }, [getIdToken, currentUser]);


    return useMemo(() => ({
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
        updateToolStatus,          // Keep if you plan to make these real API calls
        updateToolAccessLevel,     // Keep if you plan to make these real API calls
        deletePlugin               // Expose the corrected deletePlugin
    }), [
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
        updateToolStatus,
        updateToolAccessLevel,
        deletePlugin
    ]);
};

export default usePluginService;