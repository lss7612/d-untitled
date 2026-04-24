import { useQuery } from '@tanstack/react-query'
import { searchBooks, type SearchBooksFilter } from '@/api/books'

export function useSearchBooks(
  clubId: number,
  filter: SearchBooksFilter,
  enabled = true
) {
  const title = filter.title?.trim() ?? ''
  const author = filter.author?.trim() ?? ''
  return useQuery({
    queryKey: ['books', clubId, { title, author }],
    queryFn: () => searchBooks(clubId, { title, author }),
    enabled: enabled && clubId > 0,
  })
}
