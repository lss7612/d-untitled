# 11 — DEVELOPER 전역 역할 + 이메일 화이트리스트

> 작성일: 2026-04-24
> 상태: v1 구현 완료

---

## 1. 왜 만들었나 (Problem)

- 플랫폼 개발자(= 나)가 모든 동호회를 돌며 진단·데이터 수선을 해야 하는데, 클럽 `ADMIN` 은 해당 클럽에만 유효하다.
- 반대로, **사내 직원만 로그인 가능** 해야 하므로 Google OAuth 가 성공해도 도메인/이메일 필터가 필요.
- 두 요구를 분리된 레이어로 풀었다: **전역 역할** + **가입 허용 화이트리스트**.

## 2. 목표 (Goal)

- 한 명 이상의 `DEVELOPER` 가 **클럽과 무관하게** 모든 관리자 엔드포인트를 통과.
- 등록되지 않은 이메일은 OAuth 이후 `/auth/email-verify` 단계에서 **가입 거부**.
- DEVELOPER 가 런타임에 화이트리스트를 편집 (재배포 없이).

## 3. 정책 / 규칙 (Rules)

- **DEVELOPER 식별**: `Member.role = DEVELOPER` (전역 enum). 최초 시드에서 특정 이메일 1명 지정.
- **권한 경계**:
  - `ClubService.requireAdmin(member, club)` 이 `DEVELOPER` 는 무조건 true 반환.
  - 일반 ADMIN 은 자기 클럽만. 다른 클럽 관리 API 호출 시 403.
  - DEVELOPER 전용 엔드포인트: `/api/v1/developer/**` — 동적 설정 편집 등.
- **화이트리스트**:
  - 저장소: `app_config` 테이블의 `type = WHITE_LIST` 행. `contents` 는 JSON 문자열(이메일 배열).
  - 매칭: lowercase exact match.
  - 비어있으면 **모두 허용** (개발 편의) — 운영 투입 전 반드시 최소 1개 값으로 시드.
- **신규 가입 차단 지점**: `OAuth2SuccessHandler` 에서 화이트리스트 조회 → 불일치 시 별도 에러 페이지로 리디렉션.

## 4. UX 흐름

### 4-1. 일반 회원
- Google 로그인 → 이메일 Verify Code → 화이트리스트 통과 시 `/home`.
- 통과 실패 시: `/login` 에 에러 배너 (예: "가입이 허용되지 않은 계정입니다").

### 4-2. DEVELOPER
- 상단 프로필에서 `/developer/whitelist` (`DeveloperWhitelistPage`) 진입.
- 현재 등록된 이메일 뱃지 리스트 + 입력창.
- [추가] / 각 뱃지의 [x] 로 즉시 저장 (PUT).
- 일반 관리자 UI 에서는 이 페이지가 안 보임.

## 5. API 표면

- `GET  /api/v1/developer/config/{type}` — `type = WHITE_LIST` 등 조회.
- `PUT  /api/v1/developer/config/{type}` — 업데이트 (JSON body `{ contents: "…" }`).

DEVELOPER 가 아닌 호출은 403. (컨트롤러에서 `member.role == DEVELOPER` 체크.)

## 6. 수용 기준 (Acceptance)

- [x] DEVELOPER 계정이 임의 클럽의 `/api/v1/admin/clubs/**` 를 200 으로 통과.
- [x] ADMIN 계정이 본인 클럽 외 관리 API 호출 시 403.
- [x] 화이트리스트 미등록 계정이 OAuth 성공 후에도 세션 미발급.
- [x] `/developer/whitelist` 페이지에서 추가/삭제 즉시 반영.
- [ ] 화이트리스트 도메인 단위 매칭 (`@doubleugames.com` 전체 허용) — 로드맵.
- [x] 비DEVELOPER의 `/api/v1/developer/**` 호출 403.

## 7. 오픈 이슈 / 향후 계획

- **DEVELOPER 승격 플로우**: 현재는 시드에서만 지정. 관리 UI 는 향후.
- **감사 로그**: 화이트리스트 변경 이력을 별도 테이블로 남기는 건 로드맵.
- **도메인 매칭**: `@회사도메인` 와일드카드 지원 검토.
- **알림**: 가입 거부 시 해당 요청자에게 안내 메일 (NotificationService 연계).
