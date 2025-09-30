# ----- Stage 1: Build the application with Maven -----
# This stage remains the same. It correctly uses a Maven image with JDK 17.
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


# ----- Stage 2: Create the final, lightweight runtime image -----
# Use a secure and minimal JRE image from Eclipse Temurin. This is the corrected line.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR file from the 'build' stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your application runs on
EXPOSE 8081

# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]