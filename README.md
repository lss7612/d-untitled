# 사내 동호회 통합 관리 플랫폼

더블유게임즈(DoubleU Games) & 더블다운인터액티브(DoubleDown Interactive) 사내 동호회의 운영 효율을 높이고 즐거운 커뮤니티 활동을 지원하는 통합 관리 솔루션.

## 목적

동호회 운영에서 반복적이고 소모적인 행정 업무를 소프트웨어화해 간소화한다. 비즈니스 수익보다 사내 임직원의 복지 향상과 운영 편의성에 집중.

- **운영 효율화** — 신청 / 발주 / 도착 / 수령 / 독후감 흐름 자동화
- **모듈형 구조** — 동호회마다 다른 규칙을 수용 (1호 모듈: 무제 독서 동호회)
- **게이미피케이션** — 활동 포인트 / 벌점 (로드맵)

## 기술 스택

- **Frontend**: React 18 + TypeScript, shadcn/ui + Tailwind, TanStack Query, Playwright (E2E)
- **Backend**: Java 21 + Spring Boot 4, Spring Security + Google OAuth, Spring Data JPA + MySQL

## 기획 문서

상세한 기능 정의 / 정책 / 구현 상태는 `docs/plan/` 참조.

- 전체 로드맵 — [docs/plan/06-implementation-roadmap.md](docs/plan/06-implementation-roadmap.md)
- 기능 정의 (TO-BE) — [docs/plan/02-feature-spec.md](docs/plan/02-feature-spec.md)
- 페이지 정의 — [docs/plan/03-page-spec.md](docs/plan/03-page-spec.md)
- 알림 명세 — [docs/plan/04-notification-spec.md](docs/plan/04-notification-spec.md)
- 도메인 모델 — [docs/plan/05-domain-model.md](docs/plan/05-domain-model.md)

각 모듈별 상세는 `docs/plan/08-...` 이후 번호 순서로 정리되어 있다.

## 로컬 실행

```bash
# DB + 백엔드 + 프론트 한 번에
docker compose up -d --build

# 또는 개별
./gradlew bootRun                # 백엔드 (8080)
cd frontend && npm run dev       # 프론트 (5173)
```

`.env` 파일에 `JWT_SECRET`, `GOOGLE_CLIENT_ID/SECRET`, `MAIL_USERNAME/PASSWORD` 등을 설정해야 한다 (`.env.example` 참조).
