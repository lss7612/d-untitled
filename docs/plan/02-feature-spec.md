# 기능 정의서 (TO-BE)

> 구글 시트 기반 수동 운영을 전면 대체하여 관리자 부담을 최소화하고 회원 경험을 개선한다.

---

## Phase 1 — 인증 & 회원 관리

### F-00: 인증

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/auth/google` | Google OAuth 2.0 인증 시작 (redirect) |
| GET | `/auth/google/callback` | OAuth 콜백 처리 → JWT 발급 |
| POST | `/auth/verify-email` | 이메일 Verify Code 발송 요청 |
| POST | `/auth/verify-email/confirm` | Verify Code 검증 및 2-step 완료 |
| POST | `/auth/logout` | 로그아웃 (토큰 무효화) |

#### 입력 / 출력 필드

**POST `/auth/verify-email`**
- 입력: `{ "email": "user@doubleugames.com" }`
- 출력: `{ "message": "인증 코드가 발송되었습니다.", "expiresIn": 300 }`

**POST `/auth/verify-email/confirm`**
- 입력: `{ "email": "user@doubleugames.com", "code": "123456" }`
- 출력: `{ "accessToken": "...", "tokenType": "Bearer" }`

#### 비즈니스 규칙

- 허용 도메인: `kr.doubledown.com`, `afewgoodsoft.com`, `doubleugames.com` — 외 도메인 즉시 차단
- Verify Code: 6자리 숫자, 유효 시간 5분
- 코드 오입력 5회 이상 → 해당 이메일 5분 잠금 (확정)
- Google OAuth 성공 후 이메일 Verify Code 미완료 상태면 접근 제한 (2-step 미완료 상태 유지)
- JWT 만료 시간: 1시간 (확정). Refresh Token 미도입 — 만료 시 재로그인 유도

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 로그인 | O | O |
| 이메일 인증 | O | O |
| 타 회원 계정 잠금 | X | O |
| 회원 권한 변경 | X | O |

---

### F-00-B: 회원 프로필 관리

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/members/me` | 내 프로필 조회 |
| PATCH | `/members/me` | 내 프로필 수정 |
| GET | `/admin/members` | 전체 회원 목록 조회 (관리자 전용) |
| PATCH | `/admin/members/{memberId}/role` | 회원 권한 변경 (관리자 전용) |

#### 입력 / 출력 필드

**PATCH `/members/me`**
- 입력: `{ "nickname": "홍길동", "department": "개발팀", "slackUserId": "U01XXXXXXX" }`
- 출력: `{ "id": 1, "email": "...", "name": "...", "nickname": "...", "department": "...", "role": "MEMBER" }`

#### 비즈니스 규칙

- `name`은 Google 계정 이름 자동 동기화 — 직접 수정 불가
- `slackUserId`는 DM 알림 발송에 사용. 미등록 시 DM 알림 발송 불가 (채널 공지만 수신)
- 관리자는 `ADMIN` / 일반 회원은 `MEMBER` — 역할은 관리자만 변경 가능

---

## Phase 2 — 무제 핵심 기능 (Google Sheets 완전 대체)

### F-01: 책 신청 폼

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/clubs/{clubId}/books/parse-url` | 알라딘 URL 파싱 → 도서 정보 반환 |
| GET | `/clubs/{clubId}/book-requests` | 이번 달 전체 신청 목록 조회 |
| POST | `/clubs/{clubId}/book-requests` | 책 신청 등록 |
| PATCH | `/clubs/{clubId}/book-requests/{requestId}` | 책 신청 수정 (잠금 전) |
| DELETE | `/clubs/{clubId}/book-requests/{requestId}` | 책 신청 취소 |
| POST | `/admin/clubs/{clubId}/book-requests/lock` | 신청 마감 잠금 처리 (관리자) |

#### 입력 / 출력 필드

**POST `/clubs/{clubId}/books/parse-url`**
- 입력: `{ "url": "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=..." }`
- 출력: `{ "title": "책 제목", "author": "저자명", "isbn": "9791234567890", "price": 18000, "currency": "KRW" }`

**POST `/clubs/{clubId}/book-requests`**
- 입력: `{ "title": "...", "author": "...", "isbn": "...", "price": 18000, "category": "LITERATURE", "sourceUrl": "https://..." }`
- 출력: `{ "id": 42, "status": "PENDING", "remainingBudget": 17000 }`

**GET `/clubs/{clubId}/book-requests`**
- Query: `?year=2026&month=3`
- 출력: 신청 목록 배열 + `{ "totalCount": 12, "myRemainingBudget": 17000 }`

#### 비즈니스 규칙

- URL 파싱 대상: 알라딘(`aladin.co.kr`) 전용 — 타 도메인 입력 시 400 오류 반환
- ISBN 중복 신청: 동일 월 내 동일 ISBN 재신청 시 409 오류 반환
- 예산 초과: 신청 시점에 잔여 예산 계산 → 초과분 포함 신청이면 400 오류 + 잔여 예산 안내
- 잠금(LOCKED) 이후 수정/취소 불가 — 403 오류 반환
- 외서 환율 처리: [결정 필요: 고정 환율 사용 / 신청 시점 환율 API 연동]
- 카테고리 코드: `LITERATURE`, `HUMANITIES`, `SELF_HELP`, `ART`, `IT`, `COMICS`, `ECONOMY`, `LIFESTYLE`, `SCIENCE`, `ETC`

**예산 규칙**
- 첫 번째 신청월 (신입 첫달): 30,000원 한도
- 두 번째 신청월부터: 35,000원 한도 (자동 전환)
- 동일 월 복수 권 신청 가능 — 합산 금액 기준 예산 초과 시 마지막 신청 차단
- 신청 페이지에 잔여 예산 실시간 표시 (예: 잔여 12,000원)

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 전체 신청 목록 조회 | O | O |
| 본인 신청 등록/수정/취소 | O (잠금 전) | O |
| 타 회원 신청 수정/삭제 | X | O |
| 신청 마감 잠금 처리 | X | O |

---

### F-02: 주문 자동화

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/clubs/{clubId}/orders/generate` | 신청 취합 → 합산 주문서 생성 |
| GET | `/admin/clubs/{clubId}/orders/{orderId}` | 주문서 조회 |
| PATCH | `/admin/clubs/{clubId}/orders/{orderId}/status` | 주문 상태 변경 |
| GET | `/clubs/{clubId}/orders/my` | 내 주문 상태 조회 |

#### 입력 / 출력 필드

**PATCH `/admin/clubs/{clubId}/orders/{orderId}/status`**
- 입력: `{ "status": "ORDERED" }` — 상태값: `PENDING` → `ORDERED` → `SHIPPING` → `ARRIVED`
- 출력: `{ "orderId": 1, "status": "ORDERED", "updatedAt": "2026-03-28T10:00:00" }`

#### 비즈니스 규칙

- 주문서 생성은 신청 마감(LOCKED) 이후에만 가능 — 이전 시도 시 409 오류
- 동일 ISBN 복수 신청 → 주문서 생성 시 1건으로 자동 합산
- 재고 확인 API: [TBD: 알라딘 Open API 스펙 확인 필요 — 폴링 or Webhook]
- 품절 감지 시 신청자에게 슬랙 DM 발송 + 관리자 채널 공지

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 내 주문 상태 조회 | O | O |
| 전체 주문서 조회/생성/상태 변경 | X | O |

---

### F-03: 인포 수령 관리

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/clubs/{clubId}/orders/{orderId}/arrive` | 도서 도착 처리 + 수령 알림 발송 |
| POST | `/clubs/{clubId}/orders/{orderId}/receive` | 수령 완료 처리 (본인) |
| GET | `/admin/clubs/{clubId}/orders/{orderId}/receive-status` | 미수령자 목록 조회 (관리자) |

#### 입력 / 출력 필드

**POST `/clubs/{clubId}/orders/{orderId}/receive`**
- 입력: 없음 (인증 토큰으로 회원 식별)
- 출력: `{ "receivedAt": "2026-03-28T14:30:00" }`

#### 비즈니스 규칙

- 도착 처리 시 해당 주문 신청자 전원에게 슬랙 DM 발송
- 수령 기한: 도착일로부터 7일 — 7일 초과 미수령자 목록을 관리자에게 알림
- 미수령 벌점 부여는 자동 부여 불가 — 관리자가 F-07을 통해 수동 부여
- [결정 필요: QR 스캔 수령 확인 도입 여부 / 버튼 클릭 방식 유지]

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 본인 수령 처리 | O | O |
| 도착 처리 / 미수령자 조회 | X | O |

---

### F-04: 독후감 제출

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/book-reports` | 이번 달 독후감 목록 조회 |
| GET | `/clubs/{clubId}/book-reports/my` | 내 독후감 조회 |
| POST | `/clubs/{clubId}/book-reports` | 독후감 제출 |
| PATCH | `/clubs/{clubId}/book-reports/{reportId}` | 독후감 수정 (마감 전) |
| GET | `/admin/clubs/{clubId}/book-reports/status` | 제출 현황 조회 (관리자) |

#### 입력 / 출력 필드

**POST `/clubs/{clubId}/book-reports`**
- 입력: `{ "bookRequestId": 42, "title": "독후감 제목", "content": "본문 내용", "rating": 4 }`
- 출력: `{ "id": 10, "submittedAt": "2026-03-28T15:00:00", "status": "SUBMITTED" }`

**GET `/admin/clubs/{clubId}/book-reports/status`**
- Query: `?year=2026&month=3`
- 출력: `{ "totalMembers": 20, "submitted": 15, "notSubmitted": [{ "memberId": 3, "name": "홍길동" }] }`

#### 비즈니스 규칙

- 독후감은 당월 책 신청 건에만 연결 가능 — 타 월 bookRequestId 사용 시 400 오류
- 마감일 이후 수정 불가 — 403 오류 반환
- 별점(rating): 1~5 정수만 허용
- 본문 최소 글자 수: [결정 필요: 100자 / 200자 / 제한 없음]
- 마감일 D-7 / D-3 / D-1 자동 리마인더 → 04-notification-spec.md 참조

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 본인 독후감 제출/수정 | O (마감 전) | O |
| 타 회원 독후감 열람 | O | O |
| 미제출자 목록 조회 | X | O |

---

### F-05: 촬영 일정 관리

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/photo-schedules` | 촬영 일정 목록 조회 |
| POST | `/admin/clubs/{clubId}/photo-schedules` | 촬영 일정 등록 (관리자) |
| PATCH | `/admin/clubs/{clubId}/photo-schedules/{scheduleId}` | 촬영 일정 수정 (관리자) |
| DELETE | `/admin/clubs/{clubId}/photo-schedules/{scheduleId}` | 촬영 일정 삭제 (관리자) |

#### 입력 / 출력 필드

**POST `/admin/clubs/{clubId}/photo-schedules`**
- 입력: `{ "scheduledAt": "2026-04-05T14:00:00", "location": "FLOOR_13", "assignedAdminId": 2 }`
- 출력: `{ "id": 5, "scheduledAt": "...", "location": "FLOOR_13", "assignedAdmin": { "id": 2, "name": "..." } }`

#### 비즈니스 규칙

- 촬영 공간: `FLOOR_13`, `FLOOR_16` — 공간별 담당 관리자 사전 등록 필요
- 일정 수정 시 기존 등록자 전원에게 슬랙 알림 자동 발송
- 동일 날짜 동일 공간 중복 등록 시 409 오류 반환
- 촬영 불참 벌점 부여: 관리자가 F-07을 통해 수동 부여

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 일정 조회 | O | O |
| 일정 등록/수정/삭제 | X | O |

---

### F-06: 일정 관리 (운영 캘린더)

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/schedules` | 월별 운영 일정 조회 |
| POST | `/admin/clubs/{clubId}/schedules` | 운영 일정 등록 (관리자) |
| PATCH | `/admin/clubs/{clubId}/schedules/{scheduleId}` | 운영 일정 수정 (관리자) |
| DELETE | `/admin/clubs/{clubId}/schedules/{scheduleId}` | 운영 일정 삭제 (관리자) |

#### 입력 / 출력 필드

**POST `/admin/clubs/{clubId}/schedules`**
- 입력: `{ "type": "BOOK_REQUEST_DEADLINE", "date": "2026-04-10", "description": "4월 책 신청 마감" }`
- 출력: `{ "id": 7, "type": "BOOK_REQUEST_DEADLINE", "date": "2026-04-10" }`

#### 비즈니스 규칙

- 일정 타입: `BOOK_REQUEST_DEADLINE`, `BOOK_REPORT_DEADLINE`, `PHOTO_SHOOT`, `MONTHLY_MEETING`
- 동일 월 동일 타입 일정이 없으면 알림 발송 시 경고 처리 (누락 방지)
- 독후감 마감일 등록 시 D-7/D-3/D-1 알림 스케줄 자동 생성

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 일정 조회 | O | O |
| 일정 등록/수정/삭제 | X | O |

---

### F-07: 벌점 관리

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/penalties/my` | 내 벌점 이력 조회 |
| GET | `/admin/clubs/{clubId}/penalties` | 전체 벌점 이력 조회 (관리자) |
| POST | `/admin/clubs/{clubId}/penalties` | 벌점 수동 부여 (관리자) |
| DELETE | `/admin/clubs/{clubId}/penalties/{penaltyId}` | 벌점 취소 (관리자) |
| POST | `/clubs/{clubId}/penalties/{penaltyId}/dispute` | 이의 제기 (회원) |
| PATCH | `/admin/clubs/{clubId}/penalties/{penaltyId}/dispute` | 이의 제기 처리 (관리자) |

#### 입력 / 출력 필드

**POST `/admin/clubs/{clubId}/penalties`**
- 입력: `{ "memberId": 3, "type": "BOOK_REPORT_MISSING", "reason": "3월 독후감 미제출", "targetMonth": "2026-03" }`
- 출력: `{ "id": 15, "memberId": 3, "type": "BOOK_REPORT_MISSING", "score": -1, "createdAt": "..." }`

**POST `/clubs/{clubId}/penalties/{penaltyId}/dispute`**
- 입력: `{ "reason": "제출했으나 시스템 오류로 누락된 것으로 추정됩니다." }`
- 출력: `{ "disputeId": 3, "status": "PENDING" }`

#### 비즈니스 규칙

- 벌점 타입별 점수: `BOOK_REPORT_MISSING` (-1), `PHOTO_ABSENT` (-1), `BOOK_NOT_RECEIVED` (-1) [결정 필요: 점수 가중치 차등 적용 여부]
- 동일 회원 동일 월 동일 타입 벌점 중복 부여 불가 — 409 오류 반환
- 이의 제기 상태: `PENDING` → `ACCEPTED`(벌점 취소) / `REJECTED`(벌점 유지)
- 이의 제기 처리 시 해당 회원에게 슬랙 DM 발송

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 본인 벌점 이력 조회 | O | O |
| 전체 벌점 이력 조회 | X | O |
| 벌점 부여/취소 | X | O |
| 이의 제기 | O | X |
| 이의 제기 처리 | X | O |

---

### F-08: 도서 대여 관리

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/library` | 책장 도서 목록 조회 |
| POST | `/admin/clubs/{clubId}/library` | 도서 등록 (관리자) |
| POST | `/clubs/{clubId}/library/{bookId}/borrow` | 대여 신청 |
| POST | `/clubs/{clubId}/library/{bookId}/return` | 반납 처리 |
| GET | `/clubs/{clubId}/library/my-borrows` | 내 대여 이력 |
| GET | `/admin/clubs/{clubId}/library/overdue` | 장기 미반납 목록 (관리자) |

#### 입력 / 출력 필드

**POST `/clubs/{clubId}/library/{bookId}/borrow`**
- 입력: `{ "expectedReturnDate": "2026-04-28" }`
- 출력: `{ "borrowId": 8, "borrowedAt": "...", "expectedReturnDate": "..." }`

#### 비즈니스 규칙

- 대여 자격: 동호회 정식 회원만 가능 — 비회원 403 오류
- 동일 도서 동시 대여 불가 — 반납 완료 후 재대여 가능
- 반납 기한 초과 14일 → 자동 알림 발송 [결정 필요: 알림 기준일 7일 / 14일 / 30일]
- 장기 미반납 기준: [TBD: 운영 규정 확정 필요]

#### 권한 구분

| 기능 | 일반 회원 | 관리자 |
|------|-----------|--------|
| 도서 목록 조회 / 대여 / 반납 | O | O |
| 도서 등록/삭제 | X | O |
| 전체 대여 현황 / 미반납 목록 | X | O |

---

## Phase 3 — 알림 & 게이미피케이션

### F-09: 스마트 리마인더

> 상세 발송 명세는 04-notification-spec.md 참조.

- 월별 과업(제출 마감, 인포 수령 등) 자동 리마인더
- 활동 달성 시 보너스 포인트, 조기 제출 시 추가 포인트

### F-10: 채널 연동

- 알림 채널: **슬랙 전용** — Slack Incoming Webhook으로 발송
- 알림 타입별 연동 채널 배정 (리마인더 / 벌점 등 구분)

### F-11: 포인트 / 벌점 시스템

#### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/clubs/{clubId}/points/my` | 내 포인트 이력 조회 |
| GET | `/clubs/{clubId}/points/ranking` | 월간 랭킹 조회 |
| POST | `/admin/clubs/{clubId}/points` | 포인트 수동 부여 (관리자) |

#### 비즈니스 규칙

- 포인트 기본 부여 기준: 독후감 제출 +1, 조기 제출(마감 3일 전) +1, 촬영 참여 +1
- [결정 필요: 포인트 환전/교환 기능 Phase 3 포함 여부]
- 랭킹 기준: 당월 누적 포인트 합산

---

## Phase 4 — 모듈 템플릿화

### F-12: 동호회 모듈 설정

- 독후감 없는 동호회용 기본 모듈 제공 (모임 중심, 재무 중심 등)
- 동호회별 운영 규칙 커스텀 설정 (벌점 항목, 알림 주기 등)

### F-13: 동호회 생성 위저드

- 기존 무제 설정 기반 템플릿 제공
- 신규 동호회 온보딩 플로우 (5단계 위저드 형태)

---

## 리스크

| 항목 | 내용 |
|------|------|
| Google OAuth 연동 복잡도 | 회사 이메일 도메인 한정 설정 필요 (Workspace 관리자 협조) |
| 외부 API 의존성 | 알라딘/교보 재고 API 스펙 전 별도 폴링 또는 캐시 로직 필요 |
| 무제 전용 vs 범용 설계 일관성 | Phase 2 무제 전용 기능이 보편 모듈로 추상화될 수 있도록 도메인 모델 설계 시 고려 필요 |
