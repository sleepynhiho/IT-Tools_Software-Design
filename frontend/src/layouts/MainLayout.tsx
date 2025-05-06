// src/components/layout/MainLayout.tsx (Corrected Version with ListItemIcon import)

import React, { useState, useEffect, useMemo } from "react";
import {
  AppBar, Toolbar, IconButton, Drawer, List, ListItem, ListItemText, Box, CssBaseline,
  TextField, InputAdornment, Button, Typography, Collapse, CircularProgress, Divider,
  ListItemIcon // <<<--- IMPORT ADDED HERE ---<<<
} from "@mui/material";
import {
  HomeOutlined, Menu as MenuIcon, Logout as LogoutIcon, ExpandMoreOutlined, HelpOutline, FavoriteBorder, Favorite
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { Search as SearchIcon } from "lucide-react";
import * as MuiIcons from "@mui/icons-material";

// --- Context and Auth Imports ---
import { useAuth } from "../context/AuthContext";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { useAllTools } from "../context/AllToolsContext";
import { signOut } from "firebase/auth";
import { auth } from "../firebaseConfig";

// --- Interface Import ---
import { PluginMetadata as Tool } from "../data/pluginList"; // Adjust path if needed

// --- MainLayout Component ---
const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const { currentUser, loading: authLoading } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [openState, setOpenState] = useState<Record<string, boolean>>({});

  const { favoriteTools, isLoading: favoritesLoading } = useFavoriteTools();
  const { allTools, isLoading: allToolsLoading, error: allToolsError } = useAllTools();

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
          const category = tool.category || 'Other';
          if (!acc[category]) acc[category] = [];
          acc[category].push(tool);
          return acc;
      }, {});
  }, [allTools]);

  const isAuthBarLoading = authLoading;
  const shouldRenderDrawerStructure = true; // Keep drawer structure always rendered

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <CssBaseline />

      {/* Sidebar Drawer */}
      {shouldRenderDrawerStructure && (
          <Drawer
              open={sidebarOpen}
              onClose={() => setSidebarOpen(false)}
              sx={{
                  width: 240, flexShrink: 0,
                  [`& .MuiDrawer-paper`]: { width: 240, boxSizing: 'border-box' },
              }}
          >
              {/* Drawer Header */}
              <Box
                  sx={{ p: 2, textAlign: "center", cursor: "pointer", "&:hover": { transition: "transform 0.2s" }, background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)", height: "130px", display: "flex", flexDirection: "column", justifyContent: "center" }}
                  onClick={() => { navigate("/"); setSidebarOpen(false); }} >
                  <Typography variant="h5" sx={{ color: "#fff", fontWeight: "bold", mb: 1 }}> IT - TOOLS </Typography>
                  <Typography variant="subtitle1" sx={{ color: "#fff" }}> Handy tools for developers </Typography>
              </Box>

              {/* Sidebar Content List */}
              <List sx={{ width: '100%', pt: 0 }}>
                  {/* Favorites Section */}
                  <div key="favorites">
                      <ListItem onClick={() => handleToggle("favorites")} sx={{ cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "space-between", flexDirection: "row", padding: "3px 10px 0px 10px", "&:hover": { color: "#ffffff", }, color: "#ffffff85", }}>
                          <Box sx={{ display: "flex", alignItems: "center", justifyContent: "center", transition: "transform 0.3s", transform: openState["favorites"] ? "rotate(0deg)" : "rotate(-90deg)", marginRight: 1, }} >
                              <ExpandMoreOutlined sx={{ fontSize: "20px", color: "inherit", }} />
                          </Box>
                          {/* Use ListItemIcon correctly */}
                          <ListItemIcon sx={{ minWidth: 'auto', mr: 1, color: currentUser ? '#ffb300' : '#ffffff85' }}>
                              {currentUser ? <Favorite sx={{ fontSize: "20px" }}/> : <FavoriteBorder sx={{ fontSize: "20px" }}/>}
                          </ListItemIcon>
                          <ListItemText primary="Favorites" sx={{ color: "#ffffff85", "& .MuiTypography-root": { fontSize: "14px", }, "&:hover .MuiTypography-root": { color: "#ffffff", }, }} />
                      </ListItem>
                      <Collapse in={openState["favorites"]} timeout="auto" unmountOnExit >
                          <List sx={{ display: "flex", flexDirection: "column", marginLeft: "10px", padding: "2px 5px", position: 'relative' }} >
                              <Box sx={{ position: "absolute", left: 8, top: 5, bottom: 10, width: "2px", backgroundColor: "#2e2e2e", zIndex: 0 }} />
                              {!currentUser ? ( // Message for anonymous users
                                  <ListItem sx={{ pl: '24px' }}><ListItemText primary="Log in to manage favorites." sx={{ '& .MuiTypography-root': { fontSize: '13px', fontStyle: 'italic', color: '#ffffff85' } }} /></ListItem>
                              ): favoritesLoading ? (
                                  <ListItem><CircularProgress size={20} /></ListItem>
                              ) : favoriteTools.length === 0 ? (
                                  <ListItem sx={{ pl: '24px' }}><ListItemText primary="No favorites yet." sx={{ '& .MuiTypography-root': { fontSize: '13px', fontStyle: 'italic' } }} /></ListItem>
                              ) : (
                                  favoriteTools.map((tool) => {
                                      const ToolIcon = MuiIcons[tool.icon as keyof typeof MuiIcons] || HelpOutline;
                                      return (
                                          <ListItem key={tool.id} sx={{ cursor: "pointer", "&:hover": { backgroundColor: "#2e2e2e", transition: "background-color 0.3s", }, position: "relative", marginLeft: "16px", display: "flex", alignItems: "center", justifyContent: "flex-start", padding: "5px 7px", borderRadius: 1, marginRight: 1, width: "205px", zIndex: 1 }} onClick={() => { navigate(`/tools/${tool.id}`); setSidebarOpen(false); }}>
                                              <Box sx={{ display: "flex", alignItems: "center", justifyContent: "center", marginRight: 1, }} > <ToolIcon sx={{ fontSize: "20px" }} /> </Box>
                                              <ListItemText primary={tool.name} sx={{ "& .MuiTypography-root": { fontSize: "14px", }, }} />
                                          </ListItem>
                                      );
                                  })
                              )}
                          </List>
                      </Collapse>
                  </div>

                  <Divider sx={{ my: 1, borderColor: 'rgba(255, 255, 255, 0.12)' }} /> {/* Separator */}

                  {/* Categories Section */}
                  {allToolsLoading ? (
                     <ListItem><CircularProgress size={20} sx={{ margin: 'auto' }} /></ListItem>
                  ) : allToolsError ? (
                     <ListItem><Typography variant="caption" color="error">Failed to load tools.</Typography></ListItem>
                  ) : (
                     Object.keys(groupedTools).sort().map((category) => (
                         <div key={category}>
                             <ListItem onClick={() => handleToggle(category)} sx={{ cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "space-between", flexDirection: "row", padding: "3px 10px 0px 10px", "&:hover": { color: "#ffffff", }, color: "#ffffff85", }}>
                                 <Box sx={{ display: "flex", alignItems: "center", justifyContent: "center", transition: "transform 0.3s", transform: openState[category] ? "rotate(0deg)" : "rotate(-90deg)", marginRight: 1, }}>
                                     <ExpandMoreOutlined sx={{ fontSize: "20px", color: "inherit", }} />
                                 </Box>
                                 <ListItemText primary={category} sx={{ color: "#ffffff85", "& .MuiTypography-root": { fontSize: "14px", }, "&:hover .MuiTypography-root": { color: "#ffffff", }, }} />
                             </ListItem>
                             <Collapse in={openState[category]} timeout="auto" unmountOnExit >
                                 <List sx={{ display: "flex", flexDirection: "column", marginLeft: "10px", padding: "2px 5px", position: 'relative' }} >
                                     <Box sx={{ position: "absolute", left: 8, top: 5, bottom: 10, width: "2px", backgroundColor: "#2e2e2e", zIndex: 0 }} />
                                     {groupedTools[category].map((tool) => {
                                         const ToolIcon = MuiIcons[tool.icon as keyof typeof MuiIcons] || HelpOutline;
                                         return (
                                             <ListItem key={tool.id} sx={{ cursor: "pointer", "&:hover": { backgroundColor: "#2e2e2e", transition: "background-color 0.3s", }, position: "relative", marginLeft: "16px", display: "flex", alignItems: "center", justifyContent: "flex-start", padding: "5px 7px", borderRadius: 1, marginRight: 1, width: "205px", zIndex: 1 }} onClick={() => { navigate(`/tools/${tool.id}`); setSidebarOpen(false); }}>
                                                 <Box sx={{ display: "flex", alignItems: "center", justifyContent: "center", marginRight: 1, }} > <ToolIcon sx={{ fontSize: "20px" }} /> </Box>
                                                 <ListItemText primary={tool.name} sx={{ "& .MuiTypography-root": { fontSize: "14px", }, }} />
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
      <Box sx={{ flexGrow: 1, display: "flex", flexDirection: "column", backgroundColor: "#1c1c1c", }} >
        {/* Top Bar */}
        <AppBar position="fixed" elevation={0} sx={{ width: "100%", zIndex: (theme) => theme.zIndex.drawer + 1, backgroundColor: "#1c1c1c", display: "flex", justifyContent: "space-between", flexDirection: "row", alignItems: "center", padding: "5px 16px", }} >
          <Toolbar>
            {shouldRenderDrawerStructure && (
               <IconButton edge="start" color="inherit" onClick={() => setSidebarOpen(true)} sx={{ mr: 1 }}>
                   <MenuIcon />
               </IconButton>
            )}
            <IconButton edge="start" color="inherit" onClick={() => navigate("/")} > <HomeOutlined /> </IconButton>
          </Toolbar>
          {/* Search Bar */}
          <TextField variant="outlined" placeholder="Search..." size="small" sx={{ backgroundColor: "#2e2e2e", borderRadius: 2, flexGrow: 1, "& .MuiOutlinedInput-root": { height: 36, "&.Mui-focused fieldset": { borderWidth: "1px", borderColor: "#1ea54c", }, }, }} InputProps={{ startAdornment: ( <InputAdornment position="start" sx={{ color: "#727272", width: 18, height: 18 }}> <SearchIcon /> </InputAdornment> ), }} />
          {/* Login/Logout Buttons */}
          <Box sx={{ display: "flex", gap: 1, marginLeft: 2, marginRight: 2, alignItems: "center", }}>
            {isAuthBarLoading ? ( <CircularProgress size={24} color="inherit" /> ) : !currentUser ? (
              <> <Button variant="contained" color="primary" onClick={() => navigate("/login")} sx={{ borderRadius: 2, background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)", "&:hover": { background: "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)", }, textTransform: "none", }} > Log In </Button> <Button variant="text" color="inherit" onClick={() => navigate("/signup")} sx={{ borderRadius: 2, position: "relative", padding: "6px 16px", overflow: "hidden", textTransform: "none", "&::before": { content: '""', position: "absolute", top: 0, left: 0, width: "100%", height: "100%", borderRadius: 2, padding: "2px", background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)", WebkitMask: "linear-gradient(white, white) content-box, linear-gradient(white, white)", WebkitMaskComposite: "destination-out", maskComposite: "exclude", }, "&:hover": { backgroundColor: "rgba(255, 255, 255, 0.08)", }, }} > Sign Up </Button> </>
            ) : (
              <> <Button variant="text" color="inherit" size="small" onClick={handleLogout} startIcon={<LogoutIcon fontSize="small" />} sx={{ borderRadius: 2, position: "relative", padding: "6px 16px", overflow: "hidden", "&::before": { content: '""', position: "absolute", top: 0, left: 0, width: "100%", height: "100%", borderRadius: 2, padding: "2px", background: "linear-gradient(48deg,#25636c 0%,#3b956f 60%,#14a058 100%)", WebkitMask: "linear-gradient(white, white) content-box, linear-gradient(white, white)", WebkitMaskComposite: "destination-out", maskComposite: "exclude", }, "&:hover": { backgroundColor: "rgba(255, 255, 255, 0.08)", }, textTransform: "none", }} > Log out </Button> </>
            )}
          </Box>
        </AppBar>

        {/* Page Content */}
        <Box component="main" sx={{ flexGrow: 1, display: "flex", alignItems: "center", justifyContent: "center", mt: "64px", p: 2, width: "100%", }} >
          {children}
        </Box>
      </Box>
    </Box>
  );
};

export default MainLayout;