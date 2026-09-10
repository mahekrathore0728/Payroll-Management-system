package com.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SalaryHistoryDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private BigDecimal previousGrossSalary;
    private BigDecimal newGrossSalary;
    private BigDecimal previousBasicSalary;
    private BigDecimal newBasicSalary;
    private BigDecimal newNetSalary;
    private LocalDate effectiveDate;
    private String reason;
    private String updatedBy;
    private LocalDateTime createdAt;

    public SalaryHistoryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public BigDecimal getPreviousGrossSalary() { return previousGrossSalary; }
    public void setPreviousGrossSalary(BigDecimal previousGrossSalary) { this.previousGrossSalary = previousGrossSalary; }

    public BigDecimal getNewGrossSalary() { return newGrossSalary; }
    public void setNewGrossSalary(BigDecimal newGrossSalary) { this.newGrossSalary = newGrossSalary; }

    public BigDecimal getPreviousBasicSalary() { return previousBasicSalary; }
    public void setPreviousBasicSalary(BigDecimal previousBasicSalary) { this.previousBasicSalary = previousBasicSalary; }

    public BigDecimal getNewBasicSalary() { return newBasicSalary; }
    public void setNewBasicSalary(BigDecimal newBasicSalary) { this.newBasicSalary = newBasicSalary; }

    public BigDecimal getNewNetSalary() { return newNetSalary; }
    public void setNewNetSalary(BigDecimal newNetSalary) { this.newNetSalary = newNetSalary; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
