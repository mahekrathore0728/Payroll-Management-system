package com.payroll.service;

import com.payroll.dto.AttendanceDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.AttendanceRecord;
import com.payroll.model.Employee;
import com.payroll.repository.AttendanceRecordRepository;
import com.payroll.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository, EmployeeRepository employeeRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<AttendanceDto> getAttendanceByDate(LocalDate date) {
        return attendanceRecordRepository.findByDate(date).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AttendanceDto> getAttendanceByDateRange(LocalDate start, LocalDate end) {
        return attendanceRecordRepository.findByDateBetweenOrderByDateDesc(start, end).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AttendanceDto> getEmployeeAttendance(Long employeeId, LocalDate start, LocalDate end) {
        return attendanceRecordRepository.findByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, start, end).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AttendanceDto> getAttendanceByDepartmentAndDate(Long departmentId, LocalDate date) {
        return attendanceRecordRepository.findByEmployeeDepartmentIdAndDate(departmentId, date).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceDto markAttendance(AttendanceDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(dto.getEmployeeId(), dto.getDate())
                .orElse(new AttendanceRecord());

        record.setEmployee(employee);
        record.setDate(dto.getDate());
        record.setStatus(dto.getStatus().toUpperCase());
        record.setCheckInTime(dto.getCheckInTime());
        record.setCheckOutTime(dto.getCheckOutTime());
        record.setRemarks(dto.getRemarks());

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return mapToDto(saved);
    }

    @Transactional
    public AttendanceDto employeeCheckIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElse(new AttendanceRecord());

        if (record.getCheckInTime() != null) {
            throw new BadRequestException("You have already checked in today at " + record.getCheckInTime());
        }

        record.setEmployee(employee);
        record.setDate(today);
        record.setCheckInTime(now);

        // Mark as late if checked in after 9:30 AM
        if (now.isAfter(LocalTime.of(9, 30))) {
            record.setStatus("LATE");
            record.setRemarks("Late check-in at " + now.toString().substring(0, 5));
        } else {
            record.setStatus("PRESENT");
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return mapToDto(saved);
    }

    @Transactional
    public AttendanceDto employeeCheckOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        AttendanceRecord record = attendanceRecordRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Please check in first before checking out."));

        if (record.getCheckOutTime() != null) {
            throw new BadRequestException("You have already checked out today at " + record.getCheckOutTime());
        }

        record.setCheckOutTime(now);
        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return mapToDto(saved);
    }

    public Map<String, Object> getAttendanceSummaryByDate(LocalDate date) {
        Map<String, Object> summary = new HashMap<>();
        long totalEmployees = employeeRepository.countByStatus("ACTIVE");
        long present = attendanceRecordRepository.countByDateAndStatus(date, "PRESENT");
        long late = attendanceRecordRepository.countByDateAndStatus(date, "LATE");
        long absent = attendanceRecordRepository.countByDateAndStatus(date, "ABSENT");
        long leave = attendanceRecordRepository.countByDateAndStatus(date, "ON_LEAVE");

        summary.put("date", date);
        summary.put("totalEmployees", totalEmployees);
        summary.put("present", present);
        summary.put("late", late);
        summary.put("absent", absent);
        summary.put("leave", leave);

        double rate = totalEmployees > 0 ? ((double) (present + late) / totalEmployees) * 100 : 0.0;
        summary.put("attendanceRate", Math.round(rate * 10.0) / 10.0);
        return summary;
    }

    public Map<String, Object> getEmployeeMonthlySummary(Long employeeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        long totalRecorded = attendanceRecordRepository.countByEmployeeIdAndDateBetween(employeeId, start, end);
        long present = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(employeeId, start, end, "PRESENT");
        long late = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(employeeId, start, end, "LATE");
        long absent = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(employeeId, start, end, "ABSENT");
        long leave = attendanceRecordRepository.countByEmployeeIdAndDateBetweenAndStatus(employeeId, start, end, "ON_LEAVE");

        Map<String, Object> map = new HashMap<>();
        map.put("totalDays", totalRecorded);
        map.put("presentDays", present);
        map.put("lateDays", late);
        map.put("absentDays", absent);
        map.put("leaveDays", leave);

        double pct = totalRecorded > 0 ? ((double) (present + late) / totalRecorded) * 100 : 0.0;
        map.put("attendancePercentage", Math.round(pct * 10.0) / 10.0);
        return map;
    }

    public AttendanceDto mapToDto(AttendanceRecord record) {
        AttendanceDto dto = new AttendanceDto();
        dto.setId(record.getId());
        if (record.getEmployee() != null) {
            dto.setEmployeeId(record.getEmployee().getId());
            dto.setEmployeeName(record.getEmployee().getFullName());
            dto.setEmployeeCode(record.getEmployee().getEmployeeCode());
            if (record.getEmployee().getDepartment() != null) {
                dto.setDepartmentName(record.getEmployee().getDepartment().getName());
            }
        }
        dto.setDate(record.getDate());
        dto.setStatus(record.getStatus());
        dto.setCheckInTime(record.getCheckInTime());
        dto.setCheckOutTime(record.getCheckOutTime());
        dto.setRemarks(record.getRemarks());
        return dto;
    }
}
