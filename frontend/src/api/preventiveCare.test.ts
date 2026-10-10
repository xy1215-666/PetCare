import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import { createVaccination, getVaccinations } from './vaccinations'
import { createDeworming, dewormingTypeLabels } from './dewormings'
import { createMedicalRecord } from './medicalRecords'
import { getUpcomingReminders } from './reminders'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const httpMock = vi.mocked(http)

describe('preventive care API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses pet-scoped routes and unwraps vaccination and deworming responses', async () => {
    const vaccination = { id: 1, petId: 7, vaccineName: '狂犬病疫苗', vaccinationDate: '2026-10-09', nextDate: '2027-10-09' }
    const deworming = { id: 2, petId: 7, type: 'EXTERNAL', medicineName: '滴剂', dewormingDate: '2026-10-09', nextDate: '2026-11-09' }
    httpMock.get.mockResolvedValueOnce({ data: { data: [vaccination] } } as never)
    httpMock.post.mockResolvedValueOnce({ data: { data: vaccination } } as never)
    httpMock.post.mockResolvedValueOnce({ data: { data: deworming } } as never)

    await expect(getVaccinations(7)).resolves.toEqual([vaccination])
    await expect(createVaccination(7, { vaccineName: '狂犬病疫苗', vaccinationDate: '2026-10-09' })).resolves.toEqual(vaccination)
    await expect(createDeworming(7, { type: 'EXTERNAL', dewormingDate: '2026-10-09' })).resolves.toEqual(deworming)
    expect(httpMock.get).toHaveBeenCalledWith('/pets/7/vaccinations')
    expect(httpMock.post).toHaveBeenNthCalledWith(1, '/pets/7/vaccinations', {
      vaccineName: '狂犬病疫苗', vaccinationDate: '2026-10-09',
    })
    expect(httpMock.post).toHaveBeenNthCalledWith(2, '/pets/7/dewormings', {
      type: 'EXTERNAL', dewormingDate: '2026-10-09',
    })
    expect(dewormingTypeLabels.EXTERNAL).toBe('体外驱虫')
  })

  it('sends medical records and reads the upcoming reminder feed', async () => {
    const medical = { id: 3, petId: 7, visitDate: '2026-10-09', reason: '复诊', cost: 0 }
    const reminder = { petId: 7, petName: '小白', type: 'MEDICAL_REVISIT', title: '复诊提醒', dueDate: '2026-10-10', daysRemaining: 1 }
    httpMock.post.mockResolvedValueOnce({ data: { data: medical } } as never)
    httpMock.get.mockResolvedValueOnce({ data: { data: [reminder] } } as never)

    await expect(createMedicalRecord(7, { visitDate: '2026-10-09', reason: '复诊', cost: 0 })).resolves.toEqual(medical)
    await expect(getUpcomingReminders()).resolves.toEqual([reminder])
    expect(httpMock.post).toHaveBeenCalledWith('/pets/7/medical-records', {
      visitDate: '2026-10-09', reason: '复诊', cost: 0,
    })
    expect(httpMock.get).toHaveBeenCalledWith('/reminders/upcoming')
  })
})
