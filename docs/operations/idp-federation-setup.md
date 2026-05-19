# 사내 IdP 페더레이션 셋업 가이드 (TK-30-1-2)

본 문서는 Keycloak realm `scheduling-system` 과 **사내 IdP (AD FS / Microsoft Entra ID / Okta)** 페더레이션 설정 절차.

DEV 환경에는 placeholder 설정만 등록 (enabled=false). PROD 배포 전 사내 IT부서 협의 후 본 가이드 따라 활성화.

---

## 1. 사전 결정 사항 (Phase 0 IT부서 확인)

사내 IT 담당자와 협의:

| 항목 | 옵션 | 본 가이드 기준 |
|---|---|---|
| **IdP 종류** | AD FS · Entra ID (Azure AD) · Okta · OneLogin · 사내 LDAP | (확인 후 채움) |
| **프로토콜** | **SAML 2.0** 우선 / **OIDC** 보조 (Entra ID는 둘 다 가능) | SAML 권장 (사내망 표준) |
| **사내 AD 그룹 → Keycloak Role 매핑** | (예) `CN=Scheduling-Planner,OU=Groups,DC=company,DC=local` → `PLANNER` | role-mappers.json 참조 |
| **NameID 형식** | email / UPN / sAMAccountName | `email` 권장 (사내 표준) |
| **인증서 갱신 주기** | 1년 · 2년 | Slack 알림 (EP-44 ST-44-3) |

---

## 2. DEV 환경 — Placeholder 동작 검증

DEV 부팅 시 자동 import 되는 IdP 설정:
- `corporate-saml` (alias) — enabled=**false** (placeholder URL)
- `corporate-oidc` (alias) — enabled=**false** (placeholder URL)

검증:
```powershell
# admin token 발급
$body = @{ grant_type='password'; client_id='admin-cli'; username='admin'; password=$env:KEYCLOAK_ADMIN_PASSWORD }
$tok  = (Invoke-WebRequest -Uri 'http://localhost:8180/realms/master/protocol/openid-connect/token' -Method POST -Body $body -UseBasicParsing).Content | ConvertFrom-Json

# Identity Providers 조회
Invoke-WebRequest -Uri 'http://localhost:8180/admin/realms/scheduling-system/identity-provider/instances' `
  -Headers @{Authorization="Bearer $($tok.access_token)"} -UseBasicParsing |
  ForEach-Object { $_.Content | ConvertFrom-Json } |
  ForEach-Object { "  $($_.alias) enabled=$($_.enabled) providerId=$($_.providerId)" }
```

예상 출력:
```
  corporate-saml enabled=False providerId=saml
  corporate-oidc enabled=False providerId=oidc
```

---

## 3. PROD 활성화 (SAML — 예: AD FS)

### 3.1 사내 IT부서로부터 SAML metadata XML 수령
- URL: `https://idp.company.local/FederationMetadata/2007-06/FederationMetadata.xml`
- 또는 파일: `idp-saml-metadata.xml`

### 3.2 Keycloak Admin Console 에서 import
1. https://scheduling.internal/auth/admin → realm `scheduling-system` 선택
2. Identity Providers → Add provider → SAML v2.0
3. **Import from URL** 또는 **Import from file** — metadata XML 사용
4. alias `corporate-saml`, displayName `사내 SSO (SAML)` 설정
5. **NameID Policy Format**: `email`
6. **Principal Type**: Attribute, **Principal Attribute**: `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress`
7. **Sync Mode**: FORCE (로그인 시 마다 group·attribute 동기)
8. **Trust Email**: ON (이메일 verified 가정)
9. Save → **enabled** 체크

### 3.3 사내 IT부서에서 Keycloak SP metadata 등록 받기
- URL: `https://scheduling.internal/realms/scheduling-system/protocol/saml/descriptor`
- SP entityId: `https://scheduling.internal/realms/scheduling-system`
- ACS URL: `https://scheduling.internal/realms/scheduling-system/broker/corporate-saml/endpoint`

### 3.4 Role Mapper 등록
1. Identity Providers → corporate-saml → Mappers → Create
2. `infrastructure/keycloak/identity-providers/role-mappers.json` 참고
3. 4 role 모두 추가 (PLANNER·STK_USER·IT_OPS·READ_ONLY) — `memberOf` 속성 + AD group regex

---

## 4. PROD 활성화 (OIDC — 예: Entra ID)

### 4.1 Azure App Registration
1. Azure Portal → Microsoft Entra ID → App registrations → New registration
2. Name: `Scheduling-System-Keycloak`
3. **Redirect URI**: `https://scheduling.internal/realms/scheduling-system/broker/corporate-oidc/endpoint`
4. Certificates & secrets → New client secret → 24개월 유효
5. API permissions → Microsoft Graph → `openid` · `profile` · `email` · `groups` 추가 (Admin consent)
6. Token configuration → Add groups claim → Security groups

### 4.2 Keycloak Admin Console
1. Identity Providers → Add provider → OpenID Connect v1.0
2. alias `corporate-oidc`, displayName `사내 SSO (OIDC)`
3. **Discovery endpoint**: `https://login.microsoftonline.com/{TENANT_ID}/v2.0/.well-known/openid-configuration`
4. 자동 import 후 **Client ID**·**Client Secret** 채움
5. **Default Scopes**: `openid profile email groups`
6. **PKCE Method**: S256
7. Save → enabled 체크

### 4.3 Group → Role Mapper
1. Mappers → Create → Identity Provider Mapper
2. Type: **OpenID Connect Claim → Role**
3. Claim: `groups`
4. Claim Value: Azure security group object ID (예: `b3c4d5e6-...`)
5. Role: `PLANNER` 등

---

## 5. Local Fallback (TK-30-1-3 — 다음 Task)

사내 IdP 미응답 (timeout 5초 이상) 시 Keycloak 자체 user store 로 폴백.
설정: realm Login → **Disabled** → `corporate-saml` 우선, fallback Keycloak local.
구현: TK-30-1-3 (별도 Task).

---

## 6. 검증 시나리오

### 케이스 1 — SAML 정상 로그인
1. https://scheduling.internal → Keycloak 로그인 페이지
2. **"사내 SSO (SAML)"** 버튼 클릭
3. AD FS 로그인 → SAML response → Keycloak 자동 user provisioning
4. realm role (PLANNER 등) 자동 매핑
5. https://scheduling.internal SPA 진입

### 케이스 2 — OIDC 정상 로그인 (Entra ID)
1. **"사내 SSO (OIDC)"** 버튼 클릭
2. Microsoft 로그인 → group claim → role 매핑
3. (이하 동일)

### 케이스 3 — IdP 응답 없음 (timeout 5초)
1. corporate-saml `Single Sign-On URL` 차단
2. 로그인 시도 → 5초 timeout → fallback (TK-30-1-3) 트리거

### 케이스 4 — First login user provisioning
1. 신규 사용자 (AD 에는 있지만 Keycloak local 에 없음)
2. **First Login Flow** 활성: email·firstName·lastName·groups 자동 동기
3. Keycloak local `Users` 화면에서 자동 등록 확인

---

## 7. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| SAML response 거부 — InvalidSignature | IdP signing cert 만료/불일치 | Keycloak Admin → Identity Providers → corporate-saml → Signing Certificate 갱신 |
| OIDC token nonce mismatch | clock skew | Keycloak 와 IdP NTP 동기 확인 |
| 사용자 첫 로그인 후 role 없음 | role mapper 미설정 | role-mappers.json 참고하여 4 role 모두 추가 |
| `corporate-saml` 버튼 안 보임 | enabled=false | Admin Console → enabled 체크 후 save |
| AD group regex 미일치 | DN 형식 차이 | `memberOf` 실제 값 확인 (예: `CN=...,OU=Groups,DC=company,DC=local`) — Keycloak Admin Console → Sessions → User events 에서 raw claim 확인 |

---

## 8. 보안 체크리스트

- [ ] SAML response 서명 검증 활성 (`validateSignature=true`)
- [ ] SAML assertion 서명 요구 (`wantAssertionsSigned=true`)
- [ ] OIDC PKCE S256 활성
- [ ] Sync Mode FORCE (group 변경 즉시 반영)
- [ ] Client Secret 사내 vault 저장 (.env 노출 금지)
- [ ] IdP signing cert 만료 30일 전 Slack 알림 (EP-44 ST-44-3 — 별도 Task)
- [ ] Brute-force protection 활성 (Keycloak default — TK-30-1-1 설정됨)

---

## 9. Phase 0 IT부서 확인 필요 항목 (체크리스트)

- [ ] IdP 종류 (AD FS / Entra ID / Okta / 기타)
- [ ] SAML metadata XML URL 또는 파일
- [ ] AD 그룹 명명 규칙 (`CN=Scheduling-Planner,...`)
- [ ] 사내 Keycloak SP 등록 절차 (사내 IT 요청 양식)
- [ ] 인증서 갱신 알림 채널 (Slack webhook)
- [ ] PROD 환경 사내 도메인 (`scheduling.internal` 또는 다른)

---

## 부록: realm 자동 import vs Admin Console 수동

본 Task 의 `realm-scheduling-system.json` 에 identityProviders 섹션 포함 (enabled=false placeholder).

PROD 활성화는 **2가지 방법**:

**A. realm-export.json 갱신** (운영 가이드 일관성)
- Admin Console 에서 한 번 설정 → Export → infrastructure/keycloak/realm-*.json 갱신 → git commit
- 다음 부팅 시 자동 적용
- 장점: GitOps, 재현 가능
- 단점: realm export 시 사용자 데이터·세션도 포함 가능 (선별 export 필요)

**B. Admin Console 수동 설정** (단순)
- DEV 부팅 후 사내 IT 담당자가 직접 console 에서 설정
- 장점: 단순
- 단점: 재구축 시 재설정 필요

**권장**: A 방식 + 환경별 분리 (`.env.staging` / `.env.prod` 에서 client_secret 등 외부화).
