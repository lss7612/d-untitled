import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  requestBookExemption,
  fetchPendingBookExemptions,
  approveBookExemption,
  rejectBookExemption,
  fetchExemptBooks,
  revokeBookExemption,
} from '@/api/bookExemptions'

export function useRequestBookExemption(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { bookId: number; reason?: string }) =>
      requestBookExemption(clubId, vars.bookId, vars.reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'bookExemptions', clubId] })
    },
  })
}

export function usePendingBookExemptions(clubId: number, enabled = true) {
  return useQuery({
    queryKey: ['admin', 'bookExemptions', clubId],
    queryFn: () => fetchPendingBookExemptions(clubId),
    enabled: enabled && clubId > 0,
  })
}

export function useApproveBookExemption(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => approveBookExemption(clubId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'bookExemptions', clubId] })
      // 승인 시 book 의 exempt 상태가 바뀌어 재신청이 가능해지므로, 책 신청 관련 쿼리도 갱신.
      qc.invalidateQueries({ queryKey: ['bookRequests'] })
    },
  })
}

export function useRejectBookExemption(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => rejectBookExemption(clubId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'bookExemptions', clubId] })
    },
  })
}

/** 관리자: 제한풀기 승인된 책 목록. AdminExemptBooksPage 용. */
export function useExemptBooks(clubId: number, enabled = true) {
  return useQuery({
    queryKey: ['admin', 'exemptBooks', clubId],
    queryFn: () => fetchExemptBooks(clubId),
    enabled: enabled && clubId > 0,
  })
}

/** 관리자: 책의 exemption 을 제거해 다시 중복 제한 상태로. */
export function useRevokeBookExemption(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (bookId: number) => revokeBookExemption(clubId, bookId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'exemptBooks', clubId] })
      qc.invalidateQueries({ queryKey: ['books', clubId] })
    },
  })
}
