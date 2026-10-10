import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as requestLogin, register as requestRegister } from '../api/auth'
import type { LoginPayload, RegisterPayload } from '../api/auth'
import { getCurrentUser } from '../api/users'
import type { CurrentUser } from '../api/users'

const ACCESS_TOKEN_STORAGE_KEY = 'petcare.accessToken'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(
    typeof window === 'undefined' ? null : window.sessionStorage.getItem(ACCESS_TOKEN_STORAGE_KEY),
  )
  const currentUser = ref<CurrentUser | null>(null)
  const isAuthenticated = computed(() => Boolean(accessToken.value))

  async function register(payload: RegisterPayload) {
    return requestRegister(payload)
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await requestLogin(payload)
    accessToken.value = result.accessToken
    window.sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, result.accessToken)

    try {
      currentUser.value = await getCurrentUser()
    } catch (error) {
      clearSession()
      throw error
    }
  }

  async function refreshCurrentUser(): Promise<CurrentUser> {
    const user = await getCurrentUser()
    currentUser.value = user
    return user
  }

  function clearSession(): void {
    accessToken.value = null
    currentUser.value = null
    if (typeof window !== 'undefined') {
      window.sessionStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY)
    }
  }

  return {
    accessToken,
    currentUser,
    isAuthenticated,
    register,
    login,
    refreshCurrentUser,
    clearSession,
  }
})
