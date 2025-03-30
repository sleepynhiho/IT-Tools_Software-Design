import { useState } from "react";
import {
  Box,
  Typography,
  Switch,
  Slider,
  TextField,
  Button,
  Paper,
  IconButton,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import FavoriteIcon from "@mui/icons-material/Favorite";

const TokenGenerator = () => {
  const [uppercase, setUppercase] = useState(true);
  const [lowercase, setLowercase] = useState(true);
  const [numbers, setNumbers] = useState(true);
  const [symbols, setSymbols] = useState(false);
  const [length, setLength] = useState(64);
  const [token, setToken] = useState("");

  const generateToken = () => {
    let chars = "";
    if (uppercase) chars += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    if (lowercase) chars += "abcdefghijklmnopqrstuvwxyz";
    if (numbers) chars += "0123456789";
    if (symbols) chars += "!@#$%^&*()_+-=[]{}|;:,.<>?";

    let result = "";
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setToken(result);
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(token);
  };

  useState(() => {
    generateToken();
  });
  // Common code
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
            Token Generator
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
          Generate random string with the chars you want, uppercase or lowercase
          letters, numbers and/or symbols.
        </Typography>
        // End of common code
        <Paper
          elevation={3}
          sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
        >
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography>Uppercase (ABC...)</Typography>
            <Switch
              checked={uppercase}
              onChange={() => setUppercase(!uppercase)}
              sx={{
                "& .MuiSwitch-switchBase.Mui-checked": { color: "#36ad6a" },
                "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
                  bgcolor: "#36ad6a",
                },
              }}
            />
          </Box>
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography>Lowercase (abc...)</Typography>
            <Switch
              checked={lowercase}
              onChange={() => setLowercase(!lowercase)}
              sx={{
                "& .MuiSwitch-switchBase.Mui-checked": { color: "#36ad6a" },
                "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
                  bgcolor: "#36ad6a",
                },
              }}
            />
          </Box>
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography>Numbers (123...)</Typography>
            <Switch
              checked={numbers}
              onChange={() => setNumbers(!numbers)}
              sx={{
                "& .MuiSwitch-switchBase.Mui-checked": { color: "#36ad6a" },
                "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
                  bgcolor: "#36ad6a",
                },
              }}
            />
          </Box>
          <Box display="flex" justifyContent="space-between" mb={2}>
            <Typography>Symbols (!;-...)</Typography>
            <Switch
              checked={symbols}
              onChange={() => setSymbols(!symbols)}
              sx={{
                "& .MuiSwitch-switchBase.Mui-checked": { color: "#36ad6a" },
                "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
                  bgcolor: "#36ad6a",
                },
              }}
            />
          </Box>

          {/* Length Slider */}
          <Typography>Length ({length})</Typography>
          <Slider
            value={length}
            onChange={(_, newValue) => setLength(newValue as number)}
            min={8}
            max={128}
            sx={{
              color: "#36ad6a",
              "& .MuiSlider-thumb": { bgcolor: "#36ad6a" },
              "& .MuiSlider-track": { bgcolor: "#36ad6a" },
              "& .MuiSlider-rail": { bgcolor: "#a1a1a1" },
            }}
          />

          {/* Token Output */}
          <TextField
            fullWidth
            value={token}
            multiline
            sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
            InputProps={{ readOnly: true }}
          />

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
              onClick={generateToken}
            >
              Refresh
            </Button>
          </Box>
        </Paper>
      </Box>
    </Box>
  );
};

export default TokenGenerator;
