package com.payroll.service;

import com.payroll.dto.DepartmentDto;
import com.payroll.exception.BadRequestException;
import com.payroll.exception.ResourceNotFoundException;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.repository.DepartmentRepository;
import com.payroll.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DepartmentDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return mapToDto(dept);
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Department code '" + dto.getCode() + "' already exists");
        }
        if (departmentRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Department name '" + dto.getName() + "' already exists");
        }

        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setCode(dto.getCode().toUpperCase().trim());
        dept.setDescription(dto.getDescription());
        dept.setManagerId(dto.getManagerId());

        Department saved = departmentRepository.save(dept);
        return mapToDto(saved);
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (!dept.getCode().equalsIgnoreCase(dto.getCode()) && departmentRepository.existsByCode(dto.getCode())) {
            throw new BadRequestException("Department code '" + dto.getCode() + "' already exists");
        }

        dept.setName(dto.getName());
        dept.setCode(dto.getCode().toUpperCase().trim());
        dept.setDescription(dto.getDescription());
        dept.setManagerId(dto.getManagerId());

        Department updated = departmentRepository.save(dept);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        List<Employee> assignedEmployees = employeeRepository.findByDepartmentId(id);
        if (!assignedEmployees.isEmpty()) {
            throw new BadRequestException("Cannot delete department with " + assignedEmployees.size() + " assigned employees. Please reassign them first.");
        }

        departmentRepository.delete(dept);
    }

    public DepartmentDto mapToDto(Department dept) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(dept.getId());
        dto.setName(dept.getName());
        dto.setCode(dept.getCode());
        dto.setDescription(dept.getDescription());
        dto.setManagerId(dept.getManagerId());

        if (dept.getManagerId() != null) {
            employeeRepository.findById(dept.getManagerId())
                    .ifPresent(m -> dto.setManagerName(m.getFullName()));
        }

        long count = employeeRepository.findByDepartmentId(dept.getId()).size();
        dto.setEmployeeCount(count);
        return dto;
    }
}
