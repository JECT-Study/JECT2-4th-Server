#!/bin/bash
IMAGE=$1

if [ -z "$IMAGE" ]; then
  echo ">>> 사용법: ./deploy.sh <이미지명:태그>"
  exit 1
fi

NETWORK="app-network"

# 현재 활성 컨테이너 확인
CURRENT=$(cat /home/ubuntu/app/current_container 2>/dev/null || echo "")

if [ "$CURRENT" == "app-blue" ]; then
  NEW_CONTAINER="app-green"
  OLD_CONTAINER="app-blue"
else
  NEW_CONTAINER="app-blue"
  OLD_CONTAINER="app-green"
fi

echo ">>> 현재: ${OLD_CONTAINER:-없음} → 새로운: $NEW_CONTAINER"

# 새 이미지 pull
echo ">>> 이미지 pull 중..."
docker pull $IMAGE

# 혹시 남아있는 동일 이름 컨테이너 정리
docker stop $NEW_CONTAINER 2>/dev/null && docker rm $NEW_CONTAINER 2>/dev/null

# 새 컨테이너 실행 (네트워크 연결 없이)
echo ">>> 새 컨테이너($NEW_CONTAINER) 실행 중..."
docker run -d --name $NEW_CONTAINER \
  -e SPRING_PROFILES_ACTIVE=prod \
  --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval=5s \
  --health-timeout=3s \
  --health-start-period=30s \
  --health-retries=10 \
  $IMAGE

# 헬스체크 (docker inspect로 healthy 상태 확인)
echo ">>> 헬스체크 시작..."
for i in $(seq 1 60); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' $NEW_CONTAINER 2>/dev/null)
  if [ "$STATUS" == "healthy" ]; then
    echo ">>> 헬스체크 통과! (${i}초)"
    break
  fi
  if [ $i -eq 60 ]; then
    echo ">>> 헬스체크 실패! 롤백합니다."
    docker stop $NEW_CONTAINER && docker rm $NEW_CONTAINER
    exit 1
  fi
  sleep 1
done

# 새 컨테이너를 네트워크에 연결 (app alias 부여 → 이 순간 트래픽 전환)
echo ">>> 트래픽 전환 중..."
docker network connect --alias app $NETWORK $NEW_CONTAINER

# 이전 컨테이너 네트워크에서 분리 + 정리
if [ -n "$CURRENT" ]; then
  echo ">>> 이전 컨테이너($OLD_CONTAINER) 정리 중..."
  docker network disconnect $NETWORK $OLD_CONTAINER 2>/dev/null
  docker stop $OLD_CONTAINER 2>/dev/null && docker rm $OLD_CONTAINER 2>/dev/null
fi

# 현재 컨테이너 기록
echo $NEW_CONTAINER > /home/ubuntu/app/current_container

# 오래된 Docker 이미지 정리
docker image prune -f

echo ">>> 배포 완료! 활성 컨테이너: $NEW_CONTAINER"