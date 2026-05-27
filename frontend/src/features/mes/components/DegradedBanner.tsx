import { Alert, Button, Space, Tag } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { ExclamationCircleOutlined, UploadOutlined } from '@ant-design/icons'
import { fetchDegradedStatus } from '../api/mesApi'
import { useAuthStore } from '@/stores/authStore'

interface Props {
  /** Excel 폴백 Modal 트리거 — PLANNER/IT_OPS 만 노출. */
  onOpenFallback: () => void
}

/**
 * Sprint 17 EP-DAY-LOCK BR-X06 — VC 시뮬뷰 상단 degraded mode 배너 (TK-DAY-LOCK-4-2).
 *
 * <p>{@code /api/v1/mes/degraded/status} 30초 polling — anyDegraded=true 시 빨강 Alert
 * + "Excel 폴백 입력" 버튼 (PLANNER/IT_OPS). NORMAL 상태에서는 미렌더 (UI noise 차단).
 */
export function DegradedBanner({ onOpenFallback }: Props) {
  const isPlannerOrItOps = useAuthStore(
    (s) => s.hasRole('PLANNER') || s.hasRole('IT_OPS'),
  )

  const query = useQuery({
    queryKey: ['mes-degraded-status'],
    queryFn: fetchDegradedStatus,
    refetchInterval: 30_000,
    staleTime: 25_000,
  })

  if (query.isLoading || !query.data) return null
  if (!query.data.anyDegraded) return null

  const degradedMachines = query.data.machines.filter((m) => m.degraded)

  return (
    <Alert
      type="error"
      showIcon
      icon={<ExclamationCircleOutlined />}
      message={
        <Space>
          <span>BR-X06 MES 폴백 모드</span>
          <Tag color="red">{query.data.summary}</Tag>
        </Space>
      }
      description={
        <Space direction="vertical" size={4} style={{ width: '100%' }}>
          <span>
            {degradedMachines.length}개 가류기 MES 미수신 — 직전 계획값 임시 사용 중 (REQ-FUNC-CO-004).
          </span>
          <Space wrap>
            {degradedMachines.map((m) => (
              <Tag key={m.machineId} color="volcano">
                {m.machineId}
                {m.lastReceivedAt ? ` · 마지막 ${m.lastReceivedAt.substring(0, 16).replace('T', ' ')}` : ' · 0건'}
              </Tag>
            ))}
          </Space>
          {isPlannerOrItOps && (
            <Button
              size="small"
              type="primary"
              danger
              icon={<UploadOutlined />}
              onClick={onOpenFallback}
              data-testid="degraded-banner-fallback-trigger"
            >
              Excel 폴백 입력
            </Button>
          )}
        </Space>
      }
      data-testid="degraded-banner"
    />
  )
}
