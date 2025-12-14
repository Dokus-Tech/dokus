# Dockerfile for production deployment of Dokus web application
# Expects the WASM app to be built locally before Docker build
FROM nginx:alpine

# Clean out default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy built WASM static files from the build context
# When building from root: docker build -f composeApp/Dockerfile .
# Output from wasmJsBrowserDistribution task
COPY composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

# Copy nginx configuration
COPY composeApp/default.conf /etc/nginx/conf.d/default.conf

# Create non-root user for nginx
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser && \
    chown -R appuser:appuser /usr/share/nginx/html && \
    chown -R appuser:appuser /var/cache/nginx && \
    chown -R appuser:appuser /var/log/nginx && \
    chown -R appuser:appuser /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R appuser:appuser /var/run/nginx.pid

# Switch to non-root user
USER appuser

# Expose nginx port
EXPOSE 8080

# Health check (use 127.0.0.1 instead of localhost for IPv4)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/ || exit 1
