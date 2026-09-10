package com.payroll;

import com.payroll.model.SalaryStructure;
import com.payroll.utils.NumberToWordsConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayrollCalculationTests {

    @Test
    public void testSalaryCalculation() {
        SalaryStructure salary = new SalaryStructure();
        salary.setBasicSalary(new BigDecimal("45000.00"));
        salary.setHra(new BigDecimal("18000.00"));
        salary.setDa(new BigDecimal("4500.00"));
        salary.setBonus(new BigDecimal("3000.00"));
        salary.setOtherAllowances(new BigDecimal("2000.00"));
        salary.setPf(new BigDecimal("5400.00"));
        salary.setTax(new BigDecimal("3500.00"));
        salary.setOtherDeductions(new BigDecimal("500.00"));

        salary.calculateTotals();

        // Gross = 45000 + 18000 + 4500 + 3000 + 2000 = 72500.00
        assertEquals(new BigDecimal("72500.00"), salary.getGrossSalary());

        // Deductions = 5400 + 3500 + 500 = 9400.00
        assertEquals(new BigDecimal("9400.00"), salary.getTotalDeductions());

        // Net = 72500 - 9400 = 63100.00
        assertEquals(new BigDecimal("63100.00"), salary.getNetSalary());
    }

    @Test
    public void testNumberToWordsConverter() {
        String words = NumberToWordsConverter.convert(new BigDecimal("63100.00"));
        assertEquals("Sixty Three Thousand One Hundred Only", words);

        String wordsSmall = NumberToWordsConverter.convert(new BigDecimal("1500.00"));
        assertEquals("One Thousand Five Hundred Only", wordsSmall);
    }
}