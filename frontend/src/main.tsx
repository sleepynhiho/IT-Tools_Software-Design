// index.tsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";

// --- Make sure paths are correct ---
import { AuthProvider } from "./context/AuthContext"; // Named import
import { FavoriteToolsProvider } from "./context/FavoriteToolsContext"; // Named import
import { AllToolsProvider } from "./context/AllToolsContext"; // <<<--- IMPORT AllToolsProvider (Named import)

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider>
      {/* AllToolsProvider MUST wrap FavoriteToolsProvider */}
      <AllToolsProvider>
        <FavoriteToolsProvider>
          <App />
        </FavoriteToolsProvider>
      </AllToolsProvider>
    </AuthProvider>
  </StrictMode>
);