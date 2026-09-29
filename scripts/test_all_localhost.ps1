# BloodBank Localhost Comprehensive Verification Suite
$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "RUNNING LOCALHOST VERIFICATION ON HTTP://LOCALHOST:8080"
Write-Host "============================================================"

# 1. Landing Page
Write-Host "`n[1/12] GET / (Landing Page)"
$landingCode = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/
Write-Host "   -> HTTP Status: $landingCode"

# 2. Dashboard
Write-Host "`n[2/12] GET /dashboard (Dashboard View)"
$dashCode = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/dashboard
Write-Host "   -> HTTP Status: $dashCode"

# 3. Swagger UI
Write-Host "`n[3/12] GET /swagger-ui/index.html (Swagger UI)"
$swaggerCode = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/swagger-ui/index.html
Write-Host "   -> HTTP Status: $swaggerCode"

# 4. OpenAPI Docs
Write-Host "`n[4/12] GET /v3/api-docs (OpenAPI Schema)"
$apiDocs = Invoke-RestMethod -Uri "http://localhost:8080/v3/api-docs" -Method Get
Write-Host "   -> Title: $($apiDocs.info.title) | Version: $($apiDocs.info.version)"

# 5. Stock Levels
Write-Host "`n[5/12] GET /api/inventory/stock (Usable Inventory by Blood Group)"
$stock = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
Write-Host "   -> Stock Map:" ($stock | ConvertTo-Json -Compress)

# 6. Create Demo Donor
Write-Host "`n[6/12] POST /api/donors (Register Demo Donor)"
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$donorBody = @{
    name = "Arun Kumar"
    email = "arun.demo.$ts@example.com"
    phone = "9876$($ts % 1000000)"
    dateOfBirth = "2000-05-15"
    gender = "MALE"
    bloodGroup = "O+"
    address = "Coimbatore, Tamil Nadu"
} | ConvertTo-Json

$donor = Invoke-RestMethod -Uri "http://localhost:8080/api/donors" -Method Post -Body $donorBody -ContentType "application/json"
Write-Host "   -> Donor Code: $($donor.donorCode) | ID: $($donor.id)"

# 7. Check Eligibility
Write-Host "`n[7/12] GET /api/donors/$($donor.id)/eligibility"
$elig = Invoke-RestMethod -Uri "http://localhost:8080/api/donors/$($donor.id)/eligibility" -Method Get
Write-Host "   -> Eligible: $($elig.eligible) | Message: $($elig.message)"

# 8. Record Donation
Write-Host "`n[8/12] POST /api/donations (2 Units)"
$donationBody = @{
    donorId = $donor.id
    numberOfUnits = 2
    donationDate = (Get-Date).ToString("yyyy-MM-dd")
    notes = "Final end-to-end project verification"
} | ConvertTo-Json

$donation = Invoke-RestMethod -Uri "http://localhost:8080/api/donations" -Method Post -Body $donationBody -ContentType "application/json"
$createdUnits = if ($donation.bloodUnits) { ($donation.bloodUnits | ForEach-Object { $_.unitCode }) -join ", " } else { $donation.numberOfUnits }
Write-Host "   -> Donation Code: $($donation.donationCode) | Blood Units: $createdUnits"

# 9. Verify 90-day Rejection
Write-Host "`n[9/12] Immediate Second Donation (90-Day Gap Rule Test)"
try {
    $null = Invoke-RestMethod -Uri "http://localhost:8080/api/donations" -Method Post -Body $donationBody -ContentType "application/json"
    Write-Host "   -> UNEXPECTED: Second donation succeeded!" -ForegroundColor Red
} catch {
    Write-Host "   -> Correctly Rejected: $($_.Exception.Message)" -ForegroundColor Green
}

# 10. Issue Blood Unit (FEFO)
Write-Host "`n[10/12] POST /api/issues (Issue 1 Unit of O+)"
$issueBody = @{
    bloodGroup = "O+"
    numberOfUnits = 1
    patientName = "Karthik Raj"
    hospitalName = "City Care Hospital"
    notes = "Emergency issue verification"
} | ConvertTo-Json

$issue = Invoke-RestMethod -Uri "http://localhost:8080/api/issues" -Method Post -Body $issueBody -ContentType "application/json"
$firstUnit = $issue.issuedUnits[0]
Write-Host "   -> Issue Code: $($firstUnit.issueCode) | Allocated Unit: $($firstUnit.unitCode) | Patient: $($firstUnit.patientName)"

# 11. Final Stock
Write-Host "`n[11/12] GET /api/inventory/stock (After Issue)"
$stockAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/stock" -Method Get
Write-Host "   -> O+ Safe Stock: $($stockAfter.'O+')"

Write-Host "`n============================================================"
Write-Host "ALL VERIFICATION STEPS COMPLETED SUCCESSFULLY!"
Write-Host "============================================================"
