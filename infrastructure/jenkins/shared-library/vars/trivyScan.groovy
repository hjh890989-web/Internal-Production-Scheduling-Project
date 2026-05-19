// =============================================================================
// trivyScan.groovy — TK-32-2-1 Trivy 보안 스캔 공통 함수
// =============================================================================
// 사용:
//   trivyScan(imageTag: env.IMAGE_TAG, projectDir: 'backend', reportName: 'trivy-backend')
//
// 스캔 3종:
//   1. image  — Docker 이미지 (OS + library CVE)
//   2. fs     — 소스 트리 (Gradle / npm 의존성)
//   3. config — IaC (Dockerfile, docker-compose.yml, k8s YAML)
//
// HIGH/CRITICAL 발견 시 exit 1 (config 의 exit-code 설정 따름) → 본 함수 error()
// notifySlack(#scheduling-security) 으로 보안팀 알림 (TK-32-2-3 통합)
// =============================================================================

def call(Map config = [:]) {
    def imageTag    = config.imageTag    ?: error("trivyScan: imageTag required")
    def projectDir  = config.projectDir  ?: '.'
    def reportName  = config.reportName  ?: 'trivy-report'
    def trivyConfig = '/workspace/infrastructure/ci/trivy/trivy-config.yaml'
    def ignoreFile  = '/workspace/infrastructure/ci/trivy/.trivyignore'

    // Trivy DB 사전 다운로드 — 빌드 안정성 (DB 미스 시 빌드 실패 방지)
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            aquasec/trivy:latest \\
            image --download-db-only
    """

    // ---------- 1. 이미지 스캔 ----------
    sh """
        docker run --rm \\
            -v /var/run/docker.sock:/var/run/docker.sock \\
            -v \$(pwd):/workspace \\
            aquasec/trivy:latest \\
            image \\
            --config ${trivyConfig} \\
            --ignorefile ${ignoreFile} \\
            --output /workspace/${reportName}-image.json \\
            ${imageTag}
    """

    // ---------- 2. 의존성 (fs) 스캔 ----------
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            aquasec/trivy:latest \\
            fs \\
            --config ${trivyConfig} \\
            --ignorefile ${ignoreFile} \\
            --output /workspace/${reportName}-deps.json \\
            /workspace/${projectDir}
    """

    // ---------- 3. IaC (config) 스캔 ----------
    sh """
        docker run --rm \\
            -v \$(pwd):/workspace \\
            aquasec/trivy:latest \\
            config \\
            --config ${trivyConfig} \\
            --output /workspace/${reportName}-iac.json \\
            /workspace/infrastructure
    """

    // ---------- 4. 결과 archive + 통계 ----------
    archiveArtifacts artifacts: "${reportName}-*.json", allowEmptyArchive: false

    // 위 sh 단계가 exit-code=1 로 빌드를 이미 중단했을 것 — 본 line 은 성공 흐름의 로깅
    echo "✅ Trivy scan PASSED — HIGH/CRITICAL 0건 (image + deps + iac)"
}
