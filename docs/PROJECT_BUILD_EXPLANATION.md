# BloodBank — Complete Project Build & Architecture Explanation

---

## 1. What the Project Does

The **Blood Bank Inventory & Donor Eligibility Tracker** is a production-grade backend system designed for clinical blood banks, hospitals, and blood transfusion centers. It automates and enforces strict medical regulations for:

1. **Donor Life-Cycle Management**: Registering voluntary blood donors, validating contact details, checking date of birth/age bounds, and maintaining full donation histories with safe soft deactivation.
2. **Donor Eligibility & Safety Cooldown Enforcement**: Automatically computing donor eligibility based on the regulatory 90-day cooldown between whole-blood donations before allowing any new collection.
3. **Atomic Donation Processing & Inventory Generation**: Recording donations transactionally while automatically generating physical blood unit barcodes (`UNT-xxxxxxxx`), calculating shelf-life expiration (42 days), and inheriting donor blood group tags.
4. **Active Inventory & Stock Monitoring**: Providing real-time aggregations of usable inventory across all 8 standard blood groups (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`), while strictly isolating units that are near-expiry, expired, issued, or discarded.
5. **Automated Expiry & Near-Expiry Scheduling**: Running background cron schedules to flag units within 7 days of expiration (`NEAR_EXPIRY`) and past expiration (`EXPIRED`), while guaranteeing that live API operations evaluate date timestamps at query time.
6. **First-Expiry, First-Out (FEFO) Hospital Blood Issuing**: Automatically allocating the oldest safe, unexpired, non-near-expiry blood units when hospitals request transfusions, preventing wastage and eliminating human dispatch error.
7. **Double-Issue Impossibility**: Enforcing double-issue protection at both the application service layer (optimistic verification and state transitions) and the database schema layer (unique foreign key constraint on `issue_records.blood_unit_id`).

---

## 2. Real-World Problem Solved

Manual and legacy spreadsheet-based blood bank management suffers from severe systemic vulnerabilities:

- **Donor Depletion & Premature Donation**: Allowing donors to donate too soon (before the 90-day physiological red blood cell replenishment period) risks donor iron deficiency, anemia, and fainting.
- **Transfusion of Expired or Degrading Blood**: Whole blood and packed red blood cells degrade significantly after 42 days. Using near-expiry blood for vulnerable patients risks bacterial proliferation and reduced oxygen-carrying efficacy.
- **Wastage via FIFO vs FEFO Mismanagement**: In standard FIFO (First-In, First-Out), blood units collected earlier but received late or units with varied collection batches get expired on the shelf while newer units are issued first. FEFO eliminates shelf spoilage.
- **Stock Phantom Discrepancies**: Physical units being dispatched without real-time inventory updates leading to catastrophic stockouts during emergency trauma surgeries.
- **Double Dispatch / Cross-Contamination**: Issuing the same unit barcode twice to different operating theaters.

---

## 3. Project Objectives

- Build an enterprise-grade RESTful API strictly conforming to HTTP/1.1 and JSON specifications.
- Provide sub-second database transactions with ACID compliance using Spring Data JPA and MySQL 8.0.
- Guarantee 100% test coverage of critical business invariants (eligibility gap, shelf-life, FEFO allocation, double-issue blocking).
- Offer interactive API documentation using OpenAPI 3.0 / Swagger UI.
- Maintain complete operational auditability from donor needle-stick to patient transfusion.

---

## 4. Complete Technology Stack & Rationale

| Technology | Version | Purpose & Rationale |
| :--- | :---: | :--- |
| **Java** | 25 (LTS) / 21 Target | Modern, strongly typed, high-performance object-oriented programming language with virtual threads, pattern matching, and record constructs. |
| **Spring Boot** | 4.1.1 | Industry-standard enterprise application framework providing dependency injection, auto-configuration, embedded Tomcat, and production monitoring. |
| **Spring Web (MVC)** | 4.1.1 | DispatcherServlet-based MVC architecture mapping HTTP REST requests to controller endpoints, JSON serialization, and centralized exception handling. |
| **Spring Data JPA** | 4.1.1 | High-level data abstraction layer reducing boilerplate SQL, providing dynamic repository implementations (`JpaRepository`), pagination, and derived query methods. |
| **Hibernate ORM** | 7.4.5.Final | Robust Object-Relational Mapping engine translating Java entity classes and object graphs to relational tables, foreign keys, and SQL dialects. |
| **MySQL Database** | 8.0 | ACID-compliant relational database management system providing durable transaction logs (InnoDB engine), primary/foreign key referential integrity, and indexing. |
| **HikariCP** | 7.0.2 | Ultra-high performance, zero-overhead JDBC connection pool managing database connections, connection reuse, and health checking. |
| **Jakarta Bean Validation** | 3.0 | Declarative constraint annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Past`, `@Positive`) ensuring invalid payloads fail before touching service logic. |
| **Springdoc OpenAPI / Swagger UI** | 2.8.5 | Auto-generates OpenAPI 3.0 schemas (`/v3/api-docs`) and renders an interactive web UI (`/swagger-ui/index.html`) for testing without third-party tools. |
| **Project Lombok** | 1.18.36 | Compile-time annotation processor eliminating repetitive boilerplate (getters, setters, constructors, builders, and `@Slf4j` loggers). |
| **H2 Database** | 2.4.240 | In-memory relational database enabling rapid, zero-dependency, isolated automated testing (`application-test.properties`). |
| **JUnit 5 (Jupiter)** | 5.x | Modern testing framework providing assertions, nested tests, parameterized tests, and test lifecycle hooks. |
| **Mockito & Spring Test** | 4.1.1 | Mocking framework and MockMvc integration suite simulating full HTTP cycles without binding real network sockets. |
| **Apache Maven** | 3.9.x | Build lifecycle management, automated dependency resolution, compilation, testing, and packaging tool. |

---

## 5. Folder & Package Structure

```text
bloodbank/
├── pom.xml                                  <- Maven Project Object Model configuration
├── mvnw & mvnw.cmd                          <- Maven Wrapper executables (Linux / Windows)
├── README.md                                <- Master repository overview
├── .gitignore                               <- Git ignore patterns for builds and OS files
├── docs/                                    <- Complete documentation and guides
│   ├── ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   ├── API_DOCUMENTATION.md
│   ├── ASSESSMENT_GUIDE.md
│   ├── DEMO_GUIDE.md
│   ├── LOCAL_DEMO_GUIDE.md
│   ├── VIVA_QUESTIONS.md
│   ├── PROJECT_BUILD_EXPLANATION.md
│   └── diagrams/                            <- Mermaid architecture, ER, sequence diagrams
│       ├── system_architecture.mmd
│       ├── er_diagram.mmd
│       ├── class_diagram.mmd
│       ├── use_case_diagram.mmd
│       ├── donation_sequence.mmd
│       └── issue_sequence.mmd
├── src/
│   ├── main/
│   │   ├── java/com/bloodbank/
│   │   │   ├── BloodbankApplication.java    <- Application entry point (@SpringBootApplication)
│   │   │   ├── config/                      <- Web, CORS, Enum converters, OpenAPI configuration
│   │   │   ├── controller/                  <- REST API endpoints (Thin controllers)
│   │   │   ├── dto/
│   │   │   │   ├── request/                 <- Incoming JSON request payloads with Jakarta validation
│   │   │   │   └── response/                <- Structured, secure API response models
│   │   │   ├── entity/                      <- JPA Entities mapped to MySQL InnoDB tables
│   │   │   ├── enums/                       <- Type-safe domain constants (BloodGroup, Status, Gender)
│   │   │   ├── exception/                   <- Custom exceptions & @RestControllerAdvice handler
│   │   │   ├── repository/                  <- Spring Data JPA repositories with query methods
│   │   │   ├── scheduler/                   <- Periodic background tasks (@Scheduled)
│   │   │   └── service/                     <- Core business logic and transaction boundaries
│   │   └── resources/
│   │       ├── application.properties       <- Production MySQL configuration with env placeholders
│   │       └── application-test.properties  <- In-memory demo profile configuration
│   └── test/
│       ├── java/com/bloodbank/
│       │   ├── BloodbankApplicationTests.java
│       │   ├── BloodBankIntegrationTest.java <- 13 End-to-end integration tests (MockMvc)
│       │   ├── scheduler/
│       │   │   └── InventoryStatusSchedulerTest.java
│       │   └── service/                     <- Isolated unit tests (Donor, Donation, Inventory, Issue)
│       └── resources/
│           └── application-test.properties  <- Automated test configuration (H2 DB)
├── start_app.ps1                            <- One-click launch script
├── test_all_localhost.ps1                   <- 19-Endpoint live verification script
└── verify_live_api.ps1                      <- 12-Step business rule verification script
```

### Explanation of Packages

- `com.bloodbank.config`: Houses infrastructure configuration. `OpenApiConfig` configures Swagger UI metadata. `WebCorsConfig` sets up Cross-Origin Resource Sharing and registers custom converters (`StringToBloodGroupConverter`) to allow human-readable blood groups (`A+`, `O-`) in URL paths without 500 errors.
- `com.bloodbank.controller`: Acts as the HTTP gateway. Controllers receive HTTP requests, trigger Jakarta `@Valid` validations, delegate immediately to services, and return typed `ResponseEntity<T>` DTOs.
- `com.bloodbank.dto.request`: Encapsulates incoming JSON bodies with strict validation annotations (`@NotBlank`, `@Email`, `@Past`, `@Pattern`).
- `com.bloodbank.dto.response`: Exposes clean, serialized representation of entities, completely hiding internal database details, password fields, or circular JPA graph relationships.
- `com.bloodbank.entity`: Contains JPA `@Entity` domain models representing physical tables (`donors`, `donations`, `blood_units`, `issue_records`) with auditing timestamps (`createdAt`, `updatedAt`).
- `com.bloodbank.enums`: Type-safe domain enums (`BloodGroup`, `BloodUnitStatus`, `Gender`) with custom `@JsonCreator` and `@JsonValue` annotations for seamless human-friendly string serialization.
- `com.bloodbank.exception`: Custom runtime exceptions (`DonorNotEligibleException`, `InsufficientStockException`, etc.) and the master `GlobalExceptionHandler` mapping errors to clean RFC 7807-style JSON payloads.
- `com.bloodbank.repository`: Spring Data JPA interfaces extending `JpaRepository<T, ID>` with customized JPQL and derived queries.
- `com.bloodbank.scheduler`: Houses `@Scheduled` cron jobs (`InventoryStatusScheduler`) that auto-evaluate unit expiry states.
- `com.bloodbank.service`: The heart of the application. Contains transaction boundaries (`@Transactional`), business rules, calculations, and orchestration.

---

## 6. File-by-File Detailed Explanation

### Core Application

- **`BloodbankApplication.java`**:
  - *Role*: Spring Boot bootstrap entry point.
  - *Annotations*: `@SpringBootApplication` (combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`), `@EnableScheduling` (activates Spring's background task executor).
  - *Main method*: Calls `SpringApplication.run(BloodbankApplication.class, args)`.

### Controllers

- **`DonorController.java`**: Exposes `/api/donors`. Thin controller delegating to `DonorService`. Endpoints:
  - `POST /api/donors`: Creates donor, returns `201 Created`.
  - `GET /api/donors`: Returns paginated donors (`200 OK`).
  - `GET /api/donors/{id}`: Returns donor details or 404.
  - `PUT /api/donors/{id}`: Updates contact details.
  - `DELETE /api/donors/{id}`: Performs soft deactivation.
  - `GET /api/donors/{id}/eligibility`: Evaluates 90-day cooldown status.
- **`DonationController.java`**: Exposes `/api/donations`. Thin controller delegating to `DonationService`.
  - `POST /api/donations`: Registers donation, generates blood units. Returns `201 Created`.
  - `GET /api/donations`: Returns paginated donation audit trail.
  - `GET /api/donations/{id}`: Returns specific donation with associated unit codes.
- **`InventoryController.java`**: Exposes `/api/inventory`.
  - `GET /api/inventory`: Paginated physical units.
  - `GET /api/inventory/stock`: Summary map of usable units for all 8 blood groups.
  - `GET /api/inventory/near-expiry`: Units expiring within 7 days.
  - `GET /api/inventory/expired`: Units past expiration date.
  - `GET /api/inventory/blood-group/{bloodGroup}`: Filter units by group.
  - `GET /api/inventory/unit/{unitCode}`: Lookup specific unit by barcode.
- **`IssueController.java`**: Exposes `/api/issues`.
  - `POST /api/issues`: Issues blood units to hospitals using FEFO. Returns `201 Created`.
  - `GET /api/issues`: Paginated list of all hospital issuance audit records.
  - `GET /api/issues/{id}`: Fetch individual issuance audit record.
- **`HomeController.java`**:
  - *Role*: Exposes `GET /` and sends an HTTP 302 Found redirect to `/swagger-ui/index.html`. Eliminates browser 404 confusion.

### Services

- **`DonorService.java`**:
  - Implements CRUD operations for donors.
  - Validates uniqueness of email and phone numbers, throwing `DuplicateResourceException` if duplicated.
  - Evaluates eligibility: checks if donor is active, checks most recent donation date, calculates whether `(today - lastDonationDate) < 90`.
  - Performs soft deletion: sets `active = false` without dropping donation or transfusion history.
- **`DonationService.java`**:
  - Orchestrates donation creation under `@Transactional`.
  - Re-evaluates donor eligibility before persisting. If ineligible, throws `DonorNotEligibleException`.
  - Generates unique donation code (`DON-xxxxxxxx`).
  - Spawns requested number of `BloodUnit` entities, assigning random barcodes (`UNT-xxxxxxxx`), collection date, and computing `expiryDate = collectionDate + 42 days`.
  - Saves donation and cascades blood unit creation.
- **`InventoryService.java`**:
  - Computes usable stock levels for all 8 blood groups.
  - Evaluates safe stock: counts only units with status `AVAILABLE` whose `expiryDate > today + 7 days`.
  - Implements `updateInventoryStatuses()` called by the background scheduler to transition units to `NEAR_EXPIRY` or `EXPIRED`.
- **`IssueService.java`**:
  - Orchestrates blood issuance under `@Transactional`.
  - Queries safe available units for the exact requested blood group, sorted by `expiryDate ASC` (First-Expiry, First-Out).
  - Validates if available count $\ge$ requested units. If insufficient, throws `InsufficientStockException`.
  - Marks chosen units as `ISSUED`.
  - Generates immutable `IssueRecord` entries linking patient, hospital, and blood unit.

### Entities

- **`Donor.java`**: Table `donors`. Stores `id`, unique `donor_code`, `name`, unique `email`, unique `phone`, `date_of_birth`, `gender`, `blood_group`, `address`, `active`, `created_at`, `updated_at`. Has `@OneToMany(mappedBy = "donor")` relationship with `Donation`.
- **`Donation.java`**: Table `donations`. Stores `id`, unique `donation_code`, `donor_id` (FK), `donation_date`, `number_of_units`, `notes`, `created_at`. Has `@OneToMany(cascade = CascadeType.ALL)` relationship with `BloodUnit`.
- **`BloodUnit.java`**: Table `blood_units`. Stores `id`, unique `unit_code`, `donation_id` (FK), `blood_group`, `collection_date`, `expiry_date`, `status` (`AVAILABLE`, `NEAR_EXPIRY`, `EXPIRED`, `ISSUED`, `DISCARDED`), `created_at`, `updated_at`. Has `@OneToOne(mappedBy = "bloodUnit")` relationship with `IssueRecord`.
- **`IssueRecord.java`**: Table `issue_records`. Stores `id`, unique `issue_code`, `blood_unit_id` (FK with UNIQUE constraint), `patient_name`, `hospital_name`, `requested_blood_group`, `issue_date`, `notes`, `created_at`.

---

## 7. Complete Request-Response Flow

```text
[Client / Swagger UI / Browser]
       │
       ▼ (HTTP POST /api/donations with JSON payload)
[Tomcat Web Server (Port 8080)]
       │
       ▼
[DispatcherServlet]
       │
       ▼
[WebCorsConfig & Converters]
       │
       ▼
[DonationController.registerDonation(@Valid @RequestBody DonationCreateRequest)]
       │
       ▼ (Passes Jakarta Bean Validation: @NotNull, @Positive)
[DonationService.registerDonation(request)] (Enters @Transactional boundary)
       │
       ├──> Calls DonorRepository.findById(donorId)
       │         └── Hibernate queries MySQL: SELECT * FROM donors WHERE id = ?
       │
       ├──> Calls DonorService.checkEligibility(donorId)
       │         └── Queries latest donation: SELECT * FROM donations WHERE donor_id = ? ORDER BY donation_date DESC
       │         └── Asserts (today - lastDate) >= 90 days
       │
       ├──> Constructs Donation entity with unique DON-xxxxxxxx code
       │
       ├──> Loops numberOfUnits: creates BloodUnit with UNT-xxxxxxxx code, expiry = today + 42
       │
       ├──> Calls DonationRepository.save(donation)
       │         └── Hibernate generates SQL: INSERT INTO donations ..., INSERT INTO blood_units ...
       │         └── MySQL executes inserts, commits transaction
       │
       ▼
[DonationResponse DTO mapped from saved entity]
       │
       ▼
[DispatcherServlet serializes to JSON via Jackson]
       │
       ▼
[Client receives HTTP 201 Created with JSON response body]
```

---

## 8. Business Rules Deep Dive

### Rule 1: Donor Eligibility & 90-Day Cooldown

- **Rule**: A donor can only donate whole blood once every 90 days.
- **Config property**: `bloodbank.donation.minimum-gap-days=90`.
- **Implementation**:
  1. If donor has 0 previous donations $\to$ `eligible = true`, `remainingDays = 0`.
  2. If donor has previous donations $\to$ finds `MAX(donation_date)`.
  3. `nextEligibleDate = lastDonationDate + 90 days`.
  4. If `today >= nextEligibleDate` $\to$ `eligible = true`.
  5. If `today < nextEligibleDate` $\to$ `eligible = false`, `remainingDays = DAYS.between(today, nextEligibleDate)`.
  6. Inactive donors (`active = false`) are unconditionally ineligible.

### Rule 2: 42-Day Blood Unit Shelf-Life

- **Rule**: Whole blood and packed red cells have a maximum viability shelf-life of 42 days from collection.
- **Config property**: `bloodbank.blood-unit.shelf-life-days=42`.
- **Implementation**: When a donation is logged, `collectionDate` defaults to the donation date (or today), and `expiryDate = collectionDate.plusDays(42)`.

### Rule 3: 7-Day Near-Expiry & Expiry Isolation

- **Rule**: Blood units within 7 days of expiration are flagged `NEAR_EXPIRY` and quarantined from general issue to prevent transfusion of deteriorating units. Units past expiration are marked `EXPIRED`.
- **Config property**: `bloodbank.inventory.near-expiry-days=7`.
- **Implementation**: Units with `expiryDate < today` are `EXPIRED`. Units where `today <= expiryDate <= today + 7` are `NEAR_EXPIRY`. Units with `expiryDate > today + 7` are `AVAILABLE`. Terminal states (`ISSUED`, `DISCARDED`) are never overwritten.

### Rule 4: First-Expiry, First-Out (FEFO) Allocation

- **Rule**: When blood is requested, the system automatically allocates units with the earliest expiration date among safe available units.
- **Implementation**: `bloodUnitRepository.findAvailableUnitsForIssue(group, today, nearExpiryThreshold)` sorts by `bu.expiryDate ASC`.

### Rule 5: Double-Issue Prevention

- **Rule**: Under no circumstances can a single physical unit be assigned to more than one patient or hospital.
- **Implementation**:
  - Application layer: `IssueService` updates unit status to `ISSUED` within a database transaction.
  - Database layer: `issue_records.blood_unit_id` has a `UNIQUE` foreign key constraint. Any duplicate insertion triggers a database-level integrity violation and rolls back the transaction.

---

## 9. Complete MySQL Database Schema

```sql
-- 1. Donors Table
CREATE TABLE donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    blood_group VARCHAR(15) NOT NULL,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_donor_blood_group (blood_group),
    INDEX idx_donor_active (active)
) ENGINE=InnoDB;

-- 2. Donations Table
CREATE TABLE donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donation_code VARCHAR(32) NOT NULL UNIQUE,
    donor_id BIGINT NOT NULL,
    donation_date DATE NOT NULL,
    number_of_units INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE RESTRICT,
    INDEX idx_donation_date (donation_date),
    INDEX idx_donation_donor (donor_id)
) ENGINE=InnoDB;

-- 3. Blood Units Table
CREATE TABLE blood_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_code VARCHAR(32) NOT NULL UNIQUE,
    donation_id BIGINT NOT NULL,
    blood_group VARCHAR(15) NOT NULL,
    collection_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE RESTRICT,
    INDEX idx_unit_blood_group (blood_group),
    INDEX idx_unit_status (status),
    INDEX idx_unit_expiry (expiry_date),
    INDEX idx_unit_fefo (blood_group, status, expiry_date)
) ENGINE=InnoDB;

-- 4. Issue Records Table
CREATE TABLE issue_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_code VARCHAR(32) NOT NULL UNIQUE,
    blood_unit_id BIGINT NOT NULL UNIQUE, -- Enforces 1:1 double-issue protection
    patient_name VARCHAR(100) NOT NULL,
    hospital_name VARCHAR(150) NOT NULL,
    requested_blood_group VARCHAR(15) NOT NULL,
    issue_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (blood_unit_id) REFERENCES blood_units(id) ON DELETE RESTRICT,
    INDEX idx_issue_date (issue_date),
    INDEX idx_issue_hospital (hospital_name)
) ENGINE=InnoDB;
```

---

## 10. Database Connection & Pooling Architecture

```text
[application.properties]
        │
        ├──> spring.datasource.url=${DB_URL:...}
        ├──> spring.datasource.username=${DB_USERNAME:...}
        └──> spring.datasource.password=${DB_PASSWORD:...}
        │
        ▼
[HikariDataSource (HikariCP 7.0.2)]
        │  - Initializes pooled connections (default: 10 connections)
        │  - Validates connection liveness with connection timeout & keepalive
        │
        ▼
[MySQL Connector/J (JDBC Driver 9.7.0)]
        │  - Manages low-level TCP socket connection to localhost:3306
        │  - Handles SSL/TLS handshake, character encoding (UTF-8), and timezone (UTC)
        │
        ▼
[Hibernate 7.4.5 (JPA Provider)]
        │  - Translates JPQL / Criteria queries to MySQL 8 dialect SQL
        │  - Manages First-Level Cache (Persistence Context) & dirty checking
        │
        ▼
[Spring Data JPA Repositories]
        │  - Provides type-safe interfaces (DonorRepository, BloodUnitRepository)
        │
        ▼
[MySQL 8.0 Server (localhost:3306)]
           - Executes InnoDB transactions with REPEATABLE_READ isolation level
```

---

## 11. API Specification & Mapping Table

| HTTP Method | URL Path | Purpose | Request DTO | Response DTO | HTTP Status | Database Table |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `GET` | `/` | Redirect to Swagger UI | None | Redirect Header | `302` | None |
| `GET` | `/swagger-ui/index.html` | Interactive Swagger UI | None | HTML/JS | `200` | None |
| `GET` | `/v3/api-docs` | OpenAPI 3.0 JSON Schema | None | JSON Schema | `200` | None |
| `GET` | `/api/inventory/stock` | Usable Stock by Blood Group | None | `Map<String, Long>` | `200` | `blood_units` |
| `POST` | `/api/donors` | Register New Donor | `DonorCreateRequest` | `DonorResponse` | `201` | `donors` |
| `GET` | `/api/donors` | List Donors (Paginated) | None (query params) | `Page<DonorResponse>` | `200` | `donors` |
| `GET` | `/api/donors/{id}` | Get Donor Profile | None | `DonorResponse` | `200` | `donors` |
| `PUT` | `/api/donors/{id}` | Update Donor Details | `DonorUpdateRequest` | `DonorResponse` | `200` | `donors` |
| `DELETE` | `/api/donors/{id}` | Soft Deactivate Donor | None | `DonorResponse` | `200` | `donors` |
| `GET` | `/api/donors/{id}/eligibility` | Check Donor Eligibility | None | `EligibilityResponse` | `200` | `donors`, `donations` |
| `POST` | `/api/donations` | Register Blood Donation | `DonationCreateRequest` | `DonationResponse` | `201` | `donations`, `blood_units` |
| `GET` | `/api/donations` | List Donations (Paginated) | None (query params) | `Page<DonationResponse>` | `200` | `donations` |
| `GET` | `/api/donations/{id}` | Get Donation Details | None | `DonationResponse` | `200` | `donations`, `blood_units` |
| `GET` | `/api/inventory` | List Physical Units | None (query params) | `Page<BloodUnitResponse>` | `200` | `blood_units` |
| `GET` | `/api/inventory/near-expiry` | List Near-Expiry Units | None (query params) | `Page<BloodUnitResponse>` | `200` | `blood_units` |
| `GET` | `/api/inventory/expired` | List Expired Units | None (query params) | `Page<BloodUnitResponse>` | `200` | `blood_units` |
| `GET` | `/api/inventory/blood-group/{grp}` | Filter Units by Blood Group | None | `Page<BloodUnitResponse>` | `200` | `blood_units` |
| `GET` | `/api/inventory/unit/{unitCode}` | Lookup Unit by Barcode | None | `BloodUnitResponse` | `200` | `blood_units` |
| `POST` | `/api/issues` | Issue Blood (FEFO) | `IssueRequest` | `IssueResponse` | `201` | `blood_units`, `issue_records` |
| `GET` | `/api/issues` | List Issuance Audit Trail | None (query params) | `Page<IssueRecordResponse>` | `200` | `issue_records` |
| `GET` | `/api/issues/{id}` | Get Issuance Audit Detail | None | `IssueRecordResponse` | `200` | `issue_records` |

---

## 12. Exception Handling Flow

When an error occurs anywhere in the stack:

1. The business logic throws a specific unchecked exception extending `RuntimeException`:
   - `ResourceNotFoundException`: Target donor, donation, unit, or issue record does not exist.
   - `DonorNotEligibleException`: Donor is inactive or hasn't finished the 90-day cooldown.
   - `InsufficientStockException`: Not enough safe, unexpired, non-near-expiry units for the requested group.
   - `DuplicateResourceException`: Email or phone number is already registered to another donor.
   - `BusinessRuleException`: Generic business rule invariant violation.
2. The exception bubbles out of the `@Transactional` boundary, triggering an automatic rollback of any pending database operations.
3. Spring MVC intercepts the exception in `GlobalExceptionHandler` (`@RestControllerAdvice`).
4. `GlobalExceptionHandler` extracts error metadata, logs the event with `@Slf4j`, and builds a standard `ErrorResponse`:

   ```json
   {
     "timestamp": "2026-09-28T22:20:00",
     "status": 400,
     "error": "DONOR_NOT_ELIGIBLE",
     "message": "Donor has not completed the minimum donation gap of 90 days. 90 day(s) remaining until next eligible donation date (2026-12-27).",
     "path": "/api/donations"
   }
   ```

5. No stack traces, database credentials, or internal class names are ever leaked to the client.

---

## 13. How to Build, Test, and Run from Scratch

### 1. Prerequisites

- **Java Development Kit**: JDK 21+ installed and configured on `PATH`.
- **MySQL Server**: MySQL 8.0 running on `localhost:3306`.
- **Database**: Database `bloodbank_db` created (`CREATE DATABASE bloodbank_db;`).

### 2. Compilation

```powershell
.\mvnw.cmd clean compile
```

### 3. Automated Testing (46 Tests via In-Memory H2)

```powershell
.\mvnw.cmd clean test
```

### 4. Packaging the Standalone Fat JAR

```powershell
.\mvnw.cmd clean package -DskipTests
```

The production JAR is generated at: `target\bloodbank-1.0.0.jar`.

### 5. Running the Application on Localhost

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

java -jar target\bloodbank-1.0.0.jar --spring.datasource.username=$env:DB_USERNAME --spring.datasource.password=$env:DB_PASSWORD
```

---

## 14. GitHub Structure & Git Workflow

- **Branching Strategy**: Clean single-trunk `main` branch with atomic commits.
- **Repository Isolation**: Git is initialized directly within `C:\Users\jaget\Downloads\bloodbank\bloodbank`, strictly isolated from the parent Windows user profile.
- **Security Compliance**: Sensitive credentials (`DB_PASSWORD`, API keys, tokens) are never committed. They are passed dynamically at runtime via environment variables (`$env:DB_PASSWORD`).
- **Ignore Rules**: `.gitignore` comprehensively filters compiled artifacts (`target/`), crash dumps (`hs_err_pid*`), logs (`*.log`), environment files (`.env`), and IDE settings (`.idea/`, `.vscode/`).

---

## 15. Complete API Connection Map

The table below details the complete execution path for every REST endpoint in the system, tracing the request from HTTP entry to database persistence and client response:

| HTTP & Endpoint | Controller | Request DTO / Params | Service Layer | Repository | Entity / DB Table | Response DTO |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST /api/donors` | `DonorController` | `DonorCreateRequest` | `DonorService.createDonor()` | `DonorRepository` | `Donor` / `donors` | `DonorResponse` (201 Created) |
| `GET /api/donors` | `DonorController` | `Pageable`, `active` | `DonorService.getAllDonors()` | `DonorRepository` | `Donor` / `donors` | `PageResponse<DonorResponse>` (200 OK) |
| `GET /api/donors/{id}` | `DonorController` | `@PathVariable id` | `DonorService.getDonorById()` | `DonorRepository` | `Donor` / `donors` | `DonorResponse` (200 OK) |
| `PUT /api/donors/{id}` | `DonorController` | `DonorUpdateRequest` | `DonorService.updateDonor()` | `DonorRepository` | `Donor` / `donors` | `DonorResponse` (200 OK) |
| `DELETE /api/donors/{id}` | `DonorController` | `@PathVariable id` | `DonorService.deactivateDonor()` | `DonorRepository` | `Donor` / `donors` | `void` (204 No Content) |
| `GET /api/donors/{id}/eligibility` | `DonorController` | `@PathVariable id` | `DonorService.checkEligibility()` | `DonorRepository`, `DonationRepository` | `Donor`, `Donation` | `EligibilityResponse` (200 OK) |
| `POST /api/donations` | `DonationController` | `DonationCreateRequest` | `DonationService.createDonation()` | `DonationRepository`, `BloodUnitRepository`, `DonorRepository` | `Donation` / `donations`, `BloodUnit` / `blood_units` | `DonationResponse` (201 Created) |
| `GET /api/donations` | `DonationController` | `Pageable` | `DonationService.getAllDonations()` | `DonationRepository` | `Donation` / `donations` | `PageResponse<DonationResponse>` (200 OK) |
| `GET /api/donations/{id}` | `DonationController` | `@PathVariable id` | `DonationService.getDonationById()` | `DonationRepository` | `Donation` / `donations` | `DonationResponse` (200 OK) |
| `GET /api/donations/donor/{donorId}` | `DonationController` | `@PathVariable donorId` | `DonationService.getDonationsByDonorId()` | `DonationRepository` | `Donation` / `donations` | `List<DonationResponse>` (200 OK) |
| `GET /api/inventory` | `InventoryController` | `Pageable` | `InventoryService.getAllInventory()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `PageResponse<BloodUnitResponse>` (200 OK) |
| `GET /api/inventory/stock` | `InventoryController` | None | `InventoryService.getAvailableStockByBloodGroup()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `Map<BloodGroup, Long>` (200 OK) |
| `GET /api/inventory/near-expiry` | `InventoryController` | `Pageable` | `InventoryService.getNearExpiryUnits()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `PageResponse<BloodUnitResponse>` (200 OK) |
| `GET /api/inventory/expired` | `InventoryController` | `Pageable` | `InventoryService.getExpiredUnits()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `PageResponse<BloodUnitResponse>` (200 OK) |
| `GET /api/inventory/blood-group/{bg}` | `InventoryController` | `@PathVariable bg`, `Pageable` | `InventoryService.getInventoryByBloodGroup()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `PageResponse<BloodUnitResponse>` (200 OK) |
| `GET /api/inventory/unit/{unitCode}` | `InventoryController` | `@PathVariable unitCode` | `InventoryService.getBloodUnitByCode()` | `BloodUnitRepository` | `BloodUnit` / `blood_units` | `BloodUnitResponse` (200 OK) |
| `POST /api/issues` | `IssueController` | `IssueRequest` | `IssueService.issueBloodUnit()` | `BloodUnitRepository`, `IssueRecordRepository` | `BloodUnit` / `blood_units`, `IssueRecord` / `issue_records` | `IssueResponse` (201 Created) |
| `GET /api/issues` | `IssueController` | `Pageable` | `IssueService.getAllIssues()` | `IssueRecordRepository` | `IssueRecord` / `issue_records` | `PageResponse<IssueResponse>` (200 OK) |
| `GET /api/issues/{id}` | `IssueController` | `@PathVariable id` | `IssueService.getIssueById()` | `IssueRecordRepository` | `IssueRecord` / `issue_records` | `IssueResponse` (200 OK) |
