#!/bin/bash
set -euo pipefail

IMAGE="${1:-}"

if [ -z "$IMAGE" ]; then
  echo ">>> 사용법: ./deploy.sh <이미지명:태그>"
  exit 1
fi

APP_DIR="${APP_DIR:-/home/ubuntu/app}"
NETWORK="${NETWORK:-app-network}"
NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"

log() {
  echo ">>> $*"
}

cleanup_new_container() {
  if [ -n "${NEW_CONTAINER:-}" ]; then
    docker stop "$NEW_CONTAINER" >/dev/null 2>&1 || true
    docker rm "$NEW_CONTAINER" >/dev/null 2>&1 || true
  fi
}

mkdir -p "$APP_DIR"

docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

if ! docker ps --format '{{.Names}}' | grep -qx "$NGINX_CONTAINER"; then
  echo ">>> Docker nginx 컨테이너($NGINX_CONTAINER)가 실행 중이 아닙니다. scripts/setup-docker-nginx.sh를 먼저 실행하세요." >&2
  exit 1
fi

NGINX_NETWORKS=$(docker inspect --format='{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$NGINX_CONTAINER")
if ! echo "$NGINX_NETWORKS" | grep -qx "$NETWORK"; then
  log "nginx 컨테이너를 $NETWORK 네트워크에 연결"
  docker network connect "$NETWORK" "$NGINX_CONTAINER"
fi

# 현재 활성 컨테이너 확인
CURRENT=$(cat "$APP_DIR/current_container" 2>/dev/null || echo "")

if [ "$CURRENT" = "app-blue" ]; then
  NEW_CONTAINER="app-green"
  OLD_CONTAINER="app-blue"
else
  NEW_CONTAINER="app-blue"
  OLD_CONTAINER="app-green"
fi

log "현재: ${CURRENT:-없음} → 새로운: $NEW_CONTAINER"

# 새 이미지 pull
log "이미지 pull 중: $IMAGE"
docker pull "$IMAGE"

# 혹시 남아있는 동일 이름 컨테이너 정리
if docker ps -a --format '{{.Names}}' | grep -qx "$NEW_CONTAINER"; then
  log "기존 $NEW_CONTAINER 컨테이너 정리"
  docker stop "$NEW_CONTAINER" >/dev/null 2>&1 || true
  docker rm "$NEW_CONTAINER" >/dev/null 2>&1 || true
fi

# 새 컨테이너 실행 (트래픽 전환 전까지 Docker 기본 bridge에만 둔다)
log "새 컨테이너($NEW_CONTAINER) 실행 중"
docker run -d --name "$NEW_CONTAINER" \
  -e SPRING_PROFILES_ACTIVE=prod \
  --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval=5s \
  --health-timeout=3s \
  --health-start-period=30s \
  --health-retries=10 \
  "$IMAGE" >/dev/null

# 헬스체크 (docker inspect로 healthy 상태 확인)
log "헬스체크 시작"
for i in $(seq 1 60); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$NEW_CONTAINER" 2>/dev/null || echo "missing")
  if [ "$STATUS" = "healthy" ]; then
    log "헬스체크 통과! (${i}초)"
    break
  fi
  if [ "$STATUS" = "unhealthy" ]; then
    echo ">>> 헬스체크 unhealthy. 롤백합니다." >&2
    docker logs --tail=100 "$NEW_CONTAINER" >&2 || true
    cleanup_new_container
    exit 1
  fi
  if [ "$i" -eq 60 ]; then
    echo ">>> 헬스체크 타임아웃. 롤백합니다." >&2
    docker logs --tail=100 "$NEW_CONTAINER" >&2 || true
    cleanup_new_container
    exit 1
  fi
  sleep 1
done

# 새 컨테이너를 네트워크에 연결 (app alias 부여 → 이 순간 트래픽 전환)
log "트래픽 전환 중"
docker network connect --alias app "$NETWORK" "$NEW_CONTAINER"

# 이전 컨테이너 네트워크에서 분리 + 정리
if [ -n "$CURRENT" ] && docker ps -a --format '{{.Names}}' | grep -qx "$CURRENT"; then
  log "이전 컨테이너($CURRENT) 정리 중"
  docker network disconnect "$NETWORK" "$CURRENT" 2>/dev/null || true
  docker stop "$CURRENT" >/dev/null 2>&1 || true
  docker rm "$CURRENT" >/dev/null 2>&1 || true
elif docker ps -a --format '{{.Names}}' | grep -qx "$OLD_CONTAINER"; then
  log "남아있는 이전 후보 컨테이너($OLD_CONTAINER) 정리 중"
  docker network disconnect "$NETWORK" "$OLD_CONTAINER" 2>/dev/null || true
  docker stop "$OLD_CONTAINER" >/dev/null 2>&1 || true
  docker rm "$OLD_CONTAINER" >/dev/null 2>&1 || true
fi

# 현재 컨테이너 기록
echo "$NEW_CONTAINER" > "$APP_DIR/current_container"
rm -f "$APP_DIR/current_port"

# 오래된 Docker 이미지 정리
docker image prune -f >/dev/null

log "배포 완료! 활성 컨테이너: $NEW_CONTAINER"
