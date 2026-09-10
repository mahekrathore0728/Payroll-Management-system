package com.payroll.service;

import com.payroll.dto.LeaveRequestDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.AttendanceRecord;
import com.payroll.model.Employee;
import com.payroll.model.LeaveBalance;
import com.payroll.model.LeaveRequest;
import com.payroll.model.Notification;
import com.payroll.repository.AttendanceRecordRepository;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.LeaveBalanceRepository;
import com.payroll.repository.LeaveRequestRepository;
import com.payroll.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository,
                        AttendanceRecordRepository attendanceRecordRepository,
                        NotificationRepository notificationRepository,
                        AuditLogService auditLogService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
    }

    public List<LeaveRequestDto> getAllLeaves() {
        return leaveRequestRepository.findAllByOrderByAppliedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDto> getDepartmentLeaves(Long departmentId) {
        return leaveRequestRepository.findByEmployeeDepartmentIdOrderByAppliedAtDesc(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDto> getEmployeeLeaves(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByAppliedAtDesc(employeeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public LeaveRequest getLeaveEntity(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + id));
    }

    public LeaveBalance getOrCreateLeaveBalance(Long employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> {
                    Employee emp = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
                    LeaveBalance balance = new LeaveBalance(emp, year);
                    return leaveBalanceRepository.save(balance);
                });
    }

    @Transactional
    public LeaveRequestDto applyLeave(Long employeeId, LeaveRequestDto dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (totalDays <= 0) {
            throw new BadRequestException("Total leave days must be at least 1");
        }

        // Check quota in leave balance
        int year = dto.getStartDate().getYear();
        LeaveBalance balance = getOrCreateLeaveBalance(employeeId, year);

        String type = dto.getLeaveType().toUpperCase();
        if ("CASUAL".equals(type) && balance.getRemainingCasual() < totalDays) {
            throw new BadRequestException("Insufficient Casual Leave balance. Remaining: " + balance.getRemainingCasual() + " days.");
        } else if ("SICK".equals(type) && balance.getRemainingSick() < totalDays) {
            throw new BadRequestException("Insufficient Sick Leave balance. Remaining: " + balance.getRemainingSick() + " days.");
        } else if ("ANNUAL".equals(type) && balance.getRemainingAnnual() < totalDays) {
            throw new BadRequestException("Insufficient Annual Leave balance. Remaining: " + balance.getRemainingAnnual() + " days.");
        }

        LeaveRequest req = new LeaveRequest();
        req.setEmployee(employee);
        req.setLeaveType(type);
        req.setStartDate(dto.getStartDate());
        req.setEndDate(dto.getEndDate());
        req.setTotalDays(totalDays);
        req.setReason(dto.getReason());
        req.setStatus("PENDING");
        req.setAppliedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(req);

        // Notify Manager and HR
        Notification notif = new Notification(
                null,
                "ROLE_MANAGER",
                "New Leave Request",
                employee.getFullName() + " applied for " + totalDays + " day(s) of " + type + " leave.",
                "LEAVE"
        );
        notificationRepository.save(notif);

        return mapToDto(saved);
    }

    @Transactional
    public LeaveRequestDto reviewLeave(Long leaveId, String status, String managerRemarks, String reviewerUsername) {
        LeaveRequest req = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BadRequestException("This leave request has already been processed with status: " + req.getStatus());
        }

        String newStatus = status.toUpperCase();
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new BadRequestException("Status must be APPROVED or REJECTED");
        }

        req.setStatus(newStatus);
        req.setManagerRemarks(managerRemarks);
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewedBy(reviewerUsername);

        if ("APPROVED".equals(newStatus)) {
            int year = req.getStartDate().getYear();
            LeaveBalance balance = getOrCreateLeaveBalance(req.getEmployee().getId(), year);

            String type = req.getLeaveType().toUpperCase();
            if ("CASUAL".equals(type)) {
                balance.setUsedCasual(balance.getUsedCasual() + req.getTotalDays());
            } else if ("SICK".equals(type)) {
                balance.setUsedSick(balance.getUsedSick() + req.getTotalDays());
            } else if ("ANNUAL".equals(type)) {
                balance.setUsedAnnual(balance.getUsedAnnual() + req.getTotalDays());
            }
            leaveBalanceRepository.save(balance);

            // Record attendance for the leave dates as ON_LEAVE
            LocalDate cur = req.getStartDate();
            while (!cur.isAfter(req.getEndDate())) {
                AttendanceRecord att = attendanceRecordRepository.findByEmployeeIdAndDate(req.getEmployee().getId(), cur)
                        .orElse(new AttendanceRecord());
                att.setEmployee(req.getEmployee());
                att.setDate(cur);
                att.setStatus("ON_LEAVE");
                att.setRemarks("Approved " + type + " Leave: " + req.getReason());
                attendanceRecordRepository.save(att);
                cur = cur.plusDays(1);
            }
        }

        LeaveRequest updated = leaveRequestRepository.save(req);

        // Notify Employee
        Notification notif = new Notification(
                req.getEmployee().getId(),
                "ROLE_EMPLOYEE",
                "Leave Request " + newStatus,
                "Your " + req.getLeaveType() + " leave request for " + req.getStartDate() + " to " + req.getEndDate() +
                        " was " + newStatus.toLowerCase() + (managerRemarks != null ? ". Remarks: " + managerRemarks : "."),
                "LEAVE"
        );
        notificationRepository.save(notif);

        String action = "APPROVED".equals(newStatus) ? "LEAVE_APPROVED" : "LEAVE_REJECTED";
        String details = ("APPROVED".equals(newStatus) ? "Approved " : "Rejected ") + req.getLeaveType() +
                " leave request for " + req.getEmployee().getFullName() + " (" + req.getTotalDays() + " days). Remarks: " +
                (managerRemarks != null ? managerRemarks : "None");
        auditLogService.log(action, "LEAVE", details);

        return mapToDto(updated);
    }

    public LeaveRequestDto mapToDto(LeaveRequest req) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(req.getId());
        if (req.getEmployee() != null) {
            dto.setEmployeeId(req.getEmployee().getId());
            dto.setEmployeeName(req.getEmployee().getFullName());
            dto.setEmployeeCode(req.getEmployee().getEmployeeCode());
            if (req.getEmployee().getDepartment() != null) {
                dto.setDepartmentName(req.getEmployee().getDepartment().getName());
            }
        }
        dto.setLeaveType(req.getLeaveType());
        dto.setStartDate(req.getStartDate());
        dto.setEndDate(req.getEndDate());
        dto.setTotalDays(req.getTotalDays());
        dto.setReason(req.getReason());
        dto.setStatus(req.getStatus());
        dto.setManagerRemarks(req.getManagerRemarks());
        dto.setAppliedAt(req.getAppliedAt());
        dto.setReviewedAt(req.getReviewedAt());
        dto.setReviewedBy(req.getReviewedBy());
        return dto;
    }
}
