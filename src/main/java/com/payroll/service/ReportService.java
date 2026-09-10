package com.payroll.service;

import com.payroll.dto.PayrollRecordDto;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.model.LeaveBalance;
import com.payroll.model.PayrollRecord;
import com.payroll.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportService {

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayrollService payrollService;

    public ReportService(PayrollRecordRepository payrollRecordRepository,
                         EmployeeRepository employeeRepository,
                         DepartmentRepository departmentRepository,
                         AttendanceRecordRepository attendanceRecordRepository,
                         LeaveBalanceRepository leaveBalanceRepository,
                         PayrollService payrollService) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.payrollService = payrollService;
    }

    public Map<String, Object> getMonthlyPayrollReport(int year, int month) {
        List<PayrollRecord> records = payrollRecordRepository.findByMonthAndYear(month, year);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        long paidCount = 0;
        long pendingCount = 0;

        List<PayrollRecordDto> dtoList = new ArrayList<>();
        for (PayrollRecord r : records) {
            totalGross = totalGross.add(r.getGrossSalary());
            totalDeductions = totalDeductions.add(r.getTotalDeductions());
            totalNet = totalNet.add(r.getNetSalary());
            if ("PAID".equalsIgnoreCase(r.getPaymentStatus())) paidCount++;
            else pendingCount++;
            dtoList.add(payrollService.mapToDto(r));
        }

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("totalRecords", records.size());
        report.put("totalGross", totalGross);
        report.put("totalDeductions", totalDeductions);
        report.put("totalNet", totalNet);
        report.put("paidCount", paidCount);
        report.put("pendingCount", pendingCount);
        report.put("records", dtoList);
        return report;
    }

    public List<Map<String, Object>> getDepartmentSummaryReport() {
        List<Department> departments = departmentRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();

        int curMonth = LocalDate.now().getMonthValue();
        int curYear = LocalDate.now().getYear();

        for (Department d : departments) {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentId", d.getId());
            map.put("departmentName", d.getName());
            map.put("departmentCode", d.getCode());

            List<Employee> emps = employeeRepository.findByDepartmentId(d.getId());
            map.put("employeeCount", emps.size());

            List<PayrollRecord> payrolls = payrollRecordRepository.findByEmployeeDepartmentIdAndMonthAndYear(d.getId(), curMonth, curYear);
            BigDecimal totalNet = payrolls.stream()
                    .map(PayrollRecord::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            map.put("currentMonthPayroll", totalNet);

            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> getAttendanceMonthlyReport(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Employee> employees = employeeRepository.findByStatus("ACTIVE");
        List<Map<String, Object>> report = new ArrayList<>();

        for (Employee e : employees) {
            long total = attendanceRecordRepository.countByEmployeeIdAndDateBetween(e.getId(), start, end);
            long present = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(e.getId(), start, end, "PRESENT");
            long late = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(e.getId(), start, end, "LATE");
            long absent = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(e.getId(), start, end, "ABSENT");
            long leave = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(e.getId(), start, end, "ON_LEAVE");

            double pct = total > 0 ? ((double) (present + late) / total) * 100 : 0.0;

            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", e.getId());
            row.put("employeeCode", e.getEmployeeCode());
            row.put("employeeName", e.getFullName());
            row.put("departmentName", e.getDepartment() != null ? e.getDepartment().getName() : "N/A");
            row.put("designation", e.getDesignation());
            row.put("totalDays", total);
            row.put("presentDays", present);
            row.put("lateDays", late);
            row.put("absentDays", absent);
            row.put("leaveDays", leave);
            row.put("attendancePercentage", Math.round(pct * 10.0) / 10.0);
            report.add(row);
        }
        return report;
    }

    public List<Map<String, Object>> getLeaveUtilizationReport(int year) {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (Employee e : employees) {
            LeaveBalance b = leaveBalanceRepository.findByEmployeeIdAndYear(e.getId(), year)
                    .orElse(new LeaveBalance(e, year));

            Map<String, Object> row = new HashMap<>();
            row.put("employeeId", e.getId());
            row.put("employeeCode", e.getEmployeeCode());
            row.put("employeeName", e.getFullName());
            row.put("departmentName", e.getDepartment() != null ? e.getDepartment().getName() : "N/A");
            row.put("casualLeaves", b.getCasualLeaves());
            row.put("usedCasual", b.getUsedCasual());
            row.put("remainingCasual", b.getRemainingCasual());
            row.put("sickLeaves", b.getSickLeaves());
            row.put("usedSick", b.getUsedSick());
            row.put("remainingSick", b.getRemainingSick());
            row.put("annualLeaves", b.getAnnualLeaves());
            row.put("usedAnnual", b.getUsedAnnual());
            row.put("remainingAnnual", b.getRemainingAnnual());
            row.put("totalRemaining", b.getTotalRemaining());
            report.add(row);
        }
        return report;
    }
}
