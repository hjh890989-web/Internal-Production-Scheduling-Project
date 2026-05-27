import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Input, Space, Table, Tag, Typography } from 'antd'
import { productSpecApi, type SpecSummary } from '@/api/productSpecApi'

const { Title, Paragraph } = Typography

/**
 * Sprint 12 EP-MASTER-UI ST-MASTER-5 — 47 품번 read 페이지 (TK-MASTER-5-2).
 *
 * <p>4 role 모두 조회 가능 (CRUD 는 Sprint 13). 검색 — hose_id substring 필터.
 * Sprint 14 EP-VC-FULL 진입 시 AG Grid 로 교체 가능.
 */
export default function ProductSpecPage() {
  const [rows, setRows] = useState<SpecSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const reload = async () => {
    setLoading(true)
    setError(null)
    try { setRows(await productSpecApi.list()) }
    catch (e) { setError(e instanceof Error ? e.message : '조회 실패') }
    finally { setLoading(false) }
  }
  useEffect(() => { void reload() }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) => r.hoseId.toLowerCase().includes(q))
  }, [rows, search])

  const columns = [
    { title: 'Hose ID', dataIndex: 'hoseId', key: 'hoseId',
      sorter: (a: SpecSummary, b: SpecSummary) => a.hoseId.localeCompare(b.hoseId) },
    { title: '규격 (spec)', dataIndex: 'spec', key: 'spec',
      render: (v: number | null) => v ?? '—',
      sorter: (a: SpecSummary, b: SpecSummary) => (a.spec ?? 0) - (b.spec ?? 0) },
    { title: '합금형 개수', dataIndex: 'compositeCount', key: 'compositeCount',
      render: (v: number | null) => v ?? '—' },
    { title: '좌 셋팅', dataIndex: 'lpLeftSetting', key: 'lpLeftSetting',
      render: (v: string | null) => v ?? '—' },
    { title: '우 셋팅', dataIndex: 'lpRightSetting', key: 'lpRightSetting',
      render: (v: string | null) => v ?? '—' },
    { title: '앵글 개수', dataIndex: 'angleCount', key: 'angleCount' },
    {
      title: '규격<7 (BR-V17)', dataIndex: 'isSpecLt7', key: 'isSpecLt7',
      render: (b: boolean) => b ? <Tag color="orange">YES</Tag> : <Tag>—</Tag>,
    },
  ]

  return (
    <div>
      <Title level={3}>품번 (Product Spec) — 47 종</Title>
      <Paragraph type="secondary">
        조회 전용 · CRUD 는 Sprint 13 EP-OC-FULL (underlying VC_CONSTRAINT / EX_CONSTRAINT 변경)
      </Paragraph>

      {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}

      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="Hose ID 검색 (substring)"
          allowClear
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: 300 }}
        />
        <Button onClick={() => void reload()}>새로고침</Button>
        <span style={{ color: '#999', fontSize: 12 }}>총 {filtered.length} / {rows.length} 품번</span>
      </Space>

      <Table
        rowKey="hoseId"
        loading={loading}
        dataSource={filtered}
        columns={columns}
        pagination={{ pageSize: 25, showSizeChanger: true }}
        size="small"
      />
    </div>
  )
}
