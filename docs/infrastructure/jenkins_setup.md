# Jenkins LTS 운영 가이드 (TK-32-1-1)

본 문서는 TK-32-1-1 산출물 (`infrastructure/jenkins/` + `infrastructure/ci/`) 운영 안내.

## 1. 첫 부팅 (DEV)

```powershell
cd infrastructure\jenkins
# admin 비밀번호 생성 (사전 생성됨 — secrets/jenkins_admin_password.txt)
docker compose -f docker-compose.jenkins.yml build
docker compose -f docker-compose.jenkins.yml up -d
docker compose -f docker-compose.jenkins.yml ps   # healthy 대기 ~120초 (플러그인 초기화)
```

첫 build ~5분 (~25 플러그인 다운로드 + Docker CLI 설치). 다음부터 ~30초 (캐시).

## 2. 접근

- URL: **http://localhost:8090** (DEV 만)
- Username: `admin`
- Password: `infrastructure/jenkins/secrets/jenkins_admin_password.txt` 내용

setupWizard 가 skip 되어 직접 로그인 화면 진입.

## 3. CASC (Configuration as Code) 갱신

```powershell
# casc/jenkins.yaml 수정 후
docker compose -f docker-compose.jenkins.yml restart jenkins
# 또는 Manage Jenkins → Configuration as Code → Reload existing configuration
```

CASC 가 Jenkins 설정의 GitOps 원천 — 모든 변경이 git 추적 가능.

## 4. Keycloak 페더레이션 활성 (Sprint 1+ — EP-30 ST-30-1 완료 후)

DEV 단계는 local admin. PROD 활성 절차:

1. Keycloak realm `scheduling-system` 에 `jenkins` client 등록:
   - Client ID: `jenkins`
   - Client authentication: ON (confidential)
   - Standard flow: ON
   - Valid redirect URIs: `http://jenkins.internal/securityRealm/finishLogin`
2. Keycloak 에서 client secret 복사
3. `infrastructure/jenkins/casc/jenkins.yaml` 수정:
   ```yaml
   securityRealm:
     keycloak:
       keycloakJson: |
         {
           "realm": "scheduling-system",
           "auth-server-url": "http://keycloak:8080",
           "ssl-required": "external",
           "resource": "jenkins",
           "public-client": false,
           "credentials": { "secret": "${KEYCLOAK_JENKINS_SECRET}" }
         }
   ```
4. `restart jenkins` → Jenkins 로그인 화면이 Keycloak 로그인으로 redirect

## 5. 표본 Job 등록

1. http://localhost:8090 로그인 → New Item → Pipeline → name: `scheduling-backend`
2. **Pipeline script from SCM**:
   - SCM: Git
   - Repository URL: `https://github.com/hjh890989-web/Internal-Production-Scheduling-Project.git`
   - Branch: `main`
   - Script Path: `infrastructure/ci/Jenkinsfile.template` (또는 각 서비스의 `Jenkinsfile`)
3. Save → **Build Now**

표본 빌드 — Backend `./gradlew build` 단계는 TK-32-1-2 에서 채움. 본 Task 시점에는 stage 들이 placeholder (echo 만).

## 6. Credentials 등록 (Sprint 1+ — Harbor·SonarQube·Slack 사용 시)

DEV 단계는 미사용. PROD 활성 절차:

```powershell
# Manage Jenkins → Credentials → System → Global → Add Credentials
# 또는 CASC 의 credentials 섹션에 추가 (이상적):
```

```yaml
# casc/jenkins.yaml 추가
credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              scope: GLOBAL
              id: "github-pat"
              username: "${GITHUB_USERNAME}"
              password: "${GITHUB_PAT}"
          - usernamePassword:
              scope: GLOBAL
              id: "harbor-credentials"
              username: "${HARBOR_USERNAME}"
              password: "${HARBOR_PASSWORD}"
          - string:
              scope: GLOBAL
              id: "sonarqube-token"
              secret: "${SONARQUBE_TOKEN}"
```

`${VAR}` 변수는 환경 변수 또는 docker secrets 에서 주입.

## 6.1 Tools 등록 (JDK 21 / Gradle 8.10 / NodeJS 20)

Jenkinsfile.backend·frontend 의 `tools { }` 블록을 활성화하려면 Jenkins Admin 에서 사전 등록 (TK-32-1-2 산출물):

1. **Manage Jenkins → Tools** → 각 도구별 add installation:
   - **JDK installations**: name=`JDK 21`, Install from adoptium.net → version `jdk-21+35`
   - **Gradle installations**: name=`Gradle 8.10`, Install from gradle.org → version `8.10.2`
   - **NodeJS installations**: name=`NodeJS 20`, Install from nodejs.org → version `20.18.0`
2. Save → `Jenkinsfile.backend`·`Jenkinsfile.frontend` 의 `tools { }` 주석 해제 → 빌드 시 자동 다운로드 + 사용
3. 또는 CASC `casc/jenkins.yaml` 의 `tool:` 섹션에 명시해서 GitOps 관리 (`adoptium-installer`·`gradleInstaller`·`nodeJSInstaller` 플러그인 의존성 추가 필요)

## 7. Shared Library 등록 (선택, 권장)

`infrastructure/jenkins/shared-library/vars/stdPipeline.groovy` 를 Jenkins Shared Library 로 등록:

1. Manage Jenkins → Global Pipeline Libraries → Add
2. Name: `scheduling-shared-library`
3. Default version: `main`
4. Source Code Management: Git → 본 저장소 URL → Library Path: `infrastructure/jenkins/shared-library`

이후 Sprint 1+ Jenkinsfile:
```groovy
@Library('scheduling-shared-library') _
stdPipeline { language = 'java' }
```
→ 5 줄로 표준 파이프라인 사용.

## 8. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| 첫 부팅 unhealthy ~120s | 플러그인 초기화 진행 중 | `start_period: 120s` — 인내. logs 에서 "Jenkins is fully up and running" 확인 |
| Plugin install fail (network) | 사내 polrxy / 인터넷 차단 | `Dockerfile.jenkins` 의 `jenkins-plugin-cli` 에 `--war` 또는 offline mirror 사용 |
| admin 로그인 거부 | `secrets/jenkins_admin_password.txt` 누락 | 12자+ 3종 패스워드 파일 생성 |
| Docker-in-Docker fail | `/var/run/docker.sock` 권한 | 호스트 root + Jenkins 컨테이너 jenkins user 그룹 매핑 |
| CASC 적용 안 됨 | `CASC_JENKINS_CONFIG` 환경 변수 오류 | logs 에서 "Configuration as Code" 로딩 확인 |
| Build timeout 30분 초과 | 빌드 시간 길음 | `options { timeout(time: 60, unit: 'MINUTES') }` 늘림 |

## 9. 운영 환경 차이

| 항목 | DEV | PROD |
|---|---|---|
| ports | `127.0.0.1:8090, 50000` | `expose: 8080, 50000` + NGINX `/jenkins/*` reverse proxy |
| securityRealm | local admin | Keycloak federation (EP-30 ST-30-1) |
| credentials | 미사용 (placeholder) | 사내 vault 주입 (`${VAR}`) |
| Harbor·SonarQube | mock / skip | 사내 인프라 연동 |
| Docker-in-Docker | sock mount | 별 agent 컨테이너 권장 (보안) |
| 백업 | volume 만 | `jenkins-data` 별 NAS 백업 |

## 10. 보안 체크리스트 (PROD)

- [ ] securityRealm = Keycloak (EP-30 활성 후)
- [ ] CSRF protection (crumbIssuer) 활성 — CASC 기본
- [ ] Docker-in-Docker 대신 별 agent
- [ ] admin 비밀번호 12자+ 3종 (NFR-SEC-007)
- [ ] CVE 추적 — `jenkins/jenkins:lts-jdk21` 정기 업데이트 (월간)
- [ ] Plugin CVE 추적 — Manage Jenkins → Plugin Manager → Updates
- [ ] Backup `jenkins-data` 일일 NAS

## 11. Sprint 0 DoD 항목 4 (이 Task)

> "CI/CD 파이프라인 초기 빌드 1회 성공 — Jenkinsfile.template 위에서 backend `./gradlew build` 또는 frontend `npm run build` 통과"

DEV 검증: Jenkins 부팅 + 로그인 + 표본 Job 생성 + 빌드 시도까지. **실 빌드 SUCCESS 는 TK-32-1-2** (Jenkinsfile stage 채워 넣은 후).
