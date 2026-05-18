# =============================================================================
# Internal Production Scheduling — Backend Dockerfile (multi-stage)
# =============================================================================
# Builder: JDK 21 (Eclipse Temurin) + Gradle wrapper → bootJar
# Runtime: JRE 21 alpine → 약 200MB (NFR-COS-001)
# =============================================================================

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
# Wrapper 권한 보장 (Windows host에서 복사 시 exec bit 손실 가능)
RUN chmod +x ./gradlew
RUN ./gradlew :app:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=30s \
    CMD wget --spider --quiet http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
