import type {
  ExpenseCategory,
  ExpenseCategoryTotal,
  ExpenseMonthTotal,
  ExpensePetTotal,
} from '../api/expenses'

export const expenseCategories: readonly { value: ExpenseCategory; label: string; icon: string; color: string }[] = [
  { value: 'FOOD', label: '食品', icon: '🍚', color: '#e59c62' },
  { value: 'MEDICAL', label: '医疗', icon: '♡', color: '#d87f79' },
  { value: 'VACCINE', label: '疫苗', icon: '✦', color: '#8c9bc7' },
  { value: 'DEWORMING', label: '驱虫', icon: '✧', color: '#88a77b' },
  { value: 'CLEANING', label: '洗护', icon: '☼', color: '#74a6aa' },
  { value: 'TOY', label: '玩具', icon: '♡', color: '#bd8eb7' },
  { value: 'DAILY_SUPPLIES', label: '日用品', icon: '⌂', color: '#d2ae67' },
  { value: 'OTHER', label: '其他', icon: '·', color: '#9aa69b' },
]

const categoryByValue = new Map(expenseCategories.map((category) => [category.value, category]))

export interface ExpenseChartPoint {
  name: string
  value: number
  category?: ExpenseCategory
  month?: string
  petId?: number | null
}

function toCents(amount: number | string): number {
  const text = String(amount).trim()
  const matched = /^(-?)(\d+)(?:\.(\d*))?$/.exec(text)
  if (!matched) {
    const parsed = Number(text)
    return Number.isFinite(parsed) ? Math.round(parsed * 100) : 0
  }

  const [, sign, whole, fraction = ''] = matched
  let cents = BigInt(whole) * 100n + BigInt((fraction + '00').slice(0, 2))
  if (fraction.length > 2 && Number(fraction[2]) >= 5) cents += 1n
  const signed = sign === '-' ? -cents : cents
  const number = Number(signed)
  return Number.isSafeInteger(number) ? number : 0
}

export function formatCny(amount: number | string): string {
  const cents = toCents(amount)
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(cents / 100)
}

export function expenseCategoryLabel(category: string): string {
  return categoryByValue.get(category as ExpenseCategory)?.label ?? '其他'
}

export function categoryChartPoints(totals: readonly ExpenseCategoryTotal[]): ExpenseChartPoint[] {
  return totals.map(({ category, amount }) => ({
    name: expenseCategoryLabel(category),
    value: toCents(amount) / 100,
    category,
  }))
}

export function monthlyTrendChartPoints(totals: readonly ExpenseMonthTotal[]): ExpenseChartPoint[] {
  return [...totals]
    .sort((left, right) => left.month.localeCompare(right.month))
    .map(({ month, amount }) => ({ month, name: month, value: toCents(amount) / 100 }))
}

export function petExpenseChartPoints(totals: readonly ExpensePetTotal[]): ExpenseChartPoint[] {
  return totals.map(({ petId, petName, amount }) => ({
    name: petName,
    petId,
    value: toCents(amount) / 100,
  }))
}

export function categoryColor(category: ExpenseCategory): string {
  return categoryByValue.get(category)?.color ?? '#9aa69b'
}

export function categoryIcon(category: ExpenseCategory): string {
  return categoryByValue.get(category)?.icon ?? '·'
}
