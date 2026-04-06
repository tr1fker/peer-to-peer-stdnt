import type { Dispatch, SetStateAction } from 'react'

type Props = {
  page: number
  setPage: Dispatch<SetStateAction<number>>
  totalPages: number
  last: boolean
}

/** Та же схема, что на странице «Заявки» (LoanRequestsPage). */
export function ClientListPagination({ page, setPage, totalPages, last }: Props) {
  if (totalPages <= 0) return null

  return (
    <p className="muted" style={{ marginTop: '1rem' }}>
      Страница {page + 1} из {totalPages}
      {page > 0 && (
        <button type="button" className="primary" style={{ marginLeft: 8 }} onClick={() => setPage((p) => p - 1)}>
          Назад
        </button>
      )}
      {!last && (
        <button type="button" className="primary" style={{ marginLeft: 8 }} onClick={() => setPage((p) => p + 1)}>
          Вперёд
        </button>
      )}
    </p>
  )
}
