import { createBrowserRouter, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { Spin } from 'antd'
import MainLayout from '@/pages/layouts/MainLayout'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { RoleGuard } from '@/routes/RoleGuard'

const HomePage = lazy(() => import('@/pages/HomePage'))
const LoginPage = lazy(() => import('@/pages/LoginPage'))
const ForbiddenPage = lazy(() => import('@/pages/ForbiddenPage'))
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'))
const OrderImportPage = lazy(() => import('@/pages/OrderImportPage'))
const VcSimulationPage = lazy(() => import('@/pages/VcSimulationPage'))
const ExMatrixPage = lazy(() => import('@/pages/ExMatrixPage'))
const MasterRestorePage = lazy(() => import('@/pages/MasterRestorePage'))
const CapacityQueuePage = lazy(() => import('@/pages/CapacityQueuePage'))
// Sprint 12 EP-MASTER-UI — IT_OPS 권한 마스터 페이지
const MasterHubPage = lazy(() => import('@/pages/master/MasterHubPage'))
const UserAdminPage = lazy(() => import('@/pages/master/UserAdminPage'))

const fallback = (
  <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
    <Spin size="large" />
  </div>
)

/**
 * React Router 6 데이터 라우터.
 * Sprint 10 EP-AUTH — /login 단독 라우트 + 나머지는 ProtectedRoute 로 wrap.
 * 모든 페이지는 React.lazy + Suspense 로 code splitting (NFR-PER-005).
 */
export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <Suspense fallback={fallback}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    errorElement: (
      <Suspense fallback={fallback}>
        <NotFoundPage />
      </Suspense>
    ),
    children: [
      { index: true, element: <Navigate to="/home" replace /> },
      {
        path: 'home',
        element: (
          <Suspense fallback={fallback}>
            <HomePage />
          </Suspense>
        ),
      },
      {
        path: 'orders/import',
        element: (
          <Suspense fallback={fallback}>
            <OrderImportPage />
          </Suspense>
        ),
      },
      {
        path: 'vc/simview',
        element: (
          <Suspense fallback={fallback}>
            <VcSimulationPage />
          </Suspense>
        ),
      },
      {
        path: 'vc/capacity-queue',
        element: (
          <RoleGuard roles={['PLANNER', 'IT_OPS', 'READ_ONLY']}>
            <Suspense fallback={fallback}>
              <CapacityQueuePage />
            </Suspense>
          </RoleGuard>
        ),
      },
      {
        path: 'extrusion-matrix',
        element: (
          <Suspense fallback={fallback}>
            <ExMatrixPage />
          </Suspense>
        ),
      },
      {
        path: 'audit/restore',
        element: (
          <RoleGuard roles={['PLANNER', 'IT_OPS', 'READ_ONLY']}>
            <Suspense fallback={fallback}>
              <MasterRestorePage />
            </Suspense>
          </RoleGuard>
        ),
      },
      {
        path: 'forbidden',
        element: (
          <Suspense fallback={fallback}>
            <ForbiddenPage />
          </Suspense>
        ),
      },
      // Sprint 12 EP-MASTER-UI — IT_OPS 전용
      {
        path: 'master',
        element: (
          <RoleGuard roles={['IT_OPS']}>
            <Suspense fallback={fallback}>
              <MasterHubPage />
            </Suspense>
          </RoleGuard>
        ),
      },
      {
        path: 'master/user',
        element: (
          <RoleGuard roles={['IT_OPS']}>
            <Suspense fallback={fallback}>
              <UserAdminPage />
            </Suspense>
          </RoleGuard>
        ),
      },
      {
        path: '*',
        element: (
          <Suspense fallback={fallback}>
            <NotFoundPage />
          </Suspense>
        ),
      },
    ],
  },
])
