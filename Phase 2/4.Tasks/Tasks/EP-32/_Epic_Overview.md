# Epic Overview — [EP-32] CI/CD (Jenkins + Harbor + SonarQube — ADR-015) ⭐

**Sprint**: S0 | **Priority**: Must ⭐⭐ (Phase 3 진입 게이트) | **SP**: 5 | **PD**: ~3.5 PD

---

## Epic 목적

> WBS §10 EP-32 인용: "Jenkins LTS + 표준 파이프라인 + Trivy 이미지 스캔 + SonarQube 품질 게이트"
> SAD ADR-015 / SRS REQ-NF-OPS-001~007 / NFR-SEC-006: "Build → Test → SonarQube → Trivy → Harbor → Deploy 자동화 파이프라인"

본 Epic은 **Phase 3 진입 결정적 인프라**. 모든 기능 Epic의 빌드·테스트·배포 표준화. 무중단 배포 (EP-33 ST-33-1 정합). 보안 (NFR EP-42) + 운영 (NFR EP-44) 모두 의존.

**Why P1 Critical (Phase 3 진입 게이트)**:
- **모든 Sprint 1~5** — 신규 Task 머지 시 CI 검증 필수
- **NFR EP-40·45** — k6 부하 회귀 + 30 user 부하 게이트
- **NFR EP-42 SEC-006** — Trivy 이미지 스캔
- **EP-33 무중단 배포** — Jenkins → NGINX upstream toggle chain

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-32-1](ST-32-1/_Story_Overview.md) | Jenkins LTS + 표준 파이프라인 | 3 | ~2.1 | T-I + A | ☐ |
| [ST-32-2](ST-32-2/_Story_Overview.md) | Trivy 이미지 스캔 + 품질 게이트 | 2 | ~1.4 | T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **Jenkins LTS** 컨테이너 (Docker Compose 통합)
- [ ] **Jenkinsfile 표준 템플릿** — build → test → SonarQube → Trivy → Harbor push → deploy
- [ ] **Harbor 이미지 registry** — 사내 image 보관
- [ ] **SonarQube 품질 게이트** — 커버리지 ≥ 80%, code smell 차단
- [ ] **Trivy 이미지 스캔** — Critical 취약점 0건
- [ ] **NGINX 무중단 배포 toggle** — EP-33 ST-33-1-2 chain
- [ ] **빌드 실패 → Slack 알림** (EP-31 ST-31-2 정합)
- [ ] **첫 빌드 성공** — Sprint 0 DoD 핵심

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §10 EP-32
- **SAD**: ADR-015 (Jenkins + Harbor + SonarQube 채택)
- **SRS REQ-NF**: OPS-001~007 (관측·CI/CD), SEC-006 (이미지 스캔)
- **선행**: EP-00 (Docker Compose)
- **후행**: **모든 Sprint 1~5 Epic** (CI 의존), **EP-30·31·33** (인프라 통합), **EP-40·45** (성능 회귀)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §10 EP-32, ADR-015 + 4차 감사 minor risk 해소 |
