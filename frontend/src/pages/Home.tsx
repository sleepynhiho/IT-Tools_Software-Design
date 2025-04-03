import { Box, Container, Grid, Typography } from "@mui/material";
import { mockMetadata } from "../data/mockMetadata";
import ToolCard from "../components/ToolCard";
import { useFavoriteTools } from "../context/FavoriteToolsContext";

const Home = () => {
  const { favoriteTools, toggleFavorite } = useFavoriteTools();

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
          {favoriteTools.length > 0 && (
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
          )}

          {/* Display the favorite tools */}
          <Grid container spacing={3} justifyContent="flex-start">
            {favoriteTools.map((tool) => (
              <Grid item xs={12} sm={6} md={4} lg={2.4} key={tool.id}>
                <ToolCard tool={tool} onFavoriteToggle={toggleFavorite} />
              </Grid>
            ))}
          </Grid>

          <Typography sx={{ mb: 1, mt: 1 }} variant="h4" component="h1">
            <span
              style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}
            >
              All the tools
            </span>
          </Typography>

          {/* Display all tools */}
          <Grid container spacing={3} justifyContent="flex-start">
            {mockMetadata.map((tool) => (
              <Grid item xs={12} sm={6} md={4} lg={2.4} key={tool.id}>
                <ToolCard tool={tool} onFavoriteToggle={toggleFavorite} />
              </Grid>
            ))}
          </Grid>
        </Box>
      </Container>
    </Box>
  );
};

export default Home;
