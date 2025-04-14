/**
 * Calls backend API to process plugin operations.
 *
 * @param toolId   Plugin name (e.g., "HashTools")
 * @param operation Operation name (e.g., "sha256", "md5")
 * @param inputData  Input data (text, image, etc. depending on the plugin)
 */
const fetchFromBackend = async (
  toolId: string,
  operation: string = '',
  inputData: Record<string, any> = {}
): Promise<any> => {
  try {
    const response = await fetch(
      `/api/debug/${toolId}/process`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          operation,
          ...inputData,
        }),
      }
    );

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("Fetch error:", error);
    return {
      success: false,
      error: "Backend not available or request failed.",
    };
  }
};

// Helper function to convert UI config to sections format
const createSectionsFromUiConfig = (item: any): any[] => {
  const sections: any[] = [];
  
  // Process input sections
  if (item.uiConfig && item.uiConfig.inputs) {
    item.uiConfig.inputs.forEach((inputSection: any) => {
      sections.push({
        id: inputSection.header || 'default',
        label: inputSection.header || 'Settings',
        inputs: inputSection.fields?.map((field: any) => ({
          id: field.name,
          label: field.label,
          type: field.type,
          default: field.default,
          min: field.min,
          max: field.max,
          required: field.required,
          options: field.options,
          containerId: 'main'
        })) || [],
        outputs: inputSection.outputs?.map((output: any) => ({
          id: output.name,
          label: output.title,
          type: output.type,
          buttons: output.buttons,
          containerId: 'main'
        })) || []
      });
    });
  }
  
  // Process global outputs if they exist and aren't already in a section
  if (item.uiConfig && item.uiConfig.outputs && !sections.some(s => s.outputs && s.outputs.length > 0)) {
    sections.push({
      id: 'outputs',
      label: 'Results',
      outputs: item.uiConfig.outputs.map((output: any) => ({
        id: output.name,
        label: output.title,
        type: output.type,
        buttons: output.buttons,
        containerId: 'main'
      }))
    });
  }
  
  return sections;
};

// Raw fallback metadata items - extend as needed with your full list
const rawFallbackItems = [
  {
    id: "TokenGenerator",
    name: "Token Generator",
    icon: "VpnKey",
    category: "Crypto",
    description:
      "Generate a random string with uppercase, lowercase, numbers, and symbols.",
    uiConfig: {
      inputs: [
        {
          header: "Token Settings",
          fields: [
            {
              name: "uppercase",
              label: "Uppercase (ABC...)",
              type: "switch",
              default: true,
            },
            {
              name: "lowercase",
              label: "Lowercase (abc...)",
              type: "switch",
              default: true,
            },
            {
              name: "numbers",
              label: "Numbers (123...)",
              type: "switch",
              default: true,
            },
            {
              name: "symbols",
              label: "Symbols (!@#...)",
              type: "switch",
              default: false,
            },
            {
              name: "length",
              label: "Length",
              type: "slider",
              min: 8,
              max: 128,
              default: 64,
            },
          ],
          outputs: [
            {
              title: "Generated Token",
              name: "tokenOutput",
              type: "text",
              buttons: ["copy", "refresh"],
            },
          ],
        },
      ],
    },
    processFunction: async (input: any) =>
      fetchFromBackend("TokenGenerator", "generate", input),
  },
  // Add the rest of your items here...
];

// Processed fallback metadata with required sections property
export const fallbackMetadata = rawFallbackItems.map(item => ({
  ...item,
  triggerUpdateOnChange: null, // Add this required property
  sections: createSectionsFromUiConfig(item) // Generate sections from uiConfig
}));