# ===== 빌드 스테이지 =====
FROM gradle:jdk25 AS build
WORKDIR /app

# 의존성 먼저 복사 (캐싱 활용)
COPY build.gradle.kts settings.gradle.kts ./
COPY buildSrc ./buildSrc
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 & 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# ===== 실행 스테이지 =====
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=18 \
  CMD curl -f http://localhost:8081/actuator/health/readiness || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]