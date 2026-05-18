// app — 메인 Spring Boot Application (모듈 합성 + bootJar 생성)
plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    // 7 도메인 모듈
    implementation(project(":common"))
    implementation(project(":order"))
    implementation(project(":vc"))
    implementation(project(":ex"))
    implementation(project(":master"))
    implementation(project(":audit"))
    implementation(project(":notify"))

    // 메인 entrypoint 의존성 (Web / Data / Security / Docs)
    implementation(libs.bundles.spring.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.springdoc.openapi)
    implementation(libs.caffeine)
}
