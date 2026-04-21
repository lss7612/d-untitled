import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import { useMissingSubmitters } from '@/hooks/useBookReports'
import { currentYearMonth, dDay, formatKoreanDate } from '@/lib/date'

const MUJE_CLUB_ID = 1

export default function AdminBookReportsPage() {
  const navigate = useNavigate()
  const [yearMonth, setYearMonth] = useState(currentYearMonth())

  const { data, isLoading, error } = useMissingSubmitters(MUJE_CLUB_ID, yearMonth)

  const isCurrentMonth = yearMonth === currentYearMonth()

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 독후감 미제출자</h2>
            <p className="text-sm text-zinc-500 mt-1">월별 미제출자 + 작성률을 확인합니다.</p>
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

        {error && <p className="text-sm text-red-400 mb-4">{error.message}</p>}

        {/* 통계 카드 */}
        {data && (
          <section className="mb-6 grid grid-cols-3 gap-3">
            <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
              <p className="text-xs text-zinc-500">전체 회원</p>
              <p className="text-2xl font-semibold text-zinc-100 mt-1">{data.totalMembers}</p>
            </div>
            <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
              <p className="text-xs text-zinc-500">제출</p>
              <p className="text-2xl font-semibold text-emerald-400 mt-1">{data.submittedCount}</p>
            </div>
            <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
              <p className="text-xs text-zinc-500">미제출</p>
              <p className="text-2xl font-semibold text-amber-400 mt-1">{data.missing.length}</p>
            </div>
          </section>
        )}

        {/* 마감 정보 */}
        {data?.deadline && (
          <div className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
            <p className="text-sm text-zinc-300">
              🗓 마감일: {formatKoreanDate(data.deadline)}{' '}
              <span className="ml-2 text-xs text-zinc-500">{dDay(data.deadline)}</span>
            </p>
          </div>
        )}

        {/* 미제출자 목록 */}
        <section className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <p className="text-sm font-medium text-zinc-200 mb-4">미제출자 ({data?.missing.length ?? 0})</p>
          {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
          {!isLoading && (data?.missing.length ?? 0) === 0 && (
            <p className="text-sm text-emerald-400 text-center py-6">🎉 미제출자가 없습니다.</p>
          )}
          {(data?.missing ?? []).length > 0 && (
            <ul className="divide-y divide-zinc-800/40">
              {data!.missing.map((m) => (
                <li key={m.memberId} className="py-3 flex items-center justify-between">
                  <div>
                    <p className="text-sm text-zinc-200">{m.memberName ?? `회원#${m.memberId}`}</p>
                    {m.memberEmail && (
                      <p className="text-xs text-zinc-500 mt-0.5">{m.memberEmail}</p>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  )
}
