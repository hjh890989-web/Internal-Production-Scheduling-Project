// order — 수주 정보 통합 (PDD-01)
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.web)            // TK-01-1-3 @RestController
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)     // TK-01-1-3 ImportTrackingService
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    // spring-modulith-starter-jpa (EventPublicationRegistry) 는 Sprint 1+ Task에서 활성
    implementation(libs.apache.poi)
    implementation(libs.excel.streaming.reader)
    // SnakeYAML 은 Spring Boot 기본 의존성 (별도 명시 불필요)
}
