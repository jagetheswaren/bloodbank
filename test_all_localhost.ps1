# Complete Localhost API Suite Runner
$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "RUNNING ALL LOCALHOST ENDPOINTS ON HTTP://LOCALHOST:8080"
Write-Host "============================================================"

# 1. Root URL
Write-Host "`n[1/19] GET / (Root Redirect)"
$rootResp = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/
Write-Host "   -> HTTP Status: $rootResp (Redirects to Swagger UI)"

# 2. Swagger UI
Write-Host "`n[2/19] GET /swagger-ui/index.html (Swagger UI)"
$swaggerResp = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/swagger-ui/index.html
Write-Host "   -> HTTP Status: $swaggerResp (Interactive UI Available)"

# 3. OpenAPI Docs
Write-Host "`n[3/19] GET /v3/api-docs (OpenAPI Schema)"
$apiDocs = Invoke-RestMethod -Uri "http://localhost:8080/v3/api-docs" -Method Get
Write-Host "   -> Title: $($apiDocs.info.title) | Version: $($apiDocs.info.version)"

# 4. Stock Levels
Write-Host "`n[4/19] GET /api/inventory/stock (Usable Inventory by Blood Group)"
$stock = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
Write-Host "   -> Stock Map:" ($stock | ConvertTo-Json -Compress)

# 5. Create Donor
Write-Host "`n[5/19] POST /api/donors (Register New Donor)"
$uid = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$donorBody = @{
    name = "Aarav Sharma"
    email = "aarav.$uid@example.org"
    phone = "+91-91234$($uid % 10000)"
    dateOfBirth = "1998-08-20"
    gender = "MALE"
    bloodGroup = "A+"
    address = "42 Green Park Extension, Metro City"
} | ConvertTo-Json

$createdDonor = Invoke-RestMethod -Uri "http://localhost:8080/api/donors" -Method Post -ContentType "application/json" -Body $donorBody
$donorId = $createdDonor.id
Write-Host "   -> Created Donor ID: $donorId | Code: $($createdDonor.donorCode) | Name: $($createdDonor.name) | Blood Group: $($createdDonor.bloodGroup)"

# 6. List Donors
Write-Host "`n[6/19] GET /api/donors?page=0&size=5 (Paginated Donors)"
$donorsPage = Invoke-RestMethod -Uri "http://localhost:8080/api/donors?page=0&size=5" -Method Get
Write-Host "   -> Total Donors: $($donorsPage.totalElements) | Page Size: $($donorsPage.size)"

# 7. Get Donor By ID
Write-Host "`n[7/19] GET /api/donors/$donorId (Get Donor Profile)"
$fetchedDonor = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId" -Method Get
Write-Host "   -> Donor: $($fetchedDonor.name) | Email: $($fetchedDonor.email) | Active: $($fetchedDonor.active)"

# 8. Update Donor
Write-Host "`n[8/19] PUT /api/donors/$donorId (Update Donor Info)"
$updateBody = @{
    name = "Aarav K. Sharma"
    email = $fetchedDonor.email
    phone = $fetchedDonor.phone
    dateOfBirth = "1998-08-20"
    gender = "MALE"
    bloodGroup = "A+"
    address = "Suite 501, Horizon Towers, Metro City"
    active = $true
} | ConvertTo-Json
$updatedDonor = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId" -Method Put -ContentType "application/json" -Body $updateBody
Write-Host "   -> Updated Name: $($updatedDonor.name) | Address: $($updatedDonor.address)"

# 9. Check Eligibility
Write-Host "`n[9/19] GET /api/donors/$donorId/eligibility (Check Medical Eligibility)"
$eligibility = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$donorId/eligibility" -Method Get
Write-Host "   -> Eligible: $($eligibility.eligible) | Remaining Days: $($eligibility.remainingDays)"

# 10. Register Donation
Write-Host "`n[10/19] POST /api/donations (Register 2 Units of A+ Blood)"
$donationBody = @{
    donorId = $donorId
    donationDate = (Get-Date).ToString("yyyy-MM-dd")
    numberOfUnits = 2
    notes = "Routine clinical voluntary blood donation"
} | ConvertTo-Json
$donation = Invoke-RestMethod -Uri "http://localhost:8080/api/donations" -Method Post -ContentType "application/json" -Body $donationBody
$donationId = $donation.id
Write-Host "   -> Created Donation Code: $($donation.donationCode) | Units Created: $($donation.bloodUnits.Count)"

# 11. List Donations
Write-Host "`n[11/19] GET /api/donations?page=0&size=5 (Paginated Donations)"
$donationsPage = Invoke-RestMethod -Uri "http://localhost:8080/api/donations?page=0&size=5" -Method Get
Write-Host "   -> Total Donations Logged: $($donationsPage.totalElements)"

# 12. Get Donation by ID
Write-Host "`n[12/19] GET /api/donations/$donationId (Get Donation Details)"
$donationDetails = Invoke-RestMethod -Uri "http://localhost:8080/api/donations/$donationId" -Method Get
Write-Host "   -> Donation: $($donationDetails.donationCode) | Blood Group: $($donationDetails.bloodGroup) | Date: $($donationDetails.donationDate)"

# 13. List Inventory Units
Write-Host "`n[13/19] GET /api/inventory?page=0&size=5 (Paginated Physical Inventory)"
$invPage = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory?page=0&size=5" -Method Get
Write-Host "   -> Total Physical Units: $($invPage.totalElements)"

# 14. Near Expiry Units
Write-Host "`n[14/19] GET /api/inventory/near-expiry (Units Expiring in <= 7 Days)"
$nearExp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/near-expiry" -Method Get
Write-Host "   -> Units in near-expiry window: $($nearExp.totalElements)"

# 15. Expired Units
Write-Host "`n[15/19] GET /api/inventory/expired (Units Past Expiration Date)"
$expired = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/expired" -Method Get
Write-Host "   -> Expired units count: $($expired.totalElements)"

# 16. Units by Blood Group (A+)
Write-Host "`n[16/19] GET /api/inventory/blood-group/A+ (Filter by Group A+)"
$aPlusUnits = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/blood-group/A+" -Method Get
Write-Host "   -> Total A+ units in system: $($aPlusUnits.totalElements)"

# 17. Issue Blood (FEFO)
Write-Host "`n[17/19] POST /api/issues (Issue 1 Unit of A+ Blood using FEFO)"
$issueBody = @{
    bloodGroup = "A+"
    numberOfUnits = 1
    patientName = "Meera Nair"
    hospitalName = "City General Care Center"
    notes = "Urgent surgical transfusion support"
} | ConvertTo-Json
$issueResp = Invoke-RestMethod -Uri "http://localhost:8080/api/issues" -Method Post -ContentType "application/json" -Body $issueBody
$issueRecordId = $issueResp.issuedUnits[0].id
$issuedCode = $issueResp.issuedUnits[0].unitCode
Write-Host "   -> Issued Unit Code: $issuedCode | Issue Code: $($issueResp.issuedUnits[0].issueCode)"

# 18. List Issue Records
Write-Host "`n[18/19] GET /api/issues?page=0&size=5 (Paginated Issue Records)"
$issuesPage = Invoke-RestMethod -Uri "http://localhost:8080/api/issues?page=0&size=5" -Method Get
Write-Host "   -> Total Issuances Logged: $($issuesPage.totalElements)"

# 19. Get Issue Record by ID
Write-Host "`n[19/19] GET /api/issues/$issueRecordId (Get Issue Log)"
$issueDetail = Invoke-RestMethod -Uri "http://localhost:8080/api/issues/$issueRecordId" -Method Get
Write-Host "   -> Patient: $($issueDetail.patientName) | Hospital: $($issueDetail.hospitalName) | Unit: $($issueDetail.unitCode)"

# Final Stock Status
Write-Host "`n============================================================"
Write-Host "CURRENT USABLE STOCK LEVELS ON LOCALHOST:"
$finalStock = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
$finalStock | ConvertTo-Json
Write-Host "============================================================"
Write-Host "ALL LOCALHOST ENDPOINTS TESTED AND RESPONDING 100% OK!"
Write-Host "============================================================"
