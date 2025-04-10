import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import {
  Box,
  Grid,
  Paper,
  Typography,
} from "@mui/material";
import { mockMetadata } from "../data/mockMetadata";
import CommonLayout from "../layouts/CommonLayout";
import RenderOutput from "./RenderOutput";
import RenderInput from "./RenderInput";

const ToolRenderer = () => {
  const { id } = useParams();
  const [metadata, setMetadata] = useState<any | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [outputs, setOutputs] = useState<Record<string, string>>({});

  useEffect(() => {
    const tool = mockMetadata.find((tool) => tool.id === id);
    if (tool) {
      setMetadata(tool);

      const initialFormData = Object.fromEntries(
        (tool.uiConfig.inputs ?? []).flatMap((section) =>
          section.fields.map((f) => [f.name, f.default])
        )
      );

      const initialOutputs = Object.fromEntries(
        (tool.uiConfig.outputs ?? []).map((output) => [output.name, ""])
      );

      setFormData(initialFormData);
      setOutputs(initialOutputs);
    }
  }, [id]);

  // Gọi processFunction mỗi khi formData thay đổi
  useEffect(() => {
    const shouldProcess =
      metadata?.processFunction &&
      Object.values(formData).some((val) => val !== undefined && val !== "");

    if (shouldProcess) {
      metadata.processFunction(formData).then((result: any) => {
        if (result.error) {
          setOutputs({ error: `Error: ${result.error}` });
        } else {
          setOutputs(result);
        }
      });
    }
  }, [formData, metadata]);

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
    <CommonLayout
      title={metadata.name}
      description={metadata.description}
      toolId={id || ""}
      icon={metadata.icon}
    >
      <Grid container spacing={3}>
        {metadata.uiConfig.inputs.map((section, index) => (
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
                  {RenderInput(field, formData[field.name], (value) =>
                    handleInputChange(field.name, value)
                  )}
                </Box>
              ))}

              {/* Outputs trong section */}
              {section.outputs?.map((output) => (
                <Box key={output.name} sx={{ mt: 3 }}>
                  <Typography sx={{ mb: 1 }}>{output.title}</Typography>
                  {RenderOutput(
                    output,
                    outputs[output.name] || "",
                    handleExecute
                  )}
                </Box>
              ))}
            </Paper>
          </Grid>
        ))}

        {/* Outputs ngoài section */}
        {(metadata.uiConfig.outputs ?? []).map((output) => (
          <Grid item xs={12} key={output.name}>
            <Paper
              sx={{ p: 3, bgcolor: "#212121", color: "white", borderRadius: 2 }}
            >
              <Typography sx={{ mb: 2 }}>{output.title}</Typography>
              {RenderOutput(output, outputs[output.name] || "", handleExecute)}
            </Paper>
          </Grid>
        ))}
      </Grid>
    </CommonLayout>
  );
};

export default ToolRenderer;
