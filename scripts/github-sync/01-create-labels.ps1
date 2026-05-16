# 01-create-labels.ps1
# Create standard labels in the repo for Phase 3 issue tracking
# Pre-req: gh auth + repo permission

$ErrorActionPreference = "Stop"
$repo = "hjh890989-web/Internal-Production-Scheduling-Project"

Write-Host "=== Creating labels in $repo ===" -ForegroundColor Cyan

$labels = @(
    # Sprint
    @{ name = "sprint:S0"; color = "e0e7ff"; desc = "Sprint 0 - Foundation (W1 D1-D2)" }
    @{ name = "sprint:S1"; color = "dbeafe"; desc = "Sprint 1 - Order Integration (W1 D3-D5)" }
    @{ name = "sprint:S2"; color = "bfdbfe"; desc = "Sprint 2 - VC Scheduling (W2)" }
    @{ name = "sprint:S3"; color = "a5f3fc"; desc = "Sprint 3 - EX Scheduling (W3)" }
    @{ name = "sprint:S4"; color = "bbf7d0"; desc = "Sprint 4 - Governance (W4)" }
    @{ name = "sprint:S5"; color = "fbcfe8"; desc = "Sprint 5 - UI plus E2E (W5)" }

    # Type
    @{ name = "type:epic"; color = "fef3c7"; desc = "Epic level - umbrella issue" }
    @{ name = "type:story"; color = "fde68a"; desc = "Story level - feature unit" }
    @{ name = "type:task"; color = "f9fafb"; desc = "Task level - actionable work" }
    @{ name = "type:backend"; color = "a7f3d0"; desc = "Backend implementation" }
    @{ name = "type:frontend"; color = "fed7aa"; desc = "Frontend implementation" }
    @{ name = "type:infra"; color = "ddd6fe"; desc = "Infrastructure" }
    @{ name = "type:test"; color = "fce7f3"; desc = "Test (unit, integration, load)" }
    @{ name = "type:docs"; color = "f3f4f6"; desc = "Documentation" }
    @{ name = "type:nfr"; color = "fee2e2"; desc = "Non-Functional Requirement" }

    # Priority
    @{ name = "priority:must"; color = "dc2626"; desc = "Must-have (Phase 1.0 blocker)" }
    @{ name = "priority:should"; color = "ea580c"; desc = "Should-have" }
    @{ name = "priority:could"; color = "facc15"; desc = "Could-have (Phase 1.x)" }

    # Owner
    @{ name = "owner:solo"; color = "8b5cf6"; desc = "Solo developer plus Claude pair" }
    @{ name = "owner:devops"; color = "06b6d4"; desc = "Infra DevOps focus" }
    @{ name = "owner:qa"; color = "ec4899"; desc = "QA focus" }

    # Cross-cutting
    @{ name = "cross-cutting"; color = "525252"; desc = "Cross-cutting (EP-30 to 34)" }

    # BR (Business Rule)
    @{ name = "br:V07"; color = "fde68a"; desc = "BR-V07 same-day lock" }
    @{ name = "br:E05"; color = "fde68a"; desc = "BR-E05 yield 2531 reference (29673-2R060)" }
    @{ name = "br:X01"; color = "fde68a"; desc = "BR-X01 confirm gate" }
    @{ name = "br:X02"; color = "fde68a"; desc = "BR-X02 audit mandatory" }
    @{ name = "br:X04"; color = "fde68a"; desc = "BR-X04 KST timezone unification" }
    @{ name = "br:X05"; color = "fde68a"; desc = "BR-X05 dual-review" }
    @{ name = "br:X06"; color = "fde68a"; desc = "BR-X06 MES fallback to Excel" }
    @{ name = "br:X07"; color = "fde68a"; desc = "BR-X07 D-2 hard constraint" }

    # Status flags
    @{ name = "critical-path"; color = "b91c1c"; desc = "Critical Path - cascade delay risk" }
    @{ name = "v1.4-new"; color = "a16207"; desc = "v1.4 new epics (EP-21, EP-13)" }
)

$created = 0
$skipped = 0

foreach ($lbl in $labels) {
    $result = gh label create $lbl.name --color $lbl.color --description $lbl.desc --repo $repo --force 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [+] $($lbl.name)" -ForegroundColor Green
        $created++
    } else {
        Write-Host "  [!] $($lbl.name) - $result" -ForegroundColor Yellow
        $skipped++
    }
}

Write-Host ""
Write-Host "=== Labels: $created created/updated, $skipped errors ===" -ForegroundColor Cyan
