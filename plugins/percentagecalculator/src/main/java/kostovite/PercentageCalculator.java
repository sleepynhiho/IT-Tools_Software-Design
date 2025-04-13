package kostovite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


// Assuming PluginInterface is standard
public class PercentageCalculator implements PluginInterface {

    // Formatting
    private static final DateTimeFormatter INPUT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#,##0.######"); // Increased precision default
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#,##0.##'%'");
    private static final Locale DEFAULT_LOCALE = Locale.US;

    // APIs and Caching
    private static final String EXCHANGE_RATE_API_URL = "https://open.er-api.com/v6/latest/";
    // private static final String INTEREST_RATE_API_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v2/accounting/od/avg_interest_rates?sort=-record_date&limit=1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Double> exchangeRatesCache = new ConcurrentHashMap<>();
    private final Map<String, Double> interestRatesCache = new ConcurrentHashMap<>();
    private volatile long lastExchangeRateUpdate = 0;
    private volatile long lastInterestRateUpdate = 0;
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour

    /**
     * Internal name, should match the class for routing.
     */
    @Override
    public String getName() {
        return "PercentageCalculator";
    }

    /**
     * Standalone execution for testing.
     */
    @Override
    public void execute() {
        System.out.println("Percentage Calculator Plugin executed (standalone test)");
        try {
            // Example percentage calculation
            Map<String, Object> params = new HashMap<>();
            params.put("uiOperation", "percentOf");
            params.put("percentage", 15.0);
            params.put("value", 200.0);
            Map<String, Object> result = process(params);
            System.out.println("percentOf Result: " + result);

            // Example compound interest calculation
            params.clear();
            params.put("uiOperation", "compoundInterest");
            params.put("principal", 10000.0);
            params.put("rate", 5.0); // 5%
            params.put("years", 5.0);
            params.put("compoundingPerYear", 12); // Monthly
            result = process(params);
            System.out.println("compoundInterest Result: " + result);

            // Example currency conversion
            params.clear();
            params.put("uiOperation", "convertCurrency");
            params.put("amount", 100.0);
            params.put("fromCurrency", "USD");
            params.put("toCurrency", "GBP");
            result = process(params);
            System.out.println("convertCurrency Result: " + result);

            // Example get rates
            params.clear();
            params.put("uiOperation", "getExchangeRates");
            params.put("baseCurrency", "EUR");
            result = process(params);
            System.out.println("getExchangeRates Result (Base EUR): " + result);


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
        metadata.put("id", "PercentageCalculator");
        metadata.put("name", "Financial & Percentage Calculator"); // Updated Name
        metadata.put("description", "Perform various percentage, financial (interest, loans, savings), and currency calculations.");
        metadata.put("icon", "Calculate"); // Changed icon
        metadata.put("category", "Finance");
        metadata.put("customUI", false);
        metadata.put("triggerUpdateOnChange", false); // Requires manual submit

        // --- Sections ---
        List<Map<String, Object>> sections = new ArrayList<>();

        // --- Section 1: Operation Selection ---
        Map<String, Object> operationSection = new HashMap<>();
        operationSection.put("id", "operationSelection");
        operationSection.put("label", "Select Calculation Type");

        List<Map<String, Object>> operationInputs = new ArrayList<>();
        operationInputs.add(Map.ofEntries(
                Map.entry("id", "uiOperation"),
                Map.entry("label", "Calculation Type:"),
                Map.entry("type", "select"),
                Map.entry("options", List.of( // Keep all operations
                        Map.of("value", "percentOf", "label", "X% of Y"),
                        Map.of("value", "percentage", "label", "X is What % of Y?"),
                        Map.of("value", "percentageChange", "label", "Percentage Change"),
                        Map.of("value", "compoundInterest", "label", "Compound Interest"),
                        Map.of("value", "simpleInterest", "label", "Simple Interest"),
                        Map.of("value", "loanPayment", "label", "Loan Payment (Mortgage)"),
                        Map.of("value", "savingsGoal", "label", "Savings Goal Contribution"),
                        Map.of("value", "convertCurrency", "label", "Currency Conversion"),
                        Map.of("value", "getExchangeRates", "label", "Get Exchange Rates"),
                        Map.of("value", "getCurrentRates", "label", "Get US Interest Rates (Approx)")
                )),
                Map.entry("default", "percentOf"),
                Map.entry("required", true)
        ));
        operationSection.put("inputs", operationInputs);
        sections.add(operationSection);

        // --- Section 2: Input Parameters (Conditional Fields) ---
        Map<String, Object> paramsSection = new HashMap<>();
        paramsSection.put("id", "parameters");
        paramsSection.put("label", "Input Parameters");
        // Conditional visibility based on operation selection

        List<Map<String, Object>> paramInputs = new ArrayList<>();

        // == Inputs for Basic Percentage ==
        paramInputs.add(createInputField("percentage", "Percentage (%):", "number", "uiOperation === 'percentOf'", 10.0, true));
        paramInputs.add(createInputField("value", "Value (X or Y):", "number", "uiOperation === 'percentOf' || uiOperation === 'percentage'", 100.0, true));
        paramInputs.add(createInputField("ofValue", "Of Value (Y):", "number", "uiOperation === 'percentage'", 100.0, true));
        paramInputs.add(createInputField("fromValue", "From Value:", "number", "uiOperation === 'percentageChange'", 100.0, true));
        paramInputs.add(createInputField("toValue", "To Value:", "number", "uiOperation === 'percentageChange'", 120.0, true));

        // == Inputs for Financial Calcs ==
        String financialCondition = "uiOperation === 'compoundInterest' || uiOperation === 'simpleInterest' || uiOperation === 'loanPayment'";
        paramInputs.add(createInputField("principal", "Principal Amount:", "number", financialCondition, 1000.0, true));
        paramInputs.add(createInputField("rate", "Annual Rate (%):", "number", financialCondition + " || uiOperation === 'savingsGoal'", 5.0, true));
        paramInputs.add(createInputField("years", "Years:", "number", financialCondition + " || uiOperation === 'savingsGoal'", 10.0, true));

        // Compounds per Year (Compound Interest, Savings Goal)
        Map<String, Object> compoundingField = createInputField("compoundingPerYear", "Compounds per Year:", "number", "uiOperation === 'compoundInterest' || uiOperation === 'savingsGoal'", 12, false);
        compoundingField.put("helperText", "e.g., 1=Annual, 4=Quarterly, 12=Monthly");
        compoundingField.put("min", 1);
        paramInputs.add(compoundingField);

        // Payments per Year (Loan Payment)
        Map<String, Object> paymentsField = createInputField("paymentsPerYear", "Payments per Year:", "number", "uiOperation === 'loanPayment'", 12, false);
        paymentsField.put("helperText", "e.g., 12=Monthly, 52=Weekly");
        paymentsField.put("min", 1);
        paramInputs.add(paymentsField);

        // Savings Goal Specific
        paramInputs.add(createInputField("goal", "Savings Goal Amount:", "number", "uiOperation === 'savingsGoal'", 10000.0, true));
        paramInputs.add(createInputField("initialDeposit", "Initial Deposit:", "number", "uiOperation === 'savingsGoal'", 0.0, false));

        // == Inputs for Currency ==
        paramInputs.add(createInputField("amount", "Amount:", "number", "uiOperation === 'convertCurrency'", 100.0, true));
        Map<String, Object> fromCurrencyField = createInputField("fromCurrency", "From Currency:", "text", "uiOperation === 'convertCurrency'", "USD", true);
        fromCurrencyField.put("placeholder", "e.g., USD, EUR, GBP");
        paramInputs.add(fromCurrencyField);
        Map<String, Object> toCurrencyField = createInputField("toCurrency", "To Currency:", "text", "uiOperation === 'convertCurrency'", "EUR", true);
        toCurrencyField.put("placeholder", "e.g., USD, EUR, GBP");
        paramInputs.add(toCurrencyField);
        Map<String, Object> baseCurrencyField = createInputField("baseCurrency", "Base Currency:", "text", "uiOperation === 'getExchangeRates'", "USD", false);
        baseCurrencyField.put("placeholder", "e.g., USD, EUR, GBP");
        paramInputs.add(baseCurrencyField);


        paramsSection.put("inputs", paramInputs);
        sections.add(paramsSection);


        // --- Section 3: Results ---
        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Calculation Results");
        resultsSection.put("condition", "success === true"); // Show only on success

        List<Map<String, Object>> resultOutputs = new ArrayList<>();

        // Basic Percentage Results
        resultOutputs.add(createOutputField("basic_result", "Result", "text", "uiOperation === 'percentOf' || uiOperation === 'percentage'"));
        resultOutputs.add(createOutputField("change_percent", "Change (%)", "text", "uiOperation === 'percentageChange'"));
        resultOutputs.add(createOutputField("change_type", "Type", "text", "uiOperation === 'percentageChange'"));
        resultOutputs.add(createOutputField("change_explanation", "Explanation", "text", "uiOperation === 'percentageChange' && typeof change_explanation !== 'undefined'"));

        // Financial Results
        resultOutputs.add(createOutputField("fin_futureValue", "Future Value", "text", "(uiOperation === 'compoundInterest' || uiOperation === 'simpleInterest') && typeof fin_futureValue !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_interestEarned", "Total Interest Earned", "text", "(uiOperation === 'compoundInterest' || uiOperation === 'simpleInterest') && typeof fin_interestEarned !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_paymentAmount", "Periodic Payment", "text", "uiOperation === 'loanPayment' && typeof fin_paymentAmount !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_totalInterest", "Total Interest Paid", "text", "uiOperation === 'loanPayment' && typeof fin_totalInterest !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_totalCost", "Total Loan Cost", "text", "uiOperation === 'loanPayment' && typeof fin_totalCost !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_monthlyContribution", "Required Periodic Savings", "text", "uiOperation === 'savingsGoal' && typeof fin_monthlyContribution !== 'undefined'"));
        resultOutputs.add(createOutputField("fin_message", "Summary", "text", "uiOperation === 'savingsGoal' && typeof fin_message !== 'undefined'"));

        // Currency Results
        resultOutputs.add(createOutputField("curr_convertedAmount", "Converted Amount", "text", "uiOperation === 'convertCurrency' && typeof curr_convertedAmount !== 'undefined'"));
        resultOutputs.add(createOutputField("curr_exchangeRate", "Effective Rate", "text", "uiOperation === 'convertCurrency' && typeof curr_exchangeRate !== 'undefined'"));

        // Formula (Common)
        Map<String, Object> formulaField = createOutputField("formula", "Formula/Method", "text", "typeof formula !== 'undefined'");
        resultOutputs.add(formulaField);

        // Rate Source Info (Common for API calls)
        Map<String, Object> sourceField = createOutputField("source", "Data Source", "text", "(uiOperation === 'getExchangeRates' || uiOperation === 'getCurrentRates' || uiOperation === 'convertCurrency') && typeof source !== 'undefined'");
        resultOutputs.add(sourceField);


        // Exchange Rates Table (Output as JSON)
        Map<String, Object> exchangeRatesJson = createOutputField("api_exchangeRates", "Exchange Rates", "json", "uiOperation === 'getExchangeRates' && typeof api_exchangeRates !== 'undefined'");
        resultOutputs.add(createOutputField("api_baseCurrency", "Base Currency", "text", "uiOperation === 'getExchangeRates' && typeof api_baseCurrency !== 'undefined'"));
        resultOutputs.add(exchangeRatesJson);

        // Interest Rates Table (Output as JSON)
        resultOutputs.add(createOutputField("api_interestRates", "Interest Rates (Approx %)", "json", "uiOperation === 'getCurrentRates' && typeof api_interestRates !== 'undefined'"));


        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);


        // --- Section 4: Detailed Tables (e.g., Amortization) ---
        Map<String, Object> tablesSection = new HashMap<>();
        tablesSection.put("id", "tables");
        tablesSection.put("label", "Details / Schedule");
        tablesSection.put("condition", "success === true && (typeof fin_yearlyBreakdown !== 'undefined' || typeof fin_amortizationSchedule !== 'undefined')"); // Show if table data exists

        List<Map<String, Object>> tableOutputs = new ArrayList<>();

        Map<String, Object> yearlyBreakdownTable = createOutputField("fin_yearlyBreakdown", "Compound Interest Breakdown", "table", "uiOperation === 'compoundInterest' && typeof fin_yearlyBreakdown !== 'undefined'");
        yearlyBreakdownTable.put("columns", List.of(
                Map.of("header", "Year", "field", "year"),
                Map.of("header", "Start Balance", "field", "formattedStartingBalance"),
                Map.of("header", "Interest", "field", "formattedInterestEarned"),
                Map.of("header", "End Balance", "field", "formattedEndingBalance")
        ));
        tableOutputs.add(yearlyBreakdownTable);

        Map<String, Object> amortizationTable = createOutputField("fin_amortizationSchedule", "Loan Amortization (Sample)", "table", "uiOperation === 'loanPayment' && typeof fin_amortizationSchedule !== 'undefined'");
        amortizationTable.put("columns", List.of(
                Map.of("header", "Pmt #", "field", "paymentNumber"),
                Map.of("header", "Payment", "field", "formattedPaymentAmount"),
                Map.of("header", "Principal", "field", "formattedPrincipalPayment"),
                Map.of("header", "Interest", "field", "formattedInterestPayment"),
                Map.of("header", "Balance", "field", "formattedRemainingBalance")
        ));
        tableOutputs.add(amortizationTable);


        tablesSection.put("outputs", tableOutputs);
        sections.add(tablesSection);


        // --- Section 5: Error Display ---
        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null)); // style handled by helper
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);


        metadata.put("sections", sections);
        return metadata;
    }

    // Helper to create input field definitions
    private Map<String, Object> createInputField(String id, String label, String type, String condition, Object defaultValue, boolean required) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        field.put("label", label);
        field.put("type", type);
        if (required) {
            field.put("required", true);
        }
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (defaultValue != null) {
            field.put("default", defaultValue);
        }
        if ("number".equals(type)) {
            field.put("min", 0); // Default min for many financial inputs
        }
        return field;
    }

    // Helper to create output field definitions
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
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error");
        }
        if ("text".equals(type) && (id.toLowerCase().contains("formula") || id.toLowerCase().contains("rate") || id.toLowerCase().contains("value") || id.toLowerCase().contains("amount") || id.toLowerCase().contains("cost"))) {
            field.put("monospace", true);
        }
        if ("json".equals(type)) {
            field.put("buttons", List.of("copy"));
        }
        return field;
    }

    /**
     * Processes the input parameters (using IDs from the new format)
     * to perform various calculations.
     */
    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String uiOperation = getStringParam(input, "uiOperation", null); // Operation is required
        String errorOutputId = "errorMessage";

        Map<String, Object> processingInput = new HashMap<>(input); // Copy for modification

        try {
            Map<String, Object> result;
            // Route based on the selected UI operation
            switch (uiOperation.toLowerCase()) {
                case "percentof" -> result = calculatePercentOf(processingInput);
                case "percentage" -> result = calculatePercentage(processingInput);
                case "percentagechange" -> result = calculatePercentageChange(processingInput);
                case "compoundinterest" -> result = calculateCompoundInterest(processingInput);
                case "simpleinterest" -> result = calculateSimpleInterest(processingInput);
                case "loanpayment" -> result = calculateLoanPayment(processingInput);
                case "savingsgoal" -> result = calculateSavingsGoal(processingInput);
                case "getcurrentrates" -> result = getCurrentInterestRates();
                case "convertcurrency" -> result = convertCurrency(processingInput);
                case "getexchangerates" -> result = getExchangeRates(processingInput);
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
                return result;
            }

        } catch (IllegalArgumentException e) { // Catch validation errors
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("Error processing percentage request: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, errorOutputId, "Unexpected error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Private Calculation Methods (Updated for new IDs)
    // ========================================================================

    private Map<String, Object> calculatePercentOf(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double percentage = parseDoubleParam(input, "percentage", null); // Required
            double value = parseDoubleParam(input, "value", null); // Required

            double calculatedValue = (percentage / 100.0) * value;

            result.put("success", true);
            // Keys match output IDs
            result.put("basic_result", DEFAULT_FORMAT.format(calculatedValue));
            result.put("formula", String.format(Locale.US, "%.2f%% × %,.2f = %,.2f", percentage, value, calculatedValue));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculatePercentage(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double value = parseDoubleParam(input, "value", null); // Required
            double ofValue = parseDoubleParam(input, "ofValue", null); // Required

            if (ofValue == 0) throw new IllegalArgumentException("Cannot calculate percentage 'of' zero.");

            double percentage = (value / ofValue) * 100.0;

            result.put("success", true);
            result.put("basic_result", PERCENTAGE_FORMAT.format(percentage));
            result.put("formula", String.format(Locale.US, "(%,.2f ÷ %,.2f) × 100 = %,.2f%%", value, ofValue, percentage));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculatePercentageChange(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double fromValue = parseDoubleParam(input, "fromValue", null); // Required
            double toValue = parseDoubleParam(input, "toValue", null); // Required

            if (fromValue == 0) {
                if (toValue == 0) {
                    result.put("success", true);
                    result.put("change_percent", PERCENTAGE_FORMAT.format(0.0));
                    result.put("change_type", "no change");
                    result.put("change_explanation", "Value remained at zero.");
                    result.put("formula", "N/A (no change)");
                } else {
                    result.put("success", true);
                    result.put("change_percent", "Infinite / Undefined");
                    result.put("change_type", toValue > 0 ? "increase" : "decrease");
                    result.put("change_explanation", "Percentage change from zero is undefined.");
                    result.put("formula", "N/A (change from zero)");
                }
                return result;
            }

            double change = toValue - fromValue;
            double percentageChange = (change / Math.abs(fromValue)) * 100.0;
            String changeType = (percentageChange >= 0) ? "increase" : "decrease";

            result.put("success", true);
            result.put("change_percent", PERCENTAGE_FORMAT.format(Math.abs(percentageChange)));
            result.put("change_type", changeType);
            result.put("formula", String.format(Locale.US, "((%,.2f - %,.2f) ÷ |%,.2f|) × 100 = %,.2f%%", toValue, fromValue, fromValue, percentageChange));
            result.put("change_explanation", String.format(Locale.US, "Value %s by %,.2f%% (%,.2f to %,.2f)", (change >= 0 ? "increased" : "decreased"), Math.abs(percentageChange), fromValue, toValue));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculateCompoundInterest(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double principal = parseDoubleParam(input, "principal", null); // Required
            double rate = parseDoubleParam(input, "rate", null); // Required
            double years = parseDoubleParam(input, "years", null); // Required
            int compoundingPerYear = parseIntParam(input, "compoundingPerYear", 1); // Optional

            if (principal <= 0) throw new IllegalArgumentException("Principal must be positive.");
            if (years <= 0) throw new IllegalArgumentException("Years must be positive.");
            if (compoundingPerYear <= 0) throw new IllegalArgumentException("Compounds per year must be positive.");

            double rateDecimal = rate / 100.0;
            double nt = (double) compoundingPerYear * years;

            double futureValue = principal * Math.pow(1 + (rateDecimal / (double) compoundingPerYear), nt);
            double interestEarned = futureValue - principal;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(DEFAULT_LOCALE);

            result.put("success", true);
            result.put("fin_futureValue", currencyFormat.format(futureValue));
            result.put("fin_interestEarned", currencyFormat.format(interestEarned));

            // Generate yearly breakdown
            List<Map<String, Object>> yearlyBreakdown = new ArrayList<>();
            double runningBalance = principal;
            for (int year = 1; year <= (int)Math.ceil(years); year++) {
                double endBalance = principal * Math.pow(1 + (rateDecimal / (double) compoundingPerYear), (double) compoundingPerYear * year);
                double yearInterest = endBalance - runningBalance;
                yearlyBreakdown.add(Map.of(
                        "year", year,
                        "formattedStartingBalance", currencyFormat.format(runningBalance),
                        "formattedInterestEarned", currencyFormat.format(yearInterest),
                        "formattedEndingBalance", currencyFormat.format(endBalance)
                ));
                runningBalance = endBalance;
            }
            result.put("fin_yearlyBreakdown", yearlyBreakdown); // Key matches output ID

            result.put("formula", String.format(Locale.US, "FV = P(1 + r/n)^(nt) = %,.2f(1 + %.4f/%d)^(%d×%.2f)", principal, rateDecimal, compoundingPerYear, compoundingPerYear, years));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculateSimpleInterest(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double principal = parseDoubleParam(input, "principal", null);
            double rate = parseDoubleParam(input, "rate", null);
            double years = parseDoubleParam(input, "years", null);

            if (principal <= 0) throw new IllegalArgumentException("Principal must be positive.");
            if (years <= 0) throw new IllegalArgumentException("Years must be positive.");

            double rateDecimal = rate / 100.0;
            double interestEarned = principal * rateDecimal * years;
            double futureValue = principal + interestEarned;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(DEFAULT_LOCALE);

            result.put("success", true);
            result.put("fin_futureValue", currencyFormat.format(futureValue));
            result.put("fin_interestEarned", currencyFormat.format(interestEarned));
            result.put("formula", String.format(Locale.US, "Interest = P×r×t = %,.2f × %.4f × %.2f", principal, rateDecimal, years));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculateLoanPayment(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double principal = parseDoubleParam(input, "principal", null);
            double rate = parseDoubleParam(input, "rate", null);
            double years = parseDoubleParam(input, "years", null);
            int paymentsPerYear = parseIntParam(input, "paymentsPerYear", 12);

            if (principal <= 0) throw new IllegalArgumentException("Principal must be positive.");
            if (years <= 0) throw new IllegalArgumentException("Years must be positive.");
            if (paymentsPerYear <= 0) throw new IllegalArgumentException("Payments per year must be positive.");

            double ratePerPeriod = (rate / 100.0) / paymentsPerYear;
            double totalPayments = years * paymentsPerYear;

            double paymentAmount;
            double totalInterest;
            double totalCost;

            if (ratePerPeriod == 0) { // Handle 0%
                paymentAmount = principal / totalPayments;
                totalInterest = 0;
            } else {
                double factor = Math.pow(1 + ratePerPeriod, totalPayments);
                paymentAmount = principal * (ratePerPeriod * factor) / (factor - 1);
                totalInterest = (paymentAmount * totalPayments) - principal;
            }
            totalCost = principal + totalInterest;

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(DEFAULT_LOCALE);

            result.put("success", true);
            result.put("fin_paymentAmount", currencyFormat.format(paymentAmount));
            result.put("fin_totalInterest", currencyFormat.format(totalInterest));
            result.put("fin_totalCost", currencyFormat.format(totalCost));

            // Generate amortization schedule (simplified sample)
            List<Map<String, Object>> amortizationSchedule = new ArrayList<>();
            double remainingBalance = principal;
            int numTotalPayments = (int)Math.round(totalPayments);

            for (int pmtNum = 1; pmtNum <= numTotalPayments; pmtNum++) {
                double interestForPeriod = remainingBalance * ratePerPeriod;
                double principalForPeriod = paymentAmount - interestForPeriod;
                if (pmtNum == numTotalPayments && Math.abs(remainingBalance - principalForPeriod) > 0.01) {
                    principalForPeriod = remainingBalance;
                    paymentAmount = principalForPeriod + interestForPeriod; // Adjust final payment
                }
                remainingBalance -= principalForPeriod;
                if (remainingBalance < 0.005) remainingBalance = 0.0; // Prevent negative due to rounding

                if (pmtNum <= 3 || pmtNum > numTotalPayments - 3 || pmtNum <= numTotalPayments - 3 && pmtNum % paymentsPerYear == 0) { // First 3, Last 3, End of Year
                    amortizationSchedule.add(Map.of(
                            "paymentNumber", pmtNum,
                            "formattedPaymentAmount", currencyFormat.format(paymentAmount),
                            "formattedPrincipalPayment", currencyFormat.format(principalForPeriod),
                            "formattedInterestPayment", currencyFormat.format(interestForPeriod),
                            "formattedRemainingBalance", currencyFormat.format(remainingBalance)
                    ));
                }
            }
            result.put("fin_amortizationSchedule", amortizationSchedule);

            result.put("formula", "PMT = P [r(1+r)^n] / [(1+r)^n - 1]"); // Standard formula notation

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> calculateSavingsGoal(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double goal = parseDoubleParam(input, "goal", null);
            double rate = parseDoubleParam(input, "rate", null);
            double years = parseDoubleParam(input, "years", null);
            double initialDeposit = parseDoubleParam(input, "initialDeposit", 0.0);
            int compoundingPerYear = parseIntParam(input, "compoundingPerYear", 12);

            if (goal <= 0) throw new IllegalArgumentException("Goal amount must be positive.");
            if (years <= 0) throw new IllegalArgumentException("Years must be positive.");
            if (compoundingPerYear <= 0) throw new IllegalArgumentException("Compounds per year must be positive.");
            if (initialDeposit < 0) throw new IllegalArgumentException("Initial deposit cannot be negative.");

            double rateDecimal = rate / 100.0;
            double r = rateDecimal / compoundingPerYear; // Rate per period
            double n = years * compoundingPerYear; // Total periods

            double fvInitial = initialDeposit * Math.pow(1 + r, n);
            double remainingGoal = goal - fvInitial;
            double periodicContribution; // Contribution per compounding period
            String message;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(DEFAULT_LOCALE);

            if (remainingGoal <= 0) {
                periodicContribution = 0;
                message = String.format(Locale.US, "Initial deposit (%s) will reach goal. No extra savings needed.", currencyFormat.format(initialDeposit));
            } else {
                if (r == 0) { periodicContribution = remainingGoal / n; }
                else { periodicContribution = remainingGoal * (r / (Math.pow(1 + r, n) - 1)); }
                message = String.format(Locale.US, "Save ~%s per period (%d times/year) to reach goal.", currencyFormat.format(periodicContribution), compoundingPerYear);
            }

            result.put("success", true);
            // Key matches output ID (assuming "monthly" label is ok for any period)
            result.put("fin_monthlyContribution", currencyFormat.format(periodicContribution));
            result.put("fin_message", message); // Key matches output ID
            result.put("formula", "PMT = FV * [r / ((1+r)^n - 1)]"); // Annuity formula

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) { result.put("success", false); result.put(errorOutputId, "Calculation error."); }
        return result;
    }

    private Map<String, Object> getCurrentInterestRates() {
        Map<String, Object> result = new HashMap<>();
        long now = System.currentTimeMillis();

        if (!interestRatesCache.isEmpty() && (now - lastInterestRateUpdate) < CACHE_DURATION_MS) {
            result.put("success", true);
            result.put("api_interestRates", new HashMap<>(interestRatesCache));
            result.put("source", "Approximations (Cached)");
            return result;
        }

        System.out.println("Using default interest rates (API fetch skipped or failed).");
        Map<String, Double> defaultRates = new LinkedHashMap<>();
        defaultRates.put("Savings Account (Approx Avg)", 0.45);
        defaultRates.put("CD - 1 Year (Approx Avg)", 5.00);
        defaultRates.put("CD - 5 Year (Approx Avg)", 4.50);
        defaultRates.put("Mortgage - 30 Year Fixed (Approx Avg)", 7.00);
        defaultRates.put("Treasury Bill - 3 Month (Approx)", 5.25);
        defaultRates.put("Treasury Note - 10 Year (Approx)", 4.50);

        interestRatesCache.clear();
        interestRatesCache.putAll(defaultRates);
        lastInterestRateUpdate = System.currentTimeMillis();

        result.put("success", true);
        result.put("api_interestRates", defaultRates); // Matches output ID
        result.put("source", "Default Approximations"); // Matches output ID
        return result;
    }

    private Map<String, Object> convertCurrency(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        try {
            double amount = parseDoubleParam(input, "amount", null);
            String fromCurrency = getStringParam(input, "fromCurrency", null).toUpperCase();
            String toCurrency = getStringParam(input, "toCurrency", null).toUpperCase();

            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

            updateExchangeRatesIfNeeded(fromCurrency); // Ensure rates are loaded/cached

            if (!exchangeRatesCache.containsKey(fromCurrency)) throw new IllegalArgumentException("Source currency code unavailable: " + fromCurrency);
            if (!exchangeRatesCache.containsKey(toCurrency)) throw new IllegalArgumentException("Target currency code unavailable: " + toCurrency);

            double fromRateToBase = exchangeRatesCache.get(fromCurrency);
            double toRateFromBase = exchangeRatesCache.get(toCurrency);

            double amountInBase = amount / fromRateToBase;
            double convertedAmount = amountInBase * toRateFromBase;
            double effectiveRate = toRateFromBase / fromRateToBase;

            NumberFormat targetFormat = NumberFormat.getCurrencyInstance(DEFAULT_LOCALE);
            try { targetFormat.setCurrency(java.util.Currency.getInstance(toCurrency)); }
            catch (Exception e) { /* Use default locale format */ }

            result.put("success", true);
            result.put("curr_convertedAmount", targetFormat.format(convertedAmount)); // Matches output ID
            result.put("curr_exchangeRate", String.format(Locale.US, "%.6f", effectiveRate)); // Matches output ID
            result.put("source", "Exchange Rate API (possibly cached)"); // Matches output ID
            result.put("formula", String.format(Locale.US, "Amount × Rate = %,.2f %s × %.6f = %s", amount, fromCurrency, effectiveRate, targetFormat.format(convertedAmount)));

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Currency conversion error: " + e.getMessage());
            result.put("success", false); result.put(errorOutputId, "Conversion failed. Check codes/API status.");
        }
        return result;
    }

    private Map<String, Object> getExchangeRates(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        String errorOutputId = "errorMessage";
        String baseCurrency = getStringParam(input, "baseCurrency", "USD").toUpperCase();
        try {
            updateExchangeRatesIfNeeded(baseCurrency); // Update cache using requested base

            result.put("success", true);
            result.put("api_baseCurrency", baseCurrency); // Matches output ID
            result.put("api_exchangeRates", new TreeMap<>(exchangeRatesCache)); // Return sorted copy, matches output ID
            result.put("source", "Exchange Rate API (possibly cached)"); // Matches output ID

        } catch (IllegalArgumentException e) { result.put("success", false); result.put(errorOutputId, e.getMessage());
        } catch (Exception e) {
            System.err.println("Get exchange rates error: " + e.getMessage());
            result.put("success", false); result.put(errorOutputId, "Could not retrieve rates.");
            result.put("api_baseCurrency", baseCurrency);
            result.put("api_exchangeRates", Collections.emptyMap());
        }
        return result;
    }


    // ========================================================================
    // Private Helper Methods (API Calls, Parsing, Formatting)
    // ========================================================================

    private void updateExchangeRatesIfNeeded(String requestedBaseCurrency) {
        long now = System.currentTimeMillis();
        // Fetch vs USD for consistent cache, but allow direct fetch if base is requested
        String fetchBase = "USD";
        // Trigger update if cache is empty, too old, OR if the requested base isn't in the cache
        // (The last condition helps if the initial fetch vs USD failed for some reason)
        boolean needsUpdate = exchangeRatesCache.isEmpty() ||
                (now - lastExchangeRateUpdate) > CACHE_DURATION_MS ||
                !exchangeRatesCache.containsKey(requestedBaseCurrency.toUpperCase());

        if (!needsUpdate) {
            return; // Cache is fresh enough and contains the base
        }

        System.out.println("Updating exchange rates cache (Fetching Base: " + fetchBase + ")");
        try {
            URL url = new URI(EXCHANGE_RATE_API_URL + fetchBase).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int status = connection.getResponseCode();
            if (status == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    JsonNode jsonNode = objectMapper.readTree(reader);
                    JsonNode ratesNode = jsonNode.path("rates"); // Use path for safety
                    if (ratesNode.isObject()) {
                        Map<String, Double> rates = new HashMap<>();
                        ratesNode.fields().forEachRemaining(entry -> rates.put(entry.getKey().toUpperCase(), entry.getValue().asDouble()));
                        if (!rates.containsKey(fetchBase)) { rates.put(fetchBase, 1.0); } // Ensure base is 1.0

                        exchangeRatesCache.clear();
                        exchangeRatesCache.putAll(rates);
                        lastExchangeRateUpdate = System.currentTimeMillis();
                        System.out.println("Exchange rates cache updated. Count: " + rates.size());
                    } else throw new IOException("Invalid 'rates' data in API response.");
                }
            } else throw new IOException("API request failed with status: " + status);
        } catch (Exception e) {
            System.err.println("Error fetching/updating exchange rates: " + e.getMessage());
            // If fetch fails, don't wipe potentially stale but usable cache immediately
            if (exchangeRatesCache.isEmpty()) {
                System.err.println("Cache empty after fetch failure. Loading defaults.");
                loadDefaultExchangeRates(); // Load defaults only if cache is empty
            } else {
                System.err.println("Using potentially stale exchange rate cache due to fetch failure.");
                // Reset timer so we try again sooner next time?
                // lastExchangeRateUpdate = System.currentTimeMillis() - CACHE_DURATION_MS + 60000; // Try again in 1 min
            }
        }
    }

    // Load hardcoded defaults if API fails and cache is empty
    private void loadDefaultExchangeRates() {
        System.err.println("Loading hardcoded default exchange rates.");
        exchangeRatesCache.clear();
        exchangeRatesCache.put("USD", 1.0);
        exchangeRatesCache.put("EUR", 0.92);
        exchangeRatesCache.put("GBP", 0.79);
        exchangeRatesCache.put("JPY", 150.0);
        exchangeRatesCache.put("CAD", 1.35);
        exchangeRatesCache.put("AUD", 1.50);
        // Add a few more common ones
        exchangeRatesCache.put("CHF", 0.88);
        exchangeRatesCache.put("CNY", 7.25);
        exchangeRatesCache.put("INR", 83.0);
        lastExchangeRateUpdate = System.currentTimeMillis();
    }

    // Null default indicates required
    private double parseDoubleParam(Map<String, Object> input, String key, Double defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null || value.toString().trim().isEmpty()) { // Also check for empty string
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required numeric parameter: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            try { return Double.parseDouble(value.toString().replace(',', '.')); }
            catch (NumberFormatException e) {
                if (defaultValue != null) return defaultValue;
                throw new IllegalArgumentException("Invalid numeric value for parameter '" + key + "': " + value);
            }
        }
    }

    // Null default indicates required
    private int parseIntParam(Map<String, Object> input, String key, Integer defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null || value.toString().trim().isEmpty()) { // Also check for empty string
            if (defaultValue != null) return defaultValue;
            throw new IllegalArgumentException("Missing required integer parameter: " + key);
        }
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) {
            double dValue = ((Number) value).doubleValue();
            if (Math.abs(dValue - Math.round(dValue)) < 0.00001) return (int) Math.round(dValue);
            else throw new IllegalArgumentException("Non-integer numeric value for integer parameter '" + key + "': " + value);
        }
        else {
            try {
                double dValue = Double.parseDouble(value.toString().replace(',', '.'));
                if (Math.abs(dValue - Math.round(dValue)) < 0.00001) return (int) Math.round(dValue);
                else throw new IllegalArgumentException("Non-integer string value for integer parameter '" + key + "': " + value);
            } catch (NumberFormatException e) {
                if (defaultValue != null) return defaultValue;
                throw new IllegalArgumentException("Invalid integer value for parameter '" + key + "': " + value);
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
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            if (defaultValue == null) throw new IllegalArgumentException("Missing required parameter: " + key);
            return defaultValue;
        }
        return strValue;
    }

}