import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { axiosErrorMessage } from '../api/errors'
import { useAuthStore } from '../store/authStore'

export function LoginPage() {
  const navigate = useNavigate()
  const setTokens = useAuthStore((s) => s.setTokens)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  /** Показываем только после отказа во входе из‑за неподтверждённого email */
  const [showResend, setShowResend] = useState(false)
  const [resendMsg, setResendMsg] = useState<string | null>(null)
  const [resendErr, setResendErr] = useState<string | null>(null)
  const [resendLoading, setResendLoading] = useState(false)

  const { data: ghOauth } = useQuery({
    queryKey: ['auth-oauth-github'],
    queryFn: async () => (await api.get<{ configured: boolean }>('/auth/oauth/github')).data,
    staleTime: 60_000,
  })

  const { data: googleOauth } = useQuery({
    queryKey: ['auth-oauth-google'],
    queryFn: async () => (await api.get<{ configured: boolean }>('/auth/oauth/google')).data,
    staleTime: 60_000,
  })

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setResendMsg(null)
    setResendErr(null)
    try {
      const { data } = await api.post('/auth/login', {
        email: email.trim().toLowerCase(),
        password,
      })
      setTokens(data.accessToken, data.refreshToken)
      navigate('/')
    } catch (err) {
      const msg = axiosErrorMessage(err, 'Неверный email или пароль')
      setError(msg)
      setShowResend(msg.toLowerCase().includes('подтвердите email'))
    }
  }

  async function onResend(e: React.FormEvent) {
    e.preventDefault()
    setResendErr(null)
    setResendMsg(null)
    setResendLoading(true)
    try {
      await api.post('/auth/resend-verification', {
        email: email.trim().toLowerCase(),
        password,
      })
      setResendMsg('Если аккаунт с этим email есть и он ещё не подтверждён, письмо отправлено повторно.')
    } catch (err) {
      setResendErr(axiosErrorMessage(err, 'Не удалось отправить'))
    } finally {
      setResendLoading(false)
    }
  }

  const anyOauth =
    ghOauth?.configured === true ||
    googleOauth?.configured === true ||
    ghOauth?.configured === false ||
    googleOauth?.configured === false

  return (
    <div className="auth-wrap card">
      <h2>Вход</h2>
      <form onSubmit={onSubmit}>
        <label>Email</label>
        <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        <label>Пароль</label>
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
        {error && <p className="error">{error}</p>}
        <button type="submit" className="primary">
          Войти
        </button>
      </form>
      {showResend && (
        <form onSubmit={onResend} className="card card-compact" style={{ marginTop: '0.75rem' }}>
          <p className="muted small" style={{ marginTop: 0 }}>
            <strong>Не пришло письмо?</strong> Ниже можно отправить ссылку снова. Уже указаны email и пароль из
            полей выше.
          </p>
          <button type="submit" className="btn" disabled={resendLoading}>
            Отправить ссылку снова
          </button>
          {resendMsg && <p className="muted small">{resendMsg}</p>}
          {resendErr && <p className="error small">{resendErr}</p>}
        </form>
      )}
      <p className="muted">
        Нет аккаунта? <Link to="/register">Регистрация</Link>
      </p>
      <div className="oauth-github-block">
        {anyOauth ? (
          <div className="oauth-github-stack">
            <p className="oauth-github-lead muted">Либо войти через</p>
            <div className="oauth-providers-row">
              {ghOauth?.configured ? (
                <div className="oauth-github-actions">
                  <a
                    className="btn btn-github-oauth"
                    href="/oauth2/authorization/github"
                    aria-label="Войти через GitHub"
                    title="Войти через GitHub"
                  >
                    <svg className="btn-github-oauth-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                      <path
                        fill="currentColor"
                        d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.565 21.796 24 17.299 24 12c0-6.627-5.373-12-12-12z"
                      />
                    </svg>
                  </a>
                  <span className="oauth-github-brand">GitHub</span>
                </div>
              ) : null}
              {googleOauth?.configured ? (
                <div className="oauth-github-actions">
                  <a
                    className="btn btn-google-oauth"
                    href="/oauth2/authorization/google"
                    aria-label="Войти через Google"
                    title="Войти через Google"
                  >
                    <svg className="btn-google-oauth-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                      <path
                        fill="#EA4335"
                        d="M12 10.2v3.9h5.3c-.2 1.3-1.5 3.8-5.3 3.8-3.2 0-5.8-2.6-5.8-5.9S8.8 6 12 6c1.8 0 3 .8 3.7 1.4l2.5-2.4C16.5 3.6 14.5 2.7 12 2.7 6.9 2.7 2.7 6.9 2.7 12s4.2 9.3 9.3 9.3c5.4 0 8.9-3.8 8.9-9.1 0-.6-.1-1.1-.2-1.6H12z"
                      />
                      <path
                        fill="#4285F4"
                        d="M3.5 7.1 6.1 9c.8-2.4 3-4.1 5.9-4.1 1.8 0 3.3.6 4.4 1.6l3.1-3C17.4 1.7 14.9.7 12 .7 8.1.7 4.7 2.8 3.5 7.1z"
                      />
                      <path
                        fill="#FBBC05"
                        d="M12 21.3c2.4 0 4.5-.8 6-2.2l-2.8-2.2c-.8.5-1.8.9-3.2.9-2.5 0-4.6-1.7-5.3-4l-3 2.3c1.2 3.3 4.6 5.2 8.3 5.2z"
                      />
                      <path
                        fill="#34A853"
                        d="M21.3 12.2c0-.8-.1-1.6-.3-2.3H12v4.5h5.3c-.3 1.3-1.1 2.4-2.4 3.1l2.8 2.2c1.6-1.5 2.6-3.8 2.6-6.5z"
                      />
                    </svg>
                  </a>
                  <span className="oauth-github-brand">Google</span>
                </div>
              ) : null}
            </div>
            {ghOauth?.configured === false || googleOauth?.configured === false ? (
              <p className="muted small oauth-hint" style={{ marginTop: '0.5rem', marginBottom: 0 }}>
                {ghOauth?.configured === false && (
                  <>
                    GitHub: задайте <code>GITHUB_CLIENT_ID</code> и <code>GITHUB_CLIENT_SECRET</code> в{' '}
                    <code>code/.env</code>.
                  </>
                )}
                {ghOauth?.configured === false && googleOauth?.configured === false ? ' ' : null}
                {googleOauth?.configured === false && (
                  <>
                    Google: <code>GOOGLE_CLIENT_ID</code> и <code>GOOGLE_CLIENT_SECRET</code> (см.{' '}
                    <code>.env.example</code>).
                  </>
                )}
              </p>
            ) : null}
          </div>
        ) : (
          <p className="muted small">…</p>
        )}
      </div>
    </div>
  )
}
