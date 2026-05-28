// app — 메인 Spring Boot Application (모듈 합성 + bootJar 생성)
plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    // 7 도메인 모듈
    implementation(project(":common"))
    implementation(project(":order"))
    implementation(project(":vc"))
    implementation(project(":ex"))
    implementation(project(":master"))
    implementation(project(":audit"))
    implementation(project(":notify"))

    // 메인 entrypoint 의존성 (Web / Data / Security / Docs)
    implementation(libs.bundles.spring.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)   // TK-30-2-1 JWT
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.springdoc.openapi)
    implementation(libs.caffeine)

    // Modulith 모듈 경계 verify + docs 생성 (TK-00-2-2)
    testImplementation(libs.spring.modulith.test)
    testImplementation("org.springframework.modulith:spring-modulith-docs")

    // ArchUnit 빌드 타임 강제 (TK-00-2-3) — Sprint 0 DoD 항목 2 달성
    testImplementation(libs.bundles.test.arch)
    // Spring Security 6 테스트 헬퍼 (@WithMockUser 등) — TK-30-2-1·2
    testImplementation(libs.spring.security.test)
    // TK-04-2-3 UnschedulableFilterIT — POI XSSF 재읽기 검증
    testImplementation(libs.apache.poi)
}

// Sprint 20 hotfix — bootRun 시 with-infra profile 자동 활성.
// default profile 은 JPA/Flyway auto-config excluded (Sprint 0 baseline) 라 AppUserRepository
// bean 미생성 → AppUserDetailsService 부팅 실패. 환경변수 SPRING_PROFILES_ACTIVE 의존 제거.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "with-infra")
}
