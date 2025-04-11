package kostovite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PercentageCalculator implements PluginInterface {

    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#,##0.##'%'");
    private static final String EXCHANGE_RATE_API_URL = "https://open.er-api.com/v6/latest/";
    private static final String INTEREST_RATE_API_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v2/accounting/od/avg_interest_rates";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Double> exchangeRates = new HashMap<>();
    private Map<String, Double> interestRates = new HashMap<>();
    private long lastExchangeRateUpdate = 0;
    private long lastInterestRateUpdate = 0;
    private static final long CACHE_DURATION = 3600000; // 1 hour in milliseconds

    @Override
    public String getName() {
        return "PercentageCalculator";
    }

    @Override
    public void execute() {
        System.out.println("Percentage Calculator Plugin executed");

        // Demonstrate basic usage
        try {
            // Example percentage calculation
            Map<String, Object> params = new HashMap<>();
            params.put("operation", "percentOf");
            params.put("percentage", 15);
            params.put("value", 200);

            Map<String, Object> result = process(params);
            System.out.println("15% of 200 = " + result.get("result"));

            // Example compound interest calculation
            Map<String, Object> compoundParams = new HashMap<>();
            compoundParams.put("operation", "compoundInterest");
            compoundParams.put("principal", 10000);
            compoundParams.put("rate", 5);
            compoundParams.put("years", 5);
            compoundParams.put("compoundingPerYear", 12);

            Map<String, Object> compoundResult = process(compoundParams);
            System.out.println("Compound Interest Result: " + compoundResult.get("formattedFutureValue"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Calculate various percentage and financial calculations");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Basic percentage operations
        definePercentageOperations(operations);

        // Financial calculations
        defineFinancialOperations(operations);

        // Currency operations
        defineCurrencyOperations(operations);

        metadata.put("operations", operations);
        return metadata;
    }

    /**
     * Define basic percentage operations
     *
     * @param operations Operations map to add to
     */
    private void definePercentageOperations(Map<String, Object> operations) {
        // Calculate X% of Y
        Map<String, Object> percentOfOperation = new HashMap<>();
        percentOfOperation.put("description", "Calculate what percentage of a value is");

        Map<String, Object> percentOfInputs = new HashMap<>();
        percentOfInputs.put("percentage", "Percentage value");
        percentOfInputs.put("value", "Base value");

        percentOfOperation.put("inputs", percentOfInputs);
        operations.put("percentOf", percentOfOperation);

        // Calculate what percent X is of Y
        Map<String, Object> percentageOperation = new HashMap<>();
        percentageOperation.put("description", "Calculate what percent one value is of another");

        Map<String, Object> percentageInputs = new HashMap<>();
        percentageInputs.put("value", "The value to find percentage for");
        percentageInputs.put("ofValue", "The base value");

        percentageOperation.put("inputs", percentageInputs);
        operations.put("percentage", percentageOperation);

        // Calculate percentage increase/decrease
        Map<String, Object> percentageChangeOperation = new HashMap<>();
        percentageChangeOperation.put("description", "Calculate percentage increase or decrease");

        Map<String, Object> percentageChangeInputs = new HashMap<>();
        percentageChangeInputs.put("fromValue", "Starting value");
        percentageChangeInputs.put("toValue", "Ending value");

        percentageChangeOperation.put("inputs", percentageChangeInputs);
        operations.put("percentageChange", percentageChangeOperation);
    }

    /**
     * Define financial calculation operations
     *
     * @param operations Operations map to add to
     */
    private void defineFinancialOperations(Map<String, Object> operations) {
        // Compound interest calculation
        Map<String, Object> compoundInterestOperation = new HashMap<>();
        compoundInterestOperation.put("description", "Calculate compound interest");

        Map<String, Object> compoundInterestInputs = new HashMap<>();
        compoundInterestInputs.put("principal", "Initial investment amount");
        compoundInterestInputs.put("rate", "Annual interest rate (percentage)");
        compoundInterestInputs.put("years", "Number of years");
        compoundInterestInputs.put("compoundingPerYear", "Number of times interest is compounded per year (default: 1)");

        compoundInterestOperation.put("inputs", compoundInterestInputs);
        operations.put("compoundInterest", compoundInterestOperation);

        // Simple interest calculation
        Map<String, Object> simpleInterestOperation = new HashMap<>();
        simpleInterestOperation.put("description", "Calculate simple interest");

        Map<String, Object> simpleInterestInputs = new HashMap<>();
        simpleInterestInputs.put("principal", "Initial amount");
        simpleInterestInputs.put("rate", "Annual interest rate (percentage)");
        simpleInterestInputs.put("years", "Number of years");

        simpleInterestOperation.put("inputs", simpleInterestInputs);
        operations.put("simpleInterest", simpleInterestOperation);

        // Loan payment calculation
        Map<String, Object> loanPaymentOperation = new HashMap<>();
        loanPaymentOperation.put("description", "Calculate loan payment amount");

        Map<String, Object> loanPaymentInputs = new HashMap<>();
        loanPaymentInputs.put("principal", "Loan amount");
        loanPaymentInputs.put("rate", "Annual interest rate (percentage)");
        loanPaymentInputs.put("years", "Loan term in years");
        loanPaymentInputs.put("paymentsPerYear", "Number of payments per year (default: 12)");

        loanPaymentOperation.put("inputs", loanPaymentInputs);
        operations.put("loanPayment", loanPaymentOperation);

        // Savings goal calculation
        Map<String, Object> savingsGoalOperation = new HashMap<>();
        savingsGoalOperation.put("description", "Calculate savings needed for a goal");

        Map<String, Object> savingsGoalInputs = new HashMap<>();
        savingsGoalInputs.put("goal", "Target amount");
        savingsGoalInputs.put("rate", "Annual interest rate (percentage)");
        savingsGoalInputs.put("years", "Number of years");
        savingsGoalInputs.put("initialDeposit", "Initial deposit amount (default: 0)");
        savingsGoalInputs.put("compoundingPerYear", "Number of times interest is compounded per year (default: 12)");

        savingsGoalOperation.put("inputs", savingsGoalInputs);
        operations.put("savingsGoal", savingsGoalOperation);

        // Get current interest rates
        Map<String, Object> getCurrentRatesOperation = new HashMap<>();
        getCurrentRatesOperation.put("description", "Get current interest rates");
        operations.put("getCurrentRates", getCurrentRatesOperation);
    }

    /**
     * Define currency-related operations
     *
     * @param operations Operations map to add to
     */
    private void defineCurrencyOperations(Map<String, Object> operations) {
        // Currency conversion
        Map<String, Object> convertCurrencyOperation = new HashMap<>();
        convertCurrencyOperation.put("description", "Convert amount from one currency to another");

        Map<String, Object> convertCurrencyInputs = new HashMap<>();
        convertCurrencyInputs.put("amount", "Amount to convert");
        convertCurrencyInputs.put("fromCurrency", "Source currency code (e.g., USD)");
        convertCurrencyInputs.put("toCurrency", "Target currency code (e.g., EUR)");

        convertCurrencyOperation.put("inputs", convertCurrencyInputs);
        operations.put("convertCurrency", convertCurrencyOperation);

        // Get exchange rates
        Map<String, Object> getExchangeRatesOperation = new HashMap<>();
        getExchangeRatesOperation.put("description", "Get current exchange rates");

        Map<String, Object> getExchangeRatesInputs = new HashMap<>();
        getExchangeRatesInputs.put("baseCurrency", "Base currency code (default: USD)");

        getExchangeRatesOperation.put("inputs", getExchangeRatesInputs);
        operations.put("getExchangeRates", getExchangeRatesOperation);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "percentOf");

            // Basic percentage operations
            if ("percentOf".equals(operation)) {
                return calculatePercentOf(input);
            } else if ("percentage".equals(operation)) {
                return calculatePercentage(input);
            } else if ("percentageChange".equals(operation)) {
                return calculatePercentageChange(input);
            }

            // Financial operations
            else if ("compoundInterest".equals(operation)) {
                return calculateCompoundInterest(input);
            } else if ("simpleInterest".equals(operation)) {
                return calculateSimpleInterest(input);
            } else if ("loanPayment".equals(operation)) {
                return calculateLoanPayment(input);
            } else if ("savingsGoal".equals(operation)) {
                return calculateSavingsGoal(input);
            } else if ("getCurrentRates".equals(operation)) {
                return getCurrentInterestRates();
            }

            // Currency operations
            else if ("convertCurrency".equals(operation)) {
                return convertCurrency(input);
            } else if ("getExchangeRates".equals(operation)) {
                return getExchangeRates(input);
            }

            else {
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
     * Calculate what X% of Y is
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculatePercentOf(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double percentage = parseDouble(input.get("percentage"));
            double value = parseDouble(input.get("value"));

            double calculatedValue = (percentage / 100.0) * value;

            result.put("success", true);
            result.put("percentage", percentage);
            result.put("value", value);
            result.put("result", calculatedValue);
            result.put("formattedResult", DEFAULT_FORMAT.format(calculatedValue));
            result.put("formula", percentage + "% × " + DEFAULT_FORMAT.format(value) +
                    " = " + DEFAULT_FORMAT.format(calculatedValue));

        } catch (Exception e) {
            result.put("error", "Error calculating percentage: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate what percent X is of Y
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculatePercentage(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double value = parseDouble(input.get("value"));
            double ofValue = parseDouble(input.get("ofValue"));

            double percentage = (value / ofValue) * 100.0;

            result.put("success", true);
            result.put("value", value);
            result.put("ofValue", ofValue);
            result.put("result", percentage);
            result.put("formattedResult", PERCENTAGE_FORMAT.format(percentage));
            result.put("formula", DEFAULT_FORMAT.format(value) + " ÷ " +
                    DEFAULT_FORMAT.format(ofValue) + " × 100 = " + PERCENTAGE_FORMAT.format(percentage));

        } catch (Exception e) {
            result.put("error", "Error calculating percentage: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate percentage increase/decrease
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculatePercentageChange(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double fromValue = parseDouble(input.get("fromValue"));
            double toValue = parseDouble(input.get("toValue"));

            double change = toValue - fromValue;
            double percentageChange = (change / Math.abs(fromValue)) * 100.0;

            String changeType = (percentageChange >= 0) ? "increase" : "decrease";

            result.put("success", true);
            result.put("fromValue", fromValue);
            result.put("toValue", toValue);
            result.put("change", change);
            result.put("percentageChange", percentageChange);
            result.put("formattedPercentageChange", PERCENTAGE_FORMAT.format(Math.abs(percentageChange)));
            result.put("changeType", changeType);

            // Create formula and explanation
            String formula = "(" + DEFAULT_FORMAT.format(toValue) + " - " +
                    DEFAULT_FORMAT.format(fromValue) + ") ÷ " + DEFAULT_FORMAT.format(Math.abs(fromValue)) +
                    " × 100 = " + PERCENTAGE_FORMAT.format(percentageChange);

            String explanation = "The value " + (percentageChange >= 0 ? "increased" : "decreased") +
                    " by " + PERCENTAGE_FORMAT.format(Math.abs(percentageChange)) +
                    " from " + DEFAULT_FORMAT.format(fromValue) +
                    " to " + DEFAULT_FORMAT.format(toValue);

            result.put("formula", formula);
            result.put("explanation", explanation);

        } catch (Exception e) {
            result.put("error", "Error calculating percentage change: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate compound interest
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculateCompoundInterest(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double principal = parseDouble(input.get("principal"));
            double rate = parseDouble(input.get("rate"));
            double years = parseDouble(input.get("years"));
            int compoundingPerYear = input.containsKey("compoundingPerYear") ?
                    Integer.parseInt(input.get("compoundingPerYear").toString()) : 1;

            // Handle edge cases
            if (principal <= 0) {
                result.put("error", "Principal must be greater than zero");
                return result;
            }
            if (years <= 0) {
                result.put("error", "Years must be greater than zero");
                return result;
            }
            if (compoundingPerYear <= 0) {
                result.put("error", "Compounding periods must be greater than zero");
                return result;
            }

            // Calculate compound interest: A = P(1 + r/n)^(nt)
            double rateDecimal = rate / 100.0;
            double base = 1 + (rateDecimal / compoundingPerYear);
            double exponent = compoundingPerYear * years;

            double futureValue = principal * Math.pow(base, exponent);
            double interestEarned = futureValue - principal;

            // Prepare the result with detailed breakdown
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            result.put("success", true);
            result.put("principal", principal);
            result.put("rate", rate);
            result.put("rateDecimal", rateDecimal);
            result.put("years", years);
            result.put("compoundingPerYear", compoundingPerYear);
            result.put("totalCompoundingPeriods", exponent);
            result.put("futureValue", futureValue);
            result.put("interestEarned", interestEarned);
            result.put("formattedPrincipal", currencyFormat.format(principal));
            result.put("formattedFutureValue", currencyFormat.format(futureValue));
            result.put("formattedInterestEarned", currencyFormat.format(interestEarned));

            // Generate a year-by-year breakdown
            List<Map<String, Object>> yearlyBreakdown = new ArrayList<>();
            double runningPrincipal = principal;

            for (int year = 1; year <= (int)years; year++) {
                double yearEndValue = principal * Math.pow(base, compoundingPerYear * year);
                double yearInterest = yearEndValue - runningPrincipal;

                Map<String, Object> yearData = new HashMap<>();
                yearData.put("year", year);
                yearData.put("startingBalance", runningPrincipal);
                yearData.put("endingBalance", yearEndValue);
                yearData.put("interestEarned", yearInterest);
                yearData.put("formattedStartingBalance", currencyFormat.format(runningPrincipal));
                yearData.put("formattedEndingBalance", currencyFormat.format(yearEndValue));
                yearData.put("formattedInterestEarned", currencyFormat.format(yearInterest));

                yearlyBreakdown.add(yearData);
                runningPrincipal = yearEndValue;
            }

            result.put("yearlyBreakdown", yearlyBreakdown);

            // Create formula
            String formula = "A = " + currencyFormat.format(principal) + " × (1 + " +
                    (rate / 100.0) + "/" + compoundingPerYear + ")^(" + compoundingPerYear +
                    " × " + years + ")";

            result.put("formula", formula);

        } catch (Exception e) {
            result.put("error", "Error calculating compound interest: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate simple interest
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculateSimpleInterest(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double principal = parseDouble(input.get("principal"));
            double rate = parseDouble(input.get("rate"));
            double years = parseDouble(input.get("years"));

            // Handle edge cases
            if (principal <= 0) {
                result.put("error", "Principal must be greater than zero");
                return result;
            }
            if (years <= 0) {
                result.put("error", "Years must be greater than zero");
                return result;
            }

            // Calculate simple interest: A = P(1 + rt)
            double rateDecimal = rate / 100.0;
            double interestEarned = principal * rateDecimal * years;
            double futureValue = principal + interestEarned;

            // Prepare the result with detailed breakdown
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            result.put("success", true);
            result.put("principal", principal);
            result.put("rate", rate);
            result.put("rateDecimal", rateDecimal);
            result.put("years", years);
            result.put("futureValue", futureValue);
            result.put("interestEarned", interestEarned);
            result.put("formattedPrincipal", currencyFormat.format(principal));
            result.put("formattedFutureValue", currencyFormat.format(futureValue));
            result.put("formattedInterestEarned", currencyFormat.format(interestEarned));

            // Create formula
            String formula = "I = " + currencyFormat.format(principal) + " × " +
                    (rate / 100.0) + " × " + years;

            result.put("formula", formula);

        } catch (Exception e) {
            result.put("error", "Error calculating simple interest: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate loan payment
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculateLoanPayment(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double principal = parseDouble(input.get("principal"));
            double rate = parseDouble(input.get("rate"));
            double years = parseDouble(input.get("years"));
            int paymentsPerYear = input.containsKey("paymentsPerYear") ?
                    Integer.parseInt(input.get("paymentsPerYear").toString()) : 12;

            // Handle edge cases
            if (principal <= 0) {
                result.put("error", "Principal must be greater than zero");
                return result;
            }
            if (years <= 0) {
                result.put("error", "Years must be greater than zero");
                return result;
            }
            if (paymentsPerYear <= 0) {
                result.put("error", "Payments per year must be greater than zero");
                return result;
            }

            // Calculate monthly payment using the formula: M = P[r(1+r)^n]/[(1+r)^n-1]
            double ratePerPeriod = (rate / 100.0) / paymentsPerYear;
            int totalPayments = (int)(years * paymentsPerYear);

            double paymentAmount;
            double totalInterest;

            if (rate == 0) {
                // For zero interest rate
                paymentAmount = principal / totalPayments;
                totalInterest = 0;
            } else {
                double numerator = ratePerPeriod * Math.pow(1 + ratePerPeriod, totalPayments);
                double denominator = Math.pow(1 + ratePerPeriod, totalPayments) - 1;

                paymentAmount = principal * (numerator / denominator);
                totalInterest = (paymentAmount * totalPayments) - principal;
            }

            // Prepare the result with detailed breakdown
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            result.put("success", true);
            result.put("principal", principal);
            result.put("rate", rate);
            result.put("years", years);
            result.put("paymentsPerYear", paymentsPerYear);
            result.put("totalPayments", totalPayments);
            result.put("paymentAmount", paymentAmount);
            result.put("totalInterest", totalInterest);
            result.put("totalCost", principal + totalInterest);
            result.put("formattedPrincipal", currencyFormat.format(principal));
            result.put("formattedPaymentAmount", currencyFormat.format(paymentAmount));
            result.put("formattedTotalInterest", currencyFormat.format(totalInterest));
            result.put("formattedTotalCost", currencyFormat.format(principal + totalInterest));

            // Create amortization schedule
            List<Map<String, Object>> amortizationSchedule = new ArrayList<>();
            double remainingBalance = principal;
            double cumulativeInterest = 0;

            for (int paymentNumber = 1; paymentNumber <= totalPayments; paymentNumber++) {
                double interestPayment = remainingBalance * ratePerPeriod;
                double principalPayment = paymentAmount - interestPayment;

                // Adjustment for the final payment due to rounding
                if (paymentNumber == totalPayments) {
                    principalPayment = remainingBalance;
                    paymentAmount = principalPayment + interestPayment;
                }

                remainingBalance -= principalPayment;
                cumulativeInterest += interestPayment;

                // Don't include every single payment to keep the response size reasonable
                if (paymentNumber <= 3 || paymentNumber > totalPayments - 3 ||
                        paymentNumber % (paymentsPerYear * 5) == 0) { // Show every 5 years and first/last 3 payments

                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("paymentNumber", paymentNumber);
                    paymentData.put("paymentAmount", paymentAmount);
                    paymentData.put("principalPayment", principalPayment);
                    paymentData.put("interestPayment", interestPayment);
                    paymentData.put("remainingBalance", Math.max(0, remainingBalance));
                    paymentData.put("formattedPaymentAmount", currencyFormat.format(paymentAmount));
                    paymentData.put("formattedPrincipalPayment", currencyFormat.format(principalPayment));
                    paymentData.put("formattedInterestPayment", currencyFormat.format(interestPayment));
                    paymentData.put("formattedRemainingBalance", currencyFormat.format(Math.max(0, remainingBalance)));

                    amortizationSchedule.add(paymentData);
                }
            }

            result.put("amortizationSchedule", amortizationSchedule);

        } catch (Exception e) {
            result.put("error", "Error calculating loan payment: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate savings required for a goal
     *
     * @param input Input parameters
     * @return Calculation result
     */
    private Map<String, Object> calculateSavingsGoal(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double goal = parseDouble(input.get("goal"));
            double rate = parseDouble(input.get("rate"));
            double years = parseDouble(input.get("years"));
            double initialDeposit = input.containsKey("initialDeposit") ?
                    parseDouble(input.get("initialDeposit")) : 0;
            int compoundingPerYear = input.containsKey("compoundingPerYear") ?
                    Integer.parseInt(input.get("compoundingPerYear").toString()) : 12;

            // Handle edge cases
            if (goal <= 0) {
                result.put("error", "Goal amount must be greater than zero");
                return result;
            }
            if (years <= 0) {
                result.put("error", "Years must be greater than zero");
                return result;
            }
            if (compoundingPerYear <= 0) {
                result.put("error", "Compounding periods must be greater than zero");
                return result;
            }

            // Calculate the future value of the initial deposit
            double rateDecimal = rate / 100.0;
            double futureValueOfInitialDeposit = initialDeposit *
                    Math.pow(1 + (rateDecimal / compoundingPerYear), years * compoundingPerYear);

            // Calculate the additional amount needed
            double additionalAmountNeeded = goal - futureValueOfInitialDeposit;

            // If the initial deposit will grow to exceed the goal, no additional savings are needed
            if (additionalAmountNeeded <= 0) {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

                result.put("success", true);
                result.put("goal", goal);
                result.put("initialDeposit", initialDeposit);
                result.put("futureValueOfInitialDeposit", futureValueOfInitialDeposit);
                result.put("additionalSavingsNeeded", 0);
                result.put("monthlyContribution", 0);
                result.put("formattedGoal", currencyFormat.format(goal));
                result.put("formattedInitialDeposit", currencyFormat.format(initialDeposit));
                result.put("formattedFutureValueOfInitialDeposit", currencyFormat.format(futureValueOfInitialDeposit));
                result.put("message", "Your initial deposit will grow to exceed your goal. No additional savings needed!");

                return result;
            }

            // Calculate required monthly contribution
            // Using the formula for future value of annuity: FV = PMT * ((1 + r)^n - 1) / r
            double r = rateDecimal / compoundingPerYear;
            double n = years * compoundingPerYear;
            double monthlyContribution = additionalAmountNeeded / ((Math.pow(1 + r, n) - 1) / r);

            // Prepare the result with detailed breakdown
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            result.put("success", true);
            result.put("goal", goal);
            result.put("rate", rate);
            result.put("years", years);
            result.put("initialDeposit", initialDeposit);
            result.put("compoundingPerYear", compoundingPerYear);
            result.put("futureValueOfInitialDeposit", futureValueOfInitialDeposit);
            result.put("additionalAmountNeeded", additionalAmountNeeded);
            result.put("monthlyContribution", monthlyContribution);
            result.put("formattedGoal", currencyFormat.format(goal));
            result.put("formattedInitialDeposit", currencyFormat.format(initialDeposit));
            result.put("formattedFutureValueOfInitialDeposit", currencyFormat.format(futureValueOfInitialDeposit));
            result.put("formattedAdditionalAmountNeeded", currencyFormat.format(additionalAmountNeeded));
            result.put("formattedMonthlyContribution", currencyFormat.format(monthlyContribution));

            // Create explanatory message
            String message = String.format(
                    "To reach your goal of %s in %s years with an initial deposit of %s and %.2f%% interest rate, " +
                            "you need to save %s per month.",
                    currencyFormat.format(goal),
                    (int)years,
                    currencyFormat.format(initialDeposit),
                    rate,
                    currencyFormat.format(monthlyContribution)
            );

            result.put("message", message);

        } catch (Exception e) {
            result.put("error", "Error calculating savings goal: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get current interest rates from the API
     *
     * @return Current interest rates
     */
    private Map<String, Object> getCurrentInterestRates() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if rates are cached and not expired
            if (!interestRates.isEmpty() &&
                    (System.currentTimeMillis() - lastInterestRateUpdate) < CACHE_DURATION) {
                result.put("success", true);
                result.put("interestRates", interestRates);
                result.put("source", "U.S. Treasury");
                result.put("cached", true);
                return result;
            }

            // Connect to the API
            URL url = new URL(INTEREST_RATE_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse the JSON response
            JsonNode jsonNode = objectMapper.readTree(response.toString());
            JsonNode dataNode = jsonNode.get("data");

            // Process the data
            Map<String, Double> rates = new HashMap<>();
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    String securityDesc = item.get("security_desc").asText();
                    double rate = item.get("avg_interest_rate_amt").asDouble();
                    rates.put(securityDesc, rate);
                }
            }

            // Store rates in cache
            interestRates = rates;
            lastInterestRateUpdate = System.currentTimeMillis();

            // Build result
            result.put("success", true);
            result.put("interestRates", rates);
            result.put("source", "U.S. Treasury");
            result.put("timestamp", lastInterestRateUpdate);
            result.put("cached", false);

        } catch (Exception e) {
            // If API fails, return default rates
            Map<String, Double> defaultRates = new HashMap<>();
            defaultRates.put("Savings Account", 0.06);
            defaultRates.put("Certificate of Deposit (1 year)", 0.15);
            defaultRates.put("Certificate of Deposit (5 year)", 0.35);
            defaultRates.put("30-Year Fixed Mortgage", 3.00);
            defaultRates.put("15-Year Fixed Mortgage", 2.30);
            defaultRates.put("Treasury Bill (3 Month)", 0.03);
            defaultRates.put("Treasury Note (5 Year)", 0.35);
            defaultRates.put("Treasury Bond (30 Year)", 1.40);

            result.put("success", true);
            result.put("interestRates", defaultRates);
            result.put("source", "Default values (API unavailable)");
            result.put("error", "Failed to fetch current interest rates: " + e.getMessage());
        }

        return result;
    }

    /**
     * Convert currency from one to another
     *
     * @param input Input parameters
     * @return Conversion result
     */
    private Map<String, Object> convertCurrency(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            double amount = parseDouble(input.get("amount"));
            String fromCurrency = ((String) input.get("fromCurrency")).toUpperCase();
            String toCurrency = ((String) input.get("toCurrency")).toUpperCase();

            // Update exchange rates if needed
            if (exchangeRates.isEmpty() ||
                    (System.currentTimeMillis() - lastExchangeRateUpdate) > CACHE_DURATION) {
                updateExchangeRates("USD"); // Use USD as default base
            }

            // Convert currency
            if (!exchangeRates.containsKey(fromCurrency)) {
                result.put("error", "Currency not found: " + fromCurrency);
                return result;
            }
            if (!exchangeRates.containsKey(toCurrency)) {
                result.put("error", "Currency not found: " + toCurrency);
                return result;
            }

            double fromRate = exchangeRates.get(fromCurrency);
            double toRate = exchangeRates.get(toCurrency);

            // Calculate conversion based on USD rates
            double amountInUSD = amount / fromRate;
            double convertedAmount = amountInUSD * toRate;

            // Format the result
            NumberFormat sourceCurrencyFormat = NumberFormat.getCurrencyInstance();
            sourceCurrencyFormat.setCurrency(java.util.Currency.getInstance(fromCurrency));

            NumberFormat targetCurrencyFormat = NumberFormat.getCurrencyInstance();
            targetCurrencyFormat.setCurrency(java.util.Currency.getInstance(toCurrency));

            result.put("success", true);
            result.put("amount", amount);
            result.put("fromCurrency", fromCurrency);
            result.put("toCurrency", toCurrency);
            result.put("convertedAmount", convertedAmount);
            result.put("exchangeRate", toRate / fromRate);
            result.put("formattedAmount", sourceCurrencyFormat.format(amount));
            result.put("formattedConvertedAmount", targetCurrencyFormat.format(convertedAmount));
            result.put("lastUpdated", lastExchangeRateUpdate);

            // Create formula explanation
            String formula = sourceCurrencyFormat.format(amount) + " × " +
                    DEFAULT_FORMAT.format(toRate / fromRate) + " = " + targetCurrencyFormat.format(convertedAmount);

            result.put("formula", formula);

        } catch (Exception e) {
            result.put("error", "Error converting currency: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get exchange rates for a base currency
     *
     * @param input Input parameters
     * @return Exchange rates
     */
    private Map<String, Object> getExchangeRates(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String baseCurrency = input.containsKey("baseCurrency") ?
                    ((String) input.get("baseCurrency")).toUpperCase() : "USD";

            // Update exchange rates if needed
            if (exchangeRates.isEmpty() ||
                    (System.currentTimeMillis() - lastExchangeRateUpdate) > CACHE_DURATION) {
                updateExchangeRates(baseCurrency);
            }

            // Build result
            result.put("success", true);
            result.put("baseCurrency", baseCurrency);
            result.put("rates", exchangeRates);
            result.put("timestamp", lastExchangeRateUpdate);

        } catch (Exception e) {
            result.put("error", "Error getting exchange rates: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update exchange rates from the API
     *
     * @param baseCurrency Base currency for rates
     */
    private void updateExchangeRates(String baseCurrency) {
        try {
            // Connect to the API
            URL url = new URL(EXCHANGE_RATE_API_URL + baseCurrency);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse the JSON response
            JsonNode jsonNode = objectMapper.readTree(response.toString());
            JsonNode ratesNode = jsonNode.get("rates");

            // Process the rates
            Map<String, Double> rates = new HashMap<>();
            if (ratesNode != null && ratesNode.isObject()) {
                ratesNode.fields().forEachRemaining(entry -> {
                    rates.put(entry.getKey(), entry.getValue().asDouble());
                });
            }

            // Store rates in cache
            exchangeRates = rates;
            lastExchangeRateUpdate = System.currentTimeMillis();

        } catch (Exception e) {
            // If API fails, use some default rates
            Map<String, Double> defaultRates = new HashMap<>();
            defaultRates.put("USD", 1.0);
            defaultRates.put("EUR", 0.85);
            defaultRates.put("GBP", 0.75);
            defaultRates.put("JPY", 110.0);
            defaultRates.put("CAD", 1.25);
            defaultRates.put("AUD", 1.35);
            defaultRates.put("CNY", 6.45);

            exchangeRates = defaultRates;
            lastExchangeRateUpdate = System.currentTimeMillis();

            System.err.println("Error fetching exchange rates: " + e.getMessage());
        }
    }

    /**
     * Parse a double value from an object
     *
     * @param value Object to parse
     * @return Parsed double
     * @throws NumberFormatException If the value cannot be parsed
     */
    private double parseDouble(Object value) {
        if (value == null) {
            throw new NumberFormatException("Value is null");
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Cannot parse value to number: " + value);
            }
        }
    }
}