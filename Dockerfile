# syntax=docker/dockerfile:1.7

# ===== 실행 스테이지 =====
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ARG JAR_FILE=build/libs/app.jar
COPY ${JAR_FILE} app.jar

HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=18 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
