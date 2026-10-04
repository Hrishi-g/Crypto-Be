# --- Stage 1: Build stage ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies to leverage Docker caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package (skipping tests for faster production builds)
COPY src ./src
RUN mvn clean package -DskipTests

# Download and extract New Relic agent
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && unzip newrelic-java.zip -d /app

# --- Stage 2: Production run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security hardening in production
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built JAR from the builder stage
COPY --from=builder --chown=spring:spring /app/target/cryptx-0.0.1-SNAPSHOT.jar app.jar

# Copy New Relic agent from the builder stage
COPY --from=builder --chown=spring:spring /app/newrelic /app/newrelic

USER spring:spring

# Expose port 8080 (standard Spring Boot port)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
