import {
  Box,
  Typography,
  useTheme,
  IconButton,
  Button,
  Chip,
  LinearProgress,
  ListItem,
  ListItemText,
  Paper,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  alpha,
  Fade,
  Tooltip,
  Stack,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import CancelIcon from "@mui/icons-material/Cancel";
import DoneIcon from "@mui/icons-material/Done";
import {
  CheckCircleIcon,
  Download as DownloadIcon,
  List,
  Table,
} from "lucide-react";
import { useState } from "react";

import type { OutputField } from "../data/pluginList";

// Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted)
const CURRENT_DATE_TIME = "2025-05-06 21:21:41";
const CURRENT_USER_LOGIN = "hanhiho";

interface RenderOutputProps {
  output: OutputField;
  value: any;
  resultData?: Record<string, any> | null;
  onRefresh?: () => void;
  disabled?: boolean;
}

function downloadDataUrl(dataUrl: string, filename: string) {
  try {
    if (
      !dataUrl ||
      typeof dataUrl !== "string" ||
      !dataUrl.startsWith("data:")
    ) {
      console.error(
        `[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Invalid data URL provided for download:`,
        dataUrl
      );
      alert("Could not initiate download: Invalid image data.");
      return;
    }
    const link = document.createElement("a");
    link.href = dataUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (e) {
    console.error(
      `[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Download failed:`,
      e
    );
    alert("Download failed. See console for details.");
  }
}

const RenderOutput: React.FC<RenderOutputProps> = ({
  output,
  value,
  resultData,
  onRefresh,
  disabled,
}) => {
  const theme = useTheme();
  const [copied, setCopied] = useState(false);
  const [hovered, setHovered] = useState(false);

  const handleCopy = () => {
    const textToCopy =
      typeof value === "object"
        ? JSON.stringify(value, null, 2)
        : String(value ?? "");
    navigator.clipboard
      .writeText(textToCopy)
      .then(() => {
        console.log(
          `[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Copied ${output.id} content to clipboard`
        );
        setCopied(true);
        setTimeout(() => setCopied(false), 2000); // Reset after 2 seconds
      })
      .catch((err) =>
        console.error(
          `[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Copy failed`,
          err
        )
      );
  };

  const handleDownload = () => {
    const filenameKey = output.downloadFilenameKey || "imageFileName";
    const filename =
      resultData?.[filenameKey] || `${output.id || "download"}.png`;
    downloadDataUrl(String(value), filename);
  };

  const handleRefresh = () => {
    if (onRefresh) {
      console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Refreshing ${output.id} data`);
      onRefresh();
    }
  };

  const displayValue = value ?? "";
  const isValueNotNull = value != null;
  const isValueStringImage =
    typeof value === "string" && value.startsWith("data:image");
  const isArrayValue = Array.isArray(displayValue);
  const hasTableConfig =
    isArrayValue && output.columns && output.columns.length > 0;
  const progressValue =
    typeof displayValue === "number"
      ? Math.min(Math.max(displayValue, output.min ?? 0), output.max ?? 100)
      : 0;
  const score = typeof displayValue === "number" ? displayValue : 0;
  const progressColor =
    score >= 90
      ? "success"
      : score >= 70
      ? "info"
      : score >= 50
      ? "warning"
      : "error";
  const suffix = output.suffix || "";

  // Check if button placement should be inside
  const isButtonInside = output.buttonPlacement?.copy === "inside";

  // Standard button elements (used when not inside) as text buttons
  const buttonElements = !isButtonInside && (
    <Stack 
      direction="row" 
      spacing={1.2} 
      sx={{ 
        mt: 0.8,
        mb: 0.5,
        opacity: 0.9,
        transition: "opacity 0.2s ease",
        "&:hover": {
          opacity: 1,
        }
      }}
    >
      {output.buttons?.includes("copy") && isValueNotNull && (
        <Button
          variant="text"
          size="small"
          onClick={handleCopy}
          disabled={!isValueNotNull}
          startIcon={copied ? <DoneIcon fontSize="small" /> : <ContentCopyIcon fontSize="small" />}
          sx={{
            minWidth: 0,
            fontSize: "0.75rem",
            textTransform: "uppercase",
            fontWeight: 500,
            letterSpacing: "0.03em",
            py: 0.3,
            px: 1.2,
            color: copied ? "#1ea54c" : "#9e9e9e",
            '&:hover': {
              backgroundColor: copied ? alpha('#1ea54c', 0.08) : 'rgba(255, 255, 255, 0.05)',
              color: copied ? '#1ea54c' : '#bdbdbd'
            }
          }}
        >
          {copied ? "Copied" : "Copy"}
        </Button>
      )}
      
      {output.buttons?.includes("refresh") && onRefresh && (
        <Button
          variant="text"
          size="small"
          onClick={handleRefresh}
          disabled={disabled}
          startIcon={<RefreshIcon fontSize="small" />}
          sx={{
            minWidth: 0,
            fontSize: "0.75rem",
            textTransform: "uppercase",
            fontWeight: 500,
            letterSpacing: "0.03em",
            py: 0.3,
            px: 1.2,
            color: '#9e9e9e',
            '&:hover': {
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              color: '#bdbdbd'
            },
            '&.Mui-disabled': {
              color: 'rgba(255, 255, 255, 0.2)'
            }
          }}
        >
          Refresh
        </Button>
      )}
      
      {output.buttons?.includes("download") && output.type === "image" && isValueStringImage && (
        <Button
          variant="text"
          size="small"
          onClick={handleDownload}
          startIcon={<DownloadIcon size={16} />}
          sx={{
            minWidth: 0,
            fontSize: "0.75rem",
            textTransform: "uppercase",
            fontWeight: 500,
            letterSpacing: "0.03em",
            py: 0.3,
            px: 1.2,
            color: '#9e9e9e',
            '&:hover': {
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              color: '#bdbdbd'
            }
          }}
        >
          Download
        </Button>
      )}
    </Stack>
  );

  // Inside button for copy
  const renderCopyButtonInside =
    output.buttons?.includes("copy") && isValueNotNull && isButtonInside;

  const renderContent = () => {
    switch (output.type) {
      case "text":
        return (
          <Box
            sx={{ position: "relative" }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
          >
            <Typography
              variant="body1"
              component="div"
              sx={{
                whiteSpace: "pre-wrap",
                wordBreak: "break-all",
                fontFamily: output.monospace ? "monospace" : "inherit",
                color:
                  output.style === "error"
                    ? theme.palette.error.main
                    : "inherit",
                pr: renderCopyButtonInside ? 4 : 0,
                minHeight: renderCopyButtonInside ? "2rem" : "auto",
                backgroundColor: "#333333",
                p: 1.2,
                borderRadius: 1.5,
                border: "1px solid",
                borderColor: hovered
                  ? "rgba(30, 165, 76, 0.3)"
                  : "rgba(255,255,255,0.08)",
                transition: "all 0.2s ease-in-out",
                boxShadow: "0 1px 3px rgba(0,0,0,0.12)",
                fontSize: "0.875rem",
                letterSpacing: output.monospace ? "0.02em" : "inherit",
              }}
            >
              {String(displayValue)}
            </Typography>

            {/* Render copy button inside the text box */}
            {renderCopyButtonInside && (
              <Fade in={hovered || copied}>
                <Box
                  sx={{
                    position: "absolute",
                    right: 8,
                    top: 8,
                    zIndex: 2,
                  }}
                >
                  <Tooltip
                    title={copied ? "Copied!" : "Copy to clipboard"}
                    placement="left"
                    arrow
                  >
                    <IconButton
                      size="small"
                      onClick={handleCopy}
                      sx={{
                        backgroundColor: copied
                          ? alpha("#1ea54c", 0.25)
                          : alpha("#000", 0.5),
                        backdropFilter: "blur(4px)",
                        "&:hover": {
                          backgroundColor: copied
                            ? alpha("#1ea54c", 0.35)
                            : alpha("#000", 0.65),
                        },
                        p: 0.5,
                        border: "1px solid",
                        borderColor: copied
                          ? alpha("#1ea54c", 0.5)
                          : "rgba(255,255,255,0.15)",
                        transition: "all 0.2s ease",
                        transform: copied ? "scale(1.05)" : "scale(1)",
                      }}
                    >
                      {copied ? (
                        <DoneIcon
                          sx={{ fontSize: "0.9rem", color: "#1ea54c" }}
                        />
                      ) : (
                        <ContentCopyIcon
                          sx={{
                            fontSize: "0.9rem",
                            color: "rgba(255,255,255,0.9)",
                          }}
                        />
                      )}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Fade>
            )}
          </Box>
        );

      case "image":
        return (
          <Box
            sx={{
              position: "relative",
              width: "fit-content",
              maxWidth: "100%",
              borderRadius: 2,
              boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
              overflow: "hidden",
              mt: 1,
              transition: "all 0.2s ease",
              "&:hover": {
                boxShadow: "0 4px 12px rgba(0,0,0,0.25)",
              },
            }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
          >
            {displayValue ? (
              <>
                <Box
                  sx={{
                    maxWidth: output.maxWidth || 350,
                    maxHeight: output.maxHeight || 350,
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    backgroundColor: "#333333",
                    p: 0,
                    position: "relative",
                  }}
                >
                  <img
                    src={String(displayValue)}
                    alt={output.label || "Output Image"}
                    style={{
                      display: "block",
                      width: "100%",
                      height: "auto",
                      objectFit: "contain",
                    }}
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = "none";
                    }}
                  />
                </Box>

                {/* Render image buttons inside if specified */}
                {isButtonInside && output.buttons?.includes("download") && (
                  <Fade in={hovered}>
                    <Box
                      sx={{
                        position: "absolute",
                        right: 12,
                        bottom: 12,
                        zIndex: 2,
                      }}
                    >
                      <Tooltip title="Download image" placement="left" arrow>
                        <IconButton
                          size="small"
                          onClick={handleDownload}
                          sx={{
                            backgroundColor: alpha("#000", 0.6),
                            backdropFilter: "blur(4px)",
                            "&:hover": {
                              backgroundColor: alpha("#000", 0.8),
                            },
                            p: 0.8,
                            border: "1px solid",
                            borderColor: "rgba(255,255,255,0.15)",
                          }}
                        >
                          <DownloadIcon size={18} color="#fff" />
                        </IconButton>
                      </Tooltip>
                    </Box>
                  </Fade>
                )}
              </>
            ) : (
              <Box
                sx={{
                  width: "100%",
                  height: 150,
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  backgroundColor: "#333333",
                  borderRadius: 2,
                }}
              >
                <Typography
                  sx={{
                    p: 2,
                    fontStyle: "italic",
                    color: "text.secondary",
                    opacity: 0.6,
                  }}
                >
                  No Image
                </Typography>
              </Box>
            )}
          </Box>
        );

      case "json":
        return (
          <Box
            sx={{ position: "relative" }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
          >
            <Box
              component="pre"
              sx={{
                bgcolor: "#333333",
                color: theme.palette.mode === "dark" ? "#fff" : "#000",
                p: 1.5,
                borderRadius: 1.5,
                overflowX: "auto",
                fontSize: "0.875rem",
                maxHeight: "400px",
                wordBreak: "break-all",
                whiteSpace: "pre-wrap",
                mt: 1,
                pr: renderCopyButtonInside ? 4 : 1.5,
                border: "1px solid",
                borderColor: hovered
                  ? "rgba(30, 165, 76, 0.3)"
                  : "rgba(255,255,255,0.08)",
                transition: "all 0.2s ease-in-out",
                boxShadow: "0 1px 3px rgba(0,0,0,0.12)",
                "&::-webkit-scrollbar": {
                  width: "8px",
                  height: "8px",
                },
                "&::-webkit-scrollbar-thumb": {
                  backgroundColor: "rgba(255,255,255,0.1)",
                  borderRadius: "4px",
                  "&:hover": {
                    backgroundColor: "rgba(255,255,255,0.2)",
                  },
                },
                "&::-webkit-scrollbar-track": {
                  backgroundColor: "rgba(0,0,0,0.1)",
                },
              }}
            >
              {JSON.stringify(displayValue, null, 2)}
            </Box>

            {/* Render copy button inside */}
            {renderCopyButtonInside && (
              <Fade in={hovered || copied}>
                <Box
                  sx={{
                    position: "absolute",
                    right: 8,
                    top: 8, // Adjust for the mt: 1 on the parent
                    zIndex: 2,
                  }}
                >
                  <Tooltip
                    title={copied ? "Copied!" : "Copy to clipboard"}
                    placement="left"
                    arrow
                  >
                    <IconButton
                      size="small"
                      onClick={handleCopy}
                      sx={{
                        backgroundColor: copied
                          ? alpha("#1ea54c", 0.25)
                          : alpha("#000", 0.5),
                        backdropFilter: "blur(4px)",
                        "&:hover": {
                          backgroundColor: copied
                            ? alpha("#1ea54c", 0.35)
                            : alpha("#000", 0.65),
                        },
                        p: 0.5,
                        border: "1px solid",
                        borderColor: copied
                          ? alpha("#1ea54c", 0.5)
                          : "rgba(255,255,255,0.15)",
                        transition: "all 0.2s ease",
                        transform: copied ? "scale(1.05)" : "scale(1)",
                      }}
                    >
                      {copied ? (
                        <DoneIcon
                          sx={{ fontSize: "0.9rem", color: "#1ea54c" }}
                        />
                      ) : (
                        <ContentCopyIcon
                          sx={{
                            fontSize: "0.9rem",
                            color: "rgba(255,255,255,0.9)",
                          }}
                        />
                      )}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Fade>
            )}
          </Box>
        );

      case "boolean":
        return displayValue ? (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              backgroundColor: alpha("#1ea54c", 0.1),
              borderRadius: 4,
              px: 2,
              py: 0.5,
              width: "fit-content",
              border: "1px solid",
              borderColor: alpha("#1ea54c", 0.3),
            }}
          >
            <CheckCircleIcon color="success" size={16} />
            <Typography
              variant="body2"
              sx={{ color: "#1ea54c", fontWeight: 500 }}
            >
              True
            </Typography>
          </Box>
        ) : (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              backgroundColor: alpha("#d32f2f", 0.1),
              borderRadius: 4,
              px: 2,
              py: 0.5,
              width: "fit-content",
              border: "1px solid",
              borderColor: alpha("#d32f2f", 0.3),
            }}
          >
            <CancelIcon color="error" sx={{ fontSize: "1rem" }} />
            <Typography
              variant="body2"
              sx={{ color: "#d32f2f", fontWeight: 500 }}
            >
              False
            </Typography>
          </Box>
        );

      case "chips":
        return isArrayValue ? (
          <Box
            sx={{
              display: "flex",
              flexWrap: "wrap",
              gap: 0.8,
              mt: 1,
              backgroundColor: "#333333",
              p: 1.5,
              borderRadius: 1.5,
              border: "1px solid",
              borderColor: "rgba(255,255,255,0.08)",
            }}
          >
            {displayValue.map((item, i) => (
              <Chip
                key={i}
                label={String(item)}
                size="small"
                sx={{
                  backgroundColor: alpha("#1ea54c", 0.1),
                  color: alpha("#fff", 0.9),
                  border: "1px solid",
                  borderColor: alpha("#1ea54c", 0.3),
                  "& .MuiChip-label": {
                    px: 1,
                  },
                }}
              />
            ))}
          </Box>
        ) : (
          <Typography color="error" variant="caption">
            Invalid data
          </Typography>
        );

      case "list":
        return isArrayValue ? (
          <Box
            sx={{ position: "relative" }}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
          >
            <Box
              sx={{
                backgroundColor: "#333333",
                p: 1.5,
                borderRadius: 1.5,
                border: "1px solid",
                borderColor: hovered
                  ? "rgba(30, 165, 76, 0.3)"
                  : "rgba(255,255,255,0.08)",
                pr: renderCopyButtonInside ? 4 : 1.5,
                transition: "all 0.2s ease-in-out",
              }}
            >
              <List dense sx={{ py: 0, listStyle: "disc", pl: 2.5 }}>
                {displayValue.map((item, i) => (
                  <ListItem
                    key={i}
                    disableGutters
                    sx={{
                      py: 0.5,
                      display: "list-item",
                      borderBottom:
                        i < displayValue.length - 1 ? "1px solid" : "none",
                      borderColor: "rgba(255,255,255,0.04)",
                    }}
                  >
                    <ListItemText
                      primary={String(item)}
                      primaryTypographyProps={{
                        variant: "body2",
                        sx: {
                          color: alpha("#fff", 0.9),
                          fontWeight: 400,
                        },
                      }}
                    />
                  </ListItem>
                ))}
              </List>
            </Box>

            {/* Render copy button inside */}
            {renderCopyButtonInside && (
              <Fade in={hovered || copied}>
                <Box
                  sx={{
                    position: "absolute",
                    right: 8,
                    top: 8,
                    zIndex: 2,
                  }}
                >
                  <Tooltip
                    title={copied ? "Copied!" : "Copy to clipboard"}
                    placement="left"
                    arrow
                  >
                    <IconButton
                      size="small"
                      onClick={handleCopy}
                      sx={{
                        backgroundColor: copied
                          ? alpha("#1ea54c", 0.25)
                          : alpha("#000", 0.5),
                        backdropFilter: "blur(4px)",
                        "&:hover": {
                          backgroundColor: copied
                            ? alpha("#1ea54c", 0.35)
                            : alpha("#000", 0.65),
                        },
                        p: 0.5,
                        border: "1px solid",
                        borderColor: copied
                          ? alpha("#1ea54c", 0.5)
                          : "rgba(255,255,255,0.15)",
                        transition: "all 0.2s ease",
                        transform: copied ? "scale(1.05)" : "scale(1)",
                      }}
                    >
                      {copied ? (
                        <DoneIcon
                          sx={{ fontSize: "0.9rem", color: "#1ea54c" }}
                        />
                      ) : (
                        <ContentCopyIcon
                          sx={{
                            fontSize: "0.9rem",
                            color: "rgba(255,255,255,0.9)",
                          }}
                        />
                      )}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Fade>
            )}
          </Box>
        ) : (
          <Typography color="error" variant="caption">
            Invalid data
          </Typography>
        );

      case "progressBar":
        const getProgressBarColor = () => {
          if (score >= 90) return "#1ea54c";
          if (score >= 70) return "#3f8ed3";
          if (score >= 50) return "#f5b82e";
          return "#d32f2f";
        };

        return (
          <Box sx={{ mt: 1 }}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                mb: 1,
              }}
            >
              <Typography
                variant="body2"
                sx={{ fontWeight: 500, color: getProgressBarColor() }}
              >
                {`${Math.round(progressValue)}${suffix}`}
              </Typography>
              <Chip
                label={
                  score >= 90
                    ? "Excellent"
                    : score >= 70
                    ? "Good"
                    : score >= 50
                    ? "Average"
                    : "Poor"
                }
                size="small"
                sx={{
                  backgroundColor: alpha(getProgressBarColor(), 0.1),
                  color: getProgressBarColor(),
                  border: `1px solid ${alpha(getProgressBarColor(), 0.3)}`,
                  height: 22,
                  fontSize: "0.7rem",
                }}
              />
            </Box>
            <Box
              sx={{
                position: "relative",
                width: "100%",
                height: 8,
                backgroundColor: alpha(getProgressBarColor(), 0.15),
                borderRadius: 4,
                overflow: "hidden",
              }}
            >
              <Box
                sx={{
                  position: "absolute",
                  height: "100%",
                  width: `${progressValue}%`,
                  backgroundColor: getProgressBarColor(),
                  borderRadius: 4,
                  transition: "width 1s cubic-bezier(0.4, 0, 0.2, 1)",
                }}
              />
            </Box>
          </Box>
        );

      case "table":
        if (hasTableConfig) {
          if ((displayValue as any[]).length === 0) {
            return (
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  py: 3,
                  backgroundColor: "#333333",
                  borderRadius: 1.5,
                  border: "1px solid rgba(255,255,255,0.08)",
                  mt: 1,
                }}
              >
                <Typography
                  variant="body2"
                  sx={{
                    fontStyle: "italic",
                    color: "text.secondary",
                    opacity: 0.7,
                  }}
                >
                  No data available
                </Typography>
              </Box>
            );
          }
          return (
            <Box
              sx={{ position: "relative" }}
              onMouseEnter={() => setHovered(true)}
              onMouseLeave={() => setHovered(false)}
            >
              <TableContainer
                component={Paper}
                elevation={0}
                variant="outlined"
                sx={{
                  mt: 1,
                  backgroundColor: "#333333",
                  border: "1px solid",
                  borderColor: hovered
                    ? "rgba(30, 165, 76, 0.3)"
                    : "rgba(255,255,255,0.08)",
                  transition: "all 0.2s ease-in-out",
                  borderRadius: 1.5,
                  overflow: "hidden",
                  "&::-webkit-scrollbar": {
                    width: "8px",
                    height: "8px",
                  },
                  "&::-webkit-scrollbar-thumb": {
                    backgroundColor: "rgba(255,255,255,0.1)",
                    borderRadius: "4px",
                    "&:hover": {
                      backgroundColor: "rgba(255,255,255,0.2)",
                    },
                  },
                }}
              >
                <Table size="small">
                  <TableHead
                    sx={{
                      bgcolor: "#252525",
                    }}
                  >
                    <TableRow>
                      {output.columns!.map((col, cIndex) => (
                        <TableCell
                          key={`${output.id}-h-${cIndex}`}
                          sx={{
                            fontWeight: "600",
                            color: "#ccc",
                            py: 1.5,
                            fontSize: "0.8rem",
                            letterSpacing: "0.03em",
                            textTransform: "uppercase",
                            opacity: 0.9,
                          }}
                        >
                          {col.header}
                        </TableCell>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(displayValue as any[]).map((row: any, rIndex: number) => (
                      <TableRow
                        key={`${output.id}-r-${rIndex}`}
                        hover
                        sx={{
                          "&:last-child td, &:last-child th": { border: 0 },
                          "&:hover": {
                            backgroundColor: "rgba(30, 165, 76, 0.05)",
                          },
                        }}
                      >
                        {output.columns!.map((col, cIndex) => (
                          <TableCell
                            key={`${output.id}-c-${rIndex}-${cIndex}`}
                            sx={{
                              borderColor: "rgba(255,255,255,0.04)",
                              py: 1.2,
                              fontWeight: col.primary ? 500 : 400,
                              fontSize: "0.875rem",
                            }}
                          >
                            {String(
                              col.field
                                .split(".")
                                .reduce(
                                  (o, k) =>
                                    o && typeof o === "object"
                                      ? o[k]
                                      : undefined,
                                  row
                                ) ?? ""
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Render copy button inside */}
              {renderCopyButtonInside && (
                <Fade in={hovered || copied}>
                  <Box
                    sx={{
                      position: "absolute",
                      right: 8,
                      top: 8, // Adjust for the mt: 1 on the parent
                      zIndex: 2,
                    }}
                  >
                    <Tooltip
                      title={copied ? "Copied!" : "Copy to clipboard"}
                      placement="left"
                      arrow
                    >
                      <IconButton
                        size="small"
                        onClick={handleCopy}
                        sx={{
                          backgroundColor: copied
                            ? alpha("#1ea54c", 0.25)
                            : alpha("#000", 0.5),
                          backdropFilter: "blur(4px)",
                          "&:hover": {
                            backgroundColor: copied
                              ? alpha("#1ea54c", 0.35)
                              : alpha("#000", 0.65),
                          },
                          p: 0.5,
                          border: "1px solid",
                          borderColor: copied
                            ? alpha("#1ea54c", 0.5)
                            : "rgba(255,255,255,0.15)",
                          transition: "all 0.2s ease",
                          transform: copied ? "scale(1.05)" : "scale(1)",
                        }}
                      >
                        {copied ? (
                          <DoneIcon
                            sx={{ fontSize: "0.9rem", color: "#1ea54c" }}
                          />
                        ) : (
                          <ContentCopyIcon
                            sx={{
                              fontSize: "0.9rem",
                              color: "rgba(255,255,255,0.9)",
                            }}
                          />
                        )}
                      </IconButton>
                    </Tooltip>
                  </Box>
                </Fade>
              )}
            </Box>
          );
        } else {
          return (
            <Typography color="error" variant="caption">
              Invalid table data/config
            </Typography>
          );
        }

      default:
        return (
          <Typography color="error" variant="caption">
            Unsupported type: {output.type}
          </Typography>
        );
    }
  };

  return (
    <Box sx={{ mb: 2 }}>
      {output.label && (
        <Typography
          variant="body2"
          sx={{
            mb: 0.5,
            fontWeight: 500,
            color: theme.palette.mode === "dark" ? "#9e9e9e" : "text.secondary",
            display: "flex",
            alignItems: "center",
            fontSize: "0.875rem",
            letterSpacing: "0.01em",
          }}
        >
          {output.label}
        </Typography>
      )}
      {renderContent()}
      {buttonElements}
    </Box>
  );
};

export default RenderOutput;