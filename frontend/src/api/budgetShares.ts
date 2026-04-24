import { apiFetch } from './client'

export type BudgetShareStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'

export interface BudgetShareResponse {
  id: number
  clubId: number
  targetMonth: string
  requesterId: number
  requesterName: string | null
  senderId: number
  senderName: string | null
  amount: number
  status: BudgetShareStatus
  note: string | null
  createdAt: string
  respondedAt: string | null
}

export interface ShareCandidate {
  memberId: number
  name: string
  email: string
  /** 이체 반영 잔여 한도 (음수일 수 있음). */
  remaining: number
}

export interface CreateBudgetShareBody {
  targetMonth: string
  senderId: number
  amount: number
  note?: string | null
}

const base = (clubId: number) => `/api/v1/clubs/${clubId}/budget-shares`

export const fetchShareCandidates = (clubId: number, yearMonth: string) =>
  apiFetch<ShareCandidate[]>(`${base(clubId)}/candidates?yearMonth=${yearMonth}`)

export const fetchIncomingShares = (clubId: number, yearMonth: string) =>
  apiFetch<BudgetShareResponse[]>(`${base(clubId)}/incoming?yearMonth=${yearMonth}`)

export const fetchOutgoingShares = (clubId: number, yearMonth: string) =>
  apiFetch<BudgetShareResponse[]>(`${base(clubId)}/outgoing?yearMonth=${yearMonth}`)

export const fetchAcceptedShares = (clubId: number, yearMonth: string) =>
  apiFetch<BudgetShareResponse[]>(`${base(clubId)}/accepted?yearMonth=${yearMonth}`)

export const createBudgetShare = (clubId: number, body: CreateBudgetShareBody) =>
  apiFetch<BudgetShareResponse>(base(clubId), {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const acceptBudgetShare = (clubId: number, id: number) =>
  apiFetch<BudgetShareResponse>(`${base(clubId)}/${id}/accept`, { method: 'POST' })

export const rejectBudgetShare = (clubId: number, id: number) =>
  apiFetch<BudgetShareResponse>(`${base(clubId)}/${id}/reject`, { method: 'POST' })

export const cancelBudgetShare = (clubId: number, id: number) =>
  apiFetch<BudgetShareResponse>(`${base(clubId)}/${id}/cancel`, { method: 'POST' })
