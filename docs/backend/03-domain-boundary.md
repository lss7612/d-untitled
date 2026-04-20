# 도메인 경계 정의

---

## 1. `user` 도메인이 제공하는 것

`user` 도메인은 플랫폼 전체에서 공유되는 **인증된 사용자 정보**를 단일 진실 공급원(Single Source of Truth)으로 관리한다.

| 제공 항목 | 설명 |
|-----------|------|
| `Member` 엔티티 | 이메일, 이름(Google 동기화), 닉네임, 부서, slackUserId, 이메일 인증 여부 |
| `MemberRepository` | 이메일/ID 기반 회원 조회 |
| `MemberService` | 프로필 조회·수정 공개 API |

`user` 도메인은 `club`, `auth`, `notification` 등 다른 도메인을 직접 의존하지 않는다. 참조 방향은 항상 외부 도메인 → `user` 단방향이다.

---

## 2. `club`이 `user`를 참조하는 방식

`club` 도메인의 엔티티들은 `Member`를 JPA 연관관계로 직접 참조하지 않고 **`memberId` (Long FK)를 필드로 보유**하는 방식을 기본으로 한다.

- 연관관계 조회가 빈번하고 성능이 중요한 경우(예: `ClubMember`) `@ManyToOne` JPA 참조를 허용한다.
- 단순 이력성 엔티티(예: `Penalty`, `Point`, `Notification`)는 `memberId` Long 필드만 유지하고 필요 시 서비스 레이어에서 `MemberRepository`를 통해 조회한다.

이 선택의 이유는 도메인 경계 유지와 N+1 쿼리 방지 사이의 균형이다. 초기에는 참조 방향을 단순하게 유지하고, 성능 병목이 확인된 시점에 `@ManyToOne` + `FetchType.LAZY`로 전환한다.

---

## 3. `ClubMember` 엔티티의 위치와 역할

`ClubMember`는 `club` 도메인 소속(`com.example.demo.club.domain.ClubMember`)이다.

- **역할**: 특정 회원이 특정 동호회에 속하는 관계를 표현하며, 동호회 단위 역할(`ADMIN` / `MEMBER`)을 보유한다. 예산 한도 전환 기준은 별도 필드 없이 `Member.createdAt`의 YearMonth로 계산한다 (가입월 30,000원, 그 다음 월부터 35,000원).
- **참조 방향**: `ClubMember → Club` (@ManyToOne), `ClubMember → Member` (@ManyToOne, `memberId` FK 기반 또는 직접 참조)
- 동일 회원이 복수 동호회에 가입 가능하므로 `(clubId, memberId)` 복합 유니크 제약을 설정한다.

---

## 4. 무제 전용 엔티티의 위치

| 엔티티 | 패키지 |
|--------|--------|
| `BookRequest` | `com.example.demo.club.untitled.domain` |
| `BookReport` | `com.example.demo.club.untitled.domain` |
| `PhotoSchedule` | `com.example.demo.club.untitled.domain` |
| `LibraryBook` | `com.example.demo.club.untitled.domain` |
| `BookLending` | `com.example.demo.club.untitled.domain` |

이 엔티티들은 `club` 공통 도메인 패키지와 같은 레벨에 있지 않고, `club.untitled` 하위에 격리한다. `club` 공통 레이어는 `club.untitled`를 직접 의존하지 않는다.

---

## 5. 엔티티 연관관계 설계

### 5-1. 연관관계 방향 원칙

- **단방향 원칙**: 연관관계는 기본적으로 단방향(자식 → 부모)으로 설정한다. 양방향이 필요한 경우 명시적으로 이유를 주석으로 기록한다.
- **FetchType**: 모든 연관관계에 `FetchType.LAZY`를 기본 설정하고, 필요 시 JPQL `JOIN FETCH`로 명시적 즉시 로딩한다.
- **도메인 간 참조**: 다른 도메인 엔티티를 JPA `@ManyToOne`으로 참조하는 경우, 해당 참조가 도메인 경계를 넘어가는지 검토 후 Long ID 참조로 대체할지 결정한다.

### 5-2. 주요 연관관계 설정

```
[club 도메인 내부]
ClubMember @ManyToOne → Club          (단방향, LAZY)
ClubMember @ManyToOne → Member        (단방향, LAZY, user 도메인 참조)
Penalty.clubId        → Club.id       (Long FK, 도메인 경계 유지)
Penalty.memberId      → Member.id     (Long FK, 도메인 경계 유지)
PenaltyDispute @ManyToOne → Penalty   (단방향, LAZY)
Point.clubId          → Club.id       (Long FK)
Point.memberId        → Member.id     (Long FK)
Schedule.clubId       → Club.id       (Long FK)

[club.untitled 도메인 내부]
BookRequest.clubId    → Club.id       (Long FK)
BookRequest.memberId  → Member.id     (Long FK)
BookReport @ManyToOne → BookRequest   (단방향, LAZY)
BookReport.memberId   → Member.id     (Long FK)
PhotoSchedule.clubId  → Club.id       (Long FK)
LibraryBook.clubId    → Club.id       (Long FK)
BookLending @ManyToOne → LibraryBook  (단방향, LAZY)
BookLending.memberId  → Member.id     (Long FK)

[notification 도메인 내부]
Notification.clubId    → Club.id      (Long FK)
Notification.recipientId → Member.id  (Long FK, nullable)
```

### 5-3. 양방향 참조 판단 기준

양방향 참조(`@OneToMany` + `@ManyToOne`)는 다음 조건을 모두 만족할 때만 허용한다.

1. 부모 엔티티에서 자식 컬렉션을 직접 순회하는 비즈니스 로직이 존재한다.
2. JPQL `JOIN FETCH`로 대체하기 어려운 경우이다.
3. 무한 순환 참조(`toString`, `equals`, 직렬화) 방지 처리를 확인한다.

현 설계에서는 양방향 참조를 허용하는 엔티티가 없다. 추후 필요 시 `BookRequest ↔ BookReport`를 최초 검토 대상으로 한다.

---

## 참고: 도메인 경계 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                    com.example.demo                     │
│                                                         │
│  ┌──────────┐   ┌──────────┐   ┌────────────────────┐  │
│  │  common  │   │   auth   │   │    notification    │  │
│  └──────────┘   └────┬─────┘   └────────────────────┘  │
│                      │ depends on                       │
│                  ┌───▼──────┐                           │
│                  │   user   │  ← 공통 Member 도메인      │
│                  └───▲──────┘                           │
│                      │ FK 참조 (memberId)               │
│  ┌───────────────────┴───────────────────────────────┐  │
│  │                    club                           │  │
│  │  Club, ClubMember, Schedule, Penalty, Point 등    │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │              club.untitled                  │  │  │
│  │  │  BookRequest, BookReport, PhotoSchedule,    │  │  │
│  │  │  LibraryBook, BookLending                   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │         club.{next} (Phase 4 예정)           │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

- `common` 패키지는 어떤 도메인도 의존하지 않는다.
- `auth`는 `user`를 의존한다 (Member 조회/생성).
- `notification`은 `user`와 `club`을 의존한다 (수신자 조회, 동호회 정보 조회).
- `club.untitled`는 `club` 공통 레이어를 의존하지만, `club` 공통 레이어는 `club.untitled`를 모른다.
- 의존 방향은 항상 구체 → 추상 (단방향)이다.
