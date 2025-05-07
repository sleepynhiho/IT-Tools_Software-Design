import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import debounce from "lodash.debounce";
import CustomPluginUIs from "./plugins/CustomPluginUIs";

import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Grid,
  Alert,
  Dialog,
  DialogContent,
  DialogActions,
  Fade,
  AlertTitle,
  alpha,
  Divider,
  Slide,
  useTheme,
  Stack,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import StarIcon from "@mui/icons-material/Star";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import TuneIcon from "@mui/icons-material/Tune";
import InputIcon from "@mui/icons-material/Input";
import AssessmentIcon from "@mui/icons-material/Assessment";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import OutputIcon from "@mui/icons-material/Output";
import CancelIcon from "@mui/icons-material/Cancel";
import LabelIcon from "@mui/icons-material/Label";
import StarIcon from "@mui/icons-material/Star";
import CommonLayout from "../layouts/CommonLayout";
import { useAllTools } from "../context/AllToolsContext";
import { useAuth } from "../context/AuthContext";
import { fetchWithAuth } from "../utils/fetchWithAuth";
import {
  PluginMetadata,
  Section,
  InputField,
  OutputField,
} from "../data/pluginList";
import { SettingsIcon } from "lucide-react";
import RenderInput from "./RenderInput";
import RenderOutput from "./RenderOutput";

const CURRENT_DATE_TIME = "2025-05-06 21:20:11";
const CURRENT_USER_LOGIN = "Kostovite";

function downloadDataUrl(dataUrl: string, filename: string) {
  try {
    if (
      !dataUrl ||
      typeof dataUrl !== "string" ||
      !dataUrl.startsWith("data:")
    ) {
      console.error("Invalid data URL provided for download:", dataUrl);
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
    console.error("Download failed:", e);
    alert("Download failed. See console for details.");
  }
}

const ToolRenderer: React.FC = () => {
  const { id: toolIdFromUrl } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const theme = useTheme();

  const {
    allTools: allPluginMetadata,
    isLoading: loadingMetadataList,
    error: metadataListError,
  } = useAllTools();

  const { getIdToken, userType } = useAuth();
  const isAdmin = userType === "admin";
  const isPremiumUser = userType === "premium" || userType === "admin";

  const [metadata, setMetadata] = useState<PluginMetadata | null>(null);
  const [loadingMetadata, setLoadingMetadata] = useState<boolean>(true);
  const [toolMetadataError, setToolMetadataError] = useState<string | null>(
    null
  );
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [resultData, setResultData] = useState<Record<string, any> | null>(
    null
  );
  const [showPremiumDialog, setShowPremiumDialog] = useState<boolean>(false);
  const evaluateCondition = useCallback(
    (
      conditionStr?: string,
      currentFormData = formData,
      currentResultData = resultData
    ): boolean => {
      if (!conditionStr) return true;
      try {
        const context = {
          ...currentFormData,
          ...(currentResultData || {}),
          success: currentResultData?.success,
        };
        const func = new Function(
          "context",
          `try { with(context) { return !!(${conditionStr}); } } catch (e) { console.error('Condition evaluation error:', '${conditionStr.replace(
            /'/g,
            "\\'"
          )}', e); return false; }`
        );
        return func(context);
      } catch (e) {
        console.error(
          "Error setting up condition evaluation for:",
          conditionStr,
          e
        );
        return false;
      }
    },
    [formData, resultData]
  );

  const processRequest = useCallback(
    async (currentFormData: Record<string, any>) => {
      if (!metadata?.id) {
        console.log("Process skipped: No valid metadata loaded.");
        setResultData({
          success: false,
          errorMessage: "Tool metadata not loaded.",
        });
        return;
      }
      if (!getIdToken) {
        console.error("Process skipped: Auth token function not available.");
        setResultData({
          success: false,
          errorMessage: "Authentication error.",
        });
        return;
      }
      if (metadata.accessLevel === 'premium' && !isPremiumUser) {
        console.log("Process skipped: Premium feature, user is not premium.");
        setShowPremiumDialog(true);
        return;
      }

      let clientSideValid = true;
      const tempErrors: Record<string, string> = {};
      metadata.sections?.forEach((section) => {
        section.inputs?.forEach((field) => {
          if (evaluateCondition(field.condition, currentFormData, resultData)) {
            if (field.required) {
              const value = currentFormData[field.id];
              if (
                value === undefined ||
                value === null ||
                String(value).trim() === ""
              ) {
                clientSideValid = false;
                tempErrors[field.id] = `${
                  field.label || field.id
                } is required.`;
              }
            }
            if (
              (field.type === "number" || field.type === "slider") &&
              currentFormData[field.id] != null &&
              currentFormData[field.id] !== ""
            ) {
              const numValue = Number(currentFormData[field.id]);
              if (isNaN(numValue)) {
                clientSideValid = false;
                tempErrors[field.id] = `Must be a valid number.`;
              } else {
                if (field.min !== undefined && numValue < field.min) {
                  clientSideValid = false;
                  tempErrors[field.id] = `Min value: ${field.min}.`;
                }
                if (field.max !== undefined && numValue > field.max) {
                  clientSideValid = false;
                  tempErrors[field.id] = `Max value: ${field.max}.`;
                }
              }
            }
          }
        });
      });
      setFormErrors(tempErrors);
      if (!clientSideValid) {
        console.log(
          "Process skipped: Client-side validation failed.",
          tempErrors
        );
        const errorKey =
          metadata.sections
            ?.flatMap((s) => s.outputs ?? [])
            .find((o) => o.style === "error" && o.type === "text")?.id ||
          "errorMessage";
        setResultData({
          success: false,
          [errorKey]: "Please fix the errors in the input fields.",
        });
        setIsProcessing(false);
        return;
      }

      setIsProcessing(true);

      const processURL = `/api/debug/${metadata.id}/process`;

      try {
        const payload: Record<string, any> = {};
        metadata.sections?.forEach((section) => {
          section.inputs?.forEach((field) => {
            if (
              evaluateCondition(field.condition, currentFormData, resultData)
            ) {
              const value = currentFormData[field.id];
              if (
                (field.type === "number" || field.type === "slider") &&
                typeof value === "string" &&
                value.trim() !== ""
              ) {
                const numVal = Number(value);
                payload[field.id] = isNaN(numVal) ? null : numVal;
              } else if (field.type === "number" || field.type === "slider") {
                payload[field.id] = value ?? null;
              } else if (field.type === "file" && typeof value === "string") {
                if (value.startsWith("data:")) {
                  console.log(`File data being sent, length: ${value.length}`);
                  payload[field.id] = value;
                } else {
                  console.error(
                    "Invalid file data format for field:",
                    field.id
                  );
                }
              } else {
                payload[field.id] = value;
              }
            }
          });
        });
        console.log(
          `Processing request to: ${processURL} with payload keys:`,
          Object.keys(payload)
        );

        const response = await fetchWithAuth(
          processURL,
          {
            method: "POST",
            body: JSON.stringify(payload),
          },
          getIdToken
        );

        const result = await response.json();
        console.log("Backend process response:", result);

        const errorKey =
          metadata.sections
            ?.flatMap((s) => s.outputs ?? [])
            .find((o) => o.style === "error" && o.type === "text")?.id ||
          "errorMessage";

        if (!response.ok || result.success === false) {
          const errorMessage =
            response.status === 403
              ? "Access Denied. You may not have permission for this tool or action."
              : result.errorMessage ||
                result.error ||
                `Request failed: ${response.status}`;
          setResultData({
            success: false,
            [errorKey]: errorMessage,
          });
        } else {
          if (result.success === undefined) result.success = true;
          setResultData({ ...result });
        }
      } catch (error: any) {
        console.error("Process request failed:", error);
        const errorKey =
          metadata.sections
            ?.flatMap((s) => s.outputs ?? [])
            .find((o) => o.style === "error" && o.type === "text")?.id ||
          "errorMessage";
        setResultData({
          success: false,
          [errorKey]:
            error.message || "Network error or failed to parse response.",
        });
      } finally {
        setIsProcessing(false);
      }
    },
    [metadata, resultData, evaluateCondition, getIdToken, isPremiumUser]
  );

  const debouncedProcessRequest = useCallback(debounce(processRequest, 500), [
    processRequest,
  ]);

  useEffect(() => {
    setLoadingMetadata(true);

    if (loadingMetadataList) {
      console.log("[ToolRenderer] Waiting for main plugin list to load...");
    } else if (metadataListError) {
      console.error(
        "[ToolRenderer] Error loading main plugin list:",
        metadataListError
      );
      setToolMetadataError(metadataListError);
      setLoadingMetadata(false);
    } else if (!Array.isArray(allPluginMetadata)) {
      console.error(
        "[ToolRenderer] Invalid plugin list format received from context."
      );
      setToolMetadataError("Invalid plugin list format.");
      setLoadingMetadata(false);
    } else {
      console.log(
        `[ToolRenderer] Finding metadata for id: ${toolIdFromUrl} in list of ${allPluginMetadata.length} from Context`
      );
      const toolMetadata = allPluginMetadata.find(
        (tool) => tool.id === toolIdFromUrl
      );

      if (toolMetadata) {
        // Check if tool is disabled and user is not admin - redirect to home
        if (toolMetadata.status === "disabled" && !isAdmin) {
          console.log(
            "[ToolRenderer] Tool is disabled and user is not admin, redirecting to home"
          );
          navigate("/");
          return;
        }

        // Check if tool is premium and user is not premium/admin - show premium dialog
        if (toolMetadata.accessLevel === "premium" && !isPremiumUser) {
          console.log(
            "[ToolRenderer] Premium tool detected, user is not premium"
          );
          setShowPremiumDialog(true);
        }

        if (!metadata || metadata.id !== toolMetadata.id) {
          console.log("[ToolRenderer] Setting metadata:", toolMetadata);
          setMetadata(toolMetadata);
          setToolMetadataError(null);
          setResultData(null);
          setFormErrors({});

          const initialFormData: Record<string, any> = {};
          toolMetadata.sections?.forEach((section) => {
            section.inputs?.forEach((input) => {
              const key = input.id;
              if (input.default !== undefined)
                initialFormData[key] = input.default;
              else if (input.type === "switch") initialFormData[key] = false;
              else if (input.type === "slider")
                initialFormData[key] = input.min ?? 0;
              else if (input.type === "number")
                initialFormData[key] = input.min ?? 0;
              else if (input.type === "color")
                initialFormData[key] = input.default ?? "#000000";
              else initialFormData[key] = "";
            });
          });
          console.log("[ToolRenderer] Initializing formData:", initialFormData);
          setFormData(initialFormData);
        }
        setLoadingMetadata(false);
      } else {
        console.error(
          `[ToolRenderer] Metadata for id "${toolIdFromUrl}" not found in context list.`
        );
        setMetadata(null);
        setToolMetadataError(`Tool with ID "${toolIdFromUrl}" not found.`);
        setFormData({});
        setResultData(null);
        setLoadingMetadata(false);
      }
    }
  }, [
    toolIdFromUrl,
    allPluginMetadata,
    loadingMetadataList,
    metadataListError,
    metadata,
    isAdmin,
    isPremiumUser,
    navigate
  ]);

  useEffect(() => {
    if (
      metadata?.triggerUpdateOnChange === true &&
      Object.keys(formData).length > 0 &&
      !isProcessing &&
      !resultData &&
      !(metadata.accessLevel === 'premium' && !isPremiumUser)
    ) {
      console.log(
        "[ToolRenderer] Effect Triggering initial process request with formData:",
        formData
      );
      processRequest(formData);
    }
  }, [
    formData,
    metadata?.triggerUpdateOnChange,
    metadata?.accessLevel,
    processRequest,
    isProcessing,
    resultData,
    isPremiumUser
  ]);

  const handleInputChange = useCallback(
    (inputId: string, value: any) => {
      const newFormData = { ...formData, [inputId]: value };
      setFormData(newFormData);
      if (formErrors[inputId]) {
        setFormErrors((prev) => {
          const newState = { ...prev };
          delete newState[inputId];
          return newState;
        });
      }
      if (metadata?.triggerUpdateOnChange === true) {
        console.log(
          `[ToolRenderer] Debouncing process request due to change in ${inputId}`
        );
        if (!(metadata.accessLevel === 'premium' && !isPremiumUser)) {
          debouncedProcessRequest(newFormData);
        }
      }
    },
    [formData, formErrors, metadata, debouncedProcessRequest, isPremiumUser]
  );

  const handleExecute = useCallback(() => {
    console.log("[ToolRenderer] Manual Execute/Refresh Triggered");
    if (metadata && Object.keys(formData).length > 0) {
      processRequest(formData);
    }
  }, [formData, metadata, processRequest]);

  const renderCustomUI = useCallback(() => {
    if (!metadata?.customUI || !metadata.id) return null;
    // Check if premium tool and user is not premium/admin
    if (metadata.accessLevel === "premium" && !isPremiumUser) {
      return (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          action={
            <Button
              color="inherit"
              size="small"
              onClick={() => navigate("/upgrade")}
            >
              UPGRADE
            </Button>
          }
        >
          This premium tool requires an upgrade to use. You can see a preview,
          but functionality is limited.
        </Alert>
      );
    }

    const CustomUIComponent = CustomPluginUIs[metadata.id];
    if (CustomUIComponent) {
      return (
        <CustomUIComponent
          plugin={metadata}
          inputValues={formData}
          setInputValues={setFormData}
          onSubmit={handleExecute}
          outputValues={resultData || {}}
          loading={isProcessing}
        />
      );
    }
    return (
      <Alert 
        severity="warning" 
        sx={{ 
          mb: 2,
          backgroundColor: 'rgba(237, 108, 2, 0.1)', 
          color: '#ffffff' 
        }}
      >
        This tool requires a custom UI component that is not available.
      </Alert>
    );
  }, [
    metadata,
    formData,
    setFormData,
    handleExecute,
    resultData,
    isProcessing,
    isPremiumUser,
    navigate,
  ]);

  const handlePremiumDialogClose = () => {
    setShowPremiumDialog(false);
  };

  const handleUpgradeClick = () => {
    navigate("/upgrade");
    setShowPremiumDialog(false);
  };

  if (loadingMetadataList || loadingMetadata) {
    return (
      <CommonLayout
        title="Loading..."
        description=""
        toolId={toolIdFromUrl || ""}
        icon=""
      >
        <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
          <CircularProgress sx={{ color: "#3b956f" }} />
        </Box>
      </CommonLayout>
    );
  }

  const displayError = toolMetadataError || metadataListError;
  if (displayError || !metadata) {
    return (
      <CommonLayout
        title="Error"
        description=""
        toolId={toolIdFromUrl || ""}
        icon=""
      >
        <Alert 
          severity="error" 
          sx={{ 
            m: 2, 
            backgroundColor: 'rgba(211, 47, 47, 0.1)',
            color: '#ffffff'
          }}
        >
          {displayError || "Tool metadata could not be loaded."}
        </Alert>
      </CommonLayout>
    );
  }

  const errorOutputFieldId =
    metadata.sections
      ?.flatMap((s) => s.outputs ?? [])
      .find((o) => o.style === "error" && o.type === "text")?.id ||
    "errorMessage";

  const isPremiumTool = metadata.accessLevel === "premium";
  const isDisabledTool = metadata.status === "disabled";

  return (
    <CommonLayout
      title={metadata.name}
      description={metadata.description}
      toolId={metadata.id}
      icon={metadata.icon}
    >
      {isPremiumTool && (
        <Fade in={true}>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              mb: 3,
              p: 1.5,
              backgroundColor: isPremiumUser
                ? alpha("#FFB300", 0.15)
                : alpha("#FFB300", 0.08),
              border: "1px solid",
              borderColor: isPremiumUser
                ? "warning.main"
                : alpha("#FFB300", 0.3),
              borderRadius: 2,
              backdropFilter: "blur(8px)",
              boxShadow: isPremiumUser
                ? `0 2px 8px ${alpha("#FFB300", 0.2)}`
                : "none",
              transition: "all 0.3s ease",
            }}
          >
            <StarIcon
              sx={{
                color: "warning.main",
                mr: 1.5,
                fontSize: "1.2rem",
                animation: isPremiumUser ? "pulse 2s infinite" : "none",
                "@keyframes pulse": {
                  "0%": { opacity: 0.8 },
                  "50%": { opacity: 1 },
                  "100%": { opacity: 0.8 },
                },
              }}
            />
            <Typography
              variant="body2"
              sx={{
                fontWeight: isPremiumUser ? "500" : "400",
                color: isPremiumUser ? "#FFB300" : alpha("#FFB300", 0.8),
                letterSpacing: "0.01em",
              }}
            >
              {isPremiumUser
                ? "Premium Tool - You have full access to this feature"
                : "Premium Tool - Some features may be limited"}
            </Typography>
          </Box>
        </Fade>
      )}

      {isDisabledTool && isAdmin && (
        <Fade in={true}>
          <Alert
            severity="warning"
            variant="outlined"
            icon={<WarningAmberIcon />}
            sx={{
              mb: 3,
              borderRadius: 2,
              "& .MuiAlert-icon": {
                color: "#f5b82e",
              },
            }}
          >
            <AlertTitle sx={{ fontWeight: 500 }}>
              Administrator Notice
            </AlertTitle>
            This tool is currently disabled and only visible to administrators.
            Regular users cannot access this tool.
          </Alert>
        </Fade>
      )}

      {resultData?.success === false && resultData[errorOutputFieldId] && (
        <Fade in={true}>
          <Alert
            severity="error"
            variant="filled"
            sx={{
              mb: 3,
              borderRadius: 2,
              boxShadow: `0 3px 8px ${alpha("#d32f2f", 0.2)}`,
            }}
            onClose={() =>
              setResultData((prev) => ({
                ...prev,
                success: undefined,
                [errorOutputFieldId]: null,
              }))
            }
          >
            <AlertTitle>Error</AlertTitle>
            {String(resultData[errorOutputFieldId])}
          </Alert>
        </Fade>
      )}

      {metadata.customUI ? (
        renderCustomUI()
      ) : (
        <Box>
          {metadata.sections
            ?.filter((section) => evaluateCondition(section.condition))
            .map((section: Section, index: number) => (
              <Box key={`${metadata.id}-${section.id}-${index}`} sx={{ mb: 3 }}>
                {section.inputs &&
                  section.inputs.filter((field) =>
                    evaluateCondition(field.condition, formData, resultData)
                  ).length > 0 && (
                    <Paper
                      sx={{
                        p: { xs: 2.5, sm: 3.5 },
                        mb: 3.5,
                        width: "100%",
                        borderRadius: 2.5,
                        boxShadow: (theme) => "0 4px 20px rgba(0,0,0,0.25)",
                        border: (theme) =>
                          `1px solid ${alpha("#ffffff", 0.05)}`,
                        overflow: "hidden",
                        position: "relative",
                        "&::before": {
                          content: '""',
                          position: "absolute",
                          top: 0,
                          left: 0,
                          right: 0,
                          height: "3px",
                          backgroundImage:
                            "linear-gradient(90deg, #3b956f, #1ea54c, #3b956f)",
                        },
                        transition: "transform 0.3s ease, box-shadow 0.3s ease",
                        "&:hover": {
                          boxShadow: (theme) => "0 6px 24px rgba(0,0,0,0.3)",
                          transform: "translateY(-2px)",
                        },
                      }}
                      elevation={0}
                    >
                      {section.label && (
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 3,
                            borderBottom: 1,
                            borderColor: "divider",
                            pb: 1.5,
                            fontWeight: 600,
                            display: "flex",
                            alignItems: "center",
                            "& svg": {
                              mr: 1.5,
                              color: "#3b956f",
                            },
                          }}
                        >
                          {/* Add section icon based on section id/type if needed */}
                          {section.label.toLowerCase().includes("input") && (
                            <InputIcon />
                          )}
                          {section.label.toLowerCase().includes("config") && (
                            <SettingsIcon />
                          )}
                          {section.label.toLowerCase().includes("option") && (
                            <TuneIcon />
                          )}
                          {section.label}
                        </Typography>
                      )}
                      <Box
                        sx={{
                          display: "flex",
                          flexDirection: "row",
                          flexWrap: "wrap",
                          mx: -1.5,
                        }}
                      >
                        {section.inputs
                          .filter((field) =>
                            evaluateCondition(
                              field.condition,
                              formData,
                              resultData
                            )
                          )
                          .map((field: InputField) => {
                            // Determine width based on field type
                            const fieldWidth =
                              field.type === "color" || field.type === "switch"
                                ? { xs: "100%", sm: "50%", md: "33.333%" }
                                : field.type === "slider"
                                ? { xs: "100%", sm: "50%", md: "66.666%" }
                                : "100%";

                            return (
                              <Box
                                sx={{
                                  px: 1.5, // padding for better spacing
                                  pb: 3, // bottom padding between items
                                  width: fieldWidth,
                                  boxSizing: "border-box",
                                }}
                                key={field.id}
                              >
                                <RenderInput
                                  field={field}
                                  value={formData[field.id]}
                                  onChange={(value) =>
                                    handleInputChange(field.id, value)
                                  }
                                  disabled={
                                    isProcessing ||
                                    (isPremiumTool && !isPremiumUser)
                                  }
                                  error={formErrors[field.id]}
                                />
                              </Box>
                            );
                          })}
                      </Box>
                    </Paper>
                  )}
                {section.outputs &&
                  section.outputs.filter((field) =>
                    evaluateCondition(field.condition, formData, resultData)
                  ).length > 0 &&
                  evaluateCondition(section.condition, formData, resultData) &&
                  (resultData ||
                    isProcessing ||
                    section.id === "errorDisplay") && (
                    <Paper
                      sx={{
                        p: { xs: 2.5, sm: 3.5 },
                        mb: 3.5,
                        borderRadius: 2.5,
                        boxShadow: (theme) => "0 4px 20px rgba(0,0,0,0.25)",
                        border: (theme) =>
                          `1px solid ${alpha("#ffffff", 0.05)}`,
                        backgroundColor: (theme) => alpha("#111", 0.4),
                        position: "relative",
                        "&::before": {
                          content: '""',
                          position: "absolute",
                          top: 0,
                          left: 0,
                          right: 0,
                          height: "3px",
                          backgroundImage:
                            "linear-gradient(90deg, #647dee, #7f53ac)",
                        },
                      }}
                      elevation={0}
                    >
                      {section.label && (
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 3,
                            borderBottom: 1,
                            borderColor: "divider",
                            pb: 1.5,
                            fontWeight: 600,
                            display: "flex",
                            alignItems: "center",
                            "& svg": {
                              mr: 1.5,
                              color: "#7f53ac",
                            },
                          }}
                        >
                          {/* Add section icon based on section id/type if needed */}
                          {section.label.toLowerCase().includes("result") && (
                            <AssessmentIcon />
                          )}
                          {section.label.toLowerCase().includes("output") && (
                            <OutputIcon />
                          )}
                          {section.label}
                        </Typography>
                      )}
                      {(resultData || section.id === "errorDisplay") &&
                        section.outputs
                          .filter((field) =>
                            evaluateCondition(
                              field.condition,
                              formData,
                              resultData
                            )
                          )
                          .map((output: OutputField) => {
                            const outputValue = resultData?.[output.id];
                            const isErrorField =
                              output.id === errorOutputFieldId;
                            const shouldRender =
                              (resultData?.success === false &&
                                isErrorField &&
                                outputValue !== undefined &&
                                outputValue !== null) ||
                              (resultData?.success !== false &&
                                outputValue !== undefined &&
                                outputValue !== null);
                            if (shouldRender) {
                              return (
                                <Box
                                  key={output.id}
                                  sx={{
                                    mb: 3,
                                    animation: "fadeIn 0.5s ease",
                                    "@keyframes fadeIn": {
                                      "0%": {
                                        opacity: 0,
                                        transform: "translateY(10px)",
                                      },
                                      "100%": {
                                        opacity: 1,
                                        transform: "translateY(0)",
                                      },
                                    },
                                  }}
                                >
                                  <RenderOutput
                                    output={output}
                                    value={outputValue}
                                    resultData={resultData}
                                    onRefresh={handleExecute}
                                    disabled={
                                      isProcessing ||
                                      (isPremiumTool && !isPremiumUser)
                                    }
                                  />
                                </Box>
                              );
                            }
                            return null;
                          })}
                      {isProcessing && section.id !== "errorDisplay" && (
                        <Box
                          sx={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            justifyContent: "center",
                            py: 4,
                            opacity: 0.8,
                          }}
                        >
                          <CircularProgress
                            size={40}
                            thickness={4}
                            sx={{ mb: 2, color: "#7f53ac" }}
                          />
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            Processing your request...
                          </Typography>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ mt: 0.5 }}
                          >
                            This may take a few moments
                          </Typography>
                        </Box>
                      )}
                    </Paper>
                  )}
              </Box>
            ))}

          {/* Manual Process Button */}
          <Box
            sx={{
              display: "flex",
              justifyContent: "center",
              mt: 2,
              mb: 4,
            }}
          >
            <Button
              variant="contained"
              color="primary"
              onClick={
                isPremiumTool && !isPremiumUser
                  ? () => setShowPremiumDialog(true)
                  : handleExecute
              }
              disabled={isProcessing || loadingMetadata}
              size="large"
              sx={{
                py: 1.5,
                px: 4,
                borderRadius: 2,
                textTransform: "none",
                fontWeight: 600,
                fontSize: "1rem",
                minWidth: "200px",
                background:
                  isPremiumTool && !isPremiumUser
                    ? "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)"
                    : "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                boxShadow:
                  isPremiumTool && !isPremiumUser
                    ? "0 4px 12px rgba(255, 179, 0, 0.4)"
                    : "0 4px 12px rgba(30, 165, 76, 0.3)",
                "&:hover": {
                  background:
                    isPremiumTool && !isPremiumUser
                      ? "linear-gradient(45deg, #FFA000 30%, #FFD54F 90%)"
                      : "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)",
                  boxShadow:
                    isPremiumTool && !isPremiumUser
                      ? "0 6px 14px rgba(255, 179, 0, 0.5)"
                      : "0 6px 14px rgba(30, 165, 76, 0.4)",
                },
                transition: "all 0.3s ease",
              }}
              startIcon={
                isProcessing ? (
                  <CircularProgress size={24} color="inherit" />
                ) : isPremiumTool && !isPremiumUser ? (
                  <StarIcon />
                ) : (
                  <PlayArrowIcon />
                )
              }
              endIcon={
                !isProcessing &&
                !isPremiumTool &&
                metadata.triggerUpdateOnChange ? (
                  <RefreshIcon />
                ) : null
              }
            >
              {isProcessing
                ? "Processing..."
                : isPremiumTool && !isPremiumUser
                ? "Upgrade to Use"
                : metadata.triggerUpdateOnChange
                ? "Regenerate Results"
                : "Process Data"}
            </Button>
          </Box>
        </Box>
      )}

      <Dialog
        open={showPremiumDialog}
        onClose={handlePremiumDialogClose}
        aria-labelledby="premium-dialog-title"
        TransitionComponent={(props) => <Slide {...props} direction="up" />}
        PaperProps={{
          sx: {
            backgroundColor: "#1e1e2f",
            color: "#ffffff",
            borderRadius: 3,
            border: "1px solid",
            borderColor: alpha("#ffb300", 0.3),
            maxWidth: "550px",
            overflow: "hidden",
            boxShadow: "0 10px 30px rgba(0,0,0,0.3)",
          },
        }}
      >
        {/* Premium badge decorative element */}
        <Box
          sx={{
            height: "100px",
            background: "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
            position: "relative",
            overflow: "hidden",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Box
            sx={{
              position: "absolute",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundImage:
                "radial-gradient(circle, rgba(255,255,255,0.3) 1px, transparent 1px)",
              backgroundSize: "15px 15px",
              opacity: 0.5,
            }}
          />

          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              position: "relative",
              zIndex: 1,
            }}
          >
            <StarIcon sx={{ fontSize: "40px", color: "#fff", mb: 1 }} />
            <Typography variant="h6" sx={{ color: "#fff", fontWeight: "bold" }}>
              Premium Feature
            </Typography>
          </Box>
        </Box>

        <DialogContent sx={{ py: 4, px: 4 }}>
          <Typography
            variant="h5"
            sx={{ mb: 2, fontWeight: 600, textAlign: "center" }}
          >
            {metadata.name}
          </Typography>
          <Typography variant="body1" sx={{ mb: 3, textAlign: "center" }}>
            This premium tool requires a subscription to unlock its full
            capabilities. Upgrade your account today to access this and all
            other premium features.
          </Typography>

          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              background: alpha("#000", 0.3),
              p: 3,
              borderRadius: 2,
              mb: 3,
              border: `1px solid ${alpha("#ffb300", 0.2)}`,
            }}
          >
            <Typography
              variant="h6"
              sx={{ fontWeight: 600, mb: 2, color: "#ffb300" }}
            >
              Premium Benefits:
            </Typography>

            <Box sx={{ display: "flex", alignItems: "center", mb: 1.5 }}>
              <CheckCircleIcon
                sx={{ color: "#ffb300", mr: 1.5, fontSize: "1.2rem" }}
              />
              <Typography variant="body2">
                Access to all premium tools
              </Typography>
            </Box>

            <Box sx={{ display: "flex", alignItems: "center", mb: 1.5 }}>
              <CheckCircleIcon
                sx={{ color: "#ffb300", mr: 1.5, fontSize: "1.2rem" }}
              />
              <Typography variant="body2">
                Priority customer support with 24/7 assistance
              </Typography>
            </Box>

            <Box sx={{ display: "flex", alignItems: "center", mb: 1.5 }}>
              <CheckCircleIcon
                sx={{ color: "#ffb300", mr: 1.5, fontSize: "1.2rem" }}
              />
              <Typography variant="body2">
                Early access to beta features and new tools
              </Typography>
            </Box>

            <Box sx={{ display: "flex", alignItems: "center" }}>
              <CheckCircleIcon
                sx={{ color: "#ffb300", mr: 1.5, fontSize: "1.2rem" }}
              />
              <Typography variant="body2">
                Unlimited usage with no daily limits
              </Typography>
            </Box>
          </Box>

          <Divider sx={{ my: 3 }} />

          <Box sx={{ textAlign: "center" }}>
            <Typography
              variant="caption"
              sx={{ color: "#a3a3a3", fontSize: "0.75rem" }}
            >
              Current Time (UTC): 2025-05-06 20:16:51 • User: hanhiho
            </Typography>
          </Box>
        </DialogContent>

        <DialogActions
          sx={{
            borderTop: `1px solid ${alpha("#fff", 0.1)}`,
            p: 3,
            display: "flex",
            justifyContent: "space-between",
          }}
        >
          <Button
            onClick={handlePremiumDialogClose}
            color="inherit"
            sx={{
              fontWeight: 400,
              opacity: 0.7,
              "&:hover": { opacity: 1 },
            }}
          >
            Maybe Later
          </Button>
          <Button
            onClick={handleUpgradeClick}
            variant="contained"
            startIcon={<StarIcon />}
            sx={{
              background: "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
              color: "#000",
              fontWeight: "bold",
              px: 4,
              py: 1,
              boxShadow: "0 4px 12px rgba(255,179,0,0.3)",
              "&:hover": {
                background: "linear-gradient(45deg, #FFA000 30%, #FFD54F 90%)",
                boxShadow: "0 6px 16px rgba(255,179,0,0.4)",
              },
            }}
          >
            Upgrade to Premium
          </Button>
        </DialogActions>
      </Dialog>
    </CommonLayout>
  );
};

export default ToolRenderer;
