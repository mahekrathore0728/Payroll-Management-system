package com.payroll.service;

import com.payroll.dto.SalaryStructureDto;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.Employee;
import com.payroll.model.SalaryStructure;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.SalaryStructureRepository;
import com.payroll.dto.SalaryHistoryDto;
import com.payroll.model.SalaryHistory;
import com.payroll.repository.SalaryHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalaryService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final AuditLogService auditLogService;

    public SalaryService(SalaryStructureRepository salaryStructureRepository,
                         EmployeeRepository employeeRepository,
                         SalaryHistoryRepository salaryHistoryRepository,
                         AuditLogService auditLogService) {
        this.salaryStructureRepository = salaryStructureRepository;
        this.employeeRepository = employeeRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.auditLogService = auditLogService;
    }

    public List<SalaryStructureDto> getAllSalaries() {
        return salaryStructureRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SalaryStructureDto getSalaryByEmployeeId(Long employeeId) {
        SalaryStructure salary = salaryStructureRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found for employee ID: " + employeeId));
        return mapToDto(salary);
    }

    public List<SalaryHistoryDto> getSalaryHistory(Long employeeId) {
        return salaryHistoryRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::mapHistoryToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryStructureDto saveOrUpdateSalary(SalaryStructureDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        SalaryStructure salary = salaryStructureRepository.findByEmployeeId(dto.getEmployeeId())
                .orElse(new SalaryStructure());

        boolean isNew = salary.getId() == null;
        BigDecimal prevGross = isNew ? BigDecimal.ZERO : (salary.getGrossSalary() != null ? salary.getGrossSalary() : BigDecimal.ZERO);
        BigDecimal prevBasic = isNew ? BigDecimal.ZERO : (salary.getBasicSalary() != null ? salary.getBasicSalary() : BigDecimal.ZERO);

        salary.setEmployee(employee);
        salary.setBasicSalary(dto.getBasicSalary() != null ? dto.getBasicSalary() : BigDecimal.ZERO);
        salary.setHra(dto.getHra() != null ? dto.getHra() : BigDecimal.ZERO);
        salary.setDa(dto.getDa() != null ? dto.getDa() : BigDecimal.ZERO);
        salary.setBonus(dto.getBonus() != null ? dto.getBonus() : BigDecimal.ZERO);
        salary.setOvertimeRate(dto.getOvertimeRate() != null ? dto.getOvertimeRate() : BigDecimal.ZERO);
        salary.setOtherAllowances(dto.getOtherAllowances() != null ? dto.getOtherAllowances() : BigDecimal.ZERO);
        salary.setPf(dto.getPf() != null ? dto.getPf() : BigDecimal.ZERO);
        salary.setTax(dto.getTax() != null ? dto.getTax() : BigDecimal.ZERO);
        salary.setOtherDeductions(dto.getOtherDeductions() != null ? dto.getOtherDeductions() : BigDecimal.ZERO);

        salary.calculateTotals();

        SalaryStructure saved = salaryStructureRepository.save(salary);

        // Record Salary Revision History
        String username = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            username = auth.getName();
        }

        String reason = (dto.getReason() != null && !dto.getReason().trim().isEmpty())
                ? dto.getReason().trim()
                : (isNew ? "Initial salary structure configuration" : "Salary structure update");

        SalaryHistory history = new SalaryHistory(
                employee,
                prevGross,
                saved.getGrossSalary(),
                prevBasic,
                saved.getBasicSalary(),
                saved.getNetSalary(),
                LocalDate.now(),
                reason,
                username
        );
        salaryHistoryRepository.save(history);

        // Audit Log
        auditLogService.log(
                "SALARY_UPDATED",
                "SALARY",
                "Updated salary structure for " + employee.getFullName() + " (" + employee.getEmployeeCode() + "). Gross: ₹" + saved.getGrossSalary() + ", Net: ₹" + saved.getNetSalary() + ". Reason: " + reason
        );

        return mapToDto(saved);
    }

    public SalaryHistoryDto mapHistoryToDto(SalaryHistory h) {
        SalaryHistoryDto dto = new SalaryHistoryDto();
        dto.setId(h.getId());
        if (h.getEmployee() != null) {
            dto.setEmployeeId(h.getEmployee().getId());
            dto.setEmployeeName(h.getEmployee().getFullName());
            dto.setEmployeeCode(h.getEmployee().getEmployeeCode());
        }
        dto.setPreviousGrossSalary(h.getPreviousGrossSalary());
        dto.setNewGrossSalary(h.getNewGrossSalary());
        dto.setPreviousBasicSalary(h.getPreviousBasicSalary());
        dto.setNewBasicSalary(h.getNewBasicSalary());
        dto.setNewNetSalary(h.getNewNetSalary());
        dto.setEffectiveDate(h.getEffectiveDate());
        dto.setReason(h.getReason());
        dto.setUpdatedBy(h.getUpdatedBy());
        dto.setCreatedAt(h.getCreatedAt());
        return dto;
    }

    public SalaryStructureDto mapToDto(SalaryStructure salary) {
        SalaryStructureDto dto = new SalaryStructureDto();
        dto.setId(salary.getId());
        if (salary.getEmployee() != null) {
            dto.setEmployeeId(salary.getEmployee().getId());
            dto.setEmployeeName(salary.getEmployee().getFullName());
            dto.setEmployeeCode(salary.getEmployee().getEmployeeCode());
            dto.setDesignation(salary.getEmployee().getDesignation());
            if (salary.getEmployee().getDepartment() != null) {
                dto.setDepartmentName(salary.getEmployee().getDepartment().getName());
            }
        }
        dto.setBasicSalary(salary.getBasicSalary());
        dto.setHra(salary.getHra());
        dto.setDa(salary.getDa());
        dto.setBonus(salary.getBonus());
        dto.setOvertimeRate(salary.getOvertimeRate());
        dto.setOtherAllowances(salary.getOtherAllowances());
        dto.setPf(salary.getPf());
        dto.setTax(salary.getTax());
        dto.setOtherDeductions(salary.getOtherDeductions());
        dto.setGrossSalary(salary.getGrossSalary());
        dto.setTotalDeductions(salary.getTotalDeductions());
        dto.setNetSalary(salary.getNetSalary());
        return dto;
    }
}
