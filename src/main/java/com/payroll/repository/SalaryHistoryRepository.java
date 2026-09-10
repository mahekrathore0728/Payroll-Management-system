package com.payroll.repository;

import com.payroll.model.SalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, Long> {
    List<SalaryHistory> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
