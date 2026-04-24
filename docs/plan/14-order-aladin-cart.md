# 14 — 마감 잠금 → 합산 주문서 → 알라딘 카트 → 도착/수령

> 작성일: 2026-04-24
> 상태: v1 구현 완료 (카트 링크 수동 결제)

---

## 1. 왜 만들었나 (Problem)

- 무제 운영은 월말에 관리자가 구글시트를 복사·정리해 **한 번에 한 카트로 발주** 한다.
- 이를 시스템화할 때 중요한 제약:
  1. 신청이 계속 들어오면 합산이 흔들린다 → **마감 잠금** 이 필요.
  2. 알라딘 결제는 우리가 자동화할 수 없음 → **카트 URL 생성** 까지만.
  3. 배송이 분할 도착할 수 있음 → 책 단위 도착/수령 추적.

## 2. 목표 (Goal)

- 관리자가 특정 (clubId, YYYY-MM) 에 대해 **신청 마감 잠금(MonthLock)** 을 걸 수 있다.
- 잠금 이후 신청은 생성·수정 불가.
- APPROVED 상태 건을 합산한 **알라딘 카트 URL** 을 관리자 화면에서 한 번에 얻는다.
- 주문 처리 후 도착 → 수령 단계를 **일괄 / 개별** 처리.

## 3. 정책 / 규칙 (Rules)

### 3-1. BookRequest 상태머신
`PENDING → APPROVED → ORDERED → ARRIVED → RECEIVED` (+ REJECTED / CANCELED)

- `PENDING`: 회원이 제출. 수정/삭제 가능.
- `APPROVED`: 관리자가 일괄 승인 (또는 자동).
- `ORDERED`: 관리자가 알라딘 결제 후 "주문 완료" 로 마킹.
- `ARRIVED`: 택배 도착. 책장에 꽂힘.
- `RECEIVED`: 회원이 수령 확인. 이후 독후감 단계로 넘어감(15 문서).

### 3-2. MonthLock
- Key: `(club_id, YYYY-MM)`. Lock 존재 시 해당 월 신청 수정 금지.
- 관리자는 잠금/해제 자유. 실수 해제 대비로 해제는 경고 모달.

### 3-3. 카트 URL 생성
- APPROVED 책들의 `aladinUrl` (혹은 ISBN) 을 합산해 **알라딘 카트 추가 URL** 을 조합.
- 결제/주문 번호는 시스템이 모른다. 관리자가 수동 "주문 완료" 버튼으로 ORDERED 전환.

### 3-4. 도착/수령
- 도착은 **책 단위로 체크박스**. 일괄 선택 + 일괄 반영 지원.
- 수령은 본인만 가능. 관리자가 "수령 간주" 처리할 수 있어야 운영이 돌아감 (이의 제기 프로세스 04 문서).

## 4. UX 흐름

### 관리자
1. `/muje/admin/book-requests` (`AdminBookRequestsPage`) — 월별 필터, 전체 탭 (`book-requests/all`).
2. [마감 잠금] 토글 — 잠금 / 해제.
3. 잠금 후 [알라딘 카트 URL 만들기] — 새 탭으로 카트 URL 열림.
4. 결제 완료 후 [주문 완료로 표시] 일괄 전환 (`mark-ordered`).
5. `/muje/admin/arrivals` (`AdminArrivalsPage`) — 도착 체크박스 + [도착 반영] (`mark-arrived`).
6. 미제출자 / 미수령자 패널 — `book-requests/unsubmitted` 등 (15 문서 연계).

### 회원
1. 책 신청 페이지에서 잠금 상태면 **신청 버튼 비활성** + 배지.
2. 내 신청 리스트에서 각 건의 상태 뱃지 (PENDING/ORDERED/ARRIVED/RECEIVED).
3. ARRIVED 상태 건에 [수령 확인] (`book-requests/mark-received`) 버튼.

## 5. API 표면

### 관리자
- `POST  /api/v1/admin/clubs/{clubId}/book-requests/lock` — 월 잠금.
- `POST  /api/v1/admin/clubs/{clubId}/book-requests/unlock` — 잠금 해제.
- `GET   /api/v1/admin/clubs/{clubId}/book-requests/all` — 전체 조회.
- `PATCH /api/v1/admin/clubs/{clubId}/book-requests/mark-arrived` — 도착 일괄.
- `PATCH /api/v1/admin/clubs/{clubId}/book-requests/mark-unarrived` — 도착 해제.
- `PATCH /api/v1/admin/clubs/{clubId}/book-requests/mark-ordered` — 주문 완료.
- `PATCH /api/v1/admin/clubs/{clubId}/book-requests/unmark-ordered` — 주문 해제.
- `GET   /api/v1/admin/clubs/{clubId}/book-requests/unsubmitted` — 미제출 회원.
- `GET   /api/v1/admin/clubs/{clubId}/orders` — 주문 이력.

### 회원
- `POST /api/v1/clubs/{clubId}/books/parse-url` — 알라딘 URL 파서 (신청 전 프리뷰).
- `POST /api/v1/clubs/{clubId}/book-requests` — 신청 생성 (잠금/중복/예산 체크).
- `PATCH /api/v1/clubs/{clubId}/book-requests/{id}` — 수정.
- `DELETE /api/v1/clubs/{clubId}/book-requests/{id}` — 취소.
- `PATCH /api/v1/clubs/{clubId}/book-requests/mark-received` — 수령 일괄.

## 6. 수용 기준 (Acceptance)

- [x] 마감 잠금 후 신청 생성/수정/삭제 모두 409.
- [x] 잠금 해제 후 다시 허용.
- [x] 카트 URL 이 APPROVED 건 전부를 포함.
- [x] 도착 체크박스 일괄 반영 시 상태 정확.
- [x] 수령은 본인만, 관리자 간주 처리 경로 있음.
- [x] 미제출 / 미수령 리스트에 이름이 정확히 노출 (동명이인 대비 이메일 tooltip).

## 7. 오픈 이슈 / 향후 계획

- **자동 결제**: 알라딘 API 연동은 내부 승인 필요 — 현 범위 밖.
- **잠금 타이머**: 매월 말일 23:59 자동 잠금(크론) + 알림 — 12 문서 연계.
- **주문 실패/환불**: 현재 정책 없음. 분실/파손 포함 정책 v2.
- **멀티 쇼핑몰**: 교보·YES24 도 합산 지원할지.
