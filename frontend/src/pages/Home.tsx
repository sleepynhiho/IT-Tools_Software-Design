// src/pages/Home.tsx
import React from 'react'; // Import React
import { Box, Container, Grid, Typography, CircularProgress } from "@mui/material";
import ToolCard from "../components/ToolCard"; // Ensure path is correct
import { useFavoriteTools } from "../context/FavoriteToolsContext";
// --- Use the CORRECT Context Hook ---
import { useAllTools } from "../context/AllToolsContext"; // <<<--- IMPORT THIS
// --- Remove the OLD Hook Import ---
// import { useAllPluginMetadata } from "../data/pluginList"; // DELETE or COMMENT OUT

const Home = () => {
  // Get state and functions from contexts
  // Favorite tools data comes from its own context
  const { favoriteTools, toggleFavorite, isFavorite } = useFavoriteTools();
  // All tools data now comes from AllToolsContext
  const { allTools, isLoading, error } = useAllTools(); // <<<--- USE THIS HOOK

  // --- Remove the OLD Hook Call ---
  // const { metadataList = [], loading: oldLoading, loadingProgress, error: oldError } = useAllPluginMetadata();

  // Log the state from the correct contexts for debugging
  console.log("Home - Render", {
      favoriteToolsCount: favoriteTools.length,
      allToolsCount: allTools.length,
      isLoadingAllTools: isLoading, // Use isLoading from useAllTools
      allToolsError: error       // Use error from useAllTools
  });

  // Helper function remains the same, uses isFavorite from useFavoriteTools
  const checkIsFavorite = (toolId: string): boolean => {
     return isFavorite(toolId);
  };

  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start", // Align content to the top
        minHeight: "calc(100vh - 64px)", // Example: Adjust for header height
        paddingTop: "10px",
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            padding: "20px",
            // Removed justifyContent center to allow top alignment
          }}
        >
          {/* Favorite Tools Section */}
          {/* This section correctly uses favoriteTools from useFavoriteTools */}
          {favoriteTools && favoriteTools.length > 0 && (
            <>
              <Typography sx={{ mb: 1 }} variant="h4" component="h1">
                <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
                  Your favorite tools
                </span>
              </Typography>
              <Grid container spacing={3} justifyContent="flex-start">
                {favoriteTools.map((tool) => (
                  <Grid item xs={12} sm={6} md={4} lg={2.4} key={`favorite-${tool.id}`}>
                    <ToolCard
                      tool={tool}
                      isFavorite={true} // Items in this list are always favorites
                      onFavoriteToggle={toggleFavorite}
                    />
                  </Grid>
                ))}
              </Grid>
            </>
          )}

          {/* All Tools Section */}
          <Typography sx={{ mb: 1, mt: favoriteTools.length > 0 ? 3 : 0 }} variant="h4" component="h1">
            <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
              All the tools
            </span>
          </Typography>

          {/* Use isLoading state FROM useAllTools */}
          {isLoading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', my: 4 }}>
              <CircularProgress
                size={60}
                thickness={4}
                sx={{ color: '#1ea54c' }} // Example color
              />
              <Typography variant="body2" sx={{ mt: 2, color: '#a3a3a3' }}>
                Loading Tools...
              </Typography>
              {/* Removed the complex loadingProgress text */}
            </Box>
          ) : error ? (
             // Use error state FROM useAllTools
            <Typography color="error">Error loading tools: {error}</Typography>
          ) : (
            // Use allTools FROM useAllTools
            <Grid container spacing={3} justifyContent="flex-start">
              {allTools && allTools.length > 0 ? (
                  allTools.map((tool) => (
                    <Grid item xs={12} sm={6} md={4} lg={2.4} key={`all-${tool.id}`}>
                      <ToolCard
                        tool={tool}
                        // Check against the isFavorite function from FavoriteToolsContext
                        isFavorite={checkIsFavorite(tool.id)}
                        onFavoriteToggle={toggleFavorite}
                      />
                    </Grid>
                  ))
              ) : (
                  <Typography sx={{ mt: 2, color: '#727272' }}>No tools available for you.</Typography>
              )}
            </Grid>
          )}
        </Box>
      </Container>
    </Box>
  );
};

export default Home;