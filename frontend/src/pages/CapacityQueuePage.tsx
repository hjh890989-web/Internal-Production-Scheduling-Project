import { Space, Tabs, Typography } from 'antd'
import { CapacityOverflowSplitPanel } from '@/features/capacity-overflow/components/CapacityOverflowSplitPanel'
import { KdSupplementPanel } from '@/features/capacity-overflow/components/KdSupplementPanel'
import { PendingQueuePanel } from '@/features/capacity-overflow/components/PendingQueuePanel'

const { Title, Paragraph } = Typography

/**
 * Sprint 7 BR-V12·V13 — Planner capa 초과 큐 + KD 잔량 보충 통합 화면.
 *
 * <p>{@code /vc/capacity-queue} 라우트, RBAC PLANNER 전용.
 * <p>탭 분리 — V12 (capa split 미리보기) · V13 (1클릭 KD 보충).
 */
export default function CapacityQueuePage() {
  return (
    <Space direction="vertical" size="middle" style={{ width: '100%', padding: 16 }}>
      <Title level={3}>Capa 초과 큐 + KD 보충 (BR-V12 · BR-V13)</Title>
      <Paragraph type="secondary">
        DI-07/08 마스터 입력 후 활성. V12 — 우선순위 split 미리보기 + 큐 등록 + Planner 승인/거절 (Sprint 8) · V13 — 1클릭 잔량 보충.
      </Paragraph>
      <Tabs
        defaultActiveKey="split"
        items={[
          {
            key: 'split',
            label: 'BR-V12 우선순위 Split',
            children: <CapacityOverflowSplitPanel />,
          },
          {
            key: 'supplement',
            label: 'BR-V13 KD 잔량 보충',
            children: <KdSupplementPanel />,
          },
          {
            key: 'pending',
            label: 'Pending Queue (Sprint 8)',
            children: <PendingQueuePanel />,
          },
        ]}
      />
    </Space>
  )
}
