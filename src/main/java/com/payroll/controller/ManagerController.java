package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.AttendanceDto;
import com.payroll.dto.EmployeeDto;
import com.payroll.dto.PayrollRecordDto;
import com.payroll.exception.UnauthorizedException;
import com.payroll.model.Employee;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.AttendanceService;
import com.payroll.service.EmployeeService;
import com.payroll.service.PayrollService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;

    public ManagerController(EmployeeService employeeService,
                             AttendanceService attendanceService,
                             PayrollService payrollService) {
        this.employeeService = employeeService;
        this.attendanceService = attendanceService;
        this.payrollService = payrollService;
    }

    @GetMapping("/team")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getTeamMembers() {
        Long deptId = getManagerDepartmentId();
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployeesByDepartment(deptId)));
    }

    @GetMapping("/team/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getTeamAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long deptId = getManagerDepartmentId();
        LocalDate qDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAttendanceByDepartmentAndDate(deptId, qDate)));
    }

    @GetMapping("/team/payroll")
    public ResponseEntity<ApiResponse<List<PayrollRecordDto>>> getTeamPayroll() {
        Long deptId = getManagerDepartmentId();
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPayrollByDepartment(deptId)));
    }

    private Long getManagerDepartmentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Employee emp = user.getEmployee();
        if (emp == null || emp.getDepartment() == null) {
            throw new UnauthorizedException("Manager is not assigned to any department");
        }
        return emp.getDepartment().getId();
    }
}
