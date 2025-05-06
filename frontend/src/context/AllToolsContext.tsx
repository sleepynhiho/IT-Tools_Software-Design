import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
  useRef,
} from "react";
import usePluginService from "../services/pluginService";
import { useAuth } from "./AuthContext";
// Add Firestore imports
import { collection, doc, setDoc, getDocs, query, where } from "firebase/firestore";
import { db } from "../firebaseConfig";

// Tool Interface
interface Tool {
  id: string;
  name: string;
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
  id: string;
  premium: boolean;
  enabled: boolean;
}

interface AllToolsContextType {
  allTools: Tool[];
  isLoading: boolean;
  error: string | null;
  refetchTools: () => void;
  updateToolStatus: (toolId: string, status: 'enabled' | 'disabled') => Promise<boolean>; 
  updateToolAccessLevel: (toolId: string, accessLevel: 'normal' | 'premium') => Promise<boolean>; 
}

// Current date and user information
const CURRENT_DATE_TIME = "2025-05-06 16:16:32"; // Current UTC time in YYYY-MM-DD HH:MM:SS format
const CURRENT_USER_LOGIN = "hanhiho"; // Current user's login

const AllToolsContext = createContext<AllToolsContextType | undefined>(
  undefined
);

export const useAllTools = (): AllToolsContextType => {
  const context = useContext(AllToolsContext);
  if (!context) {
    throw new Error("useAllTools must be used within an AllToolsProvider");
  }
  return context;
};

export const AllToolsProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { currentUser, loading: isAuthLoading, userType } = useAuth();
  const pluginService = usePluginService();

  const [allTools, setAllTools] = useState<Tool[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const fetchedForUserRef = useRef<string | null | undefined>(undefined);
  
  // Track if tools were already saved to Firestore
  const [toolsSavedToFirestore, setToolsSavedToFirestore] = useState<boolean>(false);

  // Helper function to convert tool to simple Firestore format
  const convertToSimpleFirestoreFormat = (tool: Tool): SimpleFirestoreTool => {
    return {
      id: tool.id,
      premium: tool.accessLevel === 'premium',
      enabled: tool.status !== 'disabled'
    };
  };

  // Helper function to sanitize a tool name for use as a Firestore document ID
  const sanitizeToolNameForFirestore = (name: string): string => {
    // Replace forward slashes with a safe character (underscore)
    return name.replace(/\//g, '_');
  };

  // Helper function to save tools to Firestore
  const saveToolsToFirestore = async (tools: Tool[]) => {
    if (!tools.length || toolsSavedToFirestore) return;
    
    try {
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Saving tools to Firestore...`);
      
      // Check if tools collection already has data
      const toolsCollection = collection(db, "tools");
      const existingToolsSnapshot = await getDocs(query(toolsCollection, where("enabled", "!=", null)));
      
      // If we already have tools, don't save again
      if (!existingToolsSnapshot.empty) {
        console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Tools already exist in Firestore, skipping save`);
        setToolsSavedToFirestore(true);
        return;
      }
      
      // Create a batch write for all tools
      const batch = [];
      for (const tool of tools) {
        const firestoreData = convertToSimpleFirestoreFormat(tool);
        // Use the sanitized tool name as the document ID
        const safeDocId = sanitizeToolNameForFirestore(tool.name);
        batch.push(
          setDoc(doc(db, "tools", safeDocId), firestoreData)
        );
      }
      
      // Execute all promises
      await Promise.all(batch);
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Saved ${tools.length} tools to Firestore with simplified model`);
      setToolsSavedToFirestore(true);
    } catch (err) {
      console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error saving tools to Firestore:`, err);
      // Continue with the application flow even if saving fails
    }
  };

  const fetchAllTools = useCallback(async () => {
    // Wait for initial auth state resolution
    if (isAuthLoading) {
      console.log(`[${CURRENT_DATE_TIME}] [AllToolsContext] Waiting for initial auth process...`);
      if (fetchedForUserRef.current === undefined) {
        setIsLoading(true);
      }
      return;
    }

    // --- CHANGE: Allow fetching even if currentUser is null ---
    const currentUserId = currentUser?.uid ?? null; // Represent anonymous as null

    // Skip refetch only if user status hasn't changed since last successful fetch
    if (
      fetchedForUserRef.current === currentUserId &&
      fetchedForUserRef.current !== undefined &&
      !error
    ) {
      // Also check for error state
      console.log(
        `[${CURRENT_DATE_TIME}] [AllToolsContext] User status hasn't changed since last successful fetch. Skipping refetch.`
      );
      setIsLoading(false); // Ensure loading is false if skipped
      return;
    }

    console.log(
      `[${CURRENT_DATE_TIME}] [AllToolsContext] Auth resolved or user changed. Fetching tools via service (User: ${
        currentUserId ?? "anonymous"
      })`
    );
    setIsLoading(true);
    setError(null); // Clear previous errors before fetching

    try {
      // pluginService.getAvailablePlugins() calls GET /api/plugins
      // fetchWithAuth inside getAvailablePlugins will handle sending token ONLY IF currentUser exists
      // Backend needs to allow anonymous requests to GET /api/plugins
      const fetchedTools = await pluginService.getAvailablePlugins();
      console.log(
        `[${CURRENT_DATE_TIME}] [AllToolsContext] Received ${fetchedTools.length} tools from service.`
      );

      // --- Ensure fetchedTools is an array before comparing/setting ---
      if (!Array.isArray(fetchedTools)) {
        console.error(
          `[${CURRENT_DATE_TIME}] [AllToolsContext] Received non-array response for tools:`,
          fetchedTools
        );
        throw new Error("Invalid data format received from server.");
      }

      // Save tools to Firestore on first load with simplified model
      if (fetchedTools.length > 0 && !toolsSavedToFirestore) {
        await saveToolsToFirestore(fetchedTools);
      }

      // Update state only if data actually changed
      // Consider a more efficient comparison for very large arrays if needed
      if (JSON.stringify(fetchedTools) !== JSON.stringify(allTools)) {
        setAllTools(fetchedTools);
        console.log(`[${CURRENT_DATE_TIME}] [AllToolsContext] Tools updated, setting new state`);
      } else {
        console.log(`[${CURRENT_DATE_TIME}] [AllToolsContext] Tools unchanged, skipping state update`);
      }
      fetchedForUserRef.current = currentUserId; // Mark successful fetch for this user state
    } catch (err: any) {
      console.error(`[${CURRENT_DATE_TIME}] [AllToolsContext] Error fetching tool data:`, err);
      setError(err.message || "Failed to load tools. Please try again.");
      setAllTools([]); // Clear tools on error
      fetchedForUserRef.current = undefined; // Reset fetch status on error
    } finally {
      // Set loading false regardless of success/error AFTER attempt
      setIsLoading(false);
      console.log(`[${CURRENT_DATE_TIME}] [AllToolsContext] Tool fetching process finished.`);
    }
  }, [isAuthLoading, currentUser, pluginService, allTools, error, toolsSavedToFirestore]);

  // Add updateToolStatus function
  const updateToolStatus = useCallback(
    async (
      toolId: string,
      status: "enabled" | "disabled"
    ): Promise<boolean> => {
      // Check if user is admin
      if (userType !== "admin") {
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Only admin users can change tool status`);
        return false;
      }

      setIsLoading(true);
      try {
        // Call your API service to update the tool status
        await pluginService.updateToolStatus(toolId, status);
        
        // Find the tool to get its name
        const tool = allTools.find(t => t.id === toolId);
        if (!tool) {
          throw new Error(`Tool with ID ${toolId} not found`);
        }
        
        // Update local state
        setAllTools((prev) =>
          prev.map((t) => (t.id === toolId ? { ...t, status } : t))
        );
        
        // Update in Firestore - simplified model
        try {
          // Use sanitized tool name as document ID
          const safeDocId = sanitizeToolNameForFirestore(tool.name);
          const toolRef = doc(db, "tools", safeDocId);
          await setDoc(toolRef, {
            enabled: status === 'enabled'
          }, { merge: true });
          console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Updated tool ${safeDocId} in Firestore`);
        } catch (firestoreErr) {
          console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error updating tool status in Firestore:`, firestoreErr);
        }

        console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Tool ${toolId} status updated to ${status}`);
        return true;
      } catch (err: any) {
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error updating tool status:`, err);
        setError(err.message || "Failed to update tool status");
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    [userType, pluginService, allTools]
  );

  const updateToolAccessLevel = useCallback(
    async (
      toolId: string,
      accessLevel: "normal" | "premium"
    ): Promise<boolean> => {
      // Check if user is admin
      if (userType !== "admin") {
        console.error(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: Only admin users can change tool access level`);
        return false;
      }
  
      setIsLoading(true);
      try {
        // Find the tool to get its name
        const tool = allTools.find(t => t.id === toolId);
        if (!tool) {
          throw new Error(`Tool with ID ${toolId} not found`);
        }
        
        // First update local state for better UX
        setAllTools((prev) =>
          prev.map((t) =>
            t.id === toolId ? { ...t, accessLevel } : t
          )
        );
        
        // Update in Firestore directly first for reliability
        const safeDocId = sanitizeToolNameForFirestore(tool.name);
        try {
          const toolRef = doc(db, "tools", safeDocId);
          await setDoc(toolRef, {
            premium: accessLevel === 'premium',
            updatedAt: new Date().toISOString()
          }, { merge: true });
          console.log(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: Updated tool ${safeDocId} premium status in Firestore`);
        } catch (firestoreErr) {
          console.error(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: Error updating tool premium status in Firestore:`, firestoreErr);
        }
        
        // Then call API service to update backend if available
        try {
          await pluginService.updateToolAccessLevel(toolId, accessLevel);
          console.log(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: API update successful for tool ${toolId}`);
        } catch (apiError) {
          // API error can be ignored - we already updated Firestore
          console.warn(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: API update failed but Firestore update succeeded:`, apiError);
        }
  
        console.log(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: Tool ${toolId} access level updated to ${accessLevel}`);
        return true;
      } catch (err: any) {
        console.error(`[2025-05-06 16:29:38] ${CURRENT_USER_LOGIN}: Error updating tool access level:`, err);
        setError(err.message || "Failed to update tool access level");
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    [userType, pluginService, allTools]
  );

  useEffect(() => {
    fetchAllTools();
  }, [fetchAllTools]); // fetchAllTools is stable due to useCallback

  return (
    <AllToolsContext.Provider
      value={{
        allTools,
        isLoading,
        error,
        refetchTools: fetchAllTools,
        updateToolStatus,
        updateToolAccessLevel,
      }}
    >
      {children}
    </AllToolsContext.Provider>
  );
};

export default AllToolsProvider;