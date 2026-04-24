# 12 — 알림(NotificationService) 추상화와 단계적 확장

> 작성일: 2026-04-24
> 상태: v1 (Logging 구현체) 완료, v2 Slack 구현 미착수

---

## 1. 왜 만들었나 (Problem)

- `04-notification-spec.md` 에서 정의한 9개 알림 트리거(독후감·책 신청 리마인더, 도서 도착, 수령 기한, 벌점, 이의 제기, 촬영 변경, 도서 미반납 등)를 실제로 붙일 시점은 아직 이르다.
- 그렇다고 호출부에 슬랙 Webhook 을 직접 박으면 나중에 **채널 전환(슬랙 → 팀즈/푸시)** 이 전파 리팩터링을 유발한다.
- 타 동호회 확장 시 알림 채널이 **동호회별로 다를 수 있다** (팀 슬랙 vs 카카오 등).

## 2. 목표 (Goal)

- 호출부는 `notificationService.send(targets, message)` 만 알도록 한다.
- 구현체를 **Logging(v1) → Slack Webhook(v2) → 채널별 라우팅(v3)** 으로 점진 교체.
- 04 문서의 스펙은 유지, 전달 레이어만 이 문서에서 관리.

## 3. 정책 / 규칙 (Rules)

- **인터페이스 위치**: `common/notification/NotificationService.java`.
- **v1 기본 구현**: `LoggingNotificationService` — 로그 스트림에만 출력. 실제 전달 안 함. 운영 전 다른 구현체로 교체.
- **메시지 모델 (권장)**:
  - `Notification { kind, title, body, targets, payload? }`
  - `kind` 는 04 스펙의 9 개 트리거 enum (`BOOK_REQUEST_REMINDER`, `ARRIVAL`, …).
- **타겟 해석**: v1 에서는 이메일 문자열만. v2 에서 슬랙 user ID 매핑 테이블 도입 검토.
- **호출 지점**: 서비스 레이어에서만. 컨트롤러에서 직접 호출 금지.
- **에러 정책**: 알림 실패는 **본 작업(예: 책 신청 저장)을 롤백하지 않는다**. 로그만 남기고 삼킴.

## 4. UX 흐름 (구현 로드맵)

### v1 (현재)
- 모든 트리거가 `LoggingNotificationService` 로 흘러 로그에 찍힘.
- 개발자는 로그로 검증. 최종 사용자에게는 아직 도달 안 함.

### v2 (Slack Webhook)
- `SlackNotificationService implements NotificationService`.
- 동호회별 Incoming Webhook URL 을 `AppConfig`(16 문서) 의 `type = SLACK_WEBHOOK_{CLUBID}` 로 관리.
- 메시지 템플릿은 04-notification-spec 그대로.

### v3 (채널 라우팅)
- `NotificationRouter` 가 kind + club 조합으로 복수 채널 fan-out.
- 푸시(FCM) / 이메일 / 슬랙 동시 전달 허용.

## 5. API 표면

알림 자체는 **내부 컴포넌트** 이므로 사용자용 REST 엔드포인트는 없다. 다만 v3 단계에서:

- `GET  /api/v1/me/notifications` — 개인 인박스 (푸시 연계)
- `PATCH /api/v1/me/notifications/{id}/read` — 읽음 처리

를 추가할 여지가 있다. v1-v2 에는 불필요.

## 6. 수용 기준 (Acceptance)

- [x] 호출부가 `NotificationService` 에만 의존 (슬랙 SDK 직접 의존 금지).
- [x] `LoggingNotificationService` 가 9개 trigger 호출 시 로그 포맷 정상.
- [ ] v2: Slack Incoming Webhook 전송 성공. (미구현)
- [ ] v2: Webhook 실패 시 원 트랜잭션 롤백 없음.
- [ ] v3: kind + club 매핑표로 채널 선택.

## 7. 오픈 이슈 / 향후 계획

- **Rate limit / 묶음 알림**: 같은 kind 가 1분 내 여러 번이면 하나로 묶기.
- **조용 시간**: 업무 외 시간 발송 정책 (04 문서에서 일부 다룸).
- **DEVELOPER 진단 채널**: 운영 경고/오류 알림을 별도 채널로 라우트.
- **템플릿 관리**: 메시지 템플릿을 `AppConfig` 에 얹어 런타임 수정 가능하게.
