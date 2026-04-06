export function axiosErrorMessage(err: unknown, fallback: string): string {
  const e = err as {
    response?: { status?: number; data?: { message?: string; error?: string } }
    message?: string
  }
  const msg = e.response?.data?.message
  if (typeof msg === 'string' && msg.length > 0) {
    return msg
  }
  if (e.response?.status === 400) {
    return 'Проверьте формат полей (email, пароль и т.д.)'
  }
  if (e.response?.status === 401) {
    return 'Неверный email или пароль'
  }
  if (e.response?.status && e.response.status >= 500) {
    return 'Ошибка сервера. Попробуйте позже.'
  }
  if (!e.response) {
    return 'Нет связи с сервером. Проверьте, что API запущен.'
  }
  return fallback
}
