import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchAllClubs,
  fetchMyClubs,
  fetchClubSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  requestJoinClub,
  fetchPendingJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  fetchClubMembers,
  changeClubMemberRole,
  type ClubRole,
  type ScheduleRequest,
} from '@/api/clubs'

export function useAllClubs() {
  return useQuery({ queryKey: ['clubs', 'all'], queryFn: fetchAllClubs })
}

export function useMyClubs() {
  return useQuery({ queryKey: ['clubs', 'my'], queryFn: fetchMyClubs })
}

export function useClubSchedules(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['clubs', clubId, 'schedules', yearMonth ?? 'all'],
    queryFn: () => fetchClubSchedules(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

/** 일정 등록/수정/삭제 후 일정 + 독후감(deadline) 캐시를 함께 무효화. */
function invalidateSchedules(qc: ReturnType<typeof useQueryClient>, clubId: number) {
  qc.invalidateQueries({ queryKey: ['clubs', clubId, 'schedules'] })
  qc.invalidateQueries({ queryKey: ['bookReports'] })
}

export function useCreateSchedule(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (req: ScheduleRequest) => createSchedule(clubId, req),
    onSuccess: () => invalidateSchedules(qc, clubId),
  })
}

export function useUpdateSchedule(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: number; req: ScheduleRequest }) =>
      updateSchedule(clubId, vars.id, vars.req),
    onSuccess: () => invalidateSchedules(qc, clubId),
  })
}

export function useDeleteSchedule(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteSchedule(clubId, id),
    onSuccess: () => invalidateSchedules(qc, clubId),
  })
}

export function useRequestJoin() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (clubId: number) => requestJoinClub(clubId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clubs', 'all'] })
      qc.invalidateQueries({ queryKey: ['clubs', 'my'] })
    },
  })
}

export function usePendingJoinRequests(clubId: number, enabled = true) {
  return useQuery({
    queryKey: ['clubs', clubId, 'join-requests'],
    queryFn: () => fetchPendingJoinRequests(clubId),
    enabled: enabled && clubId > 0,
  })
}

export function useApproveJoinRequest(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) => approveJoinRequest(clubId, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clubs', clubId, 'join-requests'] })
      qc.invalidateQueries({ queryKey: ['clubs', 'all'] })
      qc.invalidateQueries({ queryKey: ['clubs', 'my'] })
    },
  })
}

export function useRejectJoinRequest(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) => rejectJoinRequest(clubId, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['clubs', clubId, 'join-requests'] })
    },
  })
}

export function useClubMembers(clubId: number, enabled = true) {
  return useQuery({
    queryKey: ['admin', 'clubMembers', clubId],
    queryFn: () => fetchClubMembers(clubId),
    enabled: enabled && clubId > 0,
  })
}

export function useChangeClubMemberRole(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { memberId: number; role: ClubRole }) =>
      changeClubMemberRole(clubId, vars.memberId, vars.role),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'clubMembers', clubId] })
      qc.invalidateQueries({ queryKey: ['clubs', 'my'] })
      qc.invalidateQueries({ queryKey: ['clubs', 'all'] })
    },
  })
}
