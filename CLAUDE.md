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

## Notes

- The root package is `com.example.demo` (the originally intended `double.club.untitled` is an invalid package name).
- H2 console is available at runtime via `spring-boot-h2console`.
- `application.properties` is minimal — database and other environment config will need to be added as the project grows.
