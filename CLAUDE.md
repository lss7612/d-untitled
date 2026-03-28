# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.demo.SomeTest"

# Clean build
./gradlew clean build
```

## Planning Docs

- 기획 문서: `docs/plan/`

---

## Project Overview

더블유게임즈(DoubleU Games) & 계열사 사내 동호회 통합 관리 플랫폼.
반복적인 행정 업무 자동화, 활동 알림, 포인트 게이미피케이션에 집중. 동호회별로 다른 규칙을 수용하는 모듈형 구조가 핵심 설계 방향.

**첫 번째 타겟**: '무제' (독서 동호회)

### 핵심 기능 범위
- **인증**: Google OAuth 2.0 + 이메일 Verify Code (2-step)
- **알림**: 월별 과업 스마트 리마인더, 채널은 슬랙(Slack Incoming Webhook) 전용
- **보상**: 포인트 / 벌점(Penalty) 시스템
- **관리자**: 회원 포인트·벌점 관리, 타겟 메시지 발송

### 로드맵
1. Google 계정 기반 인증 + 기본 회원 관리
2. 무제(독서 동호회) 전용 기능 + 알림 자동화
3. 포인트/벌점 + 슬랙 채널 연동
4. 모듈 템플릿화 및 타 동호회 확장

---

## Stack

- **Java 21**, Spring Boot 4.0.4
- **Spring Web MVC** for REST APIs
- **Spring Data JPA** + **MySQL** (production)
- **Lombok** for boilerplate reduction
- **Spring Boot DevTools** for hot reload during development

## Project Purpose

Internal club management platform for DoubleU Games (더블유게임즈) and its affiliates employees. The goal is to automate repetitive admin work for company clubs (동호회), not to generate business revenue.

Core features planned:
- **Auth**: Google OAuth 2.0 + email-based 2-step verification
- **Notifications**: Automated monthly reminders (book reports, photo/receipt uploads, etc.) delivered via Slack (Incoming Webhook)
- **Points/Penalties**: Gamification layer for club participation
- **Admin**: Member management, targeted messaging, manual task automation

## Architecture

The system is designed as a **modular/plugin architecture** where each club type can have custom logic layered on top of shared infrastructure (auth, members, notifications, points).

- **First prototype target**: "무제" (독서 동호회 / reading club) — book report submission and verification
- **Future clubs** plug in as additional modules without changing core

Planned roadmap:
1. Google account auth + basic member management
2. "무제"-specific features + notification automation
3. Points/penalty system + Slack channel integration
4. Module templating for other clubs

## Notes

- The root package is `com.example.demo` (the originally intended `double.club.untitled` is an invalid package name).
- MySQL 연결 설정은 `application.properties`에 추가 필요.
- `application.properties` is minimal — database and other environment config will need to be added as the project grows.
