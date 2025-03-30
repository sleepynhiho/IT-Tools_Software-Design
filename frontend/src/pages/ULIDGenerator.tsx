import { useState, useEffect } from "react";
import {
  Box,
  Button,
  IconButton,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import { ulid } from "ulid";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import FavoriteIcon from "@mui/icons-material/Favorite";

const ULIDGenerator = () => {
  const [quantity, setQuantity] = useState(1);
  const [format, setFormat] = useState("raw");
  const [ulidValue, setUlidValue] = useState("");

  const generateULID = () => {
    const ulids = Array.from({ length: quantity }, () => ulid());
    setUlidValue(
      format === "json" ? JSON.stringify(ulids, null, 2) : ulids.join("\n")
    );
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(ulidValue);
  };

  useEffect(() => {
    generateULID();
  }, [quantity, format]);

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
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignContent: "center",
          }}
        >
          <Typography variant="h4" sx={{ fontWeight: "bold" }}>
            ULID Generator
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

        <Typography sx={{ color: "gray", mb: 3 }}>
          Generate random Universally Unique Lexicographically Sortable
          Identifier (ULID).
        </Typography>

        <Paper
          elevation={3}
          sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
        >
          <Box
            display={"flex"}
            justifyContent="space-between"
            flexDirection="row"
            alignItems="center"
            sx={{ mb: 2 }}
          >
            <Typography sx={{ mb: 1 }}>Quantity:</Typography>
            <Box display="flex" alignItems="center" sx={{ mb: 2, flexGrow: 1 }}>
              <TextField
                type="number"
                value={quantity}
                onChange={(e) =>
                  setQuantity(Math.max(1, parseInt(e.target.value) || 1))
                }
                sx={{
                  textAlign: "center",
                  mx: 1,
                  bgcolor: "#333",
                  color: "white",
                  flexGrow: 1,
                  height: "30px",
                  borderRadius: 1,
                  "& .MuiInputBase-root": { height: "30px" },
                }}
                inputProps={{
                  min: 1,
                  style: {
                    textAlign: "start",
                    fontSize: "14px",
                    color: "white",
                    padding: "8px 12px", // Căn chỉnh văn bản bên trong
                  },
                }}
              />
              <IconButton
                sx={{ color: "white" }}
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
              >
                -
              </IconButton>
              <IconButton
                sx={{ color: "white" }}
                onClick={() => setQuantity(quantity + 1)}
              >
                +
              </IconButton>
            </Box>
          </Box>

          <Typography sx={{ mb: 1 }}>Format:</Typography>
          <Box display="flex" gap={1} sx={{ mb: 2 }}>
            <Button
              variant={format === "raw" ? "contained" : "outlined"}
              color="success"
              onClick={() => setFormat("raw")}
            >
              Raw
            </Button>
            <Button
              variant={format === "json" ? "contained" : "outlined"}
              color="secondary"
              onClick={() => setFormat("json")}
            >
              JSON
            </Button>
          </Box>

          <Box
            sx={{
              bgcolor: "#292929",
              p: 2,
              borderRadius: 1,
              fontFamily: "monospace",
              wordBreak: "break-word",
              whiteSpace: "pre-wrap",
            }}
          >
            {ulidValue}
          </Box>

          {/* Action Buttons */}
          <Box mt={2} display="flex" justifyContent="space-between">
            <Button
              variant="contained"
              startIcon={<ContentCopyIcon />}
              onClick={copyToClipboard}
              sx={{ bgcolor: "#36ad6a", "&:hover": { bgcolor: "#2e8b57" } }}
            >
              Copy
            </Button>
            <Button
              variant="contained"
              startIcon={<RefreshIcon />}
              sx={{ bgcolor: "#f57c00", "&:hover": { bgcolor: "#e65100" } }}
              onClick={generateULID}
            >
              Refresh
            </Button>
          </Box>
        </Paper>
      </Box>
    </Box>
  );
};

export default ULIDGenerator;
