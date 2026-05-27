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
 * <p>마스터 데이터 4 카드 진입점 — 사용자/우선순위/KD/품번. Sprint 14 EP-VC-FULL 진입 시
 * 장비/셋팅그룹 등 추가. RoleGuard IT_OPS 가 상위 router 에서 처리.
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
      description: 'Sprint 14 EP-VC-FULL 진입 후 활성',
      path: '/master/machine',
      enabled: false,
    },
    {
      key: 'setting-group',
      title: '셋팅 그룹 + 합금형',
      description: 'Sprint 14 EP-VC-FULL 진입 후 활성',
      path: '/master/setting-group',
      enabled: false,
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
