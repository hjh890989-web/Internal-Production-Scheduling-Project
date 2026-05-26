import { Layout, Menu, Typography } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useUIStore } from '@/stores/uiStore'

const { Header, Sider, Content, Footer } = Layout
const { Title, Text } = Typography

/**
 * 공통 레이아웃 — Header (로고 + 제목) + Sider (메뉴) + Content (Outlet) + Footer (sub 로고).
 *
 * <p>FCB (Fuel-Cost-Automatic-Billing-Project) 패턴 적용 — top nav main 로고 + footer EVS 로고.
 * 로고 source — public/logos/ (Vite static path, build 시 dist/logos/ 그대로 copy).
 */
export default function MainLayout() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const collapsed = useUIStore((s) => s.sidebarCollapsed)

  const menuItems = [
    { key: '/home', label: t('menu.home') },
    { key: '/orders/import', label: t('menu.orders') },
    { key: '/vc/simview', label: t('menu.vc') },
    { key: '/vc/capacity-queue', label: t('menu.capacityQueue') },
    { key: '/extrusion-matrix', label: t('menu.ex') },
    { key: '/master', label: t('menu.master'), disabled: true },
    { key: '/audit/restore', label: t('menu.audit') },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <a
          href="/home"
          onClick={(e) => { e.preventDefault(); navigate('/home') }}
          title="홈으로 — Check In · 사내 공정 스케줄링"
          style={{ display: 'flex', alignItems: 'center', textDecoration: 'none' }}
        >
          <img
            src="/logos/cheek-in-main-logo.svg"
            alt="Check In · 사내 공정 스케줄링"
            style={{ height: 36, background: '#fff', padding: '4px 8px', borderRadius: 4 }}
          />
        </a>
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          {t('app.title')}
        </Title>
      </Header>
      <Layout>
        <Sider collapsed={collapsed} width={220} theme="light">
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            onClick={(e) => navigate(e.key)}
            items={menuItems}
          />
        </Sider>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
      <Footer style={{ textAlign: 'center', background: '#f5f5f5', padding: '16px 24px' }}>
        <img
          src="/logos/cheek-in-evs-logo.svg"
          alt="Check In · EVS"
          style={{ height: 28, marginBottom: 8 }}
        />
        <div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            송우산업 사내 공정 스케줄링 시스템 (S-D 베타 · Hybrid Dev Mode)
          </Text>
        </div>
      </Footer>
    </Layout>
  )
}
