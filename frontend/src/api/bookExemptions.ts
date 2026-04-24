import { apiFetch } from './client'
import type { BookResponse } from './books'

export interface BookExemptionResponse {
  id: number
  clubId: number
  bookId: number
  bookTitle: string | null
  bookAuthor: string | null
  memberId: number
  memberName: string | null
  memberEmail: string | null
  reason: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  processedAt: string | null
}

/** 회원: 중복 책에 대한 제한풀기 신청. */
export function requestBookExemption(
  clubId: number,
  bookId: number,
  reason?: string
): Promise<BookExemptionResponse> {
  return apiFetch<BookExemptionResponse>(`/api/v1/clubs/${clubId}/book-exemptions`, {
    method: 'POST',
    body: JSON.stringify({ bookId, reason: reason ?? null }),
  })
}

/** 관리자: PENDING 상태의 제한풀기 신청 목록. */
export function fetchPendingBookExemptions(
  clubId: number
): Promise<BookExemptionResponse[]> {
  return apiFetch<BookExemptionResponse[]>(`/api/v1/admin/clubs/${clubId}/book-exemptions`)
}

/** 관리자: 승인. */
export function approveBookExemption(
  clubId: number,
  id: number
): Promise<BookExemptionResponse> {
  return apiFetch<BookExemptionResponse>(
    `/api/v1/admin/clubs/${clubId}/book-exemptions/${id}/approve`,
    { method: 'POST' }
  )
}

/** 관리자: 거절. */
export function rejectBookExemption(
  clubId: number,
  id: number
): Promise<BookExemptionResponse> {
  return apiFetch<BookExemptionResponse>(
    `/api/v1/admin/clubs/${clubId}/book-exemptions/${id}/reject`,
    { method: 'POST' }
  )
}

/** 관리자: 현재 제한풀기(exemption)가 승인된 상태인 책 목록. */
export function fetchExemptBooks(clubId: number): Promise<BookResponse[]> {
  return apiFetch<BookResponse[]>(`/api/v1/admin/clubs/${clubId}/books/exempt`)
}

/** 관리자: 책의 exemption 을 제거해 다시 중복 제한 상태로 되돌린다 (idempotent). */
export function revokeBookExemption(
  clubId: number,
  bookId: number
): Promise<BookResponse> {
  return apiFetch<BookResponse>(
    `/api/v1/admin/clubs/${clubId}/books/${bookId}/exemption`,
    { method: 'DELETE' }
  )
}
