import { Box, Typography, IconButton } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import { useFavoriteTools } from "../context/FavoriteToolsContext";

interface CommonLayoutProps {
  title: string;
  description: string;
  toolId: string;
  icon: string;
  children: React.ReactNode;
}

const CommonLayout: React.FC<CommonLayoutProps> = ({
  title,
  description,
  toolId,
  icon,
  children,
}) => {
  const { toggleFavorite, isFavorite } = useFavoriteTools();

  console.log("CommonLayout", { title, description, toolId, icon });
  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        height: "100%",
        width: "100%",
      }}
    >
      <Box
        sx={{
          p: 1,
          maxWidth: 600,
          height: "400px",
          display: "flex",
          flexDirection: "column",
          justifyContent: "flex-start",
        }}
      >
        <Box sx={{ display: "flex", justifyContent: "space-between" }}>
          <Typography variant="h4" sx={{ fontWeight: "bold" }}>
            {title}
          </Typography>

          <IconButton
            onClick={(e) => {
              e.stopPropagation();
              toggleFavorite({
                id: toolId,
                name: title,
                icon,
                category: "",
              });
            }}
          >
            {isFavorite(toolId) ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteIcon sx={{ color: "custom.icon" }} />
            )}
          </IconButton>
        </Box>
        <Box sx={{ display: "flex", justifyContent: "flex-start", my: 1 }}>
          <Box
            sx={{ width: "200px", height: "1px", backgroundColor: "#a1a1a1" }}
          />
        </Box>
        <Typography sx={{ color: "gray", mb: 3 }}>{description}</Typography>

        {children}
      </Box>
    </Box>
  );
};

export default CommonLayout;
