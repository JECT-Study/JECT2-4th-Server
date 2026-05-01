# api.vs.io.kr HTTPS 배포/인증서 발급 워크플로우

## 구조

- Route53: `api.vs.io.kr` A 레코드가 EC2 public IP를 가리켜야 합니다.
- NGINX: Docker 컨테이너가 80/443을 열고 Spring Boot 앱 컨테이너(`app:8080`)로 프록시합니다.
- 인증서: Let's Encrypt HTTP-01 webroot challenge를 사용합니다.
- 인증서 파일 위치(서버): `/home/ubuntu/app/certbot/conf`
- ACME challenge webroot(서버): `/home/ubuntu/app/certbot/www`

## 사전 준비

Route53에서 `api.vs.io.kr` A 레코드가 EC2 public IP를 가리키고 있어야 합니다.
또한 EC2 보안 그룹/방화벽에서 80, 443 포트가 열려 있어야 합니다.

DNS 설정은 이 저장소의 배포 스크립트에서 변경하지 않습니다.

## 서버 최초 세팅 + 인증서 발급(수동)

EC2에 SSH 접속 후 실행합니다.

```bash
curl -fsSL https://raw.githubusercontent.com/JECT-Study/JECT2-4th-Server/main/scripts/setup-docker-nginx.sh | bash
curl -fsSL https://raw.githubusercontent.com/JECT-Study/JECT2-4th-Server/main/scripts/issue-https-cert.sh \
  | CERTBOT_EMAIL=team@example.com bash
```

이 스크립트는 다음을 수행합니다.

1. 인증서가 없으면 HTTP bootstrap NGINX 설정을 적용합니다.
2. `http://api.vs.io.kr/.well-known/acme-challenge/...` 접근성을 확인합니다.
3. `certbot/certbot` Docker 이미지로 Let's Encrypt 인증서를 발급합니다.
4. HTTPS NGINX 설정으로 교체하고 reload 합니다.
5. 매일 03:17에 인증서 renew 후 NGINX reload 하는 cron을 등록합니다.

> 이메일 없이 발급하려면 `CERTBOT_EMAIL`을 생략할 수 있습니다. 테스트 발급은 `CERTBOT_STAGING=true`를 함께 지정하세요.

## GitHub Actions에서 수동 발급

`Actions` → `Issue HTTPS Certificate` → `Run workflow`를 실행합니다.

필요 secrets:

- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_SSH_KEY`

입력값:

- `domain`: 기본 `api.vs.io.kr`
- `certbot_email`: Let's Encrypt 알림 이메일(선택)
- `staging`: 테스트 발급 여부

## 일반 배포 시 동작

`release.yml`의 서버 배포 단계는 매번 `scripts/setup-docker-nginx.sh`를 먼저 실행합니다.

- 인증서가 없으면 HTTP 설정(`nginx/http.conf`)으로 NGINX를 기동합니다.
- 인증서가 있으면 HTTPS 설정(`nginx/app.conf`)으로 NGINX를 기동합니다.

따라서 최초 인증서 발급 이후에는 일반 배포만으로 HTTPS 설정이 유지됩니다.

## 점검 명령

```bash
curl -I http://api.vs.io.kr
curl -I https://api.vs.io.kr
ssh ubuntu@<EC2_HOST> 'cd /home/ubuntu/app && docker compose ps nginx && docker exec nginx nginx -t'
```

정상 상태:

- `http://api.vs.io.kr` → `301 https://api.vs.io.kr/...`
- `https://api.vs.io.kr` → 앱 응답 또는 Spring Security 응답
- `docker exec nginx nginx -t` → successful
