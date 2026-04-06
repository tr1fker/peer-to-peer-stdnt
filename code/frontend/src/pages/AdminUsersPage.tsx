import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import { ClientListPagination } from '../components/ClientListPagination'
import { Modal } from '../components/Modal'
import { useClientPagination } from '../hooks/useClientPagination'

const ADMIN_USERS_PAGE_SIZE = 10

type AdminUser = {
  id: number
  email: string
  enabled: boolean
  blocked: boolean
  blockedReason: string | null
  blockedAt: string | null
  roles: string[]
  fullName: string
}

type AdminUserDetail = {
  id: number
  email: string
  enabled: boolean
  emailVerified: boolean
  createdAt: string
  blocked: boolean
  blockedReason: string | null
  blockedAt: string | null
  roles: string[]
  fullName: string
  university: string | null
  studentGroup: string | null
  profileVerificationStatus: string
  verifiedAcademicRecordsCount: number
  pendingAcademicRecordsCount: number
  openLoanRequestsCount: number
  activeLoansCount: number
}

/** Как на странице «Профиль»: русские подписи и цвета ролей. */
function normalizeRoleKey(role: string): string {
  const t = role.trim()
  if (t.startsWith('ROLE_')) {
    return t
  }
  return `ROLE_${t}`
}

function roleDisplay(role: string): { label: string; cls: string } {
  switch (normalizeRoleKey(role)) {
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

function RoleChips({ roles }: { roles: string[] }) {
  const sorted = [...roles].sort((a, b) => normalizeRoleKey(a).localeCompare(normalizeRoleKey(b)))
  return (
    <div className="role-chips">
      {sorted.map((r) => {
        const { label, cls } = roleDisplay(r)
        return (
          <span key={r} className={cls}>
            {label}
          </span>
        )
      })}
    </div>
  )
}

function profileVerificationRu(s: string) {
  switch (s) {
    case 'VERIFIED':
      return 'Подтверждено'
    case 'REJECTED':
      return 'Отклонено'
    case 'BLOCKED':
      return 'Заблокирован'
    default:
      return 'На проверке'
  }
}

function formatTs(iso: string | null) {
  if (iso == null || iso === '') return '—'
  try {
    return new Date(iso).toLocaleString('ru-RU')
  } catch {
    return iso
  }
}

export function AdminUsersPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: async () => (await api.get<AdminUser[]>('/admin/users')).data,
  })
  const usersPag = useClientPagination(data, ADMIN_USERS_PAGE_SIZE)
  const [detailUserId, setDetailUserId] = useState<number | null>(null)
  const [blockReason, setBlockReason] = useState('')
  const [revokeReason, setRevokeReason] = useState('')
  const [revokeNotice, setRevokeNotice] = useState<string | null>(null)

  const detailQuery = useQuery({
    queryKey: ['admin-user', detailUserId],
    queryFn: async () => (await api.get<AdminUserDetail>(`/admin/users/${detailUserId}`)).data,
    enabled: detailUserId != null,
  })

  const closeDetail = () => {
    setDetailUserId(null)
    setBlockReason('')
    setRevokeReason('')
    setRevokeNotice(null)
  }

  const blockMut = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) =>
      api.post(`/admin/users/${id}/block`, { reason: reason.trim() ? reason.trim() : null }),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['admin-users'] })
      qc.invalidateQueries({ queryKey: ['admin-user', vars.id] })
      qc.invalidateQueries({ queryKey: ['academic-pending'] })
      setBlockReason('')
    },
  })

  const unblockMut = useMutation({
    mutationFn: async (id: number) => api.post(`/admin/users/${id}/unblock`),
    onSuccess: (_, id) => {
      qc.invalidateQueries({ queryKey: ['admin-users'] })
      qc.invalidateQueries({ queryKey: ['admin-user', id] })
    },
  })

  const revokeMut = useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) =>
      (
        await api.post<{ revokedCount: number }>(`/admin/users/${id}/revoke-verified-academic`, {
          reason: reason.trim() ? reason.trim() : null,
        })
      ).data,
    onSuccess: (res, vars) => {
      qc.invalidateQueries({ queryKey: ['admin-users'] })
      qc.invalidateQueries({ queryKey: ['admin-user', vars.id] })
      qc.invalidateQueries({ queryKey: ['academic-pending'] })
      setRevokeReason('')
      setRevokeNotice(
        res.revokedCount > 0
          ? `Подтверждение снято с ${res.revokedCount} записей. Счётчики и статус профиля обновлены.`
          : 'Подтверждённых записей не было — изменений нет.',
      )
    },
  })

  const detail = detailQuery.data
  const isTargetAdmin = detail?.roles.includes('ROLE_ADMIN') ?? false

  return (
    <div>
      <h2>Админ: пользователи</h2>
      <p className="muted small" style={{ marginTop: '-0.35rem', marginBottom: '1rem' }}>
        Блокировка — не бан: пользователь может войти, но не может подавать успеваемость, создавать заявки и
        добавлять поручителя. Все неотклонённые записи успеваемости помечаются отклонёнными; в профиле статус
        «Заблокирован». Отдельно можно только снять подтверждение с уже проверенных записей успеваемости (без
        блокировки аккаунта) — лимит займа пересчитается.
      </p>
      {isLoading && <p className="muted">Загрузка…</p>}
      {!isLoading && !data?.length && <p className="muted">Нет пользователей</p>}
      {!!data?.length && (
        <div className="table-wrap admin-users-table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>ФИО</th>
                <th>Роли</th>
                <th>Статус</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {usersPag.slice.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.email}</td>
                  <td>{u.fullName}</td>
                  <td className="admin-users-td-roles">
                    <RoleChips roles={u.roles} />
                  </td>
                  <td>
                    {u.blocked ? (
                      <span className="badge badge-cancel">Заблокирован</span>
                    ) : (
                      <span className="badge badge-open">Активен</span>
                    )}
                  </td>
                  <td>
                    <button type="button" className="btn primary" onClick={() => setDetailUserId(u.id)}>
                      Подробнее
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.length > 0 && (
        <ClientListPagination
          page={usersPag.page}
          setPage={usersPag.setPage}
          totalPages={usersPag.totalPages}
          last={usersPag.last}
        />
      )}

      <Modal
        title={detail ? `Пользователь: ${detail.email}` : 'Пользователь'}
        open={detailUserId != null}
        onClose={closeDetail}
        panelClassName="modal-panel--wide"
      >
        {detailUserId != null && detailQuery.isLoading && <p className="muted">Загрузка…</p>}
        {detailUserId != null && detailQuery.isError && (
          <p className="error small">Не удалось загрузить данные пользователя.</p>
        )}
        {detail && (
          <div className="admin-user-detail">
            <div className="admin-user-detail-grid">
              <div>
                <span className="admin-user-detail-label">ID</span>
                <span className="admin-user-detail-value">{detail.id}</span>
              </div>
              <div>
                <span className="admin-user-detail-label">Email</span>
                <span className="admin-user-detail-value">{detail.email}</span>
              </div>
              <div>
                <span className="admin-user-detail-label">ФИО</span>
                <span className="admin-user-detail-value">{detail.fullName}</span>
              </div>
              <div className="admin-user-detail-roles">
                <span className="admin-user-detail-label">Роли</span>
                <RoleChips roles={detail.roles} />
              </div>
              <div>
                <span className="admin-user-detail-label">Учёба</span>
                <span className="admin-user-detail-value">
                  {detail.university?.trim() ? detail.university : '—'}
                  {detail.studentGroup?.trim() ? `, ${detail.studentGroup}` : ''}
                </span>
              </div>
              <div>
                <span className="admin-user-detail-label">Статус успеваемости в профиле</span>
                <span className="admin-user-detail-value">{profileVerificationRu(detail.profileVerificationStatus)}</span>
              </div>
              <div>
                <span className="admin-user-detail-label">Аккаунт</span>
                <span className="admin-user-detail-value">
                  {detail.enabled ? 'Включён' : 'Выключен'}, email: {detail.emailVerified ? 'подтверждён' : 'не подтверждён'}
                </span>
              </div>
              <div>
                <span className="admin-user-detail-label">Регистрация</span>
                <span className="admin-user-detail-value">{formatTs(detail.createdAt)}</span>
              </div>
              <div>
                <span className="admin-user-detail-label">Блокировка</span>
                <span className="admin-user-detail-value">
                  {detail.blocked ? (
                    <>
                      да, с {formatTs(detail.blockedAt)}
                      {detail.blockedReason?.trim() ? ` — ${detail.blockedReason}` : ''}
                    </>
                  ) : (
                    'нет'
                  )}
                </span>
              </div>
              <div>
                <span className="admin-user-detail-label">Успеваемость</span>
                <span className="admin-user-detail-value">
                  подтверждённых записей: <strong>{detail.verifiedAcademicRecordsCount}</strong>, в очереди:{' '}
                  <strong>{detail.pendingAcademicRecordsCount}</strong>
                </span>
              </div>
              <div>
                <span className="admin-user-detail-label">Займы</span>
                <span className="admin-user-detail-value">
                  открытых заявок: <strong>{detail.openLoanRequestsCount}</strong>, активных займов:{' '}
                  <strong>{detail.activeLoansCount}</strong>
                </span>
              </div>
            </div>

            <div className="admin-user-detail-actions">
              <h4 className="admin-user-detail-actions-title">Действия</h4>

              {isTargetAdmin && (
                <p className="muted small">К учётной записи администратора массовые ограничения не применяются.</p>
              )}

              {!isTargetAdmin && (
                <>
                  <div className="admin-user-action-block">
                    <p className="admin-user-action-heading">Снять подтверждение успеваемости</p>
                    <p className="muted small" style={{ marginTop: 0 }}>
                      Все записи, которые сейчас <strong>подтверждены</strong> администратором, будут помечены как
                      отклонённые (как при ручном отклонении). Ожидающие проверки записи не трогаем. Статус в профиле и
                      лимит займа пересчитаются. Блокировка входа не выполняется.
                    </p>
                    <label htmlFor="revoke-reason">Комментарий в записях (необязательно)</label>
                    <textarea
                      id="revoke-reason"
                      rows={2}
                      value={revokeReason}
                      onChange={(e) => setRevokeReason(e.target.value)}
                      placeholder="Будет добавлено к служебной пометке"
                      disabled={revokeMut.isPending}
                    />
                    <button
                      type="button"
                      className="btn"
                      disabled={revokeMut.isPending || detail.verifiedAcademicRecordsCount < 1}
                      onClick={() => {
                        if (
                          !window.confirm(
                            `Снять подтверждение со всех проверенных записей успеваемости (${detail.verifiedAcademicRecordsCount} шт.)?`,
                          )
                        ) {
                          return
                        }
                        setRevokeNotice(null)
                        revokeMut.mutate({ id: detail.id, reason: revokeReason })
                      }}
                    >
                      Снять подтверждение с проверенных записей
                    </button>
                    {revokeNotice != null && <p className="muted small admin-user-revoke-notice">{revokeNotice}</p>}
                    {revokeMut.isError && (
                      <p className="error small">
                        {(revokeMut.error as { response?: { data?: { message?: string } } })?.response?.data
                          ?.message ?? 'Не удалось выполнить операцию'}
                      </p>
                    )}
                  </div>

                  <div className="admin-user-action-block">
                    <p className="admin-user-action-heading">Блокировка заёмщика</p>
                    <p className="muted small" style={{ marginTop: 0 }}>
                      Ограничивает заёмщикские действия и отклоняет все неотклонённые записи успеваемости.
                    </p>
                    {detail.blocked ? (
                      <button
                        type="button"
                        className="btn"
                        disabled={unblockMut.isPending}
                        onClick={() => {
                          if (window.confirm(`Снять блокировку с ${detail.email}?`)) {
                            unblockMut.mutate(detail.id)
                          }
                        }}
                      >
                        Разблокировать
                      </button>
                    ) : (
                      <>
                        <label htmlFor="detail-block-reason">Комментарий (необязательно)</label>
                        <textarea
                          id="detail-block-reason"
                          rows={3}
                          value={blockReason}
                          onChange={(e) => setBlockReason(e.target.value)}
                          placeholder="Причина для внутреннего учёта"
                          disabled={blockMut.isPending}
                        />
                        {blockMut.isError && (
                          <p className="error small">
                            {(blockMut.error as { response?: { data?: { message?: string } } })?.response?.data
                              ?.message ?? 'Не удалось заблокировать'}
                          </p>
                        )}
                        <button
                          type="button"
                          className="btn-danger"
                          disabled={blockMut.isPending}
                          onClick={() => {
                            if (
                              !window.confirm(
                                `Заблокировать пользователя ${detail.email}? Будут отклонены неотклонённые записи успеваемости.`,
                              )
                            ) {
                              return
                            }
                            blockMut.mutate({ id: detail.id, reason: blockReason })
                          }}
                        >
                          Заблокировать
                        </button>
                      </>
                    )}
                  </div>
                </>
              )}
            </div>

            <div className="modal-actions modal-actions--leading" style={{ marginTop: '1rem' }}>
              <button type="button" className="btn" onClick={closeDetail}>
                Закрыть
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
