// src/context/AllToolsContext.tsx

import React, {
  createContext, useContext, useState, useEffect, useCallback, ReactNode, useRef
} from "react";
import usePluginService from "../services/pluginService";
import { useAuth } from "./AuthContext";
import { collection, doc, setDoc, getDocs, query, where, deleteDoc } from "firebase/firestore"; // Added deleteDoc
import { db } from "../firebaseConfig";

// Current date and time information for logging
const CURRENT_DATE_TIME = "2025-05-06 18:46:58";
const CURRENT_USER_LOGIN = "Kostovite";

// Tool Interface from your backend/plugin service
interface Tool {
  id: string;    // This will now be the Firestore Document ID
  name: string;  // This will be a field in the Firestore document
  icon: string;
  category: string;
  accessLevel?: "normal" | "premium" | "admin";
  status?: "enabled" | "disabled";
  description?: string;
  sections?: any[];
  uiConfig?: any;
}

// Simplified Firestore tool model
interface SimpleFirestoreTool {
  id: string;       // Store the original tool.id here as a field (redundant if doc ID is tool.id, but can be useful)
  name: string;     // Store the original tool.name here as a field
  premium: boolean;
  enabled: boolean;
  category?: string; // Optional: store category too
  updatedAt?: string;
}

interface AllToolsContextType {
  allTools: Tool[];
  isLoading: boolean;
  error: string | null;
  refetchTools: () => void;
  updateToolStatus: (toolId: string, status: 'enabled' | 'disabled') => Promise<boolean>;
  updateToolAccessLevel: (toolId: string, accessLevel: 'normal' | 'premium') => Promise<boolean>;
  removeTool: (toolId: string, toolName: string) => Promise<boolean>; // Added new function
}

const getLogPrefix = (currentUser: any) => `[${CURRENT_DATE_TIME}] [User: ${currentUser?.uid ?? (currentUser?.email ?? 'anonymous')}]`;

const AllToolsContext = createContext<AllToolsContextType | undefined>(undefined);

export const useAllTools = (): AllToolsContextType => {
  const context = useContext(AllToolsContext);
  if (!context) {
    throw new Error("useAllTools must be used within an AllToolsProvider");
  }
  return context;
};

export const AllToolsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { currentUser, loading: isAuthLoading, userType } = useAuth();
  const pluginService = usePluginService();

  const [allTools, setAllTools] = useState<Tool[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const fetchedForUserRef = useRef<string | null | undefined>(undefined);
  const [toolsSavedToFirestore, setToolsSavedToFirestore] = useState<boolean>(false);

  // Helper function to convert tool to simple Firestore format
  const convertToSimpleFirestoreFormat = (tool: Tool): SimpleFirestoreTool => {
    return {
      id: tool.id,       // Store the original ID as a field
      name: tool.name,   // Store the original name as a field
      premium: tool.accessLevel === 'premium',
      enabled: tool.status !== 'disabled', // Assuming 'status' is provided or defaulted
      category: tool.category, // Optionally store category
    };
  };

  // Helper function to save/verify tools in Firestore
  const saveToolsToFirestore = useCallback(async (toolsToSave: Tool[]) => { // Renamed parameter
    const logP = getLogPrefix(currentUser);
    if (!toolsToSave.length || toolsSavedToFirestore) {
      if (toolsSavedToFirestore) console.log(`${logP} [AllToolsContext] Initial save/verify to Firestore already done or skipped for this session.`);
      return;
    }

    try {
      console.log(`${logP} [AllToolsContext] Attempting to save/verify ${toolsToSave.length} tools in Firestore...`);
      const toolsCollectionRef = collection(db, "tools");

      // Check if we should even attempt a bulk save (e.g., if collection seems populated)
      // This check is to prevent re-populating on every app start if not needed.
      const initialCheckSnapshot = await getDocs(query(toolsCollectionRef, where("id", "!=", "")));
      if (!initialCheckSnapshot.empty && initialCheckSnapshot.size >= toolsToSave.length * 0.5 /* Heuristic to guess if populated */) {
         console.log(`${logP} [AllToolsContext] Tools collection appears populated. Skipping bulk initial save.`);
         setToolsSavedToFirestore(true); // Mark as done for this session
         return;
      }

      const savePromises = toolsToSave.map(tool => {
        const firestoreData = convertToSimpleFirestoreFormat(tool);
        // --- USE tool.id AS DOCUMENT ID ---
        // Ensure tool.id is safe for Firestore document IDs (no '/', not empty, not just '.' or '..')
        // If tool.id can contain '/', you'll need a sanitization function for it.
        if (!tool.id || tool.id.includes('/') || tool.id === "." || tool.id === "..") {
            console.error(`${logP} [AllToolsContext] Invalid tool.id for Firestore document: '${tool.id}' for tool named '${tool.name}'. Skipping save for this tool.`);
            return Promise.resolve(); // Skip this one
        }
        const toolDocRef = doc(db, "tools", tool.id);
        return setDoc(toolDocRef, firestoreData, { merge: true }); // Use merge:true
      });

      await Promise.all(savePromises);
      console.log(`${logP} [AllToolsContext] Finished saving/verifying tools in Firestore.`);
      setToolsSavedToFirestore(true); // Mark as done for this session
    } catch (err) {
      console.error(`${logP} [AllToolsContext] Error saving/verifying tools in Firestore:`, err);
      // Don't block app functionality if this fails, but log it.
    }
  }, [currentUser, toolsSavedToFirestore]); // Added currentUser for logPrefix


  const fetchAllTools = useCallback(async () => {
    const logP = getLogPrefix(currentUser);
    if (isAuthLoading) { 
      console.log(`${logP} [AllToolsContext] Waiting for initial auth process...`);
      if (fetchedForUserRef.current === undefined) setIsLoading(true); 
      return; 
    }
    
    const currentUserId = currentUser?.uid ?? null;
    if (fetchedForUserRef.current === currentUserId && fetchedForUserRef.current !== undefined && !error) { 
      console.log(`${logP} [AllToolsContext] User status hasn't changed since last successful fetch. Skipping refetch.`);
      setIsLoading(false); 
      return; 
    }

    console.log(`${logP} [AllToolsContext] Fetching tools (User: ${currentUserId ?? "anonymous"})`);
    setIsLoading(true);
    setError(null);

    try {
      const fetchedToolsFromAPI: Tool[] = await pluginService.getAvailablePlugins();
      console.log(`${logP} [AllToolsContext] Received ${fetchedToolsFromAPI.length} tools from API.`);

      if (!Array.isArray(fetchedToolsFromAPI)) { 
        console.error(`${logP} [AllToolsContext] Received non-array data:`, fetchedToolsFromAPI);
        throw new Error("Invalid data format."); 
      }

      // Attempt to save/verify tools in Firestore (only once per app session if tools are fetched)
      if (fetchedToolsFromAPI.length > 0 && !toolsSavedToFirestore) {
        await saveToolsToFirestore(fetchedToolsFromAPI);
      }

      // --- Integrate Firestore status and accessLevel into fetchedToolsFromAPI ---
      let toolsWithFirestoreData: Tool[] = [];
      if (fetchedToolsFromAPI.length > 0) {
          const firestoreToolsSnap = await getDocs(collection(db, "tools"));
          const firestoreToolsMap = new Map<string, SimpleFirestoreTool>();
          firestoreToolsSnap.forEach(docSnap => {
              // The document ID IS tool.id
              firestoreToolsMap.set(docSnap.id, docSnap.data() as SimpleFirestoreTool);
          });

          toolsWithFirestoreData = fetchedToolsFromAPI.map(apiTool => {
              const firestoreToolData = firestoreToolsMap.get(apiTool.id); // Use apiTool.id
              if (firestoreToolData) {
                  return {
                      ...apiTool,
                      name: firestoreToolData.name || apiTool.name, // Prefer Firestore name if available
                      status: firestoreToolData.enabled ? "enabled" : "disabled",
                      accessLevel: firestoreToolData.premium ? "premium" : apiTool.accessLevel || "normal",
                  };
              }
              return { // Default if not in Firestore (should have been saved by saveToolsToFirestore)
                  ...apiTool,
                  status: "enabled", // Default to enabled if no Firestore record
                  accessLevel: apiTool.accessLevel || "normal",
              };
          });
          console.log(`${logP} [AllToolsContext] Merged API tools with Firestore overrides.`);
      } else {
          toolsWithFirestoreData = [];
      }
      // --- End Firestore Integration ---

      if (JSON.stringify(toolsWithFirestoreData) !== JSON.stringify(allTools)) {
        setAllTools(toolsWithFirestoreData);
        console.log(`${logP} [AllToolsContext] Tools updated, setting new state.`);
      } else { 
        console.log(`${logP} [AllToolsContext] Tools unchanged, skipping state update.`);
      }
      fetchedForUserRef.current = currentUserId;
    } catch (err: any) { 
      console.error(`${logP} [AllToolsContext] Error fetching tool data:`, err);
      setError(err.message || "Failed to load tools. Please try again.");
      setAllTools([]); // Clear tools on error
      fetchedForUserRef.current = undefined; // Reset fetch status on error
    }
    finally { 
      setIsLoading(false); 
      console.log(`${logP} [AllToolsContext] Tool fetching process finished.`); 
    }
  }, [isAuthLoading, currentUser, pluginService, allTools, error, toolsSavedToFirestore, saveToolsToFirestore]);


  const updateToolStatus = useCallback(
    async (toolId: string, status: "enabled" | "disabled"): Promise<boolean> => {
      const logP = getLogPrefix(currentUser);
      if (userType !== "admin") { 
        console.error(`${logP} [AllToolsContext] Only admin users can change tool status`);
        return false; 
      }

      const toolToUpdate = allTools.find(t => t.id === toolId);
      if (!toolToUpdate) { 
        console.error(`${logP} [AllToolsContext] Tool with ID ${toolId} not found`);
        return false; 
      }

      setIsLoading(true);
      setError(null);
      
      try {
        const toolDocRef = doc(db, "tools", toolId); // USE toolId AS DOCUMENT ID
        await setDoc(toolDocRef, {
            id: toolId, // Keep the ID field
            name: toolToUpdate.name, // Ensure name is preserved/updated
            enabled: status === 'enabled',
            updatedAt: new Date().toISOString()
        }, { merge: true });
        
        console.log(`${logP} [AllToolsContext] Updated tool ${toolId} status to ${status} in Firestore.`);

        // Update the local state
        setAllTools((prevTools) => prevTools.map((t) => (t.id === toolId ? { ...t, status } : t)));
        
        // Optional: Call backend API
        // await pluginService.updateToolStatus(toolId, status); 
        
        return true;
      } catch (err: any) { 
        console.error(`${logP} [AllToolsContext] Error updating tool status:`, err);
        setError(`Failed to update tool status: ${err.message}`);
        return false; 
      }
      finally { 
        setIsLoading(false); 
      }
    },
    [userType, allTools, currentUser]
  );

  const updateToolAccessLevel = useCallback(
    async (toolId: string, accessLevel: "normal" | "premium"): Promise<boolean> => {
      const logP = getLogPrefix(currentUser);
      if (userType !== "admin") { 
        console.error(`${logP} [AllToolsContext] Only admin users can change tool access level`);
        return false;
      }
      
      const toolToUpdate = allTools.find(t => t.id === toolId);
      if (!toolToUpdate) { 
        console.error(`${logP} [AllToolsContext] Tool with ID ${toolId} not found`);
        return false;
      }

      setIsLoading(true);
      setError(null);
      
      try {
        const toolDocRef = doc(db, "tools", toolId); // USE toolId AS DOCUMENT ID
        await setDoc(toolDocRef, {
            id: toolId, // Keep the ID field
            name: toolToUpdate.name, // Ensure name is preserved/updated
            premium: accessLevel === 'premium',
            updatedAt: new Date().toISOString()
        }, { merge: true });
        
        console.log(`${logP} [AllToolsContext] Updated tool ${toolId} access to ${accessLevel} in Firestore.`);

        // Update the local state
        setAllTools((prevTools) => prevTools.map((t) => (t.id === toolId ? { ...t, accessLevel } : t)));
        
        // Optional: Call backend API
        // await pluginService.updateToolAccessLevel(toolId, accessLevel);
        
        return true;
      } catch (err: any) { 
        console.error(`${logP} [AllToolsContext] Error updating tool access level:`, err);
        setError(`Failed to update tool access level: ${err.message}`);
        return false;
      }
      finally { 
        setIsLoading(false); 
      }
    },
    [userType, allTools, currentUser]
  );

  // --- UPDATED: Function to remove/delete a tool ---
  const removeTool = useCallback(async (toolId: string, toolName: string): Promise<boolean> => {
    const logP = getLogPrefix(currentUser);

    if (userType !== "admin") {
        console.error(`${logP} [AllToolsContext] Permission Denied: Only admin can remove tools.`);
        setError("Permission Denied: Only admin can remove tools.");
        return false;
    }

    if (!pluginService.deletePlugin) { // Check if deletePlugin exists on the service
        console.error(`${logP} [AllToolsContext] deletePlugin function not available on pluginService.`);
        setError("Client error: Delete function not available.");
        return false;
    }

    // --- Use toolName for the backend API call as per backend @PathVariable ---
    const identifierForBackendDelete = toolName;

    console.log(`${logP} [AllToolsContext] Attempting to remove tool ID: ${toolId}, Backend Identifier: ${identifierForBackendDelete}`);
    setIsLoading(true);
    setError(null);

    try {
        // 1. Call the backend service to delete the JAR and unload
        await pluginService.deletePlugin(identifierForBackendDelete); // Pass toolName for backend
        console.log(`${logP} [AllToolsContext] Backend deletion request successful for ${identifierForBackendDelete}.`);

        // 2. Delete from Firestore 'tools' collection (using toolId as document ID)
        try {
            const toolDocRef = doc(db, "tools", toolId); // Use toolId for Firestore
            await deleteDoc(toolDocRef);
            console.log(`${logP} [AllToolsContext] Tool ${toolId} deleted from Firestore 'tools' collection.`);
        } catch (firestoreError) {
            console.error(`${logP} [AllToolsContext] Error deleting tool ${toolId} from Firestore:`, firestoreError);
            // Continue, backend deletion is primary for operation, but log this.
        }

        // 3. Refetch the tools list to update the UI
        await fetchAllTools(); // fetchAllTools will set isLoading to false
        return true;

    } catch (err: any) {
        console.error(`${logP} [AllToolsContext] Error removing tool ${identifierForBackendDelete}:`, err);
        setError(err.message || "Failed to remove tool.");
        setIsLoading(false); // Ensure loading is false on error
        return false;
    }
  }, [userType, pluginService, fetchAllTools, currentUser]); // Added currentUser to dependencies

  useEffect(() => {
    fetchAllTools();
  }, [fetchAllTools]);

  return (
    <AllToolsContext.Provider
      value={{
        allTools,
        isLoading,
        error,
        refetchTools: fetchAllTools,
        updateToolStatus,
        updateToolAccessLevel,
        removeTool,
      }}
    >
      {children}
    </AllToolsContext.Provider>
  );
};

export default AllToolsProvider;