# Production Dockerfile - multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle files for dependency caching
COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY build-logic build-logic
COPY foundation foundation
COPY features/processor features/processor
COPY features/ai features/ai

# Build the application
RUN chmod +x gradlew && ./gradlew :features:processor:backend:shadowJar --no-daemon -x test

# Production image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Copy the built JAR
COPY --from=builder /build/features/processor/backend/build/libs/backend-*.jar app.jar

# Copy resources
COPY features/processor/backend/src/main/resources/ ./resources/

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx1g", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=100", \
  "-Dconfig.override_with_env_vars=true", \
  "-jar", \
  "app.jar"]
