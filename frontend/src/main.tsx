// index.tsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import { FavoriteToolsProvider } from "./context/FavoriteToolsContext";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <FavoriteToolsProvider>
      <App />
    </FavoriteToolsProvider>
  </StrictMode>
);
