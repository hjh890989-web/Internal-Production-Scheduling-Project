// audit — 감사 (BR-X02). schema 'audit' 별도, INSERT-only role
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation("org.springframework.boot:spring-boot-starter-aop")    // TK-11-1-2 @Auditable AspectJ
    implementation(libs.spring.boot.starter.security)    // TK-11-1-2 SecurityContext actor 조회
    implementation(libs.spring.boot.starter.web)         // TK-19-1-1 AuditSnapshotController
    implementation(libs.spring.modulith.starter.core)
}
