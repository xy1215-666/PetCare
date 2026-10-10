import { http, type ApiResponse } from './http'

export interface MedicalRecord {
  id: number
  petId: number
  visitDate: string
  hospital: string | null
  reason: string
  examination: string | null
  result: string | null
  medicine: string | null
  doctorAdvice: string | null
  cost: number
  nextVisitDate: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface MedicalRecordPayload {
  visitDate: string
  hospital?: string
  reason: string
  examination?: string
  result?: string
  medicine?: string
  doctorAdvice?: string
  cost: number
  nextVisitDate?: string
  remark?: string
}

export async function getMedicalRecords(petId: number): Promise<MedicalRecord[]> {
  const response = await http.get<ApiResponse<MedicalRecord[]>>(`/pets/${petId}/medical-records`)
  return response.data.data
}

export async function getMedicalRecord(petId: number, recordId: number): Promise<MedicalRecord> {
  const response = await http.get<ApiResponse<MedicalRecord>>(`/pets/${petId}/medical-records/${recordId}`)
  return response.data.data
}

export async function createMedicalRecord(petId: number, payload: MedicalRecordPayload): Promise<MedicalRecord> {
  const response = await http.post<ApiResponse<MedicalRecord>>(`/pets/${petId}/medical-records`, payload)
  return response.data.data
}

export async function updateMedicalRecord(
  petId: number,
  recordId: number,
  payload: MedicalRecordPayload,
): Promise<MedicalRecord> {
  const response = await http.put<ApiResponse<MedicalRecord>>(
    `/pets/${petId}/medical-records/${recordId}`,
    payload,
  )
  return response.data.data
}

export async function deleteMedicalRecord(petId: number, recordId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/pets/${petId}/medical-records/${recordId}`)
}
