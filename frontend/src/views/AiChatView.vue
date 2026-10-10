<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendAiMessage } from '../api/aiChat'
import { getApiErrorMessage } from '../api/errors'

type MessageRole = 'assistant' | 'user'

interface ChatMessage {
  id: number
  role: MessageRole
  content: string
}

const messages = ref<ChatMessage[]>([
  {
    id: 1,
    role: 'assistant',
    content: '你好，我可以陪你聊聊日常养宠问题。你可以从一个具体问题开始。',
  },
])
const draft = ref('')
const loading = ref(false)
const errorMessage = ref('')
const transcript = ref<HTMLElement | null>(null)
let nextMessageId = 2

async function scrollToLatest(): Promise<void> {
  await nextTick()
  transcript.value?.scrollTo({ top: transcript.value.scrollHeight, behavior: 'smooth' })
}

async function sendMessage(): Promise<void> {
  const message = draft.value.trim()
  if (!message || loading.value) return

  messages.value.push({ id: nextMessageId++, role: 'user', content: message })
  draft.value = ''
  errorMessage.value = ''
  loading.value = true
  await scrollToLatest()

  try {
    const response = await sendAiMessage(message)
    messages.value.push({ id: nextMessageId++, role: 'assistant', content: response.content })
  } catch (error: unknown) {
    errorMessage.value = getApiErrorMessage(error, '暂时无法发送消息，请稍后重试。')
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
    await scrollToLatest()
  }
}
</script>

<template>
  <section class="ai-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">PETCARE COMPANION</p>
        <h1>PetCare 智能助手</h1>
        <p class="subtitle">聊聊日常照护，让每一天都更安心。</p>
      </div>
      <span class="sparkle" aria-hidden="true">✦</span>
    </header>

    <section class="chat-card" aria-label="智能助手对话">
      <div class="chat-notice">
        <span class="notice-icon" aria-hidden="true">i</span>
        当前为单轮问答，助手暂时无法读取宠物档案，也不会保存对话记录。
      </div>

      <div ref="transcript" class="transcript" role="log" aria-live="polite" aria-relevant="additions">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <span v-if="message.role === 'assistant'" class="message-avatar" aria-hidden="true">P</span>
          <div class="message-bubble" :class="`${message.role}-bubble`">
            {{ message.content }}
          </div>
          <span v-if="message.role === 'user'" class="message-avatar user-avatar" aria-hidden="true">我</span>
        </article>

        <article v-if="loading" class="message-row assistant" aria-label="助手正在回复">
          <span class="message-avatar" aria-hidden="true">P</span>
          <div class="message-bubble assistant-bubble typing">正在认真思考…</div>
        </article>
      </div>

      <p v-if="errorMessage" class="send-error" role="alert">{{ errorMessage }}</p>

      <form class="composer" @submit.prevent="sendMessage">
        <label class="sr-only" for="ai-message">输入消息</label>
        <textarea
          id="ai-message"
          v-model="draft"
          maxlength="4000"
          rows="3"
          :disabled="loading"
          placeholder="写下你想了解的养宠问题…"
        />
        <div class="composer-footer">
          <span class="character-count">{{ draft.length }} / 4000</span>
          <button class="send-button" type="submit" :disabled="loading || !draft.trim()">
            {{ loading ? '发送中…' : '发送' }}
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      </form>
    </section>
  </section>
</template>

<style scoped>
.ai-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 9px;
  color: #7e9b83;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

h1 {
  margin: 0;
  color: #2f4035;
  font-size: clamp(27px, 4vw, 36px);
  font-weight: 650;
  letter-spacing: -0.035em;
}

.subtitle {
  margin: 10px 0 0;
  color: #7c897f;
  font-size: 14px;
}

.sparkle {
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border-radius: 19px;
  background: #e7efe4;
  color: #719778;
  font-size: 25px;
}

.chat-card {
  overflow: hidden;
  border: 1px solid #e5ebe2;
  border-radius: 24px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 18px 48px rgb(54 74 56 / 6%);
}

.chat-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 22px;
  border-bottom: 1px solid #edf1eb;
  background: #f8faf6;
  color: #748178;
  font-size: 12px;
  line-height: 1.6;
}

.notice-icon {
  display: grid;
  width: 19px;
  height: 19px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: #e7efe4;
  color: #587d61;
  font-family: Georgia, serif;
  font-size: 13px;
  font-weight: 700;
}

.transcript {
  display: flex;
  min-height: 310px;
  max-height: 52vh;
  flex-direction: column;
  gap: 22px;
  overflow-y: auto;
  padding: 28px 24px;
  scroll-behavior: smooth;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.user {
  justify-content: flex-end;
}

.message-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  background: #eaf1e7;
  color: #5b8062;
  font-size: 14px;
  font-weight: 700;
}

.user-avatar {
  border-radius: 50%;
  background: #f2e9d9;
  color: #96794c;
  font-size: 12px;
}

.message-bubble {
  max-width: min(76%, 620px);
  padding: 13px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.assistant-bubble {
  border-bottom-left-radius: 6px;
  background: #f1f5ee;
  color: #46564a;
}

.user-bubble {
  border-bottom-right-radius: 6px;
  background: #58866b;
  color: #fff;
}

.typing {
  color: #819187;
}

.send-error {
  margin: 0 24px 14px;
  color: #ae584d;
  font-size: 13px;
}

.composer {
  padding: 18px 20px 20px;
  border-top: 1px solid #edf1eb;
  background: #fff;
}

textarea {
  display: block;
  width: 100%;
  min-height: 74px;
  resize: vertical;
  border: 0;
  outline: 0;
  background: transparent;
  color: #344238;
  font: inherit;
  font-size: 14px;
  line-height: 1.6;
}

textarea::placeholder {
  color: #a1aba3;
}

.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
}

.character-count {
  color: #a1aaa2;
  font-size: 11px;
}

.send-button {
  display: inline-flex;
  min-width: 94px;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 10px 15px;
  border: 0;
  border-radius: 12px;
  background: #58866b;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  transition: background 150ms ease, transform 150ms ease;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #47785b;
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  clip-path: inset(50%);
}

@media (max-width: 640px) {
  .page-heading {
    margin-bottom: 20px;
  }

  .sparkle {
    width: 44px;
    height: 44px;
    border-radius: 15px;
  }

  .chat-card {
    border-radius: 19px;
  }

  .chat-notice {
    align-items: flex-start;
    padding: 13px 16px;
  }

  .transcript {
    min-height: 280px;
    padding: 20px 15px;
  }

  .message-bubble {
    max-width: 82%;
    padding: 11px 13px;
    font-size: 13px;
  }

  .composer {
    padding: 14px 15px 16px;
  }
}
</style>
