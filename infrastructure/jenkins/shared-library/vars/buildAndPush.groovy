// =============================================================================
// buildAndPush.groovy — Shared Library 공통 함수
// =============================================================================
// 사용 (Sprint 1+ Jenkinsfile 에서):
//   @Library('scheduling-shared-library') _
//   def imageTag = buildAndPush(service: 'backend', context: 'backend')
//
// 또는 stdPipeline 안에서 자동 호출.
//
// 호출 시 카운트:
//   1. docker build -t ${registry}/${service}:${tag} -t :latest ${context}
//   2. harbor 로그인 → push (${tag} + latest) → logout
//   3. imageTag (${registry}/${service}:${tag}) 반환
// =============================================================================

def call(Map config = [:]) {
    def registry  = config.registry  ?: 'harbor.internal/scheduling'
    def service   = config.service   ?: error('service 필수 — buildAndPush(service: "backend")')
    def context   = config.context   ?: '.'
    def credId    = config.credId    ?: 'harbor-credentials'
    def harborHost = registry.tokenize('/')[0]    // 예: harbor.internal

    def tag = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    def imageTag = "${registry}/${service}:${tag}"
    def latestTag = "${registry}/${service}:latest"

    echo "🐳 buildAndPush: ${imageTag}"

    // 1. docker build
    sh "docker build -t ${imageTag} -t ${latestTag} ${context}"

    // 2. harbor push (main/develop 만 의도된 경우 호출자에서 when 가드)
    withCredentials([usernamePassword(
        credentialsId: credId,
        usernameVariable: 'HARBOR_USER',
        passwordVariable: 'HARBOR_PASS',
    )]) {
        sh """
            echo "\$HARBOR_PASS" | docker login ${harborHost} -u "\$HARBOR_USER" --password-stdin
            docker push ${imageTag}
            docker push ${latestTag}
            docker logout ${harborHost}
        """
    }

    return imageTag
}
