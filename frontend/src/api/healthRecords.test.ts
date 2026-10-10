import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import {
  createHealthRecord,
  deleteHealthRecord,
  getHealthRecord,
  getHealthRecords,
  updateHealthRecord,
} from './healthRecords'
import type { HealthRecord, HealthRecordPayload } from './healthRecords'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const httpMock = vi.mocked(http)

const sampleRecord: HealthRecord = {
  id: 31,
  petId: 17,
  recordDate: '2026-10-09',
  appetite: 'NORMAL',
  waterIntake: 'NORMAL',
  energyLevel: 'ACTIVE',
  activityLevel: 'HIGH',
  sleepStatus: 'NORMAL',
  stoolStatus: 'NORMAL',
  urinationStatus: 'NORMAL',
  abnormal: false,
  abnormalDescription: null,
  remark: null,
  createTime: '2026-10-09T08:00:00',
  updateTime: '2026-10-09T08:00:00',
}

const samplePayload: HealthRecordPayload = {
  recordDate: sampleRecord.recordDate,
  appetite: sampleRecord.appetite,
  waterIntake: sampleRecord.waterIntake,
  energyLevel: sampleRecord.energyLevel,
  activityLevel: sampleRecord.activityLevel,
  sleepStatus: sampleRecord.sleepStatus,
  stoolStatus: sampleRecord.stoolStatus,
  urinationStatus: sampleRecord.urinationStatus,
  abnormal: false,
}

describe('health record API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('reads health record list and detail and unwraps the API envelope', async () => {
    httpMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'success', data: [sampleRecord] } } as never)
    httpMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'success', data: sampleRecord } } as never)

    await expect(getHealthRecords(17)).resolves.toEqual([sampleRecord])
    await expect(getHealthRecord(17, 31)).resolves.toEqual(sampleRecord)
    expect(httpMock.get).toHaveBeenNthCalledWith(1, '/pets/17/health-records')
    expect(httpMock.get).toHaveBeenNthCalledWith(2, '/pets/17/health-records/31')
  })

  it('creates, updates, and deletes a health record using the pet-scoped routes', async () => {
    httpMock.post.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.put.mockResolvedValueOnce({ data: { data: sampleRecord } } as never)
    httpMock.delete.mockResolvedValueOnce({ data: { data: null } } as never)

    await expect(createHealthRecord(17, samplePayload)).resolves.toEqual(sampleRecord)
    await expect(updateHealthRecord(17, 31, samplePayload)).resolves.toEqual(sampleRecord)
    await expect(deleteHealthRecord(17, 31)).resolves.toBeUndefined()
    expect(httpMock.post).toHaveBeenCalledWith('/pets/17/health-records', samplePayload)
    expect(httpMock.put).toHaveBeenCalledWith('/pets/17/health-records/31', samplePayload)
    expect(httpMock.delete).toHaveBeenCalledWith('/pets/17/health-records/31')
  })
})
