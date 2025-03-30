import { Box, Typography, IconButton } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import { useState } from "react";

const CommonLayout = ({ title, description, children }: any) => {
  const [liked, setLiked] = useState(false);

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
              setLiked(!liked);
            }}
          >
            {liked ? (
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

        {/* Nội dung riêng của từng công cụ */}
        {children}
      </Box>
    </Box>
  );
};

export default CommonLayout;
