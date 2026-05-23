import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import ko from './locales/ko.json'
import en from './locales/en.json'

/**
 * i18next 초기화 — 한국어 기본 + 영문 fallback (REQ-NF-USA-003·004, EP-43).
 *
 * <p>Sprint 6 EP-43 — EN locale 추가. lng 는 localStorage `i18nextLng` 가 우선,
 * 미설정 시 navigator.language 기반 자동 감지 (ko-* → ko, 그 외 → en).
 */
const detectInitialLanguage = (): 'ko' | 'en' => {
  const stored = typeof localStorage !== 'undefined'
    ? localStorage.getItem('i18nextLng')
    : null
  if (stored === 'ko' || stored === 'en') return stored
  const nav = typeof navigator !== 'undefined' ? navigator.language : 'ko'
  return nav.startsWith('ko') ? 'ko' : 'en'
}

void i18n.use(initReactI18next).init({
  resources: {
    ko: { translation: ko },
    en: { translation: en },
  },
  lng: detectInitialLanguage(),
  fallbackLng: 'ko',
  interpolation: {
    escapeValue: false, // React 가 자체 escape
  },
  react: {
    useSuspense: false,
  },
})

export default i18n
