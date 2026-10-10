import { http, type ApiResponse } from './http'

export interface CurrentUser {
  id: number
  username: string
  email: string | null
  phone: string | null
  role: string
  createTime: string
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await http.get<ApiResponse<CurrentUser>>('/users/me')
  return response.data.data
}
