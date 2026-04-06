import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api/client'
import { useAuthStore } from '../store/authStore'
import { useMemo, useState } from 'react'

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

type PageResp = {
  content: LoanRequest[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

function statusBadge(status: string) {
  if (status === 'OPEN') return 'badge badge-open'
  if (status === 'FUNDED') return 'badge badge-funded'
  if (status === 'CANCELLED') return 'badge badge-cancel'
  return 'badge'
}

function statusRu(status: string) {
  if (status === 'OPEN') return 'Открыта'
  if (status === 'FUNDED') return 'Собрана'
  if (status === 'CANCELLED') return 'Отменена'
  return status
}

function reputationLabel(n: number | undefined) {
  const v = n ?? 500
  if (v >= 700) return 'высокая'
  if (v >= 400) return 'средняя'
  return 'низкая'
}

type SortField =
  | 'id'
  | 'amount'
  | 'termMonths'
  | 'interestRatePercent'
  | 'reputation'
  | 'status'
  | 'funded'
  | 'createdAt'

function compareLoanRequests(a: LoanRequest, b: LoanRequest, field: SortField, dir: 'asc' | 'desc'): number {
  const mul = dir === 'asc' ? 1 : -1
  switch (field) {
    case 'id':
      return (a.id - b.id) * mul
    case 'amount':
      return (Number(a.amount) - Number(b.amount)) * mul
    case 'termMonths':
      return (a.termMonths - b.termMonths) * mul
    case 'interestRatePercent':
      return (Number(a.interestRatePercent) - Number(b.interestRatePercent)) * mul
    case 'reputation':
      return ((a.borrowerReputationPoints ?? 500) - (b.borrowerReputationPoints ?? 500)) * mul
    case 'status':
      return a.status.localeCompare(b.status) * mul
    case 'funded':
      return (Number(a.fundedAmount) - Number(b.fundedAmount)) * mul
    case 'createdAt':
      return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * mul
    default:
      return 0
  }
}

export function LoanRequestsPage() {
  const access = useAuthStore((s) => s.accessToken)
  const location = useLocation()
  const favoritesOnly = location.pathname === '/loans/favorites'
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [sortField, setSortField] = useState<SortField>('createdAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')

  const { data, isLoading } = useQuery({
    queryKey: ['loan-requests', page, statusFilter, sortField, sortDir],
    queryFn: async () => {
      const params: Record<string, string | number> = {
        page,
        size: 10,
        sortField,
        sortDir,
      }
      if (statusFilter) params.status = statusFilter
      return (await api.get<PageResp>('/loan-requests', { params })).data
    },
    enabled: !favoritesOnly,
  })

  const { data: favorites, isLoading: favLoading } = useQuery({
    queryKey: ['favorite-loan-requests'],
    queryFn: async () => (await api.get<LoanRequest[]>('/me/favorite-loan-requests')).data,
    enabled: !!access && favoritesOnly,
  })

  const { data: favoriteIds = [] } = useQuery({
    queryKey: ['favorite-loan-request-ids'],
    queryFn: async () => (await api.get<number[]>('/me/favorite-loan-request-ids')).data,
    enabled: !!access && !favoritesOnly,
  })

  const favSet = new Set(favoriteIds)

  const toggleFavMut = useMutation({
    mutationFn: async ({ id, add }: { id: number; add: boolean }) => {
      if (add) await api.post(`/me/favorite-loan-requests/${id}`)
      else await api.delete(`/me/favorite-loan-requests/${id}`)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['favorite-loan-request-ids'] })
      qc.invalidateQueries({ queryKey: ['favorite-loan-requests'] })
    },
  })

  const rows: LoanRequest[] = favoritesOnly ? favorites ?? [] : data?.content ?? []
  const displayRows = useMemo(() => {
    if (!favoritesOnly) return rows
    const copy = [...rows]
    copy.sort((a, b) => compareLoanRequests(a, b, sortField, sortDir))
    return copy
  }, [favoritesOnly, rows, sortField, sortDir])
  const loading = favoritesOnly ? favLoading : isLoading || !data

  const toggleSort = (field: SortField) => {
    setPage(0)
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDir(field === 'createdAt' ? 'desc' : 'asc')
    }
  }

  const sortableTh = (field: SortField, label: string) => {
    const active = sortField === field
    const ariaSort = active ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'
    return (
      <th aria-sort={ariaSort}>
        <button type="button" className="th-sort" onClick={() => toggleSort(field)} title={`Сортировать: ${label}`}>
          {label}
          {active ? (
            <span className="th-sort-hint" aria-hidden>
              {sortDir === 'asc' ? '▲' : '▼'}
            </span>
          ) : null}
        </button>
      </th>
    )
  }

  return (
    <div>
      <div className="toolbar">
        <h2 style={{ margin: 0, flex: '1 1 auto' }}>
          {favoritesOnly ? 'Избранные заявки' : 'Заявки на займ'}
        </h2>
        {access && (
          <Link to="/loans/new" className="btn primary">
            + Новая заявка
          </Link>
        )}
      </div>

      {favoritesOnly && (
        <p className="muted small" style={{ marginTop: '-0.35rem' }}>
          <Link to="/loans">← Все заявки</Link>
        </p>
      )}

      {!favoritesOnly && (
        <div className="toolbar" style={{ marginTop: '-0.5rem' }}>
          <label>
            Статус
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setPage(0)
              }}
            >
              <option value="">Все</option>
              <option value="OPEN">Открыта</option>
              <option value="FUNDED">Профинансирована</option>
              <option value="CANCELLED">Отменена</option>
            </select>
          </label>
          {data != null && (
            <span className="muted small">Всего записей: {data.totalElements}</span>
          )}
        </div>
      )}

      {loading ? (
        <p className="muted">Загрузка…</p>
      ) : displayRows.length === 0 ? (
        <div className="card">
          <p className="muted">
            {favoritesOnly
              ? 'Пока нет избранных заявок. Откройте заявку и нажмите звезду.'
              : 'Нет заявок по выбранным условиям.'}
          </p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {access && <th aria-label="Избранное" />}
                {sortableTh('id', 'ID')}
                {sortableTh('amount', 'Сумма')}
                {sortableTh('termMonths', 'Срок')}
                {sortableTh('interestRatePercent', 'Ставка')}
                {sortableTh('reputation', 'Репутация')}
                {sortableTh('status', 'Статус')}
                {sortableTh('funded', 'Собрано')}
                <th />
              </tr>
            </thead>
            <tbody>
              {displayRows.map((r) => {
                const pct =
                  Number(r.amount) > 0
                    ? Math.min(100, Math.round((Number(r.fundedAmount) / Number(r.amount)) * 100))
                    : 0
                const rep = r.borrowerReputationPoints ?? 500
                const isFav = favoritesOnly || favSet.has(r.id)
                return (
                  <tr key={r.id}>
                    {access && (
                      <td style={{ width: '2.5rem' }}>
                        <button
                          type="button"
                          className="favorite-star"
                          aria-label={isFav ? 'Убрать из избранного' : 'В избранное'}
                          aria-pressed={isFav}
                          disabled={toggleFavMut.isPending}
                          onClick={(e) => {
                            e.preventDefault()
                            toggleFavMut.mutate({ id: r.id, add: !isFav })
                          }}
                        >
                          {isFav ? '★' : '☆'}
                        </button>
                      </td>
                    )}
                    <td>
                      <Link to={`/requests/${r.id}`}>#{r.id}</Link>
                    </td>
                    <td>{Number(r.amount).toLocaleString('ru-RU')} BYN</td>
                    <td>{r.termMonths} мес.</td>
                    <td>{r.interestRatePercent}%</td>
                    <td>
                      <span title="Шкала 0–1000: своевременные платежи повышают, просрочка снижает">
                        {rep}{' '}
                        <span className="muted small">({reputationLabel(rep)})</span>
                      </span>
                    </td>
                    <td>
                      <span className={statusBadge(r.status)}>{statusRu(r.status)}</span>
                    </td>
                    <td>
                      <span className="muted small">{pct}%</span>
                      <br />
                      <span className="small">{Number(r.fundedAmount).toLocaleString('ru-RU')} BYN</span>
                    </td>
                    <td>
                      <Link to={`/requests/${r.id}`} className="btn">
                        Открыть
                      </Link>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {!favoritesOnly && data && data.totalPages > 0 && (
        <p className="muted" style={{ marginTop: '1rem' }}>
          Страница {data.page + 1} из {data.totalPages}
          {data.page > 0 && (
            <button type="button" className="primary" style={{ marginLeft: 8 }} onClick={() => setPage((p) => p - 1)}>
              Назад
            </button>
          )}
          {!data.last && (
            <button type="button" className="primary" style={{ marginLeft: 8 }} onClick={() => setPage((p) => p + 1)}>
              Вперёд
            </button>
          )}
        </p>
      )}
    </div>
  )
}
