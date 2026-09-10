package com.payroll.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_emp_year", columnNames = {"employee_id", "year"})
    }
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private int year;

    @Column(name = "casual_leaves", nullable = false)
    private int casualLeaves = 12;

    @Column(name = "used_casual", nullable = false)
    private int usedCasual = 0;

    @Column(name = "sick_leaves", nullable = false)
    private int sickLeaves = 10;

    @Column(name = "used_sick", nullable = false)
    private int usedSick = 0;

    @Column(name = "annual_leaves", nullable = false)
    private int annualLeaves = 15;

    @Column(name = "used_annual", nullable = false)
    private int usedAnnual = 0;

    public LeaveBalance() {}

    public LeaveBalance(Employee employee, int year) {
        this.employee = employee;
        this.year = year;
        this.casualLeaves = 12;
        this.sickLeaves = 10;
        this.annualLeaves = 15;
    }

    public int getRemainingCasual() { return Math.max(0, casualLeaves - usedCasual); }
    public int getRemainingSick() { return Math.max(0, sickLeaves - usedSick); }
    public int getRemainingAnnual() { return Math.max(0, annualLeaves - usedAnnual); }
    public int getTotalRemaining() { return getRemainingCasual() + getRemainingSick() + getRemainingAnnual(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getCasualLeaves() { return casualLeaves; }
    public void setCasualLeaves(int casualLeaves) { this.casualLeaves = casualLeaves; }

    public int getUsedCasual() { return usedCasual; }
    public void setUsedCasual(int usedCasual) { this.usedCasual = usedCasual; }

    public int getSickLeaves() { return sickLeaves; }
    public void setSickLeaves(int sickLeaves) { this.sickLeaves = sickLeaves; }

    public int getUsedSick() { return usedSick; }
    public void setUsedSick(int usedSick) { this.usedSick = usedSick; }

    public int getAnnualLeaves() { return annualLeaves; }
    public void setAnnualLeaves(int annualLeaves) { this.annualLeaves = annualLeaves; }

    public int getUsedAnnual() { return usedAnnual; }
    public void setUsedAnnual(int usedAnnual) { this.usedAnnual = usedAnnual; }
}
