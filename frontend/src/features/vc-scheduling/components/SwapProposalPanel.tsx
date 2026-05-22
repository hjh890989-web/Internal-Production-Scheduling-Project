import { Button, Empty, Space, Table, Tag, Typography, message } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useSwapProposals } from '../hooks/useSwapProposals'
import type { SwapProposalDto } from '../api/swapApi'

const { Text } = Typography

/**
 * Planner 1클릭 수용 패널 — TK-15-2-1+3 (EP-15 ST-15-2, REQ-FUNC-VC-018).
 *
 * <p>PROPOSED 목록 Table + Accept / Reject 버튼. accept 시 백엔드 atomic swap +
 * audit reason 자동 캡쳐 ({@code @Auditable}).
 */
export function SwapProposalPanel() {
  const { listQuery, accept, reject } = useSwapProposals('PROPOSED')

  const onAccept = (id: string) => {
    accept.mutate(
      { id, note: '1-click accept' },
      {
        onSuccess: () => void message.success('Swap 수용 — atomic rotation swap 완료'),
        onError: (e) => void message.error(`수용 실패: ${String(e)}`),
      },
    )
  }
  const onReject = (id: string) => {
    reject.mutate(
      { id, note: '거절' },
      {
        onSuccess: () => void message.success('Swap 거절'),
        onError: (e) => void message.error(`거절 실패: ${String(e)}`),
      },
    )
  }

  const columns = [
    { title: '제안 ID', dataIndex: 'proposalId', width: 280, ellipsis: true },
    { title: '제안자 (STK)', dataIndex: 'proposedBy', width: 120 },
    {
      title: '제안 시각',
      dataIndex: 'proposedAt',
      width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 100,
      render: (v: SwapProposalDto['status']) => (
        <Tag color={v === 'PROPOSED' ? 'gold' : v === 'ACCEPTED' ? 'green' : 'red'}>
          {v}
        </Tag>
      ),
    },
    {
      title: '액션',
      key: 'actions',
      width: 200,
      render: (_: unknown, row: SwapProposalDto) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<CheckCircleOutlined />}
            onClick={() => onAccept(row.proposalId)}
            loading={accept.isPending}
          >
            수용
          </Button>
          <Button
            danger
            size="small"
            icon={<CloseCircleOutlined />}
            onClick={() => onReject(row.proposalId)}
            loading={reject.isPending}
          >
            거절
          </Button>
        </Space>
      ),
    },
  ]

  if (listQuery.isLoading) return <Text>로딩 중...</Text>
  if (!listQuery.data || listQuery.data.length === 0) {
    return <Empty description="처리 대기 제안 없음" />
  }
  return (
    <Table<SwapProposalDto>
      rowKey="proposalId"
      columns={columns}
      dataSource={listQuery.data}
      size="small"
      pagination={false}
    />
  )
}
