# Slack 채널 정책 — Phase 1.0 운영 (TK-32-2-3 + EP-31)

본 문서는 사내 공정 스케줄링 시스템의 Slack 알림 채널 운영 정책.
REQ-NF-OPS-003 (60초 이내) + REQ-NF-OPS-004 (에스컬레이션) 정합.

---

## 1. 채널 목록

| 채널 | 용도 | 알림 출처 | 필수 구독자 | 빈도 |
|---|---|---|---|---|
| `#scheduling-builds` | 모든 CI 빌드 시작·성공·실패 | Jenkins (`notifySlack`) | 백엔드 리드, 프론트 리드, DevOps | 빌드당 1건 |
| `#scheduling-security` | Trivy CVE + SonarQube Quality Gate 위반 | Jenkins (`trivyScan` 실패, Quality Gate 실패) | 보안 담당자, DevOps, 백엔드 리드 | 위반 시 |
| `#scheduling-deploys` | PROD Blue-Green 배포 시작·완료·롤백 | Jenkins Deploy 단계 | STK-01 공장장, STK-08 IT lead, 백엔드 리드 | 배포당 1건 |
| `#scheduling-alerts` | Grafana 운영 알림 (Critical) | AlertManager → Slack webhook | 공장장 (에스컬), IT lead, DevOps | 인시던트 시 |
| `#scheduling-warnings` | Grafana Warning 알림 (영업시간) | AlertManager → Slack webhook | DevOps, 백엔드 리드 | 일일 빈도 |
| `#scheduling-backup` | 일일 PG 백업 성공·실패 | pg-backup.sh → Slack curl | DevOps, IT lead | 일 1건 |

---

## 2. SLA (REQ-NF-OPS-003·004)

| 알림 유형 | 도달 SLA | 응답 SLA | 비고 |
|---|---|---|---|
| Critical (DOWN, 보안 위반) | 60초 이내 | 15분 이내 | AlertManager group_wait=0s |
| Warning (성능 저하) | 90초 이내 | 영업시간 4h | AlertManager group_wait=30s |
| Build/Deploy | 30초 이내 | 비공식 | Jenkins slack plugin retry 3회 |
| 일일 백업 | 24h 이내 (배치 완료 시점) | 영업일 시작 | pg-backup.sh 후속 curl |

---

## 3. 인증·관리

### Bot 등록 (사내 Slack 관리자 협조)
1. workspace.slack.com → Apps → Create Custom App
2. Scopes: `chat:write`, `chat:write.public`, `users:read`
3. Bot Token 발급 (`xoxb-...`) — Jenkins credentials 등록 (`slack-bot-token`)
4. 각 채널 초대: `/invite @scheduling-bot`

### Token Rotation (NFR-SEC-007)
- 분기 1회 (1월·4월·7월·10월) Bot Token 재발급
- 사내 vault 또는 Jenkins CASC `${SLACK_BOT_TOKEN}` 갱신
- 갱신 후 첫 알림 도달 확인 → 사고 시 IT lead 즉시 호출

---

## 4. 채널 구독자 변동 시

- 신규 입사·전출 시 본 문서 + 사내 Slack 그룹 갱신
- 분기 1회 (1·4·7·10월 1일) 정기 audit — 미사용 구독자 정리
- 이 문서 갱신 PR 은 IT lead + 백엔드 리드 dual-review (BR-X05 정신 적용)

---

## 5. 사고 대응

### 5.1 알림 미도달 (Slack 자체 장애)
1. Jenkins console log 의 `slackSend` 실패 메시지 확인
2. Slack status.slack.com 점검
3. 5분 이상 장애 시 대체 채널:
   - **PROD 알림**: 사내 이메일 (Sprint 1+ ST-44-3 보조 채널 추가)
   - **인시던트**: IT lead 휴대전화 직접 호출 (장비 다운 시)

### 5.2 알림 폭주 (한 시점 100건 이상)
1. AlertManager `group_interval` 조정 (현재 1분 → 5분)
2. `inhibit_rules` 추가로 cascade 알람 억제
3. 사후 보고서 작성 (Slack `#scheduling-alerts` thread)
