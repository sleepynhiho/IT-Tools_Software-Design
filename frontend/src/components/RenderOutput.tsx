/* eslint-disable @typescript-eslint/no-explicit-any */
import { Box, Paper, Typography, TextField, Button } from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import { DownloadIcon } from "lucide-react";

const RenderOutput = (output: any, value: string, onRefresh: () => void) => {
  if (!output) return null;

  const displayValue = value || output.default;
  let content;

  switch (output.type) {
    case "typography":
      content = (
        <Typography sx={{ mb: 2, color: "white" }} variant="h6">
          {displayValue}
        </Typography>
      );
      break;
    case "image":
      content = (
        <Box textAlign="center">
          <img
            src={displayValue}
            alt={output.title || "Generated image"}
            style={{ maxWidth: "100%", borderRadius: 8 }}
          />
        </Box>
      );
      break;

    case "text":
    default:
      content = (
        <TextField
          fullWidth
          value={value}
          multiline
          InputProps={{ readOnly: true }}
          sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
        />
      );
      break;
  }

  return (
    <Paper sx={{ p: 2, bgcolor: "#212121", color: "white", borderRadius: 2 }}>
      <Typography variant="subtitle1" sx={{ mb: 2 }}>
        {output.title}
      </Typography>
      {content}
      <Box mt={2} display="flex" gap={2}>
        {output.buttons?.includes("copy") && output.type !== "image" && (
          <Button
            variant="contained"
            startIcon={<ContentCopyIcon />}
            onClick={() => navigator.clipboard.writeText(value)}
            sx={{ bgcolor: "#36ad6a" }}
          >
            Copy
          </Button>
        )}
        {output.buttons?.includes("refresh") && (
          <Button
            variant="contained"
            startIcon={<RefreshIcon />}
            onClick={onRefresh}
            sx={{ bgcolor: "#f57c00" }}
          >
            Refresh
          </Button>
        )}
        {output.buttons?.includes("download") && output.type === "image" && (
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            onClick={() => {
              const link = document.createElement("a");
              link.href = displayValue;
              link.download = `${output.name || "image"}.png`;
              document.body.appendChild(link);
              link.click();
              document.body.removeChild(link);
            }}
            sx={{ bgcolor: "#1976d2" }}
          >
            Download
          </Button>
        )}
      </Box>
    </Paper>
  );
};

export default RenderOutput;
