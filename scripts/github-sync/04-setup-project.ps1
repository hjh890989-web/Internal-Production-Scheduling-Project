# 04-setup-project.ps1
# GitHub Projects v2 setup - custom fields + add all repo issues
# Pre-req: gh auth (scopes: project, read:org) + 03 issues complete

param(
    [string]$Owner = "hjh890989-web",
    [string]$Repo = "hjh890989-web/Internal-Production-Scheduling-Project",
    [string]$ProjectTitle = "Internal Production Scheduling - Phase 3"
)

$ErrorActionPreference = "Continue"

Write-Host "=== GitHub Projects v2 Setup ===" -ForegroundColor Cyan

# Step 1: Find or create project
$existingProjects = gh project list --owner $Owner --format json 2>&1 | ConvertFrom-Json
$project = $existingProjects.projects | Where-Object { $_.title -eq $ProjectTitle } | Select-Object -First 1

if (-not $project) {
    Write-Host "Creating project: $ProjectTitle" -ForegroundColor Yellow
    $created = gh project create --owner $Owner --title $ProjectTitle --format json 2>&1 | ConvertFrom-Json
    $project = $created
}

$projectNum = $project.number
Write-Host "Project #$projectNum" -ForegroundColor Green

# Step 2: Add custom fields
Write-Host ""
Write-Host "--- Adding custom fields ---" -ForegroundColor Cyan

# Helper to add field (idempotent - skip if exists)
function Add-Field {
    param([string]$Name, [string]$Type, [string]$Options = "")

    # Check existing
    $fields = gh project field-list $projectNum --owner $Owner --format json 2>&1 | ConvertFrom-Json
    $exists = $fields.fields | Where-Object { $_.name -eq $Name }
    if ($exists) {
        Write-Host "  [=] $Name (already exists)" -ForegroundColor Gray
        return
    }

    $args = @('project','field-create',$projectNum,'--owner',$Owner,'--name',$Name,'--data-type',$Type)
    if ($Options) { $args += @('--single-select-options',$Options) }
    $result = & gh @args 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [+] $Name ($Type)" -ForegroundColor Green
    } else {
        Write-Host "  [!] $Name - $result" -ForegroundColor Red
    }
}

Add-Field -Name "Sprint" -Type "SINGLE_SELECT" -Options "S0,S1,S2,S3,S4,S5,Beta"
Add-Field -Name "Epic" -Type "TEXT"
Add-Field -Name "SP" -Type "NUMBER"
Add-Field -Name "PD" -Type "NUMBER"
Add-Field -Name "Start" -Type "DATE"
Add-Field -Name "Target" -Type "DATE"
Add-Field -Name "Priority" -Type "SINGLE_SELECT" -Options "Must,Should,Could"
Add-Field -Name "BR" -Type "TEXT"

# Step 3: Add all repo issues to project
Write-Host ""
Write-Host "--- Adding all repo issues to project ---" -ForegroundColor Cyan
$issues = gh issue list --repo $Repo --state all --limit 1000 --json number 2>&1 | ConvertFrom-Json
Write-Host "Found $($issues.Count) issues" -ForegroundColor Gray

$added = 0
foreach ($issue in $issues) {
    $url = "https://github.com/$Repo/issues/$($issue.number)"
    & gh project item-add $projectNum --owner $Owner --url $url 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $added++
        if ($added % 20 -eq 0) { Write-Host "  ... $added of $($issues.Count) added" }
    }
    Start-Sleep -Milliseconds 100
}
Write-Host "  Total $added issues added to project" -ForegroundColor Green

Write-Host ""
Write-Host "=== Project setup complete ===" -ForegroundColor Cyan
Write-Host "URL: https://github.com/users/$Owner/projects/$projectNum" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps (web UI):" -ForegroundColor Yellow
Write-Host "  1. Open project URL"
Write-Host "  2. View - New view - 'Roadmap by Sprint' (Board, group by Sprint field)"
Write-Host "  3. View - New view - 'Critical Path' (Table, filter: label:critical-path)"
Write-Host "  4. View - New view - 'NFR Epics' (Table, filter: label:type:nfr)"
Write-Host "  5. View - New view - 'Timeline' (Roadmap, x-axis: Start/Target)"
Write-Host "  6. View - New view - 'v1.4 New' (Table, filter: label:v1.4-new)"
