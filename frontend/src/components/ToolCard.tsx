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
        width: { xs: "166px", sm: "166px", md: "260px" },
        height: "200px",
        display: "flex",
        flexDirection: "column",
        cursor: "pointer",
        p: 2,
        border: "1px solid #282828",
        backgroundColor: "main.background.default",
        "&:hover": { borderColor: "#1ea54c" },
        transition: "border-color 0.3s ease-in-out",
        overflow: "hidden", 
      }}
      onClick={() => navigate(`/tools/${tool.id}`)}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          width: "100%",
          height: "100%", 
          justifyContent: "space-between",
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "flex-start",
            justifyContent: "space-between",
            flexDirection: "row",
            width: "100%",
            mb: 1, 
          }}
        >
          <IconComponent sx={{ 
            fontSize: { xs: 40, sm: 45, md: 50 }, 
            color: "custom.icon" 
          }} />
          <IconButton 
            onClick={handleFavoriteClick}
            sx={{ p: { xs: 0.5, sm: 0.75, md: 1 } }} 
          >
            {isFavorite ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteIcon sx={{ color: "custom.icon" }} />
            )}
          </IconButton>
        </Box>

        <Box sx={{ flex: 1, overflow: "hidden" }}> 
          <Typography 
            variant="h6" 
            sx={{
              fontSize: { xs: '0.9rem', sm: '1rem', md: '1.25rem' },
              fontWeight: 500,
              mb: 0.5, 
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis", 
            }}
            title={tool.name} 
          >
            {tool.name}
          </Typography>
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{
              display: "-webkit-box",
              WebkitLineClamp: { xs: 3, sm: 2, md: 3 },
              WebkitBoxOrient: "vertical",
              overflow: "hidden",
              textOverflow: "ellipsis",
              fontSize: { xs: '0.75rem', sm: '0.75rem', md: '0.875rem' }, 
            }}
            title={tool.description}
          >
            {tool.description}
          </Typography>
        </Box>
      </Box>
    </Card>
  );
};

export default ToolCard;