# 09 — 보유책 페이지 마감 + 쿼터 기반 제한풀기 리팩터

> 작성일: 2026-04-23
> 상태: **계획 단계 (다음 세션 인계용)**
> 선행: `docs/plan/08-book-catalog-exemption.md` (v1 구현 완료)

---

## 1. 배경

08번 문서로 Phase B (책 카탈로그 + 중복 체크 + 제한풀기) 가 구현되어 있다. 현재 상태:

- `books` 테이블 + `exemption_granted_at` 컬럼으로 **책 단위 영구 해제** 방식.
- 제한풀기가 승인되면 그 책은 이후 누구든 다시 신청 가능 (무제한).
- 프론트에 `OwnedBooksPage`, `AdminBookExemptionsPage`, `AdminExemptBooksPage` 가 존재.

이 세션에서 남은 두 가지 작업:

1. **보유책 페이지(UX) 마감** — 검색/페이징/정렬 등 실사용 품질 보강.
2. **제한풀기 모델을 "영구 해제" → "쿼터(N권 추가 허용)" 로 리팩터** — 승인되면 해당 책을 **N권까지만** 더 구매할 수 있도록.

---

## 2. 목표

### 2-1. 보유책 페이지 (OwnedBooksPage)
- 모든 클럽 멤버가 접근 가능 (현재도 접근은 됨).
- 제목/저자 기반 부분 일치 검색 (서버 사이드).
- 목록 정렬: 최근 등록순 기본, 옵션으로 제목순.
- exemption 상태(`exemption_granted_at`/쿼터 잔량) 표시.
- 모바일 레이아웃 다듬기.

### 2-2. 쿼터 기반 제한풀기
- 관리자가 승인 시 **추가 허용 권수(quota)** 를 지정.
- 예: "해피 니팅" 에 quota = 2 를 승인하면 그 책을 **2권 더** 신청 가능.
- quota 가 소진되면 다시 중복 차단이 걸린다.
- 승인 이력(누가 몇 권 승인했는지) 보존.

---

## 3. 설계

### 3-1. 스키마 변경

#### `books` 테이블
- `exemption_granted_at` **제거**(혹은 레거시로 유지하되 쿼리에서 사용 안 함).
- 신규 컬럼:
  - `exemption_quota INT NOT NULL DEFAULT 0` — 누적 승인된 추가 권수.
  - `exemption_used INT NOT NULL DEFAULT 0` — 그 중 이미 RECEIVED 된 권수.
- "신청 가능 여부" = `exemption_quota - exemption_used > 0` 또는 카탈로그에 그 `(title, author)` 가 아직 없는 경우.

#### `book_exemption_request` 테이블
- `granted_quota INT NULL` — APPROVED 시 관리자가 지정한 권수.
- 기존 `status` 그대로 (PENDING/APPROVED/REJECTED).

### 3-2. 카운터 증감 로직 (핵심)

`BookRequestService` 상태 전이에서:
- **신청 생성 (PENDING → 카탈로그 중복 시)**:
  - `exemption_quota - exemption_used > 0` 이면 통과 (차감은 아직 X).
  - 그렇지 않으면 기존처럼 `DUPLICATE_BOOK` 409.
- **RECEIVED 전이 시**:
  - 카탈로그의 기존 책이면 `exemption_used += 1` (쿼터 소진).
  - 카탈로그에 없던 책이면 기존 로직대로 `upsertCatalogEntry`.
- **환불/취소 (RECEIVED 이후)**: 범위에서 제외 — 보수적으로 `exemption_used` 감소 안 시킴.

**동시성 주의**: `exemption_used += 1` 은 `@Query UPDATE ... WHERE id = :id` 로 원자적 증가 권장 (또는 비관적 락).

### 3-3. 승인 엔드포인트 변경
- `POST /api/v1/admin/clubs/{clubId}/book-exemptions/{id}/approve`
  - Body: `{ quota: number }` — 최소 1.
  - 동작:
    - `book.exemption_quota += quota`
    - `request.granted_quota = quota`
    - `request.status = APPROVED`
- 기존 `exemption_granted_at` 관련 컬럼/필드 제거.

### 3-4. 마이그레이션
- `ddl-auto=update` 이므로 신규 컬럼은 자동 추가.
- **수동 처리 필요**:
  - 기존 `exemption_granted_at` 이 NOT NULL 인 행 → `exemption_quota = 999` (사실상 무제한) 로 백필.
  - 그 후 컬럼 삭제는 수동 `ALTER TABLE DROP COLUMN`.

---

## 4. 변경 파일

### 백엔드
- `domain/Book.java` — 컬럼 2개 추가, `exemption_granted_at` 제거.
- `domain/BookExemptionRequest.java` — `grantedQuota` 필드.
- `repository/BookRepository.java` — 원자적 `incrementUsed(bookId)` 쿼리.
- `service/BookExemptionService.java` — approve() 가 quota 받음.
- `service/BookCatalogService.java` (혹은 `BookRequestService`) — RECEIVED 전이 시 used 증가.
- `service/BookRequestService.java` — 중복 체크를 `quota - used > 0` 기준으로.
- `dto/` — `ApproveBookExemptionRequest { int quota }`, `BookResponse` 에 quota/used 필드.
- `controller/BookExemptionAdminController.java` — approve 가 body 받도록.

### 프론트엔드
- `api/bookExemptions.ts` — approve(id, quota).
- `api/books.ts` / `BookResponse` 타입 — quota/used 필드.
- `pages/AdminBookExemptionsPage.tsx` — approve 버튼에 권수 입력 모달.
- `pages/AdminExemptBooksPage.tsx` — quota/used 표시, "남은 권수" 컬럼.
- `pages/OwnedBooksPage.tsx` — 검색/정렬/페이징 + 잔량 뱃지.
- `pages/BookRequestPage.tsx` — 중복 경고 카드에서 "남은 쿼터 X권" 메시지.

### 문서
- `docs/backend/06-erd.md` — books/book_exemption_request 섹션 갱신.
- 본 문서 (`09-...md`) 를 참조 추가.

---

## 5. 수용 기준

- [ ] 관리자가 approve 시 quota(정수, ≥1) 입력 필수.
- [ ] 승인 후 해당 책으로 quota 만큼 신청/수령이 정상 진행.
- [ ] quota 초과하는 신청은 다시 `DUPLICATE_BOOK` 409.
- [ ] `OwnedBooksPage` 에서 "제목 또는 저자" 부분 일치 검색 서버 요청.
- [ ] 각 책 행에 quota/used (예: "추가 허용 2/3") 표시.
- [ ] 기존 영구 해제 데이터가 깨지지 않고 quota=999 로 마이그레이션.
- [ ] 프론트 `npx tsc --noEmit` 통과.
- [ ] 백엔드 `./gradlew test` 통과.

---

## 6. 리스크 / 오픈 이슈

- **quota 소진 후 재승인 UX**: 같은 책에 대해 추가 쿼터를 주려면 새 request 를 PENDING 으로 띄워야 하나, 아니면 관리자가 `AdminExemptBooksPage` 에서 직접 +N 할 수 있게 해야 하나? → 권장: 후자(별도 `POST .../books/{id}/quota` 엔드포인트). 0번째 구현에서는 전자만 해도 충분.
- **used 감소 정책**: 환불/취소 케이스가 앞으로 생기면 정책 재검토.
- **동시성**: 순간적으로 여러 회원이 동시에 같은 책 신청 → used 초과 가능. DB UPDATE ... WHERE used < quota 로 조건부 증가하거나 비관적 락 필수.

---

## 7. 작업 순서 (권장)

1. 스키마/엔티티 변경 (Book, BookExemptionRequest) + 리포지토리 원자 쿼리.
2. BookExemptionService.approve(quota) 구현 + 컨트롤러/DTO.
3. BookRequestService 중복 체크 로직을 quota 기반으로.
4. RECEIVED 전이 시 used 증가 (원자적).
5. 프론트 approve 모달, 목록 표시.
6. OwnedBooksPage 검색/페이징.
7. 레거시 `exemption_granted_at` 데이터 백필 → 컬럼 drop.
8. ERD/문서 갱신.
