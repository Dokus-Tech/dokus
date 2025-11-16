# Dockerfile for production deployment
# Expects the JAR to be built locally before Docker build
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Copy the pre-built JAR (using wildcard to avoid version hardcoding)
COPY features/payment/backend/build/libs/backend-*.jar app.jar

# Copy all resources (configs, migrations, etc.)
COPY features/payment/backend/src/main/resources/ ./resources/

# Create directory for logs
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 6093

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:6093/health || exit 1

# Run the application
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=100", \
  "-Dconfig.override_with_env_vars=true", \
  "-jar", \
  "app.jar"]
