# 워크북 분류 정확도 리포트 v1.0

**작성**: 2026-05-20 | **Task**: TK-01-1-4 | **Test**: `ClassificationAccuracyTest`
**검증 대상**: TC-OC-002 (≥99% 정확도) — REQ-FUNC-OC-002

---

## 1. 요약

| 항목 | 값 |
|---|---|
| 데이터셋 | DS-ORDER-3X (30 합성 워크북) |
| 정확도 | **30/30 = 100.0%** ✓ |
| 목표 | ≥99% (TC-OC-002) |
| 결과 | **PASSED** (목표 +1.0%pt 초과) |

---

## 2. SourceType 별 분포

| SourceType | 워크북 수 | 정확 분류 | 정확도 |
|---|---:|---:|---:|
| MONTHLY_FORECAST | 7 | 7 | 100.0% |
| WEEKLY_PLAN | 7 | 7 | 100.0% |
| CONFIRMED_ORDER | 8 | 8 | 100.0% |
| KD_ORDER | 8 | 8 | 100.0% |
| **합계** | **30** | **30** | **100.0%** |

---

## 3. 어휘 변형 회귀 (분류기 견고성 — SRS-RSK-007)

각 SourceType별 7~8건의 어휘 변형 모두 정확 분류:

- **한글·영문 혼합** 헤더 (월별 예상 / FORECAST / 월간 예상)
- **시트명 변형** (Forecast / 월간 예상 / FORECAST)
- **파일명 변형** (`monthly_forecast_jan.xlsx`, `FORECAST_FEB_2026.xlsx`, `예상 발주 4월.xlsx`)
- **컬럼 헤더 변형** (한글·영문·동의어)

---

## 4. 데이터셋 위치

```
backend/order/src/test/resources/workbooks/DS-ORDER-3X/
├── monthly/    7 .xlsx
├── weekly/     7 .xlsx
├── confirmed/  8 .xlsx
└── kd/         8 .xlsx
```

생성 스크립트: `scripts/generate_test_workbooks.py` (seed=42 — 재현 가능).

---

## 5. 알고리즘 동작 (참고)

- **Score**: `(0.4 + 0.3 × matches) × weight`, max 1.0
- **임계치**: 0.5 미만 → UNRECOGNIZED
- **KD weight**: 1.5 (강한 시그니처, cross-source 오분류 방지)
- **excluded 키워드**: 충돌 시 score 0 (강제 제외)
- **결정적**: 같은 입력 → 같은 결과 (Flaky 0)

---

## 6. 운영 알려진 한계 (실 데이터 적용 시)

| 시나리오 | 분류 결과 | 권장 대응 |
|---|---|---|
| 파일명에 다중 source 키워드 혼합 (예: "주차" + "확정") | 점수 동률 시 알고리즘 순서 의존 | YAML 룰셋 가중치 조정 + IT 관리자 결재 |
| 신규 어휘 도래 (예: "예약 발주") | UNRECOGNIZED | 운영자 → header-signatures.yaml 추가 |
| 동일 의미·완전 신규 sheet 구조 | confidence 낮을 가능성 | 0.5~0.7 confidence 시 사용자 확인 모달 (ST-01-2 UI) |

---

## 7. Phase 0 실 데이터 보강 권장 (TR-01 완화)

본 v1.0 리포트는 **합성 데이터** 기반. 실 운영 진입 전:

1. P1 김정훈 주임 보유 실 워크북 5~10건 회귀 세트에 추가
2. 정확도 재측정 (≥99% 유지 확인)
3. 미달 시 `header-signatures.yaml` 룰셋 보강 + 본 리포트 v1.1 갱신

---

## 8. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | TK-01-1-4 | 초안 — DS-ORDER-3X 30 워크북 100% 정확도 달성 |
