import { create } from 'zustand'

function parseJwtPayload(token: string): { sub?: string; email?: string; roles?: string[] } | null {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

function parseJwtRoles(token: string): string[] {
  const json = parseJwtPayload(token)
  if (!json?.roles) return []
  return Array.isArray(json.roles) ? json.roles.map(String) : []
}

export function getUserIdFromToken(token: string | null): number | null {
  if (!token) return null
  const json = parseJwtPayload(token)
  const sub = json?.sub
  if (sub == null || sub === '') return null
  const n = Number(sub)
  return Number.isFinite(n) ? n : null
}

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  roles: string[]
  setTokens: (access: string, refresh: string) => void
  logout: () => void
}

const storedAccess = sessionStorage.getItem('accessToken')
const storedRefresh = sessionStorage.getItem('refreshToken')

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: storedAccess,
  refreshToken: storedRefresh,
  roles: storedAccess ? parseJwtRoles(storedAccess) : [],
  setTokens: (access, refresh) => {
    sessionStorage.setItem('accessToken', access)
    sessionStorage.setItem('refreshToken', refresh)
    set({ accessToken: access, refreshToken: refresh, roles: parseJwtRoles(access) })
  },
  logout: () => {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    set({ accessToken: null, refreshToken: null, roles: [] })
  },
}))
