# 16 — 동적 설정 (AppConfig)

> 작성일: 2026-04-24
> 상태: v1 구현 완료 (DEVELOPER 수동 편집)

---

## 1. 왜 만들었나 (Problem)

- 운영 중 **재배포 없이** 바꿔야 할 값들이 있다:
  - 이메일 화이트리스트 (11 문서)
  - 월별 기본 예산 (13)
  - 슬랙 Webhook URL (12 v2)
  - 월 마감일, 기본 마감 정책 등
- `application.properties` 는 빌드에 박혀 있어 수정 시 재배포 필요 → 운영 민첩성이 떨어진다.

## 2. 목표 (Goal)

- `AppConfig` 단일 테이블로 문자열/JSON 기반 설정을 저장.
- DEVELOPER 가 런타임에 조회/수정.
- 각 도메인(예산, 알림, 화이트리스트) 이 서비스 레이어에서 AppConfig 를 조회해 사용.

## 3. 정책 / 규칙 (Rules)

- **Key 명명**: 단일 enum `AppConfigType` (대문자 스네이크). 예:
  - `WHITE_LIST` — 이메일 배열 JSON.
  - `DEFAULT_MONTHLY_BUDGET` — 숫자 문자열.
  - `SLACK_WEBHOOK_<CLUBID>` — 향후.
- **값 타입**: `contents` 는 항상 문자열. 파싱은 호출 측 책임 (`Jackson ObjectMapper` 공용).
- **캐싱**: v1 은 요청마다 DB 조회 (간단함 우선). 부하 커지면 TTL 캐시로 전환.
- **접근 제어**: 모든 수정 엔드포인트는 DEVELOPER 전용. 조회도 DEVELOPER 만 — 일반 회원이 값이 궁금할 이유 없음.
- **시드**: `AppConfigSeeder` 가 빈 값이면 기본값으로 insert (최초 부팅 1회).

## 4. UX 흐름

- v1: `/developer/whitelist` (`DeveloperWhitelistPage`) 는 `WHITE_LIST` key 의 전용 UI.
- 그 외 key 는 **전용 UI 가 없어 DEVELOPER 가 `/api/v1/developer/config/{type}` 를 직접 호출** 하거나 DB 에 UPDATE 로 수정. (v1 한계)
- v2: AppConfig 전용 관리 페이지 — key 선택 → form 렌더 → 저장.

## 5. API 표면

- `GET /api/v1/developer/config/{type}` — 현재 값 (`AppConfigResponse { type, contents }`).
- `PUT /api/v1/developer/config/{type}` — 값 덮어쓰기 (`{ contents: string }`).

DEVELOPER 가 아닌 호출은 403.

## 6. 수용 기준 (Acceptance)

- [x] 부팅 시 기본 키들이 없으면 시드로 채워짐.
- [x] PUT 후 다음 요청부터 값이 즉시 반영 (재시작 불필요).
- [x] 비DEVELOPER 의 `/api/v1/developer/config/**` 호출 403.
- [x] `contents` 가 JSON 인 경우 파싱 실패는 호출 측에서 방어적으로 처리.
- [ ] 설정 변경 이력(누가 언제 무엇을) — 감사 로그 미구현.

## 7. 오픈 이슈 / 향후 계획

- **전용 관리 UI**: 현 커버리지는 화이트리스트 뿐. 예산 기본값·Webhook 도 UI 필요.
- **스키마 검증**: key 별 타입(숫자/배열/URL) 을 레이어에서 검증 — Zod/Valibot 수준의 contract.
- **환경 분리**: 스테이징/운영에서 다른 값이 필요한 키 (Webhook URL) 의 환경별 관리.
- **이력/롤백**: 변경 로그 + 이전 값으로 되돌리기.
