import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Empty,
  InputNumber,
  Progress,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { splitCapacity, type SplitResult } from '../api/capacityOverflowApi'

const { Text, Paragraph } = Typography

interface RequiredRow {
  hoseId: string
  qty: number
}

/**
 * BR-V12 Planner 추가 요청 큐 미리보기 — Sprint 7 (REQ-FUNC-VC-022).
 *
 * <p>capa 초과 시 (a) 자동 채택분 + (b) 추가 요청 큐 분리 미리보기.
 * Planner 가 (b) 승인 여부 결정 (Sprint 8+ 승인 confirm UI).
 *
 * <p>활성 조건 — DI-07 PRODUCT_PRIORITY 마스터 입력 후 본격 활용.
 */
export function CapacityOverflowSplitPanel() {
  const [rows, setRows] = useState<RequiredRow[]>([
    { hoseId: '29673-2R060', qty: 60 },
    { hoseId: '28422-2M800', qty: 50 },
    { hoseId: '28421-2M800', qty: 40 },
  ])
  const [dailyCapa, setDailyCapa] = useState<number>(90)   // BR-V05 default LP 72 + IC 18
  const [result, setResult] = useState<SplitResult | null>(null)

  const splitMutation = useMutation({
    mutationFn: () => {
      const required: Record<string, number> = {}
      rows.forEach((r) => {
        if (r.hoseId && r.qty > 0) required[r.hoseId] = r.qty
      })
      return splitCapacity(required, dailyCapa)
    },
    onSuccess: (r) => {
      setResult(r)
      void message.success(
        `Split 완료 — 채택 ${r.totalAccepted} / 추가 요청 ${r.totalQueued}`,
      )
    },
    onError: (e) => void message.error(`Split 실패: ${String(e)}`),
  })

  const acceptedRows = result
    ? Object.entries(result.accepted).map(([hose, qty]) => ({ hose, qty }))
    : []
  const queueRows = result
    ? Object.entries(result.requestQueue).map(([hose, qty]) => ({ hose, qty }))
    : []

  const total = acceptedRows.reduce((s, r) => s + r.qty, 0)
    + queueRows.reduce((s, r) => s + r.qty, 0)
  const capacityFillRate = total > 0
    ? Math.round((result?.totalAccepted ?? 0) / dailyCapa * 100)
    : 0

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="BR-V12 — capa 초과 시 PRODUCT_PRIORITY rank ASC 정렬 → 자동 채택 + 추가 요청 큐"
        description="활성 조건: 수주통합 후 DI-07 PRODUCT_PRIORITY 마스터 입력. 미등록 hose 는 rank 99 fallback."
      />

      <Card title="입력 — daily capacity + hose 별 요구량">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space>
            <Text strong>Daily capacity (BR-V05):</Text>
            <InputNumber
              min={1}
              max={1000}
              value={dailyCapa}
              onChange={(v) => setDailyCapa(v ?? 90)}
              addonAfter="회전"
            />
            <Text type="secondary">기본 LP 72 + IC 18 = 90 (1일)</Text>
          </Space>
          <Table
            rowKey="hoseId"
            size="small"
            pagination={false}
            dataSource={rows}
            columns={[
              { title: 'Hose ID', dataIndex: 'hoseId' },
              {
                title: '요구량',
                dataIndex: 'qty',
                render: (v: number, r: RequiredRow, idx: number) => (
                  <InputNumber
                    min={0}
                    value={v}
                    onChange={(nv) => {
                      const next = [...rows]
                      next[idx] = { ...r, qty: nv ?? 0 }
                      setRows(next)
                    }}
                  />
                ),
              },
            ]}
          />
          <Button
            type="primary"
            onClick={() => splitMutation.mutate()}
            loading={splitMutation.isPending}
          >
            Split 미리보기
          </Button>
        </Space>
      </Card>

      {result && (
        <>
          <Card title="결과 요약">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Space>
                <Tag color="green">자동 채택: {result.totalAccepted}</Tag>
                <Tag color="gold">추가 요청 큐: {result.totalQueued}</Tag>
                <Tag color="blue">총 요구: {total}</Tag>
                <Tag>daily_capa: {dailyCapa}</Tag>
              </Space>
              <Paragraph>
                <Text>capa 사용률 — </Text>
                <Progress
                  percent={capacityFillRate}
                  size="small"
                  status={capacityFillRate >= 100 ? 'exception' : 'normal'}
                  style={{ display: 'inline-block', width: 200 }}
                />
              </Paragraph>
            </Space>
          </Card>

          <Card title="(a) 자동 채택 — Allocator 즉시 진행">
            {acceptedRows.length > 0 ? (
              <Table
                size="small"
                pagination={false}
                rowKey="hose"
                dataSource={acceptedRows}
                columns={[
                  { title: 'Hose ID', dataIndex: 'hose' },
                  { title: '채택 qty', dataIndex: 'qty' },
                ]}
              />
            ) : (
              <Empty description="채택분 없음" />
            )}
          </Card>

          <Card title="(b) 추가 요청 큐 — Planner 승인 대기">
            {queueRows.length > 0 ? (
              <>
                <Alert
                  type="warning"
                  message="Planner 승인 필요"
                  description="본 큐는 Sprint 8+ 의 1클릭 승인 모달과 연동 예정. 현재는 미리보기만."
                  style={{ marginBottom: 12 }}
                />
                <Table
                  size="small"
                  pagination={false}
                  rowKey="hose"
                  dataSource={queueRows}
                  columns={[
                    { title: 'Hose ID', dataIndex: 'hose' },
                    { title: '대기 qty', dataIndex: 'qty', render: (v) => <Tag color="gold">{v}</Tag> },
                  ]}
                />
              </>
            ) : (
              <Empty description="추가 요청 없음 — capa 내 모두 채택" />
            )}
          </Card>
        </>
      )}
    </Space>
  )
}
