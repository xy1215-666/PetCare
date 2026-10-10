import { http, type ApiResponse } from './http'

export type ExpenseCategory =
  | 'FOOD'
  | 'MEDICAL'
  | 'VACCINE'
  | 'DEWORMING'
  | 'CLEANING'
  | 'TOY'
  | 'DAILY_SUPPLIES'
  | 'OTHER'

export interface ExpenseRecord {
  id: number
  petId: number | null
  petName: string | null
  category: ExpenseCategory
  amount: number
  expenseDate: string
  merchant: string | null
  description: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface ExpensePayload {
  petId: number | null
  category: ExpenseCategory
  amount: number
  expenseDate: string
  merchant?: string
  description?: string
  remark?: string
}

export interface ExpenseFilters {
  petId?: number
  category?: ExpenseCategory
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface ExpensePage {
  records: ExpenseRecord[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface ExpenseSummary {
  month: string
  totalAmount: number
  recordCount: number
  previousMonthAmount: number
  changeRate: number | null
}

export interface ExpenseCategoryTotal {
  category: ExpenseCategory
  amount: number
}

export interface ExpenseMonthTotal {
  month: string
  amount: number
}

export interface ExpensePetTotal {
  petId: number | null
  petName: string
  amount: number
}

export async function getExpenses(filters: ExpenseFilters = {}): Promise<ExpensePage> {
  const response = await http.get<ApiResponse<ExpensePage>>('/expenses', { params: filters })
  return response.data.data
}

export async function getExpense(expenseId: number): Promise<ExpenseRecord> {
  const response = await http.get<ApiResponse<ExpenseRecord>>(`/expenses/${expenseId}`)
  return response.data.data
}

export async function createExpense(payload: ExpensePayload): Promise<ExpenseRecord> {
  const response = await http.post<ApiResponse<ExpenseRecord>>('/expenses', payload)
  return response.data.data
}

export async function updateExpense(expenseId: number, payload: ExpensePayload): Promise<ExpenseRecord> {
  const response = await http.put<ApiResponse<ExpenseRecord>>(`/expenses/${expenseId}`, payload)
  return response.data.data
}

export async function deleteExpense(expenseId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/expenses/${expenseId}`)
}

export async function getMonthlyExpenseSummary(month: string, petId?: number): Promise<ExpenseSummary> {
  const response = await http.get<ApiResponse<ExpenseSummary>>('/expenses/summary/monthly', {
    params: { month, ...(petId === undefined ? {} : { petId }) },
  })
  return response.data.data
}

export async function getExpenseCategoryStatistics(
  startDate: string,
  endDate: string,
  petId?: number,
): Promise<ExpenseCategoryTotal[]> {
  const response = await http.get<ApiResponse<ExpenseCategoryTotal[]>>('/expenses/statistics/categories', {
    params: { startDate, endDate, ...(petId === undefined ? {} : { petId }) },
  })
  return response.data.data
}

export async function getExpenseMonthlyTrend(months = 6, petId?: number): Promise<ExpenseMonthTotal[]> {
  const response = await http.get<ApiResponse<ExpenseMonthTotal[]>>('/expenses/statistics/monthly-trend', {
    params: { months, ...(petId === undefined ? {} : { petId }) },
  })
  return response.data.data
}

export async function getExpensePetStatistics(): Promise<ExpensePetTotal[]> {
  const response = await http.get<ApiResponse<ExpensePetTotal[]>>('/expenses/statistics/by-pet')
  return response.data.data
}
