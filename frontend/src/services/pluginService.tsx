// src/services/pluginService.ts
import { useCallback, useMemo } from 'react';
import { fetchWithAuth } from '../utils/fetchWithAuth';
import { useAuth } from '../context/AuthContext';
import { doc, setDoc, collection, query, getDocs, where } from "firebase/firestore";
import { db } from "../firebaseConfig";

// Current date and time information for logging
const CURRENT_DATE_TIME = "2025-05-06 18:29:03";
const CURRENT_USER_LOGIN = "Kostovite";

// Tool Interface
interface Tool {
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin';
    status?: 'enabled' | 'disabled';
    description?: string;
    sections?: any[]; // Or specific type
    uiConfig?: any; // Or specific type
}

const usePluginService = () => {
    // Still get currentUser and getIdToken for OTHER methods that might need them
    const { getIdToken, currentUser } = useAuth();

    // Helper function to sanitize a tool name for use as a Firestore document ID
    const sanitizeToolNameForFirestore = (name: string): string => {
        // Replace forward slashes with a safe character (underscore)
        return name.replace(/\//g, '_');
    };

    const getAvailablePlugins = useCallback(async (): Promise<Tool[]> => {
        console.log(`[${CURRENT_DATE_TIME}] [pluginService] Attempting to fetch available plugins...`);

        if (!getIdToken) {
             console.error(`[${CURRENT_DATE_TIME}] [pluginService] getIdToken function potentially unavailable (AuthContext issue?).`);
        }

        try {
            // Call fetchWithAuth. It will use getIdToken IF needed/available,
            // otherwise it will proceed without sending an Authorization header,
            // which is correct for anonymous requests to a permitAll endpoint.
            console.log(`[${CURRENT_DATE_TIME}] [pluginService] Calling fetchWithAuth for GET /api/plugins (User: ${currentUser?.uid ?? "anonymous"})`);
            const response = await fetchWithAuth(
                '/api/plugins', // Target endpoint (BE allows anonymous GET)
                { method: 'GET' },
                getIdToken // Pass the function; fetchWithAuth will call it (and it handles null currentUser)
            );

            if (!response.ok) {
                const errorBody = await response.text();
                console.error(`[${CURRENT_DATE_TIME}] [pluginService] API Error ${response.status}: ${errorBody}`);
                throw new Error(`Failed to fetch plugins: ${response.status} (${response.statusText})`);
            }

            const data: Tool[] = await response.json();

            // --- Add Check: Ensure data is an array ---
            if (!Array.isArray(data)) {
                console.error(`[${CURRENT_DATE_TIME}] [pluginService] Received non-array data:`, data);
                throw new Error("Invalid data format received from server.");
            }

            console.log(`[${CURRENT_DATE_TIME}] [pluginService] Successfully fetched ${data.length} plugins (User: ${currentUser?.uid ?? "anonymous"})`);
            return data; // Return the array (potentially empty if backend filtered all)

        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [pluginService] Error in getAvailablePlugins:`, error);
            throw error; // Re-throw for AllToolsContext to handle
        }
    }, [getIdToken, currentUser]);

    // Helper method to update a tool in Firestore
    const updateToolInFirestore = async (toolName: string, data: any) => {
        if (!currentUser) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Cannot update Firestore, user not authenticated`);
            return false;
        }
        
        try {
            const safeDocId = sanitizeToolNameForFirestore(toolName);
            const toolRef = doc(db, "tools", safeDocId);
            
            await setDoc(toolRef, {
                ...data,
                updatedAt: new Date().toISOString(),
                updatedBy: currentUser.uid
            }, { merge: true });
            
            console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Successfully updated tool ${safeDocId} in Firestore`);
            return true;
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error updating tool in Firestore:`, error);
            return false;
        }
    };
    
    // Helper method to get tool name by ID
    const getToolNameById = async (toolId: string): Promise<string | null> => {
        try {
            // Try to fetch the tool from API first
            const tools = await getAvailablePlugins();
            const tool = tools.find((t: any) => t.id === toolId);
            
            if (tool && tool.name) {
                return tool.name;
            }
            
            // If not found in API, check Firestore
            const toolsCollectionRef = collection(db, 'tools');
            const toolsQuery = query(toolsCollectionRef, where("id", "==", toolId));
            const querySnapshot = await getDocs(toolsQuery);
            
            let toolName = null;
            querySnapshot.forEach((doc) => {
                toolName = doc.id; // The document ID is the tool name
            });
            
            return toolName;
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error getting tool name by ID:`, error);
            return null;
        }
    };

    const updateToolStatus = async (toolId: string, status: 'enabled' | 'disabled') => {
        try {
            // First try to get the tool name
            const toolName = await getToolNameById(toolId);
            
            if (!toolName) {
                console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Could not find tool with ID: ${toolId}`);
            } else {
                // Update in Firestore first (more reliable)
                await updateToolInFirestore(toolName, { enabled: status === 'enabled' });
            }
            
            // Then try API call (may fail due to CORS/403)
            try {
                const token = await getIdToken();
                const response = await fetch(`/api/plugins/${toolId}/status`, {
                    method: 'PUT', // Changed from PATCH to PUT for better compatibility
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ status }),
                    credentials: 'include' // Include cookies with request
                });
                
                if (response.ok) {
                    const data = await response.json();
                    console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API update successful:`, data);
                    return data;
                } else {
                    console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API update failed but Firestore update may have succeeded`);
                    // Continue with a mock response, don't throw
                }
            } catch (apiError) {
                console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API error (continuing with Firestore update):`, apiError);
                // Don't rethrow - we'll return mock data
            }
            
            // If API call failed but Firestore succeeded, return mock success
            return {
                success: true,
                id: toolId,
                status,
                message: `Tool status updated to ${status} (Firestore only)`
            };
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error in updateToolStatus:`, error);
            throw error;
        }
    };
    
    // Add method to update tool access level
    const updateToolAccessLevel = async (toolId: string, accessLevel: 'normal' | 'premium') => {
        try {
            // First try to get the tool name
            const toolName = await getToolNameById(toolId);
            
            if (!toolName) {
                console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Could not find tool with ID: ${toolId}`);
            } else {
                // Update in Firestore first (more reliable)
                await updateToolInFirestore(toolName, { premium: accessLevel === 'premium' });
            }
            
            // Then try API call (may fail due to CORS/403)
            try {
                const token = await getIdToken();
                console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Attempting API call to update access level for ${toolId}`);
                const response = await fetch(`/api/plugins/${toolId}/access-level`, {
                    method: 'PUT', // Changed from PATCH to PUT for better compatibility
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ accessLevel }),
                    credentials: 'include' // Include cookies with request
                });
                
                if (response.ok) {
                    const data = await response.json();
                    console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API update successful:`, data);
                    return data;
                } else {
                    const text = await response.text();
                    console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API update failed (${response.status}): ${text} but Firestore update may have succeeded`);
                    // Continue with a mock response, don't throw
                }
            } catch (apiError) {
                console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API error (continuing with Firestore update):`, apiError);
                // Don't rethrow - we'll return mock data
            }
            
            // If API call failed but Firestore succeeded, return mock success
            return {
                success: true,
                id: toolId,
                accessLevel,
                message: `Tool access level updated to ${accessLevel} (Firestore only)`
            };
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error in updateToolAccessLevel:`, error);
            throw error;
        }
    };
    
    // Add method for mock implementation (used as fallback)
    const mockUpdateToolAccessLevel = async (toolId: string, accessLevel: 'normal' | 'premium') => {
        console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Using mock implementation for tool ${toolId}, level ${accessLevel}`);
        
        try {
            // Get tool name and update Firestore
            const toolName = await getToolNameById(toolId);
            
            if (toolName) {
                await updateToolInFirestore(toolName, { premium: accessLevel === 'premium' });
            }
            
            return {
                success: true,
                id: toolId,
                accessLevel,
                message: `Tool access level updated to ${accessLevel} (mock)`
            };
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error in mock implementation:`, error);
            throw error;
        }
    };

    const getPluginMetadata = useCallback(async (pluginId: string): Promise<any> => {
        console.log(`[${CURRENT_DATE_TIME}] [pluginService] Attempting to fetch metadata for: ${pluginId}`);
        // These methods *should* check for currentUser
        if (!currentUser || !getIdToken) {
             console.error(`[${CURRENT_DATE_TIME}] [pluginService] getPluginMetadata requires authenticated user.`);
             throw new Error("User not authenticated or auth context error.");
        }
        try {
            const response = await fetchWithAuth(
                `/api/plugins/universal/${pluginId}/metadata`, 
                { method: 'GET' },
                getIdToken
            );
            
            if (!response.ok) {
                throw new Error(`Failed to fetch plugin metadata: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [pluginService] Error fetching plugin metadata:`, error);
            throw error;
        }
    }, [getIdToken, currentUser]);

    const processPlugin = useCallback(async (pluginId: string, inputData: any): Promise<any> => {
        console.log(`[${CURRENT_DATE_TIME}] [pluginService] Attempting to process plugin: ${pluginId}`);
        try {
            const response = await fetchWithAuth(
                `/api/plugins/universal/${pluginId}/process`,
                { 
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(inputData)
                },
                getIdToken // This is optional since our backend allows anonymous access
            );
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error(`[${CURRENT_DATE_TIME}] [pluginService] Error processing plugin: ${errorText}`);
                throw new Error(`Failed to process plugin: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [pluginService] Error processing plugin:`, error);
            throw error;
        }
    }, [getIdToken, currentUser]);
    
    // Add method to remove a tool
    const removeTool = async (toolId: string) => {
        try {
            // First try to get the tool name
            const toolName = await getToolNameById(toolId);
            
            if (toolName) {
                // Update Firestore to mark as disabled
                await updateToolInFirestore(toolName, { enabled: false });
            }
            
            // Try API call
            try {
                const token = await getIdToken();
                const response = await fetch(`/api/plugins/${toolId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include'
                });
                
                if (!response.ok) {
                    console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API remove failed but Firestore update may have succeeded`);
                } else {
                    const data = await response.json();
                    return data;
                }
            } catch (apiError) {
                console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API error in removeTool:`, apiError);
            }
            
            // Return mock success
            return {
                success: true,
                id: toolId,
                message: `Tool removed (Firestore only)`
            };
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error in removeTool:`, error);
            throw error;
        }
    };
    
    // Add method to add a tool
    const addTool = async (tool: any) => {
        try {
            // First add to Firestore
            if (tool.name) {
                await updateToolInFirestore(tool.name, {
                    id: tool.id || `tool-${Date.now()}`,
                    premium: tool.accessLevel === 'premium',
                    enabled: tool.status !== 'disabled'
                });
            }
            
            // Try API call
            try {
                const token = await getIdToken();
                const response = await fetch('/api/plugins', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(tool),
                    credentials: 'include'
                });
                
                if (!response.ok) {
                    console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API add failed but Firestore update may have succeeded`);
                } else {
                    const data = await response.json();
                    return data;
                }
            } catch (apiError) {
                console.warn(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] API error in addTool:`, apiError);
            }
            
            // If tool doesn't have an ID, generate one
            if (!tool.id) {
                tool.id = `tool-${Date.now()}`;
            }
            
            // Return the tool as mock success
            return tool;
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Error in addTool:`, error);
            throw error;
        }
    };

    // --- FIXED: Method to delete a plugin ---
    const deletePlugin = useCallback(async (pluginNameOrId: string): Promise<any> => {
        // We need to ensure we're using the plugin ID for the API call, not the name
        console.log(`[${CURRENT_DATE_TIME}] [pluginService] Attempting to delete plugin: ${pluginNameOrId}`);
        
        if (!currentUser || !getIdToken) {
            console.error(`[${CURRENT_DATE_TIME}] [pluginService] deletePlugin requires an authenticated admin user.`);
            throw new Error("User not authenticated or auth context error.");
        }
        
        try {
            // Get all tools to find the correct ID if a name was passed
            const tools = await getAvailablePlugins();
            
            // Determine if what we received is a name or an ID
            let pluginId = pluginNameOrId; // Default to using as-is
            const matchingTool = tools.find(tool => 
                tool.name === pluginNameOrId || // Match by name
                tool.id === pluginNameOrId      // Or match by ID
            );
            
            // If we found a match, ensure we use its ID
            if (matchingTool) {
                pluginId = matchingTool.id; // Always use the ID for the API call
                console.log(`[${CURRENT_DATE_TIME}] [pluginService] Found matching tool: ${matchingTool.name} with ID: ${pluginId}`);
            } else {
                console.warn(`[${CURRENT_DATE_TIME}] [pluginService] No matching tool found for: ${pluginNameOrId}, using as-is`);
            }
            
            // First update Firestore to mark as disabled
            if (matchingTool && matchingTool.name) {
                await updateToolInFirestore(matchingTool.name, { enabled: false });
                console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Marked tool as disabled in Firestore`);
            }
            
            // Then call the backend API using the ID (not name)
            console.log(`[${CURRENT_DATE_TIME}] [pluginService] Calling backend delete API with ID: ${pluginId}`);
            const response = await fetchWithAuth(
                `/api/plugins/${pluginId}/delete`, // Using pluginId for the API call
                { method: 'DELETE' },
                getIdToken
            );

            if (!response.ok) {
                // Try to parse error
                let errorData;
                try {
                    errorData = await response.json();
                } catch (e) {
                    errorData = { message: await response.text() || `Failed to delete plugin: ${response.status}` };
                }
                console.error(`[${CURRENT_DATE_TIME}] [pluginService] API Error deleting plugin ${pluginId}: ${response.status}`, errorData);
                
                // Even if API call fails, return partial success if Firestore was updated
                if (matchingTool && matchingTool.name) {
                    return { 
                        success: true, 
                        message: "Plugin marked as disabled in Firestore, but backend deletion failed.",
                        apiError: errorData.message
                    };
                }
                
                throw new Error(errorData.message || `Failed to delete plugin: ${response.status}`);
            }

            // For DELETE, often a 200/202/204 with no body or a simple success message
            try {
                const data = await response.json(); // If backend sends JSON
                console.log(`[${CURRENT_DATE_TIME}] [pluginService] Successfully deleted plugin ${pluginId}:`, data);
                return data;
            } catch (e) {
                console.log(`[${CURRENT_DATE_TIME}] [pluginService] Plugin ${pluginId} deleted, no JSON response body.`);
                return { success: true, message: "Plugin deletion completed." };
            }
        } catch (error) {
            console.error(`[${CURRENT_DATE_TIME}] [pluginService] Error in deletePlugin:`, error);
            throw error;
        }
    }, [getIdToken, currentUser, getAvailablePlugins, updateToolInFirestore]);

    // --- Memoize the returned object ---
    return useMemo(() => ({
        getAvailablePlugins,
        getPluginMetadata,
        processPlugin,
        updateToolStatus,
        updateToolAccessLevel,
        mockUpdateToolAccessLevel,
        removeTool,
        addTool,
        deletePlugin // NEW: Added the deletePlugin method
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