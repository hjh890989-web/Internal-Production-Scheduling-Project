// =============================================================================
// settings.gradle.kts — Spring Modulith 멀티모듈 구성
// =============================================================================
// 7 도메인 모듈 (SAD §4 컴포넌트 뷰):
//   - common  : 공통 (BR·BrCode·ProblemDetail). 다른 도메인 모듈에 의존 금지 (ArchUnit 검증)
//   - order   : 수주 정보 통합 (PDD-01)
//   - vc      : 성형 가류 스케줄링 (PDD-02)
//   - ex      : 압출 스케줄링 (PDD-03)
//   - master  : 마스터 데이터 (제약·우선순위·KD)
//   - audit   : 감사 (BR-X02)
//   - notify  : WebSocket / 알림
// + app       : 메인 Spring Boot Application (모듈 합성)
// =============================================================================

rootProject.name = "scheduling"

include(
    ":common",
    ":order",
    ":vc",
    ":ex",
    ":master",
    ":audit",
    ":notify",
    ":app",
)
