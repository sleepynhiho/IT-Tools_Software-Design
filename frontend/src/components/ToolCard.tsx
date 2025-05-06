import {
  Card,
  Typography,
  IconButton,
  Box,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  CircularProgress,
  Alert,
} from "@mui/material";
import * as MuiIcons from "@mui/icons-material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import DeleteIcon from "@mui/icons-material/Delete";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import BlockIcon from "@mui/icons-material/Block";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import StarIcon from "@mui/icons-material/Star";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import LockIcon from "@mui/icons-material/Lock";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAuth } from "../context/AuthContext";
import { doc, getDoc, setDoc, serverTimestamp } from "firebase/firestore";
import { db } from "../firebaseConfig";

// Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted)
const CURRENT_DATE_TIME = "2025-05-06 16:52:20";
const CURRENT_USER_LOGIN = "hanhiho";

const ToolCard = ({
  tool,
  onFavoriteToggle,
  onToolRemove,
  onToolStatusToggle,
  onToolAccessLevelChange,
}: {
  tool: {
    id: string;
    name: string;
    description: string;
    icon: string;
    accessLevel?: "normal" | "premium" | "admin";
    status?: "enabled" | "disabled";
  };
  onFavoriteToggle: (tool: any) => void;
  onToolRemove?: (toolId: string) => void;
  onToolStatusToggle?: (
    toolId: string,
    newStatus: "enabled" | "disabled"
  ) => void;
  onToolAccessLevelChange?: (
    toolId: string,
    newLevel: "normal" | "premium"
  ) => void;
}) => {
  const IconComponent =
    MuiIcons[tool.icon as keyof typeof MuiIcons] || MuiIcons.HelpOutline;

  const { favoriteTools } = useFavoriteTools();
  const { currentUser, userType, refreshUserData } = useAuth();
  const isAdmin = userType === "admin";
  const isPremiumUser = userType === "premium" || userType === "admin";

  const isFavorite = favoriteTools.some((fav) => fav.id === tool.id);
  
  // State to track the Firestore premium status
  const [firestorePremium, setFirestorePremium] = useState<boolean | null>(null);
  // State to track the Firestore enabled status
  const [firestoreEnabled, setFirestoreEnabled] = useState<boolean | null>(null);
  
  // Use either the Firestore premium status or the tool's accessLevel
  const isPremium = firestorePremium !== null ? firestorePremium : tool.accessLevel === "premium";
  // Use either the Firestore enabled status or the tool's status
  const isEnabled = firestoreEnabled !== null ? firestoreEnabled : tool.status !== "disabled";
  
  const needsUpgrade = isPremium && !isPremiumUser;
  const isDisabled = !isEnabled;

  const navigate = useNavigate();

  // Add state for admin actions menu
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  // Add state for premium upgrade dialog
  const [showUpgradeDialog, setShowUpgradeDialog] = useState(false);
  // Add state for disabled tool dialog
  const [showDisabledDialog, setShowDisabledDialog] = useState(false);
  
  // Add state for upgrade process
  const [upgrading, setUpgrading] = useState(false);
  const [upgradeError, setUpgradeError] = useState<string | null>(null);
  const [upgradeSuccess, setUpgradeSuccess] = useState(false);

  // Helper function to sanitize a tool name for use as a Firestore document ID
  const sanitizeToolNameForFirestore = (name: string): string => {
    // Replace forward slashes with a safe character (underscore)
    return name.replace(/\//g, '_');
  };

  // Check Firestore for the status when component mounts
  useEffect(() => {
    const checkFirestoreToolStatus = async () => {
      try {
        const safeDocId = sanitizeToolNameForFirestore(tool.name);
        const toolDocRef = doc(db, "tools", safeDocId);
        const docSnap = await getDoc(toolDocRef);
        
        if (docSnap.exists()) {
          const toolData = docSnap.data();
          console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Tool data from Firestore:`, toolData);
          
          // If premium field exists in Firestore, use it
          if (toolData.premium !== undefined) {
            setFirestorePremium(toolData.premium);
            console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Tool ${tool.name} premium status from Firestore: ${toolData.premium}`);
          }
          
          // If enabled field exists in Firestore, use it
          if (toolData.enabled !== undefined) {
            setFirestoreEnabled(toolData.enabled);
            console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Tool ${tool.name} enabled status from Firestore: ${toolData.enabled}`);
          }
        }
      } catch (error) {
        console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error fetching tool status from Firestore:`, error);
      }
    };
    
    checkFirestoreToolStatus();
  }, [tool.name]);

  const handleCardClick = () => {
    // If tool is disabled and user is not admin, show disabled dialog
    if (isDisabled && !isAdmin) {
      setShowDisabledDialog(true);
      return;
    }
    
    // If premium tool and user is not premium or admin, show upgrade dialog
    if (needsUpgrade) {
      setShowUpgradeDialog(true);
      return;
    }
    
    // Otherwise navigate to the tool
    navigate(`/tools/${tool.id}`);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = (event?: React.MouseEvent) => {
    if (event) {
      event.stopPropagation();
    }
    setAnchorEl(null);
  };

  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onFavoriteToggle(tool);
  };

  const handleRemoveClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    handleMenuClose();
    if (onToolRemove) {
      onToolRemove(tool.id);
    }
  };

  const handleStatusToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    handleMenuClose();
    if (onToolStatusToggle) {
      const newStatus = isEnabled ? "disabled" : "enabled";
      onToolStatusToggle(tool.id, newStatus);
      
      // Update local state immediately for better UX
      setFirestoreEnabled(newStatus === "enabled");
    }
  };

  const handleAccessLevelChange = (e: React.MouseEvent) => {
    e.stopPropagation();
    handleMenuClose();
    if (onToolAccessLevelChange) {
      const newLevel = isPremium ? "normal" : "premium";
      onToolAccessLevelChange(tool.id, newLevel);
      
      // Update local state immediately for better UX
      setFirestorePremium(newLevel === "premium");
    }
  };

  const handleUpgradeDialogClose = () => {
    if (!upgrading) {
      setShowUpgradeDialog(false);
      // Reset states after closing
      setTimeout(() => {
        setUpgradeError(null);
        setUpgradeSuccess(false);
      }, 300);
    }
  };
  
  const handleDisabledDialogClose = () => {
    setShowDisabledDialog(false);
  };

  // Handle upgrade to premium action
  const handleUpgradeClick = async () => {
    if (!currentUser) {
      navigate('/login');
      return;
    }
    
    setUpgrading(true);
    setUpgradeError(null);
    
    try {
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Starting premium upgrade process...`);
      
      // Get reference to the user document
      const userDocRef = doc(db, "users", currentUser.uid);
      
      // Update the userType to premium in Firestore
      await setDoc(userDocRef, {
        userType: "premium",
        upgradedAt: serverTimestamp()
      }, { merge: true });
      
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Successfully updated user to premium in Firestore`);
      
      // Now also update the tool's premium status in Firestore for this tool
      const safeDocId = sanitizeToolNameForFirestore(tool.name);
      const toolRef = doc(db, "tools", safeDocId);
      
      await setDoc(toolRef, {
        premium: true,
        updatedAt: serverTimestamp()
      }, { merge: true });
      
      console.log(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Successfully updated tool ${safeDocId} premium status in Firestore`);
      
      // Update local state for immediate UI feedback
      setFirestorePremium(true);
      
      // Show success state
      setUpgradeSuccess(true);
      
      // Refresh user data in context
      await refreshUserData();
      
      // Close dialog after a delay
      setTimeout(() => {
        setShowUpgradeDialog(false);
        navigate(`/tools/${tool.id}`); // Navigate to the tool after upgrading
      }, 2000);
    } catch (error) {
      console.error(`[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Error upgrading to premium:`, error);
      setUpgradeError("Failed to upgrade your account. Please try again later.");
    } finally {
      setUpgrading(false);
    }
  };

  return (
    <>
      <Card
        sx={{
          width: { xs: "166px", sm: "166px", md: "260px" },
          height: "200px",
          display: "flex",
          flexDirection: "column",
          cursor: "pointer",
          p: 2,
          border: "1px solid #282828",
          backgroundColor: "main.background.default",
          "&:hover": { 
            borderColor: isDisabled 
              ? "#d32f2f"  // Red border for disabled tools
              : (needsUpgrade ? "#ffb300" : "#1ea54c") 
          },
          transition: "border-color 0.3s ease-in-out",
          overflow: "hidden",
          // Add semi-transparent overlay for disabled tools (even for non-admins)
          position: "relative",
          ...(isDisabled && {
            "&::after": {
              content: '""',
              position: "absolute",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: "rgba(0, 0, 0, 0.5)",
              zIndex: 1,
            },
          }),
        }}
        onClick={handleCardClick}
      >
        {/* Premium badge - now uses isPremium which considers Firestore data */}
        {isPremium && (
          <Box
            sx={{
              position: "absolute",
              top: 0,
              right: 0,
              backgroundColor: "#ffb300",
              color: "black",
              px: 1,
              py: 0.5,
              fontSize: "0.7rem",
              fontWeight: "bold",
              borderBottomLeftRadius: "4px",
              zIndex: 2,
            }}
          >
            PREMIUM
          </Box>
        )}

        {/* Disabled badge - visible to all users */}
        {isDisabled && (
          <Box
            sx={{
              position: "absolute",
              top: 0,
              left: 0,
              backgroundColor: "#d32f2f",
              color: "white",
              px: 1,
              py: 0.5,
              fontSize: "0.7rem",
              fontWeight: "bold",
              borderBottomRightRadius: "4px",
              zIndex: 2,
            }}
          >
            DISABLED
          </Box>
        )}

        {/* Premium lock overlay */}
        {needsUpgrade && !isDisabled && (
          <Box
            sx={{
              position: "absolute",
              top: "50%",
              right: "10px",
              transform: "translateY(-50%)",
              backgroundColor: "rgba(0, 0, 0, 0.6)",
              borderRadius: "50%",
              width: "40px",
              height: "40px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 2,
            }}
          >
            <LockIcon sx={{ color: "#ffb300", fontSize: "24px" }} />
          </Box>
        )}
        
        {/* Disabled lock overlay (for non-admins) */}
        {isDisabled && !isAdmin && (
          <Box
            sx={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              backgroundColor: "rgba(0, 0, 0, 0.6)",
              borderRadius: "50%",
              width: "60px",
              height: "60px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 2,
            }}
          >
            <BlockIcon sx={{ color: "#d32f2f", fontSize: "32px" }} />
          </Box>
        )}

        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            width: "100%",
            height: "100%",
            justifyContent: "space-between",
            mt: 1,
          }}
        >
          <Box
            sx={{
              display: "flex",
              alignItems: "flex-start",
              justifyContent: "space-between",
              flexDirection: "row",
              width: "100%",
              mb: 1,
            }}
          >
            <IconComponent
              sx={{
                fontSize: { xs: 40, sm: 45, md: 50 },
                color: isDisabled ? "#d32f2f" : (isPremium ? "#ffb300" : "custom.icon"),
                opacity: isDisabled ? 0.5 : 1,
              }}
            />
            <Box sx={{ display: "flex" }}>
              {/* Admin menu */}
              {isAdmin && (
                <>
                  <IconButton
                    onClick={handleMenuOpen}
                    sx={{ p: { xs: 0.5, sm: 0.75, md: 1 } }}
                  >
                    <MoreVertIcon sx={{ color: "custom.icon" }} />
                  </IconButton>
                  <Menu
                    anchorEl={anchorEl}
                    open={open}
                    onClose={(e) => handleMenuClose(e as React.MouseEvent)}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <MenuItem onClick={handleStatusToggle}>
                      {isEnabled ? (
                        <>
                          <BlockIcon
                            fontSize="small"
                            sx={{ mr: 1, color: "#d32f2f" }}
                          />
                          Disable Tool
                        </>
                      ) : (
                        <>
                          <CheckCircleIcon
                            fontSize="small"
                            sx={{ mr: 1, color: "#4caf50" }}
                          />
                          Enable Tool
                        </>
                      )}
                    </MenuItem>
                    <MenuItem onClick={handleAccessLevelChange}>
                      {isPremium ? (
                        <>
                          <StarBorderIcon
                            fontSize="small"
                            sx={{ mr: 1, color: "#a3a3a3" }}
                          />
                          Downgrade to Normal
                        </>
                      ) : (
                        <>
                          <StarIcon
                            fontSize="small"
                            sx={{ mr: 1, color: "#ffb300" }}
                          />
                          Upgrade to Premium
                        </>
                      )}
                    </MenuItem>
                    <MenuItem
                      onClick={handleRemoveClick}
                      sx={{ color: "#d32f2f" }}
                    >
                      <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
                      Remove Tool
                    </MenuItem>
                  </Menu>
                </>
              )}
              {/* Premium indicator for all tools */}
              {isPremium && (
                <Box 
                  sx={{ 
                    display: 'flex',
                    alignItems: 'center',
                    mr: 1
                  }}
                >
                  <StarIcon 
                    sx={{ 
                      color: "#ffb300", 
                      fontSize: "16px",
                      verticalAlign: 'middle',
                      opacity: isDisabled ? 0.5 : 1,
                    }} 
                  />
                </Box>
              )}
              {/* Favorite button */}
              <IconButton
                onClick={handleFavoriteClick}
                sx={{ p: { xs: 0.5, sm: 0.75, md: 1 } }}
                disabled={isDisabled && !isAdmin}
              >
                {isFavorite ? (
                  <FavoriteIcon 
                    color="error" 
                    sx={{ opacity: isDisabled ? 0.5 : 1 }}
                  />
                ) : (
                  <FavoriteIcon 
                    sx={{ 
                      color: "custom.icon", 
                      opacity: isDisabled ? 0.5 : 1, 
                    }}
                  />
                )}
              </IconButton>
            </Box>
          </Box>

          <Box
            sx={{
              flex: 1,
              overflow: "hidden",
              opacity: isDisabled ? 0.5 : 1,
            }}
          >
            <Typography
              variant="h6"
              sx={{
                fontSize: { xs: "0.9rem", sm: "1rem", md: "1.25rem" },
                fontWeight: 500,
                mb: 0.5,
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis",
                color: isDisabled ? "#d32f2f" : (isPremium && needsUpgrade ? "#ffb300" : "inherit"),
              }}
              title={tool.name}
            >
              {tool.name}
            </Typography>
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{
                display: "-webkit-box",
                WebkitLineClamp: { xs: 3, sm: 2, md: 3 },
                WebkitBoxOrient: "vertical",
                overflow: "hidden",
                textOverflow: "ellipsis",
                fontSize: { xs: "0.75rem", sm: "0.75rem", md: "0.875rem" },
              }}
              title={tool.description}
            >
              {tool.description}
            </Typography>
          </Box>
        </Box>
      </Card>

      {/* Premium Upgrade Dialog */}
      <Dialog
        open={showUpgradeDialog}
        onClose={handleUpgradeDialogClose}
        aria-labelledby="upgrade-dialog-title"
        PaperProps={{
          sx: {
            backgroundColor: "#2e2e2e",
            color: "#ffffff",
            borderRadius: "8px",
            border: "1px solid #3b956f",
            maxWidth: "500px",
          },
        }}
      >
        <DialogTitle
          id="upgrade-dialog-title"
          sx={{
            borderBottom: "1px solid rgba(255,255,255,0.1)",
            display: "flex",
            alignItems: "center",
          }}
        >
          <StarIcon sx={{ color: "#ffb300", mr: 1.5 }} />
          {upgradeSuccess ? "Upgrade Successful!" : "Premium Feature"}
        </DialogTitle>
        <DialogContent sx={{ py: 3 }}>
          {upgradeSuccess ? (
            <Box sx={{ textAlign: "center", py: 2 }}>
              <CheckCircleIcon sx={{ color: "#4caf50", fontSize: "64px", mb: 2 }} />
              <Typography variant="h6" sx={{ mb: 2 }}>
                Your account has been upgraded to Premium!
              </Typography>
              <Typography variant="body1">
                You now have access to {tool.name} and all premium features.
              </Typography>
            </Box>
          ) : (
            <>
              <Typography variant="h6" sx={{ mb: 2 }}>
                {tool.name} is a Premium Tool
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                This feature requires a premium subscription to access. Upgrade your
                account to unlock this tool and all other premium features.
              </Typography>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  background: "rgba(0,0,0,0.2)",
                  p: 2,
                  borderRadius: "4px",
                  mb: 2,
                }}
              >
                <Box
                  sx={{
                    mr: 2,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <StarIcon sx={{ color: "#ffb300", fontSize: "32px" }} />
                </Box>
                <Box>
                  <Typography variant="body1" sx={{ fontWeight: "bold", mb: 0.5 }}>
                    Premium Benefits:
                  </Typography>
                  <Typography variant="body2" sx={{ mb: 0.5 }}>
                    • Access to all premium tools
                  </Typography>
                  <Typography variant="body2" sx={{ mb: 0.5 }}>
                    • Priority customer support
                  </Typography>
                  <Typography variant="body2">
                    • Early access to new features
                  </Typography>
                </Box>
              </Box>
              
              {upgradeError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {upgradeError}
                </Alert>
              )}
              
              <Typography
                variant="caption"
                sx={{ color: "#a3a3a3", display: "block", textAlign: "center" }}
              >
                Current Time (UTC): {CURRENT_DATE_TIME}
              </Typography>
            </>
          )}
        </DialogContent>
        <DialogActions
          sx={{
            borderTop: "1px solid rgba(255,255,255,0.1)",
            p: 2,
            justifyContent: "space-between",
          }}
        >
          {!upgradeSuccess && !upgrading ? (
            <>
              <Button onClick={handleUpgradeDialogClose} color="inherit" disabled={upgrading}>
                Maybe Later
              </Button>
              <Button
                onClick={handleUpgradeClick}
                variant="contained"
                disabled={upgrading}
                sx={{
                  background:
                    "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                  "&:hover": {
                    background:
                      "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)",
                  },
                }}
              >
                {upgrading ? (
                  <>
                    <CircularProgress size={20} color="inherit" sx={{ mr: 1 }} />
                    Upgrading...
                  </>
                ) : (
                  "Upgrade to Premium"
                )}
              </Button>
            </>
          ) : upgradeSuccess ? (
            <Button
              onClick={() => {
                setShowUpgradeDialog(false);
                navigate(`/tools/${tool.id}`);
              }}
              variant="contained"
              fullWidth
              sx={{
                background:
                  "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                "&:hover": {
                  background:
                    "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)",
                },
              }}
            >
              Continue to {tool.name}
            </Button>
          ) : (
            <CircularProgress size={24} sx={{ mx: "auto" }} />
          )}
        </DialogActions>
      </Dialog>

      {/* Disabled Tool Dialog */}
      <Dialog
        open={showDisabledDialog}
        onClose={handleDisabledDialogClose}
        aria-labelledby="disabled-dialog-title"
        PaperProps={{
          sx: {
            backgroundColor: "#2e2e2e",
            color: "#ffffff",
            borderRadius: "8px",
            border: "1px solid #d32f2f",
            maxWidth: "500px",
          },
        }}
      >
        <DialogTitle
          id="disabled-dialog-title"
          sx={{
            borderBottom: "1px solid rgba(255,255,255,0.1)",
            display: "flex",
            alignItems: "center",
            color: "#d32f2f",
          }}
        >
          <BlockIcon sx={{ color: "#d32f2f", mr: 1.5 }} />
          Tool Unavailable
        </DialogTitle>
        <DialogContent sx={{ py: 3 }}>
          <Box sx={{ textAlign: "center", py: 2 }}>
            <BlockIcon sx={{ color: "#d32f2f", fontSize: "64px", mb: 2 }} />
            <Typography variant="h6" sx={{ mb: 2 }}>
              {tool.name} is currently unavailable
            </Typography>
            <Typography variant="body1" sx={{ mb: 2 }}>
              This tool has been temporarily disabled by the administrator. 
              Please try again later or contact support for more information.
            </Typography>
            <Typography
              variant="caption"
              sx={{ color: "#a3a3a3", display: "block", textAlign: "center", mt: 2 }}
            >
              Current Time (UTC): {CURRENT_DATE_TIME}
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions
          sx={{
            borderTop: "1px solid rgba(255,255,255,0.1)",
            p: 2,
            justifyContent: "center",
          }}
        >
          <Button
            onClick={handleDisabledDialogClose}
            variant="contained"
            sx={{
              minWidth: "120px",
              backgroundColor: "#d32f2f",
              "&:hover": {
                backgroundColor: "#b71c1c",
              },
            }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default ToolCard;