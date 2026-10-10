import { http, type ApiResponse } from './http'

export interface AiChatResponse {
  content: string
}

export async function sendAiMessage(message: string): Promise<AiChatResponse> {
  const response = await http.post<ApiResponse<AiChatResponse>>(
    '/ai/chat',
    { message },
    { timeout: 60_000 },
  )
  return response.data.data
}
