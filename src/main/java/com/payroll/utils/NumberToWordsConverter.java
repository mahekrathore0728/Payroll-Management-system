package com.payroll.utils;

import java.math.BigDecimal;

public class NumberToWordsConverter {

    private static final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null) return "Zero Only";
        long number = amount.longValue();
        if (number == 0) return "Zero Only";

        String words = convertNumber(number);
        return words.trim() + " Only";
    }

    private static String convertNumber(long n) {
        if (n < 0) return "Minus " + convertNumber(-n);
        if (n < 20) return units[(int) n];
        if (n < 100) return tens[(int) n / 10] + ((n % 10 != 0) ? " " : "") + units[(int) n % 10];
        if (n < 1000) return units[(int) n / 100] + " Hundred" + ((n % 100 != 0) ? " and " : "") + convertNumber(n % 100);
        if (n < 100000) return convertNumber(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " : "") + convertNumber(n % 1000);
        if (n < 10000000) return convertNumber(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " : "") + convertNumber(n % 100000);
        return convertNumber(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " : "") + convertNumber(n % 10000000);
    }
}
