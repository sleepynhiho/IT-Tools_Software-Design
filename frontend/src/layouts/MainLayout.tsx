import { useState } from "react";
import {
  AppBar,
  Toolbar,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemText,
  Box,
  Typography,
  CssBaseline,
} from "@mui/material";
import { Menu as MenuIcon } from "@mui/icons-material";
import { tools } from "../data/tools";
import { useNavigate } from "react-router-dom";

const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <CssBaseline />

      {/* Sidebar */}
      <Drawer open={sidebarOpen} onClose={() => setSidebarOpen(false)}>
        <List>
          {tools.map((tool) => (
            <ListItem
              sx={{ cursor: "pointer" }}
              button
              key={tool.id}
              onClick={() => navigate(tool.path)}
            >
              <ListItemText primary={tool.name} />
            </ListItem>
          ))}
        </List>
      </Drawer>

      {/* Main Content */}
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
          sx={{ width: "100%", zIndex: 1100, backgroundColor: "#269a62" }}
        >
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={() => setSidebarOpen(true)}
            >
              <MenuIcon />
            </IconButton>
            <Typography variant="h6">Dev Tools</Typography>
          </Toolbar>
        </AppBar>

        {/* Page Content */}
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            mt: "64px", // Để tránh bị che bởi AppBar
            p: 2,
            width: "100%",
          }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  );
};

export default MainLayout;
