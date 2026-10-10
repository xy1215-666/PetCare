import { describe, expect, it } from 'vitest'
import type { WeightRecord } from '../api/weightRecords'
import { formatWeightChange, latestWeightChange, weightChartPoints } from './weightMetrics'

const history: WeightRecord[] = [
  { id: 2, petId: 8, weight: 4.7, recordDate: '2026-10-01', remark: null, createTime: '', updateTime: '' },
  { id: 1, petId: 8, weight: 4.8, recordDate: '2026-09-01', remark: null, createTime: '', updateTime: '' },
]

describe('weight history calculations', () => {
  it('sorts dated entries for the chart and calculates change from the previous record', () => {
    expect(weightChartPoints(history)).toEqual([
      { date: '2026-09-01', weight: 4.8 },
      { date: '2026-10-01', weight: 4.7 },
    ])
    expect(latestWeightChange(history)).toBe(-0.1)
    expect(formatWeightChange(-0.1)).toBe('−0.1 kg')
  })

  it('does not report a change when there is only one weight entry', () => {
    expect(latestWeightChange([history[0]!])).toBeNull()
    expect(formatWeightChange(0.235)).toBe('+0.235 kg')
  })
})
