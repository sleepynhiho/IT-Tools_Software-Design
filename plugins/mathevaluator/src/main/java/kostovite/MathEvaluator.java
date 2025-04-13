package kostovite;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException; // Import IOException
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.imageio.ImageIO;

import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;
import org.mariuszgromada.math.mxparser.mXparser; // Import for static methods/constants

// Assuming PluginInterface is standard
public class MathEvaluator implements PluginInterface {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.##########"); // Default formatter

    public MathEvaluator() {
        // Accept mXparser non-commercial license terms
        License.iConfirmNonCommercialUse("Kostovite");
        // Use getLicense() which includes version info
        System.out.println("mXparser License Confirmed for MathEvaluator. Info: " + mXparser.getLicense());

        // Create upload directory if it doesn't exist - Consider error handling
        try {
            // Consider making configurable
            String uploadDir = "math-images";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created math image directory: " + directory.getAbsolutePath());
                } else {
                    System.err.println("Failed to create math image directory: " + directory.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating math image directory: " + e.getMessage());
        }
    }

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "MathEvaluator";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("Math Evaluator Plugin executed (standalone test)");
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uiOperation", "evaluate"); // Use ID
            params.put("expression", "sqrt(variableX^2 + variableY^2)"); // Use ID and match variable names
            params.put("variableX", 3.0); // Use new variable ID
            params.put("variableY", 4.0); // Use new variable ID
            params.put("precision", 5);  // Use new ID
            params.put("generateImage", true); // Use new ID

            Map<String, Object> result = process(params);
            System.out.println("Test Evaluation Result: " + result);

            Map<String, Object> funcParams = new HashMap<>();
            funcParams.put("uiOperation", "getFunctions");
            Map<String, Object> funcResult = process(funcParams);
            System.out.println("Supported Functions Result: " + funcResult);

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates metadata in the NEW format (sections, id, etc.).
     */
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        // --- Top Level Attributes (New Format) ---
        metadata.put("id", "MathEvaluator"); // ID matches class name
        metadata.put("name", "Math Expression Evaluator"); // User-facing name
        metadata.put("description", "Evaluate mathematical expressions with variables, format output, and generate images.");
        metadata.put("icon", "Calculate");
        metadata.put("category", "Math");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Input Expression and Variables ---
        Map<String, Object> inputSection = new HashMap<>();
        inputSection.put("id", "inputConfig");
        inputSection.put("label", "Expression & Variables");

        List<Map<String, Object>> mainInputs = new ArrayList<>();

        // Operation Selection (even if only one primary op now, good for future)
        mainInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Action:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of(
                        Map.of("value", "evaluate", "label", "Evaluate Expression"),
                        Map.of("value", "getFunctions", "label", "List Supported Functions")
                )),
                Map.entry("default", "evaluate"),
                Map.entry("required", true)
        ));


        // Expression Input
        mainInputs.add(Map.ofEntries(
                Map.entry("id", "expression"),
                Map.entry("label", "Expression:"),
                Map.entry("type", "text"),
                Map.entry("multiline", true),
                Map.entry("rows", 3),
                Map.entry("placeholder", "e.g., sin(pi/4) + log10(100)"),
                Map.entry("required", true),
                Map.entry("condition", "uiOperation === 'evaluate'"), // Only show for evaluate
                Map.entry("helperText", "Enter the mathematical expression.")
        ));

        // Variable Inputs (Example for x, y, z) - Consider a more dynamic approach later if needed
        mainInputs.add(createVariableInput("variableX", "Variable x ="));
        mainInputs.add(createVariableInput("variableY", "Variable y ="));
        mainInputs.add(createVariableInput("variableZ", "Variable z ="));


        inputSection.put("inputs", mainInputs);
        sections.add(inputSection);

        // --- Section 2: Formatting Options ---
        Map<String, Object> optionsSection = new HashMap<>();
        optionsSection.put("id", "options");
        optionsSection.put("label", "Output Options");
        optionsSection.put("condition", "uiOperation === 'evaluate'"); // Only show for evaluate

        List<Map<String, Object>> optionInputs = new ArrayList<>();

        // Precision Slider
        optionInputs.add(Map.ofEntries(
                Map.entry("id", "precision"),
                Map.entry("label", "Decimal Precision:"),
                Map.entry("type", "slider"),
                Map.entry("min", 0),
                Map.entry("max", 15),
                Map.entry("default", 10),
                Map.entry("required", false)
        ));

        // Generate Image Switch
        optionInputs.add(Map.ofEntries(
                Map.entry("id", "generateImage"),
                Map.entry("label", "Generate Image of Equation"),
                Map.entry("type", "switch"),
                Map.entry("default", true), // Default to generating image
                Map.entry("required", false)
        ));

        optionsSection.put("inputs", optionInputs);
        sections.add(optionsSection);


        // --- Section 3: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Result");
        // Show if evaluate succeeded OR if functions were requested and succeeded
        resultsSection.put("condition", "success === true && (uiOperation === 'evaluate' || uiOperation === 'getFunctions')");

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Evaluated Expression (Formatted for Image, Raw for Text)
        resultOutputs.add(createOutputField("evaluatedExpression", "Evaluated Expression", "text", "uiOperation === 'evaluate'"));
        // Calculated Result
        resultOutputs.add(createOutputField("calculatedResult", "Result", "text", "uiOperation === 'evaluate'"));
        // Raw Numeric Result (Optional)
        // resultOutputs.add(createOutputField("rawResult", "Raw Result", "text", "uiOperation === 'evaluate' && typeof rawResult !== 'undefined'"));
        // Rendered Image
        Map<String, Object> imageOutput = createOutputField("renderedImage", "", "image", "uiOperation === 'evaluate' && typeof renderedImage !== 'undefined'");
        imageOutput.put("buttons", List.of("download"));
        // Add filename to metadata for download button context
        imageOutput.put("downloadFilenameKey", "imageFileName"); // Tell frontend where to find filename in results
        resultOutputs.add(imageOutput);

        // Supported Functions Table
        Map<String, Object> functionsTable = createOutputField("supportedFunctions", "Supported Functions", "table", "uiOperation === 'getFunctions' && typeof supportedFunctions !== 'undefined'");
        functionsTable.put("columns", List.of(
                Map.of("header", "Name", "field", "name"),
                Map.of("header", "Description", "field", "description"),
                Map.of("header", "Syntax", "field", "syntax")
        ));
        resultOutputs.add(functionsTable);


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 4: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false"); // Show only on failure

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        // Extended mXparser error message if available
        errorOutputs.add(createOutputField("parserErrorMessage", "Parser Message", "text", "typeof parserErrorMessage !== 'undefined'"));


        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create variable input field definitions
    private Map<String, Object> createVariableInput(String id, String label) {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("label", label),
                Map.entry("type", "number"),
                Map.entry("required", false), // Variables are optional
                Map.entry("placeholder", "Enter value..."),
                Map.entry("condition", "uiOperation === 'evaluate'") // Only show for evaluate
        );
    }

    // Helper to create output field definitions more easily
    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", type);
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) { // Basic check for error fields
            field.put("style", "error");
        }
        // Add monospace for specific result types
        if ("text".equals(type) && (id.toLowerCase().contains("result") || id.toLowerCase().contains("expression"))) {
            field.put("monospace", true);
        }
        if ("json".equals(type)) { // Add copy button for JSON type
            field.put("buttons", List.of("copy"));
        }
        return field;
    }


    /**
     * Processes the input parameters (using IDs from the new format)
     * to evaluate an expression or list functions.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        // Read the operation using the ID defined in UI metadata
        String uiOperation = getStringParam(input, "uiOperation", "evaluate"); // Default to evaluate
        String errorOutputId = "errorMessage"; // Matches the error output field ID

        // Include the operation in the input map for context
        Map<String, Object> processingInput = new HashMap<>(input);

        try {
            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "evaluate" -> result = evaluateExpression(processingInput);
                case "getfunctions" -> result = getSupportedFunctions();
                default -> {
                    return Map.of("success", false, errorOutputId, "Unsupported operation: " + uiOperation);
                }
            }

            // Add uiOperation to success result for context
            if (result.get("success") == Boolean.TRUE) {
                Map<String, Object> finalResult = new HashMap<>(result);
                finalResult.put("uiOperation", uiOperation);
                return finalResult;
            } else {
                // Ensure error key consistency
                if (result.containsKey("error") && !result.containsKey(errorOutputId)) {
                    Map<String, Object> finalResult = new HashMap<>(result);
                    finalResult.put(errorOutputId, result.get("error"));
                    finalResult.remove("error");
                    return finalResult;
                }
                return result; // Return error as is
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing math request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Calculation Methods (Updated for new IDs)
    // ========================================================================

    /**
     * Evaluate a mathematical expression using mXparser.
     */
    private Map<String, Object> evaluateExpression(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        String parserErrorId = "parserErrorMessage"; // Specific ID for parser errors

        try {
            // Get parameters using NEW IDs
            String expression = getStringParam(input, "expression", null); // Required
            // **Use getIntParam here**
            int precision = getIntParam(input); // Default 10
            boolean generateImage = getBooleanParam(input); // Default true

            // Create expression parser
            Expression expr = new Expression(expression);

            // Add variables (Check x, y, z specifically for now)
            addVariableIfPresent(expr, input, "variableX", "x");
            addVariableIfPresent(expr, input, "variableY", "y");
            addVariableIfPresent(expr, input, "variableZ", "z");
            // Add more variable inputs if needed

            // Check Syntax FIRST
            if (!expr.checkSyntax()) {
                result.put("success", false);
                result.put(errorOutputId, "Syntax error in expression.");
                result.put(parserErrorId, expr.getErrorMessage()); // Provide specific parser error
                return result;
            }

            // Calculate the result
            double calculatedResult = expr.calculate();

            // Check for calculation errors (NaN usually indicates issues like div by zero, log neg, etc.)
            if (Double.isNaN(calculatedResult)) {
                result.put("success", false);
                result.put(errorOutputId, "Calculation error (e.g., division by zero, invalid input for function).");
                // mXparser might not have a separate error message after successful syntax check but NaN result
                result.put(parserErrorId, "Result is Not-a-Number (NaN). Check function domains (e.g., sqrt of negative).");
                return result;
            }

            // --- Prepare Success Output ---
            result.put("success", true);

            // Set decimal format precision
            decimalFormat.setMaximumFractionDigits(Math.max(0, precision)); // Ensure non-negative
            String formattedResult = decimalFormat.format(calculatedResult);

            result.put("evaluatedExpression", expression); // Echo raw expression
            result.put("calculatedResult", formattedResult); // Formatted result
            // result.put("rawResult", calculatedResult); // Optional: raw double value

            // Generate image if requested
            if (generateImage) {
                try {
                    Map<String, Object> imageGenResult = generateExpressionImage(expression, formattedResult);
                    if (imageGenResult.containsKey("imageBase64")) {
                        result.put("renderedImage", imageGenResult.get("imageBase64")); // Matches output ID
                        result.put("imageFileName", imageGenResult.get("fileName")); // For download button context
                    } else {
                        System.err.println("Image generation failed: " + imageGenResult.get("error"));
                        // Don't fail the whole operation, just omit the image
                    }
                } catch (Exception imgEx) {
                    System.err.println("Exception during image generation: " + imgEx.getMessage());
                    // Omit image on error
                }
            }

        } catch (IllegalArgumentException e) { // Catch our own validation errors
            result.put("success", false);
            result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors during setup/parsing
            System.err.println("Unexpected error during evaluation: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put(errorOutputId, "Unexpected error evaluating expression.");
        }
        return result;
    }

    /** Adds argument to expression if present and valid in input map */
    private void addVariableIfPresent(Expression expr, Map<String, Object> input, String inputId, String variableName) {
        Object value = input.get(inputId);
        if (value != null && !value.toString().trim().isEmpty()) { // Check not null AND not empty string
            try {
                double doubleValue;
                if (value instanceof Number) {
                    doubleValue = ((Number)value).doubleValue();
                } else {
                    // Attempt to parse string, allow for different decimal separators potentially
                    String valStr = value.toString().replace(',', '.');
                    doubleValue = Double.parseDouble(valStr);
                }
                expr.defineArgument(variableName, doubleValue);
                System.out.println("Defined variable: " + variableName + " = " + doubleValue); // Debug log
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse variable '" + inputId + "' value '" + value + "' as number. Ignoring.");
                // Optionally throw an exception here if invalid variable input should halt processing
                // throw new IllegalArgumentException("Invalid value for variable '" + variableName + "': " + value);
            }
        }
    }


    /**
     * Generate an image representation of the expression and its result.
     */
    private Map<String, Object> generateExpressionImage(String expression, String formattedResult) throws IOException {
        Map<String, Object> imageResult = new HashMap<>();
        int width; // Initial width
        int height; // Initial height
        int padding = 10;

        Font mathFont = new Font("DejaVu Sans", Font.PLAIN, 20);
        if (!canDisplayMath(mathFont)) {
            mathFont = new Font("Arial", Font.PLAIN, 20); // Fallback
        }

        String equationText = expression + " = " + formattedResult;

        // --- Calculate text width dynamically ---
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempG2d = tempImage.createGraphics();
        tempG2d.setFont(mathFont);
        FontMetrics fm = tempG2d.getFontMetrics();
        int textWidth = fm.stringWidth(equationText);
        int textHeight = fm.getHeight();
        tempG2d.dispose();

        width = textWidth + (2 * padding); // Adjust width based on text
        height = textHeight + (2* padding); // Adjust height based on font


        // --- Create final image ---
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // Setup rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Draw Text
            g2d.setColor(Color.BLACK);
            g2d.setFont(mathFont);
            // Center text vertically and horizontally
            int y = padding + fm.getAscent(); // Position based on ascent
            g2d.drawString(equationText, padding, y);

        } finally {
            g2d.dispose();
        }

        // Convert to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", baos)) {
            throw new IOException("Failed to write generated image to byte stream.");
        }
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUri = "data:image/png;base64," + base64Image;

        String fileName = "math_result_" + LocalDateTime.now().format(formatter) + ".png";

        imageResult.put("imageBase64", dataUri);
        imageResult.put("fileName", fileName);

        return imageResult;
    }

    /** Check if font can display basic math symbols */
    private boolean canDisplayMath(Font font) {
        return font.canDisplay('√') && font.canDisplay('÷') && font.canDisplay('≠') && font.canDisplay('π');
    }

    /**
     * Get a list of supported mathematical functions from mXparser.
     */
    private Map<String, Object> getSupportedFunctions() {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            // Manually list common functions
            List<Map<String, String>> functions = new ArrayList<>();
            addFunctionInfo(functions, "+", "Addition", "a + b");
            addFunctionInfo(functions, "-", "Subtraction", "a - b");
            addFunctionInfo(functions, "*", "Multiplication", "a * b");
            addFunctionInfo(functions, "/", "Division", "a / b");
            addFunctionInfo(functions, "^", "Power", "a^b");
            addFunctionInfo(functions, "sqrt", "Square Root", "sqrt(x)");
            addFunctionInfo(functions, "sin", "Sine (radians)", "sin(x)");
            addFunctionInfo(functions, "cos", "Cosine (radians)", "cos(x)");
            addFunctionInfo(functions, "tan", "Tangent (radians)", "tan(x)");
            addFunctionInfo(functions, "asin", "Arcsine (inverse sine)", "asin(x)");
            addFunctionInfo(functions, "acos", "Arccosine (inverse cosine)", "acos(x)");
            addFunctionInfo(functions, "atan", "Arctangent (inverse tangent)", "atan(x)");
            addFunctionInfo(functions, "ln", "Natural Logarithm (base e)", "ln(x)");
            addFunctionInfo(functions, "log10", "Common Logarithm (base 10)", "log10(x)");
            addFunctionInfo(functions, "log", "Logarithm (base b)", "log(b, x)");
            addFunctionInfo(functions, "abs", "Absolute Value", "abs(x)");
            addFunctionInfo(functions, "round", "Round to nearest integer", "round(x, n_digits)");
            addFunctionInfo(functions, "floor", "Floor (largest integer <= x)", "floor(x)");
            addFunctionInfo(functions, "ceil", "Ceiling (smallest integer >= x)", "ceil(x)");
            addFunctionInfo(functions, "!", "Factorial", "n!");
            addFunctionInfo(functions, "mod", "Modulo", "mod(a, b)");
            addFunctionInfo(functions, "pi", "Constant Pi", "pi");
            addFunctionInfo(functions, "e", "Constant Euler's number", "e");
            // Add more...

            result.put("success", true);
            result.put("supportedFunctions", functions); // Matches output ID

        } catch (Exception e) {
            System.err.println("Error retrieving functions: " + e.getMessage());
            result.put("success", false);
            result.put(errorOutputId, "Could not retrieve function list.");
        }
        return result;
    }

    /** Helper to add function info to the list */
    private void addFunctionInfo(List<Map<String, String>> list, String name, String description, String syntax) {
        list.add(Map.of("name", name, "description", description, "syntax", syntax));
    }


    // ========================================================================
    // Parameter Parsing Helpers
    // ========================================================================

    // Null default indicates required
    private double parseDoubleParam(Map<String, Object> input, String key, Double defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required numeric parameter: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            try {
                // Replace comma potentially used as decimal separator
                return Double.parseDouble(value.toString().replace(',', '.'));
            } catch (NumberFormatException e) {
                if (defaultValue != null) return defaultValue;
                throw new IllegalArgumentException("Invalid numeric value for parameter '" + key + "': " + value);
            }
        }
    }

    // *** ADDED getIntParam definition ***
    // Null default indicates required
    private int getIntParam(Map<String, Object> input) throws IllegalArgumentException {
        Object value = input.get("precision");
        if (value == null) {
            return 10;
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            // Allow for slight precision issues when casting from double slider value
            if (Math.abs(dValue - Math.round(dValue)) < 0.00001) {
                return ((Number) value).intValue();
            } else {
                return 10; // Return default if not integer-like
            }
        }
        else {
            try {
                // Try parsing as double first to handle potential decimals, then cast
                double dValue = Double.parseDouble(value.toString());
                if (Math.abs(dValue - Math.round(dValue)) < 0.00001) {
                    return (int) Math.round(dValue);
                } else {
                    return 10;
                }
            } catch (NumberFormatException e) {
                return 10;
            }
        }
    }

    // Null default indicates required
    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        String strValue = value.toString(); // Don't trim expressions
        if (strValue.isEmpty() && defaultValue == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        // Return empty string if not required and empty, otherwise return value
        return strValue.isEmpty() ? defaultValue : strValue;
    }

    // Null default indicates required
    private boolean getBooleanParam(Map<String, Object> input) {
        Object value = input.get("generateImage");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return "true".equalsIgnoreCase(value.toString());
        }
        return true;
    }
}