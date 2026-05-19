import type { ThemeConfig } from 'antd'

/**
 * Ant Design 5 theme token — 한글 친화 (Pretendard) + 사내 표준 색상.
 * Sprint 1+ 디자인 확정 후 brand 색상 갱신.
 */
export const theme: ThemeConfig = {
  token: {
    colorPrimary: '#1677ff',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',

    fontFamily:
      "'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif",
    fontSize: 14,
    fontSizeHeading1: 32,
    fontSizeHeading2: 24,

    borderRadius: 6,
    wireframe: false,
  },
  components: {
    Layout: {
      bodyBg: '#f5f5f5',
      headerBg: '#001529',
    },
    Table: {
      headerBg: '#fafafa',
      rowHoverBg: '#e6f4ff',
    },
  },
}
