# Dockerfile for Dokus modular monolith server
# Expects the server JAR to be built locally before Docker build:
#   ./gradlew :backendApp:shadowJar
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    tzdata

# Create non-root user
RUN addgroup -g 1001 -S dokus && adduser -u 1001 -S dokus -G dokus

# Copy the pre-built fat JAR (shadowJar) (wildcard avoids version hardcoding)
COPY backendApp/build/libs/backendApp-*-all.jar app.jar

# Create directory for logs
RUN mkdir -p /app/logs && chown -R dokus:dokus /app

USER dokus

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
