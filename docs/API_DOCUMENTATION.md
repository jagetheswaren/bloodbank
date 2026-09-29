# BloodBank REST API Documentation

## 1. Overview
The BloodBank backend exposes a clean, intuitive RESTful API adhering to JSON standards, semantic HTTP verbs, and consistent status codes.

- **Base URL**: `http://localhost:8080`
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3.1 JSON Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 2. API Endpoints Summary

| Module | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Donor** | `POST` | `/api/donors` | Register a new donor |
| | `GET` | `/api/donors` | List donors with pagination & sorting |
| | `GET` | `/api/donors/{id}` | Get donor profile by ID |
| | `PUT` | `/api/donors/{id}` | Update donor details |
| | `DELETE` | `/api/donors/{id}` | Deactivate donor (safe soft delete) |
| | `GET` | `/api/donors/{id}/eligibility` | Check donor eligibility (90-day gap) |
| **Donation** | `POST` | `/api/donations` | Record donation & generate units |
| | `GET` | `/api/donations` | List donations with pagination |
| | `GET` | `/api/donations/{id}` | Get donation record by ID |
| | `GET` | `/api/donations/donor/{donorId}` | List donations for specific donor |
| **Inventory** | `GET` | `/api/inventory` | List all inventory units |
| | `GET` | `/api/inventory/stock` | Real-time stock counts for all 8 blood groups |
| | `GET` | `/api/inventory/near-expiry` | Units expiring within 7 days |
| | `GET` | `/api/inventory/expired` | Units past expiry date |
| | `GET` | `/api/inventory/blood-group/{bg}` | Units filtered by blood group |
| | `GET` | `/api/inventory/unit/{unitCode}` | Unit details by unit code |
| **Blood Issue**| `POST` | `/api/issues` | Issue units using FEFO algorithm |
| | `GET` | `/api/issues` | List issue audit records |
| | `GET` | `/api/issues/{id}` | Get issue record by ID |
| **Notifications** | `GET` | `/api/notifications` | List recent email notification audit records |
| | `GET` | `/api/notifications/status` | Get email notification subsystem status |

---

## 3. Detailed Endpoint Specifications

### 3.1. Donor Management

#### `POST /api/donors`
Register a new blood donor.

**Request Body**:
```json
{
  "name": "Evelyn Reed",
  "email": "evelyn.reed@example.com",
  "phone": "+15551234567",
  "dateOfBirth": "1994-06-15",
  "gender": "FEMALE",
  "bloodGroup": "O+",
  "address": "456 Elm Street, Springfield"
}
```

**Response (201 Created)**:
```json
{
  "id": 1,
  "donorCode": "DNR-B4A198CD",
  "name": "Evelyn Reed",
  "email": "evelyn.reed@example.com",
  "phone": "+15551234567",
  "dateOfBirth": "1994-06-15",
  "gender": "FEMALE",
  "bloodGroup": "O+",
  "address": "456 Elm Street, Springfield",
  "active": true,
  "createdAt": "2026-09-28T10:45:00",
  "updatedAt": "2026-09-28T10:45:00"
}
```

---

#### `GET /api/donors/{id}/eligibility`
Evaluates whether a donor is eligible to donate.

**Response (200 OK — First-time Eligible)**:
```json
{
  "donorId": 1,
  "donorCode": "DNR-B4A198CD",
  "donorName": "Evelyn Reed",
  "eligible": true,
  "lastDonationDate": null,
  "nextEligibleDate": "2026-09-28",
  "remainingDays": 0,
  "message": "First-time donor has no previous donations and is fully eligible to donate."
}
```

**Response (200 OK — Ineligible within 90-Day Gap)**:
```json
{
  "donorId": 1,
  "donorCode": "DNR-B4A198CD",
  "donorName": "Evelyn Reed",
  "eligible": false,
  "lastDonationDate": "2026-09-28",
  "nextEligibleDate": "2026-12-27",
  "remainingDays": 90,
  "message": "Donor has not completed the minimum donation gap of 90 days. 90 day(s) remaining until next eligible donation date (2026-12-27)."
}
```

---

### 3.2. Donation Management

#### `POST /api/donations`
Registers a donation, enforces pre-save eligibility, and generates blood units.

**Request Body**:
```json
{
  "donorId": 1,
  "donationDate": "2026-09-28",
  "numberOfUnits": 2,
  "notes": "Annual community blood drive"
}
```

**Response (201 Created)**:
```json
{
  "id": 1,
  "donationCode": "DON-C28F5041",
  "donorId": 1,
  "donorCode": "DNR-B4A198CD",
  "donorName": "Evelyn Reed",
  "bloodGroup": "O+",
  "donationDate": "2026-09-28",
  "numberOfUnits": 2,
  "notes": "Annual community blood drive",
  "bloodUnits": [
    {
      "id": 1,
      "unitCode": "UNT-6B9A897E",
      "donationCode": "DON-C28F5041",
      "bloodGroup": "O+",
      "collectionDate": "2026-09-28",
      "expiryDate": "2026-11-09",
      "status": "AVAILABLE",
      "createdAt": "2026-09-28T10:46:00",
      "updatedAt": "2026-09-28T10:46:00"
    },
    {
      "id": 2,
      "unitCode": "UNT-7F3C1210",
      "donationCode": "DON-C28F5041",
      "bloodGroup": "O+",
      "collectionDate": "2026-09-28",
      "expiryDate": "2026-11-09",
      "status": "AVAILABLE",
      "createdAt": "2026-09-28T10:46:00",
      "updatedAt": "2026-09-28T10:46:00"
    }
  ],
  "createdAt": "2026-09-28T10:46:00"
}
```

**Response (400 Bad Request — Ineligible)**:
```json
{
  "timestamp": "2026-09-28T10:47:00",
  "status": 400,
  "error": "DONOR_NOT_ELIGIBLE",
  "message": "Donor has not completed the minimum donation gap of 90 days. 90 day(s) remaining until next eligible donation date (2026-12-27).",
  "path": "/api/donations"
}
```

---

#### `GET /api/donations/donor/{donorId}`
Retrieves all historical donation sessions recorded for a specific donor, ordered chronologically descending.

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "donationCode": "DON-C28F5041",
    "donorId": 1,
    "donorCode": "DNR-B4A198CD",
    "donorName": "Evelyn Reed",
    "bloodGroup": "O+",
    "donationDate": "2026-09-28",
    "numberOfUnits": 2,
    "notes": "Annual community blood drive",
    "bloodUnits": [
      {
        "id": 1,
        "unitCode": "UNT-6B9A897E",
        "donationCode": "DON-C28F5041",
        "bloodGroup": "O+",
        "collectionDate": "2026-09-28",
        "expiryDate": "2026-11-09",
        "status": "AVAILABLE",
        "createdAt": "2026-09-28T10:46:00",
        "updatedAt": "2026-09-28T10:46:00"
      }
    ],
    "createdAt": "2026-09-28T10:46:00"
  }
]
```

---

### 3.3. Inventory Management

#### `GET /api/inventory/stock`
Returns real-time counts of genuinely safe, issueable `AVAILABLE` units across all eight blood groups.

**Response (200 OK)**:
```json
{
  "A+": 0,
  "A-": 0,
  "B+": 0,
  "B-": 0,
  "AB+": 0,
  "AB-": 0,
  "O+": 2,
  "O-": 0
}
```

---

### 3.4. Blood Issuing (FEFO)

#### `POST /api/issues`
Issues blood units to a patient using First Expire, First Out (FEFO) allocation.

**Request Body**:
```json
{
  "bloodGroup": "O+",
  "numberOfUnits": 1,
  "patientName": "Marcus Vance",
  "hospitalName": "Memorial Hospital",
  "notes": "Emergency cardiovascular surgery"
}
```

**Response (201 Created)**:
```json
{
  "patientName": "Marcus Vance",
  "hospitalName": "Memorial Hospital",
  "requestedBloodGroup": "O+",
  "numberOfUnitsRequested": 1,
  "numberOfUnitsIssued": 1,
  "issuedUnits": [
    {
      "id": 1,
      "issueCode": "ISS-E3D2A109",
      "unitCode": "UNT-6B9A897E",
      "bloodGroup": "O+",
      "requestedBloodGroup": "O+",
      "patientName": "Marcus Vance",
      "hospitalName": "Memorial Hospital",
      "issueDate": "2026-09-28T10:50:00",
      "notes": "Emergency cardiovascular surgery",
      "createdAt": "2026-09-28T10:50:00"
    }
  ],
  "message": "Successfully issued 1 unit(s) of O+ blood."
}
```

**Response (400 Bad Request — Insufficient Stock)**:
```json
{
  "timestamp": "2026-09-28T10:51:00",
  "status": 400,
  "error": "INSUFFICIENT_STOCK",
  "message": "Insufficient safe inventory for blood group O+. Requested: 10 unit(s), Available: 1 unit(s). Expired and near-expiry units cannot be issued.",
  "path": "/api/issues"
}
```

---

### 3.5. Notification Subsystem

#### `GET /api/notifications`
Retrieves the latest 50 email notification attempts (Sent, Failed, or Skipped) for audit inspection. Never exposes SMTP passwords or secrets.

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "notificationType": "DONATION_RECORDED",
    "recipient": "marcus.donor@example.test",
    "subject": "BloodBank — Donation Recorded Successfully",
    "relatedEntityType": "DONATION",
    "relatedEntityId": 1,
    "status": "SENT",
    "sentAt": "2026-09-28T10:46:02",
    "failureReason": null,
    "createdAt": "2026-09-28T10:46:01"
  }
]
```

---

#### `GET /api/notifications/status`
Checks whether real email notifications and SMTP sending are active, along with administrative email targets and total recorded notification logs.

**Response (200 OK)**:
```json
{
  "mailEnabled": false,
  "fromAddress": "no-reply@bloodbank.org",
  "adminRecipient": "admin@bloodbank.org",
  "eligibilityRemindersEnabled": false,
  "totalLogsRecorded": 12
}
```

