import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import { CheckboxRow } from '@/components/ui/checkbox-row'
import {
  useAdminBookRequests,
  useOrder,
  useMarkOrdered,
  useMarkLocked,
} from '@/hooks/useOrders'
import { useMyBookRequests, useLockBookRequests, useUnlockBookRequests } from '@/hooks/useBookRequests'
import type { AdminBookRequestRow } from '@/api/orders'

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

function buildAutomationScript(kCodes: string[]): string {
  const codes = kCodes.join(',')
  return `javascript:(async()=>{const codes='${codes}';const arr=codes.split(',').filter(Boolean);let ok=0;for(const c of arr){try{const r=await fetch('/shop/wbasket.aspx?AddBook='+c.trim(),{credentials:'include'});if(r.ok)ok++;}catch(e){}await new Promise(r=>setTimeout(r,300));}alert('알라딘 카트 담기 완료: '+ok+'/'+arr.length);})();`
}

export default function AdminBookRequestsPage() {
  const navigate = useNavigate()
  const { data: rows, isLoading } = useAdminBookRequests(MUJE_CLUB_ID)
  const { data: myMonth } = useMyBookRequests(MUJE_CLUB_ID)
  const { data: order } = useOrder(MUJE_CLUB_ID)
  const lockMut = useLockBookRequests(MUJE_CLUB_ID)
  const unlockMut = useUnlockBookRequests(MUJE_CLUB_ID)
  const markOrderedMut = useMarkOrdered(MUJE_CLUB_ID)
  const markLockedMut = useMarkLocked(MUJE_CLUB_ID)

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())

  const groups = useMemo(() => groupByMember(rows ?? []), [rows])
  const allIds = useMemo(() => (rows ?? []).map((r) => r.id), [rows])
  const allChecked = allIds.length > 0 && allIds.every((id) => selectedIds.has(id))

  function toggleAll() {
    setSelectedIds(allChecked ? new Set() : new Set(allIds))
  }

  function toggleGroup(group: MemberGroup) {
    const groupIds = group.rows.map((r) => r.id)
    const allSelected = groupIds.every((id) => selectedIds.has(id))
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (allSelected) groupIds.forEach((id) => next.delete(id))
      else groupIds.forEach((id) => next.add(id))
      return next
    })
  }

  function toggleOne(id: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const selectedRows = (rows ?? []).filter((r) => selectedIds.has(r.id))
  const selectedQty = selectedRows.length
  const selectedAmount = selectedRows.reduce((s, r) => s + r.price, 0)

  // 상태별 분류
  const lockedSelected = selectedRows.filter((r) => r.status === 'LOCKED')
  const orderedSelected = selectedRows.filter((r) => r.status === 'ORDERED')
  const allSelectedAreLocked = selectedRows.length > 0 && lockedSelected.length === selectedRows.length
  const allSelectedAreOrdered = selectedRows.length > 0 && orderedSelected.length === selectedRows.length

  // 카트 스크립트는 LOCKED + K-CODE 있는 것만
  const cartTargets = lockedSelected.filter((r) => !!r.aladinItemCode)
  const cartTargetKCodes = cartTargets.map((r) => r.aladinItemCode as string)
  const missingCodeCount = lockedSelected.length - cartTargets.length

  const locked = myMonth?.locked ?? false

  function handleLock() {
    if (!confirm('이번 달 신청을 잠그시겠습니까? PENDING → LOCKED.')) return
    lockMut.mutate(undefined, {
      onSuccess: (r) => toast.success(`잠금 완료 (${r.affected}건)`),
      onError: (e) => toast.error(e.message),
    })
  }

  function handleUnlock() {
    if (!confirm('잠금을 해제하시겠습니까?\n* LOCKED 책만 PENDING으로 되돌아가며, ORDERED 책은 변경되지 않습니다.')) return
    unlockMut.mutate(undefined, {
      onSuccess: (r) => toast.success(`해제 완료 (${r.affected}건)`),
      onError: (e) => toast.error(e.message),
    })
  }

  async function handleCopyScript() {
    if (cartTargetKCodes.length === 0) {
      toast.warning('카트에 담을 책이 없습니다 (LOCKED + K-CODE 필요).')
      return
    }
    const script = buildAutomationScript(cartTargetKCodes)
    try {
      await navigator.clipboard.writeText(script)
      window.open('https://www.aladin.co.kr/', '_blank')
      toast.success(
        '스크립트 복사 + 알라딘 새 탭. 알라딘 탭에서 F12 → Console에 붙여넣기 후 Enter.',
        { duration: 8000 }
      )
    } catch {
      toast.error('클립보드 복사 실패')
    }
  }

  function handleMarkOrdered() {
    if (lockedSelected.length === 0) return
    if (!confirm(`선택한 ${lockedSelected.length}권을 신청완료로 처리하시겠습니까?`)) return
    markOrderedMut.mutate(
      lockedSelected.map((r) => r.id),
      {
        onSuccess: (o) => {
          toast.success(`신청완료 처리 (${lockedSelected.length}권). 누적: ${o.totalQuantity}권`)
          setSelectedIds(new Set())
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function handleMarkLocked() {
    if (orderedSelected.length === 0) return
    if (!confirm(
      `⚠ 신청완료를 취소하시겠습니까?\n\n` +
      `이미 알라딘에서 결제했다면 취소하지 마세요.\n` +
      `선택한 ${orderedSelected.length}권이 LOCKED 상태로 되돌아갑니다.`
    )) return
    markLockedMut.mutate(
      orderedSelected.map((r) => r.id),
      {
        onSuccess: () => {
          toast.success(`신청완료 취소 (${orderedSelected.length}권)`)
          setSelectedIds(new Set())
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 책 신청 관리</h2>
            <p className="text-sm text-zinc-500 mt-1">전체 신청 / 잠금 / 카트 담기 / 신청완료 처리</p>
          </div>
          <Button variant="ghost" onClick={() => navigate('/muje')}>
            ← 대시보드
          </Button>
        </div>

        {/* 잠금 영역 */}
        <section className="mb-6 rounded-xl border border-amber-900/40 bg-amber-950/20 p-4 flex items-center justify-between">
          <div>
            <p className="text-xs text-amber-400 mb-1">신청 마감 잠금</p>
            <p className="text-sm text-zinc-300">
              현재: <span className={locked ? 'text-amber-400' : 'text-emerald-400'}>{locked ? '🔒 잠금' : '🟢 신청 가능'}</span>
            </p>
          </div>
          {locked ? (
            <Button onClick={handleUnlock} disabled={unlockMut.isPending} variant="ghost">
              {unlockMut.isPending ? '해제 중...' : '잠금 해제'}
            </Button>
          ) : (
            <Button onClick={handleLock} disabled={lockMut.isPending}>
              {lockMut.isPending ? '잠그는 중...' : '신청 마감 잠금'}
            </Button>
          )}
        </section>

        {/* 합산 주문서 (자동 누적) */}
        <section className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <p className="text-sm font-medium text-zinc-200 mb-3">합산 주문서 (자동 누적)</p>
          {!order && (
            <p className="text-xs text-zinc-500">
              아직 신청완료 처리된 책이 없습니다. 카트 담기 후 "선택한 N권 신청완료 처리"를 누르면 자동으로 누적됩니다.
            </p>
          )}
          {order && (
            <div>
              <div className="grid grid-cols-3 gap-3 mb-3">
                <div className="rounded-lg bg-zinc-950/60 p-3">
                  <p className="text-xs text-zinc-500">총 종</p>
                  <p className="text-lg font-semibold">{order.items.length}</p>
                </div>
                <div className="rounded-lg bg-zinc-950/60 p-3">
                  <p className="text-xs text-zinc-500">총 권수</p>
                  <p className="text-lg font-semibold">{order.totalQuantity}</p>
                </div>
                <div className="rounded-lg bg-zinc-950/60 p-3">
                  <p className="text-xs text-zinc-500">합계 금액</p>
                  <p className="text-lg font-semibold">₩{order.totalAmount.toLocaleString()}</p>
                </div>
              </div>
              <details className="text-xs">
                <summary className="cursor-pointer text-zinc-400">합산 항목 ({order.items.length})</summary>
                <ul className="mt-2 space-y-1">
                  {order.items.map((it) => (
                    <li key={it.id} className="flex justify-between text-zinc-400">
                      <span>{it.title} × {it.quantity}</span>
                      <span>₩{it.subtotal.toLocaleString()}</span>
                    </li>
                  ))}
                </ul>
              </details>
            </div>
          )}
        </section>

        {/* 카트 담기 + 신청완료 처리 패널 (잠금 후에만 활성) */}
        <section className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <div className="flex items-center justify-between mb-3">
            <div>
              <p className="text-sm font-medium text-zinc-200">카트 담기 / 신청완료 처리</p>
              <p className="text-xs text-zinc-500 mt-1">
                선택: {selectedQty}권 · ₩{selectedAmount.toLocaleString()}
                {missingCodeCount > 0 && allSelectedAreLocked && (
                  <span className="ml-2 text-amber-400">
                    (K-CODE 없는 {missingCodeCount}권 카트 제외)
                  </span>
                )}
              </p>
            </div>
          </div>

          {!locked ? (
            <p className="text-sm text-zinc-500 py-4 text-center">
              📌 신청 마감 잠금 후에 사용 가능합니다.
            </p>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
                <Button onClick={handleCopyScript} disabled={cartTargetKCodes.length === 0}>
                  자동 카트 담기 스크립트 복사 ({cartTargetKCodes.length}권)
                </Button>
                <Button
                  onClick={handleMarkOrdered}
                  disabled={!allSelectedAreLocked || markOrderedMut.isPending}
                >
                  {markOrderedMut.isPending ? '처리 중...' : `선택한 ${lockedSelected.length}권 신청완료 처리`}
                </Button>
                <Button
                  variant="ghost"
                  onClick={handleMarkLocked}
                  disabled={!allSelectedAreOrdered || markLockedMut.isPending}
                >
                  {markLockedMut.isPending ? '취소 중...' : `선택한 ${orderedSelected.length}권 신청완료 취소`}
                </Button>
              </div>
              <ol className="text-xs text-zinc-600 mt-3 leading-relaxed list-decimal list-inside space-y-1">
                <li>카트에 담을 책들을 체크 → "스크립트 복사" → 알라딘 탭 F12 콘솔에 붙여넣기 → 카트 자동 담기.</li>
                <li>알라딘에서 결제 진행 후 모모로 돌아옴.</li>
                <li>방금 담은 책들을 다시 체크 → "신청완료 처리" → 회색으로 비활성화 + 합산 주문서 누적.</li>
                <li>실수했다면 ORDERED 행을 체크 → "신청완료 취소"로 LOCKED 상태로 되돌릴 수 있습니다.</li>
              </ol>
            </>
          )}
        </section>

        {/* 신청 목록 — 회원별 그룹 */}
        <section className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-medium text-zinc-200">전체 신청 ({rows?.length ?? 0}건)</p>
            <CheckboxRow checked={allChecked} onChange={() => toggleAll()} className="px-3 py-2 text-xs text-zinc-400">
              전체 선택
            </CheckboxRow>
          </div>

          {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
          {!isLoading && groups.length === 0 && <p className="text-sm text-zinc-500">신청이 없습니다.</p>}

          <div className="space-y-4">
            {groups.map((g) => {
              const groupAmount = g.rows.reduce((s, r) => s + r.price, 0)
              const groupChecked = g.rows.every((r) => selectedIds.has(r.id))
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
                    {g.rows.map((r) => {
                      const isOrdered = r.status === 'ORDERED'
                      return (
                        <li
                          key={r.id}
                          className={`border-t border-zinc-800/30 ${
                            isOrdered ? 'bg-emerald-950/20 opacity-70' : 'hover:bg-zinc-950/30'
                          }`}
                        >
                          <CheckboxRow
                            checked={selectedIds.has(r.id)}
                            onChange={() => toggleOne(r.id)}
                          >
                            {r.thumbnailUrl && (
                              <img src={r.thumbnailUrl} alt={r.title} className="w-8 h-10 object-cover rounded border border-zinc-800" />
                            )}
                            <div className="flex-1 min-w-0">
                              <p className={`text-sm truncate ${isOrdered ? 'text-zinc-400' : 'text-zinc-200'}`}>
                                {r.title}
                                {isOrdered && (
                                  <span className="ml-2 text-xs text-emerald-400">✓ 신청완료</span>
                                )}
                              </p>
                              <p className="text-xs text-zinc-500 truncate">
                                {r.author} · {r.categoryLabel} · {r.statusLabel}
                                {!r.aladinItemCode && r.status === 'LOCKED' && (
                                  <span className="text-amber-400 ml-1">· (K-CODE 없음)</span>
                                )}
                              </p>
                            </div>
                            <p className="text-sm text-zinc-300 whitespace-nowrap">₩{r.price.toLocaleString()}</p>
                          </CheckboxRow>
                        </li>
                      )
                    })}
                  </ul>
                </div>
              )
            })}
          </div>
        </section>
      </main>
    </div>
  )
}
