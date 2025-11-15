# Dokus Configuration Guide

## Overview

The Dokus deployment configuration has been simplified to support two environments:
- **Cloud** (formerly "production") - For cloud/production deployments
- **Local** (formerly "dev") - For local development

## Critical Fix: CACHE_TYPE Environment Variable

**Problem:** Docker containers were failing with the error:
```
ConfigException$UnresolvedSubstitution: Could not resolve substitution to a value: ${CACHE_TYPE}
```

**Solution:** The `CACHE_TYPE` environment variable is now properly configured in:
- `deployment/.env.example` (with default value `redis`)
- `deployment/docker-compose.yml` (all services now include `CACHE_TYPE`)

## Environment Setup

### Cloud/Production Deployment

**Configuration Files:**
- `deployment/.env.example` - Copy to `.env` and customize
- `deployment/docker-compose.yml` - Docker Compose configuration
- `features/*/backend/src/main/resources/application-cloud.conf` - Service-specific cloud config

**How to Deploy:**

1. **First Time Setup:**
   ```bash
   cd deployment
   cp .env.example .env
   # Edit .env and fill in your secure values

   # Run the installation script
   ./dokus.sh
   ```

2. **Manual Deployment:**
   ```bash
   cd deployment
   docker compose pull
   docker compose up -d
   ```

**Required Environment Variables:**
```bash
# Database
DB_USERNAME=dokus
DB_PASSWORD=<your-secure-password>

# Redis Cache
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<your-secure-password>

# RabbitMQ
RABBITMQ_USERNAME=dokus
RABBITMQ_PASSWORD=<your-secure-password>

# JWT Authentication
JWT_SECRET=<64+ character secret>
JWT_ISSUER=https://yourdomain.com
JWT_AUDIENCE=dokus-api

# CRITICAL: Cache Configuration
CACHE_TYPE=redis

# Security - CORS
CORS_ALLOWED_HOSTS=https://yourdomain.com,https://www.yourdomain.com

# Security - API Keys
MONITORING_API_KEY=<your-key>
ADMIN_API_KEY=<your-key>
INTEGRATION_API_KEY=<your-key>

# Security - Request Signing
REQUEST_SIGNING_ENABLED=true
REQUEST_SIGNING_SECRET=<32+ character secret>

# Logging
LOG_LEVEL=INFO
```

### Local Development

**Configuration Files:**
- Base defaults in `features/*/backend/src/main/resources/application.conf`
- `dev.sh` script for local environment management

**How to Start:**

```bash
# First time: build and start all services
./dev.sh start

# Subsequent starts
./dev.sh start

# View status
./dev.sh status

# View logs
./dev.sh logs [service-name]

# Access databases
./dev.sh db

# Access Redis
./dev.sh redis

# Run tests
./dev.sh test
```

**Local Development Defaults:**
- Databases: PostgreSQL on ports 5541-5547
- Services: HTTP ports 7091-7097 (Auth, Invoicing, Expense, Payment, Reporting, Audit, Banking)
- Redis: Port 6380
- RabbitMQ: Port 5672 (UI on 15672)
- Cache Type: `memory` (default, can override to `redis`)

## Configuration Hierarchy

### 1. Base Configuration (`application.conf`)
- Defines all default values
- Suitable for local development
- Uses memory caching by default
- All values can be overridden via environment variables

### 2. Cloud Configuration (`application-cloud.conf`)
- Extends base configuration
- Production-optimized settings
- **Requires** environment variables for sensitive data
- Uses Redis caching (requires `CACHE_TYPE=redis`)

### 3. Environment Variables
- Highest priority
- Override both base and cloud configs
- Set via `.env` file or docker-compose environment

## Migration Guide

### From Old Setup (prod/dev/local)

**Old Environment Names:**
- `ENVIRONMENT=prod` → Now `ENVIRONMENT=cloud`
- `ENVIRONMENT=dev` → Removed (use base config)
- `ENVIRONMENT=local` → Now default for local development

**Old Configuration Files:**
- `application-prod.conf` → Renamed to `application-cloud.conf`
- `application-dev.conf` → Removed (merged into base `application.conf`)
- `application-local.conf` → Not used

**Scripts Updated:**
- `deployment/dokus.sh` → Now references "cloud" environment
- `dev.sh` → Uses base config (local environment)

## Service Ports

### Cloud/Production (60xx range)
- Auth Service: 6091
- Invoicing Service: 6092
- Expense Service: 6093
- Payment Service: 6094
- Reporting Service: 6095
- Audit Service: 6096
- Banking Service: 6097

### Local Development (70xx range)
- Auth Service: 7091
- Invoicing Service: 7092
- Expense Service: 7093
- Payment Service: 7094
- Reporting Service: 7095
- Audit Service: 7096
- Banking Service: 7097

### Databases (Local Development)
- Auth: 5541
- Invoicing: 5542
- Expense: 5543
- Payment: 5544
- Reporting: 5545
- Audit: 5546
- Banking: 5547

## Troubleshooting

### Docker Containers Crash with "CACHE_TYPE not found"

**Symptom:**
```
ConfigException$UnresolvedSubstitution: application-cloud.conf: Could not resolve substitution to a value: ${CACHE_TYPE}
```

**Solution:**
1. Ensure `.env` file exists in `deployment/` directory
2. Verify `CACHE_TYPE=redis` is set in `.env`
3. Restart services: `docker compose down && docker compose up -d`

### Missing Configuration for "ktor" Key

**Symptom:**
```
ConfigException$Missing: No configuration setting found for key 'ktor'
```

**Solution:**
This usually means the application couldn't find `application.conf`. Check:
1. JAR file includes resources: `./gradlew clean build`
2. Docker image was built correctly
3. Working directory is correct

### Services Can't Connect to Redis/PostgreSQL

**Symptom:**
Services fail health checks or timeout on startup

**Solution:**
1. Check `.env` file has correct passwords
2. Verify Redis/PostgreSQL containers are healthy: `docker compose ps`
3. Check networks: `docker network ls`
4. View logs: `docker compose logs redis` or `docker compose logs postgres-auth`

## Best Practices

### Security
1. **Never commit `.env` file** - Contains secrets
2. **Generate strong passwords** - Use `openssl rand -base64 32`
3. **Rotate secrets regularly** - Especially JWT_SECRET
4. **Use HTTPS in production** - Update CORS_ALLOWED_HOSTS accordingly
5. **Review API keys** - Generate unique keys per environment

### Performance
1. **Use Redis for caching** - Set `CACHE_TYPE=redis` in cloud
2. **Configure connection pools** - Adjust DB_POOL_* variables as needed
3. **Monitor metrics** - All services expose `/metrics` endpoint
4. **Enable compression** - Enabled by default in cloud config

### Monitoring
1. **Check health endpoints** - Each service has `/health` endpoint
2. **View logs** - Use `docker compose logs -f [service]`
3. **Metrics collection** - Prometheus-compatible metrics on port 7090
4. **Distributed tracing** - Enable with `TRACING_ENABLED=true` and configure Jaeger

## Quick Reference

### Cloud Deployment Commands
```bash
cd deployment

# Start services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop services
docker compose stop

# Restart a service
docker compose restart auth-service

# Update to latest images
docker compose pull && docker compose up -d

# Clean everything (WARNING: deletes data!)
docker compose down -v
```

### Local Development Commands
```bash
# Start everything
./dev.sh start

# Check status with health checks
./dev.sh status

# View all logs
./dev.sh logs

# View specific service logs
./dev.sh logs auth-service-dev

# Access database (interactive menu)
./dev.sh db

# Access Redis CLI
./dev.sh redis

# Reset database (interactive menu)
./dev.sh reset-db

# Run tests
./dev.sh test

# Clean everything
./dev.sh clean
```

## Additional Resources

- Docker Compose Documentation: https://docs.docker.com/compose/
- HOCON Configuration: https://github.com/lightbend/config
- Ktor Documentation: https://ktor.io/
- Project README: `/README.md`
- Architecture Documentation: `/CLAUDE.md`
