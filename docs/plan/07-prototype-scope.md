# 프로토타입 범위 정의서 (Prototype Scope)

> 작성일: 2026-03-28
> 목적: "실제로 돌아가는 무제 동호회 프로토타입"의 최소 범위를 정의한다.
> 기준: 핵심 사이클(책 신청 → 수령 → 독후감 제출)을 완성하면서, 실제 운영진과 회원이 구글 시트 수작업 없이 써볼 수 있는 수준.
>
> **[현재 단계] v0 프로토타입: 로그인(인증) 기능까지만 구현.**
> 이후 v1, v2로 핵심 사이클 기능을 순차 추가한다.

---

## 0. 단계별 프로토타입 범위

| 버전 | 범위 | 상태 |
|------|------|------|
| v0 | 로그인 (Google OAuth + 이메일 2-step + 동호회 목록) | **현재 구현 대상** |
| v1 | 책 신청 + 독후감 제출 (핵심 사이클) | 다음 단계 |
| v2 | 주문/수령 관리 + 관리자 대시보드 | 그 다음 |
| v3 | Slack 알림 + 벌점 + 포인트 | Phase 3 |

---

## 1. 기능(Feature) 및 페이지(Page) IN/OUT 표

### Feature IN/OUT

| Feature | 이름 | 판정 | 이유 |
|---------|------|------|------|
| F-00 | 인증 (Google OAuth 2.0 + 이메일 2-step) | IN | 모든 기능의 전제 조건 |
| F-00-B | 회원 프로필 관리 | IN (최소) | 닉네임·부서 수정까지만. Slack ID 등록은 UI에 포함하되 알림 없이 저장만 |
| F-01 | 책 신청 폼 | IN | 핵심 사이클 시작점. 알라딘 URL 파싱 포함 |
| F-02 | 주문 자동화 | IN (최소) | 주문서 생성 + 상태 변경(PENDING→ARRIVED)까지. 알라딘 재고 API 연동은 제외 |
| F-03 | 인포 수령 관리 | IN (최소) | 버튼 클릭 수령 방식으로만. Slack 알림 없이, 미수령자 목록 관리자 조회까지 |
| F-04 | 독후감 제출 | IN | 핵심 사이클 종료점. 텍스트 에디터 + 별점 + 마감 표시 |
| F-05 | 촬영 일정 관리 | OUT | 핵심 사이클과 직접 관련 없음. 알림도 제외되므로 프로토 가치 낮음 |
| F-06 | 운영 일정 관리 (캘린더) | IN (최소) | 관리자가 책 신청 마감일·독후감 마감일을 등록해야 대시보드 표시 가능. 마감 2가지 타입만 |
| F-07 | 벌점 관리 | OUT | 자동 알림 없이 수동 부여만 남는다면 핵심 페인포인트 해소에 기여 없음. 이의 제기 플로우는 특히 제외 |
| F-08 | 도서 대여 관리 | OUT | 도메인 복잡도 대비 핵심 사이클과 거리 멂. TBD 항목(운영 규정) 미확정 상태 |
| F-09 | 스마트 리마인더 | OUT | Slack 연동 전체 제외. 프로토는 수동 확인으로 대체 |
| F-10 | 채널 연동 (Slack) | OUT | 프로토는 UI 내 현황 조회로 알림 대체. Slack Webhook 설정 불필요 |
| F-11 | 포인트/벌점 시스템 | OUT | Phase 3 기능. 프로토 핵심 가치와 무관 |
| F-12 | 동호회 모듈 설정 | OUT | Phase 4 기능 |
| F-13 | 동호회 생성 위저드 | OUT | Phase 4 기능 |

### Page IN/OUT

| Page | 이름 | 판정 | 이유 |
|------|------|------|------|
| PAGE-01 | 로그인 | IN | 진입점. 필수 |
| PAGE-01-B | 이메일 Verify Code 입력 | IN | 2-step 인증 필수 |
| PAGE-02 | 동호회 목록 | IN | 로그인 후 진입점. 무제 카드 1개만 표시 |
| PAGE-03 | 무제 Welcome (대시보드) | IN | 핵심 허브 페이지. 잔여 예산 + 일정 요약 + 메뉴 진입 |
| PAGE-04 | 책 신청 | IN | 핵심 사이클 시작 |
| PAGE-05 | 독후감 제출 | IN | 핵심 사이클 종료 |
| PAGE-06 | 벌점 이력 | OUT | F-07 제외에 따라 함께 제외 |
| PAGE-07 | 관리자 대시보드 | IN (최소) | 책 신청 현황 + 독후감 제출률 + 미수령 인원 조회. 슬랙 발송 버튼 제외. 벌점 부여 모달 제외 |
| PAGE-08 | 내 정보 | IN (최소) | 프로필 수정 + 책 신청/독후감 이력 탭. 포인트·벌점 요약 제외 |

---

## 2. 백엔드 ↔ 프론트엔드 논의 요약

### 2-1. Slack 알림 (F-09, F-10) 프로토 포함 여부

**백엔드**: Slack Incoming Webhook 설정, 스케줄러 크론, 발송 이력 도메인까지 설계해야 해서 프로토 공수가 크게 늘어남. `notification` 패키지 전체 스킵 가능. 도착 알림(F-03) 역시 버튼 클릭 수령 방식으로만 구현하고 알림 발송은 제외해도 수령 처리 자체는 작동함.

**프론트엔드**: 알림이 없으면 사용자가 수동으로 대시보드에 접속해서 현황을 확인하는 방식으로 대체 가능. 프로토 목적(구글 시트 수작업 제거)은 UI 내 현황 조회만으로도 충분히 체감됨. Slack ID 입력 필드는 PAGE-08에 유지하되 저장만 하고 알림은 보내지 않는 형태로 처리 가능.

**결론**: 제외. Slack 연동 전체를 프로토에서 제외한다. `notification` 패키지는 골격만 설계하고 실제 발송 구현은 Phase 3에 위임한다.

---

### 2-2. 포인트/벌점 시스템 (F-07, F-11) 프로토 포함 여부

**백엔드**: 벌점은 자동 부여가 아닌 관리자 수동 부여 방식이고, Slack DM 연동도 없으면 단순 CRUD다. 하지만 이의 제기 플로우(`PenaltyDispute`)와 상태 전이(`PENDING→ACCEPTED/REJECTED`)까지 포함하면 도메인 복잡도 대비 프로토 가치가 낮음. 포인트는 자동 부여 트리거가 F-04 독후감 제출에 묶여 있어 F-04 구현 시 포인트 이벤트 발행 처리가 추가로 필요해짐.

**프론트엔드**: PAGE-06(벌점 이력), PAGE-08의 포인트·벌점 요약 섹션이 없어도 핵심 사이클 UX는 완성됨. 관리자 대시보드(PAGE-07)에서 벌점 부여 모달을 제외하면 관리자 기능도 현황 조회 중심으로 단순해짐.

**결론**: 제외. `Penalty`, `PenaltyDispute`, `Point` 엔티티는 설계는 해두되 API 구현은 Phase 3에 위임한다. PAGE-06은 제외, PAGE-08에서 포인트·벌점 요약 섹션 제외.

---

### 2-3. 주문 자동화 (F-02) 범위

**백엔드**: 알라딘 재고 API 스펙이 TBD 상태(`T1` 블로커)여서 재고 확인 폴링 로직은 설계 불가. 주문서 생성(신청 취합) + 상태 수동 변경(PENDING→ORDERED→SHIPPING→ARRIVED)까지만 구현하면 관리자가 수동으로 주문 상태를 업데이트하는 방식으로 동작 가능.

**프론트엔드**: 관리자 대시보드에 주문 상태 변경 드롭다운을 포함하면 충분. 회원 측은 내 주문 상태 조회(`GET /clubs/{clubId}/orders/my`)로 현재 상태 확인 가능. 재고 품절 Slack 알림은 제외되므로 프론트 추가 작업 없음.

**결론**: 부분 포함. 주문서 생성 + 상태 수동 변경 + 내 주문 상태 조회까지 IN. 알라딘 재고 API 연동 및 품절 알림은 OUT.

---

### 2-4. 촬영 일정 관리 (F-05) 프로토 포함 여부

**백엔드**: `PhotoSchedule` 엔티티와 CRUD API를 별도로 구현해야 하며, 일정 변경 시 Slack 알림도 묶여 있음. 알림 없이 CRUD만 구현해도 독립적인 도메인이라 공수가 발생함. 핵심 사이클(책 신청 → 수령 → 독후감)과 직접 연결되지 않음.

**프론트엔드**: 촬영 일정 조회 화면을 따로 만들어야 하고, PAGE-03 대시보드에 촬영일 표시를 넣으려면 운영 일정(F-06) 안에 `PHOTO_SHOOT` 타입을 같이 관리하는 방식으로 단순화할 수 있음. 별도 PAGE 불필요.

**결론**: 제외. `PhotoSchedule` 전용 엔티티 및 API 제외. 촬영일 정보가 필요하면 F-06 운영 일정의 `PHOTO_SHOOT` 타입으로 관리하는 방식으로 대체 가능하나, 프로토에서는 운영 일정도 마감일 2가지(`BOOK_REQUEST_DEADLINE`, `BOOK_REPORT_DEADLINE`)로 제한하므로 사실상 촬영 관련 기능은 전부 제외.

---

### 2-5. 운영 일정 관리 (F-06) 범위

**백엔드**: 운영 일정이 없으면 대시보드에서 "이번 달 책 신청 마감 D-day"를 표시할 수 없음. 또한 독후감 마감일 기반 수정 잠금 로직(`BookReport` 서비스 레이어)이 `Schedule` 테이블을 조회해야 함. 최소한 `BOOK_REQUEST_DEADLINE`, `BOOK_REPORT_DEADLINE` 2가지 타입만 지원하면 충분.

**프론트엔드**: 관리자가 매월 마감일 2개를 등록하는 간단한 폼이면 충분. 운영 일정 등록 화면은 PAGE-07 관리자 대시보드에 인라인 섹션으로 추가하면 별도 페이지 불필요.

**결론**: 부분 포함. `BOOK_REQUEST_DEADLINE`, `BOOK_REPORT_DEADLINE` 2가지 타입만 IN. `PHOTO_SHOOT`, `MONTHLY_MEETING` 타입은 OUT. 관리자 대시보드 내 인라인 폼으로 처리.

---

### 2-6. 도서 대여 (F-08) 프로토 포함 여부

**백엔드**: `LibraryBook`, `BookLending` 엔티티, 대여/반납 CRUD, 장기 미반납 목록 API. 장기 미반납 기준일(`T2` 블로커)이 미확정이고 핵심 사이클과 무관함.

**프론트엔드**: 별도 페이지 또는 내 정보 탭 추가가 필요함. 현재 계획에 없는 추가 공수.

**결론**: 제외. Phase 2 후반에 추가.

---

### 2-7. Magic UI 애니메이션 프로토 적용 기준

**프론트엔드**: 프로토타입이지만 실제 운영진/회원이 처음 접하는 화면이므로 첫인상 품질이 중요함. 단, 애니메이션 구현 자체가 개발 공수를 크게 차지하면 안 됨. 기준: "설치 후 바로 쓸 수 있는 것"만 적용, "커스터마이징이 필요한 것"은 제외.

**백엔드**: 프론트엔드 판단에 위임. API 레이어와 무관함.

**결론**: 프로토에서 Magic UI 적용 범위를 아래로 제한함.

| 적용 | 컴포넌트 | 대상 |
|------|----------|------|
| IN | ShimmerButton | PAGE-01 Google 로그인 버튼 |
| IN | AnimatedGridPattern | PAGE-01 배경 |
| IN | MagicCard | PAGE-02 동호회 카드 |
| IN | NumberTicker | PAGE-03 잔여 예산 숫자, PAGE-07 현황 수치 |
| IN | BentoGrid + BentoCard | PAGE-03 메뉴 버튼 3개 |
| OUT | AnimatedList | 설치 후 커스터마이징 필요, 일정 요약은 shadcn/ui Card로 충분 |
| OUT | Marquee | 미제출자 목록 스크롤은 Table로 충분 |
| OUT | BorderBeam | D-day 배너는 shadcn/ui Badge로 충분 |
| OUT | AnimatedShinyText | 벌점 페이지 자체 제외 |

---

### 2-8. 관리자 기능 필수 선별

**백엔드**: 관리자 기능 중 프로토에서 반드시 있어야 하는 것: (1) 책 신청 마감 잠금 — 없으면 신청 취소/수정이 계속 가능해 주문서 생성이 불가능함. (2) 주문서 생성 + 상태 변경 — 주문 사이클 진행에 필수. (3) 독후감 제출 현황 조회 — 관리자가 미제출자를 파악하는 핵심 기능. (4) 운영 일정 등록 — 마감일 기반 로직 전제 조건.

**프론트엔드**: 슬랙 공지 발송 버튼은 Slack 연동 제외에 따라 PAGE-07에서 제거. 벌점 부여 모달도 제거. 관리자 대시보드는 "조회 + 상태 변경" 중심으로 단순화.

**결론**: 관리자 프로토 필수 기능 = 책 신청 마감 잠금 + 주문서 생성/상태 변경 + 도착 처리/미수령자 조회 + 독후감 제출 현황 + 운영 일정 등록(마감일 2가지) + 회원 권한 변경.

---

## 3. 프로토타입 API 목록

> `/api/v1` prefix 생략. 프로토타입에서 실제 구현할 엔드포인트만 수록.

### 인증 (F-00)

| Method | Path | 설명 |
|--------|------|------|
| GET | /auth/google | Google OAuth 시작 |
| GET | /auth/google/callback | OAuth 콜백 → JWT 발급 |
| POST | /auth/verify-email | 이메일 코드 발송 |
| POST | /auth/verify-email/confirm | 코드 검증 + 2-step 완료 |
| POST | /auth/logout | 로그아웃 |

### 회원 (F-00-B)

| Method | Path | 설명 |
|--------|------|------|
| GET | /members/me | 내 프로필 조회 |
| PATCH | /members/me | 내 프로필 수정 (닉네임, 부서, Slack ID) |
| GET | /admin/members | 전체 회원 목록 (관리자) |
| PATCH | /admin/members/{memberId}/role | 회원 권한 변경 (관리자) |

### 동호회 공통

| Method | Path | 설명 |
|--------|------|------|
| GET | /clubs | 동호회 목록 조회 |
| GET | /clubs/{clubId}/schedules | 운영 일정 목록 조회 |
| POST | /admin/clubs/{clubId}/schedules | 운영 일정 등록 (마감일 2가지만) |
| PATCH | /admin/clubs/{clubId}/schedules/{id} | 운영 일정 수정 |
| DELETE | /admin/clubs/{clubId}/schedules/{id} | 운영 일정 삭제 |

### 책 신청 (F-01)

| Method | Path | 설명 |
|--------|------|------|
| POST | /clubs/{clubId}/books/parse-url | 알라딘 URL 파싱 |
| GET | /clubs/{clubId}/book-requests | 이번 달 신청 목록 |
| POST | /clubs/{clubId}/book-requests | 책 신청 등록 |
| PATCH | /clubs/{clubId}/book-requests/{id} | 신청 수정 (잠금 전) |
| DELETE | /clubs/{clubId}/book-requests/{id} | 신청 취소 (잠금 전) |
| POST | /admin/clubs/{clubId}/book-requests/lock | 신청 마감 잠금 |

### 주문 관리 (F-02, 최소 범위)

| Method | Path | 설명 |
|--------|------|------|
| POST | /admin/clubs/{clubId}/orders/generate | 신청 취합 → 주문서 생성 |
| GET | /admin/clubs/{clubId}/orders/{orderId} | 주문서 조회 |
| PATCH | /admin/clubs/{clubId}/orders/{orderId}/status | 주문 상태 변경 |
| GET | /clubs/{clubId}/orders/my | 내 주문 상태 조회 |

### 수령 관리 (F-03, 최소 범위)

| Method | Path | 설명 |
|--------|------|------|
| POST | /admin/clubs/{clubId}/orders/{orderId}/arrive | 도서 도착 처리 |
| POST | /clubs/{clubId}/orders/{orderId}/receive | 수령 완료 처리 |
| GET | /admin/clubs/{clubId}/orders/{orderId}/receive-status | 미수령자 목록 조회 |

### 독후감 (F-04)

| Method | Path | 설명 |
|--------|------|------|
| GET | /clubs/{clubId}/book-reports | 이번 달 독후감 목록 |
| GET | /clubs/{clubId}/book-reports/my | 내 독후감 조회 |
| POST | /clubs/{clubId}/book-reports | 독후감 제출 |
| PATCH | /clubs/{clubId}/book-reports/{id} | 독후감 수정 (마감 전) |
| GET | /admin/clubs/{clubId}/book-reports/status | 독후감 제출 현황 (관리자) |

---

## 4. 프로토타입 페이지 목록

| Page | 이름 | 핵심 구현 포인트 | Magic UI |
|------|------|-----------------|---------|
| PAGE-01 | 로그인 | Google OAuth 버튼 + 도메인 제한 안내 | ShimmerButton, AnimatedGridPattern |
| PAGE-01-B | 이메일 인증 | 6자리 OTP 입력 + 60초 타이머 + 오입력 횟수 | shadcn/ui InputOTP |
| PAGE-02 | 동호회 목록 | 무제 카드 1개. 향후 다수 카드 확장 구조 | MagicCard |
| PAGE-03 | 무제 대시보드 | 잔여 예산 + 마감 D-day 요약 + 메뉴 3개 버튼 | NumberTicker, BentoGrid |
| PAGE-04 | 책 신청 | URL 파싱 + 도서 정보 자동 입력 + 신청 목록 + 잠금 상태 표시 | — |
| PAGE-05 | 독후감 제출 | 텍스트 에디터 + 별점 + D-day 배너 + 마감 후 읽기 전용 | — |
| PAGE-07 | 관리자 대시보드 | 현황 카드 3개 + 주문 상태 변경 + 마감 잠금 + 일정 등록 인라인 폼 | NumberTicker |
| PAGE-08 | 내 정보 | 프로필 수정 + 책 신청 이력 탭 + 독후감 이력 탭 | — |

> 관리자 대시보드(PAGE-07)에는 별도 관리자 전용 페이지를 만들지 않고, `/admin` 경로에 단일 페이지로 구성한다. 운영 일정 등록 폼은 대시보드 내 인라인 섹션으로 포함한다.

---

## 5. 제외 항목 및 이유

> 프로토 이후 추가 시 참고용.

| Feature/Page | 이유 | 추가 시기 (권장) |
|--------------|------|-----------------|
| F-05 촬영 일정 | 핵심 사이클과 무관. 알림 없이 CRUD만 추가하는 것은 페인포인트 해소 기여 낮음 | Phase 2 후반 또는 Phase 3 |
| F-07 벌점 관리 | Slack DM 알림 없이는 수동 CRUD로만 남아 가치 낮음. 이의 제기 플로우가 도메인 복잡도 상승 | Phase 3 (Slack 연동과 함께) |
| F-08 도서 대여 | 운영 규정(TBD) 미확정. 핵심 사이클과 무관. 도메인 복잡도 대비 프로토 가치 낮음 | Phase 2 후반 |
| F-09 스마트 리마인더 | Slack Webhook 설정, 스케줄러, DLQ 정책 등 인프라 준비 필요. 프로토에서 수동 확인으로 대체 가능 | Phase 3 |
| F-10 Slack 채널 연동 | F-09와 동일. notification 패키지 전체 Phase 3 |
| F-11 포인트 시스템 | 게이미피케이션은 운영 안정화 이후 도입이 적절 | Phase 3 |
| F-12/F-13 모듈화 | Phase 4 기획 항목 | Phase 4 |
| PAGE-06 벌점 이력 | F-07 제외에 따라 함께 제외 | F-07 추가 시 함께 |
| 알라딘 재고 API 연동 | TBD 블로커(T1). 스펙 확인 전 설계 불가 | Phase 2 (스펙 확인 후) |
| Slack 공지 발송 버튼 (PAGE-07) | Slack 연동 전체 제외 | Phase 3 |
| 벌점 부여 모달 (PAGE-07) | F-07 제외에 따라 함께 제외 | F-07 추가 시 함께 |
| 포인트·벌점 요약 (PAGE-08) | F-07, F-11 제외에 따라 함께 제외 | F-07, F-11 추가 시 함께 |
| QR 스캔 수령 방식 | 버튼 클릭 방식으로 단순화. QR 도입은 운영 경험 쌓은 후 결정 | 미정 |

---

## 6. 프로토타입 구현 순서

### 백엔드 구현 순서

의존 관계 기준: 공통 인프라 → 인증/회원 → 동호회 공통 → 무제 전용 핵심 → 관리자 기능.

```
[STEP 1] 공통 인프라 세팅
  - 패키지 구조 생성 (com.example.demo.*)
  - common: ApiResponse, GlobalExceptionHandler, ErrorCode, BusinessException 계층
  - application.properties: H2 dev 설정 확인, JPA DDL-auto 설정
  - Spring Security 기본 설정 (JWT 필터 자리 잡기)

[STEP 2] 인증 (F-00)
  - Member 엔티티 + MemberRepository
  - Google OAuth 2.0 연동 (OAuth2SuccessHandler → JWT 발급)
  - 이메일 Verify Code 발송/검증 (EmailVerifyService, In-Memory 저장소)
  - JwtTokenProvider + JwtAuthenticationFilter
  - AuthController: /auth/google, /auth/google/callback, /auth/verify-email, /auth/verify-email/confirm, /auth/logout

[STEP 3] 회원 프로필 (F-00-B)
  - MemberController: GET/PATCH /members/me
  - MemberController: GET /admin/members, PATCH /admin/members/{id}/role

[STEP 4] 동호회 공통 + 운영 일정 (F-06 최소)
  - Club, ClubMember 엔티티
  - ClubController: GET /clubs
  - Schedule 엔티티 (BOOK_REQUEST_DEADLINE, BOOK_REPORT_DEADLINE 타입만)
  - ScheduleController: GET /clubs/{clubId}/schedules
  - ScheduleController(admin): POST/PATCH/DELETE /admin/clubs/{clubId}/schedules

[STEP 5] 책 신청 (F-01) — 핵심 사이클 시작
  - BookRequest 엔티티
  - AladinApiClient: URL 파싱 외부 API 클라이언트
  - BookRequestService: 신청 등록, 예산 계산, ISBN 중복 검사, 잠금 처리
  - BookRequestController: POST /parse-url, GET/POST/PATCH/DELETE /book-requests
  - BookRequestController(admin): POST /book-requests/lock

[STEP 6] 주문 관리 (F-02 최소) + 수령 관리 (F-03 최소)
  - Order 엔티티 (또는 BookRequest 상태 확장 방식 검토)
  - BookOrderService: 주문서 생성(신청 취합), 상태 변경
  - 수령 처리: receive 엔드포인트 + 미수령자 조회
  - BookOrderController(admin): POST /generate, GET /{orderId}, PATCH /{orderId}/status, POST /{orderId}/arrive, GET /{orderId}/receive-status
  - BookOrderController: POST /{orderId}/receive, GET /orders/my

[STEP 7] 독후감 (F-04) — 핵심 사이클 종료
  - BookReport 엔티티
  - BookReportService: 제출, 수정, 마감일 기반 잠금(Schedule 조회)
  - BookReportController: GET/POST/PATCH /book-reports, GET /book-reports/my
  - BookReportController(admin): GET /admin/book-reports/status
```

### 프론트엔드 구현 순서

의존 관계 기준: 프로젝트 세팅 → 인증 플로우 → 공통 레이아웃 → 핵심 사이클 페이지 → 관리자 → 보조 페이지.

```
[STEP 1] 프로젝트 초기 세팅
  - Next.js 15 프로젝트 생성 (TypeScript, App Router, Tailwind CSS)
  - shadcn/ui 초기화 + 필수 컴포넌트 설치
    (Button, Input, Form, Dialog, Tabs, Card, Table, Badge, Toast/Sonner,
     Select, Textarea, InputOTP, Alert, DropdownMenu)
  - Magic UI 설치: ShimmerButton, AnimatedGridPattern, MagicCard, NumberTicker, BentoGrid
  - Framer Motion, TanStack Query, Zustand, Axios, react-hook-form + zod 설치
  - API 클라이언트 설정: lib/api/client.ts (axios 인스턴스 + JWT 인터셉터)
  - QueryProvider 설정
  - Route Guard 미들웨어 설정 (인증 상태 확인, /admin 역할 확인)
  - MSW 설치 + 기본 Mock 핸들러 구성 (백엔드 의존 없이 선행 구현용)

[STEP 2] 인증 플로우 (PAGE-01, PAGE-01-B)
  - PAGE-01: Google 로그인 버튼(ShimmerButton) + AnimatedGridPattern 배경
  - /auth/callback: 토큰 파싱 + emailVerified 분기 처리
  - PAGE-01-B: InputOTP 6자리 + 60초 타이머 + 오입력 횟수 카운트
  - 토큰 저장 (localStorage, MVP) + axios 인터셉터 주입

[STEP 3] 동호회 목록 (PAGE-02)
  - 동호회 카드(MagicCard) + 무제 진입
  - useQuery: 동호회 목록 API 연동

[STEP 4] 무제 대시보드 (PAGE-03)
  - 공통 레이아웃 (헤더 + 사이드 메뉴)
  - 잔여 예산 NumberTicker
  - 이번 달 마감 D-day 요약 (Book Request Deadline, Book Report Deadline)
  - BentoGrid 메뉴: 책 신청 / 독후감 / 내 정보
  - useQuery: 내 프로필 + 운영 일정 API 연동

[STEP 5] 책 신청 (PAGE-04) — 핵심 사이클 시작
  - 알라딘 URL 입력 + 파싱 버튼 (useMutation: parse-url)
  - 도서 정보 자동 입력 영역
  - 카테고리 드롭다운 + 제출 버튼
  - 잔여 예산 배너
  - 이번 달 내 신청 목록 (useQuery)
  - 잠금 상태 시 폼 비활성화 처리

[STEP 6] 독후감 제출 (PAGE-05) — 핵심 사이클 종료
  - 연결 도서 정보 표시 (내 당월 신청 조회)
  - 제목 + 본문 Textarea + 별점 선택
  - D-day 배너 (shadcn/ui Badge)
  - 마감 이후 읽기 전용 전환
  - useMutation: 제출/수정, useQuery: 내 독후감

[STEP 7] 관리자 대시보드 (PAGE-07)
  - 현황 카드 3개: 책 신청 현황 / 독후감 제출률 / 미수령 인원 (NumberTicker)
  - 월 선택 필터
  - 마감 잠금 버튼 (useMutation)
  - 주문서 생성 버튼 + 주문 상태 변경 드롭다운 (useMutation)
  - 도착 처리 버튼 + 미수령자 목록 (useQuery)
  - 독후감 미제출자 목록 (useQuery)
  - 운영 일정 등록 인라인 폼 (useMutation: schedules)

[STEP 8] 내 정보 (PAGE-08)
  - 프로필 수정 폼 (닉네임, 부서, Slack ID) + 저장 (useMutation)
  - Slack ID 미등록 경고 배너
  - 탭: 책 신청 이력 (useQuery) / 독후감 이력 (useQuery)
```

### 백엔드 ↔ 프론트엔드 병렬 진행 원칙

- STEP 1(인프라 세팅)은 양측 동시 진행.
- 백엔드 STEP 2(인증 API) 완료 전까지 프론트는 MSW Mock으로 인증 플로우 UI를 선행 구현.
- 백엔드 STEP 5(책 신청 API) 완료 전까지 프론트는 Mock으로 PAGE-04 선행 구현.
- E2E 테스트(Playwright) 핵심 시나리오 4가지(로그인, 책 신청, 독후감 제출, 관리자 현황 조회)는 백엔드 API 연동 완료 후 작성.
