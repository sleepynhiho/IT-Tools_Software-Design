import { useState } from "react";
import {
  Box,
  Button,
  IconButton,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import FavoriteIcon from "@mui/icons-material/Favorite";

const bases = [
  { label: "Binary (2)", base: 2 },
  { label: "Octal (8)", base: 8 },
  { label: "Decimal (10)", base: 10 },
  { label: "Hexadecimal (16)", base: 16 },
  { label: "Base64 (64)", base: 64 },
];

const convertBase = (num, fromBase, toBase) => {
  if (toBase === 64) {
    return btoa(String.fromCharCode(parseInt(num, fromBase)));
  }
  return parseInt(num, fromBase).toString(toBase);
};

const IntegerBaseConverter = () => {
  const [inputNumber, setInputNumber] = useState("42");
  const [inputBase, setInputBase] = useState(10);

  const handleCopy = (text) => {
    navigator.clipboard.writeText(text);
  };

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
            Integer base converter
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
          Convert a number between different bases (decimal, hexadecimal,
          binary, octal, base64, ...)
        </Typography>

        <Paper
          elevation={3}
          sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
        >
          <Typography>Input number</Typography>
          <TextField
            fullWidth
            size="small"
            variant="outlined"
            value={inputNumber}
            onChange={(e) => setInputNumber(e.target.value)}
            sx={{ bgcolor: "#333", input: { color: "white", py: 0.5 }, my: 1 }}
          />

          <Typography>Input base</Typography>
          <TextField
            fullWidth
            size="small"
            type="number"
            variant="outlined"
            value={inputBase}
            onChange={(e) => setInputBase(Number(e.target.value) || 10)}
            sx={{ bgcolor: "#333", input: { color: "white", py: 0.5 }, my: 1 }}
          />

          {bases.map(({ label, base }) => (
            <Box
              key={base}
              display="flex"
              alignItems="center"
              sx={{
                my: 1,
                bgcolor: "#292929",
                p: 1,
                borderRadius: 1,
                color: "white",
              }}
            >
              <Typography sx={{ color: "white", width: "210px" }}>
                {label}
              </Typography>
              <TextField
                fullWidth
                size="small"
                disabled
                value={convertBase(inputNumber, inputBase, base)}
                sx={{
                  bgcolor: "#333",
                  "& .MuiInputBase-input": { color: "white !important" },
                  "& .Mui-disabled": {
                    WebkitTextFillColor: "white !important",
                  },
                }}
              />

              <IconButton
                onClick={() =>
                  handleCopy(convertBase(inputNumber, inputBase, base))
                }
              >
                <ContentCopyIcon sx={{ color: "white" }} />
              </IconButton>
            </Box>
          ))}
        </Paper>
      </Box>
    </Box>
  );
};

export default IntegerBaseConverter;
