import axios from 'axios'
import type { ApiResponse } from './http'

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.length > 0) {
      return message
    }
    if (error.code === 'ECONNABORTED') {
      return '请求超时，请稍后再试。'
    }
    if (!error.response) {
      return '暂时无法连接服务，请检查网络后重试。'
    }
  }
  return fallback
}
