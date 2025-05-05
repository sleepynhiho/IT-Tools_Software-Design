package kostovite;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class XMLToJSON implements PluginInterface {

    private static final String ERROR_OUTPUT_ID = "errorMessage";

    @Override
    public String getName() {
        return "XMLToJSON";
    }

    @Override
    public void execute() {
        System.out.println("XMLToJSON Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();

            // Test simple XML conversion
            params.put("inputXML", "<note><to>Tove</to><from>Jani</from><heading>Reminder</heading><body>Don't forget me this weekend!</body></note>");
            Map<String, Object> result1 = process(params);
            System.out.println("Test 1 (Simple XML): " + result1);

            // Test XML with attributes
            params.put("inputXML", "<root attrib=\"value\"><element>Text</element></root>");
            Map<String, Object> result2 = process(params);
            System.out.println("Test 2 (XML with attributes): " + result2);

            // Test nested XML structure
            params.put("inputXML", "<library><book><title>Java Programming</title><author>John Doe</author><year>2020</year></book><book><title>XML Basics</title><author>Jane Smith</author><year>2018</year></book></library>");
            Map<String, Object> result3 = process(params);
            System.out.println("Test 3 (Nested XML): " + result3);

            // Test invalid XML
            params.put("inputXML", "<unclosed>This XML is not valid");
            Map<String, Object> result4 = process(params);
            System.out.println("Test 4 (Invalid XML): " + result4);

            // Test empty input
            params.put("inputXML", "");
            Map<String, Object> result5 = process(params);
            System.out.println("Test 5 (Empty input): " + result5);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes ---
        metadata.put("customUI", false);
        metadata.put("name", "XML to JSON");
        metadata.put("icon", "Code");
        metadata.put("description", "Convert XML to JSON");
        metadata.put("id", "XMLToJSON");
        metadata.put("category", "Converter");

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section: XML to JSON ---
        Map<String, Object> xmlToJsonSection = new HashMap<>();
        xmlToJsonSection.put("id", "xmlToJson");
        xmlToJsonSection.put("label", "");

        // --- XML to JSON Inputs ---
        List<Map<String, Object>> xmlInputs = new ArrayList<>();
        xmlInputs.add(Map.ofEntries(
                Map.entry("label", "Your XML content"),
                Map.entry("placeholder", "Paste your XML here..."),
                Map.entry("required", true),
                Map.entry("multiline", true),
                Map.entry("containerId", "input"),
                Map.entry("id", "inputXML"),
                Map.entry("type", "text"),
                Map.entry("width", 600),
                Map.entry("height", 420)
        ));
        xmlToJsonSection.put("inputs", xmlInputs);

        // --- XML to JSON Outputs ---
        List<Map<String, Object>> xmlOutputs = new ArrayList<>();
        xmlOutputs.add(Map.ofEntries(
                Map.entry("label", "Converted JSON"),
                Map.entry("monospace", true),
                Map.entry("buttons", List.of("copy")),
                Map.entry("buttonPlacement", Map.of("copy", "inside")),
                Map.entry("id", "jsonOutput"),
                Map.entry("type", "text"),
                Map.entry("width", 440),
                Map.entry("height", 36),
                Map.entry("containerId", "output")
        ));
        xmlToJsonSection.put("outputs", xmlOutputs);

        sections.add(xmlToJsonSection);

        // --- Error Section ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(Map.ofEntries(
                Map.entry("id", ERROR_OUTPUT_ID),
                Map.entry("label", "Details"),
                Map.entry("type", "text"),
                Map.entry("style", "error")
        ));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);

        metadata.put("sections", sections);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        try {
            String xmlString = getStringParam(input, "inputXML", null);

            // Validation
            if (xmlString == null || xmlString.trim().isEmpty()) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "XML input is required.");
            }

            xmlString = xmlString.trim();

            // Check if input is valid XML
            Document document;
            try {
                document = parseXML(xmlString);
            } catch (Exception e) {
                return Map.of("success", false, ERROR_OUTPUT_ID, "Invalid XML format. Please check your input: " + e.getMessage());
            }

            // Convert XML to JSON
            String jsonString = convertXmlToJson(document);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("jsonOutput", jsonString);

            return result;

        } catch (Exception e) {
            System.err.println("Error processing XML to JSON conversion: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, ERROR_OUTPUT_ID, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // Parse XML string to Document
    private Document parseXML(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }

    // Convert XML Document to JSON string
    private String convertXmlToJson(Document document) {
        try {
            // Start building JSON
            StringBuilder jsonBuilder = new StringBuilder();
            Element rootElement = document.getDocumentElement();
            jsonBuilder.append("{\n");
            processElement(rootElement, jsonBuilder, 2);
            jsonBuilder.append("\n}");
            return jsonBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting XML to JSON: " + e.getMessage(), e);
        }
    }

    // Process an XML element and its children to build JSON
    private void processElement(Element element, StringBuilder jsonBuilder, int indent) {
        String indentStr = " ".repeat(indent);
        String elementName = element.getNodeName();

        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        boolean hasAttributes = attributes.getLength() > 0;

        jsonBuilder.append(indentStr).append("\"").append(elementName).append("\": ");

        // Check for child elements or text content
        NodeList childNodes = element.getChildNodes();
        boolean hasChildElements = hasChildElements(childNodes);

        if (hasChildElements) {
            // Element has child elements, create an object
            jsonBuilder.append("{\n");

            // Add attributes as properties
            if (hasAttributes) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    jsonBuilder.append(indentStr + "  \"@")
                            .append(attr.getNodeName())
                            .append("\": \"")
                            .append(escapeJsonString(attr.getNodeValue()))
                            .append("\",\n");
                }
            }

            // Process child elements
            Map<String, List<Element>> childElements = groupChildElements(childNodes);

            int elementCounter = 0;
            for (Map.Entry<String, List<Element>> entry : childElements.entrySet()) {
                String childName = entry.getKey();
                List<Element> elements = entry.getValue();

                if (elements.size() == 1) {
                    // Single child element
                    Element childElement = elements.get(0);
                    if (hasOnlyTextContent(childElement)) {
                        // Simple text content
                        String content = getTextContent(childElement).trim();
                        jsonBuilder.append(indentStr + "  \"")
                                .append(childName)
                                .append("\": \"")
                                .append(escapeJsonString(content))
                                .append("\"");
                    } else {
                        // Nested object
                        jsonBuilder.append(indentStr + "  \"")
                                .append(childName)
                                .append("\": {");
                        processElement(childElement, new StringBuilder(), indent + 4);
                        jsonBuilder.append(indentStr + "  }");
                    }
                } else {
                    // Array of elements
                    jsonBuilder.append(indentStr + "  \"")
                            .append(childName)
                            .append("\": [\n");

                    for (int i = 0; i < elements.size(); i++) {
                        Element childElement = elements.get(i);
                        jsonBuilder.append(indentStr + "    {");

                        if (hasOnlyTextContent(childElement)) {
                            String content = getTextContent(childElement).trim();
                            jsonBuilder.append("\"#text\": \"")
                                    .append(escapeJsonString(content))
                                    .append("\"");
                        } else {
                            processElement(childElement, new StringBuilder(), indent + 6);
                        }

                        jsonBuilder.append(i < elements.size() - 1 ? "},\n" : "}\n");
                    }

                    jsonBuilder.append(indentStr + "  ]");
                }

                elementCounter++;
                if (elementCounter < childElements.size()) {
                    jsonBuilder.append(",\n");
                }
            }

            // Handle text content if present alongside child elements
            String textContent = getDirectTextContent(element).trim();
            if (!textContent.isEmpty()) {
                if (childElements.size() > 0) {
                    jsonBuilder.append(",\n");
                }
                jsonBuilder.append(indentStr + "  \"#text\": \"")
                        .append(escapeJsonString(textContent))
                        .append("\"");
            }

            jsonBuilder.append("\n").append(indentStr).append("}");
        } else {
            // Element has only text content or is empty
            String content = element.getTextContent().trim();
            if (content.isEmpty() && hasAttributes) {
                // Only attributes, no content
                jsonBuilder.append("{\n");
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    jsonBuilder.append(indentStr + "  \"@")
                            .append(attr.getNodeName())
                            .append("\": \"")
                            .append(escapeJsonString(attr.getNodeValue()))
                            .append("\"");
                    if (i < attributes.getLength() - 1) {
                        jsonBuilder.append(",\n");
                    }
                }
                jsonBuilder.append("\n").append(indentStr).append("}");
            } else if (!content.isEmpty()) {
                // Has text content
                if (hasAttributes) {
                    // Text content with attributes
                    jsonBuilder.append("{\n");
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Node attr = attributes.item(i);
                        jsonBuilder.append(indentStr + "  \"@")
                                .append(attr.getNodeName())
                                .append("\": \"")
                                .append(escapeJsonString(attr.getNodeValue()))
                                .append("\",\n");
                    }
                    jsonBuilder.append(indentStr + "  \"#text\": \"")
                            .append(escapeJsonString(content))
                            .append("\"")
                            .append("\n").append(indentStr).append("}");
                } else {
                    // Just text content
                    jsonBuilder.append("\"").append(escapeJsonString(content)).append("\"");
                }
            } else {
                // Empty element
                jsonBuilder.append("{}");
            }
        }
    }

    // Check if NodeList contains any Element nodes
    private boolean hasChildElements(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    // Group child elements by node name
    private Map<String, List<Element>> groupChildElements(NodeList nodes) {
        Map<String, List<Element>> grouped = new LinkedHashMap<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String nodeName = element.getNodeName();

                if (!grouped.containsKey(nodeName)) {
                    grouped.put(nodeName, new ArrayList<>());
                }

                grouped.get(nodeName).add(element);
            }
        }

        return grouped;
    }

    // Check if element has only text content (no child elements)
    private boolean hasOnlyTextContent(Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }
        return true;
    }

    // Get direct text content (excluding child element content)
    private String getDirectTextContent(Element element) {
        StringBuilder content = new StringBuilder();
        NodeList childNodes = element.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                content.append(node.getNodeValue());
            }
        }

        return content.toString();
    }

    // Get text content of an element
    private String getTextContent(Element element) {
        return element.getTextContent();
    }

    // Escape special characters in JSON strings
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        result.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            result.append('0');
                        }
                        result.append(hex);
                    } else {
                        result.append(c);
                    }
            }
        }
        return result.toString();
    }

    // Helper method to get string parameters
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) {
                return null;
            }
            return defaultValue;
        }
        return value.toString();
    }
}