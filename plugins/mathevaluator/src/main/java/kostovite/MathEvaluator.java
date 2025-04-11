package kostovite;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    private String uploadDir = "math-images";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.##########");

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
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Evaluate and format mathematical expressions");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Evaluate operation
        Map<String, Object> evaluateOperation = new HashMap<>();
        evaluateOperation.put("description", "Evaluate a mathematical expression");

        Map<String, Object> evaluateInputs = new HashMap<>();
        evaluateInputs.put("expression", "Mathematical expression to evaluate");
        evaluateInputs.put("formatOutput", "Whether to format the expression (default: true)");
        evaluateInputs.put("generateImage", "Whether to generate a rendered image (default: true)");
        evaluateInputs.put("precision", "Number of decimal places for result (default: 10)");
        evaluateInputs.put("variables", "Map of variable names to values");

        evaluateOperation.put("inputs", evaluateInputs);
        operations.put("evaluate", evaluateOperation);

        // Get supported functions operation
        Map<String, Object> getFunctionsOperation = new HashMap<>();
        getFunctionsOperation.put("description", "Get list of supported mathematical functions");
        operations.put("getSupportedFunctions", getFunctionsOperation);

        metadata.put("operations", operations);
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
        Pattern powerPattern = Pattern.compile("\\^\\((\\d+)\\/(\\d+)\\)");
        Matcher powerMatcher = powerPattern.matcher(formatted);
        StringBuffer sb = new StringBuffer();
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