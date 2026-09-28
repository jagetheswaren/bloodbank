# BloodBank Production Startup Script
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -Path $projectRoot

if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) {
    $entered = Read-Host -Prompt "Enter MySQL password for '$env:DB_USERNAME' (or press Enter if empty)"
    if ($entered) {
        $env:DB_PASSWORD = $entered
    }
}

$jarPath = "target\bloodbank-1.0.0.jar"
if (-not (Test-Path $jarPath)) {
    $jarPath = "target\bloodbank-0.0.1-SNAPSHOT.jar"
}

Write-Host "============================================================"
Write-Host "Starting Blood Bank Spring Boot Application..."
Write-Host "Web Portal: http://localhost:8080"
Write-Host "Dashboard:  http://localhost:8080/dashboard"
Write-Host "Swagger UI: http://localhost:8080/swagger-ui/index.html"
Write-Host "============================================================"

java -jar $jarPath
