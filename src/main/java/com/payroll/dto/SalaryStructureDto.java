package com.payroll.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class SalaryStructureDto {
    private Long id;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String designation;

    @NotNull(message = "Basic salary is required")
    @PositiveOrZero(message = "Basic salary must be zero or positive")
    private BigDecimal basicSalary;

    @PositiveOrZero(message = "HRA must be zero or positive")
    private BigDecimal hra = BigDecimal.ZERO;

    @PositiveOrZero(message = "DA must be zero or positive")
    private BigDecimal da = BigDecimal.ZERO;

    @PositiveOrZero(message = "Bonus must be zero or positive")
    private BigDecimal bonus = BigDecimal.ZERO;

    @PositiveOrZero(message = "Overtime rate must be zero or positive")
    private BigDecimal overtimeRate = BigDecimal.ZERO;

    @PositiveOrZero(message = "Other allowances must be zero or positive")
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @PositiveOrZero(message = "PF must be zero or positive")
    private BigDecimal pf = BigDecimal.ZERO;

    @PositiveOrZero(message = "Tax must be zero or positive")
    private BigDecimal tax = BigDecimal.ZERO;

    @PositiveOrZero(message = "Other deductions must be zero or positive")
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;

    public SalaryStructureDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }

    public BigDecimal getDa() { return da; }
    public void setDa(BigDecimal da) { this.da = da; }

    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal overtimeRate) { this.overtimeRate = overtimeRate; }

    public BigDecimal getOtherAllowances() { return otherAllowances; }
    public void setOtherAllowances(BigDecimal otherAllowances) { this.otherAllowances = otherAllowances; }

    public BigDecimal getPf() { return pf; }
    public void setPf(BigDecimal pf) { this.pf = pf; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { this.otherDeductions = otherDeductions; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    private String reason;
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
