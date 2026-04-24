import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Shield, ShieldOff } from 'lucide-react'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import { useMe } from '@/hooks/useMe'
import { useClubMembers, useChangeClubMemberRole } from '@/hooks/useClubs'
import type { ClubRole, ClubMemberResponse } from '@/api/clubs'

const MUJE_CLUB_ID = 1

export default function AdminClubMembersPage() {
  const navigate = useNavigate()
  const { data: me } = useMe()
  const { data: members, isLoading, error } = useClubMembers(MUJE_CLUB_ID)
  const changeRoleMut = useChangeClubMemberRole(MUJE_CLUB_ID)

  function handleToggleRole(member: ClubMemberResponse) {
    const next: ClubRole = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'
    const label = next === 'ADMIN' ? '관리자로 지정' : '일반 멤버로 변경'
    const name = member.memberName ?? member.memberEmail ?? '이 회원'
    const ok = window.confirm(`${name}을(를) ${label}하시겠습니까?`)
    if (!ok) return
    changeRoleMut.mutate(
      { memberId: member.memberId, role: next },
      {
        onSuccess: () => toast.success(`${name} → ${label} 완료`),
        onError: (e: Error) => toast.error(e.message || '변경 실패'),
      }
    )
  }

  const sorted = members
    ? [...members].sort((a, b) => {
        // ADMIN 먼저, 이름 사전순
        if (a.role !== b.role) return a.role === 'ADMIN' ? -1 : 1
        return (a.memberName ?? '').localeCompare(b.memberName ?? '')
      })
    : []

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />
      <main className="max-w-3xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 멤버 관리</h2>
            <p className="text-sm text-zinc-500 mt-1">
              동호회 관리자(ClubRole.ADMIN)를 지정·해제합니다.
            </p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
        {error && <p className="text-sm text-red-400">{error.message}</p>}

        {sorted.length > 0 && (
          <ul className="space-y-2">
            {sorted.map((m) => {
              const isSelf = me?.id === m.memberId
              const isAdmin = m.role === 'ADMIN'
              return (
                <li
                  key={m.memberId}
                  className="flex items-center justify-between rounded-xl border border-zinc-800/40 bg-zinc-900/50 px-4 py-3"
                >
                  <div className="min-w-0 flex items-center gap-3">
                    <span
                      className={
                        'inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-medium shrink-0 ' +
                        (isAdmin
                          ? 'bg-amber-950/40 text-amber-300 border border-amber-900/40'
                          : 'bg-zinc-800/50 text-zinc-400 border border-zinc-700/40')
                      }
                    >
                      {isAdmin ? (
                        <>
                          <Shield size={10} /> ADMIN
                        </>
                      ) : (
                        'MEMBER'
                      )}
                    </span>
                    <div className="min-w-0">
                      <p className="text-sm text-zinc-200 truncate">
                        {m.memberName ?? '(이름 없음)'}
                        {isSelf && (
                          <span className="ml-2 text-[10px] text-zinc-500">(본인)</span>
                        )}
                      </p>
                      <p className="text-xs text-zinc-500 truncate">{m.memberEmail}</p>
                    </div>
                  </div>
                  {!isSelf && (
                    <button
                      onClick={() => handleToggleRole(m)}
                      disabled={changeRoleMut.isPending}
                      className={
                        'flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 shrink-0 ' +
                        (isAdmin
                          ? 'bg-zinc-700 hover:bg-zinc-600 text-zinc-200'
                          : 'bg-amber-700 hover:bg-amber-600 text-white')
                      }
                    >
                      {isAdmin ? (
                        <>
                          <ShieldOff size={12} /> 관리자 해제
                        </>
                      ) : (
                        <>
                          <Shield size={12} /> 관리자 지정
                        </>
                      )}
                    </button>
                  )}
                </li>
              )
            })}
          </ul>
        )}

        {!isLoading && sorted.length === 0 && (
          <p className="text-sm text-zinc-500">활성 멤버가 없습니다.</p>
        )}
      </main>
    </div>
  )
}
