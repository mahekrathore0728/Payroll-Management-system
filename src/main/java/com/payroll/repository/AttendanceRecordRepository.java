package com.payroll.repository;

import com.payroll.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<AttendanceRecord> findByEmployeeIdOrderByDateDesc(Long employeeId);
    List<AttendanceRecord> findByEmployeeIdAndDateBetweenOrderByDateDesc(Long employeeId, LocalDate start, LocalDate end);
    List<AttendanceRecord> findByDate(LocalDate date);
    List<AttendanceRecord> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
    List<AttendanceRecord> findByEmployeeDepartmentIdAndDate(Long departmentId, LocalDate date);

    long countByDate(LocalDate date);
    long countByDateAndStatus(LocalDate date, String status);
    long countByEmployeeIdAndDateBetweenAndStatus(Long employeeId, LocalDate start, LocalDate end, String status);
    long countByEmployeeIdAndDateBetween(Long employeeId, LocalDate start, LocalDate end);

    @Query("SELECT a.status, COUNT(a) FROM AttendanceRecord a WHERE a.date = :date GROUP BY a.status")
    List<Object[]> getAttendanceSummaryByDate(@Param("date") LocalDate date);
}
