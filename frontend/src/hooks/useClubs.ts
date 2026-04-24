import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchAllClubs,
  fetchMyClubs,
  fetchClubSchedules,
  requestJoinClub,
  fetchPendingJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  fetchClubMembers,
  changeClubMemberRole,
  type ClubRole,
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
