# College Assessment & Evaluation Guide

This guide maps the BloodBank Inventory and Donor Eligibility Tracker codebase to the evaluation rubric for college laboratory assessments, viva voce, and project demonstrations.

---

## 1. Technical Implementation (40 Marks)

| Criterion | Implementation Evidence in Codebase | Key File References |
| :--- | :--- | :--- |
| **Spring Boot Framework** | Spring Boot 4.x / Java 21, autoconfiguration, custom configuration properties, component scanning, and scheduling. | `pom.xml`<br/>`BloodbankApplication.java`<br/>`BloodBankProperties.java` |
| **Layered Architecture & CRUD** | Clean REST APIs for Donors, Donations, Inventory, and Issuing without business logic in controllers. | `DonorController.java`<br/>`DonationController.java`<br/>`InventoryController.java`<br/>`IssueController.java` |
| **Database Integration** | Spring Data JPA with Hibernate ORM, connection pooling with HikariCP, and relational mapping to MySQL (and H2 in tests). | `application.properties`<br/>`application-test.properties`<br/>`entity/` |
| **Core Business Rules** | - **90-Day Donor Eligibility Gap**: Calculated in Service layer before saving.<br/>- **42-Day Blood Unit Shelf Life**: Generated upon donation.<br/>- **7-Day Auto Near-Expiry Flagging**: Evaluated via scheduled background job and queries.<br/>- **FEFO Blood Issuing**: Units with earliest safe expiry issued first.<br/>- **Issuing Safety Guards**: Near-expiry, expired, and previously issued units blocked from issuing. | `DonorService.java#checkEligibility`<br/>`DonationService.java#registerDonation`<br/>`InventoryService.java#updateInventoryStatuses`<br/>`IssueService.java#issueBlood` |

---

## 2. System Design & Architecture (25 Marks)

| Criterion | Implementation Evidence in Codebase | Key File References |
| :--- | :--- | :--- |
| **Layered Architecture** | Client → Controller → DTO Validation → Service → Repository → Hibernate/JPA → Database. | `docs/ARCHITECTURE.md`<br/>`docs/diagrams/system_architecture.mmd` |
| **ER Modeling & DB Design** | 4 normalized relational tables in BCNF/3NF (`donors`, `donations`, `blood_units`, `issue_records`) with primary keys, foreign keys, unique constraints, and composite indexes. | `docs/DATABASE_DESIGN.md`<br/>`entity/` |
| **UML Diagrams** | Complete Mermaid diagrams for System Architecture, ER Diagram, Class Diagram, Use Case Diagram, and Sequence Diagrams. | `docs/diagrams/` |
| **Safe API Contracts** | Strict DTO boundaries preventing circular entity serialization and JPA leakage. | `dto/request/`<br/>`dto/response/` |

---

## 3. Code Quality & Efficiency (20 Marks)

| Criterion | Implementation Evidence in Codebase | Key File References |
| :--- | :--- | :--- |
| **Modularity & Clean Code** | Constructor injection (`@RequiredArgsConstructor` with `final` fields), no field injection, no giant classes, clean naming. | `service/`<br/>`controller/` |
| **Data Integrity & Transactions** | `@Transactional` on mutation services ensuring atomic rollbacks if unit creation or issuing fails. | `DonationService.java`<br/>`IssueService.java` |
| **Centralized Error Handling** | `@RestControllerAdvice` converting domain exceptions into uniform HTTP responses (400, 404, 409, 500) without exposing Java stack traces. | `GlobalExceptionHandler.java`<br/>`exception/` |
| **Jakarta Bean Validation** | Declarative validations (`@NotNull`, `@NotBlank`, `@Email`, `@Past`, `@Positive`, `@Pattern`) with `@Valid` on controller request bodies. | `dto/request/` |
| **Query Efficiency & Indexing** | Indexed columns (`blood_group`, `status`, `expiry_date`), composite index `(status, expiry_date)`, and JPQL aggregations preventing full-table in-memory filtering. | `BloodUnitRepository.java`<br/>`BloodUnit.java` |
| **Structured Logging** | SLF4J logging at critical events (donor registered, donation accepted, units generated, unit status updated, blood issued, stock warnings). | All service classes |

---

## 4. Presentation & Communication (15 Marks)

| Criterion | Preparation & Resources | Key File References |
| :--- | :--- | :--- |
| **5-Minute Live Demo** | Step-by-step walkthrough covering donor registration, gap rejection, donation unit generation, stock verification, FEFO issuing, and test execution. | `docs/DEMO_GUIDE.md` |
| **Viva Voce Defense** | Comprehensive questions and concise student-friendly answers covering Spring Boot, JPA, transactions, REST, and DBMS theory. | `docs/VIVA_QUESTIONS.md` |
| **Automated Test Evidence** | 42 automated unit, service, and MockMvc integration tests running against in-memory H2 without requiring live MySQL connection. | `src/test/java/` |
