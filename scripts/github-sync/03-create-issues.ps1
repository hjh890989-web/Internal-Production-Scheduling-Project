# 03-create-issues.ps1
# Bulk-register Phase 2 Tasks/ files as GitHub Issues
# Pre-req: gh auth + labels (01) + milestones (02) complete
# Policy:
#   EP-XX/_Epic_Overview.md      -> 1 issue (type:epic)
#   EP-XX/ST-XX-X/_Story_Overview.md -> skip
#   EP-XX/ST-XX-X/TK-XX-X-X.md   -> 1 issue (type:task)
# Rate-limit: 200ms sleep per create

param(
    [switch]$DryRun = $false,
    [string]$Repo = "hjh890989-web/Internal-Production-Scheduling-Project",
    [string]$TasksRoot = "Phase 2\4.Tasks\Tasks",
    [int]$MaxIssues = 9999
)

$ErrorActionPreference = "Continue"
Write-Host "=== Repo: $Repo ===" -ForegroundColor Cyan
Write-Host "=== Tasks root: $TasksRoot ===" -ForegroundColor Cyan
$mode = if ($DryRun) { "DRY RUN" } else { "EXECUTE" }
Write-Host "=== Mode: $mode ===" -ForegroundColor Yellow

# Milestone map (title -> number)
$milestoneMap = @{}
$msResult = gh api "repos/$Repo/milestones?state=all" --paginate --jq '.[] | {number, title}' 2>&1 | ConvertFrom-Json
foreach ($m in $msResult) {
    $milestoneMap[$m.title] = $m.number
}
Write-Host "Found $($milestoneMap.Count) milestones" -ForegroundColor Gray

$sprintToMilestone = @{
    "S0" = "Sprint 0 - Foundation"
    "S1" = "Sprint 1 - Order Integration"
    "S2" = "Sprint 2 - VC Scheduling"
    "S3" = "Sprint 3 - EX Scheduling"
    "S4" = "Sprint 4 - Governance"
    "S5" = "Sprint 5 - UI plus E2E"
}

# Frontmatter parser
function Parse-Frontmatter {
    param([string]$Content)
    if ($Content -notmatch '(?s)^---\r?\n(.*?)\r?\n---') { return $null }
    $fm = $Matches[1]
    $result = @{}
    foreach ($line in $fm -split "`n") {
        if ($line -match '^([\w-]+):\s*(.*)$') {
            $val = $Matches[2].Trim()
            $val = $val -replace "^['""]", "" -replace "['""]$", ""
            $result[$Matches[1]] = $val
        }
    }
    return $result
}

# Parse labels from frontmatter labels string
function Parse-Labels {
    param([string]$LabelStr)
    $labels = @()
    foreach ($l in $LabelStr -split ',') {
        $l = $l.Trim() -replace "^['""]", "" -replace "['""]$", ""
        if ($l) { $labels += $l }
    }
    return $labels
}

# Write UTF-8 file without BOM
function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8)
}

# Known epic categories
$criticalEpics = @('EP-00','EP-99','EP-01','EP-02','EP-03','EP-04','EP-05','EP-21','EP-07','EP-08','EP-09','EP-12-INFRA','EP-EX13','EP-EX14','EP-10','EP-11','EP-13','EP-15','EP-E2E')
$v14NewEpics = @('EP-21','EP-13')
$crossEpics = @('EP-30','EP-31','EP-32','EP-33','EP-34')
$nfrEpics = @('EP-40','EP-41','EP-42','EP-43','EP-44','EP-45','EP-46','EP-47')

# Valid labels set (only allow labels that exist in repo)
$validLabels = @(
    'sprint:S0','sprint:S1','sprint:S2','sprint:S3','sprint:S4','sprint:S5',
    'type:epic','type:story','type:task','type:backend','type:frontend','type:infra','type:test','type:docs','type:nfr',
    'priority:must','priority:should','priority:could',
    'owner:solo','owner:devops','owner:qa',
    'cross-cutting',
    'br:V07','br:E05','br:X01','br:X02','br:X04','br:X05','br:X06','br:X07',
    'critical-path','v1.4-new'
)

$count = 0
$created = 0
$failed = 0

# Function: filter labels to only valid ones
function Filter-Labels {
    param([array]$Labels)
    $out = @()
    foreach ($l in $Labels) {
        if ($validLabels -contains $l) { $out += $l }
    }
    return ($out | Sort-Object -Unique)
}

# --- Step 1: Epic issues ---
Write-Host ""
Write-Host "--- Epic issues ---" -ForegroundColor Cyan
$epicFiles = Get-ChildItem -Path $TasksRoot -Recurse -Filter "_Epic_Overview.md"
foreach ($file in $epicFiles) {
    if ($count -ge $MaxIssues) { break }
    $count++

    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $fm = Parse-Frontmatter $content
    $epicId = $file.Directory.Name

    # Title extraction: try frontmatter, then first line with [EP-XX]
    $issueTitle = $null
    if ($fm -and $fm.title) {
        $title = $fm.title -replace "^\[$epicId\]\s*", ""
        $issueTitle = "[$epicId] $title"
    } else {
        $firstLines = ($content -split "`n" | Select-Object -First 5) -join "`n"
        $escEpic = [regex]::Escape($epicId)
        if ($firstLines -match "\[$escEpic\]\s*([^\r\n]+)") {
            $issueTitle = "[$epicId] $($Matches[1].Trim())"
        } else {
            $issueTitle = "[$epicId] Epic Overview"
        }
    }

    # Sprint from body **Sprint**: SX line
    $sprintFromBody = $null
    if ($content -match "\*\*Sprint\*\*\s*:\s*(S\d)") {
        $sprintFromBody = $Matches[1]
    }

    $labels = @("type:epic")
    if ($fm -and $fm.labels) {
        $labels += Parse-Labels $fm.labels
    }
    if ($sprintFromBody) { $labels += "sprint:$sprintFromBody" }
    if ($content -match "\*\*Priority\*\*\s*:\s*Must") { $labels += "priority:must" }
    elseif ($content -match "\*\*Priority\*\*\s*:\s*Should") { $labels += "priority:should" }
    elseif ($content -match "\*\*Priority\*\*\s*:\s*Could") { $labels += "priority:could" }

    if ($criticalEpics -contains $epicId) { $labels += "critical-path" }
    if ($v14NewEpics -contains $epicId) { $labels += "v1.4-new" }
    if ($crossEpics -contains $epicId) { $labels += "cross-cutting" }
    if ($nfrEpics -contains $epicId) { $labels += "type:nfr" }

    $finalLabels = Filter-Labels $labels

    # Milestone from frontmatter or body
    $msName = $null
    if ($fm -and $fm.labels -and $fm.labels -match "sprint:(S\d)") {
        $msName = $sprintToMilestone[$Matches[1]]
    } elseif ($sprintFromBody) {
        $msName = $sprintToMilestone[$sprintFromBody]
    }

    Write-Host ("  [{0,3}] {1}" -f $count, $issueTitle) -NoNewline

    if ($DryRun) {
        Write-Host " (dry, $($finalLabels.Count) lbls)" -ForegroundColor Gray
    } else {
        $tempBody = [System.IO.Path]::GetTempFileName()
        Write-Utf8NoBom -Path $tempBody -Content $content

        $args = @('issue','create','--repo',$Repo,'--title',$issueTitle,'--body-file',$tempBody)
        if ($finalLabels.Count -gt 0) { $args += @('--label', ($finalLabels -join ',')) }
        if ($msName) { $args += @('--milestone',$msName) }

        $result = & gh @args 2>&1
        Remove-Item $tempBody -Force -ErrorAction SilentlyContinue

        if ($LASTEXITCODE -eq 0) {
            Write-Host " -> $result" -ForegroundColor Green
            $created++
        } else {
            Write-Host " [FAIL]" -ForegroundColor Red
            Write-Host "    $result" -ForegroundColor Red
            $failed++
        }
        Start-Sleep -Milliseconds 200
    }
}

# --- Step 2: Task issues (TK-*.md) ---
Write-Host ""
Write-Host "--- Task issues ---" -ForegroundColor Cyan
$taskFiles = Get-ChildItem -Path $TasksRoot -Recurse -Filter "TK-*.md"
foreach ($file in $taskFiles) {
    if ($count -ge $MaxIssues) { break }
    $count++

    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $fm = Parse-Frontmatter $content

    $tkId = $file.BaseName
    if ($fm -and $fm.title) {
        $title = $fm.title -replace "^\[$tkId\]\s*", ""
        $issueTitle = "[$tkId] $title"
    } else {
        $issueTitle = "[$tkId] Task"
    }

    $storyDir = $file.Directory.Name
    $epicDir = $file.Directory.Parent.Name

    $labels = @("type:task")
    if ($fm -and $fm.labels) {
        $labels += Parse-Labels $fm.labels
    }
    if ($criticalEpics -contains $epicDir) { $labels += "critical-path" }
    if ($v14NewEpics -contains $epicDir) { $labels += "v1.4-new" }
    if ($crossEpics -contains $epicDir) { $labels += "cross-cutting" }
    if ($nfrEpics -contains $epicDir) { $labels += "type:nfr" }

    $finalLabels = Filter-Labels $labels

    $msName = $null
    if ($fm -and $fm.labels -and $fm.labels -match "sprint:(S\d)") {
        $sprintCode = $Matches[1]
        $msName = $sprintToMilestone[$sprintCode]
    }

    Write-Host ("  [{0,3}] {1}" -f $count, $issueTitle) -NoNewline

    if ($DryRun) {
        Write-Host " (dry, $($finalLabels.Count) lbls)" -ForegroundColor Gray
    } else {
        $tempBody = [System.IO.Path]::GetTempFileName()
        Write-Utf8NoBom -Path $tempBody -Content $content

        $args = @('issue','create','--repo',$Repo,'--title',$issueTitle,'--body-file',$tempBody)
        if ($finalLabels.Count -gt 0) { $args += @('--label', ($finalLabels -join ',')) }
        if ($msName) { $args += @('--milestone',$msName) }

        $result = & gh @args 2>&1
        Remove-Item $tempBody -Force -ErrorAction SilentlyContinue

        if ($LASTEXITCODE -eq 0) {
            Write-Host " -> $result" -ForegroundColor Green
            $created++
        } else {
            Write-Host " [FAIL]" -ForegroundColor Red
            Write-Host "    $result" -ForegroundColor Red
            $failed++
        }
        Start-Sleep -Milliseconds 200
    }
}

Write-Host ""
Write-Host "=== Total: $count files, $created created, $failed failed ===" -ForegroundColor Cyan
