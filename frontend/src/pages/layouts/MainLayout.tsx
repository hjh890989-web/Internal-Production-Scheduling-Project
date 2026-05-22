import { Layout, Menu, Typography } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useUIStore } from '@/stores/uiStore'

const { Header, Sider, Content } = Layout
const { Title } = Typography

/**
 * 공통 레이아웃 — Header (제목) + Sider (메뉴) + Content (Outlet).
 * Sprint 1+ UI Story에서 menu item 활성화 (disabled 제거).
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
    { key: '/extrusion-matrix', label: t('menu.ex') },
    { key: '/master', label: t('menu.master'), disabled: true },
    { key: '/audit/restore', label: t('menu.audit') },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
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
    </Layout>
  )
}
