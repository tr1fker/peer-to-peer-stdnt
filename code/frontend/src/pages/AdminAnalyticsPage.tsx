import { useQuery } from '@tanstack/react-query'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api/client'

type LabelCount = { label: string; count: number }
type DailyCount = { date: string; count: number }

type Overview = {
  totals: {
    users: number
    emailVerifiedUsers: number
    blockedUsers: number
    loanRequests: number
    loans: number
  }
  loanRequestsByStatus: LabelCount[]
  loansByStatus: LabelCount[]
  usersByRole: LabelCount[]
  newUsersByDay: DailyCount[]
  newLoanRequestsByDay: DailyCount[]
}

const CHART_COLORS = ['#3d8bfd', '#34c759', '#ffb020', '#ff5c5c', '#a78bfa', '#8b9bb8']

/** Подсказки Recharts по умолчанию красят значение в чёрный — на тёмном фоне не читается. */
const CHART_TOOLTIP = {
  contentStyle: {
    background: '#131b2e',
    border: '1px solid #2a3a5c',
    borderRadius: 8,
    color: '#e8edf7',
  },
  labelStyle: { color: '#e8edf7' },
  itemStyle: { color: '#e8edf7' },
} as const

function formatDayLabel(isoDate: string) {
  try {
    const d = new Date(isoDate + 'T12:00:00Z')
    return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })
  } catch {
    return isoDate
  }
}

export function AdminAnalyticsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-analytics-overview'],
    queryFn: async () => (await api.get<Overview>('/admin/analytics/overview')).data,
  })

  if (isLoading) {
    return (
      <div className="page-block">
        <p className="muted">Загрузка статистики…</p>
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="page-block">
        <p className="form-error">Не удалось загрузить аналитику. Проверьте права администратора и соединение.</p>
      </div>
    )
  }

  const usersTimeline = data.newUsersByDay.map((row) => ({
    ...row,
    dayLabel: formatDayLabel(row.date),
  }))
  const requestsTimeline = data.newLoanRequestsByDay.map((row) => ({
    ...row,
    dayLabel: formatDayLabel(row.date),
  }))

  return (
    <div className="page-block admin-analytics">
      <header className="admin-analytics-header">
        <h1>Аналитика</h1>
        <p className="muted">
          Сводка по пользователям, заявкам и займам. Динамика за последние 14 дней (UTC).
        </p>
      </header>

      <section className="admin-analytics-kpis" aria-label="Ключевые показатели">
        <div className="admin-analytics-kpi">
          <span className="admin-analytics-kpi-value">{data.totals.users}</span>
          <span className="admin-analytics-kpi-label">Пользователей</span>
        </div>
        <div className="admin-analytics-kpi">
          <span className="admin-analytics-kpi-value">{data.totals.emailVerifiedUsers}</span>
          <span className="admin-analytics-kpi-label">С подтверждённой почтой</span>
        </div>
        <div className="admin-analytics-kpi">
          <span className="admin-analytics-kpi-value">{data.totals.blockedUsers}</span>
          <span className="admin-analytics-kpi-label">Заблокировано</span>
        </div>
        <div className="admin-analytics-kpi">
          <span className="admin-analytics-kpi-value">{data.totals.loanRequests}</span>
          <span className="admin-analytics-kpi-label">Заявок всего</span>
        </div>
        <div className="admin-analytics-kpi">
          <span className="admin-analytics-kpi-value">{data.totals.loans}</span>
          <span className="admin-analytics-kpi-label">Займов всего</span>
        </div>
      </section>

      <div className="admin-analytics-grid">
        <section className="card admin-analytics-chart-card">
          <h2>Заявки по статусу</h2>
          <div className="admin-analytics-chart-wrap" role="img" aria-label="Круговая диаграмма заявок по статусу">
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={data.loanRequestsByStatus}
                  dataKey="count"
                  nameKey="label"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                >
                  {data.loanRequestsByStatus.map((_, i) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip {...CHART_TOOLTIP} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="card admin-analytics-chart-card">
          <h2>Займы по статусу</h2>
          <div className="admin-analytics-chart-wrap" role="img" aria-label="Столбчатая диаграмма займов по статусу">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.loansByStatus} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a3a5c" />
                <XAxis dataKey="label" tick={{ fill: '#8b9bb8', fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fill: '#8b9bb8', fontSize: 12 }} />
                <Tooltip {...CHART_TOOLTIP} />
                <Bar dataKey="count" fill="#3d8bfd" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="card admin-analytics-chart-card">
          <h2>Роли пользователей</h2>
          <p className="muted admin-analytics-chart-hint">Сколько учётных записей имеет каждую роль (один человек может быть и заёмщиком, и инвестором).</p>
          <div className="admin-analytics-chart-wrap" role="img" aria-label="Круговая диаграмма ролей">
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={data.usersByRole}
                  dataKey="count"
                  nameKey="label"
                  cx="50%"
                  cy="50%"
                  innerRadius={48}
                  outerRadius={88}
                  paddingAngle={2}
                >
                  {data.usersByRole.map((_, i) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Legend wrapperStyle={{ color: '#8b9bb8' }} />
                <Tooltip {...CHART_TOOLTIP} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="card admin-analytics-chart-card admin-analytics-chart-card-wide">
          <h2>Новые пользователи по дням</h2>
          <div className="admin-analytics-chart-wrap admin-analytics-chart-wrap-tall" role="img" aria-label="Линия регистраций по дням">
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={usersTimeline} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a3a5c" />
                <XAxis dataKey="dayLabel" tick={{ fill: '#8b9bb8', fontSize: 11 }} interval="preserveStartEnd" />
                <YAxis allowDecimals={false} tick={{ fill: '#8b9bb8', fontSize: 12 }} />
                <Tooltip {...CHART_TOOLTIP} />
                <Line type="monotone" dataKey="count" name="Регистрации" stroke="#34c759" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="card admin-analytics-chart-card admin-analytics-chart-card-wide">
          <h2>Новые заявки по дням</h2>
          <div className="admin-analytics-chart-wrap admin-analytics-chart-wrap-tall" role="img" aria-label="Линия новых заявок по дням">
            <ResponsiveContainer width="100%" height={320}>
              <LineChart data={requestsTimeline} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a3a5c" />
                <XAxis dataKey="dayLabel" tick={{ fill: '#8b9bb8', fontSize: 11 }} interval="preserveStartEnd" />
                <YAxis allowDecimals={false} tick={{ fill: '#8b9bb8', fontSize: 12 }} />
                <Tooltip {...CHART_TOOLTIP} />
                <Line type="monotone" dataKey="count" name="Заявки" stroke="#3d8bfd" strokeWidth={2} dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>
    </div>
  )
}
