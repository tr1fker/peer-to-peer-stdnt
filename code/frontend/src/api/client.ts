import axios from 'axios'
import { useAuthStore } from '../store/authStore'

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

function isPublicAuthPath(url: string | undefined): boolean {
  if (!url) return false
  return (
    url.includes('auth/login') ||
    url.includes('auth/register') ||
    url.includes('auth/register/github-complete') ||
    url.includes('auth/register/oauth-complete') ||
    url.includes('auth/refresh') ||
    url.includes('auth/verify-email') ||
    url.includes('auth/resend-verification')
  )
}

api.interceptors.request.use((config) => {
  // Do not send an old session JWT on login/register — avoids odd 401 handling and server-side context noise.
  if (isPublicAuthPath(config.url)) {
    delete config.headers.Authorization
    return config
  }
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing = false

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config
    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }
    // Wrong password / validation on login returns 401 — must not trigger refresh (would mask the real error or logout).
    if (isPublicAuthPath(original?.url)) {
      return Promise.reject(error)
    }
    original._retry = true
    const refresh = useAuthStore.getState().refreshToken
    if (!refresh) {
      useAuthStore.getState().logout()
      return Promise.reject(error)
    }
    if (refreshing) {
      return Promise.reject(error)
    }
    refreshing = true
    try {
      const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken: refresh }, { withCredentials: true })
      useAuthStore.getState().setTokens(data.accessToken, data.refreshToken)
      original.headers.Authorization = `Bearer ${data.accessToken}`
      refreshing = false
      return api(original)
    } catch (e) {
      refreshing = false
      useAuthStore.getState().logout()
      return Promise.reject(e)
    }
  },
)
