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

# Expose the service port
EXPOSE 8082

# Run the application with docker profile
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]