param(
    [string]$BackendUrl = "http://localhost:8080",
    [string]$WebUrl = "http://localhost:5173"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $repoRoot

function Invoke-Check {
    param(
        [string]$Name,
        [scriptblock]$Script
    )

    Write-Host "==> $Name"
    & $Script
    Write-Host "PASS: $Name"
}

Invoke-Check "No stale backend layer package imports" {
    $result = rg "edu\.cit\.barcenas\.queuems\.(controller|service|dto)" backend/src/main/java backend/src/test/java
    if ($LASTEXITCODE -eq 0) {
        throw "Found stale backend layer imports."
    }
}

Invoke-Check "No stale web service/page import aliases" {
    $result = rg "@/features/dashboard/pages|from '../api'|from './api'|from './admin|from './teller|from './users" web/src
    if ($LASTEXITCODE -eq 0) {
        throw "Found stale web imports."
    }
}

Invoke-Check "No stale Android layer package references" {
    $result = rg "\.ui\.|\.repository\.|\.viewmodel\.|\.service\.|\.utils\.|features\.requests\.(api|utils|service|features|viewmodel)" mobile/app/src/main mobile/app/src/test mobile/app/src/androidTest
    if ($LASTEXITCODE -eq 0) {
        throw "Found stale Android package references."
    }
}

Invoke-Check "Required backend endpoints are present" {
    $required = @(
        "@RequestMapping(`"api/auth`")",
        "@RequestMapping(`"api/requests`")",
        "@RequestMapping(`"api/counters`")",
        "@RequestMapping(`"api/teller`")",
        "@RequestMapping(`"api/admin`")",
        "@RequestMapping(`"api/holidays`")",
        "@PutMapping(`"/fcm-token`")",
        "@PostMapping(value = `"/{id}/attachment`""
    )

    $source = (Get-ChildItem backend/src/main/java/edu/cit/barcenas/queuems/feature -Recurse -Filter *.java |
        ForEach-Object { Get-Content -Raw $_.FullName }) -join "`n"

    foreach ($needle in $required) {
        if ($source -notmatch [regex]::Escape($needle)) {
            throw "Missing endpoint marker: $needle"
        }
    }
}

Invoke-Check "Git whitespace check" {
    git diff --check
}

Write-Host ""
Write-Host "Smoke checks completed. Run full build/test commands from docs/Software_Test_Plan_QueueMS.md before final submission."
