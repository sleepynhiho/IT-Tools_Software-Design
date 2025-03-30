import { Switch, Slider, TextField, Box, Typography } from "@mui/material";

// Định nghĩa kiểu dữ liệu cho field
interface InputFieldConfig {
  name: string;
  label: string;
  type: "switch" | "slider" | "text";
  default: boolean | number | string;
  min?: number;
  max?: number;
}

// Định nghĩa kiểu dữ liệu cho props
interface InputFieldProps {
  field: InputFieldConfig;
  value: boolean | number | string;
  onChange: (name: string, value: boolean | number | string) => void;
}

const InputField: React.FC<InputFieldProps> = ({ field, value, onChange }) => {
  if (field.type === "switch") {
    return (
      <Box display="flex" justifyContent="space-between" mb={2}>
        <Typography>{field.label}</Typography>
        <Switch
          checked={value as boolean}
          onChange={(e) => onChange(field.name, e.target.checked)}
        />
      </Box>
    );
  }

  if (field.type === "slider") {
    return (
      <Box mb={2}>
        <Typography>
          {field.label} ({value})
        </Typography>
        <Slider
          value={value as number}
          onChange={(_, newValue) => onChange(field.name, newValue as number)}
          min={field.min}
          max={field.max}
          sx={{ color: "#36ad6a" }}
        />
      </Box>
    );
  }

  if (field.type === "text") {
    return (
      <TextField
        fullWidth
        label={field.label}
        value={value as string}
        onChange={(e) => onChange(field.name, e.target.value)}
        sx={{ bgcolor: "#333", color: "white", borderRadius: 1 }}
      />
    );
  }

  return null;
};

export default InputField;
