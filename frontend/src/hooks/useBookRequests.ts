import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchMyBookRequests,
  parseBookUrl,
  createBookRequest,
  deleteBookRequest,
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

export function useDeleteBookRequest(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteBookRequest(clubId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookRequests', 'my', clubId] })
    },
  })
}
