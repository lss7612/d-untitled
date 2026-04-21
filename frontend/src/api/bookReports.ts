import { apiFetch } from './client'

export const MIN_CONTENT_LENGTH = 50

export interface BookReportResponse {
  id: number
  bookRequestId: number | null
  memberId: number
  memberName: string | null
  memberEmail: string | null
  targetMonth: string
  bookTitle: string
  bookAuthor: string | null
  bookThumbnailUrl: string | null
  title: string
  content: string
  submittedAt: string
  updatedAt: string
}

export interface BookRequestSummary {
  bookRequestId: number
  title: string
  author: string | null
  thumbnailUrl: string | null
}

export interface MyBookReportsResponse {
  targetMonth: string
  deadline: string | null
  overdue: boolean
  report: BookReportResponse | null
  myRequests: BookRequestSummary[]
}

export interface BookReportPayload {
  bookRequestId?: number | null
  bookTitle?: string | null
  bookAuthor?: string | null
  bookThumbnailUrl?: string | null
  title: string
  content: string
}

export interface MissingSubmitter {
  memberId: number
  memberName: string | null
  memberEmail: string | null
}

export interface MissingSubmittersResponse {
  targetMonth: string
  deadline: string | null
  totalMembers: number
  submittedCount: number
  missing: MissingSubmitter[]
}

export const fetchMyBookReports = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<MyBookReportsResponse>(`/api/v1/clubs/${clubId}/book-reports/my${qs}`)
}

export const fetchClubBookReports = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<BookReportResponse[]>(`/api/v1/clubs/${clubId}/book-reports${qs}`)
}

export const submitBookReport = (clubId: number, payload: BookReportPayload) =>
  apiFetch<BookReportResponse>(`/api/v1/clubs/${clubId}/book-reports`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const updateBookReport = (clubId: number, id: number, payload: BookReportPayload) =>
  apiFetch<BookReportResponse>(`/api/v1/clubs/${clubId}/book-reports/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })

export const fetchMissingSubmitters = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<MissingSubmittersResponse>(`/api/v1/admin/clubs/${clubId}/book-reports/missing${qs}`)
}
