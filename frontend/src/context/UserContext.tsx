import React, { createContext, useContext, useState, useEffect } from "react";
import { auth, db } from "../firebaseConfig";
import { onAuthStateChanged } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";

interface UserContextType {
  user: any | null;
  userType: "normal" | "premium" | "admin" | null;
  loading: boolean;
}

const UserContext = createContext<UserContextType>({ 
  user: null, 
  userType: null,
  loading: true 
});

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<any | null>(null);
  const [userType, setUserType] = useState<"normal" | "premium" | "admin" | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (authUser) => {
      if (authUser) {
        setUser(authUser);
        
        // Fetch user type from Firestore
        try {
          const userDocRef = doc(db, "users", authUser.uid);
          const userDoc = await getDoc(userDocRef);
          
          if (userDoc.exists()) {
            const userData = userDoc.data();
            const type = userData.userType || "normal";
            setUserType(type as "normal" | "premium" | "admin");
          } else {
            // Default to normal if no document exists
            setUserType("normal");
          }
        } catch (error) {
          console.error("Error fetching user data:", error);
          setUserType("normal"); // Default fallback
        }
      } else {
        setUser(null);
        setUserType(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  return (
    <UserContext.Provider value={{ user, userType, loading }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);