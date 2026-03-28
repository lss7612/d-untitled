# Blue-Green 무중단 배포

## 개념

두 개의 동일한 실행 환경(Blue, Green)을 준비하고, 한 번에 하나만 트래픽을 받도록 한다.
배포 시 신규 버전을 대기 슬롯에 기동한 뒤 Health check 통과 후 Nginx upstream을 교체한다.

```
배포 전                      배포 중                      배포 후

Nginx → Blue (v1) [활성]     Nginx → Blue (v1) [활성]     Nginx → Green (v2) [활성]
        Green     [없음]             Green (v2) [기동중]          Blue         [제거됨]
                                     ↑ Health check 대기
```

## 핵심: nginx -s reload

`nginx -s reload`는 설정 파일을 다시 읽으면서도 **현재 처리 중인 커넥션을 끊지 않는다**.
새로운 커넥션부터 새 upstream으로 라우팅하므로 순단이 발생하지 않는다.

---

## deploy.sh

**파일 위치**: `scripts/deploy.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG="${1:?IMAGE_TAG 인자 필요}"
COMPOSE="docker compose -f docker-compose.prod.yml"

# ── 1. 현재 활성 슬롯 감지 ────────────────────────────────────────────────
if docker ps --format '{{.Names}}' | grep -q "backend-blue"; then
  CURRENT="blue"
  NEXT="green"
else
  CURRENT="green"
  NEXT="blue"
fi

echo "▶ 배포 시작: $CURRENT → $NEXT  (이미지: $IMAGE_TAG)"

# ── 2. 새 슬롯 기동 ──────────────────────────────────────────────────────
IMAGE_TAG=$IMAGE_TAG $COMPOSE --profile $NEXT up -d backend-$NEXT

# ── 3. Health check 대기 (최대 60초) ────────────────────────────────────
echo "⏳ Health check 대기..."
for i in $(seq 1 12); do
  STATUS=$($COMPOSE exec -T backend-$NEXT \
    wget -qO- http://localhost:8080/actuator/health 2>/dev/null || echo "")
  if echo "$STATUS" | grep -q '"status":"UP"'; then
    echo "✅ backend-$NEXT 정상 응답"
    break
  fi
  if [ $i -eq 12 ]; then
    echo "❌ Health check 실패 — 배포 중단"
    $COMPOSE --profile $NEXT stop backend-$NEXT
    $COMPOSE --profile $NEXT rm -f backend-$NEXT
    exit 1
  fi
  sleep 5
done

# ── 4. Nginx upstream 교체 ───────────────────────────────────────────────
sed -i "s/backend-$CURRENT:8080/backend-$NEXT:8080/" ./nginx/upstream.conf
$COMPOSE exec -T nginx nginx -s reload
echo "🔀 Nginx upstream → backend-$NEXT"

# ── 5. 구 슬롯 제거 ─────────────────────────────────────────────────────
$COMPOSE --profile $CURRENT stop backend-$CURRENT
$COMPOSE --profile $CURRENT rm -f backend-$CURRENT
echo "🗑  backend-$CURRENT 제거 완료"

echo "✅ 배포 완료: backend-$NEXT ($IMAGE_TAG)"
```

---

## 배포 흐름 다이어그램

```
scripts/deploy.sh <git-sha>
        │
        ├─ [1] 현재 슬롯 감지 (blue or green)
        │
        ├─ [2] 새 슬롯 docker compose up
        │         └─ 이미지: ghcr.io/<owner>/backend:<sha>
        │
        ├─ [3] Health check 폴링
        │         ├─ GET /actuator/health → {"status":"UP"} 대기
        │         └─ 60초 초과 시 새 슬롯 제거 후 exit 1 (자동 롤백)
        │
        ├─ [4] nginx/upstream.conf 수정
        │         └─ nginx -s reload (기존 커넥션 유지, 순단 없음)
        │
        └─ [5] 구 슬롯 stop & rm
```

---

## 롤백

배포 실패(Health check timeout) 시 스크립트가 자동으로 새 슬롯을 제거하고 종료한다.
기존 슬롯(Blue)이 계속 트래픽을 받으므로 서비스는 영향 없다.

수동 롤백이 필요한 경우:
```bash
# 이전 이미지 태그로 재배포
./scripts/deploy.sh <이전-git-sha>
```

---

## 검증 방법

```bash
# 배포 중 순단 여부 확인 (별도 터미널에서 실행)
while true; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost/api/actuator/health
  sleep 0.5
done
# 배포 전후 내내 200이 유지되어야 함
```
