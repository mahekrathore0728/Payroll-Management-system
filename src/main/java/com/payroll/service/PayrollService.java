package com.payroll.service;

import com.payroll.dto.PayrollProcessRequest;
import com.payroll.dto.PayrollRecordDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.Employee;
import com.payroll.model.Notification;
import com.payroll.model.PayrollRecord;
import com.payroll.model.SalaryStructure;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.NotificationRepository;
import com.payroll.repository.PayrollRecordRepository;
import com.payroll.repository.SalaryStructureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public PayrollService(PayrollRecordRepository payrollRecordRepository,
                          EmployeeRepository employeeRepository,
                          SalaryStructureRepository salaryStructureRepository,
                          NotificationRepository notificationRepository,
                          AuditLogService auditLogService) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.employeeRepository = employeeRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
    }

    public List<PayrollRecordDto> getAllPayrollRecords() {
        return payrollRecordRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PayrollRecordDto> getPayrollByMonthAndYear(int month, int year) {
        return payrollRecordRepository.findByMonthAndYear(month, year).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PayrollRecordDto> getPayrollByEmployeeId(Long employeeId) {
        return payrollRecordRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PayrollRecordDto> getPayrollByDepartment(Long departmentId) {
        return payrollRecordRepository.findByEmployeeDepartmentId(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public PayrollRecordDto getPayrollById(Long id) {
        PayrollRecord record = payrollRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found with ID: " + id));
        return mapToDto(record);
    }

    public PayrollRecord getPayrollEntity(Long id) {
        return payrollRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found with ID: " + id));
    }

    @Transactional
    public PayrollRecordDto processPayroll(PayrollProcessRequest req) {
        Employee employee = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + req.getEmployeeId()));

        if (payrollRecordRepository.existsByEmployeeIdAndMonthAndYear(req.getEmployeeId(), req.getMonth(), req.getYear())) {
            throw new BadRequestException("Payroll record already exists for " + employee.getFullName() +
                    " (" + employee.getEmployeeCode() + ") for period " + String.format("%02d/%d", req.getMonth(), req.getYear()));
        }

        SalaryStructure salary = salaryStructureRepository.findByEmployeeId(req.getEmployeeId())
                .orElseThrow(() -> new BadRequestException("Salary structure not configured for employee " + employee.getFullName()));

        BigDecimal overtimeHours = req.getOvertimeHours() != null ? req.getOvertimeHours() : BigDecimal.ZERO;
        BigDecimal overtimeRate = salary.getOvertimeRate() != null ? salary.getOvertimeRate() : BigDecimal.ZERO;
        BigDecimal overtimePay = overtimeHours.multiply(overtimeRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal additionalBonus = req.getAdditionalBonus() != null ? req.getAdditionalBonus() : BigDecimal.ZERO;
        BigDecimal totalBonus = (salary.getBonus() != null ? salary.getBonus() : BigDecimal.ZERO).add(additionalBonus);

        BigDecimal basic = salary.getBasicSalary() != null ? salary.getBasicSalary() : BigDecimal.ZERO;
        BigDecimal hra = salary.getHra() != null ? salary.getHra() : BigDecimal.ZERO;
        BigDecimal da = salary.getDa() != null ? salary.getDa() : BigDecimal.ZERO;
        BigDecimal otherAllowances = salary.getOtherAllowances() != null ? salary.getOtherAllowances() : BigDecimal.ZERO;

        BigDecimal grossSalary = basic.add(hra).add(da).add(totalBonus).add(overtimePay).add(otherAllowances)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal pf = salary.getPf() != null ? salary.getPf() : BigDecimal.ZERO;
        BigDecimal tax = salary.getTax() != null ? salary.getTax() : BigDecimal.ZERO;
        BigDecimal additionalDeductions = req.getAdditionalDeductions() != null ? req.getAdditionalDeductions() : BigDecimal.ZERO;
        BigDecimal otherDeductions = (salary.getOtherDeductions() != null ? salary.getOtherDeductions() : BigDecimal.ZERO)
                .add(additionalDeductions);

        BigDecimal totalDeductions = pf.add(tax).add(otherDeductions).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Total deductions exceed gross earnings. Net salary cannot be negative.");
        }

        String payrollNumber = String.format("PAY-%d%02d-%s-%d", req.getYear(), req.getMonth(),
                employee.getEmployeeCode(), System.currentTimeMillis() % 10000);

        PayrollRecord record = new PayrollRecord();
        record.setPayrollNumber(payrollNumber);
        record.setEmployee(employee);
        record.setMonth(req.getMonth());
        record.setYear(req.getYear());
        record.setBasicSalary(basic);
        record.setHra(hra);
        record.setDa(da);
        record.setBonus(totalBonus);
        record.setOvertimePay(overtimePay);
        record.setOtherAllowances(otherAllowances);
        record.setGrossSalary(grossSalary);
        record.setPfDeduction(pf);
        record.setTaxDeduction(tax);
        record.setOtherDeductions(otherDeductions);
        record.setTotalDeductions(totalDeductions);
        record.setNetSalary(netSalary);
        record.setPaymentStatus("PENDING");
        record.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "BANK_TRANSFER");
        record.setCreatedAt(LocalDateTime.now());

        PayrollRecord saved = payrollRecordRepository.save(record);

        // Notify employee
        Notification notification = new Notification(
                employee.getId(),
                "ROLE_EMPLOYEE",
                "Payroll Processed",
                String.format("Your payroll for %02d/%d has been generated. Net pay: ₹%,.2f", req.getMonth(), req.getYear(), netSalary),
                "PAYROLL"
        );
        notificationRepository.save(notification);

        auditLogService.log("PAYROLL_PROCESSED", "PAYROLL",
                "Processed payroll for " + employee.getFullName() + " (" + employee.getEmployeeCode() + ") for period " + String.format("%02d/%d", req.getMonth(), req.getYear()) + ". Net pay: ₹" + netSalary);

        return mapToDto(saved);
    }

    @Transactional
    public PayrollRecordDto updatePaymentStatus(Long id, String status) {
        PayrollRecord record = payrollRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found with ID: " + id));

        record.setPaymentStatus(status);
        if ("PAID".equalsIgnoreCase(status)) {
            record.setPaymentDate(LocalDate.now());

            Notification notification = new Notification(
                    record.getEmployee().getId(),
                    "ROLE_EMPLOYEE",
                    "Salary Disbursed",
                    String.format("Your salary of ₹%,.2f for %02d/%d has been disbursed successfully.",
                            record.getNetSalary(), record.getMonth(), record.getYear()),
                    "PAYROLL"
            );
            notificationRepository.save(notification);
        }

        PayrollRecord updated = payrollRecordRepository.save(record);

        auditLogService.log("PAYROLL_STATUS_UPDATED", "PAYROLL",
                "Updated payment status to " + status + " for " + (record.getEmployee() != null ? record.getEmployee().getFullName() : "ID " + id) + " (" + record.getPayrollNumber() + ")");

        return mapToDto(updated);
    }

    public PayrollRecordDto mapToDto(PayrollRecord record) {
        PayrollRecordDto dto = new PayrollRecordDto();
        dto.setId(record.getId());
        dto.setPayrollNumber(record.getPayrollNumber());
        if (record.getEmployee() != null) {
            dto.setEmployeeId(record.getEmployee().getId());
            dto.setEmployeeName(record.getEmployee().getFullName());
            dto.setEmployeeCode(record.getEmployee().getEmployeeCode());
            dto.setDesignation(record.getEmployee().getDesignation());
            if (record.getEmployee().getDepartment() != null) {
                dto.setDepartmentName(record.getEmployee().getDepartment().getName());
            }
        }
        dto.setMonth(record.getMonth());
        dto.setYear(record.getYear());
        dto.setBasicSalary(record.getBasicSalary());
        dto.setHra(record.getHra());
        dto.setDa(record.getDa());
        dto.setBonus(record.getBonus());
        dto.setOvertimePay(record.getOvertimePay());
        dto.setOtherAllowances(record.getOtherAllowances());
        dto.setGrossSalary(record.getGrossSalary());
        dto.setPfDeduction(record.getPfDeduction());
        dto.setTaxDeduction(record.getTaxDeduction());
        dto.setOtherDeductions(record.getOtherDeductions());
        dto.setTotalDeductions(record.getTotalDeductions());
        dto.setNetSalary(record.getNetSalary());
        dto.setPaymentStatus(record.getPaymentStatus());
        dto.setPaymentDate(record.getPaymentDate());
        dto.setPaymentMethod(record.getPaymentMethod());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }
}
