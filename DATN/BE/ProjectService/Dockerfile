# ==============================================
# ProjectService Dockerfile - Simple Pre-built JAR
# ==============================================
# Build JAR first: mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the pre-built JAR file
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Copy Kafka SSL certificate (required for Aiven Kafka)
COPY src/main/resources/ca.pem /app/ca.pem

# Expose the service port
EXPOSE 8086

# Run the application with docker profile
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
