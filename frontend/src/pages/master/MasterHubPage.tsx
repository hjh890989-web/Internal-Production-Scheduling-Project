import { Card, Col, Row, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'

const { Title, Paragraph } = Typography

interface MasterCard {
  key: string
  title: string
  description: string
  path: string
  enabled: boolean
}

/**
 * Sprint 12 EP-MASTER-UI Hub 페이지 (TK-MASTER-1-1, IT_OPS 권한).
 *
 * <p>마스터 데이터 진입점. Sprint 21 EP-CRUD-MASTER-2 (ST-CRUD-6) 에서 장비/셋팅그룹/
 * 합금형 제약/라인/휴일 5 entity 카드 활성 — IT_OPS 마스터 전체 자체 운영.
 * RoleGuard IT_OPS 가 상위 router 에서 처리.
 */
export default function MasterHubPage() {
  const navigate = useNavigate()

  const cards: MasterCard[] = [
    {
      key: 'user',
      title: '사용자 관리',
      description: '사번/PIN/role + 잠금 해제 + 신규 사용자 추가 (NFR-SEC-007)',
      path: '/master/user',
      enabled: true,
    },
    {
      key: 'product-priority',
      title: '우선순위 (PRODUCT_PRIORITY)',
      description: 'BR-V12 capacity overflow 시 사용 (Sprint 7 V033)',
      path: '/master/product-priority',
      enabled: true,
    },
    {
      key: 'kd-order',
      title: 'KD 발주 (KD_ORDER)',
      description: 'BR-V13 KD 보충 시 사용 (Sprint 7 V033)',
      path: '/master/kd-order',
      enabled: true,
    },
    {
      key: 'product-spec',
      title: '품번 (47 종)',
      description: '품번 spec 조회 (CRUD 는 Sprint 13)',
      path: '/master/product-spec',
      enabled: true,
    },
    {
      key: 'machine',
      title: '장비 (LP/IC)',
      description: 'LP-01~04 + IC-01 · 회전수(주/야) + active toggle (BR-V04)',
      path: '/master/machine',
      enabled: true,
    },
    {
      key: 'setting-group',
      title: '셋팅 그룹 (1~8)',
      description: 'setting_group 1~8 + active toggle (BR-V12/V13)',
      path: '/master/setting-group',
      enabled: true,
    },
    {
      key: 'vc-constraint',
      title: '성형 제약 + 합금형',
      description: '47 품번 composite_count(1·2·3·6) + slot 가용성 + mold_qty (BR-V14)',
      path: '/master/vc-constraint',
      enabled: true,
    },
    {
      key: 'line',
      title: '라인 (line_type)',
      description: 'line_type CRUD + product 호환 매핑',
      path: '/master/line',
      enabled: true,
    },
    {
      key: 'holiday',
      title: '휴일 (HOLIDAY)',
      description: '연도별 휴일 추가/삭제 · WorkingCalendar 정합 (BR-X04 KST)',
      path: '/master/holiday',
      enabled: true,
    },
  ]

  return (
    <div>
      <Title level={3}>마스터 데이터 관리</Title>
      <Paragraph type="secondary">
        IT 운영팀 전용 · 모든 변경은 audit_log 기록 (BR-X02) · 사번 + reason 자동 부착
      </Paragraph>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {cards.map((c) => (
          <Col key={c.key} xs={24} sm={12} md={8} lg={6}>
            <Card
              hoverable={c.enabled}
              title={c.title}
              onClick={() => c.enabled && navigate(c.path)}
              style={{
                cursor: c.enabled ? 'pointer' : 'not-allowed',
                opacity: c.enabled ? 1 : 0.5,
              }}
            >
              <Paragraph style={{ marginBottom: 0, fontSize: 13 }}>{c.description}</Paragraph>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}
