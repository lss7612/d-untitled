import { apiFetch } from './client'
import type { BudgetShareResponse } from './budgetShares'

export const BOOK_CATEGORIES = [
  { code: 'LITERATURE', label: '문학' },
  { code: 'HUMANITIES', label: '인문' },
  { code: 'SELF_DEVELOPMENT', label: '자기계발' },
  { code: 'ARTS', label: '예술' },
  { code: 'IT', label: 'IT' },
  { code: 'COMICS', label: '만화' },
  { code: 'ECONOMY', label: '경제' },
  { code: 'LIFESTYLE', label: '라이프스타일' },
  { code: 'SCIENCE', label: '과학' },
  { code: 'ETC', label: '기타' },
] as const

export type BookCategory = (typeof BOOK_CATEGORIES)[number]['code']

export interface ParsedBookResponse {
  title: string
  author: string | null
  publisher: string | null
  isbn: string
  originalPrice: string
  currency: string
  exchangeRate: string
  priceKrw: number
  thumbnailUrl: string | null
  sourceUrl: string
}

export interface BookRequestResponse {
  id: number
  memberId: number
  title: string
  author: string | null
  publisher: string | null
  isbn: string
  price: number
  originalPrice: string | null
  currency: string
  exchangeRate: string | null
  category: BookCategory
  categoryLabel: string
  sourceUrl: string
  thumbnailUrl: string | null
  status: string
  statusLabel: string
  targetMonth: string
  createdAt: string
  arrivedAt: string | null
  receivedAt: string | null
}

export interface MyBookRequestsResponse {
  targetMonth: string
  /** 정책 기반 기본 한도. */
  budgetLimit: number
  /** 이번 달 ACCEPTED 로 받은 나눔 총액. */
  transferIn: number
  /** 이번 달 ACCEPTED 로 전달한 나눔 총액. */
  transferOut: number
  /** budgetLimit + transferIn - transferOut. */
  effectiveLimit: number
  budgetUsed: number
  /** effectiveLimit - budgetUsed. */
  budgetRemaining: number
  locked: boolean
  requests: BookRequestResponse[]
  /** 배너에 "○○님에게 받은 N원" 뱃지를 렌더할 ACCEPTED 레코드. */
  acceptedShares: BudgetShareResponse[]
}

export const parseBookUrl = (clubId: number, url: string) =>
  apiFetch<ParsedBookResponse>(`/api/v1/clubs/${clubId}/books/parse-url`, {
    method: 'POST',
    body: JSON.stringify({ url }),
  })

export const fetchMyBookRequests = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<MyBookRequestsResponse>(`/api/v1/clubs/${clubId}/book-requests/my${qs}`)
}

export const createBookRequest = (clubId: number, url: string, category: BookCategory) =>
  apiFetch<BookRequestResponse>(`/api/v1/clubs/${clubId}/book-requests`, {
    method: 'POST',
    body: JSON.stringify({ url, category }),
  })

export const deleteBookRequest = (clubId: number, id: number) =>
  apiFetch<null>(`/api/v1/clubs/${clubId}/book-requests/${id}`, { method: 'DELETE' })

export interface LockResult {
  yearMonth: string
  locked: boolean
}

export const lockBookRequests = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<LockResult>(`/api/v1/admin/clubs/${clubId}/book-requests/lock${qs}`, {
    method: 'POST',
  })
}

export const unlockBookRequests = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<LockResult>(`/api/v1/admin/clubs/${clubId}/book-requests/unlock${qs}`, {
    method: 'POST',
  })
}

export interface MarkReceivedResult {
  affected: number
}

export const markReceived = (clubId: number, ids: number[]) =>
  apiFetch<MarkReceivedResult>(`/api/v1/clubs/${clubId}/book-requests/mark-received`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })
