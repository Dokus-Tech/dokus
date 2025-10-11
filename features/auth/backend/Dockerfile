# Dockerfile for production deployment
# Expects the JAR to be built locally before Docker build
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Copy the pre-built JAR (using wildcard to avoid version hardcoding)
COPY features/auth/backend/build/libs/backend-*.jar app.jar

# Copy all resources (configs, migrations, etc.)
COPY features/auth/backend/src/main/resources/ ./resources/

# Create directory for logs
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose both possible ports (actual port will be determined by config)
EXPOSE 8091 9091 5005

# Health check (will use the correct port based on environment)
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8091/metrics || curl -f http://localhost:9091/metrics || exit 1

# Run the application
# The config file will be selected based on ENVIRONMENT variable
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=100", \
  "-Dconfig.override_with_env_vars=true", \
  "-jar", \
  "app.jar"]