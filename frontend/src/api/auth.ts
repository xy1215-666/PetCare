import { http, type ApiResponse } from './http'

export interface RegisterPayload {
  username: string
  password: string
  email?: string
  phone?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface UserSummary {
  id: number
  username: string
  email: string | null
  phone: string | null
  role: string
}

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserSummary
}

export async function register(payload: RegisterPayload): Promise<UserSummary> {
  const response = await http.post<ApiResponse<UserSummary>>('/auth/register', payload)
  return response.data.data
}

export async function login(payload: LoginPayload): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/login', payload)
  return response.data.data
}
