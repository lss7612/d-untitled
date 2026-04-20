import { useNavigate } from 'react-router-dom'
import AppHeader from '@/components/AppHeader'
import { MagicCard } from '@/components/ui/magic-card'
import { useAllClubs } from '@/hooks/useClubs'
import type { ClubResponse } from '@/api/clubs'

const CLUB_EMOJI: Record<string, string> = {
  READING: '📚',
  GENERAL: '🎯',
}

export default function AllClubsPage() {
  const navigate = useNavigate()
  const { data: clubs, isLoading, error } = useAllClubs()

  function handleClubClick(club: ClubResponse) {
    if (club.name === '무제') navigate('/muje')
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-2xl mx-auto px-6 py-14">
        <h2 className="text-xl font-semibold mb-1 text-zinc-200">동호회</h2>
        <p className="text-sm text-zinc-500 mb-8">참여 가능한 모든 동호회입니다.</p>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
        {error && <p className="text-sm text-red-400">동호회 목록을 불러오지 못했습니다.</p>}

        <div className="grid gap-3">
          {clubs?.map((club) => (
            <div key={club.id} onClick={() => handleClubClick(club)} className="cursor-pointer rounded-xl">
              <MagicCard
                className="p-5 rounded-xl border border-zinc-800/40 bg-zinc-900/50"
                gradientColor="#27272a"
              >
                <div className="flex items-center gap-4">
                  <span className="text-2xl">{CLUB_EMOJI[club.type] ?? '🏷'}</span>
                  <div className="flex-1">
                    <p className="font-medium text-zinc-200">
                      {club.name}
                      {club.joined && (
                        <span className="ml-2 text-xs text-emerald-400 align-middle">
                          {club.myRole === 'ADMIN' ? '관리자' : '가입'}
                        </span>
                      )}
                    </p>
                    <p className="text-sm text-zinc-500 mt-0.5">{club.description}</p>
                  </div>
                </div>
              </MagicCard>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}
