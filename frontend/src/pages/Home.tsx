import { Box, Container, Grid, Typography, CircularProgress } from "@mui/material";
import ToolCard from "../components/ToolCard";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAllTools } from "../context/AllToolsContext";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const Home = () => {
  const navigate = useNavigate();
  const { favoriteTools, toggleFavorite, isFavorite } = useFavoriteTools();
  const { 
    allTools, 
    isLoading, 
    error, 
    updateToolStatus,
    updateToolAccessLevel 
  } = useAllTools();
  const { userType } = useAuth();
  const isAdmin = userType === 'admin';

  // Only show enabled tools to non-admin users
  const visibleTools = isAdmin
    ? allTools
    : allTools.filter(tool => tool.status !== 'disabled');
    
  // Get only visible favorite tools
  const visibleFavoriteTools = favoriteTools.filter(tool => {
    // For non-admin users, filter out disabled tools
    if (!isAdmin && tool.status === 'disabled') return false;
    return true;
  });

  console.log("Home - Render", {
      favoriteToolsCount: visibleFavoriteTools.length,
      allToolsCount: visibleTools.length,
      isLoadingAllTools: isLoading,
      allToolsError: error,
      isUserAdmin: isAdmin
  });

  const checkIsFavorite = (toolId: string): boolean => {
     return isFavorite(toolId);
  };

  // Handler for removing a tool
  const handleRemoveTool = async (toolId: string) => {
    if (isAdmin) {
      try {
        // await removeTool(toolId);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to remove tool:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for toggling tool status
  const handleToolStatusToggle = async (toolId: string, newStatus: 'enabled' | 'disabled') => {
    if (isAdmin) {
      try {
        await updateToolStatus(toolId, newStatus);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to update tool status:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for changing tool access level
  const handleToolAccessLevelChange = async (toolId: string, newLevel: 'normal' | 'premium') => {
    if (isAdmin) {
      try {
        await updateToolAccessLevel(toolId, newLevel);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to update tool access level:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for navigating to add tool page
  const handleAddToolClick = () => {
    navigate("/admin/tools/new");
  };

  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
        minHeight: "calc(100vh - 64px)",
        paddingTop: "10px",
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            padding: "20px",
          }}
        >
          {/* Favorite Tools Section - only show if there are visible favorites */}
          {visibleFavoriteTools && visibleFavoriteTools.length > 0 && (
            <>
              <Typography sx={{ mb: 1 }} variant="h4" component="h1">
                <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
                  Your favorite tools
                </span>
              </Typography>
              <Grid container spacing={3} justifyContent="flex-start">
                {visibleFavoriteTools.map((tool) => (
                  <Grid item xs={12} sm={6} md={4} lg={2.4} key={`favorite-${tool.id}`}>
                    <ToolCard
                      tool={tool}
                      isFavorite={true}
                      onFavoriteToggle={toggleFavorite}
                      onToolRemove={isAdmin ? handleRemoveTool : undefined}
                      onToolStatusToggle={isAdmin ? handleToolStatusToggle : undefined}
                      onToolAccessLevelChange={isAdmin ? handleToolAccessLevelChange : undefined}
                    />
                  </Grid>
                ))}
              </Grid>
            </>
          )}

          {/* All Tools Section */}
          <Typography sx={{ mb: 1, mt: visibleFavoriteTools.length > 0 ? 3 : 0 }} variant="h4" component="h1">
            <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
              All the tools
            </span>
          </Typography>

          {isLoading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', my: 4 }}>
              <CircularProgress
                size={60}
                thickness={4}
                sx={{ color: '#1ea54c' }}
              />
              <Typography variant="body2" sx={{ mt: 2, color: '#a3a3a3' }}>
                Loading Tools...
              </Typography>
            </Box>
          ) : error ? (
            <Typography color="error">Error loading tools: {error}</Typography>
          ) : (
            <Grid container spacing={3} justifyContent="flex-start">
              {visibleTools && visibleTools.length > 0 ? (
                  visibleTools.map((tool) => (
                    <Grid item xs={12} sm={6} md={4} lg={2.4} key={`all-${tool.id}`}>
                      <ToolCard
                        tool={tool}
                        isFavorite={checkIsFavorite(tool.id)}
                        onFavoriteToggle={toggleFavorite}
                        onToolRemove={isAdmin ? handleRemoveTool : undefined}
                        onToolStatusToggle={isAdmin ? handleToolStatusToggle : undefined}
                        onToolAccessLevelChange={isAdmin ? handleToolAccessLevelChange : undefined}
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