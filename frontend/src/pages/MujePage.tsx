import { toast } from 'sonner'
import { useNavigate } from 'react-router-dom'
import AppHeader from '@/components/AppHeader'
import { DotPattern } from '@/components/ui/dot-pattern'
import { useMe } from '@/hooks/useMe'
import { useClubSchedules, useMyClubs } from '@/hooks/useClubs'
import { currentYearMonth, formatKoreanDate, dDay, scheduleLabel } from '@/lib/date'

const MUJE_CLUB_ID = 1

const MENU_ITEMS = [
  { emoji: '📚', label: '책 신청', path: '/muje/book-requests' },
  { emoji: '✍️', label: '독후감 제출', path: '/muje/book-reports' },
  { emoji: '👤', label: '내 정보', path: null },
]

export default function MujePage() {
  const navigate = useNavigate()
  const { data: me, isLoading: meLoading } = useMe()
  const { data: schedules, isLoading: schedulesLoading } = useClubSchedules(
    MUJE_CLUB_ID,
    currentYearMonth()
  )
  const { data: myClubs } = useMyClubs()
  const isAdmin = myClubs?.some((c) => c.id === MUJE_CLUB_ID && c.myRole === 'ADMIN') ?? false

  function handleMenu(path: string | null) {
    if (path) navigate(path)
    else toast.info('준비 중입니다.', { description: '다음 청크에서 연결됩니다.' })
  }

  const greeting = meLoading ? '안녕하세요' : `안녕하세요, ${me?.name ?? ''}님`

  return (
    <div className="relative min-h-screen bg-zinc-950 text-zinc-200 overflow-hidden">
      <DotPattern className="text-zinc-800/40" />

      <div className="relative z-10">
        <AppHeader />

        <main className="max-w-2xl mx-auto px-6 py-14">
          <div className="mb-10">
            <h2 className="text-2xl font-semibold text-zinc-100">{greeting}</h2>
            <p className="text-sm text-zinc-500 mt-1">무제 독서 동호회입니다.</p>
          </div>

          <section className="mb-10">
            <h3 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">
              이번 달 일정
            </h3>
            {schedulesLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
            {!schedulesLoading && schedules?.length === 0 && (
              <p className="text-sm text-zinc-500">등록된 일정이 없습니다.</p>
            )}
            {schedules && schedules.length > 0 && (
              <div className="grid grid-cols-3 gap-3">
                {schedules.map((s) => (
                  <div
                    key={s.id}
                    className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4"
                  >
                    <p className="text-xs text-zinc-500 mb-1">{scheduleLabel(s.typeCode)}</p>
                    <p className="text-sm font-medium text-zinc-200">{formatKoreanDate(s.date)}</p>
                    <p className="text-xs text-zinc-400 mt-1">{dDay(s.date)}</p>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section>
            <h3 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">메뉴</h3>
            <div className="grid grid-cols-3 gap-3">
              {MENU_ITEMS.map((item) => (
                <button
                  key={item.label}
                  onClick={() => handleMenu(item.path)}
                  className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5 flex flex-col items-center gap-2 hover:bg-zinc-800/50 transition-colors cursor-pointer"
                >
                  <span className="text-2xl">{item.emoji}</span>
                  <span className="text-sm text-zinc-300">{item.label}</span>
                </button>
              ))}
            </div>
          </section>

          {isAdmin && (
            <section className="mt-6">
              <h3 className="text-sm font-medium text-amber-400 uppercase tracking-wider mb-3">관리자</h3>
              <div className="grid grid-cols-1 gap-3">
                <button
                  onClick={() => navigate('/muje/admin/book-requests')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">⚙️</span>
                  <div>
                    <p className="text-sm text-zinc-200">책 신청 관리</p>
                    <p className="text-xs text-zinc-500 mt-0.5">전체 신청 / 잠금 / 합산 주문서 / 알라딘 카트</p>
                  </div>
                </button>
                <button
                  onClick={() => navigate('/muje/admin/book-reports')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">📋</span>
                  <div>
                    <p className="text-sm text-zinc-200">독후감 미제출자</p>
                    <p className="text-xs text-zinc-500 mt-0.5">월별 작성률 / 미제출자 명단</p>
                  </div>
                </button>
              </div>
            </section>
          )}
        </main>
      </div>
    </div>
  )
}
