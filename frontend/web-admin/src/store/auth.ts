import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo } from '@/api/auth'
import router from '@/router'

const TOKEN_KEY = 'screen_cast_token'
const USER_ID_KEY = 'screen_cast_user_id'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const userId = ref<number | null>(
    localStorage.getItem(USER_ID_KEY) ? Number(localStorage.getItem(USER_ID_KEY)) : null
  )
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(account: string, password: string) {
    const res = await authApi.login({ account, password })
    token.value = res.data.accessToken
    user.value = res.data.userInfo
    userId.value = res.data.userInfo.id
    localStorage.setItem(TOKEN_KEY, res.data.accessToken)
    localStorage.setItem(USER_ID_KEY, String(res.data.userInfo.id))
    return res.data
  }

  async function register(username: string, password: string, email?: string) {
    const res = await authApi.register({ username, password, email })
    token.value = res.data.accessToken
    user.value = res.data.userInfo
    userId.value = res.data.userInfo.id
    localStorage.setItem(TOKEN_KEY, res.data.accessToken)
    localStorage.setItem(USER_ID_KEY, String(res.data.userInfo.id))
    return res.data
  }

  async function fetchCurrentUser() {
    if (!token.value) return null
    try {
      const res = await authApi.getCurrentUser()
      user.value = res.data
      return res.data
    } catch {
      logout()
      return null
    }
  }

  function logout() {
    token.value = null
    user.value = null
    userId.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
    router.push('/login')
  }

  return {
    token,
    user,
    userId,
    isLoggedIn,
    login,
    register,
    fetchCurrentUser,
    logout
  }
})
