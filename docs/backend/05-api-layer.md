# API 레이어 구조

---

## 1. Controller 패키지 구성 및 URL 설계

모든 REST API는 `/api/v1` prefix를 공통으로 사용한다. 관리자 전용 기능은 `/api/v1/admin` prefix로 분리하여 Spring Security에서 Role 기반 접근 제어를 적용한다.

```
[인증]
POST   /api/v1/auth/google                                    → OAuth 2.0 시작
GET    /api/v1/auth/google/callback                           → OAuth 콜백
POST   /api/v1/auth/verify-email                              → 이메일 코드 발송
POST   /api/v1/auth/verify-email/confirm                      → 이메일 코드 검증
POST   /api/v1/auth/logout                                    → 로그아웃

[회원]
GET    /api/v1/members/me                                     → 내 프로필
PATCH  /api/v1/members/me                                     → 내 프로필 수정

[동호회 공통]
GET    /api/v1/clubs/{clubId}/schedules                       → 운영 일정 목록
GET    /api/v1/clubs/{clubId}/penalties/my                    → 내 벌점 이력
POST   /api/v1/clubs/{clubId}/penalties/{id}/dispute          → 이의 제기
GET    /api/v1/clubs/{clubId}/points/my                       → 내 포인트 이력
GET    /api/v1/clubs/{clubId}/points/ranking                  → 월간 랭킹

[무제 전용]
POST   /api/v1/clubs/{clubId}/books/parse-url                 → 알라딘 URL 파싱
GET    /api/v1/clubs/{clubId}/book-requests                   → 책 신청 목록
POST   /api/v1/clubs/{clubId}/book-requests                   → 책 신청 등록
PATCH  /api/v1/clubs/{clubId}/book-requests/{id}              → 책 신청 수정
DELETE /api/v1/clubs/{clubId}/book-requests/{id}              → 책 신청 취소
GET    /api/v1/clubs/{clubId}/book-reports                    → 독후감 목록
POST   /api/v1/clubs/{clubId}/book-reports                    → 독후감 제출
PATCH  /api/v1/clubs/{clubId}/book-reports/{id}               → 독후감 수정
GET    /api/v1/clubs/{clubId}/photo-schedules                 → 촬영 일정 목록
GET    /api/v1/clubs/{clubId}/library                         → 책장 도서 목록
POST   /api/v1/clubs/{clubId}/library/{bookId}/borrow         → 대여 신청
POST   /api/v1/clubs/{clubId}/library/{bookId}/return         → 반납 처리
GET    /api/v1/clubs/{clubId}/library/my-borrows              → 내 대여 이력
GET    /api/v1/clubs/{clubId}/orders/my                       → 내 주문 상태

[관리자]
GET    /api/v1/admin/members                                  → 전체 회원 목록
PATCH  /api/v1/admin/members/{memberId}/role                  → 권한 변경
POST   /api/v1/admin/clubs/{clubId}/schedules                 → 일정 등록
PATCH  /api/v1/admin/clubs/{clubId}/schedules/{id}            → 일정 수정
DELETE /api/v1/admin/clubs/{clubId}/schedules/{id}            → 일정 삭제
GET    /api/v1/admin/clubs/{clubId}/penalties                 → 전체 벌점 이력
POST   /api/v1/admin/clubs/{clubId}/penalties                 → 벌점 수동 부여
DELETE /api/v1/admin/clubs/{clubId}/penalties/{id}            → 벌점 취소
PATCH  /api/v1/admin/clubs/{clubId}/penalties/{id}/dispute    → 이의 제기 처리
POST   /api/v1/admin/clubs/{clubId}/points                    → 포인트 수동 부여
POST   /api/v1/admin/clubs/{clubId}/book-requests/lock        → 신청 마감 잠금
POST   /api/v1/admin/clubs/{clubId}/orders/generate           → 주문서 생성
GET    /api/v1/admin/clubs/{clubId}/orders/{orderId}          → 주문서 조회
PATCH  /api/v1/admin/clubs/{clubId}/orders/{orderId}/status   → 주문 상태 변경
POST   /api/v1/admin/clubs/{clubId}/orders/{orderId}/arrive   → 도착 처리
GET    /api/v1/admin/clubs/{clubId}/orders/{orderId}/receive-status → 미수령자 조회
GET    /api/v1/admin/clubs/{clubId}/book-reports/status       → 독후감 제출 현황
POST   /api/v1/admin/clubs/{clubId}/photo-schedules           → 촬영 일정 등록
PATCH  /api/v1/admin/clubs/{clubId}/photo-schedules/{id}      → 촬영 일정 수정
DELETE /api/v1/admin/clubs/{clubId}/photo-schedules/{id}      → 촬영 일정 삭제
POST   /api/v1/admin/clubs/{clubId}/library                   → 도서 등록
GET    /api/v1/admin/clubs/{clubId}/library/overdue           → 장기 미반납 목록
```

---

## 2. 공통 응답 포맷 (`ApiResponse<T>`)

모든 API 응답은 `ApiResponse<T>` 래퍼를 통해 일관된 포맷을 보장한다.

```java
// com.example.demo.common.response.ApiResponse
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String errorCode       // 실패 시에만 포함
) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> created(T data) { ... }
    public static ApiResponse<Void> error(String errorCode, String message) { ... }
}
```

**성공 응답 예시**

```json
{
  "success": true,
  "data": { "id": 42, "status": "PENDING" },
  "message": null,
  "errorCode": null
}
```

**실패 응답 예시**

```json
{
  "success": false,
  "data": null,
  "message": "동일 월에 이미 신청된 ISBN입니다.",
  "errorCode": "BOOK_REQUEST_DUPLICATE_ISBN"
}
```

---

## 3. 예외 처리 전략 (`@ControllerAdvice`)

`GlobalExceptionHandler`를 `com.example.demo.common.exception` 패키지에 단일 진입점으로 배치한다.

**예외 계층 구조**

```
RuntimeException
└── BusinessException            # 비즈니스 규칙 위반 (4xx 계열)
    ├── NotFoundException        # 404 — 리소스 없음
    ├── ForbiddenException       # 403 — 권한 없음
    ├── ConflictException        # 409 — 중복 / 상태 충돌
    └── BadRequestException      # 400 — 잘못된 입력
```

**처리 전략**

| 예외 유형 | HTTP 상태 | 처리 위치 |
|-----------|-----------|-----------|
| `BusinessException` 하위 | 각 예외에 매핑된 코드 | `GlobalExceptionHandler` |
| `MethodArgumentNotValidException` | 400 | `GlobalExceptionHandler` (Bean Validation 실패) |
| `AccessDeniedException` | 403 | Spring Security 핸들러 |
| `AuthenticationException` | 401 | Spring Security 핸들러 |
| `Exception` (미처리) | 500 | `GlobalExceptionHandler` (로그 기록 후 generic 응답) |

`ErrorCode` enum에 에러 코드 문자열(`errorCode`)과 기본 메시지, HTTP 상태를 함께 정의하여 `BusinessException` 생성 시 코드만 지정하면 응답이 자동 구성되도록 설계한다.

```java
// com.example.demo.common.exception.ErrorCode
public enum ErrorCode {
    BOOK_REQUEST_DUPLICATE_ISBN(HttpStatus.CONFLICT, "동일 월에 이미 신청된 ISBN입니다."),
    BOOK_REQUEST_LOCKED(HttpStatus.FORBIDDEN, "마감 잠금 이후 수정/취소가 불가합니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    // ...
    ;
}
```
