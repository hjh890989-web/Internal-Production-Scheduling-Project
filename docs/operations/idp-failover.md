# 사내 IdP 장애 시 Fallback 절차 (TK-30-1-3)

본 문서는 사내 IdP (AD FS / Microsoft Entra ID / Okta) 미응답 시 Keycloak local user 로 fallback 하는 절차.
TK-30-1-2 (IdP 페더레이션) 의 후행. SAML/OIDC 미응답 → Keycloak local 로그인.

---

## 1. Trigger 조건

다음 중 하나라도 해당하면 fallback 활성화:

| 조건 | 감지 |
|---|---|
| 사내 IdP 미응답 (TCP timeout 5초) | NGINX upstream timeout / Keycloak broker error |
| SAML SSO redirect URL 응답 없음 | Keycloak `org.keycloak.broker.saml.SAMLEndpoint` exception 로그 |
| OIDC discovery endpoint 503/502 | Keycloak `org.keycloak.broker.oidc.OIDCIdentityProvider` exception |
| 사용자가 IT 부서에 로그인 불가 보고 | Slack #it-incident 채널 |

---

## 2. Fallback 흐름

### 정상 흐름 (사내 IdP 정상 시)
```
사용자 → https://scheduling.internal → Keycloak 로그인 페이지
       → "사내 SSO (SAML)" 버튼 클릭 → AD FS 로그인 → JWT 발급 → SPA 진입
```

### Fallback 흐름 (IdP 장애)
```
사용자 → https://scheduling.internal → Keycloak 로그인 페이지
       → "사내 SSO" 버튼 클릭 → 5초 timeout
       → Keycloak 자동으로 ID/PW 입력 폼 표시 (Username Password Form — ALTERNATIVE)
       → emergency 계정 (또는 평소 Local 계정) ID/PW 입력 → 로그인
       → 첫 로그인 시 비밀번호 변경 강제 (temporary=true)
       → JWT 발급 → SPA 진입
```

Keycloak 의 기본 `browser` flow 가 이미 `Identity Provider Redirector` (ALTERNATIVE) + `Username Password Form` (ALTERNATIVE) 구조 — 별도 설정 없이도 IdP 없는 사용자는 자동 ID/PW.

---

## 3. Emergency 계정 (사전 생성됨)

DEV/PROD realm `scheduling-system` 에 자동 import (`realm-scheduling-system.json` § users):

| Username | Role | 용도 |
|---|---|---|
| `emergency-planner-1` | PLANNER | 생산계획팀 1차 대응 |
| `emergency-planner-2` | PLANNER | 생산계획팀 2차 대응 |
| `emergency-itops` | IT_OPS | IT 운영 직접 복구 |

**초기 비밀번호** (DEV — `realm-scheduling-system.json` 명시):
- `Emergency_Plan1_#2026`
- `Emergency_Plan2_#2026`
- `Emergency_ITOps_#2026`

⚠ **PROD 환경 운영 규칙**:
- 초기 비밀번호 = **봉인 봉투** 에 인쇄해 IT 부서 금고 보관
- 사용 시점에 **IT lead 결재** 후 봉투 개봉
- 사용 직후 **즉시 비밀번호 변경** + audit 로그 기록
- 분기별 1회 fallback 드릴 (Q1·Q2·Q3·Q4 — IT lead + DevOps + PM)

---

## 4. PROD 배포 시 emergency 계정 비밀번호 갱신

```powershell
# 1. admin token (master realm)
$tok = ...

# 2. emergency-planner-1 비밀번호 갱신 (새 봉인 봉투용)
$NEW_PWD = "Q2_2026_Planner1_$(Get-Random -Maximum 99999)#"
$body = @{
    type = "password"; value = $NEW_PWD; temporary = $true
} | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8180/admin/realms/scheduling-system/users/$USER_ID/reset-password" `
  -Method PUT -Headers @{Authorization="Bearer $tok"} -Body $body -ContentType "application/json"

# 3. 새 비밀번호를 봉인 봉투 인쇄 → IT 금고 보관
# 4. 기존 봉투 폐기 (감독 하)
```

---

## 5. 드릴 절차 (분기 1회)

1. **사전 공지**: 드릴 시간 IT 부서 + DevOps + PM 에게 공유 (실 사용자 영향 회피)
2. **사내 IdP 응답 차단**: 사내 firewall 일시 차단 또는 Keycloak admin 에서 corporate-saml.enabled=false
3. **로그인 검증**:
   - emergency-planner-1 로 로그인 → PLANNER role 토큰 발급 확인
   - emergency-itops 로 로그인 → IT_OPS role 토큰 발급 확인
4. **복구**: IdP 차단 해제 + Keycloak corporate-saml.enabled=true
5. **결과 기록**: 결과 + 발견 이슈 회의록 + Slack #operations 공유

---

## 6. 보안 사건 감지

emergency 계정 로그인 시 자동 알림 (Sprint 1+ — EP-44 ST-44-3 Slack webhook):

```
Keycloak event: USER_LOGIN  user=emergency-planner-1  realm=scheduling-system
  → Slack #security-alerts 채널 자동 알림
  → "Emergency 계정 사용 — IT lead 결재 확인 필요"
```

설정 (Sprint 1+ Task):
- Keycloak Events → Listeners → `event-listener-spi` 활성
- Spring Boot audit 모듈 (EP-11) 이 Keycloak event 구독 + Slack webhook 호출

---

## 7. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| Fallback 화면 안 보임 (IdP 만 표시) | identity-provider-redirector requirement=REQUIRED | Admin Console → Authentication → browser flow → identity-provider-redirector → ALTERNATIVE 변경 |
| emergency 계정 로그인 거부 (Account is disabled) | enabled=false 또는 lockout | Admin Console → Users → emergency-planner-1 → Enable + Reset failures |
| 비밀번호 변경 강제 안 됨 | credentials.temporary=false | Reset password 시 Temporary 체크 |
| 사내 IdP 복구 후에도 fallback 화면 표시 | Keycloak corporate-saml.enabled=false 상태 | Admin Console → Identity Providers → Enable |
| brute-force lockout 5회 후 잠금 | 실패 5회 (TK-30-1-1 정책) | 60초 대기 또는 Admin Console → Sessions → Reset user |

---

## 8. 운영 환경 차이

| 항목 | DEV | PROD |
|---|---|---|
| emergency 비밀번호 | `.env` + realm-export 평문 | 봉인 봉투 + IT 금고 보관 |
| Fallback 발동 알림 | 로그만 | Slack #security-alerts 자동 (Sprint 1+) |
| 드릴 주기 | 임의 | 분기 1회 의무 |
| 초기 비밀번호 temporary | true | true |
| 패스워드 정책 | 12자+ 3종 (NFR-SEC-007) | 동일 |

---

## 9. 검증 (DEV)

```powershell
# 1. emergency-planner-1 로그인 시도 (admin token 발급)
$body = @{
    grant_type = 'password'; client_id = 'admin-cli'
    username = 'emergency-planner-1'
    password = 'Emergency_Plan1_#2026'
} 
$r = Invoke-WebRequest -Uri 'http://localhost:8180/realms/scheduling-system/protocol/openid-connect/token' `
     -Method POST -Body $body -UseBasicParsing
# 응답: 200 + access_token (단 temporary=true 라 첫 로그인 시 비밀번호 변경 요구)
```

DEV 검증 시 `Account is not fully set up` 응답 받을 수 있음 — `temporary: true` 첫 로그인 시 변경 강제. 정상 동작.

---

## 10. PROD sign-off 체크리스트

- [ ] emergency 계정 3개 사전 생성 + temporary 비밀번호 봉인
- [ ] Authentication Flow `browser` — identity-provider-redirector ALTERNATIVE 확인
- [ ] IdP 차단 시뮬레이션 → fallback ID/PW 화면 자동 표시 확인
- [ ] emergency-planner-1 로 로그인 → PLANNER role 토큰 정상 발급
- [ ] 비밀번호 변경 후 정상 로그인 + audit 로그 기록
- [ ] IT 부서 sign-off (장애 대응 절차 검토)
- [ ] 분기 드릴 일정 등록
