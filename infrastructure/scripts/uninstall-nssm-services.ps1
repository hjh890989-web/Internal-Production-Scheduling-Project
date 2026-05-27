# =============================================================================
# uninstall-nssm-services.ps1 — Sprint 19 EP-BETA-LAUNCH TK-BETA-5-2
# =============================================================================
# install-nssm-services.ps1 로 등록한 backend + frontend Windows 서비스 제거.
# 서비스 STOP → REMOVE — NSSM CLI 사용.
#
# 사용 (관리자 PowerShell):
#   .\uninstall-nssm-services.ps1
# =============================================================================

#Requires -RunAsAdministrator

[CmdletBinding()]
param(
    [string]$NssmPath = "C:\nssm-2.24\win64\nssm.exe",
    [string]$BackendServiceName = "Scheduling-Backend",
    [string]$FrontendServiceName = "Scheduling-Frontend"
)

$ErrorActionPreference = "Continue"

if (-not (Test-Path $NssmPath)) {
    Write-Error "NSSM not found at $NssmPath — -NssmPath 인자로 경로 지정"
    exit 1
}

Write-Host "=== Scheduling 베타 서비스 제거 ===" -ForegroundColor Cyan

foreach ($svc in @($BackendServiceName, $FrontendServiceName)) {
    Write-Host "[$svc] 중지 + 제거..." -ForegroundColor Yellow
    try {
        & $NssmPath stop $svc confirm 2>&1 | Out-Null
        Start-Sleep -Seconds 2
        & $NssmPath remove $svc confirm 2>&1 | Out-Null
        Write-Host "    OK — $svc 제거됨" -ForegroundColor Green
    } catch {
        Write-Warning "$svc 제거 실패 (이미 미존재 가능): $_"
    }
}

Write-Host ""
Write-Host "=== 제거 완료 ===" -ForegroundColor Cyan
Write-Host "재설치: .\install-nssm-services.ps1" -ForegroundColor DarkGray
