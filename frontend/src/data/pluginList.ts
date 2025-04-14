import { useEffect, useState } from "react";
// import { fallbackMetadata } from "./fallbackMetadata"; // Import the fallback data

// --- Interfaces (with proper exports) ---
export interface PluginMetadata {
  triggerUpdateOnChange: any;
  id: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  customUI?: boolean;
  sections: Section[]; // Array of sections
  version?: string;
  // Add frontend-specific fields for processing
  processFunction?: (input: any) => Promise<any>;
  
  // Additional frontend properties for fallback data
  uiConfig?: any;
}

export interface Section {
  id: string;
  label: string;
  inputs?: InputField[];
  outputs?: OutputField[];
  condition?: string;
}

export interface InputField {
  id: string;
  label: string;
  type: string;
  containerId?: string;
  width?: number;
  height?: number;
  buttons?: ("minus" | "plus")[];
  default?: any;
  min?: number;
  max?: number;
  step?: number;
  required?: boolean;
  placeholder?: string;
  helperText?: string;
  multiline?: boolean;
  rows?: number;
  condition?: string;
  options?: any;
  accept?: string;
  action?: any;
}

type TableRow = {
  id: string;
  description: string;
  value: string;
};

export interface OutputField {
  id: string;
  label: string;
  type: string;
  width?: number;
  height?: number;
  buttons?: ("copy" | "refresh" | "download")[];
  buttonPlacement?: Record<string, "inside" | "outside">;
  containerId?: string;
  condition?: string;
  monospace?: boolean;
  multiline?: boolean;
  rows?: TableRow[];
  default?: any;
  name?: string;
  fontSize?: number;
  style?: string;
  downloadFilenameKey?: string;
  maxWidth?: number;
  maxHeight?: number;
  min?: number;
  max?: number;
  suffix?: string;
  columns?: boolean;
}

// --- PluginListResponse Interface ---
export interface PluginListResponse {
  loadedPlugins: string[];
}

/**
 * Helper function to add a delay between requests
 */
const delay = (ms: number): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * Custom hook to fetch metadata for all available universal plugins.
 * Uses sequential fetching to avoid overwhelming the backend.
 */
export const useAllPluginMetadata = () => {
  // State for the list of successfully fetched metadata
  const [metadataList, setMetadataList] = useState<PluginMetadata[]>([]);
  // State to track if the fetching process is ongoing
  const [loading, setLoading] = useState<boolean>(true);
  // State to store any error that occurs during the fetching process
  const [error, setError] = useState<string | null>(null);
  // Track if we're using backend or fallback data
  const [dataSource, setDataSource] = useState<'backend' | 'fallback' | null>(null);

  useEffect(() => {
    const fetchAllMetadata = async () => {
      setLoading(true);
      setError(null);
      console.log("Starting to fetch all plugin metadata...");

      try {
        // 1. Fetch the list of plugin names using relative URL for Vite proxy
        const pluginListUrl = `/api/plugins/universal/manual-load`;
        console.log("Fetching plugin list from:", pluginListUrl);
        const pluginListRes = await fetch(pluginListUrl);

        if (!pluginListRes.ok) {
          throw new Error(
            `Failed to load plugin list: ${pluginListRes.status} ${pluginListRes.statusText}`
          );
        }

        // Parse the plugin list response
        const pluginListData: PluginListResponse = await pluginListRes.json();
        const { loadedPlugins } = pluginListData;

        // Validate the received list
        if (!Array.isArray(loadedPlugins)) {
          throw new Error("Invalid plugin list format received from server.");
        }
        console.log(
          `Found ${loadedPlugins.length} plugins reported by server:`,
          loadedPlugins
        );

        if (loadedPlugins.length === 0) {
          console.log("No plugins listed by the server. Using fallback data.");
          setMetadataList(fallbackMetadata); // Use fallback data
          setDataSource('fallback');
          setLoading(false);
          return;
        }

        // 2. Fetch metadata SEQUENTIALLY with delay between requests
        console.log(`Fetching metadata for ${loadedPlugins.length} plugins sequentially...`);
        const validMetadata: PluginMetadata[] = [];
        
        for (const pluginName of loadedPlugins) {
          try {
            // Add a small delay between requests
            if (validMetadata.length > 0) {
              await delay(300); // 300ms delay between requests
            }
            
            // Fetch the plugin metadata
            const url = `/api/plugins/universal/${pluginName}/metadata`;
            const res = await fetch(url);
            
            if (!res.ok) {
              throw new Error(`${res.status} ${res.statusText}`);
            }
            
            // Parse metadata 
            const metadata: PluginMetadata = await res.json();
            
            // Validate
            if (
              !metadata ||
              typeof metadata.id !== "string" ||
              typeof metadata.name !== "string" ||
              !Array.isArray(metadata.sections)
            ) {
              throw new Error(`Invalid metadata structure received for ${pluginName}`);
            }

            // Add a process function
            metadata.processFunction = async (input: any) => {
              try {
                const response = await fetch(`/api/plugins/universal/${metadata.id}/process`, {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify(input),
                });
                
                if (!response.ok) {
                  throw new Error(`API error: ${response.status}`);
                }
                
                return await response.json();
              } catch (error) {
                console.error(`Plugin '${metadata.id}' execution failed:`, error);
                return {
                  success: false,
                  error: "Backend not available or request failed."
                };
              }
            };

            console.log(`Fetched metadata for plugin: ${pluginName}`);
            validMetadata.push(metadata);
          } catch (err) {
            // Log the error but continue with next plugin
            console.error(`[FAIL] Fetching metadata for ${pluginName}:`, err);
          }
        }

        console.log(
          `Successfully loaded metadata for ${validMetadata.length} out of ${loadedPlugins.length} plugins.`
        );
        
        if (validMetadata.length > 0) {
          setMetadataList(validMetadata);
          setDataSource('backend');
        } else {
          console.log("No plugins fetched successfully, using fallback data");
          setMetadataList(fallbackMetadata);
          setDataSource('fallback');
        }
      } catch (err: any) {
        // Catch errors from fetching the initial list
        console.error("FATAL: Failed to fetch plugin metadata:", err);
        setError(
          err.message || "An unknown error occurred while fetching plugin data"
        );
        // Use fallback data as fallback
        setMetadataList(fallbackMetadata);
        setDataSource('fallback');
      } finally {
        setLoading(false);
        console.log("Finished metadata fetching process.");
      }
    };

    fetchAllMetadata();
  }, []);

  // Return the state variables for the consuming component
  return { metadataList, loading, error, dataSource };
};

/**
 * Helper to convert backend plugin metadata format to frontend format if needed
 * This is used when we need to adapt backend data to match our frontend expectations
 */
export const convertBackendMetadataToFrontendFormat = (backendMetadata: PluginMetadata): PluginMetadata => {
  // If the metadata already has uiConfig, it's probably already in frontend format
  if (backendMetadata.uiConfig) {
    return backendMetadata;
  }

  // Otherwise, convert from backend format (sections) to frontend format (uiConfig)
  return {
    ...backendMetadata,
    uiConfig: {
      sections: backendMetadata.sections?.map(section => ({
        header: section.label,
        fields: section.inputs?.map(input => ({
          name: input.id,
          label: input.label,
          type: mapInputType(input.type),
          default: input.default,
          options: input.options ? 
            Array.isArray(input.options) ? 
              input.options.map((opt: any) => typeof opt === 'string' ? opt : (opt.label || opt.value)) : 
              input.options.map((opt: any) => opt.label || opt.value) : 
            undefined,
          min: input.min,
          max: input.max,
          required: input.required,
          placeholder: input.placeholder,
          helperText: input.helperText,
        })) || [],
      })) || [],
      outputs: backendMetadata.sections?.flatMap(section => 
        section.outputs?.map(output => ({
          title: output.label || output.id,
          name: output.id,
          type: output.type,
          buttons: output.buttons || [],
        })) || []
      ) || []
    }
  };
};

/**
 * Helper function to map backend input types to frontend types
 */
const mapInputType = (backendType: string): string => {
  const typeMap: Record<string, string> = {
    'text': 'text',
    'number': 'number',
    'select': 'select',
    'file': 'file',
    'password': 'password',
    'checkbox': 'switch',
    'color': 'color',
    'button': 'button',
    // Add more mappings as needed
  };
  
  return typeMap[backendType] || backendType;
};