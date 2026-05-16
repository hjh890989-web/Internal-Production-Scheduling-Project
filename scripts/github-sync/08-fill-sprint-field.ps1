# 08-fill-sprint-field.ps1
# Auto-fill Project's "Sprint" custom field by reading sprint label on each issue
# Maps issue label sprint:SX -> project Sprint field option SX

$ErrorActionPreference = "Continue"
$ProjectNum = 4
$Owner = "@me"
$Repo = "hjh890989-web/Internal-Production-Scheduling-Project"

# Step 1: Project metadata - get Sprint field ID + option IDs
Write-Host "Fetching project fields..." -ForegroundColor Cyan
$fields = gh project field-list $ProjectNum --owner $Owner --format json 2>&1 | ConvertFrom-Json
$sprintField = $fields.fields | Where-Object { $_.name -eq "Sprint" }
if (-not $sprintField) { Write-Host "Sprint field not found!" -ForegroundColor Red; exit 1 }
$sprintFieldId = $sprintField.id
Write-Host "Sprint field ID: $sprintFieldId" -ForegroundColor Gray

# Build option map: name -> id
$sprintOptionMap = @{}
foreach ($opt in $sprintField.options) {
    $sprintOptionMap[$opt.name] = $opt.id
}
Write-Host "Sprint options: $($sprintOptionMap.Keys -join ', ')" -ForegroundColor Gray

# Step 2: Project ID (needed for graphql)
$projectMeta = gh project view $ProjectNum --owner $Owner --format json 2>&1 | ConvertFrom-Json
$projectId = $projectMeta.id
Write-Host "Project ID: $projectId" -ForegroundColor Gray

# Step 3: Fetch all project items (with content URL + item ID)
Write-Host "Fetching project items..." -ForegroundColor Cyan
$items = gh project item-list $ProjectNum --owner $Owner --limit 2000 --format json 2>&1 | ConvertFrom-Json
Write-Host "Project items: $($items.items.Count)" -ForegroundColor Gray

# Step 4: For each item, find sprint label and set field
Write-Host "Setting Sprint field on each item..." -ForegroundColor Cyan
$updated = 0
$skipped = 0
$failed = 0

foreach ($item in $items.items) {
    if (-not $item.content.url) { $skipped++; continue }
    if ($item.content.type -ne "Issue") { $skipped++; continue }

    # Get issue labels (already in item.labels typically)
    $sprintLabel = $item.labels | Where-Object { $_ -match "^sprint:(S\d)$" }
    if (-not $sprintLabel) { $skipped++; continue }

    $sprintLabel -match "sprint:(S\d)" | Out-Null
    $sprintName = $Matches[1]
    $optionId = $sprintOptionMap[$sprintName]
    if (-not $optionId) { $skipped++; continue }

    # Set field value
    $result = & gh project item-edit `
        --id $item.id `
        --field-id $sprintFieldId `
        --single-select-option-id $optionId `
        --project-id $projectId 2>&1

    if ($LASTEXITCODE -eq 0) {
        $updated++
        if ($updated % 25 -eq 0) {
            Write-Host "  ... $updated / $($items.items.Count) updated" -ForegroundColor Green
        }
    } else {
        $failed++
        if ($failed -le 3) { Write-Host "  FAIL: $result" -ForegroundColor Yellow }
    }
    Start-Sleep -Milliseconds 200
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Updated: $updated" -ForegroundColor Green
Write-Host "  Skipped (no sprint label): $skipped" -ForegroundColor Gray
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
exit 0
