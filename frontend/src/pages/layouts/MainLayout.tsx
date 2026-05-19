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
    { key: '/orders', label: t('menu.orders'), disabled: true },
    { key: '/vc', label: t('menu.vc'), disabled: true },
    { key: '/ex', label: t('menu.ex'), disabled: true },
    { key: '/master', label: t('menu.master'), disabled: true },
    { key: '/audit', label: t('menu.audit'), disabled: true },
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
