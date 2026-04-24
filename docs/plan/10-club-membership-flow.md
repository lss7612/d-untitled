# 10 — 동호회 가입 / 승인 / 역할 변경 플로우

> 작성일: 2026-04-24
> 상태: v1 구현 완료 (무제는 한시적 자동 가입 유지)

---

## 1. 왜 만들었나 (Problem)

- 초기엔 "로그인만 하면 무제에 자동 합류" 로 동작 (MEMORY 의 `project_club_join_flow`). 데모용 편의.
- 실제 운영에서는 **여러 동호회가 공존** 하고, 새 회원이 들어올 때 관리자가 명단을 직접 수락/거절해야 한다.
- 자동 가입 상태로 두면 관리자 화면에서 "이 사람 누구?" 가 발생하고 동호회별 벌점/포인트 집계가 오염된다.

## 2. 목표 (Goal)

- 회원이 동호회를 **발견** 하고 **가입 신청** 을 명시적으로 보낼 수 있다.
- 관리자(클럽 ADMIN 또는 전역 DEVELOPER)가 신청을 **승인 / 거절** 한다.
- 승인된 회원의 **역할(MEMBER ↔ ADMIN)** 을 관리자가 변경할 수 있다.

## 3. 정책 / 규칙 (Rules)

- **클럽 내 역할(ClubRole)**: `MEMBER`, `ADMIN`. 클럽마다 독립.
- **전역 역할**: `DEVELOPER` (→ 자세한 정의는 `11-developer-role-and-whitelist.md`). DEVELOPER 는 모든 클럽의 관리자 엔드포인트를 통과.
- **중복 신청 방지**: 같은 `(member, club)` 에 대해 PENDING 이 1건만 허용. PENDING 인 상태에서 재신청은 무시(409).
- **탈퇴/강퇴**: v1 범위 밖. 일단 "승인된 회원 제거" 는 관리자가 DB 로 직접 처리.
- **무제(독서 동호회) 한시 예외**: 데모 기간 중엔 로그인 직후 무제에 자동 가입됨. v2 로 넘어가기 전 제거 예정 (아래 "오픈 이슈" 참조).

## 4. UX 흐름

### 4-1. 회원
1. `/home` (`AllClubsPage`) — 가입 가능 / 가입됨 / 신청 대기 3 가지 상태의 타일을 본다.
2. 가입 가능 타일 → [가입 신청] 버튼 → 토스트로 "신청이 접수되었습니다" 확인.
3. 신청 대기 중인 타일은 "대기 중" 상태 뱃지.
4. 승인되면 다음 접속 시 `내 동호회` 섹션에 뜬다. (푸시/메일 알림은 12 문서 참고)

### 4-2. 관리자
1. `MujePage` 관리자 섹션의 **회원 관리** 타일 → `/muje/admin/members` (`AdminClubMembersPage`).
2. 상단 "가입 신청" 탭: PENDING 목록 → [승인] / [거절].
3. "회원" 탭: 현재 멤버 + 역할 뱃지 → [역할 변경] 버튼으로 MEMBER ↔ ADMIN 토글.

## 5. API 표면

### 회원
- `GET  /api/v1/clubs/my` — 내 가입 동호회 목록.
- `GET  /api/v1/clubs` (공개 목록) — 가입 가능 동호회. *(상세 엔드포인트는 `ClubController` 참조)*
- `POST /api/v1/clubs/{clubId}/join-requests` — 가입 신청.

### 관리자 (ClubRole.ADMIN 또는 DEVELOPER)
- `GET  /api/v1/admin/clubs/{clubId}/join-requests` — PENDING 목록.
- `POST /api/v1/admin/clubs/{clubId}/join-requests/{memberId}/approve`
- `POST /api/v1/admin/clubs/{clubId}/join-requests/{memberId}/reject`
- `GET  /api/v1/admin/clubs/{clubId}/members` — 멤버 전원.
- `PATCH /api/v1/admin/clubs/{clubId}/members/{memberId}/role` — 역할 변경.

## 6. 수용 기준 (Acceptance)

- [x] 비가입 회원이 가입 신청 → PENDING 생성, UI 뱃지 "대기 중".
- [x] 관리자가 승인 → 해당 회원이 `my clubs` 에 노출.
- [x] 관리자가 거절 → PENDING 이 REJECTED 로 전환, 재신청 가능.
- [x] PENDING 중 재신청 409.
- [x] 역할 변경으로 ADMIN 권한이 즉시 반영됨 (로그인 토큰 만료 없이).
- [ ] 무제 자동 가입 제거 후 "가입 신청 필수" 로 전환 (오픈 이슈).
- [x] 비관리자의 관리자 엔드포인트 호출 403.

## 7. 오픈 이슈 / 향후 계획

- **무제 자동 가입 제거**: `ClubBootstrap` 에서 `ensureDefaultMembership` 류 로직을 제거해야 한다. 기존 테스트 계정 영향 범위 확인 후 전환.
- **탈퇴/강퇴 플로우**: 관리자 UI + 이력 보존 (탈퇴 사유·날짜).
- **초대 링크**: 현재 "발견 → 신청" 모델뿐. 폐쇄형 동호회를 위한 초대 토큰은 로드맵 밖.
- **알림 연계**: 승인/거절 시 회원에게 `NotificationService` 를 통한 통지 (12 문서).
