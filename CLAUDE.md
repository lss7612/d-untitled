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

## Stack

- **Java 21**, Spring Boot 4.0.4
- **Spring Web MVC** for REST APIs
- **Spring Data JPA** + **H2** (in-memory, dev) + **MySQL** (production)
- **Lombok** for boilerplate reduction
- **Spring Boot DevTools** for hot reload during development

## Project Purpose

Internal club management platform for Wemade / DoubleDown Interactive employees. The goal is to automate repetitive admin work for company clubs (동호회), not to generate business revenue.

Core features planned:
- **Auth**: Google OAuth 2.0 + email-based 2-step verification
- **Notifications**: Automated monthly reminders (book reports, photo/receipt uploads, etc.) delivered via KakaoTalk or Slack
- **Points/Penalties**: Gamification layer for club participation
- **Admin**: Member management, targeted messaging, manual task automation

## Architecture

The system is designed as a **modular/plugin architecture** where each club type can have custom logic layered on top of shared infrastructure (auth, members, notifications, points).

- **First prototype target**: "무제" (독서 동호회 / reading club) — book report submission and verification
- **Future clubs** plug in as additional modules without changing core

Planned roadmap:
1. Google account auth + basic member management
2. "무제"-specific features + notification automation
3. Points/penalty system + KakaoTalk/Slack channel integration
4. Module templating for other clubs

## Notes

- The root package is `com.example.demo` (the originally intended `double.club.untitled` is an invalid package name).
- H2 console is available at runtime via `spring-boot-h2console`.
- `application.properties` is minimal — database and other environment config will need to be added as the project grows.
