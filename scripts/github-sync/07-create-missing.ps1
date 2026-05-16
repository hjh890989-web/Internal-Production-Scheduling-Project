# 07-create-missing.ps1
# Create only the 18 missing TK issues (EX11/12/13/14 + VC15/16) with slow pace
# to avoid secondary rate limit.

$ErrorActionPreference = "Continue"
$Repo = "hjh890989-web/Internal-Production-Scheduling-Project"
$ProjectNum = 4
$Owner = "@me"

# Missing TK file paths (EP-EX11~14 + EP-VC15·16)
$missingDirs = @("EP-EX11","EP-EX12","EP-EX13","EP-EX14","EP-VC15","EP-VC16")

# Collect TK file paths
$tkFiles = @()
foreach ($epDir in $missingDirs) {
    $files = Get-ChildItem -Path "Phase 2\4.Tasks\Tasks\$epDir" -Recurse -Filter "TK-*.md" -File
    $tkFiles += $files
}
Write-Host "Found $($tkFiles.Count) missing TK files" -ForegroundColor Cyan

# Milestone map
$milestoneMap = @{}
gh api "repos/$Repo/milestones?state=all" --jq '.[] | {number, title}' 2>&1 | ConvertFrom-Json | ForEach-Object { $milestoneMap[$_.title] = $_.number }
$sprintToMs = @{
    "S0" = "Sprint 0 - Foundation"
    "S1" = "Sprint 1 - Order Integration"
    "S2" = "Sprint 2 - VC Scheduling"
    "S3" = "Sprint 3 - EX Scheduling"
    "S4" = "Sprint 4 - Governance"
    "S5" = "Sprint 5 - UI plus E2E"
}

# Valid labels
$validLabels = @(
    'sprint:S0','sprint:S1','sprint:S2','sprint:S3','sprint:S4','sprint:S5',
    'type:epic','type:story','type:task','type:backend','type:frontend','type:infra','type:test','type:docs','type:nfr',
    'priority:must','priority:should','priority:could',
    'cross-cutting',
    'br:V07','br:E05','br:X01','br:X02','br:X04','br:X05','br:X06','br:X07',
    'critical-path','v1.4-new'
)

function Parse-FM {
    param([string]$Content)
    if ($Content -notmatch '(?s)^---\r?\n(.*?)\r?\n---') { return $null }
    $result = @{}
    foreach ($line in $Matches[1] -split "`n") {
        if ($line -match '^([\w-]+):\s*(.*)$') {
            $result[$Matches[1]] = ($Matches[2].Trim() -replace "^['""]", "" -replace "['""]$", "")
        }
    }
    return $result
}

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

$created = 0
$failed = 0
$projectAdded = 0

foreach ($file in $tkFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $fm = Parse-FM $content
    $tkId = $file.BaseName
    $epicDir = $file.Directory.Parent.Name
    $storyDir = $file.Directory.Name

    # Title
    if ($fm -and $fm.title) {
        $titleClean = $fm.title -replace "^\[$tkId\]\s*", ""
        $issueTitle = "[$tkId] $titleClean"
    } else {
        $issueTitle = "[$tkId] Task"
    }

    # Labels
    $labels = @("type:task")
    if ($fm -and $fm.labels) {
        foreach ($l in $fm.labels -split ',') {
            $labels += ($l.Trim() -replace "^['""]", "" -replace "['""]$", "")
        }
    }
    # All these 6 epics are critical-path
    $labels += "critical-path"
    $finalLabels = ($labels | Where-Object { $validLabels -contains $_ } | Sort-Object -Unique)

    # Milestone
    $msName = $null
    if ($fm -and $fm.labels -and $fm.labels -match "sprint:(S\d)") {
        $msName = $sprintToMs[$Matches[1]]
    }

    Write-Host ("[{0,2}/{1}] {2}" -f ($created + $failed + 1), $tkFiles.Count, $issueTitle) -NoNewline

    $tempBody = [System.IO.Path]::GetTempFileName()
    Write-Utf8NoBom -Path $tempBody -Content $content

    $args = @('issue','create','--repo',$Repo,'--title',$issueTitle,'--body-file',$tempBody)
    if ($finalLabels.Count -gt 0) { $args += @('--label', ($finalLabels -join ',')) }
    if ($msName) { $args += @('--milestone',$msName) }

    $issueUrl = & gh @args 2>&1
    Remove-Item $tempBody -Force -ErrorAction SilentlyContinue

    if ($LASTEXITCODE -eq 0 -and $issueUrl -match "github.com/.*/issues/\d+") {
        Write-Host " OK $issueUrl" -ForegroundColor Green
        $created++

        # Add to project (slow pace)
        Start-Sleep -Milliseconds 1500
        $addResult = & gh project item-add $ProjectNum --owner $Owner --url "$issueUrl" 2>&1
        if ($LASTEXITCODE -eq 0) {
            $projectAdded++
        } else {
            Write-Host "    project add failed: $addResult" -ForegroundColor Yellow
        }
    } else {
        Write-Host " FAIL: $issueUrl" -ForegroundColor Red
        $failed++
        if ($issueUrl -match "secondary rate limit") {
            Write-Host "  ABORT - secondary rate limit" -ForegroundColor Red
            break
        }
    }

    # Slow pace: 2 sec between issue creates (to avoid secondary rate limit)
    Start-Sleep -Milliseconds 2000
}

Write-Host ""
Write-Host "=== Result ===" -ForegroundColor Cyan
Write-Host "  Created: $created / $($tkFiles.Count)" -ForegroundColor Green
Write-Host "  Added to project: $projectAdded" -ForegroundColor Green
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Yellow" } else { "Green" })
exit 0
