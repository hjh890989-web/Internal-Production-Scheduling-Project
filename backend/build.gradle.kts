// =============================================================================
// 루트 build.gradle.kts
// =============================================================================
// 책임:
//   - allprojects: group·version·repository
//   - subprojects: Java toolchain 21, dependency-management BOM, 컴파일 옵션,
//                  Test 공통 설정, 모든 subproject에 공통 의존성 (SLF4J + test BOM)
//
// 본 Task는 빌드 시스템·의존성 관리만 담당. 모듈 경계 정의(NamedInterface)
// 는 TK-00-2-2, ArchUnit 강제는 TK-00-2-3.
// =============================================================================

plugins {
    java
    alias(libs.plugins.spring.deps) apply false
    alias(libs.plugins.spring.boot) apply false
}

// subprojects { } 컨텍스트는 type-safe `libs` accessor에 접근 못함 →
// 최상위에서 값 추출 후 클로저에서 참조.
val javaVersion              = libs.versions.java.get().toInt()
val springBootVersion        = libs.versions.spring.boot.get()
val springModulithVersion    = libs.versions.spring.modulith.get()
val testCoreBundle           = libs.bundles.test.core

allprojects {
    group = "com.scheduling"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(javaVersion)
            vendor = JvmVendorSpec.ADOPTIUM
        }
    }

    // dependency-management BOM imports — Spring Boot + Modulith 버전 일괄 관리
    extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("org.springframework.modulith:spring-modulith-bom:$springModulithVersion")
        }
    }

    dependencies {
        // 모든 subproject 공통
        "implementation"("org.slf4j:slf4j-api")
        "testImplementation"(testCoreBundle)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        // KST 통일 (BR-X04)
        systemProperty("user.timezone", "Asia/Seoul")
    }
}
