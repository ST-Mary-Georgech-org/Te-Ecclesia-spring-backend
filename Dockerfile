FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Copy only necessary files first for better caching
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Download dependencies
RUN ./gradlew build --no-daemon --stacktrace -x test || return 0

# copy source code
COPY . .

# Ensure gradlew is executable
RUN chmod +x gradlew

# Build application
RUN ./gradlew :app:bootJar --no-daemon -x test

# ---- Stage 2: Run the application ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated JAR from the builder image
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]