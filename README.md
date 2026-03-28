# 🏢 사내 동호회 통합 관리 플랫폼 (Internal Club Management System)

더블유게임즈(DoubleU Games) & 더블다운인터액티브(DoubleDown Interactive) 사내 동호회의 운영 효율을 높이고 즐거운 커뮤니티 활동을 지원하는 통합 관리 솔루션입니다.

## 📎 기획 문서 (Planning Docs)

- /docs/plan 폴더 참조.

---

## 🎯 프로젝트 개요 (Project Overview)

본 프로젝트는 동호회 운영 과정에서 발생하는 **반복적이고 소모적인 행정 업무를 소프트웨어화하여 간소화**하는 것을 목적으로 합니다. 비즈니스 수익보다는 사내 임직원의 복지 향상과 운영 편의성에 집중합니다.

- **핵심 가치**: 운영 효율화, 활동 자동 알림, 활동 게이미피케이션(포인트제)
- **확장성**: 각 동호회별 상이한 규칙을 수용할 수 있는 모듈형 구조 (Prototype: 독서 동호회 '무제')
- **AI 활용**: MCP를 통한 운영 자동화 및 AI Agent 기반의 행정 처리

---

## 🛠 기술 스택 (Tech Stack)

### 🎨 Frontend
- **Library**: **React 18+** (TypeScript)
- **UI & Styling**: **shadcn/ui**, Tailwind CSS
- **State Management**: **TanStack Query** (Server State 관리)
- **E2E Testing**: **Playwright** (사용자 시나리오 자동 테스트)

### ⚙️ Backend
- **Language**: Java 21
- **Framework**: **Spring Boot 4.0.4**
- **Auth**: Spring Security, **Google OAuth 2.0** (사내 계정 인증)
- **Database**: **Spring Data JPA** / Hibernate — MySQL (production)

### 🤖 AI & Operations (Experimental)
- **Model Context Protocol (MCP)**: Claude 기반 운영 자동화 레이어 (DB 핸들링 및 API 연동)
- **AI Agent**: 반복 행정 업무(리마인더 발송, 데이터 집계) 자동화

---

## 👥 주요 사용자 및 기능 (Key Features)

### 1. 일반 회원 (General User)
- **인증**: 사내 구글 계정 로그인 및 이메일 Verification Code 인증.
- **대시보드**: 가입 동호회 목록 및 월별 정기 과업(독후감, 사진 업로드 등) 확인.
- **알림**: 슬랙(Slack) Incoming Webhook으로 리마인더 수신.
- **보상**: 미션 수행 시 활동 포인트 적립.

### 2. 관리자 (Admin)
- **회원 관리**: 특정 회원 대상 포인트/벌점 부여 및 메시지 발송.
- **자동화**: 반복적인 공지 및 정산 업무를 시스템(또는 AI Agent)이 대행.
- **커스텀 모듈**: 동호회별 특화 기능(예: 독후감 검증) 커스텀 구현 지원.

---

## 🧩 확장 구조 (Scalability)

1. **Core Module**: 인증, 알림, 포인트 등 전 동호회 공통 기능.
2. **Club Module**: 각 동호회별 독립적 로직 (최초 프로토타입: **독서 동호회 '무제'**).
3. **External API**: Google Auth, Slack Webhook 연동.

---

## 🚀 로드맵 (Roadmap)

1. **Step 1**: 구글 계정 기반 인증 체계 및 기본 회원 관리 구축
2. **Step 2**: '무제(독서 동호회)' 전용 기능 및 알림 자동화 구현
3. **Step 3**: 포인트/벌점 시스템 및 슬랙 채널 연동
4. **Step 4**: 타 동호회 확장을 위한 모듈 템플릿화