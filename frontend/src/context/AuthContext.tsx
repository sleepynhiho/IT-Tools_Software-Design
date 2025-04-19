import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import { onAuthStateChanged, signOut, User, getIdToken } from 'firebase/auth';
import { auth } from '../firebaseConfig'; // Your initialized Firebase auth instance
import { CircularProgress, Box } from '@mui/material'; // For loading indicator

// Define the shape of the context value
interface AuthContextProps {
  currentUser: User | null;
  loading: boolean; // Indicates if the initial auth check is complete
  getIdToken: () => Promise<string | null>; // Function to get ID token for API calls
  logout: () => Promise<void>; // Function to log out
}

// Create the context with an undefined initial value
const AuthContext = createContext<AuthContextProps | undefined>(undefined);

// Create the Provider component
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true); // Start loading until first check is done

  useEffect(() => {
    console.log("[AuthProvider] Setting up Firebase auth listener...");
    // Listen for authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setLoading(false); // Set loading to false once state is determined
      console.log("[AuthProvider] Auth State Changed, User:", user ? user.uid : 'null');
    });

    // Clean up subscription on unmount
    return () => {
        console.log("[AuthProvider] Cleaning up Firebase auth listener.");
        unsubscribe();
    }
  }, []); // Empty dependency array means this effect runs only once on mount

  // Function to get the current user's ID token
  const getToken = async (): Promise<string | null> => {
    if (currentUser) {
      try {
        // Consider forceRefresh based on token expiration needs
        const token = await getIdToken(currentUser, /* forceRefresh */ false);
        return token;
      } catch (error) {
        console.error("[AuthProvider] Error getting ID token:", error);
        // Optional: Handle error, maybe force logout
        // await logout();
        return null;
      }
    }
    return null; // No user, no token
  };

  // Function to handle logout
  const handleLogout = async (): Promise<void> => {
     console.log("[AuthProvider] Attempting logout...");
     try {
        await signOut(auth);
        console.log("[AuthProvider] Logout successful.");
        // currentUser state will be updated automatically by onAuthStateChanged
     } catch (error) {
        console.error("[AuthProvider] Error signing out:", error);
        // Optional: Provide feedback to the user
        throw error; // Re-throw so calling components can react if needed
     }
  };


  // While checking user state, show a loading indicator or empty screen
  // This prevents rendering the app in a potentially incorrect intermediate state
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  // Provide the context value to children components
  return (
    <AuthContext.Provider value={{ currentUser, loading, getIdToken: getToken, logout: handleLogout }}>
      {children}
    </AuthContext.Provider>
  );
};

// Create a custom hook for easy consumption of the context
export const useAuth = (): AuthContextProps => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};