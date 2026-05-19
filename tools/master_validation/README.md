# Master Data Validation — EP-99

Sprint 0 마스터 데이터 무결성 검증 도구 모음. WBS §5.1 EP-99 ST-99-1·2 산출.

---

## 1. 도구 개요

| 스크립트 | Task | 검증 대상 |
|---|---|---|
| `validate_vc_master_kl.py` | TK-99-1-1 | 성형 마스터 K/L열 47품번 ∈ {o,x} |
| `cross_check_special_rules.py` | TK-99-1-2 | 28422-08HA0/28422-2M800/28421-2M800 룰 ↔ 마스터 |
| `sql/vc_constraint_regression.sql` | TK-99-1-3 | 회귀 SQL — CHECK/NULL/중복/미정의 호기 |
| `validate_ex_master_b.py` | TK-99-2-1 | 압출 마스터 B열(규격) 정수형 무결성 |
| `analyze_spec_distribution.py` | TK-99-2-2 | 규격<7 품번 식별 + 분포 통계 + VC/EX 일관성 |
| `analyze_br_v17_impact.py` | TK-99-2-3 | BR-V17 영향 7품번 + 수주 빈도 + 특수제약 중첩 |

---

## 2. 실행

```bash
# 단일 실행
python tools/master_validation/validate_vc_master_kl.py

# 5종 일괄
for s in validate_vc_master_kl validate_ex_master_b cross_check_special_rules \
         analyze_spec_distribution analyze_br_v17_impact; do
    python tools/master_validation/${s}.py
done

# SQL 회귀 (Sprint 1+ Flyway 적용 후)
docker compose exec postgres psql -U app_user -d scheduling \
    -f /tools/vc_constraint_regression.sql
```

---

## 3. 리포트 위치

- `tools/master_validation/reports/<script>_<YYYY-MM-DD>.md`
- 일자별 누적 보존 → As-Is 베이스라인 + 변동 추적

---

## 4. dual-review (BR-X05)

마스터 변경 또는 리포트 sign-off 시 P1 김정훈 주임 + STK-08 IT lead 양쪽 결재.
각 리포트 하단의 dual-review 표 작성.

---

## 5. CI 통합 (Sprint 1+)

```yaml
# Jenkinsfile.master-data (Sprint 1+ 추가 예정)
stage('Master Data Validation') {
    steps {
        sh 'python tools/master_validation/validate_vc_master_kl.py'
        sh 'python tools/master_validation/validate_ex_master_b.py'
        sh 'python tools/master_validation/cross_check_special_rules.py'
    }
}
```

비정상 종료 (exit !=0) 시 빌드 FAILED → Slack `#scheduling-security` 알림 (TK-32-2-3).
