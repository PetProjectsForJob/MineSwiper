# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Копируем Gradle wrapper и конфиги
COPY gradlew .
COPY gradle gradle/
COPY build.gradle settings.gradle ./
COPY src src/

# Делаем gradlew исполняемым
RUN chmod +x gradlew

# Собираем JAR
RUN ./gradlew clean build -x test --no-daemon --stacktrace

# Stage 2: Run
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Копируем JAR из builder
COPY --from=builder /app/build/libs/minesweeper-0.0.1-SNAPSHOT.jar app.jar

# Проброс порта
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java","-jar","app.jar","--server.port=8080"]
