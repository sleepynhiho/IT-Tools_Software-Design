import { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import debounce from "lodash.debounce";
import {
  Box,
  Grid,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  FormHelperText,
} from "@mui/material";
import CommonLayout from "../layouts/CommonLayout";
import RenderOutput from "./RenderOutput"; // Assuming RenderOutput is in the same folder or accessible
import RenderInput from "./RenderInput"; // Assuming RenderInput exists and works as expected
import {
  InputField,
  OutputField,
  PluginMetadata,
  Section,
  useAllPluginMetadata,
} from "../data/pluginList";
import { groupBy } from "lodash"; // dùng lodash để group theo type
import React from "react";

const API_BASE_URL = "http://192.168.192.2:8081"; // Your API base URL
// --- End Configuration ---

const ToolRenderer = () => {
  const { id } = useParams<{ id: string }>();
  const {
    metadataList: allPluginMetadata,
    loading: loadingMetadataList,
    error: metadataListError,
  } = useAllPluginMetadata();

  const [metadata, setMetadata] = useState<PluginMetadata | null>(null);
  const [loadingMetadata, setLoadingMetadata] = useState<boolean>(true);
  const [metadataError, setMetadataError] = useState<string | null>(null);

  const [formData, setFormData] = useState<Record<string, any>>({});
  const [outputs, setOutputs] = useState<Record<string, any>>({});
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [processError, setProcessError] = useState<string | null>(null);

  // --- API Call Logic ---
  const processRequest = useCallback(
    async (currentFormData: Record<string, any>) => {
      if (!metadata?.id) {
        console.log("Process skipped: No metadata loaded.");
        return;
      }

      setIsProcessing(true);
      setProcessError(null);

      const processURL = `${API_BASE_URL}/api/debug/${metadata.id}/process`;
      console.log(
        `Processing request to: ${processURL} with data:`,
        currentFormData
      );

      try {
        const response = await fetch(processURL, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify(currentFormData),
        });

        const result = await response.json();
        console.log("Backend process response:", result);

        if (!response.ok || result.success === false) {
          const errorMsg =
            result?.error ||
            `Request failed with status ${response.status} ${
              response.statusText || ""
            }`;
          throw new Error(errorMsg);
        }

        // IMPORTANT: Assumes API returns an object like: { outputId1: value1, outputId2: value2, ... }
        // Adjust if the API response structure is different (e.g., result.data)
        setOutputs(result);
      } catch (error: any) {
        console.error("Process request failed:", error);
        setProcessError(
          error.message || "An unknown error occurred during processing."
        );
      } finally {
        setIsProcessing(false);
      }
    },
    [metadata]
  ); // Dependency: metadata (contains id)

  // Debounced version for automatic processing on input change
  const debouncedProcessRequest = useCallback(debounce(processRequest, 400), [
    processRequest,
  ]); // Debounce depends on the latest processRequest

  // 1. Effect for loading metadata and initializing form
  useEffect(() => {
    if (loadingMetadataList || !Array.isArray(allPluginMetadata)) {
      setLoadingMetadata(!loadingMetadataList);
      setMetadataError(metadataListError);
      return;
    }

    console.log("Finding metadata for id:", id);
    const toolMetadata = allPluginMetadata.find(
      (tool: PluginMetadata) => tool.id === id
    ); // Use PluginMetadata type

    if (toolMetadata) {
      console.log("Metadata found:", toolMetadata);
      setMetadata(toolMetadata);
      setMetadataError(null);

      const initialFormData: Record<string, any> = {};
      const initialOutputs: Record<string, any> = {};

      toolMetadata.sections?.forEach((section: Section) => {
        section.inputs?.forEach((input: InputField) => {
          if (input.default !== undefined) {
            initialFormData[input.id] = input.default;
          } else {
            switch (input.type) {
              case "switch":
                initialFormData[input.id] = false;
                break;
              case "slider":
                initialFormData[input.id] = input.min ?? 0;
                break;
              case "number":
                initialFormData[input.id] = input.min ?? 0;
                break;
              default:
                initialFormData[input.id] = "";
                break;
            }
          }
        });
        section.outputs?.forEach((output: OutputField) => {
          initialOutputs[output.id] = output.default ?? null; // Initialize with default or null
        });
      });

      console.log("Initializing formData:", initialFormData);
      setFormData(initialFormData);
      setOutputs(initialOutputs);
      setProcessError(null);
      setIsProcessing(false);

      // Initial process request for specific tools if needed
      if (
        toolMetadata.id === "TokenGenerator" &&
        Object.keys(initialFormData).length > 0
      ) {
        console.log("Triggering initial process request for TokenGenerator");
        // Use a copy of initialFormData to avoid potential stale closures if processRequest runs async immediately
        processRequest({ ...initialFormData });
      }

      setLoadingMetadata(false);
    } else {
      console.error(`Metadata for id "${id}" not found.`);
      setMetadata(null);
      setMetadataError(`Tool with ID "${id}" not found.`);
      setFormData({});
      setOutputs({});
      setLoadingMetadata(false);
    }

    // processRequest is included as it's called within the effect.
  }, [
    id,
    allPluginMetadata,
    loadingMetadataList,
    metadataListError,
    processRequest,
  ]);

  // 2. Input change handler
  const handleInputChange = useCallback(
    (inputId: string, value: any) => {
      console.log(`Input Changed: ID=${inputId}, Value=${value}`);
      setFormData((prev) => {
        const newFormData = { ...prev, [inputId]: value };

        // Decide if automatic processing should trigger
        // Example: Only trigger for TokenGenerator on specific inputs
        const shouldTriggerAutomatically =
          metadata?.id === "TokenGenerator" &&
          [
            "useUppercase",
            "useLowercase",
            "useNumbers",
            "useSpecial",
            "length",
          ].includes(inputId);

        // Alternative: Trigger on any input change
        // const shouldTriggerAutomatically = true;

        if (shouldTriggerAutomatically) {
          console.log(`Debouncing process request due to change in ${inputId}`);
          // Pass the *new* form data to the debounced function
          debouncedProcessRequest(newFormData);
        }

        return newFormData;
      });

      // Clear processing error on user input
      if (processError) {
        setProcessError(null);
      }
    },
    [metadata, debouncedProcessRequest, processError]
  ); // Dependencies

  // 3. Manual execution handler (e.g., for Refresh button)
  const handleExecute = useCallback(() => {
    console.log("Manual Execute/Refresh Triggered");
    if (metadata && Object.keys(formData).length > 0) {
      // Use current formData from state
      processRequest(formData);
    } else {
      console.log("Manual Execute skipped: No metadata or form data.");
    }
  }, [formData, metadata, processRequest]); // Dependencies

  // --- Render Logic ---

  if (loadingMetadataList) {
    return (
      <CommonLayout
        title="Loading Tools..."
        description=""
        toolId={id || ""}
        icon=""
      >
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress />
        </Box>
      </CommonLayout>
    );
  }

  if (metadataListError) {
    return (
      <CommonLayout title="Error" description="" toolId={id || ""} icon="">
        <Alert severity="error" sx={{ m: 2 }}>
          Failed to load tool list: {metadataListError}
        </Alert>
      </CommonLayout>
    );
  }

  if (loadingMetadata) {
    return (
      <CommonLayout
        title="Loading Tool..."
        description=""
        toolId={id || ""}
        icon=""
      >
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress />
        </Box>
      </CommonLayout>
    );
  }

  if (metadataError || !metadata) {
    return (
      <CommonLayout title="Error" description="" toolId={id || ""} icon="">
        <Alert severity="error" sx={{ m: 2 }}>
          {metadataError || "Tool not found."}
        </Alert>
      </CommonLayout>
    );
  }

  // --- Main UI Render ---
  return (
    <CommonLayout
      title={metadata.name}
      description={metadata.description}
      toolId={id || ""}
      icon={metadata.icon}
    >
      {processError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setProcessError(null)}
        >
          Processing Error: {processError}
        </Alert>
      )}

      <Grid container spacing={3}>
        {metadata?.sections?.map((section: Section, index: number) => (
          <Grid item xs={12} key={`${section.id}-${index}`}>
            <Paper
              sx={{
                p: { xs: 2, sm: 3 },
                bgcolor: "#212121",
                color: "white",
                borderRadius: 2,
                mb: 3,
              }}
              elevation={3}
            >
              {section.label && (
                <Typography
                  variant="h6"
                  sx={{ mb: 2.5, borderBottom: "1px solid #444", pb: 1 }}
                >
                  {section.label}
                </Typography>
              )}
              {/* Render Inputs */}

              <Grid container spacing={2.5}>
                {
                  // Nhóm input theo type
                  Object.entries(groupBy(section.inputs, "type")).map(
                    ([type, inputsOfType]) => {
                      // Nếu có nhiều input cùng loại, hiển thị theo grid
                      if (inputsOfType.length > 1) {
                        return (
                          <React.Fragment key={type}>
                            {inputsOfType.map((input: InputField) => (
                              <Grid item xs={12} sm={6} key={input.id}>
                                <Box sx={{ mb: 2 }}>
                                  {input.type !== "switch" && input.label && (
                                    <Typography
                                      variant="body1"
                                      gutterBottom
                                      sx={{ fontWeight: 500 }}
                                    >
                                      {input.label}
                                    </Typography>
                                  )}
                                  <RenderInput
                                    field={input}
                                    value={formData[input.id]}
                                    onChange={(value) =>
                                      handleInputChange(input.id, value)
                                    }
                                    disabled={isProcessing}
                                  />
                                  {input.helperText && (
                                    <FormHelperText
                                      sx={{
                                        color: "#ccc",
                                        ml: input.type === "switch" ? 0 : 1,
                                      }}
                                    >
                                      {input.helperText}
                                    </FormHelperText>
                                  )}
                                </Box>
                              </Grid>
                            ))}
                          </React.Fragment>
                        );
                      } else {
                        // Nếu chỉ có 1 input của loại đó → full width
                        const input = inputsOfType[0];
                        return (
                          <Grid item xs={12} key={input.id}>
                            <Box sx={{ mb: 2 }}>
                              {input.type !== "switch" && input.label && (
                                <Typography
                                  variant="body1"
                                  gutterBottom
                                  sx={{ fontWeight: 500 }}
                                >
                                  {input.label}
                                </Typography>
                              )}
                              {
                                <RenderInput
                                  field={input}
                                  value={formData[input.id]}
                                  onChange={(value) =>
                                    handleInputChange(input.id, value)
                                  }
                                  disabled={isProcessing}
                                />
                              }
                              {input.helperText && (
                                <FormHelperText
                                  sx={{
                                    color: "#ccc",
                                    ml: input.type === "switch" ? 0 : 1,
                                  }}
                                >
                                  {input.helperText}
                                </FormHelperText>
                              )}
                            </Box>
                          </Grid>
                        );
                      }
                    }
                  )
                }
              </Grid>
              {/* Render Outputs */}
              {section.outputs && section.outputs.length > 0 && (
                <Box sx={{ mt: 3, pt: 3, borderTop: "1px solid #444" }}>
                  {section.outputs.map((output: OutputField) => (
                    <Box key={output.id} sx={{ mb: 3 }}>
                      {/* RenderOutput handles the display of the output value and actions */}
                      {RenderOutput({
                        // Pass props as an object
                        output: output,
                        value: outputs[output.id],
                        onRefresh: handleExecute, // Pass manual execute for refresh button
                        disabled: isProcessing, // Disable output actions while processing
                      })}
                    </Box>
                  ))}
                  {/* Display loading indicator within the output section during processing */}
                  {isProcessing && (
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "center",
                        alignItems: "center",
                        pt: 2,
                      }}
                    >
                      <CircularProgress size={24} sx={{ color: "white" }} />
                      <Typography
                        sx={{ ml: 1, color: "white" }}
                        variant="body2"
                      >
                        Processing...
                      </Typography>
                    </Box>
                  )}
                </Box>
              )}
            </Paper>
          </Grid>
        ))}
      </Grid>
    </CommonLayout>
  );
};

export default ToolRenderer;
