package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.AttendanceDto;
import com.payroll.exception.UnauthorizedException;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // Admin & HR: View all attendance
    @GetMapping("/api/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId) {

        List<AttendanceDto> list;
        if (departmentId != null && date != null) {
            list = attendanceService.getAttendanceByDepartmentAndDate(departmentId, date);
        } else if (startDate != null && endDate != null) {
            list = attendanceService.getAttendanceByDateRange(startDate, endDate);
        } else {
            LocalDate queryDate = (date != null) ? date : LocalDate.now();
            list = attendanceService.getAttendanceByDate(queryDate);
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // Admin & HR: Mark / update attendance
    @PostMapping("/api/attendance/mark")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<AttendanceDto>> markAttendance(@Valid @RequestBody AttendanceDto dto) {
        AttendanceDto saved = attendanceService.markAttendance(dto);
        return ResponseEntity.ok(ApiResponse.ok("Attendance marked successfully", saved));
    }

    // Admin & HR: Daily summary
    @GetMapping("/api/attendance/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getAttendanceSummaryByDate(queryDate)));
    }

    // Employee Self-Service: Check-in
    @PostMapping("/api/employee/me/attendance/check-in")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkIn() {
        Long empId = getAuthenticatedEmployeeId();
        AttendanceDto dto = attendanceService.employeeCheckIn(empId);
        return ResponseEntity.ok(ApiResponse.ok("Check-in successful at " + dto.getCheckInTime(), dto));
    }

    // Employee Self-Service: Check-out
    @PostMapping("/api/employee/me/attendance/check-out")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkOut() {
        Long empId = getAuthenticatedEmployeeId();
        AttendanceDto dto = attendanceService.employeeCheckOut(empId);
        return ResponseEntity.ok(ApiResponse.ok("Check-out successful at " + dto.getCheckOutTime(), dto));
    }

    // Employee Self-Service: View own attendance
    @GetMapping("/api/employee/me/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getMyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long empId = getAuthenticatedEmployeeId();
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        List<AttendanceDto> list = attendanceService.getEmployeeAttendance(empId, start, end);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // Employee Self-Service: Monthly summary
    @GetMapping("/api/employee/me/attendance/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAttendanceSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long empId = getAuthenticatedEmployeeId();
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getEmployeeMonthlySummary(empId, y, m)));
    }

    private Long getAuthenticatedEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        Long empId = details.getEmployeeId();
        if (empId == null) {
            throw new UnauthorizedException("No employee record associated with this account");
        }
        return empId;
    }
}
