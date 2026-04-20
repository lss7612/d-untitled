# 모듈 확장 전략

---

## 1. 두 번째 동호회 추가 시 작업 범위

새로운 동호회(예: 사진 동호회 "픽셀")를 추가할 때 수정/추가가 필요한 파일은 아래로 한정된다. 공통 코어 레이어는 수정하지 않는다.

**추가 대상 (신규 파일)**

```
com.example.demo.club.pixel/       # 새 동호회 전용 모듈 패키지
├── domain/                        # 픽셀 전용 엔티티
├── repository/
├── service/
├── dto/
└── controller/
```

**수정 대상 (최소 변경)**

- `Club.type` Enum에 신규 동호회 타입 추가 (예: `PHOTO`)
- Spring Security 권한 설정에서 신규 경로 패턴 추가
- `Schedule` / `Penalty`는 `typeCode`(String) 기반이므로 Core enum 수정 불필요. 신규 plugin이 자신이 지원하는 typeCode set을 SPI로 선언하면 됨

**수정 불필요**

- `user`, `auth`, `notification`, `common` 패키지 전체
- 기존 `club.untitled` 패키지 전체

---

## 2. 공통 인터페이스 설계 방향 (Phase 4 준비)

Phase 4 모듈 템플릿화를 대비하여 무제 전용 모듈은 `ClubPlugin` 인터페이스를 염두에 두고 설계한다. 현재 단계에서는 인터페이스를 강제 구현하지 않으나, 서비스 클래스가 해당 계약을 자연스럽게 따르도록 메서드 시그니처를 설계한다.

```java
// Phase 4 도입 예정 — 현재는 참조용 계획만 기재
interface ClubPlugin {
    String getClubType();                                  // 동호회 유형 식별자 (예: "READING")
    Set<String> getSupportedScheduleTypeCodes();           // 지원하는 일정 typeCode (SPI 등록)
    Set<String> getSupportedPenaltyTypeCodes();            // 지원하는 벌점 typeCode (SPI 등록)
    // ...
}

// 예시: 무제 (독서 동호회) 플러그인
class ReadingClubPlugin implements ClubPlugin {
    public String getClubType() { return "READING"; }
    public Set<String> getSupportedScheduleTypeCodes() {
        return Set.of("BOOK_REQUEST_DEADLINE", "BOOK_REPORT_DEADLINE", "PHOTO_SHOOT", "MONTHLY_MEETING");
    }
    public Set<String> getSupportedPenaltyTypeCodes() {
        return Set.of("BOOK_REPORT_MISSING", "PHOTO_ABSENT", "BOOK_NOT_RECEIVED");
    }
}
```

`Schedule.typeCode`와 `Penalty.typeCode`는 String으로 저장되며, 각 plugin이 자신이 지원하는 코드 집합을 위와 같이 선언한다. Core 레이어는 plugin 인터페이스만 알고 동호회 전용 코드 값을 알지 못한다. Phase 4에서 Spring의 `@ConditionalOnProperty` 또는 모듈 활성화 설정으로 플러그인을 선택적으로 등록하는 구조를 목표로 한다.
