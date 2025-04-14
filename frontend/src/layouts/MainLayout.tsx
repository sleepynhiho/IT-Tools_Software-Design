import { useState, useEffect } from "react";
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
} from "@mui/material";
import { HomeOutlined, Menu as MenuIcon } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { SearchIcon } from "lucide-react";
import * as MuiIcons from "@mui/icons-material";
import { useFavoriteTools } from "../context/FavoriteToolsContext";
import { fetchPluginMetadata } from "../data/pluginMetadata";
import { fallbackMetadata } from "../data/fallbackMetadata";

const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const [tools, setTools] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const [openCategories, setOpenCategories] = useState<Record<string, boolean>>(
    {}
  );

  const { favoriteTools } = useFavoriteTools();

  // Fetch plugin metadata from backend when component mounts
  useEffect(() => {
    const loadPlugins = async () => {
      setLoading(true);
      try {
        const metadata = await fetchPluginMetadata();
        
        if (metadata && metadata.length > 0) {
          // Convert backend metadata format to your application format
          const formattedPlugins = metadata.map(plugin => ({
            id: plugin.id,
            name: plugin.name,
            icon: plugin.icon || 'Extension', // Default icon
            category: plugin.category || 'Other',
            description: plugin.description || '',
            // Keep the rest of your plugin structure as needed
            uiConfig: {
              // Map backend sections to your UI config format based on your needs
              inputs: plugin.sections
                ?.filter((section: { inputs: string | any[]; }) => section.inputs && section.inputs.length > 0)
                .map((section: { label: any; inputs: any[]; }) => ({
                  header: section.label,
                  fields: section.inputs.map((input: { id: any; label: any; type: string; default: any; options: any[]; }) => ({
                    name: input.id,
                    label: input.label,
                    type: mapInputType(input.type),
                    default: input.default,
                    options: input.options ? 
                      Array.isArray(input.options) ? 
                        input.options : 
                        input.options.map((opt: { label: any; value: any; }) => opt.label || opt.value) : 
                      undefined,
                  }))
                })) || [],
              outputs: plugin.sections
                ?.flatMap((section: { outputs: any; }) => section.outputs || [])
                .map((output: { label: any; id: any; type: any; buttons: any; }) => ({
                  title: output.label || output.id,
                  name: output.id,
                  type: output.type,
                  buttons: output.buttons || [],
                })) || []
            },
            // Add a process function
            processFunction: async (input: any) => {
              try {
                const response = await fetch(
                  `/api/plugins/universal/${plugin.id}/process`,
                  {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(input),
                  }
                );
                
                if (!response.ok) {
                  throw new Error(`API error: ${response.status}`);
                }
                
                return await response.json();
              } catch (error) {
                console.error(`Plugin '${plugin.id}' execution failed:`, error);
                return {
                  success: false,
                  error: "Backend not available or request failed."
                };
              }
            }
          }));
          
          setTools(formattedPlugins);
        } else {
          // Fallback to mock data if no plugins returned
          console.warn("No plugins returned from backend, using fallback data");
          setTools(fallbackMetadata);
        }
      } catch (err) {
        console.error("Error loading plugins:", err);
        // Fallback to mock data on error
        setTools(fallbackMetadata);
      } finally {
        setLoading(false);
      }
    };

    loadPlugins();
  }, []);

  // Handle category toggle
  const handleCategoryToggle = (category: string) => {
    setOpenCategories((prevState) => ({
      ...prevState,
      [category]: !prevState[category],
    }));
  };

  // Group tools by category
  const groupedTools = tools.reduce<Record<string, (typeof tools)[0][]>>(
    (acc, tool) => {
      if (!acc[tool.category]) acc[tool.category] = [];
      acc[tool.category].push(tool);
      return acc;
    },
    {}
  );

  // Helper function to map backend input types to frontend types
  const mapInputType = (backendType: string) => {
    const typeMap: Record<string, string> = {
      'text': 'text',
      'number': 'number',
      'select': 'select',
      'file': 'file',
      'password': 'password',
      'checkbox': 'switch',
      // Add more mappings as needed
    };
    
    return typeMap[backendType] || backendType;
  };

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <CssBaseline />

      {/* Sidebar */}
      <Drawer open={sidebarOpen} onClose={() => setSidebarOpen(false)}>
        <Box
          sx={{
            p: 2,
            textAlign: "center",
            cursor: "pointer",
            "&:hover": {
              transition: "transform 0.2s",
            },
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
            IT - TOOLS
          </Typography>
          <Typography variant="subtitle1" sx={{ color: "#fff" }}>
            Handy tools for developers
          </Typography>
        </Box>

        {/* Loading indicator when fetching tools */}
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          /* Tools list */
          <List>
            <div key="favorites">
              <ListItem
                onClick={() => handleCategoryToggle("favorites")}
                sx={{
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  flexDirection: "row",
                  padding: "3px 10px 0px 10px",
                  "&:hover": {
                    color: "#ffffff",
                  },
                  color: "#ffffff85",
                }}
              >
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    transition: "transform 0.3s",
                    transform: openCategories["favorites"]
                      ? "rotate(0deg)"
                      : "rotate(-90deg)",
                    marginRight: 1,
                  }}
                >
                  <MuiIcons.ExpandMoreOutlined
                    sx={{
                      fontSize: "20px",
                      color: "inherit",
                    }}
                  />
                </Box>

                <ListItemText
                  primary="Favorites"
                  sx={{
                    color: "#ffffff85",
                    "& .MuiTypography-root": {
                      fontSize: "14px",
                    },
                    "&:hover .MuiTypography-root": {
                      color: "#ffffff",
                    },
                  }}
                />
              </ListItem>
              <Collapse
                in={openCategories["favorites"]}
                timeout="auto"
                unmountOnExit
              >
                <List
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    marginLeft: "10px",
                    padding: "2px 5px",
                  }}
                >
                  {/* Vertical line */}
                  <Box
                    sx={{
                      position: "absolute",
                      left: 0,
                      top: 0,
                      bottom: 0,
                      width: "2px",
                      backgroundColor: "#2e2e2e",
                      zIndex: -1,
                      marginLeft: "8px",
                      marginTop: "5px",
                      marginBottom: "10px",
                    }}
                  />
                  {favoriteTools.map((tool) => {
                    const ToolIcon =
                      MuiIcons[tool.icon as keyof typeof MuiIcons] ||
                      MuiIcons.HelpOutline;

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
                        }}
                        onClick={() => navigate(`/tools/${tool.id}`)}
                      >
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            marginRight: 1,
                          }}
                        >
                          <ToolIcon sx={{ fontSize: "20px" }} />
                        </Box>
                        <ListItemText
                          primary={tool.name}
                          sx={{
                            "& .MuiTypography-root": {
                              fontSize: "14px",
                            },
                          }}
                        />
                      </ListItem>
                    );
                  })}
                </List>
              </Collapse>
            </div>
            {Object.keys(groupedTools).map((category) => {
              return (
                <div key={category}>
                  {/* Category header */}
                  <ListItem
                    onClick={() => handleCategoryToggle(category)}
                    sx={{
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      flexDirection: "row",
                      padding: "3px 10px 0px 10px",
                      "&:hover": {
                        color: "#ffffff",
                      },
                      color: "#ffffff85",
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        transition: "transform 0.3s",
                        transform: openCategories[category]
                          ? "rotate(0deg)"
                          : "rotate(-90deg)",
                        marginRight: 1,
                      }}
                    >
                      <MuiIcons.ExpandMoreOutlined
                        sx={{
                          fontSize: "20px",
                          color: "inherit",
                        }}
                      />
                    </Box>

                    <ListItemText
                      primary={category}
                      sx={{
                        color: "#ffffff85",
                        "& .MuiTypography-root": {
                          fontSize: "14px",
                        },
                        "&:hover .MuiTypography-root": {
                          color: "#ffffff",
                        },
                      }}
                    />
                  </ListItem>
                  <Collapse
                    in={openCategories[category]}
                    timeout="auto"
                    unmountOnExit
                  >
                    <List
                      sx={{
                        display: "flex",
                        flexDirection: "column",
                        marginLeft: "10px",
                        padding: "2px 5px",
                      }}
                    >
                      {/* Vertical line */}
                      <Box
                        sx={{
                          position: "absolute",
                          left: 0,
                          top: 0,
                          bottom: 0,
                          width: "2px",
                          backgroundColor: "#2e2e2e",
                          zIndex: -1,
                          marginLeft: "8px",
                          marginTop: "5px",
                          marginBottom: "10px",
                        }}
                      />

                      {groupedTools[category].map((tool) => {
                        const ToolIcon =
                          MuiIcons[tool.icon as keyof typeof MuiIcons] ||
                          MuiIcons.HelpOutline;

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
                            }}
                            onClick={() => navigate(`/tools/${tool.id}`)}
                          >
                            <Box
                              sx={{
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                marginRight: 1,
                              }}
                            >
                              <ToolIcon sx={{ fontSize: "20px" }} />
                            </Box>
                            <ListItemText
                              primary={tool.name}
                              sx={{
                                "& .MuiTypography-root": {
                                  fontSize: "14px",
                                },
                              }}
                            />
                          </ListItem>
                        );
                      })}
                    </List>
                  </Collapse>
                </div>
              );
            })}
          </List>
        )}
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
          elevation={0}
          sx={{
            width: "100%",
            zIndex: 1100,
            backgroundColor: "#1c1c1c",
            display: "flex",
            justifyContent: "space-between",
            flexDirection: "row",
            alignItems: "center",
            padding: "5px 16px",
          }}
        >
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={() => setSidebarOpen(true)}
            >
              <MenuIcon />
            </IconButton>
            <IconButton edge="start" color="inherit" sx={{ ml: 1 }}>
              <HomeOutlined />
            </IconButton>
          </Toolbar>

          {/* Search Bar */}
          <TextField
            variant="outlined"
            placeholder="Search..."
            size="small"
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

          {/* Log In & Sign Up Buttons */}
          <Box
            sx={{
              display: "flex",
              gap: 1,
              marginLeft: 2,
              marginRight: 2,
              alignItems: "center",
            }}
          >
            <Box sx={{ display: "flex", gap: 1, marginLeft: 2 }}>
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
                }}
              >
                Log In
              </Button>
            </Box>

            <Button
              variant="text"
              color="inherit"
              onClick={() => navigate("/signup")}
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
              }}
            >
              Sign Up
            </Button>
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
            mt: "64px",
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