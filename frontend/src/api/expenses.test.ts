import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import {
  createExpense,
  deleteExpense,
  getExpenseCategoryStatistics,
  getExpense,
  getExpenseMonthlyTrend,
  getExpensePetStatistics,
  getExpenses,
  getMonthlyExpenseSummary,
  updateExpense,
} from './expenses'
import type { ExpensePayload, ExpenseRecord } from './expenses'

vi.mock('./http', () => ({
  http: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

const httpMock = vi.mocked(http)
const sampleRecord: ExpenseRecord = {
  id: 4, petId: 2, petName: '小白', category: 'FOOD', amount: 129.9,
  expenseDate: '2026-10-10', merchant: null, description: '猫粮', remark: null,
  createTime: '2026-10-10T08:00:00', updateTime: '2026-10-10T08:00:00',
}
const samplePayload: ExpensePayload = {
  petId: 2, category: 'FOOD', amount: 129.9, expenseDate: '2026-10-10', description: '猫粮',
}

describe('expense API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('reads filtered page and unwraps its response envelope', async () => {
    const page = { records: [sampleRecord], total: 1, page: 1, size: 20, totalPages: 1 }
    httpMock.get.mockResolvedValueOnce({ data: { data: page } } as never)
    await expect(getExpenses({ petId: 2, category: 'FOOD', page: 1 })).resolves.toEqual(page)
    expect(httpMock.get).toHaveBeenCalledWith('/expenses', { params: { petId: 2, category: 'FOOD', page: 1 } })
  })

  it('loads a single record by id', async () => {
    httpMock.get.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    await expect(getExpense(4)).resolves.toEqual(sampleRecord)
    expect(httpMock.get).toHaveBeenCalledWith('/expenses/4')
  })

  it('creates, updates, deletes and calls the statistics endpoints', async () => {
    httpMock.post.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.put.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.delete.mockResolvedValueOnce({ data: { data: null } } as never)
    httpMock.get.mockResolvedValue({ data: { data: [] } } as never)
    httpMock.get.mockResolvedValueOnce({ data: { data: { month: '2026-10' } } } as never)

    await expect(createExpense(samplePayload)).resolves.toEqual(sampleRecord)
    await expect(updateExpense(4, samplePayload)).resolves.toEqual(sampleRecord)
    await expect(deleteExpense(4)).resolves.toBeUndefined()
    await expect(getMonthlyExpenseSummary('2026-10', 2)).resolves.toMatchObject({ month: '2026-10' })
    await getExpenseCategoryStatistics('2026-10-01', '2026-10-31')
    await getExpenseMonthlyTrend(6)
    await getExpensePetStatistics()

    expect(httpMock.post).toHaveBeenCalledWith('/expenses', samplePayload)
    expect(httpMock.put).toHaveBeenCalledWith('/expenses/4', samplePayload)
    expect(httpMock.delete).toHaveBeenCalledWith('/expenses/4')
    expect(httpMock.get).toHaveBeenNthCalledWith(1, '/expenses/summary/monthly', {
      params: { month: '2026-10', petId: 2 },
    })
  })
})
