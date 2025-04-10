/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState } from "react";
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
} from "@mui/material";
import { ChromePicker } from "react-color";

const RenderInput = (
  field: any,
  value: any,
  onChange: (value: any) => void
) => {
  switch (field.type) {
    case "button":
      return (
        <Button
          variant="contained"
          onClick={() => onChange(field.value)}
          sx={{ bgcolor: "#36ad6a" }}
        >
          {field.label}
        </Button>
      );
    case "text":
      return (
        <TextField
          fullWidth
          multiline={field.type === "textarea"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          sx={{ bgcolor: "#333", borderRadius: 1 }}
        />
      );
    case "color":
      return <ColorPickerField value={value} onChange={onChange} />;
    case "number":
      return (
        <TextField
          fullWidth
          type="number"
          variant="outlined"
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          sx={{
            bgcolor: "#333",
            borderRadius: 1,
            "& .MuiOutlinedInput-root": {
              "&.Mui-focused": {
                bgcolor: "#1ea54c1a",
                "& fieldset": {
                  borderColor: "#36ad6a",
                },
              },
              "&:hover fieldset": {
                borderColor: "#36ad6a",
              },
              "& input": {
                color: "#fff",
              },
            },
            "& .MuiInputLabel-root": {
              color: "#ccc",
            },
            "& .MuiInputLabel-root.Mui-focused": {
              color: "#36ad6a",
            },
          }}
        />
      );
    case "switch":
      return (
        <Switch
          checked={value}
          onChange={(e) => onChange(e.target.checked)}
          sx={{
            "& .MuiSwitch-switchBase.Mui-checked": { color: "#36ad6a" },
            "& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track": {
              bgcolor: "#36ad6a",
            },
          }}
        />
      );
    case "slider":
      return (
        <Slider
          min={field.min}
          max={field.max}
          value={value}
          onChange={(_, newVal) => onChange(newVal)}
          sx={{
            color: "#36ad6a",
            "& .MuiSlider-thumb": { bgcolor: "#36ad6a" },
            "& .MuiSlider-track": { bgcolor: "#36ad6a" },
            "& .MuiSlider-rail": { bgcolor: "#a1a1a1" },
          }}
        />
      );
    case "select":
      return (
        <Select
          fullWidth
          value={value}
          onChange={(e) => onChange(e.target.value)}
          sx={{ bgcolor: "#333", borderRadius: 1 }}
        >
          {field.options.map((option: string) => (
            <MenuItem key={option} value={option}>
              {option}
            </MenuItem>
          ))}
        </Select>
      );
    case "buttons":
      return (
        <ToggleButtonGroup
          exclusive
          value={value}
          onChange={(_, newVal) => newVal && onChange(newVal)}
        >
          {field.options.map((option: any) => (
            <ToggleButton key={option.value} value={option.value}>
              {option.name}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      );
    case "password":
      return (
        <TextField
          fullWidth
          type="password"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          sx={{ bgcolor: "#333", borderRadius: 1 }}
        />
      );
    default:
      return null;
  }
};

const ColorPickerField = ({
  value,
  onChange,
}: {
  value: string;
  onChange: (val: string) => void;
}) => {
  const [showPicker, setShowPicker] = useState(false);

  return (
    <Box position="relative" display="flex" flexDirection="column" gap={1}>
      {/* Thanh màu, click vào sẽ mở picker */}
      <Box
        sx={{
          width: "100%",
          height: 40,
          borderRadius: 1,
          bgcolor: value,
          border: "1px solid #aaa",
          cursor: "pointer",
        }}
        onClick={() => setShowPicker((prev) => !prev)}
      />

      {/* Chrome Picker - chỉ hiện khi showPicker === true */}
      {showPicker && (
        <Box position="absolute" zIndex={10} top={50}>
          <ChromePicker
            color={value}
            onChange={(color) => onChange(color.hex)}
            disableAlpha
            styles={{
              default: {
                picker: { background: "#222", borderRadius: "8px" },
              },
            }}
          />
        </Box>
      )}

      {/* TextField để chỉnh HEX trực tiếp */}
      <TextField
        fullWidth
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        sx={{ bgcolor: "#333", borderRadius: 1 }}
      />
    </Box>
  );
};

export default RenderInput;
