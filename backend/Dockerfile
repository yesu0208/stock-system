# Build stage
FROM eclipse-temurin:21 AS build
WORKDIR /app

# Gradle wrapper와 설정 복사
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# 실행권한 부여
RUN chmod +x ./gradlew

# 빌드 (테스트 제외)
RUN ./gradlew clean bootJar -x test

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 결과 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Fargate용 포트
EXPOSE 8080

# 컨테이너 시작 시 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
