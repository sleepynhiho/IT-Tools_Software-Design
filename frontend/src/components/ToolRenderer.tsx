import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import debounce from "lodash.debounce";
import CustomPluginUIs from "./plugins/CustomPluginUIs";

import {
  Box,
  Typography,
  Paper,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  FormHelperText,
  CircularProgress,
  Grid,
  Card,
  CardContent,
  CardHeader,
  Switch,
  FormControlLabel,
  Slider,
  CssBaseline,
  Container,
  Alert,
  IconButton,
  Chip,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  useTheme,
  Stack,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import DownloadIcon from "@mui/icons-material/Download";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
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
  const isAdmin = userType === 'admin';
  const isPremiumUser = userType === 'premium' || userType === 'admin';

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
        if (toolMetadata.status === 'disabled' && !isAdmin) {
          console.log("[ToolRenderer] Tool is disabled and user is not admin, redirecting to home");
          navigate('/');
          return;
        }

        if (toolMetadata.accessLevel === 'premium' && !isPremiumUser) {
          console.log("[ToolRenderer] Premium tool detected, user is not premium");
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
                initialFormData[key] =
                  input.min ??
                  0;
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
    
    if (metadata.accessLevel === 'premium' && !isPremiumUser) {
      return (
        <Alert 
          severity="warning" 
          sx={{ 
            mb: 2,
            backgroundColor: 'rgba(255, 179, 0, 0.1)',
            color: '#ffffff'
          }}
          action={
            <Button 
              color="warning" 
              size="small"
              onClick={() => navigate('/upgrade')}
              sx={{ color: '#ffb300' }}
            >
              UPGRADE
            </Button>
          }
        >
          This premium tool requires an upgrade to use. You can see a preview, but functionality is limited.
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
    navigate
  ]);

  const handlePremiumDialogClose = () => {
    setShowPremiumDialog(false);
  };

  const handleUpgradeClick = () => {
    navigate('/upgrade');
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

  const isPremiumTool = metadata.accessLevel === 'premium';
  const isDisabledTool = metadata.status === 'disabled';

  return (
    <CommonLayout
      title={metadata.name}
      description={metadata.description}
      toolId={metadata.id}
      icon={metadata.icon}
    >
      {isPremiumTool && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            mb: 2,
            p: 1,
            backgroundColor: isPremiumUser ? 'rgba(255, 179, 0, 0.1)' : 'rgba(255, 179, 0, 0.05)',
            border: '1px solid',
            borderColor: 'warning.main',
            borderRadius: 1,
            width: '100%',
          }}
        >
          <StarIcon sx={{ color: 'warning.main', mr: 1 }} />
          <Typography variant="body2" sx={{ fontWeight: 'medium', color: isPremiumUser ? '#ffffff' : '#a3a3a3' }}>
            {isPremiumUser 
              ? 'Premium Tool - You have full access to this feature' 
              : 'Premium Tool - Some features may be limited'}
          </Typography>
        </Box>
      )}

      {isDisabledTool && isAdmin && (
        <Alert 
          severity="warning" 
          sx={{ 
            mb: 2,
            backgroundColor: 'rgba(237, 108, 2, 0.1)', 
            color: '#ffffff',
            width: '100%',
          }}
        >
          This tool is currently disabled. Only administrators can view it.
        </Alert>
      )}

      {resultData?.success === false && resultData[errorOutputFieldId] && (
        <Alert
          severity="error"
          sx={{ 
            mb: 2,
            backgroundColor: 'rgba(211, 47, 47, 0.1)',
            color: '#ffffff',
            width: '100%',
          }}
          onClose={() =>
            setResultData((prev) => ({
              ...prev,
              success: undefined,
              [errorOutputFieldId]: null,
            }))
          }
        >
          {String(resultData[errorOutputFieldId])}
        </Alert>
      )}

      {metadata.customUI ? (
        renderCustomUI()
      ) : (
        <>
          {metadata.sections
            ?.filter((section) => evaluateCondition(section.condition))
            .map((section: Section, index: number) => (
              <Box
                key={`${metadata.id}-${section.id}-${index}`}
                sx={{ width: '100%', mb: 3 }}
              >
                {section.inputs &&
                  section.inputs.filter((field) =>
                    evaluateCondition(field.condition, formData, resultData)
                  ).length > 0 && (
                    <Paper 
                      sx={{ 
                        p: { xs: 1.5, sm: 2, md: 3 },
                        mb: 3,
                        backgroundColor: '#232323',
                        color: '#ffffff',
                        width: '100%',
                        boxSizing: 'border-box'
                      }} 
                      elevation={2}
                    >
                      {section.label && (
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 2.5,
                            borderBottom: 1,
                            borderColor: "divider",
                            pb: 1,
                            color: "#ffffff"
                          }}
                        >
                          {section.label}
                        </Typography>
                      )}
                      <Stack spacing={2} width="100%">
                        {section.inputs
                          .filter((field) =>
                            evaluateCondition(
                              field.condition,
                              formData,
                              resultData
                            )
                          )
                          .map((field: InputField) => (
                            <Box key={field.id} width="100%">
                              <RenderInput
                                field={field}
                                value={formData[field.id]}
                                onChange={(value) =>
                                  handleInputChange(field.id, value)
                                }
                                disabled={isProcessing || (isPremiumTool && !isPremiumUser)}
                                error={formErrors[field.id]}
                              />
                            </Box>
                          ))}
                      </Stack>
                    </Paper>
                  )}
                {section.outputs &&
                  section.outputs.filter((field) =>
                    evaluateCondition(field.condition, formData, resultData)
                  ).length > 0 &&
                  evaluateCondition(
                    section.condition,
                    formData,
                    resultData
                  ) &&
                  (resultData ||
                    isProcessing ||
                    section.id === "errorDisplay") && (
                    <Paper 
                      sx={{ 
                        p: { xs: 1.5, sm: 2, md: 3 },
                        mb: 3,
                        backgroundColor: '#232323',
                        color: '#ffffff',
                        width: '100%',
                        boxSizing: 'border-box'
                      }} 
                      elevation={2}
                    >
                      {section.label && (
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 2.5,
                            borderBottom: 1,
                            borderColor: "divider",
                            pb: 1,
                            color: "#ffffff"
                          }}
                        >
                          {section.label}
                        </Typography>
                      )}
                      <Stack spacing={2} width="100%">
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
                                  <Box key={output.id} width="100%">
                                    <RenderOutput
                                      output={output}
                                      value={outputValue}
                                      resultData={resultData}
                                      onRefresh={handleExecute}
                                      disabled={isProcessing || (isPremiumTool && !isPremiumUser)}
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
                              alignItems: "center",
                              pt: 1,
                            }}
                          >
                            <CircularProgress size={20} sx={{ color: "#3b956f" }} />
                            <Typography variant="body2" sx={{ ml: 1, color: "#a3a3a3" }}>
                              Processing...
                            </Typography>
                          </Box>
                        )}
                      </Stack>
                    </Paper>
                  )}
              </Box>
            ))}

          <Button
            variant="contained"
            color="primary"
            onClick={isPremiumTool && !isPremiumUser ? () => setShowPremiumDialog(true) : handleExecute}
            disabled={isProcessing || loadingMetadata}
            sx={{ 
              mt: 1,
              backgroundColor: "#3b956f",
              '&:hover': {
                backgroundColor: "#2d7d5b"
              }
            }}
            startIcon={
              isProcessing ? (
                <CircularProgress size={20} color="inherit" />
              ) : isPremiumTool && !isPremiumUser ? (
                <StarIcon />
              ) : (
                <RefreshIcon />
              )
            }
          >
            {isProcessing
              ? "Processing..."
              : isPremiumTool && !isPremiumUser
              ? "Upgrade to Use"
              : metadata.triggerUpdateOnChange
              ? "Regenerate Manually"
              : "Process"}
          </Button>
        </>
      )}
      
      <Dialog
        open={showPremiumDialog}
        onClose={handlePremiumDialogClose}
        aria-labelledby="premium-dialog-title"
        PaperProps={{
          sx: {
            backgroundColor: "#2e2e2e",
            color: "#ffffff",
            borderRadius: "8px",
            border: "1px solid #3b956f",
            maxWidth: "500px",
            width: '100%'
          }
        }}
      >
        <DialogTitle id="premium-dialog-title" sx={{ 
          borderBottom: "1px solid rgba(255,255,255,0.1)",
          display: "flex",
          alignItems: "center"
        }}>
          <StarIcon sx={{ color: "#ffb300", mr: 1.5 }} />
          Premium Feature
        </DialogTitle>
        <DialogContent sx={{ py: 3 }}>
          <Typography variant="h6" sx={{ mb: 2, color: "#ffffff" }}>
            {metadata.name} is a Premium Tool
          </Typography>
          <Typography variant="body1" sx={{ mb: 2, color: "#a3a3a3" }}>
            This feature requires a premium subscription to access. Upgrade your account to unlock this tool and all other premium features.
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center", background: "rgba(0,0,0,0.2)", p: 2, borderRadius: "4px", mb: 2 }}>
            <Box sx={{ mr: 2, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <StarIcon sx={{ color: "#ffb300", fontSize: "32px" }} />
            </Box>
            <Box>
              <Typography variant="body1" sx={{ fontWeight: "bold", mb: 0.5, color: "#ffffff" }}>
                Premium Benefits:
              </Typography>
              <Typography variant="body2" sx={{ mb: 0.5, color: "#a3a3a3" }}>
                • Access to all premium tools
              </Typography>
              <Typography variant="body2" sx={{ mb: 0.5, color: "#a3a3a3" }}>
                • Priority customer support
              </Typography>
              <Typography variant="body2" sx={{ color: "#a3a3a3" }}>
                • Early access to new features
              </Typography>
            </Box>
          </Box>
          <Typography variant="caption" sx={{ color: "#a3a3a3", display: "block", textAlign: "center" }}>
            Current Time (UTC): {CURRENT_DATE_TIME}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ borderTop: "1px solid rgba(255,255,255,0.1)", p: 2, justifyContent: "space-between" }}>
          <Button 
            onClick={handlePremiumDialogClose} 
            color="inherit"
            sx={{ color: "#a3a3a3" }}
          >
            Maybe Later
          </Button>
          <Button 
            onClick={handleUpgradeClick}
            variant="contained"
            sx={{ 
              background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
              "&:hover": { 
                background: "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)" 
              }
            }}
          >
            Upgrade to Premium
          </Button>
        </DialogActions>
      </Dialog>
    </CommonLayout>
  );
};

interface RenderInputProps {
  field: InputField;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
  error?: string;
}
const RenderInput: React.FC<RenderInputProps> = ({
  field,
  value,
  onChange,
  disabled,
  error,
}) => {
  const theme = useTheme();
  
  const commonProps = {
    fullWidth: true,
    size: "small",
    variant: "outlined",
    label: field.label,
    disabled: disabled,
    required: field.required,
    error: !!error,
    helperText: error || field.helperText,
    placeholder: field.placeholder,
    sx: {
      width: '100%',
      '& .MuiOutlinedInput-root': {
        color: '#ffffff',
        '& fieldset': {
          borderColor: '#525252',
        },
        '&:hover fieldset': {
          borderColor: '#3b956f',
        },
        '&.Mui-focused fieldset': {
          borderColor: '#3b956f',
        },
      },
      '& .MuiInputLabel-root': {
        color: '#a3a3a3',
      },
      '& .MuiFormHelperText-root': {
        color: !!error ? '#d32f2f' : '#a3a3a3',
      },
    }
  } as const;
  switch (field.type) {
    case "text":
    case "password":
      return (
        <TextField
          {...commonProps}
          type={field.type}
          multiline={field.multiline}
          rows={field.rows || 2}
          maxRows={field.maxRows || 6}
          value={value ?? ""}
          onChange={(e) => onChange(e.target.value)}
        />
      );
    case "number":
      return (
        <TextField
          {...commonProps}
          type="number"
          value={value ?? ""}
          onChange={(e) => onChange(e.target.value)}
          inputProps={{
            min: field.min,
            max: field.max,
            step: field.step ?? "any",
            style: { color: '#ffffff' }
          }}
        />
      );
    case "select":
      return (
        <FormControl
          fullWidth
          size="small"
          variant="outlined"
          disabled={disabled}
          required={field.required}
          error={!!error}
          sx={{
            width: '100%',
            '& .MuiOutlinedInput-root': {
              color: '#ffffff',
              '& fieldset': {
                borderColor: '#525252',
              },
              '&:hover fieldset': {
                borderColor: '#3b956f',
              },
              '&.Mui-focused fieldset': {
                borderColor: '#3b956f',
              },
            },
            '& .MuiInputLabel-root': {
              color: '#a3a3a3',
            },
            '& .MuiFormHelperText-root': {
              color: !!error ? '#d32f2f' : '#a3a3a3',
            },
            '& .MuiMenu-paper': {
              backgroundColor: '#232323',
            }
          }}
        >
          <InputLabel>{field.label}</InputLabel>
          <Select
            label={field.label}
            value={value ?? field.default ?? ""}
            onChange={(e) => onChange(e.target.value)}
            MenuProps={{
              PaperProps: {
                sx: {
                  bgcolor: '#232323',
                  color: '#ffffff',
                  '& .MuiMenuItem-root:hover': {
                    bgcolor: 'rgba(59, 149, 111, 0.1)',
                  },
                  '& .MuiMenuItem-root.Mui-selected': {
                    bgcolor: 'rgba(59, 149, 111, 0.2)',
                  },
                }
              }
            }}
          >
            {!field.required && field.default === undefined && (
              <MenuItem value="">
                <em>None</em>
              </MenuItem>
            )}
            {field.options?.map((option: any, index: number) => (
              <MenuItem
                key={index}
                value={typeof option === "object" ? option.value : option}
              >
                {typeof option === "object" ? option.label : option}
              </MenuItem>
            ))}
          </Select>
          {(error || field.helperText) && (
            <FormHelperText error={!!error}>
              {error || field.helperText}
            </FormHelperText>
          )}
        </FormControl>
      );
    case "switch":
      return (
        <Box sx={{ width: '100%' }}>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              width: "100%",
              minHeight: "40px",
            }}
          >
            <Typography
              variant="body2"
              sx={{ color: disabled ? "text.disabled" : "#ffffff", mr: 1 }}
            >
              {field.label || ""}
            </Typography>
            <Switch
              name={field.id}
              checked={!!value}
              onChange={(e) => onChange(e.target.checked)}
              disabled={disabled}
              sx={{
                '& .MuiSwitch-switchBase.Mui-checked': {
                  color: '#3b956f',
                },
                '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                  backgroundColor: '#3b956f',
                },
              }}
            />
          </Box>
          {(error || field.helperText) && (
            <FormHelperText 
              error={!!error} 
              sx={{ 
                ml: 1,
                color: !!error ? '#d32f2f' : '#a3a3a3',
              }}
            >
              {error || field.helperText}
            </FormHelperText>
          )}
        </Box>
      );
    case "slider":
      return (
        <Box sx={{ px: 1, width: '100%' }}>
          <Typography 
            gutterBottom 
            id={`${field.id}-label`} 
            sx={{ color: '#ffffff' }}
          >
            {field.label} ({value ?? field.default ?? field.min})
          </Typography>
          <Slider
            aria-labelledby={`${field.id}-label`}
            value={
              typeof value === "number"
                ? value
                : field.default ?? field.min ?? 0
            }
            onChange={(event, newValue) => onChange(newValue as number)}
            valueLabelDisplay="auto"
            step={field.step ?? 1}
            marks={!!field.step}
            min={field.min ?? 0}
            max={field.max ?? 100}
            disabled={disabled}
            sx={{
              color: '#3b956f',
              '& .MuiSlider-thumb': {
                '&:hover, &.Mui-focusVisible': {
                  boxShadow: '0 0 0 8px rgba(59, 149, 111, 0.16)',
                },
              },
              '& .MuiSlider-valueLabel': {
                backgroundColor: '#3b956f',
              },
            }}
          />
          {(error || field.helperText) && (
            <FormHelperText 
              error={!!error}
              sx={{ 
                color: !!error ? '#d32f2f' : '#a3a3a3',
              }}
            >
              {error || field.helperText}
            </FormHelperText>
          )}
        </Box>
      );
    case "color":
      return (
        <FormControl fullWidth size="small" sx={{ width: '100%' }}>
          <Typography
            variant="body2"
            sx={{ mb: 1, color: disabled ? "text.disabled" : "#ffffff" }}
          >
            {field.label}
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center", width: '100%' }}>
            <input
              type="color"
              id={`color-input-${field.id}`}
              disabled={disabled}
              value={value ?? field.default ?? "#000000"}
              onChange={(e) => onChange(e.target.value)}
              style={{
                border: "1px solid #525252",
                borderRadius: "4px",
                padding: 0,
                height: "40px",
                width: "45px",
                marginRight: "10px",
                cursor: disabled ? "not-allowed" : "pointer",
                background: "none",
              }}
            />
            <TextField
              size="small"
              variant="outlined"
              disabled={disabled}
              sx={{ 
                flexGrow: 1,
                '& .MuiOutlinedInput-root': {
                  color: '#ffffff',
                  '& fieldset': {
                    borderColor: '#525252',
                  },
                },
              }}
              value={value ?? field.default ?? "#000000"}
              onChange={(e) => onChange(e.target.value)}
              error={!!error}
              inputProps={{
                style: { color: '#ffffff' }
              }}
            />
          </Box>
          {(error || field.helperText) && (
            <FormHelperText 
              error={!!error}
              sx={{ 
                color: !!error ? '#d32f2f' : '#a3a3a3',
              }}
            >
              {error || field.helperText}
            </FormHelperText>
          )}
        </FormControl>
      );
    case "file":
      return (
        <Box sx={{ width: '100%' }}>
          <Typography variant="body2" sx={{ mb: 1, color: '#ffffff' }}>
            {field.label}
          </Typography>
          <Button
            component="label"
            variant="outlined"
            size="small"
            disabled={disabled}
            sx={{
              borderColor: '#525252',
              color: '#ffffff',
              '&:hover': {
                borderColor: '#3b956f',
                backgroundColor: 'rgba(59, 149, 111, 0.1)',
              },
            }}
          >
            Upload File
            <input
              type="file"
              hidden
              accept={field.accept || "*/*"}
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  const file = e.target.files[0];
                  if (file.size > 5 * 1024 * 1024) {
                    alert("File is too large. Maximum size is 5MB.");
                    return;
                  }
                  const reader = new FileReader();
                  reader.onloadend = () => {
                    const result = reader.result as string;
                    if (
                      typeof result === "string" &&
                      result.startsWith("data:")
                    ) {
                      console.log(
                        `File read successfully, length: ${result.length}`
                      );
                      onChange(result);
                    } else {
                      console.error("Invalid file data format");
                      onChange(null);
                    }
                  };
                  reader.onerror = () => {
                    console.error("File reading error");
                    onChange(null);
                  };
                  reader.readAsDataURL(file);
                }
              }}
            />
          </Button>
          {typeof value === "string" && value.startsWith("data:image") && (
            <img
              src={value}
              alt="Preview"
              height="50"
              style={{ marginLeft: "10px", verticalAlign: "middle" }}
            />
          )}
          {(error || field.helperText) && (
            <FormHelperText 
              error={!!error}
              sx={{ 
                color: !!error ? '#d32f2f' : '#a3a3a3',
              }}
            >
              {error || field.helperText}
            </FormHelperText>
          )}
        </Box>
      );
    case "button":
      return (
        <Button
          id={`button-${field.id}`}
          variant="contained"
          size="small"
          color={
            (field.color as
              | "primary"
              | "secondary"
              | "error"
              | "info"
              | "success"
              | "warning"
              | undefined) || "primary"
          }
          onClick={() => {
            if (field.action) {
              console.log(`Button clicked: ${field.action}`);
            } else {
              alert(
                `Action '${field.action}' clicked (needs specific frontend logic)`
              );
            }
          }}
          disabled={disabled}
          sx={{
            backgroundColor: "#3b956f",
            '&:hover': {
              backgroundColor: "#2d7d5b"
            }
          }}
        >
          {field.label}
        </Button>
      );
    case "hidden":
      return null;
    case "webcamPreview":
      return (
        <Box
          sx={{
            border: "1px dashed #525252",
            minHeight: 200,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            bgcolor: "black",
            width: '100%'
          }}
        >
          <Typography sx={{ color: '#525252' }}>
            Webcam Preview Area (Requires Frontend JS)
          </Typography>
        </Box>
      );
    default:
      return (
        <Typography color="error">
          Unsupported input type: {field.type}
        </Typography>
      );
  }
};

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
  disabled,
}) => {
  const theme = useTheme();
  const handleCopy = () => {
    const textToCopy =
      typeof value === "object"
        ? JSON.stringify(value, null, 2)
        : String(value ?? "");
    navigator.clipboard
      .writeText(textToCopy)
      .catch((err) => console.error("Copy failed", err));
  };
  const handleDownload = () => {
    const filenameKey = output.downloadFilenameKey || "imageFileName";
    const filename =
      resultData?.[filenameKey] || `${output.id || "download"}.png`;
    downloadDataUrl(String(value), filename);
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
  const buttonElements = (
    <Box
      sx={{ mt: 1, display: "flex", gap: 0.5, justifyContent: "flex-start" }}
    >
      {output.buttons?.includes("copy") && isValueNotNull && (
        <IconButton 
          size="small" 
          title="Copy" 
          onClick={handleCopy}
          sx={{ color: theme.palette.custom?.icon }}
        >
          <ContentCopyIcon sx={{ fontSize: "1rem" }} />
        </IconButton>
      )}
      {output.buttons?.includes("refresh") && onRefresh && (
        <IconButton
          size="small"
          title="Refresh/Regenerate"
          onClick={onRefresh}
          disabled={disabled}
          sx={{ color: theme.palette.custom?.icon }}
        >
          <RefreshIcon sx={{ fontSize: "1rem" }} />
        </IconButton>
      )}
      {output.buttons?.includes("download") &&
        output.type === "image" &&
        isValueStringImage && (
          <IconButton 
            size="small" 
            title="Download" 
            onClick={handleDownload}
            sx={{ color: theme.palette.custom?.icon }}
          >
            <DownloadIcon sx={{ fontSize: "1rem" }} />
          </IconButton>
        )}
    </Box>
  );
  const renderContent = () => {
    switch (output.type) {
      case "text":
        return (
          <Typography
            variant="body1"
            component="div"
            sx={{
              whiteSpace: "pre-wrap",
              wordBreak: "break-all",
              fontFamily: output.monospace ? "monospace" : "inherit",
              color: output.style === "error" ? theme.palette.error.main : "#ffffff",
              width: '100%',
            }}
          >
            {String(displayValue)}
          </Typography>
        );
      case "image":
        return (
          <Box
            sx={{
              display: "flex",
              justifyContent: "flex-start",
              alignItems: "center",
              width: "100%",
              maxWidth: output.maxWidth || { xs: '100%', sm: '350px', md: '450px' },
              height: "auto",
              border: "1px solid",
              borderColor: "#525252",
              borderRadius: 1,
              p: 0.5,
              mt: 1,
              overflow: "auto",
            }}
          >
            {displayValue ? (
              <img
                src={String(displayValue)}
                alt={output.label || "Output Image"}
                style={{
                  display: "block",
                  width: "100%",
                  height: "auto",
                  objectFit: "contain",
                  maxHeight: output.maxHeight || "500px",
                }}
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display = "none";
                }}
              />
            ) : (
              <Typography
                sx={{ p: 2, fontStyle: "italic", color: "#a3a3a3" }}
              >
                No Image
              </Typography>
            )}
          </Box>
        );
      case "json":
        return (
          <Box
            component="pre"
            sx={{
              bgcolor: "#2e2e2e",
              color: "#ffffff",
              p: 1.5,
              borderRadius: 1,
              overflowX: "auto",
              overflowY: "auto",
              fontSize: "0.875rem",
              maxHeight: { xs: "200px", sm: "300px", md: "400px" },
              width: '100%',
              wordBreak: "break-all",
              whiteSpace: "pre-wrap",
              mt: 1,
            }}
          >
            {JSON.stringify(displayValue, null, 2)}
          </Box>
        );
      case "boolean":
        return displayValue ? (
          <CheckCircleIcon color="success" sx={{ verticalAlign: "middle" }} />
        ) : (
          <CancelIcon color="error" sx={{ verticalAlign: "middle" }} />
        );
      case "chips":
        return isArrayValue ? (
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1, mt: 1, width: '100%' }}>
            {displayValue.map((item, i) => (
              <Chip 
                key={i} 
                label={String(item)} 
                size="small"
                sx={{
                  backgroundColor: 'rgba(59, 149, 111, 0.1)',
                  color: '#ffffff',
                  border: '1px solid #3b956f'
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
          <List dense sx={{ py: 0, listStyle: "disc", pl: 2.5, width: '100%' }}>
            {displayValue.map((item, i) => (
              <ListItem
                key={i}
                disableGutters
                sx={{ py: 0, display: "list-item" }}
              >
                <ListItemText
                  primary={String(item)}
                  primaryTypographyProps={{ 
                    variant: "body2",
                    sx: { color: '#ffffff' }
                  }}
                />
              </ListItem>
            ))}
          </List>
        ) : (
          <Typography color="error" variant="caption">
            Invalid data
          </Typography>
        );
      case "progressBar":
        return (
          <Box sx={{ display: "flex", alignItems: "center", mt: 1, width: '100%' }}>
            <Box sx={{ width: "100%", mr: 1 }}>
              <LinearProgress
                variant="determinate"
                value={progressValue}
                color={progressColor}
                sx={{ height: 10, borderRadius: 5 }}
              />
            </Box>
            <Box sx={{ minWidth: 50 }}>
              <Typography 
                variant="body2" 
                sx={{ color: "#a3a3a3" }}
              >
                {`${Math.round(progressValue)}${suffix}`}
              </Typography>
            </Box>
          </Box>
        );
      case "table":
        if (hasTableConfig) {
          if ((displayValue as any[]).length === 0) {
            return (
              <Typography
                variant="body2"
                sx={{ fontStyle: "italic", color: "#a3a3a3", mt: 1 }}
              >
                No data.
              </Typography>
            );
          }
          return (
            <TableContainer
              component={Paper}
              elevation={0}
              variant="outlined"
              sx={{ 
                mt: 1,
                backgroundColor: '#232323',
                borderColor: '#525252',
                width: '100%',
                overflowX: 'auto',
              }}
            >
              <Table size="small">
                <TableHead
                  sx={{
                    bgcolor: "#2e2e2e",
                  }}
                >
                  <TableRow>
                    {output.columns!.map((col, cIndex) => (
                      <TableCell
                        key={`${output.id}-h-${cIndex}`}
                        sx={{ 
                          fontWeight: "bold",
                          color: '#ffffff'
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
                        '&:hover': {
                          backgroundColor: 'rgba(59, 149, 111, 0.1)'
                        }
                      }}
                    >
                      {output.columns!.map((col, cIndex) => (
                        <TableCell 
                          key={`${output.id}-c-${rIndex}-${cIndex}`}
                          sx={{ color: '#ffffff' }}
                        >
                          {String(
                            col.field
                              .split(".")
                              .reduce(
                                (o, k) =>
                                  o && typeof o === "object" ? o[k] : undefined,
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
    <Box sx={{ mb: 1.5, width: '100%' }}>
      {output.label && (
        <Typography
          variant="body2"
          sx={{ mb: 0.5, fontWeight: "medium", color: "#a3a3a3" }}
        >
          {output.label}
        </Typography>
      )}
      {renderContent()}
      {buttonElements}
    </Box>
  );
};

export default ToolRenderer;