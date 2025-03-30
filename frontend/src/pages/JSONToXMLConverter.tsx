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

const JSONToXMLConverter = () => {
  const [json, setJson] = useState(
    JSON.stringify({ a: { _attributes: { x: "1.234", y: "It's" } } }, null, 2)
  );
  const [xml, setXml] = useState("");
  const [liked, setLiked] = useState(false);

  const handleConvert = () => {
    try {
      // Validate JSON input
      const parsedJson = JSON.parse(json);
      try {
        const result = convert.json2xml(parsedJson, {
          compact: true,
          spaces: 2,
        });
        setXml(result);
      } catch (conversionError) {
        setXml(
          "Valid JSON, but cannot be converted to XML. Please check the structure."
        );
      }
    } catch (error) {
      setXml("Invalid JSON");
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(xml);
  };

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
            JSON to XML Converter
          </Typography>
          <IconButton onClick={() => setLiked(!liked)}>
            {liked ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteIcon sx={{ color: "custom.icon" }} />
            )}
          </IconButton>
        </Box>

        <Typography sx={{ color: "gray", mb: 3 }}>
          Convert JSON content into XML format easily.
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
            {/* JSON Input */}
            <Box flex={1}>
              <Typography>Your JSON content</Typography>
              <TextField
                fullWidth
                multiline
                minRows={10}
                value={json}
                onChange={(e) => setJson(e.target.value)}
                sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
              />
            </Box>

            {/* XML Output */}
            <Box flex={1}>
              <Typography>Converted XML</Typography>
              <TextField
                fullWidth
                multiline
                minRows={10}
                value={xml} // Fixed to use the xml state variable
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

export default JSONToXMLConverter;
