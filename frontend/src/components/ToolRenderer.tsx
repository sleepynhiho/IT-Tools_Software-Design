import React, { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import debounce from 'lodash.debounce'; // Ensure installed: npm install lodash.debounce @types/lodash.debounce

// MUI Imports
import {
    Box, Typography, Paper, TextField, Select, MenuItem, FormControl, InputLabel,
    Button, FormHelperText, CircularProgress, Grid, Card, CardContent, CardHeader,
    Switch, FormControlLabel, Slider, CssBaseline, Container, Alert, IconButton,
    Chip, LinearProgress, List, ListItem, ListItemText, ListItemIcon,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow
} from "@mui/material";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import RefreshIcon from '@mui/icons-material/Refresh';
import DownloadIcon from '@mui/icons-material/Download';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import LabelIcon from '@mui/icons-material/Label'; // Example list icon
import { useTheme } from '@mui/material/styles';

// Local Imports
import CommonLayout from "../layouts/CommonLayout"; // Adjust path if needed
import { PluginMetadata, Section, InputField, OutputField, useAllPluginMetadata } from "../data/pluginList"; // Adjust path, ensure interfaces match NEW format
// NOTE: Removed lodash 'groupBy' import as the rendering logic simplifies it.

// --- Configuration ---
const API_BASE_URL = "http://192.168.192.2:8081"; // Use your actual backend URL
// --- End Configuration ---


// --- Helper Functions ---

// Helper function for file download (used for image output)
function downloadDataUrl(dataUrl: string, filename: string) {
    try {
        if (!dataUrl || typeof dataUrl !== 'string' || !dataUrl.startsWith('data:')) {
            console.error("Invalid data URL provided for download:", dataUrl);
            alert("Could not initiate download: Invalid image data.");
            return;
        }
        const link = document.createElement('a');
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

// --- Main Component ---
const ToolRenderer: React.FC = () => {
    // --- Hooks ---
    const { id: toolIdFromUrl } = useParams<{ id: string }>();
    const {
        metadataList: allPluginMetadata,
        loading: loadingMetadataList,
        error: metadataListError,
    } = useAllPluginMetadata(); // Hook expecting NEW format

    const [metadata, setMetadata] = useState<PluginMetadata | null>(null);
    const [loadingMetadata, setLoadingMetadata] = useState<boolean>(true);
    const [toolMetadataError, setToolMetadataError] = useState<string | null>(null);
    const [formData, setFormData] = useState<Record<string, any>>({});
    const [formErrors, setFormErrors] = useState<Record<string, string>>({});
    const [isProcessing, setIsProcessing] = useState<boolean>(false);
    const [resultData, setResultData] = useState<Record<string, any> | null>(null);
    const theme = useTheme(); // Theme hook called unconditionally

    // --- Condition Evaluation ---
    const evaluateCondition = useCallback((condition?: string, currentFormData = formData, currentResultData = resultData ): boolean => {
        if (!condition) return true;
        const context = { ...currentFormData, ...(currentResultData || {}) };
        try {
            const func = new Function(...Object.keys(context), `try { return !!(${condition}); } catch (e) { console.error('Cond Eval Err:', condition, e); return false; }`);
            const result = func(...Object.values(context));
            return result;
        } catch (e) {
            console.error('Error evaluating condition:', condition, e);
            return false;
        }
    }, [formData, resultData]);

    // --- API Call Logic ---
    const processRequest = useCallback(async (currentFormData: Record<string, any>) => {
        if (!metadata?.id) {
            console.log("Process skipped: No valid metadata loaded.");
            setResultData({ success: false, errorMessage: "Tool metadata not loaded."});
            return;
        }

        let clientSideValid = true;
        const tempErrors: Record<string, string> = {};
         metadata.sections?.forEach(section => {
             section.inputs?.forEach(field => {
                 if (evaluateCondition(field.condition, currentFormData, resultData)) {
                     if (field.required) {
                         const value = currentFormData[field.id];
                         if (value === undefined || value === null || String(value).trim() === '') {
                             clientSideValid = false; tempErrors[field.id] = `${field.label || field.id} is required.`;
                         }
                     }
                      if ((field.type === 'number' || field.type === 'slider') && currentFormData[field.id] != null && currentFormData[field.id] !== '') {
                           const numValue = Number(currentFormData[field.id]);
                           if (isNaN(numValue)) { clientSideValid = false; tempErrors[field.id] = `Must be a valid number.`; }
                           else {
                               if (field.min !== undefined && numValue < field.min) { clientSideValid = false; tempErrors[field.id] = `Min value: ${field.min}.`; }
                               if (field.max !== undefined && numValue > field.max) { clientSideValid = false; tempErrors[field.id] = `Max value: ${field.max}.`; }
                           }
                      }
                 }
             });
         });

         setFormErrors(tempErrors);
         if (!clientSideValid) {
             console.log("Process skipped: Client-side validation failed.", tempErrors);
             setResultData({ success: false, errorMessage: "Please fix the errors in the input fields." });
             setIsProcessing(false);
             return;
         }

        setIsProcessing(true);
        const processURL = `${API_BASE_URL}/api/debug/${metadata.id}/process`;

        try {
            const payload: Record<string, any> = {};
             metadata.sections?.forEach(section => {
                  section.inputs?.forEach(field => {
                      if (evaluateCondition(field.condition, currentFormData, resultData)) {
                          const value = currentFormData[field.id];
                          if ((field.type === 'number' || field.type === 'slider') && typeof value === 'string' && value.trim() !== '') {
                               const numVal = Number(value);
                               payload[field.id] = isNaN(numVal) ? null : numVal;
                          } else if (field.type === 'number' || field.type === 'slider') {
                               payload[field.id] = value ?? null;
                          } else {
                               payload[field.id] = value;
                           }
                      }
                  });
              });
            console.log(`Processing request to: ${processURL} with payload:`, payload);

            const response = await fetch(processURL, { method: "POST", headers: { "Content-Type": "application/json", "Accept": "application/json" }, body: JSON.stringify(payload) });
            const result = await response.json();
            console.log("Backend process response:", result);

            const errorKey = metadata.sections?.flatMap(s => s.outputs ?? []).find(o => o.style === 'error' && o.type === 'text')?.id || 'errorMessage';

            if (!response.ok || result.success === false) {
                setResultData({ success: false, [errorKey]: result.errorMessage || result.error || `Request failed: ${response.status}` });
            } else {
                setResultData({ success: true, ...result });
            }
        } catch (error: any) {
            console.error("Process request failed:", error);
             const errorKey = metadata.sections?.flatMap(s => s.outputs ?? []).find(o => o.style === 'error' && o.type === 'text')?.id || 'errorMessage';
            setResultData({ success: false, [errorKey]: error.message || "Network error." });
        } finally {
            setIsProcessing(false);
        }
    }, [metadata, resultData, evaluateCondition]); // evaluateCondition added

    const debouncedProcessRequest = useCallback(debounce(processRequest, 500), [processRequest]);

    // --- Effects ---
    useEffect(() => {
        if (loadingMetadataList) { setLoadingMetadata(true); return; }
        if (metadataListError) { setToolMetadataError(metadataListError); setLoadingMetadata(false); return; }
        if (!Array.isArray(allPluginMetadata)) { setToolMetadataError("Invalid plugin list format."); setLoadingMetadata(false); return; }

        console.log(`Finding metadata for id: ${toolIdFromUrl} in list of ${allPluginMetadata.length}`);
        const toolMetadata = allPluginMetadata.find((tool) => tool.id === toolIdFromUrl);

        if (toolMetadata) {
             if (!metadata || metadata.id !== toolMetadata.id) {
                console.log("Metadata found / changed:", toolMetadata);
                setMetadata(toolMetadata);
                setToolMetadataError(null);
                setResultData(null);
                setFormErrors({});

                const initialFormData: Record<string, any> = {};
                toolMetadata.sections?.forEach((section) => {
                    section.inputs?.forEach((input) => {
                        const key = input.id;
                        if (input.default !== undefined) initialFormData[key] = input.default;
                        else if (input.type === 'switch') initialFormData[key] = false;
                        else if (input.type === 'slider') initialFormData[key] = input.min ?? 0;
                        else if (input.type === 'number') initialFormData[key] = input.min ?? 0;
                        else if (input.type === 'color') initialFormData[key] = input.default ?? '#000000';
                        else initialFormData[key] = '';
                    });
                });
                console.log("Initializing formData:", initialFormData);
                setFormData(initialFormData);

                if (toolMetadata.triggerUpdateOnChange === true && Object.keys(initialFormData).length > 0) {
                    console.log(`Triggering initial process request for ${toolMetadata.id}`);
                    setTimeout(() => processRequest(initialFormData), 50); // Short timeout
                }
             }
            setLoadingMetadata(false);
        } else {
            console.error(`Metadata for id "${toolIdFromUrl}" not found.`);
            setMetadata(null);
            setToolMetadataError(`Tool with ID "${toolIdFromUrl}" not found.`);
            setFormData({});
            setResultData(null);
            setLoadingMetadata(false);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [toolIdFromUrl, allPluginMetadata, loadingMetadataList, metadataListError, metadata?.id]); // Removed processRequest, added metadata.id

     // Trigger initial request *after* form data is set and if dynamic
     useEffect(() => {
         if (metadata?.triggerUpdateOnChange === true && Object.keys(formData).length > 0 && !isProcessing && !resultData) {
             console.log("Effect Triggering initial process request with formData:", formData);
             processRequest(formData);
         }
      // eslint-disable-next-line react-hooks/exhaustive-deps
     }, [formData, metadata?.triggerUpdateOnChange, processRequest]); // Trigger when formData is set


    const handleInputChange = useCallback(
        (inputId: string, value: any) => {
            const newFormData = { ...formData, [inputId]: value };
            setFormData(newFormData);
            if (formErrors[inputId]) { setFormErrors(prev => { const newState = {...prev}; delete newState[inputId]; return newState; }); }
            if (metadata?.triggerUpdateOnChange === true) {
                console.log(`Debouncing process request due to change in ${inputId}`);
                debouncedProcessRequest(newFormData);
            }
        },
        [formData, formErrors, metadata, debouncedProcessRequest]
    );

    const handleExecute = useCallback(() => {
        console.log("Manual Execute/Refresh Triggered");
        if (metadata && Object.keys(formData).length > 0) {
            processRequest(formData);
        }
    }, [formData, metadata, processRequest]);

    // --- Render Logic ---

    if (loadingMetadataList || loadingMetadata) {
        return (
            <CommonLayout title="Loading..." description="" toolId={toolIdFromUrl || ""} icon="">
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress /></Box>
            </CommonLayout>
        );
    }

    const displayError = toolMetadataError || metadataListError;
    if (displayError || !metadata) {
        return (
            <CommonLayout title="Error" description="" toolId={toolIdFromUrl || ""} icon="">
                <Alert severity="error" sx={{ m: 2 }}>{displayError || "Tool metadata could not be loaded."}</Alert>
            </CommonLayout>
        );
    }

    const errorOutputFieldId = metadata.sections?.flatMap(s => s.outputs ?? []).find(o => o.style === 'error' && o.type === 'text')?.id || 'errorMessage';

    return (
        <CommonLayout
            title={metadata.name}
            description={metadata.description}
            toolId={metadata.id}
            icon={metadata.icon}
        >
            {/* Display Processing Errors */}
            {resultData?.success === false && resultData[errorOutputFieldId] && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setResultData(prev => ({...prev, success: undefined, [errorOutputFieldId]: null}))}>
                    {String(resultData[errorOutputFieldId])}
                </Alert>
            )}

            <Grid container spacing={3}>
                {/* Render Sections */}
                {metadata.sections?.filter(section => evaluateCondition(section.condition)).map((section: Section, index: number) => (
                    <Grid item xs={12} key={`${section.id}-${index}`}>
                        {/* Render Inputs */}
                        {section.inputs && section.inputs.filter(field => evaluateCondition(field.condition)).length > 0 && (
                             <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }} elevation={2}>
                                 {section.label && ( <Typography variant="h6" sx={{ mb: 2.5, borderBottom: 1, borderColor: 'divider', pb: 1 }}>{section.label}</Typography> )}
                                 <Grid container spacing={2.5}>
                                      {section.inputs.filter(field => evaluateCondition(field.condition)).map((field: InputField) => (
                                           // Using Grid v2 props directly
                                           <Grid xs={12} sm={(field.type === 'switch' || field.type === 'color' || field.type === 'slider') ? 6 : 12} md={(field.type === 'color' || field.type === 'switch') ? 4 : (field.type === 'slider' ? 8 : 12)} key={field.id}>
                                               <RenderInput
                                                    field={field}
                                                    value={formData[field.id]}
                                                    onChange={(value) => handleInputChange(field.id, value)}
                                                    disabled={isProcessing}
                                                    error={formErrors[field.id]}
                                                />
                                           </Grid>
                                      ))}
                                 </Grid>
                             </Paper>
                         )}

                        {/* Render Outputs */}
                         {section.outputs && section.outputs.filter(field => evaluateCondition(field.condition, formData, resultData)).length > 0 && evaluateCondition(section.condition, formData, resultData) && (
                              (resultData || isProcessing || section.id === 'errorDisplay') && (
                                   <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }} elevation={2}>
                                        {section.label && ( <Typography variant="h6" sx={{ mb: 2.5, borderBottom: 1, borderColor: 'divider', pb: 1 }}>{section.label}</Typography> )}
                                        {(resultData?.success === true || resultData?.success === false) && section.outputs.filter(field => evaluateCondition(field.condition, formData, resultData)).map((output: OutputField) => {
                                            const outputValue = resultData?.[output.id];
                                            const isErrorField = output.id === errorOutputFieldId;
                                            const shouldRender = (resultData?.success === false && isErrorField && outputValue !== undefined) || // Show error field if error exists and value present
                                                               (resultData?.success === true && outputValue !== undefined && outputValue !== null); // Show success field if success and value exists

                                            if (shouldRender) {
                                                 return ( <Box key={output.id} sx={{ mb: 2 }}> {RenderOutput({ output: output, value: outputValue, resultData: resultData, onRefresh: handleExecute, disabled: isProcessing })} </Box> );
                                            }
                                            return null;
                                        })}
                                         {isProcessing && section.id !== 'errorDisplay' && (
                                             <Box sx={{ display: 'flex', alignItems: 'center', pt: 1 }}>
                                                 <CircularProgress size={20} /> <Typography variant="body2" sx={{ ml: 1 }}>Processing...</Typography>
                                             </Box>
                                         )}
                                   </Paper>
                              )
                         )}
                    </Grid>
                ))}
            </Grid>

           {/* Manual Process Button */}
           <Button
               variant="contained" color="primary" onClick={handleExecute} disabled={isProcessing || loadingMetadata} sx={{ mt: 1 }}
               startIcon={isProcessing ? <CircularProgress size={20} color="inherit"/> : <RefreshIcon />} >
               {isProcessing ? 'Processing...' : (metadata.triggerUpdateOnChange ? 'Regenerate Manually' : 'Process')}
           </Button>

        </CommonLayout>
    );
};


// ========================================================================
// Inline RenderInput and RenderOutput Components
// ========================================================================

interface RenderInputProps {
    field: InputField;
    value: any;
    onChange: (value: any) => void;
    disabled?: boolean;
    error?: string;
}

// Define RenderInput as a functional component
const RenderInput: React.FC<RenderInputProps> = ({ field, value, onChange, disabled, error }) => {
    const commonProps = {
        fullWidth: true, size: "small", variant: "outlined", label: field.label,
        disabled: disabled, required: field.required, error: !!error,
        helperText: error || field.helperText, placeholder: field.placeholder,
    } as const;

    switch (field.type) {
        case 'text':
        case 'password':
            return ( <TextField {...commonProps} type={field.type} multiline={field.multiline} rows={field.rows} value={value ?? ''} onChange={(e) => onChange(e.target.value)} /> );
        case 'number':
            return ( <TextField {...commonProps} type="number" value={value ?? ''} onChange={(e) => onChange(e.target.value)} inputProps={{ min: field.min, max: field.max, step: field.step ?? 'any' }} /> );
        case 'select':
            return (
                <FormControl fullWidth size="small" variant="outlined" disabled={disabled} required={field.required} error={!!error}>
                    <InputLabel>{field.label}</InputLabel>
                    <Select label={field.label} value={value ?? field.default ?? ''} onChange={(e) => onChange(e.target.value)}>
                        {!field.required && field.default === undefined && <MenuItem value=""><em>None</em></MenuItem>}
                        {field.options?.map((option: any, index: number) => ( <MenuItem key={index} value={typeof option === 'object' ? option.value : option}>{typeof option === 'object' ? option.label : option}</MenuItem> ))}
                    </Select>
                    {(error || field.helperText) && ( <FormHelperText error={!!error}>{error || field.helperText}</FormHelperText> )}
                </FormControl>
            );
        case 'switch':
             return (
                 <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', minHeight: '40px' }}>
                     <Typography variant="body2" sx={{ color: disabled ? 'text.disabled' : 'inherit', mr: 1 }}>{field.label || ''}</Typography>
                     <Switch name={field.id} checked={!!value} onChange={(e) => onChange(e.target.checked)} disabled={disabled} />
                     {/* Render HelperText below */}
                      {(error || field.helperText) && (<FormHelperText error={!!error} sx={{ width: '100%', textAlign: 'left', mt: -1, position:'relative', top:'10px'}}>{error || field.helperText}</FormHelperText>)}
                 </Box>
             );
        case 'slider':
            return (
                <Box sx={{ px: 1 }}>
                    <Typography gutterBottom id={`${field.id}-label`}>{field.label} ({value ?? field.default ?? field.min})</Typography>
                    <Slider aria-labelledby={`${field.id}-label`} value={typeof value === 'number' ? value : (field.default ?? field.min ?? 0)} onChange={(event, newValue) => onChange(newValue as number)} valueLabelDisplay="auto" step={field.step ?? 1} marks={!!field.step} min={field.min ?? 0} max={field.max ?? 100} disabled={disabled} />
                    {(error || field.helperText) && ( <FormHelperText error={!!error}>{error || field.helperText}</FormHelperText> )}
                </Box>
            );
        case 'color':
             return (
                 <FormControl fullWidth size="small">
                    <Typography variant="body2" sx={{mb: 1, color: disabled ? 'text.disabled' : 'inherit'}}>{field.label}</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <input type="color" id={`color-input-${field.id}`} disabled={disabled} value={value ?? field.default ?? '#000000'} onChange={e => onChange(e.target.value)} style={{ border: '1px solid #ccc', borderRadius: '4px', padding: 0, height: '40px', width: '45px', marginRight: '10px', cursor: disabled ? 'not-allowed' : 'pointer', background: 'none' }} />
                         <TextField size="small" variant="outlined" disabled={disabled} sx={{flexGrow: 1}} value={value ?? field.default ?? '#000000'} onChange={e => onChange(e.target.value)} error={!!error} />
                    </Box>
                    {(error || field.helperText) && ( <FormHelperText error={!!error}>{error || field.helperText}</FormHelperText> )}
                </FormControl>
             );
         case 'file': // Basic File Input
              return (
                  <Box>
                      <Typography variant="body2" sx={{mb: 1}}>{field.label}</Typography>
                      <Button component="label" variant="outlined" size="small" disabled={disabled}>
                          Upload File
                          <input type="file" hidden accept={field.accept || '*/*'} onChange={(e) => {
                              if (e.target.files && e.target.files[0]) {
                                  const file = e.target.files[0];
                                  const reader = new FileReader();
                                  reader.onloadend = () => { onChange(reader.result); };
                                  reader.onerror = () => { console.error("File reading error"); onChange(null); }
                                  reader.readAsDataURL(file);
                              } else { onChange(null); } // Clear value if no file selected
                          }} />
                      </Button>
                       {/* Basic preview for images */}
                       {typeof value === 'string' && value.startsWith('data:image') && <img src={value} alt="Preview" height="50" style={{marginLeft: '10px', verticalAlign: 'middle'}}/>}
                       {(error || field.helperText) && ( <FormHelperText error={!!error}>{error || field.helperText}</FormHelperText> )}
                  </Box>
              );
         case 'button': // Placeholder for frontend action button
              return ( <Button variant="contained" size="small" onClick={() => alert(`Action '${field.action}' clicked (needs specific frontend logic)`)} disabled={disabled}>{field.label}</Button> );
         case 'hidden': return null;
         case 'webcamPreview':
              return ( <Box sx={{ border: '1px dashed grey', minHeight: 200, display: 'flex', alignItems:'center', justifyContent:'center', bgcolor:'black'}}><Typography sx={{color: 'grey'}}>Webcam Preview Area (Requires Frontend JS)</Typography></Box> );

        default: return <Typography color="error">Unsupported input type: {field.type}</Typography>;
    }
};

// --- Inline RenderOutput ---
interface RenderOutputProps {
    output: OutputField;
    value: any;
    resultData?: Record<string, any> | null;
    onRefresh?: () => void;
    disabled?: boolean;
}

// Define RenderOutput as functional component
const RenderOutput: React.FC<RenderOutputProps> = ({ output, value, resultData, onRefresh, disabled }) => {
    const theme = useTheme();
    const handleCopy = () => {
         const textToCopy = typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value ?? '');
         navigator.clipboard.writeText(textToCopy).catch(err => console.error('Copy failed', err));
    };
    const handleDownload = () => {
         const filenameKey = output.downloadFilenameKey || 'imageFileName'; // Key from metadata or default
         const filename = resultData?.[filenameKey] || `${output.id || 'download'}.png`;
         downloadDataUrl(String(value), filename);
    };

    const renderButtons = () => {
         const placement = 'outside'; // Simplify to always outside
         const buttons = (
             <Box sx={{ mt: 1, display: 'flex', gap: 0.5, justifyContent: 'flex-start' }}> {/* Align buttons left */}
                 {output.buttons?.includes('copy') && value != null && ( <IconButton size="small" title="Copy" onClick={handleCopy}><ContentCopyIcon sx={{ fontSize: '1rem'}}/></IconButton> )}
                 {output.buttons?.includes('refresh') && onRefresh && ( <IconButton size="small" title="Refresh/Regenerate" onClick={onRefresh} disabled={disabled}><RefreshIcon sx={{ fontSize: '1rem'}}/></IconButton> )}
                 {output.buttons?.includes('download') && output.type === 'image' && value && typeof value === 'string' && value.startsWith('data:image') && ( <IconButton size="small" title="Download" onClick={handleDownload}><DownloadIcon sx={{ fontSize: '1rem'}}/></IconButton> )}
             </Box>
         );
         return { buttons, placement };
    };

    const { buttons, placement } = renderButtons();

    const mainContent = () => {
        const displayValue = value ?? ''; // Default empty string
        switch(output.type) {
            case 'text':
                 return ( <Typography variant="body1" component="div" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontFamily: output.monospace ? 'monospace' : 'inherit', color: output.style === 'error' ? theme.palette.error.main : 'inherit' }}>{String(displayValue)}</Typography> );
            case 'image':
                  return (
                      <Box sx={{ display: 'flex', justifyContent: 'flex-start', alignItems: 'center', width: 'fit-content', maxWidth: output.maxWidth || 350, maxHeight: output.maxHeight || 350, border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 0.5, mt: 1 }}>
                          {displayValue ? <img src={String(displayValue)} alt={output.label || 'Output Image'} style={{ display: 'block', width: '100%', height: 'auto', objectFit: 'contain' }} onError={(e) => { (e.target as HTMLImageElement).style.display='none'; }} /> : <Typography sx={{p:2, fontStyle:'italic', color:'text.secondary'}}>No Image</Typography>}
                       </Box>
                   );
            case 'json':
                 return ( <Box component="pre" sx={{ bgcolor: theme.palette.mode === 'dark' ? '#2e2e2e' : '#f5f5f5', color: theme.palette.mode === 'dark' ? '#fff' : '#000', p: 1.5, borderRadius: 1, overflowX: 'auto', fontSize: '0.875rem', maxHeight: '400px', wordBreak: 'break-all', whiteSpace: 'pre-wrap', mt: 1 }}>{JSON.stringify(displayValue, null, 2)}</Box> );
            case 'boolean':
                 return displayValue ? <CheckCircleIcon color="success" sx={{ verticalAlign: 'middle'}}/> : <CancelIcon color="error" sx={{ verticalAlign: 'middle'}}/>;
            case 'chips':
                 return Array.isArray(displayValue) ? ( <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>{displayValue.map((item, i) => <Chip key={i} label={String(item)} size="small" />)}</Box> ) : <Typography color="error" variant="caption">Invalid data</Typography>;
            case 'list':
                  return Array.isArray(displayValue) ? (
                      <List dense sx={{py: 0, listStyle: 'disc', pl: 2.5 /* Indent list items */}}>
                          {displayValue.map((item, i) => ( <ListItem key={i} disableGutters sx={{py: 0, display: 'list-item'}}><ListItemText primary={String(item)} primaryTypographyProps={{variant: 'body2'}}/></ListItem> ))}
                      </List>
                  ) : <Typography color="error" variant="caption">Invalid data</Typography>;
            case 'progressBar':
                 const progressValue = typeof displayValue === 'number' ? Math.min(Math.max(displayValue, output.min ?? 0), output.max ?? 100) : 0;
                 const score = typeof displayValue === 'number' ? displayValue : 0;
                 const color = score >= 90 ? 'success' : (score >= 70 ? 'info' : (score >= 50 ? 'warning' : 'error'));
                 const suffix = output.suffix || '';
                 return (
                     <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                          <Box sx={{ width: '100%', mr: 1 }}><LinearProgress variant="determinate" value={progressValue} color={color} sx={{height: 10, borderRadius: 5}}/></Box>
                          <Box sx={{ minWidth: 50 }}><Typography variant="body2" color="text.secondary">{`${Math.round(progressValue)}${suffix}`}</Typography></Box>
                      </Box>
                  );
             case 'table':
                 if (Array.isArray(displayValue) && output.columns && output.columns.length > 0) {
                      if (displayValue.length === 0) { return <Typography variant="body2" sx={{ fontStyle: 'italic', color: 'text.secondary', mt: 1 }}>No data.</Typography>; }
                     return (
                         <TableContainer component={Paper} elevation={0} variant="outlined" sx={{ mt: 1 }}>
                             <Table size="small">
                                 <TableHead sx={{ bgcolor: 'action.hover' }}>
                                     <TableRow>{output.columns.map((col, cIndex) => ( <TableCell key={`${output.id}-h-${cIndex}`} sx={{ fontWeight: 'bold' }}>{col.header}</TableCell> ))}</TableRow>
                                 </TableHead>
                                 <TableBody>
                                     {displayValue.map((row: any, rIndex: number) => (
                                         <TableRow key={`${output.id}-r-${rIndex}`} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                             {output.columns!.map((col, cIndex) => ( <TableCell key={`${output.id}-c-${rIndex}-${cIndex}`}>{String(col.field.split('.').reduce((o, k) => (o && typeof o === 'object' ? o[k] : undefined), row) ?? '')}</TableCell> ))}
                                         </TableRow>
                                     ))}
                                 </TableBody>
                             </Table>
                         </TableContainer>
                     );
                 } else { return <Typography color="error" variant="caption">Invalid table data/config</Typography>; }

             default: return <Typography color="error" variant="caption">Unsupported type: {output.type}</Typography>;
        }
    };

    return (
         <Box sx={{mb: 1.5}}>
             {output.label && ( <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 'medium', color: 'text.secondary' }}>{output.label}</Typography> )}
            {mainContent()}
            {buttons} {/* Render buttons outside */}
        </Box>
    );
};

export default ToolRenderer;