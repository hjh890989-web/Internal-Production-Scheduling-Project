# 06-add-remaining-v2.ps1
# Resume: add remaining repo issues to project (fixed: owner @me)
# - Uses @me for owner argument (gh CLI requires this for user-owned projects)
# - Robust to GraphQL/REST API errors
# - Idempotent

param(
    [string]$Repo = "hjh890989-web/Internal-Production-Scheduling-Project",
    [int]$ProjectNum = 4
)

$ErrorActionPreference = "Continue"

Write-Host "=== Resume project item add (v2) ===" -ForegroundColor Cyan
Write-Host "Project #$ProjectNum" -ForegroundColor Gray

# Check GraphQL rate limit
$rl = gh api rate_limit --jq '.resources.graphql.remaining' 2>&1
if ($rl -match "^\d+$" -and [int]$rl -lt 500) {
    Write-Host "WARN: GraphQL remaining = $rl, low. Continuing anyway..." -ForegroundColor Yellow
} else {
    Write-Host "GraphQL remaining: $rl" -ForegroundColor Gray
}

# Step 1: Fetch project items (using @me)
Write-Host "Fetching project items..." -ForegroundColor Gray
$projectItemsRaw = gh project item-list $ProjectNum --owner "@me" --limit 2000 --format json 2>&1
$existingUrls = @{}
try {
    $parsed = $projectItemsRaw | ConvertFrom-Json
    foreach ($item in $parsed.items) {
        if ($item.content.url) {
            $existingUrls[$item.content.url] = $true
        }
    }
    Write-Host "Already in project: $($existingUrls.Count)" -ForegroundColor Green
} catch {
    Write-Host "Failed to parse project items: $projectItemsRaw" -ForegroundColor Red
    exit 1
}

# Step 2: Fetch repo issues
Write-Host "Fetching repo issues..." -ForegroundColor Gray
$issuesRaw = gh issue list --repo $Repo --state all --limit 2000 --json number,url 2>&1
$issues = @()
try {
    $issues = $issuesRaw | ConvertFrom-Json
    Write-Host "Total repo issues: $($issues.Count)" -ForegroundColor Green
} catch {
    Write-Host "Failed to parse repo issues: $issuesRaw" -ForegroundColor Red
    exit 1
}

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

# Step 4: Add missing
$added = 0
$failed = 0
$idx = 0
foreach ($issue in $missing) {
    $idx++

    $stdout = & gh project item-add $ProjectNum --owner "@me" --url $issue.url 2>&1
    $ok = $LASTEXITCODE -eq 0

    if ($ok) {
        $added++
        if ($added % 25 -eq 0) {
            Write-Host ("  [{0,3}/{1}] added (failed {2})" -f $added, $missing.Count, $failed) -ForegroundColor Green
        }
    } else {
        $failed++
        if ($failed -le 3) {
            Write-Host ("  FAIL #$($issue.number): $stdout") -ForegroundColor Yellow
        }
        # If rate limit hit, abort to avoid wasting attempts
        if ($stdout -match "rate limit") {
            Write-Host "  RATE LIMIT - aborting at $added added" -ForegroundColor Red
            break
        }
    }

    Start-Sleep -Milliseconds 300
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Added this run: $added" -ForegroundColor Green
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
Write-Host "  Total in project now: $($existingUrls.Count + $added)" -ForegroundColor Green
Write-Host "  Remaining to add: $($missing.Count - $added)" -ForegroundColor $(if ($missing.Count - $added -gt 0) { "Yellow" } else { "Green" })
Write-Host "  URL: https://github.com/users/hjh890989-web/projects/$ProjectNum"

exit 0
