import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import ko from './locales/ko.json'

/**
 * i18next 초기화 — 한국어 단일 (REQ-NF-USA-003).
 * 향후 영문 추가 시 resources 에 en 추가.
 */
void i18n.use(initReactI18next).init({
  resources: {
    ko: { translation: ko },
  },
  lng: 'ko',
  fallbackLng: 'ko',
  interpolation: {
    escapeValue: false, // React 가 자체 escape
  },
  react: {
    useSuspense: false,
  },
})

export default i18n
