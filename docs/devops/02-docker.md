# Docker 설정

## 파일 구조

```
project-root/
├── Dockerfile                    # 백엔드 multi-stage build
├── frontend/
│   └── Dockerfile                # 프론트엔드 multi-stage build
├── nginx/
│   ├── nginx.conf                # Nginx 메인 설정
│   └── upstream.conf             # 활성 슬롯 upstream (deploy.sh가 수정)
├── docker-compose.dev.yml        # 로컬 개발 환경
└── docker-compose.prod.yml       # 프로덕션 환경
```

---

## Dockerfile — 백엔드

**파일 위치**: `Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 레이어 먼저 캐시 (소스 변경 시 재다운로드 방지)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Runtime (JRE만 포함 — 이미지 크기 최소화)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**포인트**:
- Builder/Runtime 스테이지 분리로 최종 이미지에 JDK·Gradle 미포함
- `dependencies` 레이어 분리로 소스 변경 시 의존성 재다운로드 방지

---

## Dockerfile — 프론트엔드

**파일 위치**: `frontend/Dockerfile`

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build          # tsc -b && vite build

# Stage 2: Nginx 정적 파일 서빙
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

**포인트**:
- `dist/` 정적 파일만 최종 이미지에 포함 (node_modules 미포함)
- Nginx Alpine 기반으로 이미지 크기 최소화

---

## nginx/nginx.conf

```nginx
include /etc/nginx/upstream.conf;   # 활성 슬롯 upstream (별도 파일 관리)

server {
    listen 80;

    # /api/* → 백엔드 컨테이너
    location /api/ {
        proxy_pass         http://backend_active;
        proxy_http_version 1.1;
        proxy_set_header   Connection "";
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
    }

    # /* → 프론트엔드 SPA (React Router 대응 fallback)
    location / {
        root      /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }
}
```

## nginx/upstream.conf (초기값: blue)

```nginx
upstream backend_active {
    server backend-blue:8080;
}
```

> `deploy.sh`가 배포 시 이 파일의 `backend-blue`/`backend-green`을 교체한 후 `nginx -s reload` 호출.

---

## docker-compose.dev.yml

```yaml
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DEVTOOLS_RESTART_ENABLED=true
    volumes:
      - ./src:/app/src    # hot reload
    networks:
      - app-network

  frontend:
    build:
      context: ./frontend
      target: builder     # 빌드 스테이지에서 vite dev server 실행
    command: npm run dev -- --host
    ports:
      - "5173:5173"
    volumes:
      - ./frontend:/app
      - /app/node_modules
    networks:
      - app-network

networks:
  app-network:
```

---

## docker-compose.prod.yml

```yaml
services:
  nginx:
    image: ghcr.io/<owner>/untitled-frontend:<TAG>
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/upstream.conf:/etc/nginx/upstream.conf  # deploy.sh가 수정
    depends_on:
      backend-blue:
        condition: service_healthy
    networks:
      - app-network

  backend-blue:
    image: ghcr.io/<owner>/untitled-backend:<TAG>
    env_file: .env.prod
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    networks:
      - app-network
    profiles: ["blue"]

  backend-green:
    image: ghcr.io/<owner>/untitled-backend:<TAG>
    env_file: .env.prod
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    networks:
      - app-network
    profiles: ["green"]

  mysql:
    image: mysql:8
    env_file: .env.prod
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

networks:
  app-network:

volumes:
  mysql-data:
```

---

## .env.prod (서버 로컬 보관, git 미추적)

```bash
# .gitignore에 .env.prod 추가 필수
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/untitled
SPRING_DATASOURCE_USERNAME=untitled_user
SPRING_DATASOURCE_PASSWORD=<비밀번호>
MYSQL_ROOT_PASSWORD=<루트 비밀번호>
MYSQL_DATABASE=untitled
```

> 시크릿은 절대 이미지에 포함하지 않는다. 런타임 시 `env_file`로 주입한다.

---

## build.gradle 추가 항목

Health check를 위해 Spring Boot Actuator 의존성 추가 필요:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

`/actuator/health` 엔드포인트가 활성화되면 Docker health check가 정상 작동한다.
