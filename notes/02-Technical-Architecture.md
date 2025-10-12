# Technical Architecture

**Last Updated:** October 2025  
**Tech Lead:** Solo Founder  
**Status:** Architecture Decided, Implementation Starting

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT APPLICATIONS                      │
├─────────────────┬─────────────────┬────────────────────────┤
│   Web Browser   │   iOS Native    │   Android Native       │
│  (Kotlin/WASM)  │     (Swift)     │      (Kotlin)          │
└────────┬────────┴────────┬────────┴────────────┬───────────┘
         │                 │                     │
         └─────────────────┼─────────────────────┘
                           │
                    ┌──────▼───────┐
                    │   API Gateway │
                    │  (Ktor HTTP)  │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │ Invoice │      │ Expense │      │  User   │
    │ Service │      │ Service │      │ Service │
    │ (Ktor)  │      │ (Ktor)  │      │ (Ktor)  │
    └────┬────┘      └────┬────┘      └────┬────┘
         │                │                 │
         └────────────────┼─────────────────┘
                          │
                   ┌──────▼───────┐
                   │  PostgreSQL  │
                   │   Database   │
                   └──────────────┘
```

---

## Technology Stack (Finalized)

### Backend Services

**Framework:** Ktor 2.3+
- Asynchronous, lightweight
- Perfect for microservices
- Kotlin-native (type safety)
- Excellent performance

**ORM:** Exposed 0.44+
- Type-safe SQL DSL
- Kotlin-first design
- Prevents SQL injection
- Great PostgreSQL support

**Database:** PostgreSQL 15+
- ACID compliance (critical for financial data)
- NUMERIC type for exact decimal calculations
- JSONB for flexible document storage
- Full-text search capabilities
- Proven reliability

**Authentication:** JWT with RS256
- Stateless tokens
- Refresh token rotation
- 15-minute access token expiry
- Secure key storage

**Caching:** Redis 7+
- Session storage
- Rate limiting
- Temporary data
- Queue management

### Frontend

**Phase 1 (MVP):** Kotlin/JS with React wrappers
- Shared data models with backend
- Type-safe API calls
- Fast development

**Phase 2 (Month 3+):** Kotlin Multiplatform
- iOS: Swift UI with shared business logic
- Android: Jetpack Compose
- Web: Kotlin/WASM (future)

### Infrastructure

**Hosting:** Render.com or Railway.app
- €20-40/month for 2 containers
- Auto-scaling
- Zero-downtime deploys
- Built-in CI/CD

**Database:** AWS RDS PostgreSQL
- db.t3.medium (€60/month)
- 100GB storage (€12/month)
- Automated backups
- Point-in-time recovery

**Caching:** AWS ElastiCache Redis
- t3.micro (€15/month)
- High availability
- Automatic failover

**File Storage:** AWS S3
- €5-10/month
- Receipts, invoices, documents
- CDN via CloudFront

**Monitoring:**
- Sentry for errors (€26/month)
- Datadog or New Relic for APM (€15/month)

**Total Infrastructure Cost:** €163/month (without K8s)

---

## Monorepo Structure

```
dokus/
├── backend/
│   ├── services/
│   │   ├── user-service/         # Auth, user management
│   │   ├── invoice-service/      # Invoicing, Peppol
│   │   ├── expense-service/      # Expense tracking
│   │   ├── payment-service/      # Stripe/Mollie integration
│   │   └── reporting-service/    # Analytics, reports
│   ├── shared/
│   │   ├── database/            # Exposed schemas, migrations
│   │   ├── auth/                # JWT utilities
│   │   ├── validation/          # Input validation
│   │   └── models/              # Shared data models
│   └── lib/
│       ├── peppol/              # Peppol integration
│       ├── vat/                 # VAT calculations
│       └── audit/               # Audit logging
├── frontend/
│   ├── web/                     # Kotlin/JS + React
│   ├── mobile/                  # Kotlin Multiplatform
│   └── shared/                  # Shared UI components
├── shared/
│   └── dto/                     # Data transfer objects
├── scripts/
│   ├── migrate.sh               # Database migrations
│   └── seed.sh                  # Test data
└── docker/
    ├── docker-compose.yml       # Local development
    └── Dockerfile.service       # Service container
```

---

## Database Schema (Core Tables)

### Multi-Tenancy Pattern: Shared Database, Shared Schema

**Why:** Simplest for MVP, lowest infrastructure cost, easy to manage

```sql
-- Every table includes tenant_id for row-level security
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    plan VARCHAR(50) NOT NULL, -- 'free', 'professional', 'business'
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    mfa_secret VARCHAR(255),
    role VARCHAR(50) NOT NULL, -- 'owner', 'member', 'accountant'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    INDEX idx_users_tenant (tenant_id),
    INDEX idx_users_email (email)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_number VARCHAR(50) NOT NULL,
    client_id UUID NOT NULL REFERENCES clients(id),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL, -- Use NUMERIC for exact calculations!
    vat_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- 'draft', 'sent', 'paid', 'overdue'
    peppol_id VARCHAR(255), -- Peppol transmission ID
    peppol_sent_at TIMESTAMPTZ,
    payment_link VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(tenant_id, invoice_number),
    INDEX idx_invoices_tenant (tenant_id),
    INDEX idx_invoices_client (client_id),
    INDEX idx_invoices_status (status)
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    vat_amount NUMERIC(12, 2),
    category VARCHAR(100) NOT NULL, -- 'software', 'hardware', 'travel', etc.
    description TEXT,
    receipt_url VARCHAR(500), -- S3 path
    is_deductible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    INDEX idx_expenses_tenant (tenant_id),
    INDEX idx_expenses_date (date),
    INDEX idx_expenses_category (category)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL, -- 'invoice.created', 'expense.updated'
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    INDEX idx_audit_tenant (tenant_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_date (created_at)
);
```

### Critical: Financial Data Type Usage

**✅ ALWAYS use NUMERIC(12, 2) for money**
- Never use FLOAT or REAL (introduces rounding errors)
- NUMERIC stores exact decimal values
- (12, 2) = 10 digits before decimal, 2 after = up to €9,999,999,999.99

**Example in Exposed:**
```kotlin
object Invoices : Table("invoices") {
    val totalAmount = decimal("total_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
}
```

---

## Security Architecture

### Authentication Flow

```
1. User Login → Email/Password or OAuth
2. Verify credentials + check MFA
3. Generate JWT access token (15 min expiry)
4. Generate JWT refresh token (7 days expiry)
5. Return both tokens to client
6. Client stores refresh token in httpOnly cookie
7. Client stores access token in memory (not localStorage!)

Token Refresh:
1. Access token expires after 15 min
2. Client sends refresh token to /auth/refresh
3. Server validates refresh token
4. Server issues new access + refresh tokens
5. Old refresh token invalidated
```

### Data Encryption

**At Rest:**
- AES-256-GCM for sensitive data
- Encrypt: password hashes, MFA secrets, API keys, bank connection tokens
- AWS RDS encryption enabled
- Encrypted S3 buckets for documents

**In Transit:**
- TLS 1.3 for all connections
- HSTS headers enforced
- Certificate pinning for mobile apps

**Application Level:**
```kotlin
// Example: Encrypt MFA secret
fun encryptMfaSecret(secret: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val secretKey = getEncryptionKey() // From env var
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encrypted = cipher.doFinal(secret.toByteArray())
    return Base64.getEncoder().encodeToString(encrypted)
}
```

### Audit Logging (Critical!)

**Log Everything Financial:**
- Invoice created, updated, deleted
- Expense created, updated, deleted
- Payment received
- VAT calculation
- User permission changes
- Export/download actions

**Audit Log Requirements:**
- Immutable (no updates/deletes)
- Cryptographic signatures to prevent tampering
- Stored for 7 years (GDPR/Belgian law)
- Include: timestamp, user, action, IP, before/after values

```kotlin
suspend fun logAudit(
    tenantId: UUID,
    userId: UUID?,
    action: String,
    entityType: String,
    entityId: UUID,
    oldValues: JsonObject?,
    newValues: JsonObject?
) {
    transaction {
        AuditLogs.insert {
            it[AuditLogs.tenantId] = tenantId
            it[AuditLogs.userId] = userId
            it[AuditLogs.action] = action
            it[AuditLogs.entityType] = entityType
            it[AuditLogs.entityId] = entityId
            it[AuditLogs.oldValues] = oldValues
            it[AuditLogs.newValues] = newValues
            it[AuditLogs.ipAddress] = getClientIp()
            it[AuditLogs.userAgent] = getUserAgent()
        }
    }
}
```

---

## Peppol Integration Architecture

### Partner vs Self-Hosted

**Decision: Partner with Access Point Provider**

| Approach | Cost | Time | Maintenance |
|----------|------|------|-------------|
| **Partner** (Pagero/EDICOM) | €2,000-5,000 setup + €0.10-0.30/invoice | 2-4 weeks | None |
| **Self-Host** | €50K+ dev cost | 6-12 months | High complexity |

**Recommendation:** Partner with Pagero or EDICOM
- Focus on product, not Peppol infrastructure
- Faster time to market
- Compliance guaranteed
- Scalable without technical debt

### Integration Flow

```
1. User creates invoice in Dokus
2. Dokus validates invoice data (amounts, VAT, etc)
3. Convert to Peppol BIS 3.0 format (UBL XML)
4. Send to Access Point via API
5. Access Point validates and transmits via Peppol network
6. Receive delivery confirmation
7. Update invoice status to 'sent'
8. Store Peppol ID for audit trail
```

### Peppol Requirements Checklist

- [ ] UBL 2.1 invoice format
- [ ] Peppol BIS 3.0 compliance
- [ ] Endpoint registration
- [ ] Schematron validation
- [ ] Belgian VAT rates (21%, 12%, 6%)
- [ ] IBAN/BIC validation
- [ ] Delivery confirmation handling
- [ ] Error handling and retry logic

---

## API Design Principles

### RESTful Standards

```
GET    /api/v1/invoices           # List invoices
GET    /api/v1/invoices/:id       # Get single invoice
POST   /api/v1/invoices           # Create invoice
PATCH  /api/v1/invoices/:id       # Update invoice
DELETE /api/v1/invoices/:id       # Delete invoice

POST   /api/v1/invoices/:id/send  # Send invoice (action)
POST   /api/v1/invoices/:id/pay   # Record payment (action)
```

### Response Format

```json
{
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "type": "invoice",
    "attributes": {
      "invoice_number": "INV-2025-001",
      "total_amount": "1250.00",
      "vat_amount": "262.50",
      "status": "sent"
    },
    "relationships": {
      "client": {
        "data": { "type": "client", "id": "..." }
      }
    }
  },
  "meta": {
    "timestamp": "2025-10-05T14:30:00Z"
  }
}
```

### Error Handling

```json
{
  "errors": [
    {
      "code": "INVALID_VAT_RATE",
      "message": "VAT rate must be 21%, 12%, or 6% for Belgium",
      "field": "vat_rate",
      "value": "19"
    }
  ],
  "meta": {
    "request_id": "req_abc123xyz"
  }
}
```

---

## Development Workflow

### Local Development Setup

```bash
# 1. Clone repository
git clone https://github.com/dokus/dokus-monorepo
cd dokus-monorepo

# 2. Start dependencies with Docker
docker-compose up -d postgres redis

# 3. Run migrations
./scripts/migrate.sh

# 4. Seed test data
./scripts/seed.sh

# 5. Start backend services
cd backend/services/user-service && ./gradlew run &
cd backend/services/invoice-service && ./gradlew run &

# 6. Start frontend
cd frontend/web && npm run dev
```

### Database Migrations

**Use Flyway for versioned migrations:**

```
db/migration/
├── V1__initial_schema.sql
├── V2__add_peppol_fields.sql
├── V3__add_audit_logs.sql
└── V4__add_payment_links.sql
```

**Never:**
- Modify existing migrations
- Delete migrations
- Skip version numbers

**Always:**
- Test migrations on copy of production data
- Write rollback scripts
- Update schema documentation

### Testing Strategy

```kotlin
// Unit Tests: Business logic
class VatCalculatorTest {
    @Test
    fun `should calculate 21% VAT correctly`() {
        val calculator = VatCalculator()
        val result = calculator.calculate(
            amount = BigDecimal("100.00"),
            rate = VatRate.STANDARD_BE
        )
        assertEquals(BigDecimal("21.00"), result)
    }
}

// Integration Tests: Database + Services
class InvoiceServiceTest {
    @Test
    fun `should create invoice with audit log`() = testTransaction {
        val invoice = invoiceService.create(...)
        val logs = auditLogRepository.findByEntity(invoice.id)
        assertTrue(logs.isNotEmpty())
    }
}

// E2E Tests: Full user flows
class InvoiceFlowTest {
    @Test
    fun `user can create and send invoice`() {
        // Login
        // Create invoice
        // Send via Peppol
        // Verify sent status
    }
}
```

**Coverage Targets:**
- Unit: 80%+ for business logic
- Integration: 70%+ for services
- E2E: Critical paths only (invoice creation, payment, expense tracking)

---

## CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run tests
        run: ./gradlew test
      
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker images
        run: |
          docker build -t dokus/user-service:${{ github.sha }} ./backend/services/user-service
          docker build -t dokus/invoice-service:${{ github.sha }} ./backend/services/invoice-service
      
      - name: Push to registry
        run: docker push dokus/*:${{ github.sha }}
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Render
        run: |
          curl -X POST https://api.render.com/deploy/...
```

---

## Performance Targets

### Response Times

| Endpoint | Target | Max |
|----------|--------|-----|
| GET /invoices | <100ms | <300ms |
| POST /invoices | <200ms | <500ms |
| POST /invoices/:id/send | <2s | <5s |
| GET /dashboard | <150ms | <400ms |

### Database

- Query time: <50ms for simple selects
- Index all foreign keys
- Use connection pooling (HikariCP)
- Limit: 50 connections initially

### Caching Strategy

```kotlin
// Cache expensive calculations
@Cacheable("vat-rates", ttl = 1.hour)
suspend fun getVatRates(country: String): List<VatRate>

// Invalidate on changes
@CacheEvict("user-settings")
suspend fun updateUserSettings(userId: UUID, settings: Settings)
```

---

## Monitoring & Observability

### Key Metrics

**Application:**
- Request rate (req/min)
- Error rate (%)
- Response time (p50, p95, p99)
- Database connection pool usage

**Business:**
- Invoices sent per day
- Payment success rate
- Average time to payment
- Trial-to-paid conversion

### Alerts

```yaml
alerts:
  - name: "High error rate"
    condition: error_rate > 5%
    duration: 5 minutes
    notify: slack, email
  
  - name: "Slow database queries"
    condition: query_time_p95 > 500ms
    duration: 10 minutes
    notify: slack
  
  - name: "Failed payments"
    condition: payment_failures > 10
    duration: 1 hour
    notify: slack, sms
```

---

## Scalability Plan

### Current Architecture (0-1,000 customers)

- Single PostgreSQL instance
- 2 backend containers
- Redis for sessions/cache
- **Cost:** €163/month
- **Supports:** Up to 10K requests/day

### Scale Phase 1 (1,000-5,000 customers)

- PostgreSQL upgrade to db.t3.large
- 4 backend containers
- Add read replicas for reporting
- **Cost:** €400/month
- **Supports:** Up to 50K requests/day

### Scale Phase 2 (5,000-20,000 customers)

- Multi-region deployment
- CDN for static assets
- Kubernetes for orchestration
- Database sharding by tenant_id
- **Cost:** €1,500/month
- **Supports:** 200K+ requests/day

---

## Security Checklist (Pre-Launch)

- [ ] HTTPS enforced (no HTTP access)
- [ ] HSTS headers configured
- [ ] CORS properly configured
- [ ] Rate limiting on all endpoints
- [ ] SQL injection prevention (Exposed parameterized queries)
- [ ] XSS prevention (input sanitization)
- [ ] CSRF tokens for state-changing operations
- [ ] Secrets in environment variables (never in code)
- [ ] MFA enforced for all users
- [ ] Audit logging for all financial operations
- [ ] Regular dependency updates (Dependabot)
- [ ] Security headers (CSP, X-Frame-Options, etc)
- [ ] PCI DSS compliance via Stripe/Mollie (no card storage)
- [ ] GDPR compliance (privacy policy, data export, deletion)
- [ ] Penetration testing before public launch

---

**Next Steps:**
1. Set up monorepo structure
2. Implement authentication service
3. Create database schema and migrations
4. Build invoice service with Peppol integration
5. Deploy to staging environment

**See Also:**
- [[05-Feature-Roadmap|Feature Roadmap]] for what to build when
- [[09-Compliance-Requirements|Compliance Requirements]] for regulatory details
- [[12-First-90-Days|First 90 Days]] for implementation timeline
