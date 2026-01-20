package com.example.trackexpense.utils;

/**
 * Utility class for formatting currency amounts.
 * BDT: No decimals (whole numbers)
 * Other currencies (USD, EUR, GBP, INR): 2 decimal places
 */
public class CurrencyFormatter {

    /**
     * Format amount with currency symbol based on currency type.
     * 
     * @param amount   The amount to format
     * @param currency Currency code (e.g., "BDT", "USD")
     * @param symbol   Currency symbol (e.g., "৳", "$")
     * @return Formatted string with symbol
     */
    public static String format(double amount, String currency, String symbol) {
        if ("BDT".equals(currency)) {
            return symbol + String.format("%,.0f", amount);
        } else {
            return symbol + String.format("%,.2f", amount);
        }
    }

    /**
     * Format amount with sign (+ or -) and currency symbol.
     * 
     * @param amount   The amount to format (should be positive)
     * @param currency Currency code
     * @param symbol   Currency symbol
     * @param isIncome true for income (+), false for expense (-)
     * @return Formatted string with sign and symbol
     */
    public static String formatWithSign(double amount, String currency, String symbol, boolean isIncome) {
        String sign = isIncome ? "+" : "-";
        if ("BDT".equals(currency)) {
            return String.format("%s%s%,.0f", sign, symbol, amount);
        } else {
            return String.format("%s%s%,.2f", sign, symbol, amount);
        }
    }

    /**
     * Format amount without currency symbol.
     * 
     * @param amount   The amount to format
     * @param currency Currency code
     * @return Formatted string without symbol
     */
    public static String formatValue(double amount, String currency) {
        if ("BDT".equals(currency)) {
            return String.format("%,.0f", amount);
        } else {
            return String.format("%,.2f", amount);
        }
    }

    /**
     * Get the format pattern for the given currency.
     * 
     * @param currency Currency code
     * @return Format pattern string
     */
    public static String getFormatPattern(String currency) {
        if ("BDT".equals(currency)) {
            return "%,.0f";
        } else {
            return "%,.2f";
        }
    }
}
