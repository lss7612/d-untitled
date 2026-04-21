import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchMyBookReports,
  fetchClubBookReports,
  submitBookReport,
  updateBookReport,
  fetchMissingSubmitters,
  type BookReportPayload,
} from '@/api/bookReports'

export function useMyBookReports(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['bookReports', 'my', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchMyBookReports(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

export function useClubBookReports(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['bookReports', 'club', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchClubBookReports(clubId, yearMonth),
    enabled: clubId > 0,
  })
}

export function useSubmitBookReport(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: BookReportPayload) => submitBookReport(clubId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookReports', 'my', clubId] })
      qc.invalidateQueries({ queryKey: ['bookReports', 'club', clubId] })
    },
  })
}

export function useUpdateBookReport(clubId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: number; payload: BookReportPayload }) =>
      updateBookReport(clubId, vars.id, vars.payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bookReports', 'my', clubId] })
      qc.invalidateQueries({ queryKey: ['bookReports', 'club', clubId] })
    },
  })
}

export function useMissingSubmitters(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['bookReports', 'missing', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchMissingSubmitters(clubId, yearMonth),
    enabled: clubId > 0,
  })
}
