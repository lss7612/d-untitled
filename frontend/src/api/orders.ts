import { apiFetch } from './client'

export interface AdminBookRequestRow {
  id: number
  memberId: number
  memberName: string | null
  memberEmail: string | null
  title: string
  author: string | null
  isbn: string
  aladinItemCode: string | null
  price: number
  category: string
  categoryLabel: string
  sourceUrl: string
  thumbnailUrl: string | null
  status: string
  statusLabel: string
  orderId: number | null
  arrivedAt: string | null
  receivedAt: string | null
}

export interface OrderItemResponse {
  id: number
  isbn: string
  aladinItemCode: string | null
  title: string
  author: string | null
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface OrderResponse {
  id: number
  clubId: number
  targetMonth: string
  status: string
  totalAmount: number
  totalQuantity: number
  createdBy: number
  createdAt: string
  orderedAt: string | null
  items: OrderItemResponse[]
}

export const fetchAdminBookRequests = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<AdminBookRequestRow[]>(`/api/v1/admin/clubs/${clubId}/book-requests/all${qs}`)
}

export const fetchOrder = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<OrderResponse | null>(`/api/v1/admin/clubs/${clubId}/orders${qs}`)
}

export const markOrdered = (clubId: number, ids: number[]) =>
  apiFetch<OrderResponse>(`/api/v1/admin/clubs/${clubId}/book-requests/mark-ordered`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })

export const unmarkOrdered = (clubId: number, ids: number[]) =>
  apiFetch<OrderResponse | null>(`/api/v1/admin/clubs/${clubId}/book-requests/unmark-ordered`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })

export interface MarkResult {
  affected: number
}

export const markArrived = (clubId: number, ids: number[]) =>
  apiFetch<MarkResult>(`/api/v1/admin/clubs/${clubId}/book-requests/mark-arrived`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })

export const markUnarrived = (clubId: number, ids: number[]) =>
  apiFetch<MarkResult>(`/api/v1/admin/clubs/${clubId}/book-requests/mark-unarrived`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })

export interface UnsubmittedMember {
  memberId: number
  name: string
  email: string
}

export interface UnsubmittedMembersResponse {
  targetMonth: string
  totalActiveMembers: number
  submittedCount: number
  unsubmitted: UnsubmittedMember[]
}

export const fetchUnsubmittedMembers = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<UnsubmittedMembersResponse>(`/api/v1/admin/clubs/${clubId}/book-requests/unsubmitted${qs}`)
}
