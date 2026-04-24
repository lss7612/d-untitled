import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import {
  useExemptBooks,
  useRevokeBookExemption,
} from '@/hooks/useBookExemptions'

const MUJE_CLUB_ID = 1

export default function AdminExemptBooksPage() {
  const navigate = useNavigate()
  const { data: rows, isLoading } = useExemptBooks(MUJE_CLUB_ID)
  const revokeMut = useRevokeBookExemption(MUJE_CLUB_ID)

  function handleRevoke(bookId: number, title: string) {
    if (
      !confirm(
        `「${title}」 제한을 다시 적용하시겠습니까?\n이후 회원이 이 책을 신청하면 다시 중복 차단됩니다.`
      )
    )
      return
    revokeMut.mutate(bookId, {
      onSuccess: () => toast.success('제한을 다시 적용했습니다.'),
      onError: (e) => toast.error(e.message),
    })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">
              관리자 — 제한 해제된 책 목록
            </h2>
            <p className="text-sm text-zinc-500 mt-1">
              제한풀기가 승인된 책입니다. 필요 시 다시 제한을 걸 수 있습니다.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}

        {!isLoading && (rows?.length ?? 0) === 0 && (
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-8 text-center">
            <p className="text-sm text-zinc-500">현재 제한 해제된 책이 없습니다.</p>
          </div>
        )}

        <ul className="space-y-3">
          {rows?.map((b) => (
            <li
              key={b.id}
              className="rounded-xl border border-amber-900/40 bg-amber-950/15 p-4 flex items-start gap-4"
            >
              {b.thumbnailUrl ? (
                <img
                  src={b.thumbnailUrl}
                  alt={b.title}
                  className="w-16 h-20 object-cover rounded-md border border-zinc-800/50 shrink-0"
                />
              ) : (
                <div className="w-16 h-20 rounded-md bg-zinc-800/50 shrink-0" />
              )}
              <div className="min-w-0 flex-1">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-medium text-zinc-100 truncate">
                    {b.title}
                  </p>
                  <span className="text-[10px] rounded-full px-2 py-0.5 bg-zinc-800/60 border border-zinc-700/50 text-zinc-300 shrink-0">
                    {b.copies}권 보유
                  </span>
                </div>
                {b.author && (
                  <p className="text-xs text-zinc-500 mt-0.5 truncate">{b.author}</p>
                )}
                <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-zinc-400">
                  {b.categoryLabel && (
                    <span className="rounded-md px-1.5 py-0.5 bg-zinc-800/50 text-zinc-300">
                      {b.categoryLabel}
                    </span>
                  )}
                  {b.price != null && (
                    <span>{b.price.toLocaleString('ko-KR')}원</span>
                  )}
                  {b.exemptionGrantedAt && (
                    <span className="text-zinc-500">
                      해제일:{' '}
                      {new Date(b.exemptionGrantedAt).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  )}
                </div>
              </div>
              <div className="shrink-0">
                <Button
                  size="sm"
                  onClick={() => handleRevoke(b.id, b.title)}
                  disabled={revokeMut.isPending}
                >
                  제한 재적용
                </Button>
              </div>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}
