import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

type Profile = {
  fullName: string
  university: string | null
  studentGroup: string | null
  verificationStatus: string
  reputationPoints: number
}

type MeAccount = {
  userId: number
  email: string
  roles: string[]
  githubLinked: boolean
  googleLinked: boolean
  blocked: boolean
}

function noticeText(code: string): string {
  switch (code) {
    case 'github_in_use':
      return 'Этот аккаунт GitHub уже привязан к другому пользователю сервиса.'
    case 'github_already_linked':
      return 'К вашему профилю уже привязан GitHub.'
    case 'google_in_use':
      return 'Этот Google-аккаунт уже привязан к другому пользователю сервиса.'
    case 'google_already_linked':
      return 'К вашему профилю уже привязан Google.'
    case 'github_link_session':
    case 'oauth_link_session':
      return 'Сессия привязки устарела. Нажмите «Привязать …» снова.'
    case 'oauth_link_wrong_provider':
      return 'Вы начали привязку одного провайдера, а завершили другим. Повторите привязку с тем же сервисом.'
    default:
      return ''
  }
}

function roleDisplay(role: string): { label: string; cls: string } {
  switch (role) {
    case 'ROLE_BORROWER':
      return { label: 'Заёмщик', cls: 'badge badge-role-borrower' }
    case 'ROLE_LENDER':
      return { label: 'Инвестор', cls: 'badge badge-role-lender' }
    case 'ROLE_ADMIN':
      return { label: 'Администратор', cls: 'badge badge-role-admin' }
    default:
      return { label: role.replace(/^ROLE_/, ''), cls: 'badge' }
  }
}

function verificationLabel(s: string) {
  if (s === 'VERIFIED') return 'Подтверждено'
  if (s === 'REJECTED') return 'Отклонено'
  if (s === 'BLOCKED') return 'Заблокирован'
  return 'На проверке'
}

export function ProfilePage() {
  const qc = useQueryClient()
  const location = useLocation()
  const navigate = useNavigate()
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    const q = new URLSearchParams(location.search)
    const n = q.get('notice')
    if (!n) {
      return
    }
    const text = noticeText(n)
    if (text) {
      setNotice(text)
    }
    q.delete('notice')
    const s = q.toString()
    navigate(`${location.pathname}${s ? `?${s}` : ''}`, { replace: true })
  }, [location.search, location.pathname, navigate])

  const { data: account } = useQuery({
    queryKey: ['me-account'],
    queryFn: async () => (await api.get<MeAccount>('/me/account')).data,
  })
  const { data } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => (await api.get<Profile>('/me/profile')).data,
  })
  const [fullName, setFullName] = useState('')
  const [university, setUniversity] = useState('')
  const [studentGroup, setStudentGroup] = useState('')

  useEffect(() => {
    if (data) {
      setFullName(data.fullName)
      setUniversity(data.university ?? '')
      setStudentGroup(data.studentGroup ?? '')
    }
  }, [data])

  const m = useMutation({
    mutationFn: async () =>
      (
        await api.put<Profile>('/me/profile', {
          fullName,
          university: university || null,
          studentGroup: studentGroup || null,
        })
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })

  const linkGithubMut = useMutation({
    mutationFn: async () => {
      await api.post('/me/oauth/github/link-start')
      window.location.href = '/oauth2/authorization/github'
    },
  })

  const linkGoogleMut = useMutation({
    mutationFn: async () => {
      await api.post('/me/oauth/google/link-start')
      window.location.href = '/oauth2/authorization/google'
    },
  })

  if (!data || !account) return <p className="muted">Загрузка…</p>

  return (
    <div className="profile-stack">
      {notice && (
        <div className="card profile-notice" role="alert">
          <p className="error" style={{ margin: 0 }}>
            {notice}
          </p>
        </div>
      )}
      {account.blocked && (
        <div className="card" role="alert" style={{ borderColor: 'rgba(255, 176, 32, 0.45)' }}>
          <h3 style={{ marginTop: 0, fontSize: '1rem' }}>Ограничение аккаунта</h3>
          <p className="muted small" style={{ margin: 0 }}>
            Аккаунт заблокирован администратором: недоступны подача успеваемости, новые заявки на займ и добавление
            поручителя. Вход и просмотр данных сохраняются; существующие займы можно обслуживать (платежи).
          </p>
        </div>
      )}
      <div className="card">
        <h2 style={{ marginTop: 0 }}>Аккаунт</h2>
        <p className="profile-email">{account.email}</p>
        <div className="role-chips">
          {account.roles.map((r) => {
            const { label, cls } = roleDisplay(r)
            return (
              <span key={r} className={cls}>
                {label}
              </span>
            )
          })}
        </div>
        <p className="muted small account-ref">
          Номер для поручителя: <strong className="account-ref-num">{account.userId}</strong>
        </p>
        <div className="github-link-row">
          {account.githubLinked ? (
            <p className="muted small" style={{ marginBottom: 0 }}>
              GitHub привязан.
            </p>
          ) : (
            <>
              <p className="muted small">Вход через GitHub</p>
              <button
                type="button"
                className="btn"
                disabled={linkGithubMut.isPending}
                onClick={() => linkGithubMut.mutate()}
              >
                Привязать GitHub
              </button>
              {linkGithubMut.isError && (
                <p className="error small">
                  {(linkGithubMut.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                    'Не удалось начать привязку'}
                </p>
              )}
            </>
          )}
        </div>
        <div className="github-link-row">
          {account.googleLinked ? (
            <p className="muted small" style={{ marginBottom: 0 }}>
              Google привязан.
            </p>
          ) : (
            <>
              <p className="muted small">Вход через Google</p>
              <button
                type="button"
                className="btn"
                disabled={linkGoogleMut.isPending}
                onClick={() => linkGoogleMut.mutate()}
              >
                Привязать Google
              </button>
              {linkGoogleMut.isError && (
                <p className="error small">
                  {(linkGoogleMut.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                    'Не удалось начать привязку'}
                </p>
              )}
            </>
          )}
        </div>
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0 }}>Профиль студента</h2>
        <p className="verification-line">
          Успеваемость: <span className="verification-value">{verificationLabel(data.verificationStatus)}</span>
        </p>
        <p className="muted small" style={{ marginTop: '0.35rem' }}>
          Репутация заёмщика: <strong>{data.reputationPoints}</strong> / 1000 — растёт за своевременные платежи и
          полное погашение займов; снижается при просрочке (см. график в «Мои займы»).
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            m.mutate()
          }}
        >
          <label>ФИО</label>
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
          <label>Вуз</label>
          <input value={university} onChange={(e) => setUniversity(e.target.value)} />
          <label>Группа</label>
          <input value={studentGroup} onChange={(e) => setStudentGroup(e.target.value)} />
          <div className="form-actions">
            <button type="submit" className="primary" disabled={m.isPending}>
              Сохранить
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
