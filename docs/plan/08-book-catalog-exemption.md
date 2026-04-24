# 08 — 책 카탈로그 + 중복 체크 + 제한풀기(Exemption)

> 작성일: 2026-04-23
> 대상 동호회: **무제 (독서)**
> 상태: v1 구현 완료 (시드 데이터 10건, 수동 INSERT)

---

## 1. 왜 만드는가 (Problem)

- 현재 `BookRequestService` 는 **같은 회원이 같은 달에 동일 ISBN** 을 재신청하는 케이스만 차단한다 (`member_id + target_month + isbn`).
- 즉 **다른 회원이 클럽에 이미 들어와 있는 책을 또 신청**하는 경우를 잡지 못한다. 실제로 매달 같은 책이 중복 구매되어 운영자가 시트에서 수작업으로 걸러내고 있었다.
- 반대로, **인기 있는 책은 여러 권 사두고 싶다** 는 요구도 있다. 자동 차단만 있으면 이 합리적인 예외를 처리할 방법이 없다.

## 2. 목표 (Goal)

1. 클럽이 보유한 책의 **카탈로그** (`books` 테이블) 를 갖는다.
2. 회원이 책 신청 시, 카탈로그에 같은 `(title, author)` 가 이미 있으면 **409 Conflict + duplicateBookId** 로 차단.
3. 회원은 해당 책에 대해 **제한풀기(exemption) 신청** 을 할 수 있다 (사유 선택 입력).
4. 관리자(ClubRole.ADMIN / DEVELOPER) 는 pending 신청을 보고 **승인 / 거절**.
5. 승인되면 **그 책 1권에 대해 영구적으로** 중복 체크에서 제외된다 (`books.exemption_granted_at`).

## 3. 설계 규칙 (Design Rules)

### 중복 판정 키
- **`(club_id, normalized_title, normalized_author)`** — ISBN 은 사용하지 않는다. 시드 데이터 (구글시트 보유책 리스트) 에 ISBN 이 없는 행이 많기 때문.
- `normalized_*` = NFKC + 공백 collapse + lowercase. 엔티티 `@PrePersist / @PreUpdate` 에서 항상 채운다 (→ `BookNames.normalize()`).
- Unique index `uk_books_club_title_author` on `(club_id, normalized_title, normalized_author)`.

### 제한풀기 = 책 자체의 속성
- `books.exemption_granted_at TIMESTAMP NULL` — 승인되는 순간 `NOW()` 로 set.
- 중복 쿼리에서 `exemptionGrantedAt == null` 인 행만 차단 대상.
- **회원별 예외가 아니라 책별 예외** — 한 번 승인하면 이후 누구든 그 책을 다시 신청할 수 있다.

### 제한풀기 신청은 별도 엔티티
- `book_exemption_request` — 누가, 왜, 언제 신청했는지 기록.
- Status: `PENDING / APPROVED / REJECTED`.
- 같은 `(club_id, book_id)` 에 대해 PENDING 은 동시에 1건만 (재신청은 REJECTED 처리된 이후에만).
- APPROVED 처리 시: `books.exemption_granted_at = NOW()` + request 의 `processed_at / processed_by_member_id` 기록.

### 에러 응답 스키마 확장
기존 `BusinessException` 은 `{ success, message, status }` 만 반환했다. 프론트가 "제한풀기 신청" 버튼을 띄우려면 **`duplicateBookId` 도 필요** → `BusinessException` 에 `details: Map<String, Object>` 필드를 추가하고 `GlobalExceptionHandler` 가 응답 body 에 실어 보낸다.

응답 예:
```json
{
  "success": false,
  "status": 409,
  "message": "이미 보유 중인 책입니다. 필요 시 제한풀기 신청을 해주세요.",
  "details": {
    "code": "DUPLICATE_BOOK",
    "duplicateBookId": 5,
    "duplicateBookTitle": "해피 니팅"
  }
}
```

## 4. 변경 파일 요약

### 백엔드
| 구분 | 파일 |
|---|---|
| 엔티티 | `club/untitled/domain/Book.java`, `BookExemptionRequest.java` |
| 유틸 | `club/untitled/util/BookNames.java` |
| 리포지토리 | `BookRepository`, `BookExemptionRequestRepository` |
| 공통 예외 | `common/exception/BusinessException.java` (details 필드), `GlobalExceptionHandler` (응답 반영) |
| 서비스 | `BookRequestService.create()` 에 clubwide duplicate check 주입, 새 `BookExemptionService` |
| DTO | `CreateBookExemptionRequest`, `BookExemptionResponse` |
| 컨트롤러 | `BookExemptionController` (회원용), `BookExemptionAdminController` (관리자용) |

### 프론트엔드
| 구분 | 파일 |
|---|---|
| API | `api/client.ts` (ApiError.details 부착), `api/bookExemptions.ts` |
| 훅 | `hooks/useBookExemptions.ts` |
| 페이지 | `pages/BookRequestPage.tsx` (중복 경고 카드), `pages/AdminBookExemptionsPage.tsx` (신규) |
| 라우트 | `App.tsx` `/muje/admin/book-exemptions`, `MujePage.tsx` 관리자 메뉴 타일 |

### 데이터
- 시드: 2026-04 월 보유책 리스트 기준 10건, MySQL MCP 로 직접 INSERT (`club_id = 1`).

## 5. API

### 회원용
- `POST /api/v1/clubs/{clubId}/book-requests` — 기존 엔드포인트. 카탈로그 중복 시 **409** + `details.duplicateBookId`.
- `POST /api/v1/clubs/{clubId}/book-exemptions`
  - Body: `{ bookId: number, reason?: string }`
  - 응답: `BookExemptionResponse`
  - 같은 책에 PENDING 이 이미 있으면 409.
  - 이미 exemption 이 승인된 책이면 400.

### 관리자용 (ClubRole.ADMIN 또는 DEVELOPER)
- `GET  /api/v1/admin/clubs/{clubId}/book-exemptions` — PENDING 목록
- `POST /api/v1/admin/clubs/{clubId}/book-exemptions/{id}/approve`
- `POST /api/v1/admin/clubs/{clubId}/book-exemptions/{id}/reject`

## 6. 수용 기준 (Acceptance)

- [x] 카탈로그에 있는 책과 동일한 `(normalized_title, normalized_author)` 로 신청 → 409, UI 에 경고 카드 + [제한풀기 신청] 버튼.
- [x] 공백/대소문자 변형 (`해피  니팅`, `해피 니팅 `) 에도 걸린다.
- [x] 카탈로그에 없는 새 책 → 정상 신청.
- [x] 제한풀기 신청 후 동일 책 재신청 → 409 (이미 대기 중).
- [x] 관리자 승인 시 `books.exemption_granted_at` 에 타임스탬프가 박히고, 이후 **다른 회원** 이 동일 책 신청 → 정상 통과.
- [x] 관리자 거절 시 `books.exemption_granted_at` 은 그대로, request 만 REJECTED.
- [x] 일반 회원이 관리자 엔드포인트 호출 → 403.
- [x] 프론트 타입체크 `npx tsc --noEmit` 통과.

## 7. 보류 / 리스크

- **저자 표기 흔들림**: `"김나리"` vs `"정성을 뜨는 421 김나리"` 같은 케이스는 normalize 만으로 못 잡는다. 시드 10건 한정으로는 문제없지만, 전체 자동 import 시 별도 dedupe 로직 필요.
- **시리즈물 동일 제목**: `"시든 꽃에 눈물을 1/2/3"` 은 숫자 덕분에 각각 별도 책으로 인식 — **의도된 동작**.
- **ISBN 기반 per-member-month 체크는 유지**: 중복체크가 두 레이어로 운영됨 (카탈로그 + 회원별 월별).
- **Google Sheets 자동 import**: 시트에 재무 요약이 섞여 있어 현재 파서로는 위험. 본 문서 범위에서는 **수동 10건** 만.
