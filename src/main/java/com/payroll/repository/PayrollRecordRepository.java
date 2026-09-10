package com.payroll.repository;

import com.payroll.model.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    Optional<PayrollRecord> findByPayrollNumber(String payrollNumber);
    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);

    List<PayrollRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);
    List<PayrollRecord> findByMonthAndYear(int month, int year);
    List<PayrollRecord> findByYearOrderByMonthDesc(int year);
    List<PayrollRecord> findByEmployeeDepartmentId(Long departmentId);
    List<PayrollRecord> findByEmployeeDepartmentIdAndMonthAndYear(Long departmentId, int month, int year);
    long countByMonthAndYear(int month, int year);

    @Query("SELECT SUM(p.netSalary) FROM PayrollRecord p WHERE p.month = :month AND p.year = :year AND p.paymentStatus = 'PAID'")
    BigDecimal sumTotalPaidSalary(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(p.netSalary) FROM PayrollRecord p WHERE p.month = :month AND p.year = :year")
    BigDecimal sumTotalPayrollForMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT p.month, SUM(p.grossSalary), SUM(p.netSalary), SUM(p.totalDeductions) FROM PayrollRecord p WHERE p.year = :year GROUP BY p.month ORDER BY p.month ASC")
    List<Object[]> getMonthlyPayrollOverview(@Param("year") int year);
}
