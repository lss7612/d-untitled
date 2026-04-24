import { toast } from 'sonner'
import { useNavigate } from 'react-router-dom'
import { Check, X } from 'lucide-react'
import AppHeader from '@/components/AppHeader'
import { DotPattern } from '@/components/ui/dot-pattern'
import { useMe } from '@/hooks/useMe'
import {
  useClubSchedules,
  useMyClubs,
  usePendingJoinRequests,
  useApproveJoinRequest,
  useRejectJoinRequest,
} from '@/hooks/useClubs'
import {
  useIncomingShares,
  useAcceptShare,
  useRejectShare,
} from '@/hooks/useBudgetShares'
import { currentYearMonth, formatKoreanDate, dDay, scheduleLabel } from '@/lib/date'

const MUJE_CLUB_ID = 1

const MENU_ITEMS = [
  { emoji: '📚', label: '책 신청', path: '/muje/book-requests' },
  { emoji: '📖', label: '보유 책 검색', path: '/muje/books' },
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
  const isDeveloper = me?.role === 'DEVELOPER'
  const canManageJoins = isAdmin || isDeveloper

  const { data: joinRequests } = usePendingJoinRequests(MUJE_CLUB_ID, canManageJoins)
  const approveMutation = useApproveJoinRequest(MUJE_CLUB_ID)
  const rejectMutation = useRejectJoinRequest(MUJE_CLUB_ID)

  // 내게 온 나눔 신청 (수락 대기)
  const currentYm = currentYearMonth()
  const { data: incomingShares = [] } = useIncomingShares(MUJE_CLUB_ID, currentYm)
  const acceptShareMut = useAcceptShare(MUJE_CLUB_ID)
  const rejectShareMut = useRejectShare(MUJE_CLUB_ID)

  function handleApprove(memberId: number, name: string | null) {
    approveMutation.mutate(memberId, {
      onSuccess: () => toast.success(`${name ?? '회원'} 가입 승인 완료`),
      onError: (e: Error) => toast.error(e.message || '승인 실패'),
    })
  }
  function handleReject(memberId: number, name: string | null) {
    rejectMutation.mutate(memberId, {
      onSuccess: () => toast.success(`${name ?? '회원'} 가입 거절`),
      onError: (e: Error) => toast.error(e.message || '거절 실패'),
    })
  }

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

          {me?.role === 'DEVELOPER' && (
            <section className="mt-6">
              <h3 className="text-sm font-medium text-purple-400 uppercase tracking-wider mb-3">개발자</h3>
              <div className="grid grid-cols-1 gap-3">
                <button
                  onClick={() => navigate('/developer/whitelist')}
                  className="rounded-xl border border-purple-900/40 bg-purple-950/20 p-5 flex items-center gap-3 hover:bg-purple-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">🔐</span>
                  <div>
                    <p className="text-sm text-zinc-200">화이트리스트 관리</p>
                    <p className="text-xs text-zinc-500 mt-0.5">도메인 외 허용 이메일 추가/삭제</p>
                  </div>
                </button>
              </div>
            </section>
          )}

          {incomingShares.length > 0 && (
            <section className="mt-6">
              <h3 className="text-sm font-medium text-sky-400 uppercase tracking-wider mb-3">
                나눔 신청 ({incomingShares.length})
              </h3>
              <ul className="space-y-2">
                {incomingShares.map((s) => (
                  <li
                    key={s.id}
                    className="flex items-center justify-between rounded-xl border border-sky-900/40 bg-sky-950/15 px-4 py-3"
                  >
                    <div className="min-w-0">
                      <p className="text-sm text-zinc-200">
                        <span className="font-medium">{s.requesterName ?? '(이름 없음)'}</span>
                        {'님이 '}
                        <span className="text-sky-300">{s.amount.toLocaleString()}원</span>
                        {' 나눔을 신청했습니다.'}
                      </p>
                      {s.note && (
                        <p className="text-xs text-zinc-500 mt-0.5 truncate">{s.note}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() =>
                          acceptShareMut.mutate(s.id, {
                            onSuccess: () => toast.success('수락 완료'),
                            onError: (e: Error) => toast.error(e.message),
                          })
                        }
                        disabled={acceptShareMut.isPending || rejectShareMut.isPending}
                        className="flex items-center gap-1 rounded-lg bg-sky-700 hover:bg-sky-600 px-3 py-1.5 text-xs font-medium text-white transition-colors disabled:opacity-50"
                      >
                        <Check size={12} /> 수락
                      </button>
                      <button
                        onClick={() =>
                          rejectShareMut.mutate(s.id, {
                            onSuccess: () => toast.success('거절 완료'),
                            onError: (e: Error) => toast.error(e.message),
                          })
                        }
                        disabled={acceptShareMut.isPending || rejectShareMut.isPending}
                        className="flex items-center gap-1 rounded-lg bg-zinc-700 hover:bg-zinc-600 px-3 py-1.5 text-xs font-medium text-zinc-200 transition-colors disabled:opacity-50"
                      >
                        <X size={12} /> 거절
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {canManageJoins && joinRequests && joinRequests.length > 0 && (
            <section className="mt-6">
              <h3 className="text-sm font-medium text-amber-400 uppercase tracking-wider mb-3">
                가입 신청 ({joinRequests.length})
              </h3>
              <ul className="space-y-2">
                {joinRequests.map((req) => (
                  <li
                    key={req.memberId}
                    className="flex items-center justify-between rounded-xl border border-amber-900/40 bg-amber-950/10 px-4 py-3"
                  >
                    <div>
                      <p className="text-sm text-zinc-200">{req.memberName ?? '(이름 없음)'}</p>
                      <p className="text-xs text-zinc-500 mt-0.5">{req.memberEmail}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleApprove(req.memberId, req.memberName)}
                        disabled={approveMutation.isPending || rejectMutation.isPending}
                        className="flex items-center gap-1 rounded-lg bg-emerald-700 hover:bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white transition-colors disabled:opacity-50"
                      >
                        <Check size={12} /> 승인
                      </button>
                      <button
                        onClick={() => handleReject(req.memberId, req.memberName)}
                        disabled={approveMutation.isPending || rejectMutation.isPending}
                        className="flex items-center gap-1 rounded-lg bg-zinc-700 hover:bg-zinc-600 px-3 py-1.5 text-xs font-medium text-zinc-200 transition-colors disabled:opacity-50"
                      >
                        <X size={12} /> 거절
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}

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
                <button
                  onClick={() => navigate('/muje/admin/arrivals')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">📦</span>
                  <div>
                    <p className="text-sm text-zinc-200">도착 / 수령 관리</p>
                    <p className="text-xs text-zinc-500 mt-0.5">도착한 책 처리 + 미수령자 확인</p>
                  </div>
                </button>
                <button
                  onClick={() => navigate('/muje/admin/members')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">🛡️</span>
                  <div>
                    <p className="text-sm text-zinc-200">멤버 관리</p>
                    <p className="text-xs text-zinc-500 mt-0.5">관리자 지정 / 해제</p>
                  </div>
                </button>
                <button
                  onClick={() => navigate('/muje/admin/book-exemptions')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">🔓</span>
                  <div>
                    <p className="text-sm text-zinc-200">제한풀기 신청</p>
                    <p className="text-xs text-zinc-500 mt-0.5">중복 책 신청 허용 요청 승인 / 거절</p>
                  </div>
                </button>
                <button
                  onClick={() => navigate('/muje/admin/exempt-books')}
                  className="rounded-xl border border-amber-900/40 bg-amber-950/20 p-5 flex items-center gap-3 hover:bg-amber-950/30 transition-colors cursor-pointer text-left"
                >
                  <span className="text-2xl">🔁</span>
                  <div>
                    <p className="text-sm text-zinc-200">제한 재적용</p>
                    <p className="text-xs text-zinc-500 mt-0.5">제한풀기 승인된 책 되돌리기</p>
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
