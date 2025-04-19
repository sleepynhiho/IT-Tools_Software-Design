import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Paper, 
  Typography, 
  CircularProgress,
  Alert
} from '@mui/material';
import CustomPluginUIs from './CustomPluginUIs';
import pluginService from '../../services/pluginService';

/**
 * Default UI component for plugins that don't have a custom UI
 */
const DefaultPluginUI = ({ 
  plugin, 
  inputValues, 
  setInputValues, 
  onSubmit, 
  outputValues, 
  loading 
}) => {
  // This would be your existing generic plugin UI implementation
  // It should render input fields based on plugin.sections
  return (
    <Box>
      <Typography>
        This plugin requires custom UI implementation.
      </Typography>
    </Box>
  );
};

/**
 * Plugin component that renders either a custom UI or default UI based on plugin metadata
 */
const Plugin = ({ plugin }) => {
  const [inputValues, setInputValues] = useState({});
  const [outputValues, setOutputValues] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Get the Custom UI component if specified in metadata
  const CustomUI = plugin?.customUI ? CustomPluginUIs[plugin.id] : null;
  
  // Initialize default values from plugin metadata
  useEffect(() => {
    if (!plugin || !plugin.sections) return;
    
    const defaults = {};
    plugin.sections.forEach(section => {
      if (!section.inputs) return;
      
      section.inputs.forEach(input => {
        if (input.default !== undefined) {
          defaults[input.id] = input.default;
        } else if (input.type === 'text' || input.type === 'hidden') {
          defaults[input.id] = '';
        }
      });
    });
    
    setInputValues(defaults);
  }, [plugin]);
  
  // Handle submission of plugin data
  const handleSubmit = async () => {
    if (!plugin) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const result = await pluginService.processPlugin(plugin.id, inputValues);
      setOutputValues(result);
    } catch (error) {
      console.error('Error processing plugin:', error);
      setError(error.message || 'An unexpected error occurred');
      setOutputValues({
        success: false,
        errorMessage: error.message || 'An unexpected error occurred'
      });
    } finally {
      setLoading(false);
    }
  };
  
  if (!plugin) {
    return <CircularProgress />;
  }
  
  return (
    <Paper 
      elevation={2} 
      sx={{ 
        p: 3, 
        mb: 3,
        borderRadius: 2,
        overflow: 'hidden'
      }}
    >
      <Typography variant="h5" component="h2" gutterBottom>
        {plugin.name}
      </Typography>
      
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {plugin.description}
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      {/* Render either custom UI or default UI */}
      <Box sx={{ mt: 2 }}>
        {CustomUI ? (
          <CustomUI
            plugin={plugin}
            inputValues={inputValues}
            setInputValues={setInputValues}
            onSubmit={handleSubmit}
            outputValues={outputValues}
            loading={loading}
          />
        ) : (
          <DefaultPluginUI
            plugin={plugin}
            inputValues={inputValues}
            setInputValues={setInputValues}
            onSubmit={handleSubmit}
            outputValues={outputValues}
            loading={loading}
          />
        )}
      </Box>
    </Paper>
  );
};

export default Plugin;