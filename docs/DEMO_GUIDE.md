# 5-Minute Project Demonstration Guide

This guide provides an exact, step-by-step presentation script designed for a 5-minute college viva demonstration.

---

## Preparation (Before Demo)

Open two terminal windows (PowerShell) and a browser:
1. **Terminal 1 (Application)**: Directory `.../bloodbank/bloodbank`
2. **Terminal 2 (Database or Test runner)**: Directory `.../bloodbank/bloodbank`
3. **Browser**: Tab ready at `http://localhost:8080/swagger-ui/index.html`

---

## 5-Minute Step-by-Step Presentation Script

### Step 1: Explain Architecture (30 Seconds)
- Open `docs/ARCHITECTURE.md`.
- Explain: *"Our project follows a standard enterprise layered Spring Boot architecture: Controller → DTO Validation → Service → Repository → Spring Data JPA/Hibernate → MySQL. The controller contains zero business logic; business rules like the 90-day donation gap and FEFO issuing are strictly encapsulated in the Service layer."*

### Step 2: Show MySQL Database Schema (30 Seconds)
- If MySQL is online:
  ```powershell
  & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -u root -p
  USE bloodbank_db;
  SHOW TABLES;
  ```
- Mention: *"We have four normalized tables in BCNF: `donors`, `donations`, `blood_units`, and `issue_records`. Foreign keys and unique constraints protect relational integrity."*

### Step 3: Start Spring Boot (30 Seconds)
- Launch the application:
  ```powershell
  # Using local MySQL:
  $env:DB_USERNAME="root"
  $env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
  .\mvnw.cmd spring-boot:run

  # Or standalone test profile (H2 in-memory):
  java -jar target\bloodbank-0.0.1-SNAPSHOT.jar --spring.profiles.active=test --server.port=8080
  ```
- Point out: *"Tomcat initializes on port 8080. Hibernate creates/updates the tables, and Spring Data JPA discovers all 4 repositories."*

### Step 4: Open Swagger UI (15 Seconds)
- In the browser, navigate to:
  [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Show the 4 tag sections:
  - **Donor Management** (`/api/donors`)
  - **Donation Management** (`/api/donations`)
  - **Inventory Management** (`/api/inventory`)
  - **Blood Issuing** (`/api/issues`)

### Step 5: Register a Donor (POST /api/donors) (20 Seconds)
- In Swagger UI, expand `POST /api/donors` → **Try it out**.
- Enter JSON:
  ```json
  {
    "name": "Sarah Jenkins",
    "email": "sarah.jenkins@example.com",
    "phone": "+19876543210",
    "dateOfBirth": "1995-08-20",
    "gender": "FEMALE",
    "bloodGroup": "O+",
    "address": "124 Baker Street"
  }
  ```
- Click **Execute**. Show **201 Created**, with generated `donorCode: "DNR-..."` and `id: 1`.

### Step 6: Retrieve Donor (GET /api/donors/{id}) (15 Seconds)
- Expand `GET /api/donors/1` → **Execute**. Show the donor details.

### Step 7 & 8: Check Eligibility (GET /api/donors/{id}/eligibility) (20 Seconds)
- Expand `GET /api/donors/1/eligibility` → **Execute**.
- Point out the response:
  ```json
  {
    "donorId": 1,
    "eligible": true,
    "lastDonationDate": null,
    "remainingDays": 0,
    "message": "First-time donor has no previous donations and is fully eligible to donate."
  }
  ```
- Explain: *"Because this donor has no previous donations on record, the service confirms they are eligible immediately."*

### Step 9 & 10: Record Donation & Show Generated Units (POST /api/donations) (30 Seconds)
- Expand `POST /api/donations` → **Try it out**.
- Enter JSON:
  ```json
  {
    "donorId": 1,
    "donationDate": "2026-09-28",
    "numberOfUnits": 2,
    "notes": "Healthy donor volunteer"
  }
  ```
- Click **Execute**. Show **201 Created**.
- Point out:
  1. `donationCode: "DON-..."` created.
  2. `bloodUnits` array contains **2 individual BloodUnit objects**.
  3. Each unit has a unique `unitCode: "UNT-..."`.
  4. `collectionDate` is today, and `expiryDate` is automatically set 42 days later (`2026-11-09`).
  5. `status: "AVAILABLE"`.
  6. The complete operation ran atomically inside a `@Transactional` block.

### Step 11 & 12: Check Inventory Stock (GET /api/inventory/stock) (20 Seconds)
- Expand `GET /api/inventory/stock` → **Execute**.
- Show that all eight blood groups are listed, and **O+ stock increased to 2**:
  ```json
  {
    "A+": 0, "A-": 0, "B+": 0, "B-": 0,
    "AB+": 0, "AB-": 0, "O+": 2, "O-": 0
  }
  ```

### Step 13 & 14: Attempt Immediate Second Donation (Gap Rejection) (30 Seconds)
- Go back to `POST /api/donations` and click **Execute** with the exact same payload for donor 1.
- Show **400 Bad Request**:
  ```json
  {
    "status": 400,
    "error": "DONOR_NOT_ELIGIBLE",
    "message": "Donor has not completed the minimum donation gap of 90 days. 90 day(s) remaining until next eligible donation date...",
    "path": "/api/donations"
  }
  ```
- Highlight: *"Our critical business rule works: the Service layer intercepted the request BEFORE database persistence and rejected it with 90 remaining days!"*

### Step 15 & 16: Issue Blood (POST /api/issues) (30 Seconds)
- Expand `POST /api/issues` → **Try it out**.
- Enter JSON:
  ```json
  {
    "bloodGroup": "O+",
    "numberOfUnits": 1,
    "patientName": "David Miller",
    "hospitalName": "Apollo Specialty Hospital",
    "notes": "Emergency surgery"
  }
  ```
- Click **Execute**. Show **201 Created**:
  - `numberOfUnitsIssued: 1`
  - Unit `UNT-...` assigned with `issueCode: "ISS-..."`.
  - In inventory, that unit's status is permanently updated to `ISSUED`.
  - Unique constraint on `blood_unit_id` prevents any unit from ever being re-issued.

### Step 17 & 18: Verify Stock Decreased (GET /api/inventory/stock) (15 Seconds)
- Execute `GET /api/inventory/stock` again.
- Show that `O+` stock has decreased from **2 to 1**.

### Step 19: Explain Expiry & Near-Expiry Protection (20 Seconds)
- Explain: *"If a unit has expired or is nearing expiry within 7 days, our FEFO query and service filters automatically exclude it from stock and refuse to issue it. The `@Scheduled` task in `InventoryStatusScheduler` continuously sweeps and flags units nearing expiry."*

### Step 20: Run Automated Tests (20 Seconds)
- In Terminal 2, run:
  ```powershell
  .\mvnw.cmd test
  ```
- Show: **46 tests passed, 0 failures, BUILD SUCCESS**.
- Conclude: *"All business rules and edge cases are verified with JUnit 5 and MockMvc integration tests."*
