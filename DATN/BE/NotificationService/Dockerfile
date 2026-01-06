# ==============================================
# NotificationService Dockerfile
# ==============================================

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Copy Kafka SSL certificate
COPY src/main/resources/ca.pem /app/ca.pem

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
