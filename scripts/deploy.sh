#!/bin/bash
IMAGE=$1
BLUE_PORT=8080
GREEN_PORT=8081

# 현재 활성 포트 확인
CURRENT_PORT=$(cat /home/ubuntu/app/current_port 2>/dev/null || echo $BLUE_PORT)

if [ "$CURRENT_PORT" == "$BLUE_PORT" ]; then
    NEW_PORT=$GREEN_PORT
    NEW_CONTAINER="green"
    OLD_CONTAINER="blue"
else
    NEW_PORT=$BLUE_PORT
    NEW_CONTAINER="blue"
    OLD_CONTAINER="green"
fi

echo ">>> 현재: $CURRENT_PORT → 새로운: $NEW_PORT"

# 새 이미지 pull
echo ">>> 이미지 pull 중..."
docker pull $IMAGE

# 새 컨테이너 실행
echo ">>> 새 컨테이너($NEW_CONTAINER) 실행 중..."
docker run -d --name $NEW_CONTAINER \
    -p $NEW_PORT:8080 \
    -e SPRING_PROFILES_ACTIVE=prod \
    $IMAGE

# Docker 헬스체크 상태 확인 방식
echo ">>> 헬스체크 시작..."
for i in {1..60}; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' $NEW_CONTAINER 2>/dev/null || echo "starting")
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

# Nginx 트래픽 전환
echo ">>> Nginx 트래픽 전환 중..."
sudo sed -i "s/server 127.0.0.1:[0-9]*/server 127.0.0.1:$NEW_PORT/" /etc/nginx/conf.d/app.conf
sudo nginx -s reload

# 이전 컨테이너 정리
echo ">>> 이전 컨테이너($OLD_CONTAINER) 정리 중..."
docker stop $OLD_CONTAINER 2>/dev/null && docker rm $OLD_CONTAINER 2>/dev/null

# 현재 포트 기록
echo $NEW_PORT > /home/ubuntu/app/current_port

# 오래된 Docker 이미지 정리
docker image prune -f

echo ">>> 배포 완료! 활성 포트: $NEW_PORT"
