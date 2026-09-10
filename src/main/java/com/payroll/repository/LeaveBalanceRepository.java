package com.payroll.repository;

import com.payroll.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
    void deleteByEmployeeId(Long employeeId);
}
