/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useState } from "react";
import {
  Box,
  TextField,
  Button,
  Switch,
  Slider,
  Select,
  MenuItem,
  Typography,
  FormHelperText,
  FormControl,
  InputLabel,
  Chip,
  alpha,
  Tooltip,
  Fade,
  Paper,
  useTheme
} from "@mui/material";

// Import icons
import ColorLensOutlinedIcon from '@mui/icons-material/ColorLensOutlined';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import VideocamOutlinedIcon from '@mui/icons-material/VideocamOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ClearIcon from '@mui/icons-material/Clear';

import type { InputField } from "../data/pluginList";

// Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted)
const CURRENT_DATE_TIME = "2025-05-06 20:40:52";
const CURRENT_USER_LOGIN = "hanhiho";

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
  const [fileHover, setFileHover] = useState(false);
  
  // Common text field styling
  const getTextFieldSx = () => ({
    '& .MuiOutlinedInput-root': {
      transition: 'all 0.2s ease-in-out',
      borderRadius: 1.5,
      backgroundColor: "#333333", // Specific background color requested
      '&:hover': {
        borderColor: 'rgb(30, 165, 76)', // Green border on hover
      },
      '&.Mui-focused': {
        backgroundColor: '#1ea54c1a', // Light green background when focused
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: 'rgb(30, 165, 76)', // Green border when focused
          borderWidth: 2,
        }
      },
      '&.Mui-disabled': {
        backgroundColor: alpha(theme.palette.action.disabled, 0.1),
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: alpha(theme.palette.action.disabled, 0.3),
        }
      },
    },
    '& .MuiInputLabel-root.Mui-focused': {
      color: 'rgb(30, 165, 76)', // Green label color when focused
    }
  });

  // Common props for text fields
  const commonProps = {
    fullWidth: true,
    size: "small",
    variant: "outlined" as const,
    label: field.label,
    disabled: disabled,
    required: field.required,
    error: !!error,
    helperText: error || field.helperText,
    placeholder: field.placeholder,
    sx: getTextFieldSx(),
    InputLabelProps: {
      shrink: true,
      sx: { fontWeight: 500 }
    }
  };

  // Log field rendering with timestamp and user
  React.useEffect(() => {
    console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Rendering field: ${field.id}, type: ${field.type}`);
  }, [field.id, field.type]);

  // Display helper text with icon if present
  const renderHelperText = (text?: string, isError = false) => {
    if (!text) return null;
    
    return (
      <FormHelperText 
        error={isError}
        sx={{ 
          mt: 0.5, 
          ml: field.type === 'switch' ? 1 : 0.5,
          display: 'flex',
          alignItems: 'center',
          '& svg': { mr: 0.5, fontSize: '0.875rem' }
        }}
      >
        {isError ? null : <InfoOutlinedIcon fontSize="small" />}
        {text}
      </FormHelperText>
    );
  };

  // Render label with tooltip if helperText exists
  const renderLabelWithTooltip = (label?: string) => {
    if (!label) return null;
    
    if (field.helperText) {
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Typography 
            variant="body2" 
            sx={{ 
              fontWeight: 500,
              color: disabled ? 'text.disabled' : 'text.primary'
            }}
          >
            {label}
            {field.required && <span style={{ color: theme.palette.error.main, marginLeft: 2 }}>*</span>}
          </Typography>
          <Tooltip title={field.helperText} arrow placement="top">
            <InfoOutlinedIcon
              fontSize="small"
              sx={{ 
                ml: 0.5, 
                opacity: 0.6, 
                fontSize: '0.875rem',
                color: theme.palette.info.main
              }}
            />
          </Tooltip>
        </Box>
      );
    }
    
    return (
      <Typography 
        variant="body2" 
        sx={{ 
          mb: 1, 
          fontWeight: 500,
          color: disabled ? 'text.disabled' : 'text.primary'
        }}
      >
        {label}
        {field.required && <span style={{ color: theme.palette.error.main, marginLeft: 2 }}>*</span>}
      </Typography>
    );
  };

  switch (field.type) {
    case "text":
    case "password":
      return (
        <TextField
          {...commonProps}
          type={field.type}
          multiline={field.multiline}
          rows={field.rows}
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
              ...getTextFieldSx(),
              position: 'relative',
              '& .MuiOutlinedInput-root': {
                backgroundColor: "#333333",
                borderRadius: 1.5,
                transition: 'all 0.2s ease',
                '&:hover': {
                  borderColor: 'rgb(30, 165, 76)',
                },
                '&.Mui-focused': {
                  backgroundColor: '#1ea54c1a',
                  '& .MuiOutlinedInput-notchedOutline': {
                    borderColor: 'rgb(30, 165, 76)',
                    borderWidth: 2,
                  }
                }
              },
              '& .MuiSelect-select': {
                display: 'flex',
                alignItems: 'center',
                gap: 1.2,
                py: 1.3,
                pl: 1.5,
                pr: 3.5,
                fontSize: '0.875rem',
                '&.Mui-disabled': {
                  backgroundColor: 'rgba(0, 0, 0, 0.2)',
                }
              },
              '& .MuiSelect-icon': {
                color: disabled ? 'rgba(255, 255, 255, 0.3)' : 'rgba(255, 255, 255, 0.7)',
                transition: 'transform 0.2s ease',
                right: 8,
              },
              '&:hover .MuiSelect-icon': {
                color: disabled ? 'rgba(255, 255, 255, 0.3)' : 'rgb(30, 165, 76)',
              },
              '& .Mui-focused .MuiSelect-icon': {
                transform: 'rotate(180deg)',
                color: 'rgb(30, 165, 76)',
              }
            }}
          >
            <InputLabel 
              shrink 
              sx={{ 
                fontWeight: 500,
                backgroundColor: alpha('#000', 0.6),
                px: 0.7,
                py: 0.3,
                borderRadius: '4px',
                fontSize: '0.75rem',
                letterSpacing: '0.02em',
                transform: 'translate(14px, -10px) scale(0.85)',
                color: error ? theme.palette.error.main : (disabled ? 'rgba(255, 255, 255, 0.4)' : 'rgba(255, 255, 255, 0.7)'),
                transition: 'color 0.2s ease',
                '&.Mui-focused': {
                  color: 'rgb(30, 165, 76)',
                }
              }}
            >
              {field.label}
              {field.required && <span style={{ color: theme.palette.error.main, marginLeft: 2 }}>*</span>}
            </InputLabel>
            
            <Select
              label={field.label}
              value={value ?? field.default ?? ""}
              onChange={(e) => {
                console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}]: Select field ${field.id} changed to ${e.target.value}`);
                onChange(e.target.value);
              }}
              displayEmpty
              MenuProps={{
                PaperProps: {
                  sx: {
                    maxHeight: 320,
                    borderRadius: 1.5,
                    boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
                    border: '1px solid',
                    borderColor: alpha('rgb(30, 165, 76)', 0.3),
                    backgroundColor: "#262626",
                    mt: 0.5,
                    '&::-webkit-scrollbar': {
                      width: '8px'
                    },
                    '&::-webkit-scrollbar-thumb': {
                      backgroundColor: 'rgba(30, 165, 76, 0.2)',
                      borderRadius: '4px',
                      '&:hover': {
                        backgroundColor: 'rgba(30, 165, 76, 0.3)'
                      }
                    },
                    '&::-webkit-scrollbar-track': {
                      backgroundColor: 'rgba(0, 0, 0, 0.1)'
                    }
                  }
                },
                TransitionProps: {
                  timeout: 150
                },
                anchorOrigin: {
                  vertical: 'bottom',
                  horizontal: 'center',
                },
                transformOrigin: {
                  vertical: 'top',
                  horizontal: 'center',
                },
              }}
            >
              {!field.required && field.default === undefined && (
                <MenuItem 
                  value="" 
                  sx={{ 
                    fontStyle: 'italic', 
                    opacity: 0.7,
                    borderRadius: 1,
                    mx: 0.5,
                    my: 0.3,
                    pl: 2,
                    py: 1.2,
                    color: 'rgba(255, 255, 255, 0.6)'
                  }}
                >
                  <em>Select an option...</em>
                </MenuItem>
              )}
              
              {field.options?.map((option: any, index: number) => {
                const optionValue = typeof option === "object" ? option.value : option;
                const optionLabel = typeof option === "object" ? option.label : option;
                const optionColor = typeof option === "object" ? option.color : undefined;
                const isSelected = value === optionValue || field.default === optionValue;
                
                return (
                  <MenuItem 
                    key={index} 
                    value={optionValue}
                    sx={{
                      borderRadius: 1,
                      mx: 0.5,
                      my: 0.3,
                      pl: 2,
                      py: 1.2,
                      transition: 'all 0.15s ease',
                      '&.Mui-selected': {
                        backgroundColor: alpha('rgb(30, 165, 76)', 0.12),
                        fontWeight: 500,
                        '&:before': {
                          content: '""',
                          position: 'absolute',
                          left: 0,
                          top: '50%',
                          transform: 'translateY(-50%)',
                          width: 3,
                          height: '60%',
                          backgroundColor: 'rgb(30, 165, 76)',
                          borderRadius: '0 3px 3px 0'
                        },
                        '&:hover': {
                          backgroundColor: alpha('rgb(30, 165, 76)', 0.18),
                        }
                      },
                      '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.04)'
                      }
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                      {optionColor && (
                        <Box 
                          component="span" 
                          sx={{
                            width: 14,
                            height: 14,
                            borderRadius: '50%',
                            bgcolor: optionColor,
                            mr: 1.5,
                            display: 'inline-block',
                            flexShrink: 0,
                            border: '1px solid',
                            borderColor: alpha(optionColor, 0.3)
                          }} 
                        />
                      )}
                      <Typography 
                        variant="body2" 
                        sx={{ 
                          flexGrow: 1,
                          fontWeight: isSelected ? 500 : 400,
                          color: isSelected ? 'rgb(30, 165, 76)' : 'inherit'
                        }}
                      >
                        {optionLabel}
                      </Typography>
                      {isSelected && (
                        <Box 
                          component="span"
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            ml: 1,
                            color: 'rgb(30, 165, 76)'
                          }}
                        >
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M9 16.2L4.8 12L3.4 13.4L9 19L21 7L19.6 5.6L9 16.2Z" fill="currentColor" />
                          </svg>
                        </Box>
                      )}
                    </Box>
                  </MenuItem>
                );
              })}
            </Select>
            {renderHelperText(error || field.helperText, !!error)}
          </FormControl>
        );
    case "switch":
      return (
        <Box>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              width: "100%",
              minHeight: "40px",
              px: 1,
              py: 0.5,
              borderRadius: 1.5,
              border: '1px solid',
              borderColor: error 
                ? theme.palette.error.main 
                : alpha(theme.palette.divider, 0.3),
              bgcolor: value 
                ? '#1ea54c1a'
                : '#333333',
              transition: 'background-color 0.3s ease',
              '&:hover': {
                borderColor: 'rgb(30, 165, 76)',
              },
            }}
          >
            <Typography
              variant="body2"
              sx={{ 
                fontWeight: 500,
                color: disabled 
                  ? "text.disabled" 
                  : value 
                    ? 'rgb(30, 165, 76)' 
                    : "text.primary", 
                mr: 1,
                transition: 'color 0.3s ease'
              }}
            >
              {field.label || ""}
              {field.required && <span style={{ color: theme.palette.error.main, marginLeft: 2 }}>*</span>}
            </Typography>
            <Switch
              name={field.id}
              checked={!!value}
              onChange={(e) => onChange(e.target.checked)}
              disabled={disabled}
              color="primary"
              size="small"
              sx={{
                '& .MuiSwitch-switchBase.Mui-checked': {
                  color: 'rgb(30, 165, 76)',
                  '&:hover': {
                    backgroundColor: alpha('rgb(30, 165, 76)', 0.1),
                  },
                },
                '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
                  backgroundColor: 'rgb(30, 165, 76)',
                },
              }}
            />
          </Box>
          {renderHelperText(error || field.helperText, !!error)}
        </Box>
      );
      
    case "slider":
      return (
        <Box 
          sx={{ 
            px: 1.5, 
            py: 1,
            borderRadius: 1.5,
            border: '1px solid',
            borderColor: error 
              ? theme.palette.error.main 
              : alpha(theme.palette.divider, 0.3),
            backgroundColor: "#333333",
            '&:hover': {
              borderColor: 'rgb(30, 165, 76)',
            },
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography 
              variant="body2" 
              id={`${field.id}-label`}
              sx={{ 
                fontWeight: 500,
                color: disabled ? 'text.disabled' : 'text.primary'
              }}
            >
              {field.label}
              {field.required && <span style={{ color: theme.palette.error.main, marginLeft: 2 }}>*</span>}
            </Typography>
            
            <Chip
              label={value ?? field.default ?? field.min ?? 0}
              size="small"
              variant="filled"
              sx={{ 
                height: 24, 
                fontSize: '0.75rem', 
                bgcolor: alpha('rgb(30, 165, 76)', 0.15),
                color: 'rgb(30, 165, 76)',
                fontWeight: 600
              }}
            />
          </Box>
          
          <Box sx={{ px: 1, mt: 2 }}>
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
              marks={field.marks === true || (field.step && field.step > 0 && (field.max! - field.min!) / field.step <= 10)}
              min={field.min ?? 0}
              max={field.max ?? 100}
              disabled={disabled}
              sx={{
                height: 4,
                color: 'rgb(30, 165, 76)',
                '& .MuiSlider-thumb': {
                  width: 14,
                  height: 14,
                  transition: '0.2s',
                  '&:hover, &.Mui-focusVisible': {
                    boxShadow: `0 0 0 8px ${alpha('rgb(30, 165, 76)', 0.16)}`,
                  },
                  '&.Mui-active': {
                    width: 20,
                    height: 20,
                  },
                },
                '& .MuiSlider-rail': {
                  opacity: 0.32,
                },
              }}
            />
          </Box>
          
          {renderHelperText(error || field.helperText, !!error)}
        </Box>
      );
      
    case "color":
      return (
        <FormControl 
          fullWidth 
          size="small"
          error={!!error}
        >
          {renderLabelWithTooltip(field.label)}
          
          <Box sx={{ 
            display: "flex", 
            alignItems: "center",
            borderRadius: 1.5,
            border: '1px solid',
            borderColor: error 
              ? theme.palette.error.main 
              : alpha(theme.palette.divider, 0.3),
            overflow: 'hidden',
            '&:hover': {
              borderColor: 'rgb(30, 165, 76)',
            },
            '&:focus-within': {
              borderColor: 'rgb(30, 165, 76)',
              backgroundColor: '#1ea54c1a',
            },
          }}>
            <Box sx={{ 
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 42,
              height: 42, 
              backgroundColor: value ?? field.default ?? "#ffffff",
              position: 'relative',
              borderRight: '1px solid',
              borderColor: alpha(theme.palette.divider, 0.2)
            }}>
              <input
                type="color"
                id={`color-input-${field.id}`}
                disabled={disabled}
                value={value ?? field.default ?? "#000000"}
                onChange={(e) => onChange(e.target.value)}
                style={{
                  opacity: 0,
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '100%',
                  cursor: disabled ? "not-allowed" : "pointer",
                }}
              />
              <ColorLensOutlinedIcon 
                sx={{ 
                  color: theme.palette.getContrastText(value ?? field.default ?? "#ffffff")
                    ,
                  fontSize: '1.2rem',
                  opacity: disabled ? 0.4 : 0.7,
                  pointerEvents: 'none'
                }} 
              />
            </Box>
            
            <TextField
              size="small"
              variant="outlined"
              disabled={disabled}
              sx={{ 
                flexGrow: 1,
                '& .MuiOutlinedInput-root': {
                  backgroundColor: "#333333",
                  border: 'none',
                  '&:focus-within': {
                    backgroundColor: '#1ea54c1a',
                  },
                  '& fieldset': {
                    border: 'none'
                  },
                },
                '& .MuiInputBase-input': {
                  py: 1.5,
                  px: 1.5
                }
              }}
              value={value ?? field.default ?? "#000000"}
              onChange={(e) => onChange(e.target.value)}
            />
          </Box>
          
          {renderHelperText(error || field.helperText, !!error)}
        </FormControl>
      );
      
    case "file":
      return (
        <Box>
          {renderLabelWithTooltip(field.label)}
          
          <Box
            sx={{
              width: '100%',
              position: 'relative',
              borderRadius: 1.5,
              border: '1px solid',
              borderColor: error
                ? theme.palette.error.main
                : fileHover
                  ? 'rgb(30, 165, 76)'
                  : alpha(theme.palette.divider, 0.3),
              transition: 'all 0.2s ease',
              overflow: 'hidden',
              backgroundColor: "#333333",
              '&:focus-within': {
                backgroundColor: '#1ea54c1a',
                borderColor: 'rgb(30, 165, 76)',
              },
              p: 1.5,
            }}
            onMouseEnter={() => !disabled && setFileHover(true)}
            onMouseLeave={() => setFileHover(false)}
          >
            <Box 
              sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                flexWrap: 'wrap',
                gap: 1.5
              }}
            >
              {/* File preview area */}
              {typeof value === "string" && value.startsWith("data:image") ? (
                <Paper 
                  elevation={0} 
                  sx={{ 
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    p: 0.5,
                    borderRadius: 1,
                    border: '1px solid',
                    borderColor: alpha(theme.palette.divider, 0.2),
                    position: 'relative',
                    overflow: 'hidden',
                    minWidth: 60,
                    height: 60
                  }}
                >
                  <img
                    src={value}
                    alt="Preview"
                    style={{ 
                      height: '100%',
                      maxWidth: '100%',
                      objectFit: 'contain'
                    }}
                  />
                  
                  {!disabled && (
                    <Fade in={fileHover}>
                      <Box sx={{
                        position: 'absolute',
                        top: 0,
                        right: 0,
                        p: 0.2,
                      }}>
                        <IconButton 
                          size="small" 
                          onClick={() => onChange("")}
                          sx={{
                            bgcolor: 'rgba(0,0,0,0.5)',
                            color: '#fff',
                            p: 0.2,
                            '&:hover': {
                              bgcolor: 'rgba(0,0,0,0.7)',
                            },
                            '& svg': {
                              fontSize: '0.75rem'
                            }
                          }}
                        >
                          <ClearIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Fade>
                  )}
                </Paper>
              ) : (
                <InsertDriveFileOutlinedIcon 
                  sx={{ 
                    color: 'rgb(30, 165, 76)',
                    opacity: disabled ? 0.4 : 0.8
                  }} 
                />
              )}

              <Box sx={{ flex: 1 }}>
                <Button
                  component="label"
                  variant="outlined"
                  size="small"
                  disabled={disabled}
                  startIcon={<FileUploadOutlinedIcon />}
                  sx={{
                    borderRadius: 1.5,
                    textTransform: 'none',
                    borderColor: alpha('rgb(30, 165, 76)', 0.5),
                    color: 'rgb(30, 165, 76)',
                    '&:hover': {
                      borderColor: 'rgb(30, 165, 76)',
                      backgroundColor: alpha('rgb(30, 165, 76)', 0.04),
                    }
                  }}
                >
                  {typeof value === "string" && value.startsWith("data:") 
                    ? "Replace File" 
                    : "Select File"}
                  <input
                    type="file"
                    hidden
                    accept={field.accept || "*/*"}
                    onChange={(e) => {
                      if (e.target.files && e.target.files[0]) {
                        const file = e.target.files[0];
                        console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] File selected: ${file.name} (${file.type}), size: ${(file.size / 1024).toFixed(1)} KB`);
                        
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
                              `[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] File read successfully, length: ${result.length}`
                            );
                            onChange(result);
                          } else {
                            console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Invalid file data format`);
                            onChange(null);
                          }
                        };
                        reader.onerror = () => {
                          console.error(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] File reading error`);
                          onChange(null);
                        };
                        reader.readAsDataURL(file);
                      }
                    }}
                  />
                </Button>
                
                {typeof value === "string" && value.startsWith("data:") && (
                  <Typography 
                    variant="caption" 
                    sx={{ 
                      display: 'block', 
                      mt: 0.5,
                      color: 'text.secondary',
                      fontSize: '0.7rem'
                    }}
                  >
                    File size: {(value.length / 1024).toFixed(1)} KB
                  </Typography>
                )}
              </Box>
            </Box>
          </Box>
          
          {renderHelperText(error || field.helperText, !!error)}
        </Box>
      );
      
    case "button":
      return (
        <Box sx={{ py: 1 }}>
          <Button
            id={`button-${field.id}`}
            variant="contained"
            size="medium"
            onClick={() => {
              console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Button clicked: ${field.action || field.id}`);
              if (field.action) {
                // Here you would add logic to handle specific actions
                console.log(`[${CURRENT_DATE_TIME}] [${CURRENT_USER_LOGIN}] Action triggered: ${field.action}`);
              }
            }}
            disabled={disabled}
            sx={{
              textTransform: 'none',
              borderRadius: 1.5,
              px: 2.5,
              bgcolor: 'rgb(30, 165, 76)',
              '&:hover': {
                bgcolor: alpha('rgb(30, 165, 76)', 0.85),
              }
            }}
          >
            {field.label}
          </Button>
          {renderHelperText(field.helperText)}
        </Box>
      );
      
    case "hidden":
      return null;
      
    case "webcamPreview":
      return (
        <Box>
          {renderLabelWithTooltip(field.label)}
          
          <Paper
            sx={{
              border: '1px dashed',
              borderColor: alpha(theme.palette.divider, 0.4),
              borderRadius: 2,
              minHeight: 200,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              bgcolor: "#333333",
              p: 2,
              overflow: 'hidden',
              '&:hover': {
                borderColor: 'rgb(30, 165, 76)',
              },
            }}
            elevation={0}
          >
            <VideocamOutlinedIcon 
              sx={{ 
                fontSize: '2.5rem', 
                color: alpha('rgb(30, 165, 76)', 0.7),
                mb: 2
              }} 
            />
            <Typography 
              sx={{ 
                color: alpha(theme.palette.grey[300], 0.9),
                textAlign: 'center',
                fontStyle: 'italic'
              }}
            >
              Webcam Preview Area
              <br />
              <span style={{ fontSize: '0.8rem', opacity: 0.7 }}>
                This feature requires camera access
              </span>
            </Typography>
          </Paper>
          {renderHelperText(field.helperText)}
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

export default RenderInput;