import { Box, Container, Grid, Typography, CircularProgress } from "@mui/material"; 
import ToolCard from "../components/ToolCard";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAllPluginMetadata } from "../data/pluginList"; 

const Home = () => {
  const { favoriteTools, toggleFavorite } = useFavoriteTools();
  const { metadataList = [], loading, loadingProgress, error } = useAllPluginMetadata(); 

  console.log("Home", { favoriteTools, metadataList, loading, loadingProgress });

  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        justifyContent: "center",
        alignContent: "center",
        minHeight: "100vh",
        paddingTop: "10px",
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            padding: "20px",
            justifyContent: "center",
          }}
        >
          {/* Favorite Tools Section */}
          {favoriteTools.length > 0 && (
            <>
              <Typography sx={{ mb: 1 }} variant="h4" component="h1">
                <span
                  style={{
                    fontSize: "1rem",
                    fontWeight: "bold",
                    color: "#a3a3a3",
                  }}
                >
                  Your favorite tools
                </span>
              </Typography>

          {/* Display the favorite tools */}
          <Grid container spacing={3} justifyContent="flex-start">
            {favoriteTools.map((tool) => (
              <Grid item xs={12} sm={6} md={4} lg={2.4} key={`favorite-${tool.id}`}>
                <ToolCard tool={tool} onFavoriteToggle={toggleFavorite} />
              </Grid>
            ))}
          </Grid>
            </>
          )}

          {/* All Tools Section */}
          <Typography sx={{ mb: 1, mt: 3 }} variant="h4" component="h1">
            <span
              style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}
            >
              All the tools
            </span>
          </Typography>

          {loading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', my: 4 }}>
              <CircularProgress 
                variant="determinate" 
                value={loadingProgress} 
                size={60}
                thickness={4}
                sx={{ 
                  color: '#1ea54c',
                  '& .MuiCircularProgress-circle': {
                    strokeLinecap: 'round',
                  }
                }}
              />
              <Typography variant="body2" sx={{ mt: 2, color: '#a3a3a3' }}>
                Loading plugins ({loadingProgress}%)
              </Typography>
              <Typography variant="caption" sx={{ mt: 0.5, color: '#727272', fontSize: '0.75rem', textAlign: 'center', maxWidth: 300 }}>
                {loadingProgress < 25 ? "Discovering available plugins..." :
                 loadingProgress < 75 ? "Loading plugin metadata..." :
                 "Almost there..."}
              </Typography>
            </Box>
          ) : error ? (
            <Typography color="error">{error}</Typography>
          ) : (
            <Grid container spacing={3} justifyContent="flex-start">
              {metadataList.map((tool) => (
                <Grid item xs={12} sm={6} md={4} lg={2.4} key={`all-${tool.id}`}>
                  <ToolCard tool={tool} onFavoriteToggle={toggleFavorite} />
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      </Container>
    </Box>
  );
};

export default Home;