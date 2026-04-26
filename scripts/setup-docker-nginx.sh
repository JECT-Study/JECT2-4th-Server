#!/bin/bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ubuntu/app}"
REPO_RAW_BASE="${REPO_RAW_BASE:-https://raw.githubusercontent.com/JECT-Study/JECT2-4th-Server/main}"
NETWORK="${NETWORK:-app-network}"
REMOVE_SYSTEM_NGINX="${REMOVE_SYSTEM_NGINX:-false}"

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

log "앱 디렉터리 준비: $APP_DIR"
mkdir -p "$APP_DIR/nginx"

log "repo에서 Docker nginx 설정 다운로드"
curl -fsSL "$REPO_RAW_BASE/docker-compose.yaml" -o "$APP_DIR/docker-compose.yaml"
curl -fsSL "$REPO_RAW_BASE/nginx/app.conf" -o "$APP_DIR/nginx/app.conf"

log "Docker 네트워크 확인: $NETWORK"
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

if systemctl list-unit-files nginx.service >/dev/null 2>&1; then
  log "기존 systemd nginx 중지/비활성화"
  sudo systemctl stop nginx 2>/dev/null || true
  sudo systemctl disable nginx 2>/dev/null || true
fi

if [ "$REMOVE_SYSTEM_NGINX" = "true" ]; then
  log "기존 OS nginx 패키지 제거"
  if command -v apt-get >/dev/null 2>&1; then
    sudo DEBIAN_FRONTEND=noninteractive apt-get remove -y nginx nginx-common || true
    sudo DEBIAN_FRONTEND=noninteractive apt-get autoremove -y || true
  else
    log "apt-get이 없어 패키지 제거는 건너뜁니다."
  fi
fi

log "Docker nginx 실행"
cd "$APP_DIR"
docker compose up -d nginx

log "Docker nginx 상태"
docker compose ps nginx

log "Docker nginx 설정 완료"
