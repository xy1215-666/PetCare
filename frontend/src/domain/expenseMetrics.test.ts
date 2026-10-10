import { describe, expect, it } from 'vitest'
import {
  categoryChartPoints,
  expenseCategories,
  expenseCategoryLabel,
  formatCny,
  monthlyTrendChartPoints,
  petExpenseChartPoints,
} from './expenseMetrics'

describe('expense metrics', () => {
  it('maps every backend category to a friendly Chinese label', () => {
    expect(expenseCategories).toHaveLength(8)
    expect(expenseCategoryLabel('FOOD')).toBe('食品')
    expect(expenseCategoryLabel('DAILY_SUPPLIES')).toBe('日用品')
    expect(expenseCategoryLabel('unknown')).toBe('其他')
  })

  it('formats amounts to exact cents in CNY', () => {
    expect(formatCny(0.1 + 0.2)).toBe('¥0.30')
    expect(formatCny('129.90')).toBe('¥129.90')
    expect(formatCny('1000000')).toBe('¥1,000,000.00')
  })

  it('converts category and pet aggregates for charts', () => {
    expect(categoryChartPoints([{ category: 'MEDICAL', amount: 300 }])).toEqual([
      { name: '医疗', value: 300, category: 'MEDICAL' },
    ])
    expect(petExpenseChartPoints([{ petId: null, petName: '未指定宠物', amount: 50 }])).toEqual([
      { name: '未指定宠物', petId: null, value: 50 },
    ])
  })

  it('sorts monthly values chronologically before charting', () => {
    expect(monthlyTrendChartPoints([
      { month: '2026-10', amount: 12 },
      { month: '2026-08', amount: 7 },
      { month: '2026-09', amount: 0 },
    ])).toEqual([
      { month: '2026-08', name: '2026-08', value: 7 },
      { month: '2026-09', name: '2026-09', value: 0 },
      { month: '2026-10', name: '2026-10', value: 12 },
    ])
  })
})
