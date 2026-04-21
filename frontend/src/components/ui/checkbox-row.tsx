import { cn } from '@/lib/utils'
import type { ReactNode } from 'react'

/**
 * 모바일/데스크탑 친화 체크박스 행 컴포넌트.
 *
 * 컨벤션 (docs/frontend/02-ui-conventions.md):
 * - <label>로 전체 행 래핑 → 클릭/터치 영역이 행 전체
 * - 최소 터치 타깃 44x44px (iOS HIG)
 * - 체크박스 크기 w-5 h-5
 * - cursor-pointer
 *
 * 직접 <input type="checkbox">를 두지 말고 이 컴포넌트를 사용하세요.
 */
interface CheckboxRowProps {
  checked: boolean
  onChange: (checked: boolean) => void
  disabled?: boolean
  className?: string
  children: ReactNode
}

export function CheckboxRow({ checked, onChange, disabled, className, children }: CheckboxRowProps) {
  return (
    <label
      className={cn(
        'flex items-center gap-3 px-4 py-3 min-h-[44px]',
        disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
        className
      )}
    >
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
        className="w-5 h-5 accent-amber-500 shrink-0"
      />
      {children}
    </label>
  )
}
