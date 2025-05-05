// src/context/AuthContext.tsx

import React, {
  createContext,
  useState,
  useEffect,
  useContext,
  useCallback, // Added useCallback
  ReactNode
} from 'react';
import { onAuthStateChanged, signOut, User, getIdToken } from 'firebase/auth';
import { auth, db } from '../firebaseConfig'; // Import db
import { doc, getDoc, setDoc, serverTimestamp } from "firebase/firestore"; // Import Firestore functions
import { CircularProgress, Box } from '@mui/material';

// --- Define User Role Type ---
export type UserType = 'normal' | 'premium' | 'admin' | null;
// ---------------------------

// Update Context Props interface
interface AuthContextProps {
  currentUser: User | null;
  userType: UserType; // <<<--- ADDED userType
  loading: boolean; // Now indicates BOTH auth check AND user type fetch are complete
  getIdToken: () => Promise<string | null>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextProps | undefined>(undefined);

// Update Provider Component (Named Export assumed)
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [userType, setUserType] = useState<UserType>(null); // <<<--- ADDED state for userType
  // Loading is true until BOTH auth state is known AND user type is fetched/set
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    console.log("[AuthProvider] Setting up Firebase auth listener...");
    let isMounted = true; // Prevent state updates if component unmounts during async ops

    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      console.log("[AuthProvider] Auth State Changed, User:", user ? user.uid : 'null');
      if (!isMounted) return;

      setCurrentUser(user);
      setUserType(null); // Reset user type initially on change

      if (user) {
        // User is logged in or just signed up
        setLoading(true); // Start loading user data process
        const userDocRef = doc(db, "users", user.uid);

        try {
          console.log(`[AuthProvider] Checking/Fetching user data from ${userDocRef.path}`);
          const docSnap = await getDoc(userDocRef);
          if (!isMounted) return; // Check mount status after async call

          if (docSnap.exists()) {
            // --- Document Exists: Read userType ---
            const userData = docSnap.data();
            console.log("[AuthProvider] Firestore document exists. Data:", userData);
            // *** Use the EXACT field name from your Firestore document ***
            const fetchedType = userData?.userType;
            if (fetchedType && ['normal', 'premium', 'admin'].includes(fetchedType)) {
                 setUserType(fetchedType as UserType);
                 console.log("[AuthProvider] User type set from Firestore:", fetchedType);
            } else {
                 // Field missing or invalid, default to normal AND update Firestore just in case
                 console.warn(`[AuthProvider] userType field missing or invalid in Firestore for user ${user.uid}. Defaulting to 'normal' and attempting update.`);
                 setUserType('normal');
                 // Attempt to set the default type using merge to avoid overwriting other data
                 await setDoc(userDocRef, { userType: 'normal' }, { merge: true });
            }
          } else {
            // --- Document DOES NOT Exist: Initialize it ---
            // This covers users logging in for the first time after signup (if signup didn't create it)
            // OR users who signed up before this logic was implemented.
            console.warn(`[AuthProvider] Firestore document not found for user ${user.uid}. Initializing with type 'normal'.`);
             setUserType('normal'); // Set local state immediately
            // Create the document with default values
             await setDoc(userDocRef, {
                userType: 'normal',
                email: user.email || '', // Get email from auth user
                createdAt: serverTimestamp(), // Add creation timestamp
                lastLogin: serverTimestamp() // Add first login time
                // DO NOT add username here unless you fetch it from somewhere else
            });
             console.log(`[AuthProvider] Initialized Firestore document for user ${user.uid}`);
          }
        } catch (error) {
          console.error("[AuthProvider] Error fetching or initializing user data in Firestore:", error);
          // Handle error case - default to normal locally, don't block login
          if(isMounted) setUserType('normal');
        } finally {
          if(isMounted) setLoading(false); // Finish loading process
        }

      } else {
        // User is logged out
        if(isMounted) {
            setUserType(null);
            setLoading(false); // No user data to load
        }
      }
    });

    // Clean up
    return () => {
        console.log("[AuthProvider] Cleaning up Firebase auth listener.");
        isMounted = false; // Mark as unmounted
        unsubscribe();
    }
  }, []); // Run only on mount

  // --- getIdToken and logout functions (useCallback added) ---
  const getToken = useCallback(async (): Promise<string | null> => {
    if (currentUser) {
      try {
        const token = await getIdToken(currentUser);
        return token;
      } catch (error) { console.error("[AuthProvider] Error getting ID token:", error); return null; }
    }
    return null;
  }, [currentUser]); // Dependency: currentUser

  const handleLogout = useCallback(async (): Promise<void> => {
     console.log("[AuthProvider] Attempting logout...");
     try {
        await signOut(auth);
        console.log("[AuthProvider] Logout successful.");
        // State updates (currentUser, userType) handled by onAuthStateChanged listener
     } catch (error) { console.error("[AuthProvider] Error signing out:", error); throw error; }
  }, []); // Dependency: none


  // Show loading indicator while auth state or user type is being determined
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  // Provide the context value including userType
  return (
    <AuthContext.Provider value={{ currentUser, userType, loading, getIdToken: getToken, logout: handleLogout }}>
      {children}
    </AuthContext.Provider>
  );
};

// --- useAuth hook (no change needed, but now returns userType) ---
export const useAuth = (): AuthContextProps => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};