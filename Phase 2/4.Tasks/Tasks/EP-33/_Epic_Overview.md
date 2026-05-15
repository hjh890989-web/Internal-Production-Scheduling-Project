# Epic Overview — [EP-33] 배포 + 백업·복원 (Docker Compose, pg_basebackup — ADR-013) ⭐

**Sprint**: S0~S5 분산 | **Priority**: Must ⭐⭐ (Phase 3 진입 게이트) | **SP**: 5 | **PD**: ~3.5 PD

---

## Epic 목적

> WBS §10 EP-33 인용: "Docker Compose v2 STG·PROD 환경 + pg_basebackup + WAL archiving + 분기 복원 드릴"
> SAD §8 / ADR-013 / SRS REQ-NF-REL-005: "STG/PROD 환경 분리 + 일 1회 백업 ≥ 30일 보존 + RPO ≤ 24h + RTO ≤ 4h"

본 Epic은 **Phase 3 진입 결정적 인프라**. NFR EP-41 (신뢰성)·EP-46 (비용) 모두 선행 의존. STG/PROD 분리 + pg_basebackup + WAL continuous archiving + 분기 복원 드릴.

**Why P1 Critical (Phase 3 진입 게이트)**:
- **EP-41 ST-41-4** (백업·RPO·RTO) — 본 Epic 선행 필수
- **EP-46 ST-46-3** (운영 ≤ 0.5 FTE) — 배포 자동화 의존
- **Phase 3 첫 배포** — STG 환경 없으면 시연 불가

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-33-1](ST-33-1/_Story_Overview.md) | Docker Compose v2 STG·PROD 환경 | 3 | ~2.1 | T-I + A | ☐ |
| [ST-33-2](ST-33-2/_Story_Overview.md) | pg_basebackup + WAL archiving + 분기 복원 드릴 | 2 | ~1.4 | T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **Docker Compose v2 STG·PROD** — 환경별 변수 (`docker-compose.stg.yml`, `docker-compose.prod.yml`)
- [ ] **`.env` 분리** — STG·PROD 비밀번호·도메인 분리 + .gitignore
- [ ] **pg_basebackup 야간 02:00 KST** — systemd timer 자동
- [ ] **WAL archiving continuous** — `archive_command` S3 또는 NAS
- [ ] **STG PITR 드릴 분기 1회** — RTO ≤ 4h 측정
- [ ] **무중단 배포** — NGINX upstream toggle (blue/green)

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §10 EP-33
- **SAD**: §8 배포·복원 ADR-013
- **SRS REQ-NF**: REL-005 (백업·RPO·RTO)
- **선행**: EP-00 (Docker Compose 기본), EP-32 (CI/CD)
- **후행**: **EP-41 (신뢰성 NFR)**, **EP-46 (운영 비용 NFR)**

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §10 EP-33, ADR-013 + REQ-NF-REL-005 |
