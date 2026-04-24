import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAdminBookRequests, fetchOrder, fetchUnsubmittedMembers, markOrdered, unmarkOrdered, markArrived, markUnarrived } from '@/api/orders'

export function useAdminBookRequests(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['admin', 'bookRequests', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchAdminBookRequests(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

export function useUnsubmittedMembers(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['admin', 'bookRequests', 'unsubmitted', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchUnsubmittedMembers(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

export function useOrder(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['admin', 'order', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchOrder(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

function invalidateAll(qc: ReturnType<typeof useQueryClient>, clubId: number) {
  qc.invalidateQueries({ queryKey: ['admin', 'order', clubId] })
  qc.invalidateQueries({ queryKey: ['admin', 'bookRequests', clubId] })
  qc.invalidateQueries({ queryKey: ['admin', 'bookRequests', 'unsubmitted', clubId] })
  qc.invalidateQueries({ queryKey: ['admin', 'budgets', 'summary', clubId] })
  qc.invalidateQueries({ queryKey: ['bookRequests', 'my', clubId] })
}

export function useMarkOrdered(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => markOrdered(clubId, ids),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useUnmarkOrdered(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => unmarkOrdered(clubId, ids),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useMarkArrived(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => markArrived(clubId, ids),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useMarkUnarrived(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => markUnarrived(clubId, ids),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}
