import { apiFetch } from './client'

export type MembershipStatus = 'PENDING' | 'ACTIVE' | 'REJECTED'

export interface ClubResponse {
  id: number
  name: string
  description: string
  type: string
  myRole: 'ADMIN' | 'MEMBER' | null
  joined: boolean
  joinStatus: MembershipStatus | null
}

export interface ScheduleResponse {
  id: number
  clubId: number
  typeCode: string
  date: string
  description: string | null
  yearMonth: string
}

export interface JoinRequestResponse {
  clubId: number
  memberId: number
  memberName: string | null
  memberEmail: string | null
  status: MembershipStatus
  requestedAt: string
}

export const fetchAllClubs = () => apiFetch<ClubResponse[]>('/api/v1/clubs')
export const fetchMyClubs = () => apiFetch<ClubResponse[]>('/api/v1/clubs/my')
export const fetchClubSchedules = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<ScheduleResponse[]>(`/api/v1/clubs/${clubId}/schedules${qs}`)
}

/** 일정 등록/수정 요청. date 는 "YYYY-MM-DD". */
export interface ScheduleRequest {
  typeCode: string
  date: string
  description?: string | null
}

export const createSchedule = (clubId: number, req: ScheduleRequest) =>
  apiFetch<ScheduleResponse>(`/api/v1/admin/clubs/${clubId}/schedules`, {
    method: 'POST',
    body: JSON.stringify(req),
  })

export const updateSchedule = (clubId: number, id: number, req: ScheduleRequest) =>
  apiFetch<ScheduleResponse>(`/api/v1/admin/clubs/${clubId}/schedules/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(req),
  })

export const deleteSchedule = (clubId: number, id: number) =>
  apiFetch<void>(`/api/v1/admin/clubs/${clubId}/schedules/${id}`, {
    method: 'DELETE',
  })

export const requestJoinClub = (clubId: number) =>
  apiFetch<JoinRequestResponse>(`/api/v1/clubs/${clubId}/join-requests`, {
    method: 'POST',
    body: JSON.stringify({}),
  })

export const fetchPendingJoinRequests = (clubId: number) =>
  apiFetch<JoinRequestResponse[]>(`/api/v1/admin/clubs/${clubId}/join-requests`)

export const approveJoinRequest = (clubId: number, memberId: number) =>
  apiFetch<JoinRequestResponse>(
    `/api/v1/admin/clubs/${clubId}/join-requests/${memberId}/approve`,
    { method: 'POST', body: JSON.stringify({}) }
  )

export const rejectJoinRequest = (clubId: number, memberId: number) =>
  apiFetch<JoinRequestResponse>(
    `/api/v1/admin/clubs/${clubId}/join-requests/${memberId}/reject`,
    { method: 'POST', body: JSON.stringify({}) }
  )

export type ClubRole = 'ADMIN' | 'MEMBER'

export interface ClubMemberResponse {
  clubId: number
  memberId: number
  memberName: string | null
  memberEmail: string | null
  role: ClubRole
  status: MembershipStatus
  joinedAt: string
}

export const fetchClubMembers = (clubId: number) =>
  apiFetch<ClubMemberResponse[]>(`/api/v1/admin/clubs/${clubId}/members`)

export const changeClubMemberRole = (clubId: number, memberId: number, role: ClubRole) =>
  apiFetch<ClubMemberResponse>(
    `/api/v1/admin/clubs/${clubId}/members/${memberId}/role`,
    { method: 'PATCH', body: JSON.stringify({ role }) }
  )
