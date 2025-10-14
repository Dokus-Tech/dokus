# Deployment and DevOps Tasks

## Current State
- Development environment using Docker
- Basic Docker Compose setup
- Local development configuration

## Task 1: Production Docker Configuration

### Production Dockerfiles
**Location:** Create production Dockerfiles for each service

#### Auth Service Production Dockerfile
**Location:** `features/auth/backend/Dockerfile.prod`

```dockerfile
# Multi-stage build for production
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Copy source code
COPY . .

# Build application
RUN gradle :features:auth:backend:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as non-root user
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

# Install health check dependencies
RUN apk add --no-cache curl

# Copy JAR from builder
COPY --from=builder /app/features/auth/backend/build/libs/*.jar app.jar
COPY --from=builder /app/features/auth/backend/src/main/resources ./resources

# Set ownership
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8091

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8091/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dconfig.override_with_env_vars=true", \
  "-jar", \
  "app.jar"]
```

### Docker Compose Production
**Location:** `docker-compose.prod.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-dokus}
      POSTGRES_USER: ${DB_USER:-dokus}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - dokus-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-dokus}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    networks:
      - dokus-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  auth-service:
    build:
      context: .
      dockerfile: features/auth/backend/Dockerfile.prod
    environment:
      ENVIRONMENT: production
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-dokus}
      DB_USER: ${DB_USER:-dokus}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_URL: redis://redis:6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - dokus-network
    restart: unless-stopped

  database-service:
    build:
      context: .
      dockerfile: foundation/database/Dockerfile.prod
    environment:
      ENVIRONMENT: production
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-dokus}
      DB_USER: ${DB_USER:-dokus}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_URL: redis://redis:6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - dokus-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx/nginx.prod.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - auth-service
      - database-service
    networks:
      - dokus-network
    restart: unless-stopped

networks:
  dokus-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
```

## Task 2: Kubernetes Deployment

### Kubernetes Manifests
**Location:** `k8s/`

#### Namespace
**Location:** `k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dokus-prod
```

#### ConfigMap
**Location:** `k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: dokus-config
  namespace: dokus-prod
data:
  ENVIRONMENT: "production"
  DB_HOST: "postgres-service"
  DB_PORT: "5432"
  DB_NAME: "dokus"
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
```

#### Auth Service Deployment
**Location:** `k8s/auth-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: dokus-prod
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: dokus/auth-service:latest
        ports:
        - containerPort: 8091
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: db-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: jwt-secret
        envFrom:
        - configMapRef:
            name: dokus-config
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8091
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8091
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: dokus-prod
spec:
  selector:
    app: auth-service
  ports:
  - port: 8091
    targetPort: 8091
  type: ClusterIP
```

#### Ingress Configuration
**Location:** `k8s/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: dokus-ingress
  namespace: dokus-prod
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.dokus.ai
    secretName: dokus-tls
  rules:
  - host: api.dokus.ai
    http:
      paths:
      - path: /auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8091
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: database-service
            port:
              number: 8070
```

## Task 3: CI/CD Pipeline

### GitHub Actions Workflow
**Location:** `.github/workflows/deploy.yml`

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: ${{ runner.os }}-gradle-
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: '**/build/test-results/test/TEST-*.xml'

  build:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Log in to GitHub Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Build and push Auth Service
      uses: docker/build-push-action@v4
      with:
        context: .
        file: features/auth/backend/Dockerfile.prod
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/auth:latest
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/auth:${{ github.sha }}
    
    - name: Build and push Database Service
      uses: docker/build-push-action@v4
      with:
        context: .
        file: foundation/database/Dockerfile.prod
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/database:latest
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/database:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Deploy to Kubernetes
      run: |
        # Setup kubectl
        # Apply manifests
        kubectl apply -f k8s/
        
        # Update image tags
        kubectl set image deployment/auth-service \
          auth-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/auth:${{ github.sha }} \
          -n dokus-prod
        
        kubectl set image deployment/database-service \
          database-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/database:${{ github.sha }} \
          -n dokus-prod
        
        # Wait for rollout
        kubectl rollout status deployment/auth-service -n dokus-prod
        kubectl rollout status deployment/database-service -n dokus-prod
```

## Task 4: Monitoring and Observability

### Prometheus Configuration
**Location:** `monitoring/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'auth-service'
    static_configs:
      - targets: ['auth-service:8091']
    metrics_path: '/metrics'
  
  - job_name: 'database-service'
    static_configs:
      - targets: ['database-service:8070']
    metrics_path: '/metrics'
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### Grafana Dashboards
**Location:** `monitoring/dashboards/`

Create JSON dashboards for:
- Application metrics (requests, latency, errors)
- Database metrics (connections, queries, slow queries)
- Business metrics (invoices created, payments processed)
- Infrastructure metrics (CPU, memory, disk)

## Task 5: Security Hardening

### Security Checklist
- [ ] Enable TLS/SSL for all services
- [ ] Implement rate limiting
- [ ] Set up WAF (Web Application Firewall)
- [ ] Configure CORS properly
- [ ] Implement API key authentication
- [ ] Set up intrusion detection
- [ ] Enable audit logging
- [ ] Implement backup encryption
- [ ] Set up vulnerability scanning
- [ ] Configure secret rotation

### Secrets Management
**Location:** `scripts/setup-secrets.sh`

```bash
#!/bin/bash

# Create Kubernetes secrets
kubectl create secret generic dokus-secrets \
  --from-literal=db-password=$(openssl rand -base64 32) \
  --from-literal=jwt-secret=$(openssl rand -base64 64) \
  --from-literal=redis-password=$(openssl rand -base64 32) \
  --from-literal=stripe-api-key=$STRIPE_API_KEY \
  --from-literal=mollie-api-key=$MOLLIE_API_KEY \
  -n dokus-prod

# Create TLS certificate
kubectl create secret tls dokus-tls \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key \
  -n dokus-prod
```

## Task 6: Backup and Disaster Recovery

### Database Backup Script
**Location:** `scripts/backup.sh`

```bash
#!/bin/bash

# Configuration
BACKUP_DIR="/backups"
DB_NAME="dokus"
S3_BUCKET="dokus-backups"
RETENTION_DAYS=30

# Create backup
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/dokus_${TIMESTAMP}.sql.gz"

# Dump database
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME | gzip > $BACKUP_FILE

# Upload to S3
aws s3 cp $BACKUP_FILE s3://${S3_BUCKET}/daily/

# Clean old backups
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
aws s3 rm s3://${S3_BUCKET}/daily/ --recursive --exclude "*" --include "*.sql.gz" \
  --include "dokus_*.sql.gz" --older-than="${RETENTION_DAYS}d"

# Verify backup
if [ $? -eq 0 ]; then
  echo "Backup successful: $BACKUP_FILE"
  # Send notification
else
  echo "Backup failed!"
  # Send alert
  exit 1
fi
```

### Restore Script
**Location:** `scripts/restore.sh`

```bash
#!/bin/bash

# Get latest backup from S3
LATEST_BACKUP=$(aws s3 ls s3://dokus-backups/daily/ | sort | tail -n 1 | awk '{print $4}')

# Download backup
aws s3 cp s3://dokus-backups/daily/$LATEST_BACKUP /tmp/

# Restore database
gunzip < /tmp/$LATEST_BACKUP | psql -h $DB_HOST -U $DB_USER -d $DB_NAME

echo "Database restored from $LATEST_BACKUP"
```

## Deployment Checklist

### Pre-Production
- [ ] All services containerized
- [ ] Environment variables configured
- [ ] Secrets management set up
- [ ] SSL certificates obtained
- [ ] Domain DNS configured
- [ ] Load balancer configured
- [ ] CDN configured (CloudFlare)
- [ ] Backup strategy implemented
- [ ] Monitoring dashboards created
- [ ] Alerts configured

### Production Launch
- [ ] Database migrations run
- [ ] Seed data loaded (if needed)
- [ ] Health checks passing
- [ ] Load testing completed
- [ ] Security scan completed
- [ ] Documentation updated
- [ ] Rollback plan tested
- [ ] Team notified
- [ ] Support channels ready

### Post-Launch
- [ ] Monitor error rates
- [ ] Check performance metrics
- [ ] Verify backup execution
- [ ] Review security logs
- [ ] Collect user feedback
- [ ] Plan next iteration

## Infrastructure Costs (Monthly)

### Option 1: Kubernetes (DigitalOcean)
- Kubernetes cluster (3 nodes): €90
- Managed PostgreSQL: €60
- Load Balancer: €12
- Object Storage: €5
- **Total: €167/month**

### Option 2: Docker Compose (VPS)
- VPS (8GB RAM): €40
- Managed PostgreSQL: €60
- Backup Storage: €5
- CDN: €20
- **Total: €125/month**

### Option 3: Serverless (AWS)
- Fargate containers: €80
- RDS PostgreSQL: €60
- ElastiCache Redis: €15
- S3 + CloudFront: €10
- **Total: €165/month**

## Support Scripts

### Health Check Script
**Location:** `scripts/health-check.sh`

```bash
#!/bin/bash

SERVICES=("auth-service:8091" "database-service:8070")

for SERVICE in "${SERVICES[@]}"; do
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://$SERVICE/health)
  if [ $RESPONSE -eq 200 ]; then
    echo "✅ $SERVICE is healthy"
  else
    echo "❌ $SERVICE is unhealthy (HTTP $RESPONSE)"
    # Send alert
  fi
done
```

## Next Steps
1. Choose deployment platform (Kubernetes/Docker/Serverless)
2. Set up CI/CD pipeline
3. Configure monitoring
4. Implement backup strategy
5. Perform security audit
6. Load test the system
7. Create runbooks
8. Train support team
