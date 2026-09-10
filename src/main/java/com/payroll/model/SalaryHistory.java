package com.payroll.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_histories")
public class SalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "previous_gross_salary", precision = 12, scale = 2)
    private BigDecimal previousGrossSalary = BigDecimal.ZERO;

    @Column(name = "new_gross_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal newGrossSalary = BigDecimal.ZERO;

    @Column(name = "previous_basic_salary", precision = 12, scale = 2)
    private BigDecimal previousBasicSalary = BigDecimal.ZERO;

    @Column(name = "new_basic_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal newBasicSalary = BigDecimal.ZERO;

    @Column(name = "new_net_salary", precision = 12, scale = 2)
    private BigDecimal newNetSalary = BigDecimal.ZERO;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate = LocalDate.now();

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SalaryHistory() {}

    public SalaryHistory(Employee employee, BigDecimal previousGrossSalary, BigDecimal newGrossSalary,
                         BigDecimal previousBasicSalary, BigDecimal newBasicSalary, BigDecimal newNetSalary,
                         LocalDate effectiveDate, String reason, String updatedBy) {
        this.employee = employee;
        this.previousGrossSalary = previousGrossSalary != null ? previousGrossSalary : BigDecimal.ZERO;
        this.newGrossSalary = newGrossSalary != null ? newGrossSalary : BigDecimal.ZERO;
        this.previousBasicSalary = previousBasicSalary != null ? previousBasicSalary : BigDecimal.ZERO;
        this.newBasicSalary = newBasicSalary != null ? newBasicSalary : BigDecimal.ZERO;
        this.newNetSalary = newNetSalary != null ? newNetSalary : BigDecimal.ZERO;
        this.effectiveDate = effectiveDate != null ? effectiveDate : LocalDate.now();
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

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
