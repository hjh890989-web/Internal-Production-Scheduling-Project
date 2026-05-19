// common — 공통 코드 (BR·BrCode·ProblemDetail).
// 다른 도메인 모듈에 의존 금지 (TK-00-2-3 ArchUnit 검증).
plugins {
    java
}

dependencies {
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    // Micrometer facade — TK-31-1-1: 도메인 모듈이 custom metric 등록 시 사용
    implementation("io.micrometer:micrometer-core")
}
