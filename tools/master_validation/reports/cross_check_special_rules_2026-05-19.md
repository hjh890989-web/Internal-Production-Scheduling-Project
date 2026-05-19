# 품번 특수 제약 cross-check 리포트 (2026-05-19)

대상: `28422-08HA0` (BR-V14) · `28422-2M800` (BR-V15) · `28421-2M800` (BR-V16)

## 1. 요약

- 검사 품번: 3 (BR-V14·V15·V16)
- 불일치 발견: **0** 건

## 2.1 28422-08HA0 — BR-V14

**룰**: LP 단일 호기 (LP-01) 단일 셋팅. max_concurrent_slots=1.

**SRS**: REQ-FUNC-VC-024

### 기대 vs 실측

| 항목 | 기대 | 실측 |
|---|---|---|
| K(좌측) | `o` | `o` |
| L(우측) | `o` | `o` |
| LP 가류기 | (any) | `20` |
| LP 앵글수 | (any) | `6` |
| IC 가류기 | 0 (LP only) | `0` |
| 합금형 | (any) | `1` |
| 사양 | (any) | `18` |

### cross-check 결과

✅ **모든 항목 명세 일치**

**VC_HOSE_RULE 시드 SQL 권장** (Sprint 1+ EP-21 Flyway):

```sql
INSERT INTO master.vc_hose_rule (hose_id, br_code, side_only, max_concurrent_slots, lp_only)
VALUES ('28422-08HA0', 'BR-V14', NULL, 1, TRUE);
```

## 2.2 28422-2M800 — BR-V15

**룰**: LP 우측 only. max_concurrent_slots=2.

**SRS**: REQ-FUNC-VC-025

### 기대 vs 실측

| 항목 | 기대 | 실측 |
|---|---|---|
| K(좌측) | `x` | `x` |
| L(우측) | `o` | `o` |
| LP 가류기 | (any) | `25` |
| LP 앵글수 | (any) | `5` |
| IC 가류기 | (any) | `20` |
| 합금형 | (any) | `2` |
| 사양 | (any) | `13.5` |

### cross-check 결과

✅ **모든 항목 명세 일치**

**VC_HOSE_RULE 시드 SQL 권장** (Sprint 1+ EP-21 Flyway):

```sql
INSERT INTO master.vc_hose_rule (hose_id, br_code, side_only, max_concurrent_slots, lp_only)
VALUES ('28422-2M800', 'BR-V15', 'right', 2, FALSE);
```

## 2.3 28421-2M800 — BR-V16

**룰**: LP 좌측 only. max_concurrent_slots=2.

**SRS**: REQ-FUNC-VC-026

### 기대 vs 실측

| 항목 | 기대 | 실측 |
|---|---|---|
| K(좌측) | `o` | `o` |
| L(우측) | `x` | `x` |
| LP 가류기 | (any) | `25` |
| LP 앵글수 | (any) | `7` |
| IC 가류기 | (any) | `20` |
| 합금형 | (any) | `2` |
| 사양 | (any) | `13.5` |

### cross-check 결과

✅ **모든 항목 명세 일치**

**VC_HOSE_RULE 시드 SQL 권장** (Sprint 1+ EP-21 Flyway):

```sql
INSERT INTO master.vc_hose_rule (hose_id, br_code, side_only, max_concurrent_slots, lp_only)
VALUES ('28421-2M800', 'BR-V16', 'left', 2, FALSE);
```


---
## dual-review (BR-X05)

| 역할 | 이름 | 사인오프 일자 |
|---|---|---|
| P1 생산계획 주임 | (서명) | (일자) |
| STK-08 IT lead | (서명) | (일자) |
