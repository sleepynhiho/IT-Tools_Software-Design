package kostovite;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

public class MathEvaluator implements PluginInterface {

    private final String uploadDir = "math-images";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.##########");

    public MathEvaluator() {
        // Create upload directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Accept mXparser non-commercial license terms
        License.iConfirmNonCommercialUse("Kostovite");
    }

    @Override
    public String getName() {
        return "MathEvaluator";
    }

    @Override
    public void execute() {
        System.out.println("Math Evaluator Plugin executed");

        // Demonstrate basic usage
        try {
            String expression = "2^2 + sqrt(16) - sin(0)";

            Map<String, Object> params = new HashMap<>();
            params.put("expression", expression);

            Map<String, Object> result = process(params);
            System.out.println("Expression: " + expression);
            System.out.println("Result: " + result.get("result"));
            System.out.println("Formatted: " + result.get("formatted"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName()); // Corresponds to ToolMetadata.name
        metadata.put("version", "1.0.0");
        metadata.put("description", "Evaluate and format mathematical expressions"); // Corresponds to ToolMetadata.description

        // Define available backend operations (for informational purposes or direct API calls)
        Map<String, Object> operations = new HashMap<>();

        // Evaluate operation
        Map<String, Object> evaluateOperation = new HashMap<>();
        evaluateOperation.put("description", "Evaluate a mathematical expression");
        Map<String, Object> evaluateInputs = new HashMap<>();
        evaluateInputs.put("expression", Map.of("type", "string", "description", "Mathematical expression to evaluate", "required", true));
        evaluateInputs.put("formatOutput", Map.of("type", "boolean", "description", "Whether to format the expression (default: true)", "required", false));
        evaluateInputs.put("generateImage", Map.of("type", "boolean", "description", "Whether to generate a rendered image (default: true)", "required", false));
        evaluateInputs.put("precision", Map.of("type", "integer", "description", "Number of decimal places for result (default: 10)", "required", false));
        evaluateInputs.put("variables", Map.of("type", "object", "description", "Map of variable names to values", "required", false));
        evaluateOperation.put("inputs", evaluateInputs);
        operations.put("evaluate", evaluateOperation);

        // Get supported functions operation
        Map<String, Object> getFunctionsOperation = new HashMap<>();
        getFunctionsOperation.put("description", "Get list of supported mathematical functions");
        operations.put("getSupportedFunctions", getFunctionsOperation);

        metadata.put("operations", operations); // Keep this for backend/API reference

        // --- Define UI Configuration ---
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("id", "MathEvaluator"); // Corresponds to ToolMetadata.id
        uiConfig.put("icon", "Calculate"); // Corresponds to ToolMetadata.icon (Material Icon name)
        uiConfig.put("category", "Math"); // Corresponds to ToolMetadata.category

        // --- Define UI Inputs ---
        List<Map<String, Object>> uiInputs = new ArrayList<>();

        // Input Section 1: Math Expression
        Map<String, Object> inputSection1 = new HashMap<>();
        inputSection1.put("header", "Math Expression");
        List<Map<String, Object>> section1Fields = new ArrayList<>();

        // Expression input field
        Map<String, Object> expressionField = new HashMap<>();
        expressionField.put("name", "expression");
        expressionField.put("label", "Expression:");
        expressionField.put("type", "text");
        expressionField.put("placeholder", "Enter a mathematical expression...");
        expressionField.put("required", true);
        expressionField.put("helperText", "Example: 2+2, sin(pi/2), sqrt(16), etc.");
        section1Fields.add(expressionField);

        inputSection1.put("fields", section1Fields);
        uiInputs.add(inputSection1);

        // Input Section 2: Variables
        Map<String, Object> inputSection2 = new HashMap<>();
        inputSection2.put("header", "Variables");
        List<Map<String, Object>> section2Fields = new ArrayList<>();

        // Variable X input
        Map<String, Object> xVariableField = new HashMap<>();
        xVariableField.put("name", "variables.x");
        xVariableField.put("label", "x =");
        xVariableField.put("type", "number");
        xVariableField.put("helperText", "Use in expressions as: x");
        xVariableField.put("required", false);
        section2Fields.add(xVariableField);

        // Variable Y input
        Map<String, Object> yVariableField = new HashMap<>();
        yVariableField.put("name", "variables.y");
        yVariableField.put("label", "y =");
        yVariableField.put("type", "number");
        yVariableField.put("helperText", "Use in expressions as: y");
        yVariableField.put("required", false);
        section2Fields.add(yVariableField);

        // Variable Z input
        Map<String, Object> zVariableField = new HashMap<>();
        zVariableField.put("name", "variables.z");
        zVariableField.put("label", "z =");
        zVariableField.put("type", "number");
        zVariableField.put("helperText", "Use in expressions as: z");
        zVariableField.put("required", false);
        section2Fields.add(zVariableField);

        inputSection2.put("fields", section2Fields);
        uiInputs.add(inputSection2);

        // Input Section 3: Options
        Map<String, Object> inputSection3 = new HashMap<>();
        inputSection3.put("header", "Options");
        List<Map<String, Object>> section3Fields = new ArrayList<>();

        // Precision slider
        Map<String, Object> precisionField = new HashMap<>();
        precisionField.put("name", "precision");
        precisionField.put("label", "Decimal Precision:");
        precisionField.put("type", "slider");
        precisionField.put("min", 0);
        precisionField.put("max", 15);
        precisionField.put("default", 10);
        precisionField.put("required", false);
        section3Fields.add(precisionField);

        // Format output switch
        Map<String, Object> formatOutputField = new HashMap<>();
        formatOutputField.put("name", "formatOutput");
        formatOutputField.put("label", "Format Math Symbols");
        formatOutputField.put("type", "switch");
        formatOutputField.put("default", true);
        formatOutputField.put("required", false);
        formatOutputField.put("helperText", "Use Unicode math symbols in output");
        section3Fields.add(formatOutputField);

        // Generate image switch
        Map<String, Object> generateImageField = new HashMap<>();
        generateImageField.put("name", "generateImage");
        generateImageField.put("label", "Generate Image");
        generateImageField.put("type", "switch");
        generateImageField.put("default", true);
        generateImageField.put("required", false);
        formatOutputField.put("helperText", "Create a rendered image of the equation");
        section3Fields.add(generateImageField);

        inputSection3.put("fields", section3Fields);
        uiInputs.add(inputSection3);

        uiConfig.put("inputs", uiInputs);

        // --- Define UI Outputs ---
        List<Map<String, Object>> uiOutputs = new ArrayList<>();

        // Output Section 1: Calculation Result
        Map<String, Object> outputSection1 = new HashMap<>();
        outputSection1.put("header", "Calculation Result");
        outputSection1.put("condition", "success");
        List<Map<String, Object>> section1OutputFields = new ArrayList<>();

        // Formatted Expression
        Map<String, Object> formattedExpressionOutput = new HashMap<>();
        formattedExpressionOutput.put("title", "Expression");
        formattedExpressionOutput.put("name", "formatted");
        formattedExpressionOutput.put("type", "text");
        formattedExpressionOutput.put("condition", "formatOutput");
        section1OutputFields.add(formattedExpressionOutput);

        // Raw Expression (shown when formatting is disabled)
        Map<String, Object> rawExpressionOutput = new HashMap<>();
        rawExpressionOutput.put("title", "Expression");
        rawExpressionOutput.put("name", "expression");
        rawExpressionOutput.put("type", "text");
        rawExpressionOutput.put("condition", "!formatOutput");
        section1OutputFields.add(rawExpressionOutput);

        // Result
        Map<String, Object> resultOutput = new HashMap<>();
        resultOutput.put("title", "Result");
        resultOutput.put("name", "formattedResult");
        resultOutput.put("type", "text");
        resultOutput.put("buttons", List.of("copy"));
        section1OutputFields.add(resultOutput);

        // Raw Result (for precision operations)
        Map<String, Object> rawResultOutput = new HashMap<>();
        rawResultOutput.put("title", "Raw Result");
        rawResultOutput.put("name", "result");
        rawResultOutput.put("type", "text");
        rawResultOutput.put("condition", "result !== formattedResult");
        section1OutputFields.add(rawResultOutput);

        outputSection1.put("fields", section1OutputFields);
        uiOutputs.add(outputSection1);

        // Output Section 2: Rendered Image
        Map<String, Object> outputSection2 = new HashMap<>();
        outputSection2.put("header", "Rendered Equation");
        outputSection2.put("condition", "generateImage && imageBase64");
        List<Map<String, Object>> section2OutputFields = new ArrayList<>();

        // Image display
        Map<String, Object> imageOutput = new HashMap<>();
        imageOutput.put("title", "");
        imageOutput.put("name", "imageBase64");
        imageOutput.put("type", "image");
        imageOutput.put("buttons", List.of("download"));
        section2OutputFields.add(imageOutput);

        outputSection2.put("fields", section2OutputFields);
        uiOutputs.add(outputSection2);

        // Output Section 3: Error Display
        Map<String, Object> outputSection3 = new HashMap<>();
        outputSection3.put("header", "Error Information");
        outputSection3.put("condition", "error");
        List<Map<String, Object>> section3OutputFields = new ArrayList<>();

        // Error message
        Map<String, Object> errorOutput = new HashMap<>();
        errorOutput.put("title", "Error");
        errorOutput.put("name", "error");
        errorOutput.put("type", "text");
        errorOutput.put("style", "error");
        section3OutputFields.add(errorOutput);

        // Extended error message
        Map<String, Object> errorMessageOutput = new HashMap<>();
        errorMessageOutput.put("title", "Details");
        errorMessageOutput.put("name", "errorMessage");
        errorMessageOutput.put("type", "text");
        errorMessageOutput.put("condition", "errorMessage");
        errorMessageOutput.put("style", "error");
        section3OutputFields.add(errorMessageOutput);

        outputSection3.put("fields", section3OutputFields);
        uiOutputs.add(outputSection3);

        // Output Section 4: Supported Functions (for getSupportedFunctions operation)
        Map<String, Object> outputSection4 = new HashMap<>();
        outputSection4.put("header", "Supported Functions");
        outputSection4.put("condition", "functions");
        List<Map<String, Object>> section4OutputFields = new ArrayList<>();

        // Functions table
        Map<String, Object> functionsOutput = new HashMap<>();
        functionsOutput.put("name", "functions");
        functionsOutput.put("type", "table");
        List<Map<String, Object>> functionsColumns = new ArrayList<>();
        functionsColumns.add(Map.of("header", "Function", "field", "name"));
        functionsColumns.add(Map.of("header", "Description", "field", "description"));
        functionsColumns.add(Map.of("header", "Example", "field", "example"));
        functionsOutput.put("columns", functionsColumns);
        section4OutputFields.add(functionsOutput);

        outputSection4.put("fields", section4OutputFields);
        uiOutputs.add(outputSection4);

        uiConfig.put("outputs", uiOutputs);

        // Add the structured uiConfig to the main metadata map
        metadata.put("uiConfig", uiConfig);

        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "evaluate");

            switch (operation.toLowerCase()) {
                case "evaluate":
                    String expression = (String) input.get("expression");
                    if (expression == null || expression.trim().isEmpty()) {
                        result.put("error", "Expression cannot be empty");
                        return result;
                    }

                    boolean formatOutput = Boolean.parseBoolean(String.valueOf(
                            input.getOrDefault("formatOutput", true)));
                    boolean generateImage = Boolean.parseBoolean(String.valueOf(
                            input.getOrDefault("generateImage", true)));

                    int precision = input.containsKey("precision") ?
                            Integer.parseInt(input.get("precision").toString()) : 10;

                    @SuppressWarnings("unchecked")
                    Map<String, Double> variables = (Map<String, Double>) input.getOrDefault("variables", new HashMap<>());

                    return evaluateExpression(expression, formatOutput, generateImage, precision, variables);

                case "getsupportedfunctions":
                    return getSupportedFunctions();

                default:
                    result.put("error", "Unsupported operation: " + operation);
                    return result;
            }

        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Evaluate a mathematical expression
     *
     * @param expression Mathematical expression to evaluate
     * @param formatOutput Whether to format the expression
     * @param generateImage Whether to generate a rendered image
     * @param precision Number of decimal places for result
     * @param variables Map of variable names to values
     * @return Result information
     */
    private Map<String, Object> evaluateExpression(String expression, boolean formatOutput,
                                                   boolean generateImage, int precision, Map<String, Double> variables) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Create expression parser
            Expression expr = new Expression(expression);

            // Add variables if provided
            for (Map.Entry<String, Double> variable : variables.entrySet()) {
                expr.defineArgument(variable.getKey(), variable.getValue());
            }

            // Calculate the result
            double calculatedResult = expr.calculate();

            // Check for errors
            if (Double.isNaN(calculatedResult)) {
                result.put("error", "Invalid expression or calculation error");
                result.put("errorMessage", expr.getErrorMessage());
                return result;
            }

            // Apply precision and format the result
            decimalFormat.setMaximumFractionDigits(precision);
            String formattedResult = decimalFormat.format(calculatedResult);

            // Format the expression if requested
            String formattedExpression = expression;
            if (formatOutput) {
                formattedExpression = formatMathExpression(expression);
            }

            // Generate image if requested
            String imageBase64 = null;
            String imagePath = null;

            if (generateImage) {
                Map<String, Object> imageResult = generateExpressionImage(expression, calculatedResult);
                imageBase64 = (String) imageResult.get("imageBase64");
                imagePath = (String) imageResult.get("imagePath");
            }

            // Build the result
            result.put("success", true);
            result.put("expression", expression);
            result.put("formatted", formattedExpression);
            result.put("result", calculatedResult);
            result.put("formattedResult", formattedResult);

            if (imageBase64 != null) {
                result.put("imageBase64", imageBase64);
                result.put("imagePath", imagePath);
            }

        } catch (Exception e) {
            result.put("error", "Error evaluating expression: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Format a math expression with proper typography
     *
     * @param expression Mathematical expression
     * @return Formatted expression with Unicode math symbols
     */
    private String formatMathExpression(String expression) {
        String formatted = expression;

        // Replace operators with their symbolic Unicode equivalents
        formatted = formatted.replace("sqrt", "√");
        formatted = formatted.replace("^2", "²");
        formatted = formatted.replace("^3", "³");
        formatted = formatted.replace("^(1/2)", "½");
        formatted = formatted.replace("*", "×");
        formatted = formatted.replace("/", "÷");
        formatted = formatted.replace("<=", "≤");
        formatted = formatted.replace(">=", "≥");
        formatted = formatted.replace("!=", "≠");
        formatted = formatted.replace("pi", "π");

        // Handle powers with regex
        Pattern powerPattern = Pattern.compile("\\^\\((\\d+)/(\\d+)\\)");
        Matcher powerMatcher = powerPattern.matcher(formatted);
        StringBuilder sb = new StringBuilder();
        while (powerMatcher.find()) {
            String numerator = powerMatcher.group(1);
            String denominator = powerMatcher.group(2);
            powerMatcher.appendReplacement(sb, "^(" + numerator + "⁄" + denominator + ")");
        }
        powerMatcher.appendTail(sb);
        formatted = sb.toString();

        // Handle general exponents
        formatted = formatted.replaceAll("\\^\\(([^)]+)\\)", "^($1)");

        // Format trig functions
        formatted = formatted.replace("sin", "sin");
        formatted = formatted.replace("cos", "cos");
        formatted = formatted.replace("tan", "tan");
        formatted = formatted.replace("asin", "sin⁻¹");
        formatted = formatted.replace("acos", "cos⁻¹");
        formatted = formatted.replace("atan", "tan⁻¹");

        return formatted;
    }

    /**
     * Generate an image of the expression and its result
     *
     * @param expression Mathematical expression
     * @param result Calculated result
     * @return Map with image data
     */
    private Map<String, Object> generateExpressionImage(String expression, double result) {
        Map<String, Object> imageResult = new HashMap<>();

        try {
            // Format the expression
            String formattedExpression = formatMathExpression(expression);
            String formattedResult = decimalFormat.format(result);

            // Create a buffered image
            int width = 500;
            int height = 100;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // Set up rendering hints for better text quality
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Fill the background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Draw expression and result
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            FontMetrics fm = g2d.getFontMetrics();

            String equationText = formattedExpression + " = " + formattedResult;
            int textWidth = fm.stringWidth(equationText);
            int x = (width - textWidth) / 2;
            int y = height / 2 + fm.getAscent() / 2;

            g2d.drawString(equationText, x, y);

            // Add a border
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawRect(0, 0, width - 1, height - 1);

            g2d.dispose();

            // Save the image
            String fileName = "math_" + LocalDateTime.now().format(formatter) + ".png";
            String filePath = uploadDir + File.separator + fileName;
            File outputFile = new File(filePath);
            ImageIO.write(image, "png", outputFile);

            // Convert to Base64 for inline display
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            imageResult.put("imageBase64", "data:image/png;base64," + base64Image);
            imageResult.put("imagePath", filePath);

        } catch (Exception e) {
            imageResult.put("error", "Error generating image: " + e.getMessage());
            e.printStackTrace();
        }

        return imageResult;
    }

    /**
     * Get a list of supported mathematical functions
     *
     * @return Map with supported functions information
     */
    private Map<String, Object> getSupportedFunctions() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, String>> functions = new ArrayList<>();

            // Basic operators
            addFunction(functions, "+", "Addition", "a + b");
            addFunction(functions, "-", "Subtraction", "a - b");
            addFunction(functions, "*", "Multiplication", "a * b");
            addFunction(functions, "/", "Division", "a / b");
            addFunction(functions, "^", "Exponentiation", "a^b or a^(n/m)");
            addFunction(functions, "%", "Modulo", "a % b");

            // Trigonometric functions
            addFunction(functions, "sin", "Sine", "sin(x)");
            addFunction(functions, "cos", "Cosine", "cos(x)");
            addFunction(functions, "tan", "Tangent", "tan(x)");
            addFunction(functions, "asin", "Arcsine", "asin(x)");
            addFunction(functions, "acos", "Arccosine", "acos(x)");
            addFunction(functions, "atan", "Arctangent", "atan(x)");

            // Logarithmic functions
            addFunction(functions, "ln", "Natural logarithm", "ln(x)");
            addFunction(functions, "log", "Logarithm base 10", "log(x)");

            // Roots
            addFunction(functions, "sqrt", "Square root", "sqrt(x)");
            addFunction(functions, "cbrt", "Cube root", "cbrt(x)");
            addFunction(functions, "root", "N-th root", "root(n, x)");

            // Other functions
            addFunction(functions, "abs", "Absolute value", "abs(x)");
            addFunction(functions, "floor", "Floor function", "floor(x)");
            addFunction(functions, "ceil", "Ceiling function", "ceil(x)");
            addFunction(functions, "round", "Round to nearest integer", "round(x)");
            addFunction(functions, "sgn", "Signum function", "sgn(x)");
            addFunction(functions, "min", "Minimum", "min(a,b,...)");
            addFunction(functions, "max", "Maximum", "max(a,b,...)");

            // Constants
            addFunction(functions, "pi", "Pi constant", "pi");
            addFunction(functions, "e", "Euler's number", "e");

            // Build the result
            result.put("success", true);
            result.put("functions", functions);

        } catch (Exception e) {
            result.put("error", "Error retrieving functions: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Helper method to add a function to the list
     *
     * @param list List to add to
     * @param name Function name
     * @param description Function description
     * @param example Usage example
     */
    private void addFunction(List<Map<String, String>> list, String name, String description, String example) {
        Map<String, String> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("example", example);
        list.add(function);
    }
}