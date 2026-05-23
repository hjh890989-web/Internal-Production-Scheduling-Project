import { describe, it, expect } from 'vitest'
import ko from '../locales/ko.json'
import en from '../locales/en.json'

/**
 * EP-43 — i18n locale 정합성 (REQ-NF-USA-003).
 *
 * <p>ko ↔ en 키 1:1 매핑 검증. 누락 시 fallback 발동되지만 사용자 혼란.
 */
function flatKeys(obj: unknown, prefix = ''): string[] {
  if (typeof obj !== 'object' || obj === null) return [prefix]
  const out: string[] = []
  for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
    const path = prefix ? `${prefix}.${k}` : k
    if (typeof v === 'object' && v !== null) {
      out.push(...flatKeys(v, path))
    } else {
      out.push(path)
    }
  }
  return out
}

describe('i18n locales', () => {
  it('ko keys === en keys (1:1 매핑 + drift 0)', () => {
    const koKeys = flatKeys(ko).sort()
    const enKeys = flatKeys(en).sort()
    expect(enKeys).toEqual(koKeys)
  })

  it('ko 모든 값은 한국어 (영문 포함 가능 — title.subtitle 같은 영문 부제 허용)', () => {
    const enChar = /^[\x20-\x7E]+$/      // 순수 영문 검증 — 한국어가 하나도 없음
    const koValues = flatKeys(ko)
      .map((k) => k.split('.').reduce<unknown>((o, p) =>
        (o as Record<string, unknown>)[p], ko))
      .filter((v): v is string => typeof v === 'string')
    // subtitle 등 일부는 영문 허용 — 비-allowed 영문 only key 카운트
    const enOnly = koValues.filter(
      (v) => enChar.test(v) && !v.includes('Internal') && !v.includes('Production'))
    expect(enOnly.length).toBeLessThanOrEqual(2)   // app.subtitle 영문 허용
  })

  it('en common.save = "Save" (sanity)', () => {
    expect(en.common.save).toBe('Save')
    expect(en.menu.audit).toBe('Audit Log')
  })

  it('ko app.title = "사내 공정 스케줄링 시스템"', () => {
    expect(ko.app.title).toBe('사내 공정 스케줄링 시스템')
  })
})
