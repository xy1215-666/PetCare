import { describe, expect, it } from 'vitest'
import { dateAfter, isDueWithinDays, localDateString } from './recordDates'

describe('record date helpers', () => {
  it('formats dates in local calendar time and handles month/year boundaries', () => {
    expect(localDateString(new Date(2026, 9, 9, 23, 45))).toBe('2026-10-09')
    expect(dateAfter(1, '2026-12-31')).toBe('2027-01-01')
    expect(dateAfter(1, '2024-02-28')).toBe('2024-02-29')
  })

  it('includes today and the end of the reminder window but excludes overdue and later dates', () => {
    expect(isDueWithinDays('2026-10-09', 30, '2026-10-09')).toBe(true)
    expect(isDueWithinDays('2026-11-08', 30, '2026-10-09')).toBe(true)
    expect(isDueWithinDays('2026-10-08', 30, '2026-10-09')).toBe(false)
    expect(isDueWithinDays('2026-11-09', 30, '2026-10-09')).toBe(false)
    expect(isDueWithinDays(null, 30, '2026-10-09')).toBe(false)
  })
})
