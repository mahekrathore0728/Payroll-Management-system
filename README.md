# Nexus HRMS - Enterprise Payroll Management System

A complete, production-ready, professional Web-Based Payroll and HR Management Platform built with **Java 25 LTS**, **Spring Boot 3.3.6**, **Spring Security 6**, **Spring Data JPA / Hibernate**, **MySQL 9.6**, **OpenPDF**, and **Bootstrap 5.3**.

Designed with an enterprise navy/blue aesthetic, modular multi-tier architecture, and strict role-based access control (RBAC).

---

## 🌟 Key Features

### 1. Role-Based Access Control (4 Tier Hierarchy)
- **ADMIN**: Complete system governance, user account administration, full company-wide visibility.
- **HR**: Employee onboarding, department configuration, salary structure definitions, payroll batch execution, attendance oversight, leave approval.
- **MANAGER**: Department-scoped team hub, team attendance tracking, team leave approval/rejection workflows, departmental payroll expenditure.
- **EMPLOYEE**: Employee self-service (ESS) portal, one-click daily check-in / check-out, leave request submission with real-time quota deduction, salary preview, and high-resolution PDF payslip downloads.

### 2. Core Business Engine
- **Salary Computation Formula**:
  - `Gross Salary` = `Basic Salary` + `HRA` + `DA` + `Bonus` + `(Overtime Hours × Overtime Rate)` + `Other Allowances`
  - `Total Deductions` = `Provident Fund (PF)` + `Income Tax (TDS)` + `Other Deductions`
  - `Net Take-Home Pay` = `Gross Salary` - `Total Deductions`
- **Duplicate Payroll Prevention**: Unique constraint validation (`employee_id` + `month` + `year`) prevents duplicate salary disbursements in the same pay period.
- **Leave Balance Engine**: Automatic tracking of Casual (12/yr), Sick (12/yr), and Annual (15/yr) quotas. Approving a leave application automatically deducts remaining quota and sets daily attendance to `ON_LEAVE`.
- **Dynamic PDF Payslip Generation**: OpenPDF-powered generator producing pixel-perfect corporate payslips featuring company header, dual-column earnings & deductions, net pay in numbers and words (e.g. *"Sixty-Five Thousand One Hundred Only"*), and authorized signature line.
- **Live Analytical Dashboard**: Interactive Chart.js graphs for 12-month gross vs net payout trends and departmental staffing distribution.
- **Report Center & CSV Export**: Instant generation and CSV export for Monthly Payroll Summaries, Department Headcount & Payroll, Monthly Attendance Rosters, and Workforce Leave Balances.

---

## 🔐 Database-Driven Authentication & Provisioning

The system operates on an authentic database-driven authentication model:

1. **Fresh Database / First-Time Installation**:
   - When the MySQL database has zero user accounts, the system automatically presents the **System Setup / Create Administrator** onboarding portal on the login page.
   - The initial Super Administrator creates their account with full name, work email, username, and password.
   - The password is encrypted using BCrypt, stored directly in MySQL, and the administrator is immediately routed to the Admin Dashboard.
   - Once initialized, the setup portal is locked down and disabled.

2. **Existing Database / Ongoing Operations**:
   - Users authenticate using their registered **Username** or **Work Email** against the database.
   - All passwords are verified using BCrypt hashing through Spring Security.
   - No hardcoded demo credentials or fake fallback values exist.

3. **Dynamic User & Employee Provisioning**:
   - Administrators and HR officers create user accounts directly via the **User Administration** view or via the **Employee Directory** modal with "Create System Login Account".
   - Supported roles: `ROLE_ADMIN`, `ROLE_HR`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`.

---

## 🚀 Database Modes & Running the Application

### 1. Database Architecture
The system supports dual database testing environments:
- **`payroll_db` (Standard / Existing Database)**: Retains all existing business records and registered users. Completely preserved and untouched by fresh database test suites.
- **`payroll_fresh_db` (Fresh / Clean Installation Testing)**: A separate database created without initial records to verify the First-Time Administrator setup portal and end-to-end dynamic workflows.

### 2. Configuration
Credentials should be placed in `application-local.properties` (gitignored, never committed):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payroll_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=payroll_app
spring.datasource.password=<YOUR_SECURE_PASSWORD>
```

### 3. Running the Application

#### Mode A: Run Against Existing Database (`payroll_db`)
Preserves existing data, employees, and user accounts:
```bash
.\mvnw.cmd spring-boot:run
```
Or with packaged JAR:
```bash
java -jar target\payroll-management-system-1.0.0.jar
```

#### Mode B: Run Against Fresh Database (`payroll_fresh_db`)
Temporarily routes to `payroll_fresh_db` without modifying `application-local.properties`:
```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/payroll_fresh_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```
Or with packaged JAR:
```bash
java -jar target\payroll-management-system-1.0.0.jar --spring.datasource.url="jdbc:mysql://localhost:3306/payroll_fresh_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

### 4. Running Automated Tests
```bash
# Run full automated test suite (39 tests across both databases)
.\mvnw.cmd test

# Compile and package production artifact
.\mvnw.cmd package -DskipTests
```

### 5. Access the Web Application
Open your browser and navigate to:
```
http://localhost:8080/
```
- If the database has **0 users**, the page automatically displays the **First-Time System Setup** form to create the initial Super Administrator.
- If user accounts already exist, the page displays the **Standard Database Authentication** form supporting either registered username or work email.