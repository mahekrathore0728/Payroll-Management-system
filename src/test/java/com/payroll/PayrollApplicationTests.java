package com.payroll;

import com.payroll.repository.DepartmentRepository;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PayrollApplicationTests {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoadsAndRepositoriesAreWired() {
        assertNotNull(employeeRepository);
        assertNotNull(departmentRepository);
        assertNotNull(userRepository);

        // DataInitializer populates sample data on startup
        assertTrue(employeeRepository.count() >= 0);
        assertTrue(departmentRepository.count() >= 0);
        assertTrue(userRepository.count() >= 0);
    }
}