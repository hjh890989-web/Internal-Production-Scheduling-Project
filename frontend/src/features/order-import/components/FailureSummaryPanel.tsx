import { Collapse, Empty, Space, Table, Tag, Typography } from 'antd'
import type { FailureField, MappingFailure } from '../types/mapping'

const { Text } = Typography

interface Props {
  failures: MappingFailure[]
}

const FIELD_LABEL: Record<FailureField, string> = {
  hose_id: '품번',
  delivery_date: '납기일',
  qty: '수량',
  customer: '거래처',
  HEADER: '헤더 행 식별',
}

/** 실패 사유별 그룹화 + 페이지네이션 — TK-01-2-2. */
export function FailureSummaryPanel({ failures }: Props) {
  if (failures.length === 0) {
    return <Empty description="실패 없음 — 모든 row 가 매핑 성공" />
  }

  const grouped = failures.reduce<Record<string, MappingFailure[]>>((acc, f) => {
    const key = f.failedField
    acc[key] = acc[key] ?? []
    acc[key].push(f)
    return acc
  }, {})

  return (
    <Collapse
      defaultActiveKey={Object.keys(grouped)}
      items={Object.entries(grouped).map(([field, items]) => ({
        key: field,
        label: (
          <Space>
            <Text strong>{FIELD_LABEL[field as FailureField] ?? field}</Text>
            <Tag color="orange">{items.length}건</Tag>
          </Space>
        ),
        children: (
          <Table<MappingFailure>
            dataSource={items}
            rowKey={(r) => `${r.sheetName}-${r.rowIndex}-${r.failedField}`}
            size="small"
            pagination={{ pageSize: 10, showSizeChanger: false }}
            columns={[
              { title: '시트', dataIndex: 'sheetName', width: 140 },
              {
                title: '행',
                dataIndex: 'rowIndex',
                width: 80,
                render: (v: number) => v + 1,
              },
              {
                title: '원본 데이터 (앞 5 셀)',
                dataIndex: 'originalCells',
                render: (cells: string[]) =>
                  cells.slice(0, 5).map((c, i) => (
                    <Tag key={i} style={{ marginBottom: 4 }}>
                      {c || '∅'}
                    </Tag>
                  )),
              },
              { title: '사유', dataIndex: 'reason' },
            ]}
          />
        ),
      }))}
    />
  )
}
