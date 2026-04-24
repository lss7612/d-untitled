import { useMemo, useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { useShareCandidates, useCreateShare } from '@/hooks/useBudgetShares'
import type { ShareCandidate } from '@/api/budgetShares'

interface Props {
  open: boolean
  clubId: number
  yearMonth: string
  onClose: () => void
}

/**
 * "나눔 받기" 모달.
 * 본인 제외 클럽 ACTIVE 멤버 리스트 + 각자 remaining 이 보이고,
 * 이름 검색 → 한 명 선택 → 금액/메모 입력 → 제출로 PENDING BudgetShare 를 만든다.
 */
export default function BudgetShareDialog({ open, clubId, yearMonth, onClose }: Props) {
  const { data: candidates, isLoading } = useShareCandidates(clubId, yearMonth, open)
  const createMut = useCreateShare(clubId)

  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<ShareCandidate | null>(null)
  const [amount, setAmount] = useState<string>('')
  const [note, setNote] = useState('')

  const filtered = useMemo(() => {
    const list = candidates ?? []
    const q = query.trim().toLowerCase()
    if (!q) return list
    return list.filter(
      (c) => c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q),
    )
  }, [candidates, query])

  function close() {
    setQuery('')
    setSelected(null)
    setAmount('')
    setNote('')
    onClose()
  }

  function submit() {
    if (!selected) return toast.warning('나눔을 받을 대상을 선택해주세요.')
    const amt = Number(amount)
    if (!Number.isFinite(amt) || amt <= 0) return toast.warning('금액은 양수여야 합니다.')
    if (Math.floor(amt) !== amt) return toast.warning('원 단위 정수로 입력해주세요.')

    createMut.mutate(
      {
        targetMonth: yearMonth,
        senderId: selected.memberId,
        amount: amt,
        note: note.trim() || null,
      },
      {
        onSuccess: () => {
          toast.success(`${selected.name}님에게 나눔 신청을 보냈습니다.`)
          close()
        },
        onError: (e) => toast.error(e.message),
      },
    )
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      onClick={close}
    >
      <div
        className="w-full max-w-md max-h-[90vh] overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950 flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 border-b border-zinc-800/60">
          <p className="text-base font-semibold text-zinc-100">예산 나눔 받기</p>
          <p className="mt-1 text-xs text-zinc-500">
            다른 멤버에게 금액을 요청합니다. 상대가 수락하면 이번 달 한도가 늘어납니다.
          </p>
        </div>

        <div className="flex-1 overflow-auto px-5 py-4 space-y-4">
          {/* 이름 검색 */}
          <div>
            <label className="block text-xs text-zinc-500 mb-1.5">받을 대상</label>
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="이름 또는 이메일로 검색"
              className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-900 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
            />
          </div>

          {/* 후보 리스트 */}
          <div className="border border-zinc-800/60 rounded-lg max-h-64 overflow-auto">
            {isLoading ? (
              <p className="px-3 py-6 text-center text-xs text-zinc-500">불러오는 중...</p>
            ) : filtered.length === 0 ? (
              <p className="px-3 py-6 text-center text-xs text-zinc-500">조건에 맞는 멤버가 없습니다.</p>
            ) : (
              <ul>
                {filtered.map((c) => {
                  const isSelected = selected?.memberId === c.memberId
                  return (
                    <li key={c.memberId}>
                      <button
                        type="button"
                        onClick={() => setSelected(c)}
                        className={[
                          'w-full text-left px-3 py-2 flex items-center justify-between gap-3 border-b border-zinc-800/40 last:border-b-0 transition',
                          isSelected
                            ? 'bg-sky-950/40 text-zinc-100'
                            : 'hover:bg-zinc-900 text-zinc-300',
                        ].join(' ')}
                      >
                        <div className="min-w-0">
                          <p className="text-sm truncate">{c.name}</p>
                          <p className="text-[11px] text-zinc-500 truncate">{c.email}</p>
                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-[11px] text-zinc-500">잔여</p>
                          <p
                            className={[
                              'text-xs',
                              c.remaining <= 0 ? 'text-zinc-600' : 'text-zinc-200',
                            ].join(' ')}
                          >
                            {c.remaining.toLocaleString()}원
                          </p>
                        </div>
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          {/* 금액 + 메모 */}
          <div className="grid grid-cols-1 gap-3">
            <div>
              <label className="block text-xs text-zinc-500 mb-1.5">금액 (원)</label>
              <input
                type="number"
                min={1}
                step={1000}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="예: 5000"
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-900 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
              />
              {selected && Number(amount) > selected.remaining && (
                <p className="mt-1 text-[11px] text-amber-400">
                  상대 잔여({selected.remaining.toLocaleString()}원)보다 큰 금액입니다. 수락 시점에 거부될 수 있어요.
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs text-zinc-500 mb-1.5">메모 (선택)</label>
              <input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                maxLength={200}
                placeholder="한 줄 사유 (예: 이번 달 번역본 한 권 추가하고 싶어요)"
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-900 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
              />
            </div>
          </div>
        </div>

        <div className="px-5 py-4 border-t border-zinc-800/60 flex justify-end gap-2">
          <Button variant="ghost" onClick={close}>
            취소
          </Button>
          <Button onClick={submit} disabled={createMut.isPending || !selected || !amount}>
            {createMut.isPending ? '보내는 중...' : '나눔 신청'}
          </Button>
        </div>
      </div>
    </div>
  )
}
