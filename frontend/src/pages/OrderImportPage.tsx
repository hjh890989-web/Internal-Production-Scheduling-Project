import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Space,
  Tag,
  Typography,
  Upload,
  message,
  type UploadFile,
} from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { MappingReviewModal } from '@/features/order-import/components/MappingReviewModal'
import { useImportStatus } from '@/features/order-import/hooks/useImportStatus'
import { useAuthStore } from '@/stores/authStore'

const { Title, Paragraph } = Typography

/**
 * 수주 엑셀 import + 매핑 보정 entry page — TK-01-2-2.
 *
 * <p>흐름:
 * <ol>
 *   <li>Excel 파일 1~3개 선택 → POST /api/v1/orders/import</li>
 *   <li>trackingId 수신 → useImportStatus 폴링 5s</li>
 *   <li>status = MAPPED / REVIEW_REQUIRED → 모달 자동 노출</li>
 *   <li>모달에서 룰 보정 후 재시도 → status 재폴링</li>
 * </ol>
 */
export default function OrderImportPage() {
  const [trackingId, setTrackingId] = useState<string | undefined>()
  const [modalOpen, setModalOpen] = useState(false)
  const [uploading, setUploading] = useState(false)
  const { data: status } = useImportStatus(trackingId, !!trackingId)
  const token = useAuthStore((s) => s.token)

  useEffect(() => {
    if (status?.status === 'MAPPED' || status?.status === 'REVIEW_REQUIRED') {
      setModalOpen(true)
    }
  }, [status?.status])

  const handleUpload = async (files: UploadFile[]) => {
    if (files.length === 0) return
    if (files.length > 3) {
      message.error('최대 3 파일까지 동시 업로드 가능합니다.')
      return
    }
    setUploading(true)
    try {
      const form = new FormData()
      files.forEach((f) => {
        if (f.originFileObj) form.append('files', f.originFileObj)
      })
      const headers: Record<string, string> = {}
      if (token) headers.Authorization = `Bearer ${token}`
      const res = await fetch('/api/v1/orders/import', {
        method: 'POST',
        body: form,
        headers,
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = (await res.json()) as { trackingId: string }
      setTrackingId(data.trackingId)
      message.success(`업로드 접수: ${data.trackingId}`)
    } catch (e) {
      message.error(`업로드 실패: ${(e as Error).message}`)
    } finally {
      setUploading(false)
    }
  }

  return (
    <Space direction="vertical" size="middle" style={{ display: 'flex' }}>
      <Title level={3}>수주 통합 — Excel Import</Title>
      <Paragraph type="secondary">
        영업·관리 부서에서 받은 월별 예상 / 주간 계획 / 확정 / KD 발주 엑셀을 최대 3개 동시 업로드.
        매핑 실패율 1% 이상 시 검토 모달이 자동 노출됩니다 (REQ-FUNC-OC-004).
      </Paragraph>

      <Card title="파일 업로드">
        <Upload.Dragger
          name="files"
          accept=".xlsx"
          multiple
          maxCount={3}
          beforeUpload={() => false} // form submit 시점에 batch 전송
          onChange={(info) => handleUpload(info.fileList.slice(0, 3))}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">드래그하거나 클릭하여 업로드</p>
          <p className="ant-upload-hint">.xlsx · 최대 3 파일 · 각 20MB 이하</p>
        </Upload.Dragger>
      </Card>

      {trackingId && status && (
        <Card
          title={
            <Space>
              <span>추적 상태</span>
              <Tag color={statusColor(status.status)}>{status.status}</Tag>
            </Space>
          }
          extra={
            <Button
              type="primary"
              onClick={() => setModalOpen(true)}
              disabled={status.status !== 'MAPPED' && status.status !== 'REVIEW_REQUIRED'}
            >
              매핑 검토 열기
            </Button>
          }
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="추적 ID">{trackingId}</Descriptions.Item>
            <Descriptions.Item label="파일">{status.filenames.join(', ')}</Descriptions.Item>
            <Descriptions.Item label="시작">{status.startedAt}</Descriptions.Item>
            <Descriptions.Item label="갱신">{status.updatedAt}</Descriptions.Item>
            {status.error && (
              <Descriptions.Item label="오류">
                <Alert type="error" message={status.error} showIcon />
              </Descriptions.Item>
            )}
          </Descriptions>
        </Card>
      )}

      <MappingReviewModal
        trackingId={trackingId}
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onProceed={() => message.info('Sprint 1+ Diff 화면 (ST-03-1) — 후속 구현')}
      />
    </Space>
  )
}

function statusColor(s: string): string {
  switch (s) {
    case 'QUEUED':
    case 'PARSING':
    case 'MAPPING':
      return 'processing'
    case 'PARSED':
    case 'MAPPED':
      return 'success'
    case 'REVIEW_REQUIRED':
      return 'warning'
    case 'FAILED':
      return 'error'
    default:
      return 'default'
  }
}
