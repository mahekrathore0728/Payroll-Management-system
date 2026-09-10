package com.payroll;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroll.dto.*;
import com.payroll.model.Department;
import com.payroll.model.User;
import com.payroll.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/payroll_fresh_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FreshDatabaseComprehensiveVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SalaryStructureRepository salaryStructureRepository;

    @Autowired
    private PayrollRecordRepository payrollRecordRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private javax.sql.DataSource dataSource;

    private static Long savedDepartmentId;
    private static Long managerEmployeeId;
    private static Long staffEmployeeId;
    private static Long payrollRecordId;

    @BeforeAll
    static void prepareFreshDatabase(@Autowired javax.sql.DataSource ds,
                                     @Autowired NotificationRepository notifRepo,
                                     @Autowired PayrollRecordRepository payRepo,
                                     @Autowired LeaveRequestRepository leaveReqRepo,
                                     @Autowired LeaveBalanceRepository leaveBalRepo,
                                     @Autowired AttendanceRecordRepository attRepo,
                                     @Autowired SalaryStructureRepository salRepo,
                                     @Autowired UserRepository userRepo,
                                     @Autowired EmployeeRepository empRepo,
                                     @Autowired DepartmentRepository deptRepo) throws Exception {
        try (var conn = ds.getConnection()) {
            String catalog = conn.getCatalog();
            if (!"payroll_fresh_db".equalsIgnoreCase(catalog)) {
                throw new IllegalStateException("SAFETY CHECK: FreshDatabaseComprehensiveVerificationTest can ONLY run against 'payroll_fresh_db'. Found catalog: " + catalog);
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
    }

    @Test
    @Order(1)
    @DisplayName("1. Fresh Database initially starts with zero business records")
    void test1_FreshDatabaseStartsEmpty() {
        assertEquals(0, userRepository.count(), "Users table must be completely empty on fresh DB");
        assertEquals(0, employeeRepository.count(), "Employees table must be completely empty on fresh DB");
        assertEquals(0, departmentRepository.count(), "Departments table must be completely empty on fresh DB");
        assertEquals(0, salaryStructureRepository.count(), "Salary structures table must be completely empty on fresh DB");
        assertEquals(0, payrollRecordRepository.count(), "Payroll records table must be completely empty on fresh DB");
        assertEquals(0, attendanceRecordRepository.count(), "Attendance table must be completely empty on fresh DB");
        assertEquals(0, leaveRequestRepository.count(), "Leave requests table must be completely empty on fresh DB");
        assertEquals(0, notificationRepository.count(), "Notifications table must be completely empty on fresh DB");
    }

    @Test
    @Order(2)
    @DisplayName("2. Setup-status endpoint reports setupRequired=true and userCount=0")
    void test2_SetupStatusReportsZeroUsers() throws Exception {
        mockMvc.perform(get("/api/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.setupRequired").value(true))
                .andExpect(jsonPath("$.data.userCount").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("3. First-time admin setup provisions super admin with BCrypt password and logs in")
    void test3_FirstTimeAdminSetup() throws Exception {
        AdminSetupRequest req = new AdminSetupRequest();
        req.setFullName("Super Administrator");
        req.setEmail("super.admin@company.com");
        req.setUsername("super_admin");
        req.setPassword("SuperAdminPass@2026");

        MvcResult result = mockMvc.perform(post("/api/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("super_admin"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"))
                .andReturn();

        // Verify in MySQL
        User admin = userRepository.findByUsername("super_admin").orElseThrow();
        assertNotNull(admin.getPassword());
        assertTrue(admin.getPassword().startsWith("$2a$") || admin.getPassword().startsWith("$2b$"),
                "Password must be stored as a BCrypt hash");
        assertNotEquals("SuperAdminPass@2026", admin.getPassword());
        assertEquals("ROLE_ADMIN", admin.getRole());
        assertNotNull(admin.getEmployee());
        assertEquals("System Administrator", admin.getEmployee().getDesignation());
    }

    @Test
    @Order(4)
    @DisplayName("4. Setup endpoint is permanently locked down once the first admin exists")
    void test4_SetupLockedAfterFirstAdmin() throws Exception {
        AdminSetupRequest secondReq = new AdminSetupRequest();
        secondReq.setFullName("Imposter Admin");
        secondReq.setEmail("imposter@company.com");
        secondReq.setUsername("imposter_admin");
        secondReq.setPassword("FakePassword123");

        mockMvc.perform(post("/api/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Confirm setup-status now reports setupRequired=false
        mockMvc.perform(get("/api/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupRequired").value(false))
                .andExpect(jsonPath("$.data.userCount").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("5. Authentication succeeds using registered username + password")
    void test5_LoginWithUsername() throws Exception {
        LoginRequest req = new LoginRequest("super_admin", "SuperAdminPass@2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("super_admin"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Authentication succeeds using registered work email + password")
    void test6_LoginWithEmail() throws Exception {
        LoginRequest req = new LoginRequest("super.admin@company.com", "SuperAdminPass@2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("super_admin"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @Order(7)
    @DisplayName("7. Authentication rejects incorrect password")
    void test7_LoginRejectsBadPassword() throws Exception {
        LoginRequest req = new LoginRequest("super_admin", "WrongPassword@2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private MockHttpSession loginAs(String principal, String password) throws Exception {
        LoginRequest req = new LoginRequest(principal, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    @Order(8)
    @DisplayName("8. Admin dynamically creates a department")
    void test8_CreateDepartment() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        DepartmentDto dept = new DepartmentDto();
        dept.setName("Operations");
        dept.setCode("OPS");
        dept.setDescription("Logistics & Operations");

        MvcResult res = mockMvc.perform(post("/api/departments")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Operations"))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        savedDepartmentId = json.get("data").get("id").asLong();
        assertTrue(savedDepartmentId > 0);
        assertEquals(1, departmentRepository.count());
    }

    @Test
    @Order(9)
    @DisplayName("9. Admin dynamically creates Manager and Employee records")
    void test9_CreateEmployees() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // Create Manager Employee
        EmployeeDto mgrEmp = new EmployeeDto();
        mgrEmp.setEmployeeCode("EMP101");
        mgrEmp.setFirstName("Sarah");
        mgrEmp.setLastName("Connor");
        mgrEmp.setEmail("sarah.connor@company.com");
        mgrEmp.setPhone("9876543210");
        mgrEmp.setGender("Female");
        mgrEmp.setJoiningDate(LocalDate.of(2025, 1, 15));
        mgrEmp.setDepartmentId(savedDepartmentId);
        mgrEmp.setDesignation("Operations Lead");
        mgrEmp.setStatus("ACTIVE");

        MvcResult mgrRes = mockMvc.perform(post("/api/employees")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrEmp)))
                .andExpect(status().isCreated())
                .andReturn();
        managerEmployeeId = objectMapper.readTree(mgrRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Create Staff Employee
        EmployeeDto staffEmp = new EmployeeDto();
        staffEmp.setEmployeeCode("EMP102");
        staffEmp.setFirstName("John");
        staffEmp.setLastName("Connor");
        staffEmp.setEmail("john.connor@company.com");
        staffEmp.setPhone("9876543211");
        staffEmp.setGender("Male");
        staffEmp.setJoiningDate(LocalDate.of(2025, 3, 1));
        staffEmp.setDepartmentId(savedDepartmentId);
        staffEmp.setDesignation("Operations Specialist");
        staffEmp.setStatus("ACTIVE");

        MvcResult staffRes = mockMvc.perform(post("/api/employees")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffEmp)))
                .andExpect(status().isCreated())
                .andReturn();
        staffEmployeeId = objectMapper.readTree(staffRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 3 total employees now: Super Admin + Sarah + John
        assertEquals(3, employeeRepository.count());
    }

    @Test
    @Order(10)
    @DisplayName("10. Admin dynamically creates and links user accounts for Manager and Employee")
    void test10_CreateUserAccounts() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // Manager user
        UserCreationRequest mgrUser = new UserCreationRequest();
        mgrUser.setFullName("Sarah Connor");
        mgrUser.setUsername("sarah_mgr");
        mgrUser.setEmail("sarah.connor@company.com");
        mgrUser.setPassword("SarahManager@2026");
        mgrUser.setConfirmPassword("SarahManager@2026");
        mgrUser.setRole("ROLE_MANAGER");
        mgrUser.setDepartmentId(savedDepartmentId);
        mgrUser.setEmployeeId(managerEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ROLE_MANAGER"));

        // Employee user
        UserCreationRequest empUser = new UserCreationRequest();
        empUser.setFullName("John Connor");
        empUser.setUsername("john_emp");
        empUser.setEmail("john.connor@company.com");
        empUser.setPassword("JohnEmployee@2026");
        empUser.setConfirmPassword("JohnEmployee@2026");
        empUser.setRole("ROLE_EMPLOYEE");
        empUser.setDepartmentId(savedDepartmentId);
        empUser.setEmployeeId(staffEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"));

        assertEquals(3, userRepository.count());
    }

    @Test
    @Order(11)
    @DisplayName("11. Role-based login routes MANAGER to /app.html#manager-dashboard and EMPLOYEE to /app.html#employee-dashboard")
    void test11_RoleBasedLoginRedirection() throws Exception {
        // Manager Login
        LoginRequest mgrReq = new LoginRequest("sarah_mgr", "SarahManager@2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#manager-dashboard"));

        // Employee Login
        LoginRequest empReq = new LoginRequest("john_emp", "JohnEmployee@2026");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));
    }

    @Test
    @Order(12)
    @DisplayName("12. Server-side role isolation blocks unauthorized access")
    void test12_ServerSideSecurityEnforcement() throws Exception {
        MockHttpSession employeeSession = loginAs("john_emp", "JohnEmployee@2026");

        // Employee cannot view user management
        mockMvc.perform(get("/api/admin/users").session(employeeSession))
                .andExpect(status().isForbidden());

        // Employee cannot create departments
        DepartmentDto fakeDept = new DepartmentDto();
        fakeDept.setName("Unauthorized Dept");
        fakeDept.setCode("UNAUTH");
        mockMvc.perform(post("/api/departments")
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fakeDept)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @DisplayName("13. Dynamic dashboard statistics reflect real database data")
    void test13_DynamicDashboardStats() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        mockMvc.perform(get("/api/dashboard/stats").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployees").value(3))
                .andExpect(jsonPath("$.data.totalDepartments").value(1));
    }

    @Test
    @Order(14)
    @DisplayName("14. Dynamic attendance marking and summary calculation")
    void test14_DynamicAttendance() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        AttendanceDto att = new AttendanceDto();
        att.setEmployeeId(staffEmployeeId);
        att.setDate(LocalDate.now());
        att.setStatus("PRESENT");
        att.setCheckInTime(LocalTime.of(9, 0));
        att.setCheckOutTime(LocalTime.of(18, 0));
        att.setRemarks("On time");

        mockMvc.perform(post("/api/attendance/mark")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(att)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));

        assertEquals(1, attendanceRecordRepository.count());
    }

    @Test
    @Order(15)
    @DisplayName("15. Dynamic leave submission and manager review workflow")
    void test15_DynamicLeaveWorkflow() throws Exception {
        MockHttpSession employeeSession = loginAs("john_emp", "JohnEmployee@2026");

        // Employee applies for leave
        LeaveRequestDto leaveReq = new LeaveRequestDto();
        leaveReq.setLeaveType("CASUAL");
        leaveReq.setStartDate(LocalDate.now().plusDays(5));
        leaveReq.setEndDate(LocalDate.now().plusDays(6));
        leaveReq.setReason("Personal family matters");

        MvcResult applyRes = mockMvc.perform(post("/api/employee/me/leaves")
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        Long leaveId = objectMapper.readTree(applyRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Manager reviews and approves leave
        MockHttpSession managerSession = loginAs("sarah_mgr", "SarahManager@2026");

        LeaveActionRequest action = new LeaveActionRequest();
        action.setStatus("APPROVED");
        action.setManagerRemarks("Approved by Operations Lead");

        mockMvc.perform(post("/api/leaves/" + leaveId + "/review")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertEquals(1, leaveRequestRepository.count());
    }

    @Test
    @Order(16)
    @DisplayName("16. Dynamic salary structure assignment")
    void test16_DynamicSalaryAssignment() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        SalaryStructureDto salary = new SalaryStructureDto();
        salary.setEmployeeId(staffEmployeeId);
        salary.setBasicSalary(new BigDecimal("60000.00"));
        salary.setHra(new BigDecimal("15000.00"));
        salary.setDa(new BigDecimal("5000.00"));
        salary.setBonus(new BigDecimal("5000.00"));
        salary.setPf(new BigDecimal("7200.00"));
        salary.setTax(new BigDecimal("4800.00"));

        mockMvc.perform(post("/api/salaries")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grossSalary").value(85000.00))
                .andExpect(jsonPath("$.data.totalDeductions").value(12000.00))
                .andExpect(jsonPath("$.data.netSalary").value(73000.00));

        assertEquals(2, salaryStructureRepository.count(), "Both employees have valid salary structures");
    }

    @Test
    @Order(17)
    @DisplayName("17. Dynamic payroll processing and pixel-perfect PDF payslip download")
    void test17_DynamicPayrollAndPayslipPdf() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        PayrollProcessRequest payReq = new PayrollProcessRequest();
        payReq.setEmployeeId(staffEmployeeId);
        payReq.setMonth(LocalDate.now().getMonthValue());
        payReq.setYear(LocalDate.now().getYear());
        payReq.setOvertimeHours(BigDecimal.ZERO);
        payReq.setAdditionalBonus(BigDecimal.ZERO);
        payReq.setAdditionalDeductions(BigDecimal.ZERO);
        payReq.setPaymentMethod("BANK_TRANSFER");

        MvcResult payRes = mockMvc.perform(post("/api/payroll/process")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andReturn();

        payrollRecordId = objectMapper.readTree(payRes.getResponse().getContentAsString()).get("data").get("id").asLong();
        assertTrue(payrollRecordId > 0);

        // Download Payslip PDF as Admin
        MvcResult pdfRes = mockMvc.perform(get("/api/payroll/" + payrollRecordId + "/payslip/pdf")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        byte[] pdfBytes = pdfRes.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 500, "Generated PDF payslip must contain real byte content");

        // Download Payslip PDF as the Employee themselves (ESS)
        MockHttpSession empSession = loginAs("john_emp", "JohnEmployee@2026");
        mockMvc.perform(get("/api/payroll/" + payrollRecordId + "/payslip/pdf").session(empSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @Order(18)
    @DisplayName("18. Dynamic notifications and report endpoints")
    void test18_DynamicNotificationsAndReports() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // Notifications
        mockMvc.perform(get("/api/notifications").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Payroll Report
        mockMvc.perform(get("/api/reports/payroll").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Departments Report
        mockMvc.perform(get("/api/reports/departments").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Attendance Report
        mockMvc.perform(get("/api/reports/attendance").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Leaves Report
        mockMvc.perform(get("/api/reports/leaves").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(19)
    @DisplayName("19. ADMIN can create HR user without employee profile and link department")
    void test19_AdminCreatesHrUserWithoutEmployee() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        UserCreationRequest hrUser = new UserCreationRequest();
        hrUser.setFullName("Toby Flenderson");
        hrUser.setUsername("toby_hr");
        hrUser.setEmail("toby.hr@company.com");
        hrUser.setPassword("TobyPass@2026");
        hrUser.setConfirmPassword("TobyPass@2026");
        hrUser.setRole("ROLE_HR");
        hrUser.setDepartmentId(savedDepartmentId);
        hrUser.setEmployeeId(null);
        hrUser.setStatus("ACTIVE");

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hrUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("toby_hr"))
                .andExpect(jsonPath("$.data.role").value("ROLE_HR"))
                .andExpect(jsonPath("$.data.departmentId").value(savedDepartmentId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        User toby = userRepository.findByUsername("toby_hr").orElseThrow();
        assertEquals("Toby Flenderson", toby.getFullName());
        assertNull(toby.getEmployee());
        assertNotNull(toby.getDepartment());
    }

    @Test
    @Order(20)
    @DisplayName("20. Role-based constraints: MANAGER and EMPLOYEE require both Department and Employee")
    void test20_RoleBasedConstraints() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // Manager missing department
        UserCreationRequest req1 = new UserCreationRequest();
        req1.setFullName("Test Manager");
        req1.setUsername("test_mgr_bad");
        req1.setEmail("mgr.bad@company.com");
        req1.setPassword("Password@123");
        req1.setConfirmPassword("Password@123");
        req1.setRole("ROLE_MANAGER");
        req1.setDepartmentId(null);
        req1.setEmployeeId(managerEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Department is required for Manager role"));

        // Manager missing employee
        UserCreationRequest req2 = new UserCreationRequest();
        req2.setFullName("Test Manager 2");
        req2.setUsername("test_mgr_bad2");
        req2.setEmail("mgr.bad2@company.com");
        req2.setPassword("Password@123");
        req2.setConfirmPassword("Password@123");
        req2.setRole("ROLE_MANAGER");
        req2.setDepartmentId(savedDepartmentId);
        req2.setEmployeeId(null);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee is required for Manager role"));

        // Employee missing department
        UserCreationRequest req3 = new UserCreationRequest();
        req3.setFullName("Test Staff");
        req3.setUsername("test_staff_bad");
        req3.setEmail("staff.bad@company.com");
        req3.setPassword("Password@123");
        req3.setConfirmPassword("Password@123");
        req3.setRole("ROLE_EMPLOYEE");
        req3.setDepartmentId(null);
        req3.setEmployeeId(staffEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Department is required for Employee role"));
    }

    @Test
    @Order(21)
    @DisplayName("21. Department and Employee mismatch fails with 400")
    void test21_DepartmentEmployeeMismatchFails() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // Create a 2nd department
        Department d2 = new Department();
        d2.setCode("FINANCE_TEST");
        d2.setName("Finance Dept");
        d2.setDescription("Finance Operations");
        d2 = departmentRepository.save(d2);

        // Try to link staffEmployeeId (who is in Engineering) with Finance department
        UserCreationRequest mismatchReq = new UserCreationRequest();
        mismatchReq.setFullName("Mismatch User");
        mismatchReq.setUsername("mismatch_usr");
        mismatchReq.setEmail("mismatch.usr@company.com");
        mismatchReq.setPassword("Password@123");
        mismatchReq.setConfirmPassword("Password@123");
        mismatchReq.setRole("ROLE_EMPLOYEE");
        mismatchReq.setDepartmentId(d2.getId());
        mismatchReq.setEmployeeId(staffEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(22)
    @DisplayName("22. Linking an Employee who already has a user account fails with 400")
    void test22_DuplicateEmployeeLinkingFails() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");

        // staffEmployeeId is already linked to john_emp
        UserCreationRequest dupReq = new UserCreationRequest();
        dupReq.setFullName("Duplicate John");
        dupReq.setUsername("john_dup");
        dupReq.setEmail("john.dup@company.com");
        dupReq.setPassword("Password@123");
        dupReq.setConfirmPassword("Password@123");
        dupReq.setRole("ROLE_EMPLOYEE");
        dupReq.setDepartmentId(savedDepartmentId);
        dupReq.setEmployeeId(staffEmployeeId);

        mockMvc.perform(post("/api/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(23)
    @DisplayName("23. Password BCrypt hashing is verified and login works with username or email")
    void test23_BcryptHashedAndLoginWorks() throws Exception {
        User toby = userRepository.findByUsername("toby_hr").orElseThrow();
        assertTrue(toby.getPassword().startsWith("$2"), "Password must be BCrypt hashed");
        assertNotEquals("TobyPass@2026", toby.getPassword(), "Plaintext password must not be stored");
        assertTrue(passwordEncoder.matches("TobyPass@2026", toby.getPassword()));

        // Login by username
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("toby_hr", "TobyPass@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_HR"));

        // Login by work email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("toby.hr@company.com", "TobyPass@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_HR"));
    }

    @Test
    @Order(24)
    @DisplayName("24. ADMIN cannot deactivate self and last active ADMIN is protected")
    void test24_SelfAndLastAdminProtection() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");
        User superAdmin = userRepository.findByUsername("super_admin").orElseThrow();

        // Self-deactivation attempt via deactivate endpoint
        mockMvc.perform(put("/api/admin/users/" + superAdmin.getId() + "/deactivate")
                        .session(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot deactivate your own administrator account."));

        // Also test toggle-status self-deactivation
        mockMvc.perform(put("/api/admin/users/" + superAdmin.getId() + "/toggle-status")
                        .session(adminSession))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot deactivate your own administrator account."));
    }

    @Test
    @Order(25)
    @DisplayName("25. Deactivating user blocks login with clean 401 message")
    void test25_DeactivateUserBlocksLoginWithClean401() throws Exception {
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");
        User john = userRepository.findByUsername("john_emp").orElseThrow();

        // Deactivate John
        mockMvc.perform(put("/api/admin/users/" + john.getId() + "/deactivate")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User account deactivated successfully"));

        // Verify John is now disabled in DB
        User deactivatedJohn = userRepository.findById(john.getId()).orElseThrow();
        assertFalse(deactivatedJohn.isEnabled());

        // Attempt login as John
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("john_emp", "JohnEmployee@2026"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This account has been deactivated. Please contact your administrator."));
    }

    @Test
    @Order(26)
    @DisplayName("26. Deactivation preserves all historical business data and reactivation restores access")
    void test26_DeactivationPreservesDataAndReactivationWorks() throws Exception {
        // Verify all of John's business records remain completely intact
        assertTrue(employeeRepository.findById(staffEmployeeId).isPresent());
        assertTrue(salaryStructureRepository.findByEmployeeId(staffEmployeeId).isPresent());
        assertTrue(attendanceRecordRepository.findByEmployeeIdOrderByDateDesc(staffEmployeeId).size() > 0);
        assertTrue(leaveRequestRepository.findByEmployeeIdOrderByAppliedAtDesc(staffEmployeeId).size() > 0);
        assertTrue(payrollRecordRepository.findById(payrollRecordId).isPresent());

        // Reactivate John as Admin
        MockHttpSession adminSession = loginAs("super_admin", "SuperAdminPass@2026");
        User john = userRepository.findByUsername("john_emp").orElseThrow();

        mockMvc.perform(put("/api/admin/users/" + john.getId() + "/activate")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User account activated successfully"));

        User reactivatedJohn = userRepository.findById(john.getId()).orElseThrow();
        assertTrue(reactivatedJohn.isEnabled());

        // John can login again immediately
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("john_emp", "JohnEmployee@2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));
    }
}
