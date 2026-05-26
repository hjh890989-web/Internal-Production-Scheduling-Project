import { Layout, Button, Typography } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuthStore } from '@/stores/authStore'

const { Header, Content, Footer } = Layout
const { Text } = Typography

/**
 * 공통 레이아웃 — Header (서브 로고 + 박스 메뉴) + Content (Outlet) + Footer (메인 로고).
 *
 * <p>FCB (Fuel-Cost-Automatic-Billing-Project) 패턴 정합 — 메뉴 아이템 다크 그라디언트 박스
 * (border + radius + hover transform). 좌측 Sider 폐지.
 */
export default function MainLayout() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const menuItems = [
    { key: '/home', label: t('menu.home') },
    { key: '/orders/import', label: t('menu.orders') },
    { key: '/vc/simview', label: t('menu.vc') },
    { key: '/vc/capacity-queue', label: t('menu.capacityQueue') },
    { key: '/extrusion-matrix', label: t('menu.ex') },
    { key: '/master', label: t('menu.master'), disabled: true },
    { key: '/audit/restore', label: t('menu.audit') },
  ]

  const buttonBaseStyle: React.CSSProperties = {
    background: 'linear-gradient(180deg, #374151 0%, #1f2937 100%)',
    borderColor: '#4b5563',
    color: '#e5e7eb',
    fontSize: 13,
    fontWeight: 500,
    borderRadius: 6,
  }

  const buttonSelectedStyle: React.CSSProperties = {
    ...buttonBaseStyle,
    background: 'linear-gradient(180deg, #4b5563 0%, #374151 100%)',
    borderColor: '#9ca3af',
    color: '#fff',
    boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.12), 0 2px 4px rgba(0,0,0,0.4)',
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '0 24px', height: 80 }}>
        <a
          href="/home"
          onClick={(e) => { e.preventDefault(); navigate('/home') }}
          title="홈으로 — Check In · EVS (사내 공정 스케줄링)"
          style={{ display: 'flex', alignItems: 'center', textDecoration: 'none', flexShrink: 0 }}
        >
          <img
            src="/logos/cheek-in-evs-logo.svg"
            alt="Check In · EVS (사내 공정 스케줄링)"
            style={{ height: 60, background: '#fff', padding: '4px 8px', borderRadius: 4 }}
          />
        </a>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, flex: 1, alignItems: 'center' }}>
          {menuItems.map((item) => {
            const selected = location.pathname === item.key
            return (
              <Button
                key={item.key}
                disabled={item.disabled}
                onClick={() => navigate(item.key)}
                style={selected ? buttonSelectedStyle : buttonBaseStyle}
              >
                {item.label}
              </Button>
            )
          })}
        </div>
        {user && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexShrink: 0 }}>
            <Text style={{ color: '#d1d5db', fontSize: 13 }}>
              {user.employeeId} ({user.role})
            </Text>
            <Button
              size="small"
              onClick={handleLogout}
              style={{
                background: 'linear-gradient(180deg, #7f1d1d 0%, #450a0a 100%)',
                borderColor: '#b91c1c',
                color: '#fecaca',
                fontSize: 12,
                fontWeight: 500,
                borderRadius: 6,
              }}
            >
              로그아웃
            </Button>
          </div>
        )}
      </Header>
      <Content style={{ padding: 24 }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center', background: '#f5f5f5', padding: '16px 24px' }}>
        <img
          src="/logos/cheek-in-main-logo.svg"
          alt="Check In"
          style={{ height: 70, marginBottom: 8 }}
        />
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            송우산업 사내 업무 자동화 플랫폼
          </Text>
        </div>
      </Footer>
    </Layout>
  )
}
