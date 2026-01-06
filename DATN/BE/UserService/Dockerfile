# ==============================================
# UserService Dockerfile - Simple Pre-built JAR
# ==============================================
# This Dockerfile expects the JAR to be pre-built locally.
# Build JAR first: mvn clean package -DskipTests
# Then: docker-compose -f docker-compose.userservice.yml build
# ==============================================

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the pre-built JAR file
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Copy Kafka SSL certificate (required for Aiven Kafka)
COPY src/main/resources/ca.pem /app/ca.pem

# Expose the service port
EXPOSE 8081

# Run the application with docker profile
# Override the Kafka SSL truststore location to use file system path
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Dspring.kafka.ssl.trust-store-location=file:/app/ca.pem", "app.jar"]