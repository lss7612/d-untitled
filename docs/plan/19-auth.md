# 19 — 인증 (Google OAuth + 이메일 2-step Verify)

> 작성일: 2026-05-04
> 상태: v1 구현 완료 (현 코드 기준 사후 문서화)

---

## 1. 왜 만들었나 (Problem)

- 사내 동호회 플랫폼은 **사내 직원만** 사용해야 한다. 외부 인사가 아무 구글 계정으로 가입해 들어오면 안 됨.
- 단순 회원가입 폼은 운영 부담 (비밀번호 관리, 잊어버림, 보안). 사내는 이미 Google Workspace 가 있으니 OAuth 로 시작하는 게 자연스러움.
- 다만 **OAuth 만으로는 부족** — 임직원 전환/퇴사로 사내 도메인 외 계정도 OAuth 통과 가능. 한 단계 더 검증 필요.

## 2. 목표 (Goal)

1. **Google OAuth 2.0** 으로 1차 인증.
2. 이메일 화이트리스트 검증 + **이메일 verify code (6자리)** 로 2차 인증.
3. **JWT** 발급 → 이후 모든 API 요청에 Authorization 헤더 사용.
4. 화이트리스트 미등록 / 잘못된 코드 / 잠금된 계정 명확히 거부.

## 3. 정책 / 규칙 (Rules)

### 3-1. 1차 — Google OAuth 2.0
- Spring Security `oauth2Login` + Google provider.
- 콜백 URI: `/api/v1/auth/google/callback`.
- 성공 시 [OAuth2SuccessHandler](../../src/main/java/com/example/demo/auth/security/OAuth2SuccessHandler.java) 가 회원 upsert + JWT 발급 + 프론트로 토큰 redirect.
- 발급되는 JWT 의 `emailVerified` 클레임이 `false` 면 일부 API 만 통과 (이메일 verify 단계 진입).

### 3-2. 2차 — 이메일 verify code
- `POST /api/v1/auth/email/send-code` — 본인 이메일로 6자리 코드 발송 ([EmailVerifyService](../../src/main/java/com/example/demo/auth/email/EmailVerifyService.java)).
- `POST /api/v1/auth/email/verify` — 코드 검증 → 통과 시 `emailVerified=true` 클레임 박힌 새 JWT 발급.
- 코드는 메모리 저장소 ([VerifyCodeStore](../../src/main/java/com/example/demo/auth/email/VerifyCodeStore.java)) — 만료 시간 + 5회 오입력 시 잠금.

### 3-3. 화이트리스트
- AppConfig 의 `WHITE_LIST` key (JSON 배열) — DEVELOPER 가 `/developer/whitelist` 페이지에서 관리 (16, 11 문서).
- OAuth 콜백에서 화이트리스트에 없으면 verify 단계 진입 거부.

### 3-4. JWT
- 시크릿: `JWT_SECRET` 환경변수.
- 만료: 1시간 (`jwt.expiration-ms=3600000`).
- 클레임: `sub` (memberId), `emailVerified` (boolean).
- Refresh 없음 (v1) — 만료 시 재로그인.

### 3-5. 발신 메일
- Spring Mail (`smtp.gmail.com:587`) + 앱 비밀번호.
- 발송 실패 시 사용자에게 "잠시 후 다시 시도" 메시지. 큰 장애로 처리 안 함.

## 4. UX 흐름

```
[Login Page] /login
   ↓ "Google 로 로그인" 클릭
Google 동의 → 콜백 (/api/v1/auth/google/callback)
   ↓ JWT (emailVerified=false) 발급 + redirect
[AuthCallbackPage] /auth/callback?token=...
   ↓ JWT 저장 → emailVerified false 면 redirect
[EmailVerifyPage] /auth/email-verify
   ↓ "코드 발송" 클릭 → 메일 수신
   ↓ 6자리 입력 → verify
   ↓ JWT (emailVerified=true) 재발급
[AllClubsPage] /home → 동호회 가입 신청
```

## 5. API 표면

### 인증
- `GET  /oauth2/authorization/google` — OAuth 시작 (Spring Security 자동 매핑)
- `GET  /api/v1/auth/google/callback` — 콜백 (Spring Security 자동 매핑, success/failure handler 가 처리)
- `POST /api/v1/auth/email/send-code` — body: `{ email: string }`
- `POST /api/v1/auth/email/verify` — body: `{ email: string, code: string }`

### 사용자
- `GET  /api/v1/members/me` — JWT 의 `sub` 로 본인 정보 조회

## 6. 수용 기준 (Acceptance)

- [x] Google OAuth 끝까지 동작 (login → callback → JWT 발급).
- [x] 화이트리스트 미등록 이메일은 verify 진입 거부.
- [x] 코드 5회 오입력 시 계정 잠금 (HTTP 429).
- [x] verify 성공 시 emailVerified=true JWT 재발급.
- [x] JWT 만료 후 401 → 프론트가 로그인 페이지로 리다이렉트.
- [x] 비인증 API 호출 401, 인증되었으나 권한 부족 시 403.

## 7. 오픈 이슈 / 향후 계획

- **Refresh Token**: 현재는 1시간 만료 후 재로그인. 사용자 피로도 누적되면 도입 고려.
- **도메인 단위 화이트리스트**: 현재 이메일 주소 단위 — `@doubledown.com` 같은 도메인 매칭 추가 (11 문서 오픈 이슈와 동일).
- **멀티 디바이스 / 세션 강제 종료**: 현재 토큰 무효화는 만료뿐. 운영 사고 시 강제 logout 경로 필요.
- **2FA (실제 OTP)**: 이메일 verify 보다 강력한 OTP/TOTP 도입은 운영 부담 큼. 사내 보안 정책 변경 시 검토.
- **계정 비활성화**: 퇴사자 계정의 즉시 차단 흐름. 현재는 화이트리스트에서 제거하는 수동 방식.
