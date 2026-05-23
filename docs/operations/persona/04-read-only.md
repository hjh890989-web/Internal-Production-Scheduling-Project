# READ_ONLY — 감사·임원 가이드

**Role**: READ_ONLY (Keycloak) | **권한 등급**: P4 (조회)
**책임**: 시스템 현황 조회 + 사업 KPI 모니터링 + audit 추적성 검증 (모든 mutation 차단)

> READ_ONLY 는 **법무·감사·임원** 대상. 모든 화면 read-only, mutation API 차단.
> NFR-SEC-004 (3년 audit 보존) 검증 진입점 + 사업 의사결정 인사이트 확보.

---

## 1. 접근 가능 화면

| 경로 | 화면 | 접근 |
|---|---|---|
| `/home` | Home | ✅ KPI 요약 |
| `/orders/import` | 수주 통합 | ✅ 매핑 검토 read only (확정 버튼 disable) |
| `/vc/simview` | 성형 시뮬뷰 | ✅ 회전 격자 read (swap 제안 disable) |
| `/extrusion-matrix` | 압출 매트릭스 | ✅ 매트릭스 + Ranking + Excel 다운로드 |
| `/audit/restore` | 마스터 복원 | ✅ forensic 조회 (실제 복원은 IT_OPS) |
| `/vc/capacity-queue` | Capa 큐 + KD 보충 | ❌ 403 (BR-V12·V13 PLANNER 단독, [BS-06](../beta-scenarios/06-capacity-overflow-kd-supplement.md) 참조) |
| Grafana | http://stg.../3000 | ✅ business-kpi 대시 + scheduling-overview |
| ❌ confirm / accept / override / record / split / supplement | 모두 403 Forbidden |

---

## 2. 사업 KPI 모니터링 (Grafana business-kpi 대시)

### 2.1 9 baseline KPI (V032 seed)

| KPI | 의미 | 임계 | 의사결정 영향 |
|---|---|---|---|
| **NS-S01** | P1·P4 만족도 | ≥ 95% | 분기별 보고 |
| **NS-S04** | Kakao 도달률 | ≥ 95% | IT 운영 보강 |
| **NS-S07** | D-1 압출 deadline 준수율 | ≥ 98% | 영업 ↔ 생산 협업 강화 |
| **NS-S09** | 신규 라인 사용률 (BR-E08) | ≥ 90% | 라인 투자 우선순위 |
| **K-V01** | 슬롯 점유율 | ≥ 85% | 가류기 증설 / 효율화 |
| **K-V02** | 가류기 가동률 | ≥ 85% | 보전 + 셋업 시간 단축 |
| **K-V04** | 일중 락 위반 (BR-V07) | = 0 (일별) | 마스터 데이터 보강 |
| **K-E02** | 압출 셋업 시간 | ≤ 30분 | 셋업 자동화 검토 |
| **K-E03** | shift 가동 효율 (BR-E04) | ≥ 75% | shift 패턴 최적화 |

### 2.2 추가 KPI (Phase 5 누적 후 확장)

- 10개 카테고리 (NS · K-V · K-E) × 19 KPI 정의 — `business_kpi.definition`
- Sprint 7+ — 베타 운영 데이터 누적 후 임계값 재조정
- 🆕 **Sprint 8+ 후보 (BR-V12·V13)** — KD remaining_qty per hose (잔량 부족 경고) · BR-V12 추가 요청 큐 누적 · BR-V13 그룹 fallback 비율 ([BS-06 KPI 영향](../beta-scenarios/06-capacity-overflow-kd-supplement.md))

---

## 3. audit 감사 추적 — NFR-SEC-004 검증

### 3.1 audit immutability 확인

```sql
-- audit.schedule_audit_log UPDATE 시도 (DBA 권한도 거부)
UPDATE audit.schedule_audit_log SET reason = '변조' WHERE audit_id = 1;
-- → ERROR: NFR-SEC-004 audit row 변조 금지

DELETE FROM audit.schedule_audit_log;
-- → ERROR: NFR-SEC-004 audit row 변조 금지

TRUNCATE audit.schedule_audit_log;
-- → ERROR: NFR-SEC-004 audit TRUNCATE 금지
```

### 3.2 audit timeline forensic UI

- `/audit/restore` 진입 → table + UUID + timestamp → snapshot + timeline
- 모든 mutation 100% 영속 (BR-X02)
- 3년 보존 — V030 월별 RANGE partition 36 (2026-01 ~ 2028-12)

---

## 4. 분기별 임원 보고 입력 (Phase 5+)

- Grafana 대시 PDF export (월별 / 분기별)
- 19 KPI 추세 + 임계값 미달 사유 + 개선 액션
- audit row count + override 발생 횟수 + Resilience4j retry 통계

---

## 5. FAQ

### Q1. mutation API 가 차단되는데 이유는?
**A**. RBAC `@PreAuthorize` 가 READ_ONLY 를 confirm/accept/override 메서드에서 제외.
법무·감사·임원의 시스템 변경 권한 분리 (compliance).

### Q2. audit 데이터가 변조될 수 있나요?
**A**. 불가능. V026 immutability 트리거 + REVOKE 권한 + 3년 보존 monthly partition.
DBA 권한도 차단.

### Q3. KPI 임계값을 변경할 수 있나요?
**A**. READ_ONLY 는 불가능. IT_OPS 가 `POST /api/v1/kpi/definitions` (Sprint 7+) 통해 변경.

### Q4. 시스템에서 출력되는 Excel 의 시트명은 항상 한국어인가요?
**A**. 네. BR-E09 정규식 `\d+월\d+일(압출)` 강제. EP-12 + EP-45 cross-browser 회귀 통과.

---

## 6. 비상 연락

| 상황 | 연락처 |
|---|---|
| 시스템 접근 불가 | 사내 IT 헬프데스크 |
| audit 데이터 의심 (변조 가능성) | IT_OPS + 법무 |
| KPI 보고 데이터 필요 | IT_OPS Slack DM |

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — READ_ONLY 페르소나 가이드 + 19 KPI + audit immutable |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over — `/vc/capacity-queue` 403 명시 + Sprint 8+ V12·V13 KPI 후보 추가 |
