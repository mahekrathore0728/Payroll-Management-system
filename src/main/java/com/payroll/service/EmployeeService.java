package com.payroll.service;

import com.payroll.dto.EmployeeDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.model.LeaveBalance;
import com.payroll.model.SalaryStructure;
import com.payroll.model.User;
import com.payroll.repository.DepartmentRepository;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.LeaveBalanceRepository;
import com.payroll.repository.SalaryStructureRepository;
import com.payroll.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           SalaryStructureRepository salaryStructureRepository,
                           LeaveBalanceRepository leaveBalanceRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<EmployeeDto> searchEmployees(String keyword, Long departmentId, String status) {
        return employeeRepository.searchEmployees(keyword, departmentId, status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public EmployeeDto getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return mapToDto(emp);
    }

    public Employee getEmployeeEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    public List<EmployeeDto> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeDto createEmployee(EmployeeDto dto) {
        if (employeeRepository.existsByEmployeeCode(dto.getEmployeeCode())) {
            throw new BadRequestException("Employee code '" + dto.getEmployeeCode() + "' already exists");
        }
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email '" + dto.getEmail() + "' is already registered to an employee");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email '" + dto.getEmail() + "' is already registered to a user");
        }

        boolean shouldCreateUser = dto.isCreateAccount() || (dto.getUsername() != null && !dto.getUsername().trim().isEmpty());
        String finalUsername = null;
        if (shouldCreateUser) {
            finalUsername = (dto.getUsername() != null && !dto.getUsername().trim().isEmpty())
                    ? dto.getUsername().trim()
                    : dto.getEmail().trim();

            if (userRepository.existsByUsername(finalUsername)) {
                throw new BadRequestException("Username '" + finalUsername + "' is already taken");
            }
            if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
                throw new BadRequestException("Account creation requires a password with at least 6 characters");
            }
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        Employee emp = new Employee();
        mapDtoToEntity(dto, emp, department);

        Employee saved = employeeRepository.save(emp);

        // Auto-create login user account if requested
        if (shouldCreateUser) {
            String rawRole = (dto.getRole() != null && !dto.getRole().trim().isEmpty())
                    ? dto.getRole().trim().toUpperCase() : "ROLE_EMPLOYEE";
            String finalRole = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;

            User user = new User(finalUsername, saved.getEmail(), passwordEncoder.encode(dto.getPassword().trim()), finalRole, saved);
            userRepository.save(user);
        }

        // Initialize SalaryStructure (using provided basic salary if specified)
        SalaryStructure salary = new SalaryStructure();
        salary.setEmployee(saved);
        BigDecimal basic = (dto.getBasicSalary() != null && dto.getBasicSalary().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getBasicSalary()
                : new BigDecimal("30000.00");
        salary.setBasicSalary(basic);
        salary.setHra(basic.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP));
        salary.setDa(basic.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP));
        salary.setBonus(BigDecimal.ZERO);
        salary.setOvertimeRate(new BigDecimal("250.00"));
        salary.setOtherAllowances(new BigDecimal("2000.00"));
        salary.setPf(basic.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP));
        salary.setTax(basic.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP));
        salary.setOtherDeductions(BigDecimal.ZERO);
        salary.calculateTotals();
        salaryStructureRepository.save(salary);

        // Auto-initialize LeaveBalance for current year
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = new LeaveBalance(saved, currentYear);
        leaveBalanceRepository.save(balance);

        auditLogService.log("EMPLOYEE_CREATED", "EMPLOYEE",
                "Created employee: " + saved.getFullName() + " (" + saved.getEmployeeCode() + ") in department: " + department.getName());

        return mapToDto(saved);
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        if (!emp.getEmployeeCode().equalsIgnoreCase(dto.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(dto.getEmployeeCode())) {
            throw new BadRequestException("Employee code '" + dto.getEmployeeCode() + "' already exists");
        }

        if (!emp.getEmail().equalsIgnoreCase(dto.getEmail()) &&
                employeeRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email '" + dto.getEmail() + "' already exists");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + dto.getDepartmentId()));

        mapDtoToEntity(dto, emp, department);
        Employee updated = employeeRepository.save(emp);

        auditLogService.log("EMPLOYEE_UPDATED", "EMPLOYEE",
                "Updated details for employee: " + updated.getFullName() + " (" + updated.getEmployeeCode() + ")");

        return mapToDto(updated);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        // Soft delete / status change to TERMINATED or delete dependencies
        emp.setStatus("INACTIVE");
        employeeRepository.save(emp);

        auditLogService.log("EMPLOYEE_DEACTIVATED", "EMPLOYEE",
                "Deactivated employee: " + emp.getFullName() + " (" + emp.getEmployeeCode() + ")");
    }

    private void mapDtoToEntity(EmployeeDto dto, Employee emp, Department department) {
        emp.setEmployeeCode(dto.getEmployeeCode().toUpperCase().trim());
        emp.setFirstName(dto.getFirstName());
        emp.setLastName(dto.getLastName());
        emp.setEmail(dto.getEmail());
        emp.setPhone(dto.getPhone());
        emp.setGender(dto.getGender());
        emp.setDob(dto.getDob());
        emp.setAddress(dto.getAddress());
        emp.setDesignation(dto.getDesignation());
        emp.setDepartment(department);
        emp.setJoiningDate(dto.getJoiningDate());
        if (dto.getStatus() != null) {
            emp.setStatus(dto.getStatus());
        }
    }

    public EmployeeDto mapToDto(Employee emp) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(emp.getId());
        dto.setEmployeeCode(emp.getEmployeeCode());
        dto.setFirstName(emp.getFirstName());
        dto.setLastName(emp.getLastName());
        dto.setEmail(emp.getEmail());
        dto.setPhone(emp.getPhone());
        dto.setGender(emp.getGender());
        dto.setDob(emp.getDob());
        dto.setAddress(emp.getAddress());
        dto.setDesignation(emp.getDesignation());
        if (emp.getDepartment() != null) {
            dto.setDepartmentId(emp.getDepartment().getId());
            dto.setDepartmentName(emp.getDepartment().getName());
        }
        dto.setJoiningDate(emp.getJoiningDate());
        dto.setStatus(emp.getStatus());

        userRepository.findByEmployeeId(emp.getId()).ifPresent(user -> {
            dto.setUsername(user.getUsername());
            dto.setRole(user.getRole());
            dto.setCreateAccount(true);
        });

        return dto;
    }
}
