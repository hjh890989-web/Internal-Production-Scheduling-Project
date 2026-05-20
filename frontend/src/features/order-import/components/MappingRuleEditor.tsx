import { useEffect } from 'react'
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  message,
  Space,
  Spin,
  Tag,
} from 'antd'
import { CloseOutlined, PlusOutlined } from '@ant-design/icons'
import { useMappingRule, useUpdateMappingRule } from '../hooks/useMappingRules'
import type { MappingRule, SourceType } from '../types/mapping'

interface Props {
  sourceType?: SourceType
}

const FIELD_LABEL: Record<string, string> = {
  hose_id: '품번',
  delivery_date: '납기일',
  qty: '수량',
  customer: '거래처',
}

/**
 * 매핑 룰 인라인 편집 — TK-01-2-2.
 *
 * <p>현재 룰셋 조회 → 별칭 추가/삭제 → "룰 저장" 클릭 시 PUT 호출.
 * 저장 성공 시 toast + 모달 외부에서 "재시도" 버튼 활성.
 */
export function MappingRuleEditor({ sourceType }: Props) {
  const { data: rule, isLoading, error } = useMappingRule(sourceType)
  const update = useUpdateMappingRule(sourceType)
  const [form] = Form.useForm<MappingRule>()

  useEffect(() => {
    if (rule) form.setFieldsValue(rule)
  }, [rule, form])

  if (!sourceType || sourceType === 'UNRECOGNIZED') {
    return (
      <Alert
        type="info"
        message="분류된 SourceType 이 없어 룰 편집을 비활성화합니다 — 워크북 헤더가 모호하거나 재업로드 필요"
      />
    )
  }

  if (isLoading) return <Spin />
  if (error || !rule) {
    return <Alert type="error" message="룰셋 조회 실패" description={String(error)} />
  }

  const fieldEntries = Object.entries(rule.fields).sort(([a], [b]) => a.localeCompare(b))

  return (
    <Form<MappingRule> form={form} layout="vertical" initialValues={rule}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Alert
          type="info"
          showIcon
          message="별칭은 대소문자·공백 무시 — 사용자 워크북 헤더 셀과 매칭."
        />
        {fieldEntries.map(([field, mapping]) => (
          <Card
            key={field}
            size="small"
            title={FIELD_LABEL[field] ?? field}
            extra={mapping.required ? <Tag color="red">필수</Tag> : <Tag>선택</Tag>}
          >
            <Form.List name={['fields', field, 'aliases']}>
              {(items, { add, remove }) => (
                <Space wrap>
                  {items.map((it) => (
                    <Space.Compact key={it.key}>
                      <Form.Item {...it} noStyle>
                        <Input style={{ width: 220 }} aria-label={`${field}-alias`} />
                      </Form.Item>
                      <Button
                        icon={<CloseOutlined />}
                        onClick={() => remove(it.name)}
                        aria-label="별칭 삭제"
                      />
                    </Space.Compact>
                  ))}
                  <Button
                    icon={<PlusOutlined />}
                    onClick={() => add('')}
                    aria-label="별칭 추가"
                  >
                    별칭 추가
                  </Button>
                </Space>
              )}
            </Form.List>
          </Card>
        ))}
        <Button
          type="primary"
          loading={update.isPending}
          onClick={() => {
            form.validateFields().then((values) => {
              update.mutate(values, {
                onSuccess: () => {
                  message.success('룰 저장 완료. 재시도하면 적용됩니다.')
                },
                onError: (e) => {
                  message.error(`룰 저장 실패: ${(e as Error).message}`)
                },
              })
            })
          }}
        >
          룰 저장
        </Button>
      </Space>
    </Form>
  )
}
