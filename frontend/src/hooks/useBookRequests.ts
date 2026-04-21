import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchMyBookRequests,
  parseBookUrl,
  createBookRequest,
  deleteBookRequest,
  lockBookRequests,
  unlockBookRequests,
  type BookCategory,
} from '@/api/bookRequests'

export function useMyBookRequests(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['bookRequests', 'my', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchMyBookRequests(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

export function useParseBookUrl(clubId: number) {
  return useMutation({
    mutationFn: (url: string) => parseBookUrl(clubId, url),
  })
}

export function useCreateBookRequest(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { url: string; category: BookCategory }) =>
      createBookRequest(clubId, vars.url, vars.category),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookRequests', 'my', clubId] })
    },
  })
}

function invalidateBookRequestsAll(qc: ReturnType<typeof useQueryClient>, clubId: number) {
  qc.invalidateQueries({ queryKey: ['bookRequests', 'my', clubId] })
  qc.invalidateQueries({ queryKey: ['admin', 'bookRequests', clubId] })
  qc.invalidateQueries({ queryKey: ['admin', 'order', clubId] })
}

export function useDeleteBookRequest(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteBookRequest(clubId, id),
    onSuccess: () => invalidateBookRequestsAll(qc, clubId),
  })
}

export function useLockBookRequests(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (yearMonth?: string) => lockBookRequests(clubId, yearMonth),
    onSuccess: () => invalidateBookRequestsAll(qc, clubId),
  })
}

export function useUnlockBookRequests(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (yearMonth?: string) => unlockBookRequests(clubId, yearMonth),
    onSuccess: () => invalidateBookRequestsAll(qc, clubId),
  })
}
