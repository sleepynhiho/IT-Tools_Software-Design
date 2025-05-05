import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
// --- IMPORTANT: Make sure these paths are correct relative to this file ---
import { useAuth } from "./AuthContext";
import { db } from "../firebaseConfig";
// !!! --- IMPORT THE NEW CONTEXT HOOK --- !!!
import { useAllTools } from "./AllToolsContext"; // <<<--- ADJUST PATH AS NEEDED
// ---
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  arrayUnion,
  arrayRemove,
} from "firebase/firestore";
import { CircularProgress, Box, Typography } from "@mui/material";

// Tool interface definition (should match the one in AllToolsContext)
interface Tool {
  id: string;
  name: string;
  icon: string;
  category: string;
}

// Define the shape of the context value
interface FavoriteToolsContextType {
  favoriteTools: Tool[];
  toggleFavorite: (tool: Tool) => Promise<void>;
  isFavorite: (toolId: string) => boolean;
  isLoading: boolean; // Combined loading state (tools OR favorites)
  error: string | null; // Combined error state
}

// Create the context
const FavoriteToolsContext = createContext<
  FavoriteToolsContextType | undefined
>(undefined);

// --- Custom Hook (Named Export) ---
export const useFavoriteTools = (): FavoriteToolsContextType => {
  const context = useContext(FavoriteToolsContext);
  if (!context) {
    throw new Error(
      "useFavoriteTools must be used within a FavoriteToolsProvider AND AllToolsProvider"
    );
  }
  return context;
};

// --- Provider Component (Named Export) ---
export const FavoriteToolsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { currentUser } = useAuth(); // Get user state from AuthContext
  // --- Consume the AllToolsContext ---
  const { allTools, isLoading: isLoadingTools, error: toolsError } = useAllTools();
  // ---------------------------------
  const [favoriteTools, setFavoriteTools] = useState<Tool[]>([]); // Local state for FAVORITE Tool objects
  const [isLoadingFavorites, setIsLoadingFavorites] = useState<boolean>(true); // Loading state specifically for favorites
  const [favoritesError, setFavoritesError] = useState<string | null>(null); // Error state specifically for favorites

  // --- REMOVED: findToolDetailsById memoized function ---
  // This was causing the circular dependency

  // --- Effect to load favorite IDs from Firestore and map them ---
  useEffect(() => {
    // Only run if the user is logged in AND the main tool list has finished loading
    if (!currentUser || isLoadingTools) {
        // If loading tools or logged out, clear local favorites and set loading state appropriately
        if (!currentUser) {
            console.log("[Favorites] User logged out or not yet identified, clearing favorites state.");
            setFavoriteTools([]);
            setIsLoadingFavorites(false); // Not loading favorites if logged out
            setFavoritesError(null);
        } else {
             console.log("[Favorites] Waiting for allTools list to finish loading...");
             // Keep existing favorites state for now, but indicate loading
             setIsLoadingFavorites(true);
        }
        return; // Exit effect early
    }

    // At this point: currentUser exists AND isLoadingTools is false.
    console.log("[Favorites] User logged in and allTools loaded. Fetching favorite IDs...");

    // Async function to fetch and process favorites for the current user
    const loadFavorites = async (userId: string) => {
      setIsLoadingFavorites(true);
      setFavoritesError(null);
      const userDocRef = doc(db, "users", userId);

      try {
        const docSnap = await getDoc(userDocRef);
        let favoriteIds: string[] = [];
        if (docSnap.exists()) {
          const data = docSnap.data();
          favoriteIds = data?.favoritePluginIds || []; // *** Ensure 'favoritePluginIds' is correct field name ***
        }

        // CHANGED: Inline tool lookup instead of using the memoized function
        // This breaks the circular dependency chain
        const fullFavoriteTools = favoriteIds
          .map(id => allTools.find(t => t.id === id))
          .filter((tool): tool is Tool => tool !== undefined);

        console.log("[Favorites] Mapping result (fullFavoriteTools):", fullFavoriteTools);
        setFavoriteTools(fullFavoriteTools); // Update the component's state

      } catch (err: any) {
        console.error("[Favorites] Error fetching/processing favorites from Firestore:", err);
        setFavoritesError("Failed to load your favorite tools. Please try refreshing.");
        setFavoriteTools([]); // Clear local state on error
      } finally {
        setIsLoadingFavorites(false); // Loading finished
      }
    };

    loadFavorites(currentUser.uid); // Load their favorites

    // CHANGED: Dependencies now only include currentUser, isLoadingTools, and allTools directly
    // Removed findToolDetailsById which was causing the circular dependency
  }, [currentUser, isLoadingTools, allTools]);

  // --- Function to toggle a tool's favorite status (add/remove) ---
  const toggleFavorite = useCallback(async (tool: Tool): Promise<void> => {
    if (!currentUser) {
      setFavoritesError("Please log in to manage your favorite tools.");
      console.warn("[Favorites] Attempted to toggle favorite while logged out.");
      return;
    }
    // Prevent toggle if main tools are still loading
    if (isLoadingTools) {
       setFavoritesError("Tool list is still loading, please wait.");
       return;
    }

    const toolId = tool.id;
    const userDocRef = doc(db, "users", currentUser.uid);
    const isCurrentlyFavorite = favoriteTools.some((fav) => fav.id === toolId);
    const previousFavorites = [...favoriteTools];

    // Optimistic UI Update
    setFavoriteTools((prev) =>
      isCurrentlyFavorite
        ? prev.filter((fav) => fav.id !== toolId)
        : [...prev, tool]
    );
    setFavoritesError(null);

    try {
      // Update Firestore
      if (isCurrentlyFavorite) {
        await updateDoc(userDocRef, {
            favoritePluginIds: arrayRemove(toolId) // *** Ensure 'favoritePluginIds' is correct field name ***
        });
      } else {
        await setDoc(userDocRef, {
            favoritePluginIds: arrayUnion(toolId) // *** Ensure 'favoritePluginIds' is correct field name ***
        }, { merge: true });
      }
    } catch (err) {
      console.error("[Favorites] Failed to update Firestore:", err);
      setFavoritesError("Failed to update your favorites list. Please try again.");
      setFavoriteTools(previousFavorites); // Rollback
    }
  },
  [currentUser, favoriteTools, isLoadingTools] // Dependencies: with isLoadingTools check
  );

  // --- Function to check if a tool ID is in the current local favorites list ---
  // CHANGED: useCallback to prevent unnecessary recreation
  const isFavorite = useCallback((toolId: string): boolean => {
    return favoriteTools.some((tool) => tool.id === toolId);
  }, [favoriteTools]); // Only depend on favoriteTools

  // --- Combined Loading State & Error ---
  // Consider loading if either the main tools OR the favorites are loading (when logged in)
  const combinedIsLoading = currentUser ? (isLoadingTools || isLoadingFavorites) : false;
  // Show the first error encountered (tool loading or favorites loading/updating)
  const combinedError = toolsError || favoritesError;

  // --- CHANGED: Skip the loading indicator rendering or make it more lightweight ---
  // To avoid costly rerenders, consider rendering a simpler loading indicator or managing it differently
  
  // Provide the state and functions to consuming components via context
  return (
    <FavoriteToolsContext.Provider
      value={{
        favoriteTools,
        toggleFavorite,
        isFavorite,
        isLoading: combinedIsLoading, // Provide combined loading state
        error: combinedError // Provide combined error state
      }}
    >
      {combinedIsLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
          <CircularProgress size={24} />
          <Typography sx={{ml: 2}}>Loading...</Typography>
        </Box>
      ) : null}
      {children}
    </FavoriteToolsContext.Provider>
  );
};

// --- REMOVED default export ---