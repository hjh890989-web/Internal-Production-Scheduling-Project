import { Card, Space, Tag, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { useHealth } from '@/api/hooks/useHealth'
import { useAuthStore } from '@/stores/authStore'

const { Paragraph } = Typography

/**
 * 표본 홈 페이지 — Sprint 1+ 실제 dashboard 로 대체.
 * 현재 골격: i18n + Zustand + TanStack Query 통합 동작 시연.
 */
export default function HomePage() {
  const { t } = useTranslation()
  const { data, isLoading, error } = useHealth()
  const user = useAuthStore((s) => s.user)

  return (
    <Space direction="vertical" size="middle" style={{ display: 'flex' }}>
      <Card title={t('app.title')}>
        <Paragraph>Phase 2/4.Tasks ST-00-3 골격 — Sprint 1+에서 본 화면 대체.</Paragraph>
        {user && (
          <Paragraph>
            로그인: {user.name} ({user.role})
          </Paragraph>
        )}
      </Card>
      <Card title="Backend Health (Actuator)">
        {isLoading && <Tag>{t('common.loading')}</Tag>}
        {error && <Tag color="error">{t('common.error')}</Tag>}
        {data && (
          <Tag color={data.status === 'UP' ? 'success' : 'error'}>{data.status}</Tag>
        )}
      </Card>
    </Space>
  )
}
