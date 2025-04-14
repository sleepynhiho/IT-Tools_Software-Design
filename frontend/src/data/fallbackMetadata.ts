/**
 * Gọi API xử lý plugin phía backend (PF4J).
 *
 * @param toolId   Tên plugin (ví dụ: "HashTools")
 * @param operation Tên thao tác cần thực hiện (ví dụ: "sha256", "md5")
 * @param inputData  Dữ liệu đầu vào (text, image, ... tùy plugin)
 */
const fetchFromBackend = async (
    toolId: string,
    operation: string,
    inputData: Record<string, any>
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
  
  
  export const fallbackMetadata = [
    // Copy your existing mockMetadata array here
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
      processFunction: async (input) =>
        fetchFromBackend("TokenGenerator", "generate", input),
    },

  ];