# ==============================================
# SchedulingService Dockerfile - Simple Pre-built JAR
# ==============================================
# Build JAR first: mvn clean package -DskipTests
# Then: docker-compose build

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the pre-built JAR file
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Copy SSL certificate files for Kafka
COPY src/main/resources/ca.pem /app/ca.pem
COPY src/main/resources/service.key /app/service.key
COPY src/main/resources/service.cert /app/service.cert

# Expose the service port
EXPOSE 8082

# Run the application with docker profile and file-based SSL truststore
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Dspring.kafka.ssl.trust-store-location=file:/app/ca.pem", "app.jar"]