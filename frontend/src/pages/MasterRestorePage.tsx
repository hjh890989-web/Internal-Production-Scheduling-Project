import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  Card,
  DatePicker,
  Divider,
  Empty,
  Input,
  Select,
  Space,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import {
  fetchSnapshot,
  fetchTimeline,
  type SnapshotResult,
  type TimelineEntry,
} from '@/features/audit-snapshot/api/auditSnapshotApi'

const { Title, Text, Paragraph } = Typography

/**
 * EP-19 MasterRestorePage — 임의 시점 마스터 복원 UI (REQ-FUNC-OC-014).
 *
 * <p>{@code /audit/restore} 라우트, RBAC IT_OPS + READ_ONLY + PLANNER.
 *
 * <p>UI 흐름:
 * <ol>
 *   <li>table + rowPk 입력 → timeline 자동 조회</li>
 *   <li>timestamp picker 선택 → 해당 시점 snapshot JSON 표시</li>
 *   <li>실제 복원은 별도 confirm 흐름 (Sprint 6+ 위험 방지)</li>
 * </ol>
 */
export default function MasterRestorePage() {
  const [table, setTable] = useState<string>('vc_schedule')
  const [rowPk, setRowPk] = useState<string>('')
  const [at, setAt] = useState<Dayjs>(dayjs())

  const snapshotQuery = useQuery<SnapshotResult>({
    queryKey: ['audit-snapshot', table, rowPk, at.toISOString()],
    queryFn: () => fetchSnapshot(table, rowPk, at.toISOString()),
    enabled: Boolean(rowPk),
    staleTime: 10_000,
  })

  const timelineQuery = useQuery<TimelineEntry[]>({
    queryKey: ['audit-timeline', table, rowPk],
    queryFn: () => fetchTimeline(table, rowPk),
    enabled: Boolean(rowPk),
    staleTime: 30_000,
  })

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%', padding: 16 }}>
      <Title level={3}>임의 시점 마스터 복원 (EP-19)</Title>

      <Alert
        type="info"
        showIcon
        message="audit forensic 조회 — 실제 복원은 별도 confirm 흐름 (위험 방지)"
        description="audit.schedule_audit_log 의 JSONB 역재생. NFR-SEC-004 immutable (3년 보존)."
      />

      <Space wrap>
        <Select
          value={table}
          onChange={setTable}
          style={{ width: 220 }}
          options={[
            { label: 'app.vc_schedule', value: 'vc_schedule' },
            { label: 'app.ex_schedule_candidate', value: 'ex_schedule_candidate' },
            { label: 'app.order', value: 'order' },
          ]}
        />
        <Input
          value={rowPk}
          onChange={(e) => setRowPk(e.target.value)}
          placeholder="row PK (UUID)"
          style={{ width: 320 }}
        />
        <DatePicker
          value={at}
          onChange={(v) => v && setAt(v)}
          showTime
          format="YYYY-MM-DD HH:mm:ss"
          style={{ width: 220 }}
        />
      </Space>

      {!rowPk && <Empty description="row PK 를 입력하세요" />}

      {rowPk && (
        <>
          <Card title="시점 snapshot (선택한 시각 기준)">
            {snapshotQuery.error ? (
              <Alert type="error" message="조회 실패" description={String(snapshotQuery.error)} />
            ) : !snapshotQuery.data ? (
              <Text>로딩 중...</Text>
            ) : (
              <Space direction="vertical" style={{ width: '100%' }}>
                <Space>
                  <Tag color={snapshotQuery.data.rowExisted ? 'green' : 'red'}>
                    rowExisted: {String(snapshotQuery.data.rowExisted)}
                  </Tag>
                  {snapshotQuery.data.lastAction && (
                    <Tag color="blue">action: {snapshotQuery.data.lastAction}</Tag>
                  )}
                  {snapshotQuery.data.capturedAt && (
                    <Text type="secondary">
                      capturedAt: {dayjs(snapshotQuery.data.capturedAt).format('YYYY-MM-DD HH:mm:ss')}
                    </Text>
                  )}
                </Space>
                <Paragraph>
                  <pre
                    style={{
                      background: '#fafafa',
                      padding: 12,
                      maxHeight: 300,
                      overflow: 'auto',
                      fontSize: 12,
                    }}
                  >
                    {snapshotQuery.data.jsonPayload
                      ? JSON.stringify(JSON.parse(snapshotQuery.data.jsonPayload), null, 2)
                      : '(no payload — row 미존재 시점)'}
                  </pre>
                </Paragraph>
              </Space>
            )}
          </Card>

          <Divider orientation="left">전체 timeline (audit history, ASC)</Divider>
          {timelineQuery.error ? (
            <Alert type="error" message="timeline 조회 실패" description={String(timelineQuery.error)} />
          ) : !timelineQuery.data ? (
            <Text>로딩 중...</Text>
          ) : timelineQuery.data.length === 0 ? (
            <Empty description="audit 기록 없음" />
          ) : (
            <Timeline
              items={timelineQuery.data.map((t) => ({
                color:
                  t.action === 'INSERT' ? 'green' : t.action === 'DELETE' ? 'red' : 'blue',
                children: (
                  <Space direction="vertical" size={0}>
                    <Space>
                      <Tag>{t.action}</Tag>
                      <Text strong>{dayjs(t.occurred_at).format('YYYY-MM-DD HH:mm:ss')}</Text>
                      <Text type="secondary">actor={t.actor}</Text>
                    </Space>
                    {t.reason && <Text type="secondary">{t.reason}</Text>}
                  </Space>
                ),
              }))}
            />
          )}
        </>
      )}
    </Space>
  )
}
