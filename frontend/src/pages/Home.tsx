import { useState, useRef } from "react";
import { 
  Box, 
  Container, 
  Grid, 
  Typography, 
  CircularProgress, 
  Fab, 
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  useTheme,
  alpha,
  Tooltip,
  Snackbar,
  Alert
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import CloseIcon from "@mui/icons-material/Close";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import FileUploadIcon from "@mui/icons-material/FileUpload";
import ToolCard from "../components/ToolCard";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAllTools } from "../context/AllToolsContext";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const Home = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const { favoriteTools, toggleFavorite, isFavorite } = useFavoriteTools();
  const { 
    allTools, 
    isLoading, 
    error, 
    updateToolStatus,
    updateToolAccessLevel,
    addTool,
    refreshTools
  } = useAllTools();
  // Correctly extract all needed auth properties
  const { userType, getIdToken } = useAuth();
  const isAdmin = userType === 'admin';

  // State for file upload dialog
  const [openUploadDialog, setOpenUploadDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileUploadError, setFileUploadError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  // Add success message handling
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  
  // Create an abort controller reference for cancelling requests
  const abortControllerRef = useRef<AbortController | null>(null);

  // Only show enabled tools to non-admin users
  const visibleTools = isAdmin
    ? allTools
    : allTools.filter(tool => tool.status !== 'disabled');
    
  // Get only visible favorite tools
  const visibleFavoriteTools = favoriteTools.filter(tool => {
    // For non-admin users, filter out disabled tools
    if (!isAdmin && tool.status === 'disabled') return false;
    return true;
  });

  console.log("Home - Render", {
      favoriteToolsCount: visibleFavoriteTools.length,
      allToolsCount: visibleTools.length,
      isLoadingAllTools: isLoading,
      allToolsError: error,
      isUserAdmin: isAdmin
  });

  const checkIsFavorite = (toolId: string): boolean => {
     return isFavorite(toolId);
  };

  // Handler for removing a tool
  const handleRemoveTool = async (toolId: string) => {
    if (isAdmin) {
      try {
        // await removeTool(toolId);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to remove tool:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for toggling tool status
  const handleToolStatusToggle = async (toolId: string, newStatus: 'enabled' | 'disabled') => {
    if (isAdmin) {
      try {
        await updateToolStatus(toolId, newStatus);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to update tool status:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for changing tool access level
  const handleToolAccessLevelChange = async (toolId: string, newLevel: 'normal' | 'premium') => {
    if (isAdmin) {
      try {
        await updateToolAccessLevel(toolId, newLevel);
        // Success feedback could be added here
      } catch (error) {
        console.error("Failed to update tool access level:", error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for Add Tool button click
  const handleAddToolClick = () => {
    setOpenUploadDialog(true);
  };

  // Check if file is a JAR file
  const isJarFile = (file: File): boolean => {
    return file.name.toLowerCase().endsWith('.jar');
  };

  // Handle file selection
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFileUploadError(null);
    const files = event.target.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (!isJarFile(file)) {
        setFileUploadError("Only JAR files are supported");
        setSelectedFile(null);
        return;
      }
      setSelectedFile(file);
      console.log("File selected:", file.name, file.type);
    } else {
      setSelectedFile(null);
    }
  };

  const handleFileUpload = async () => {
    if (!selectedFile) {
      setFileUploadError("No file selected");
      return;
    }

    if (!isJarFile(selectedFile)) {
      setFileUploadError("Only JAR files are supported");
      return;
    }

    if (!isAdmin) {
      setFileUploadError("Only admin users can upload plugins");
      return;
    }

    setIsUploading(true);
    setFileUploadError(null);

    // Create a new AbortController for this request
    abortControllerRef.current = new AbortController();
    const signal = abortControllerRef.current.signal;

    try {
      // Get the authentication token using the getIdToken function from AuthContext
      const token = await getIdToken();
      
      if (!token) {
        throw new Error("Authentication failed. Please log in again.");
      }

      // Create FormData
      const formData = new FormData();
      formData.append('file', selectedFile);
      
      // Your backend URL - using proxy configured in vite.config.ts
      const backendUrl = '/api/plugins/upload';
      
      // Make the API call
      const response = await fetch(backendUrl, {
        method: 'POST',
        body: formData,
        headers: {
          // Authorization header with Firebase token
          'Authorization': `Bearer ${token}`
        },
        signal: signal // Add the abort signal
      });
      
      if (!response.ok) {
        let errorMessage = `Upload failed: ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch (e) {
          // Ignore parse errors
        }
        throw new Error(errorMessage);
      }
      
      const result = await response.json();
      console.log('Upload successful:', result);
      
      // Set success message and show snackbar
      setSuccessMessage("Plugin uploaded successfully!");
      setOpenSnackbar(true);
      
      // Close dialog after successful upload
      setOpenUploadDialog(false);
      setSelectedFile(null);
      
      // Reload the tool list
      try {
        // Refresh the tool list - assuming this method exists in your AllToolsContext
        if (typeof refreshTools === 'function') {
          await refreshTools();
        }
      } catch (refreshError) {
        console.error("Error refreshing tool list:", refreshError);
      }
      
    } catch (error: any) {
      // Check if the error is due to an abort
      if (error.name === 'AbortError') {
        console.log('Upload aborted');
        return;
      }
      
      console.error('File upload error:', error);
      setFileUploadError(`Error uploading file: ${error.message}`);
    } finally {
      setIsUploading(false);
      abortControllerRef.current = null;
    }
  };

  // Handle dialog close
  const handleCloseDialog = () => {
    // If an upload is in progress, abort it
    if (isUploading && abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    setOpenUploadDialog(false);
    setSelectedFile(null);
    setFileUploadError(null);
  };

  // Handle drag over event
  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!isDragging) setIsDragging(true);
  };
  
  // Handle drag leave event
  const handleDragLeave = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
  };

  // Handle drop event
  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setIsDragging(false);
    setFileUploadError(null);
    
    if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      
      if (!isJarFile(file)) {
        setFileUploadError("Only JAR files are supported");
        return;
      }
      
      setSelectedFile(file);
      console.log("File dropped:", file.name, file.type);
    }
  };

  // Format file size for display
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} bytes`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Box
      component="main"
      sx={{
        display: "flex",
        justifyContent: "center",
        alignItems: "flex-start",
        minHeight: "calc(100vh - 64px)",
        paddingTop: "10px",
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            padding: "20px",
          }}
        >
          {/* Favorite Tools Section - only show if there are visible favorites */}
          {visibleFavoriteTools && visibleFavoriteTools.length > 0 && (
            <>
              <Typography sx={{ mb: 1 }} variant="h4" component="h1">
                <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
                  Your favorite tools
                </span>
              </Typography>
              <Grid container spacing={3} justifyContent="flex-start">
                {visibleFavoriteTools.map((tool) => (
                  <Grid item xs={12} sm={6} md={4} lg={2.4} key={`favorite-${tool.id}`}>
                    <ToolCard
                      tool={tool}
                      isFavorite={true}
                      onFavoriteToggle={toggleFavorite}
                      onToolRemove={isAdmin ? handleRemoveTool : undefined}
                      onToolStatusToggle={isAdmin ? handleToolStatusToggle : undefined}
                      onToolAccessLevelChange={isAdmin ? handleToolAccessLevelChange : undefined}
                    />
                  </Grid>
                ))}
              </Grid>
            </>
          )}

          {/* All Tools Section */}
          <Typography sx={{ mb: 1, mt: visibleFavoriteTools.length > 0 ? 3 : 0 }} variant="h4" component="h1">
            <span style={{ fontSize: "1rem", fontWeight: "bold", color: "#a3a3a3" }}>
              All the tools
            </span>
          </Typography>

          {isLoading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', my: 4 }}>
              <CircularProgress
                size={60}
                thickness={4}
                sx={{ color: '#3b956f' }}
              />
              <Typography variant="body2" sx={{ mt: 2, color: '#a3a3a3' }}>
                Loading Tools...
              </Typography>
            </Box>
          ) : error ? (
            <Typography color="error">Error loading tools: {error}</Typography>
          ) : (
            <Grid container spacing={3} justifyContent="flex-start">
              {visibleTools && visibleTools.length > 0 ? (
                  visibleTools.map((tool) => (
                    <Grid item xs={12} sm={6} md={4} lg={2.4} key={`all-${tool.id}`}>
                      <ToolCard
                        tool={tool}
                        isFavorite={checkIsFavorite(tool.id)}
                        onFavoriteToggle={toggleFavorite}
                        onToolRemove={isAdmin ? handleRemoveTool : undefined}
                        onToolStatusToggle={isAdmin ? handleToolStatusToggle : undefined}
                        onToolAccessLevelChange={isAdmin ? handleToolAccessLevelChange : undefined}
                      />
                    </Grid>
                  ))
              ) : (
                  <Typography sx={{ mt: 2, color: '#727272' }}>No tools available for you.</Typography>
              )}
            </Grid>
          )}

          {/* Add Tool FAB for admins */}
          {isAdmin && (
            <Tooltip title="Upload JAR plugin">
              <Fab 
                color="primary" 
                aria-label="add tool"
                onClick={handleAddToolClick}
                sx={{ 
                  position: 'fixed', 
                  bottom: 30, 
                  right: 30,
                  background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                  "&:hover": { 
                    background: "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)" 
                  }
                }}
              >
                <AddIcon />
              </Fab>
            </Tooltip>
          )}

          {/* File Upload Dialog */}
          <Dialog 
            open={openUploadDialog} 
            onClose={handleCloseDialog}
            maxWidth="sm"
            fullWidth
            PaperProps={{
              sx: {
                borderRadius: 2,
                bgcolor: "#232323",
                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
              }
            }}
          >
            <DialogTitle 
              sx={{ 
                pb: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <UploadFileIcon sx={{ mr: 1.5, color: '#3b956f' }} />
                <Typography variant="h6" component="div" sx={{ color: '#ffffff' }}>
                  Upload JAR Plugin
                </Typography>
              </Box>
              <IconButton 
                edge="end" 
                color="inherit" 
                onClick={handleCloseDialog}
                aria-label="close"
                sx={{ color: '#a3a3a3' }}
              >
                <CloseIcon />
              </IconButton>
            </DialogTitle>
            
            <DialogContent sx={{ py: 3 }}>
              {/* Drag and Drop Area */}
              <Box
                sx={{
                  border: `2px dashed ${selectedFile ? '#3b956f' : isDragging ? '#3b956f' : '#525252'}`,
                  borderRadius: 2,
                  p: 3,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  minHeight: '200px',
                  backgroundColor: selectedFile || isDragging ? 'rgba(59, 149, 111, 0.1)' : '#2e2e2e',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    borderColor: '#3b956f',
                    backgroundColor: 'rgba(59, 149, 111, 0.1)',
                  },
                }}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => document.getElementById('file-upload-input')?.click()}
              >
                <input
                  type="file"
                  id="file-upload-input"
                  accept=".jar"
                  style={{ display: 'none' }}
                  onChange={handleFileSelect}
                />
                
                <FileUploadIcon 
                  sx={{ 
                    fontSize: 64, 
                    color: selectedFile || isDragging ? '#3b956f' : '#525252',
                    mb: 2 
                  }} 
                />
                
                {selectedFile ? (
                  <>
                    <Typography variant="subtitle1" fontWeight="medium" gutterBottom sx={{ color: '#ffffff' }}>
                      Selected file: {selectedFile.name}
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#a3a3a3' }}>
                      {formatFileSize(selectedFile.size)}
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#3b956f', mt: 1 }}>
                      JAR file ready for upload
                    </Typography>
                  </>
                ) : (
                  <>
                    <Typography variant="h6" gutterBottom sx={{ color: '#ffffff' }}>
                      Drag & Drop JAR File
                    </Typography>
                    <Typography variant="body2" sx={{ color: '#a3a3a3', textAlign: 'center' }}>
                      Drop your JAR plugin here, or click to browse
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#3b956f', mt: 1.5, display: 'block' }}>
                      Only .jar files are supported
                    </Typography>
                  </>
                )}
              </Box>
              
              {/* Error Message */}
              {fileUploadError && (
                <Typography 
                  variant="body2" 
                  color="error" 
                  sx={{ 
                    mt: 2, 
                    p: 1, 
                    bgcolor: 'rgba(211, 47, 47, 0.1)',
                    borderRadius: 1,
                    textAlign: 'center'
                  }}
                >
                  {fileUploadError}
                </Typography>
              )}

              <Typography variant="caption" sx={{ display: 'block', mt: 2, color: '#a3a3a3', textAlign: 'center' }}>
                Uploaded plugins will be available immediately after processing
              </Typography>
            </DialogContent>
            
            <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
              <Button 
                variant="outlined" 
                onClick={handleCloseDialog}
                disabled={isUploading}
                sx={{ 
                  color: '#a3a3a3',
                  borderColor: '#525252',
                  '&:hover': {
                    borderColor: '#a3a3a3',
                  }
                }}
              >
                Cancel
              </Button>
              <Button 
                onClick={handleFileUpload}
                variant="contained"
                disabled={!selectedFile || isUploading}
                startIcon={isUploading ? <CircularProgress size={20} color="inherit" /> : <UploadFileIcon />}
                sx={{
                  ml: 1,
                  background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                  "&:hover": { 
                    background: "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)" 
                  }
                }}
              >
                {isUploading ? "Uploading..." : "Upload Plugin"}
              </Button>
            </DialogActions>
          </Dialog>

          {/* Success Snackbar */}
          <Snackbar
            open={openSnackbar}
            autoHideDuration={6000}
            onClose={() => setOpenSnackbar(false)}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
          >
            <Alert
              onClose={() => setOpenSnackbar(false)}
              severity="success"
              sx={{ 
                width: '100%', 
                backgroundColor: 'rgba(59, 149, 111, 0.9)',
                color: '#ffffff'
              }}
            >
              {successMessage}
            </Alert>
          </Snackbar>
        </Box>
      </Container>
    </Box>
  );
};

export default Home;