import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import {
  useMyBookReports,
  useClubBookReports,
  useSubmitBookReport,
  useUpdateBookReport,
} from '@/hooks/useBookReports'
import {
  MIN_CONTENT_LENGTH,
  type BookReportPayload,
  type BookReportResponse,
  type BookRequestSummary,
} from '@/api/bookReports'
import { currentYearMonth, dDay, formatKoreanDate } from '@/lib/date'

const MUJE_CLUB_ID = 1

type BookSource = 'request' | 'custom'

interface EditorProps {
  myRequests: BookRequestSummary[]
  initial: BookReportResponse | null
  onSubmit: (payload: BookReportPayload) => void
  onCancel?: () => void
  pending: boolean
}

function ReportEditor({ myRequests, initial, onSubmit, onCancel, pending }: EditorProps) {
  const initialMode: BookSource = initial
    ? initial.bookRequestId ? 'request' : 'custom'
    : myRequests.length > 0 ? 'request' : 'custom'

  const [mode, setMode] = useState<BookSource>(initialMode)
  const [bookRequestId, setBookRequestId] = useState<number | null>(initial?.bookRequestId ?? myRequests[0]?.bookRequestId ?? null)
  const [bookTitle, setBookTitle] = useState(initial?.bookTitle ?? '')
  const [bookAuthor, setBookAuthor] = useState(initial?.bookAuthor ?? '')
  const [bookThumbnailUrl, setBookThumbnailUrl] = useState(initial?.bookThumbnailUrl ?? '')
  const [title, setTitle] = useState(initial?.title ?? '')
  const [content, setContent] = useState(initial?.content ?? '')

  useEffect(() => {
    if (mode === 'request' && bookRequestId == null && myRequests.length > 0) {
      setBookRequestId(myRequests[0].bookRequestId)
    }
  }, [mode, myRequests, bookRequestId])

  const charCount = content.length
  const tooShort = charCount < MIN_CONTENT_LENGTH

  function handleSubmit() {
    if (!title.trim()) return toast.warning('독후감 제목을 입력하세요.')
    if (tooShort) return toast.warning(`본문 최소 ${MIN_CONTENT_LENGTH}자`)

    if (mode === 'request') {
      if (bookRequestId == null) return toast.warning('신청한 책을 선택하세요.')
      onSubmit({ bookRequestId, title: title.trim(), content })
    } else {
      if (!bookTitle.trim()) return toast.warning('도서 제목을 입력하세요.')
      onSubmit({
        bookRequestId: null,
        bookTitle: bookTitle.trim(),
        bookAuthor: bookAuthor.trim() || null,
        bookThumbnailUrl: bookThumbnailUrl.trim() || null,
        title: title.trim(),
        content,
      })
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-xs text-zinc-500 mb-2">독후감 대상 도서</label>
        <div className="flex gap-2 mb-3">
          <button
            type="button"
            onClick={() => setMode('request')}
            disabled={myRequests.length === 0}
            className={`px-3 py-1.5 rounded-lg text-sm border ${
              mode === 'request'
                ? 'border-amber-500 bg-amber-950/40 text-amber-300'
                : 'border-zinc-800 bg-zinc-950 text-zinc-400 disabled:opacity-50'
            }`}
          >
            내가 신청한 책에서
          </button>
          <button
            type="button"
            onClick={() => setMode('custom')}
            className={`px-3 py-1.5 rounded-lg text-sm border ${
              mode === 'custom'
                ? 'border-amber-500 bg-amber-950/40 text-amber-300'
                : 'border-zinc-800 bg-zinc-950 text-zinc-400'
            }`}
          >
            다른 책 (직접 입력)
          </button>
        </div>

        {mode === 'request' && (
          <select
            value={bookRequestId ?? ''}
            onChange={(e) => setBookRequestId(Number(e.target.value))}
            className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
          >
            {myRequests.map((br) => (
              <option key={br.bookRequestId} value={br.bookRequestId}>
                {br.title} {br.author ? `· ${br.author}` : ''}
              </option>
            ))}
          </select>
        )}

        {mode === 'custom' && (
          <div className="space-y-2">
            <input
              type="text"
              placeholder="도서 제목 *"
              value={bookTitle}
              onChange={(e) => setBookTitle(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
            />
            <input
              type="text"
              placeholder="저자 (선택)"
              value={bookAuthor}
              onChange={(e) => setBookAuthor(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
            />
            <input
              type="url"
              placeholder="표지 이미지 URL (선택)"
              value={bookThumbnailUrl}
              onChange={(e) => setBookThumbnailUrl(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
            />
          </div>
        )}
      </div>

      <div>
        <label className="block text-xs text-zinc-500 mb-1">독후감 제목</label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
        />
      </div>

      <div>
        <div className="flex justify-between mb-1">
          <label className="text-xs text-zinc-500">본문</label>
          <span className={`text-xs ${tooShort ? 'text-amber-400' : 'text-zinc-500'}`}>
            {charCount} / {MIN_CONTENT_LENGTH}자 이상
          </span>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={8}
          className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600 resize-y"
        />
      </div>

      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button variant="ghost" onClick={onCancel}>
            취소
          </Button>
        )}
        <Button onClick={handleSubmit} disabled={pending}>
          {pending ? '저장 중...' : initial ? '수정 저장' : '제출'}
        </Button>
      </div>
    </div>
  )
}

function ReportView({ report }: { report: BookReportResponse }) {
  return (
    <div className="space-y-3">
      <div className="flex items-start gap-4">
        {report.bookThumbnailUrl && (
          <img
            src={report.bookThumbnailUrl}
            alt={report.bookTitle}
            className="w-20 h-28 object-cover rounded border border-zinc-800"
          />
        )}
        <div className="flex-1 min-w-0">
          <p className="text-xs text-zinc-500 mb-1">{report.bookRequestId ? '신청한 책' : '직접 추가한 책'}</p>
          <p className="font-medium text-zinc-100">{report.bookTitle}</p>
          {report.bookAuthor && <p className="text-xs text-zinc-500 mt-1">{report.bookAuthor}</p>}
        </div>
      </div>
      <div>
        <p className="text-sm font-medium text-zinc-200">{report.title}</p>
        <p className="text-sm text-zinc-300 mt-2 whitespace-pre-wrap leading-relaxed">{report.content}</p>
      </div>
      <p className="text-xs text-zinc-600">
        제출 {new Date(report.submittedAt).toLocaleString('ko-KR')}
        {report.updatedAt && report.updatedAt !== report.submittedAt && (
          <> · 수정 {new Date(report.updatedAt).toLocaleString('ko-KR')}</>
        )}
      </p>
    </div>
  )
}

function FeedCard({ report }: { report: BookReportResponse }) {
  const [open, setOpen] = useState(false)
  return (
    <div
      className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 hover:bg-zinc-900/80 transition-colors cursor-pointer"
      onClick={() => setOpen(!open)}
    >
      <div className="px-5 py-4 flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <p className="font-medium text-zinc-100">{report.memberName ?? `회원#${report.memberId}`}</p>
          <p className="text-xs text-zinc-500 mt-1 truncate">📖 {report.bookTitle}</p>
        </div>
        <span className="text-zinc-600 text-sm ml-3">{open ? '▲' : '▼'}</span>
      </div>
      {open && (
        <div className="px-5 pb-5 border-t border-zinc-800/40 pt-3">
          <p className="text-sm font-medium text-zinc-200 mb-2">{report.title}</p>
          <p className="text-sm text-zinc-300 whitespace-pre-wrap leading-relaxed">{report.content}</p>
          <p className="text-xs text-zinc-600 mt-3">
            {new Date(report.submittedAt).toLocaleString('ko-KR')}
          </p>
        </div>
      )}
    </div>
  )
}

export default function BookReportPage() {
  const navigate = useNavigate()
  const [yearMonth, setYearMonth] = useState(currentYearMonth())

  const isCurrentMonth = yearMonth === currentYearMonth()

  const { data: mine, isLoading: mineLoading } = useMyBookReports(MUJE_CLUB_ID, yearMonth)
  const { data: feed, isLoading: feedLoading } = useClubBookReports(MUJE_CLUB_ID, yearMonth)
  const submitMut = useSubmitBookReport(MUJE_CLUB_ID)
  const updateMut = useUpdateBookReport(MUJE_CLUB_ID)

  const [editing, setEditing] = useState(false)

  const report = mine?.report ?? null
  const overdue = mine?.overdue ?? false
  const deadline = mine?.deadline
  const myRequests = mine?.myRequests ?? []

  const ddayLabel = useMemo(() => (deadline ? dDay(deadline) : null), [deadline])
  const dateLabel = useMemo(() => (deadline ? formatKoreanDate(deadline) : null), [deadline])

  function handleSubmit(payload: BookReportPayload) {
    if (report) {
      updateMut.mutate(
        { id: report.id, payload },
        {
          onSuccess: () => {
            toast.success('수정 완료')
            setEditing(false)
          },
          onError: (e) => toast.error(e.message),
        }
      )
    } else {
      submitMut.mutate(payload, {
        onSuccess: () => {
          toast.success('제출 완료')
          setEditing(false)
        },
        onError: (e) => toast.error(e.message),
      })
    }
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">독후감</h2>
            <p className="text-sm text-zinc-500 mt-1">
              이번 달 독후감 1건을 제출하고 다른 회원의 글도 읽어보세요.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {/* 월 picker */}
        <div className="mb-6 flex items-center gap-3">
          <label className="text-xs text-zinc-500">월 선택</label>
          <input
            type="month"
            value={yearMonth}
            onChange={(e) => setYearMonth(e.target.value)}
            className="px-3 py-1.5 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
          />
          {!isCurrentMonth && (
            <button
              onClick={() => setYearMonth(currentYearMonth())}
              className="text-xs text-amber-400 hover:underline"
            >
              이번 달로
            </button>
          )}
        </div>

        {/* 마감 D-day 배너 (현재 월일 때만) */}
        {isCurrentMonth && deadline && (
          <div
            className={`mb-6 rounded-xl border p-4 ${
              overdue ? 'border-red-900/40 bg-red-950/20' : 'border-zinc-800/40 bg-zinc-900/50'
            }`}
          >
            <p className={`text-sm ${overdue ? 'text-red-300' : 'text-zinc-300'}`}>
              {overdue ? '⚠ 마감 지남' : '🗓'} 독후감 마감: {dateLabel}{' '}
              <span className="ml-2 text-xs text-zinc-500">{ddayLabel}</span>
            </p>
            <p className="text-xs text-zinc-500 mt-1">
              상태: {report ? '✓ 제출 완료' : '미제출'}
              {!report && ' · 본문 50자 이상'}
            </p>
          </div>
        )}

        {/* 내 독후감 */}
        {!mineLoading && (
          <section className="mb-8">
            <h3 className="text-xs text-zinc-500 uppercase tracking-wider mb-3">내 독후감</h3>
            <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
              {!report && !editing && isCurrentMonth && (
                <div className="text-center py-6">
                  <p className="text-sm text-zinc-400 mb-4">아직 이번 달 독후감을 작성하지 않았습니다.</p>
                  <Button onClick={() => setEditing(true)} disabled={overdue}>
                    {overdue ? '마감되었습니다' : '독후감 작성'}
                  </Button>
                </div>
              )}
              {!report && !editing && !isCurrentMonth && (
                <p className="text-sm text-zinc-500 text-center py-6">
                  이 달의 내 독후감 기록이 없습니다.
                </p>
              )}
              {report && !editing && (
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <p className="text-sm font-medium text-zinc-200">{yearMonth} 내 독후감</p>
                    {isCurrentMonth && !overdue && (
                      <Button variant="ghost" onClick={() => setEditing(true)}>
                        수정
                      </Button>
                    )}
                  </div>
                  <ReportView report={report} />
                  {overdue && (
                    <p className="mt-4 text-xs text-zinc-500">📌 마감 후엔 수정 불가</p>
                  )}
                </div>
              )}
              {editing && (
                <ReportEditor
                  myRequests={myRequests}
                  initial={report}
                  pending={submitMut.isPending || updateMut.isPending}
                  onSubmit={handleSubmit}
                  onCancel={() => setEditing(false)}
                />
              )}
            </div>
          </section>
        )}

        {/* 동호회 피드 */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs text-zinc-500 uppercase tracking-wider">
              동호회 독후감 ({feed?.length ?? 0})
            </h3>
            <p className="text-xs text-zinc-600">{yearMonth}</p>
          </div>
          {feedLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
          {!feedLoading && (feed?.length ?? 0) === 0 && (
            <p className="text-sm text-zinc-500 text-center py-8">
              이 달 작성된 독후감이 없습니다.
            </p>
          )}
          <div className="space-y-3">
            {feed?.map((r) => (
              <FeedCard key={r.id} report={r} />
            ))}
          </div>
        </section>
      </main>
    </div>
  )
}
