// vc — 성형 가류 스케줄링 (PDD-02)
plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(project(":order"))    // OrderChangedEvent 구독 (REQ-FUNC-VC-015)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)
    // spring-modulith-starter-jpa 는 Sprint 1+ Task에서 활성
}
