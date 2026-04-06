import { isAxiosError } from 'axios'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { axiosErrorMessage } from '../api/errors'
import { useAuthStore } from '../store/authStore'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const setTokens = useAuthStore((s) => s.setTokens)
  const [status, setStatus] = useState<'idle' | 'loading' | 'ok' | 'err'>('idle')
  const [message, setMessage] = useState<string | null>(null)

  const token = useMemo(() => params.get('token')?.trim() ?? '', [params])

  useEffect(() => {
    if (!token) {
      setStatus('err')
      setMessage('В ссылке нет токена. Откройте письмо ещё раз или запросите новое на странице входа.')
      return
    }

    const ac = new AbortController()
    setStatus('loading')
    ;(async () => {
      try {
        const { data } = await api.post('/auth/verify-email', { token }, { signal: ac.signal })
        setTokens(data.accessToken, data.refreshToken)
        setStatus('ok')
        setMessage('Email подтверждён. Сейчас перенаправим на главную…')
        setTimeout(() => navigate('/', { replace: true }), 1200)
      } catch (err) {
        if (isAxiosError(err) && err.code === 'ERR_CANCELED') return
        setStatus('err')
        setMessage(axiosErrorMessage(err, 'Не удалось подтвердить email'))
      }
    })()

    return () => ac.abort()
  }, [token, navigate, setTokens])

  return (
    <div className="auth-wrap card">
      <h2>Подтверждение email</h2>
      {status === 'loading' && <p className="muted">Проверяем ссылку…</p>}
      {message && <p className={status === 'err' ? 'error' : 'muted'}>{message}</p>}
      {status === 'err' && (
        <p>
          <Link to="/login">Страница входа</Link>
        </p>
      )}
    </div>
  )
}
