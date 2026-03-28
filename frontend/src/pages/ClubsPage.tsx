import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { MagicCard } from '@/components/ui/magic-card'

const MY_CLUBS = [
  {
    id: 1,
    name: '무제',
    description: '독서 동호회 — 매달 책을 읽고 독후감을 나눕니다.',
    emoji: '📚',
  },
]

export default function ClubsPage() {
  function handleClubClick() {
    toast.info('준비 중입니다.', { description: '무제 대시보드는 Phase 2에서 연결됩니다.' })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-2xl mx-auto px-6 py-14">
        <h2 className="text-xl font-semibold mb-1 text-zinc-200">내 동호회</h2>
        <p className="text-sm text-zinc-500 mb-8">가입된 동호회 목록입니다.</p>

        <div className="grid gap-3">
          {MY_CLUBS.map((club) => (
            <MagicCard
              key={club.id}
              onClick={handleClubClick}
              className="cursor-pointer p-5 rounded-xl border border-zinc-800/40 bg-zinc-900/50"
              gradientColor="#27272a"
            >
              <div className="flex items-center gap-4">
                <span className="text-2xl">{club.emoji}</span>
                <div>
                  <p className="font-medium text-zinc-200">{club.name}</p>
                  <p className="text-sm text-zinc-500 mt-0.5">{club.description}</p>
                </div>
              </div>
            </MagicCard>
          ))}
        </div>
      </main>
    </div>
  )
}
