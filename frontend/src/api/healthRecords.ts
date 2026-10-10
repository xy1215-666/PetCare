import { http, type ApiResponse } from './http'

export type AppetiteLevel = 'EXCELLENT' | 'NORMAL' | 'REDUCED' | 'POOR'
export type WaterIntake = 'INCREASED' | 'NORMAL' | 'REDUCED' | 'UNKNOWN'
export type EnergyLevel = 'ACTIVE' | 'NORMAL' | 'LOW' | 'VERY_LOW'
export type ActivityLevel = 'HIGH' | 'NORMAL' | 'LOW' | 'VERY_LOW'
export type SleepStatus = 'LESS' | 'NORMAL' | 'MORE' | 'UNKNOWN'
export type StoolStatus = 'NORMAL' | 'SOFT' | 'DIARRHEA' | 'CONSTIPATED' | 'UNKNOWN'
export type UrinationStatus = 'NORMAL' | 'INCREASED' | 'REDUCED' | 'ABNORMAL' | 'UNKNOWN'

export interface HealthRecord {
  id: number
  petId: number
  recordDate: string
  appetite: AppetiteLevel
  waterIntake: WaterIntake
  energyLevel: EnergyLevel
  activityLevel: ActivityLevel
  sleepStatus: SleepStatus
  stoolStatus: StoolStatus
  urinationStatus: UrinationStatus
  abnormal: boolean
  abnormalDescription: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export type HealthRecordPayload = Omit<HealthRecord,
  'id' | 'petId' | 'abnormalDescription' | 'remark' | 'createTime' | 'updateTime'
> & {
  abnormalDescription?: string
  remark?: string
}

export async function getHealthRecords(petId: number): Promise<HealthRecord[]> {
  const response = await http.get<ApiResponse<HealthRecord[]>>(`/pets/${petId}/health-records`)
  return response.data.data
}

export async function getHealthRecord(petId: number, recordId: number): Promise<HealthRecord> {
  const response = await http.get<ApiResponse<HealthRecord>>(`/pets/${petId}/health-records/${recordId}`)
  return response.data.data
}

export async function createHealthRecord(petId: number, payload: HealthRecordPayload): Promise<HealthRecord> {
  const response = await http.post<ApiResponse<HealthRecord>>(`/pets/${petId}/health-records`, payload)
  return response.data.data
}

export async function updateHealthRecord(
  petId: number,
  recordId: number,
  payload: HealthRecordPayload,
): Promise<HealthRecord> {
  const response = await http.put<ApiResponse<HealthRecord>>(
    `/pets/${petId}/health-records/${recordId}`,
    payload,
  )
  return response.data.data
}

export async function deleteHealthRecord(petId: number, recordId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/pets/${petId}/health-records/${recordId}`)
}
