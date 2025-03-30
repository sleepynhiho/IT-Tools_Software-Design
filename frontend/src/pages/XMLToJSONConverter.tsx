import { useState } from "react";
import {
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  IconButton,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import FavoriteIcon from "@mui/icons-material/Favorite";

import convert from "xml-js";

const XMLToJSONConverter = () => {
  const [xml, setXml] = useState('<a x="1.234" y="It\'s"/>');
  const [json, setJson] = useState("");

  const handleConvert = () => {
    try {
      const result = convert.xml2json(xml, { compact: true, spaces: 2 });
      setJson(result);
    } catch (error) {
      setJson("Invalid XML");
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(json);
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
            XML to JSON Converter
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
          Convert XML content into JSON format easily.
        </Typography>

        <Paper
          elevation={3}
          sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
        >
          <Box
            sx={{
              display: "flex",
              gap: 2,
              p: 3,
              color: "white",
              flexDirection: "row",
            }}
          >
            {/* XML Input */}
            <Box flex={1}>
              <Typography>Your XML content</Typography>
              <TextField
                fullWidth
                multiline
                minRows={10}
                value={xml}
                onChange={(e) => setXml(e.target.value)}
                sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
              />
            </Box>

            {/* JSON Output */}
            <Box flex={1}>
              <Typography>Converted JSON</Typography>
              <TextField
                fullWidth
                multiline
                minRows={10}
                value={json}
                InputProps={{ readOnly: true }}
                sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
              />
              <IconButton onClick={handleCopy} sx={{ color: "white" }}>
                <ContentCopyIcon />
              </IconButton>
            </Box>
          </Box>
          <Button
            variant="contained"
            sx={{
              mt: 2,
              bgcolor: "#36ad6a",
              color: "white",
              "&:hover": { bgcolor: "#1565c0" },
            }}
            fullWidth
            onClick={handleConvert}
          >
            Convert
          </Button>
        </Paper>
      </Box>
    </Box>
  );
};

export default XMLToJSONConverter;
