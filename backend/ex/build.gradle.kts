// ex — 압출 스케줄링 (PDD-03)
plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(project(":master"))   // TK-07-2-1 master.api.WorkingCalendar
    implementation(project(":vc"))       // TK-07-1-1 vc.events.VcConfirmedEvent
    implementation(project(":audit"))    // TK-11-1-2 @Auditable AOP

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)        // TK-10-2-1 ExConfirmController
    implementation(libs.spring.boot.starter.security)   // TK-10-2-1 @PreAuthorize
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)
    // spring-modulith-starter-jpa 는 Sprint 1+ Task에서 활성

    testImplementation(libs.spring.boot.starter.test)
}
