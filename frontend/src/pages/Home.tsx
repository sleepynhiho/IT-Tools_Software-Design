import { useState } from "react";
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
  alpha
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

// Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted)
const CURRENT_DATE_TIME = "2025-05-06 18:39:11";
const CURRENT_USER_LOGIN = "hanhiho";

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
    addTool
  } = useAllTools();
  const { userType } = useAuth();
  const isAdmin = userType === 'admin';

  // State for file upload dialog
  const [openUploadDialog, setOpenUploadDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileUploadError, setFileUploadError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

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

  console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Home - Render`, {
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
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Failed to remove tool:`, error);
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
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Failed to update tool status:`, error);
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
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Failed to update tool access level:`, error);
        // Error feedback could be added here
      }
    }
  };

  // Handler for Add Tool button click
  const handleAddToolClick = () => {
    setOpenUploadDialog(true);
  };

  // Handle file selection
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFileUploadError(null);
    const files = event.target.files;
    if (files && files.length > 0) {
      const file = files[0];
      setSelectedFile(file);
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: File selected: ${file.name} (${file.type})`);
    } else {
      setSelectedFile(null);
    }
  };

  // Handle file upload submission
  const handleFileUpload = async () => {
    if (!selectedFile) {
      setFileUploadError("No file selected");
      return;
    }

    setIsUploading(true);
    setFileUploadError(null);

    try {
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Processing file: ${selectedFile.name}`);
      
      // Create file reader
      const reader = new FileReader();
      
      // Set up file reader onload handler
      reader.onload = async (e) => {
        try {
          const result = e.target?.result;
          
          // Process the file (could be JSON, binary, or any other format)
          console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: File loaded successfully, size: ${selectedFile.size} bytes`);
          
          // Just simulate adding a tool based on the file name (in real app, process the file content)
          const mockTool = {
            name: selectedFile.name.split('.')[0],
            description: `Tool created from ${selectedFile.name}`,
            icon: "Build",
            status: "enabled",
            accessLevel: "normal"
          };
          
          // Add the tool
          await addTool(mockTool);
          
          // Close dialog after successful upload
          setOpenUploadDialog(false);
          setSelectedFile(null);
          
          // You might want to add a success notification here
        } catch (processError) {
          console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error processing file:`, processError);
          setFileUploadError("Error processing file");
        } finally {
          setIsUploading(false);
        }
      };
      
      // Set up error handler
      reader.onerror = () => {
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: File reading error`);
        setFileUploadError("Error reading file");
        setIsUploading(false);
      };
      
      // Read the file as binary data
      reader.readAsArrayBuffer(selectedFile);
      
    } catch (error) {
      console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: File upload error:`, error);
      setFileUploadError("Error uploading file");
      setIsUploading(false);
    }
  };

  // Handle dialog close
  const handleCloseDialog = () => {
    setOpenUploadDialog(false);
    setSelectedFile(null);
    setFileUploadError(null);
  };

  // Handle drag over event
  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
  };

  // Handle drop event
  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    
    setFileUploadError(null);
    
    if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      setSelectedFile(file);
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: File dropped: ${file.name} (${file.type})`);
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
                sx={{ color: '#1ea54c' }}
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
                bgcolor: theme.palette.mode === "dark" ? "#1e1e2f" : "#ffffff",
                backgroundImage: theme.palette.mode === "dark" 
                  ? 'linear-gradient(rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.05))'
                  : 'none',
                boxShadow: theme.palette.mode === "dark" 
                  ? '0 8px 32px rgba(0, 0, 0, 0.5)'
                  : '0 8px 32px rgba(0, 0, 0, 0.1)',
              }
            }}
          >
            <DialogTitle 
              sx={{ 
                pb: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: `1px solid ${alpha(theme.palette.divider, 0.1)}`
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <UploadFileIcon sx={{ mr: 1.5, color: '#3b956f' }} />
                <Typography variant="h6" component="div">
                  Add New Tool
                </Typography>
              </Box>
              <IconButton 
                edge="end" 
                color="inherit" 
                onClick={handleCloseDialog}
                aria-label="close"
              >
                <CloseIcon />
              </IconButton>
            </DialogTitle>
            
            <DialogContent sx={{ py: 3 }}>
              {/* Drag and Drop Area */}
              <Box
                sx={{
                  border: `2px dashed ${selectedFile ? '#3b956f' : theme.palette.divider}`,
                  borderRadius: 2,
                  p: 3,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  minHeight: '200px',
                  backgroundColor: alpha(theme.palette.background.default, 0.4),
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    borderColor: '#3b956f',
                    backgroundColor: alpha('#3b956f', 0.05),
                    color: '#3b956f',
                  },
                }}
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                onClick={() => document.getElementById('file-upload-input')?.click()}
              >
                <input
                  type="file"
                  id="file-upload-input"
                  style={{ display: 'none' }}
                  onChange={handleFileSelect}
                />
                
                <FileUploadIcon 
                  sx={{ 
                    fontSize: 64, 
                    color: selectedFile ? '#3b956f' : theme.palette.mode === "dark" ? '#666' : '#aaa',
                    mb: 2 
                  }} 
                />
                
                {selectedFile ? (
                  <>
                    <Typography variant="subtitle1" fontWeight="medium" gutterBottom>
                      Selected file: {selectedFile.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {formatFileSize(selectedFile.size)}
                    </Typography>
                  </>
                ) : (
                  <>
                    <Typography variant="h6" gutterBottom>
                      Drag & Drop Any File
                    </Typography>
                    <Typography variant="body2" color="lightgray" align="center">
                      Drop your file here, or click to browse
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
                    bgcolor: alpha(theme.palette.error.main, 0.1),
                    borderRadius: 1,
                    textAlign: 'center'
                  }}
                >
                  {fileUploadError}
                </Typography>
              )}
            </DialogContent>
            
            <DialogActions sx={{ px: 3, py: 2, borderTop: `1px solid ${alpha(theme.palette.divider, 0.1)}` }}>
              <Button 
                variant="outlined" 
                onClick={handleCloseDialog}
                disabled={isUploading}
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
                {isUploading ? "Uploading..." : "Upload Tool"}
              </Button>
            </DialogActions>
          </Dialog>
        </Box>
      </Container>
    </Box>
  );
};

export default Home;