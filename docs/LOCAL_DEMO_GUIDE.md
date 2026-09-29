# Blood Bank Inventory & Donor Eligibility Tracker — Local Demo Guide

This guide provides step-by-step instructions for running and evaluating the application locally using MySQL and Swagger UI.

---

## 1. Quick Startup

### Option A: Using the PowerShell Startup Script
```powershell
cd C:\Users\jaget\Downloads\bloodbank\bloodbank
.\scripts\start_app.ps1
```

### Option B: Manual Startup via JAR
```powershell
cd C:\Users\jaget\Downloads\bloodbank\bloodbank

$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

java -jar target\bloodbank-1.0.0.jar --spring.datasource.username=$env:DB_USERNAME --spring.datasource.password=$env:DB_PASSWORD
```

### Option C: Startup via Maven Wrapper
```powershell
cd C:\Users\jaget\Downloads\bloodbank\bloodbank

$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

.\mvnw.cmd spring-boot:run
```

---

## 2. Interactive Interfaces

Once the application logs `Started BloodbankApplication in ... seconds`:

- **Web Application Portal**: [http://localhost:8080/](http://localhost:8080/)
- **Clinical Dashboard**: [http://localhost:8080/dashboard](http://localhost:8080/dashboard)
- **Donor Registry**: [http://localhost:8080/donors](http://localhost:8080/donors)
- **Inventory Stock**: [http://localhost:8080/inventory](http://localhost:8080/inventory)
- **FEFO Blood Issuing**: [http://localhost:8080/issues/new](http://localhost:8080/issues/new)
- **Swagger UI Interactive Documentation**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI v3 Definition**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 3. End-to-End Live Workflow (Swagger UI or cURL)

### Step 1: Check Current Stock Levels
- **Method**: `GET`
- **URL**: `http://localhost:8080/api/inventory/stock`
- **Response**:
```json
{
  "A+": 6,
  "A-": 0,
  "B+": 3,
  "B-": 0,
  "AB+": 0,
  "AB-": 0,
  "O+": 4,
  "O-": 0
}
```

---

### Step 2: Register a New Donor
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/donors`
- **Payload**:
```json
{
  "name": "Arjun Singhania",
  "email": "arjun.singhania@example.org",
  "phone": "+91-9876501234",
  "dateOfBirth": "1994-06-21",
  "gender": "MALE",
  "bloodGroup": "O+",
  "address": "Flat 302, Palm Grove Residences, Indiranagar, Bangalore"
}
```
- **Response**: `201 Created`
```json
{
  "id": 11,
  "donorCode": "DNR-7A3B1289",
  "name": "Arjun Singhania",
  "email": "arjun.singhania@example.org",
  "phone": "+91-9876501234",
  "dateOfBirth": "1994-06-21",
  "gender": "MALE",
  "bloodGroup": "O+",
  "address": "Flat 302, Palm Grove Residences, Indiranagar, Bangalore",
  "active": true,
  "createdAt": "2026-09-28T22:20:00"
}
```

---

### Step 3: Check Medical Eligibility Before Donation
- **Method**: `GET`
- **URL**: `http://localhost:8080/api/donors/11/eligibility`
- **Response**: `200 OK`
```json
{
  "donorId": 11,
  "eligible": true,
  "lastDonationDate": null,
  "nextEligibleDate": null,
  "remainingDays": 0,
  "message": "First-time donor has no previous donations and is fully eligible to donate."
}
```

---

### Step 4: Register Blood Donation (2 Units)
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/donations`
- **Payload**:
```json
{
  "donorId": 11,
  "donationDate": "2026-09-28",
  "numberOfUnits": 2,
  "notes": "Voluntary blood drive donor - vitals normal"
}
```
- **Response**: `201 Created`
```json
{
  "id": 11,
  "donationCode": "DON-C2A84F91",
  "donorId": 11,
  "donorCode": "DNR-7A3B1289",
  "donorName": "Arjun Singhania",
  "bloodGroup": "O+",
  "donationDate": "2026-09-28",
  "numberOfUnits": 2,
  "notes": "Voluntary blood drive donor - vitals normal",
  "bloodUnits": [
    {
      "id": 21,
      "unitCode": "UNT-6D3E4F11",
      "bloodGroup": "O+",
      "status": "AVAILABLE",
      "collectionDate": "2026-09-28",
      "expiryDate": "2026-11-09"
    },
    {
      "id": 22,
      "unitCode": "UNT-9B7C1A22",
      "bloodGroup": "O+",
      "status": "AVAILABLE",
      "collectionDate": "2026-09-28",
      "expiryDate": "2026-11-09"
    }
  ]
}
```

---

### Step 5: Test 90-Day Cooldown Protection
Attempt to register a second donation for the same donor immediately:
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/donations`
- **Payload**:
```json
{
  "donorId": 11,
  "donationDate": "2026-09-28",
  "numberOfUnits": 1,
  "notes": "Illegal second attempt"
}
```
- **Response**: `400 Bad Request`
```json
{
  "timestamp": "2026-09-28T22:21:00",
  "status": 400,
  "error": "DONOR_NOT_ELIGIBLE",
  "message": "Donor has not completed the minimum donation gap of 90 days. 90 day(s) remaining until next eligible donation date (2026-12-27).",
  "path": "/api/donations"
}
```

---

### Step 6: Issue Blood Using FEFO (First-Expiry, First-Out)
- **Method**: `POST`
- **URL**: `http://localhost:8080/api/issues`
- **Payload**:
```json
{
  "bloodGroup": "O+",
  "numberOfUnits": 1,
  "patientName": "Sunita Verma",
  "hospitalName": "St. John National Hospital",
  "notes": "Emergency surgery blood requirement"
}
```
- **Response**: `201 Created`
```json
{
  "status": "SUCCESS",
  "message": "Successfully issued 1 unit(s) of O+ blood using FEFO.",
  "requestedBloodGroup": "O+",
  "numberOfUnitsIssued": 1,
  "patientName": "Sunita Verma",
  "hospitalName": "St. John National Hospital",
  "issuedUnits": [
    {
      "id": 8,
      "issueCode": "ISS-87F9C110",
      "bloodUnitId": 21,
      "unitCode": "UNT-6D3E4F11",
      "requestedBloodGroup": "O+",
      "patientName": "Sunita Verma",
      "hospitalName": "St. John National Hospital",
      "issueDate": "2026-09-28T22:22:00"
    }
  ]
}
```

---

### Step 7: Automated Verification of All 19 Endpoints
To run the automated suite testing all 19 endpoints in sequence:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test_all_localhost.ps1
```

---

## 4. Synthetic Demo Data & Reset Script

The application automatically seeds a realistic synthetic demonstration dataset on startup if the database is empty or demo data is not yet present:
- **Synthetic Donors**: 24 donors across all 8 blood groups (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`) with safe `@example.test` domains.
- **Eligibility Demonstrations**: Demonstrates first-time donors, donors within 90-day cooldown, donors with exactly 90 days elapsed, and inactive donors.
- **Inventory States**: Safe available stock, near-expiry units (< 7 days), expired units, and issued units.
- **FEFO Allocation**: Multiple O+ units with staggered safe expiry dates demonstrating First-Expire First-Out ordering.

### Development Demo Reset Script
To safely reset only synthetic demo records (preserving user-created records and database schema) for a fresh demonstration:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\reset_demo_data.ps1
```
*Note: This script only removes records with `*-DEMO-*` codes and will never delete user-created production data.*

