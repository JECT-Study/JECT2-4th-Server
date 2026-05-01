#!/bin/bash
set -euo pipefail

DOMAIN="${DOMAIN:-api.vs.io.kr}"
APP_DIR="${APP_DIR:-/home/ubuntu/app}"
REPO_RAW_BASE="${REPO_RAW_BASE:-https://raw.githubusercontent.com/JECT-Study/JECT2-4th-Server/main}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
CERTBOT_STAGING="${CERTBOT_STAGING:-false}"
NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"

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

mkdir -p "$APP_DIR/nginx" "$APP_DIR/certbot/www" "$APP_DIR/certbot/conf"
cd "$APP_DIR"

if [ ! -f "$APP_DIR/docker-compose.yaml" ]; then
  log "docker-compose.yaml 다운로드"
  curl -fsSL "$REPO_RAW_BASE/docker-compose.yaml" -o "$APP_DIR/docker-compose.yaml"
fi

if [ ! -f "$APP_DIR/certbot/conf/live/$DOMAIN/fullchain.pem" ] || [ ! -f "$APP_DIR/certbot/conf/live/$DOMAIN/privkey.pem" ]; then
  log "인증서가 없어 HTTP bootstrap nginx 설정을 적용"
  curl -fsSL "$REPO_RAW_BASE/nginx/http.conf" | sed "s/api.vs.io.kr/$DOMAIN/g" > "$APP_DIR/nginx/app.conf"
elif [ ! -f "$APP_DIR/nginx/app.conf" ]; then
  log "nginx HTTPS 설정 다운로드"
  curl -fsSL "$REPO_RAW_BASE/nginx/app.conf" | sed "s/api.vs.io.kr/$DOMAIN/g" > "$APP_DIR/nginx/app.conf"
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$NGINX_CONTAINER"; then
  log "nginx 컨테이너 시작(HTTP challenge용)"
  docker compose up -d nginx
fi

log "ACME webroot challenge 테스트 파일 생성"
TOKEN="health-$(date +%s)"
mkdir -p "$APP_DIR/certbot/www/.well-known/acme-challenge"
echo "$TOKEN" > "$APP_DIR/certbot/www/.well-known/acme-challenge/$TOKEN"
if ! curl -fsS --max-time 10 "http://$DOMAIN/.well-known/acme-challenge/$TOKEN" | grep -qx "$TOKEN"; then
  rm -f "$APP_DIR/certbot/www/.well-known/acme-challenge/$TOKEN"
  echo ">>> http://$DOMAIN/.well-known/acme-challenge/ 경로가 외부에서 접근되지 않습니다." >&2
  echo ">>> Route53 A/AAAA 레코드가 이 서버의 public IP를 가리키는지, 보안그룹이 80/443을 허용하는지 확인하세요." >&2
  exit 1
fi
rm -f "$APP_DIR/certbot/www/.well-known/acme-challenge/$TOKEN"

EMAIL_ARGS=(--register-unsafely-without-email)
if [ -n "$CERTBOT_EMAIL" ]; then
  EMAIL_ARGS=(--email "$CERTBOT_EMAIL")
fi

STAGING_ARGS=()
if [ "$CERTBOT_STAGING" = "true" ]; then
  STAGING_ARGS=(--staging)
fi

log "Let's Encrypt 인증서 발급: $DOMAIN"
docker run --rm \
  -v "$APP_DIR/certbot/conf:/etc/letsencrypt" \
  -v "$APP_DIR/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot \
  --webroot-path /var/www/certbot \
  --domain "$DOMAIN" \
  --agree-tos \
  --non-interactive \
  "${EMAIL_ARGS[@]}" \
  "${STAGING_ARGS[@]}"

log "HTTPS nginx 설정 적용"
curl -fsSL "$REPO_RAW_BASE/nginx/app.conf" | sed "s/api.vs.io.kr/$DOMAIN/g" > "$APP_DIR/nginx/app.conf"
docker compose up -d nginx
docker exec "$NGINX_CONTAINER" nginx -t
docker exec "$NGINX_CONTAINER" nginx -s reload

log "인증서 자동 갱신 cron 등록"
RENEW_SCRIPT="$APP_DIR/renew-https-cert.sh"
cat > "$RENEW_SCRIPT" <<RENEW_EOF
#!/bin/bash
set -euo pipefail
cd "$APP_DIR"
docker run --rm \\
  -v "$APP_DIR/certbot/conf:/etc/letsencrypt" \\
  -v "$APP_DIR/certbot/www:/var/www/certbot" \\
  certbot/certbot renew --webroot --webroot-path /var/www/certbot --quiet
if docker ps --format '{{.Names}}' | grep -qx "$NGINX_CONTAINER"; then
  docker exec "$NGINX_CONTAINER" nginx -s reload >/dev/null 2>&1 || true
fi
RENEW_EOF
chmod +x "$RENEW_SCRIPT"

CRON_LINE="17 3 * * * $RENEW_SCRIPT >> $APP_DIR/certbot/renew.log 2>&1"
TMP_CRON=$(mktemp)
{ crontab -l 2>/dev/null || true; } | grep -vF "$RENEW_SCRIPT" > "$TMP_CRON" || true
echo "$CRON_LINE" >> "$TMP_CRON"
crontab "$TMP_CRON"
rm -f "$TMP_CRON"

log "HTTPS 인증서 발급 및 NGINX 전환 완료: https://$DOMAIN"
