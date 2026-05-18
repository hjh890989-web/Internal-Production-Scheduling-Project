// audit — 감사 (BR-X02). schema 'audit' 별도, INSERT-only role
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.modulith.starter.core)
    // spring-modulith-starter-jpa + events-jpa 는 Sprint 1+ Task에서 활성
}
