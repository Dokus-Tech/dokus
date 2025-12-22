# Banking Service

Bank connections and transaction management service for the Dokus platform.

## Overview

The Banking service provides bank account integration and transaction synchronization:

- **Bank Connections**: Connect bank accounts via third-party providers (Plaid, Tink, Nordigen)
- **Transaction Syncing**: Automatically import transactions from connected bank accounts
- **Reconciliation**: Match bank transactions to invoices and expenses
- **Multi-Tenant**: Complete tenant isolation with encrypted credentials

The service enables automatic bank transaction import and reconciliation to streamline financial tracking.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             REST API Routes                                  │
│                     ┌──────────────────────────────┐                        │
│                     │        Health Routes          │                        │
│                     │    (Foundation health checks) │                        │
│                     └──────────────┬───────────────┘                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Repository Layer                                │
│                     ┌──────────────────────────────┐                        │
│                     │     BankingRepository        │                        │
│                     │ (Connections & Transactions) │                        │
│                     └──────────────────────────────┘                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────┐
│                               Database                                       │
│  ┌──────────────────────┐   ┌──────────────────────┐                        │
│  │  BankConnectionsTable │   │  BankTransactionsTable│                        │
│  │  (Account links)      │   │  (Synced transactions)│                        │
│  └──────────────────────┘   └──────────────────────┘                        │
│                                     │                                        │
│                    ┌────────────────┼────────────────┐                      │
│                    ▼                ▼                ▼                      │
│              ┌──────────┐    ┌──────────┐    ┌──────────┐                   │
│              │ TenantTable│   │ ExpensesTable│  │ InvoicesTable│              │
│              │ (Auth)     │   │ (Cashflow)  │  │ (Cashflow)  │              │
│              └──────────┘    └──────────┘    └──────────┘                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Database Schema

### BankConnectionsTable

Stores bank account connections with encrypted access tokens.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `tenant_id` | UUID | FK to TenantTable (Auth service) |
| `provider` | Enum | Bank provider (PLAID, TINK, NORDIGEN) |
| `institution_id` | VARCHAR(255) | Bank institution identifier |
| `institution_name` | VARCHAR(255) | Bank institution name |
| `account_id` | VARCHAR(255) | External account identifier |
| `account_name` | VARCHAR(255) | Account display name |
| `account_type` | Enum | Account type (CHECKING, SAVINGS, etc.) |
| `currency` | Enum | Currency (EUR, USD, etc.) |
| `access_token` | TEXT | Encrypted access token |
| `last_synced_at` | DATETIME | Last successful sync timestamp |
| `is_active` | BOOLEAN | Whether connection is active |
| `created_at` | DATETIME | Creation timestamp |
| `updated_at` | DATETIME | Last update timestamp |

**Indexes:**
- `tenant_id, is_active` - Active connections lookup
- `tenant_id, provider, account_id` (unique) - Prevent duplicate connections

### BankTransactionsTable

Stores transactions synced from bank connections.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `bank_connection_id` | UUID | FK to BankConnectionsTable |
| `tenant_id` | UUID | FK to TenantTable (Auth service) |
| `external_id` | VARCHAR(255) | Bank's transaction identifier |
| `date` | DATE | Transaction date |
| `amount` | DECIMAL(12,2) | Transaction amount |
| `description` | TEXT | Transaction description |
| `merchant_name` | VARCHAR(255) | Merchant/payee name |
| `category` | VARCHAR(100) | Bank-provided category |
| `is_pending` | BOOLEAN | Whether transaction is pending |
| `expense_id` | UUID | FK to ExpensesTable (reconciliation) |
| `invoice_id` | UUID | FK to InvoicesTable (reconciliation) |
| `is_reconciled` | BOOLEAN | Whether matched to expense/invoice |
| `created_at` | DATETIME | Creation timestamp |

**Indexes:**
- `tenant_id, date` - Date range queries
- `bank_connection_id, date` - Per-connection queries
- `tenant_id, is_reconciled` - Unreconciled transactions
- `bank_connection_id, external_id` (unique) - Prevent duplicates

## Key Files

```
features/banking/backend/
├── src/main/kotlin/ai/dokus/banking/backend/
│   ├── Application.kt              # Entry point, Ktor configuration
│   ├── config/
│   │   └── DependencyInjection.kt  # Koin DI module setup
│   ├── database/
│   │   └── BankingTables.kt        # Database table initialization
│   └── plugins/
│       ├── Routing.kt              # Health routes
│       └── Database.kt             # Database configuration
├── src/main/resources/
│   ├── application.conf            # Base configuration
│   ├── application-local.conf      # Local development
│   ├── application-cloud.conf      # Cloud/production
│   ├── application-pro.conf        # Self-hosted (Pro)
│   └── application-lite.conf       # Self-hosted (Lite)
└── build.gradle.kts
```

### Foundation Layer

The service uses shared components from `foundation/`:

```
foundation/database/
├── tables/banking/
│   ├── BankConnectionsTable.kt     # Connection table definition
│   └── BankTransactionsTable.kt    # Transaction table definition
└── repository/banking/
    └── BankingRepository.kt        # Connection & transaction operations
```

## Repository Operations

### BankingRepository

| Method | Description |
|--------|-------------|
| `createConnection()` | Create a new bank connection |
| `getConnection()` | Get a connection by ID (tenant-scoped) |
| `listConnections()` | List connections for a tenant |
| `updateLastSyncedAt()` | Update sync timestamp |
| `deactivateConnection()` | Soft-delete a connection |
| `createTransaction()` | Create a synced transaction |
| `getTransaction()` | Get a transaction by ID (tenant-scoped) |
| `listTransactions()` | List transactions with filters |
| `reconcileWithExpense()` | Link transaction to expense |
| `reconcileWithInvoice()` | Link transaction to invoice |

## Configuration

### Configuration Profiles

| Profile | File | Use Case | Description |
|---------|------|----------|-------------|
| **local** | `application-local.conf` | Development | Local PostgreSQL, memory caching |
| **cloud** | `application-cloud.conf` | Production | Docker services, Redis caching |
| **pro** | `application-pro.conf` | Self-hosted Pro | Full features, larger pools |
| **lite** | `application-lite.conf` | Self-hosted Lite | Smaller resource footprint |

### HOCON Configuration

```hocon
ktor {
  deployment {
    port = 8080
    host = "0.0.0.0"
    environment = "local"
  }
}

database {
  url = "jdbc:postgresql://localhost:15546/dokus_banking"
  username = "dev"
  password = "devpassword"
  pool {
    maxSize = 20
    minSize = 5
  }
}

jwt {
  issuer = "dokus"
  audience = "dokus-api"
  secret = ${JWT_SECRET}
}

caching {
  type = "memory"  # or "redis"
  redis {
    host = "localhost"
    port = 6379
  }
}

server {
  bankingEnabled = true
}
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_SECRET` | Secret for JWT validation | Yes |
| `DB_URL` | PostgreSQL connection URL | No (has default) |
| `DB_USERNAME` | Database username | No (has default) |
| `DB_PASSWORD` | Database password | Yes (production) |
| `REDIS_HOST` | Redis server hostname | No |
| `REDIS_PASSWORD` | Redis password | No |
| `BANKING_ENABLED` | Enable banking features | No (default: true) |

## Integration Points

### Dependencies (This Service Requires)

| Service | Dependency | Description |
|---------|------------|-------------|
| **Auth** | `TenantTable` | Multi-tenant isolation |
| **Foundation** | `DatabaseFactory` | Database connection management |
| **Foundation** | `JwtValidator` | JWT token validation |

### Dependents (Services That Use Banking)

| Service | Integration | Description |
|---------|-------------|-------------|
| **Cashflow** | `ExpensesTable`, `InvoicesTable` | Transaction reconciliation |

## Security

### Multi-Tenant Isolation

**CRITICAL**: All repository queries MUST include `tenant_id` filter to prevent data leakage.

```kotlin
// CORRECT - Always filter by tenant
suspend fun getConnection(connectionId: BankConnectionId, tenantId: TenantId)

// WRONG - Never query without tenant filter
suspend fun getConnection(connectionId: BankConnectionId) // DON'T DO THIS
```

### Access Token Security

- Bank access tokens are stored encrypted in the database
- Access tokens should be encrypted BEFORE passing to repository methods
- Never expose raw access tokens in API responses
- Use secure key management for encryption keys

## Development

### Local Development

1. Ensure PostgreSQL is running on port 15546
2. Create the database: `dokus_banking`
3. Configure environment variables
4. Run the service:
   ```bash
   ./gradlew :features:banking:backend:run
   ```

### Health Check

The service exposes standard health endpoints:
- `GET /health` - Basic health check
- `GET /health/ready` - Readiness probe
- `GET /health/live` - Liveness probe

### Building

```bash
# Build with shadow JAR
./gradlew :features:banking:backend:shadowJar

# Run tests
./gradlew :features:banking:backend:test
```

## Bank Providers

The service supports multiple bank aggregation providers:

| Provider | Description | Status |
|----------|-------------|--------|
| **Plaid** | US/Canada bank connections | Supported |
| **Tink** | European bank connections | Supported |
| **Nordigen** | European PSD2 bank connections | Supported |

## Dependencies

- **Ktor** - Web framework with Netty engine
- **Exposed** - SQL ORM for Kotlin
- **Koin** - Dependency injection
- **Kotlinx Serialization** - JSON serialization
- **HikariCP** - Connection pooling
- **Flyway** - Database migrations
- **Micrometer** - Metrics collection
