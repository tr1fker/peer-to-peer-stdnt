import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import { ClientListPagination } from '../components/ClientListPagination'
import { Modal } from '../components/Modal'
import { useClientPagination } from '../hooks/useClientPagination'

const ADMIN_ACADEMIC_PAGE_SIZE = 10

type PendingRec = {
  recordId: number
  gradeAverage: number | string
  description: string | null
  submittedAt: string
  userId: number
  userEmail: string
  profileFullName: string
  university: string | null
  studentGroup: string | null
  profileVerificationStatus: string
}

function profileStatusRu(s: string) {
  switch (s) {
    case 'VERIFIED':
      return 'Проверен'
    case 'PENDING':
      return 'На проверке'
    case 'REJECTED':
      return 'Отклонён'
    case 'BLOCKED':
      return 'Заблокирован'
    default:
      return s
  }
}

function formatGrade(v: number | string) {
  const n = typeof v === 'string' ? Number(v) : v
  return Number.isFinite(n) ? n.toLocaleString('ru-RU', { maximumFractionDigits: 2 }) : String(v)
}

export function AdminAcademicPage() {
  const qc = useQueryClient()
  const [detail, setDetail] = useState<PendingRec | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const { data } = useQuery({
    queryKey: ['academic-pending'],
    queryFn: async () => (await api.get<PendingRec[]>('/admin/academic-records/pending')).data,
  })
  const pendingPag = useClientPagination(data, ADMIN_ACADEMIC_PAGE_SIZE)
  const verify = useMutation({
    mutationFn: async (recordId: number) => api.post(`/admin/academic-records/${recordId}/verify`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['academic-pending'] })
      qc.invalidateQueries({ queryKey: ['academic-mine'] })
      qc.invalidateQueries({ queryKey: ['profile'] })
      qc.invalidateQueries({ queryKey: ['credit-limit'] })
      setDetail(null)
      setRejectReason('')
    },
  })
  const reject = useMutation({
    mutationFn: async ({ recordId, reason }: { recordId: number; reason: string }) =>
      api.post(`/admin/academic-records/${recordId}/reject`, {
        reason: reason.trim() ? reason.trim() : null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['academic-pending'] })
      qc.invalidateQueries({ queryKey: ['academic-mine'] })
      qc.invalidateQueries({ queryKey: ['profile'] })
      qc.invalidateQueries({ queryKey: ['credit-limit'] })
      setDetail(null)
      setRejectReason('')
    },
  })

  const busy = verify.isPending || reject.isPending

  function closeModal() {
    setDetail(null)
    setRejectReason('')
  }

  return (
    <div>
      <h2>Админ: успеваемость</h2>
      {!data?.length && <p className="muted">Нет записей на проверку</p>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Запись</th>
              <th>Email</th>
              <th>Балл</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {pendingPag.slice.map((r) => (
              <tr key={r.recordId}>
                <td>#{r.recordId}</td>
                <td>{r.userEmail}</td>
                <td>{formatGrade(r.gradeAverage)}</td>
                <td>
                  <button type="button" className="btn" onClick={() => setDetail(r)}>
                    Подробнее
                  </button>{' '}
                  <button type="button" className="primary" onClick={() => verify.mutate(r.recordId)} disabled={busy}>
                    Подтвердить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data && data.length > 0 && (
        <ClientListPagination
          page={pendingPag.page}
          setPage={pendingPag.setPage}
          totalPages={pendingPag.totalPages}
          last={pendingPag.last}
        />
      )}

      <Modal
        title={detail ? `Запись #${detail.recordId}` : ''}
        open={detail != null}
        onClose={closeModal}
        panelClassName="modal-panel--wide"
      >
        {detail && (
          <>
            <p className="detail-modal-lead muted small">
              {detail.profileFullName?.trim() || 'ФИО не указано'} · {detail.userEmail}
            </p>
            <div className="detail-sheet">
              <div className="detail-row">
                <span className="detail-label">Вуз</span>
                <span className="detail-value">{detail.university?.trim() ? detail.university : '—'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Группа</span>
                <span className="detail-value">{detail.studentGroup?.trim() ? detail.studentGroup : '—'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Статус профиля</span>
                <span className="detail-value">{profileStatusRu(detail.profileVerificationStatus)}</span>
              </div>
              <div className="detail-row detail-row--highlight">
                <span className="detail-label">Средний балл</span>
                <span className="detail-value detail-value--strong">{formatGrade(detail.gradeAverage)}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Подано</span>
                <span className="detail-value">{new Date(detail.submittedAt).toLocaleString('ru-RU')}</span>
              </div>
              <div className="detail-row detail-row--stack">
                <span className="detail-label">Комментарий студента</span>
                {detail.description?.trim() ? (
                  <div className="detail-value-block">{detail.description}</div>
                ) : (
                  <span className="detail-value muted">—</span>
                )}
              </div>
            </div>
            <div className="admin-reject-block">
              <h4 className="admin-reject-title">Отклонить запись</h4>
              <p className="muted small" style={{ margin: '0 0 0.5rem' }}>
                Студент увидит статус «Отклонено» и сможет отправить новые данные. Комментарий необязателен.
              </p>
              <label htmlFor="reject-reason">Комментарий для студента</label>
              <textarea
                id="reject-reason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={2}
                placeholder="Например: неверный формат справки"
                disabled={busy}
              />
              {reject.isError && (
                <p className="error small" style={{ marginTop: '0.35rem' }}>
                  {(reject.error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                    'Не удалось отклонить'}
                </p>
              )}
            </div>
            <div className="modal-actions modal-actions--leading">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                Закрыть
              </button>
              <button
                type="button"
                className="primary"
                onClick={() => verify.mutate(detail.recordId)}
                disabled={busy}
              >
                Подтвердить
              </button>
              <button
                type="button"
                className="btn-danger"
                onClick={() => {
                  if (!window.confirm('Отклонить эту запись? Студент увидит отказ и сможет подать данные снова.')) {
                    return
                  }
                  reject.mutate({ recordId: detail.recordId, reason: rejectReason })
                }}
                disabled={busy}
              >
                Отклонить
              </button>
            </div>
          </>
        )}
      </Modal>
    </div>
  )
}
