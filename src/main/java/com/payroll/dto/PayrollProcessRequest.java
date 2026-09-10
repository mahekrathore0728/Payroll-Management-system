package com.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class PayrollProcessRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Invalid year")
    private Integer year;

    @PositiveOrZero(message = "Overtime hours must be positive or zero")
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @PositiveOrZero(message = "Bonus must be positive or zero")
    private BigDecimal additionalBonus = BigDecimal.ZERO;

    @PositiveOrZero(message = "Other deductions must be positive or zero")
    private BigDecimal additionalDeductions = BigDecimal.ZERO;

    private String paymentMethod = "BANK_TRANSFER";

    public PayrollProcessRequest() {}

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getAdditionalBonus() { return additionalBonus; }
    public void setAdditionalBonus(BigDecimal additionalBonus) { this.additionalBonus = additionalBonus; }

    public BigDecimal getAdditionalDeductions() { return additionalDeductions; }
    public void setAdditionalDeductions(BigDecimal additionalDeductions) { this.additionalDeductions = additionalDeductions; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
