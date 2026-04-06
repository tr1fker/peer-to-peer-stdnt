import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { CreditLimitTiersHint } from '../components/CreditLimitTiersHint'
import { useAuthStore } from '../store/authStore'

type RateOk = { from?: string; to?: string; rate?: number | string }
type RateErr = { error?: string }

function parseRate(data: unknown): { state: 'ok'; text: string } | { state: 'unavailable' } {
  if (!data || typeof data !== 'object') return { state: 'unavailable' }
  const o = data as RateOk & RateErr
  if (o.error === 'RATE_UNAVAILABLE' || o.rate == null) return { state: 'unavailable' }
  const n = typeof o.rate === 'string' ? Number(o.rate) : o.rate
  if (!Number.isFinite(n)) return { state: 'unavailable' }
  const from = o.from ?? 'USD'
  const to = o.to ?? 'BYN'
  const formatted = n >= 1 ? n.toLocaleString('ru-RU', { maximumFractionDigits: 2 }) : String(n)
  return { state: 'ok', text: `1 ${from} ≈ ${formatted} ${to}` }
}

export function HomePage() {
  const access = useAuthStore((s) => s.accessToken)
  const { data: rateRaw } = useQuery({
    queryKey: ['usd-byn'],
    queryFn: async () => (await api.get('/rates/usd-byn')).data,
    retry: 1,
    staleTime: 60_000,
  })

  const { data: limit } = useQuery({
    queryKey: ['credit-limit'],
    queryFn: async () =>
      (await api.get<{ maxAmount: string; canCreateLoanRequest?: boolean }>('/me/credit-limit')).data,
    enabled: !!access,
  })

  const rate = parseRate(rateRaw)

  return (
    <div>
      <section className="hero">
        <h1>P2P-кредитование студентов</h1>
        <p className="hero-lead">
          Лимит по успеваемости, сбор инвестиций, график платежей и гарантии платформы.
        </p>
        <div className="hero-actions">
          <Link to="/loans" className="btn primary">
            Смотреть заявки
          </Link>
          {!access && (
            <Link to="/register" className="btn">
              Регистрация
            </Link>
          )}
        </div>
      </section>

      <div className="feature-grid">
        <div className="feature-card">
          <h3>Успеваемость</h3>
          <p>Средний балл по 10-балльной шкале и проверка — пересчёт лимита. Пороги сумм см. в блоке «Ваш лимит» ниже или на странице «Успеваемость».</p>
        </div>
        <div className="feature-card">
          <h3>Инвестиции</h3>
          <p>Инвестиции частями в открытые заявки.</p>
        </div>
        <div className="feature-card">
          <h3>Гарантии</h3>
          <p>Резерв платформы и поручитель.</p>
        </div>
      </div>

      <div className="card card-compact">
        <h3 className="card-title-inline">Справочно: курс USD (НБ РБ)</h3>
        {rate.state === 'ok' ? (
          <>
            <p className="rate-line">{rate.text}</p>
            <p className="muted small">Курс НБ РБ. Суммы в BYN.</p>
          </>
        ) : (
          <p className="muted">Курс временно недоступен. На займы не влияет.</p>
        )}
      </div>

      {limit && (
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Ваш лимит</h3>
          {limit.canCreateLoanRequest === true ? (
            <>
              <p className="limit-line">
                До <strong>{Number(limit.maxAmount).toLocaleString('ru-RU')} BYN</strong> на одну заявку
              </p>
              <p className="muted small">По подтверждённой успеваемости.</p>
            </>
          ) : (
            <>
              <p className="limit-line">
                После подтверждения записи — до{' '}
                <strong>{Number(limit.maxAmount).toLocaleString('ru-RU')} BYN</strong> на одну заявку
              </p>
              <p className="muted small">
                Сейчас создать заявку нельзя. Раздел «Успеваемость» — подайте данные и дождитесь проверки.
              </p>
            </>
          )}
          <CreditLimitTiersHint />
        </div>
      )}
    </div>
  )
}
