import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import { useSearchBooks } from '@/hooks/useBooks'
import type { BookResponse } from '@/api/books'

const MUJE_CLUB_ID = 1

export default function OwnedBooksPage() {
  const navigate = useNavigate()
  const [titleInput, setTitleInput] = useState('')
  const [authorInput, setAuthorInput] = useState('')
  const [filter, setFilter] = useState<{ title: string; author: string }>({
    title: '',
    author: '',
  })

  const { data: books, isLoading, isError, error } = useSearchBooks(MUJE_CLUB_ID, filter)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFilter({ title: titleInput.trim(), author: authorInput.trim() })
  }

  function handleReset() {
    setTitleInput('')
    setAuthorInput('')
    setFilter({ title: '', author: '' })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">보유 책 검색</h2>
            <p className="text-sm text-zinc-500 mt-1">
              무제 동호회가 보유 중인 책을 제목 또는 저자로 검색합니다.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-xl border border-zinc-800/50 bg-zinc-900/40 p-4 mb-6 grid grid-cols-1 sm:grid-cols-[1fr_1fr_auto_auto] gap-3"
        >
          <input
            type="text"
            value={titleInput}
            onChange={(e) => setTitleInput(e.target.value)}
            placeholder="제목"
            className="px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
          />
          <input
            type="text"
            value={authorInput}
            onChange={(e) => setAuthorInput(e.target.value)}
            placeholder="저자"
            className="px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
          />
          <Button type="submit">검색</Button>
          <Button type="button" variant="ghost" onClick={handleReset}>
            초기화
          </Button>
        </form>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}

        {isError && (
          <p className="text-sm text-rose-400">
            불러오지 못했습니다: {error instanceof Error ? error.message : '알 수 없는 오류'}
          </p>
        )}

        {!isLoading && !isError && (books?.length ?? 0) === 0 && (
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-8 text-center">
            <p className="text-sm text-zinc-500">
              {filter.title || filter.author ? '검색 결과가 없습니다.' : '등록된 책이 없습니다.'}
            </p>
          </div>
        )}

        <ul className="space-y-3">
          {books?.map((b) => <BookCard key={b.id} book={b} />)}
        </ul>
      </main>
    </div>
  )
}

function BookCard({ book }: { book: BookResponse }) {
  return (
    <li className="rounded-xl border border-zinc-800/50 bg-zinc-900/40 p-4 flex items-start gap-4">
      {book.thumbnailUrl ? (
        <img
          src={book.thumbnailUrl}
          alt={book.title}
          className="w-16 h-20 object-cover rounded-md border border-zinc-800/50 shrink-0"
        />
      ) : (
        <div className="w-16 h-20 rounded-md bg-zinc-800/50 shrink-0" />
      )}
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <p className="text-sm font-medium text-zinc-100 truncate">{book.title}</p>
          <div className="flex items-center gap-2 shrink-0">
            {book.exempt && (
              <span className="text-[10px] rounded-full px-2 py-0.5 bg-amber-900/30 border border-amber-800/40 text-amber-300">
                🔓 제한 해제됨
              </span>
            )}
            <span className="text-[10px] rounded-full px-2 py-0.5 bg-zinc-800/60 border border-zinc-700/50 text-zinc-300">
              {book.copies}권 보유
            </span>
          </div>
        </div>
        {book.author && (
          <p className="text-xs text-zinc-500 mt-0.5 truncate">{book.author}</p>
        )}
        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-zinc-400">
          {book.categoryLabel && (
            <span className="rounded-md px-1.5 py-0.5 bg-zinc-800/50 text-zinc-300">
              {book.categoryLabel}
            </span>
          )}
          {book.price != null && (
            <span className="text-zinc-400">
              {book.price.toLocaleString('ko-KR')}원
            </span>
          )}
          {book.sourceUrl && (
            <a
              href={book.sourceUrl}
              target="_blank"
              rel="noreferrer"
              className="text-sky-400 hover:underline"
            >
              상세
            </a>
          )}
        </div>
      </div>
    </li>
  )
}
