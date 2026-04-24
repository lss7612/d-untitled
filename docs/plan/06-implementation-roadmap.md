# 구현 로드맵 (Implementation Roadmap)

> 이 문서는 백엔드 / 프론트엔드 / 기획 세 팀이 Phase 1~4를 구체화하고 구현하기 위한 단계별 실행 계획서다.

---

## 1. 전체 타임라인

```
[Phase 1] 인증 & 회원 관리
──────────────────────────────────────────────────────
기획    ▶ [결정 필요] 항목 1~3 확정 (JWT 만료, 잠금 시간, Slack ID 미등록 정책)
백엔드  ▶ 도메인 패키지 구조 설계 → Member / Club / ClubMember 엔티티 → 인증 API
프론트  ▶ 프로젝트 초기 설정 → PAGE-01 / PAGE-01-B / PAGE-02

[Phase 2] 무제 핵심 기능 (Google Sheets 완전 대체)
──────────────────────────────────────────────────────
기획    ▶ [결정 필요] 항목 4~8 확정 (외서 환율, QR 수령, 독후감 글자 수, 벌점 가중치, 도서 대여 알림 기준)
백엔드  ▶ 무제 전용 엔티티 설계 → F-01~F-08 API 구현 (순차)
프론트  ▶ PAGE-03 ~ PAGE-08 구현 (백엔드 API 준비 순서에 맞춰 병렬)

[Phase 3] 알림 & 게이미피케이션
──────────────────────────────────────────────────────
기획    ▶ [결정 필요] 항목 9~12 확정 (D-1 명단 공개, 수령 장소 필드, DLQ 도입, 알림 이력 보존 기간)
백엔드  ▶ Slack Webhook 연동 → NT-01~NT-09 스케줄러 구현 → 포인트 API (F-09~F-11)
프론트  ▶ 포인트 랭킹 UI → 알림 이력 뷰 (관리자)

[Phase 4] 모듈 템플릿화
──────────────────────────────────────────────────────
기획    ▶ 타 동호회 온보딩 시나리오 정의 → 동호회 생성 위저드 5단계 UX 설계
백엔드  ▶ ClubPlugin 인터페이스 설계 → ReadingClubPlugin 분리 → 신규 동호회 확장 검증
프론트  ▶ 동호회 생성 위저드 UI (F-13)
```

---

## 2. Phase별 팀 역할 분담표

### Phase 1 — 인증 & 회원 관리

| 구분 | 선행 조건 | 작업 항목 |
|------|-----------|-----------|
| 기획 | 없음 | JWT 만료 시간 / 이메일 잠금 시간 / Slack ID 미등록 알림 여부 3가지 확정 |
| 백엔드 | 기획 결정 항목 1~3 확정 후 착수 | 패키지 구조 설계 → `Member` / `Club` / `ClubMember` 엔티티 → F-00 / F-00-B API |
| 프론트 | 백엔드 `/auth/*` API 스펙 확정 | 프로젝트 초기 설정 → PAGE-01 (Google OAuth 버튼) → PAGE-01-B (6자리 코드 입력) → PAGE-02 (동호회 목록) |

- 백엔드가 Google OAuth 콜백 처리 완료 전, 프론트는 Mock API로 PAGE-01-B UI를 선행 구현한다.

---

### Phase 2 — 무제 핵심 기능

| 구분 | 선행 조건 | 작업 항목 |
|------|-----------|-----------|
| 기획 | Phase 1 완료 | 외서 환율 정책 / QR 수령 여부 / 독후감 최소 글자 수 / 벌점 가중치 / 도서 대여 알림 기준일 확정 |
| 백엔드 | 기획 결정 항목 4~8 확정 후 착수 | F-01 (책 신청) → F-02 (주문) → F-03 (인포 수령) → F-04 (독후감) → F-05 (촬영 일정) → F-06 (운영 일정) → F-07 (벌점) → F-08 (도서 대여) |
| 프론트 | 각 Feature API 스펙 확정 즉시 병렬 착수 | PAGE-03 (대시보드) → PAGE-04 (책 신청) → PAGE-05 (독후감) → PAGE-06 (벌점 이력) → PAGE-07 (관리자) → PAGE-08 (내 정보) |

- F-01 알라딘 URL 파싱 API는 외부 의존성이 있어 백엔드 착수 우선순위 1위로 처리한다.
- F-02 주문 상태 자동화는 알라딘 Open API 스펙 확인 완료 후 설계를 확정한다.

---

### Phase 3 — 알림 & 게이미피케이션

| 구분 | 선행 조건 | 작업 항목 |
|------|-----------|-----------|
| 기획 | Phase 2 완료 | D-1 미제출자 명단 공개 여부 / 수령 장소 필드 추가 여부 / DLQ 도입 여부 / 알림 이력 보존 기간 확정 |
| 백엔드 | 기획 결정 항목 9~12 확정 후 착수 | Slack Incoming Webhook 설정 → NT-01~NT-09 발송 로직 → Spring Scheduler 크론 설정 → F-09 리마인더 → F-10 채널 연동 → F-11 포인트 API |
| 프론트 | Phase 2 완료 | 포인트 랭킹 뷰 (PAGE-08 확장) → 관리자 알림 이력 뷰 (PAGE-07 확장) |

---

### Phase 4 — 모듈 템플릿화

| 구분 | 선행 조건 | 작업 항목 |
|------|-----------|-----------|
| 기획 | Phase 3 완료 | 타 동호회 온보딩 시나리오 정의 → 동호회 생성 위저드 5단계 UX 설계 → 신규 동호회 운영 규칙 커스텀 항목 목록 정의 |
| 백엔드 | 기획 설계 완료 | `ClubPlugin` 인터페이스 설계 → `ReadingClubPlugin`으로 무제 전용 도메인 분리 → `BaseClubPlugin` 구현 가이드 작성 |
| 프론트 | 기획 위저드 UX 확정 | 동호회 생성 위저드 5단계 UI (F-13) |

---

## 3. 기획 잔여 결정 사항

> 구현 착수 전 반드시 결정해야 할 항목을 우선순위 순으로 정렬한다.

### [P0] Phase 1 착수 전 — 결정 완료

| 번호 | 항목 | 결정 | 관련 기능 |
|------|------|------|-----------|
| 1 | JWT 만료 시간 및 Refresh Token 도입 여부 | **Sliding 1시간** (API 호출 시 갱신, 비활동 1시간이면 만료, Refresh Token 미도입) | F-00 인증 |
| 2 | 이메일 오입력 잠금 시간 | **5분** | F-00 인증, PAGE-01-B |
| 3 | Slack ID 미등록 회원 DM 실패 시 관리자 알림 여부 | **로그만 기록** (채널 @멘션으로 대체) | F-10 채널 연동, NT 전체 |

### [P0] 인프라 결정

| 번호 | 항목 | 결정 |
|------|------|------|
| I-1 | 이메일 발송 SMTP | **Gmail SMTP (회사 Workspace 계정 + 앱 비밀번호)**. `application.properties`에 `spring.mail.*` 설정. ~45명 규모에서 일일 한도(500) 충분. Phase 3에서 SES/Workspace SMTP API 전환 검토 |
| I-2 | 프론트 프레임워크 | **Vite + React 18 + TypeScript + React Router v6**. Next.js 미사용 |
| I-3 | 최초 ADMIN 임명 | DB 시드 (마이그레이션 또는 직접 SQL). 별도 UI 없음 |

### [P1] Phase 2 착수 전 — 기능 설계에 직접 영향

| 번호 | 항목 | 결정 / 선택지 | 관련 기능 |
|------|------|---------------|-----------|
| 4 | 외서 환율 처리 방식 | **신청 시점 환율 1개 임시 적용 (확정)** — `BookRequest.exchangeRate`에 동결 | F-01 책 신청 |
| 5 | 인포 수령 확인 방식 | 버튼 클릭 유지 / QR 스캔 도입 | F-03 인포 수령, PAGE-04 |
| 6 | 독후감 본문 최소 글자 수 | 100자 / 200자 / 제한 없음 | F-04 독후감, PAGE-05 |
| 7 | 벌점 점수 가중치 차등 적용 여부 | 항목 무관 모두 -1점 / 항목별 차등 | F-07 벌점, 도메인 모델 |
| 8 | 도서 대여 미반납 알림 기준일 | 7일 / 14일 / 30일 | F-08 도서 대여, NT-09 |

### [P2] Phase 3 착수 전 — 알림 정책 영향

| 번호 | 항목 | 선택지 | 관련 기능 |
|------|------|--------|-----------|
| 9 | D-1 리마인더 미제출자 명단 공개 여부 | 채널 공개 / 개인 DM만 발송 | NT-01 / NT-02 |
| 10 | 인포 수령 알림 수령 장소 필드 추가 여부 | 장소 필드 추가 / 고정 문구 사용 | NT-04 |
| 11 | Dead Letter Queue 도입 여부 | 초기 미도입(재시도 3회 후 로그) / DLQ 도입 | NT 실패 처리 정책 |
| 12 | 알림 발송 이력 보존 기간 | 30일 / 90일 | Notification 도메인 |

### [TBD] 별도 확인 필요 (외부 의존성)

| 번호 | 항목 | 내용 |
|------|------|------|
| T1 | 알라딘 Open API 재고 확인 스펙 | 폴링 방식 / Webhook 지원 여부 확인 필요 → F-02 주문 자동화 설계 블로커 |
| T2 | 장기 미반납 운영 규정 | 몇 일 초과를 '장기'로 볼 것인지 운영진 합의 필요 → F-08 설계 블로커 |
| T3 | 알림 실패 수신 이메일 수신자 확정 | 관리자 이메일 주소 확정 필요 → NT 실패 처리 정책 완성 블로커 |

---

## 4. 백엔드 도메인 설계 방향

### 패키지 구조 원칙

> 정답은 [docs/backend/02-package-structure.md](../backend/02-package-structure.md)에 있다. 본 섹션은 그 요약본이다.

`common`, `auth`, `user`, `club`, `notification`을 top-level로 분리한다. `auth`와 `notification`은 횡단 관심사이므로 도메인 패키지(`user`, `club`)와 독립적으로 위치한다. 무제 전용 모듈은 `club.untitled` 하위에 격리한다.

```
com.example.demo
├── common/                 # 전역 공통 (config, exception, response, util)
├── auth/                   # 인증 — Google OAuth + 이메일 2-step + JWT
├── user/                   # 회원 공통 도메인 — Member 엔티티
├── club/                   # 동호회 공통 코어 + 무제 전용 모듈
│   ├── domain/             # 공통: Club, ClubMember, Schedule, Penalty, PenaltyDispute, Point
│   ├── repository/
│   ├── service/
│   ├── dto/
│   ├── controller/
│   └── untitled/           # 무제 전용 모듈
│       ├── domain/         # BookRequest, BookReport, PhotoSchedule, LibraryBook, BookLending
│       ├── repository/
│       ├── service/
│       ├── dto/
│       ├── controller/
│       └── external/       # AladinApiClient 등
└── notification/           # 알림 인프라 — Notification 엔티티 + Slack 발송 + 스케줄러
```

각 도메인은 `domain / repository / service / dto / controller` 5단 레이어로 구성한다.

### 도메인 ↔ 기능 스펙 연결

| 패키지 위치 | 소속 도메인 | 커버하는 기능 | 기획 Phase |
|-------------|------------|---------------|------------|
| `user` | Member | F-00, F-00-B (인증, 프로필) | Phase 1 |
| `auth` | (도메인 없음, 인증 처리) | F-00 OAuth + 이메일 2-step | Phase 1 |
| `club.domain` | Club, ClubMember, Schedule | F-06 (운영 일정), PAGE-02 (동호회 목록) | Phase 1~2 |
| `club.domain` | Penalty, PenaltyDispute, Point | F-07 (벌점), F-11 (포인트) | Phase 2~3 |
| `notification` | Notification | NT-01~NT-09 알림 전체 | Phase 3 |
| `club.untitled` | BookRequest | F-01 (책 신청), F-02 (주문) | Phase 2 |
| `club.untitled` | BookReport | F-04 (독후감) | Phase 2 |
| `club.untitled` | PhotoSchedule | F-05 (촬영 일정) | Phase 2 |
| `club.untitled` | LibraryBook, BookLending | F-08 (도서 대여) | Phase 2 |

### Phase 4 모듈화 방향

- Phase 2 구현 시점부터 `club.untitled` 내 비즈니스 로직을 `ClubPlugin` 인터페이스로 추상화할 수 있도록 의존성 방향을 단방향으로 유지한다.
- `club.domain` 공통 레이어는 `club.untitled`를 직접 참조하지 않는다. `untitled`가 `club.domain`을 참조하는 방향만 허용한다.
- `Schedule`/`Penalty`의 타입은 Core enum 대신 **String typeCode**로 저장하고, 각 `ClubPlugin`이 자신이 지원하는 typeCode set을 SPI로 선언한다. 신규 동호회 추가 시 Core enum 수정이 발생하지 않도록 한다.
- Phase 4에서 `club.untitled`를 `ReadingClubPlugin`으로 분리하고, 신규 동호회는 `BaseClubPlugin`을 구현해 독립 모듈로 추가하는 방식으로 확장한다.

---

## 5. 프론트엔드 기술 구성 계획

### 기술 스택

| 역할 | 채택 기술 |
|------|-----------|
| UI 프레임워크 | **Vite + React 18+ (TypeScript)** — Next.js 미사용 (확정) |
| 컴포넌트 | shadcn/ui |
| 애니메이션/고급 UI | Magic UI |
| 서버 상태 관리 | TanStack Query (React Query v5) |
| E2E 테스트 | Playwright |
| 라우팅 | React Router v6 |

### 페이지별 구현 접근 방향

| 페이지 | 핵심 구현 포인트 | TanStack Query 활용 |
|--------|-----------------|---------------------|
| PAGE-01 (로그인) | Google OAuth 리다이렉트 처리. 비인증 사용자만 접근 가능 | 없음 (단순 리다이렉트) |
| PAGE-01-B (이메일 인증) | 6자리 숫자 입력 필드 + 60초 쿨타임 타이머 + 오입력 횟수 카운트 | `useMutation` (코드 검증 API) |
| PAGE-02 (동호회 목록) | 동호회 카드 목록. Phase 4 이후 다수 동호회 확장 대비 카드 컴포넌트 재사용 설계 | `useQuery` (동호회 목록) |
| PAGE-03 (무제 대시보드) | 이번 달 잔여 예산 + 주요 일정 3개 요약 카드. Skeleton UI 필수 | `useQuery` (대시보드 요약) |
| PAGE-04 (책 신청) | 알라딘 URL 입력 → 파싱 → 자동 입력. 잔여 예산 실시간 표시. 잠금 시 폼 비활성화 | `useMutation` (URL 파싱, 신청), `useQuery` (내 신청 목록) |
| PAGE-05 (독후감 제출) | 텍스트 에디터 + 별점 + 마감 D-day 배너. 마감 이후 읽기 전용 전환 | `useMutation` (제출), `useQuery` (내 독후감) |
| PAGE-06 (벌점 이력) | 벌점 목록 + 이의 제기 모달. 상태별 배지(PENDING/ACCEPTED/REJECTED) | `useQuery` (벌점 이력), `useMutation` (이의 제기) |
| PAGE-07 (관리자 대시보드) | 월 선택 필터 + 현황 카드 3개 + 벌점 부여 모달 + 슬랙 발송 모달. ADMIN 전용 | `useQuery` (월별 현황), `useMutation` (벌점 부여, 슬랙 발송) |
| PAGE-08 (내 정보) | 프로필 수정 폼 + 탭 메뉴 3개. Slack ID 미등록 경고 배너 | `useQuery` (프로필, 이력), `useMutation` (프로필 저장) |

### 공통 설계 규칙

- **인증 가드**: 모든 페이지는 라우터 레벨에서 인증 상태를 확인한다. 미인증 시 PAGE-01로 리다이렉트한다.
- **역할 가드**: PAGE-07은 `ADMIN` 역할 확인 후 진입을 허용한다. 역할 미보유 시 403 전용 페이지를 렌더링한다.
- **낙관적 업데이트**: 책 신청 취소, 수령 완료 처리 등 사용자 응답성이 중요한 액션에 TanStack Query 낙관적 업데이트를 적용한다.
- **Mock API 선행 개발**: 백엔드 API 미완성 구간은 MSW(Mock Service Worker) 기반 Mock으로 UI를 선행 구현한다.
- **E2E 테스트 우선 시나리오 (Playwright)**: 로그인 플로우, 책 신청 정상/오류 플로우, 독후감 제출 플로우, 관리자 벌점 부여 플로우 4가지를 핵심으로 우선 작성한다.

---

## 실제 구현 기록 (로드맵 대비)

초기 로드맵 이후 실제 구현된 부가 모듈. 상세는 각 문서 참조.

- **Phase 2 (무제 핵심)**
  - 책 신청 + 마감 잠금 + 합산 주문서 → `14-order-aladin-cart.md`
  - 독후감 + 피드 + 미제출 관리 → `15-book-report-feed.md`
  - 월별 예산 + 나눔 → `13-budget-module.md`
- **Phase 3 (운영 자동화)**
  - 책 카탈로그 + 중복 체크 + 제한풀기 → `08-book-catalog-exemption.md`, `09-owned-books-and-quota-exemption.md`
  - 알림 추상화 (Logging v1 완료, Slack v2 예정) → `12-notification-abstraction.md`
- **Phase 3.5 (권한/운영 확장)**
  - 가입 신청/승인/역할 변경 → `10-club-membership-flow.md`
  - DEVELOPER 전역 역할 + 화이트리스트 → `11-developer-role-and-whitelist.md`
  - 동적 설정(AppConfig) → `16-app-config.md`
  - 개발 지원(시드/진단) → `17-dev-tooling.md`
