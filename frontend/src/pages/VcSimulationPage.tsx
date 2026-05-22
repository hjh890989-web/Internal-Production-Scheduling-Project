import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Card, DatePicker, Divider, Space, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { VcRotationGrid } from '@/features/vc-scheduling/components/VcRotationGrid'
import { SwapProposalPanel } from '@/features/vc-scheduling/components/SwapProposalPanel'
import { fetchVcSlots, type VcSlotRow } from '@/features/vc-scheduling/api/vcScheduleApi'

const { Title } = Typography
const { RangePicker } = DatePicker

/**
 * EP-15 VcSimulationPage — STK_USER 시뮬뷰 + 회전 격자 (BR-V04 1~18 회전).
 *
 * <p>{@code /vc/simview} 라우트, RBAC STK_USER + PLANNER + IT_OPS + READ_ONLY.
 */
export default function VcSimulationPage() {
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs(),
    dayjs().add(7, 'day'),
  ])
  const [from, to] = range
  const fromStr = from.format('YYYY-MM-DD')
  const toStr = to.format('YYYY-MM-DD')

  const query = useQuery<VcSlotRow[]>({
    queryKey: ['vc-slots', fromStr, toStr],
    queryFn: () => fetchVcSlots(fromStr, toStr),
    staleTime: 30_000,
  })

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%', padding: 16 }}>
      <Title level={3}>성형 시뮬뷰 (EP-15)</Title>
      <Space>
        <RangePicker
          value={range}
          onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
          allowClear={false}
        />
      </Space>

      {query.error ? (
        <Alert type="error" message="시뮬뷰 조회 실패" description={String(query.error)} />
      ) : null}

      <Card bodyStyle={{ padding: 0 }}>
        <VcRotationGrid rows={query.data ?? []} loading={query.isLoading} />
      </Card>

      <Divider orientation="left">현장 swap 제안 (Planner 1클릭 수용)</Divider>
      <SwapProposalPanel />
    </Space>
  )
}
