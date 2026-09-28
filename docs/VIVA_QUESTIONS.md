# Viva Voce Examination Guide

Comprehensive, student-friendly questions and answers covering all architectural, design, framework, and database concepts implemented in this assessment project.

---

### 1. Spring Boot & Architecture

#### What is Spring Boot?
Spring Boot is an open-source Java framework built on top of the Spring framework. It simplifies building production-ready stand-alone applications by providing pre-configured starter dependencies and embedded servlet containers (such as Apache Tomcat).

#### Why Spring Boot?
Spring Boot eliminates complex XML configuration through **auto-configuration**, provides opinionated starters, includes embedded servers so applications can be run as a standalone JAR, and offers production features like health checks and metrics.

#### What is REST?
REST stands for **Representational State Transfer**. It is an architectural style for distributed hypermedia systems that relies on stateless communication, standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`), and resource-oriented URIs.

#### What is a REST API?
A REST API is an application programming interface that follows REST principles, enabling clients (browsers, mobile apps, Postman) to exchange structured data (typically JSON) with backend servers over HTTP.

#### Controller vs Service vs Repository?
- **Controller**: The entry point for HTTP requests. It deserializes JSON, triggers validation, and delegates to the service layer. It contains **no** business logic.
- **Service**: The business logic brain. It implements validation rules (e.g. 90-day donation gap, FEFO algorithm), coordinates transactions, and controls business workflows.
- **Repository**: The persistence abstraction. It handles data access operations, executes SQL queries, and manages entity persistence using Spring Data JPA.

#### What is Dependency Injection (DI)?
Dependency Injection is an Inversion of Control (IoC) design pattern where Spring's IoC container automatically provides required dependent objects to a class at runtime rather than having the class instantiate them using `new`.

#### What is Constructor Injection?
Constructor injection passes dependencies into a class via its constructor. In our application, we use Lombok's `@RequiredArgsConstructor` with `final` fields. It ensures dependencies cannot be null, makes classes immutable and thread-safe, and facilitates unit testing with mocks without needing reflection.

---

### 2. DTOs & Validation

#### Why use DTOs (Data Transfer Objects)?
DTOs decouple internal database entities from external API contracts. They allow us to control exactly what data clients send and receive, preventing over-fetching and unintended field updates.

#### Why don't we return JPA Entities directly through REST APIs?
1. **Security**: Internal database IDs or sensitive properties should not be exposed.
2. **Circular Reference**: Bidirectional relationships (e.g., `Donor` has `List<Donation>`, and `Donation` has `Donor`) cause infinite recursion and `StackOverflowError` during Jackson JSON serialization.
3. **LazyInitializationException**: Accessing lazily loaded child collections outside active transactions triggers runtime Hibernate exceptions.

#### What is `@Valid`?
`@Valid` is a standard Jakarta Bean Validation annotation placed on controller parameters. It instructs Spring's dispatcher servlet to automatically validate incoming request bodies against the validation constraints declared on the DTO before entering the controller method.

#### What is Bean Validation?
Bean Validation (Jakarta Validation / Hibernate Validator) is a declarative metadata specification. It uses annotations like `@NotBlank`, `@NotNull`, `@Email`, `@PastOrPresent`, `@Positive`, and `@Size` directly on POJO fields to enforce data integrity.

---

### 3. JPA, Hibernate & Transactions

#### What is JPA?
JPA (**Jakarta Persistence API**) is a standard Java specification that defines how Java objects (entities) map to relational database tables (ORM).

#### What is Hibernate?
Hibernate is the industry-standard Object-Relational Mapping (ORM) framework that provides the concrete runtime implementation of the JPA specification.

#### What is `JpaRepository`?
`JpaRepository` is an interface provided by Spring Data JPA that exposes standard CRUD operations, pagination, sorting, and query execution methods (`save`, `findById`, `findAll`, `deleteById`) without writing boilerplate DAO implementation classes.

#### What is `@Transactional`?
`@Transactional` defines the boundary of a database transaction. It ensures that all database modifications within a method follow **ACID** (Atomicity, Consistency, Isolation, Durability) properties. If an unchecked exception occurs, all database changes within the transaction are rolled back.

#### Why is donation registration transactional?
When a donor donates blood, the system must:
1. Save the `Donation` record.
2. Generate and persist `N` individual `BloodUnit` records.
If unit generation fails halfway (e.g., database constraint failure or server crash), `@Transactional` guarantees the entire operation rolls back, preventing orphaned donation records or corrupt inventory.

#### What is `@ManyToOne`, `@OneToMany`, and `@OneToOne`?
- `@ManyToOne`: Multiple child entities relate to one parent entity (e.g. many `Donation`s belong to one `Donor`).
- `@OneToMany`: One parent entity owns multiple child entities (e.g. one `Donation` has many `BloodUnit`s).
- `@OneToOne`: Exactly one entity maps to one other entity (e.g. one `BloodUnit` corresponds to at most one `IssueRecord`).

---

### 4. Database & Relational Design

#### Primary Key vs Foreign Key?
- **Primary Key**: A column (or set of columns) that uniquely identifies each row in a database table. It cannot contain `NULL` values.
- **Foreign Key**: A column that establishes a link between data in two tables by referencing the primary key of another table, enforcing referential integrity.

#### What is a Unique Constraint?
A database rule ensuring all values in a column or set of columns are unique across all rows (e.g., `email`, `phone`, `donor_code`, and `unit_code`).

#### What is a Database Index?
A data structure (typically a B-Tree) created on specific columns to significantly speed up row retrieval without scanning the entire table. In our application, we index `blood_group`, `status`, and `expiry_date` on `blood_units` to optimize FEFO allocation queries.

#### What is Normalization?
Normalization is the process of organizing database tables to reduce data redundancy and eliminate anomalies (insertion, update, deletion anomalies). Our schema is normalized to 3NF/BCNF.

---

### 5. BloodBank Business Logic & Rules

#### What is an enum, and why use the `BloodGroup` enum?
An enum is a special Java type representing a fixed set of constants. We use `BloodGroup` (`A_POSITIVE`, `O_NEGATIVE`, etc.) to eliminate typos, guarantee compile-time type safety, and restrict inputs to the 8 valid human blood groups. We use `@JsonValue` and `@JsonCreator` to serialize/deserialize user-friendly strings like `"A+"` and `"O-"`.

#### How is donor eligibility calculated?
1. Fetch donor and verify active status.
2. Retrieve the latest donation record for the donor.
3. If no previous donation exists, donor is immediately eligible.
4. If a previous donation exists, calculate `nextEligibleDate = lastDonationDate + 90 days`.
5. If `today >= nextEligibleDate`, donor is eligible; otherwise, calculate `remainingDays` and reject.

#### Why is eligibility verified in the Service Layer?
The Service layer owns business rules. Checking eligibility in the Controller would leak business logic into the HTTP routing layer. Furthermore, verifying eligibility inside `@Transactional` in the service guarantees that eligibility is verified **before** any database write occurs.

#### How does blood expiry work?
Every collected `BloodUnit` has an `expiryDate` calculated as `collectionDate + 42 days` (configurable shelf life).

#### What is `NEAR_EXPIRY`?
A blood unit is flagged as `NEAR_EXPIRY` when its remaining shelf life is within the configured window of 7 days (`expiryDate <= today + 7 days`).

#### Why can't `NEAR_EXPIRY` blood be issued?
Blood nearing expiry poses clinical risks because patient transport, cross-matching, infusion duration, and biological efficacy require adequate shelf life. To ensure patient safety, our system strictly blocks issuing `NEAR_EXPIRY` and `EXPIRED` units.

#### How does FEFO work?
FEFO stands for **First Expire, First Out**. When blood is issued, the system queries safe `AVAILABLE` units sorted by `expiryDate ASC`. The unit with the earliest safe expiry date is allocated first, minimizing inventory wastage.

#### How does current stock calculation work?
`GET /api/inventory/stock` aggregates counts of genuinely safe `AVAILABLE` units grouped by blood group. It filters out `EXPIRED`, `NEAR_EXPIRY`, `ISSUED`, and `DISCARDED` units, and verifies actual expiry dates against current system time.

#### How does the scheduler work?
`InventoryStatusScheduler` runs periodically in the background (configured with `@Scheduled(fixedRate = 60000)`). It sweeps active inventory, auto-flagging units entering the 7-day window as `NEAR_EXPIRY` and past-due units as `EXPIRED`.

#### What is `@EnableScheduling`?
A Spring configuration annotation that activates background task execution and enables Spring to discover and execute methods annotated with `@Scheduled`.

#### What is `@RestControllerAdvice`?
An annotation that allows centralized, global exception handling across all `@RestController` classes. Methods annotated with `@ExceptionHandler` intercept specific exceptions and return uniform JSON error responses.

#### How is duplicate blood issuing prevented?
1. **Logical Check**: The service checks that `status == AVAILABLE` and verifies no issue record exists.
2. **State Transition**: The unit status is immediately set to `ISSUED`.
3. **Database Constraint**: `issue_records` has a `UNIQUE (blood_unit_id)` constraint that physically prevents two rows from referencing the same blood unit.

---

### 6. Build, Tooling & Testing

#### What does Maven do?
Apache Maven is a build automation and dependency management tool. It manages project dependencies, compiles Java code, runs automated tests, and packages the application into an executable JAR.

#### What is `pom.xml`?
`pom.xml` (Project Object Model) is the core Maven configuration file containing project metadata, dependencies, plugins, and build profiles.

#### What is Swagger / OpenAPI?
OpenAPI is a machine-readable API specification standard. Swagger UI is an interactive web interface generated automatically from OpenAPI metadata, allowing developers to inspect and test REST endpoints directly in a browser.

#### Why use H2 for testing and MySQL for production?
- **H2**: An in-memory, zero-configuration database that runs directly inside the JVM. It allows our 36 automated unit and integration tests to execute instantly without requiring a live external MySQL database.
- **MySQL**: An enterprise-grade, persistent relational database management system designed for production data durability, concurrent transactions, and scale.
