import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import { ClientListPagination } from '../components/ClientListPagination'
import { CreditLimitTiersHint } from '../components/CreditLimitTiersHint'
import { useClientPagination } from '../hooks/useClientPagination'

const ACADEMIC_MINE_PAGE_SIZE = 5

type Rec = {
  id: number
  gradeAverage: string
  description: string | null
  submittedAt: string
  verified: boolean
  verifiedAt: string | null
  rejected: boolean
  rejectedAt: string | null
  rejectionReason: string | null
}

function recordStatus(r: Rec): string {
  if (r.verified) return 'Подтверждено'
  if (r.rejected) return 'Отклонено'
  return 'На проверке'
}

function parseGrade(s: string): number | null {
  const n = Number(String(s).trim().replace(',', '.'))
  return Number.isFinite(n) ? n : null
}

function formatGradeDisplay(n: number) {
  return n.toLocaleString('ru-RU', { maximumFractionDigits: 2 })
}

type ProfileLite = { verificationStatus: string }

function profileVerificationRu(s: string) {
  if (s === 'VERIFIED') return 'Подтверждено'
  if (s === 'REJECTED') return 'Отклонено'
  if (s === 'BLOCKED') return 'Заблокирован'
  return 'На проверке'
}

function profileStatusVariant(s: string): 'verified' | 'pending' | 'rejected' | 'blocked' {
  if (s === 'VERIFIED') return 'verified'
  if (s === 'REJECTED') return 'rejected'
  if (s === 'BLOCKED') return 'blocked'
  return 'pending'
}

type MeAccountLite = { blocked: boolean }

export function AcademicPage() {
  const qc = useQueryClient()
  const { data } = useQuery({
    queryKey: ['academic-mine'],
    queryFn: async () => (await api.get<Rec[]>('/academic-records/mine')).data,
  })
  const recPag = useClientPagination(data, ACADEMIC_MINE_PAGE_SIZE)
  const { data: account } = useQuery({
    queryKey: ['me-account'],
    queryFn: async () => (await api.get<MeAccountLite>('/me/account')).data,
  })
  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => (await api.get<ProfileLite>('/me/profile')).data,
  })
  const [grade, setGrade] = useState('8.5')
  const [description, setDescription] = useState('')
  const m = useMutation({
    mutationFn: async () =>
      (await api.post<Rec>('/academic-records', { gradeAverage: grade, description: description || null })).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['academic-mine'] })
      qc.invalidateQueries({ queryKey: ['profile'] })
      qc.invalidateQueries({ queryKey: ['credit-limit'] })
    },
  })

  const verifiedGrades =
    data?.filter((r) => r.verified).map((r) => parseGrade(r.gradeAverage)).filter((n): n is number => n != null) ??
    []
  const bestVerifiedGrade = verifiedGrades.length > 0 ? Math.max(...verifiedGrades) : null
  const hasPendingRecord = data?.some((r) => !r.verified && !r.rejected) ?? false
  const accountBlocked = account?.blocked === true

  return (
    <div>
      <div className="academic-page-head">
        <div className="academic-page-lead">
          <h2 className="academic-page-title">Успеваемость</h2>
          <p className="academic-page-lead-text muted">
            Подтверждённый средний балл влияет на лимит займа. Записи проверяет администратор; в профиле отображается
            итоговый статус проверки.
          </p>
        </div>
        <div className="academic-summary-card">
          {profile == null || data === undefined ? (
            <p className="muted small" style={{ margin: 0 }}>
              Загрузка…
            </p>
          ) : (
            <>
              <div className="academic-summary-profile-row">
                <div className="academic-summary-profile-text">
                  <span className="academic-summary-profile-title">Успеваемость в профиле</span>
                  <span className="academic-summary-profile-hint muted small">
                    Как отображается проверка в карточке профиля
                  </span>
                </div>
                <span
                  className={`academic-status-pill academic-status-pill--${profileStatusVariant(
                    profile.verificationStatus,
                  )}`}
                >
                  {profileVerificationRu(profile.verificationStatus)}
                </span>
              </div>
              {bestVerifiedGrade != null ? (
                <>
                  <p className="academic-summary-label" style={{ marginTop: '0.85rem' }}>
                    Балл для лимита займа
                  </p>
                  <p className="academic-summary-grade">{formatGradeDisplay(bestVerifiedGrade)}</p>
                  <p className="muted small academic-summary-hint">
                    Считается <strong>наивысший</strong> подтверждённый балл из всех записей — не последняя
                    подтверждённая. Если подтвердили и 10, и 5, для лимита остаётся <strong>10</strong>.
                  </p>
                </>
              ) : (
                <>
                  <p className="academic-summary-label" style={{ marginTop: '0.85rem' }}>
                    Балл для лимита
                  </p>
                  <p className="academic-summary-empty">Подтверждённого балла пока нет</p>
                  <p className="muted small academic-summary-hint">
                    После проверки администратором здесь появится значение. Пока лимит по успеваемости по этому
                    баллу не действует.
                  </p>
                </>
              )}
              {accountBlocked && (
                <p className="academic-summary-blocked muted small">
                  Аккаунт заблокирован: новые записи успеваемости сейчас не принимаются.
                </p>
              )}
              {hasPendingRecord && !accountBlocked && (
                <p className="academic-summary-pending muted small">
                  Есть запись, ожидающая проверки — статус профиля может быть «На проверке».
                </p>
              )}
            </>
          )}
        </div>
      </div>
      <div className={`card${accountBlocked ? ' academic-submit-card--blocked' : ''}`}>
        <h3 style={{ marginTop: 0 }}>Отправить средний балл</h3>
        {accountBlocked && (
          <div className="loan-new-status loan-new-status--warn academic-blocked-banner" role="alert">
            <p className="loan-new-status-title">Подача успеваемости недоступна</p>
            <p className="muted small loan-new-status-text">
              Администратор ограничил заёмщикские действия для вашего аккаунта. Поля формы отключены до снятия
              блокировки.
            </p>
          </div>
        )}
        <CreditLimitTiersHint compact noDivider />
        <p className="muted small" style={{ marginBottom: '1rem' }}>
          Если у вас уже висит заявка на проверке, при новой отправке она будет автоматически снята — в очереди у
          администратора останется только последняя. Лимит по займу считается по <strong>наивысшему</strong>{' '}
          подтверждённому баллу среди всех ваших записей, а не по порядку подтверждения.
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (accountBlocked) return
            m.mutate()
          }}
        >
          <label>Средний балл по 10-балльной шкале (1–10)</label>
          <input
            value={grade}
            onChange={(e) => setGrade(e.target.value)}
            required
            type="number"
            min={1}
            max={10}
            step="0.01"
            inputMode="decimal"
            disabled={accountBlocked}
            aria-disabled={accountBlocked}
          />
          <label>Комментарий</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            disabled={accountBlocked}
            aria-disabled={accountBlocked}
          />
          <div className="form-actions">
            <button type="submit" className="primary" disabled={m.isPending || accountBlocked}>
              Отправить
            </button>
          </div>
        </form>
      </div>
      <div className="card">
        <h3>Мои записи</h3>
        {!data?.length && <p className="muted">Пока нет</p>}
        <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Балл</th>
              <th>Статус</th>
              <th>Дата</th>
              <th>Комментарий проверки</th>
            </tr>
          </thead>
          <tbody>
            {recPag.slice.map((r) => (
              <tr key={r.id}>
                <td>{r.gradeAverage}</td>
                <td>{recordStatus(r)}</td>
                <td>{r.submittedAt}</td>
                <td className="muted">
                  {r.rejected && r.rejectionReason?.trim() ? r.rejectionReason : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
        {data && data.length > 0 && (
          <ClientListPagination
            page={recPag.page}
            setPage={recPag.setPage}
            totalPages={recPag.totalPages}
            last={recPag.last}
          />
        )}
      </div>
    </div>
  )
}
