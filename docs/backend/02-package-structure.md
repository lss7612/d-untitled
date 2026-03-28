# 패키지 구조 설계

> Java 21 / Spring Boot 4.0.4 기반 백엔드 패키지 구조 설계 문서.

---

루트 패키지 `com.example.demo` 하위를 도메인 중심으로 구성한다.
공통 인프라(`common`, `auth`, `notification`)와 도메인 레이어(`user`, `club`)를 명확히 분리한다.
무제 전용 모듈은 `club.untitled` 하위 패키지에 격리하여, 이후 동호회 추가 시 `club.{clubType}` 패턴으로 병렬 확장할 수 있도록 한다.

```
com.example.demo
│
├── common/                          # 전역 공통 유틸리티
│   ├── config/                      # Spring, Security, JPA 설정 클래스
│   ├── exception/                   # 글로벌 예외 정의 및 핸들러
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ErrorCode.java
│   │   └── BusinessException.java
│   ├── response/                    # 공통 API 응답 포맷
│   │   └── ApiResponse.java
│   └── util/                        # 범용 유틸 (날짜 변환, YearMonth 헬퍼 등)
│
├── auth/                            # 인증 도메인 (Google OAuth 2.0 + 이메일 2-step)
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── EmailVerifyService.java
│   ├── dto/
│   ├── security/                    # Spring Security 설정, JWT 필터
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── OAuth2SuccessHandler.java
│   └── repository/
│       └── EmailVerifyCodeRepository.java   # Redis or In-Memory 저장소
│
├── user/                            # 회원 공통 도메인
│   ├── domain/
│   │   └── Member.java              # @Entity — 플랫폼 전체 회원
│   ├── repository/
│   │   └── MemberRepository.java
│   ├── service/
│   │   └── MemberService.java
│   ├── dto/
│   └── controller/
│       └── MemberController.java    # /api/v1/members
│
├── club/                            # 동호회 공통 코어 도메인
│   ├── domain/
│   │   ├── Club.java                # @Entity — 동호회
│   │   ├── ClubMember.java          # @Entity — 동호회 회원 매핑 (role 포함)
│   │   ├── Schedule.java            # @Entity — 운영 일정
│   │   ├── Penalty.java             # @Entity — 벌점
│   │   ├── PenaltyDispute.java      # @Entity — 벌점 이의 제기
│   │   └── Point.java               # @Entity — 포인트
│   ├── repository/
│   ├── service/
│   │   ├── ClubService.java
│   │   ├── ClubMemberService.java
│   │   ├── ScheduleService.java
│   │   ├── PenaltyService.java
│   │   └── PointService.java
│   ├── dto/
│   ├── controller/
│   │   ├── ClubController.java      # /api/v1/clubs
│   │   ├── ScheduleController.java  # /api/v1/clubs/{clubId}/schedules
│   │   ├── PenaltyController.java   # /api/v1/clubs/{clubId}/penalties
│   │   └── PointController.java     # /api/v1/clubs/{clubId}/points
│   └── untitled/                    # 무제 (독서 동호회) 전용 모듈
│       ├── domain/
│       │   ├── BookRequest.java     # @Entity — 책 신청
│       │   ├── BookReport.java      # @Entity — 독후감
│       │   ├── PhotoSchedule.java   # @Entity — 촬영 일정
│       │   ├── LibraryBook.java     # @Entity — 책장 도서
│       │   └── BookLending.java     # @Entity — 도서 대여
│       ├── repository/
│       ├── service/
│       │   ├── BookRequestService.java
│       │   ├── BookReportService.java
│       │   ├── PhotoScheduleService.java
│       │   └── BookLendingService.java
│       ├── dto/
│       ├── controller/
│       │   ├── BookRequestController.java   # /api/v1/clubs/{clubId}/book-requests
│       │   ├── BookReportController.java    # /api/v1/clubs/{clubId}/book-reports
│       │   ├── PhotoScheduleController.java # /api/v1/clubs/{clubId}/photo-schedules
│       │   └── BookLendingController.java   # /api/v1/clubs/{clubId}/library
│       └── external/
│           └── AladinApiClient.java         # 알라딘 URL 파싱 외부 API 클라이언트
│
└── notification/                    # 알림 공통 인프라
    ├── domain/
    │   └── Notification.java        # @Entity — 알림 발송 이력
    ├── repository/
    │   └── NotificationRepository.java
    ├── service/
    │   ├── NotificationService.java         # 발송 진입점 (채널 분기)
    │   └── SlackNotificationSender.java     # Slack Webhook/DM 발송 구현체
    ├── scheduler/
    │   └── NotificationScheduler.java       # @Scheduled 기반 스케줄 알림
    └── dto/
```
