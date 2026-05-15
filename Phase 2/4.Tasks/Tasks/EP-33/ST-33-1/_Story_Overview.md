# Story Overview — [EP-33] ST-33-1 Docker Compose v2 STG·PROD 환경

**Sprint**: S0 | **Epic**: EP-33 | **Priority**: Must ⭐⭐ | **SP**: 3 | **PD**: ~2.1 PD

## Story 목적
> WBS §10 EP-33 ST-33-1 / ADR-013: "Docker Compose v2 STG·PROD 환경 분리"

## 포함 Task 목록

| Task | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-33-1-1](TK-33-1-1.md) | STG 환경 (`docker-compose.stg.yml`) | 0.7 | DevOps |
| [TK-33-1-2](TK-33-1-2.md) | PROD 환경 (`docker-compose.prod.yml`) + 무중단 배포 | 0.8 | DevOps |
| [TK-33-1-3](TK-33-1-3.md) | 환경별 변수 분리 + secrets 관리 | 0.6 | DevOps + Security |

> 선행: [EP-00 ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md), [EP-32](../../EP-32/) (CI/CD)
> 후행: ST-33-2

## Story 레벨 DoD
- [ ] `docker-compose.stg.yml` — STG 환경 정의 (test data + 베타 사용자)
- [ ] `docker-compose.prod.yml` — PROD 환경 (production data + 실사용자)
- [ ] `.env.stg`·`.env.prod` 분리 (gitignore)
- [ ] 무중단 배포 — NGINX upstream toggle (blue/green)
- [ ] 운영 가이드 (`docs/operations/deploy.md`)

## References
- WBS §10 EP-33 ST-33-1, ADR-013, SAD §8

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
