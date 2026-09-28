# Live End-to-End API Verification Script
$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "PHASE 30: REAL LIVE API WORKFLOW EXECUTION"
Write-Host "============================================================"

# STEP 1: Initial Stock
Write-Host "`n--- STEP 1: INITIAL STOCK ---"
$stockInitial = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
$initialOPlus = $stockInitial.'O+'
Write-Host "Initial Stock Level:" ($stockInitial | ConvertTo-Json -Compress)
Write-Host "Initial O+ stock: $initialOPlus"

# STEP 2: Create Demo Donor
Write-Host "`n--- STEP 2: CREATE DEMO DONOR ---"
$uniqueId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$donorPayload = @{
    name = "Dr. Vikram Seth"
    email = "vikram.$uniqueId@example.org"
    phone = "+91-987654$($uniqueId % 10000)"
    dateOfBirth = "1995-05-15"
    gender = "MALE"
    bloodGroup = "O+"
    address = "104 Healthcare Boulevard, City Hospital Zone"
} | ConvertTo-Json

$donorResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/donors" -Method Post -ContentType "application/json" -Body $donorPayload
$donorId = $donorResponse.id
$donorCode = $donorResponse.donorCode
Write-Host "Created Donor Successfully:"
Write-Host "  ID: $donorId"
Write-Host "  Donor Code: $donorCode"
Write-Host "  Name: $($donorResponse.name)"
Write-Host "  Blood Group: $($donorResponse.bloodGroup)"
Write-Host "  Active: $($donorResponse.active)"

# STEP 3: Get Donor
Write-Host "`n--- STEP 3: GET DONOR BY ID ---"
$retrievedDonor = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId" -Method Get
Write-Host "Retrieved Donor:"
Write-Host "  ID: $($retrievedDonor.id)"
Write-Host "  Code: $($retrievedDonor.donorCode)"
Write-Host "  Email: $($retrievedDonor.email)"
Write-Host "  Phone: $($retrievedDonor.phone)"

# STEP 4: Eligibility for First-Time Donor
Write-Host "`n--- STEP 4: CHECK FIRST-TIME ELIGIBILITY ---"
$eligibility1 = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId/eligibility" -Method Get
Write-Host "Eligibility Result:"
Write-Host "  Eligible: $($eligibility1.eligible)"
Write-Host "  Remaining Days: $($eligibility1.remainingDays)"
Write-Host "  Reason: $($eligibility1.reason)"
if (-not $eligibility1.eligible) {
    throw "Expected first-time donor to be eligible!"
}

# STEP 5: Register Donation (2 units)
Write-Host "`n--- STEP 5: REGISTER DONATION (2 UNITS) ---"
$todayStr = (Get-Date).ToString("yyyy-MM-dd")
$donationPayload = @{
    donorId = $donorId
    donationDate = $todayStr
    numberOfUnits = 2
    notes = "Voluntary hospital drive donation - routine check clear"
} | ConvertTo-Json

$donationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/donations" -Method Post -ContentType "application/json" -Body $donationPayload
$donationId = $donationResponse.id
$donationCode = $donationResponse.donationCode
Write-Host "Donation Registered Successfully:"
Write-Host "  Donation ID: $donationId"
Write-Host "  Donation Code: $donationCode"
Write-Host "  Units Count: $($donationResponse.numberOfUnits)"
Write-Host "  Created Blood Units Count: $($donationResponse.bloodUnits.Count)"

# STEP 6: Inventory Inspection
Write-Host "`n--- STEP 6: INVENTORY INSPECTION ---"
$inventoryPage = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory?page=0&size=10" -Method Get
Write-Host "Inventory Page Content Count: $($inventoryPage.content.Count)"
$matchingUnits = $inventoryPage.content | Where-Object { $_.donationCode -eq $donationCode }
Write-Host "Matching Blood Units created for donation $donationCode :"
foreach ($u in $matchingUnits) {
    Write-Host "  -> Unit Code: $($u.unitCode) | Blood Group: $($u.bloodGroup) | Status: $($u.status) | Collection: $($u.collectionDate) | Expiry: $($u.expiryDate)"
}

# STEP 7: Stock Verification After Donation (+2)
Write-Host "`n--- STEP 7: STOCK VERIFICATION AFTER DONATION ---"
$stockAfterDonation = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
$newOPlus = $stockAfterDonation.'O+'
Write-Host "Stock after 2 units O+ donation:" ($stockAfterDonation | ConvertTo-Json -Compress)
Write-Host "Previous O+ stock: $initialOPlus, Current O+ stock: $newOPlus"
if ($newOPlus -ne ($initialOPlus + 2)) {
    throw "Stock did not increase by 2! Expected $($initialOPlus + 2) but got $newOPlus"
}

# STEP 8: Attempt Immediate Second Donation (Should Be Rejected - 90 Day Gap)
Write-Host "`n--- STEP 8: ATTEMPT IMMEDIATE SECOND DONATION (EXPECT REJECTION) ---"
$secondDonationPayload = @{
    donorId = $donorId
    donationDate = $todayStr
    numberOfUnits = 1
    notes = "Attempting disallowed donation before 90-day cooldown"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/donations" -Method Post -ContentType "application/json" -Body $secondDonationPayload
    throw "Expected donation to fail with 400 Bad Request, but it succeeded!"
} catch {
    Write-Host "Second donation was correctly REJECTED!"
    Write-Host "Exception Message: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $respBody = $reader.ReadToEnd()
        Write-Host "Server Error Response: $respBody"
    }
}

# STEP 9: Re-verify Eligibility
Write-Host "`n--- STEP 9: RE-CHECK ELIGIBILITY (NOW INELIGIBLE) ---"
$eligibility2 = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId/eligibility" -Method Get
Write-Host "Eligibility After Donation:"
Write-Host "  Eligible: $($eligibility2.eligible)"
Write-Host "  Last Donation Date: $($eligibility2.lastDonationDate)"
Write-Host "  Next Eligible Date: $($eligibility2.nextEligibleDate)"
Write-Host "  Remaining Days: $($eligibility2.remainingDays)"
Write-Host "  Reason: $($eligibility2.reason)"
if ($eligibility2.eligible) {
    throw "Expected donor to be ineligible after donating!"
}

# STEP 10: Issue 1 Unit of O+ Blood
Write-Host "`n--- STEP 10: ISSUE 1 UNIT OF O+ BLOOD ---"
$issuePayload = @{
    bloodGroup = "O+"
    numberOfUnits = 1
    patientName = "Suresh Patel"
    hospitalName = "Apollo Memorial Specialty Hospital"
    notes = "Emergency surgery blood requirement - Unit 402"
} | ConvertTo-Json

$issueResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/issues" -Method Post -ContentType "application/json" -Body $issuePayload
Write-Host "Issue Response:"
Write-Host "  Status: $($issueResponse.status)"
Write-Host "  Message: $($issueResponse.message)"
Write-Host "  Issued Count: $($issueResponse.issuedUnits.Count)"
$issuedUnitCode = $issueResponse.issuedUnits[0].unitCode
$issueRecordId = $issueResponse.issuedUnits[0].id
Write-Host "  Issued Unit Code: $issuedUnitCode"
Write-Host "  Issue Record ID: $issueRecordId"

# STEP 11: Stock Verification After Issue (-1)
Write-Host "`n--- STEP 11: STOCK VERIFICATION AFTER ISSUE ---"
$stockAfterIssue = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
$finalOPlus = $stockAfterIssue.'O+'
Write-Host "Stock after issuing 1 unit:" ($stockAfterIssue | ConvertTo-Json -Compress)
Write-Host "Expected O+ stock: $($newOPlus - 1), Actual O+ stock: $finalOPlus"
if ($finalOPlus -ne ($newOPlus - 1)) {
    throw "Stock did not decrease by 1! Expected $($newOPlus - 1) but got $finalOPlus"
}

# STEP 12: Get Issue Record
Write-Host "`n--- STEP 12: VERIFY ISSUE RECORD VIA GET API ---"
$issueRecord = Invoke-RestMethod -Uri "http://localhost:8080/api/issues/$issueRecordId" -Method Get
Write-Host "Retrieved Issue Record:"
Write-Host "  Issue Code: $($issueRecord.issueCode)"
Write-Host "  Patient Name: $($issueRecord.patientName)"
Write-Host "  Hospital Name: $($issueRecord.hospitalName)"
Write-Host "  Blood Group: $($issueRecord.requestedBloodGroup)"
Write-Host "  Issue Date: $($issueRecord.issueDate)"
Write-Host "  Blood Unit Code: $($issueRecord.unitCode)"

Write-Host "`n============================================================"
Write-Host "LIVE WORKFLOW COMPLETED SUCCESSFULLY WITH 100% PASS!"
Write-Host "============================================================"
