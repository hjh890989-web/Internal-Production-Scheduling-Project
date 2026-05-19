// =============================================================================
// notifySlack.groovy — TK-32-2-3 Jenkins → Slack 알림 공통 함수
// =============================================================================
// REQ-NF-OPS-003: "시스템 에러·알림 발송 실패는 60초 이내 Slack 알림" 직접 충족.
//
// 사용 패턴:
//   notifySlack(channel: '#scheduling-builds', status: 'SUCCESS')
//   notifySlack(channel: '#scheduling-security', status: 'FAILURE', extra: 'Trivy 5건')
//
// 채널 정책: docs/operations/slack_channels.md 참조.
// =============================================================================

def call(Map config) {
    def channel = config.channel ?: '#scheduling-builds'
    def status  = config.status  ?: currentBuild.currentResult  // SUCCESS·FAILURE·UNSTABLE·ABORTED
    def extra   = config.extra   ?: ''

    def colorMap = [
        'SUCCESS':  'good',          // 녹색
        'UNSTABLE': 'warning',       // 노랑
        'FAILURE':  'danger',        // 빨강
        'ABORTED':  '#cccccc',       // 회색
    ]
    def emojiMap = [
        'SUCCESS':  ':white_check_mark:',
        'UNSTABLE': ':warning:',
        'FAILURE':  ':x:',
        'ABORTED':  ':octagonal_sign:',
    ]

    def color = colorMap[status] ?: '#cccccc'
    def emoji = emojiMap[status] ?: ':grey_question:'
    def author = env.GIT_AUTHOR ?: 'unknown'
    def commitMsg = (env.GIT_MESSAGE ?: '').take(80)
    def jobName = env.JOB_NAME ?: 'unknown'
    def buildNum = env.BUILD_NUMBER ?: '?'
    def buildUrl = env.BUILD_URL ?: '#'
    def branch = env.BRANCH_NAME ?: (env.GIT_BRANCH ?: 'unknown')
    def duration = currentBuild.durationString?.replace(' and counting', '') ?: '?'

    def message = """\
${emoji} *${jobName}* #${buildNum} — ${status}
• 브랜치: `${branch}`
• 커밋: ${author} — ${commitMsg}
• 소요: ${duration}
• <${buildUrl}|빌드 결과 보기>${extra ? '\n• ' + extra : ''}"""

    // Jenkins slack plugin 의 slackSend — 재시도 3회 내장.
    // tokenCredentialId 는 CASC unclassified.slackNotifier 에서 'slack-bot-token' 지정.
    slackSend(channel: channel, color: color, message: message)
}
