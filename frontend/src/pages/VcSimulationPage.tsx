import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, DatePicker, Divider, List, Space, Tag, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import dayjs, { type Dayjs } from 'dayjs'
import { VcRotationGrid } from '@/features/vc-scheduling/components/VcRotationGrid'
import { SwapProposalPanel } from '@/features/vc-scheduling/components/SwapProposalPanel'
import { ConfirmModal, type ConfirmTarget } from '@/features/vc-scheduling/components/ConfirmModal'
import { DegradedBanner } from '@/features/mes/components/DegradedBanner'
import { ExcelFallbackModal } from '@/features/mes/components/ExcelFallbackModal'
import { fetchVcSlots, type VcSlotRow } from '@/features/vc-scheduling/api/vcScheduleApi'
import { stompClient, TOPIC_VC_SCHEDULE_UPDATES } from '@/api/stompClient'
import { useAuthStore } from '@/stores/authStore'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

/**
 * EP-15 VcSimulationPage — STK_USER 시뮬뷰 + 회전 격자 (BR-V04 1~18 회전).
 *
 * <p>{@code /vc/simview} 라우트, RBAC STK_USER + PLANNER + IT_OPS + READ_ONLY.
 *
 * <p>Sprint 14 ST-VC-4 — STOMP 실시간 broadcast (VcChangedEvent/VcConfirmedEvent)
 * 구독 → react-query invalidateQueries 로 1초 내 자동 갱신 (REQ-NF-PER-004 p95 ≤ 2초).
 */
export default function VcSimulationPage() {
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs(),
    dayjs().add(7, 'day'),
  ])
  const [stompConnected, setStompConnected] = useState(false)
  const [confirmTarget, setConfirmTarget] = useState<ConfirmTarget | null>(null)
  const [fallbackOpen, setFallbackOpen] = useState(false)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const isPlanner = useAuthStore((s) => s.hasRole('PLANNER'))

  const [from, to] = range
  const fromStr = from.format('YYYY-MM-DD')
  const toStr = to.format('YYYY-MM-DD')

  const query = useQuery<VcSlotRow[]>({
    queryKey: ['vc-slots', fromStr, toStr],
    queryFn: () => fetchVcSlots(fromStr, toStr),
    staleTime: 30_000,
  })

  // Sprint 14 ST-VC-4 — STOMP subscribe → vc-slots invalidate (PLANNER 변경 → STK 자동 갱신)
  useEffect(() => {
    let unsubscribe: (() => void) | null = null
    let cancelled = false

    stompClient
      .connect({ debug: false })
      .then(() => {
        if (cancelled) return
        setStompConnected(true)
        unsubscribe = stompClient.subscribe(TOPIC_VC_SCHEDULE_UPDATES, () => {
          // payload 무시 — invalidate 로 useQuery refetch 유발
          void queryClient.invalidateQueries({ queryKey: ['vc-slots'] })
        })
      })
      .catch(() => {
        if (!cancelled) setStompConnected(false)
      })

    return () => {
      cancelled = true
      if (unsubscribe) unsubscribe()
      // disconnect 안 함 — 다른 페이지 (CapacityQueue 등) 가 같은 connection 사용 가능
    }
  }, [queryClient])

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%', padding: 16 }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Title level={3} style={{ margin: 0 }}>성형 시뮬뷰 (EP-15)</Title>
        <Tag color={stompConnected ? 'green' : 'default'}>
          {stompConnected ? '● STOMP 실시간 연결' : '○ STOMP 미연결 (수동 갱신)'}
        </Tag>
      </Space>
      <DegradedBanner onOpenFallback={() => setFallbackOpen(true)} />
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <RangePicker
          value={range}
          onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
          allowClear={false}
        />
        {/* Sprint 14 ST-VC-5 — Capa/KD 통합 link (PLANNER + IT_OPS + READ_ONLY) */}
        <Space>
          <Button onClick={() => navigate('/vc/capacity-queue')}>
            Capa 큐 + KD 보충 →
          </Button>
          {isPlanner && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              ⓘ capa 초과 시 자동 큐 분리 (Phase 5+ allocator chain — 현재는 수동)
            </Text>
          )}
        </Space>
      </Space>
      {query.data && query.data.length === 0 && (
        <Alert
          type="info"
          showIcon
          message="시뮬뷰 데이터 없음"
          description="수주 확정 후 자동 입력 진입 (Phase 5+). 현재 V039 시드 (99999-SAMPLE-*) 가 있어야 1주 horizon 표시."
        />
      )}

      {query.error ? (
        <Alert type="error" message="시뮬뷰 조회 실패" description={String(query.error)} />
      ) : null}

      <Card styles={{ body: { padding: 0 } }}>
        <VcRotationGrid rows={query.data ?? []} loading={query.isLoading} />
      </Card>

      {isPlanner && (
        <>
          <Divider orientation="left">확정 대기 CANDIDATE (Sprint 16 EP-CONFIRM, BR-X01·X05)</Divider>
          <CandidateConfirmList
            rows={query.data ?? []}
            onConfirm={(t) => setConfirmTarget(t)}
          />
        </>
      )}

      <Divider orientation="left">현장 swap 제안 (Planner 1클릭 수용)</Divider>
      <SwapProposalPanel />

      <ConfirmModal
        target={confirmTarget}
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onSuccess={() => {
          setConfirmTarget(null)
          void queryClient.invalidateQueries({ queryKey: ['vc-slots'] })
        }}
      />

      <ExcelFallbackModal
        open={fallbackOpen}
        onClose={() => setFallbackOpen(false)}
        onSuccess={() => {
          setFallbackOpen(false)
          void queryClient.invalidateQueries({ queryKey: ['mes-degraded-status'] })
        }}
      />
    </Space>
  )
}

interface CandidateListProps {
  rows: VcSlotRow[]
  onConfirm: (target: ConfirmTarget) => void
}

function CandidateConfirmList({ rows, onConfirm }: CandidateListProps) {
  const candidates = useMemo(() => rows.filter((r) => r.status === 'CANDIDATE'), [rows])

  if (candidates.length === 0) {
    return (
      <Alert
        type="success"
        showIcon
        message="확정 대기 row 없음"
        description="모든 CANDIDATE row 가 확정 완료 또는 시뮬뷰에 데이터가 없습니다."
      />
    )
  }

  return (
    <List
      size="small"
      bordered
      dataSource={candidates}
      renderItem={(r) => (
        <List.Item
          actions={[
            <Button
              key="confirm"
              type="primary"
              size="small"
              data-testid={`confirm-trigger-${r.vcScheduleId}`}
              onClick={() =>
                onConfirm({
                  vcScheduleId: r.vcScheduleId,
                  hoseId: r.hoseId,
                  machineId: r.machineId,
                  productionDate: r.productionDate,
                  rotationNo: r.rotationNo,
                  slotPosition: r.slotPosition,
                  plannedQty: r.plannedQty,
                })
              }
            >
              확정
            </Button>,
          ]}
        >
          <Space size="middle">
            <Tag color="orange">CANDIDATE</Tag>
            <Text strong>{r.hoseId}</Text>
            <Text type="secondary">
              {r.machineId}·{r.slotPosition} · {r.productionDate} · rot{r.rotationNo} · qty{' '}
              {r.plannedQty}
            </Text>
          </Space>
        </List.Item>
      )}
    />
  )
}
