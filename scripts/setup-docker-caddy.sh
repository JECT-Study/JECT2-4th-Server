#!/bin/bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ubuntu/app}"
DOMAIN="${DOMAIN:-api.vs.io.kr}"
REPO_RAW_BASE="${REPO_RAW_BASE:-https://raw.githubusercontent.com/JECT-Study/JECT2-4th-Server/main}"
NETWORK="${NETWORK:-app-network}"
CADDY_CONTAINER="${CADDY_CONTAINER:-caddy}"

log() {
  echo ">>> $*"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo ">>> 필수 명령어를 찾을 수 없습니다: $1" >&2
    exit 1
  fi
}

require_command curl
require_command docker

if ! docker compose version >/dev/null 2>&1; then
  echo ">>> Docker Compose v2 플러그인을 찾을 수 없습니다." >&2
  exit 1
fi

mkdir -p "$APP_DIR"

log "docker-compose.yaml 다운로드"
curl -fsSL "$REPO_RAW_BASE/docker-compose.yaml" -o "$APP_DIR/docker-compose.yaml"

log "Caddyfile 다운로드"
curl -fsSL "$REPO_RAW_BASE/Caddyfile" | sed "s/api.vs.io.kr/$DOMAIN/g" > "$APP_DIR/Caddyfile"

log "Docker 네트워크 확인: $NETWORK"
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

log "Caddy 실행"
cd "$APP_DIR"
docker compose up -d caddy

log "Caddy 상태"
docker compose ps caddy

log "Caddy 설정 완료 (HTTPS 인증서는 첫 요청 시 자동 발급됩니다)"
