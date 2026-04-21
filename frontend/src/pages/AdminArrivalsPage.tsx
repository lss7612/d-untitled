import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import { CheckboxRow } from '@/components/ui/checkbox-row'
import {
  useAdminBookRequests,
  useMarkArrived,
  useMarkUnarrived,
} from '@/hooks/useOrders'
import type { AdminBookRequestRow } from '@/api/orders'
import { currentYearMonth } from '@/lib/date'

const MUJE_CLUB_ID = 1

interface MemberGroup {
  memberId: number
  memberName: string
  rows: AdminBookRequestRow[]
}

function groupByMember(rows: AdminBookRequestRow[]): MemberGroup[] {
  const map = new Map<number, MemberGroup>()
  for (const r of rows) {
    const g = map.get(r.memberId) ?? {
      memberId: r.memberId,
      memberName: r.memberName ?? `회원#${r.memberId}`,
      rows: [],
    }
    g.rows.push(r)
    map.set(r.memberId, g)
  }
  return Array.from(map.values())
}

interface SectionProps {
  title: string
  rows: AdminBookRequestRow[]
  selected: Set<number>
  setSelected: (s: Set<number>) => void
  emptyText: string
  showArrivedAt?: boolean
  showReceivedAt?: boolean
}

function GroupedSection({ title, rows, selected, setSelected, emptyText, showArrivedAt, showReceivedAt }: SectionProps) {
  const groups = useMemo(() => groupByMember(rows), [rows])
  const allIds = rows.map((r) => r.id)
  const allChecked = allIds.length > 0 && allIds.every((id) => selected.has(id))

  function toggleAll() {
    setSelected(allChecked ? new Set() : new Set(allIds))
  }
  function toggleGroup(g: MemberGroup) {
    const ids = g.rows.map((r) => r.id)
    const allOn = ids.every((id) => selected.has(id))
    const next = new Set(selected)
    if (allOn) ids.forEach((id) => next.delete(id))
    else ids.forEach((id) => next.add(id))
    setSelected(next)
  }
  function toggleOne(id: number) {
    const next = new Set(selected)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setSelected(next)
  }

  return (
    <section className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5 mb-6">
      <div className="flex items-center justify-between mb-3">
        <p className="text-sm font-medium text-zinc-200">
          {title} ({rows.length}건)
        </p>
        {rows.length > 0 && (
          <CheckboxRow checked={allChecked} onChange={() => toggleAll()} className="px-3 py-2 text-xs text-zinc-400">
            전체 선택
          </CheckboxRow>
        )}
      </div>

      {rows.length === 0 && <p className="text-sm text-zinc-500 text-center py-6">{emptyText}</p>}

      <div className="space-y-3">
        {groups.map((g) => {
          const groupAmount = g.rows.reduce((s, r) => s + r.price, 0)
          const groupChecked = g.rows.every((r) => selected.has(r.id))
          return (
            <div key={g.memberId} className="border border-zinc-800/40 rounded-lg overflow-hidden">
              <CheckboxRow
                checked={groupChecked}
                onChange={() => toggleGroup(g)}
                className="bg-zinc-950/40"
              >
                <span className="text-sm font-medium text-zinc-200">{g.memberName}</span>
                <span className="text-xs text-zinc-500">
                  {g.rows.length}권 · ₩{groupAmount.toLocaleString()}
                </span>
              </CheckboxRow>
              <ul>
                {g.rows.map((r) => (
                  <li key={r.id} className="border-t border-zinc-800/30 hover:bg-zinc-950/30">
                    <CheckboxRow checked={selected.has(r.id)} onChange={() => toggleOne(r.id)}>
                      {r.thumbnailUrl && (
                        <img src={r.thumbnailUrl} alt={r.title} className="w-8 h-10 object-cover rounded border border-zinc-800" />
                      )}
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-zinc-200 truncate">{r.title}</p>
                        <p className="text-xs text-zinc-500 truncate">
                          {r.author} · {r.categoryLabel}
                          {showArrivedAt && r.arrivedAt && (
                            <span className="ml-2 text-amber-400">
                              · 도착 {new Date(r.arrivedAt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                            </span>
                          )}
                          {showReceivedAt && r.receivedAt && (
                            <span className="ml-2 text-emerald-400">
                              · 수령 {new Date(r.receivedAt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit' })}
                            </span>
                          )}
                        </p>
                      </div>
                    </CheckboxRow>
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </div>
    </section>
  )
}

export default function AdminArrivalsPage() {
  const navigate = useNavigate()
  const [yearMonth, setYearMonth] = useState(currentYearMonth())

  const { data: rows, isLoading } = useAdminBookRequests(MUJE_CLUB_ID, yearMonth)
  const markArrivedMut = useMarkArrived(MUJE_CLUB_ID)
  const markUnarrivedMut = useMarkUnarrived(MUJE_CLUB_ID)

  const [orderedSelected, setOrderedSelected] = useState<Set<number>>(new Set())
  const [arrivedSelected, setArrivedSelected] = useState<Set<number>>(new Set())

  const orderedRows = (rows ?? []).filter((r) => r.status === 'ORDERED')
  const arrivedRows = (rows ?? []).filter((r) => r.status === 'ARRIVED')
  const receivedRows = (rows ?? []).filter((r) => r.status === 'RECEIVED')

  function handleArrive() {
    const ids = Array.from(orderedSelected)
    if (ids.length === 0) return toast.warning('선택한 책이 없습니다.')
    if (!confirm(`선택한 ${ids.length}권을 도착 처리하시겠습니까?`)) return
    markArrivedMut.mutate(ids, {
      onSuccess: (r) => {
        toast.success(`도착 처리 완료 (${r.affected}권)`)
        setOrderedSelected(new Set())
      },
      onError: (e) => toast.error(e.message),
    })
  }

  function handleUnarrive() {
    const ids = Array.from(arrivedSelected)
    if (ids.length === 0) return toast.warning('선택한 책이 없습니다.')
    if (!confirm(
      `⚠ 도착 처리를 취소하시겠습니까?\n\n` +
      `회원이 이미 수령한 책은 취소하지 마세요.\n` +
      `선택한 ${ids.length}권이 ORDERED 상태로 되돌아갑니다.`
    )) return
    markUnarrivedMut.mutate(ids, {
      onSuccess: (r) => {
        toast.success(`도착 처리 취소 완료 (${r.affected}권)`)
        setArrivedSelected(new Set())
      },
      onError: (e) => toast.error(e.message),
    })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 도착 / 수령 관리</h2>
            <p className="text-sm text-zinc-500 mt-1">신청완료된 책의 도착 처리 + 미수령자 확인</p>
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
          {yearMonth !== currentYearMonth() && (
            <button onClick={() => setYearMonth(currentYearMonth())} className="text-xs text-amber-400 hover:underline">
              이번 달로
            </button>
          )}
        </div>

        {/* 통계 */}
        <section className="mb-6 grid grid-cols-3 gap-3">
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
            <p className="text-xs text-zinc-500">도착 대기 (ORDERED)</p>
            <p className="text-2xl font-semibold text-amber-400 mt-1">{orderedRows.length}</p>
          </div>
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
            <p className="text-xs text-zinc-500">미수령 (ARRIVED)</p>
            <p className="text-2xl font-semibold text-rose-400 mt-1">{arrivedRows.length}</p>
          </div>
          <div className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-4">
            <p className="text-xs text-zinc-500">수령 완료 (RECEIVED)</p>
            <p className="text-2xl font-semibold text-emerald-400 mt-1">{receivedRows.length}</p>
          </div>
        </section>

        {isLoading && <p className="text-sm text-zinc-500 mb-4">불러오는 중...</p>}

        {/* 액션 영역 — 도착 처리 */}
        <section className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <p className="text-sm font-medium text-zinc-200 mb-2">도착 처리</p>
          <p className="text-xs text-zinc-500 mb-3">
            오늘 도착한 책들을 체크하고 한 번에 처리하세요. 회원별 / 책별 / 전체 체크박스 가능.
          </p>
          <div className="flex flex-wrap gap-2">
            <Button onClick={handleArrive} disabled={orderedSelected.size === 0 || markArrivedMut.isPending}>
              {markArrivedMut.isPending ? '처리 중...' : `선택한 ${orderedSelected.size}권 도착 처리`}
            </Button>
            <Button
              variant="ghost"
              onClick={handleUnarrive}
              disabled={arrivedSelected.size === 0 || markUnarrivedMut.isPending}
            >
              {markUnarrivedMut.isPending ? '취소 중...' : `선택한 ${arrivedSelected.size}권 도착 처리 취소`}
            </Button>
          </div>
        </section>

        {/* 도착 대기 목록 */}
        <GroupedSection
          title="도착 대기 (도착 처리)"
          rows={orderedRows}
          selected={orderedSelected}
          setSelected={setOrderedSelected}
          emptyText="도착 대기 중인 책이 없습니다. (먼저 책 신청 관리에서 신청완료 처리를 해주세요)"
        />

        {/* 미수령 목록 */}
        <GroupedSection
          title="미수령 (회원이 아직 수령 처리 안 함)"
          rows={arrivedRows}
          selected={arrivedSelected}
          setSelected={setArrivedSelected}
          emptyText="미수령 도서가 없습니다."
          showArrivedAt
        />

        {/* 수령 완료 (접힌 상태) */}
        <details className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <summary className="cursor-pointer text-sm text-zinc-400">
            수령 완료 ({receivedRows.length}건) 보기
          </summary>
          <div className="mt-3 space-y-2">
            {receivedRows.map((r) => (
              <div key={r.id} className="flex items-center gap-3 px-2 py-1 text-xs text-zinc-500">
                {r.thumbnailUrl && <img src={r.thumbnailUrl} alt="" className="w-6 h-8 object-cover rounded border border-zinc-800" />}
                <span className="text-zinc-300">{r.memberName ?? `회원#${r.memberId}`}</span>
                <span className="flex-1 truncate">{r.title}</span>
                {r.receivedAt && (
                  <span className="text-emerald-400">
                    {new Date(r.receivedAt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit' })}
                  </span>
                )}
              </div>
            ))}
            {receivedRows.length === 0 && <p className="text-xs text-zinc-500">수령 완료된 책 없음.</p>}
          </div>
        </details>
      </main>
    </div>
  )
}
