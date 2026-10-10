import { http, type ApiResponse } from './http'

export type PetType = 'CAT' | 'DOG' | 'OTHER'
export type PetGender = 'MALE' | 'FEMALE' | 'UNKNOWN'

export interface PetProfile {
  id: number
  name: string
  type: PetType
  breed: string | null
  gender: PetGender
  birthday: string | null
  weight: number | null
  neutered: boolean | null
  homeDate: string | null
  remark: string | null
  createTime: string
}

export interface CreatePetPayload {
  name: string
  type: PetType
  breed?: string
  gender?: PetGender
  birthday?: string
  weight?: number
  neutered?: boolean | null
  homeDate?: string
  remark?: string
}

export async function getMyPets(): Promise<PetProfile[]> {
  const response = await http.get<ApiResponse<PetProfile[]>>('/pets')
  return response.data.data
}

export async function getPet(petId: number): Promise<PetProfile> {
  const response = await http.get<ApiResponse<PetProfile>>(`/pets/${petId}`)
  return response.data.data
}

export async function createPet(payload: CreatePetPayload): Promise<PetProfile> {
  const response = await http.post<ApiResponse<PetProfile>>('/pets', payload)
  return response.data.data
}
