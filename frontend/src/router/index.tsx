import { createBrowserRouter, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { Spin } from 'antd'
import MainLayout from '@/pages/layouts/MainLayout'

const HomePage = lazy(() => import('@/pages/HomePage'))
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'))
const OrderImportPage = lazy(() => import('@/pages/OrderImportPage'))

const fallback = (
  <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
    <Spin size="large" />
  </div>
)

/**
 * React Router 6 데이터 라우터.
 * Sprint 1+ UI Story 추가 시 children 에 라우트 추가 (예: /orders, /vc, /ex, /master, /audit).
 * 모든 페이지는 React.lazy + Suspense 로 code splitting (NFR-PER-005).
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
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
