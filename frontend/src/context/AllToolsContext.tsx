// src/context/AllToolsContext.tsx

import React, {
    createContext, useContext, useState, useEffect, useCallback, ReactNode, useRef
  } from "react";
  import usePluginService from "../services/pluginService";
  import { useAuth } from "./AuthContext";
  
  // Tool Interface
  interface Tool {
    id: string;
    name: string;
    icon: string;
    category: string;
    accessLevel?: 'normal' | 'premium' | 'admin';
    description?: string;
    sections?: any[];
    uiConfig?: any;
  }
  
  interface AllToolsContextType {
    allTools: Tool[];
    isLoading: boolean;
    error: string | null;
    refetchTools: () => void;
  }
  
  const AllToolsContext = createContext<AllToolsContextType | undefined>(undefined);
  
  export const useAllTools = (): AllToolsContextType => {
    const context = useContext(AllToolsContext);
    if (!context) {
      throw new Error("useAllTools must be used within an AllToolsProvider");
    }
    return context;
  };
  
  export const AllToolsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const { currentUser, loading: isAuthLoading } = useAuth();
    const pluginService = usePluginService();
  
    const [allTools, setAllTools] = useState<Tool[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const fetchedForUserRef = useRef<string | null | undefined>(undefined);
  
    const fetchAllTools = useCallback(async () => {
      // Wait for initial auth state resolution
      if (isAuthLoading) {
        console.log("[AllToolsContext] Waiting for initial auth process...");
        if (fetchedForUserRef.current === undefined) {
             setIsLoading(true);
        }
        return;
      }
  
      // --- CHANGE: Allow fetching even if currentUser is null ---
      const currentUserId = currentUser?.uid ?? null; // Represent anonymous as null
  
      // Skip refetch only if user status hasn't changed since last successful fetch
      if (fetchedForUserRef.current === currentUserId && fetchedForUserRef.current !== undefined && !error) { // Also check for error state
          console.log("[AllToolsContext] User status hasn't changed since last successful fetch. Skipping refetch.");
          setIsLoading(false); // Ensure loading is false if skipped
          return;
      }
  
      console.log(`[AllToolsContext] Auth resolved or user changed. Fetching tools via service (User: ${currentUserId ?? "anonymous"})`);
      setIsLoading(true);
      setError(null); // Clear previous errors before fetching
  
      try {
        // pluginService.getAvailablePlugins() calls GET /api/plugins
        // fetchWithAuth inside getAvailablePlugins will handle sending token ONLY IF currentUser exists
        // Backend needs to allow anonymous requests to GET /api/plugins
        const fetchedTools = await pluginService.getAvailablePlugins();
        console.log(`[AllToolsContext] Received ${fetchedTools.length} tools from service.`);
  
        // --- Ensure fetchedTools is an array before comparing/setting ---
        if (!Array.isArray(fetchedTools)) {
            console.error("[AllToolsContext] Received non-array response for tools:", fetchedTools);
            throw new Error("Invalid data format received from server.");
        }
  
        // Update state only if data actually changed
        // Consider a more efficient comparison for very large arrays if needed
        if (JSON.stringify(fetchedTools) !== JSON.stringify(allTools)) {
          setAllTools(fetchedTools);
          console.log("[AllToolsContext] Tools updated, setting new state");
        } else {
          console.log("[AllToolsContext] Tools unchanged, skipping state update");
        }
        fetchedForUserRef.current = currentUserId; // Mark successful fetch for this user state
  
      } catch (err: any) {
        console.error("[AllToolsContext] Error fetching tool data:", err);
        setError(err.message || "Failed to load tools. Please try again.");
        setAllTools([]); // Clear tools on error
        fetchedForUserRef.current = undefined; // Reset fetch status on error
      } finally {
         // Set loading false regardless of success/error AFTER attempt
         setIsLoading(false);
        console.log("[AllToolsContext] Tool fetching process finished.");
      }
    }, [isAuthLoading, currentUser, pluginService, allTools, error]); // Added 'error' to dependencies
  
    useEffect(() => {
      fetchAllTools();
    }, [fetchAllTools]); // fetchAllTools is stable due to useCallback
  
    return (
      <AllToolsContext.Provider value={{ allTools, isLoading, error, refetchTools: fetchAllTools }}>
        {children}
      </AllToolsContext.Provider>
    );
  };
  
export default AllToolsProvider; 