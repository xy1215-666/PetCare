import type { WeightRecord } from '../api/weightRecords'

export interface WeightChartPoint {
  date: string
  weight: number
}

function ascendingByDate(records: readonly WeightRecord[]): WeightRecord[] {
  return [...records].sort((left, right) =>
    left.recordDate.localeCompare(right.recordDate) || left.id - right.id,
  )
}

export function weightChartPoints(records: readonly WeightRecord[]): WeightChartPoint[] {
  return ascendingByDate(records).map(({ recordDate, weight }) => ({ date: recordDate, weight }))
}

export function latestWeightChange(records: readonly WeightRecord[]): number | null {
  const ordered = ascendingByDate(records)
  if (ordered.length < 2) return null
  return Math.round((ordered.at(-1)!.weight - ordered.at(-2)!.weight) * 1000) / 1000
}

export function formatWeightChange(change: number): string {
  const prefix = change > 0 ? '+' : change < 0 ? '−' : ''
  return `${prefix}${Number(Math.abs(change).toFixed(3))} kg`
}
