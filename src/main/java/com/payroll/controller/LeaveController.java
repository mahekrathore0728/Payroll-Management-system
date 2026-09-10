package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.LeaveActionRequest;
import com.payroll.dto.LeaveRequestDto;
import com.payroll.exception.UnauthorizedException;
import com.payroll.model.Employee;
import com.payroll.model.LeaveBalance;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // Admin & HR: View all leaves
    @GetMapping("/api/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getAllLeaves() {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.getAllLeaves()));
    }

    // Manager: View team leaves only
    @GetMapping("/api/manager/leaves")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getTeamLeaves() {
        CustomUserDetails user = getAuthenticatedUserDetails();
        Employee emp = user.getEmployee();
        if (emp == null || emp.getDepartment() == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        List<LeaveRequestDto> list = leaveService.getDepartmentLeaves(emp.getDepartment().getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // Admin, HR, Manager: Approve or Reject
    @PostMapping("/api/leaves/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> reviewLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveActionRequest request) {
        CustomUserDetails user = getAuthenticatedUserDetails();
        String role = user.getUser().getRole();

        // If reviewer is a Manager, enforce that the employee belongs to manager's department
        if ("ROLE_MANAGER".equals(role)) {
            com.payroll.model.LeaveRequest entity = leaveService.getLeaveEntity(id);
            Employee mgr = user.getEmployee();
            if (mgr == null || mgr.getDepartment() == null ||
                entity.getEmployee() == null || entity.getEmployee().getDepartment() == null ||
                !mgr.getDepartment().getId().equals(entity.getEmployee().getDepartment().getId())) {
                throw new UnauthorizedException("Security Violation: Managers are only authorized to review leave requests within their own department.");
            }
        }

        LeaveRequestDto reviewed = leaveService.reviewLeave(id, request.getStatus(), request.getManagerRemarks(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Leave request " + request.getStatus().toLowerCase() + " successfully", reviewed));
    }

    // Employee Self-Service: Apply for leave
    @PostMapping("/api/employee/me/leaves")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> applyLeave(@Valid @RequestBody LeaveRequestDto dto) {
        CustomUserDetails user = getAuthenticatedUserDetails();
        Long empId = user.getEmployeeId();
        if (empId == null) {
            throw new UnauthorizedException("No employee profile found for user");
        }
        LeaveRequestDto created = leaveService.applyLeave(empId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Leave request submitted successfully", created));
    }

    // Employee Self-Service: View own leave requests
    @GetMapping("/api/employee/me/leaves")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getMyLeaves() {
        CustomUserDetails user = getAuthenticatedUserDetails();
        Long empId = user.getEmployeeId();
        if (empId == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok(leaveService.getEmployeeLeaves(empId)));
    }

    // Employee Self-Service: View own leave balance
    @GetMapping("/api/employee/me/leaves/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyLeaveBalance(
            @RequestParam(required = false) Integer year) {
        CustomUserDetails user = getAuthenticatedUserDetails();
        Long empId = user.getEmployeeId();
        if (empId == null) {
            throw new UnauthorizedException("No employee profile found for user");
        }
        int qYear = (year != null) ? year : LocalDate.now().getYear();
        LeaveBalance balance = leaveService.getOrCreateLeaveBalance(empId, qYear);

        Map<String, Object> map = new HashMap<>();
        map.put("year", balance.getYear());
        map.put("casualLeaves", balance.getCasualLeaves());
        map.put("usedCasual", balance.getUsedCasual());
        map.put("remainingCasual", balance.getRemainingCasual());
        map.put("sickLeaves", balance.getSickLeaves());
        map.put("usedSick", balance.getUsedSick());
        map.put("remainingSick", balance.getRemainingSick());
        map.put("annualLeaves", balance.getAnnualLeaves());
        map.put("usedAnnual", balance.getUsedAnnual());
        map.put("remainingAnnual", balance.getRemainingAnnual());
        map.put("totalRemaining", balance.getTotalRemaining());

        return ResponseEntity.ok(ApiResponse.ok(map));
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        return (CustomUserDetails) auth.getPrincipal();
    }
}
