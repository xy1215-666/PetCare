import { http, type ApiResponse } from './http'

export type DewormingType = 'INTERNAL' | 'EXTERNAL' | 'BOTH'

export const dewormingTypeLabels: Record<DewormingType, string> = {
  INTERNAL: '体内驱虫',
  EXTERNAL: '体外驱虫',
  BOTH: '体内外驱虫',
}

export interface DewormingRecord {
  id: number
  petId: number
  type: DewormingType
  medicineName: string | null
  dewormingDate: string
  nextDate: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface DewormingPayload {
  type: DewormingType
  medicineName?: string
  dewormingDate: string
  nextDate?: string
  remark?: string
}

export async function getDewormings(petId: number): Promise<DewormingRecord[]> {
  const response = await http.get<ApiResponse<DewormingRecord[]>>(`/pets/${petId}/dewormings`)
  return response.data.data
}

export async function getDeworming(petId: number, recordId: number): Promise<DewormingRecord> {
  const response = await http.get<ApiResponse<DewormingRecord>>(`/pets/${petId}/dewormings/${recordId}`)
  return response.data.data
}

export async function createDeworming(petId: number, payload: DewormingPayload): Promise<DewormingRecord> {
  const response = await http.post<ApiResponse<DewormingRecord>>(`/pets/${petId}/dewormings`, payload)
  return response.data.data
}

export async function updateDeworming(
  petId: number,
  recordId: number,
  payload: DewormingPayload,
): Promise<DewormingRecord> {
  const response = await http.put<ApiResponse<DewormingRecord>>(
    `/pets/${petId}/dewormings/${recordId}`,
    payload,
  )
  return response.data.data
}

export async function deleteDeworming(petId: number, recordId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/pets/${petId}/dewormings/${recordId}`)
}
