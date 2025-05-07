import React, { useState, useEffect, useMemo } from "react";
import {
  AppBar,
  Toolbar,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemText,
  Box,
  CssBaseline,
  TextField,
  InputAdornment,
  Button,
  Typography,
  Collapse,
  CircularProgress,
  Divider,
  ListItemIcon,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Tooltip,
  Badge,
  Slide,
  Snackbar,
  Alert,
  Backdrop,
  Paper,
} from "@mui/material";
import { TransitionProps } from "@mui/material/transitions";
import {
  HomeOutlined,
  Menu as MenuIcon,
  Logout as LogoutIcon,
  ExpandMoreOutlined,
  HelpOutline,
  FavoriteBorder,
  Favorite,
  Close as CloseIcon,
} from "@mui/icons-material";
import StarIcon from "@mui/icons-material/Star";
import EmojiEventsIcon from "@mui/icons-material/EmojiEvents";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import { useNavigate } from "react-router-dom";
import { Search as SearchIcon } from "lucide-react";
import * as MuiIcons from "@mui/icons-material";

// Firestore imports
import { doc, getDoc, updateDoc, serverTimestamp } from "firebase/firestore";
import { db } from "../firebaseConfig";

// --- Context and Auth Imports ---
import { useAuth } from "../context/AuthContext";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAllTools } from "../context/AllToolsContext";
import { signOut } from "firebase/auth";
import { auth } from "../firebaseConfig";

// --- Interface Import ---
import { PluginMetadata as Tool } from "../data/pluginList"; // Adjust path if needed

// Slide transition for dialog
const Transition = React.forwardRef(function Transition(
  props: TransitionProps & {
    children: React.ReactElement;
  },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

// --- MainLayout Component ---
const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const {
    currentUser,
    loading: authLoading,
    userType,
    refreshUserData,
  } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [openState, setOpenState] = useState<Record<string, boolean>>({});
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<Tool[]>([]);
  const [showSearchResults, setShowSearchResults] = useState(false);

  // Premium upgrade dialog states
  const [premiumDialogOpen, setPremiumDialogOpen] = useState(false);
  const [upgrading, setUpgrading] = useState(false);
  const [upgradeSuccess, setUpgradeSuccess] = useState(false);
  const [upgradeError, setUpgradeError] = useState<string | null>(null);

  const isPremiumUser = userType === "premium" || userType === "admin";

  const handleSearch = (query: string) => {
    setSearchQuery(query);

    if (!query.trim()) {
      setSearchResults([]);
      setShowSearchResults(false);
      return;
    }

    // Filter tools based on search query
    const results = allTools.filter(
      (tool) =>
        tool.name.toLowerCase().includes(query.toLowerCase()) ||
        (tool.description &&
          tool.description.toLowerCase().includes(query.toLowerCase()))
    );

    setSearchResults(results);
    setShowSearchResults(true);
  };

  const { favoriteTools, isLoading: favoritesLoading } = useFavoriteTools();
  const {
    allTools,
    isLoading: allToolsLoading,
    error: allToolsError,
  } = useAllTools();

  // Current time from user's message
  const currentTime = "2025-05-06 16:03:00";
  const currentUserLogin = "hanhiho";

  const handleLogout = async () => {
    try {
      await signOut(auth);
      console.log("User signed out successfully");
      navigate("/login");
    } catch (error) {
      console.error("Error signing out: ", error);
    }
  };

  const handleToggle = (key: string) => {
    setOpenState((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const groupedTools = useMemo(() => {
    if (!allTools || !Array.isArray(allTools)) return {};
    return allTools.reduce<Record<string, Tool[]>>((acc, tool) => {
      const category = tool.category || "Other";
      if (!acc[category]) acc[category] = [];
      acc[category].push(tool);
      return acc;
    }, {});
  }, [allTools]);

  const isAuthBarLoading = authLoading;
  const shouldRenderDrawerStructure = true; // Keep drawer structure always rendered

  // Calculate AppBar height for proper spacing
  const appBarHeight = 64; // Default height of MUI AppBar

  // Handle premium upgrade dialog open
  const handleUpgradeClick = () => {
    setPremiumDialogOpen(true);
  };

  // Handle premium upgrade dialog close
  const handleCloseDialog = () => {
    if (!upgrading) {
      setPremiumDialogOpen(false);

      // Reset states after a delay
      setTimeout(() => {
        setUpgradeSuccess(false);
        setUpgradeError(null);
      }, 500);
    }
  };

  // Handle premium upgrade request
  const handleUpgradeRequest = async () => {
    if (!currentUser) return;

    setUpgrading(true);
    setUpgradeError(null);

    try {
      // Get reference to the user document
      const userDocRef = doc(db, "users", currentUser.uid);

      // Update the userType to premium
      await updateDoc(userDocRef, {
        userType: "premium",
        upgradedAt: serverTimestamp(),
      });

      // Show success state
      setUpgradeSuccess(true);

      // Refresh user data in context
      await refreshUserData();

      // Close dialog after a delay
      setTimeout(() => {
        setPremiumDialogOpen(false);
        setUpgrading(false);

        // Reset success state after dialog closes
        setTimeout(() => {
          setUpgradeSuccess(false);
        }, 500);
      }, 2000);
    } catch (error) {
      console.error("Error upgrading to premium:", error);
      setUpgradeError(
        "Failed to upgrade your account. Please try again later."
      );
      setUpgrading(false);
    }
  };

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <CssBaseline />

      {/* Sidebar Drawer */}
      {shouldRenderDrawerStructure && (
        <Drawer
          open={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          sx={{
            width: 240,
            flexShrink: 0,
            [`& .MuiDrawer-paper`]: {
              width: 240,
              boxSizing: "border-box",
              paddingTop: `${appBarHeight}px`, // Add padding to top of drawer content
            },
          }}
        >
          {/* Drawer Header - No need for additional top margin since we added paddingTop to the Drawer */}
          <Box
            sx={{
              p: 2,
              textAlign: "center",
              cursor: "pointer",
              "&:hover": { transition: "transform 0.2s" },
              background:
                "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
              height: "130px",
              display: "flex",
              flexDirection: "column",
              justifyContent: "center",
            }}
            onClick={() => {
              navigate("/");
              setSidebarOpen(false);
            }}
          >
            <Typography
              variant="h5"
              sx={{ color: "#fff", fontWeight: "bold", mb: 1 }}
            >
              {" "}
              IT - TOOLS{" "}
            </Typography>
            <Typography variant="subtitle1" sx={{ color: "#fff" }}>
              {" "}
              Handy tools for developers{" "}
            </Typography>
          </Box>

          {/* Sidebar Content List */}
          <List sx={{ width: "100%", pt: 0 }}>
            {/* Favorites Section */}
            <div key="favorites">
              <ListItem
                onClick={() => handleToggle("favorites")}
                sx={{
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  flexDirection: "row",
                  padding: "3px 10px 0px 10px",
                  "&:hover": { color: "#ffffff" },
                  color: "#ffffff85",
                }}
              >
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    transition: "transform 0.3s",
                    transform: openState["favorites"]
                      ? "rotate(0deg)"
                      : "rotate(-90deg)",
                    marginRight: 1,
                  }}
                >
                  <ExpandMoreOutlined
                    sx={{ fontSize: "20px", color: "inherit" }}
                  />
                </Box>
                {/* Use ListItemIcon correctly */}
                <ListItemIcon
                  sx={{
                    minWidth: "auto",
                    mr: 1,
                    color: currentUser ? "#ffb300" : "#ffffff85",
                  }}
                >
                  {currentUser ? (
                    <Favorite sx={{ fontSize: "20px" }} />
                  ) : (
                    <FavoriteBorder sx={{ fontSize: "20px" }} />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary="Favorites"
                  sx={{
                    color: "#ffffff85",
                    "& .MuiTypography-root": { fontSize: "14px" },
                    "&:hover .MuiTypography-root": { color: "#ffffff" },
                  }}
                />
              </ListItem>
              <Collapse
                in={openState["favorites"]}
                timeout="auto"
                unmountOnExit
              >
                <List
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    marginLeft: "10px",
                    padding: "2px 5px",
                    position: "relative",
                  }}
                >
                  <Box
                    sx={{
                      position: "absolute",
                      left: 8,
                      top: 5,
                      bottom: 10,
                      width: "2px",
                      backgroundColor: "#2e2e2e",
                      zIndex: 0,
                    }}
                  />
                  {!currentUser ? ( // Message for anonymous users
                    <ListItem sx={{ pl: "24px" }}>
                      <ListItemText
                        primary="Log in to manage favorites."
                        sx={{
                          "& .MuiTypography-root": {
                            fontSize: "13px",
                            fontStyle: "italic",
                            color: "#ffffff85",
                          },
                        }}
                      />
                    </ListItem>
                  ) : favoritesLoading ? (
                    <ListItem>
                      <CircularProgress size={20} />
                    </ListItem>
                  ) : favoriteTools.length === 0 ? (
                    <ListItem sx={{ pl: "24px" }}>
                      <ListItemText
                        primary="No favorites yet."
                        sx={{
                          "& .MuiTypography-root": {
                            fontSize: "13px",
                            fontStyle: "italic",
                          },
                        }}
                      />
                    </ListItem>
                  ) : (
                    favoriteTools.map((tool) => {
                      const ToolIcon =
                        MuiIcons[tool.icon as keyof typeof MuiIcons] ||
                        HelpOutline;
                      return (
                        <ListItem
                          key={tool.id}
                          sx={{
                            cursor: "pointer",
                            "&:hover": {
                              backgroundColor: "#2e2e2e",
                              transition: "background-color 0.3s",
                            },
                            position: "relative",
                            marginLeft: "16px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "flex-start",
                            padding: "5px 7px",
                            borderRadius: 1,
                            marginRight: 1,
                            width: "205px",
                            zIndex: 1,
                          }}
                          onClick={() => {
                            navigate(`/tools/${tool.id}`);
                            setSidebarOpen(false);
                          }}
                        >
                          <Box
                            sx={{
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              marginRight: 1,
                            }}
                          >
                            {" "}
                            <ToolIcon sx={{ fontSize: "20px" }} />{" "}
                          </Box>
                          <ListItemText
                            primary={tool.name}
                            sx={{
                              "& .MuiTypography-root": { fontSize: "14px" },
                            }}
                          />
                        </ListItem>
                      );
                    })
                  )}
                </List>
              </Collapse>
            </div>
            <Divider sx={{ my: 1, borderColor: "rgba(255, 255, 255, 0.12)" }} />{" "}
            {/* Separator */}
            {/* Categories Section */}
            {allToolsLoading ? (
              <ListItem>
                <CircularProgress size={20} sx={{ margin: "auto" }} />
              </ListItem>
            ) : allToolsError ? (
              <ListItem>
                <Typography variant="caption" color="error">
                  Failed to load tools.
                </Typography>
              </ListItem>
            ) : (
              Object.keys(groupedTools)
                .sort()
                .map((category) => (
                  <div key={category}>
                    <ListItem
                      onClick={() => handleToggle(category)}
                      sx={{
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        flexDirection: "row",
                        padding: "3px 10px 0px 10px",
                        "&:hover": { color: "#ffffff" },
                        color: "#ffffff85",
                      }}
                    >
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          transition: "transform 0.3s",
                          transform: openState[category]
                            ? "rotate(0deg)"
                            : "rotate(-90deg)",
                          marginRight: 1,
                        }}
                      >
                        <ExpandMoreOutlined
                          sx={{ fontSize: "20px", color: "inherit" }}
                        />
                      </Box>
                      <ListItemText
                        primary={category}
                        sx={{
                          color: "#ffffff85",
                          "& .MuiTypography-root": { fontSize: "14px" },
                          "&:hover .MuiTypography-root": { color: "#ffffff" },
                        }}
                      />
                    </ListItem>
                    <Collapse
                      in={openState[category]}
                      timeout="auto"
                      unmountOnExit
                    >
                      <List
                        sx={{
                          display: "flex",
                          flexDirection: "column",
                          marginLeft: "10px",
                          padding: "2px 5px",
                          position: "relative",
                        }}
                      >
                        <Box
                          sx={{
                            position: "absolute",
                            left: 8,
                            top: 5,
                            bottom: 10,
                            width: "2px",
                            backgroundColor: "#2e2e2e",
                            zIndex: 0,
                          }}
                        />
                        {groupedTools[category].map((tool) => {
                          const ToolIcon =
                            MuiIcons[tool.icon as keyof typeof MuiIcons] ||
                            HelpOutline;
                          return (
                            <ListItem
                              key={tool.id}
                              sx={{
                                cursor: "pointer",
                                "&:hover": {
                                  backgroundColor: "#2e2e2e",
                                  transition: "background-color 0.3s",
                                },
                                position: "relative",
                                marginLeft: "16px",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "flex-start",
                                padding: "5px 7px",
                                borderRadius: 1,
                                marginRight: 1,
                                width: "205px",
                                zIndex: 1,
                              }}
                              onClick={() => {
                                navigate(`/tools/${tool.id}`);
                                setSidebarOpen(false);
                              }}
                            >
                              <Box
                                sx={{
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  marginRight: 1,
                                }}
                              >
                                {" "}
                                <ToolIcon sx={{ fontSize: "20px" }} />{" "}
                              </Box>
                              <ListItemText
                                primary={tool.name}
                                sx={{
                                  "& .MuiTypography-root": { fontSize: "14px" },
                                }}
                              />
                            </ListItem>
                          );
                        })}
                      </List>
                    </Collapse>
                  </div>
                ))
            )}
          </List>
        </Drawer>
      )}

      {/* Main Content Area */}
      <Box
        sx={{
          flexGrow: 1,
          display: "flex",
          flexDirection: "column",
          backgroundColor: "#1c1c1c",
        }}
      >
        {/* Top Bar */}
        <AppBar
          position="fixed"
          elevation={0}
          sx={{
            width: "100%",
            zIndex: (theme) => theme.zIndex.drawer + 1,
            backgroundColor: "#1c1c1c",
            display: "flex",
            justifyContent: "space-between",
            flexDirection: "row",
            alignItems: "center",
            padding: "5px 16px",
          }}
        >
          <Toolbar sx={{ gap: 1 }}>
            {shouldRenderDrawerStructure && (
              <IconButton
                edge="start"
                color="inherit"
                onClick={() => setSidebarOpen(true)}
                sx={{ mr: 1 }}
              >
                <MenuIcon />
              </IconButton>
            )}
            <IconButton
              edge="start"
              color="inherit"
              onClick={() => navigate("/")}
            >
              <HomeOutlined />
            </IconButton>

            {/* Premium Upgrade Button - Only show for logged-in non-premium users */}
            {currentUser && !isPremiumUser && (
              <Tooltip title="Upgrade to Premium">
                <Button
                  variant="contained"
                  size="small"
                  onClick={handleUpgradeClick}
                  startIcon={<StarIcon />}
                  sx={{
                    ml: 1,
                    background:
                      "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
                    color: "#000",
                    fontWeight: "bold",
                    fontSize: "0.7rem",
                    height: "30px",
                    border: "1px solid #FFB300",
                    boxShadow: "0 3px 5px 2px rgba(255, 179, 0, .3)",
                    textTransform: "none",
                    "&:hover": {
                      background:
                        "linear-gradient(45deg, #FFA000 30%, #FFD54F 90%)",
                      boxShadow: "0 4px 6px 2px rgba(255, 179, 0, .4)",
                    },
                  }}
                >
                  Upgrade
                </Button>
              </Tooltip>
            )}

            {/* Premium Badge - Show for premium users */}
            {currentUser && isPremiumUser && (
              <Tooltip title="Premium Member">
                <Badge
                  badgeContent=""
                  sx={{
                    ml: 1,
                    "& .MuiBadge-badge": {
                      bgcolor: "#FFB300",
                      color: "#FFB300",
                      boxShadow: "0 0 0 2px #1c1c1c",
                      "&::after": {
                        position: "absolute",
                        top: 0,
                        left: 0,
                        width: "100%",
                        height: "100%",
                        borderRadius: "50%",
                        animation: "ripple 1.2s infinite ease-in-out",
                        border: "1px solid currentColor",
                        content: '""',
                      },
                    },
                    "@keyframes ripple": {
                      "0%": {
                        transform: "scale(.8)",
                        opacity: 1,
                      },
                      "100%": {
                        transform: "scale(2.4)",
                        opacity: 0,
                      },
                    },
                  }}
                >
                  <StarIcon sx={{ color: "#FFB300", fontSize: "20px" }} />
                </Badge>
              </Tooltip>
            )}
          </Toolbar>

          {/* Search Bar */}
          <TextField
            variant="outlined"
            placeholder="Search..."
            size="small"
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            sx={{
              backgroundColor: "#2e2e2e",
              borderRadius: 2,
              flexGrow: 1,
              "& .MuiOutlinedInput-root": {
                height: 36,
                "&.Mui-focused fieldset": {
                  borderWidth: "1px",
                  borderColor: "#1ea54c",
                },
              },
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment
                  position="start"
                  sx={{ color: "#727272", width: 18, height: 18 }}
                >
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          {/* Search results dropdown */}
          {showSearchResults && searchQuery.trim() !== "" && (
            <>
              {/* Backdrop overlay to handle click outside */}
              <Box
                sx={{
                  position: "fixed",
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  bgcolor: "rgba(0, 0, 0, 0.5)",
                  zIndex: 1200,
                }}
                onClick={() => setShowSearchResults(false)}
              />

              <Paper
                sx={{
                  position: "fixed", // Changed from absolute to fixed
                  top: "50%", // Center vertically
                  left: "50%",
                  transform: "translate(-50%, -50%)", // Center both horizontally and vertically
                  maxHeight: "80vh", // Limit height to 80% of viewport
                  overflow: "auto",
                  zIndex: 1300, // Higher than backdrop
                  bgcolor: "#2e2e2e",
                  border: "1px solid #3e3e3e",
                  borderRadius: 1,
                  boxShadow: "0 8px 16px rgba(0, 0, 0, 0.3)",
                  width: "500px",
                  maxWidth: "95vw", // Responsive width
                }}
                elevation={24} // Higher elevation for more prominence
              >
                {/* Optional header with search info */}
                <Box
                  sx={{
                    p: 2,
                    borderBottom: "1px solid #3e3e3e",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Typography variant="subtitle1">
                    Search Results for "{searchQuery}"
                  </Typography>
                  <IconButton
                    size="small"
                    onClick={() => setShowSearchResults(false)}
                    sx={{ color: "#aaa" }}
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Box>

                {searchResults.length > 0 ? (
                  <List
                    sx={{
                      padding: 0,
                      margin: 0,
                      maxHeight: "calc(80vh - 60px)", // Adjust for header height
                      overflowY: "auto",
                    }}
                  >
                    {searchResults.map((tool) => {
                      const ToolIcon =
                        MuiIcons[tool.icon as keyof typeof MuiIcons] ||
                        HelpOutline;
                      return (
                        <ListItem
                          key={tool.id}
                          onClick={() => {
                            navigate(`/tools/${tool.id}`);
                            setShowSearchResults(false);
                            setSearchQuery("");
                          }}
                          sx={{
                            "&:hover": {
                              backgroundColor: "#3e3e3e",
                              transition: "background-color 0.3s",
                            },
                            padding: "12px 16px", // More padding for better UX
                            cursor: "pointer",
                            transition: "all 0.2s",
                          }}
                          divider
                        >
                          <ListItemIcon>
                            <ToolIcon sx={{ color: "#1ea54c" }} />
                          </ListItemIcon>
                          <ListItemText
                            primary={
                              <Typography
                                variant="body1"
                                sx={{ fontWeight: 500 }}
                              >
                                {tool.name}
                              </Typography>
                            }
                            secondary={
                              tool.description && (
                                <Typography
                                  variant="body2"
                                  sx={{ color: "#aaa", mt: 0.5 }}
                                >
                                  {tool.description.slice(0, 100) +
                                    (tool.description.length > 100
                                      ? "..."
                                      : "")}
                                </Typography>
                              )
                            }
                          />
                        </ListItem>
                      );
                    })}
                  </List>
                ) : (
                  <Box sx={{ p: 3, textAlign: "center" }}>
                    <Typography variant="body1">
                      No tools found matching "{searchQuery}"
                    </Typography>
                  </Box>
                )}

                {/* Footer with timestamp */}
                <Box
                  sx={{
                    p: 2,
                    borderTop: "1px solid #3e3e3e",
                    bgcolor: "rgba(0,0,0,0.2)",
                    fontSize: "0.75rem",
                    color: "#888",
                    textAlign: "center",
                  }}
                >
                  {`Current Time (UTC): 2025-05-06 19:21:51 • ${
                    searchResults.length
                  } result${searchResults.length !== 1 ? "s" : ""}`}
                </Box>
              </Paper>
            </>
          )}
          {/* Login/Logout Buttons */}
          <Box
            sx={{
              display: "flex",
              gap: 1,
              marginLeft: 2,
              marginRight: 2,
              alignItems: "center",
              justifyContent: "center",
              height: "100%",
            }}
          >
            {isAuthBarLoading ? (
              <CircularProgress size={24} color="inherit" />
            ) : !currentUser ? (
              <>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={() => navigate("/login")}
                  sx={{
                    borderRadius: 2,
                    background:
                      "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                    "&:hover": {
                      background:
                        "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)",
                    },
                    textTransform: "none",
                    padding: "6px 16px",
                  }}
                >
                  Log In
                </Button>
                <Button
                  variant="text"
                  color="inherit"
                  onClick={() => navigate("/signup")}
                  sx={{
                    borderRadius: 2,
                    position: "relative",
                    padding: "6px 16px",
                    overflow: "hidden",
                    textTransform: "none",
                    "&::before": {
                      content: '""',
                      position: "absolute",
                      top: 0,
                      left: 0,
                      width: "100%",
                      height: "100%",
                      borderRadius: 2,
                      padding: "2px",
                      background:
                        "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                      WebkitMask:
                        "linear-gradient(white, white) content-box, linear-gradient(white, white)",
                      WebkitMaskComposite: "destination-out",
                      maskComposite: "exclude",
                    },
                    "&:hover": { backgroundColor: "rgba(255, 255, 255, 0.08)" },
                  }}
                >
                  Sign Up
                </Button>
              </>
            ) : (
              <>
                <Typography
                  variant="caption"
                  sx={{
                    color: isPremiumUser ? "#FFB300" : "text.secondary",
                    mr: 1,
                    display: { xs: "none", sm: "flex" },
                    alignItems: "center",
                  }}
                >
                  {isPremiumUser && (
                    <StarIcon
                      sx={{ fontSize: "0.8rem", mr: 0.5, color: "#FFB300" }}
                    />
                  )}
                  {userType === "admin"
                    ? "Admin"
                    : userType === "premium"
                    ? "Premium"
                    : "Normal"}{" "}
                  account
                </Typography>
                <Button
                  variant="text"
                  color="inherit"
                  size="small"
                  onClick={handleLogout}
                  startIcon={<LogoutIcon fontSize="small" />}
                  sx={{
                    borderRadius: 2,
                    position: "relative",
                    padding: "6px 16px",
                    overflow: "hidden",
                    "&::before": {
                      content: '""',
                      position: "absolute",
                      top: 0,
                      left: 0,
                      width: "100%",
                      height: "100%",
                      borderRadius: 2,
                      padding: "2px",
                      background:
                        "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                      WebkitMask:
                        "linear-gradient(white, white) content-box, linear-gradient(white, white)",
                      WebkitMaskComposite: "destination-out",
                      maskComposite: "exclude",
                    },
                    "&:hover": { backgroundColor: "rgba(255, 255, 255, 0.08)" },
                    textTransform: "none",
                  }}
                >
                  Log out
                </Button>
              </>
            )}
          </Box>
        </AppBar>

        {/* Page Content */}
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            mt: `${appBarHeight}px`, // Use the same appBarHeight variable for consistency
            p: 2,
            width: "100%",
          }}
        >
          {children}
        </Box>
      </Box>

      {/* Premium Upgrade Dialog */}
      <Dialog
        open={premiumDialogOpen}
        TransitionComponent={Transition}
        keepMounted
        onClose={handleCloseDialog}
        aria-describedby="premium-upgrade-dialog-description"
        PaperProps={{
          sx: {
            borderRadius: "12px",
            backgroundColor: "#1c1c1c",
            backgroundImage:
              "linear-gradient(rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.05))",
            maxWidth: "500px",
            width: "100%",
            overflowY: "visible",
            position: "relative",
            p: 0,
          },
        }}
      >
        {/* Premium badge decorative element */}
        <Box
          sx={{
            position: "absolute",
            top: "-25px",
            left: "50%",
            transform: "translateX(-50%)",
            width: "50px",
            height: "50px",
            backgroundColor: "#1c1c1c",
            borderRadius: "50%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            boxShadow: "0 0 15px rgba(255, 179, 0, 0.7)",
            border: "2px solid #FFB300",
            zIndex: 1,
          }}
        >
          {upgradeSuccess ? (
            <CheckCircleIcon sx={{ fontSize: "30px", color: "#4caf50" }} />
          ) : (
            <StarIcon sx={{ fontSize: "30px", color: "#FFB300" }} />
          )}
        </Box>

        {/* Golden gradient header */}
        <Box
          sx={{
            height: "80px",
            background: "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
            borderTopLeftRadius: "12px",
            borderTopRightRadius: "12px",
            position: "relative",
            overflow: "hidden",
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
        </Box>

        {/* Content */}
        <DialogContent sx={{ px: 3, py: 4, mt: 2 }}>
          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              mb: 3,
            }}
          >
            <Typography
              variant="h4"
              sx={{
                fontWeight: "bold",
                mb: 1,
                background: "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
              }}
            >
              {upgradeSuccess ? "Congratulations!" : "Upgrade to Premium"}
            </Typography>

            <Typography
              variant="body1"
              textAlign="center"
              sx={{ opacity: 0.8 }}
            >
              {upgradeSuccess
                ? "You are now a premium member with access to all premium features!"
                : "Get access to exclusive tools and premium features"}
            </Typography>
          </Box>

          {!upgradeSuccess && (
            <>
              <Box sx={{ mb: 4 }}>
                {/* Premium features list */}
                <List sx={{ py: 0 }}>
                  <ListItem sx={{ pl: 0 }}>
                    <ListItemIcon sx={{ minWidth: "40px" }}>
                      <StarIcon sx={{ color: "#FFB300" }} />
                    </ListItemIcon>
                    <ListItemText
                      primary="Access to all premium tools"
                      secondary="Use our advanced tools for extensive functionality"
                    />
                  </ListItem>

                  <ListItem sx={{ pl: 0 }}>
                    <ListItemIcon sx={{ minWidth: "40px" }}>
                      <EmojiEventsIcon sx={{ color: "#FFB300" }} />
                    </ListItemIcon>
                    <ListItemText
                      primary="Priority support"
                      secondary="Get faster responses from our support team"
                    />
                  </ListItem>

                  <ListItem sx={{ pl: 0 }}>
                    <ListItemIcon sx={{ minWidth: "40px" }}>
                      <FavoriteBorder sx={{ color: "#FFB300" }} />
                    </ListItemIcon>
                    <ListItemText
                      primary="Unlimited favorites"
                      secondary="Save as many tools as you want for quick access"
                    />
                  </ListItem>
                </List>
              </Box>

              {/* Current time info */}
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  flexDirection: "column",
                  opacity: 0.7,
                  mb: 2,
                }}
              >
                <Typography variant="caption" textAlign="center">
                  Current Date (UTC): {currentTime}
                </Typography>
                <Typography variant="caption" textAlign="center">
                  User: {currentUserLogin}
                </Typography>
              </Box>
            </>
          )}

          {upgradeError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {upgradeError}
            </Alert>
          )}

          {/* Animated success message */}
          <Collapse in={upgradeSuccess}>
            <Box
              sx={{
                textAlign: "center",
                my: 3,
                p: 3,
                border: "1px solid rgba(76, 175, 80, 0.5)",
                borderRadius: "8px",
                backgroundColor: "rgba(76, 175, 80, 0.1)",
              }}
            >
              <CheckCircleIcon
                sx={{ fontSize: "48px", color: "#4caf50", mb: 1 }}
              />
              <Typography variant="h6" gutterBottom>
                Upgrade Successful!
              </Typography>
              <Typography variant="body2">
                Your account has been upgraded to Premium. You now have access
                to all premium features.
              </Typography>
            </Box>
          </Collapse>
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 3 }}>
          {!upgradeSuccess && (
            <>
              <Button
                onClick={handleCloseDialog}
                disabled={upgrading}
                sx={{ color: "text.secondary" }}
              >
                Cancel
              </Button>
              <Button
                variant="contained"
                onClick={handleUpgradeRequest}
                disabled={upgrading}
                startIcon={
                  upgrading ? (
                    <CircularProgress size={20} color="inherit" />
                  ) : (
                    <StarIcon />
                  )
                }
                sx={{
                  background:
                    "linear-gradient(45deg, #FF8E01 30%, #FFB300 90%)",
                  color: "#000",
                  fontWeight: "bold",
                  boxShadow: "0 3px 5px 2px rgba(255, 179, 0, .3)",
                  "&:hover": {
                    background:
                      "linear-gradient(45deg, #FFA000 30%, #FFD54F 90%)",
                  },
                  "&.Mui-disabled": {
                    background: "rgba(255, 179, 0, 0.3)",
                    color: "rgba(0, 0, 0, 0.3)",
                  },
                }}
              >
                {upgrading ? "Upgrading..." : "Upgrade Now"}
              </Button>
            </>
          )}

          {upgradeSuccess && (
            <Button
              variant="contained"
              onClick={handleCloseDialog}
              sx={{
                width: "100%",
                background:
                  "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)",
                "&:hover": {
                  background:
                    "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)",
                },
              }}
            >
              Start Using Premium Features
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MainLayout;
