import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import {
  createWeightRecord,
  deleteWeightRecord,
  getWeightRecords,
  updateWeightRecord,
} from './weightRecords'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const httpMock = vi.mocked(http)
const sampleRecord = {
  id: 12,
  petId: 8,
  weight: 4.7,
  recordDate: '2026-10-01',
  remark: null,
  createTime: '2026-10-01T08:00:00',
  updateTime: '2026-10-01T08:00:00',
}

describe('weight record API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads dated history from the pet-scoped route', async () => {
    httpMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'success', data: [sampleRecord] } } as never)
    await expect(getWeightRecords(8)).resolves.toEqual([sampleRecord])
    expect(httpMock.get).toHaveBeenCalledWith('/pets/8/weight-records')
  })

  it('creates, edits, and deletes weight entries through the pet-scoped routes', async () => {
    const payload = { recordDate: '2026-10-01', weight: 4.7 }
    httpMock.post.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.put.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.delete.mockResolvedValueOnce({ data: { data: null } } as never)

    await expect(createWeightRecord(8, payload)).resolves.toEqual(sampleRecord)
    await expect(updateWeightRecord(8, 12, payload)).resolves.toEqual(sampleRecord)
    await expect(deleteWeightRecord(8, 12)).resolves.toBeUndefined()
    expect(httpMock.post).toHaveBeenCalledWith('/pets/8/weight-records', payload)
    expect(httpMock.put).toHaveBeenCalledWith('/pets/8/weight-records/12', payload)
    expect(httpMock.delete).toHaveBeenCalledWith('/pets/8/weight-records/12')
  })
})
