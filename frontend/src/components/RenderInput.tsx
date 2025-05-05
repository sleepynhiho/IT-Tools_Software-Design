/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useState } from "react"; // Import React
import {
  Box,
  TextField,
  Button,
  Switch,
  Slider,
  Select,
  MenuItem,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  FormHelperText,
  useTheme,
  IconButton,
  InputAdornment,
} from "@mui/material";
import { ChromePicker } from "react-color";
import { DateTimePicker } from "@mui/x-date-pickers/DateTimePicker";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import AddIcon from "@mui/icons-material/Add";
import RemoveIcon from "@mui/icons-material/Remove";
import dayjs from "dayjs";

import type { InputField } from "../data/pluginList";
interface RenderInputProps {
  field: InputField;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

// Define primary styles that will be applied to form elements
const PRIMARY_COLOR = "#1ea54c";
const PRIMARY_COLOR_HOVER = "#1ca348";
const PRIMARY_BACKGROUND_FOCUS = "rgba(30, 165, 76, 0.1)"; // #1ea54c with 10% opacity

const ColorPickerField = ({
  value,
  onChange,
  disabled = false,
}: {
  value: string;
  onChange: (val: string) => void;
  disabled?: boolean;
}) => {
  const [showPicker, setShowPicker] = useState(false);
  const handleClosePicker = () => setShowPicker(false);
  const handleSwatchClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!disabled) setShowPicker((prev) => !prev);
  };
  return (
    <Box
      position="relative"
      display="flex"
      flexDirection="column"
      gap={1}
      sx={{ width: "100%" }}
    >
      <Box
        sx={{
          width: "100%",
          height: 40,
          borderRadius: 1,
          bgcolor: value,
          border: "1px solid #aaa",
          cursor: disabled ? "not-allowed" : "pointer",
          opacity: disabled ? 0.5 : 1,
          position: "relative",
          "&::after": disabled
            ? {
                content: '""',
                position: "absolute",
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: "rgba(0, 0, 0, 0.1)",
                borderRadius: "inherit",
              }
            : {},
        }}
        onClick={handleSwatchClick}
      />
      {showPicker && !disabled && (
        <>
          <Box
            sx={{
              position: "fixed",
              top: 0,
              right: 0,
              bottom: 0,
              left: 0,
              zIndex: 9,
            }}
            onClick={handleClosePicker}
          />
          <Box position="absolute" zIndex={10} top={50} left={0}>
            <ChromePicker
              color={value}
              onChange={(color) => onChange(color.hex)}
              disableAlpha
            />
          </Box>
        </>
      )}
      <TextField
        fullWidth
        type="text"
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value)}
        sx={{
          bgcolor: "#333",
          borderRadius: 1,
          "& .MuiInputBase-input": { color: "white", fontFamily: "monospace" },
          "& .MuiOutlinedInput-root": {
            "& fieldset": { borderColor: "#555" },
            "&:hover fieldset": { borderColor: PRIMARY_COLOR + " !important" },
            "&.Mui-focused fieldset": { borderColor: PRIMARY_COLOR + " !important" },
            "&.Mui-focused": { backgroundColor: PRIMARY_BACKGROUND_FOCUS },
          },
        }}
        InputLabelProps={{ shrink: true }}
        disabled={disabled}
      />
    </Box>
  );
};

const RenderInput: React.FC<RenderInputProps> = ({
  field,
  value,
  onChange,
  disabled = false,
}) => {
  const theme = useTheme();
  const [showPassword, setShowPassword] = useState(false);
  
  // Calculate width based on metadata or use defaults
  const getWidthFromMetadata = () => {
    // If field has explicit width and height, use those
    if (field.width && field.height) {
      return {
        width: field.width,
        height: field.height
      };
    }
    
    // Default widths for different field types
    const halfWidthCalc = `calc(50% - ${theme.spacing(1.25)})`;
    const fullWidth = "100%";
    const autoWidth = "auto";
    
    // Return different defaults based on field type
    switch (field.type) {
      case "switch":
        return { width: { xs: fullWidth, sm: halfWidthCalc } };
      case "slider":
        return { width: { xs: fullWidth, sm: "65%" }, minWidth: "250px" };
      case "textarea":
      case "texarea":
      case "text":
      case "password":
      case "number":
      case "select":
      case "color":
      case "datetime":
        return { width: { xs: fullWidth, sm: halfWidthCalc } };
      case "button":
        return { width: autoWidth };
      case "buttons":
        return { width: { xs: fullWidth, sm: autoWidth } };
      default:
        return { width: fullWidth };
    }
  };
  
  const renderWithWrapper = (inputElement: React.ReactNode) => {
    // Get appropriate width styling
    const widthStyle = getWidthFromMetadata();
    
    return (
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          gap: 0.5,
          ...widthStyle,
        }}
      >
        {field.type !== "switch" && field.label && (
          <Typography variant="body1" sx={{ fontWeight: 500, color: "white" }}>
            {field.label}
          </Typography>
        )}
        {inputElement}
        {field.helperText && (
          <FormHelperText sx={{ color: "#ccc", mt: 0.5 }}>
            {field.helperText}
          </FormHelperText>
        )}
      </Box>
    );
  };

  const handleDateTimeChange = (newValue: dayjs.Dayjs | null) => {
    onChange(newValue ? newValue.toISOString() : null);
  };
  const currentDateTimeValue = value ? dayjs(value) : null;

  // Number input handlers
  const handleNumberIncrement = () => {
    const currentValue = Number(value ?? field.min ?? 0);
    const step = field.step ?? 1;
    let newValue = currentValue + step;
    if (field.max !== undefined && newValue > field.max) {
      newValue = field.max;
    }
    onChange(newValue);
  };

  const handleNumberDecrement = () => {
    const currentValue = Number(value ?? field.min ?? 0);
    const step = field.step ?? 1;
    let newValue = currentValue - step;
    if (field.min !== undefined && newValue < field.min) {
      newValue = field.min;
    }
    onChange(newValue);
  };

  // Define common text field styles with !important flags for hover and focus
  const textFieldSx = {
    bgcolor: "#333",
    borderRadius: 1,
    "& .MuiInputBase-input": { 
      color: "white"
    },
    "& .MuiInputLabel-root": { 
      color: "#ccc" 
    },
    "& .MuiOutlinedInput-root": {
      "& fieldset": { 
        borderColor: "#555" 
      },
      "&:hover fieldset": { 
        borderColor: `${PRIMARY_COLOR} !important` 
      },
      "&.Mui-focused fieldset": { 
        borderColor: `${PRIMARY_COLOR} !important` 
      },
      "&.Mui-focused": { 
        backgroundColor: PRIMARY_BACKGROUND_FOCUS 
      }
    },
    "& .MuiInputBase-input::placeholder": { 
      color: "#888", 
      opacity: 1 
    }
  };

  // --- Switch cho các loại Input ---
  switch (field.type) {
    case "button":
      return renderWithWrapper(
        <Button
          variant="contained"
          onClick={() => onChange(field.value)}
          sx={{ 
            bgcolor: PRIMARY_COLOR, 
            "&:hover": { bgcolor: PRIMARY_COLOR_HOVER } 
          }}
          disabled={disabled}
        >
          {field.label}
        </Button>
      );

    case "switch": {
      const trackHeight = 22;
      const thumbSize = 18;
      const desiredSwitchWidth = 40;
      const switchBasePadding = (trackHeight - thumbSize) / 2;
      const switchHeight = trackHeight + 2 * switchBasePadding;
      const finalCheckedTransform = `calc(${desiredSwitchWidth}px - ${thumbSize}px - ${
        2 * switchBasePadding
      }px)`;
      return renderWithWrapper(
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <Typography
            variant="body1"
            sx={{ fontWeight: 500, color: "white", mr: 1 }}
          >
            {field.label}
          </Typography>
          <Switch
            checked={!!value}
            onChange={(e) => onChange(e.target.checked)}
            sx={{
              width: `${desiredSwitchWidth}px`,
              height: `${switchHeight}px`,
              padding: 0,
              display: "inline-flex",
              "& .MuiSwitch-track": {
                height: `${trackHeight}px`,
                borderRadius: `${trackHeight / 2}px`,
                opacity: 0.7,
                backgroundColor: "#aaa",
                boxSizing: "border-box",
                width: "100%",
                transition: theme.transitions.create(["background-color"], {
                  duration: theme.transitions.duration.shortest,
                }),
              },
              "& .MuiSwitch-thumb": {
                width: `${thumbSize}px`,
                height: `${thumbSize}px`,
                color: "white",
                boxShadow: "none",
                boxSizing: "border-box",
                transition: theme.transitions.create(["transform"], {
                  duration: theme.transitions.duration.shortest,
                }),
              },
              "& .MuiSwitch-switchBase": {
                padding: `${switchBasePadding}px`,
                boxSizing: "border-box",
                transform: "translateX(0px)",
                "&.Mui-checked": {
                  transform: `translateX(${finalCheckedTransform})`,
                  color: "#fff",
                  "& + .MuiSwitch-track": {
                    backgroundColor: PRIMARY_COLOR,
                    opacity: 1,
                  },
                },
                "&.Mui-disabled + .MuiSwitch-track": { opacity: 0.3 },
                "&.Mui-disabled .MuiSwitch-thumb": {
                  opacity: 0.5,
                  color: "#ccc",
                },
              },
            }}
            disabled={disabled}
          />
        </Box>
      );
    }

    case "slider":
      return renderWithWrapper(
        <Box sx={{ width: "100%", px: 1 }}>
          <Slider
            min={field.min ?? 0}
            max={field.max ?? 100}
            step={field.step ?? 1}
            value={
              typeof value === "number"
                ? value
                : field.default ?? field.min ?? 0
            }
            onChange={(_, newVal) => onChange(newVal as number)}
            valueLabelDisplay="auto"
            sx={{
              width: "100%",
              color: PRIMARY_COLOR,
              "& .MuiSlider-thumb": {
                backgroundColor: "white",
                "&:hover, &.Mui-focusVisible": {
                  boxShadow: `0px 0px 0px 8px ${PRIMARY_BACKGROUND_FOCUS}`,
                },
                "&.Mui-active": {
                  boxShadow: `0px 0px 0px 14px ${PRIMARY_BACKGROUND_FOCUS}`,
                },
              },
              "& .MuiSlider-rail": { color: "#aaa", opacity: 0.7 },
              "& .MuiSlider-mark": {
                backgroundColor: "#ccc",
                height: 8,
                width: 1,
                marginTop: -3,
              },
              "& .MuiSlider-markActive": {
                backgroundColor: "currentColor",
                opacity: 1,
              },
            }}
            disabled={disabled}
            marks={!!field.step && (field.max - field.min) / field.step <= 50}
          />
        </Box>
      );
      
    case "texarea":
    case "textarea":
    case "text":
      return renderWithWrapper(
        <TextField
          fullWidth
          multiline={field.type === "textarea" || field.type === "texarea"}
          value={value ?? ""}
          onChange={(e) => onChange(e.target.value)}
          sx={textFieldSx}
          disabled={disabled}
          rows={field.rows}
          placeholder={field.placeholder}
          inputProps={{
            style: {
              height: field.height ? `${field.height - 16}px` : undefined
            }
          }}
        />
      );

    case "password":
      return renderWithWrapper(
        <TextField
          fullWidth
          type={showPassword ? "text" : "password"}
          value={value ?? ""}
          onChange={(e) => onChange(e.target.value)}
          placeholder={field.placeholder}
          disabled={disabled}
          sx={textFieldSx}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  aria-label="toggle password visibility"
                  onClick={() => setShowPassword((show) => !show)}
                  onMouseDown={(event) => event.preventDefault()} 
                  edge="end"
                  sx={{ color: "grey" }}
                  disabled={disabled}
                >
                  {showPassword ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment>
            ),
            style: {
              height: field.height ? `${field.height}px` : undefined
            }
          }}
        />
      );

    case "number":
      return renderWithWrapper(
        <TextField
          fullWidth
          type="number"
          value={value ?? ""}
          onChange={(e) => {
            const numValue =
              e.target.value === "" ? "" : Number(e.target.value);
            onChange(numValue);
          }}
          placeholder={field.placeholder}
          disabled={disabled}
          sx={textFieldSx}
          InputProps={{
            inputProps: { 
              min: field.min, 
              max: field.max, 
              step: field.step,
              style: {
                height: field.height ? `${field.height - 16}px` : undefined
              }
            },
            startAdornment: field.buttons?.includes("minus") ? (
              <InputAdornment position="start">
                <IconButton
                  onClick={handleNumberDecrement}
                  disabled={
                    disabled ||
                    (field.min !== undefined &&
                      Number(value ?? field.min) <= field.min)
                  }
                  size="small"
                  sx={{ color: "grey" }}
                >
                  <RemoveIcon fontSize="small" />
                </IconButton>
              </InputAdornment>
            ) : undefined,
            endAdornment: field.buttons?.includes("plus") ? (
              <InputAdornment position="end">
                <IconButton
                  onClick={handleNumberIncrement}
                  disabled={
                    disabled ||
                    (field.max !== undefined &&
                      Number(value ?? field.min) >= field.max)
                  }
                  size="small"
                  sx={{ color: "grey" }}
                >
                  <AddIcon fontSize="small" />
                </IconButton>
              </InputAdornment>
            ) : undefined,
          }}
        />
      );

    case "select":
      return renderWithWrapper(
        <Select
          fullWidth
          value={value ?? field.options?.[0]?.value ?? ""}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          sx={{
            bgcolor: "#333",
            borderRadius: 1,
            color: "white",
            "& .MuiSelect-icon": { color: "#ccc" },
            "& .MuiOutlinedInput-notchedOutline": { 
              borderColor: "#555" 
            },
            "&:hover .MuiOutlinedInput-notchedOutline": { 
              borderColor: `${PRIMARY_COLOR} !important` 
            },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": { 
              borderColor: `${PRIMARY_COLOR} !important` 
            },
            "&.Mui-focused": { 
              backgroundColor: PRIMARY_BACKGROUND_FOCUS 
            },
            ...(field.height ? { height: `${field.height}px` } : {})
          }}
          MenuProps={{
            PaperProps: {
              sx: {
                bgcolor: "#444",
                color: "white",
                "& .MuiMenuItem-root": {
                  "&:hover": { bgcolor: "#555" },
                  "&.Mui-selected": {
                    bgcolor: PRIMARY_BACKGROUND_FOCUS,
                    "&:hover": { bgcolor: "rgba(30, 165, 76, 0.3)" },
                  },
                },
              },
            },
          }}
        >
          {(field.options || []).map((option: any) => (
            <MenuItem key={option.value} value={option.value}>
              {option.label}
            </MenuItem>
          ))}
        </Select>
      );

    case "color": 
      return renderWithWrapper(
        <ColorPickerField
          value={value}
          onChange={onChange}
          disabled={disabled}
        />
      );

    case "datetime":
      return renderWithWrapper(
        <DateTimePicker
          value={currentDateTimeValue}
          onChange={handleDateTimeChange}
          disabled={disabled}
          sx={{
            width: "100%",
            bgcolor: "#333",
            borderRadius: 1,
            "& .MuiInputBase-input": { color: "white" },
            "& .MuiOutlinedInput-root": {
              "& fieldset": { borderColor: "#555" },
              "&:hover fieldset": { borderColor: `${PRIMARY_COLOR} !important` },
              "&.Mui-focused fieldset": { borderColor: `${PRIMARY_COLOR} !important` },
              "&.Mui-focused": { backgroundColor: PRIMARY_BACKGROUND_FOCUS },
              ...(field.height ? { height: `${field.height}px` } : {})
            },
            "& .MuiSvgIcon-root": { color: "#ccc" },
          }}
        />
      );

    case "buttons": 
      return renderWithWrapper(
        <ToggleButtonGroup
          value={value}
          exclusive
          onChange={(_, newValue) => {
            if (newValue !== null) {
              onChange(newValue);
            }
          }}
          aria-label={field.label || "options"}
          disabled={disabled}
          sx={{
            display: "inline-flex",
            flexWrap: "wrap",
            ...(field.width ? { width: field.width } : {}),
            "& .MuiToggleButtonGroup-grouped": {
              color: "#ccc",
              borderColor: "#555",
              "&:not(:first-of-type)": {
                borderLeftColor: "#555",
                marginLeft: "-1px",
              },
              "&.Mui-selected": {
                color: "white",
                backgroundColor: PRIMARY_BACKGROUND_FOCUS,
                borderColor: PRIMARY_COLOR,
                "&:hover": { backgroundColor: "rgba(30, 165, 76, 0.3)" },
              },
              "&:hover": { backgroundColor: "#444" },
              "&.Mui-disabled": { color: "#777", borderColor: "#444" },
              ...(field.height ? { height: `${field.height}px` } : {})
            },
          }}
        >
          {(field.options || []).map((option: any) => (
            <ToggleButton
              key={option.value}
              value={option.value}
              aria-label={option.label}
            >
              {" "}
              {option.label}{" "}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      );

    default:
      console.warn("Unknown input field type:", field.type, field);
      return renderWithWrapper(
        <Typography sx={{ color: "red", fontStyle: "italic" }}>
          Unknown input type: {field.type}
        </Typography>
      );
  }
};

export default RenderInput;