import React, { createContext, useContext, useState } from "react";

interface Tool {
  id: string;
  name: string;
  icon: string;
  category: string;
}

interface FavoriteToolsContextType {
  favoriteTools: Tool[];
  toggleFavorite: (tool: Tool) => void;
  isFavorite: (toolId: string) => boolean;
}

const FavoriteToolsContext = createContext<
  FavoriteToolsContextType | undefined
>(undefined);

export const useFavoriteTools = () => {
  const context = useContext(FavoriteToolsContext);
  if (!context) {
    throw new Error(
      "useFavoriteTools must be used within a FavoriteToolsProvider"
    );
  }
  return context;
};

export const FavoriteToolsProvider: React.FC<
  React.PropsWithChildren<object>
> = ({ children }) => {
  const [favoriteTools, setFavoriteTools] = useState<Tool[]>([]);

  const toggleFavorite = (tool: Tool) => {
    setFavoriteTools((prev) => {
      if (prev.some((fav) => fav.id === tool.id)) {
        return prev.filter((fav) => fav.id !== tool.id);
      } else {
        return [...prev, tool];
      }
    });
  };

  const isFavorite = (toolId: string) => {
    return favoriteTools.some((tool) => tool.id === toolId);
  };

  return (
    <FavoriteToolsContext.Provider
      value={{ favoriteTools, toggleFavorite, isFavorite }}
    >
      {children}
    </FavoriteToolsContext.Provider>
  );
};
