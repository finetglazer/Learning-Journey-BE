# ----- Stage 1: Build the application with Maven -----
# Use your pre-built base image from Docker Hub
FROM ologndev/datn-be-images:latest AS build

WORKDIR /app

# We already have dependencies, so we just need to copy the code
COPY src ./src
# This step will now be very fast
RUN mvn clean package -DskipTests


# ----- Stage 2: Create the final, lightweight runtime image -----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]