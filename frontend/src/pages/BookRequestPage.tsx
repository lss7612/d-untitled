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
  useMarkReceived,
} from '@/hooks/useBookRequests'
import { useMyClubs } from '@/hooks/useClubs'
import { CheckboxRow } from '@/components/ui/checkbox-row'
import { BOOK_CATEGORIES, type BookCategory, type ParsedBookResponse } from '@/api/bookRequests'

const MUJE_CLUB_ID = 1

export default function BookRequestPage() {
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const [category, setCategory] = useState<BookCategory>('LITERATURE')
  const [parsed, setParsed] = useState<ParsedBookResponse | null>(null)

  const { data: my, isLoading } = useMyBookRequests(MUJE_CLUB_ID)
  const { data: myClubs } = useMyClubs()
  const parseMut = useParseBookUrl(MUJE_CLUB_ID)
  const markReceivedMut = useMarkReceived(MUJE_CLUB_ID)
  const [receiveSelected, setReceiveSelected] = useState<Set<number>>(new Set())
  const createMut = useCreateBookRequest(MUJE_CLUB_ID)
  const deleteMut = useDeleteBookRequest(MUJE_CLUB_ID)

  const isAdmin = myClubs?.some((c) => c.id === MUJE_CLUB_ID && c.myRole === 'ADMIN') ?? false

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
            <div className="mt-4 -mx-4 -mb-4 px-4 py-2 rounded-b-xl bg-amber-950/40 border-t border-amber-900/50">
              <p className="text-xs text-amber-300">🔒 이번 달 신청이 마감되었습니다. 새 신청 / 수정 / 취소가 불가합니다.</p>
            </div>
          )}
        </div>

        {/* 관리자 안내 */}
        {isAdmin && (
          <div className="mb-6 rounded-xl border border-amber-900/40 bg-amber-950/20 p-4 flex items-center justify-between">
            <p className="text-sm text-amber-300">
              관리자는 전체 신청 / 잠금 / 합산 주문서 / 카트 담기를{' '}
              <button
                onClick={() => navigate('/muje/admin/book-requests')}
                className="underline text-amber-200 hover:text-amber-100"
              >
                관리자 화면
              </button>
              에서 처리하세요.
            </p>
          </div>
        )}

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

        {/* 도착한 내 책 — 수령 처리 */}
        {(() => {
          const arrived = (my?.requests ?? []).filter((br) => br.status === 'ARRIVED')
          if (arrived.length === 0) return null
          const allChecked = arrived.length > 0 && arrived.every((br) => receiveSelected.has(br.id))
          function toggleAllArrived() {
            setReceiveSelected(allChecked ? new Set() : new Set(arrived.map((br) => br.id)))
          }
          function toggleOne(id: number) {
            const next = new Set(receiveSelected)
            if (next.has(id)) next.delete(id)
            else next.add(id)
            setReceiveSelected(next)
          }
          function handleReceive() {
            const ids = Array.from(receiveSelected)
            if (ids.length === 0) return toast.warning('선택한 책이 없습니다.')
            if (!confirm(`선택한 ${ids.length}권을 수령 완료 처리하시겠습니까?`)) return
            markReceivedMut.mutate(ids, {
              onSuccess: (r) => {
                toast.success(`수령 완료 (${r.affected}권)`)
                setReceiveSelected(new Set())
              },
              onError: (e) => toast.error(e.message),
            })
          }
          return (
            <section className="mb-6 rounded-xl border border-emerald-900/40 bg-emerald-950/20 p-5">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <p className="text-sm font-medium text-emerald-300">📦 도착한 내 책 ({arrived.length}권)</p>
                  <p className="text-xs text-zinc-500 mt-1">받으신 책을 체크하고 수령 완료 처리하세요.</p>
                </div>
                <CheckboxRow
                  checked={allChecked}
                  onChange={() => toggleAllArrived()}
                  className="px-3 py-2 text-xs text-zinc-400"
                >
                  전체 선택
                </CheckboxRow>
              </div>
              <ul className="space-y-1">
                {arrived.map((br) => (
                  <li key={br.id} className="border-t border-zinc-800/30">
                    <CheckboxRow checked={receiveSelected.has(br.id)} onChange={() => toggleOne(br.id)}>
                      {br.thumbnailUrl && (
                        <img src={br.thumbnailUrl} alt={br.title} className="w-10 h-12 object-cover rounded border border-zinc-800" />
                      )}
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-zinc-200 truncate">{br.title}</p>
                        <p className="text-xs text-zinc-500 truncate">
                          {br.author}
                          {br.arrivedAt && (
                            <span className="ml-2 text-amber-400">
                              · 도착 {new Date(br.arrivedAt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit' })}
                            </span>
                          )}
                        </p>
                      </div>
                    </CheckboxRow>
                  </li>
                ))}
              </ul>
              <div className="mt-3 flex justify-end">
                <Button onClick={handleReceive} disabled={receiveSelected.size === 0 || markReceivedMut.isPending}>
                  {markReceivedMut.isPending ? '처리 중...' : `선택한 ${receiveSelected.size}권 수령 완료`}
                </Button>
              </div>
            </section>
          )
        })()}

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
                    {br.price.toLocaleString()}원 · {br.statusLabel}
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
