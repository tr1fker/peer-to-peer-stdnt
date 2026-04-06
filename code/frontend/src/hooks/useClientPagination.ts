import { useEffect, useMemo, useState } from 'react'

/** Клиентская пагинация полного списка (без запросов с page/size на сервер). */
export function useClientPagination<T>(items: T[] | undefined, pageSize: number) {
  const [page, setPage] = useState(0)
  const total = items?.length ?? 0
  const totalPages = total === 0 ? 0 : Math.ceil(total / pageSize)
  const maxPage = Math.max(0, totalPages - 1)

  useEffect(() => {
    setPage((p) => Math.min(p, maxPage))
  }, [maxPage])

  const slice = useMemo(
    () => (items ?? []).slice(page * pageSize, page * pageSize + pageSize),
    [items, page, pageSize],
  )

  const last = totalPages === 0 || page >= maxPage

  return { page, setPage, slice, totalPages, last, total }
}
