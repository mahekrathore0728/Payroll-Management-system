package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.model.AuditLog;
import com.payroll.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(@RequestParam(required = false) String module) {
        List<AuditLog> logs = (module != null && !module.isBlank())
                ? auditLogService.getLogsByModule(module.trim().toUpperCase())
                : auditLogService.getRecentLogs();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
