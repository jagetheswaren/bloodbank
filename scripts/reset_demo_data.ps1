# Optional Development/Demo-Only Reset Script
# Removes ONLY synthetic demo records (prefixed with DNR-DEMO-, DON-DEMO-, UNT-DEMO-, ISS-DEMO-)
# and restarts the application to re-seed clean demonstration data.
# NEVER run in production.

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -Path $projectRoot

Write-Host "============================================================"
Write-Host "BLOODBANK SYNTHETIC DEMO DATA RESET"
Write-Host "============================================================"
Write-Host "This will remove only synthetic demonstration records and preserve all schema and real records."

$confirmation = Read-Host "Are you sure you want to clean and re-seed demo data? (y/N)"
if ($confirmation -ne "y" -and $confirmation -ne "Y") {
    Write-Host "Operation cancelled."
    exit 0
}

if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) {
    $entered = Read-Host -Prompt "Enter MySQL password for '$env:DB_USERNAME' (or press Enter if empty)"
    if ($entered) {
        $env:DB_PASSWORD = $entered
    }
}

$cleanupSql = @"
USE bloodbank_db;
DELETE FROM issue_records WHERE issue_code LIKE 'ISS-DEMO-%';
DELETE FROM blood_units WHERE unit_code LIKE 'UNT-DEMO-%';
DELETE FROM donations WHERE donation_code LIKE 'DON-DEMO-%';
DELETE FROM donors WHERE donor_code LIKE 'DNR-DEMO-%';
"@

$passArg = if ($env:DB_PASSWORD) { "-p$env:DB_PASSWORD" } else { "" }
& mysql -u $env:DB_USERNAME $passArg -e $cleanupSql 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Synthetic demo records deleted successfully."
    Write-Host "Next time the application starts with bloodbank.demo.seed-enabled=true, demo data will be re-created."
} else {
    Write-Host "MySQL clean failed or MySQL not running."
}
