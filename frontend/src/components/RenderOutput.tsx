import { JSX } from "react";
import { Box, Typography, TextField, Button, useTheme } from "@mui/material";
import { SxProps, Theme } from "@mui/material/styles";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Download as DownloadIcon } from "lucide-react";

import type { OutputField } from "../data/pluginList";

interface RenderOutputProps {
  output: OutputField;
  value: any;
  resultData?: Record<string, any> | null;
  onRefresh?: () => void;
  disabled?: boolean;
}

const RenderOutput: React.FC<RenderOutputProps> = ({
  output,
  value,
  resultData,
  onRefresh,
  disabled = false,
}) => {
  // IMPORTANT: All hooks MUST be called at the top level, unconditionally
  const theme = useTheme();

  if (!output) {
    return null;
  }

  // Helper functions for button operations
  const handleCopy = () => {
    const textToCopy = typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value ?? '');
    navigator.clipboard.writeText(textToCopy).catch(err => console.error('Copy failed', err));
  };
  
  const handleDownload = () => {
    const filenameKey = output.downloadFilenameKey || 'imageFileName'; 
    const filename = resultData?.[filenameKey] || `${output.id || 'download'}.png`;
    downloadDataUrl(String(value), filename);
  };

  function downloadDataUrl(dataUrl: string, filename: string) {
    try {
      if (!dataUrl || typeof dataUrl !== 'string' || !dataUrl.startsWith('data:')) {
        console.error("Invalid data URL provided for download:", dataUrl);
        alert("Could not initiate download: Invalid image data.");
        return;
      }
      const link = document.createElement('a');
      link.href = dataUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e) {
      console.error("Download failed:", e);
      alert("Download failed. See console for details.");
    }
  }

  const getButtonStyle = (
    buttonType: "copy" | "refresh" | "download"
  ): SxProps<Theme> => {
    const placement = output.buttonPlacement?.[buttonType];

    if (placement === "outside" || placement === undefined) {
      if (placement === "outside") {
        return {
          bgcolor: "#ffffff14",
          color: "#ffffffd1",
          "&:hover": { bgcolor: "#ffffff29" },
        };
      } else {
        let baseSx: SxProps<Theme> = {};
        switch (buttonType) {
          case "copy":
            baseSx = { bgcolor: "#36ad6a", "&:hover": { bgcolor: "#2a8c53" } };
            break;
          case "refresh":
            baseSx = { bgcolor: "#f57c00", "&:hover": { bgcolor: "#e65100" } };
            break;
          case "download":
            baseSx = { bgcolor: "#1976d2", "&:hover": { bgcolor: "#115293" } };
            break;
        }
        return { ...baseSx, color: "white" };
      }
    }

    return {
      bgcolor: "transparent", 
      color: "white", 
      minWidth: "auto",
      padding: "4px", 
      boxShadow: "none",
      border: "1px solid rgba(255, 255, 255, 0.2)", 
      backdropFilter: "blur(2px)", 
      "&:hover": {
        bgcolor: "rgba(255, 255, 255, 0.15)", 
        borderColor: "rgba(255, 255, 255, 0.3)", 
      },
    };
  };

  // --- Helper Function to Render a Single Button ---
  const renderButton = (buttonType: "copy" | "refresh" | "download") => {
    const buttonSx = getButtonStyle(buttonType);
    const placement = output.buttonPlacement?.[buttonType];
    const isInside = placement === "inside";

    const commonProps = {
      size: "small",
      sx: buttonSx,
      disabled: disabled,
    } as const;

    switch (buttonType) {
      case "copy":
        if (output.type === "image") return null;
        return (
          <Button
            {...commonProps}
            key="copy"
            onClick={handleCopy}
            disabled={
              !(value !== undefined && value !== null
                ? String(value)
                : String(output.default ?? "")) || disabled
            }
            title="Copy"
            sx={{
              ...buttonSx,
              minWidth: isInside ? "14px" : undefined,
              border: "none",
            }}
          >
            {isInside && (
              <ContentCopyIcon
                sx={{
                  fontSize: isInside ? "1rem" : "inherit",
                  color: "white",
                  border: "none", 
                }}
              />
            )}
            {!isInside && "Copy"}
          </Button>
        );
      case "refresh":
        return onRefresh ? (
          <Button
            {...commonProps}
            key="refresh"
            onClick={onRefresh}
            title="Refresh"
          >
            {isInside && (
              <RefreshIcon sx={{ fontSize: isInside ? "1rem" : "inherit" }} />
            )}
            {!isInside && "Refresh"}
          </Button>
        ) : null;
      case "download": {
        const displayValue =
          value !== undefined && value !== null
            ? String(value)
            : String(output.default ?? "");
        if (output.type !== "image" || !displayValue) return null;
        return (
          <Button
            {...commonProps}
            key="download"
            onClick={handleDownload}
            title="Download"
          >
            <DownloadIcon size={isInside ? 14 : 16} />{" "}
            {!isInside && "Download"}
          </Button>
        );
      }
      default:
        return null;
    }
  };

  // --- Filter Buttons ---
  const insideButtons =
    output.buttons?.filter((b) => output.buttonPlacement?.[b] === "inside") ??
    [];
  const outsideButtons =
    output.buttons?.filter((b) => output.buttonPlacement?.[b] !== "inside") ??
    [];

  // Get the effective display value for this output
  const getDisplayValue = () => {
    // If this is a direct value from the backend, use it
    if (resultData && resultData[output.id] !== undefined) {
      return resultData[output.id];
    }
    
    // Otherwise use the value passed directly to this component
    return value !== undefined && value !== null ? value : output.default ?? "";
  };

  // Render text content (the default type in our metadata)
  const renderTextContent = () => {
    const displayValue = getDisplayValue();
    
    return (
      <TextField
        fullWidth
        value={String(displayValue)}
        multiline={false}  // From metadata, all are single line
        rows={1}
        InputProps={{ 
          readOnly: true, 
          style: { 
            color: "white", 
            fontFamily: output.monospace ? "monospace" : "inherit",
            height: output.height ? `${output.height}px` : undefined
          }
        }}
        sx={{
          bgcolor: "#333",
          borderRadius: 1,
          width: output.width ? output.width : "100%",
          "& .MuiInputBase-input.Mui-disabled": {
            WebkitTextFillColor: "#aaa",
            color: "#aaa",
            cursor: "not-allowed",
          },
          "& .MuiInputBase-input": { cursor: "text" },
        }}
        disabled={disabled}
      />
    );
  };

  return (
    <Box
      sx={{
        position: "relative",
        p: 2,
        bgcolor: "#212121",
        color: "white",
        borderRadius: 2,
      }}
    >
      {/* Label */}
      {output.label && (
        <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: "bold" }}>
          {output.label}
        </Typography>
      )}

      <Box sx={{ position: "relative" }}>
        {/* For this specific tool, all outputs are text type */}
        {renderTextContent()}

        {/* Inside buttons */}
        {insideButtons.length > 0 && (
          <Box
            sx={{
              position: "absolute",
              bottom: theme.spacing(1),
              right: theme.spacing(1),
              zIndex: 2,
              display: "flex",
              gap: theme.spacing(0.75),
            }}
          >
            {insideButtons.map((buttonType) => renderButton(buttonType))}
          </Box>
        )}
      </Box>

      {/* Outside buttons */}
      {outsideButtons.length > 0 && (
        <Box
          mt={2}
          display="flex"
          flexWrap="wrap"
          gap={1.5}
          justifyContent="center"
        >
          {outsideButtons.map((buttonType) => renderButton(buttonType))}
        </Box>
      )}
    </Box>
  );
};

export default RenderOutput;