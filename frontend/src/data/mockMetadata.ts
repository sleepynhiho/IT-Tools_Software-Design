import { Label } from "@mui/icons-material";

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

export const mockMetadata = [
  {
    id: "token-generator",
    name: "Token Generator",
    icon: "VpnKey",
    category: "Crypto",
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
    category: "Crypto",
    description:
      "Hash a text string using MD5, SHA1, SHA256, SHA224, SHA512, SHA384, SHA3, or RIPEMD160.",
    uiConfig: {
      sections: [
        {
          header: "Hash Configuration",
          fields: [
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
    category: "Crypto",
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
    category: "Crypto",
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
    category: "Converter",
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
    category: "Converter",
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
  {
    id: "date-time-converter",
    name: "Date-time converter",
    icon: "CalendarToday",
    category: "Converter",
    description: "Convert date and time into the various different formats",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "input",
              label: "Put your date string here...",
              type: "text",
              default: "",
            },
            {
              name: "date-type",
              label: "Date type",
              type: "select",
              options: [
                "JS locale date string",
                "ISO 8601",
                "ISO 9075",
                "RFC 3339",
                "RFC 7231",
                "Unix timestamp",
                "Timestamp",
                "UTC format",
                "Mongo ObjectID",
                "Excel date/time",
              ],
              default: "Timestamp",
            },
          ],
        },
      ],

      outputs: [
        {
          title: "JS locale date string",
          name: "JS-locale-date-string",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "ISO 8601",
          name: "ISO-8601",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "ISO 9075",
          name: "ISO-9075",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "RFC 3339",
          name: "RFC-3339",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "RFC 7231",
          name: "RFC-7231",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Unix timestamp",
          name: "Unix-timestamp",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Timestamp",
          name: "Timestamp",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "UTC format",
          name: "UTC-format",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Mongo ObjectID",
          name: "Mongo-ObjectID",
          type: "text",
          buttons: ["copy"],
        },
        {
          title: "Excel date/time",
          name: "Excel-date-time",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("date-time-converter", input),
  },
  {
    id: "qr-code-generator",
    name: "QR Code Generator",
    icon: "QrCode",
    category: "Images & Videos",
    description:
      "Generate and download a QR code for a URL (or just plain text), and customize the background and foreground colors.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "input",
              label: "Text: ",
              type: "text",
              default: "https://it-tools.tech",
            },
            {
              name: "foreground-color",
              label: "Foreground color:",
              type: "color",
              default: "#000000ff",
            },
            {
              name: "background-color",
              label: "Background color:",
              type: "color",
              default: "#ffffff00",
            },
            {
              name: "error-resistance",
              label: "Error resistance: ",
              type: "select",
              options: ["low", "medium", "quartile", "high"],
              default: "medium",
            },
          ],
        },
      ],

      outputs: [
        {
          title: "QR Code",
          name: "qrCode",
          type: "image",
          buttons: ["download"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("qr-code-generator", input),
  },
  {
    id: "wifi-qr-code-generator",
    name: "WiFi QR Code Generator",
    icon: "Wifi",
    category: "Images & Videos",
    description:
      "Generate and download QR codes for quick connections to WiFi networks.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "Encryption method",
              label: "Encryption method",
              type: "select",
              options: ["No password", "WPA/WPA2", "WEP", "WPA2-EAP"],
              default: "WPA/WPA2",
            },
            {
              name: "ssid",
              label: "SSID (Network Name)",
              type: "text",
              default: "",
            },
            {
              name: "hidden ssid",
              label: "Hidden SSID",
              type: "switch",
              default: false,
            },
            {
              name: "password",
              label: "Password",
              type: "text",
              default: "",
            },
            {
              name: "foreground-color",
              label: "Foreground color:",
              type: "color",
              default: "#000000ff",
            },
            {
              name: "background-color",
              label: "Background color:",
              type: "color",
              default: "#ffffff00",
            },
          ],
        },
      ],

      outputs: [
        {
          title: "WiFi QR Code",
          name: "wifiQrCode",
          type: "image",
          buttons: ["download"],
        },
      ],
    },
  },
  {
    id: "camera-recorder",
    name: "Camera Recorder",
    icon: "CameraAlt",
    category: "Images & Videos",
    description: "Take a picture or record a video from your webcam or camera.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "video",
              label: "Video: ",
              type: "select",
              options: ["Integrated Camera", "External Camera"],
              default: "Integrated Camera",
            },
            {
              name: "audio",
              label: "Audio: ",
              type: "select",
              options: ["Integrated Microphone", "External Microphone"],
              default: "Integrated Microphone",
            },
            {
              name: "start-webcam",
              label: "Start Webcam",
              type: "button",
              default: "Start Webcam",
            },
            {
              name: "Take screenshot",
              label: "Take screenshot",
              type: "button",
              default: "Take screenshot",
            },
          ],
        },
      ],
      outputs: [
        {
          title: "Camera",
          name: "camera",
          type: "video",
          buttons: ["take picture", "record video"],
        },
        {
          title: "Video Recorder",
          name: "videoRecorder",
          type: "video",
          buttons: ["record video"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("camera-recorder", input),
  },
  {
    id: "json-prettify-format",
    name: "JSON prettify and format",
    icon: "FormatQuote",
    category: "Development",
    description:
      "Prettify your JSON string into a friendly, human-readable format.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "sort-keys",
              label: "Sort keys",
              type: "switch",
              default: false,
            },
            {
              name: "indent-size",
              label: "Indent size",
              type: "number",
              default: 2,
            },
            {
              name: "raw-json",
              label: "Your raw json",
              type: "text",
              default: '{"hello": "world", "foo": "bar"}',
            },
          ],
        },
      ],

      outputs: [
        {
          title: "Prettified version of your JSON",
          name: "prettifiedJson",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("json-prettify-format", input),
  },
  {
    id: "json-minify",
    name: "JSON minify",
    icon: "Compress",
    category: "Development",
    description:
      "Minify and compress your JSON by removing unnecessary whitespace.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "raw-json",
              label: "Your raw json",
              type: "text",
              default: `{
  "hello": [
    "world"
  ]
}`,
            },
          ],
        },
      ],

      outputs: [
        {
          title: "Minified version of your JSON",
          name: "minifiedJson",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("json-minify", input),
  },
  {
    id: "email-normalizer",
    name: "Email Normalizer",
    icon: "Email",
    category: "Development",
    description:
      "Normalize email addresses by converting them to lowercase and removing unnecessary characters.",

    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "raw-emails",
              label: "Raw emails to normalize:",
              type: "text",
              default: '',
            },
          ],
        },
      ],

      outputs: [
        {
          title: "Normalized emails:",
          name: "normalizedEmails",
          type: "text",
          buttons: ["copy"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("email-normalizer", input),
  },
  {
    id: "ipv4-subnet-calculator",
    name: "IPv4 subnet calculator",
    icon: "NetworkCheck",
    category: "Network",
    description:
      "Parse your IPv4 CIDR blocks and get all the info you need about your subnet.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "ipv4",
              label: "An IPv4 address with or without mask",
              type: "text",
              default: '192.168.11.0/24',
            },
            {
              name: "previous-block",
              label: "Previous block",
              type: "button",
              default: "Previous block",
            },
            {
              name: "next-block",
              label: "Next block",
              type: "button",
              default: "Next block",
            }
          ],
        },
      ],

      outputs: [
        { title: "Netmask", name: "netmask", type: "text", buttons: ["copy"] },
        { title: "Network address", name: "networkAddress", type: "text", buttons: ["copy"] },
        { title: "Network mask", name: "networkMask", type: "text", buttons: ["copy"] },
        { title: "Network mask in binary", name: "binaryMask", type: "text", buttons: ["copy"] },
        { title: "CIDR notation", name: "cidr", type: "text", buttons: ["copy"] },
        { title: "Wildcard mask", name: "wildcardMask", type: "text", buttons: ["copy"] },
        { title: "Network size", name: "networkSize", type: "text", buttons: ["copy"] },
        { title: "First address", name: "firstAddress", type: "text", buttons: ["copy"] },
        { title: "Last address", name: "lastAddress", type: "text", buttons: ["copy"] },
        { title: "Broadcast address", name: "broadcastAddress", type: "text", buttons: ["copy"] },
        { title: "IP class", name: "ipClass", type: "text", buttons: ["copy"] }
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("ipv4-subnet-calculator", input),
  },
  {
    id: "ipv4-address-converter",
    name: "IPv4 address converter",
    icon: "Dns",
    category: "Network",
    description:
      "Convert an IP address into decimal, binary, hexadecimal, or even an IPv6 representation of it.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "ipv4-address",
              label: "The ipv4 address:",
              type: "text",
              default: '192.168.1.1',
            }
          ],
        },
      ],

      outputs: [
        { title: "Decimal", name: "decimal", type: "text", buttons: ["copy"] },
        { title: "Hexadecimal", name: "hexadecimal", type: "text", buttons: ["copy"] },
        { title: "Binary", name: "binary", type: "text", buttons: ["copy"] },
        { title: "IPv6", name: "ipv6", type: "text", buttons: ["copy"] },
        { title: "IPv6 (short): ", name: "ipv6-short", type: "text", buttons: ["copy"] },
        
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("ipv4-address-converter", input),
  },
  {
    id: "mac-address-lookup",
    name: "MAC address lookup",
    icon: "FindInPage",
    category: "Network",
    description:
      "Find the vendor and manufacturer of a device by its MAC address.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "mac-address",
              label: "MAC address:",
              type: "text",
              default: '20:37:06:12:34:56',
            }
          ],
        },
      ],

      outputs: [
        { title: "Vendor info:", name: "vendor-info", type: "text", buttons: ["copy"] },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("mac-address-lookup", input),
  },
  {
    id: "lorem-ipsum-generator",
    name: "Lorem Ipsum Generator",
    icon: "TextSnippet",
    category: "Text",
    description:
      "Lorem ipsum is a placeholder text commonly used to demonstrate the visual form of a document or a typeface without relying on meaningful content",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "paragraphs",
              label: "Number of paragraphs:",
              type: "number",
              default: 1,
            },
            {
              name: "sentences",
              label: "Sentences per paragraph",
              type: "number",
              default: 5,
            },
            {
              name: "words",
              label: "Words per sentence",
              type: "number",
              default: 0,
            },
            {
              name: "start-with-lorem-ipsum",
              label: "Start with lorem ipsum",
              type: "switch",
              default: true,
            },
            {
              name: "As html",
              label: "As html",
              type: "switch",
              default: false,
            }
          ],
        },
      ],

      outputs: [
        {
          title: "Generated Lorem Ipsum",
          name: "loremIpsum",
          type: "text",
          buttons: ["copy", "refresh"],
        },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("lorem-ipsum-generator", input),
  },
  {
    id: "text-statistics",
    name: "Text statistics",
    icon: "Assessment",
    category: "Text",
    description:
      "Get information about a text, the number of characters, the number of words, its size in bytes, ...",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "raw-text",
              label: "Your text",
              type: "text",
              default: '',
            }
          ],
        },
      ],

      outputs: [
        { title: "Character count", name: "character-count", type: "number"},
        { title: "Word count", name: "word-count", type: "number"},
        { title: "Line count", name: "line-count", type: "number"},
        { title: "Byte size", name: "byte-size", type: "text"},
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("text-statistics", input),
  },
  {
    id: "string-obfuscator",
    name: "String obfuscator",
    icon: "VisibilityOff",
    category: "Text",
    description:
      "Obfuscate a string (like a secret, an IBAN, or a token) to make it shareable and identifiable without revealing its content.",
    uiConfig: {
      sections: [
        {
          header: "",
          fields: [
            {
              name: "string-to-obfuscate",
              label: "String to obfuscate:",
              type: "text",
              default: 'Lorem ipsum dolor sit amet',
            },
            {
              name: "keep-first",
              label: "Keep first: ",
              type: "number",
              default: 4,
            },
            {
              name: "keep-last",
              label: "Keep last: ",
              type: "number",
              default: 4,
            },
            {
              name: "keep-spaces",
              label: "Keep spaces:",
              type: "switch",
              default: 'true',
            },
          ],
        },
      ],

      outputs: [
        { title: "Obfucscated string", name: "obfuscated-string", type: "text", buttons: ["copy"] },
      ],
    },
    processFunction: async (input) =>
      fetchFromBackend("string-obfuscator", input),
  },
];
