package com.payroll;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroll.dto.EmployeeDto;
import com.payroll.dto.LoginRequest;
import com.payroll.model.Department;
import com.payroll.model.Employee;
import com.payroll.model.SalaryStructure;
import com.payroll.model.User;
import com.payroll.repository.DepartmentRepository;
import com.payroll.repository.EmployeeRepository;
import com.payroll.repository.SalaryStructureRepository;
import com.payroll.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DatabaseAuthAndSecurityTests {

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
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = "SecureSuitePass@2026!";

    private User createOrGetTestUser(String username, String email, String role, Employee employee) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User(username, email, passwordEncoder.encode(TEST_PASSWORD), role, employee);
            u.setEnabled(true);
            return userRepository.save(u);
        });
    }

    private Department createOrGetTestDepartment() {
        return departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department("Engineering", "ENG", "Engineering & Technology", null);
            return departmentRepository.save(d);
        });
    }

    private Employee createOrGetTestEmployee(String code, String first, String last, String email, Department dept) {
        return employeeRepository.findByEmployeeCode(code).orElseGet(() -> {
            Employee e = new Employee();
            e.setEmployeeCode(code);
            e.setFirstName(first);
            e.setLastName(last);
            e.setEmail(email);
            e.setPhone("9876543999");
            e.setGender("Other");
            e.setJoiningDate(LocalDate.now());
            e.setDepartment(dept);
            e.setDesignation("Test Specialist");
            e.setStatus("ACTIVE");
            return employeeRepository.save(e);
        });
    }

    @Test
    @DisplayName("1. Setup status returns valid information without requiring authentication")
    void testSetupStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupRequired").isBoolean())
                .andExpect(jsonPath("$.data.userCount").isNumber());
    }

    @Test
    @DisplayName("2. Setup flow is rejected when database users already exist")
    void testSetupAdminBlockedWhenUsersExist() throws Exception {
        com.payroll.dto.AdminSetupRequest request = new com.payroll.dto.AdminSetupRequest();
        request.setFullName("Secondary Setup Admin");
        request.setEmail("secondary.setup@company.com");
        request.setUsername("secondary_setup_admin");
        request.setPassword("SecureAdminPass@2026");

        mockMvc.perform(post("/api/auth/setup-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("3. Database authentication succeeds using registered username")
    void testLoginWithUsername() throws Exception {
        User testUser = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest request = new LoginRequest(testUser.getUsername(), TEST_PASSWORD, false);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andReturn();

        assertNotNull(result.getRequest().getSession(false), "Session must be established on successful login");
    }

    @Test
    @DisplayName("4. Database authentication succeeds using registered work email")
    void testLoginWithEmail() throws Exception {
        User testUser = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest request = new LoginRequest(testUser.getEmail(), TEST_PASSWORD, false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("5. Database authentication rejects invalid credentials with 401 Unauthorized")
    void testLoginWithInvalidPassword() throws Exception {
        User testUser = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest request = new LoginRequest(testUser.getUsername(), "WrongPassword999!", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("6. Database authentication rejects non-existent username with 401 Unauthorized")
    void testLoginWithNonExistentUser() throws Exception {
        LoginRequest request = new LoginRequest("non_existent_auth_user_999", "AnyPassword@2026", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("7. Passwords in MySQL database are BCrypt hashed and never stored in plain text")
    void testPasswordsAreBCryptHashedInDatabase() {
        List<User> allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty(), "Users must exist in the database");

        for (User user : allUsers) {
            String hash = user.getPassword();
            assertNotNull(hash, "Password hash cannot be null for user: " + user.getUsername());
            assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                    "Password hash must be a valid BCrypt hash for user: " + user.getUsername());
            assertTrue(hash.length() >= 50, "BCrypt hash length must be at least 50 characters");
        }
    }

    @Test
    @DisplayName("8. Passwords are never serialized or exposed in DTO responses")
    void testPasswordNeverExposedInDto() throws Exception {
        User testAdmin = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest adminLogin = new LoginRequest(testAdmin.getUsername(), TEST_PASSWORD, false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        MvcResult empResult = mockMvc.perform(get("/api/employees").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String content = empResult.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(content);
        JsonNode dataArray = rootNode.get("data");

        assertTrue(dataArray.isArray(), "Employees response should contain data array");
        for (JsonNode empNode : dataArray) {
            assertFalse(empNode.hasNonNull("password"), "Employee DTO must never leak password field");
        }
    }

    @Test
    @DisplayName("9. Dynamic employee creation creates linked User account and initial salary structure")
    void testDynamicEmployeeCreationWithAccountAndSalary() throws Exception {
        User testAdmin = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest adminLogin = new LoginRequest(testAdmin.getUsername(), TEST_PASSWORD, false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        Department department = createOrGetTestDepartment();

        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000);
        String uniqueCode = "DYN" + uniqueSuffix;
        String uniqueEmail = "dynamic." + uniqueSuffix + "@company.com";
        String uniqueUsername = "dynamic" + uniqueSuffix;
        String dynPassword = "SecureDynPass@2026";

        java.util.Map<String, Object> empPayload = new java.util.HashMap<>();
        empPayload.put("employeeCode", uniqueCode);
        empPayload.put("firstName", "Dynamic");
        empPayload.put("lastName", "Tester" + uniqueSuffix);
        empPayload.put("email", uniqueEmail);
        empPayload.put("phone", "9876" + (uniqueSuffix.length() >= 6 ? uniqueSuffix.substring(0, 6) : "123456"));
        empPayload.put("gender", "Other");
        empPayload.put("joiningDate", LocalDate.now().toString());
        empPayload.put("departmentId", department.getId());
        empPayload.put("designation", "Quality Assurance Engineer");
        empPayload.put("status", "ACTIVE");
        empPayload.put("createAccount", true);
        empPayload.put("username", uniqueUsername);
        empPayload.put("password", dynPassword);
        empPayload.put("role", "ROLE_EMPLOYEE");
        empPayload.put("basicSalary", 55000.00);

        mockMvc.perform(post("/api/employees")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Optional<Employee> persistedEmp = employeeRepository.findByEmployeeCode(uniqueCode);
        assertTrue(persistedEmp.isPresent(), "Persisted employee must be found in database");

        Optional<User> persistedUser = userRepository.findByUsername(uniqueUsername);
        assertTrue(persistedUser.isPresent(), "Linked user must be found in database");
        User user = persistedUser.get();
        assertEquals(uniqueEmail, user.getEmail());
        assertEquals("ROLE_EMPLOYEE", user.getRole());
        assertTrue(passwordEncoder.matches(dynPassword, user.getPassword()),
                "BCrypt password must verify successfully against provided plaintext");

        Optional<SalaryStructure> persistedSalary = salaryStructureRepository.findByEmployeeId(persistedEmp.get().getId());
        assertTrue(persistedSalary.isPresent(), "Salary structure must be configured automatically");
        assertEquals(0, new BigDecimal("55000.00").compareTo(persistedSalary.get().getBasicSalary()));

        // Verify login with username
        LoginRequest newLogin = new LoginRequest(uniqueUsername, dynPassword, false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(uniqueUsername))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));

        // Verify login with email
        LoginRequest newLoginByEmail = new LoginRequest(uniqueEmail, dynPassword, false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginByEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));
    }

    @Test
    @DisplayName("10. Role isolation: Employee cannot access restricted admin endpoints")
    void testRoleIsolationForEmployee() throws Exception {
        User testEmp = createOrGetTestUser("test_emp_iso", "test_emp_iso@company.com", "ROLE_EMPLOYEE", null);
        LoginRequest employeeLogin = new LoginRequest(testEmp.getUsername(), TEST_PASSWORD, false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeLogin)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession employeeSession = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/api/admin/users").session(employeeSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/payroll/process")
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Employee self-service: Employee can access own salary info via /api/salaries/me")
    void testEmployeeSelfSalaryAccess() throws Exception {
        Department dept = createOrGetTestDepartment();
        Employee emp = createOrGetTestEmployee("EMPTEST01", "Self", "Tester", "self.tester@company.com", dept);
        User testEmp = createOrGetTestUser("test_emp_self", "test_emp_self@company.com", "ROLE_EMPLOYEE", emp);

        if (salaryStructureRepository.findByEmployeeId(emp.getId()).isEmpty()) {
            SalaryStructure s = new SalaryStructure();
            s.setEmployee(emp);
            s.setBasicSalary(new BigDecimal("50000.00"));
            s.setHra(new BigDecimal("20000.00"));
            s.setDa(new BigDecimal("5000.00"));
            s.setPf(new BigDecimal("6000.00"));
            s.setTax(new BigDecimal("5000.00"));
            s.calculateTotals();
            salaryStructureRepository.save(s);
        }

        LoginRequest employeeLogin = new LoginRequest(testEmp.getUsername(), TEST_PASSWORD, false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeLogin)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession employeeSession = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/api/salaries/me").session(employeeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("12. Existing database records are preserved intact")
    void testExistingDatabaseRecordsPreserved() {
        assertTrue(departmentRepository.count() >= 1, "Existing departments must be preserved");
        assertTrue(employeeRepository.count() >= 1, "Existing employees must be preserved");
        assertTrue(userRepository.count() >= 1, "Existing users must be preserved");

        for (User u : userRepository.findAll()) {
            assertNotNull(u.getUsername(), "Username must exist");
            assertNotNull(u.getPassword(), "Password must exist");
            assertTrue(u.getPassword().startsWith("$2"), "Password must be BCrypt hashed for user " + u.getUsername());
        }
    }

    @Test
    @DisplayName("13. ADMIN role can log in by username or email and is redirected to /app.html#dashboard")
    void testAdminLoginAndDashboardRedirect() throws Exception {
        User adminUser = createOrGetTestUser("test_admin_suite", "test_admin_suite@company.com", "ROLE_ADMIN", null);

        // Login by username
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(adminUser.getUsername(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"));

        // Login by email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(adminUser.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"));
    }

    @Test
    @DisplayName("14. HR role can log in by username or email and is redirected to /app.html#dashboard")
    void testHrLoginAndDashboardRedirect() throws Exception {
        User hrUser = createOrGetTestUser("test_hr_suite", "test_hr_suite@company.com", "ROLE_HR", null);

        // Login by username
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(hrUser.getUsername(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_HR"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"));

        // Login by email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(hrUser.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_HR"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#dashboard"));
    }

    @Test
    @DisplayName("15. MANAGER role can log in by username or email and is redirected to /app.html#manager-dashboard")
    void testManagerLoginAndDashboardRedirect() throws Exception {
        Department dept = createOrGetTestDepartment();
        Employee mgrEmp = createOrGetTestEmployee("MGRTEST01", "Manager", "Tester", "mgr.tester@company.com", dept);
        User mgrUser = createOrGetTestUser("test_mgr_suite", "test_mgr_suite@company.com", "ROLE_MANAGER", mgrEmp);

        // Login by username
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(mgrUser.getUsername(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#manager-dashboard"));

        // Login by email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(mgrUser.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#manager-dashboard"));
    }

    @Test
    @DisplayName("16. EMPLOYEE role can log in by username or email and is redirected to /app.html#employee-dashboard")
    void testEmployeeLoginAndDashboardRedirect() throws Exception {
        Department dept = createOrGetTestDepartment();
        Employee staffEmp = createOrGetTestEmployee("EMPTEST02", "Staff", "Tester", "staff.tester@company.com", dept);
        User empUser = createOrGetTestUser("test_emp_suite", "test_emp_suite@company.com", "ROLE_EMPLOYEE", staffEmp);

        // Login by username
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(empUser.getUsername(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));

        // Login by primary user email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(empUser.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));

        // Login by linked employee email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(staffEmp.getEmail(), TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_EMPLOYEE"))
                .andExpect(jsonPath("$.data.redirectUrl").value("/app.html#employee-dashboard"));
    }

    @Test
    @DisplayName("17. Dynamic dashboard statistics reflect real database data")
    void testDynamicDashboardStats() throws Exception {
        User testAdmin = createOrGetTestUser("test_admin_auth", "test_admin_auth@company.com", "ROLE_ADMIN", null);
        LoginRequest adminLogin = new LoginRequest(testAdmin.getUsername(), TEST_PASSWORD, false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(get("/api/dashboard/stats").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEmployees").isNumber())
                .andExpect(jsonPath("$.data.totalDepartments").isNumber())
                .andExpect(jsonPath("$.data.currentMonthPayroll").isNumber())
                .andExpect(jsonPath("$.data.attendanceSummary").isMap())
                .andExpect(jsonPath("$.data.chartLabels").isArray());
    }
}
