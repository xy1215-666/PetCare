import { http, type ApiResponse } from './http'

export interface VaccinationRecord {
  id: number
  petId: number
  vaccineName: string
  vaccinationDate: string
  nextDate: string | null
  hospital: string | null
  batchNumber: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface VaccinationPayload {
  vaccineName: string
  vaccinationDate: string
  nextDate?: string
  hospital?: string
  batchNumber?: string
  remark?: string
}

export async function getVaccinations(petId: number): Promise<VaccinationRecord[]> {
  const response = await http.get<ApiResponse<VaccinationRecord[]>>(`/pets/${petId}/vaccinations`)
  return response.data.data
}

export async function getVaccination(petId: number, recordId: number): Promise<VaccinationRecord> {
  const response = await http.get<ApiResponse<VaccinationRecord>>(`/pets/${petId}/vaccinations/${recordId}`)
  return response.data.data
}

export async function createVaccination(petId: number, payload: VaccinationPayload): Promise<VaccinationRecord> {
  const response = await http.post<ApiResponse<VaccinationRecord>>(`/pets/${petId}/vaccinations`, payload)
  return response.data.data
}

export async function updateVaccination(
  petId: number,
  recordId: number,
  payload: VaccinationPayload,
): Promise<VaccinationRecord> {
  const response = await http.put<ApiResponse<VaccinationRecord>>(
    `/pets/${petId}/vaccinations/${recordId}`,
    payload,
  )
  return response.data.data
}

export async function deleteVaccination(petId: number, recordId: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/pets/${petId}/vaccinations/${recordId}`)
}
