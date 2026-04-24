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
  useUnmarkOrdered,
  useUnsubmittedMembers,
} from '@/hooks/useOrders'
import { useMyBookRequests, useLockBookRequests, useUnlockBookRequests } from '@/hooks/useBookRequests'
import { useBudgetSummary } from '@/hooks/useBudgets'
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

/**
 * 카트 담기 대상 하나. 같은 책이 N권 선택된 경우 count=N 으로 집계되어 들어온다.
 * kCode 는 AddBook 용, isbn 은 UpdateQty 용 (알라딘 내부 API 가 ISBN 기준 수량 조정).
 */
interface CartItem {
  kCode: string
  isbn: string
  count: number
}

/**
 * 알라딘 장바구니 자동화 스크립트.
 *
 * 알고리즘: 각 유니크 (K-code, qty) 마다 알라딘 실제 담기 엔드포인트 한 번 호출.
 *   `GET /shop/BasketAjax.aspx?method=basketaddwithexistcheck&isbn=<K-code>&qty=<N>&_=<ts>`
 *
 * - 파라미터명은 `isbn` 이지만 실제 값은 **K-code** (알라딘 내부 컨벤션).
 * - `qty` 로 한 번에 N권 담기. 기존 `AddBook` + `UpdateQty` 2단계는 K-code/ISBN 키 불일치로 실제 수량
 *   반영이 안 되는 케이스가 있어 폐기.
 * - 응답은 `{"Success":"true","Exist":"false","BranchType":..,"Title":"..","Price":"..."}` 형식.
 *   실패면 `Success:"false"`. 이미 카트에 있는 동일 상품이면 `Exist:"true"`.
 */
function buildAutomationScript(items: CartItem[]): string {
  // 스크립트 안으로 데이터 직렬화. BACKSLASH/QUOTE 이스케이프는 JSON.stringify 에 위임.
  const json = JSON.stringify(items)
  return `javascript:(async()=>{
    const items=${json};
    let ok=0,already=0,fail=0;const failLog=[];
    for(const it of items){
      const ts=Date.now();
      const url='/shop/BasketAjax.aspx?method=basketaddwithexistcheck&isbn='+encodeURIComponent(it.kCode)+'&qty='+it.count+'&_='+ts;
      try{
        const r=await fetch(url,{credentials:'include',headers:{'Accept':'application/json, text/javascript, */*; q=0.01','X-Requested-With':'XMLHttpRequest'}});
        const j=await r.json();
        if(j.Success==='true'){
          if(j.Exist==='true')already++;else ok++;
        }else{
          fail++;failLog.push(it.kCode+': '+(j.AlertMessage||'Success=false'));
        }
      }catch(e){fail++;failLog.push(it.kCode+': '+e);}
      await new Promise(r=>setTimeout(r,300));
    }
    const total=items.reduce((s,x)=>s+x.count,0);
    let msg='알라딘 카트 담기 완료\\n신규: '+ok+'종 · 이미있음: '+already+'종 · 실패: '+fail+'종\\n총 '+total+'권 요청 ('+items.length+'종)';
    if(failLog.length)msg+='\\n\\n실패:\\n'+failLog.join('\\n');
    alert(msg);
  })();`.replace(/\n\s+/g, '')
}

/** 선택된 PENDING 행을 ISBN 기준으로 집계해 CartItem[] 로 변환. K-code 없거나 ISBN 없으면 제외. */
function aggregateCartItems(rows: AdminBookRequestRow[]): CartItem[] {
  const map = new Map<string, CartItem>()
  for (const r of rows) {
    if (!r.aladinItemCode || !r.isbn) continue
    const key = r.isbn
    const existing = map.get(key)
    if (existing) {
      existing.count += 1
    } else {
      map.set(key, { kCode: r.aladinItemCode, isbn: r.isbn, count: 1 })
    }
  }
  return Array.from(map.values())
}

export default function AdminBookRequestsPage() {
  const navigate = useNavigate()
  const { data: rows, isLoading } = useAdminBookRequests(MUJE_CLUB_ID)
  const { data: myMonth } = useMyBookRequests(MUJE_CLUB_ID)
  const { data: order } = useOrder(MUJE_CLUB_ID)
  const { data: budgetSummary } = useBudgetSummary(MUJE_CLUB_ID)
  const { data: unsubmittedData } = useUnsubmittedMembers(MUJE_CLUB_ID)
  const lockMut = useLockBookRequests(MUJE_CLUB_ID)
  const unlockMut = useUnlockBookRequests(MUJE_CLUB_ID)
  const markOrderedMut = useMarkOrdered(MUJE_CLUB_ID)
  const unmarkOrderedMut = useUnmarkOrdered(MUJE_CLUB_ID)

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
  const pendingSelected = selectedRows.filter((r) => r.status === 'PENDING')
  const orderedSelected = selectedRows.filter((r) => r.status === 'ORDERED')
  const allSelectedArePending = selectedRows.length > 0 && pendingSelected.length === selectedRows.length
  const allSelectedAreOrdered = selectedRows.length > 0 && orderedSelected.length === selectedRows.length

  // 카트 스크립트는 PENDING + K-CODE 있는 것만. 같은 책은 ISBN 기준 집계해 count=N 으로.
  const cartItems = useMemo(() => aggregateCartItems(pendingSelected), [pendingSelected])
  const cartItemTotalQty = cartItems.reduce((s, x) => s + x.count, 0)
  const missingCodeCount = pendingSelected.length - cartItemTotalQty

  const locked = myMonth?.locked ?? false

  function handleLock() {
    if (!confirm('이번 달 신청을 잠그시겠습니까? 회원이 추가 신청할 수 없게 됩니다.')) return
    lockMut.mutate(undefined, {
      onSuccess: () => toast.success('잠금 완료'),
      onError: (e) => toast.error(e.message),
    })
  }

  function handleUnlock() {
    if (!confirm('잠금을 해제하시겠습니까? 회원이 다시 추가 신청할 수 있게 됩니다. 이미 주문 처리된 책에는 영향 없습니다.')) return
    unlockMut.mutate(undefined, {
      onSuccess: () => toast.success('해제 완료'),
      onError: (e) => toast.error(e.message),
    })
  }

  async function handleCopyScript() {
    if (cartItems.length === 0) {
      toast.warning('카트에 담을 책이 없습니다 (신청 대기 + K-CODE + ISBN 필요).')
      return
    }
    const script = buildAutomationScript(cartItems)
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
    if (pendingSelected.length === 0) return
    if (!confirm(`선택한 ${pendingSelected.length}권을 신청완료로 처리하시겠습니까?`)) return
    markOrderedMut.mutate(
      pendingSelected.map((r) => r.id),
      {
        onSuccess: (o) => {
          toast.success(`신청완료 처리 (${pendingSelected.length}권). 누적: ${o.totalQuantity}권`)
          setSelectedIds(new Set())
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function handleUnmarkOrdered() {
    if (orderedSelected.length === 0) return
    if (!confirm(
      `⚠ 신청완료를 취소하시겠습니까?\n\n` +
      `이미 알라딘에서 결제했다면 취소하지 마세요.\n` +
      `선택한 ${orderedSelected.length}권이 신청 대기(PENDING) 상태로 되돌아갑니다.`
    )) return
    unmarkOrderedMut.mutate(
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

        {/* 예산 사용 현황 */}
        <section className="mb-6 rounded-xl border border-amber-900/40 bg-amber-950/20 p-5">
          <p className="text-sm font-medium text-amber-200 mb-3">예산 사용 현황</p>
          {!budgetSummary ? (
            <p className="text-xs text-zinc-500">불러오는 중...</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div className="rounded-lg bg-zinc-950/60 p-3">
                <p className="text-xs text-amber-400/80">신청된 예산</p>
                <p className="text-lg font-semibold text-zinc-100">
                  ₩{budgetSummary.totalRequestedAmount.toLocaleString()}
                </p>
              </div>
              <div className="rounded-lg bg-zinc-950/60 p-3">
                <p className="text-xs text-amber-400/80">전체 예산</p>
                <p className="text-lg font-semibold text-zinc-100">
                  ₩{budgetSummary.totalBaseLimit.toLocaleString()}
                </p>
              </div>
              <div className="rounded-lg bg-zinc-950/60 p-3">
                <p className="text-xs text-amber-400/80">사용률</p>
                <p
                  className={`text-lg font-semibold ${
                    budgetSummary.usagePercent >= 80 ? 'text-orange-400' : 'text-amber-300'
                  }`}
                >
                  {budgetSummary.usagePercent.toFixed(1)}%
                </p>
                <div className="mt-2 h-1.5 w-full bg-zinc-800/60 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${
                      budgetSummary.usagePercent >= 80 ? 'bg-orange-500' : 'bg-amber-400'
                    }`}
                    style={{ width: `${Math.min(100, budgetSummary.usagePercent)}%` }}
                  />
                </div>
              </div>
            </div>
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
                {missingCodeCount > 0 && allSelectedArePending && (
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
                <Button onClick={handleCopyScript} disabled={cartItems.length === 0}>
                  자동 카트 담기 스크립트 복사 ({cartItems.length}종 · 총 {cartItemTotalQty}권)
                </Button>
                <Button
                  onClick={handleMarkOrdered}
                  disabled={!allSelectedArePending || markOrderedMut.isPending}
                >
                  {markOrderedMut.isPending ? '처리 중...' : `선택한 ${pendingSelected.length}권 신청완료 처리`}
                </Button>
                <Button
                  variant="ghost"
                  onClick={handleUnmarkOrdered}
                  disabled={!allSelectedAreOrdered || unmarkOrderedMut.isPending}
                >
                  {unmarkOrderedMut.isPending ? '취소 중...' : `선택한 ${orderedSelected.length}권 신청완료 취소`}
                </Button>
              </div>
              <ol className="text-xs text-zinc-600 mt-3 leading-relaxed list-decimal list-inside space-y-1">
                <li>카트에 담을 책들을 체크 → "스크립트 복사" → 알라딘 탭 F12 콘솔에 붙여넣기 → 카트 자동 담기.</li>
                <li>알라딘에서 결제 진행 후 모모로 돌아옴.</li>
                <li>방금 담은 책들을 다시 체크 → "신청완료 처리" → 회색으로 비활성화 + 합산 주문서 누적.</li>
                <li>실수했다면 ORDERED 행을 체크 → "신청완료 취소"로 PENDING 상태로 되돌릴 수 있습니다.</li>
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

          {/* 미신청 회원 */}
          {unsubmittedData && (
            <div className="mb-4 rounded-lg border border-amber-900/30 bg-amber-950/10 p-3">
              {unsubmittedData.unsubmitted.length === 0 ? (
                <p className="text-sm text-emerald-300">
                  🎉 모든 회원이 신청 완료 ({unsubmittedData.submittedCount}/{unsubmittedData.totalActiveMembers})
                </p>
              ) : (
                <>
                  <p className="text-xs text-amber-400 mb-2">
                    미신청 회원 {unsubmittedData.unsubmitted.length}명
                    <span className="text-zinc-500 ml-1">
                      · 제출 {unsubmittedData.submittedCount}/{unsubmittedData.totalActiveMembers}
                    </span>
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {unsubmittedData.unsubmitted.map((m) => (
                      <span
                        key={m.memberId}
                        className="inline-flex items-center gap-1.5 rounded-full bg-zinc-950/60 border border-amber-900/30 px-2.5 py-1 text-xs"
                      >
                        <span className="text-zinc-200">{m.name}</span>
                        <span className="text-zinc-500">·</span>
                        <span className="text-zinc-500">{m.email}</span>
                      </span>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}

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
                                {!r.aladinItemCode && r.status === 'PENDING' && (
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
