package com.payroll;

import com.payroll.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/payroll_fresh_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
})
public class FreshDatabaseInitTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private NotificationRepository notifRepo;
    @Autowired
    private PayrollRecordRepository payRepo;
    @Autowired
    private LeaveRequestRepository leaveReqRepo;
    @Autowired
    private LeaveBalanceRepository leaveBalRepo;
    @Autowired
    private AttendanceRecordRepository attRepo;
    @Autowired
    private SalaryStructureRepository salRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private EmployeeRepository empRepo;
    @Autowired
    private DepartmentRepository deptRepo;

    @Test
    void preparePristineFreshDatabase() throws Exception {
        try (var conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            if (!"payroll_fresh_db".equalsIgnoreCase(catalog)) {
                throw new IllegalStateException("SAFETY CHECK: Only payroll_fresh_db can be cleared! Current catalog: " + catalog);
            }
        }
        notifRepo.deleteAll();
        payRepo.deleteAll();
        leaveReqRepo.deleteAll();
        leaveBalRepo.deleteAll();
        attRepo.deleteAll();
        salRepo.deleteAll();
        userRepo.deleteAll();
        empRepo.deleteAll();
        deptRepo.deleteAll();

        assertEquals(0, userRepo.count(), "Users count must be 0");
        assertEquals(0, empRepo.count(), "Employees count must be 0");
        assertEquals(0, deptRepo.count(), "Departments count must be 0");
        assertEquals(0, salRepo.count(), "Salaries count must be 0");
        assertEquals(0, payRepo.count(), "Payrolls count must be 0");
        assertEquals(0, attRepo.count(), "Attendance count must be 0");
        assertEquals(0, leaveReqRepo.count(), "Leaves count must be 0");
        assertEquals(0, notifRepo.count(), "Notifications count must be 0");
    }
}
