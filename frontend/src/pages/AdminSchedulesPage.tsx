import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import AppHeader from '@/components/AppHeader'
import { Button } from '@/components/ui/button'
import {
  useClubSchedules,
  useCreateSchedule,
  useUpdateSchedule,
  useDeleteSchedule,
} from '@/hooks/useClubs'
import {
  currentYearMonth,
  dDay,
  formatKoreanDate,
  scheduleLabel,
} from '@/lib/date'
import type { ScheduleResponse } from '@/api/clubs'

const MUJE_CLUB_ID = 1

/** v1 UI 노출 typeCode 화이트리스트. PHOTO_SHOOT/MONTHLY_MEETING 은 v2 이후. */
const TYPE_OPTIONS: Array<{ code: string; label: string }> = [
  { code: 'BOOK_REQUEST_DEADLINE', label: '책 신청 마감' },
  { code: 'BOOK_REPORT_DEADLINE', label: '독후감 마감' },
]

/** 해당 월의 1일 ISO 문자열 — date input 의 default 용. */
function firstDayOf(yearMonth: string): string {
  return `${yearMonth}-01`
}

export default function AdminSchedulesPage() {
  const navigate = useNavigate()
  const [yearMonth, setYearMonth] = useState(currentYearMonth())

  const { data: schedules, isLoading, error } = useClubSchedules(MUJE_CLUB_ID, yearMonth)
  const createMut = useCreateSchedule(MUJE_CLUB_ID)
  const updateMut = useUpdateSchedule(MUJE_CLUB_ID)
  const deleteMut = useDeleteSchedule(MUJE_CLUB_ID)

  // 신규 등록 폼 state
  const [newTypeCode, setNewTypeCode] = useState(TYPE_OPTIONS[0].code)
  const [newDate, setNewDate] = useState(firstDayOf(yearMonth))
  const [newDescription, setNewDescription] = useState('')

  // 인라인 편집 state
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editDate, setEditDate] = useState('')
  const [editDescription, setEditDescription] = useState('')

  // 같은 typeCode 가 이미 있으면 시각 경고
  const existingTypeCodes = useMemo(
    () => new Set((schedules ?? []).map((s) => s.typeCode)),
    [schedules]
  )
  const newTypeAlreadyExists = existingTypeCodes.has(newTypeCode)

  const isCurrentMonth = yearMonth === currentYearMonth()

  function handleMonthChange(next: string) {
    setYearMonth(next)
    setNewDate(firstDayOf(next))
    setEditingId(null)
  }

  function handleCreate() {
    if (!newDate) {
      toast.warning('날짜를 입력해주세요.')
      return
    }
    createMut.mutate(
      {
        typeCode: newTypeCode,
        date: newDate,
        description: newDescription.trim() || null,
      },
      {
        onSuccess: () => {
          toast.success('일정을 추가했습니다.')
          setNewDescription('')
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function startEdit(s: ScheduleResponse) {
    setEditingId(s.id)
    setEditDate(s.date)
    setEditDescription(s.description ?? '')
  }

  function cancelEdit() {
    setEditingId(null)
  }

  function handleUpdate(s: ScheduleResponse) {
    if (!editDate) {
      toast.warning('날짜를 입력해주세요.')
      return
    }
    updateMut.mutate(
      {
        id: s.id,
        req: {
          typeCode: s.typeCode,
          date: editDate,
          description: editDescription.trim() || null,
        },
      },
      {
        onSuccess: () => {
          toast.success('일정을 수정했습니다.')
          setEditingId(null)
        },
        onError: (e) => toast.error(e.message),
      }
    )
  }

  function handleDelete(s: ScheduleResponse) {
    if (!confirm(`「${scheduleLabel(s.typeCode)} · ${formatKoreanDate(s.date)}」 일정을 삭제하시겠습니까?`)) {
      return
    }
    deleteMut.mutate(s.id, {
      onSuccess: () => toast.success('일정을 삭제했습니다.'),
      onError: (e) => toast.error(e.message),
    })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <AppHeader />

      <main className="max-w-3xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-semibold text-zinc-100">관리자 — 일정 관리</h2>
            <p className="text-sm text-zinc-500 mt-1">
              책 신청 마감일 / 독후감 마감일을 등록하고 수정합니다.
            </p>
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
            onChange={(e) => handleMonthChange(e.target.value)}
            className="px-3 py-1.5 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
          />
          {!isCurrentMonth && (
            <button
              onClick={() => handleMonthChange(currentYearMonth())}
              className="text-xs text-amber-400 hover:underline"
            >
              이번 달로
            </button>
          )}
        </div>

        {error && <p className="text-sm text-red-400 mb-4">{error.message}</p>}

        {/* 신규 등록 카드 */}
        <section className="mb-6 rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <p className="text-sm font-medium text-zinc-200 mb-3">신규 일정 등록</p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <div>
              <label className="block text-xs text-zinc-500 mb-1">종류</label>
              <select
                value={newTypeCode}
                onChange={(e) => setNewTypeCode(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
              >
                {TYPE_OPTIONS.map((o) => (
                  <option key={o.code} value={o.code}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-zinc-500 mb-1">날짜</label>
              <input
                type="date"
                value={newDate}
                onChange={(e) => setNewDate(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
              />
            </div>
            <div>
              <label className="block text-xs text-zinc-500 mb-1">설명 (선택)</label>
              <input
                type="text"
                value={newDescription}
                onChange={(e) => setNewDescription(e.target.value)}
                placeholder="예: 이번 달 독후감 마감"
                maxLength={200}
                className="w-full px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
              />
            </div>
          </div>
          {newTypeAlreadyExists && (
            <p className="mt-2 text-xs text-amber-400">
              ⚠ 이 달에 이미 같은 종류의 일정이 있습니다. 추가하면 가장 빠른 날짜가 마감으로 사용됩니다.
            </p>
          )}
          <div className="mt-4 flex justify-end">
            <Button onClick={handleCreate} disabled={createMut.isPending || !newDate}>
              {createMut.isPending ? '추가 중...' : '추가'}
            </Button>
          </div>
        </section>

        {/* 일정 리스트 */}
        <section className="rounded-xl border border-zinc-800/40 bg-zinc-900/50 p-5">
          <p className="text-sm font-medium text-zinc-200 mb-4">
            이 달 일정 ({schedules?.length ?? 0})
          </p>
          {isLoading && <p className="text-sm text-zinc-500">불러오는 중...</p>}
          {!isLoading && (schedules?.length ?? 0) === 0 && (
            <p className="text-sm text-zinc-500 text-center py-8">
              이 달 일정이 없습니다. 위에서 추가해주세요.
            </p>
          )}
          {(schedules ?? []).length > 0 && (
            <ul className="divide-y divide-zinc-800/40">
              {schedules!.map((s) => (
                <li key={s.id} className="py-3">
                  {editingId === s.id ? (
                    // 편집 모드
                    <div className="space-y-2">
                      <div className="flex items-center gap-2">
                        <span className="inline-block px-2 py-0.5 rounded-md text-[11px] bg-zinc-800/60 border border-zinc-700/50 text-zinc-300">
                          {scheduleLabel(s.typeCode)}
                        </span>
                        <span className="text-xs text-zinc-500">종류는 변경할 수 없습니다</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <input
                          type="date"
                          value={editDate}
                          onChange={(e) => setEditDate(e.target.value)}
                          className="px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 focus:outline-none focus:border-zinc-600"
                        />
                        <input
                          type="text"
                          value={editDescription}
                          onChange={(e) => setEditDescription(e.target.value)}
                          maxLength={200}
                          placeholder="설명 (선택)"
                          className="px-3 py-2 rounded-lg border border-zinc-800 bg-zinc-950 text-sm text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600"
                        />
                      </div>
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" onClick={cancelEdit} disabled={updateMut.isPending}>
                          취소
                        </Button>
                        <Button onClick={() => handleUpdate(s)} disabled={updateMut.isPending}>
                          {updateMut.isPending ? '저장 중...' : '저장'}
                        </Button>
                      </div>
                    </div>
                  ) : (
                    // 보기 모드
                    <div className="flex items-center gap-3">
                      <span className="inline-block px-2 py-0.5 rounded-md text-[11px] bg-zinc-800/60 border border-zinc-700/50 text-zinc-300 shrink-0">
                        {scheduleLabel(s.typeCode)}
                      </span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-zinc-200">
                          {formatKoreanDate(s.date)}
                          <span className="ml-2 text-xs text-zinc-500">{dDay(s.date)}</span>
                        </p>
                        {s.description && (
                          <p className="text-xs text-zinc-500 mt-0.5 truncate">{s.description}</p>
                        )}
                      </div>
                      <div className="flex gap-1 shrink-0">
                        <Button size="sm" variant="ghost" onClick={() => startEdit(s)}>
                          수정
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleDelete(s)}
                          disabled={deleteMut.isPending}
                        >
                          삭제
                        </Button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  )
}
