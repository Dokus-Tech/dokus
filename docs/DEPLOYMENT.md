# Deployment Guide

**Last Updated:** October 2025
**Status:** Production Deployment Guide

---

## Table of Contents

1. [Deployment Overview](#deployment-overview)
2. [Environment Configuration](#environment-configuration)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Cloud Platform Options](#cloud-platform-options)
6. [Database Deployment](#database-deployment)
7. [Monitoring & Logging](#monitoring--logging)
8. [CI/CD Pipeline](#cicd-pipeline)
9. [Scaling Strategy](#scaling-strategy)
10. [Backup & Disaster Recovery](#backup--disaster-recovery)
11. [Security Checklist](#security-checklist)

---

## Deployment Overview

Dokus follows a **microservices architecture** with independent backend services and multiplatform client applications. This guide covers deploying both the backend infrastructure and distributing client applications.

### Deployment Components

**Backend Services:**
- Auth Service (Ktor)
- Invoicing Service (Ktor)
- Expense Service (Ktor)
- Payment Service (Ktor)
- Reporting Service (Ktor)

**Infrastructure:**
- PostgreSQL 17 (primary database)
- Redis 8 (cache/sessions)
- Nginx (reverse proxy)
- Prometheus + Grafana (monitoring)

**Client Applications:**
- Web (WASM) - Hosted on CDN
- Android - Google Play Store
- iOS - Apple App Store
- Desktop - Direct download (DMG/MSI/DEB)

---

## Environment Configuration

### Environment Variables

**Required for all services:**

```bash
# Database
DATABASE_HOST=postgres.dokus.internal
DATABASE_PORT=5432
DATABASE_NAME=dokus
DATABASE_USER=dokus_user
DATABASE_PASSWORD=<secure-password>
DATABASE_POOL_SIZE=20

# Redis
REDIS_HOST=redis.dokus.internal
REDIS_PORT=6379
REDIS_PASSWORD=<secure-password>

# JWT Authentication
JWT_SECRET=<256-bit-secret>
JWT_ACCESS_TOKEN_EXPIRY=900         # 15 minutes in seconds
JWT_REFRESH_TOKEN_EXPIRY=604800     # 7 days in seconds

# Service Configuration
SERVICE_PORT=8080
LOG_LEVEL=INFO
ENVIRONMENT=production              # development|staging|production

# External Services
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USER=apikey
SMTP_PASSWORD=<sendgrid-api-key>

STRIPE_API_KEY=<stripe-secret-key>
STRIPE_WEBHOOK_SECRET=<webhook-secret>

# Peppol (for invoicing service)
PEPPOL_ENDPOINT=https://peppol-partner.example.com/api
PEPPOL_API_KEY=<peppol-api-key>
```

**Service-specific variables:**

```bash
# Auth Service
AUTH_SERVICE_URL=https://auth.dokus.ai
MFA_ISSUER=Dokus

# Invoicing Service
INVOICING_SERVICE_URL=https://invoicing.dokus.ai
INVOICE_PDF_STORAGE=s3://dokus-invoices/

# Payment Service
PAYMENT_SERVICE_URL=https://payment.dokus.ai
MOLLIE_API_KEY=<mollie-api-key>
```

### Configuration Management

**Development:** `.env` files (never commit to git)

```bash
# .env.development
DATABASE_HOST=localhost
DATABASE_PORT=5432
LOG_LEVEL=DEBUG
```

**Production:** Kubernetes Secrets or cloud provider secret managers

```bash
# Create Kubernetes secret
kubectl create secret generic dokus-secrets \
  --from-literal=database-password=<password> \
  --from-literal=jwt-secret=<secret> \
  --from-literal=redis-password=<password> \
  --namespace=dokus
```

---

## Docker Deployment

### Development Environment

**docker-compose.dev.yml:**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: dokus_dev
      POSTGRES_USER: dokus
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./server/database/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:8-alpine
    command: redis-server --requirepass dev_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  auth-service:
    build:
      context: .
      dockerfile: features/auth/backend/Dockerfile
    environment:
      DATABASE_HOST: postgres
      DATABASE_NAME: dokus_dev
      REDIS_HOST: redis
      JWT_SECRET: dev-secret-change-in-production
      LOG_LEVEL: DEBUG
    ports:
      - "8000:8080"
    depends_on:
      - postgres
      - redis

  invoicing-service:
    build:
      context: .
      dockerfile: features/invoicing/backend/Dockerfile
    environment:
      DATABASE_HOST: postgres
      REDIS_HOST: redis
      LOG_LEVEL: DEBUG
    ports:
      - "8001:8080"
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
  redis_data:
```

**Start development environment:**

```bash
# Start all services
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down
```

### Production Environment

**docker-compose.prod.yml:**

```yaml
version: '3.9'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
      - ./static:/usr/share/nginx/html
    depends_on:
      - auth-service
      - invoicing-service
      - expense-service
      - payment-service

  auth-service:
    image: registry.dokus.ai/dokus/auth-service:${VERSION}
    env_file:
      - .env.production
    deploy:
      replicas: 3
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  invoicing-service:
    image: registry.dokus.ai/dokus/invoicing-service:${VERSION}
    env_file:
      - .env.production
    deploy:
      replicas: 3

  expense-service:
    image: registry.dokus.ai/dokus/expense-service:${VERSION}
    env_file:
      - .env.production
    deploy:
      replicas: 2

  payment-service:
    image: registry.dokus.ai/dokus/payment-service:${VERSION}
    env_file:
      - .env.production
    deploy:
      replicas: 2

  postgres:
    image: postgres:17-alpine
    env_file:
      - .env.production
    volumes:
      - postgres_data:/var/lib/postgresql/data
    deploy:
      placement:
        constraints:
          - node.role == manager

  redis:
    image: redis:8-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    deploy:
      placement:
        constraints:
          - node.role == manager

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

**Deploy to production:**

```bash
# Set version
export VERSION=1.0.0

# Deploy with Docker Swarm
docker stack deploy -c docker-compose.prod.yml dokus

# Check status
docker stack services dokus

# View logs
docker service logs dokus_auth-service
```

### Building Docker Images

**Dockerfile for Ktor services:**

```dockerfile
# features/auth/backend/Dockerfile
FROM gradle:8.12-jdk21 AS build
WORKDIR /app

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source code
COPY features/auth/backend ./features/auth/backend
COPY foundation ./foundation

# Build fat JAR
RUN gradle :features:auth:backend:shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built JAR
COPY --from=build /app/features/auth/backend/build/libs/*-all.jar app.jar

# Run as non-root user
RUN addgroup -S dokus && adduser -S dokus -G dokus
USER dokus

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**Build and push images:**

```bash
# Build image
docker build -t registry.dokus.ai/dokus/auth-service:1.0.0 \
  -f features/auth/backend/Dockerfile .

# Push to registry
docker push registry.dokus.ai/dokus/auth-service:1.0.0

# Tag as latest
docker tag registry.dokus.ai/dokus/auth-service:1.0.0 \
  registry.dokus.ai/dokus/auth-service:latest
docker push registry.dokus.ai/dokus/auth-service:latest
```

---

## Kubernetes Deployment

### Namespace Setup

```yaml
# kubernetes/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dokus
  labels:
    environment: production
```

### Secrets

```yaml
# kubernetes/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: dokus-secrets
  namespace: dokus
type: Opaque
stringData:
  database-host: "postgres.dokus.svc.cluster.local"
  database-name: "dokus"
  database-user: "dokus_user"
  database-password: "<secure-password>"
  redis-host: "redis.dokus.svc.cluster.local"
  redis-password: "<secure-password>"
  jwt-secret: "<256-bit-secret>"
  smtp-password: "<smtp-password>"
  stripe-api-key: "<stripe-key>"
```

### Auth Service Deployment

```yaml
# kubernetes/auth-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dokus-auth-service
  namespace: dokus
  labels:
    app: dokus-auth-service
    version: v1.0.0
spec:
  replicas: 3
  selector:
    matchLabels:
      app: dokus-auth-service
  template:
    metadata:
      labels:
        app: dokus-auth-service
        version: v1.0.0
    spec:
      containers:
      - name: auth-service
        image: registry.dokus.ai/dokus/auth-service:1.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: DATABASE_HOST
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: database-host
        - name: DATABASE_NAME
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: database-name
        - name: DATABASE_USER
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: database-user
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: database-password
        - name: REDIS_HOST
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: redis-host
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: redis-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: dokus-secrets
              key: jwt-secret
        - name: SERVICE_PORT
          value: "8080"
        - name: LOG_LEVEL
          value: "INFO"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: dokus-auth-service
  namespace: dokus
spec:
  selector:
    app: dokus-auth-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

### Ingress Configuration

```yaml
# kubernetes/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: dokus-ingress
  namespace: dokus
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://app.dokus.ai"
spec:
  ingressClassName: nginx
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
            name: dokus-auth-service
            port:
              number: 80
      - path: /invoices
        pathType: Prefix
        backend:
          service:
            name: dokus-invoicing-service
            port:
              number: 80
      - path: /expenses
        pathType: Prefix
        backend:
          service:
            name: dokus-expense-service
            port:
              number: 80
      - path: /payments
        pathType: Prefix
        backend:
          service:
            name: dokus-payment-service
            port:
              number: 80
```

### Horizontal Pod Autoscaler

```yaml
# kubernetes/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: dokus-auth-hpa
  namespace: dokus
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: dokus-auth-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Deploy to Kubernetes

```bash
# Apply all configurations
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/auth-deployment.yaml
kubectl apply -f kubernetes/invoicing-deployment.yaml
kubectl apply -f kubernetes/expense-deployment.yaml
kubectl apply -f kubernetes/payment-deployment.yaml
kubectl apply -f kubernetes/ingress.yaml
kubectl apply -f kubernetes/hpa.yaml

# Check deployment status
kubectl get pods -n dokus
kubectl get services -n dokus
kubectl get ingress -n dokus

# View logs
kubectl logs -f deployment/dokus-auth-service -n dokus

# Scale manually
kubectl scale deployment dokus-auth-service --replicas=5 -n dokus
```

---

## Cloud Platform Options

### AWS Deployment

**Recommended Services:**
- **EKS** (Elastic Kubernetes Service) for microservices
- **RDS PostgreSQL** for database
- **ElastiCache Redis** for caching
- **S3** for invoice PDFs and receipts
- **CloudFront** for CDN (Web app)
- **Route 53** for DNS
- **Certificate Manager** for SSL/TLS
- **CloudWatch** for monitoring

**Architecture:**

```
Internet
    ↓
Route 53 (DNS)
    ↓
CloudFront (CDN)
    ↓
Application Load Balancer
    ↓
EKS Cluster (us-east-1)
├── Auth Service (3 pods)
├── Invoicing Service (3 pods)
├── Expense Service (2 pods)
└── Payment Service (2 pods)
    ↓
├── RDS PostgreSQL (Multi-AZ)
├── ElastiCache Redis (Cluster)
└── S3 (Invoice storage)
```

**Terraform Example:**

```hcl
# terraform/aws/main.tf
provider "aws" {
  region = "eu-west-1"  # Ireland for European customers
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "dokus-prod"
  cluster_version = "1.28"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    main = {
      min_size     = 3
      max_size     = 10
      desired_size = 3

      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"
    }
  }
}

module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "dokus-prod"

  engine               = "postgres"
  engine_version       = "17.0"
  family               = "postgres17"
  major_engine_version = "17"
  instance_class       = "db.t4g.large"

  allocated_storage     = 100
  max_allocated_storage = 500

  db_name  = "dokus"
  username = "dokus_admin"
  port     = 5432

  multi_az               = true
  db_subnet_group_name   = module.vpc.database_subnet_group
  vpc_security_group_ids = [module.security_group_rds.security_group_id]

  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
}
```

**Deploy to AWS:**

```bash
# Configure kubectl for EKS
aws eks update-kubeconfig --name dokus-prod --region eu-west-1

# Deploy using kubectl
kubectl apply -f kubernetes/

# Or use Helm
helm install dokus ./helm/dokus -n dokus
```

### Google Cloud Platform (GCP)

**Recommended Services:**
- **GKE** (Google Kubernetes Engine)
- **Cloud SQL PostgreSQL**
- **Memorystore Redis**
- **Cloud Storage** for files
- **Cloud CDN**
- **Cloud Load Balancing**
- **Cloud Monitoring**

### Azure

**Recommended Services:**
- **AKS** (Azure Kubernetes Service)
- **Azure Database for PostgreSQL**
- **Azure Cache for Redis**
- **Azure Blob Storage**
- **Azure CDN**
- **Azure Front Door**
- **Azure Monitor**

### DigitalOcean (Cost-Effective Option)

**Recommended Services:**
- **Kubernetes** (DOKS)
- **Managed PostgreSQL**
- **Managed Redis**
- **Spaces** (S3-compatible storage)
- **CDN**
- **Load Balancer**

**Estimated Cost:** €200-300/month for 0-1,000 users

```bash
# Install doctl CLI
brew install doctl

# Authenticate
doctl auth init

# Create Kubernetes cluster
doctl kubernetes cluster create dokus-prod \
  --region fra1 \
  --version 1.28 \
  --node-pool "name=main;size=s-2vcpu-4gb;count=3"

# Get kubeconfig
doctl kubernetes cluster kubeconfig save dokus-prod
```

---

## Database Deployment

### Managed PostgreSQL (Recommended)

**Advantages:**
- Automatic backups
- Point-in-time recovery
- Automatic failover
- Security patches
- Monitoring included

**Configuration:**

```sql
-- Initial database setup
CREATE DATABASE dokus;
CREATE USER dokus_app WITH ENCRYPTED PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE dokus TO dokus_app;

-- Enable extensions
\c dokus
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For full-text search
```

### Running Migrations

**Using Flyway CLI:**

```bash
# Install Flyway
brew install flyway

# Configure flyway.conf
cat > flyway.conf << EOF
flyway.url=jdbc:postgresql://postgres.dokus.internal:5432/dokus
flyway.user=dokus_app
flyway.password=<password>
flyway.locations=filesystem:./foundation/database/migrations
EOF

# Run migrations
flyway migrate

# Check migration status
flyway info

# Rollback (if supported)
flyway undo
```

**In Docker:**

```bash
docker run --rm \
  -v $(pwd)/foundation/database/migrations:/flyway/sql \
  flyway/flyway:latest \
  -url=jdbc:postgresql://postgres:5432/dokus \
  -user=dokus_app \
  -password=<password> \
  migrate
```

**In CI/CD Pipeline:**

```yaml
# .github/workflows/deploy.yml
- name: Run Database Migrations
  run: |
    ./gradlew :foundation:database:flywayMigrate \
      -Dflyway.url=${{ secrets.DATABASE_URL }} \
      -Dflyway.user=${{ secrets.DATABASE_USER }} \
      -Dflyway.password=${{ secrets.DATABASE_PASSWORD }}
```

### Database Connection Pooling

**HikariCP Configuration (in application.conf):**

```hocon
# Ktor application.conf
database {
  driver = "org.postgresql.Driver"
  url = "jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}"
  user = ${DATABASE_USER}
  password = ${DATABASE_PASSWORD}

  hikari {
    maximumPoolSize = 20
    minimumIdle = 5
    idleTimeout = 600000        # 10 minutes
    connectionTimeout = 30000   # 30 seconds
    maxLifetime = 1800000       # 30 minutes
    leakDetectionThreshold = 60000  # 1 minute
  }
}
```

---

## Monitoring & Logging

### Prometheus Metrics

**prometheus.yml:**

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'dokus-auth'
    static_configs:
      - targets: ['auth-service:8080']
    metrics_path: '/metrics'

  - job_name: 'dokus-invoicing'
    static_configs:
      - targets: ['invoicing-service:8080']
    metrics_path: '/metrics'

  - job_name: 'dokus-postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
```

**Exposing Metrics in Ktor:**

```kotlin
// Install Micrometer plugin
install(MicrometerMetrics) {
    registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    meterBinders = listOf(
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        ProcessorMetrics(),
        UptimeMetrics()
    )
}

// Expose /metrics endpoint
routing {
    get("/metrics") {
        call.respond(registry.scrape())
    }
}
```

### Grafana Dashboards

**Key Metrics to Monitor:**

1. **Application Metrics:**
   - Request rate (requests/second)
   - Error rate (%)
   - Response times (p50, p95, p99)
   - Active sessions

2. **Database Metrics:**
   - Query execution time
   - Connection pool usage
   - Deadlocks
   - Slow queries

3. **Infrastructure Metrics:**
   - CPU usage (%)
   - Memory usage (%)
   - Disk I/O
   - Network traffic

4. **Business Metrics:**
   - Active tenants
   - Invoices created/sent per day
   - Payment success rate
   - API errors by tenant

### Structured Logging

**Logback configuration (logback.xml):**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <logger name="ai.dokus" level="INFO"/>
    <logger name="io.ktor" level="INFO"/>
    <logger name="Exposed" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

**Request Context Logging:**

```kotlin
install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/api") }
    mdc("requestId") { UUID.randomUUID().toString() }
    mdc("tenantId") { call.principal<JWTPrincipal>()?.payload?.getClaim("tenant_id")?.asString() }
}
```

### Centralized Logging

**Options:**
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Loki + Grafana**
- **Cloud provider logging** (CloudWatch, Stackdriver, Azure Monitor)

**Loki setup (Docker):**

```yaml
# docker-compose.logging.yml
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yaml:/etc/loki/local-config.yaml
    command: -config.file=/etc/loki/local-config.yaml

  promtail:
    image: grafana/promtail:latest
    volumes:
      - /var/log:/var/log
      - ./promtail-config.yaml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
```

---

## CI/CD Pipeline

### GitHub Actions

**.github/workflows/deploy.yml:**

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
    tags: ['v*']

env:
  REGISTRY: registry.dokus.ai
  IMAGE_PREFIX: dokus

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew test

      - name: Run integration tests
        run: ./gradlew integrationTest
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/dokus_test

    services:
      postgres:
        image: postgres:17-alpine
        env:
          POSTGRES_DB: dokus_test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [auth, invoicing, expense, payment]

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ secrets.REGISTRY_USERNAME }}
          password: ${{ secrets.REGISTRY_PASSWORD }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}-service
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          file: features/${{ matrix.service }}/backend/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')

    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/k8s-set-context@v4
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG }}

      - name: Run database migrations
        run: |
          kubectl run flyway-migrate --rm -i --restart=Never \
            --image=flyway/flyway:latest \
            --env="FLYWAY_URL=${{ secrets.DATABASE_URL }}" \
            --env="FLYWAY_USER=${{ secrets.DATABASE_USER }}" \
            --env="FLYWAY_PASSWORD=${{ secrets.DATABASE_PASSWORD }}" \
            -- migrate

      - name: Deploy to Kubernetes
        run: |
          export VERSION=${GITHUB_REF#refs/tags/v}
          envsubst < kubernetes/auth-deployment.yaml | kubectl apply -f -
          envsubst < kubernetes/invoicing-deployment.yaml | kubectl apply -f -
          envsubst < kubernetes/expense-deployment.yaml | kubectl apply -f -
          envsubst < kubernetes/payment-deployment.yaml | kubectl apply -f -

      - name: Wait for rollout
        run: |
          kubectl rollout status deployment/dokus-auth-service -n dokus
          kubectl rollout status deployment/dokus-invoicing-service -n dokus

      - name: Smoke tests
        run: |
          curl -f https://api.dokus.ai/health || exit 1
          curl -f https://api.dokus.ai/metrics || exit 1

  deploy-web:
    needs: test
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build WASM app
        run: ./gradlew wasmJsBrowserProductionWebpack

      - name: Deploy to CDN
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: eu-west-1

      - name: Upload to S3
        run: |
          aws s3 sync composeApp/build/dist/wasmJs/productionExecutable \
            s3://app.dokus.ai/ \
            --delete \
            --cache-control max-age=31536000

      - name: Invalidate CloudFront
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"
```

### GitLab CI/CD

**.gitlab-ci.yml:**

```yaml
stages:
  - test
  - build
  - deploy

variables:
  DOCKER_DRIVER: overlay2
  REGISTRY: registry.dokus.ai
  IMAGE_PREFIX: dokus

test:
  stage: test
  image: gradle:8.12-jdk21
  services:
    - postgres:17-alpine
  variables:
    POSTGRES_DB: dokus_test
    POSTGRES_USER: dokus
    POSTGRES_PASSWORD: test
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: '**/build/test-results/test/TEST-*.xml'

build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $REGISTRY
  script:
    - docker build -t $REGISTRY/$IMAGE_PREFIX/auth-service:$CI_COMMIT_TAG \
        -f features/auth/backend/Dockerfile .
    - docker push $REGISTRY/$IMAGE_PREFIX/auth-service:$CI_COMMIT_TAG
  only:
    - tags

deploy:production:
  stage: deploy
  image: bitnami/kubectl:latest
  before_script:
    - echo $KUBE_CONFIG | base64 -d > kubeconfig
    - export KUBECONFIG=kubeconfig
  script:
    - kubectl set image deployment/dokus-auth-service \
        auth-service=$REGISTRY/$IMAGE_PREFIX/auth-service:$CI_COMMIT_TAG \
        -n dokus
    - kubectl rollout status deployment/dokus-auth-service -n dokus
  only:
    - tags
  environment:
    name: production
    url: https://api.dokus.ai
```

---

## Scaling Strategy

### Vertical Scaling (Single Service)

**Database:**
```bash
# Increase RDS instance size
aws rds modify-db-instance \
  --db-instance-identifier dokus-prod \
  --db-instance-class db.m6g.xlarge \
  --apply-immediately
```

**Kubernetes Pods:**
```yaml
resources:
  requests:
    memory: "1Gi"    # Increased from 512Mi
    cpu: "500m"      # Increased from 250m
  limits:
    memory: "2Gi"    # Increased from 1Gi
    cpu: "1000m"     # Increased from 500m
```

### Horizontal Scaling (Multiple Replicas)

**Manual scaling:**
```bash
kubectl scale deployment dokus-auth-service --replicas=10 -n dokus
```

**Auto-scaling (HPA):**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: dokus-auth-hpa
spec:
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Database Scaling

**Read Replicas:**
```sql
-- Configure read-only connections
val readOnlyConnection = Database.connect(
    url = "jdbc:postgresql://read-replica.dokus.internal:5432/dokus",
    driver = "org.postgresql.Driver",
    user = "dokus_app",
    password = password
)

// Use read replica for queries
transaction(readOnlyConnection) {
    Invoices.selectAll().toList()
}
```

**Sharding by tenant_id:**
```kotlin
// Route tenant to specific shard
fun getDatabaseForTenant(tenantId: UUID): Database {
    val shardId = tenantId.hashCode() % NUM_SHARDS
    return shardDatabases[shardId]
}
```

### Caching Strategy

**Redis caching:**
```kotlin
suspend fun getInvoice(id: UUID, tenantId: UUID): Invoice? {
    val cacheKey = "invoice:$tenantId:$id"

    // Try cache first
    val cached = redis.get(cacheKey)
    if (cached != null) {
        return Json.decodeFromString<Invoice>(cached)
    }

    // Fetch from database
    val invoice = dbQuery {
        Invoices.select {
            (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
        }.singleOrNull()?.let { mapToInvoice(it) }
    }

    // Cache for 5 minutes
    if (invoice != null) {
        redis.setex(cacheKey, 300, Json.encodeToString(invoice))
    }

    return invoice
}
```

---

## Backup & Disaster Recovery

### Database Backups

**Automated Backups (RDS):**
```bash
# Configure automated backups
aws rds modify-db-instance \
  --db-instance-identifier dokus-prod \
  --backup-retention-period 30 \
  --preferred-backup-window "03:00-04:00" \
  --apply-immediately
```

**Manual Snapshot:**
```bash
# Create manual snapshot
aws rds create-db-snapshot \
  --db-instance-identifier dokus-prod \
  --db-snapshot-identifier dokus-prod-manual-2025-10-25

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier dokus-prod-restored \
  --db-snapshot-identifier dokus-prod-manual-2025-10-25
```

**PostgreSQL pg_dump:**
```bash
# Full backup
pg_dump -h postgres.dokus.internal -U dokus_app -Fc dokus > dokus-backup-$(date +%Y%m%d).dump

# Restore backup
pg_restore -h postgres.dokus.internal -U dokus_app -d dokus dokus-backup-20251025.dump

# Backup to S3
pg_dump -h postgres.dokus.internal -U dokus_app -Fc dokus | \
  aws s3 cp - s3://dokus-backups/database/dokus-backup-$(date +%Y%m%d).dump
```

### Disaster Recovery Plan

**Recovery Time Objective (RTO):** 2 hours
**Recovery Point Objective (RPO):** 1 hour

**DR Procedures:**

1. **Database Failure:**
   - Automatic failover to standby (Multi-AZ)
   - Manual restore from snapshot if needed
   - Maximum data loss: 1 hour (snapshot interval)

2. **Service Failure:**
   - Kubernetes auto-restarts failed pods
   - Health checks trigger pod replacement
   - Load balancer routes traffic to healthy pods

3. **Region Failure:**
   - Maintain standby environment in secondary region
   - Regular data replication
   - DNS failover to secondary region

4. **Complete Infrastructure Loss:**
   - Restore from offsite backups (S3)
   - Rebuild infrastructure using Terraform
   - Restore database from latest snapshot
   - Redeploy services from container registry

**DR Testing:**
```bash
# Quarterly DR drill
# 1. Create test environment
terraform apply -var-file=disaster-recovery.tfvars

# 2. Restore latest backup
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier dokus-dr-test \
  --db-snapshot-identifier latest

# 3. Deploy services
kubectl apply -f kubernetes/ -n dokus-dr

# 4. Run smoke tests
./scripts/smoke-tests.sh https://dr.dokus.internal

# 5. Document results and destroy test environment
terraform destroy -var-file=disaster-recovery.tfvars
```

---

## Security Checklist

### Pre-Deployment Security

- [ ] All secrets in environment variables or secret managers (never in code)
- [ ] TLS 1.3 enforced for all connections
- [ ] Database connections encrypted
- [ ] JWT secret is 256+ bits
- [ ] Passwords hashed with bcrypt (12+ rounds) or Argon2id
- [ ] CORS configured with specific origins (not *)
- [ ] Rate limiting enabled on all endpoints
- [ ] SQL injection prevention (using Exposed ORM)
- [ ] Input validation on all endpoints
- [ ] Multi-tenant isolation verified (tenant_id in all queries)
- [ ] Audit logging enabled for all financial operations
- [ ] Security headers configured (CSP, HSTS, X-Frame-Options)
- [ ] Container images scanned for vulnerabilities
- [ ] Dependencies updated (no known CVEs)
- [ ] Network policies configured (Kubernetes)
- [ ] Principle of least privilege for all service accounts

### Post-Deployment Monitoring

- [ ] Failed login attempts monitored
- [ ] Abnormal access patterns detected
- [ ] Database query performance monitored
- [ ] Error rates tracked
- [ ] Security scan scheduled (weekly)
- [ ] Dependency updates automated (Dependabot)
- [ ] Backup verification (monthly)
- [ ] Disaster recovery test (quarterly)
- [ ] Penetration testing (annually)
- [ ] Compliance audit (annually)

---

## Related Documentation

- [Setup Guide](./SETUP.md) - Local development setup
- [Architecture](./ARCHITECTURE.md) - System design
- [Security](./SECURITY.md) - Security best practices
- [Database](./DATABASE.md) - Database schema
- [API Reference](./API.md) - API documentation

---

**Last Updated:** October 2025
