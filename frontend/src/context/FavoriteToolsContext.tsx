import React, {
  createContext, useContext, useState, useEffect, useCallback, ReactNode, useRef,
} from "react";
import { useAuth } from "./AuthContext";
import { db } from "../firebaseConfig";
import { useAllTools } from "./AllToolsContext";
import {
  doc, getDoc, setDoc, updateDoc, arrayUnion, arrayRemove, DocumentReference, // Import DocumentReference
} from "firebase/firestore";
import { CircularProgress, Box, Typography } from "@mui/material";

// --- Constants ---
const LOCAL_STORAGE_FAVORITES_KEY = "anonymousFavorites"; // Key for localStorage

// --- Interfaces ---
interface Tool { // Ensure this matches the Tool type used everywhere
  id: string;
  name: string;
  icon: string;
  category: string;
  // Add other relevant fields if needed by favorites logic
}

interface FavoriteToolsContextType {
  favoriteTools: Tool[]; // Holds Tool objects (either from local or Firestore)
  toggleFavorite: (tool: Tool) => Promise<void>;
  isFavorite: (toolId: string) => boolean;
  isLoading: boolean; // Combined loading state
  error: string | null; // Combined error state
}

const FavoriteToolsContext = createContext<FavoriteToolsContextType | undefined>(undefined);

export const useFavoriteTools = (): FavoriteToolsContextType => {
  const context = useContext(FavoriteToolsContext);
  if (!context) {
    throw new Error("useFavoriteTools must be used within a FavoriteToolsProvider AND AllToolsProvider");
  }
  return context;
};

export const FavoriteToolsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { currentUser, loading: authLoading } = useAuth();
  const { allTools, isLoading: isLoadingTools, error: toolsError } = useAllTools();

  // State for favorite Tool OBJECTS (derived from IDs and allTools)
  const [favoriteTools, setFavoriteTools] = useState<Tool[]>([]);
  // State for favorite IDs (string array) - manages the source of truth
  const [favoriteToolIds, setFavoriteToolIds] = useState<string[]>([]);

  const [isLoadingFavorites, setIsLoadingFavorites] = useState<boolean>(true); // Loading state specifically for favorites logic
  const [favoritesError, setFavoritesError] = useState<string | null>(null);

  // Ref to track if initial sync has occurred after login to prevent race conditions
  const initialSyncDoneRef = useRef<boolean>(false);

  // --- Helper: Get anonymous favorites from localStorage ---
  const getAnonymousFavorites = useCallback((): string[] => {
    try {
      const stored = localStorage.getItem(LOCAL_STORAGE_FAVORITES_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch (e) {
      console.error("Error reading anonymous favorites from localStorage:", e);
      return [];
    }
  }, []);

  // --- Helper: Set anonymous favorites to localStorage ---
  const setAnonymousFavorites = useCallback((ids: string[]) => {
    try {
      localStorage.setItem(LOCAL_STORAGE_FAVORITES_KEY, JSON.stringify(ids));
    } catch (e) {
      console.error("Error saving anonymous favorites to localStorage:", e);
    }
  }, []);

  // --- Effect 1: Load initial state based on auth status ---
  useEffect(() => {
    // Reset sync flag when user logs out or on initial load without user
     if (!currentUser) {
       initialSyncDoneRef.current = false;
     }

    // Don't run if auth is still loading OR allTools list is still loading
    if (authLoading || isLoadingTools) {
        console.log("[Favorites] Waiting for auth and allTools list...");
        // Keep loading true if we haven't loaded anything yet
        if (favoriteTools.length === 0 && favoriteToolIds.length === 0 && !favoritesError) {
           setIsLoadingFavorites(true);
        }
      return;
    }

    // At this point, auth is resolved and allTools are loaded/available

    setIsLoadingFavorites(true);
    setFavoritesError(null);

    if (currentUser) {
      // --- User is LOGGED IN ---
      console.log("[Favorites] User logged in. Starting favorite sync/load process...");
      const userDocRef = doc(db, "users", currentUser.uid);

      const syncAndLoadFavorites = async () => {
          let firestoreIds: string[] = [];
          let localAnonymousIds: string[] = [];

          try {
              // 1. Get Firestore favorites
              const docSnap = await getDoc(userDocRef);
              if (docSnap.exists()) {
                  firestoreIds = docSnap.data()?.favoritePluginIds || [];
                  console.log("[Favorites] Fetched Firestore favorites:", firestoreIds);
              } else {
                  console.warn("[Favorites] User document not found in Firestore during sync.");
                  // Optionally create the document here if AuthProvider didn't
              }

              // 2. Get anonymous favorites from localStorage (only if initial sync hasn't happened)
               if (!initialSyncDoneRef.current) {
                 localAnonymousIds = getAnonymousFavorites();
                 console.log("[Favorites] Fetched localStorage favorites for sync:", localAnonymousIds);
               } else {
                  console.log("[Favorites] Initial sync already done, skipping localStorage read.");
               }


              // 3. Merge if needed (only on initial sync after login)
              let finalIds = firestoreIds;
              if (!initialSyncDoneRef.current && localAnonymousIds.length > 0) {
                  const mergedSet = new Set([...firestoreIds, ...localAnonymousIds]);
                  finalIds = Array.from(mergedSet);
                  console.log("[Favorites] Merged Firestore & localStorage:", finalIds);

                  // 4. Update Firestore with merged list if changes were made
                  if (finalIds.length !== firestoreIds.length || !finalIds.every(id => firestoreIds.includes(id))) {
                      console.log("[Favorites] Updating Firestore with merged favorites...");
                      await setDoc(userDocRef, { favoritePluginIds: finalIds }, { merge: true });
                      console.log("[Favorites] Firestore updated successfully.");
                  }

                  // 5. Clear localStorage anonymous favorites after successful sync
                  setAnonymousFavorites([]); // Clear local storage
                  console.log("[Favorites] Cleared anonymous favorites from localStorage.");
               }

              // 6. Update local state with the final list of IDs
              setFavoriteToolIds(finalIds);
              initialSyncDoneRef.current = true; // Mark sync as done for this session

          } catch (err: any) {
              console.error("[Favorites] Error during sync/load favorites:", err);
              setFavoritesError("Failed to load/sync favorites. Please try refreshing.");
              setFavoriteToolIds([]); // Reset on error
              initialSyncDoneRef.current = false; // Allow retry on next effect run if possible
          } finally {
              setIsLoadingFavorites(false);
          }
      };

      syncAndLoadFavorites();

    } else {
      // --- User is LOGGED OUT (Anonymous) ---
      console.log("[Favorites] User is anonymous. Loading favorites from localStorage.");
      const localIds = getAnonymousFavorites();
      setFavoriteToolIds(localIds);
      setIsLoadingFavorites(false);
      initialSyncDoneRef.current = false; // Reset sync flag
    }

    // Dependencies: Re-run when user logs in/out OR when the tool list finishes loading
  }, [currentUser, authLoading, isLoadingTools, getAnonymousFavorites, setAnonymousFavorites]); // allTools dependency removed from here, handled by isLoadingTools


  // --- Effect 2: Derive favoriteTools (Tool objects) from favoriteToolIds and allTools ---
  useEffect(() => {
      if (isLoadingTools) return; // Wait for tools to be ready

       console.log(`[Favorites] Deriving favorite Tool objects from ${favoriteToolIds.length} IDs and ${allTools.length} total tools.`);
      const fullFavoriteTools = favoriteToolIds
          .map(id => allTools.find(tool => tool.id === id))
          .filter((tool): tool is Tool => tool !== undefined); // Type guard

      // Avoid unnecessary state update if objects haven't changed
      // Simple comparison based on length and IDs might suffice
      if (fullFavoriteTools.length !== favoriteTools.length || !fullFavoriteTools.every(t => favoriteTools.some(ft => ft.id === t.id))) {
          console.log("[Favorites] Updating favoriteTools state with derived objects:", fullFavoriteTools);
          setFavoriteTools(fullFavoriteTools);
      } else {
         console.log("[Favorites] Derived favoriteTools objects unchanged, skipping state update.");
      }

  }, [favoriteToolIds, allTools, isLoadingTools, favoriteTools]); // Depend on IDs, allTools, and current favoriteTools


  // --- Function to toggle a tool's favorite status ---
  const toggleFavorite = useCallback(async (tool: Tool): Promise<void> => {
    const toolId = tool.id;
    const isCurrentlyFavorite = favoriteToolIds.includes(toolId);

    // Optimistic UI update for IDs
    const newIds = isCurrentlyFavorite
      ? favoriteToolIds.filter(id => id !== toolId)
      : [...favoriteToolIds, toolId];
    setFavoriteToolIds(newIds); // Update ID state immediately

    if (currentUser) {
      // --- LOGGED IN: Update Firestore ---
      console.log(`[Favorites] Toggling Firestore favorite for ${toolId} (User: ${currentUser.uid})`);
      const userDocRef = doc(db, "users", currentUser.uid);
      try {
        if (isCurrentlyFavorite) {
          await updateDoc(userDocRef, { favoritePluginIds: arrayRemove(toolId) });
        } else {
          // Use set with merge to ensure the document/field exists
          await setDoc(userDocRef, { favoritePluginIds: arrayUnion(toolId) }, { merge: true });
        }
      } catch (err) {
        console.error("[Favorites] Failed to update Firestore:", err);
        setFavoritesError("Failed to update your favorites list. Please try again.");
        setFavoriteToolIds(favoriteToolIds); // Rollback ID state on error
      }
    } else {
      // --- ANONYMOUS: Update localStorage ---
      console.log(`[Favorites] Toggling localStorage favorite for ${toolId}`);
      setAnonymousFavorites(newIds);
    }
  }, [currentUser, favoriteToolIds, setAnonymousFavorites]); // Dependencies


  // --- Function to check if a tool ID is in the current favorites list ---
  const isFavorite = useCallback((toolId: string): boolean => {
    return favoriteToolIds.includes(toolId);
  }, [favoriteToolIds]); // Depend only on the ID list


  // --- Combined Loading and Error State ---
  // Loading if auth is loading, or allTools are loading, or favorites-specific logic is loading
  const combinedIsLoading = authLoading || isLoadingTools || isLoadingFavorites;
  // Show the first error encountered
  const combinedError = toolsError || favoritesError;


  return (
    <FavoriteToolsContext.Provider
      value={{
        favoriteTools, // The derived Tool objects
        toggleFavorite,
        isFavorite,
        isLoading: combinedIsLoading,
        error: combinedError
      }}
    >
      {/* Optional: Keep or remove the loading indicator */}
      {/* {combinedIsLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 1, position: 'absolute', top: 0, left: 0, right: 0, bgcolor: 'background.paper', zIndex: 10 }}>
          <CircularProgress size={20} />
          <Typography variant="caption" sx={{ml: 1}}>Loading Tools/Favorites...</Typography>
        </Box>
      )} */}
      {children}
    </FavoriteToolsContext.Provider>
  );
};