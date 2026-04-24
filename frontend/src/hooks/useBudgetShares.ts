import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import {
  acceptBudgetShare,
  cancelBudgetShare,
  createBudgetShare,
  fetchAcceptedShares,
  fetchIncomingShares,
  fetchOutgoingShares,
  fetchShareCandidates,
  rejectBudgetShare,
  type CreateBudgetShareBody,
} from '@/api/budgetShares'

/**
 * 예산 나눔 관련 훅 묶음.
 * 어떤 액션이든 성공하면 본인 책신청 배너(`bookRequests.my`) 와 나눔 목록 전체를 invalidate 한다.
 * remaining/effectiveLimit 이 즉시 재계산되어 UI가 갱신되어야 하기 때문.
 */

function invalidateAll(qc: QueryClient, clubId: number) {
  qc.invalidateQueries({ queryKey: ['bookRequests', 'my', clubId] })
  qc.invalidateQueries({ queryKey: ['budgetShares', clubId] })
}

export function useShareCandidates(clubId: number, yearMonth: string, enabled = true) {
  return useQuery({
    queryKey: ['budgetShares', clubId, 'candidates', yearMonth],
    queryFn: () => fetchShareCandidates(clubId, yearMonth),
    enabled: enabled && clubId > 0 && !!yearMonth,
  })
}

export function useIncomingShares(clubId: number, yearMonth: string) {
  return useQuery({
    queryKey: ['budgetShares', clubId, 'incoming', yearMonth],
    queryFn: () => fetchIncomingShares(clubId, yearMonth),
    enabled: clubId > 0 && !!yearMonth,
  })
}

export function useOutgoingShares(clubId: number, yearMonth: string) {
  return useQuery({
    queryKey: ['budgetShares', clubId, 'outgoing', yearMonth],
    queryFn: () => fetchOutgoingShares(clubId, yearMonth),
    enabled: clubId > 0 && !!yearMonth,
  })
}

export function useAcceptedShares(clubId: number, yearMonth: string) {
  return useQuery({
    queryKey: ['budgetShares', clubId, 'accepted', yearMonth],
    queryFn: () => fetchAcceptedShares(clubId, yearMonth),
    enabled: clubId > 0 && !!yearMonth,
  })
}

export function useCreateShare(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateBudgetShareBody) => createBudgetShare(clubId, body),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useAcceptShare(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => acceptBudgetShare(clubId, id),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useRejectShare(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => rejectBudgetShare(clubId, id),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}

export function useCancelShare(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cancelBudgetShare(clubId, id),
    onSuccess: () => invalidateAll(qc, clubId),
  })
}
