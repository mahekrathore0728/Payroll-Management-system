package com.payroll.service;

import com.payroll.dto.AdminSetupRequest;
import com.payroll.dto.UserCreationRequest;
import com.payroll.dto.UserProfileDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.model.User;
import com.payroll.repository.DepartmentRepository;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    public long countUsers() {
        return userRepository.count();
    }

    public UserProfileDto getProfileByUsername(String usernameOrLogin) {
        User user = userRepository.findByUsernameOrEmailIgnoreCase(usernameOrLogin.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrLogin));
        return mapToProfileDto(user);
    }

    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileDto updateProfile(String username, UserProfileDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Employee emp = user.getEmployee();
        if (emp != null) {
            // Only update allowed fields by employee self-service
            if (dto.getPhone() != null) emp.setPhone(dto.getPhone());
            if (dto.getAddress() != null) emp.setAddress(dto.getAddress());
            employeeRepository.save(emp);
        }

        return mapToProfileDto(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.isEnabled()) {
            return; // Already deactivated
        }

        // 1. Self-deactivation protection
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && auth.getName().equalsIgnoreCase(user.getUsername())) {
            throw new BadRequestException("You cannot deactivate your own administrator account.");
        }

        // 2. Last active ADMIN protection
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            if (userRepository.countActiveAdmins() <= 1) {
                throw new BadRequestException("Cannot deactivate the last active administrator account.");
            }
        }

        user.setEnabled(false);
        userRepository.save(user);

        auditLogService.log("USER_DEACTIVATED", "USER_MANAGEMENT",
                "Deactivated user account: " + user.getUsername() + " (" + user.getRole() + ")");
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setEnabled(true);
        userRepository.save(user);

        auditLogService.log("USER_ACTIVATED", "USER_MANAGEMENT",
                "Activated user account: " + user.getUsername() + " (" + user.getRole() + ")");
    }

    @Transactional
    public void toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (user.isEnabled()) {
            deactivateUser(userId);
        } else {
            activateUser(userId);
        }
    }

    @Transactional
    public UserProfileDto setupInitialAdmin(AdminSetupRequest req) {
        if (userRepository.count() > 0) {
            throw new BadRequestException("Initial administrator setup has already been completed.");
        }

        String[] nameParts = req.getFullName().trim().split("\\s+", 2);
        String first = nameParts[0];
        String last = nameParts.length > 1 ? nameParts[1] : "";

        String empCode = "EMP001";
        int seq = 1;
        while (employeeRepository.findByEmployeeCode(empCode).isPresent()) {
            empCode = String.format("ADM%03d", seq++);
        }

        Employee adminEmp = new Employee();
        adminEmp.setEmployeeCode(empCode);
        adminEmp.setFirstName(first);
        adminEmp.setLastName(last.isEmpty() ? "Admin" : last);
        adminEmp.setEmail(req.getEmail().trim());
        adminEmp.setDesignation("System Administrator");
        adminEmp.setJoiningDate(LocalDate.now());
        adminEmp.setStatus("ACTIVE");
        Employee savedEmp = employeeRepository.save(adminEmp);

        User adminUser = new User(
                req.getUsername().trim(),
                req.getEmail().trim(),
                passwordEncoder.encode(req.getPassword().trim()),
                "ROLE_ADMIN",
                savedEmp
        );
        adminUser.setFullName(req.getFullName().trim());
        User savedUser = userRepository.save(adminUser);
        return mapToProfileDto(savedUser);
    }

    @Transactional
    public UserProfileDto createUser(UserCreationRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Work email is required");
        }
        if (req.getPassword() == null || req.getPassword().trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }
        if (req.getConfirmPassword() != null && !req.getConfirmPassword().trim().isEmpty()) {
            if (!req.getPassword().equals(req.getConfirmPassword())) {
                throw new BadRequestException("Passwords do not match");
            }
        }
        if (userRepository.existsByUsername(req.getUsername().trim())) {
            throw new BadRequestException("Username '" + req.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(req.getEmail().trim())) {
            throw new BadRequestException("Email '" + req.getEmail() + "' is already in use");
        }

        String rawRole = (req.getRole() != null && !req.getRole().trim().isEmpty())
                ? req.getRole().trim().toUpperCase() : "ROLE_EMPLOYEE";
        String finalRole = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;

        // Role-based validation
        if ("ROLE_MANAGER".equals(finalRole)) {
            if (req.getDepartmentId() == null) {
                throw new BadRequestException("Department is required for Manager role");
            }
            if (req.getEmployeeId() == null) {
                throw new BadRequestException("Employee is required for Manager role");
            }
        } else if ("ROLE_EMPLOYEE".equals(finalRole)) {
            if (req.getDepartmentId() == null) {
                throw new BadRequestException("Department is required for Employee role");
            }
            if (req.getEmployeeId() == null) {
                throw new BadRequestException("Employee is required for Employee role");
            }
        }

        Employee emp = null;
        Department dept = null;

        if (req.getEmployeeId() != null) {
            emp = employeeRepository.findById(req.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + req.getEmployeeId()));

            if (userRepository.findByEmployeeId(emp.getId()).isPresent()) {
                throw new BadRequestException("Employee '" + emp.getEmployeeCode() + " - " + emp.getFirstName() + " " + emp.getLastName() + "' already has a linked login account");
            }

            // Department validation for linked employee
            if (req.getDepartmentId() != null) {
                dept = departmentRepository.findById(req.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));

                if (emp.getDepartment() != null && !emp.getDepartment().getId().equals(req.getDepartmentId())) {
                    throw new BadRequestException("Selected employee (" + emp.getEmployeeCode() + ") belongs to department '" + emp.getDepartment().getName() + "', which does not match the selected department '" + dept.getName() + "'.");
                }
            } else if (emp.getDepartment() != null) {
                dept = emp.getDepartment();
            }
        } else if (req.getDepartmentId() != null) {
            dept = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + req.getDepartmentId()));
        }

        boolean enabled = true;
        if (req.getStatus() != null && req.getStatus().equalsIgnoreCase("INACTIVE")) {
            enabled = false;
        }

        String fullName = req.getFullName() != null && !req.getFullName().trim().isEmpty()
                ? req.getFullName().trim()
                : (emp != null ? (emp.getFirstName() + " " + emp.getLastName()).trim() : req.getUsername().trim());

        User user = new User(
                req.getUsername().trim(),
                req.getEmail().trim(),
                passwordEncoder.encode(req.getPassword().trim()),
                finalRole,
                emp
        );
        user.setFullName(fullName);
        user.setDepartment(dept != null ? dept : (emp != null ? emp.getDepartment() : null));
        user.setEnabled(enabled);

        User saved = userRepository.save(user);

        auditLogService.log("USER_CREATED", "USER_MANAGEMENT",
                "Created user account: " + saved.getUsername() + " (" + saved.getRole() + ")" +
                        (saved.getEmployee() != null ? " linked to employee: " + saved.getEmployee().getFullName() : ""));

        return mapToProfileDto(saved);
    }

    public UserProfileDto mapToProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setAccountStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");
        dto.setStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE");

        if (user.getEmployee() != null) {
            Employee e = user.getEmployee();
            dto.setEmployeeId(e.getId());
            dto.setEmployeeCode(e.getEmployeeCode());
            dto.setFirstName(e.getFirstName());
            dto.setLastName(e.getLastName());
            dto.setEmail(user.getEmail() != null ? user.getEmail() : e.getEmail());
            dto.setPhone(e.getPhone());
            dto.setGender(e.getGender());
            dto.setDob(e.getDob());
            dto.setAddress(e.getAddress());
            dto.setDesignation(e.getDesignation());
            if (e.getDepartment() != null) {
                dto.setDepartmentId(e.getDepartment().getId());
                dto.setDepartmentName(e.getDepartment().getName());
            } else if (user.getDepartment() != null) {
                dto.setDepartmentId(user.getDepartment().getId());
                dto.setDepartmentName(user.getDepartment().getName());
            }
            dto.setJoiningDate(e.getJoiningDate());
            dto.setEmployeeStatus(e.getStatus());
        } else {
            String fullName = user.getFullName() != null && !user.getFullName().trim().isEmpty()
                    ? user.getFullName().trim() : user.getUsername();
            String[] parts = fullName.split("\\s+", 2);
            dto.setFirstName(parts[0]);
            dto.setLastName(parts.length > 1 ? parts[1] : "");
            dto.setEmail(user.getEmail() != null ? user.getEmail() : user.getUsername() + "@company.com");
            dto.setDesignation(user.getRole().replace("ROLE_", "") + " Administrator");
            if (user.getDepartment() != null) {
                dto.setDepartmentId(user.getDepartment().getId());
                dto.setDepartmentName(user.getDepartment().getName());
            }
            dto.setEmployeeStatus("N/A");
        }
        return dto;
    }
}
