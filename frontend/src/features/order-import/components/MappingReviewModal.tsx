import {
  Alert,
  Button,
  Modal,
  Progress,
  Space,
  Spin,
  Tabs,
  Table,
  Tag,
  message,
} from 'antd'
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons'
import { useMappingResult } from '../hooks/useImportStatus'
import { useRetryMapping } from '../hooks/useRetryMapping'
import { ConfidenceBadge } from './ConfidenceBadge'
import { FailureSummaryPanel } from './FailureSummaryPanel'
import { MappingRuleEditor } from './MappingRuleEditor'
import { successRate, type OrderDraft } from '../types/mapping'

interface Props {
  trackingId: string | undefined
  open: boolean
  onClose: () => void
  onProceed?: () => void
}

/**
 * 매핑 검토 + 보정 모달 — TK-01-2-2 (REQ-FUNC-OC-004).
 *
 * <p>3 탭:
 *  - 실패 — failures 사유별 그룹 (FailureSummaryPanel)
 *  - 매핑 룰 보정 — MappingRuleEditor (별칭 추가)
 *  - 성공 — successes 미리보기 (OrderDraft 표)
 *
 * <p>"재시도" 버튼 — useRetryMapping mutation → 모달 닫지 않고 데이터만 갱신 (세션 보존).
 * "다음: 변경 검토" — 실패 0 일 때만 활성, onProceed 호출.
 */
export function MappingReviewModal({ trackingId, open, onClose, onProceed }: Props) {
  const { data: result, isLoading, error } = useMappingResult(trackingId, open && !!trackingId)
  const retry = useRetryMapping(trackingId)

  const successCount = result?.successes.length ?? 0
  const failureCount = result?.failures.length ?? 0
  const total = successCount + failureCount
  const rate = result ? successRate(result) * 100 : 0
  const isCriticalFailure = result ? rate < 95 : false

  const handleRetry = () => {
    retry.mutate(undefined, {
      onSuccess: () => message.success('재매핑 트리거 — 결과 자동 갱신'),
      onError: (e) => message.error(`재시도 실패: ${(e as Error).message}`),
    })
  }

  return (
    <Modal
      title="수주 데이터 매핑 검토"
      open={open}
      onCancel={onClose}
      width={1100}
      destroyOnClose={false}
      footer={
        <Space>
          <Button onClick={onClose}>닫기</Button>
          <Button
            type="primary"
            danger={isCriticalFailure}
            loading={retry.isPending}
            onClick={handleRetry}
            disabled={!trackingId || failureCount === 0}
          >
            {isCriticalFailure ? '룰 보정 후 재시도' : '재시도'}
          </Button>
          <Button
            type="primary"
            disabled={failureCount > 0}
            onClick={onProceed}
          >
            다음: 변경 검토
          </Button>
        </Space>
      }
    >
      {isLoading && <Spin tip="매핑 결과 불러오는 중..." />}
      {error && (
        <Alert
          type="error"
          message="매핑 결과 조회 실패"
          description={String(error)}
          showIcon
        />
      )}
      {result && (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert
            message={
              <Space>
                <span>분류:</span>
                <ConfidenceBadge sourceType={result.sourceType} confidence={1.0} />
                <span>·</span>
                <span>매핑 성공률: {rate.toFixed(1)}%</span>
              </Space>
            }
            type={isCriticalFailure ? 'warning' : 'success'}
            showIcon
            icon={isCriticalFailure ? <WarningOutlined /> : <CheckCircleOutlined />}
          />
          <Progress
            percent={Math.round(rate)}
            status={isCriticalFailure ? 'exception' : 'success'}
            format={() => `${successCount}/${total} (${rate.toFixed(1)}%)`}
          />
          <Tabs
            defaultActiveKey={failureCount > 0 ? 'failures' : 'success'}
            items={[
              {
                key: 'failures',
                label: `실패 ${failureCount}건`,
                children: <FailureSummaryPanel failures={result.failures} />,
                disabled: failureCount === 0,
              },
              {
                key: 'rules',
                label: '매핑 룰 보정',
                children: <MappingRuleEditor sourceType={result.sourceType} />,
              },
              {
                key: 'success',
                label: `성공 ${successCount}건`,
                children: <SuccessPreviewTable successes={result.successes} />,
              },
            ]}
          />
        </Space>
      )}
    </Modal>
  )
}

function SuccessPreviewTable({ successes }: { successes: OrderDraft[] }) {
  return (
    <Table<OrderDraft>
      dataSource={successes}
      rowKey="orderId"
      size="small"
      pagination={{ pageSize: 20, showSizeChanger: false }}
      columns={[
        { title: '품번', dataIndex: 'hoseId', width: 160 },
        { title: '납기일', dataIndex: 'deliveryDate', width: 120 },
        { title: '수량', dataIndex: 'qty', width: 80 },
        {
          title: '구분',
          dataIndex: 'orderType',
          width: 100,
          render: (v: string) => <Tag>{v}</Tag>,
        },
        { title: '거래처', dataIndex: 'customer' },
      ]}
    />
  )
}
