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

HEALTHCHECK --interval=5s --timeout=3s --start-period=30s --retries=10 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]