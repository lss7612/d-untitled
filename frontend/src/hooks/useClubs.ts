import { useQuery } from '@tanstack/react-query'
import { fetchAllClubs, fetchMyClubs, fetchClubSchedules } from '@/api/clubs'

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
