# 13 — 예산 모듈 (월별 한도 + 회원간 나눔)

> 작성일: 2026-04-24
> 상태: v1 구현 완료

---

## 1. 왜 만들었나 (Problem)

- 무제는 회사 지원금으로 매달 회원 1인당 **정해진 금액까지** 책을 살 수 있다.
- 기존에는 `BudgetCalculator` 유틸이 신청마다 즉석 합산만 해주고, **월별 한도가 DB 에 스냅샷** 으로 남지 않아 관리자가 월중에 한도를 올려줄 방법이 없었다.
- 또한, 책을 많이 안 사는 달엔 남는 예산을 **동료에게 넘기고 싶다** 는 요구가 있다 (아이 돌봄·출장 등으로 한 달 스킵).

## 2. 목표 (Goal)

- 월 단위 개인 한도(`MemberMonthlyBudget`)를 DB 로 관리하고 관리자가 조정.
- 같은 클럽 회원끼리 **예산 나눔(BudgetShare)** — 요청 → 수락/거절 흐름.
- 책 신청 시 `effectiveLimit = base + transferIn - transferOut` 기준으로 검증.

## 3. 정책 / 규칙 (Rules)

- **기본 한도**: `BudgetPolicy.defaultLimit(member, yearMonth)` 가 결정 (`DefaultBudgetPolicy` 는 정책 한 줄 고정, 추후 연차·직급 등으로 확장 가능).
- **스냅샷 시점**: 해당 월 최초 신청/조회 시 `MemberMonthlyBudget` upsert.
- **관리자 조정(Adjust)**:
  - `PATCH /admin/clubs/{clubId}/budgets/{memberId}` 로 base 덮어쓰기.
  - 이미 사용된 금액 아래로는 내릴 수 없음(400).
- **나눔 정책**:
  - 같은 `(club, yearMonth)` 안에서만 가능.
  - `from → to, amount` 로 생성, 상태 `PENDING / ACCEPTED / REJECTED / CANCELED`.
  - ACCEPTED 시 즉시 `from.effectiveLimit -= amount`, `to.effectiveLimit += amount`.
  - 한 번 ACCEPTED 된 건 취소 불가(REJECTED/CANCELED 는 PENDING 에만).
  - `from` 의 남은 잔액 이상으로는 보낼 수 없음.
- **이월 금지**: 월이 바뀌면 새 스냅샷. 전월 잔액 자동 이월 없음 (과소비 방지).

## 4. UX 흐름

### 4-1. 회원 — 책 신청 페이지 (`BookRequestPage`)
- 상단 예산 배너: `12,000 / 30,000 사용`. 초과 시 [책 신청] 버튼 비활성 + 안내.
- 배너 옆 [예산 나눔] 버튼 → `BudgetShareDialog` 모달:
  - 후보(같은 클럽, 같은 달) 중 선택, 금액 입력 → [요청 보내기].

### 4-2. 회원 — 무제 대시보드 (`MujePage`)
- "받은 나눔 요청" 섹션에 PENDING 뱃지. [수락] / [거절] 버튼.
- "보낸 요청" 섹션: 상태 뱃지 + [취소] (PENDING 만).
- "수락된 이력" 섹션: 이번 달 누가 몇 원을 주고 받았는지 요약.

### 4-3. 관리자 — 예산 관리 타일
- 회원별 `base / used / effective` 테이블.
- [조정] 인풋으로 base 수정.

## 5. API 표면

### 관리자
- `GET   /api/v1/admin/clubs/{clubId}/budgets/summary` — 회원별 예산 요약 (+ YYYY-MM 파라미터).
- `PATCH /api/v1/admin/clubs/{clubId}/budgets/{memberId}` — base 조정.

### 회원 — 나눔
- `GET  /api/v1/clubs/{clubId}/budget-shares/candidates` — 이번 달 나눔 가능한 동료.
- `GET  /api/v1/clubs/{clubId}/budget-shares/incoming` — 받은 요청.
- `GET  /api/v1/clubs/{clubId}/budget-shares/outgoing` — 보낸 요청.
- `GET  /api/v1/clubs/{clubId}/budget-shares/accepted` — 이번 달 수락 이력.
- `POST /api/v1/clubs/{clubId}/budget-shares` — 생성.
- `POST /api/v1/clubs/{clubId}/budget-shares/{id}/accept`
- `POST /api/v1/clubs/{clubId}/budget-shares/{id}/reject`
- `POST /api/v1/clubs/{clubId}/budget-shares/{id}/cancel`

## 6. 수용 기준 (Acceptance)

- [x] 신청이 effectiveLimit 을 넘으면 400 + 사유 메시지.
- [x] 관리자 base 조정이 effective 에 즉시 반영.
- [x] 나눔 수락 시 양측 effective 가 원자적으로 변경.
- [x] 같은 PENDING 을 양쪽에서 수락/거절해도 하나만 성공 (동시성 방어).
- [x] 월이 바뀌면 새 스냅샷, 이월 없음.
- [ ] 월 마감(MonthLock, 14 문서) 이후 나눔 생성 금지 — 확인 필요.

## 7. 오픈 이슈 / 향후 계획

- **정책 확장**: 연차/직급별 기본 한도, 특별 프로모션 월 한도 등을 `BudgetPolicy` 구현체로.
- **나눔 한도**: 1인당 보낼 수 있는 상한 정책 필요한지 검토.
- **알림 연계**: 나눔 요청/수락 시 NotificationService 트리거 추가.
- **예산 소진률 대시보드**: 관리자용 차트.
