# 05-add-remaining-issues.ps1
# Resume: add remaining repo issues to project (idempotent)
# - Lists items already in project
# - Lists all repo issues
# - Adds only missing ones
# - Robust to individual failures

param(
    [string]$Owner = "hjh890989-web",
    [string]$Repo = "hjh890989-web/Internal-Production-Scheduling-Project",
    [int]$ProjectNum = 4
)

$ErrorActionPreference = "Continue"

Write-Host "=== Resume project item add ===" -ForegroundColor Cyan
Write-Host "Project #$ProjectNum" -ForegroundColor Gray

# Step 1: Get items already in project (issue URLs)
Write-Host "Fetching project items..." -ForegroundColor Gray
$projectItems = gh project item-list $ProjectNum --owner $Owner --limit 2000 --format json 2>&1 | ConvertFrom-Json
$existingUrls = @{}
foreach ($item in $projectItems.items) {
    if ($item.content.url) {
        $existingUrls[$item.content.url] = $true
    }
}
Write-Host "Already in project: $($existingUrls.Count)" -ForegroundColor Green

# Step 2: Get all repo issues
Write-Host "Fetching repo issues..." -ForegroundColor Gray
$issues = gh issue list --repo $Repo --state all --limit 2000 --json number,url 2>&1 | ConvertFrom-Json
Write-Host "Total repo issues: $($issues.Count)" -ForegroundColor Green

# Step 3: Find missing
$missing = @()
foreach ($issue in $issues) {
    if (-not $existingUrls.ContainsKey($issue.url)) {
        $missing += $issue
    }
}
Write-Host "Missing from project: $($missing.Count)" -ForegroundColor Yellow

if ($missing.Count -eq 0) {
    Write-Host "Nothing to do." -ForegroundColor Green
    exit 0
}

# Step 4: Add missing - tolerant of failures
$added = 0
$failed = 0
$idx = 0
foreach ($issue in $missing) {
    $idx++

    # Suppress all output and check exit code
    $stdout = & gh project item-add $ProjectNum --owner $Owner --url $issue.url 2>&1
    $ok = $LASTEXITCODE -eq 0

    if ($ok) {
        $added++
        if ($added % 20 -eq 0) {
            Write-Host ("  [{0,3}/{1}] added (skipped {2})" -f $added, $missing.Count, $failed) -ForegroundColor Green
        }
    } else {
        $failed++
        if ($failed -le 5) {
            Write-Host ("  [{0,3}/{1}] FAIL #{2}: {3}" -f $idx, $missing.Count, $issue.number, $stdout) -ForegroundColor Yellow
        } elseif ($failed -eq 6) {
            Write-Host "  ... (suppressing further failure messages)" -ForegroundColor Gray
        }
    }

    Start-Sleep -Milliseconds 250
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Added: $added" -ForegroundColor Green
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
Write-Host "  Total in project now: $($existingUrls.Count + $added)" -ForegroundColor Green
Write-Host "  URL: https://github.com/users/$Owner/projects/$ProjectNum"

# Always exit 0 - we report internal failures via variables above
exit 0
