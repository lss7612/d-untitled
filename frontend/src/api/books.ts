import { apiFetch } from './client'

export interface BookResponse {
  id: number
  title: string
  author: string | null
  category: string | null
  categoryLabel: string | null
  price: number | null
  sourceUrl: string | null
  thumbnailUrl: string | null
  exempt: boolean
  exemptionGrantedAt: string | null
  copies: number
  createdAt: string
}

export interface SearchBooksFilter {
  title?: string
  author?: string
}

/** 회원: 보유 책 검색 (제목/저자 contains, 정규화 후 매칭). */
export function searchBooks(
  clubId: number,
  filter: SearchBooksFilter
): Promise<BookResponse[]> {
  const params = new URLSearchParams()
  if (filter.title?.trim()) params.append('title', filter.title.trim())
  if (filter.author?.trim()) params.append('author', filter.author.trim())
  const qs = params.toString()
  return apiFetch<BookResponse[]>(
    `/api/v1/clubs/${clubId}/books${qs ? `?${qs}` : ''}`
  )
}
