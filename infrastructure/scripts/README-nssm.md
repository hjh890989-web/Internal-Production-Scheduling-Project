# NSSM Windows 자동시작 (Sprint 19 EP-BETA-LAUNCH ST-BETA-5)

베타 운영 PC 재부팅 시 backend (Spring Boot) + frontend (Vite) 자동 기동.

## 전제 조건

| 항목 | 설치 / 확인 |
|---|---|
| **Java 21 JDK** | `java -version` → 21.x |
| **Node.js 20+** | `node -v` → v20+ ; `npm -v` → v10+ |
| **Docker Desktop** | Settings → General → "Start Docker Desktop when you sign in" ✅ |
| **Postgres/Redis 컨테이너** | `docker update --restart=unless-stopped scheduling-postgres scheduling-redis` |
| **NSSM** | https://nssm.cc/download → `C:\nssm-2.24\` 에 압축 해제 (기본 경로) |

## 설치 (관리자 PowerShell)

```powershell
cd "E:\Antigavity Workspace\Internal Production Scheduling Project\infrastructure\scripts"
.\install-nssm-services.ps1
```

**옵션** — 기본값 다르게 사용 시:
```powershell
.\install-nssm-services.ps1 `
    -NssmPath "D:\tools\nssm.exe" `
    -ProjectRoot "D:\projects\scheduling"
```

### 동작
- `Scheduling-Backend` 서비스 등록 (`AUTO_START`)
  - 실행: `backend\gradlew.bat :app:bootRun --no-daemon --console=plain`
  - 환경: `SPRING_PROFILES_ACTIVE=with-infra`
  - 로그: `backend\logs\nssm-backend-stdout.log` (10MB rotation)
- `Scheduling-Frontend` 서비스 등록 (`AUTO_START`)
  - 실행: `npm run dev` (frontend 디렉토리)
  - 로그: `frontend\logs\nssm-frontend-stdout.log` (10MB rotation)

## 첫 기동 (수동)

설치 직후 한 번 수동 시작:

```powershell
Start-Service Scheduling-Backend
Start-Sleep -Seconds 20
Start-Service Scheduling-Frontend
```

**검증**:
- `Get-Service Scheduling-*` → Status = Running
- http://localhost:8080/api/actuator/health → `{"status":"UP"}`
- http://localhost:5173 → 로그인 화면

## 일상 운영

```powershell
# 상태 확인
Get-Service Scheduling-Backend, Scheduling-Frontend

# 재시작 (backend 코드 변경 시)
Restart-Service Scheduling-Backend

# 로그 tail (별도 PowerShell 창)
Get-Content "E:\Antigavity Workspace\Internal Production Scheduling Project\backend\logs\nssm-backend-stdout.log" -Tail 100 -Wait
```

## 제거

```powershell
.\uninstall-nssm-services.ps1
```

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `Service did not respond...` | Backend 부팅 30초 초과 | NSSM `AppStopMethodConsole` 15초로 설정됨 — 30초+로 늘리려면 `nssm set Scheduling-Backend AppStopMethodConsole 30000` |
| `port 8080 already in use` | Backend 중복 기동 / 이전 PowerShell 잔여 | `Get-NetTCPConnection -State Listen -LocalPort 8080` 으로 PID 확인 후 `Stop-Process` |
| Frontend Vite 1회만 시작 후 종료 | npm.cmd PATH 미인식 | `Get-Command npm.cmd` 확인, 없으면 Node.js 재설치 |
| Docker 컨테이너 미기동 | Docker Desktop 자동시작 비활성 | Docker Desktop → Settings → General → "Start Docker Desktop when you sign in" |
| `Migration failed` (Flyway) | DB schema 충돌 | backend 로그에서 V0XX 확인 — DEV 환경이면 `docker exec scheduling-postgres psql ... DROP DATABASE scheduling; CREATE DATABASE scheduling;` 후 재시작 |

## 보안 주의

- NSSM 서비스는 LocalSystem 계정 default — `.\install-nssm-services.ps1` 후 `services.msc` → 속성 → 로그온 탭 에서 전용 계정 설정 권장 (PROD).
- `nssm-backend-stderr.log` 에 비밀번호 / 토큰 노출 가능성 — 정기 로그 검토.
