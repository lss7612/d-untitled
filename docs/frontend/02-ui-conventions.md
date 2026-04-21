# UI / UX 컨벤션

> 이 문서는 프론트 전반에 적용되는 UI/UX 컨벤션의 단일 진실 공급원이다.
> 새 화면 / 컴포넌트 만들 때 먼저 확인할 것.
> 새 컨벤션이 정해지면 여기에 추가한다.

---

## 1. 체크박스 행 (Checkbox Row)

### 규칙

- **항상 [`<CheckboxRow>`](../../frontend/src/components/ui/checkbox-row.tsx) 컴포넌트 사용.**
  - 직접 `<input type="checkbox">`를 두지 않는다.
  - 부득이한 경우(특수 레이아웃)에도 아래 패턴을 동일하게 적용:
    - `<label>`로 전체 행 래핑 → 클릭/터치 영역 = 행 전체
    - 최소 터치 타깃 **44 × 44 px** (`min-h-[44px]`) — iOS HIG 권장
    - 입력 자체 크기 **`w-5 h-5`** (네이티브 기본보다 큼)
    - `cursor-pointer` (또는 disabled 시 `cursor-not-allowed`)
- 모바일/데스크탑 동일 패턴. 별도 미디어 쿼리 분기 X.

### 왜?

- 모바일에서 손가락으로 작은 체크박스만 정확히 누르기 어려움.
- 다중 선택 화면(관리자 책 신청 관리 등)은 모바일에서 빠르게 처리하는 패턴이 흔함.
- `<label>` 래핑은 네이티브 동작이라 추가 JS 없이 클릭 영역 확장됨 + 키보드 접근성 유지.

### 사용 예

```tsx
import { CheckboxRow } from '@/components/ui/checkbox-row'

<CheckboxRow
  checked={selected.has(id)}
  onChange={(c) => toggle(id, c)}
>
  <img src={thumbnailUrl} ... />
  <span className="flex-1">{title}</span>
  <span className="text-zinc-400">{price}</span>
</CheckboxRow>
```

### 회귀 검증 시나리오

- 행의 **체크박스가 아닌 영역**(텍스트 / 이미지 / 빈 공간)을 클릭/터치 → 토글 동작
- Chrome DevTools 모바일 시뮬레이터에서 손가락 크기로 터치 가능
- 키보드 Tab → input 포커스 → Space로 토글

---

## 2. (향후 추가)

- 폼 입력 (input/textarea/select)
- 버튼 사이즈/계층 (primary / ghost / link)
- 모달/다이얼로그
- 토스트
- 카드/리스트 패딩 표준
- 색상 토큰 (현재 zinc + amber 위주)

새 패턴이 두 군데 이상에서 반복되면 컨벤션으로 승격해 여기에 정착시킨다.
