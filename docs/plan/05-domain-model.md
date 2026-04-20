# 도메인 모델 정의서

> 공통 코어 도메인과 무제 전용 도메인을 명확히 구분한다.
> Phase 4 모듈 템플릿화를 고려해 코어/확장 경계를 지금 확립한다.

---

## 도메인 경계 원칙

```
[Core Domain]          [무제 전용 Domain]      [인프라 / 횡단 관심사]
─────────────          ─────────────────      ────────────────────
Member                 BookRequest             Notification (알림 발송 이력)
Club                   BookReport
ClubMember             PhotoSchedule
Schedule               BookLending
Penalty                LibraryBook
PenaltyDispute
Point
```

- **Core Domain**: 모든 동호회에 공통 적용되는 도메인
- **무제 전용 Domain**: 독서 동호회 특화 기능. Phase 4에서 모듈로 추상화 예정
- **인프라 / 횡단 관심사**: 특정 동호회 도메인이 아닌 횡단 관심사. `com.example.demo.notification` 패키지에 격리하며, Slack 발송 / 스케줄러 / 발송 이력을 담당
- **SPI 방향**: Phase 4부터 `ClubPlugin` 인터페이스로 동호회별 `ScheduleType` / `PenaltyType` typeCode를 등록한다. Core Enum에 동호회 전용 타입을 박지 않는다

---

## Core Domain 엔티티

### Member (회원)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `email` | String | Google 계정 이메일 (Unique) |
| `name` | String | Google 계정 이름 (수정 불가) |
| `nickname` | String | 서비스 내 닉네임 |
| `department` | String | 부서명 |
| `slackUserId` | String | Slack DM 발송용 사용자 ID (nullable) |
| `emailVerified` | Boolean | 2-step 이메일 인증 완료 여부 |
| `createdAt` | LocalDateTime | 가입 일시 |
| `updatedAt` | LocalDateTime | 최종 수정 일시 |

---

### Club (동호회)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `name` | String | 동호회명 (예: 무제) |
| `description` | String | 한줄 소개 |
| `type` | Enum | 동호회 유형 — `READING`, `GENERAL` [TBD: 타입 확장 여부] |
| `createdAt` | LocalDateTime | 생성 일시 |

---

### ClubMember (동호회 회원 매핑)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `memberId` | Long | FK → Member |
| `role` | Enum | `ADMIN`, `MEMBER` |
| `joinedAt` | LocalDateTime | 가입 일시 |

- `role`은 동호회 단위로 부여. 동일 회원이 A 동호회 ADMIN / B 동호회 MEMBER 가능
- 예산 한도 전환 기준은 `Member.createdAt`의 YearMonth로 계산한다. 별도 필드를 두지 않는다 (가입월 = 30,000원 한도, 그 다음 월부터 35,000원).

---

### Schedule (운영 일정)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `typeCode` | String | 일정 타입 코드 (예: `"BOOK_REQUEST_DEADLINE"`, `"BOOK_REPORT_DEADLINE"`). 각 `ClubPlugin`이 자신이 지원하는 typeCode set을 SPI로 등록한다. Core enum에 동호회 전용 타입을 박지 않는다 |
| `date` | LocalDate | 일정 날짜 |
| `description` | String | 일정 설명 (nullable) |
| `yearMonth` | YearMonth | 귀속 월 (리마인더 스케줄링 기준) |

---

### Penalty (벌점)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `memberId` | Long | FK → Member |
| `typeCode` | String | 벌점 타입 코드 (예: `"BOOK_REPORT_MISSING"`, `"PHOTO_ABSENT"`, `"BOOK_NOT_RECEIVED"`). 각 `ClubPlugin`이 SPI로 등록 |
| `score` | Int | 벌점 점수 (음수값. 기본 -1) |
| `reason` | String | 부여 사유 |
| `targetMonth` | YearMonth | 귀속 월 |
| `status` | Enum | `ACTIVE`, `CANCELLED` |
| `createdBy` | Long | FK → Member (관리자) |
| `createdAt` | LocalDateTime | 부여 일시 |

---

### PenaltyDispute (벌점 이의 제기)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `penaltyId` | Long | FK → Penalty |
| `memberId` | Long | FK → Member (신청자) |
| `reason` | String | 이의 사유 |
| `status` | Enum | `PENDING`, `ACCEPTED`, `REJECTED` |
| `reviewedBy` | Long | FK → Member (처리 관리자, nullable) |
| `reviewComment` | String | 처리 사유 (nullable) |
| `createdAt` | LocalDateTime | 신청 일시 |
| `reviewedAt` | LocalDateTime | 처리 일시 (nullable) |

---

### Point (포인트)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `memberId` | Long | FK → Member |
| `type` | Enum | `BOOK_REPORT_SUBMIT`, `EARLY_SUBMIT`, `PHOTO_ATTEND`, `MANUAL` |
| `score` | Int | 포인트 점수 (양수) |
| `reason` | String | 부여 사유 |
| `targetMonth` | YearMonth | 귀속 월 |
| `createdAt` | LocalDateTime | 부여 일시 |

---

## 인프라 / 횡단 관심사 엔티티

> 특정 동호회 도메인이 아닌 횡단 관심사. `com.example.demo.notification` 패키지에 격리한다.

### Notification (알림 발송 이력)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `type` | Enum | `BOOK_REPORT_REMINDER`, `PENALTY_NOTICE`, `RECEIVE_ALERT` 등 |
| `channel` | Enum | `SLACK_CHANNEL`, `SLACK_DM` |
| `recipientId` | Long | FK → Member (DM 수신자, nullable) |
| `message` | String | 실제 발송 메시지 본문 |
| `success` | Boolean | 발송 성공 여부 |
| `failReason` | String | 실패 사유 (nullable) |
| `sentAt` | LocalDateTime | 발송 시도 일시 |

---

## 무제 전용 Domain 엔티티

### BookRequest (책 신청)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `memberId` | Long | FK → Member |
| `title` | String | 도서명 |
| `author` | String | 저자 |
| `isbn` | String | ISBN-13 |
| `price` | Int | 도서 가격 (원화 환산 기준 — 외서는 `originalPrice` × `exchangeRate`로 계산하여 저장) |
| `originalPrice` | Decimal | 원래 통화 기준 가격 (외서일 때만 의미. 국내서는 `price`와 동일) |
| `currency` | String | 통화 코드 (기본 `KRW`. 외서: `USD` / `JPY` 등) |
| `exchangeRate` | Decimal | 신청 시점 환율(`originalPrice → KRW`). 신청 시점에 동결한다. 국내서는 1.0 |
| `category` | Enum | 카테고리 코드 |
| `sourceUrl` | String | 알라딘 상품 URL |
| `status` | Enum | `PENDING`, `LOCKED`, `ORDERED`, `SHIPPING`, `ARRIVED`, `RECEIVED` |
| `targetMonth` | YearMonth | 신청 귀속 월 |
| `createdAt` | LocalDateTime | 신청 일시 |
| `updatedAt` | LocalDateTime | 최종 수정 일시 |

---

### BookReport (독후감)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `bookRequestId` | Long | FK → BookRequest |
| `memberId` | Long | FK → Member |
| `title` | String | 독후감 제목 |
| `content` | Text | 본문 |
| `rating` | Int | 별점 (1~5) |
| `status` | Enum | `SUBMITTED`, `LATE_SUBMITTED` [결정 필요: 지각 제출 구분 여부] |
| `submittedAt` | LocalDateTime | 제출 일시 |
| `updatedAt` | LocalDateTime | 최종 수정 일시 |

---

### PhotoSchedule (촬영 일정)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `scheduledAt` | LocalDateTime | 촬영 일시 |
| `location` | Enum | `FLOOR_13`, `FLOOR_16` |
| `assignedAdminId` | Long | FK → Member (담당 관리자) |
| `createdAt` | LocalDateTime | 등록 일시 |
| `updatedAt` | LocalDateTime | 수정 일시 |

---

### LibraryBook (책장 도서)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `title` | String | 도서명 |
| `author` | String | 저자 |
| `isbn` | String | ISBN-13 |
| `available` | Boolean | 대여 가능 여부 |
| `registeredAt` | LocalDateTime | 등록 일시 |

---

### BookLending (도서 대여)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `clubId` | Long | FK → Club |
| `bookId` | Long | FK → LibraryBook |
| `memberId` | Long | FK → Member |
| `borrowedAt` | LocalDateTime | 대여 일시 |
| `expectedReturnDate` | LocalDate | 반납 예정일 |
| `returnedAt` | LocalDateTime | 실제 반납 일시 (nullable) |
| `status` | Enum | `BORROWED`, `RETURNED`, `OVERDUE` |

---

## 엔티티 관계 요약

```
Member ──< ClubMember >── Club
Member ──< BookRequest ──< BookReport
Member ──< BookLending >── LibraryBook
Member ──< Penalty ──< PenaltyDispute
Member ──< Point
Club ──< Schedule
Club ──< Notification
Club ──< PhotoSchedule
```

---

## 공통 코어 vs 무제 전용 경계 요약

| 구분 | 엔티티 | Phase 4 추상화 방향 |
|------|--------|---------------------|
| Core | Member, Club, ClubMember, Schedule, Penalty, PenaltyDispute, Point | 모든 동호회 공통 사용. `Schedule`/`Penalty`의 `typeCode`는 각 `ClubPlugin`이 SPI로 등록 |
| 무제 전용 | BookRequest, BookReport, PhotoSchedule, LibraryBook, BookLending | `ClubPlugin` 인터페이스로 추상화 예정 |
| 인프라 | Notification | 동호회와 무관한 횡단 관심사. `com.example.demo.notification` 패키지에 격리 |

- Phase 4에서 무제 전용 도메인을 `ReadingClubPlugin` 모듈로 분리
- 신규 동호회는 `BaseClubPlugin`을 구현하고 필요한 엔티티만 활성화하는 방식으로 확장
