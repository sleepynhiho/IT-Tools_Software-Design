import { useEffect, useState } from "react";

// --- Interfaces (ADD EXPORT HERE) ---
export interface PluginMetadata {
  // <--- ADD EXPORT
  id: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  customUI?: boolean;
  sections: Section[]; // Array of sections
  version?: string;
}

export interface Section {
  // <--- ADD EXPORT
  id: string;
  label: string;
  inputs?: InputField[];
  outputs?: OutputField[];
  condition?: string;
}

export interface InputField {
  // <--- ADD EXPORT
  id: string;
  label: string;
  type: string;
  containerId: string;
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
}

type TableRow = {
  id: string;
  description: string;
  value: string;
};




export interface OutputField {
  // <--- ADD EXPORT
  id: string;
  label: string;
  type: string;
  width?: number;
  height?: number;
  buttons?: ("copy" | "refresh" | "download")[];
  buttonPlacement?: Record<string, "inside" | "outside">;
  containerId: string;
  condition?: string;
  monospace?: boolean;
  multiline?: boolean;
  rows?: TableRow[];
  default?: any; // Keep default
  name?: string; // Keep name for download
  fontSize?: number; // Keep fontSize for download
}
// --- End Interfaces ---

// --- PluginListResponse Interface --- (Keep as is or export if needed elsewhere)
interface PluginListResponse {
  loadedPlugins: string[];
}

// --- API_BASE_URL --- (Keep as is)
const API_BASE_URL = "http://192.168.192.2:8081";

/**
 * Custom hook to fetch metadata for all available universal plugins.
 * Fetches a list of plugin names first, then fetches metadata for each.
 * Returns the list of valid metadata objects, loading state, and any overall error.
 */
export const useAllPluginMetadata = () => {
  // State for the list of successfully fetched metadata
  const [metadataList, setMetadataList] = useState<PluginMetadata[]>([]);
  // State to track if the fetching process is ongoing
  const [loading, setLoading] = useState<boolean>(false);
  // State to store any error that occurs during the fetching process
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchAllMetadata = async () => {
      setLoading(true);
      setError(null);
      console.log("Starting to fetch all plugin metadata...");

      try {
        // 1. Fetch the list of plugin names
        const pluginListUrl = `${API_BASE_URL}/api/plugins/universal/manual-load`;
        console.log("Fetching plugin list from:", pluginListUrl);
        const pluginListRes = await fetch(pluginListUrl);

        if (!pluginListRes.ok) {
          // Throw error if the list itself cannot be fetched
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
          console.log("No plugins listed by the server.");
          setMetadataList([]); // Set to empty list
          setLoading(false);
          return; // Exit early if no plugins to fetch
        }

        // 2. Create promises to fetch metadata for each plugin
        const metadataPromises = loadedPlugins.map(
          async (pluginName: string): Promise<PluginMetadata> => {
            // Construct the URL for fetching individual plugin metadata
            const url = `${API_BASE_URL}/api/plugins/universal/${pluginName}/metadata`;
            // Log inside map can be very noisy if many plugins, keep outside for batch start/end
            const res = await fetch(url);
            if (!res.ok) {
              // Throw an error specific to this plugin fetch, will be caught by Promise.allSettled
              throw new Error(`${res.status} ${res.statusText}`); // Keep error concise for logging below
            }
            // Assume the response is valid PluginMetadata JSON
            const metadata = await res.json();
            // Optional but recommended: Basic validation of the received metadata structure
            if (
              !metadata ||
              typeof metadata.id !== "string" ||
              typeof metadata.name !== "string" ||
              !Array.isArray(metadata.sections)
            ) {
              throw new Error(`Invalid metadata structure received`);
            }

            console.log(`Fetched metadata for plugin: ${pluginName}`, metadata); // Log the fetched metadata
            return metadata as PluginMetadata; // Cast to expected type
          }
        );

        // 3. Wait for all metadata fetch promises to settle
        console.log(
          `Fetching metadata for ${loadedPlugins.length} plugins in parallel...`
        );
        const metadataResults = await Promise.allSettled(metadataPromises);
        console.log("All metadata fetches settled.");

        // 4. Process the results, filtering out failures
        const validMetadata: PluginMetadata[] = [];
        metadataResults.forEach((result, index) => {
          const pluginName = loadedPlugins[index]; // Get corresponding plugin name for logging
          if (result.status === "fulfilled") {
            // Successfully fetched and validated metadata
            validMetadata.push(result.value);
            // console.log(`[OK] Fetched metadata for ${pluginName}`);
          } else {
            // Fetch failed for this specific plugin
            console.error(
              `[FAIL] Fetching metadata for ${pluginName}:`,
              result.reason?.message || result.reason
            );
            // Do not add to validMetadata list
            // Optionally: Collect these errors separately if needed
          }
        });

        console.log(
          `Successfully loaded metadata for ${validMetadata.length} out of ${loadedPlugins.length} plugins.`
        );
        setMetadataList(validMetadata); // Update state with only the successfully fetched metadata
      } catch (err: any) {
        // Catch errors from fetching the initial list or other major issues during setup
        console.error("FATAL: Failed to fetch plugin metadata:", err);
        setError(
          err.message || "An unknown error occurred while fetching plugin data"
        );
        setMetadataList([]); // Clear any potentially partial list on major error
      } finally {
        setLoading(false); // Ensure loading is always set to false
        console.log("Finished metadata fetching process.");
      }
    };

    fetchAllMetadata();
  }, []); // Empty dependency array ensures this runs only once when the hook mounts

  // Return the state variables for the consuming component
  return { metadataList, loading, error };
};
