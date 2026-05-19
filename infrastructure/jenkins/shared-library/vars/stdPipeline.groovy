// =============================================================================
// stdPipeline.groovy — 표준 Pipeline Shared Library entry point
// =============================================================================
// Sprint 1+ Jenkinsfile 에서 사용:
//   @Library('scheduling-shared-library') _
//   stdPipeline {
//       language = 'java'   // 또는 'typescript'
//       harborRepo = 'scheduling-backend'
//   }
//
// 본 골격은 Sprint 1+ 실 step 추가 (TK-32-1-2 + ST-32-2) 후 본격 활용.
// =============================================================================

def call(Map config) {
    pipeline {
        agent any

        options {
            timeout(time: 30, unit: 'MINUTES')
            timestamps()
            ansiColor('xterm')
            buildDiscarder(logRotator(numToKeepStr: '50'))
        }

        environment {
            APP_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
        }

        stages {
            stage('Build') {
                steps {
                    script {
                        if (config.language == 'java') {
                            sh './gradlew clean build --no-daemon'
                        } else if (config.language == 'typescript') {
                            sh 'npm ci && npm run build'
                        } else {
                            error("config.language 미지정 — 'java' 또는 'typescript' 필요")
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {
                        if (config.language == 'java') {
                            sh './gradlew test --no-daemon'
                            junit '**/build/test-results/test/*.xml'
                        } else if (config.language == 'typescript') {
                            sh 'npm run test:run'
                        }
                    }
                }
            }

            // Sprint 1+ 단계들 — TK-32-1-2 / ST-32-2 에서 채움
        }

        post {
            always {
                cleanWs()
            }
        }
    }
}
