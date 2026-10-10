import { describe, expect, it } from 'vitest'
import { healthStatusLabel } from './healthLabels'

describe('health status labels', () => {
  it('shows user-friendly Chinese labels for the controlled backend values', () => {
    expect(healthStatusLabel('appetite', 'EXCELLENT')).toBe('很好')
    expect(healthStatusLabel('waterIntake', 'REDUCED')).toBe('比平时少')
    expect(healthStatusLabel('energyLevel', 'VERY_LOW')).toBe('很差')
    expect(healthStatusLabel('activityLevel', 'HIGH')).toBe('活动较多')
    expect(healthStatusLabel('sleepStatus', 'UNKNOWN')).toBe('不确定')
    expect(healthStatusLabel('stoolStatus', 'CONSTIPATED')).toBe('便秘')
    expect(healthStatusLabel('urinationStatus', 'ABNORMAL')).toBe('有异常')
  })
})
