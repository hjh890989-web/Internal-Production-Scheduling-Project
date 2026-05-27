import { useEffect, useMemo, useState } from 'react'
import {
  Alert, Button, Card, Col, Form, Input, Modal, Row, Space, Spin, Statistic, Table, Tag, Typography, message,
} from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import {
  orderDiffApi,
  type DiffSummaryResponse,
  type DiffType,
  type RowDiffSummary,
  type Severity,
} from '@/api/orderDiffApi'
import { HttpError } from '@/api/client'
import { useAuthStore } from '@/stores/authStore'

const { Title, Paragraph, Text } = Typography

const SEVERITY_COLOR: Record<Severity, string> = {
  CRITICAL: 'red',
  IMPORTANT: 'orange',
  STANDARD: 'blue',
}

const DIFF_TYPE_COLOR: Record<DiffType, string> = {
  NEW: 'green',
  MODIFIED: 'blue',
  DELETED: 'red',
  UNCHANGED: 'default',
}

interface DecisionForm {
  reason: string
}

/**
 * Sprint 13 EP-OC-FULL DiffPage (TK-OC-3·4, BR-O02 + BR-X02).
 *
 * <p>trackingId 기준 OrderChange 의 severity 분류 시각 + PLANNER 확정/거절.
 * CRITICAL row 빨간 highlight + 상단 정렬. PLANNER 만 확정 가능 (BR-X05 작성자).
 */
export default function DiffPage() {
  const { trackingId } = useParams<{ trackingId: string }>()
  const navigate = useNavigate()
  const hasPlannerRole = useAuthStore((s) => s.hasRole('PLANNER'))

  const [data, setData] = useState<DiffSummaryResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [decisionModal, setDecisionModal] = useState<'commit' | 'reject' | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const reload = async () => {
    if (!trackingId) return
    setLoading(true)
    setError(null)
    try { setData(await orderDiffApi.get(trackingId)) }
    catch (e) {
      if (e instanceof HttpError) setError(`HTTP ${e.status} — diff 조회 실패`)
      else setError('네트워크 오류')
    } finally { setLoading(false) }
  }
  useEffect(() => { void reload() /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [trackingId])

  // CRITICAL 상단 정렬
  const sortedRows = useMemo(() => {
    if (!data) return []
    const order: Record<string, number> = { CRITICAL: 0, IMPORTANT: 1, STANDARD: 2 }
    return [...data.rows].sort((a, b) => {
      const oa = a.severity ? order[a.severity] ?? 9 : 9
      const ob = b.severity ? order[b.severity] ?? 9 : 9
      return oa - ob
    })
  }, [data])

  const handleDecision = async (values: DecisionForm) => {
    if (!trackingId || !decisionModal) return
    setSubmitting(true)
    try {
      if (decisionModal === 'commit') {
        await orderDiffApi.commit(trackingId, values.reason)
        message.success(`수주 확정 완료 — ${trackingId.substring(0, 8)} (${data?.totalRows ?? 0} rows)`)
      } else {
        await orderDiffApi.reject(trackingId, values.reason)
        message.success(`수주 거절 완료 — ${trackingId.substring(0, 8)}`)
      }
      setDecisionModal(null)
      navigate('/home')
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('권한 없음 — PLANNER 만 확정 가능 (BR-X05)')
      } else if (e instanceof HttpError && e.status === 404) {
        message.error('trackingId 미존재 또는 diff 미진행')
      } else {
        message.error('처리 실패')
      }
    } finally { setSubmitting(false) }
  }

  const columns = [
    {
      title: '심각도', dataIndex: 'severity', key: 'severity', width: 110,
      render: (s: Severity | null) =>
        s ? <Tag color={SEVERITY_COLOR[s]}>{s}</Tag> : <Tag>미분류</Tag>,
    },
    {
      title: '변경 유형', dataIndex: 'diffType', key: 'diffType', width: 100,
      render: (t: DiffType) => <Tag color={DIFF_TYPE_COLOR[t]}>{t}</Tag>,
    },
    { title: 'Hose ID', dataIndex: 'hoseId', key: 'hoseId', width: 150 },
    { title: '납기', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 110 },
    {
      title: '필드 변경 (JSON)', dataIndex: 'fieldDiffs', key: 'fieldDiffs', ellipsis: true,
      render: (s: string) => <Text code style={{ fontSize: 11 }}>{s}</Text>,
    },
    { title: '버전', key: 'version', width: 80,
      render: (_: unknown, r: RowDiffSummary) => `${r.previousVersion} → ${r.newVersion}` },
  ]

  if (!trackingId) {
    return <Alert type="error" message="trackingId 누락" />
  }

  return (
    <div>
      <Title level={3}>수주 import 검토 — {trackingId.substring(0, 8)}…</Title>
      <Paragraph type="secondary">
        BR-O02 severity 분류 — CRITICAL (납기 변경 / hose ID 변경 / 신규 / 삭제 / ±20%) /
        IMPORTANT / STANDARD. PLANNER 만 확정 (BR-X05 작성자).
      </Paragraph>

      {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}
      {loading && <Spin size="large" style={{ display: 'block', margin: '40px auto' }} />}

      {data && (
        <>
          {/* severity 통계 카드 4종 */}
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col span={6}><Card><Statistic title="전체 row" value={data.totalRows} /></Card></Col>
            <Col span={6}>
              <Card><Statistic title="CRITICAL" value={data.criticalCount}
                valueStyle={{ color: '#cf1322' }} /></Card>
            </Col>
            <Col span={6}>
              <Card><Statistic title="IMPORTANT" value={data.importantCount}
                valueStyle={{ color: '#d46b08' }} /></Card>
            </Col>
            <Col span={6}>
              <Card><Statistic title="STANDARD" value={data.standardCount}
                valueStyle={{ color: '#1677ff' }} /></Card>
            </Col>
          </Row>

          {/* 확정/거절 액션 (PLANNER 만) */}
          <Space style={{ marginBottom: 16 }}>
            <Button
              type="primary"
              size="large"
              disabled={!hasPlannerRole || data.totalRows === 0}
              onClick={() => setDecisionModal('commit')}
            >
              확정 (BR-X05)
            </Button>
            <Button
              size="large"
              danger
              disabled={!hasPlannerRole || data.totalRows === 0}
              onClick={() => setDecisionModal('reject')}
            >
              거절
            </Button>
            <Button onClick={() => void reload()}>새로고침</Button>
            {!hasPlannerRole && (
              <Text type="secondary">조회 전용 — PLANNER 만 확정/거절 가능</Text>
            )}
          </Space>

          <Table
            rowKey="changeId"
            dataSource={sortedRows}
            columns={columns}
            pagination={{ pageSize: 50, showSizeChanger: true }}
            size="small"
            rowClassName={(r) => r.severity === 'CRITICAL' ? 'diff-row-critical' : ''}
            style={{ marginTop: 8 }}
          />

          {/* CRITICAL row 배경 강조 (inline CSS — index.html 또는 main.css 통합 시 cleanup) */}
          <style>{`
            .diff-row-critical { background-color: #fff1f0 !important; }
            .diff-row-critical:hover > td { background-color: #ffccc7 !important; }
          `}</style>
        </>
      )}

      {/* 확정/거절 reason Modal */}
      <Modal
        title={decisionModal === 'commit' ? '수주 확정 (BR-X05)' : '수주 거절'}
        open={!!decisionModal}
        onCancel={() => setDecisionModal(null)}
        footer={null}
        destroyOnClose
      >
        <Alert
          type={decisionModal === 'commit' ? 'info' : 'warning'}
          message={
            decisionModal === 'commit'
              ? `${data?.totalRows ?? 0} row 확정 — Sprint 14 EP-VC-FULL listener 가 성형 스케줄 입력 진입 (BR-X02 audit).`
              : 'PLANNER 가 입력 파일 재요청 의도 — event 미발행, audit_log 만 기록.'
          }
          style={{ marginBottom: 16 }}
        />
        <Form<DecisionForm> layout="vertical" onFinish={handleDecision}>
          <Form.Item
            label="사유 (필수, BR-X02 audit)"
            name="reason"
            rules={[{ required: true, message: '사유 입력 필수' }]}
          >
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={submitting}
              danger={decisionModal === 'reject'}
            >
              {decisionModal === 'commit' ? '확정 진행' : '거절 진행'}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
