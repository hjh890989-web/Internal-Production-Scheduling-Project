// sockjs-client browser polyfill — Node.js `global` 참조 회피 (Vite 5 + sockjs 호환)
// main.tsx 최상단 — 다른 import 전에 적용. Vite 의 define option 은 pre-bundled deps 에
// 적용 안 됨, runtime polyfill 필요.
;(window as unknown as { global: typeof window }).global = window

import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'

import App from './App'
import './i18n'
import { theme } from './theme'
import { queryClient } from './queries/queryClient'
import './styles/global.css'

// dayjs 한국어 로케일 (Ant Design DatePicker 등 내부 사용)
dayjs.locale('ko')

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={koKR} theme={theme}>
      <QueryClientProvider client={queryClient}>
        <App />
        {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>,
)
