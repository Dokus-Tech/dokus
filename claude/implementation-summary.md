# Dokus Backend Implementation Summary

## Quick Start for Claude Code

This document provides a step-by-step guide for Claude Code to autonomously implement the backend improvements.

## Current State Assessment

### ✅ What's Working
- Basic project structure with Kotlin + Ktor
- Auth service skeleton with JWT
- Database service with partial invoice implementation
- Docker development environment
- KotlinX RPC configuration
- Multi-tenant architecture foundation

### 🚧 What Needs Implementation
1. **Database Layer** (Priority 1)
   - Complete all 14 table definitions
   - Implement repositories with tenant isolation
   - Create Flyway migrations

2. **Core Services** (Priority 2)
   - Client management
   - Expense tracking
   - Payment processing
   - Reporting

3. **Integrations** (Priority 3)
   - Peppol e-invoicing
   - Stripe/Mollie payments
   - Email service
   - File storage (S3)

4. **Production Setup** (Priority 4)
   - CI/CD pipeline
   - Monitoring
   - Security hardening
   - Backup strategy

## Implementation Commands

### Step 1: Set Up Development Environment
```bash
# Clone and setup
git clone [repository]
cd dokus

# Start development environment
docker-compose -f docker-compose.dev.yml up -d

# Run database migrations
./gradlew :foundation:database:flywayMigrate

# Build all services
./gradlew clean build
```

### Step 2: Implement Database Schema
```bash
# Navigate to database module
cd foundation/database

# Create schema files
mkdir -p src/main/kotlin/ai/dokus/foundation/database/schema
mkdir -p src/main/resources/db/migration

# Copy schema implementations from Task Document #2
# Create: ClientsTable.kt, ExpensesTable.kt, PaymentsTable.kt, etc.

# Create migrations
# Copy V1__initial_schema.sql from Task Document #2

# Run migrations
./gradlew flywayMigrate
```

### Step 3: Implement Repositories
```bash
# Create repository package
mkdir -p src/main/kotlin/ai/dokus/foundation/database/repositories

# Implement BaseRepository.kt (from Task Document #2)
# Implement entity repositories: ClientRepository.kt, ExpenseRepository.kt, etc.

# Test repositories
./gradlew test
```

### Step 4: Implement Services
```bash
# For each feature module (auth, clients, expenses, payments):
cd features/[feature]/backend

# Create service implementation
mkdir -p src/main/kotlin/ai/dokus/[feature]/backend/services

# Copy service implementations from Task Document #3
# Implement: ClientApiImpl.kt, ExpenseApiImpl.kt, PaymentApiImpl.kt

# Register services in Application.kt
# Update the RPC configuration
```

### Step 5: Add Tests
```bash
# Create test directories
mkdir -p src/test/kotlin/ai/dokus/[feature]/backend

# Implement tests for:
# - Repositories (with H2 in-memory database)
# - Services (with mocked repositories)
# - Integration tests (full flow)

# Run tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
```

### Step 6: Docker Production Setup
```bash
# Create production Dockerfiles
cp features/auth/backend/Dockerfile.dev features/auth/backend/Dockerfile.prod
# Update for production (multi-stage build, security hardening)

# Build production images
docker build -f features/auth/backend/Dockerfile.prod -t dokus/auth:latest .
docker build -f foundation/database/Dockerfile.prod -t dokus/database:latest .

# Test production containers
docker-compose -f docker-compose.prod.yml up
```

## File Structure After Implementation

```
dokus/
├── foundation/
│   ├── database/
│   │   └── src/main/kotlin/ai/dokus/foundation/database/
│   │       ├── schema/
│   │       │   ├── TenantsTable.kt
│   │       │   ├── UsersTable.kt
│   │       │   ├── ClientsTable.kt
│   │       │   ├── InvoicesTable.kt
│   │       │   ├── ExpensesTable.kt
│   │       │   ├── PaymentsTable.kt
│   │       │   └── ... (all 14 tables)
│   │       ├── repositories/
│   │       │   ├── BaseRepository.kt
│   │       │   ├── ClientRepository.kt
│   │       │   ├── InvoiceRepository.kt
│   │       │   ├── ExpenseRepository.kt
│   │       │   └── ... (all repositories)
│   │       └── DatabaseFactory.kt
│   └── apispec/
│       └── src/commonMain/kotlin/ai/dokus/foundation/apispec/
│           ├── ClientApi.kt
│           ├── InvoiceApi.kt
│           ├── ExpenseApi.kt
│           └── PaymentApi.kt
├── features/
│   ├── auth/backend/
│   ├── clients/backend/
│   ├── invoices/backend/
│   ├── expenses/backend/
│   └── payments/backend/
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   ├── deployments/
│   └── ingress.yaml
├── .github/workflows/
│   └── deploy.yml
└── docker-compose.prod.yml
```

## Validation Checklist

After implementation, verify:

### Database
- [ ] All 14 tables created
- [ ] Indexes properly configured
- [ ] Multi-tenant isolation enforced
- [ ] Audit logging working
- [ ] Migrations versioned

### Services
- [ ] All CRUD operations implemented
- [ ] KotlinX RPC endpoints working
- [ ] Validation in place
- [ ] Error handling comprehensive
- [ ] Authentication/authorization working

### Testing
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Test coverage >80%
- [ ] Multi-tenant isolation tested
- [ ] Performance benchmarks met

### Production
- [ ] Docker images building
- [ ] Health checks passing
- [ ] Metrics exposed
- [ ] Logs structured
- [ ] Secrets externalized

## Performance Targets

Ensure implementation meets:
- Response time: <100ms p95
- Database queries: <50ms
- Concurrent users: 1000+
- Uptime: 99.9%
- Test coverage: 80%+

## Security Requirements

Implement:
- JWT authentication
- Multi-tenant isolation
- Input validation
- SQL injection prevention
- Rate limiting
- Audit logging
- Encrypted connections
- Secure secret storage

## Quick Commands Reference

```bash
# Development
./gradlew build                          # Build all modules
./gradlew test                           # Run tests
./gradlew :features:auth:backend:run    # Run auth service
docker-compose up -d                     # Start dependencies

# Database
./gradlew flywayMigrate                  # Run migrations
./gradlew flywayClean                    # Clean database
psql -h localhost -U dokus -d dokus      # Connect to DB

# Docker
docker build -t dokus/auth:latest .      # Build image
docker-compose logs -f auth-service      # View logs
docker-compose restart auth-service      # Restart service

# Testing
./gradlew test --tests "*Repository*"    # Test repositories
./gradlew jacocoTestReport               # Coverage report
./gradlew integrationTest                # Integration tests

# Production
kubectl apply -f k8s/                    # Deploy to K8s
kubectl rollout status deployment/auth   # Check deployment
kubectl logs -f deployment/auth          # View logs
```

## Support Documentation

Refer to these documents for detailed implementation:
1. **Backend Roadmap** - Overall implementation plan
2. **Database Schema Tasks** - Complete schema implementation
3. **Service Implementation Tasks** - Service-by-service guide
4. **Deployment Tasks** - Production setup guide

## Success Metrics

Implementation is complete when:
1. ✅ All database tables implemented
2. ✅ All services accessible via RPC
3. ✅ Peppol integration working
4. ✅ Payment processing functional
5. ✅ Tests passing with >80% coverage
6. ✅ Production deployment ready
7. ✅ Monitoring and alerting configured
8. ✅ Documentation complete

## Next Actions for Claude Code

1. **Immediate (Today)**
   - Complete database schema files
   - Implement BaseRepository
   - Create first migration script

2. **Tomorrow**
   - Implement Client service
   - Add repository tests
   - Create integration tests

3. **This Week**
   - Complete all core services
   - Implement Peppol integration
   - Set up CI/CD pipeline

4. **Next Week**
   - Production deployment setup
   - Load testing
   - Security audit
   - Launch preparation

---

**Ready to implement!** Start with Step 1 and work through systematically. Each task document provides copy-paste code that can be directly implemented. The architecture is sound and follows best practices for Kotlin backend development.
