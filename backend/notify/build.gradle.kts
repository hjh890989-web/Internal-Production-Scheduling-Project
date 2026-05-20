// notify — WebSocket / 알림 (BR-X06: MES 폴백)
plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(project(":order"))    // TK-03-3-1 OrderDiffPersistedEvent 구독

    implementation(libs.spring.boot.starter.web)          // TK-03-3-2 AckController
    implementation(libs.spring.boot.starter.data.jpa)     // TK-03-3-2 NotificationRepository
    implementation(libs.spring.boot.starter.security)     // TK-03-3-2 @PreAuthorize
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.validation)   // @ConfigurationProperties Validation
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)

    testImplementation(libs.spring.boot.starter.test)
}
