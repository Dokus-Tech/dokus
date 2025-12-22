# Payment Service

Payment transaction tracking service for recording and reconciling payments against invoices.

## Overview

The Payment service manages payment records for the Dokus platform:

- **Payment Tracking**: Records payments made against invoices
- **Multiple Payment Methods**: Supports bank transfers, cards, PayPal, Stripe, cash, and checks
- **Bank Reconciliation**: Links payments to bank transactions for automatic reconciliation
- **Payment Status**: Tracks pending and overdue payments for cash flow visibility

The service is designed to work with the Cashflow service (invoices) and Banking service (bank transactions) for complete financial tracking.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Payment Service                            │
│  (Ktor + Netty, JWT Authentication, Multi-tenant Isolation)     │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────┐
        │                         │                     │
        ▼                         ▼                     ▼
┌───────────────┐       ┌─────────────────┐   ┌─────────────────┐
│ Payment API   │       │  Payment Repo   │   │     Redis       │
│ (REST Routes) │       │  (Exposed ORM)  │   │   (Caching)     │
└───────────────┘       └─────────────────┘   └─────────────────┘
        │                         │
        │                         ▼
        │               ┌─────────────────┐
        │               │   PostgreSQL    │
        │               │ (PaymentsTable) │
        │               └─────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│                     External Services                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐    │
│  │    Auth     │    │  Cashflow   │    │     Banking     │    │
│  │ (TenantId)  │    │ (Invoices)  │    │ (Transactions)  │    │
│  └─────────────┘    └─────────────┘    └─────────────────┘    │
└───────────────────────────────────────────────────────────────┘
```

## Key Files

```
features/payment/backend/
├── src/main/kotlin/ai/dokus/payment/backend/
│   ├── Application.kt                    # Service entry point
│   ├── config/
│   │   └── DependencyInjection.kt        # Koin DI setup
│   ├── database/
│   │   ├── PaymentTables.kt              # Table initialization
│   │   └── utils/
│   │       ├── DatabaseFactory.kt
│   │       └── DateTimeUtils.kt
│   ├── plugins/
│   │   ├── Database.kt                   # DB connection lifecycle
│   │   └── Routing.kt                    # Route configuration
│   └── routes/
│       └── PaymentRoutes.kt              # REST API endpoints
├── build.gradle.kts
└── README.md
```

### Foundation Dependencies

Payment-related types in the foundation module:

| File | Description |
|------|-------------|
| `foundation/database/tables/payment/PaymentsTable.kt` | Database table definition |
| `foundation/database/repository/payment/PaymentRepository.kt` | Data access layer |
| `foundation/domain/routes/PaymentRoutes.kt` | Type-safe route definitions |
| `foundation/domain/model/PaymentDto.kt` | Payment data transfer object |
| `foundation/domain/enums/FinancialEnums.kt` | PaymentMethod enum |

## Database Schema

### PaymentsTable

Payment transactions linked to invoices.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `tenant_id` | UUID | Multi-tenant isolation (FK to TenantTable) |
| `invoice_id` | UUID | Associated invoice (FK to InvoicesTable) |
| `amount` | DECIMAL(12,2) | Payment amount |
| `payment_date` | DATE | When payment was made |
| `payment_method` | ENUM | How payment was made (see below) |
| `transaction_id` | VARCHAR(255) | External/bank transaction reference |
| `notes` | TEXT | Optional payment notes |
| `created_at` | TIMESTAMP | Record creation time |

**Indexes:**
- `tenant_id` - For multi-tenant queries
- `invoice_id` - For invoice lookups
- `payment_date` - For date range queries
- `(tenant_id, payment_date)` - Composite for tenant date queries
- `UNIQUE(invoice_id, transaction_id)` - Prevent duplicate payments

### Payment Methods

```kotlin
enum class PaymentMethod {
    BankTransfer,  // BANK_TRANSFER - Bank wire transfer
    CreditCard,    // CREDIT_CARD - Credit card payment
    DebitCard,     // DEBIT_CARD - Debit card payment
    PayPal,        // PAYPAL - PayPal payment
    Stripe,        // STRIPE - Stripe payment
    Cash,          // CASH - Cash payment
    Check,         // CHECK - Check payment
    Other          // OTHER - Other payment method
}
```

## REST API Reference

Base path: `/api/v1/payments`

All endpoints require JWT authentication and operate within tenant context.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/payments` | List payments with filters |
| `GET` | `/api/v1/payments/pending` | List pending payments |
| `GET` | `/api/v1/payments/overdue` | List overdue payments |
| `GET` | `/api/v1/payments/{id}` | Get payment by ID |
| `GET` | `/api/v1/payments/{id}/refunds` | List refunds for payment |

### Query Parameters

**List Payments (`GET /api/v1/payments`)**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `status` | string | - | Filter by payment status |
| `fromDate` | date | - | Filter payments from date (ISO 8601) |
| `toDate` | date | - | Filter payments to date (ISO 8601) |
| `limit` | int | 50 | Maximum results per page |
| `offset` | int | 0 | Pagination offset |

### Health Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Service health check |
| `GET` | `/health/live` | Liveness probe |
| `GET` | `/health/ready` | Readiness probe |

## Repository Operations

The `PaymentRepository` provides the following operations:

```kotlin
// Create a payment record
suspend fun createPayment(
    tenantId: TenantId,
    invoiceId: InvoiceId,
    amount: Money,
    paymentDate: LocalDate,
    paymentMethod: PaymentMethod,
    transactionId: TransactionId?,
    notes: String?
): Result<PaymentDto>

// Get payment by ID (tenant-scoped)
suspend fun getPayment(
    paymentId: PaymentId,
    tenantId: TenantId
): Result<PaymentDto?>

// List payments by invoice
suspend fun listByInvoice(invoiceId: InvoiceId): Result<List<PaymentDto>>

// List payments by tenant with filters
suspend fun listByTenant(
    tenantId: TenantId,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    paymentMethod: PaymentMethod? = null,
    limit: Int = 50,
    offset: Int = 0
): Result<List<PaymentDto>>

// Get total amount paid for an invoice
suspend fun getTotalPaid(invoiceId: InvoiceId): Result<Money>

// Delete payment (tenant-scoped)
suspend fun deletePayment(
    paymentId: PaymentId,
    tenantId: TenantId
): Result<Boolean>

// Link payment to bank transaction
suspend fun reconcile(
    paymentId: PaymentId,
    tenantId: TenantId,
    transactionId: TransactionId
): Result<Boolean>
```

## Multi-Tenant Security

**CRITICAL**: All database operations MUST filter by `tenant_id`.

```kotlin
// CORRECT - Always include tenant context
PaymentsTable.selectAll().where {
    (PaymentsTable.id eq paymentId) and
    (PaymentsTable.tenantId eq tenantId)
}

// WRONG - Never query without tenant isolation
PaymentsTable.selectAll().where {
    PaymentsTable.id eq paymentId  // Security vulnerability!
}
```

The tenant ID is extracted from the JWT token:
```kotlin
val tenantId = dokusPrincipal.requireTenantId()
```

## Integration Points

### Service Dependencies

| Service | Dependency | Purpose |
|---------|------------|---------|
| **Auth** | TenantTable (FK) | Multi-tenant isolation |
| **Cashflow** | InvoicesTable (FK) | Payment-to-invoice linking |
| **Banking** | transaction_id field | Bank reconciliation |

### Data Flow

```
1. Invoice Created (Cashflow) → Invoice ID available
2. Payment Received → Payment record created (Payment)
3. Bank Transaction Synced (Banking) → Transaction ID available
4. Reconciliation → Payment linked to bank transaction
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KTOR_DEPLOYMENT_PORT` | Server port | 8080 |
| `KTOR_DEPLOYMENT_HOST` | Server host | 0.0.0.0 |
| `DATABASE_URL` | PostgreSQL connection URL | - |
| `JWT_SECRET` | JWT signing secret | - |
| `REDIS_URL` | Redis connection URL | - |

### HOCON Configuration

```hocon
ktor {
    deployment {
        port = 8080
        host = "0.0.0.0"
        environment = "development"
    }
}

database {
    jdbcUrl = "jdbc:postgresql://localhost:5432/dokus"
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = 10
}

jwt {
    secret = ${JWT_SECRET}
    issuer = "dokus"
    audience = "dokus-users"
    realm = "dokus"
}
```

## Development

### Running Locally

```bash
# From project root
./gradlew :features:payment:backend:run
```

### Prerequisites

- PostgreSQL with tables: `tenants` (auth), `invoices` (cashflow)
- Redis for caching
- JWT secret configured

### Service Startup Flow

1. Load configuration (`AppBaseConfig`)
2. Initialize Koin dependency injection
3. Connect to PostgreSQL database
4. Initialize `PaymentsTable` (idempotent)
5. Configure JWT authentication
6. Start Ktor server with Netty

### Implementation Status

> **Note**: This service provides API endpoints that are currently stub implementations (returning empty lists). The `PaymentRepository` in the foundation module is fully implemented and ready for integration.

**Implemented:**
- Database schema and table initialization
- PaymentRepository with full CRUD + reconciliation
- Type-safe route definitions
- JWT authentication setup
- Health endpoints

**TODO:**
- Wire routes to PaymentRepository
- Add create/update/delete endpoints
- Implement pending/overdue payment logic
- Add refund support

## Dependencies

- **Ktor** - Web framework with Netty engine
- **Exposed** - Kotlin SQL framework (ORM)
- **Koin** - Dependency injection
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **kotlinx.serialization** - JSON serialization
- **kotlinx-datetime** - Date/time handling
