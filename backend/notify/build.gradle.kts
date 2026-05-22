// notify — WebSocket / 알림 (BR-X06: MES 폴백)
plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(project(":order"))    // TK-03-3-1 OrderDiffPersistedEvent 구독
    implementation(project(":ex"))       // TK-EX14-1-2 ExReplanCompletedEvent 구독

    implementation(libs.spring.boot.starter.web)          // TK-03-3-2 AckController
    implementation(libs.spring.boot.starter.data.jpa)     // TK-03-3-2 NotificationRepository
    implementation(libs.spring.boot.starter.security)     // TK-03-3-2 @PreAuthorize
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.validation)   // @ConfigurationProperties Validation
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)
    implementation(libs.spring.modulith.starter.jpa)      // TK-41-2-1 EventPublicationRegistry 활성
    implementation(libs.spring.modulith.events.jpa)
    implementation("org.springframework.boot:spring-boot-starter-aop")    // TK-41-1-1 Resilience4j @Retry AspectJ
    implementation(libs.resilience4j.spring.boot3)        // TK-41-1-1 @Retry + @CircuitBreaker

    testImplementation(libs.spring.boot.starter.test)
}
