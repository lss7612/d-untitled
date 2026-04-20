import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import {
  useMyBookRequests,
  useParseBookUrl,
  useCreateBookRequest,
  useDeleteBookRequest,
} from '@/hooks/useBookRequests'
import { BOOK_CATEGORIES, type BookCategory, type ParsedBookResponse } from '@/api/bookRequests'

const MUJE_CLUB_ID = 1

export default function BookRequestPage() {
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const [category, setCategory] = useState<BookCategory>('LITERATURE')
  const [parsed, setParsed] = useState<ParsedBookResponse | null>(null)

  const { data: my, isLoading } = useMyBookRequests(MUJE_CLUB_ID)
  const parseMut = useParseBookUrl(MUJE_CLUB_ID)
  const createMut = useCreateBookRequest(MUJE_CLUB_ID)
  const deleteMut = useDeleteBookRequest(MUJE_CLUB_ID)

  function handleParse() {
    if (!url.trim()) return
    parseMut.mutate(url.trim(), {
      onSuccess: (data) => setParsed(data),
      onError: (e) => toast.error(e.message),
    })
  }

  function handleCreate() {
    if (!parsed) return
    createMut.mutate(
      { url: parsed.sourceUrl, category },
      {
        onSuccess: () => {
          toast.success('책 신청이 접수되었습니다.')
          setUrl('')
          setParsed(null)
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function handleDelete(id: number) {
    if (!confirm('이 신청을 취소하시겠습니까?')) return
    deleteMut.mutate(id, {
      onSuccess: () => toast.success('신청을 취소했습니다.'),
      onError: (e) => toast.error(e.message),
    })
  }

  const locked = my?.locked ?? false

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">책 신청</h2>
            <p className="text-sm text-zinc-500 mt-1">
              알라딘 도서 URL을 입력하면 정보가 자동 입력됩니다.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {/* 잔여 예산 배너 */}
        <div className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
          {isLoading ? (
            <p className="text-sm text-zinc-500">불러오는 중...</p>
          ) : (
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-zinc-500">이번 달 잔여 예산</p>
                <p className="text-2xl font-semibold text-zinc-100 mt-1">
                  {my?.budgetRemaining.toLocaleString()}원
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs text-zinc-500">사용 / 한도</p>
                <p className="text-sm text-zinc-400 mt-1">
                  {my?.budgetUsed.toLocaleString()} / {my?.budgetLimit.toLocaleString()}원
                </p>
              </div>
            </div>
          )}
          {locked && (
            <p className="mt-3 text-xs text-amber-400">⚠ 이번 달 신청이 마감되었습니다.</p>
          )}
        </div>

        {/* URL 입력 */}
        <div className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <label className="block text-xs text-zinc-500 mb-2">알라딘 URL</label>
          <div className="flex gap-2">
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=..."
              disabled={locked}
              className="flex-1 px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600 disabled:opacity-50"
            />
            <Button onClick={handleParse} disabled={!url.trim() || parseMut.isPending || locked}>
              {parseMut.isPending ? '파싱 중...' : '파싱'}
            </Button>
          </div>
        </div>

        {/* 파싱 결과 + 카테고리 + 신청 */}
        {parsed && (
          <div className="mb-8 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
            <div className="flex gap-4">
              {parsed.thumbnailUrl && (
                <img
                  src={parsed.thumbnailUrl}
                  alt={parsed.title}
                  className="w-24 h-32 object-cover rounded border border-zinc-800"
                />
              )}
              <div className="flex-1">
                <p className="font-medium text-zinc-100">{parsed.title}</p>
                <p className="text-sm text-zinc-400 mt-1">{parsed.author}</p>
                {parsed.publisher && <p className="text-xs text-zinc-500 mt-1">{parsed.publisher}</p>}
                <p className="text-xs text-zinc-500 mt-2">ISBN: {parsed.isbn}</p>
                <div className="mt-3">
                  <p className="text-sm text-zinc-200">
                    {parsed.priceKrw.toLocaleString()}원
                  </p>
                  {parsed.currency !== 'KRW' && (
                    <p className="text-xs text-zinc-500 mt-0.5">
                      원래 가격: {parsed.originalPrice} {parsed.currency} (환율 {parsed.exchangeRate})
                    </p>
                  )}
                </div>
              </div>
            </div>

            <div className="mt-5">
              <label className="block text-xs text-zinc-500 mb-2">카테고리</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as BookCategory)}
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
              >
                {BOOK_CATEGORIES.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="mt-5 flex justify-end">
              <Button onClick={handleCreate} disabled={createMut.isPending}>
                {createMut.isPending ? '신청 중...' : '신청하기'}
              </Button>
            </div>
          </div>
        )}

        {/* 내 신청 목록 */}
        <section>
          <h3 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">
            이번 달 내 신청 ({my?.requests.length ?? 0})
          </h3>
          {my?.requests.length === 0 && (
            <p className="text-sm text-zinc-500">아직 신청한 도서가 없습니다.</p>
          )}
          <div className="space-y-2">
            {my?.requests.map((br) => (
              <div
                key={br.id}
                className="flex items-center gap-4 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4"
              >
                {br.thumbnailUrl && (
                  <img
                    src={br.thumbnailUrl}
                    alt={br.title}
                    className="w-12 h-16 object-cover rounded border border-zinc-800"
                  />
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-zinc-100 truncate">{br.title}</p>
                  <p className="text-xs text-zinc-500 mt-0.5">
                    {br.author} · {br.categoryLabel}
                  </p>
                  <p className="text-xs text-zinc-400 mt-1">
                    {br.price.toLocaleString()}원 · {br.status}
                  </p>
                </div>
                {br.status === 'PENDING' && (
                  <Button
                    variant="ghost"
                    onClick={() => handleDelete(br.id)}
                    disabled={deleteMut.isPending}
                  >
                    취소
                  </Button>
                )}
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  )
}
