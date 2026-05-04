# 18 — 일정 관리 통합 화면 (Schedule CRUD UI)

> 작성일: 2026-05-04
> 상태: 계획 단계
> 선행: `Schedule` REST API 완성 (v1 백엔드, [club/controller/ScheduleController.java](../../src/main/java/com/example/demo/club/controller/ScheduleController.java))

---

## 1. 왜 만들었나 (Problem)

- `Schedule` 도메인은 백엔드 인프라가 100% 깔려 있다 — 엔티티, REST API (`POST/PATCH/DELETE /api/v1/admin/clubs/{clubId}/schedules`), 권한 체크, `ClubBootstrap` 의 매월 자동 시드 (15일 = 책 신청 마감, 말일 = 독후감 마감).
- 그런데 **관리자가 마감일을 UI 에서 등록/수정/삭제할 화면이 없다**. 시드된 기본 마감일을 바꾸거나 새 일정을 추가하려면 DB 직접 INSERT 또는 dev seed 도구로만 가능.
- 결과적으로 매달 다른 마감일을 잡아야 하는 자연스러운 운영 흐름이 끊어져 있다 — 운영자가 매번 개발자에게 요청해야 한다.

## 2. 목표 (Goal)

- 관리자가 `/muje/admin/schedules` 통합 화면에서 일정 CRUD 를 모두 한다.
- 사용자 대시보드(MujePage) 의 "이번 달 일정" 카드와 BookReportPage 의 마감 표시는 변경 없이 그대로 동작.
- v1 은 **CRUD UI 만**. 자동 잠금 cron 과 캘린더 뷰는 별도 차회.

## 3. 정책 / 규칙 (Rules)

### 3-1. typeCode 화이트리스트 (v1 UI 노출 대상)
- `BOOK_REQUEST_DEADLINE` — 책 신청 마감 (표시용)
- `BOOK_REPORT_DEADLINE` — 독후감 제출 마감 (서비스에서 마감 검증에 사용)

`PHOTO_SHOOT`, `MONTHLY_MEETING` 은 정의만 되어 있으나 v1 UI 에서는 제외. 향후 v2 에서 활성화.

### 3-2. 동시 등록 정책
- 한 `(clubId, yearMonth, typeCode)` 조합당 1개 권장. **서버에서 강제하지 않음** (v1 한계).
- UI 에서 같은 typeCode 가 이미 있으면 시각적으로 경고하되 추가 가능.

### 3-3. 표시용 vs 게이트 — 의도적 분리
- **`BOOK_REQUEST_DEADLINE` 은 표시용**: 사용자에게 "이번 달 책 신청 마감일 = X일" 을 보여주는 알림 데이터일 뿐.
- 실제 신청 차단 게이트는 별개 테이블 `month_locks` (`MonthLockService.lock/unlock`). 관리자가 명시적으로 토글.
- 두 시스템은 의도적으로 분리 유지 — 마감일 변경이 자동으로 잠금을 유발하지 않아 운영 사고 위험 적음.
- v2 에서 자동 잠금 cron 도입 시 이 정책 재검토.

### 3-4. BOOK_REPORT_DEADLINE 다중 등록 시 동작
- `BookReportService.findDeadline()` 는 가장 빠른 날짜 1건을 반환 ([BookReportService.java:194](../../src/main/java/com/example/demo/club/untitled/service/BookReportService.java#L194)).
- 같은 월에 여러 개 박아도 가장 빠른 것이 마감으로 작동. UI 가이드만 v1.

### 3-5. 권한
- 모든 수정 엔드포인트는 관리자 (또는 DEVELOPER) 전용. `clubService.requireAdmin(clubId, memberId)` 로 검증.
- 비관리자 화면 진입 시 메뉴 노출 X. 직접 URL 호출 시 백엔드 403.

## 4. UX 흐름

### 진입
- MujePage 관리자 섹션 (책 신청 관리 / 도착 관리 옆) → "📅 일정 관리" 버튼 → `/muje/admin/schedules`.

### 화면 구성
1. **헤더**: 제목 "관리자 — 일정 관리" + ← 대시보드 버튼.
2. **월 선택**: `<input type="month">` (default = 이번 달). 변경 시 해당 월 일정 자동 로드.
3. **신규 등록 카드**:
   - typeCode `<select>`: BOOK_REQUEST_DEADLINE / BOOK_REPORT_DEADLINE.
   - date `<input type="date">`.
   - description `<textarea maxLength=200>` (선택).
   - "추가" 버튼.
4. **일정 리스트**:
   - 행: 라벨 뱃지 + date (한국식 + D-day) + description + 수정/삭제 버튼.
   - 수정 → 인라인 편집 모드 (date / description) + 저장/취소.
   - 삭제 → confirm() 후 실행.
5. **empty state**: "이 달 일정이 없습니다" + 신규 등록 안내.

## 5. API 표면

이미 완성된 백엔드 — 수정 없음. 프론트가 새로 호출.

```
GET    /api/v1/clubs/{clubId}/schedules?yearMonth=YYYY-MM   회원 (조회)
POST   /api/v1/admin/clubs/{clubId}/schedules               관리자 (생성)
PATCH  /api/v1/admin/clubs/{clubId}/schedules/{id}          관리자 (수정)
DELETE /api/v1/admin/clubs/{clubId}/schedules/{id}          관리자 (삭제)
```

DTO: `ScheduleRequest { typeCode, date, description }`, `ScheduleResponse { id, clubId, typeCode, date, description, yearMonth }`.

## 6. 수용 기준 (Acceptance)

- [ ] `/muje/admin/schedules` 진입 가능 (관리자만 메뉴 노출).
- [ ] 월 선택 변경 시 해당 월 일정 자동 로드.
- [ ] 신규 일정 등록 (typeCode select + date + description) 후 즉시 리스트 반영.
- [ ] 기존 일정 수정 / 삭제 후 즉시 리스트 반영.
- [ ] 변경 후 MujePage "이번 달 일정" 카드 / BookReportPage 마감 표시 즉시 반영 (react-query invalidate).
- [ ] 비관리자가 직접 URL 접근 시 백엔드 403.

## 7. 오픈 이슈 / 향후 계획

- **자동 잠금 cron**: BOOK_REQUEST_DEADLINE 도달 시 MonthLock 자동 잠금 + 알림 발송 ([14 문서](14-order-aladin-cart.md) 오픈 이슈와 연계). v2 에서 12 문서의 NotificationService 와 묶어 도입.
- **캘린더 뷰**: 일정이 월 2~3개 이상으로 늘어나면 리스트 + 캘린더 토글.
- **추가 typeCode 활성화**: PHOTO_SHOOT, MONTHLY_MEETING 등을 운영에서 실제 쓰게 되면 UI 에 노출.
- **`month_locks` ↔ `schedule[BOOK_REQUEST_DEADLINE]` 동기화 정책 재검토**: 자동 잠금 도입 시 두 데이터가 동시에 진실의 원천이 되면 정책 충돌 가능. 차회 작업.
- **중복 방지 서버 강제**: 같은 (club, yearMonth, typeCode) 중복 INSERT 차단. 운영 데이터 쌓이고 실수 잦으면 도입.
- **반복 일정**: 매월 동일 날짜 반복 등록은 `ClubBootstrap` 시드가 처리 중. UI 에서 별도 반복 옵션은 v2.
