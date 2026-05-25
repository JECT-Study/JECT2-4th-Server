# syntax=docker/dockerfile:1.7

# ===== 실행 스테이지 =====
FROM eclipse-temurin:25-jre
WORKDIR /app

# HEALTHCHECK에서 wget을 사용하므로 명시적으로 설치
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=build/libs/app.jar
COPY ${JAR_FILE} app.jar

HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=18 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
