package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.SalaryStructureDto;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.SalaryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salaries")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<SalaryStructureDto>>> getAllSalaries() {
        return ResponseEntity.ok(ApiResponse.ok(salaryService.getAllSalaries()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> getSalaryByEmployeeId(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(salaryService.getSalaryByEmployeeId(employeeId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> getMySalary() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new com.payroll.exception.UnauthorizedException("Authentication required");
        }
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Long empId = user.getEmployeeId();
        if (empId == null) {
            throw new com.payroll.exception.UnauthorizedException("No employee profile found for user");
        }
        return ResponseEntity.ok(ApiResponse.ok(salaryService.getSalaryByEmployeeId(empId)));
    }

    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<com.payroll.dto.SalaryHistoryDto>>> getSalaryHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(salaryService.getSalaryHistory(employeeId)));
    }

    @GetMapping("/me/history")
    public ResponseEntity<ApiResponse<List<com.payroll.dto.SalaryHistoryDto>>> getMySalaryHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new com.payroll.exception.UnauthorizedException("Authentication required");
        }
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        Long empId = user.getEmployeeId();
        if (empId == null) {
            throw new com.payroll.exception.UnauthorizedException("No employee profile found for user");
        }
        return ResponseEntity.ok(ApiResponse.ok(salaryService.getSalaryHistory(empId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<SalaryStructureDto>> saveSalaryStructure(@Valid @RequestBody SalaryStructureDto dto) {
        SalaryStructureDto saved = salaryService.saveOrUpdateSalary(dto);
        return ResponseEntity.ok(ApiResponse.ok("Salary structure updated successfully", saved));
    }
}
