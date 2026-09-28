# BloodBank Startup Script
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) {
    $entered = Read-Host -Prompt "Enter MySQL password for '$env:DB_USERNAME' (or press Enter if empty)"
    if ($entered) {
        $env:DB_PASSWORD = $entered
    }
}

$javaBin = "C:\Program Files\Java\jdk-26.0.1\bin\java.exe"
if (-not (Test-Path $javaBin)) {
    $javaBin = "java"
}

Write-Host "============================================================"
Write-Host "Starting Blood Bank Spring Boot Application..."
Write-Host "URL: http://localhost:8080"
Write-Host "Swagger UI: http://localhost:8080/swagger-ui/index.html"
Write-Host "============================================================"

& $javaBin -jar target\bloodbank-0.0.1-SNAPSHOT.jar
