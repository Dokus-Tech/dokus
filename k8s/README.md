# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the Dokus backend services.

## Prerequisites

- Kubernetes cluster 1.28+
- kubectl configured and authenticated
- cert-manager installed (for TLS certificates)
- nginx-ingress-controller installed
- Secrets configured (see below)

## Architecture

```
┌─────────────────────┐
│   Ingress (HTTPS)   │
│   api.dokus.ai      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  dokus-database     │
│  (3 replicas)       │
│  Port: 8070         │
└──────────┬──────────┘
           │
     ┌─────┴─────┬─────────┐
     ▼           ▼         ▼
┌─────────┐ ┌─────────┐ ┌─────────┐
│PostgreSQL│ │  Redis  │ │  MinIO  │
│(StatefulSet)│(StatefulSet)│(StatefulSet)│
└─────────┘ └─────────┘ └─────────┘
```

## Initial Setup

### 1. Create Namespaces

```bash
kubectl apply -f namespace.yml
```

### 2. Configure Secrets

**IMPORTANT**: Never commit secrets to git. Use kubectl or a secret management tool.

```bash
# Set database secrets
kubectl create secret generic dokus-database-secrets \
  --from-literal=DB_USERNAME='produser' \
  --from-literal=DB_PASSWORD='<generate-strong-password>' \
  --from-literal=JWT_SECRET='<generate-jwt-secret>' \
  --from-literal=REDIS_PASSWORD='<generate-redis-password>' \
  --from-literal=MINIO_ACCESS_KEY='<generate-access-key>' \
  --from-literal=MINIO_SECRET_KEY='<generate-secret-key>' \
  --namespace=dokus-production

# Set PostgreSQL secrets
kubectl create secret generic postgres-secrets \
  --from-literal=POSTGRES_USER='produser' \
  --from-literal=POSTGRES_PASSWORD='<same-as-above>' \
  --namespace=dokus-production

# Set Redis secrets
kubectl create secret generic redis-secrets \
  --from-literal=REDIS_PASSWORD='<same-as-above>' \
  --namespace=dokus-production
```

### 3. Deploy Infrastructure

```bash
# Deploy PostgreSQL
kubectl apply -f postgres-statefulset.yml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres \
  --namespace=dokus-production --timeout=300s

# Deploy Redis
kubectl apply -f redis-statefulset.yml

# Wait for Redis to be ready
kubectl wait --for=condition=ready pod -l app=redis \
  --namespace=dokus-production --timeout=300s
```

### 4. Run Database Migrations

```bash
# Run migrations using a Job or manually
kubectl exec -it postgres-0 --namespace=dokus-production -- \
  psql -U produser -d dokus -f /migrations/V1__initial_schema.sql
```

### 5. Deploy Application

```bash
# Deploy database service
kubectl apply -f database-deployment.yml

# Wait for deployment to be ready
kubectl rollout status deployment/dokus-database \
  --namespace=dokus-production --timeout=5m
```

### 6. Configure Ingress

```bash
# Install cert-manager (if not already installed)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer for Let's Encrypt
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: devops@dokus.ai
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF

# Deploy ingress
kubectl apply -f ingress.yml
```

## Verification

### Check Pod Status

```bash
# Check all pods
kubectl get pods --namespace=dokus-production

# Check deployment status
kubectl get deployments --namespace=dokus-production

# Check services
kubectl get services --namespace=dokus-production
```

### Check Logs

```bash
# Database service logs
kubectl logs -f deployment/dokus-database --namespace=dokus-production

# PostgreSQL logs
kubectl logs -f statefulset/postgres --namespace=dokus-production

# Redis logs
kubectl logs -f statefulset/redis --namespace=dokus-production
```

### Health Checks

```bash
# Check health endpoint
curl https://api.dokus.ai/health

# Check metrics endpoint
curl https://api.dokus.ai/metrics
```

## Scaling

### Manual Scaling

```bash
# Scale database service
kubectl scale deployment/dokus-database \
  --replicas=5 \
  --namespace=dokus-production
```

### Horizontal Pod Autoscaler

HPA is already configured in `database-deployment.yml`:
- Min replicas: 3
- Max replicas: 10
- CPU target: 70%
- Memory target: 80%

View HPA status:

```bash
kubectl get hpa --namespace=dokus-production
```

## Monitoring

### Prometheus Metrics

The database service exposes Prometheus metrics on port 9090:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8070"
  prometheus.io/path: "/metrics"
```

### Grafana Dashboards

Import the Grafana dashboards from `/monitoring/grafana/` directory.

## Backup & Recovery

### Database Backup

```bash
# Create backup
kubectl exec -it postgres-0 --namespace=dokus-production -- \
  pg_dump -U produser dokus > backup-$(date +%Y%m%d).sql

# Upload to S3
aws s3 cp backup-$(date +%Y%m%d).sql s3://dokus-backups/
```

### Restore from Backup

```bash
# Download from S3
aws s3 cp s3://dokus-backups/backup-20250114.sql ./restore.sql

# Restore
kubectl exec -i postgres-0 --namespace=dokus-production -- \
  psql -U produser -d dokus < restore.sql
```

## Troubleshooting

### Pod Not Starting

```bash
# Describe pod to see events
kubectl describe pod <pod-name> --namespace=dokus-production

# Check logs
kubectl logs <pod-name> --namespace=dokus-production --previous
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
kubectl exec -it postgres-0 --namespace=dokus-production -- \
  psql -U produser -d dokus -c "SELECT 1"

# Test from database pod
kubectl exec -it <database-pod> --namespace=dokus-production -- \
  nc -zv postgres 5432
```

### Redis Connection Issues

```bash
# Test Redis connection
kubectl exec -it redis-0 --namespace=dokus-production -- \
  redis-cli --pass <redis-password> ping
```

## Rollback

### Rollback Deployment

```bash
# View rollout history
kubectl rollout history deployment/dokus-database \
  --namespace=dokus-production

# Rollback to previous version
kubectl rollout undo deployment/dokus-database \
  --namespace=dokus-production

# Rollback to specific revision
kubectl rollout undo deployment/dokus-database \
  --to-revision=2 \
  --namespace=dokus-production
```

## Cleanup

### Delete All Resources

```bash
# Delete all resources in production namespace
kubectl delete all --all --namespace=dokus-production

# Delete namespace
kubectl delete namespace dokus-production
```

## Security Best Practices

1. **Secrets Management**: Use external secret managers (HashiCorp Vault, AWS Secrets Manager)
2. **Network Policies**: Already configured to restrict pod-to-pod communication
3. **RBAC**: Configure Role-Based Access Control for service accounts
4. **Pod Security**: RunAsNonRoot and ReadOnlyRootFilesystem enabled
5. **TLS**: All external communication uses HTTPS via Ingress
6. **Resource Limits**: All pods have resource requests and limits defined
7. **Security Context**: Containers run as non-root user (UID 1001)

## CI/CD Integration

The GitHub Actions workflow (`.github/workflows/ci-cd.yml`) automatically:
- Builds Docker images on push to main/develop
- Pushes images to GitHub Container Registry
- Deploys to staging (develop branch) or production (main branch)
- Runs smoke tests after deployment
- Rolls back on failure

## Support

For issues or questions:
- Create an issue in the GitHub repository
- Contact DevOps team: devops@dokus.ai
