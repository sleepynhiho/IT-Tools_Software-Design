import React, { useRef } from 'react';
import { Box } from '@mui/material';
import WebCamCapture, { WebCamCaptureMethods } from './WebCamCapture';
import { PluginMetadata } from '../../data/pluginList';

// Define props interface
interface WebCamCapturePluginProps {
  plugin: PluginMetadata;
  inputValues: Record<string, any>;
  setInputValues: (values: Record<string, any>) => void;
  onSubmit: () => void;
  outputValues: Record<string, any>;
  loading: boolean;
}

/**
 * WebCamCapturePlugin - Custom UI component for the WebCamCapture plugin
 * 
 * This component handles the "action" property from the tool metadata and maps
 * it to the appropriate method in the WebCamCapture component.
 */
const WebCamCapturePlugin: React.FC<WebCamCapturePluginProps> = ({ 
  plugin,
  inputValues, 
  setInputValues, 
  onSubmit, 
  outputValues, 
  loading
}) => {
  // Reference to the WebCamCapture component for method access
  const webcamRef = useRef<WebCamCaptureMethods>(null);
  
  // Function to handle action buttons configured in metadata
  const handleButtonAction = (action: string) => {
    console.log(`WebCam action triggered: ${action}`);
    
    switch (action) {
      case 'capturePhoto':
        if (webcamRef.current) {
          webcamRef.current.capturePhoto();
        }
        break;
      
      case 'startRecording':
        if (webcamRef.current) {
          webcamRef.current.startRecording();
        }
        break;
      
      case 'stopRecording':
        if (webcamRef.current) {
          webcamRef.current.stopRecording();
        }
        break;
        
      default:
        console.warn(`Unknown webcam action: ${action}`);
    }
  };
  
  // Find all button inputs to map their actions
  React.useEffect(() => {
    if (plugin && plugin.sections) {
      // Process each section that might contain action buttons
      plugin.sections.forEach(section => {
        if (section.inputs) {
          // Find button inputs with actions
          section.inputs.forEach(input => {
            if (input.type === 'button' && input.action) {
              // Override the default button behavior to use our custom actions
              const buttonElement = document.getElementById(`button-${input.id}`);
              if (buttonElement) {
                buttonElement.addEventListener('click', (e) => {
                  e.preventDefault();
                  handleButtonAction(input.action!);
                });
              }
            }
          });
        }
      });
    }
    
    // Cleanup event listeners on unmount
    return () => {
      if (plugin && plugin.sections) {
        plugin.sections.forEach(section => {
          if (section.inputs) {
            section.inputs.forEach(input => {
              if (input.type === 'button' && input.action) {
                const buttonElement = document.getElementById(`button-${input.id}`);
                if (buttonElement) {
                  buttonElement.removeEventListener('click', () => {});
                }
              }
            });
          }
        });
      }
    };
  }, [plugin]);
  
  return (
    <Box>
      <WebCamCapture
        ref={webcamRef}
        inputValues={inputValues}
        setInputValues={setInputValues}
        onSubmit={onSubmit}
        outputValues={outputValues}
        loading={loading}
        options={{
          imageFieldName: 'capturedImageData',
          videoFieldName: 'capturedVideoData',
          videoMimeTypeFieldName: 'capturedVideoMimeType',
          fileNameFieldName: 'outputFileName',
          aspectRatio: '16/9',
          videoQuality: 'high'
        }}
      />
    </Box>
  );
};

export default WebCamCapturePlugin;