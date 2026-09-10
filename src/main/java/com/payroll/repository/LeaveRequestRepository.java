package com.payroll.repository;

import com.payroll.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);
    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(String status);
    List<LeaveRequest> findAllByOrderByAppliedAtDesc();
    List<LeaveRequest> findByEmployeeDepartmentIdOrderByAppliedAtDesc(Long departmentId);
    List<LeaveRequest> findByEmployeeDepartmentIdAndStatusOrderByAppliedAtDesc(Long departmentId, String status);
    long countByStatus(String status);
    long countByEmployeeDepartmentIdAndStatus(Long departmentId, String status);
}
