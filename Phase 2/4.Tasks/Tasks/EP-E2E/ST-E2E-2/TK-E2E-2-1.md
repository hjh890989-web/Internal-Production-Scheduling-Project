---
name: DevOps Task (Beta User Setup)
about: 단일 작업 단위 — 0.4 PD
title: "[TK-E2E-2-1] 베타 사용자 설정 (Keycloak + 권한)"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-2, type:infra, priority:must, owner:devops'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-2-1
- **소속**: EP-E2E / ST-E2E-2 / Sprint S5
- **우선순위**: Must / **추정**: 0.4 PD (~3.2h) / **Owner**: DevOps
- **작업 요약**: Keycloak realm에 4 베타 사용자 + role 부여. PLANNER 2명 (생산계획팀), STK_USER 2명 (현장 STK-03 패드). 초기 비밀번호 + 첫 로그인 시 변경 강제. 사용 환경 (URL·접속 방법) 안내문서.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-2 (Task 1)
- **연관**: 선행 [TK-00-1-1](../../EP-00/ST-00-1/TK-00-1-1.md) (Keycloak)

---

## :hammer_and_wrench: Implementation Plan

```
infra/keycloak/
  beta-users.yaml                                [사용자 정의]
  setup-beta-users.sh                            [스크립트]
docs/beta/
  beta_access_guide.md                           [사용자 안내]
```

### `beta-users.yaml`

```yaml
realm: scheduling-system
users:
  - username: beta-planner-1
    email: planner1@company.com
    firstName: 김
    lastName: 생산계획
    enabled: true
    requiredActions: [UPDATE_PASSWORD]
    realmRoles: [PLANNER, USER]
    credentials:
      - type: password
        value: BetaInit2026!
        temporary: true

  - username: beta-planner-2
    email: planner2@company.com
    firstName: 이
    lastName: 생산계획
    enabled: true
    realmRoles: [PLANNER, USER]
    # ...

  - username: beta-stk-1
    email: stk1@company.com
    firstName: 박
    lastName: 현장
    enabled: true
    realmRoles: [STK_USER, USER]
    # ...

  - username: beta-stk-2
    email: stk2@company.com
    firstName: 최
    lastName: 현장
    enabled: true
    realmRoles: [STK_USER, USER]
    # ...
```

### `setup-beta-users.sh`

```bash
#!/bin/bash
# Keycloak admin CLI로 베타 사용자 일괄 생성
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8080}
REALM=scheduling-system

kcadm.sh config credentials --server $KEYCLOAK_URL --realm master \
    --user admin --password $KEYCLOAK_ADMIN_PASSWORD

# beta-users.yaml 읽어서 사용자 생성
yq '.users[]' infra/keycloak/beta-users.yaml | while read user; do
    kcadm.sh create users -r $REALM -f - <<< "$user"
done

echo "베타 4명 사용자 생성 완료"
```

### `docs/beta/beta_access_guide.md`

```markdown
# 베타 사용자 안내

## 접속 URL
- 시스템: https://scheduling.internal/
- 시뮬뷰 (현장 STK): https://scheduling.internal/simview
- 후보 비교 (Planner): https://scheduling.internal/candidate-comparison

## 첫 로그인 절차
1. 위 URL 접속
2. Keycloak 로그인 페이지 — 발급된 비밀번호 입력
3. 비밀번호 변경 (강제)
4. MFA 설정 (선택)

## 베타 기간 (1주)
- 시작: 2026-05-22 (월)
- 종료: 2026-05-28 (일)
- 병행 운영: 기존 Excel 워크플로우 + 본 시스템 양쪽 사용

## 피드백 채널
- Slack: #scheduling-beta
- Daily standup: 매일 09:00 (15분)
- 사후 설문: NS-02 (베타 종료 후)

## 알려진 한계
- 일부 충돌 (5% 정도) → 알림으로 통보 (정상 동작)
- Excel 양방향: import + export 모두 가능
```

---

## :test_tube: Acceptance Criteria

**검증**: A

- [ ] **베타 4명 Keycloak 계정** 생성 (yaml + script)
- [ ] **PLANNER role 2명**, **STK_USER role 2명**
- [ ] **첫 로그인 비밀번호 변경 강제**
- [ ] **베타 사용자 로그인 검증** — 시뮬뷰 / 후보 비교 접근
- [ ] **`beta_access_guide.md`** 문서 작성 + 사용자 배포

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] Keycloak users + 가이드
- [ ] 베타 4명 첫 로그인 성공
- [ ] 코드 리뷰 1명 이상 승인

---

## :construction: Dependencies

- **선행**: [TK-00-1-1](../../EP-00/ST-00-1/TK-00-1-1.md) (Keycloak)
- **후행**: [TK-E2E-2-2](TK-E2E-2-2.md), [TK-E2E-2-3](TK-E2E-2-3.md)
- **Critical Path**: ⭐ (Phase 3 진입)

---

## :memo: Implementation Notes

- 초기 비밀번호 BetaInit2026! — 첫 로그인 시 즉시 변경 강제 (REQ-FUNC-CO-007)
- yaml 정의 + script — IaC 패턴 (Phase 2+ Terraform 검토)
