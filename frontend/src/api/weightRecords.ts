import { http, type ApiResponse } from './http'

export interface WeightRecord {
  id: number
  petId: number
  weight: number
  recordDate: string
  remark: string | null
  createTime: string
  updateTime: string
}

export interface WeightRecordPayload {
  weight: number
  recordDate: string
  remark?: string
}

export async function getWeightRecords(petId: number): Promise<WeightRecord[]> {
  const response = await http.get<ApiResponse<WeightRecord[]>>(`/pets/${petId}/weight-records`)
  return response.data.data
}

export async function createWeightRecord(petId: number, payload: WeightRecordPayload): Promise<WeightRecord> {
  const response = await http.post<ApiResponse<WeightRecord>>(`/pets/${petId}/weight-records`, payload)
  return response.data.data
}

export async function updateWeightRecord(
  petId: number,
  recordId: number,
  payload: WeightRecordPayload,
): Promise<WeightRecord> {
  const response = await http.put<ApiResponse<WeightRecord>>(
    `/pets/${petId}/weight-records/${recordId}`,
    payload,
  )
  return response.data.data
}

export async function deleteWeightRecord(petId: number, recordId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/pets/${petId}/weight-records/${recordId}`)
}
