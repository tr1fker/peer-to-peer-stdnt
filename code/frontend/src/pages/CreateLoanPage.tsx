import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { CreditLimitTiersHint } from '../components/CreditLimitTiersHint'

function parseAmountInput(raw: string): number | null {
  const t = raw.trim().replace(/\s/g, '').replace(',', '.')
  if (t === '') return null
  const n = Number(t)
  return Number.isFinite(n) ? n : null
}

type AmountIssue = 'empty' | 'bad' | 'low' | 'over'

function getAmountIssue(amount: string, maxAmount: number): AmountIssue | null {
  const t = amount.trim()
  if (t === '') return 'empty'
  const n = parseAmountInput(amount)
  if (n === null) return 'bad'
  if (n <= 0) return 'low'
  if (n > maxAmount) return 'over'
  return null
}

function amountIssueHint(issue: AmountIssue | null, maxFormatted: string): string | null {
  if (issue == null) return null
  switch (issue) {
    case 'empty':
      return 'Введите сумму.'
    case 'bad':
      return 'Введите число (можно с десятичной запятой или точкой).'
    case 'low':
      return 'Сумма должна быть больше нуля.'
    case 'over':
      return `Не больше лимита: ${maxFormatted} BYN.`
    default:
      return null
  }
}

export function CreateLoanPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { data: creditMeta, isLoading } = useQuery({
    queryKey: ['credit-limit'],
    queryFn: async () =>
      (
        await api.get<{
          maxAmount: string
          canCreateLoanRequest?: boolean
          accountBlocked?: boolean
        }>('/me/credit-limit')
      ).data,
  })
  const [amount, setAmount] = useState('')
  const [amountBootstrapped, setAmountBootstrapped] = useState(false)
  const limitPanelRef = useRef<HTMLDivElement>(null)
  const [termMonths, setTermMonths] = useState(12)
  const [purpose, setPurpose] = useState('Учёба')
  const [interestRatePercent, setInterestRatePercent] = useState('12')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (
      !creditMeta ||
      creditMeta.canCreateLoanRequest !== true ||
      creditMeta.accountBlocked === true ||
      amountBootstrapped
    ) {
      return
    }
    const max = Number(creditMeta.maxAmount)
    if (!Number.isFinite(max) || max <= 0) return
    setAmount(String(Math.round(max)))
    setAmountBootstrapped(true)
  }, [creditMeta, amountBootstrapped])

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    const max = creditMeta ? Number(creditMeta.maxAmount) : 0
    if (
      creditMeta?.canCreateLoanRequest === true &&
      creditMeta.accountBlocked !== true &&
      getAmountIssue(amount, max) != null
    ) {
      setError('Исправьте сумму заявки.')
      return
    }
    try {
      await api.post('/loan-requests', {
        amount,
        termMonths,
        purpose,
        interestRatePercent,
      })
      qc.invalidateQueries({ queryKey: ['loan-requests'] })
      qc.invalidateQueries({ queryKey: ['me-loan-requests'] })
      navigate('/loans')
    } catch (ex: unknown) {
      const msg =
        (ex as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Не удалось создать заявку (проверьте лимит и поля)'
      setError(String(msg))
    }
  }

  if (isLoading) {
    return (
      <div className="card">
        <h2>Новая заявка на займ</h2>
        <p className="muted">Загрузка…</p>
      </div>
    )
  }

  const accountBlocked = creditMeta?.accountBlocked === true
  const canCreate = creditMeta?.canCreateLoanRequest === true && !accountBlocked
  const maxAmountNum = creditMeta != null && canCreate ? Number(creditMeta.maxAmount) : 0
  const limitText =
    creditMeta != null && canCreate
      ? `${Number(creditMeta.maxAmount).toLocaleString('ru-RU')} BYN`
      : null
  const amountIssue = canCreate && Number.isFinite(maxAmountNum) ? getAmountIssue(amount, maxAmountNum) : null
  const amountInvalid = amountIssue != null
  const maxAmountFormatted =
    creditMeta != null && canCreate ? Number(creditMeta.maxAmount).toLocaleString('ru-RU') : ''

  function bumpLimitPanel() {
    const el = limitPanelRef.current
    if (!el) return
    el.classList.remove('loan-new-status--shake')
    void el.offsetWidth
    el.classList.add('loan-new-status--shake')
  }

  function handleAmountChange(value: string) {
    setAmount(value)
    if (!canCreate || !Number.isFinite(maxAmountNum)) return
    const issue = getAmountIssue(value, maxAmountNum)
    if (issue != null) {
      bumpLimitPanel()
    }
  }

  return (
    <div className={`card loan-new-card${canCreate ? '' : ' loan-new-card--blocked'}`}>
      <div className="loan-new-grid">
        <div className="loan-new-main-col">
          <h2 className="loan-new-title">Новая заявка на займ</h2>
          {accountBlocked ? (
            <div className="loan-new-status loan-new-status--warn" role="alert">
              <p className="loan-new-status-title">Аккаунт заблокирован</p>
              <p className="muted small loan-new-status-text">
                Администратор ограничил заёмщикские действия: новые заявки и подача успеваемости недоступны. Вход
                в систему сохраняется. По вопросам обратитесь в поддержку.
              </p>
            </div>
          ) : canCreate && limitText != null ? (
            <div
              ref={limitPanelRef}
              className="loan-new-status loan-new-status--ok loan-new-status--limit-panel"
              role="status"
              onAnimationEnd={(e) => {
                if (e.animationName === 'loan-limit-shake' && e.target === limitPanelRef.current) {
                  limitPanelRef.current?.classList.remove('loan-new-status--shake')
                }
              }}
            >
              <p className="loan-new-status-title">Доступный лимит</p>
              <p className="loan-new-limit-value">
                до <strong>{limitText}</strong> на одну заявку
              </p>
              <p className="muted small loan-new-status-note">
                Сумма ниже не должна превышать этот лимит (по подтверждённой успеваемости).
              </p>
            </div>
          ) : (
            <div className="loan-new-status loan-new-status--warn" role="alert">
              <p className="loan-new-status-title">Успеваемость не подтверждена</p>
              <p className="muted small loan-new-status-text">
                Создать заявку можно только после того, как администратор подтвердит хотя бы одну запись об
                успеваемости. Заполните раздел «Успеваемость» и дождитесь проверки.
              </p>
              <Link to="/academic" className="btn primary loan-new-status-link">
                Перейти к успеваемости
              </Link>
            </div>
          )}
          <form
            className="loan-new-form"
            onSubmit={(e) => {
              if (!canCreate) {
                e.preventDefault()
                return
              }
              onSubmit(e)
            }}
          >
            <div className="form-row-with-hint">
              <div className="form-row-main">
                <label htmlFor="loan-amount">Сумма</label>
                <input
                  id="loan-amount"
                  value={amount}
                  onChange={(e) => handleAmountChange(e.target.value)}
                  required
                  inputMode="decimal"
                  disabled={!canCreate}
                  aria-invalid={amountInvalid}
                  className={amountInvalid ? 'input-invalid' : undefined}
                />
                {canCreate && amountIssue != null && (
                  <p className="error small loan-amount-field-error" role="alert">
                    {amountIssueHint(amountIssue, maxAmountFormatted)}
                  </p>
                )}
              </div>
              <p className="form-row-hint muted small">
                Сумма в <strong>BYN</strong>. Не больше вашего лимита по подтверждённой успеваемости. После
                публикации инвесторы смогут вносить деньги частями, пока не наберётся нужная сумма.
              </p>
            </div>
            <div className="form-row-with-hint">
              <div className="form-row-main">
                <label htmlFor="loan-term">Срок (месяцев)</label>
                <input
                  id="loan-term"
                  type="number"
                  min={1}
                  value={termMonths}
                  onChange={(e) => setTermMonths(Number(e.target.value))}
                  required
                  disabled={!canCreate}
                />
              </div>
              <p className="form-row-hint muted small">
                На сколько месяцев вы планируете вернуть займ. От срока зависит распределение платежей по
                графику после полного сбора заявки.
              </p>
            </div>
            <div className="form-row-with-hint">
              <div className="form-row-main">
                <label htmlFor="loan-purpose">Цель</label>
                <input
                  id="loan-purpose"
                  value={purpose}
                  onChange={(e) => setPurpose(e.target.value)}
                  placeholder="Например: оплата семестра"
                  disabled={!canCreate}
                />
              </div>
              <p className="form-row-hint muted small">
                Кратко опишите, на что пойдут средства. Это видят инвесторы при выборе заявки — чем яснее
                цель, тем проще принять решение.
              </p>
            </div>
            <div className="form-row-with-hint">
              <div className="form-row-main">
                <label htmlFor="loan-rate">Ставка % годовых (для инвесторов)</label>
                <input
                  id="loan-rate"
                  value={interestRatePercent}
                  onChange={(e) => setInterestRatePercent(e.target.value)}
                  required
                  inputMode="decimal"
                  disabled={!canCreate}
                />
              </div>
              <p className="form-row-hint muted small">
                Годовая процентная ставка, которую вы предлагаете инвесторам. Выше ставка обычно привлекает
                быстрее, но увеличивает полную стоимость займа для вас.
              </p>
            </div>
            {error && <p className="error">{error}</p>}
            <div className="form-actions">
              <button type="submit" className="primary" disabled={!canCreate || amountInvalid}>
                Создать
              </button>
            </div>
          </form>
        </div>
        <aside className="loan-new-aside" aria-label="Справка по заявке">
          <div className="loan-new-aside-block">
            <h3 className="loan-new-aside-heading">Зачем эта страница</h3>
            <p className="loan-new-aside-text muted small">
              Вы создаёте <strong>открытую заявку</strong>: она появляется в общем списке, и инвесторы могут
              вкладывать в неё любые суммы в пределах остатка. Когда наберётся запрошенная сумма, заявка
              переходит к оформлению займа по правилам платформы.
            </p>
            <p className="loan-new-aside-text muted small" style={{ marginBottom: 0 }}>
              До сбора средств заявку можно отменить (если ещё нет инвестиций). После публикации следите за
              разделом «Мои займы» и уведомлениями.
            </p>
          </div>
          <div className="loan-new-aside-block loan-new-aside-block--tiers">
            <h3 className="loan-new-aside-heading">Лимит по успеваемости</h3>
            <p className="muted small loan-new-tiers-lead">
              Действует <strong>наивысший</strong> подтверждённый средний балл (10-балльная шкала):
            </p>
            <CreditLimitTiersHint hideIntro noDivider />
          </div>
        </aside>
      </div>
    </div>
  )
}
