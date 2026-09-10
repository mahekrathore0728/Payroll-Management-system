package com.payroll;

import com.payroll.dto.DashboardStatsDto;
import com.payroll.dto.SalaryHistoryDto;
import com.payroll.dto.SalaryStructureDto;
import com.payroll.model.AuditLog;
import com.payroll.model.Employee;
import com.payroll.model.SalaryHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class NewFeaturesAndAnalyticsTests {

    @Test
    @DisplayName("Test SalaryHistory entity and reason persistence")
    public void testSalaryHistoryModel() {
        Employee emp = new Employee();
        emp.setId(10L);
        emp.setEmployeeCode("EMP010");
        emp.setFirstName("John");
        emp.setLastName("Doe");

        SalaryHistory history = new SalaryHistory(
                emp,
                new BigDecimal("50000.00"),
                new BigDecimal("58000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("35000.00"),
                new BigDecimal("50000.00"),
                LocalDate.of(2026, 9, 1),
                "Annual appraisal 16% hike",
                "admin"
        );

        assertEquals(emp, history.getEmployee());
        assertEquals(new BigDecimal("50000.00"), history.getPreviousGrossSalary());
        assertEquals(new BigDecimal("58000.00"), history.getNewGrossSalary());
        assertEquals(new BigDecimal("30000.00"), history.getPreviousBasicSalary());
        assertEquals(new BigDecimal("35000.00"), history.getNewBasicSalary());
        assertEquals(new BigDecimal("50000.00"), history.getNewNetSalary());
        assertEquals(LocalDate.of(2026, 9, 1), history.getEffectiveDate());
        assertEquals("Annual appraisal 16% hike", history.getReason());
        assertEquals("admin", history.getUpdatedBy());
    }

    @Test
    @DisplayName("Test SalaryHistoryDto mappings and fields")
    public void testSalaryHistoryDto() {
        SalaryHistoryDto dto = new SalaryHistoryDto();
        dto.setId(1L);
        dto.setEmployeeId(10L);
        dto.setEmployeeName("John Doe");
        dto.setEmployeeCode("EMP010");
        dto.setPreviousGrossSalary(new BigDecimal("50000.00"));
        dto.setNewGrossSalary(new BigDecimal("58000.00"));
        dto.setPreviousBasicSalary(new BigDecimal("30000.00"));
        dto.setNewBasicSalary(new BigDecimal("35000.00"));
        dto.setNewNetSalary(new BigDecimal("50000.00"));
        dto.setEffectiveDate(LocalDate.of(2026, 9, 1));
        dto.setReason("Promotion to Senior Engineer");
        dto.setUpdatedBy("hr_manager");
        dto.setCreatedAt(LocalDateTime.now());

        assertEquals(1L, dto.getId());
        assertEquals("John Doe", dto.getEmployeeName());
        assertEquals("Promotion to Senior Engineer", dto.getReason());
        assertEquals("hr_manager", dto.getUpdatedBy());
    }

    @Test
    @DisplayName("Test SalaryStructureDto reason field")
    public void testSalaryStructureDtoReason() {
        SalaryStructureDto dto = new SalaryStructureDto();
        dto.setEmployeeId(5L);
        dto.setBasicSalary(new BigDecimal("40000.00"));
        dto.setReason("Market correction adjustment");

        assertEquals("Market correction adjustment", dto.getReason());
    }

    @Test
    @DisplayName("Test AuditLog entity")
    public void testAuditLogModel() {
        AuditLog log = new AuditLog("EMPLOYEE_CREATED", "EMPLOYEE", "superadmin", "Created employee Jane Smith (EMP002)");
        assertEquals("EMPLOYEE_CREATED", log.getAction());
        assertEquals("EMPLOYEE", log.getModule());
        assertEquals("superadmin", log.getPerformedBy());
        assertEquals("Created employee Jane Smith (EMP002)", log.getDetails());
        assertNotNull(log.getTimestamp());
    }

    @Test
    @DisplayName("Test DashboardStatsDto alert metrics")
    public void testDashboardAlertMetrics() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setMissingSalaryCount(3L);
        stats.setPayrollPendingCurrentMonth(5L);
        stats.setMissingAttendanceToday(2L);
        stats.setPendingLeaveRequests(4L);

        assertEquals(3L, stats.getMissingSalaryCount());
        assertEquals(5L, stats.getPayrollPendingCurrentMonth());
        assertEquals(2L, stats.getMissingAttendanceToday());
        assertEquals(4L, stats.getPendingLeaveRequests());
    }
}
