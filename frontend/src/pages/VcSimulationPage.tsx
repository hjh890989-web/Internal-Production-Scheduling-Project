import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Divider,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import type { TableColumnsType, TableProps } from 'antd'
import { useNavigate } from 'react-router-dom'
import dayjs, { type Dayjs } from 'dayjs'
import { VcRotationGrid } from '@/features/vc-scheduling/components/VcRotationGrid'
import { SwapProposalPanel } from '@/features/vc-scheduling/components/SwapProposalPanel'
import { ConfirmModal, type ConfirmTarget } from '@/features/vc-scheduling/components/ConfirmModal'
import { BatchConfirmModal } from '@/features/vc-scheduling/components/BatchConfirmModal'
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
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [batchOpen, setBatchOpen] = useState(false)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const isPlanner = useAuthStore((s) => s.hasRole('PLANNER'))
  const employeeId = useAuthStore((s) => s.user?.employeeId ?? null)

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
          <Divider orientation="left">확정 대기 CANDIDATE (Sprint 16/17 EP-CONFIRM, BR-X01·X05)</Divider>
          <CandidateConfirmTable
            rows={query.data ?? []}
            currentEmployeeId={employeeId}
            selectedRowKeys={selectedRowKeys}
            onSelectChange={setSelectedRowKeys}
            onSingleConfirm={(t) => setConfirmTarget(t)}
            onOpenBatch={() => setBatchOpen(true)}
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

      <BatchConfirmModalWrapper
        rows={query.data ?? []}
        currentEmployeeId={employeeId}
        selectedRowKeys={selectedRowKeys}
        open={batchOpen}
        onClose={() => setBatchOpen(false)}
        onSuccess={(count) => {
          setBatchOpen(false)
          setSelectedRowKeys([])
          message.success(`${count} 건 일괄 확정 완료`)
          void queryClient.invalidateQueries({ queryKey: ['vc-slots'] })
        }}
      />
    </Space>
  )
}

interface CandidateTableProps {
  rows: VcSlotRow[]
  currentEmployeeId: string | null
  selectedRowKeys: React.Key[]
  onSelectChange: (keys: React.Key[]) => void
  onSingleConfirm: (target: ConfirmTarget) => void
  onOpenBatch: () => void
}

type AuthorFilter = 'ALL' | 'MINE' | 'OTHERS' | 'LEGACY'

function CandidateConfirmTable({
  rows,
  currentEmployeeId,
  selectedRowKeys,
  onSelectChange,
  onSingleConfirm,
  onOpenBatch,
}: CandidateTableProps) {
  const allCandidates = useMemo(() => rows.filter((r) => r.status === 'CANDIDATE'), [rows])

  const selfAuthoredKeys = useMemo(
    () =>
      new Set(
        allCandidates
          .filter((r) => r.createdBy && currentEmployeeId && r.createdBy === currentEmployeeId)
          .map((r) => r.vcScheduleId),
      ),
    [allCandidates, currentEmployeeId],
  )

  // Sprint 19 hotfix — 검색/필터 (Hose ID substring + 작성자 분류)
  const [search, setSearch] = useState('')
  const [authorFilter, setAuthorFilter] = useState<AuthorFilter>('ALL')

  const candidates = useMemo(() => {
    const q = search.trim().toLowerCase()
    return allCandidates.filter((r) => {
      // Hose ID 검색
      if (q && !r.hoseId.toLowerCase().includes(q)) return false
      // 작성자 필터
      if (authorFilter === 'MINE') return selfAuthoredKeys.has(r.vcScheduleId)
      if (authorFilter === 'OTHERS')
        return !selfAuthoredKeys.has(r.vcScheduleId) && !!r.createdBy
      if (authorFilter === 'LEGACY') return !r.createdBy
      return true
    })
  }, [allCandidates, search, authorFilter, selfAuthoredKeys])

  // 필터 변경 시 선택 자동 해제 (안 보이는 row 선택 잔여 차단)
  const clearSelectionIfHidden = (next: AuthorFilter, nextSearch: string) => {
    if (selectedRowKeys.length === 0) return
    const visibleIds = new Set(
      allCandidates
        .filter((r) => {
          const q = nextSearch.trim().toLowerCase()
          if (q && !r.hoseId.toLowerCase().includes(q)) return false
          if (next === 'MINE') return selfAuthoredKeys.has(r.vcScheduleId)
          if (next === 'OTHERS') return !selfAuthoredKeys.has(r.vcScheduleId) && !!r.createdBy
          if (next === 'LEGACY') return !r.createdBy
          return true
        })
        .map((r) => r.vcScheduleId),
    )
    const remaining = selectedRowKeys.filter((k) => visibleIds.has(String(k)))
    if (remaining.length !== selectedRowKeys.length) {
      onSelectChange(remaining)
    }
  }

  if (allCandidates.length === 0) {
    return (
      <Alert
        type="success"
        showIcon
        message="확정 대기 row 없음"
        description="모든 CANDIDATE row 가 확정 완료 또는 시뮬뷰에 데이터가 없습니다."
      />
    )
  }

  const columns: TableColumnsType<VcSlotRow> = [
    {
      title: '상태',
      key: 'status',
      width: 110,
      render: (_, r) =>
        selfAuthoredKeys.has(r.vcScheduleId) ? (
          <Tag color="red">본인 작성</Tag>
        ) : (
          <Tag color="orange">CANDIDATE</Tag>
        ),
    },
    { title: 'Hose ID', dataIndex: 'hoseId', width: 160 },
    {
      title: '머신·슬롯',
      key: 'machineSlot',
      width: 130,
      render: (_, r) => `${r.machineId}·${r.slotPosition}`,
    },
    { title: '생산일', dataIndex: 'productionDate', width: 130 },
    {
      title: '회전',
      key: 'rot',
      width: 80,
      render: (_, r) => `rot${r.rotationNo}`,
    },
    { title: '수량', dataIndex: 'plannedQty', width: 90, align: 'right' as const },
    {
      title: '작성자',
      dataIndex: 'createdBy',
      width: 110,
      render: (v) => v ?? <Text type="secondary">legacy</Text>,
    },
    {
      title: '단건 확정',
      key: 'single',
      width: 110,
      render: (_, r) => (
        <Button
          type="primary"
          size="small"
          disabled={selfAuthoredKeys.has(r.vcScheduleId)}
          data-testid={`confirm-trigger-${r.vcScheduleId}`}
          onClick={() =>
            onSingleConfirm({
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
        </Button>
      ),
    },
  ]

  const rowSelection: TableProps<VcSlotRow>['rowSelection'] = {
    selectedRowKeys,
    onChange: onSelectChange,
    getCheckboxProps: (r) => ({
      disabled: selfAuthoredKeys.has(r.vcScheduleId),
      name: r.vcScheduleId,
    }),
  }

  const selectedNonSelfCount = selectedRowKeys.filter(
    (k) => !selfAuthoredKeys.has(String(k)),
  ).length

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="small">
      <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="Hose ID 검색 (substring)"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              clearSelectionIfHidden(authorFilter, e.target.value)
            }}
            onSearch={(v) => {
              setSearch(v)
              clearSelectionIfHidden(authorFilter, v)
            }}
            style={{ width: 240 }}
            data-testid="candidate-search"
          />
          <Select
            value={authorFilter}
            onChange={(v: AuthorFilter) => {
              setAuthorFilter(v)
              clearSelectionIfHidden(v, search)
            }}
            style={{ width: 180 }}
            options={[
              { value: 'ALL', label: '작성자: 전체' },
              { value: 'MINE', label: '본인 작성' },
              { value: 'OTHERS', label: '다른 PLANNER' },
              { value: 'LEGACY', label: 'legacy (NULL)' },
            ]}
            data-testid="candidate-author-filter"
          />
          <Text type="secondary">
            필터 결과 <Tag>{candidates.length}</Tag> / 전체 CANDIDATE {allCandidates.length} · 본인 작성{' '}
            <Tag color="red">{selfAuthoredKeys.size}</Tag>
          </Text>
        </Space>
        <Space wrap>
          <Button
            onClick={() =>
              onSelectChange(
                candidates
                  .filter((r) => !selfAuthoredKeys.has(r.vcScheduleId))
                  .map((r) => r.vcScheduleId),
              )
            }
            data-testid="batch-select-all"
          >
            필터 결과 전체 선택 (본인 제외)
          </Button>
          <Button onClick={() => onSelectChange([])} disabled={selectedRowKeys.length === 0}>
            선택 해제
          </Button>
          <Button
            type="primary"
            disabled={selectedNonSelfCount === 0}
            onClick={onOpenBatch}
            data-testid="batch-confirm-trigger"
          >
            선택 일괄 확정 ({selectedNonSelfCount}건)
          </Button>
        </Space>
      </Space>
      <Table<VcSlotRow>
        size="small"
        bordered
        rowKey="vcScheduleId"
        columns={columns}
        dataSource={candidates}
        rowSelection={rowSelection}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        scroll={{ x: 'max-content' }}
        locale={{
          emptyText: search || authorFilter !== 'ALL'
            ? '필터 결과 없음 — 검색어/작성자 필터 조정'
            : '확정 대기 row 없음',
        }}
      />
    </Space>
  )
}

interface BatchModalWrapperProps {
  rows: VcSlotRow[]
  currentEmployeeId: string | null
  selectedRowKeys: React.Key[]
  open: boolean
  onClose: () => void
  onSuccess: (count: number) => void
}

function BatchConfirmModalWrapper({
  rows,
  currentEmployeeId,
  selectedRowKeys,
  open,
  onClose,
  onSuccess,
}: BatchModalWrapperProps) {
  const candidates = useMemo(() => rows.filter((r) => r.status === 'CANDIDATE'), [rows])

  const { selected, excludedSelfAuthoredCount } = useMemo(() => {
    const keySet = new Set(selectedRowKeys.map(String))
    const all = candidates.filter((r) => keySet.has(r.vcScheduleId))
    const sel = all.filter((r) => !r.createdBy || r.createdBy !== currentEmployeeId)
    return { selected: sel, excludedSelfAuthoredCount: all.length - sel.length }
  }, [candidates, selectedRowKeys, currentEmployeeId])

  return (
    <BatchConfirmModal
      selected={selected}
      excludedSelfAuthoredCount={excludedSelfAuthoredCount}
      open={open}
      onClose={onClose}
      onSuccess={onSuccess}
    />
  )
}
