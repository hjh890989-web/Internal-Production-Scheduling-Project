# Story Overview — [EP-32] ST-32-1 Jenkins LTS + 표준 파이프라인

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-32 CI/CD (Jenkins + Harbor + SonarQube — ADR-015) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD (3 SP × 0.7 PD)

---

## Story 목적

> SAD ADR-015 인용: "**Jenkins LTS** + (별 호스트) Harbor·SonarQube — 한국 사내 표준, 플러그인 풍부, 사내 SSO 통합 용이"

본 Story는 **Jenkins LTS 컨테이너** + **표준 Jenkinsfile 파이프라인** + **NGINX upstream 토글 무중단 배포** 인프라를 셋업한다. ST-00-2의 ArchUnit 테스트 + ST-00-3의 lint/Vitest 게이트가 본 Story의 파이프라인 통해 매 PR/merge 시 강제됨. **Sprint 0 DoD 항목 4번 직접 달성**: "CI/CD 파이프라인 초기 빌드 1회 성공".

**Why 본 Story가 Phase 0 마지막 핵심 작업인가**:
- 모든 Sprint 1+ 작업의 **PR/merge 게이트** — 본 Story가 없으면 코드 품질 회귀 방지 불가
- ST-00-2 ArchUnit + ST-00-3 lint가 **Jenkins 파이프라인 안에서 실행**되어야 빌드 실패 시 merge 차단
- ADR-015 의사결정 검증 — Jenkins LTS + Harbor + SonarQube + Trivy 조합이 사내 인프라에서 실제 동작하는지 사전 확인
- SAD §5.8 CI/CD 명세 (테스트 게이트 80% 커버리지·SonarQube quality gate·Trivy 취약점 스캔)의 구현 기반

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-32-1-1](TK-32-1-1.md) | Jenkins LTS 컨테이너 + Jenkinsfile declarative 표준 템플릿 | 0.7 | DevOps | T-I + I | ☐ |
| [TK-32-1-2](TK-32-1-2.md) | 파이프라인 단계: checkout → build → test → SonarQube → Harbor push | 0.8 | DevOps + 백엔드 | T-I + D | ☐ |
| [TK-32-1-3](TK-32-1-3.md) | NGINX upstream 토글 무중단 배포 (Blue-Green ≤30초) | 0.6 | DevOps | T-I + D | ☐ |

> **선행 의존**: ST-00-1 (Docker Compose — Jenkins는 별 서버지만 DEV는 같은 호스트 가능), ST-00-2 (Backend 빌드 대상), ST-00-3 (Frontend 빌드 대상)
> **후행 차단**: 모든 Sprint 1+ Epic의 PR/merge — 본 Story의 파이프라인 게이트 통과 필수

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **`docker compose up jenkins`로 Jenkins LTS 컨테이너 부팅 + healthcheck 통과**
- [ ] Jenkinsfile declarative 표준 템플릿 작성 — 모든 Sprint 1+ Epic이 본 템플릿 재사용
- [ ] **Sprint 0 DoD 항목 4번 직접 달성**: CI/CD 파이프라인 초기 빌드 1회 성공 (Backend·Frontend 둘 다)
- [ ] PR → Jenkins 자동 트리거 (webhook 또는 SCM polling)
- [ ] 빌드 단계: checkout → Gradle/npm build → 단위 테스트 → SonarQube → Trivy → Harbor push
- [ ] 빌드 실패 시 PR 머지 차단 + Slack 알림 (ST-32-2와 결합)
- [ ] NGINX upstream 토글 무중단 배포 시나리오 (Blue-Green ≤30초) 검증
- [ ] 코드 리뷰 1명 이상 승인 (DevOps + 백엔드 리드)
- [ ] Sprint Review 데모: PR 생성 → Jenkins 빌드 → 단계별 통과 → Harbor push → 배포 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.10 EP-32 ST-32-1
- **SAD ADR**: `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md`
  - **ADR-015** ("**(a) Jenkins LTS** + (별 호스트) Harbor·SonarQube — 한국 사내 표준, 플러그인 풍부, 사내 SSO 통합 용이")
- **SAD §5.8 CI/CD**:
  - CI 서버: Jenkins LTS (별 서버, 사내 보안 정책 적합)
  - 파이프라인 정의: Jenkinsfile declarative (코드로 관리)
  - 테스트 게이트: 단위 80% 커버리지 + 통합 + SonarQube quality gate
  - 코드 품질: SonarQube CE + Spotless (Java) + ESLint·Prettier (TS)
  - 취약점 스캔: Trivy (이미지 + 의존성)
  - 컨테이너 레지스트리: Harbor 또는 Nexus Repository (사내)
- **SAD §3 컨테이너 뷰** (line 175~176): `CIZONE [CI/CD — 별 서버 권장]` + JENKINS
- **SAD §5.6 컨테이너·배포** (line 404): "Blue-Green via `docker compose up -d --no-deps --build` + NGINX upstream 토글 — 30명 규모에선 잠깐 중단 허용 가능, 단 NGINX 토글로 ≤30초"
- **SRS REQ-NF**:
  - **REQ-NF-OPS-003** ("시스템 에러·알림 발송 실패는 60초 이내 Slack 알림") → 빌드 실패 알림
  - **REQ-NF-REL-001** ("영업시간 가용성 ≥99.5%") → 무중단 배포 (≤30초 토글)
  - **REQ-NF-COM-004** (API 전방 호환성) → ArchUnit·SonarQube 게이트로 회귀 방지
- **연관 Story (병렬)**:
  - 선행: [ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md) (Docker Compose), [ST-00-2](../../EP-00/ST-00-2/_Story_Overview.md) (Backend 빌드 대상), [ST-00-3](../../EP-00/ST-00-3/_Story_Overview.md) (Frontend 빌드 대상)
  - 같은 Epic: [ST-32-2](../ST-32-2/_Story_Overview.md) (Trivy + SonarQube quality gate + 알림 — 본 Story의 파이프라인 위에 게이트 추가)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | 초안 작성. 성격: CI/CD 인프라 — 6번째 도메인 (DevOps 파이프라인) |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.10 EP-32 ST-32-1 + SAD ADR-015·§5.8 기반. Task 기반 분해 v1 여섯 번째 적용 (CI/CD — 6번째 도메인) |
