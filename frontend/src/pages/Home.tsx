import { Grid, Container, Box } from "@mui/material";
import ToolCard from "../components/ToolCard";
import { mockMetadata } from "../data/mockMetadata";

const Home = () => {
  return (
    <Box
      component="main"
      sx={{
        // flexGrow: 1,
        display: "flex",
        justifyContent: "center", // Căn giữa theo chiều ngang
        alignContent: "center",
        minHeight: "100vh", // Đảm bảo full chiều cao màn hình
        paddingTop: "64px", // Để tránh bị che bởi AppBar
      }}
    >
      <Container maxWidth="lg">
        <Grid container spacing={3} justifyContent="flex-start">
          {mockMetadata.map((tool) => (
            <Grid item xs={12} sm={6} md={4} lg={2.4} key={tool.id}>
              <ToolCard tool={tool} />
            </Grid>
          ))}
        </Grid>
      </Container>
    </Box>
  );
};

export default Home;
