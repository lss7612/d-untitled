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

export const markLocked = (clubId: number, ids: number[]) =>
  apiFetch<OrderResponse | null>(`/api/v1/admin/clubs/${clubId}/book-requests/mark-locked`, {
    method: 'PATCH',
    body: JSON.stringify({ ids }),
  })
