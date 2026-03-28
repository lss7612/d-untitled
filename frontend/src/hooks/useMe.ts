import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '@/api/members'

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: fetchMe })
}
