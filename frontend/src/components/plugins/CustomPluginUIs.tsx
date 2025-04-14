import React from 'react';
import WebCamCapturePlugin from './WebCamCapturePlugin';
import { PluginMetadata } from '../data/pluginList';

// Define props interface for custom UI components
export interface CustomPluginUIProps {
  plugin: PluginMetadata;
  inputValues: Record<string, any>;
  setInputValues: (values: Record<string, any>) => void;
  onSubmit: () => void;
  outputValues: Record<string, any>;
  loading: boolean;
}

// Type definition for the registry
export interface CustomPluginUIRegistry {
  [pluginId: string]: React.ComponentType<CustomPluginUIProps>;
}

/**
 * Registry of custom UI components for plugins with customUI=true
 * Maps plugin IDs to their custom UI components
 */
const CustomPluginUIs: CustomPluginUIRegistry = {
  WebCamCapture: WebCamCapturePlugin
  // Add more custom UIs here as needed
};

export default CustomPluginUIs;