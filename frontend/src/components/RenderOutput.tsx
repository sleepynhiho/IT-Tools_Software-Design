/* eslint-disable @typescript-eslint/no-explicit-any */
import { JSX } from "react";
import {
  Box,
  Typography,
  TextField,
  Button,
  useTheme, // Import useTheme
} from "@mui/material";
import { SxProps, Theme } from "@mui/material/styles"; // Import SxProps
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Download as DownloadIcon } from "lucide-react";

// Import OutputField từ nguồn duy nhất
import type { OutputField } from "../data/pluginList"; // <-- Đảm bảo đường dẫn đúng

// --- Component Props Interface ---
interface RenderOutputProps {
  output: OutputField;
  value: any;
  onRefresh: () => void;
  disabled?: boolean;
}

const RenderOutput = ({
  output,
  value,
  onRefresh,
  disabled = false,
}: RenderOutputProps): JSX.Element | null => {
  const theme = useTheme();

  if (!output) {
    return null;
  }

  // --- Function to determine button style ---
  const getButtonStyle = (
    buttonType: "copy" | "refresh" | "download"
  ): SxProps<Theme> => {
    const placement = output.buttonPlacement?.[buttonType];

    // --- Style for OUTSIDE buttons (hoặc không xác định) ---
    if (placement === "outside" || placement === undefined) {
      // Style riêng cho nút outside nếu placement là 'outside'
      if (placement === "outside") {
        return {
          bgcolor: "#ffffff14",
          color: "#ffffffd1",
          "&:hover": { bgcolor: "#ffffff29" },
          // Giữ các style chung cho nút outside nếu cần
          // ví dụ: padding: '6px 12px', // Padding lớn hơn cho outside
        };
      }
      // Style mặc định nếu placement không xác định (coi như outside)
      else {
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
        // Thêm style chung cho nút outside/mặc định ở đây nếu cần
        return { ...baseSx, color: "white" /* Đảm bảo chữ/icon trắng */ };
      }
    }

    // --- Style for INSIDE buttons ---
    // Áp dụng cho tất cả các nút có placement là 'inside'
    return {
      bgcolor: "transparent", // Nền trong suốt
      color: "white", // Màu icon/chữ trắng
      minWidth: "auto", // Cho phép thu nhỏ
      padding: "4px", // Padding nhỏ chỉ đủ cho icon
      boxShadow: "none", // Bỏ shadow
      border: "1px solid rgba(255, 255, 255, 0.2)", // Thêm viền mờ để dễ thấy hơn (tùy chọn)
      backdropFilter: "blur(2px)", // Thêm hiệu ứng blur nhẹ phía sau (tùy chọn)
      "&:hover": {
        bgcolor: "rgba(255, 255, 255, 0.15)", // Hover sáng hơn chút
        borderColor: "rgba(255, 255, 255, 0.3)", // Viền rõ hơn khi hover
      },
    };
  };

  // --- Helper Function to Render a Single Button ---
  const renderButton = (buttonType: "copy" | "refresh" | "download") => {
    const buttonSx = getButtonStyle(buttonType);
    const placement = output.buttonPlacement?.[buttonType];
    const isInside = placement === "inside";

    const commonProps = {
      // variant không cần thiết nữa vì style tự định nghĩa hết
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
            onClick={() =>
              navigator.clipboard.writeText(
                value !== undefined && value !== null
                  ? String(value)
                  : String(output.default ?? "")
              )
            }
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
                  color: "white", // Đặt màu icon là trắng
                  border: "none", // Bỏ viền
                }}
              />
            )}
            {!isInside && "Copy"} {/* Chỉ hiển thị text cho outside */}
          </Button>
        );
      case "refresh":
        return (
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
        );
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
            onClick={() => {
              /* Download */
            }}
            title="Download"
          >
            <DownloadIcon size={isInside ? 14 : 16} />{" "}
            {/* Icon nhỏ hơn cho inside */}
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

  return (
    // Container chính với position relative
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
      <Typography variant="subtitle1" sx={{ mb: 1, fontWeight: "bold" }}>
        {output.label}
      </Typography>

      {/* Box chứa content và nút inside */}
      <Box sx={{ position: "relative" }}>
        {/* Phần tử nội dung output */}
        {(() => {
          const displayValue =
            value !== undefined && value !== null
              ? String(value)
              : String(output.default ?? "");
          switch (output.type) {
            // ... (các case render content giữ nguyên)
            case "typography":
              return (
                <Typography
                  sx={{
                    color: "white",
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                  }}
                  variant="body1"
                >
                  {" "}
                  {displayValue}{" "}
                </Typography>
              );
            case "image":
              return (
                <Box textAlign="center">
                  {" "}
                  {displayValue ? (
                    <img
                      src={displayValue}
                      alt={output.label || "Generated image"}
                      style={{
                        maxWidth: "100%",
                        maxHeight: "400px",
                        borderRadius: 8,
                        display: "block",
                        margin: "0 auto",
                      }}
                    />
                  ) : (
                    <Typography sx={{ color: "#888", fontStyle: "italic" }}>
                      No image generated yet
                    </Typography>
                  )}{" "}
                </Box>
              );
            case "text":
            default:
              return (
                <TextField
                  fullWidth
                  value={displayValue}
                  multiline
                  rows={output.rows || 4}
                  InputProps={{ readOnly: true, style: { color: "white" } }}
                  sx={{
                    bgcolor: "#333",
                    borderRadius: 1,
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
          }
        })()}

        {/* --- Container cho nút INSIDE --- */}
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
            {/* Render các nút inside */}
            {insideButtons.map((buttonType) => renderButton(buttonType))}
          </Box>
        )}
      </Box>

      {/* --- Container cho nút OUTSIDE --- */}
      {outsideButtons.length > 0 && (
        <Box
          mt={2}
          display="flex"
          flexWrap="wrap"
          gap={1.5}
          justifyContent="center"
        >
          {/* Render các nút outside */}
          {outsideButtons.map((buttonType) => renderButton(buttonType))}
        </Box>
      )}
    </Box>
  );
};

export default RenderOutput;
