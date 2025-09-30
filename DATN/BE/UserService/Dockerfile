# ----- Stage 1: Build the application with Maven -----
# Use a Maven image that includes the same JDK version your project needs
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory inside the build container
WORKDIR /app

# Copy the pom.xml to leverage Docker's layer caching
# If pom.xml doesn't change, Docker won't re-download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of your application's source code
COPY src ./src

# Run the maven package command to build your JAR file.
# -DskipTests is recommended for faster CI/CD builds
RUN mvn clean package -DskipTests


# ----- Stage 2: Create the final, lightweight runtime image -----
# Use a slim JRE for a smaller final image size
FROM openjdk:17-jre-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the 'build' stage into this final image
# The wildcard '*' is safe here because we know the build stage created exactly one JAR
COPY --from=build /app/target/*.jar app.jar

# Expose the port your application runs on
EXPOSE 8081

# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]