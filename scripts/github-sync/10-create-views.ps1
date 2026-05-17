# 10-create-views.ps1
# Create 5 empty views with proper names + layouts via GraphQL.
# Note: GitHub API does NOT support setting filter/group/sort via GraphQL.
# Users must still configure filter/group manually for each view.

$ErrorActionPreference = "Continue"
$projectId = "PVT_kwHOEFoy7s4BX29s"

$views = @(
    @{ name = "Roadmap by Sprint"; layout = "BOARD_LAYOUT" }
    @{ name = "Critical Path"; layout = "TABLE_LAYOUT" }
    @{ name = "NFR Epics"; layout = "TABLE_LAYOUT" }
    @{ name = "Timeline (Phase 3)"; layout = "ROADMAP_LAYOUT" }
    @{ name = "v1.4 New Epics"; layout = "TABLE_LAYOUT" }
)

Write-Host "Creating 5 project views..." -ForegroundColor Cyan

$mutation = 'mutation($projectId: ID!, $name: String!, $layout: ProjectV2ViewLayout!) { createProjectV2View(input: { projectId: $projectId, name: $name, layout: $layout }) { projectV2View { id name layout } } }'

$created = 0
$failed = 0

foreach ($v in $views) {
    $result = gh api graphql `
        -f query="$mutation" `
        -F projectId="$projectId" `
        -F name="$($v.name)" `
        -F layout="$($v.layout)" 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [+] $($v.name) ($($v.layout))" -ForegroundColor Green
        $created++
    } else {
        Write-Host "  [!] $($v.name) - $result" -ForegroundColor Red
        $failed++
    }
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Created: $created / $($views.Count)" -ForegroundColor Green
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
Write-Host ""
Write-Host "NOTE: Filters and group-by settings must be configured manually in the web UI" -ForegroundColor Yellow
Write-Host "  - Roadmap by Sprint -> Slice by: Sprint" -ForegroundColor Gray
Write-Host "  - Critical Path -> Filter: label:critical-path" -ForegroundColor Gray
Write-Host "  - NFR Epics -> Filter: label:type:nfr" -ForegroundColor Gray
Write-Host "  - Timeline -> Date fields: Start, Target" -ForegroundColor Gray
Write-Host "  - v1.4 New Epics -> Filter: label:v1.4-new" -ForegroundColor Gray
