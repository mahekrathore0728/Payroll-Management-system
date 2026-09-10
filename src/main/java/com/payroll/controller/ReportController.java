package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/payroll")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayrollReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyPayrollReport(y, m)));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentsReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDepartmentSummaryReport()));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAttendanceReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getAttendanceMonthlyReport(y, m)));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLeavesReport(
            @RequestParam(required = false) Integer year) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getLeaveUtilizationReport(y)));
    }
}
