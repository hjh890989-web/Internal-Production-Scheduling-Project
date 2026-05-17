# 09-fill-date-fields.ps1
# Auto-fill Start and Target date fields based on Sprint label
# Maps each Sprint label to v2.0 AI-accelerated date ranges

$ErrorActionPreference = "Continue"
$ProjectNum = 4
$Owner = "@me"

# v2.0 Sprint date ranges
$sprintDates = @{
    "S0" = @{ Start = "2026-05-18"; Target = "2026-05-19" }
    "S1" = @{ Start = "2026-05-20"; Target = "2026-05-22" }
    "S2" = @{ Start = "2026-05-25"; Target = "2026-05-29" }
    "S3" = @{ Start = "2026-06-01"; Target = "2026-06-05" }
    "S4" = @{ Start = "2026-06-08"; Target = "2026-06-12" }
    "S5" = @{ Start = "2026-06-15"; Target = "2026-06-19" }
}
# Fallback (NFR/cross-cutting epics distributed across all phases)
$fallback = @{ Start = "2026-05-18"; Target = "2026-06-19" }

# Step 1: Project metadata
Write-Host "Fetching project fields..." -ForegroundColor Cyan
$fields = gh project field-list $ProjectNum --owner $Owner --format json --limit 50 2>&1 | ConvertFrom-Json
$startField = $fields.fields | Where-Object { $_.name -eq "Start" }
$targetField = $fields.fields | Where-Object { $_.name -eq "Target" }
if (-not $startField -or -not $targetField) {
    Write-Host "Start/Target field not found" -ForegroundColor Red
    exit 1
}
$startFieldId = $startField.id
$targetFieldId = $targetField.id
Write-Host "Start field: $startFieldId" -ForegroundColor Gray
Write-Host "Target field: $targetFieldId" -ForegroundColor Gray

$projectMeta = gh project view $ProjectNum --owner $Owner --format json 2>&1 | ConvertFrom-Json
$projectId = $projectMeta.id

# Step 2: Fetch items
Write-Host "Fetching project items..." -ForegroundColor Cyan
$items = gh project item-list $ProjectNum --owner $Owner --limit 2000 --format json 2>&1 | ConvertFrom-Json
Write-Host "Project items: $($items.items.Count)" -ForegroundColor Gray

# Step 3: For each, set date fields
Write-Host "Filling date fields..." -ForegroundColor Cyan
$updated = 0
$fallbacked = 0
$failed = 0

foreach ($item in $items.items) {
    if ($item.content.type -ne "Issue") { continue }

    # Find sprint from labels
    $sprintLabel = $item.labels | Where-Object { $_ -match "^sprint:(S\d)$" }
    $dates = $null

    if ($sprintLabel) {
        $sprintLabel -match "sprint:(S\d)" | Out-Null
        $sprintName = $Matches[1]
        $dates = $sprintDates[$sprintName]
    } else {
        $dates = $fallback
        $fallbacked++
    }

    # Set Start
    $r1 = & gh project item-edit `
        --id $item.id `
        --field-id $startFieldId `
        --date $dates.Start `
        --project-id $projectId 2>&1
    $ok1 = $LASTEXITCODE -eq 0

    Start-Sleep -Milliseconds 100

    # Set Target
    $r2 = & gh project item-edit `
        --id $item.id `
        --field-id $targetFieldId `
        --date $dates.Target `
        --project-id $projectId 2>&1
    $ok2 = $LASTEXITCODE -eq 0

    if ($ok1 -and $ok2) {
        $updated++
        if ($updated % 25 -eq 0) {
            Write-Host "  ... $updated / $($items.items.Count) updated" -ForegroundColor Green
        }
    } else {
        $failed++
        if ($failed -le 3) {
            Write-Host "  FAIL: start=$r1 / target=$r2" -ForegroundColor Yellow
        }
    }

    Start-Sleep -Milliseconds 150
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Updated: $updated" -ForegroundColor Green
Write-Host "  Used fallback (NFR/cross): $fallbacked" -ForegroundColor Gray
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
exit 0
