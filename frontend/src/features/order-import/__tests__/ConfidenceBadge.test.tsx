import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { ConfidenceBadge } from '../components/ConfidenceBadge'

describe('ConfidenceBadge — TK-01-2-2', () => {
  it('confidence ≥ 0.7 → green Tag + 한국어 라벨', () => {
    const { container } = render(<ConfidenceBadge sourceType="MONTHLY_FORECAST" confidence={0.95} />)
    const tag = container.querySelector('.ant-tag')
    expect(tag?.className).toContain('green')
    expect(tag?.textContent).toContain('월별 예상')
    expect(tag?.textContent).toContain('95%')
  })

  it('confidence 0.5~0.7 → orange Tag', () => {
    const { container } = render(<ConfidenceBadge sourceType="WEEKLY_PLAN" confidence={0.6} />)
    expect(container.querySelector('.ant-tag')?.className).toContain('orange')
  })

  it('confidence < 0.5 → red Tag', () => {
    const { container } = render(<ConfidenceBadge sourceType="UNRECOGNIZED" confidence={0.3} />)
    expect(container.querySelector('.ant-tag')?.className).toContain('red')
  })

  it('sourceType 미지정 → 미분류 라벨', () => {
    const { container } = render(<ConfidenceBadge confidence={0} />)
    expect(container.textContent).toContain('미분류')
  })

  it('5 SourceType 모두 한국어 라벨 매핑', () => {
    const map: Array<['MONTHLY_FORECAST' | 'WEEKLY_PLAN' | 'CONFIRMED_ORDER' | 'KD_ORDER', string]> = [
      ['MONTHLY_FORECAST', '월별 예상'],
      ['WEEKLY_PLAN', '주간 계획'],
      ['CONFIRMED_ORDER', '확정 발주'],
      ['KD_ORDER', 'KD 발주'],
    ]
    map.forEach(([st, label]) => {
      const { container } = render(<ConfidenceBadge sourceType={st} confidence={0.9} />)
      expect(container.textContent).toContain(label)
    })
  })
})
