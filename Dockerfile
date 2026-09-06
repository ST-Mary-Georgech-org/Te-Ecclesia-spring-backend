FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the pre-built Spring Boot JAR from Gradle build
COPY app/build/libs/app-*.jar app.jar

# Expose Hugging Face default port
EXPOSE 7860

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar", "--server.port=7860"]