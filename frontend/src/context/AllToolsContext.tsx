import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import usePluginService from "../services/pluginService"; // Import the service hook (assuming it's memoized now)
import { useAuth } from "./AuthContext"; // Import useAuth to check login status

// Define the Tool interface based on what the backend /api/plugins endpoint returns
// It should include all necessary details for display. accessLevel is optional
// on the frontend if the backend handles all filtering.
interface Tool {
  id: string;
  name: string;
  icon: string;
  category: string;
  accessLevel?: 'normal' | 'premium' | 'admin'; // Optional on frontend now
  description?: string;
  // Include other properties returned by your backend API
}

// Define the shape of this context's value
interface AllToolsContextType {
  allTools: Tool[]; // This holds the user-specific, filtered list from the backend
  isLoading: boolean; // True if either auth or tool fetch is in progress
  error: string | null; // Any error during fetching
  refetchTools: () => void; // Function to manually trigger a refetch
}

// Create the context
const AllToolsContext = createContext<AllToolsContextType | undefined>(
  undefined
);

// Create the custom hook for consuming the context
export const useAllTools = (): AllToolsContextType => {
  const context = useContext(AllToolsContext);
  if (!context) {
    // Ensure this provider is included in your component tree (e.g., in index.tsx)
    throw new Error("useAllTools must be used within an AllToolsProvider");
  }
  return context;
};

// --- Provider Component (Named Export assumed) ---
export const AllToolsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    // Get authentication status and user object
    const { currentUser, loading: isAuthLoading } = useAuth();
    // Get the memoized plugin service hook
    const pluginService = usePluginService();

    // State to hold the final, filtered list of tools received from the backend
    const [allTools, setAllTools] = useState<Tool[]>([]);
    // Loading state reflects combined auth/tool fetch status
    const [isLoading, setIsLoading] = useState<boolean>(true);
    // Error state for fetch errors
    const [error, setError] = useState<string | null>(null);

    // --- Function to fetch the user-specific tool list ---
    // useCallback ensures this function reference is stable unless dependencies change
    const fetchAllTools = useCallback(async () => {
        // Don't attempt fetch until authentication status is resolved
        if (isAuthLoading) {
            console.log("[AllToolsContext] Waiting for auth process...");
            // Ensure loading stays true if we haven't loaded tools yet
            if (allTools.length === 0) setIsLoading(true);
            return;
        }

        // If user is logged out, clear tools and finish loading
        if (!currentUser) {
            console.log("[AllToolsContext] No user logged in. Clearing tool list.");
            setAllTools([]);
            setIsLoading(false);
            setError(null);
            return;
        }

        // User is logged in and auth is ready, proceed to fetch their accessible tools
        console.log("[AllToolsContext] Auth ready. Fetching user-specific tools via service...");
        setIsLoading(true); // Indicate fetching has started
        setError(null); // Clear previous errors

        try {
            // Call the service method (which uses fetchWithAuth internally)
            // This endpoint (`/api/plugins`) should return the filtered list from the backend
            const fetchedTools = await pluginService.getAvailablePlugins();
            console.log(`[AllToolsContext] Received ${fetchedTools.length} accessible tools from service.`);
            
            // ADDED: Compare with existing state to avoid unnecessary updates
            if (JSON.stringify(fetchedTools) !== JSON.stringify(allTools)) {
                // Update state with the received list only if different
                setAllTools(fetchedTools);
                console.log("[AllToolsContext] Tools updated, setting new state");
            } else {
                console.log("[AllToolsContext] Tools unchanged, skipping state update");
            }
        } catch (err: any) {
            console.error("[AllToolsContext] Error fetching tool data:", err);
            // Set error state to display feedback to the user
            setError(err.message || "Failed to load tools. Please try again.");
            setAllTools([]); // Clear tools state on error
        } finally {
            setIsLoading(false); // Mark loading as complete (success or error)
            console.log("[AllToolsContext] Tool fetching process finished.");
        }
    // Dependencies: Re-run only when auth status changes or the service instance changes (which shouldn't happen often with useMemo)
    }, [isAuthLoading, currentUser, pluginService]);

    // --- Effect to trigger the fetch on mount and when dependencies change ---
    useEffect(() => {
        fetchAllTools();
    }, [fetchAllTools]); // Dependency array contains the stable fetchAllTools function

    // --- Provide the context value to children ---
    return (
        <AllToolsContext.Provider
            value={{
                allTools, // Provide the list of tools (already filtered by backend)
                isLoading, // Provide the current loading status
                error,     // Provide any error message
                refetchTools: fetchAllTools, // Provide the function to allow manual refetching
            }}
        >
            {children}
        </AllToolsContext.Provider>
    );
};

export default AllToolsProvider;