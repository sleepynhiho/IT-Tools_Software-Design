import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import {
  Box,
  Grid,
  Paper,
  Typography,
  TextField,
  Button,
  Switch,
  Slider,
  Select,
  MenuItem,
  ToggleButton,
  ToggleButtonGroup,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import { mockMetadata } from "../data/mockMetadata";
import CommonLayout from "../layouts/CommonLayout";

const ToolRenderer = () => {
  const { id } = useParams();
  const [metadata, setMetadata] = useState<any | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [outputs, setOutputs] = useState<Record<string, string>>({});

  useEffect(() => {
    const tool = mockMetadata.find((tool) => tool.id === id);
    if (tool) {
      setMetadata(tool);
      setFormData(
        Object.fromEntries(
          tool.uiConfig.sections.flatMap((section) =>
            section.fields.map((f) => [f.name, f.default])
          )
        )
      );
      setOutputs(
        Object.fromEntries(
          (tool.uiConfig.outputs ?? []).map((output) => [output.name, ""])
        )
      );
    }
  }, [id]);

  const handleInputChange = (name: string, value: any) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleExecute = async () => {
    if (!metadata?.processFunction) return;

    const result = await metadata.processFunction(formData);
    if (result.error) {
      setOutputs({ error: `Error: ${result.error}` });
    } else {
      setOutputs(result);
    }
  };

  if (!metadata) return <Typography>Loading...</Typography>;

  return (
    <CommonLayout title={metadata.name} description={metadata.description}>
      <Grid container spacing={3}>
        {metadata.uiConfig.sections.map((section, index) => (
          <Grid item xs={12} sm={12} md={12} key={index}>
            <Paper
              sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
            >
              <Typography variant="h6" sx={{ mb: 2 }}>
                {section.header}
              </Typography>

              {section.fields.map((field) => (
                <Box
                  key={field.name}
                  sx={{
                    mb: 2,
                    display: "flex",
                    flexDirection: "row",
                    width: "100%",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Typography sx={{ mb: 1, p: 1 }}>{field.label}</Typography>
                  {renderField(field, formData[field.name], (value) =>
                    handleInputChange(field.name, value)
                  )}
                </Box>
              ))}

              {/* Outputs trong section */}
              {section.outputs?.map((output) => (
                <Box key={output.name} sx={{ mt: 3 }}>
                  <Typography sx={{ mb: 1 }}>{output.title}</Typography>
                  {renderOutput(
                    output,
                    outputs[output.name] || "",
                    handleExecute
                  )}
                </Box>
              ))}
            </Paper>
          </Grid>
        ))}
        {/* 
        <Grid item xs={12}>
          <Paper
            sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
          >
            <Box display="flex" justifyContent="space-between">
              <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={handleExecute}
                sx={{ bgcolor: "#f57c00" }}
              >
                Execute
              </Button>
            </Box>
          </Paper>
        </Grid> */}

        {/* Outputs ngoài section */}
        {(metadata.uiConfig.outputs ?? []).map((output) => (
          <Grid item xs={12} key={output.name}>
            <Paper
              sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
            >
              <Typography sx={{ mb: 2 }}>{output.title}</Typography>
              {renderOutput(output, outputs[output.name] || "", handleExecute)}
            </Paper>
          </Grid>
        ))}
      </Grid>
    </CommonLayout>
  );
};

/** Render các input field */
const renderField = (
  field: any,
  value: any,
  onChange: (value: any) => void
) => {
  switch (field.type) {
    case "text":
    case "number":
      return (
        <TextField
          fullWidth
          type={field.type}
          value={value}
          onChange={(e) =>
            onChange(
              field.type === "number" ? Number(e.target.value) : e.target.value
            )
          }
          sx={{ bgcolor: "#333", borderRadius: 1 }}
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

/** Render output fields */
const renderOutput = (output: any, value: string, onRefresh: () => void) => {
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
      {content}
      <Box mt={2} display="flex" gap={2}>
        {output.buttons?.includes("copy") && (
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
      </Box>
    </Paper>
  );
};

export default ToolRenderer;
