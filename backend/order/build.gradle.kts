// order — 수주 정보 통합 (PDD-01)
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    // spring-modulith-starter-jpa (EventPublicationRegistry) 는 Sprint 1+ Task에서 활성
    implementation(libs.apache.poi)
    implementation(libs.excel.streaming.reader)
}
