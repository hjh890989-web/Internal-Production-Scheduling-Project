import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import {
  acceptRequest,
  listQueueRequests,
  rejectRequest,
  type CapacityOverflowRequest,
} from '../api/capacityOverflowApi'

const { Text } = Typography
const { TextArea } = Input

/**
 * Sprint 8 BR-V12 Planner Pending Queue 승인 워크플로우 — REQ-FUNC-VC-022.
 *
 * <p>Tab1 에서 영속화된 요청들을 Planner 가 1클릭 승인 또는 거절 (reason 필수). priority_rank
 * ASC 정렬 + 1초 stale time. Reject 클릭 시 reason 입력 modal.
 */
export function PendingQueuePanel() {
  const queryClient = useQueryClient()
  const [rejectTarget, setRejectTarget] = useState<CapacityOverflowRequest | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  const query = useQuery<CapacityOverflowRequest[]>({
    queryKey: ['capacity-overflow-pending'],
    queryFn: () => listQueueRequests('PENDING'),
    staleTime: 5_000,
  })

  const acceptMutation = useMutation({
    mutationFn: (id: string) => acceptRequest(id),
    onSuccess: (r) => {
      void message.success(`승인 완료 — ${r.hoseId} qty=${r.requestedQty}`)
      void queryClient.invalidateQueries({ queryKey: ['capacity-overflow-pending'] })
    },
    onError: (e) => void message.error(`승인 실패: ${String(e)}`),
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      rejectRequest(id, reason),
    onSuccess: (r) => {
      void message.success(`거절 완료 — ${r.hoseId} (${r.decisionReason})`)
      setRejectTarget(null)
      setRejectReason('')
      void queryClient.invalidateQueries({ queryKey: ['capacity-overflow-pending'] })
    },
    onError: (e) => void message.error(`거절 실패: ${String(e)}`),
  })

  const handleRejectSubmit = () => {
    if (!rejectTarget) return
    if (!rejectReason.trim()) {
      void message.warning('reason 필수 (BR-X02 audit)')
      return
    }
    rejectMutation.mutate({ id: rejectTarget.requestId, reason: rejectReason.trim() })
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="BR-V12 — Pending Queue (Planner 1클릭 승인/거절)"
        description="Tab1 '큐 등록' 후 영속화된 요청. priority_rank ASC 정렬. 결정 후 immutable (V034 trigger)."
      />

      <Card
        title="Pending 요청 목록"
        extra={
          <Button
            onClick={() => query.refetch()}
            loading={query.isFetching}
          >
            새로고침
          </Button>
        }
      >
        {query.error ? (
          <Alert type="error" message="조회 실패" description={String(query.error)} />
        ) : (query.data?.length ?? 0) === 0 ? (
          <Empty description="Pending 요청 없음" />
        ) : (
          <Table
            size="small"
            rowKey="requestId"
            pagination={false}
            loading={query.isLoading}
            dataSource={query.data ?? []}
            columns={[
              {
                title: 'Rank',
                dataIndex: 'priorityRank',
                width: 70,
                render: (v: number) => (
                  <Tag color={v === 1 ? 'red' : v <= 3 ? 'orange' : 'default'}>{v}</Tag>
                ),
              },
              { title: 'Hose ID', dataIndex: 'hoseId', width: 160 },
              {
                title: 'Qty',
                dataIndex: 'requestedQty',
                width: 80,
                render: (v: number) => <Tag color="gold">{v}</Tag>,
              },
              {
                title: '요청 시각',
                dataIndex: 'requestedAt',
                render: (v: string) => dayjs(v).format('YY-MM-DD HH:mm'),
              },
              { title: '요청자', dataIndex: 'requestedBy' },
              {
                title: '액션',
                width: 180,
                render: (_: unknown, r: CapacityOverflowRequest) => (
                  <Space>
                    <Button
                      type="primary"
                      size="small"
                      onClick={() => acceptMutation.mutate(r.requestId)}
                      loading={acceptMutation.isPending}
                    >
                      승인
                    </Button>
                    <Button
                      danger
                      size="small"
                      onClick={() => setRejectTarget(r)}
                    >
                      거절
                    </Button>
                  </Space>
                ),
              },
            ]}
          />
        )}
      </Card>

      <Modal
        title={`거절 — ${rejectTarget?.hoseId} qty=${rejectTarget?.requestedQty}`}
        open={rejectTarget !== null}
        onCancel={() => {
          setRejectTarget(null)
          setRejectReason('')
        }}
        onOk={handleRejectSubmit}
        okText="거절 확정"
        cancelText="취소"
        okButtonProps={{ danger: true, loading: rejectMutation.isPending }}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Text>
            거절 사유 (필수) — audit log 에 영구 기록 (BR-X02).
          </Text>
          <TextArea
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="예: capa 불충분 — 다음 주 이월"
            rows={3}
            maxLength={500}
            showCount
          />
        </Space>
      </Modal>
    </Space>
  )
}
