# 배포 가이드

## 전체 흐름

```
main 브랜치 push
  → GitHub Actions (release.yml)
    ├─ 버전 계산
    ├─ GitHub Release 생성
    ├─ Docker 이미지 빌드 → ECR 푸시
    └─ EC2 SSH 접속 → 무중단 배포
```

---

## 버전 관리

`calculate-version` 액션이 git 태그를 기반으로 버전을 자동 계산합니다.

- 이미지 태그: `v{YYYY}.{MMDD}.{빌드번호}` (예: `v2026.0501.10`)
- ECR 이미지: `{ACCOUNT_ID}.dkr.ecr.ap-southeast-2.amazonaws.com/backend:v2026.0501.10`
- 컨테이너 이름: `app-v2026.0501.10`

---

## GitHub Actions 파이프라인

### 1. AWS 인증 (OIDC)

장기 자격증명(Access Key) 없이 GitHub Actions OIDC로 AWS Role을 임시 획득합니다.

```yaml
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
    aws-region: ${{ secrets.AWS_REGION }}
```

- IAM Role: `github-actions-ecr-push` (ECR push 권한)
- Trust Policy: `repo:JECT-Study/JECT2-4th-Server:ref:refs/heads/main` 으로 제한

### 2. Docker 이미지 빌드 & ECR 푸시

```
ECR: {ACCOUNT_ID}.dkr.ecr.ap-southeast-2.amazonaws.com/backend
태그: v{version}, latest 두 가지 동시 푸시
보존 정책: 최근 10개 이미지만 유지
```

### 3. EC2 배포

SSH로 EC2에 접속해 두 스크립트를 순서대로 실행합니다.

```bash
curl .../setup-docker-nginx.sh | bash        # nginx 설정
curl .../deploy.sh | bash -s {IMAGE}:{TAG}   # 앱 배포
```

---

## EC2 인프라 구성

```
EC2 (ap-southeast-2)
├─ Docker 네트워크: app-network
├─ nginx 컨테이너 (포트 80/443 외부 노출)
│   └─ upstream: app:8080 (DNS alias 기반)
└─ 앱 컨테이너: app-v{version}
    ├─ 포트 8080: API (nginx를 통해서만 접근)
    └─ 포트 8081: actuator (내부 전용, 외부 접근 불가)
```

### ECR 인증 (EC2)

EC2에 `ec2-ecr-pull` IAM Instance Profile이 연결되어 있습니다.
`amazon-ecr-credential-helper`가 Instance Profile 권한으로 ECR 인증을 자동 처리합니다.

```bash
# ~/.docker/config.json
{"credHelpers": {"637131561434.dkr.ecr.ap-southeast-2.amazonaws.com": "ecr-login"}}
```

---

## 무중단 배포 (deploy.sh)

### 동작 원리

nginx가 `app` DNS alias로 upstream을 동적 조회합니다 (`resolver 127.0.0.11 valid=5s`).
새 컨테이너가 `app` alias를 획득하면 nginx가 자동으로 트래픽을 전환합니다.

### 컨테이너 이름

Blue-Green 고정 이름(`app-blue`, `app-green`) 대신 버전 기반 이름을 사용합니다.

```bash
VERSION=$(echo "$IMAGE" | cut -d':' -f2)  # v2026.0501.10
NEW_CONTAINER="app-${VERSION}"             # app-v2026.0501.10
```

현재 활성 컨테이너는 `app` alias를 가진 컨테이너를 조회해 탐지합니다.

### 배포 단계

```
1. 새 이미지 pull
2. 새 컨테이너 기동 (app-network 미연결, 트래픽 없음)
3. Readiness probe 헬스체크 대기
   - 엔드포인트: GET http://localhost:8081/actuator/health/readiness
   - Docker HEALTHCHECK 기반 (Dockerfile에 정의)
   - start-period: 90s / interval: 10s / retries: 18 / timeout: 180s
4. healthy 확인 후 app alias 부여 (old + new 동시 트래픽 수신)
5. sleep 6s (nginx DNS 캐시 TTL 5s 만료 대기)
6. docker stop (SIGTERM → graceful shutdown → in-flight 요청 완료 후 종료)
7. docker network disconnect (DNS에서 old 제거)
8. docker rm
```

### Graceful Shutdown

```yaml
# application-prod.yml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

SIGTERM 수신 시 Spring Boot가 처리 중인 요청을 최대 30초간 완료 후 종료합니다.

### 헬스체크 실패 시 롤백

새 컨테이너가 타임아웃(180초) 내에 healthy 상태가 되지 않으면:
- 새 컨테이너를 즉시 stop & remove
- 기존 컨테이너는 그대로 유지 (트래픽 영향 없음)

---

## nginx 설정 (setup-docker-nginx.sh)

```
certbot/conf/live/{DOMAIN}/fullchain.pem 존재 여부 확인 (sudo)
  ├─ 있음: HTTPS 설정 적용 (80 → 443 리다이렉트, SSL)
  └─ 없음: HTTP bootstrap 설정 적용 (인증서 발급 전 임시)
```

> `live/` 디렉토리가 `root:root 700`으로 보호되므로 `sudo test -f`로 체크합니다.

HTTPS 인증서 발급은 `scripts/issue-https-cert.sh`를 참고하세요.

---

## 필수 GitHub Secrets

| Secret | 설명 |
|--------|------|
| `AWS_ROLE_ARN` | GitHub Actions용 IAM Role ARN |
| `AWS_REGION` | AWS 리전 (`ap-southeast-2`) |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USERNAME` | SSH 유저명 (`ubuntu`) |
| `EC2_SSH_KEY` | EC2 접속 PEM 키 |
| `DATABASE_HOST` | DB 호스트 |
| `DATABASE_USERNAME` | DB 유저명 |
| `DATABASE_PASSWORD` | DB 비밀번호 |
| `APP_JWT_SECRET` | JWT 서명 키 |
| `NPM_TOKEN` | npm 패키지 배포 토큰 |
| `DISCORD_WEBHOOK_URL` | 배포 알림 웹훅 |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 |
