import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { axiosErrorMessage } from '../api/errors'

type Submitted = { email: string; message: string }

const PASSWORD_RULE_ITEMS = [
  'От 8 до 128 символов',
  'Строчная и заглавная латинская буква (a–z, A–Z)',
  'Хотя бы одна цифра',
  'Хотя бы один спецсимвол (не буква и не цифра)',
] as const

function passwordIsValid(password: string): boolean {
  if (password.length < 8 || password.length > 128) return false
  if (!/[a-z]/.test(password)) return false
  if (!/[A-Z]/.test(password)) return false
  if (!/\d/.test(password)) return false
  if (!/[^A-Za-z0-9]/.test(password)) return false
  return true
}

function fullNameHasThreeWords(fullName: string): boolean {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  return parts.length === 3
}

export function RegisterPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [university, setUniversity] = useState('')
  const [studentGroup, setStudentGroup] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<Submitted | null>(null)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!fullNameHasThreeWords(fullName)) {
      setError('Укажите ФИО тремя словами через пробел: фамилия, имя и отчество.')
      return
    }
    if (!passwordIsValid(password)) {
      setError('Пароль не соответствует требованиям в списке под полем.')
      return
    }
    try {
      const { data } = await api.post<Submitted>('/auth/register', {
        email: email.trim().toLowerCase(),
        password,
        fullName: fullName.trim(),
        university: university.trim() || null,
        studentGroup: studentGroup.trim() || null,
      })
      setDone({ email: data.email, message: data.message })
    } catch (err) {
      setError(axiosErrorMessage(err, 'Не удалось зарегистрироваться. Проверьте данные или попробуйте позже.'))
    }
  }

  if (done) {
    return (
      <div className="auth-wrap card">
        <h2>Почти готово</h2>
        <p>{done.message}</p>
        <p className="muted small">
          Адрес: <strong>{done.email}</strong>
        </p>
        <p className="muted small">
          Если почта не настроена на сервере, ссылка выводится в лог backend (консоль Docker / терминал Spring).
        </p>
        <p>
          <Link to="/login" className="btn primary">
            Перейти ко входу
          </Link>
        </p>
      </div>
    )
  }

  return (
    <div className="auth-wrap card">
      <h2>Регистрация</h2>
      <form onSubmit={onSubmit}>
        <label>Email</label>
        <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        <label>Пароль</label>
        <div className="register-password-field">
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            required
            minLength={8}
            maxLength={128}
            autoComplete="new-password"
          />
          <ul className="muted small register-password-rules">
            {PASSWORD_RULE_ITEMS.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </div>
        <label>ФИО</label>
        <input
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          required
          placeholder="Иванов Иван Иванович"
        />
        <p className="muted small register-fio-hint">
          Три слова через пробел: фамилия, имя и отчество (двойные фамилии можно через дефис — одно слово).
        </p>
        <label>Вуз</label>
        <input value={university} onChange={(e) => setUniversity(e.target.value)} />
        <label>Группа</label>
        <input value={studentGroup} onChange={(e) => setStudentGroup(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <button type="submit" className="primary">
          Создать аккаунт
        </button>
      </form>
      <p className="muted">
        Уже есть аккаунт? <Link to="/login">Вход</Link>
      </p>
    </div>
  )
}
