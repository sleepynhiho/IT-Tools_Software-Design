import { Card, Typography, IconButton, Box } from "@mui/material";
import * as MuiIcons from "@mui/icons-material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import { useNavigate } from "react-router-dom";
import { useFavoriteTools } from "../context/FavoriteToolsContext";

const ToolCard = ({
  tool,
  onFavoriteToggle,
}: {
  tool: { id: string; name: string; description: string; icon: string };
  onFavoriteToggle: (tool: any) => void;
}) => {
  const IconComponent =
    MuiIcons[tool.icon as keyof typeof MuiIcons] || MuiIcons.HelpOutline;

  const { favoriteTools } = useFavoriteTools();
  const isFavorite = favoriteTools.some((fav) => fav.id === tool.id);

  const navigate = useNavigate();

  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onFavoriteToggle(tool);
  };

  return (
    <Card
      sx={{
        width: "100%",
        maxWidth: { xs: "100%", sm: "166px", md: "230px" },
        height: "200px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        cursor: "pointer",
        p: 2,
        border: "1px solid #282828",
        backgroundColor: "main.background.default",
        "&:hover": { borderColor: "#1ea54c" },
        transition: "border-color 0.3s ease-in-out",
      }}
      onClick={() => navigate(`/tools/${tool.id}`)}
    >
      <Box
        sx={{
          display: "flex",
          alignItems: "start",
          gap: 2,
          flexDirection: "column",
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            flexDirection: "row",
            width: "100%",
          }}
        >
          <IconComponent sx={{ fontSize: 50, color: "custom.icon" }} />
          <IconButton onClick={handleFavoriteClick}>
            {isFavorite ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteIcon sx={{ color: "custom.icon" }} />
            )}
          </IconButton>
        </Box>

        <Box>
          <Typography variant="h6">{tool.name}</Typography>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{
              display: "-webkit-box",
              WebkitLineClamp: 2,
              WebkitBoxOrient: "vertical",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {tool.description}
          </Typography>
        </Box>
      </Box>
    </Card>
  );
};

export default ToolCard;
