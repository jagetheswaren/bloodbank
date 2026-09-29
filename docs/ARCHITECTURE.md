# System Architecture Documentation

## 1. Overview
The BloodBank Inventory and Donor Eligibility Tracker is engineered following standard enterprise layered architecture principles using **Spring Boot 4 / Java 21** and **Spring Data JPA / Hibernate** backed by **MySQL 8.0** (with an in-memory **H2** database for automated test suites).

The application enforces a strict separation of concerns:
- **Clients (Browser / Postman / Swagger UI)** interact solely through standardized RESTful JSON endpoints.
- **Controllers** remain thin orchestrators with zero business logic.
- **DTOs & Jakarta Bean Validation** ensure safe data contracts and boundary validation.
- **Service Layer** encapsulates all domain business logic (e.g., 90-day donor gap verification, 42-day blood unit shelf life, 7-day near-expiry auto-flagging, and FEFO blood allocation).
- **Repository Layer** provides high-performance data access queries leveraging database indexes.
- **Spring Data JPA / Hibernate** manages object-relational mapping, transactions, and relational integrity.

---

## 2. Layered Architecture Diagram

```mermaid
graph TD
    subgraph ClientLayer ["1. Client & Presentation Layer"]
        C1["Web Browser / Client UI"]
        C2["Swagger UI / OpenAPI Documentation<br/><code>/swagger-ui/index.html</code>"]
        C3["Postman / REST Clients"]
    end

    subgraph ControllerLayer ["2. REST Controller Layer (Thin)"]
        Ctrl1["DonorController<br/><code>/api/donors</code>"]
        Ctrl2["DonationController<br/><code>/api/donations</code>"]
        Ctrl3["InventoryController<br/><code>/api/inventory</code>"]
        Ctrl4["IssueController<br/><code>/api/issues</code>"]
        Ctrl5["NotificationController<br/><code>/api/notifications</code>"]
        GEH["GlobalExceptionHandler<br/><code>@RestControllerAdvice</code>"]
    end

    subgraph ValidationLayer ["3. Request / Response DTOs & Validation"]
        DTO1["DonorCreateRequest / DonorUpdateRequest<br/><code>@Valid, @NotBlank, @Email</code>"]
        DTO2["DonationCreateRequest<br/><code>@Positive, @NotNull</code>"]
        DTO3["IssueRequest<br/><code>@NotNull, @Size</code>"]
        DTO4["Response DTOs<br/>DonorResponse, EligibilityResponse, BloodUnitResponse, StockResponse, IssueResponse"]
    end

    subgraph ServiceLayer ["4. Business Logic & Service Layer (@Service, @Transactional)"]
        S1["DonorService<br/>- Minimum 90-day donation gap calculation<br/>- Active/inactive state enforcement<br/>- Soft deactivation"]
        S2["DonationService<br/>- Atomic donation processing<br/>- Pre-persistence eligibility validation<br/>- Blood unit batch generation (42-day shelf life)"]
        S3["InventoryService<br/>- Real-time stock aggregation across 8 blood groups<br/>- Near-expiry (7-day) & expired status auto-flagging<br/>- Stale data protection"]
        S4["IssueService<br/>- FEFO (First Expire, First Out) unit selection<br/>- Duplicate issue prevention<br/>- Expired & near-expiry safety blocks"]
        SCHED["InventoryStatusScheduler<br/><code>@Scheduled(fixedRate = 60s)</code>"]
        NEL["NotificationEventListener<br/><code>@Async @TransactionalEventListener(AFTER_COMMIT)</code>"]
        ES["EmailService & TemplateService<br/>- Real SMTP & Fallback Logging"]
    end

    subgraph RepositoryLayer ["5. Persistence & Repository Layer (Spring Data JPA)"]
        R1["DonorRepository"]
        R2["DonationRepository"]
        R3["BloodUnitRepository<br/>- Custom HQL queries with FEFO ordering<br/>- Group-by stock counts<br/>- Expiry window filtering"]
        R4["IssueRecordRepository"]
        R5["NotificationLogRepository"]
    end

    subgraph DatabaseLayer ["6. Database Layer"]
        DB1[("MySQL 8.0 Production DB<br/><code>bloodbank_db</code>")]
        DB2[("H2 In-Memory Test DB<br/><code>bloodbank_test_db</code>")]
    end

    C1 --> Ctrl1 & Ctrl2 & Ctrl3 & Ctrl4 & Ctrl5
    C2 --> Ctrl1 & Ctrl2 & Ctrl3 & Ctrl4 & Ctrl5
    C3 --> Ctrl1 & Ctrl2 & Ctrl3 & Ctrl4 & Ctrl5

    Ctrl1 & Ctrl2 & Ctrl3 & Ctrl4 --> DTO1 & DTO2 & DTO3
    DTO1 & DTO2 & DTO3 --> GEH

    Ctrl1 --> S1
    Ctrl2 --> S2
    Ctrl3 --> S3
    Ctrl4 --> S4
    Ctrl5 --> R5
    SCHED --> S3
    S1 & S2 & S3 & S4 -.->|Application Events| NEL
    NEL --> ES
    ES --> R5

    S1 --> R1 & R2
    S2 --> R2 & R3 & S1
    S3 --> R3
    S4 --> R3 & R4

    R1 & R2 & R3 & R4 & R5 --> DB1
    R1 & R2 & R3 & R4 & R5 -.-> DB2
```

---

## 3. Component Details & Responsibility Matrix

| Layer | Component | Core Responsibilities |
| :--- | :--- | :--- |
| **Presentation** | `DonorController` | Handles HTTP routing for donor CRUD, soft delete, and eligibility queries. No business logic. |
| | `DonationController` | Handles donation registration requests and donation history. |
| | `InventoryController` | Exposes inventory units, stock level maps across 8 blood groups, near-expiry units, and expired units. |
| | `IssueController` | Receives blood issuing requests and returns issued unit audits. |
| | `NotificationController` | Exposes notification audit logs and email dispatch subsystem status. |
| | `GlobalExceptionHandler` | Centralized `@RestControllerAdvice` intercepting domain and validation exceptions, returning standardized JSON error bodies. |
| **DTOs & Validation** | `dto.request.*` | Immutable data carriers annotated with Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@PastOrPresent`, `@Positive`, etc.). |
| | `dto.response.*` | API representations preventing JPA entity leaks and circular serialization. |
| **Business Logic** | `DonorService` | Enforces unique email/phone constraints, calculates minimum 90-day donation gap, and handles soft deactivation. |
| | `DonationService` | Validates donor eligibility **before** persistence, atomically persists donation records, and generates individual units with 42-day shelf life. |
| | `InventoryService` | Aggregates available stock across all 8 blood groups (ignoring expired, near-expiry, issued, or discarded blood), and executes status evaluations. |
| | `IssueService` | Implements FEFO (First Expire, First Out) selection of available units, blocks unsafe units, and prevents reissue. |
| | `InventoryStatusScheduler` | Background scheduled task periodically triggering status updates for units entering near-expiry or expired windows. |
| | `NotificationEventListener` | Asynchronous (`@Async`, `@TransactionalEventListener(AFTER_COMMIT)`) event handler decoupling email dispatches from database commits. |
| | `EmailService` | Production SMTP mail sender utilizing `JavaMailSender` and Thymeleaf template rendering with safe fallback logging. |
| **Data Access** | `repository.*` | Interfaces extending `JpaRepository` (`DonorRepository`, `DonationRepository`, `BloodUnitRepository`, `IssueRecordRepository`, `NotificationLogRepository`). |
| **Entities** | `entity.*` | JPA entities (`Donor`, `Donation`, `BloodUnit`, `IssueRecord`, `NotificationLog`) defining tables, relations, and database constraints. |
