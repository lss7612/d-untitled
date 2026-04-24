import { apiFetch } from './client'

export interface BudgetSummary {
  targetMonth: string
  totalRequestedAmount: number
  totalBaseLimit: number
  usagePercent: number
}

export const fetchBudgetSummary = (clubId: number, yearMonth?: string) => {
  const qs = yearMonth ? `?yearMonth=${yearMonth}` : ''
  return apiFetch<BudgetSummary>(`/api/v1/admin/clubs/${clubId}/budgets/summary${qs}`)
}
