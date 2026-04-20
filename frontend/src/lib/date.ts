export function currentYearMonth(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export function formatKoreanDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

export function dDay(iso: string): string {
  const target = new Date(iso)
  target.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diff = Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (diff === 0) return 'D-Day'
  if (diff > 0) return `D-${diff}`
  return `D+${Math.abs(diff)}`
}

export const SCHEDULE_TYPE_LABELS: Record<string, string> = {
  BOOK_REQUEST_DEADLINE: '책 신청 마감',
  BOOK_REPORT_DEADLINE: '독후감 마감',
  PHOTO_SHOOT: '촬영일',
  MONTHLY_MEETING: '월례 미팅',
}

export function scheduleLabel(typeCode: string): string {
  return SCHEDULE_TYPE_LABELS[typeCode] ?? typeCode
}
