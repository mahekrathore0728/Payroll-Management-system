package com.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    // Admin / HR
    private long totalEmployees;
    private long totalDepartments;
    private BigDecimal currentMonthPayroll = BigDecimal.ZERO;
    private BigDecimal totalSalaryPaid = BigDecimal.ZERO;
    private long pendingLeaveRequests;
    private Map<String, Long> attendanceSummary = new HashMap<>();
    private List<String> chartLabels = new ArrayList<>();
    private List<BigDecimal> chartGrossData = new ArrayList<>();
    private List<BigDecimal> chartNetData = new ArrayList<>();
    private List<String> deptChartLabels = new ArrayList<>();
    private List<Long> deptChartData = new ArrayList<>();

    // Smart Alerts
    private long missingSalaryCount;
    private long payrollPendingCurrentMonth;
    private long missingAttendanceToday;

    // Manager
    private long totalTeamMembers;
    private double teamAttendancePercentage;
    private BigDecimal teamPayrollSummary = BigDecimal.ZERO;

    // Employee
    private String employeeName;
    private String designation;
    private String departmentName;
    private BigDecimal currentGrossSalary = BigDecimal.ZERO;
    private BigDecimal currentNetSalary = BigDecimal.ZERO;
    private int remainingLeaves;
    private int casualLeavesRemaining;
    private int sickLeavesRemaining;
    private int annualLeavesRemaining;
    private double employeeAttendancePercentage;
    private long employeePresentDays;
    private long employeeAbsentDays;
    private long employeeLeaveDays;
    private PayrollRecordDto latestPayslip;

    public DashboardStatsDto() {}

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }

    public BigDecimal getCurrentMonthPayroll() { return currentMonthPayroll; }
    public void setCurrentMonthPayroll(BigDecimal currentMonthPayroll) { this.currentMonthPayroll = currentMonthPayroll; }

    public BigDecimal getTotalSalaryPaid() { return totalSalaryPaid; }
    public void setTotalSalaryPaid(BigDecimal totalSalaryPaid) { this.totalSalaryPaid = totalSalaryPaid; }

    public long getPendingLeaveRequests() { return pendingLeaveRequests; }
    public void setPendingLeaveRequests(long pendingLeaveRequests) { this.pendingLeaveRequests = pendingLeaveRequests; }

    public Map<String, Long> getAttendanceSummary() { return attendanceSummary; }
    public void setAttendanceSummary(Map<String, Long> attendanceSummary) { this.attendanceSummary = attendanceSummary; }

    public List<String> getChartLabels() { return chartLabels; }
    public void setChartLabels(List<String> chartLabels) { this.chartLabels = chartLabels; }

    public List<BigDecimal> getChartGrossData() { return chartGrossData; }
    public void setChartGrossData(List<BigDecimal> chartGrossData) { this.chartGrossData = chartGrossData; }

    public List<BigDecimal> getChartNetData() { return chartNetData; }
    public void setChartNetData(List<BigDecimal> chartNetData) { this.chartNetData = chartNetData; }

    public List<String> getDeptChartLabels() { return deptChartLabels; }
    public void setDeptChartLabels(List<String> deptChartLabels) { this.deptChartLabels = deptChartLabels; }

    public List<Long> getDeptChartData() { return deptChartData; }
    public void setDeptChartData(List<Long> deptChartData) { this.deptChartData = deptChartData; }

    public long getTotalTeamMembers() { return totalTeamMembers; }
    public void setTotalTeamMembers(long totalTeamMembers) { this.totalTeamMembers = totalTeamMembers; }

    public double getTeamAttendancePercentage() { return teamAttendancePercentage; }
    public void setTeamAttendancePercentage(double teamAttendancePercentage) { this.teamAttendancePercentage = teamAttendancePercentage; }

    public BigDecimal getTeamPayrollSummary() { return teamPayrollSummary; }
    public void setTeamPayrollSummary(BigDecimal teamPayrollSummary) { this.teamPayrollSummary = teamPayrollSummary; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public BigDecimal getCurrentGrossSalary() { return currentGrossSalary; }
    public void setCurrentGrossSalary(BigDecimal currentGrossSalary) { this.currentGrossSalary = currentGrossSalary; }

    public BigDecimal getCurrentNetSalary() { return currentNetSalary; }
    public void setCurrentNetSalary(BigDecimal currentNetSalary) { this.currentNetSalary = currentNetSalary; }

    public int getRemainingLeaves() { return remainingLeaves; }
    public void setRemainingLeaves(int remainingLeaves) { this.remainingLeaves = remainingLeaves; }

    public int getCasualLeavesRemaining() { return casualLeavesRemaining; }
    public void setCasualLeavesRemaining(int casualLeavesRemaining) { this.casualLeavesRemaining = casualLeavesRemaining; }

    public int getSickLeavesRemaining() { return sickLeavesRemaining; }
    public void setSickLeavesRemaining(int sickLeavesRemaining) { this.sickLeavesRemaining = sickLeavesRemaining; }

    public int getAnnualLeavesRemaining() { return annualLeavesRemaining; }
    public void setAnnualLeavesRemaining(int annualLeavesRemaining) { this.annualLeavesRemaining = annualLeavesRemaining; }

    public double getEmployeeAttendancePercentage() { return employeeAttendancePercentage; }
    public void setEmployeeAttendancePercentage(double employeeAttendancePercentage) { this.employeeAttendancePercentage = employeeAttendancePercentage; }

    public long getEmployeePresentDays() { return employeePresentDays; }
    public void setEmployeePresentDays(long employeePresentDays) { this.employeePresentDays = employeePresentDays; }

    public long getEmployeeAbsentDays() { return employeeAbsentDays; }
    public void setEmployeeAbsentDays(long employeeAbsentDays) { this.employeeAbsentDays = employeeAbsentDays; }

    public long getEmployeeLeaveDays() { return employeeLeaveDays; }
    public void setEmployeeLeaveDays(long employeeLeaveDays) { this.employeeLeaveDays = employeeLeaveDays; }

    public PayrollRecordDto getLatestPayslip() { return latestPayslip; }
    public void setLatestPayslip(PayrollRecordDto latestPayslip) { this.latestPayslip = latestPayslip; }

    public long getMissingSalaryCount() { return missingSalaryCount; }
    public void setMissingSalaryCount(long missingSalaryCount) { this.missingSalaryCount = missingSalaryCount; }

    public long getPayrollPendingCurrentMonth() { return payrollPendingCurrentMonth; }
    public boolean isPayrollPendingCurrentMonth() { return payrollPendingCurrentMonth > 0; }
    public void setPayrollPendingCurrentMonth(long payrollPendingCurrentMonth) { this.payrollPendingCurrentMonth = payrollPendingCurrentMonth; }

    public long getMissingAttendanceToday() { return missingAttendanceToday; }
    public void setMissingAttendanceToday(long missingAttendanceToday) { this.missingAttendanceToday = missingAttendanceToday; }
}
