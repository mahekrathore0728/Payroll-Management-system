package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.DashboardStatsDto;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.model.LeaveBalance;
import com.payroll.model.PayrollRecord;
import com.payroll.repository.*;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.AttendanceService;
import com.payroll.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public DashboardController(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository,
                               PayrollRecordRepository payrollRecordRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               SalaryStructureRepository salaryStructureRepository,
                               AttendanceService attendanceService,
                               PayrollService payrollService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.attendanceService = attendanceService;
        this.payrollService = payrollService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String role = userDetails.getUser().getRole();

        DashboardStatsDto stats = new DashboardStatsDto();
        LocalDate today = LocalDate.now();
        int curMonth = today.getMonthValue();
        int curYear = today.getYear();

        if ("ROLE_ADMIN".equals(role) || "ROLE_HR".equals(role)) {
            List<Employee> activeEmps = employeeRepository.findByStatus("ACTIVE");
            stats.setTotalEmployees(activeEmps.size());
            stats.setTotalDepartments(departmentRepository.count());

            BigDecimal curPayroll = payrollRecordRepository.sumTotalPayrollForMonth(curMonth, curYear);
            stats.setCurrentMonthPayroll(curPayroll != null ? curPayroll : BigDecimal.ZERO);

            BigDecimal paidSalary = payrollRecordRepository.sumTotalPaidSalary(curMonth, curYear);
            stats.setTotalSalaryPaid(paidSalary != null ? paidSalary : BigDecimal.ZERO);

            stats.setPendingLeaveRequests(leaveRequestRepository.countByStatus("PENDING"));

            // Smart Dashboard Alert metrics
            long missingSalary = activeEmps.stream()
                    .filter(e -> !salaryStructureRepository.existsByEmployeeId(e.getId()))
                    .count();
            stats.setMissingSalaryCount(missingSalary);

            long processedPayrollCount = payrollRecordRepository.countByMonthAndYear(curMonth, curYear);
            stats.setPayrollPendingCurrentMonth(Math.max(0, activeEmps.size() - processedPayrollCount));

            long markedToday = attendanceRecordRepository.countByDate(today);
            stats.setMissingAttendanceToday(Math.max(0, activeEmps.size() - markedToday));

            // Attendance summary for today
            Map<String, Long> attSummary = new HashMap<>();
            attSummary.put("present", attendanceRecordRepository.countByDateAndStatus(today, "PRESENT"));
            attSummary.put("late", attendanceRecordRepository.countByDateAndStatus(today, "LATE"));
            attSummary.put("absent", attendanceRecordRepository.countByDateAndStatus(today, "ABSENT"));
            attSummary.put("leave", attendanceRecordRepository.countByDateAndStatus(today, "ON_LEAVE"));
            stats.setAttendanceSummary(attSummary);

            // Monthly payroll chart data - safe casting and complete 12 months mapping
            List<Object[]> monthlyData = payrollRecordRepository.getMonthlyPayrollOverview(curYear);
            Map<Integer, BigDecimal[]> monthMap = new HashMap<>();
            for (Object[] row : monthlyData) {
                if (row != null && row.length >= 3 && row[0] != null) {
                    int m = ((Number) row[0]).intValue();
                    BigDecimal gross = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    BigDecimal net = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                    monthMap.put(m, new BigDecimal[]{gross, net});
                }
            }

            List<String> months = new ArrayList<>();
            List<BigDecimal> grossList = new ArrayList<>();
            List<BigDecimal> netList = new ArrayList<>();
            String[] monthNames = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            for (int m = 1; m <= 12; m++) {
                months.add(monthNames[m]);
                if (monthMap.containsKey(m)) {
                    grossList.add(monthMap.get(m)[0]);
                    netList.add(monthMap.get(m)[1]);
                } else {
                    grossList.add(BigDecimal.ZERO);
                    netList.add(BigDecimal.ZERO);
                }
            }
            stats.setChartLabels(months);
            stats.setChartGrossData(grossList);
            stats.setChartNetData(netList);

            // Department distribution chart data
            List<Department> depts = departmentRepository.findAll();
            List<String> deptLabels = new ArrayList<>();
            List<Long> deptCounts = new ArrayList<>();
            for (Department d : depts) {
                deptLabels.add(d.getName());
                deptCounts.add((long) employeeRepository.findByDepartmentId(d.getId()).size());
            }
            stats.setDeptChartLabels(deptLabels);
            stats.setDeptChartData(deptCounts);

        } else if ("ROLE_MANAGER".equals(role)) {
            Employee mgr = userDetails.getEmployee();
            if (mgr != null && mgr.getDepartment() != null) {
                Long deptId = mgr.getDepartment().getId();
                List<Employee> team = employeeRepository.findByDepartmentId(deptId);
                stats.setTotalTeamMembers(team.size());

                long presentCount = attendanceRecordRepository.findByEmployeeDepartmentIdAndDate(deptId, today).stream()
                        .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                        .count();
                double attRate = team.isEmpty() ? 0.0 : ((double) presentCount / team.size()) * 100;
                stats.setTeamAttendancePercentage(Math.round(attRate * 10.0) / 10.0);

                stats.setPendingLeaveRequests(leaveRequestRepository.countByEmployeeDepartmentIdAndStatus(deptId, "PENDING"));

                BigDecimal teamPayroll = payrollRecordRepository.findByEmployeeDepartmentIdAndMonthAndYear(deptId, curMonth, curYear).stream()
                        .map(PayrollRecord::getNetSalary)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTeamPayrollSummary(teamPayroll);
            }
        } else {
            // Employee Self-Service Dashboard
            Employee emp = userDetails.getEmployee();
            if (emp != null) {
                stats.setEmployeeName(emp.getFullName());
                stats.setDesignation(emp.getDesignation());
                if (emp.getDepartment() != null) stats.setDepartmentName(emp.getDepartment().getName());

                // Current Month Attendance Summary
                Map<String, Object> attSummary = attendanceService.getEmployeeMonthlySummary(emp.getId(), curYear, curMonth);
                stats.setEmployeeAttendancePercentage((double) attSummary.getOrDefault("attendancePercentage", 0.0));
                stats.setEmployeePresentDays((long) attSummary.getOrDefault("presentDays", 0L));
                stats.setEmployeeAbsentDays((long) attSummary.getOrDefault("absentDays", 0L));
                stats.setEmployeeLeaveDays((long) attSummary.getOrDefault("leaveDays", 0L));

                // Leave Balance
                LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(emp.getId(), curYear)
                        .orElse(new LeaveBalance(emp, curYear));
                stats.setRemainingLeaves(balance.getTotalRemaining());
                stats.setCasualLeavesRemaining(balance.getRemainingCasual());
                stats.setSickLeavesRemaining(balance.getRemainingSick());
                stats.setAnnualLeavesRemaining(balance.getRemainingAnnual());

                // Latest Payslip & Salary
                List<PayrollRecord> payrolls = payrollRecordRepository.findByEmployeeIdOrderByYearDescMonthDesc(emp.getId());
                if (!payrolls.isEmpty()) {
                    PayrollRecord latest = payrolls.get(0);
                    stats.setLatestPayslip(payrollService.mapToDto(latest));
                    stats.setCurrentGrossSalary(latest.getGrossSalary());
                    stats.setCurrentNetSalary(latest.getNetSalary());
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
