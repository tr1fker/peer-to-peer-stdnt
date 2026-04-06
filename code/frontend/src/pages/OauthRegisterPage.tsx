import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { axiosErrorMessage } from '../api/errors'
import { useAuthStore } from '../store/authStore'

function fullNameHasThreeWords(fullName: string): boolean {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  return parts.length === 3
}

export function OauthRegisterPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const setTokens = useAuthStore((s) => s.setTokens)
  const pending = params.get('pending')
  const [fullName, setFullName] = useState('')
  const [university, setUniversity] = useState('')
  const [studentGroup, setStudentGroup] = useState('')
  const [error, setError] = useState<string | null>(null)

  const canSubmit = useMemo(
    () => !!pending && fullNameHasThreeWords(fullName),
    [pending, fullName],
  )

  const m = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<{ accessToken: string; refreshToken: string; expiresInSeconds: number }>(
        '/auth/register/oauth-complete',
        {
          pendingToken: pending,
          fullName: fullName.trim(),
          university: university.trim() || null,
          studentGroup: studentGroup.trim() || null,
        },
      )
      return data
    },
    onSuccess: (data) => {
      setTokens(data.accessToken, data.refreshToken)
      qc.invalidateQueries({ queryKey: ['me-account'] })
      navigate('/', { replace: true })
    },
    onError: (err) => setError(axiosErrorMessage(err, 'Не удалось завершить регистрацию')),
  })

  if (!pending) {
    return (
      <div className="auth-wrap card">
        <h2>Регистрация через соцвход</h2>
        <p className="error">Нет данных входа. Начните с кнопки входа через GitHub или Google.</p>
        <Link to="/login">На страницу входа</Link>
      </div>
    )
  }

  return (
    <div className="auth-wrap card">
      <h2>Завершите регистрацию</h2>
      <p className="muted small">Заполните профиль студента.</p>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          setError(null)
          if (!fullNameHasThreeWords(fullName)) {
            setError('Укажите ФИО тремя словами через пробел: фамилия, имя и отчество.')
            return
          }
          if (canSubmit) m.mutate()
        }}
      >
        <label>ФИО</label>
        <input
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          required
          placeholder="Иванов Иван Иванович"
        />
        <p className="muted small">Три слова через пробел: фамилия, имя и отчество.</p>
        <label>Вуз</label>
        <input value={university} onChange={(e) => setUniversity(e.target.value)} />
        <label>Группа</label>
        <input value={studentGroup} onChange={(e) => setStudentGroup(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <button type="submit" className="primary" disabled={!canSubmit || m.isPending}>
          Создать аккаунт
        </button>
      </form>
      <p className="muted small">
        <Link to="/login">Отмена</Link>
      </p>
    </div>
  )
}
