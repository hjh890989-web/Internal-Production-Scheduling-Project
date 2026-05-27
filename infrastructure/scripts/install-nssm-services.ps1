# =============================================================================
# install-nssm-services.ps1 — Sprint 19 EP-BETA-LAUNCH TK-BETA-5-1
# =============================================================================
# NSSM (Non-Sucking Service Manager) 를 이용해 backend (Spring Boot bootRun) +
# frontend (Vite dev) 를 Windows 서비스로 등록 — PC 재부팅 시 자동 기동.
#
# 사용 (관리자 PowerShell):
#   .\install-nssm-services.ps1
#
# 의존:
#   - NSSM 설치: https://nssm.cc/download (기본 경로 C:\nssm-2.24)
#   - Java 21 JDK + Node.js 20+ + Gradle wrapper (프로젝트 동봉)
#   - Docker Desktop (Postgres/Redis 컨테이너) — 별도 자동시작 설정
#
# 베타 운영 정합:
#   PROJECT_ROOT = E:\Antigavity Workspace\Internal Production Scheduling Project (default)
#   환경변수: SPRING_PROFILES_ACTIVE=with-infra
# =============================================================================

#Requires -RunAsAdministrator

[CmdletBinding()]
param(
    [string]$NssmPath = "C:\nssm-2.24\win64\nssm.exe",
    [string]$ProjectRoot = "E:\Antigavity Workspace\Internal Production Scheduling Project",
    [string]$BackendServiceName = "Scheduling-Backend",
    [string]$FrontendServiceName = "Scheduling-Frontend"
)

$ErrorActionPreference = "Stop"

# 1) NSSM 존재 확인
if (-not (Test-Path $NssmPath)) {
    Write-Error "NSSM not found at $NssmPath — https://nssm.cc/download 에서 설치 후 -NssmPath 인자로 경로 지정"
    exit 1
}

# 2) ProjectRoot 검증
if (-not (Test-Path "$ProjectRoot\backend\gradlew.bat")) {
    Write-Error "ProjectRoot 가 올바르지 않음: $ProjectRoot (backend\gradlew.bat 미존재)"
    exit 1
}

Write-Host "=== Scheduling 베타 서비스 등록 시작 ===" -ForegroundColor Cyan
Write-Host "NSSM: $NssmPath"
Write-Host "Project Root: $ProjectRoot"
Write-Host ""

# 3) Backend 서비스 등록
Write-Host "[1/2] Backend ($BackendServiceName) 등록 중..." -ForegroundColor Yellow
& $NssmPath install $BackendServiceName "$ProjectRoot\backend\gradlew.bat"
& $NssmPath set $BackendServiceName AppParameters ":app:bootRun --no-daemon --console=plain"
& $NssmPath set $BackendServiceName AppDirectory "$ProjectRoot\backend"
& $NssmPath set $BackendServiceName AppEnvironmentExtra "SPRING_PROFILES_ACTIVE=with-infra"
& $NssmPath set $BackendServiceName DisplayName "사내 공정 스케줄링 Backend (Sprint 19 베타)"
& $NssmPath set $BackendServiceName Description "Spring Boot bootRun :app — port 8080, profile=with-infra (Postgres + Redis 컨테이너 선행 필요)"
& $NssmPath set $BackendServiceName Start SERVICE_AUTO_START
& $NssmPath set $BackendServiceName AppStdout "$ProjectRoot\backend\logs\nssm-backend-stdout.log"
& $NssmPath set $BackendServiceName AppStderr "$ProjectRoot\backend\logs\nssm-backend-stderr.log"
& $NssmPath set $BackendServiceName AppStopMethodConsole 15000
& $NssmPath set $BackendServiceName AppStopMethodWindow 5000
& $NssmPath set $BackendServiceName AppRotateFiles 1
& $NssmPath set $BackendServiceName AppRotateOnline 1
& $NssmPath set $BackendServiceName AppRotateBytes 10485760
Write-Host "    OK — $BackendServiceName 등록 (AUTO_START, stdout: backend\logs\nssm-backend-*.log)" -ForegroundColor Green

# 4) Frontend 서비스 등록
Write-Host "[2/2] Frontend ($FrontendServiceName) 등록 중..." -ForegroundColor Yellow
$npmCmd = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
if (-not $npmCmd) {
    Write-Error "npm.cmd not found in PATH — Node.js 20+ 설치 + PATH 등록 필요"
    exit 1
}
& $NssmPath install $FrontendServiceName $npmCmd
& $NssmPath set $FrontendServiceName AppParameters "run dev"
& $NssmPath set $FrontendServiceName AppDirectory "$ProjectRoot\frontend"
& $NssmPath set $FrontendServiceName DisplayName "사내 공정 스케줄링 Frontend (Sprint 19 베타)"
& $NssmPath set $FrontendServiceName Description "Vite dev server — port 5173 (Backend 8080 reverse proxy)"
& $NssmPath set $FrontendServiceName Start SERVICE_AUTO_START
& $NssmPath set $FrontendServiceName AppStdout "$ProjectRoot\frontend\logs\nssm-frontend-stdout.log"
& $NssmPath set $FrontendServiceName AppStderr "$ProjectRoot\frontend\logs\nssm-frontend-stderr.log"
& $NssmPath set $FrontendServiceName AppStopMethodConsole 10000
& $NssmPath set $FrontendServiceName AppRotateFiles 1
& $NssmPath set $FrontendServiceName AppRotateOnline 1
& $NssmPath set $FrontendServiceName AppRotateBytes 10485760
Write-Host "    OK — $FrontendServiceName 등록 (AUTO_START, stdout: frontend\logs\nssm-frontend-*.log)" -ForegroundColor Green

# 5) 로그 디렉토리 생성
$null = New-Item -ItemType Directory -Force -Path "$ProjectRoot\backend\logs"
$null = New-Item -ItemType Directory -Force -Path "$ProjectRoot\frontend\logs"

Write-Host ""
Write-Host "=== 등록 완료 ===" -ForegroundColor Cyan
Write-Host "다음 단계:"
Write-Host "  1) Docker Desktop 자동시작 활성 확인 (Settings → General → Start Docker Desktop when you sign in)"
Write-Host "  2) Postgres/Redis 컨테이너 자동시작 (docker update --restart=unless-stopped scheduling-postgres scheduling-redis)"
Write-Host "  3) 본 스크립트 직후 한 번 수동 기동: Start-Service $BackendServiceName, $FrontendServiceName"
Write-Host "  4) 다음 PC 재부팅 시 자동 기동 확인 (port 8080 + 5173)"
Write-Host ""
Write-Host "제거: .\uninstall-nssm-services.ps1" -ForegroundColor DarkGray
