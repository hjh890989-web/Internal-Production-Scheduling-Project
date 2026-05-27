import { useState } from 'react'
import { Alert, Badge, Button, Card, DatePicker, Space, Tabs, Typography, message } from 'antd'
import { DownloadOutlined, WifiOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import dayjs, { type Dayjs } from 'dayjs'
import { ExMatrixGrid } from '@/features/ex-scheduling/components/ExMatrixGrid'
import { CandidateRankingTable } from '@/features/ex-scheduling/components/CandidateRankingTable'
import { useExMatrix } from '@/features/ex-scheduling/hooks/useExMatrix'
import { downloadExMatrixXlsx } from '@/features/ex-scheduling/api/exMatrixApi'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

/**
 * EP-17 ExMatrixPage — 일자×shift×라인 매트릭스 + EP-EX14 chain UI.
 *
 * <p>{@code /extrusion-matrix} 라우트, RBAC STK_USER + PLANNER + IT_OPS.
 * STOMP push 수신 시 lastUpdate badge + 자동 grid 갱신 (useExMatrix invalidate).
 */
export default function ExMatrixPage() {
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(2, 'day'),
    dayjs().add(7, 'day'),
  ])
  const navigate = useNavigate()
  const [from, to] = range
  const fromStr = from.format('YYYY-MM-DD')
  const toStr = to.format('YYYY-MM-DD')

  const { data, isLoading, error, connected, lastUpdate } = useExMatrix(fromStr, toStr)

  const onDownload = async () => {
    try {
      const blob = await downloadExMatrixXlsx(fromStr, toStr)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `EX_MATRIX_${fromStr}_${toStr}.xlsx`
      a.click()
      URL.revokeObjectURL(url)
      void message.success('압출 매트릭스 다운로드 완료')
    } catch (e) {
      void message.error(`다운로드 실패: ${String(e)}`)
    }
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%', padding: 16 }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Title level={3} style={{ margin: 0 }}>압출 매트릭스 (EP-17)</Title>
        <Button onClick={() => navigate('/vc/simview')}>
          ← 성형 시뮬뷰
        </Button>
      </Space>

      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <RangePicker
            value={range}
            onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
            allowClear={false}
          />
          <Button type="primary" icon={<DownloadOutlined />} onClick={onDownload}>
            Excel 다운로드 (EP-12)
          </Button>
        </Space>
        <Space>
          <Badge
            status={connected ? 'success' : 'default'}
            text={
              <Text type={connected ? 'success' : 'secondary'}>
                <WifiOutlined /> STOMP {connected ? 'connected' : 'disconnected'}
              </Text>
            }
          />
          {lastUpdate && (
            <Text type="secondary">
              마지막 cascade: {dayjs(lastUpdate.completedAt).format('HH:mm:ss')}
              {' '}({lastUpdate.triggeredCount}건 갱신)
            </Text>
          )}
        </Space>
      </Space>

      {data && data.length === 0 && !isLoading && (
        <Alert
          type="info"
          showIcon
          message="압출 매트릭스 데이터 없음"
          description="VC 확정 후 자동 입력 (Sprint 6 EP-EX13/14 chain). 현재 V040 시드 99999-SAMPLE-EX-* 가 있어야 표시 (V039 vc_schedule sample chain)."
        />
      )}

      {error ? (
        <Alert type="error" message="매트릭스 조회 실패" description={String(error)} />
      ) : null}

      <Tabs
        defaultActiveKey="matrix"
        items={[
          {
            key: 'matrix',
            label: '매트릭스 (EP-17)',
            children: (
              <Card bodyStyle={{ padding: 0 }}>
                <ExMatrixGrid rows={data ?? []} loading={isLoading} />
              </Card>
            ),
          },
          {
            key: 'ranking',
            label: '다중 후보 ranking (EP-18)',
            children: <CandidateRankingTable from={fromStr} to={toStr} />,
          },
        ]}
      />
    </Space>
  )
}
