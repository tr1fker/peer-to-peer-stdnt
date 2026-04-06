import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { Modal } from '../components/Modal'
import { getUserIdFromToken, useAuthStore } from '../store/authStore'
import { useState } from 'react'

type LoanRequest = {
  id: number
  borrowerId: number
  amount: string
  termMonths: number
  purpose: string | null
  status: string
  interestRatePercent: string
  createdAt: string
  fundedAmount: string
  borrowerReputationPoints: number
}

function statusClass(s: string) {
  if (s === 'OPEN') return 'badge badge-open'
  if (s === 'FUNDED') return 'badge badge-funded'
  if (s === 'CANCELLED') return 'badge badge-cancel'
  return 'badge'
}

function statusRu(s: string) {
  if (s === 'OPEN') return 'Открыта'
  if (s === 'FUNDED') return 'Собрана'
  if (s === 'CANCELLED') return 'Отменена'
  return s
}

export function LoanRequestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const access = useAuthStore((s) => s.accessToken)
  const roles = useAuthStore((s) => s.roles)
  const myId = getUserIdFromToken(access)

  const [investOpen, setInvestOpen] = useState(false)
  const [investAmount, setInvestAmount] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const { data: r, isLoading } = useQuery({
    queryKey: ['loan-request', id],
    queryFn: async () => (await api.get<LoanRequest>(`/loan-requests/${id}`)).data,
    enabled: !!id,
  })

  const { data: favoriteIds = [] } = useQuery({
    queryKey: ['favorite-loan-request-ids'],
    queryFn: async () => (await api.get<number[]>('/me/favorite-loan-request-ids')).data,
    enabled: !!access,
  })

  const isFavorite = r != null && favoriteIds.includes(r.id)

  const toggleFavMut = useMutation({
    mutationFn: async ({ add }: { add: boolean }) => {
      if (!id) return
      if (add) await api.post(`/me/favorite-loan-requests/${id}`)
      else await api.delete(`/me/favorite-loan-requests/${id}`)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['favorite-loan-request-ids'] })
      qc.invalidateQueries({ queryKey: ['favorite-loan-requests'] })
    },
  })

  const cancelMut = useMutation({
    mutationFn: async () => api.post(`/loan-requests/${id}/cancel`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loan-request', id] })
      qc.invalidateQueries({ queryKey: ['loan-requests'] })
      qc.invalidateQueries({ queryKey: ['me-loan-requests'] })
      navigate('/loans')
    },
  })

  const investMut = useMutation({
    mutationFn: async () => api.post(`/loan-requests/${id}/invest`, { amount: investAmount }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['loan-request', id] })
      qc.invalidateQueries({ queryKey: ['loan-requests'] })
      qc.invalidateQueries({ queryKey: ['me-investments'] })
      qc.invalidateQueries({ queryKey: ['loans-borrower'] })
      setInvestOpen(false)
      setInvestAmount('')
      setFormError(null)
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Ошибка инвестиции'
      setFormError(String(msg))
    },
  })

  if (!id) return null
  if (isLoading || !r) return <p className="muted">Загрузка…</p>

  const amountNum = Number(r.amount)
  const fundedNum = Number(r.fundedAmount)
  const pct = amountNum > 0 ? Math.min(100, Math.round((fundedNum / amountNum) * 100)) : 0
  const remaining = Math.max(0, amountNum - fundedNum)
  const isBorrower = myId != null && myId === r.borrowerId
  const canInvest = access && roles.includes('ROLE_LENDER') && r.status === 'OPEN' && !isBorrower
  const canCancel = access && isBorrower && r.status === 'OPEN'

  return (
    <div className="page-detail">
      <Link to="/loans" className="back-link">
        ← К списку заявок
      </Link>

      <div className="card detail-hero">
        <div className="detail-hero-top">
          <span className={statusClass(r.status)}>{statusRu(r.status)}</span>
          <span className="muted">Заявка #{r.id}</span>
        </div>
        <h1 className="detail-title">{Number(r.amount).toLocaleString('ru-RU')} BYN</h1>
        <p className="detail-meta">
          Срок {r.termMonths} мес. · ставка {r.interestRatePercent}% годовых · репутация заёмщика{' '}
          <strong>{r.borrowerReputationPoints ?? 500}</strong> / 1000
        </p>
        {access && !isBorrower && (
          <p className="muted small" style={{ marginTop: '-0.25rem' }}>
            <button
              type="button"
              className="favorite-star favorite-star--inline"
              aria-label={isFavorite ? 'Убрать из избранного' : 'В избранное'}
              aria-pressed={isFavorite}
              disabled={toggleFavMut.isPending}
              onClick={() => toggleFavMut.mutate({ add: !isFavorite })}
            >
              {isFavorite ? '★ В избранном' : '☆ В избранное'}
            </button>
          </p>
        )}
        {r.purpose && <p className="purpose">{r.purpose}</p>}

        <div className="progress-wrap">
          <div className="progress-label">
            <span>Собрано</span>
            <span>
              {Number(r.fundedAmount).toLocaleString('ru-RU')} / {Number(r.amount).toLocaleString('ru-RU')} BYN ({pct}%)
            </span>
          </div>
          <div className="progress-bar" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
            <div className="progress-fill" style={{ width: `${pct}%` }} />
          </div>
          {r.status === 'OPEN' && (
            <p className="muted small">Осталось: {remaining.toLocaleString('ru-RU')} BYN</p>
          )}
        </div>

        <div className="detail-actions">
          {canInvest && (
            <button type="button" className="primary" onClick={() => setInvestOpen(true)}>
              Инвестировать
            </button>
          )}
          {canCancel && (
            <button
              type="button"
              className="btn-danger"
              disabled={cancelMut.isPending}
              onClick={() => {
                if (window.confirm('Отменить заявку? Только если ещё нет инвестиций.')) cancelMut.mutate()
              }}
            >
              Отменить заявку
            </button>
          )}
        </div>
      </div>

      <Modal title="Инвестиция в заявку" open={investOpen} onClose={() => setInvestOpen(false)}>
        <p className="muted small">До {remaining.toLocaleString('ru-RU')} BYN</p>
        <label>Сумма</label>
        <input
          value={investAmount}
          onChange={(e) => setInvestAmount(e.target.value)}
          placeholder="Например 10000"
          inputMode="decimal"
        />
        {formError && <p className="error">{formError}</p>}
        <div className="modal-actions">
          <button type="button" onClick={() => setInvestOpen(false)}>
            Отмена
          </button>
          <button
            type="button"
            className="primary"
            disabled={investMut.isPending || !investAmount}
            onClick={() => investMut.mutate()}
          >
            Подтвердить
          </button>
        </div>
      </Modal>
    </div>
  )
}
