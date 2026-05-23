import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  InputNumber,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { supplementKd, type SupplementResult } from '../api/capacityOverflowApi'

const { Text } = Typography

/**
 * BR-V13 Planner 1클릭 KD 잔량 보충 — Sprint 7 (REQ-FUNC-VC-023).
 *
 * <p>capa 부족 시 동일 hose 1차 + 동일 셋팅 그룹 hose 2차 fallback 으로 KD 잔량 자동 차감.
 * 활성 조건 — DI-08 KD_ORDER 마스터 입력 후.
 */
export function KdSupplementPanel() {
  const [hoseId, setHoseId] = useState<string>('29673-2R060')
  const [shortage, setShortage] = useState<number>(80)
  const [result, setResult] = useState<SupplementResult | null>(null)

  const supplementMutation = useMutation({
    mutationFn: () => supplementKd(hoseId, shortage),
    onSuccess: (r) => {
      setResult(r)
      const ok = r.supplemented >= r.shortage
      void (ok ? message.success : message.warning)(
        ok
          ? `보충 완료 — ${r.supplemented} 차감 (${r.consumed.length} KD orders)`
          : `부분 보충 — ${r.supplemented}/${r.shortage} (잔량 부족)`,
      )
    },
    onError: (e) => void message.error(`보충 실패: ${String(e)}`),
  })

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="BR-V13 — capa 부족 시 KD_ORDER 잔량 우선순위 보충"
        description="1차: 동일 hose · 2차: 동일 셋팅 그룹 hose fallback. atomic consume + audit 자동."
      />

      <Card title="입력 — 부족 hose + shortage qty">
        <Space>
          <Text strong>Hose ID:</Text>
          <Input
            value={hoseId}
            onChange={(e) => setHoseId(e.target.value)}
            placeholder="29673-2R060"
            style={{ width: 200 }}
          />
          <Text strong>부족량:</Text>
          <InputNumber
            min={0}
            value={shortage}
            onChange={(v) => setShortage(v ?? 0)}
            addonAfter="회전"
          />
          <Button
            type="primary"
            onClick={() => supplementMutation.mutate()}
            loading={supplementMutation.isPending}
            disabled={!hoseId || shortage <= 0}
          >
            KD 잔량 보충
          </Button>
        </Space>
      </Card>

      {result && (
        <>
          <Card title="보충 결과 요약">
            <Space size="large">
              <Statistic title="요청 shortage" value={result.shortage} suffix="회전" />
              <Statistic
                title="실 보충"
                value={result.supplemented}
                suffix="회전"
                valueStyle={{
                  color: result.supplemented >= result.shortage ? '#389e0d' : '#d48806',
                }}
              />
              <Statistic
                title="부족 잔여"
                value={result.shortage - result.supplemented}
                suffix="회전"
                valueStyle={{
                  color: result.shortage - result.supplemented === 0 ? '#389e0d' : '#cf1322',
                }}
              />
              <Statistic title="소진 KD orders" value={result.consumed.length} />
            </Space>
          </Card>

          <Card title="소진 KD orders 내역">
            {result.consumed.length > 0 ? (
              <Table
                size="small"
                pagination={false}
                rowKey="kdOrderId"
                dataSource={result.consumed}
                columns={[
                  {
                    title: '#',
                    width: 50,
                    render: (_: unknown, __: unknown, i: number) => i + 1,
                  },
                  {
                    title: '출처',
                    dataIndex: 'fromHose',
                    render: (v: string) =>
                      v === hoseId ? <Tag color="green">동일 hose</Tag> : (
                        <Tag color="blue">그룹 ({v})</Tag>
                      ),
                  },
                  {
                    title: 'KD Order ID',
                    dataIndex: 'kdOrderId',
                    ellipsis: true,
                  },
                  { title: '차감 qty', dataIndex: 'qty' },
                ]}
              />
            ) : (
              <Empty description="KD 잔량 0 — 보충 불가" />
            )}
          </Card>
        </>
      )}
    </Space>
  )
}
