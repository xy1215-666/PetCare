import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import { sendAiMessage } from './aiChat'

vi.mock('./http', () => ({
  http: {
    post: vi.fn(),
  },
}))

const httpMock = vi.mocked(http)

describe('AI chat API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('sends only the message and unwraps the standard API response', async () => {
    httpMock.post.mockResolvedValueOnce({
      data: { code: 200, message: 'success', data: { content: '欢迎使用 PetCare。' } },
    } as never)

    await expect(sendAiMessage('你好')).resolves.toEqual({ content: '欢迎使用 PetCare。' })
    expect(httpMock.post).toHaveBeenCalledWith('/ai/chat', { message: '你好' }, { timeout: 60_000 })
  })
})
