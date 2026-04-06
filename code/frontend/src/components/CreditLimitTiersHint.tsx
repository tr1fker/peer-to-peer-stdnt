/**
 * Пороги должны совпадать с {@code AverageGradeCreditLimitStrategy} на бэкенде.
 */
const TIERS: { minGrade: string; maxAmount: number }[] = [
  { minGrade: '9,0', maxAmount: 200_000 },
  { minGrade: '8,0', maxAmount: 120_000 },
  { minGrade: '6,0', maxAmount: 60_000 },
]

function fmtByn(n: number) {
  return `${n.toLocaleString('ru-RU')} BYN`
}

type Props = {
  /** Короткий вариант без длинного вводного абзаца */
  compact?: boolean
  /** Без линии сверху (например, в начале карточки) */
  noDivider?: boolean
  /** Только список порогов (если заголовок уже снаружи) */
  hideIntro?: boolean
}

export function CreditLimitTiersHint({ compact, noDivider, hideIntro }: Props) {
  const cls = `credit-tiers-hint${noDivider ? ' credit-tiers-hint--flush' : ''}`
  return (
    <div className={cls} role="note" aria-label="Пороги лимита по среднему баллу">
      {!hideIntro && !compact && (
        <p className="credit-tiers-intro muted small">
          Лимит на одну заявку — по <strong>наивысшему</strong> подтверждённому среднему баллу (10-балльная
          шкала). Ниже — до какой суммы можно претендовать при соответствующем балле:
        </p>
      )}
      {!hideIntro && compact && (
        <p className="credit-tiers-intro muted small" style={{ marginTop: 0 }}>
          Пороги лимита (наибольший подтверждённый средний балл):
        </p>
      )}
      <ul className="credit-tiers-list">
        {TIERS.map(({ minGrade, maxAmount }) => (
          <li key={minGrade}>
            <span className="credit-tiers-grade">от {minGrade}</span>
            <span className="credit-tiers-dash"> — </span>
            <span>до {fmtByn(maxAmount)}</span>
          </li>
        ))}
        <li>
          <span className="credit-tiers-grade">ниже 6,0</span>
          <span className="credit-tiers-dash"> — </span>
          <span>до {fmtByn(20_000)}</span>
        </li>
      </ul>
    </div>
  )
}
