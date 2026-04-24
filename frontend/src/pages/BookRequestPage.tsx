import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import BudgetShareDialog from '@/components/BudgetShareDialog'
import { Button } from '@/components/ui/button'
import {
  useMyBookRequests,
  useParseBookUrl,
  useCreateBookRequest,
  useDeleteBookRequest,
  useMarkReceived,
} from '@/hooks/useBookRequests'
import {
  useIncomingShares,
  useOutgoingShares,
  useAcceptShare,
  useRejectShare,
  useCancelShare,
} from '@/hooks/useBudgetShares'
import { useMe } from '@/hooks/useMe'
import { useMyClubs } from '@/hooks/useClubs'
import { CheckboxRow } from '@/components/ui/checkbox-row'
import { BOOK_CATEGORIES, type BookCategory, type ParsedBookResponse } from '@/api/bookRequests'
import {
  useRequestBookExemption,
  useRequestBookExemptionByUrl,
} from '@/hooks/useBookExemptions'
import type { ApiError } from '@/api/client'

const MUJE_CLUB_ID = 1

export default function BookRequestPage() {
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const [category, setCategory] = useState<BookCategory>('LITERATURE')
  const [parsed, setParsed] = useState<ParsedBookResponse | null>(null)

  const { data: my, isLoading } = useMyBookRequests(MUJE_CLUB_ID)
  const { data: myClubs } = useMyClubs()
  const { data: me } = useMe()
  const parseMut = useParseBookUrl(MUJE_CLUB_ID)
  const markReceivedMut = useMarkReceived(MUJE_CLUB_ID)
  const [receiveSelected, setReceiveSelected] = useState<Set<number>>(new Set())
  const createMut = useCreateBookRequest(MUJE_CLUB_ID)
  const deleteMut = useDeleteBookRequest(MUJE_CLUB_ID)
  const exemptionMut = useRequestBookExemption(MUJE_CLUB_ID)
  const exemptionByUrlMut = useRequestBookExemptionByUrl(MUJE_CLUB_ID)

  // 중복 책 에러 상태 (duplicate detected -> 제한풀기 버튼 노출)
  const [duplicateBook, setDuplicateBook] = useState<{
    id: number
    title: string
  } | null>(null)
  // 같은 달 타 회원 신청과 충돌 시 — bookId 가 없어 URL 을 다시 써야 한다.
  const [monthlyConflict, setMonthlyConflict] = useState<{
    url: string
    bookTitle: string
    requesterName: string
    requesterEmail: string | null
  } | null>(null)
  const [exemptionReason, setExemptionReason] = useState('')

  const targetMonth = my?.targetMonth ?? ''
  const { data: incoming = [] } = useIncomingShares(MUJE_CLUB_ID, targetMonth)
  const { data: outgoing = [] } = useOutgoingShares(MUJE_CLUB_ID, targetMonth)
  const acceptShareMut = useAcceptShare(MUJE_CLUB_ID)
  const rejectShareMut = useRejectShare(MUJE_CLUB_ID)
  const cancelShareMut = useCancelShare(MUJE_CLUB_ID)
  const [shareDialogOpen, setShareDialogOpen] = useState(false)

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
    setDuplicateBook(null)
    setMonthlyConflict(null)
    createMut.mutate(
      { url: parsed.sourceUrl, category },
      {
        onSuccess: () => {
          toast.success('책 신청이 접수되었습니다.')
          setUrl('')
          setParsed(null)
          setDuplicateBook(null)
          setMonthlyConflict(null)
        },
        onError: (e) => {
          const err = e as ApiError
          if (err.details?.code === 'DUPLICATE_BOOK') {
            setDuplicateBook({
              id: Number(err.details.duplicateBookId),
              title: String(err.details.duplicateBookTitle ?? parsed.title),
            })
            toast.error(err.message)
          } else if (err.details?.code === 'DUPLICATE_MONTHLY_REQUEST') {
            setMonthlyConflict({
              url: parsed.sourceUrl,
              bookTitle: String(err.details.bookTitle ?? parsed.title),
              requesterName: String(err.details.requesterName ?? '다른 회원'),
              requesterEmail: err.details.requesterEmail
                ? String(err.details.requesterEmail)
                : null,
            })
            toast.error(err.message)
          } else {
            toast.error(e.message)
          }
        },
      }
    )
  }

  function handleRequestExemption() {
    if (!duplicateBook) return
    exemptionMut.mutate(
      { bookId: duplicateBook.id, reason: exemptionReason.trim() || undefined },
      {
        onSuccess: () => {
          toast.success('제한풀기 신청이 접수되었습니다. 관리자 승인을 기다려주세요.')
          setDuplicateBook(null)
          setExemptionReason('')
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function handleRequestExemptionByUrl() {
    if (!monthlyConflict) return
    exemptionByUrlMut.mutate(
      { url: monthlyConflict.url, reason: exemptionReason.trim() || undefined },
      {
        onSuccess: () => {
          toast.success('제한풀기 신청이 접수되었습니다. 관리자 승인을 기다려주세요.')
          setMonthlyConflict(null)
          setExemptionReason('')
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
          {isLoading || !my ? (
            <p className="text-sm text-zinc-500">불러오는 중...</p>
          ) : (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-zinc-500">이번 달 잔여 예산</p>
                  <p className="text-2xl font-semibold text-zinc-100 mt-1">
                    {my.budgetRemaining.toLocaleString()}원
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-zinc-500">사용 / 실한도</p>
                  <p className="text-sm text-zinc-400 mt-1">
                    {my.budgetUsed.toLocaleString()} / {my.effectiveLimit.toLocaleString()}원
                  </p>
                </div>
              </div>

              {/* 세부 내역: 기본 한도 · 받은 나눔 · 전달한 나눔 */}
              <div className="mt-4 grid grid-cols-3 gap-2 text-xs">
                <div className="rounded-lg bg-zinc-950/60 border border-zinc-800/50 px-3 py-2">
                  <p className="text-zinc-500">기본 한도</p>
                  <p className="mt-0.5 text-zinc-200">{my.budgetLimit.toLocaleString()}원</p>
                </div>
                <div className="rounded-lg bg-sky-950/30 border border-sky-900/40 px-3 py-2">
                  <p className="text-sky-300/80">받은 나눔</p>
                  <p className="mt-0.5 text-sky-200">+{my.transferIn.toLocaleString()}원</p>
                </div>
                <div className="rounded-lg bg-rose-950/20 border border-rose-900/30 px-3 py-2">
                  <p className="text-rose-300/80">전달한 나눔</p>
                  <p className="mt-0.5 text-rose-200">−{my.transferOut.toLocaleString()}원</p>
                </div>
              </div>

              {/* ACCEPTED 이체 뱃지 */}
              {my.acceptedShares.length > 0 && me && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {my.acceptedShares.map((s) => {
                    const iAmRequester = s.requesterId === me.id
                    const counterpart = iAmRequester ? s.senderName : s.requesterName
                    return (
                      <span
                        key={s.id}
                        className={[
                          'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] border',
                          iAmRequester
                            ? 'bg-sky-950/40 border-sky-900/60 text-sky-200'
                            : 'bg-rose-950/30 border-rose-900/50 text-rose-200',
                        ].join(' ')}
                        title={s.note ?? ''}
                      >
                        {iAmRequester
                          ? `${counterpart ?? '?'}님에게 받음 +${s.amount.toLocaleString()}원`
                          : `${counterpart ?? '?'}님에게 전달 −${s.amount.toLocaleString()}원`}
                      </span>
                    )
                  })}
                </div>
              )}

              {/* 나눔 받기 버튼 */}
              <div className="mt-4 flex justify-end">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setShareDialogOpen(true)}
                  disabled={locked}
                >
                  나눔 받기
                </Button>
              </div>
            </>
          )}
          {locked && (
            <div className="mt-4 -mx-4 -mb-4 px-4 py-2 rounded-b-xl bg-amber-950/40 border-t border-amber-900/50">
              <p className="text-xs text-amber-300">🔒 이번 달 신청이 마감되었습니다. 새 신청 / 수정 / 취소가 불가합니다.</p>
            </div>
          )}
        </div>

        {/* 내게 온 나눔 신청 (수락/거절) */}
        {incoming.length > 0 && (
          <section className="mb-6 rounded-xl border border-sky-900/40 bg-sky-950/20 p-5">
            <p className="text-sm font-medium text-sky-300 mb-3">
              💌 내게 온 나눔 신청 ({incoming.length})
            </p>
            <ul className="space-y-2">
              {incoming.map((s) => (
                <li
                  key={s.id}
                  className="rounded-lg border border-sky-900/40 bg-zinc-950/50 px-3 py-2.5 flex items-center gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-zinc-200">
                      <span className="font-medium">{s.requesterName ?? '?'}</span>
                      {'님이 '}
                      <span className="text-sky-300">{s.amount.toLocaleString()}원</span>
                      {' 나눔을 요청했습니다.'}
                    </p>
                    {s.note && <p className="text-xs text-zinc-500 mt-0.5 truncate">{s.note}</p>}
                  </div>
                  <Button
                    size="sm"
                    onClick={() =>
                      acceptShareMut.mutate(s.id, {
                        onSuccess: () => toast.success('수락했습니다.'),
                        onError: (e) => toast.error(e.message),
                      })
                    }
                    disabled={acceptShareMut.isPending}
                  >
                    수락
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      rejectShareMut.mutate(s.id, {
                        onSuccess: () => toast.success('거절했습니다.'),
                        onError: (e) => toast.error(e.message),
                      })
                    }
                    disabled={rejectShareMut.isPending}
                  >
                    거절
                  </Button>
                </li>
              ))}
            </ul>
          </section>
        )}

        {/* 내가 보낸 나눔 신청 (취소) */}
        {outgoing.length > 0 && (
          <section className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/40 p-5">
            <p className="text-sm font-medium text-zinc-400 mb-3">
              ✉️ 내가 보낸 나눔 신청 ({outgoing.length})
            </p>
            <ul className="space-y-2">
              {outgoing.map((s) => (
                <li
                  key={s.id}
                  className="rounded-lg border border-zinc-800/40 bg-zinc-950/50 px-3 py-2.5 flex items-center gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-zinc-300">
                      <span className="font-medium">{s.senderName ?? '?'}</span>
                      {'님에게 '}
                      <span className="text-zinc-200">{s.amount.toLocaleString()}원</span>
                      {' 요청 · 대기 중'}
                    </p>
                    {s.note && <p className="text-xs text-zinc-500 mt-0.5 truncate">{s.note}</p>}
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      cancelShareMut.mutate(s.id, {
                        onSuccess: () => toast.success('나눔 신청을 취소했습니다.'),
                        onError: (e) => toast.error(e.message),
                      })
                    }
                    disabled={cancelShareMut.isPending}
                  >
                    취소
                  </Button>
                </li>
              ))}
            </ul>
          </section>
        )}

        {/* 나눔 다이얼로그 */}
        {targetMonth && (
          <BudgetShareDialog
            open={shareDialogOpen}
            clubId={MUJE_CLUB_ID}
            yearMonth={targetMonth}
            onClose={() => setShareDialogOpen(false)}
          />
        )}

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

        {/* 중복 책 경고 + 제한풀기 신청 */}
        {duplicateBook && (
          <div className="mb-8 rounded-xl border border-amber-900/50 bg-amber-950/30 p-5">
            <div className="flex items-start gap-3">
              <span className="text-lg leading-none">⚠️</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-amber-200">
                  이미 보유 중인 책입니다
                </p>
                <p className="text-sm text-amber-100/80 mt-1 truncate">
                  「{duplicateBook.title}」
                </p>
                <p className="text-xs text-amber-300/70 mt-2">
                  같은 책이지만 꼭 한 번 더 신청하고 싶다면, 아래에 사유를 적어
                  관리자에게 제한풀기를 신청할 수 있습니다.
                </p>
              </div>
            </div>
            <div className="mt-4">
              <label className="block text-xs text-amber-300/80 mb-2">
                제한풀기 사유 <span className="text-amber-400/60">(선택)</span>
              </label>
              <textarea
                value={exemptionReason}
                onChange={(e) => setExemptionReason(e.target.value)}
                placeholder="예: 여러 명이 돌려 읽고 싶습니다."
                rows={3}
                maxLength={500}
                className="w-full px-3 py-2 rounded-lg border border-amber-900/50 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-amber-700"
              />
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <Button
                variant="ghost"
                onClick={() => {
                  setDuplicateBook(null)
                  setExemptionReason('')
                }}
                disabled={exemptionMut.isPending}
              >
                닫기
              </Button>
              <Button
                onClick={handleRequestExemption}
                disabled={exemptionMut.isPending}
              >
                {exemptionMut.isPending ? '신청 중...' : '제한풀기 신청'}
              </Button>
            </div>
          </div>
        )}

        {/* 같은 달 타 회원 신청 충돌 경고 + 제한풀기 신청 */}
        {monthlyConflict && (
          <div className="mb-8 rounded-xl border border-amber-900/50 bg-amber-950/30 p-5">
            <div className="flex items-start gap-3">
              <span className="text-lg leading-none">⚠️</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-amber-200">
                  이번 달 다른 회원이 이미 신청한 책입니다
                </p>
                <p className="text-sm text-amber-100/80 mt-1 truncate">
                  「{monthlyConflict.bookTitle}」
                  <span className="text-amber-300/80">
                    {' · '}
                    {monthlyConflict.requesterName}
                    {monthlyConflict.requesterEmail ? `(${monthlyConflict.requesterEmail})` : ''}님
                  </span>
                </p>
                <p className="text-xs text-amber-300/70 mt-2">
                  같은 책이지만 꼭 한 번 더 신청하고 싶다면, 사유를 적어
                  관리자에게 제한풀기를 신청할 수 있습니다. 승인되면 같은 책도
                  중복 없이 신청할 수 있습니다.
                </p>
              </div>
            </div>
            <div className="mt-4">
              <label className="block text-xs text-amber-300/80 mb-2">
                제한풀기 사유 <span className="text-amber-400/60">(선택)</span>
              </label>
              <textarea
                value={exemptionReason}
                onChange={(e) => setExemptionReason(e.target.value)}
                placeholder="예: 여러 명이 돌려 읽고 싶습니다."
                rows={3}
                maxLength={500}
                className="w-full px-3 py-2 rounded-lg border border-amber-900/50 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-amber-700"
              />
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <Button
                variant="ghost"
                onClick={() => {
                  setMonthlyConflict(null)
                  setExemptionReason('')
                }}
                disabled={exemptionByUrlMut.isPending}
              >
                닫기
              </Button>
              <Button
                onClick={handleRequestExemptionByUrl}
                disabled={exemptionByUrlMut.isPending}
              >
                {exemptionByUrlMut.isPending ? '신청 중...' : '제한풀기 신청'}
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
