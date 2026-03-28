# 배포 아키텍처

## 전체 구조

```
Internet
    │
    ▼
┌───────────────────────────┐
│   Nginx (:80 / :443)      │
│   - 프론트엔드 정적 파일  │  ← React/Vite 빌드 결과물 서빙
│   - /api/* 리버스 프록시  │  ← 백엔드 upstream으로 전달
└────────────┬──────────────┘
             │ upstream 핫스위칭 (무중단)
   ┌─────────┴──────────────┐
   │                        │
┌──▼──────────────┐  ┌──────▼──────────────┐
│  backend-blue   │  │  backend-green       │
│  (:8080)        │  │  (:8081)             │
│  Spring Boot    │  │  Spring Boot         │
│  [현재 활성]    │  │  [다음 버전 대기]    │
└──┬──────────────┘  └──┬──────────────────┘
   └──────────┬──────────┘
              ▼
   ┌───────────────────┐
   │  MySQL :3306      │  ← Blue/Green 공유 (교체 대상 아님)
   └───────────────────┘
```

## 컨테이너 역할

| 컨테이너 | 이미지 | 역할 |
|----------|--------|------|
| `nginx` | `untitled-frontend:<sha>` | 프론트엔드 정적 파일 서빙 + 리버스 프록시 |
| `backend-blue` | `untitled-backend:<sha>` | Spring Boot API 서버 (활성 슬롯 A) |
| `backend-green` | `untitled-backend:<sha>` | Spring Boot API 서버 (활성 슬롯 B) |
| `mysql` | `mysql:8` | 영속 데이터 저장소 |

## Zero-Downtime 원칙

배포 중 순단이 발생하지 않도록 **Blue-Green 배포 전략**을 사용한다.

1. 신규 버전을 **Green 슬롯**에 기동
2. Health check 통과 확인
3. Nginx upstream을 Green으로 교체 (`nginx -s reload`)
   - `reload`는 기존 처리 중인 커넥션을 유지하므로 순단 없음
4. 구 버전(Blue 슬롯) 제거

> 자세한 스위칭 절차는 [03-blue-green.md](./03-blue-green.md) 참고.

## 환경 분리

| 환경 | 설정 파일 | DB | 비고 |
|------|-----------|-----|------|
| 로컬 개발 | `docker-compose.dev.yml` | H2 (in-memory) | hot reload 포함 |
| 프로덕션 | `docker-compose.prod.yml` | MySQL 8 | Blue-Green 적용 |
