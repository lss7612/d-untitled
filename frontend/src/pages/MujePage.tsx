import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { DotPattern } from '@/components/ui/dot-pattern'
import { useMe } from '@/hooks/useMe'

const SCHEDULE = [
  { label: '책 신청 마감', date: '4월 5일', dday: 'D-7' },
  { label: '독후감 마감', date: '4월 12일', dday: 'D-14' },
  { label: '촬영일', date: '4월 19일', dday: 'D-21' },
]

const MENU_ITEMS = [
  { emoji: '📚', label: '책 신청' },
  { emoji: '✍️', label: '독후감 제출' },
  { emoji: '👤', label: '내 정보' },
]

export default function MujePage() {
  const { data: me, isLoading } = useMe()

  function handleMenu() {
    toast.info('준비 중입니다.')
  }

  const greeting = isLoading
    ? '안녕하세요'
    : `안녕하세요, ${me?.name ?? ''}님`

  return (
    <div className="relative min-h-screen bg-zinc-950 text-zinc-200 overflow-hidden">
      <DotPattern className="text-zinc-800/40" />

      <div className="relative z-10">
        <AppHeader />

        <main className="max-w-2xl mx-auto px-6 py-14">
          {/* 인사 */}
          <div className="mb-10">
            <h2 className="text-2xl font-semibold text-zinc-100">
              {greeting}
            </h2>
            <p className="text-sm text-zinc-500 mt-1">무제 독서 동호회입니다.</p>
          </div>

          {/* 이번 달 일정 */}
          <section className="mb-10">
            <h3 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">
              이번 달 일정
            </h3>
            <div className="grid grid-cols-3 gap-3">
              {SCHEDULE.map((item) => (
                <div
                  key={item.label}
                  className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4"
                >
                  <p className="text-xs text-zinc-500 mb-1">{item.label}</p>
                  <p className="text-sm font-medium text-zinc-200">{item.date}</p>
                  <p className="text-xs text-zinc-400 mt-1">{item.dday}</p>
                </div>
              ))}
            </div>
          </section>

          {/* 메뉴 버튼 */}
          <section>
            <h3 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">
              메뉴
            </h3>
            <div className="grid grid-cols-3 gap-3">
              {MENU_ITEMS.map((item) => (
                <button
                  key={item.label}
                  onClick={handleMenu}
                  className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5 flex flex-col items-center gap-2 hover:bg-zinc-800/50 transition-colors cursor-pointer"
                >
                  <span className="text-2xl">{item.emoji}</span>
                  <span className="text-sm text-zinc-300">{item.label}</span>
                </button>
              ))}
            </div>
          </section>
        </main>
      </div>
    </div>
  )
}
