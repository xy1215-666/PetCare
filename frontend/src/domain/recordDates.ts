export function localDateString(date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function dateAfter(days: number, baseDate = localDateString()): string {
  const [year, month, day] = baseDate.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  date.setDate(date.getDate() + days)
  return localDateString(date)
}

export function isDueWithinDays(dueDate: string | null, days: number, today = localDateString()): boolean {
  return dueDate !== null && dueDate >= today && dueDate <= dateAfter(days, today)
}
