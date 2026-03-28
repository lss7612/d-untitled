# CI/CD 파이프라인

## 개요

`main` 브랜치에 push되면 GitHub Actions가 자동으로 4단계 파이프라인을 실행한다.

```
git push main
     │
     ▼
[1] test        → ./gradlew test (백엔드 단위 테스트)
     │
     ▼
[2] build-push  → Docker multi-stage build
                   ghcr.io/<owner>/untitled-backend:<sha>
                   ghcr.io/<owner>/untitled-frontend:<sha>
     │
     ▼
[3] deploy      → SSH 접속 → scripts/deploy.sh <sha>
                   (Blue-Green 무중단 배포 실행)
```

---

## .github/workflows/deploy.yml

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  # ── 1. 테스트 ────────────────────────────────────────────────────────────
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test --no-daemon

  # ── 2. 이미지 빌드 & 레지스트리 push ─────────────────────────────────────
  build-push:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write       # ghcr.io push 권한
    steps:
      - uses: actions/checkout@v4

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}  # 별도 시크릿 불필요

      - name: Build & Push backend
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository }}/backend:${{ github.sha }}

      - name: Build & Push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: ghcr.io/${{ github.repository }}/frontend:${{ github.sha }}

  # ── 3. 서버 배포 ────────────────────────────────────────────────────────
  deploy:
    needs: build-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /app
            ./scripts/deploy.sh ${{ github.sha }}
```

---

## GitHub Secrets 설정

| Secret 이름 | 값 |
|-------------|-----|
| `SERVER_HOST` | 배포 서버 IP 또는 도메인 |
| `SERVER_USER` | SSH 접속 계정 (예: `ubuntu`) |
| `SSH_PRIVATE_KEY` | SSH 프라이빗 키 전체 내용 (`-----BEGIN ...`) |

> `GITHUB_TOKEN`은 GitHub이 자동 제공하므로 별도 등록 불필요.

---

## 이미지 태그 정책

- 형식: `ghcr.io/<owner>/<repo>/backend:<git-sha>`
- `latest` 태그 단독 사용 금지 — 어떤 버전인지 추적 불가
- git sha(40자) 사용으로 배포 버전과 커밋을 1:1 연결

---

## 선택적 배포 전략

### 근본 원인: API 호환성이 깨질 때만 문제

프론트/백엔드를 따로 배포하는 것 자체는 문제없다. 문제는 **API 계약이 깨지는 순간** 발생한다.

```
❌ 위험한 경우
  백엔드: 응답에서 title 필드 제거
  프론트: 아직 구버전 — title 참조 중 → 런타임 오류

✅ 안전한 경우
  백엔드: title 유지 + titleName 추가 (두 필드 공존)
  프론트: 배포 후 titleName으로 전환
  백엔드: title 제거 (프론트 배포 완료 후)
```

### Expand-Contract 패턴 (핵심 원칙)

API를 **절대 바로 제거하지 않는다**. 3단계로 나눈다.

```
[1단계] Expand   → 새 필드/엔드포인트 추가 (구 것도 유지)
[2단계] Migrate  → 프론트엔드 새 API로 전환 배포
[3단계] Contract → 구 필드/엔드포인트 제거
```

### 배포 순서 원칙: 항상 백엔드 먼저

- **백엔드 먼저**: 구 API를 유지한 채 신 API를 추가하면 구 프론트도 계속 동작
- **프론트 나중**: 백엔드 신 API 준비 후 전환하면 안전
- **프론트 먼저는 위험**: 신 API를 호출하는데 백엔드가 아직 구버전이면 404/500

| 상황 | 대응 |
|------|------|
| 필드 추가 | 백엔드만 배포해도 안전 |
| 필드 제거/변경 | Expand-Contract 3단계 — 절대 한 번에 하지 않음 |
| 기능 추가 | 백엔드 먼저 → 프론트 나중 |
| 긴급 핫픽스 | `workflow_dispatch`로 해당 서비스만 수동 트리거 |

---

### Path Filter 기반 자동 선택 배포

변경된 서비스만 빌드·배포되도록 `dorny/paths-filter`를 사용한다.
프론트와 백엔드가 동시에 변경된 경우 백엔드가 먼저 완료된 후 프론트가 실행된다.

```yaml
jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'src/**'
              - 'build.gradle'
              - 'Dockerfile'
            frontend:
              - 'frontend/**'

  deploy-backend:
    needs: [test, changes]
    if: needs.changes.outputs.backend == 'true'
    # ... 백엔드 빌드 & Blue-Green 배포

  deploy-frontend:
    needs: [deploy-backend, changes]   # 백엔드 완료 후 실행 (순서 보장)
    if: needs.changes.outputs.frontend == 'true'
    # ... 프론트엔드 빌드 & Nginx 재시작
```

### 수동 트리거 (긴급 배포)

```yaml
on:
  workflow_dispatch:
    inputs:
      service:
        description: '배포할 서비스'
        type: choice
        options: [all, backend-only, frontend-only]
        default: all
      image_tag:
        description: '배포할 이미지 태그 (기본: 최신 커밋 sha)'
        required: false
```

---

## 서버 초기 설정 (최초 1회)

```bash
# 서버에서 실행
mkdir -p /app/nginx /app/scripts

# nginx/upstream.conf 초기값
echo 'upstream backend_active { server backend-blue:8080; }' > /app/nginx/upstream.conf

# deploy.sh 실행 권한
chmod +x /app/scripts/deploy.sh

# ghcr.io 로그인 (서버에서 1회)
echo $CR_PAT | docker login ghcr.io -u <github-username> --password-stdin
```
