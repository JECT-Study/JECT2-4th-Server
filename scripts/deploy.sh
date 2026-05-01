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
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-180}"
HEALTH_LOG_INTERVAL_SECONDS="${HEALTH_LOG_INTERVAL_SECONDS:-10}"

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

REQUIRED_ENV_VARS=(
  DATABASE_HOST
  DATABASE_USERNAME
  DATABASE_PASSWORD
  APP_JWT_SECRET
  APP_JWT_ACCESS_TOKEN_EXPIRATION_SECONDS
  APP_JWT_REFRESH_TOKEN_EXPIRATION_SECONDS
)

MISSING_ENV_VARS=()
for ENV_VAR in "${REQUIRED_ENV_VARS[@]}"; do
  if [ -z "${!ENV_VAR:-}" ]; then
    MISSING_ENV_VARS+=("$ENV_VAR")
  fi
done

if [ "${#MISSING_ENV_VARS[@]}" -gt 0 ]; then
  echo ">>> 필수 운영 환경 변수가 없습니다: ${MISSING_ENV_VARS[*]}" >&2
  echo ">>> GitHub Actions secrets 또는 환경 변수로 값을 설정한 뒤 다시 배포하세요." >&2
  exit 1
fi

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

# ECR 이미지인 경우 로그인
if [[ "$IMAGE" == *".dkr.ecr."* ]]; then
  ECR_REGISTRY=$(echo "$IMAGE" | cut -d'/' -f1)
  REGION=$(echo "$IMAGE" | sed 's/.*\.dkr\.ecr\.\([^.]*\)\..*/\1/')

  if ! command -v aws &>/dev/null; then
    log "AWS CLI 설치 중..."
    curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
    unzip -q /tmp/awscliv2.zip -d /tmp/aws-install
    sudo /tmp/aws-install/aws/install --update
    rm -rf /tmp/awscliv2.zip /tmp/aws-install
  fi

  log "ECR 로그인 중: $ECR_REGISTRY"
  aws ecr get-login-password --region "$REGION" \
    | docker login --username AWS --password-stdin "$ECR_REGISTRY"
fi

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
  -e DATABASE_HOST \
  -e DATABASE_USERNAME \
  -e DATABASE_PASSWORD \
  -e APP_JWT_SECRET \
  -e APP_JWT_ACCESS_TOKEN_EXPIRATION_SECONDS \
  -e APP_JWT_REFRESH_TOKEN_EXPIRATION_SECONDS \
  --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval=10s \
  --health-timeout=5s \
  --health-start-period=90s \
  --health-retries=18 \
  "$IMAGE" >/dev/null

# 헬스체크 (docker inspect로 healthy 상태 확인)
# Docker health status가 start-period 직후 일시적으로 unhealthy가 될 수 있어
# unhealthy 즉시 실패 대신 전체 타임아웃까지 여유 있게 대기한다.
log "헬스체크 시작 (최대 ${HEALTH_TIMEOUT_SECONDS}초)"
HEALTH_PASSED=false
for i in $(seq 1 "$HEALTH_TIMEOUT_SECONDS"); do
  RUNNING=$(docker inspect --format='{{.State.Running}}' "$NEW_CONTAINER" 2>/dev/null || echo "false")
  STATUS=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}unknown{{end}}' "$NEW_CONTAINER" 2>/dev/null || echo "missing")

  if [ "$RUNNING" != "true" ]; then
    echo ">>> 새 컨테이너가 실행 중이 아닙니다. 롤백합니다." >&2
    docker logs --tail=100 "$NEW_CONTAINER" >&2 || true
    cleanup_new_container
    exit 1
  fi

  if [ "$STATUS" = "healthy" ]; then
    log "헬스체크 통과! (${i}초)"
    HEALTH_PASSED=true
    break
  fi

  if [ $((i % HEALTH_LOG_INTERVAL_SECONDS)) -eq 0 ]; then
    log "헬스체크 대기 중... status=$STATUS (${i}/${HEALTH_TIMEOUT_SECONDS}초)"
  fi

  sleep 1
done

if [ "$HEALTH_PASSED" != "true" ]; then
  echo ">>> 헬스체크 타임아웃. 롤백합니다." >&2
  docker logs --tail=100 "$NEW_CONTAINER" >&2 || true
  cleanup_new_container
  exit 1
fi

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
