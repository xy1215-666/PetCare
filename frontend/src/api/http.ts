import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const API_UNAUTHORIZED_EVENT = 'petcare:unauthorized'

let accessTokenProvider: () => string | null = () => null

export function setAccessTokenProvider(provider: () => string | null): void {
  accessTokenProvider = provider
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15_000,
  headers: {
    Accept: 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const accessToken = accessTokenProvider()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error)
      && error.response?.status === 401
      && typeof window !== 'undefined'
    ) {
      window.dispatchEvent(new Event(API_UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)
