// notify — WebSocket / 알림 (BR-X06: MES 폴백)
plugins {
    java
}

dependencies {
    implementation(project(":common"))

    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)
}
