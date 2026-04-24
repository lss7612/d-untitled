import { useQuery } from '@tanstack/react-query'
import { fetchBudgetSummary } from '@/api/budgets'

export function useBudgetSummary(clubId: number, yearMonth?: string) {
  return useQuery({
    queryKey: ['admin', 'budgets', 'summary', clubId, yearMonth ?? 'current'],
    queryFn: () => fetchBudgetSummary(clubId, yearMonth),
    enabled: clubId > 0,
  })
}
