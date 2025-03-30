const fetchFromBackend = async (toolId: string, input: Record<string, any>) => {
  try {
    const response = await fetch(
      `https://api.example.com/tools/${toolId}/execute`,
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
    return { error: "Backend not available or request failed." };
  }
};

// export const mockMetadata = [
//   {
//     id: "advanced-data-transformer",
//     name: "Advanced Data Transformer",
//     icon: "Transform",
//     description:
//       "Perform complex transformations on input data with customizable options.",
//     uiConfig: {
//       sections: [
//         {
//           header: "Input Configuration",
//           fields: [
//             {
//               name: "inputText",
//               label: "Input Text",
//               type: "text",
//               default: "",
//             },
//             {
//               name: "inputNumber",
//               label: "Input Number",
//               type: "number",
//               default: 10,
//               min: 1,
//               max: 100,
//             },
//             {
//               name: "inputSwitch",
//               label: "Enable Advanced Mode",
//               type: "switch",
//               default: false,
//             },
//             {
//               name: "inputSlider",
//               label: "Processing Intensity",
//               type: "slider",
//               min: 1,
//               max: 10,
//               default: 5,
//             },
//             {
//               name: "inputDropdown",
//               label: "Transformation Type",
//               type: "select",
//               options: [
//                 "Uppercase",
//                 "Lowercase",
//                 "Reverse",
//                 "Base64 Encode",
//                 "Base64 Decode",
//               ],
//               default: "Uppercase",
//             },
//           ],
//         },
//         {
//           header: "Action Selection",
//           fields: [
//             {
//               name: "inputButtons",
//               label: "Choose Operation",
//               type: "buttons",
//               options: [
//                 { name: "Transform", value: "transform" },
//                 { name: "Analyze", value: "analyze" },
//                 { name: "Optimize", value: "optimize" },
//               ],
//               default: "transform",
//             },
//           ],
//         },
//       ],
//       outputs: [
//         {
//           title: "Transformed Output",
//           name: "transformedOutput",
//           type: "text",
//           buttons: ["copy", "refresh"],
//         },
//         {
//           title: "Analysis Report",
//           name: "analysisReport",
//           type: "text",
//           buttons: ["copy"],
//         },
//       ],
//     },
//     processFunction: async (input) => {
//       return await fetchFromBackend("advanced-data-transformer", input);
//     },
//   },
// ];

export const mockMetadata = [
  {
    id: "token-generator",
    name: "Token Generator",
    icon: "VpnKey",
    description:
      "Generate a random string with uppercase, lowercase, numbers, and symbols.",
    uiConfig: {
      sections: [
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
      fetchFromBackend("token-generator", input),
  },
  {
    id: "hash-text",
    name: "Hash Text",
    icon: "Lock",
    description:
      "Hash a text string using MD5, SHA1, SHA256, SHA224, SHA512, SHA384, SHA3, or RIPEMD160.",
    uiConfig: {
      sections: [
        {
          header: "Hash Configuration",
          fields: [
            // {
            //   name: "algorithm",
            //   label: "Algorithm",
            //   type: "select",
            //   options: [
            //     "MD5",
            //     "SHA1",
            //     "SHA256",
            //     "SHA224",
            //     "SHA512",
            //     "SHA384",
            //     "SHA3",
            //     "RIPEMD160",
            //   ],
            //   default: "SHA256",
            // },
            {
              name: "input",
              label: "Your text to hash: ",
              type: "text",
              default: "",
            },
            {
              name: "encoding",
              label: "Digest Encoding",
              type: "select",
              options: [
                "Binary (base 2)",
                "Hexadecimal (base 16)",
                "Base64 (base 64)",
                "Base64url (base 64url)",
              ],
              default: "Binary (base 2)",
            },
          ],
        },
      ],

      outputs: [
        {
          title: "MD5",
          name: "MD5",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA1",
          name: "SHA1",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA256",
          name: "SHA256",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA224",
          name: "SHA224",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA512",
          name: "SHA512",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA384",
          name: "SHA384",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "SHA3",
          name: "SHA3",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "RIPEMD160",
          name: "RIPEMD160",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) => fetchFromBackend("hash-text", input),
  },
  {
    id: "ulid-generator",
    name: "ULID Generator",
    icon: "FormatListNumbered",
    description:
      "Generate random Universally Unique Lexicographically Sortable Identifier (ULID).",
    uiConfig: {
      sections: [
        {
          header: "Settings",
          fields: [
            {
              name: "quantity",
              label: "Quantity",
              type: "number",
              default: 1,
              min: 1,
            },
            {
              name: "format",
              label: "Format",
              type: "buttons",
              options: [
                { name: "Raw", value: "raw" },
                { name: "JSON", value: "json" },
              ],
              default: "raw",
            },
          ],
        },
      ],
      outputs: [
        {
          title: "Generated ULID",
          name: "ulid",
          type: "text",
          buttons: ["copy", "refresh"],
        },
      ],
    },
    processFunction: async () => fetchFromBackend("ulid-generator", {}),
  },
  {
    id: "password-strength-analyzer",
    name: "Password Strength Analyzer",
    icon: "Shield",
    description:
      "Analyze the strength of your password and estimate crack time.",
    uiConfig: {
      sections: [
        {
          header: "Password Input",
          fields: [
            {
              name: "password",
              label: "Password",
              type: "password",
              default: "",
            },
          ],
          outputs: [
            {
              title: "Duration to crack this password with brute force",
              name: "duration",
              type: "typography",
              default: "Instantly",
            },
            {
              title: "Password length",
              name: "passwordLength",
              type: "typography",
              default: 0,
            },
            {
              title: "Entropy",
              name: "entropy",
              type: "typography",
              default: 0,
            },
            {
              title: "Character set size",
              name: "charSetSize",
              type: "typography",
              default: 0,
            },
            {
              title: "Score",
              name: "score",
              type: "typography",
              default: "0 / 100",
            },
          ],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("password-strength", input),
  },
  {
    id: "integer-base-converter",
    name: "Integer Base Converter",
    icon: "Functions",
    description:
      "Convert a number between different bases (decimal, hexadecimal, binary, octal, base64, etc.)",
    uiConfig: {
      sections: [
        {
          header: "Conversion Settings",
          fields: [
            {
              name: "Input number",
              label: "Number",
              type: "number",
              default: "42",
            },
            {
              name: "Input base",
              label: "Base",
              type: "number",
              default: "10",
            },
          ],
        },
      ],
      outputs: [
        {
          title: "Binary (2)",
          name: "binary",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Octal (8)",
          name: "octal",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Decimal (10)",
          name: "decimal",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Hexadecimal (16)",
          name: "hexadecimal",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Base64 (64)",
          name: "base64",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("integer-base-converter", input),
  },
  {
    id: "xml-to-json",
    name: "XML to JSON Converter",
    icon: "Transform",
    description: "Convert XML to JSON format.",
    uiConfig: {
      sections: [
        {
          header: "XML Input",
          fields: [
            { name: "xml", label: "XML Input", type: "text", default: "" },
          ],
        },
      ],
      outputs: [
        {
          title: "JSON Output",
          name: "jsonOutput",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) => fetchFromBackend("xml-to-json", input),
  },
];
