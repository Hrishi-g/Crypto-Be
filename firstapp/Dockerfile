# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy configuration and source files
COPY pom.xml .

RUN mvn dependency:go-offline
COPY src ./src

# Build the package and skip running tests to speed up the build
RUN mvn clean package -DskipTests -Dsentry.skip=true

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot's default port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar","/app/firstapp-0.0.1-SNAPSHOT.jar"]