# Story Overview — [EP-32] ST-32-2 Trivy 이미지 스캔 + 품질 게이트

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-32 CI/CD (Jenkins + Harbor + SonarQube — ADR-015) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD (2 SP × 0.7 PD)

---

## Story 목적

> SAD §5.8 인용: "**테스트 게이트** = 단위 80% 커버리지 + 통합 + **SonarQube quality gate** / **취약점 스캔** = **Trivy** (이미지 + 의존성)"

본 Story는 ST-32-1의 Jenkinsfile 파이프라인에 **3가지 게이트**를 추가:
1. **Trivy 이미지·의존성 취약점 스캔** (HIGH/CRITICAL 0건 강제) — OWASP Top 10 차단
2. **SonarQube quality gate** (80% 커버리지·중복·코드 스멜·보안 핫스팟) — 단일 위반 시 빌드 실패
3. **Slack 알림** (성공·실패·취약점 발견) — 60초 이내 (REQ-NF-OPS-003)

**Why ST-32-1 다음에 본 Story가 오는가**:
- ST-32-1이 **파이프라인 인프라**를 셋업 → 본 Story는 **품질·보안 게이트**를 그 위에 추가
- 빌드 실패 시 PR 머지 차단 = 운영 가용성 보호 (REQ-NF-REL-001·SEC 일반)
- Slack 알림 = 인시던트 SLA 60초 이내 (REQ-NF-OPS-003) 직접 충족

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-32-2-1](TK-32-2-1.md) | Trivy 컨테이너 + 이미지·의존성 취약점 스캔 통합 | 0.5 | DevOps + 보안 | T-I + I | ☐ |
| [TK-32-2-2](TK-32-2-2.md) | SonarQube CE 컨테이너 + quality gate (커버리지 80%·중복·핫스팟) | 0.5 | DevOps + 백엔드 | T-I + A | ☐ |
| [TK-32-2-3](TK-32-2-3.md) | 빌드 실패·취약점 발견 시 Slack 알림 (≤60초) | 0.4 | DevOps | T-I + D | ☐ |

> **선행 의존**: ST-32-1 (Jenkins + Jenkinsfile.template — 본 Story가 게이트 추가)
> **후행 차단**: 모든 Sprint 1+ Epic의 PR/merge — 본 Story의 게이트 통과 필수

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과
- [ ] **Trivy 스캔에서 HIGH/CRITICAL 취약점 0건 강제** (빌드 실패 게이트)
- [ ] **SonarQube quality gate 통과**: 단위 테스트 ≥80% 라인 커버리지 + 코드 스멜 0 (Critical) + 중복 ≤3%
- [ ] **Slack 알림 ≤60초** 도달 (REQ-NF-OPS-003) — 빌드 성공·실패 + 보안 이슈 발견 시
- [ ] 빌드 실패 시 PR 머지 차단 (Branch Protection Rule)
- [ ] Sprint Review 데모: 의도적 취약 의존성 추가 PR → 빌드 FAILED + Slack 알림 시연

---

## References (공통)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.10 EP-32 ST-32-2
- **SAD ADR**: **ADR-015** (Jenkins + Harbor + SonarQube + Trivy)
- **SAD §5.8** (line 426~428): 테스트 게이트·코드 품질·취약점 스캔 정의
- **SRS REQ-NF**:
  - **REQ-NF-OPS-003** ("시스템 에러·알림 발송 실패는 **60초 이내** Slack 알림") → 빌드 실패 알림
  - REQ-NF-SEC-005 (민감 데이터 보호) → Trivy가 dependencies CVE 차단
  - 모든 NFR-SEC → Trivy + SonarQube 보안 핫스팟
- **연관 Story**: [ST-32-1](../ST-32-1/_Story_Overview.md) (Jenkinsfile 파이프라인 — 본 Story가 게이트 추가)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | 초안 작성 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.10 EP-32 ST-32-2 + SAD ADR-015·§5.8 기반 |
