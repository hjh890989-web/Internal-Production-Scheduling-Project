// master — 마스터 데이터 (제약·우선순위·KD)
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    // spring-modulith-starter-jpa 는 Sprint 1+ Task에서 활성
    implementation(libs.apache.poi)
}
