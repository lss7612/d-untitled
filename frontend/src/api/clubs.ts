import { apiFetch } from './client'

export interface ClubResponse {
  id: number
  name: string
  description: string
  type: string
  myRole: 'ADMIN' | 'MEMBER' | null
  joined: boolean
}

export interface ScheduleResponse {
  id: number
  clubId: number
  typeCode: string
  date: string
  description: string | null
  yearMonth: string
}

export const fetchAllClubs = () => apiFetch<ClubResponse[]>('/api/v1/clubs')
export const fetchMyClubs = () => apiFetch<ClubResponse[]>('/api/v1/clubs/my')
export const fetchClubSchedules = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<ScheduleResponse[]>(`/api/v1/clubs/${clubId}/schedules${qs}`)
}
