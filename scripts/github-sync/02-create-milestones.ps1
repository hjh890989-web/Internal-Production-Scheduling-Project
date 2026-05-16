# 02-create-milestones.ps1
# Create Sprint milestones in the repo (v2.0 AI-accelerated, 5 weeks)
# Pre-req: gh auth + repo permission

$ErrorActionPreference = "Stop"
$repo = "hjh890989-web/Internal-Production-Scheduling-Project"

Write-Host "=== Creating milestones in $repo ===" -ForegroundColor Cyan

# v2.0 AI-accelerated (5 weeks, 25 business days)
$milestones = @(
    @{ title = "Sprint 0 - Foundation"; due = "2026-05-19T17:00:00Z"; desc = "W1 D1-D2 - Infra plus cross-cutting baseline" }
    @{ title = "Sprint 1 - Order Integration"; due = "2026-05-22T17:00:00Z"; desc = "W1 D3-D5 - EP-01 02 03 Order integration" }
    @{ title = "Sprint 2 - VC Scheduling"; due = "2026-05-29T17:00:00Z"; desc = "W2 - EP-04 05 06 21 VC scheduling (v1.4 new)" }
    @{ title = "Sprint 3 - EX Scheduling"; due = "2026-06-05T17:00:00Z"; desc = "W3 - EP-07 08 09 12I EX13 EX14 EX scheduling" }
    @{ title = "Sprint 4 - Governance"; due = "2026-06-12T17:00:00Z"; desc = "W4 - EP-10 11 12 13 14 Governance plus same-day lock (v1.4)" }
    @{ title = "Sprint 5 - UI plus E2E"; due = "2026-06-19T17:00:00Z"; desc = "W5 - EP-15 to 20 plus E2E plus NFR plus Beta GO" }
    @{ title = "Beta GO"; due = "2026-06-19T17:00:00Z"; desc = "Phase 1.0 Beta release gate" }
)

foreach ($m in $milestones) {
    $result = gh api "repos/$repo/milestones" `
        --method POST `
        -f "title=$($m.title)" `
        -f "state=open" `
        -f "description=$($m.desc)" `
        -f "due_on=$($m.due)" 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [+] $($m.title)" -ForegroundColor Green
    } else {
        # Try update if exists
        $titleEsc = $m.title
        $existing = gh api "repos/$repo/milestones?state=all" --jq ".[] | select(.title==\`"$titleEsc\`") | .number" 2>&1
        if ($existing -match "^\d+$") {
            gh api "repos/$repo/milestones/$existing" `
                --method PATCH `
                -f "description=$($m.desc)" `
                -f "due_on=$($m.due)" 2>&1 | Out-Null
            Write-Host "  [~] $($m.title) (updated #$existing)" -ForegroundColor Yellow
        } else {
            Write-Host "  [!] $($m.title) - $result" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== Milestones ===" -ForegroundColor Cyan
gh api "repos/$repo/milestones?state=all" --jq '.[] | "  #\(.number) \(.title) (due \(.due_on))"' 2>&1
