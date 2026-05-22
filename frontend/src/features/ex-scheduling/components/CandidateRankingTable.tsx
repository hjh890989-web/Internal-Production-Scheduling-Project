import { useQuery } from '@tanstack/react-query'
import { Alert, Progress, Table, Tag, Typography } from 'antd'
import { fetchRanking, type RankedCandidate } from '../api/rankingApi'

const { Text } = Typography

interface Props {
  from: string
  to: string
}

/**
 * EP-18 다중 후보 ranking 비교 테이블 — TK-18-1-1 (REQ-FUNC-XT-001 ≥ 3 distinct).
 *
 * <p>3 점수 (slack / balance / setting) + totalScore Progress 시각화.
 */
export function CandidateRankingTable({ from, to }: Props) {
  const query = useQuery<RankedCandidate[]>({
    queryKey: ['ex-ranking', from, to],
    queryFn: () => fetchRanking(from, to, 10),
    staleTime: 30_000,
  })

  if (query.error) {
    return <Alert type="error" message="Ranking 조회 실패" description={String(query.error)} />
  }
  if (!query.data) return <Text>로딩 중...</Text>

  const columns = [
    {
      title: '순위',
      key: 'rank',
      width: 60,
      render: (_: unknown, __: RankedCandidate, idx: number) => (
        <Tag color={idx === 0 ? 'gold' : idx < 3 ? 'blue' : 'default'}>{idx + 1}</Tag>
      ),
    },
    { title: '품번', dataIndex: 'hoseId', width: 140 },
    { title: 'deadline', dataIndex: 'extrusionDeadline', width: 110 },
    {
      title: 'yield',
      dataIndex: 'vcYield',
      width: 90,
      render: (v: number) => (v === 2531 ? <Tag color="blue">{v} ⭐</Tag> : v),
    },
    {
      title: '기한 여유',
      dataIndex: 'slackDaysScore',
      width: 130,
      render: (v: number) => <Progress percent={Math.round(v * 100)} size="small" />,
    },
    {
      title: '라인 균형',
      dataIndex: 'balanceScore',
      width: 130,
      render: (v: number) => <Progress percent={Math.round(v * 100)} size="small" />,
    },
    {
      title: '셋팅 단일',
      dataIndex: 'settingScore',
      width: 130,
      render: (v: number) => <Progress percent={Math.round(v * 100)} size="small" />,
    },
    {
      title: 'total',
      dataIndex: 'totalScore',
      width: 130,
      render: (v: number) => (
        <Progress percent={Math.round(v * 100)} size="small" strokeColor="#1677ff" />
      ),
    },
  ]

  return (
    <Table<RankedCandidate>
      rowKey="exCandidateId"
      columns={columns}
      dataSource={query.data}
      loading={query.isLoading}
      pagination={{ pageSize: 10 }}
      size="small"
    />
  )
}
