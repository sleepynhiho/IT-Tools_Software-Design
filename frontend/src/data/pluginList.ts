import { useEffect, useRef, useState } from "react";
import { fallbackMetadata } from "./fallbackMetadata"; // Import the fallback data

// Configuration constants
const USE_FALLBACK_METADATA = false; // Set to true to always use fallback data
const BATCH_SIZE = 4; // Number of plugins to load in parallel
const BATCH_DELAY = 200; // Delay between batches in milliseconds
const MAX_RETRIES = 2; // Maximum number of retries for failed requests
const RETRY_DELAY = 50; // Delay between retries in milliseconds

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
 * Helper function to fetch a single plugin's metadata with retry
 */
const fetchPluginMetadata = async (
  pluginName: string,
  maxRetries: number = MAX_RETRIES,
  retryDelay: number = RETRY_DELAY
): Promise<PluginMetadata | null> => {
  let retries = 0;
  
  while (retries <= maxRetries) {
    try {
      const url = `/api/plugins/universal/${pluginName}/metadata`;
      const res = await fetch(url);
      
      if (!res.ok) {
        throw new Error(`${res.status} ${res.statusText}`);
      }
      
      const metadata: PluginMetadata = await res.json();
      
      // Basic validation
      if (
        !metadata ||
        typeof metadata.id !== "string" ||
        typeof metadata.name !== "string" ||
        !Array.isArray(metadata.sections)
      ) {
        throw new Error(`Invalid metadata structure received`);
      }

      // Add a process function to call the backend
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
      return metadata;
    } catch (err) {
      retries++;
      if (retries <= maxRetries) {
        console.log(`Retrying ${pluginName} (${retries}/${maxRetries})...`);
        await delay(retryDelay);
      } else {
        console.error(`[FAIL] Fetching metadata for ${pluginName}:`, err);
        return null;
      }
    }
  }
  
  return null;
};

/**
 * Process plugins in batches with parallelism but controlled load
 */
async function processPluginsInBatches(
  pluginNames: string[], 
  batchSize: number = BATCH_SIZE,
  delayBetweenBatches: number = BATCH_DELAY,
  progressCallback?: (progress: number) => void
): Promise<PluginMetadata[]> {
  const validMetadata: PluginMetadata[] = [];
  const batches = [];
  
  // Create batches of plugins
  for (let i = 0; i < pluginNames.length; i += batchSize) {
    batches.push(pluginNames.slice(i, i + batchSize));
  }
  
  console.log(`Processing ${pluginNames.length} plugins in ${batches.length} batches of ${batchSize}`);
  
  // Process each batch
  for (let i = 0; i < batches.length; i++) {
    const batch = batches[i];
    const batchIndex = i + 1;
    
    // If not the first batch, add delay
    if (i > 0 && delayBetweenBatches > 0) {
      await delay(delayBetweenBatches);
    }
    
    console.log(`Processing batch ${batchIndex}/${batches.length} with ${batch.length} plugins`);
    
    // Process all plugins in the current batch in parallel
    const batchResults = await Promise.allSettled(
      batch.map(pluginName => fetchPluginMetadata(pluginName))
    );
    
    // Collect successful results
    batchResults.forEach((result, index) => {
      if (result.status === 'fulfilled' && result.value) {
        validMetadata.push(result.value);
      } else {
        console.error(`Failed to fetch ${batch[index]} in batch ${batchIndex}`);
      }
    });
    
    console.log(`Batch ${batchIndex} complete: ${validMetadata.length} plugins loaded so far`);
    
    // Update progress if a callback is provided
    if (progressCallback) {
      const progress = Math.min(10 + Math.floor(((i + 1) / batches.length) * 90), 99);
      progressCallback(progress);
    }
  }
  
  return validMetadata;
}

/**
 * Custom hook to fetch metadata for all available universal plugins.
 * Uses batch processing for better performance while maintaining reliability.
 */
export const useAllPluginMetadata = () => {
  const [metadataList, setMetadataList] = useState<PluginMetadata[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingProgress, setLoadingProgress] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [dataSource, setDataSource] = useState<'backend' | 'fallback' | null>(null);
  
  // Use a ref to track if the fetch has already been initiated
  const fetchInitiatedRef = useRef<boolean>(false);

  useEffect(() => {
    // Use fallback data directly if configured to do so
    if (USE_FALLBACK_METADATA) {
      console.log("Using fallback metadata (configured)");
      setMetadataList(fallbackMetadata);
      setDataSource('fallback');
      setLoading(false);
      setLoadingProgress(100);
      return;
    }
    
    // Prevent multiple fetches in development mode with React.StrictMode
    if (fetchInitiatedRef.current) {
      console.log('Fetch already initiated, skipping duplicate fetch');
      return;
    }
    
    fetchInitiatedRef.current = true;
    
    const fetchAllMetadata = async () => {
      setLoading(true);
      setLoadingProgress(0);
      setError(null);
      console.log("Starting to fetch all plugin metadata...");

      try {
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
        
        if (!Array.isArray(loadedPlugins)) {
          throw new Error("Invalid plugin list format received from server.");
        }
        console.log(`Found ${loadedPlugins.length} plugins reported by server:`, loadedPlugins);
        
        setLoadingProgress(10); // 10% progress - plugin list loaded

        if (loadedPlugins.length === 0) {
          console.log("No plugins listed by the server. Using fallback data.");
          setMetadataList(fallbackMetadata);
          setDataSource('fallback');
          setLoading(false);
          setLoadingProgress(100);
          return;
        }
        
        // Process plugins in batches - good balance between speed and reliability
        const validMetadata = await processPluginsInBatches(
          loadedPlugins, 
          BATCH_SIZE, 
          BATCH_DELAY, 
          setLoadingProgress
        );
        
        console.log(`Batch processing complete: ${validMetadata.length} out of ${loadedPlugins.length} plugins loaded`);
        
        if (validMetadata.length > 0) {
          setMetadataList(validMetadata);
          setDataSource('backend');
        } else {
          console.log("No plugins fetched successfully, using fallback data");
          setMetadataList(fallbackMetadata);
          setDataSource('fallback');
        }
        
        setLoadingProgress(100); // 100% progress - all done
      } catch (err: any) {
        console.error("FATAL: Failed to fetch plugin metadata:", err);
        setError(err.message || "An unknown error occurred while fetching plugin data");
        setMetadataList(fallbackMetadata);
        setDataSource('fallback');
        setLoadingProgress(100); // Error, but still complete
      } finally {
        setLoading(false);
        console.log("Finished metadata fetching process.");
      }
    };

    fetchAllMetadata();
  }, []);

  return { metadataList, loading, loadingProgress, error, dataSource };
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