# Magic UI MCP 조사 및 프론트엔드 구현 계획

> 작성일: 2026-03-28
> 대상 프로젝트: untitled (사내 동호회 관리 플랫폼 — 무제 독서 동호회)
> 참고 페이지: PAGE-01 ~ PAGE-08
>
> ⚠️ **프레임워크 최종 결정**: 본 문서의 "Next.js 15 권장" 내용은 stale이다.
> 최종 결정은 **Vite + React 18 + TypeScript + React Router v6**이며, 실제 코드도 이 구조로 구현되어 있다.
> 정답은 [docs/plan/06-implementation-roadmap.md](../plan/06-implementation-roadmap.md) [P0] 인프라 결정 표(I-2)를 따른다.
> Magic UI 컴포넌트 조사 내용 자체는 유효하므로 참고용으로 유지한다.

---

## 1. Magic UI MCP 개요

### Magic UI란?

Magic UI는 React + Tailwind CSS + Framer Motion 기반의 **애니메이션 특화 UI 컴포넌트 라이브러리**다. shadcn/ui가 기반 디자인 시스템을 제공한다면, Magic UI는 그 위에 **시각적 임팩트가 높은 인터랙션 컴포넌트**를 추가로 제공하는 레이어 역할을 한다.

- 공식 사이트: https://magicui.design
- GitHub: https://github.com/magicuidesign/magicui
- 라이선스: MIT

**핵심 특징:**

- Framer Motion 기반 부드러운 애니메이션 컴포넌트 (Animated Beam, Border Beam, Shimmer Button 등)
- shadcn/ui CLI와 동일한 방식으로 컴포넌트 단위 설치 (`npx shadcn@latest add`)
- 자체 레지스트리 URL을 통해 shadcn/ui 레지스트리 호환 방식으로 배포됨
- 컴포넌트 소스가 직접 프로젝트에 복사되어 자유롭게 수정 가능

### Magic UI MCP란?

MCP(Model Context Protocol)는 Anthropic이 정의한 표준 프로토콜로, AI 모델이 외부 도구 및 데이터 소스와 상호작용할 수 있게 하는 인터페이스다.

**Magic UI MCP**는 Magic UI 팀이 제공하는 MCP 서버로, Claude Code 등 MCP 호환 AI 클라이언트에서 다음을 가능하게 한다:

- 자연어 프롬프트로 Magic UI 컴포넌트를 프로젝트에 자동 설치
- 컴포넌트 목록 조회, 사용 예시 검색
- 컴포넌트 선택 → 설치 명령 자동 실행의 워크플로우 자동화

MCP 서버 형태: **로컬 stdio 기반 MCP 서버** (npm 패키지로 배포)

- 참고: https://magicui.design/docs/mcp
- npm: `@magicui/mcp` (패키지명은 확인 필요)

### shadcn/ui와의 관계 및 차이점

| 구분 | shadcn/ui | Magic UI |
|------|-----------|----------|
| 역할 | 기반 디자인 시스템 | 애니메이션/비주얼 특화 확장 레이어 |
| 컴포넌트 성격 | Button, Input, Dialog 등 범용 UI 원소 | Shimmer Button, Animated Beam, Marquee 등 시각 효과 중심 |
| 설치 방식 | `npx shadcn@latest add <component>` | `npx shadcn@latest add <magicui-registry-url>` |
| 의존성 | Radix UI, Tailwind CSS | shadcn/ui + Framer Motion |
| 커스터마이징 | 소스 직접 복사 방식 | 동일 (소스 직접 복사) |
| 사용 목적 | 기능적 UI 구성 요소 | 랜딩 페이지, 대시보드 비주얼 강화 |

Magic UI는 shadcn/ui를 **전제 조건**으로 한다. shadcn/ui 초기화 이후 Magic UI 컴포넌트를 추가하는 구조다.

---

## 2. Claude Code에서 사용하는 방법

### 설치 및 MCP 서버 등록

**사전 조건:**

1. shadcn/ui가 초기화된 React/Next.js 프로젝트
2. Node.js 18 이상
3. Claude Code 설치

**MCP 서버 등록 방법 (Claude Code CLI):**

```bash
# Claude Code 설정에 Magic UI MCP 서버 추가
claude mcp add magicui -- npx @magicui/mcp
```

또는 `.claude/settings.local.json` (또는 전역 설정 `~/.claude/settings.json`)에 직접 등록:

```json
{
  "mcpServers": {
    "magicui": {
      "command": "npx",
      "args": ["-y", "@magicui/mcp"]
    }
  }
}
```

> 참고: MCP 서버 등록 방법은 https://magicui.design/docs/mcp 에서 최신 가이드를 확인할 것. 패키지명과 명령어는 변경될 수 있음.

### 실제 사용 흐름

MCP 서버가 등록된 상태에서 Claude Code 세션 내에서 자연어로 지시:

**예시 프롬프트:**

```
Magic UI의 ShimmerButton 컴포넌트를 설치하고 로그인 페이지의 Google 로그인 버튼에 적용해줘.
```

```
Magic UI에서 대시보드 카드에 어울리는 애니메이션 컴포넌트를 추천하고 설치해줘.
```

**내부 동작 흐름:**

1. Claude Code가 MCP 서버를 통해 Magic UI 컴포넌트 레지스트리 조회
2. 적합한 컴포넌트 선택 후 `npx shadcn@latest add [registry-url]` 실행
3. 컴포넌트 소스가 `components/magicui/` 디렉토리에 복사
4. Claude가 해당 컴포넌트를 import하여 타겟 파일에 적용

**MCP 없이 수동 설치하는 경우 (fallback):**

```bash
# 예: AnimatedBeam 컴포넌트 설치
npx shadcn@latest add "https://magicui.design/r/animated-beam"

# 예: ShimmerButton 설치
npx shadcn@latest add "https://magicui.design/r/shimmer-button"
```

---

## 3. 이 프로젝트에서 활용 전략

### PAGE별 Magic UI 활용 여부 분류

| PAGE | 페이지명 | Magic UI 활용도 | 주요 Magic UI 컴포넌트 후보 |
|------|----------|-----------------|----------------------------|
| PAGE-01 | 로그인 | 높음 | ShimmerButton (Google 로그인), AnimatedGridPattern (배경) |
| PAGE-01-B | 이메일 Verify Code | 낮음 | — (shadcn/ui InputOTP으로 충분) |
| PAGE-02 | 동호회 목록 | 중간 | MagicCard (동호회 카드 hover 효과) |
| PAGE-03 | 무제 대시보드 | 높음 | NumberTicker (잔여 예산, 포인트), AnimatedList (일정 요약), BentoGrid (메뉴 카드) |
| PAGE-04 | 책 신청 | 낮음 | — (폼 위주, shadcn/ui로 충분) |
| PAGE-05 | 독후감 제출 | 낮음 | — (텍스트 에디터 위주) |
| PAGE-06 | 벌점 이력 | 낮음 | — (테이블/목록 위주) |
| PAGE-07 | 관리자 대시보드 | 높음 | NumberTicker (현황 수치), BentoGrid (요약 카드 3개), Marquee (미제출자 스크롤 목록) |
| PAGE-08 | 내 정보 | 낮음 | — (shadcn/ui Tabs + Card로 충분) |

### shadcn/ui 기본 컴포넌트로 처리할 것 vs. Magic UI로 처리할 것

**shadcn/ui 기본 컴포넌트로 처리:**

- 모든 폼 입력: `Input`, `Textarea`, `Select`, `Form`, `Label`
- 대화상자 및 모달: `Dialog`, `AlertDialog`, `Sheet`
- 네비게이션: `NavigationMenu`, `Tabs`, `Breadcrumb`
- 피드백 요소: `Toast` / `Sonner`, `Badge`, `Alert`
- 데이터 표시: `Table`, `Card` (기본형), `Separator`
- 액션 버튼 (일반): `Button` (shadcn/ui 기본)
- OTP 입력 (PAGE-01-B): `InputOTP`

**Magic UI로 처리:**

- **로그인 버튼** (PAGE-01): `ShimmerButton` — 첫 진입 인상을 강하게
- **로그인 페이지 배경** (PAGE-01): `AnimatedGridPattern` 또는 `DotPattern`
- **동호회 카드** (PAGE-02): `MagicCard` — hover 시 빛 번짐 효과
- **잔여 예산/포인트 숫자** (PAGE-03, PAGE-08): `NumberTicker` — 숫자 롤링 애니메이션
- **대시보드 메뉴 버튼** (PAGE-03): `BentoGrid` + `BentoCard`
- **이번 달 일정 요약** (PAGE-03): `AnimatedList`
- **관리자 현황 카드 3개** (PAGE-07): `NumberTicker` + `BentoGrid`
- **D-day 배너** (PAGE-05): `BorderBeam` 적용한 강조 카드
- **벌점 상태 배지** (PAGE-06): `AnimatedShinyText`

### 적용 우선순위

프론트엔드 구현 우선순위는 사용자 임팩트와 백엔드 Phase 의존성을 기준으로 정렬:

**1순위 — 인증 플로우 (PAGE-01, PAGE-01-B)**
- 모든 페이지의 진입 전제 조건
- Google OAuth redirect 처리, JWT 저장, 2-step 흐름 확립
- Magic UI: ShimmerButton, AnimatedGridPattern 적용

**2순위 — 무제 대시보드 (PAGE-03)**
- 사용자가 매번 진입하는 허브 페이지
- NumberTicker, BentoGrid, AnimatedList로 시각적 품질 확보

**3순위 — 핵심 워크플로우 (PAGE-04, PAGE-05)**
- Phase 2 백엔드(F-01, F-04) 완료 후 연동
- 폼 위주라 Magic UI 비중 낮음; shadcn/ui + 유효성 검사에 집중

**4순위 — 동호회 목록 (PAGE-02) + 내 정보 (PAGE-08)**
- MagicCard 적용으로 목록 페이지 품질 향상
- PAGE-08은 탭 기반 이력 조회 구현 집중

**5순위 — 관리자 대시보드 (PAGE-07)**
- ADMIN 역할 사용자만 접근; 내부 도구 성격
- BentoGrid + NumberTicker로 현황 파악 용이하게

**6순위 — 벌점 이력 (PAGE-06)**
- 단순 목록/모달 구조; Magic UI 최소 적용

---

## 4. 프론트엔드 구현 전 체크리스트

### 프레임워크 선택: Next.js vs Vite + React

| 기준 | Next.js 15 (App Router) | Vite + React 18 |
|------|-------------------------|-----------------|
| SSR/SSG 필요성 | 낮음 (내부 인증 필요 페이지 위주) | 충분 |
| Google OAuth redirect 처리 | App Router Route Handler로 처리 가능 | React Router + Express BFF 필요 |
| 배포 복잡도 | Vercel 원클릭 or 컨테이너 배포 | 정적 파일 + 별도 프록시 설정 |
| shadcn/ui 호환성 | 공식 지원 | 공식 지원 |
| **권장** | **Next.js 15** | — |

> 이유: Google OAuth callback URL 처리를 위해 서버 사이드 Route Handler가 있는 편이 구현이 간단하고, 향후 SSR 전환 시 유연성이 높다.

### 프로젝트 초기 세팅 항목

```
[ ] 1. Next.js 15 프로젝트 생성 (TypeScript, App Router, Tailwind CSS)
[ ] 2. shadcn/ui 초기화 (npx shadcn@latest init)
[ ] 3. 필수 shadcn/ui 컴포넌트 일괄 설치
        Button, Input, Form, Dialog, Sheet, Tabs, Card,
        Table, Badge, Toast/Sonner, Select, Textarea,
        InputOTP, Alert, Separator, DropdownMenu
[ ] 4. Magic UI MCP 서버 등록 (.claude/settings.local.json)
[ ] 5. Magic UI 컴포넌트 설치 (MCP 또는 npx 수동)
        ShimmerButton, AnimatedGridPattern, MagicCard,
        NumberTicker, BentoGrid, AnimatedList, BorderBeam
[ ] 6. Framer Motion 설치 (Magic UI 의존성)
        npm install framer-motion
[ ] 7. TanStack Query 설치 및 Provider 설정
        npm install @tanstack/react-query @tanstack/react-query-devtools
[ ] 8. 전역 상태 관리 라이브러리 선택 (Zustand 권장)
        npm install zustand
[ ] 9. HTTP 클라이언트 설치
        npm install axios  (또는 ky)
[ ] 10. 폼 유효성 검사 라이브러리
        npm install react-hook-form zod @hookform/resolvers
[ ] 11. 코드 품질 도구
        ESLint + Prettier + Husky + lint-staged
[ ] 12. Playwright 설치 (E2E 테스트)
        npm install -D @playwright/test && npx playwright install
[ ] 13. 환경 변수 파일 구성
        .env.local: NEXT_PUBLIC_API_BASE_URL, NEXT_PUBLIC_GOOGLE_CLIENT_ID 등
```

### TanStack Query 세팅 및 API 클라이언트 구성 방향

**디렉토리 구조 (안):**

```
src/
  lib/
    api/
      client.ts          # axios 인스턴스 (baseURL, 인터셉터)
      auth.ts            # /auth/* API 함수
      members.ts         # /members/* API 함수
      bookRequests.ts    # /clubs/{clubId}/book-requests/* API 함수
      bookReports.ts     # /clubs/{clubId}/book-reports/* API 함수
      penalties.ts       # /clubs/{clubId}/penalties/* API 함수
    query/
      keys.ts            # Query Key 팩토리 (중앙 관리)
      hooks/
        useBookRequests.ts
        useBookReports.ts
        usePenalties.ts
        useMe.ts
  providers/
    QueryProvider.tsx    # QueryClient + ReactQueryDevtools 래퍼
```

**API 클라이언트 핵심 설정:**

- `axios` 인터셉터에서 `Authorization: Bearer <token>` 자동 주입
- 401 응답 시 토큰 갱신 로직 (Refresh Token 도입 시) 또는 로그인 페이지로 리다이렉트
- TanStack Query `staleTime`: 기본 1분, 사용자 정보/예산 등 빈번 갱신 데이터는 30초
- Mutation 성공 후 관련 Query invalidate 패턴 사용

### 인증 흐름 처리 방식

**전체 흐름:**

```
1. 사용자가 로그인 버튼 클릭
        ↓
2. GET /auth/google → Spring Boot가 Google OAuth 인증 URL로 redirect
        ↓
3. Google 인증 완료 → GET /auth/google/callback (Spring Boot)
        ↓
4. Spring Boot가 JWT + 이메일 인증 상태를 포함하여
   프론트엔드 callback URL로 redirect
   예: https://frontend.domain/auth/callback?token=xxx&emailVerified=false
        ↓
5. Next.js /auth/callback 페이지에서 token 파싱
   - emailVerified=false → PAGE-01-B (이메일 Verify Code)로 이동
   - emailVerified=true  → PAGE-02 (동호회 목록)으로 이동
        ↓
6. PAGE-01-B: 코드 입력 → POST /auth/verify-email/confirm
   → 성공 시 accessToken 갱신 또는 기존 토큰에 verified 상태 반영
        ↓
7. PAGE-02 이후 모든 요청: Authorization: Bearer <token> 헤더 포함
```

**토큰 저장 전략:**

- `httpOnly` 쿠키: XSS에 안전하나 CORS 설정 필요 — **권장** (백엔드와 협의 필요)
- `localStorage`: 구현 간단하나 XSS 취약 — 내부 사내 도구 특성상 초기 단계에서 허용 가능한 트레이드오프
- 초기 MVP는 `localStorage` 사용, 이후 `httpOnly` 쿠키로 전환 계획 수립

**Route Guard 구현:**

```
- 비인증 사용자 → /login 으로 redirect (middleware.ts 또는 레이아웃 컴포넌트)
- 인증 완료 사용자가 /login 접근 → /clubs 로 redirect
- ADMIN 역할 아닌 사용자가 /admin/* 접근 → 403 페이지
- 무제 비회원이 /clubs/{id}/* 접근 → /clubs 로 redirect
```

---

## 참고 출처

| 항목 | URL |
|------|-----|
| Magic UI 공식 사이트 | https://magicui.design |
| Magic UI GitHub | https://github.com/magicuidesign/magicui |
| Magic UI MCP 공식 문서 | https://magicui.design/docs/mcp |
| Magic UI 컴포넌트 목록 | https://magicui.design/docs/components |
| shadcn/ui 공식 사이트 | https://ui.shadcn.com |
| shadcn/ui 레지스트리 | https://ui.shadcn.com/docs/registry |
| TanStack Query 공식 문서 | https://tanstack.com/query/latest |
| Framer Motion | https://www.framer.com/motion |
| Next.js 15 App Router | https://nextjs.org/docs/app |
| MCP (Model Context Protocol) | https://modelcontextprotocol.io |

> 주의: WebFetch 권한 제한으로 인해 위 URL들은 직접 검증되지 않았습니다. Magic UI MCP 패키지명(`@magicui/mcp`)과 세부 설치 방법은 https://magicui.design/docs/mcp 에서 반드시 최신 내용을 재확인하세요.
