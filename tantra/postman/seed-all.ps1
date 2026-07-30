# Seed the entire catalog by running the Postman collections in order with Newman (Postman's CLI).
# Prereqs: the app is running, and (for a clean run) a fresh DB. Needs Node.js (uses `npx newman`).
#
# Usage:
#   .\seed-all.ps1                          # against http://localhost:8080
#   .\seed-all.ps1 http://192.168.1.5:8080

param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Order matters: catalog modules first, then the shared business-profile form, then sample addresses.
$collections = @(
  "00_Agriculture_Seed",
  "12_AnimalLivestock_Seed",
  "11_BusinessProfile",
  "10_Address"
)

Write-Host "Seeding Tantra catalog against $BaseUrl"
foreach ($c in $collections) {
  Write-Host ""
  Write-Host "==================== > $c ===================="
  npx --yes newman run "$dir\$c.postman_collection.json" --env-var "baseUrl=$BaseUrl" --reporters cli
  if ($LASTEXITCODE -ne 0) { throw "Collection $c failed" }
}

Write-Host ""
Write-Host "Done. Catalog seeded."
