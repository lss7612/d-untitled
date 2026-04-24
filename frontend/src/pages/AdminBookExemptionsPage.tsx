import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import {
  usePendingBookExemptions,
  useApproveBookExemption,
  useRejectBookExemption,
} from '@/hooks/useBookExemptions'

const MUJE_CLUB_ID = 1

export default function AdminBookExemptionsPage() {
  const navigate = useNavigate()
  const { data: rows, isLoading } = usePendingBookExemptions(MUJE_CLUB_ID)
  const approveMut = useApproveBookExemption(MUJE_CLUB_ID)
  const rejectMut = useRejectBookExemption(MUJE_CLUB_ID)

  function handleApprove(id: number, title: string | null) {
    if (!confirm(`「${title ?? '이 책'}」 제한풀기를 승인하시겠습니까?\n\n승인 이후 누구나 다시 이 책을 신청할 수 있습니다.`)) return
    approveMut.mutate(id, {
      onSuccess: () => toast.success('승인 완료'),
      onError: (e) => toast.error(e.message),
    })
  }

  function handleReject(id: number, title: string | null) {
    if (!confirm(`「${title ?? '이 책'}」 제한풀기 신청을 거절하시겠습니까?`)) return
    rejectMut.mutate(id, {
      onSuccess: () => toast.success('거절 완료'),
      onError: (e) => toast.error(e.message),
    })
  }

  const busy = approveMut.isPending || rejectMut.isPending

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 제한풀기 신청</h2>
            <p className="text-sm text-zinc-500 mt-1">
              이미 보유 중인 책에 대한 중복 신청 허용 요청을 검토합니다.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}

        {!isLoading && (rows?.length ?? 0) === 0 && (
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-8 text-center">
            <p className="text-sm text-zinc-500">대기 중인 제한풀기 신청이 없습니다.</p>
          </div>
        )}

        <ul className="space-y-3">
          {rows?.map((r) => (
            <li
              key={r.id}
              className="rounded-xl border border-amber-900/40 bg-amber-950/15 p-5"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-zinc-100 truncate">
                    {r.bookTitle ?? `책#${r.bookId}`}
                  </p>
                  {r.bookAuthor && (
                    <p className="text-xs text-zinc-500 mt-0.5 truncate">{r.bookAuthor}</p>
                  )}
                  <p className="text-xs text-zinc-400 mt-2">
                    신청자: <span className="text-zinc-200">{r.memberName ?? `회원#${r.memberId}`}</span>
                    {r.memberEmail && (
                      <span className="text-zinc-500 ml-1">({r.memberEmail})</span>
                    )}
                  </p>
                  <p className="text-xs text-zinc-500 mt-1">
                    신청일:{' '}
                    {new Date(r.createdAt).toLocaleString('ko-KR', {
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                  {r.reason && (
                    <div className="mt-3 rounded-lg border border-zinc-800/50 bg-zinc-950/60 px-3 py-2">
                      <p className="text-xs text-zinc-500 mb-1">사유</p>
                      <p className="text-sm text-zinc-200 whitespace-pre-wrap">{r.reason}</p>
                    </div>
                  )}
                </div>
                <div className="flex flex-col gap-2 shrink-0">
                  <Button
                    size="sm"
                    onClick={() => handleApprove(r.id, r.bookTitle)}
                    disabled={busy}
                  >
                    승인
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => handleReject(r.id, r.bookTitle)}
                    disabled={busy}
                  >
                    거절
                  </Button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}
