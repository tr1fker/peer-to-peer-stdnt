import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { ClientListPagination } from '../components/ClientListPagination'
import { useClientPagination } from '../hooks/useClientPagination'

const MY_LOANS_PAGE_SIZE = 5
const INSTALLMENTS_PAGE_SIZE = 5

type LoanRequestRow = {
  id: number
  borrowerId: number
  amount: string
  termMonths: number
  purpose: string | null
  status: string
  interestRatePercent: string
  createdAt: string
  fundedAmount: string
}

type Loan = {
  id: number
  loanRequestId: number
  principal: string
  interestRatePercent: string
  startDate: string
  endDate: string
  status: string
}

type Inst = {
  id: number
  installmentNumber: number
  amountDue: string
  dueDate: string
  status: string
}

type Guarantee = {
  id: number
  loanId: number
  guarantorUserId: number | null
  guaranteeType: string
  coverageAmount: string
  status: string
}

type MyInvestment = {
  investmentId: number
  amount: string
  loanRequestId: number
  loanRequestStatus: string
  loanRequestAmount: string
  fundedAmount: string
  loanId: number | null
  createdAt: string
}

type Detail = { mode: 'borrower' | 'lender'; loanId: number }

function instBadge(status: string) {
  if (status === 'PAID') return 'badge badge-paid'
  if (status === 'SCHEDULED') return 'badge badge-scheduled'
  return 'badge'
}

function instStatusRu(status: string) {
  if (status === 'PAID') return 'Оплачен'
  if (status === 'SCHEDULED') return 'К оплате'
  if (status === 'OVERDUE') return 'Просрочен'
  return status
}

function guaranteeTypeRu(t: string) {
  if (t === 'PLATFORM_POOL') return 'Резерв платформы'
  if (t === 'CO_SIGNER') return 'Поручитель'
  return t
}

function coSignerStatusRu(s: string) {
  if (s === 'PENDING') return 'Ожидает ответа поручителя'
  if (s === 'ACTIVE') return 'Поручитель согласился'
  if (s === 'DECLINED') return 'Поручитель отказался'
  if (s === 'RELEASED') return 'Снято'
  if (s === 'CALLED') return 'Исполнено'
  return s
}

function coSignerStatusBadge(s: string) {
  if (s === 'PENDING') return 'badge badge-scheduled'
  if (s === 'ACTIVE') return 'badge badge-paid'
  if (s === 'DECLINED') return 'badge badge-cancel'
  return 'badge'
}

type CoSignerInvitation = {
  guaranteeId: number
  loanId: number
  loanRequestId: number
  borrowerEmail: string
  borrowerFullName: string
  loanPrincipal: string
  coverageAmount: string
  status: string
}

function loanStatusRu(s: string) {
  if (s === 'ACTIVE') return 'Действует'
  if (s === 'COMPLETED') return 'Погашен'
  if (s === 'DEFAULTED') return 'Просрочен'
  return s
}

function requestStatusRu(s: string) {
  if (s === 'OPEN') return 'Открыта'
  if (s === 'FUNDED') return 'Собрана'
  if (s === 'CANCELLED') return 'Отменена'
  return s
}

function requestStatusBadge(s: string) {
  if (s === 'OPEN') return 'badge badge-open'
  if (s === 'FUNDED') return 'badge badge-funded'
  if (s === 'CANCELLED') return 'badge badge-cancel'
  return 'badge'
}

async function downloadPortfolioCsv() {
  const res = await api.get<Blob>('/me/portfolio-export.csv', { responseType: 'blob' })
  const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'peerlend-portfolio.csv'
  a.click()
  URL.revokeObjectURL(url)
}

export function MyLoansPage() {
  const qc = useQueryClient()
  const [detail, setDetail] = useState<Detail | null>(null)
  const [exportErr, setExportErr] = useState<string | null>(null)
  const [exportBusy, setExportBusy] = useState(false)
  const [coGuarantorId, setCoGuarantorId] = useState('')
  const [coCoverage, setCoCoverage] = useState('')
  const [coError, setCoError] = useState<string | null>(null)

  const detailLoanId = detail?.loanId ?? null
  const isBorrowerDetail = detail?.mode === 'borrower'

  const { data: myRequests, isError: myRequestsError, refetch: refetchRequests } = useQuery({
    queryKey: ['me-loan-requests'],
    queryFn: async () => (await api.get<LoanRequestRow[]>('/me/loan-requests')).data,
  })

  const { data: borrowed, isError: borrowedError, refetch: refetchBorrowed } = useQuery({
    queryKey: ['loans-borrower'],
    queryFn: async () => (await api.get<Loan[]>('/me/loans/borrower')).data,
  })

  const detailLoanMeta = borrowed?.find((l) => l.id === detail?.loanId)

  const { data: investments, isError: investmentsError, refetch: refetchInvestments } = useQuery({
    queryKey: ['me-investments'],
    queryFn: async () => (await api.get<MyInvestment[]>('/me/investments')).data,
  })

  const reqPag = useClientPagination(myRequests, MY_LOANS_PAGE_SIZE)
  const borrowedPag = useClientPagination(borrowed, MY_LOANS_PAGE_SIZE)
  const invPag = useClientPagination(investments, MY_LOANS_PAGE_SIZE)

  const { data: installments, error: installmentsError } = useQuery({
    queryKey: ['installments', detailLoanId],
    queryFn: async () => (await api.get<Inst[]>(`/loans/${detailLoanId}/installments`)).data,
    enabled: detailLoanId != null,
  })

  const instPag = useClientPagination(installments, INSTALLMENTS_PAGE_SIZE)
  useEffect(() => {
    instPag.setPage(0)
  }, [detailLoanId])

  const { data: guarantees } = useQuery({
    queryKey: ['guarantees', detailLoanId],
    queryFn: async () => (await api.get<Guarantee[]>(`/loans/${detailLoanId}/guarantees`)).data,
    enabled: detailLoanId != null,
  })

  const { data: guarantorInvites, isError: guarantorInvitesError } = useQuery({
    queryKey: ['guarantor-invitations'],
    queryFn: async () => (await api.get<CoSignerInvitation[]>('/me/guarantor-invitations')).data,
  })

  const payMut = useMutation({
    mutationFn: async ({ loanId, instId }: { loanId: number; instId: number }) =>
      api.post(`/loans/${loanId}/installments/${instId}/pay`),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['installments', vars.loanId] })
      qc.invalidateQueries({ queryKey: ['loans-borrower'] })
    },
  })

  const coSignMut = useMutation({
    mutationFn: async (loanId: number) =>
      api.post(`/loans/${loanId}/guarantees/co-signer`, {
        guarantorUserId: Number(coGuarantorId),
        coverageAmount: coCoverage,
      }),
    onSuccess: (_, loanId) => {
      qc.invalidateQueries({ queryKey: ['guarantees', loanId] })
      qc.invalidateQueries({ queryKey: ['guarantor-invitations'] })
      setCoGuarantorId('')
      setCoCoverage('')
      setCoError(null)
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Не удалось отправить приглашение'
      setCoError(String(msg))
    },
  })

  const acceptGuarantorMut = useMutation({
    mutationFn: async (guaranteeId: number) =>
      (await api.post<Guarantee>(`/me/guarantor-invitations/${guaranteeId}/accept`)).data,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['guarantor-invitations'] })
      qc.invalidateQueries({ queryKey: ['guarantees', data.loanId] })
    },
  })

  const declineGuarantorMut = useMutation({
    mutationFn: async (guaranteeId: number) =>
      (await api.post<Guarantee>(`/me/guarantor-invitations/${guaranteeId}/decline`)).data,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['guarantor-invitations'] })
      qc.invalidateQueries({ queryKey: ['guarantees', data.loanId] })
    },
  })

  function loanIdForRequest(requestId: number): number | undefined {
    return borrowed?.find((l) => l.loanRequestId === requestId)?.id
  }

  const loadError =
    myRequestsError || borrowedError || investmentsError
      ? 'Не удалось загрузить данные. Проверьте вход в систему и попробуйте снова.'
      : null

  const guarantorBusy = acceptGuarantorMut.isPending || declineGuarantorMut.isPending

  return (
    <div>
      <div className="toolbar" style={{ marginBottom: '1rem' }}>
        <h2 style={{ margin: 0, flex: '1 1 auto' }}>Портфель</h2>
        <button
          type="button"
          className="btn"
          disabled={exportBusy}
          onClick={() => {
            setExportErr(null)
            setExportBusy(true)
            void downloadPortfolioCsv()
              .catch((e: unknown) => {
                const msg =
                  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ??
                  'Не удалось скачать файл'
                setExportErr(String(msg))
              })
              .finally(() => setExportBusy(false))
          }}
        >
          {exportBusy ? 'Формируем…' : 'Скачать CSV'}
        </button>
      </div>
      {exportErr && <p className="error small">{exportErr}</p>}
      <p className="muted small" style={{ marginTop: '-0.5rem', marginBottom: '1.25rem' }}>
        CSV (UTF‑8, разделитель «;»): ваши заявки как заёмщика, займы и инвестиции — для отчёта или Excel.
      </p>

      {loadError && (
        <div className="card" style={{ marginBottom: '1rem' }}>
          <p className="error">{loadError}</p>
          <button
            type="button"
            onClick={() => {
              void refetchRequests()
              void refetchBorrowed()
              void refetchInvestments()
            }}
          >
            Повторить
          </button>
        </div>
      )}

      <h2>Меня указали поручителем</h2>
      <p className="muted small" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
        Здесь приглашения от заёмщиков: вы сами решаете, согласиться быть поручителем по займу или отказаться. Пока статус
        «Ожидает ответа», обязательства не действуют. В приложении график платежей по займу оплачивает только заёмщик;
        отдельной кнопки «оплатить как поручитель» нет — сумма покрытия отражает договорённость и прозрачность для
        участников (учебная модель без взыскания с поручителя в коде).
      </p>
      {guarantorInvitesError && (
        <p className="error small" style={{ marginBottom: '1rem' }}>
          Не удалось загрузить приглашения поручителя.
        </p>
      )}
      {!guarantorInvites?.length ? (
        <div className="card" style={{ marginBottom: '2rem' }}>
          <p className="muted">Пока нет записей — вас ни по какому займу не приглашали поручителем.</p>
        </div>
      ) : (
        <div className="guarantor-invites-stack">
          {guarantorInvites.map((inv) => (
            <div key={inv.guaranteeId} className="card guarantor-invite-card">
              <div className="guarantor-invite-meta muted small">
                <Link to={`/requests/${inv.loanRequestId}`}>Заявка №{inv.loanRequestId}</Link>
                <span className="guarantor-invite-meta-sep" aria-hidden>
                  ·
                </span>
                <span>Займ №{inv.loanId}</span>
              </div>
              <div className="guarantor-invite-borrower">
                <div className="guarantor-invite-label muted small">Заёмщик</div>
                <div className="guarantor-invite-borrower-name">{inv.borrowerFullName}</div>
                <div className="guarantor-invite-borrower-email muted small">{inv.borrowerEmail}</div>
              </div>
              <dl className="guarantor-invite-dl">
                <div>
                  <dt>Тело займа</dt>
                  <dd>{Number(inv.loanPrincipal).toLocaleString('ru-RU')} BYN</dd>
                </div>
                <div>
                  <dt>Поручительство</dt>
                  <dd>{Number(inv.coverageAmount).toLocaleString('ru-RU')} BYN</dd>
                </div>
                <div>
                  <dt>Статус</dt>
                  <dd>
                    <span className={coSignerStatusBadge(inv.status)}>{coSignerStatusRu(inv.status)}</span>
                  </dd>
                </div>
              </dl>
              {inv.status === 'PENDING' && (
                <div className="guarantor-invite-card-actions">
                  <div className="guarantor-invites-actions">
                    <button
                      type="button"
                      className="primary"
                      disabled={guarantorBusy}
                      onClick={() => {
                        if (
                          window.confirm(
                            'Согласиться быть поручителем по этому займу на указанную сумму покрытия?',
                          )
                        ) {
                          acceptGuarantorMut.mutate(inv.guaranteeId)
                        }
                      }}
                    >
                      Согласиться
                    </button>
                    <button
                      type="button"
                      className="btn"
                      disabled={guarantorBusy}
                      onClick={() => {
                        if (window.confirm('Отказаться от поручительства по этому приглашению?')) {
                          declineGuarantorMut.mutate(inv.guaranteeId)
                        }
                      }}
                    >
                      Отказаться
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <h2>Мои заявки</h2>
      <p className="muted small" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
        Здесь все ваши заявки как заёмщика. Займ появляется в блоке ниже только после полного сбора суммы.
      </p>
      {!myRequests?.length ? (
        <div className="card">
          <p className="muted">Заявок пока нет.</p>
          <p className="muted small">
            Создайте заявку в разделе «Новая заявка» (при подтверждённой успеваемости).
          </p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Заявка</th>
                <th>Сумма</th>
                <th>Собрано</th>
                <th>Статус</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {reqPag.slice.map((r) => {
                const lid = loanIdForRequest(r.id)
                return (
                  <tr key={r.id}>
                    <td>#{r.id}</td>
                    <td>{Number(r.amount).toLocaleString('ru-RU')} BYN</td>
                    <td className="muted small">
                      {Number(r.fundedAmount).toLocaleString('ru-RU')} / {Number(r.amount).toLocaleString('ru-RU')} BYN
                    </td>
                    <td>
                      <span className={requestStatusBadge(r.status)}>{requestStatusRu(r.status)}</span>
                    </td>
                    <td>
                      <Link to={`/requests/${r.id}`}>Карточка</Link>
                      {lid != null && r.status === 'FUNDED' && (
                        <>
                          {' · '}
                          <button
                            type="button"
                            onClick={() =>
                              setDetail(
                                detail?.mode === 'borrower' && detail.loanId === lid
                                  ? null
                                  : { mode: 'borrower', loanId: lid },
                              )
                            }
                          >
                            {detail?.mode === 'borrower' && detail.loanId === lid ? 'Скрыть займ' : 'Займ'}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      {myRequests && myRequests.length > 0 && (
        <ClientListPagination
          page={reqPag.page}
          setPage={reqPag.setPage}
          totalPages={reqPag.totalPages}
          last={reqPag.last}
        />
      )}

      <h2 style={{ marginTop: '2.25rem' }}>Мои займы (заёмщик)</h2>
      <p className="muted small" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
        Активные договоры после полного финансирования заявки. В таблице ниже в колонке «Тело займа» — сумма
        без процентов. В графике платежей суммы обычно <strong>больше</strong>: это уже полный ежемесячный
        платёж (доля тела + проценты по ставке на весь срок). Месяцы чуть отличаются из‑за округления копеек;
        последний платёж подгоняет итог так, чтобы в сумме вышло «тело + проценты».
      </p>
      {!borrowed?.length ? (
        <div className="card">
          <p className="muted">Пока нет активных займов — заявка должна быть полностью профинансирована.</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Займ</th>
                <th>Заявка</th>
                <th>Тело займа</th>
                <th>Период</th>
                <th>Статус</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {borrowedPag.slice.map((l) => (
                <tr key={l.id}>
                  <td>#{l.id}</td>
                  <td>
                    <Link to={`/requests/${l.loanRequestId}`}>#{l.loanRequestId}</Link>
                  </td>
                  <td>{Number(l.principal).toLocaleString('ru-RU')} BYN</td>
                  <td className="muted small">
                    {l.startDate} → {l.endDate}
                  </td>
                  <td>
                    <span className="badge badge-open">{loanStatusRu(l.status)}</span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className={detail?.mode === 'borrower' && detail.loanId === l.id ? 'primary' : ''}
                      onClick={() =>
                        setDetail(
                          detail?.mode === 'borrower' && detail.loanId === l.id
                            ? null
                            : { mode: 'borrower', loanId: l.id },
                        )
                      }
                    >
                      {detail?.mode === 'borrower' && detail.loanId === l.id ? 'Скрыть' : 'Платежи'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {borrowed && borrowed.length > 0 && (
        <ClientListPagination
          page={borrowedPag.page}
          setPage={borrowedPag.setPage}
          totalPages={borrowedPag.totalPages}
          last={borrowedPag.last}
        />
      )}

      {detail != null && (
        <div className="card loan-panel">
          <h3 style={{ marginTop: 0 }}>
            Займ #{detail.loanId}
            <span className="muted small" style={{ fontWeight: 'normal', marginLeft: '0.5rem' }}>
              {detail.mode === 'borrower' ? '(заёмщик)' : '(инвестор, просмотр)'}
            </span>
          </h3>
          {installmentsError && (
            <p className="error small">
              {(installmentsError as { message?: string }).message ?? 'Не удалось загрузить график'}
            </p>
          )}

          <div className="subgrid two">
            <div>
              <h4 className="muted small" style={{ margin: '0 0 0.5rem', textTransform: 'uppercase' }}>
                График платежей
              </h4>
              <div className="loan-schedule-explainer">
                <p style={{ margin: '0 0 0.5rem' }}>
                  Здесь указана сумма <strong>одного платежа по графику</strong> — не только возврат тела, но и
                  ваши проценты: платформа делит <strong>тело + проценты за весь срок</strong> на число месяцев.
                  Поэтому цифра в строке месяца не равна «телу займа» из таблицы выше.
                </p>
                <p style={{ margin: 0 }}>
                  Округление до копеек делается по месяцам; чаще всего <strong>последний</strong> платёж чуть
                  отличается от остальных — так закрывается остаток, чтобы сумма всех платежей совпала с полным
                  долгом (тело + проценты).
                  {detailLoanMeta != null && (
                    <>
                      {' '}
                      Ставка по этому договору:{' '}
                      <strong>
                        {Number(detailLoanMeta.interestRatePercent).toLocaleString('ru-RU', {
                          maximumFractionDigits: 2,
                        })}
                        % годовых
                      </strong>
                      .
                    </>
                  )}
                </p>
              </div>
              {!installments?.length ? (
                <p className="muted small">{detailLoanId != null ? 'Нет данных' : '—'}</p>
              ) : (
                <div className="table-wrap loan-installments-table-wrap">
                  <table className="loan-installments-table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Платёж</th>
                        <th>Срок</th>
                        <th>Статус</th>
                        {isBorrowerDetail && (
                          <th className="loan-installments-table__actions" aria-label="Действия" />
                        )}
                      </tr>
                    </thead>
                    <tbody>
                      {instPag.slice.map((i) => (
                        <tr key={i.id}>
                          <td>{i.installmentNumber}</td>
                          <td>{Number(i.amountDue).toLocaleString('ru-RU')} BYN</td>
                          <td className="muted small">{i.dueDate}</td>
                          <td>
                            <span className={instBadge(i.status)}>{instStatusRu(i.status)}</span>
                          </td>
                          {isBorrowerDetail && (
                            <td>
                              {i.status === 'SCHEDULED' && (
                                <button
                                  type="button"
                                  className="primary"
                                  disabled={payMut.isPending}
                                  onClick={() => payMut.mutate({ loanId: detail.loanId, instId: i.id })}
                                >
                                  Оплатить
                                </button>
                              )}
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {installments && installments.length > 0 && (
                <ClientListPagination
                  page={instPag.page}
                  setPage={instPag.setPage}
                  totalPages={instPag.totalPages}
                  last={instPag.last}
                />
              )}
            </div>

            <div>
              <h4 className="muted small" style={{ margin: '0 0 0.5rem', textTransform: 'uppercase' }}>
                Гарантии возврата
              </h4>
              {!guarantees?.length ? (
                <p className="muted small">Нет данных</p>
              ) : (
                <ul style={{ margin: 0, paddingLeft: '1.1rem', color: 'var(--muted)', fontSize: '0.88rem' }}>
                  {guarantees.map((g) => (
                    <li key={g.id} style={{ marginBottom: '0.35rem' }}>
                      <strong style={{ color: 'var(--text)' }}>{guaranteeTypeRu(g.guaranteeType)}</strong> —{' '}
                      {Number(g.coverageAmount).toLocaleString('ru-RU')} BYN
                      {g.guaranteeType === 'CO_SIGNER' && (
                        <>
                          {' '}
                          <span className={coSignerStatusBadge(g.status)} style={{ marginLeft: '0.35rem' }}>
                            {coSignerStatusRu(g.status)}
                          </span>
                        </>
                      )}
                      {g.guarantorUserId != null && g.guaranteeType === 'CO_SIGNER' && (
                        <span className="muted"> · поручитель (№{g.guarantorUserId})</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}

              {isBorrowerDetail && (
                <div style={{ marginTop: '1.25rem' }}>
                  <h4 className="muted small" style={{ margin: '0 0 0.5rem', textTransform: 'uppercase' }}>
                    Пригласить поручителя
                  </h4>
                  <p className="muted small" style={{ margin: '0 0 0.5rem' }}>
                    Укажите номер учётной записи пользователя (в его профиле) и сумму покрытия. Ему уйдёт приглашение в
                    блоке «Меня указали поручителем» — до согласия статус в списке гарантий будет «Ожидает ответа
                    поручителя». Сумма всех активных и ожидающих поручительств не может превышать тело займа.
                  </p>
                  <label>Номер учётной записи поручителя</label>
                  <input value={coGuarantorId} onChange={(e) => setCoGuarantorId(e.target.value)} inputMode="numeric" />
                  <label>Сумма покрытия</label>
                  <input value={coCoverage} onChange={(e) => setCoCoverage(e.target.value)} placeholder="10000" />
                  {coError && <p className="error">{coError}</p>}
                  <button
                    type="button"
                    className="primary"
                    disabled={coSignMut.isPending || !coGuarantorId || !coCoverage}
                    onClick={() => coSignMut.mutate(detail.loanId)}
                  >
                    Отправить приглашение
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <h2 style={{ marginTop: '2.25rem' }}>Мои инвестиции</h2>
      <p className="muted small" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
        Вклады в чужие заявки. График платежей доступен после активации займа (статус заявки «Собрана»).
      </p>
      {!investments?.length ? (
        <div className="card">
          <p className="muted">Инвестиций пока нет — откройте заявку и вложите средства (роль кредитора).</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Заявка</th>
                <th>Моя сумма</th>
                <th>Собрано</th>
                <th>Статус заявки</th>
                <th>Займ</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {invPag.slice.map((inv) => {
                const activeLoanId = inv.loanId
                return (
                  <tr key={inv.investmentId}>
                    <td>
                      <Link to={`/requests/${inv.loanRequestId}`}>#{inv.loanRequestId}</Link>
                    </td>
                    <td>{Number(inv.amount).toLocaleString('ru-RU')} BYN</td>
                    <td className="muted small">
                      {Number(inv.fundedAmount).toLocaleString('ru-RU')} /{' '}
                      {Number(inv.loanRequestAmount).toLocaleString('ru-RU')} BYN
                    </td>
                    <td>
                      <span className={requestStatusBadge(inv.loanRequestStatus)}>
                        {requestStatusRu(inv.loanRequestStatus)}
                      </span>
                    </td>
                    <td>{activeLoanId != null ? `#${activeLoanId}` : '—'}</td>
                    <td>
                      {activeLoanId != null ? (
                        <button
                          type="button"
                          className={
                            detail?.mode === 'lender' && detail.loanId === activeLoanId ? 'primary' : ''
                          }
                          onClick={() =>
                            setDetail(
                              detail?.mode === 'lender' && detail.loanId === activeLoanId
                                ? null
                                : { mode: 'lender', loanId: activeLoanId },
                            )
                          }
                        >
                          {detail?.mode === 'lender' && detail.loanId === activeLoanId
                            ? 'Скрыть график'
                            : 'График'}
                        </button>
                      ) : (
                        <span className="muted small">Ожидает сбора</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      {investments && investments.length > 0 && (
        <ClientListPagination
          page={invPag.page}
          setPage={invPag.setPage}
          totalPages={invPag.totalPages}
          last={invPag.last}
        />
      )}
    </div>
  )
}
