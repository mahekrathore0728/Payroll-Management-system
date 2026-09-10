package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.PayrollProcessRequest;
import com.payroll.dto.PayrollRecordDto;
import com.payroll.exception.UnauthorizedException;
import com.payroll.model.PayrollRecord;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.PdfPayslipService;
import com.payroll.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final PdfPayslipService pdfPayslipService;

    public PayrollController(PayrollService payrollService, PdfPayslipService pdfPayslipService) {
        this.payrollService = payrollService;
        this.pdfPayslipService = pdfPayslipService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<PayrollRecordDto>>> getAllPayroll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        List<PayrollRecordDto> list;
        if (month != null && year != null) {
            list = payrollService.getPayrollByMonthAndYear(month, year);
        } else {
            list = payrollService.getAllPayrollRecords();
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<PayrollRecordDto>> processPayroll(@Valid @RequestBody PayrollProcessRequest request) {
        PayrollRecordDto record = payrollService.processPayroll(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payroll processed successfully", record));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<PayrollRecordDto>> updateStatus(@PathVariable Long id,
                                                                      @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "PAID");
        PayrollRecordDto updated = payrollService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Payment status updated to " + status, updated));
    }

    @GetMapping("/{id}/payslip")
    public ResponseEntity<ApiResponse<PayrollRecordDto>> getPayslipDetails(@PathVariable Long id) {
        PayrollRecord entity = payrollService.getPayrollEntity(id);
        verifyPayslipAccess(entity);
        return ResponseEntity.ok(ApiResponse.ok(payrollService.mapToDto(entity)));
    }

    @GetMapping("/{id}/payslip/pdf")
    public ResponseEntity<byte[]> downloadPayslipPdf(@PathVariable Long id) {
        PayrollRecord entity = payrollService.getPayrollEntity(id);
        verifyPayslipAccess(entity);

        byte[] pdfBytes = pdfPayslipService.generatePayslipPdf(entity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "payslip-" + entity.getPayrollNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private void verifyPayslipAccess(PayrollRecord entity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String role = userDetails.getUser().getRole();

        // If employee, verify that the payslip belongs to them
        if ("ROLE_EMPLOYEE".equals(role)) {
            Long userEmpId = userDetails.getEmployeeId();
            if (userEmpId == null || !userEmpId.equals(entity.getEmployee().getId())) {
                throw new UnauthorizedException("Security Violation: You are not authorized to view or download another employee's payslip.");
            }
        } else if ("ROLE_MANAGER".equals(role)) {
            com.payroll.model.Employee mgr = userDetails.getEmployee();
            if (mgr == null || mgr.getDepartment() == null ||
                entity.getEmployee() == null || entity.getEmployee().getDepartment() == null ||
                !mgr.getDepartment().getId().equals(entity.getEmployee().getDepartment().getId())) {
                throw new UnauthorizedException("Security Violation: Managers are only authorized to view payslips within their own department.");
            }
        }
    }
}
