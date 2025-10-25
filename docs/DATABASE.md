# Database Schema

**Last Updated:** October 2025
**Database:** PostgreSQL 17
**ORM:** Exposed 0.61.0
**Status:** Production Schema

---

## Table of Contents

1. [Overview](#overview)
2. [Design Principles](#design-principles)
3. [Schema Hierarchy](#schema-hierarchy)
4. [Core Tables](#core-tables)
5. [Exposed ORM Implementation](#exposed-orm-implementation)
6. [Indexing Strategy](#indexing-strategy)
7. [Migration Management](#migration-management)
8. [Best Practices](#best-practices)

---

## Overview

The Dokus database schema is designed for a **multi-tenant SaaS application** where each tenant (freelancer/company) has isolated data within a shared PostgreSQL database.

### Key Characteristics

- **Multi-Tenancy**: Shared database with `tenant_id` discriminator on every table
- **Financial Precision**: NUMERIC(12, 2) for all monetary values (never FLOAT!)
- **Audit Compliance**: Complete immutable audit trail for financial operations
- **Type Safety**: UUID primary keys, proper foreign key constraints
- **Performance**: Strategic indexes for common query patterns
- **GDPR Ready**: Data export and deletion capabilities

### Technology Stack

```yaml
Database: PostgreSQL 17
ORM: JetBrains Exposed 0.61.0
Migration Tool: Flyway 11.15.0
Connection Pool: HikariCP 7.0.2
```

---

## Design Principles

### 1. Multi-Tenancy with Row-Level Isolation

Every table (except system/lookup tables) includes a `tenant_id` column.

**Security-Critical Rule:**
```kotlin
// ✅ CORRECT - Always filter by tenant_id
fun getInvoice(id: UUID, tenantId: UUID) = transaction {
    Invoices.select {
        (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
    }.singleOrNull()
}

// ❌ WRONG - Security vulnerability! Data leak!
fun getInvoice(id: UUID) = transaction {
    Invoices.select { Invoices.id eq id }.singleOrNull()
}
```

**Migration Path:**
- **0-1,000 tenants**: Shared database ✅ (Current MVP)
- **1,000-10,000 tenants**: Add read replicas
- **10,000+ tenants**: Consider database-per-tenant or sharding

### 2. Financial Data Precision

**CRITICAL: Always use NUMERIC, never FLOAT/REAL**

```sql
-- ✅ CORRECT - Exact decimal precision
total_amount NUMERIC(12, 2)  -- Up to €9,999,999,999.99

-- ❌ WRONG - Introduces rounding errors (catastrophic for finance!)
total_amount FLOAT  -- Can lose cents in calculations!
```

**Why NUMERIC(12, 2)?**
- `12` total digits: Handles invoices up to €9.99 billion
- `2` decimal places: Cent precision (required by law)
- No rounding errors (FLOAT has precision loss)
- Tax-compliant calculations

**Exposed Implementation:**
```kotlin
val totalAmount = decimal("total_amount", 12, 2)
val vatAmount = decimal("vat_amount", 12, 2)
```

### 3. Immutable Audit Logging

**Requirements (Legal Compliance):**
- Log ALL financial operations (create, update, delete)
- Store 7 years minimum (GDPR + Belgian tax law)
- Never update or delete audit logs
- Include: who, what, when, where, before/after

**Audit Log Pattern:**
```kotlin
suspend fun auditLog(
    tenantId: UUID,
    userId: UUID?,
    action: String,        // "invoice.created"
    entityType: String,    // "invoice"
    entityId: UUID,
    oldValues: String?,    // JSON before changes
    newValues: String?     // JSON after changes
) {
    transaction {
        AuditLogs.insert {
            it[this.tenantId] = tenantId
            it[this.userId] = userId
            it[this.action] = action
            it[this.entityType] = entityType
            it[this.entityId] = entityId
            it[this.oldValues] = oldValues
            it[this.newValues] = newValues
            it[ipAddress] = getClientIp()
            it[userAgent] = getUserAgent()
        }
    }
}
```

### 4. Type Safety with UUIDs

**Benefits:**
- Non-sequential (security - no enumeration attacks)
- Globally unique (no ID collisions)
- Can be generated client-side
- No database round-trip for ID generation

**Implementation:**
```kotlin
object Invoices : UUIDTable("invoices") {
    // Automatically creates UUID primary key as `id`
}
```

---

## Schema Hierarchy

```
Tenants (Root Entity)
│
├── Users
│   └── RefreshTokens
│
├── TenantSettings (1:1)
│
├── Clients
│   └── Invoices
│       ├── InvoiceItems
│       └── Payments
│
├── Expenses
│
├── BankConnections
│   └── BankTransactions
│
├── VatReturns
│
├── AuditLogs
│
└── Attachments
```

**Relationship Summary:**
- **1:Many** - Tenant → Users, Clients, Invoices, Expenses
- **1:1** - Tenant → TenantSettings
- **Many:1** - Invoices → Client
- **1:Many** - Invoice → InvoiceItems

---

## Core Tables

### Authentication & Multi-Tenancy

#### Tenants

Root entity representing each customer account.

**Exposed Definition:**
```kotlin
object Tenants : UUIDTable("tenants") {
    // Identity
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()

    // Subscription
    val plan = varchar("plan", 50)  // 'free', 'professional', 'business'
    val status = varchar("status", 50).default("active")
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()

    // Localization
    val country = varchar("country", 2).default("BE")  // ISO 3166-1 alpha-2
    val language = varchar("language", 5).default("en")

    // Business info
    val vatNumber = varchar("vat_number", 50).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, email)
        index(false, status)
    }
}
```

**Status Values:**
- `active` - Paying customer
- `suspended` - Payment failed
- `cancelled` - Subscription ended

#### Users

People with access to a tenant's account.

**Exposed Definition:**
```kotlin
object Users : UUIDTable("users") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    val email = varchar("email", 255).uniqueIndex()

    // Authentication
    val passwordHash = varchar("password_hash", 255)  // bcrypt
    val mfaSecret = varchar("mfa_secret", 255).nullable()  // TOTP, encrypted!

    // Authorization
    val role = varchar("role", 50)  // 'owner', 'member', 'accountant', 'viewer'

    // Profile
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()

    // Status
    val isActive = bool("is_active").default(true)
    val lastLoginAt = datetime("last_login_at").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, email)
        index(false, tenantId, isActive)
    }
}
```

**Role Permissions:**
- `owner` - Full access, billing, settings
- `member` - Create/edit invoices and expenses
- `accountant` - View-only financial data
- `viewer` - Dashboard only

#### RefreshTokens

JWT refresh token management.

**Exposed Definition:**
```kotlin
object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, userId)
        index(false, token)
        index(false, expiresAt)  // For cleanup jobs
    }
}
```

**Token Lifecycle:**
- Access token: 15 minutes
- Refresh token: 7 days
- Revoked on logout
- Automatically cleaned up after expiry

---

### Business Entities

#### Clients

Customers who receive invoices.

**Exposed Definition:**
```kotlin
object Clients : UUIDTable("clients") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)

    // Basic info
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val vatNumber = varchar("vat_number", 50).nullable()

    // Address
    val addressLine1 = varchar("address_line_1", 255).nullable()
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).nullable()

    // Contact
    val contactPerson = varchar("contact_person", 255).nullable()
    val phone = varchar("phone", 50).nullable()

    // Peppol (Belgium 2026 requirement)
    val peppolId = varchar("peppol_id", 100).nullable()
    val peppolEnabled = bool("peppol_enabled").default(false)

    // Additional
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, name)
        index(false, tenantId, isActive)
    }
}
```

#### Invoices

Core financial documents.

**Exposed Definition:**
```kotlin
object Invoices : UUIDTable("invoices") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    val clientId = reference("client_id", Clients, onDelete = ReferenceOption.RESTRICT)

    // Identification
    val invoiceNumber = varchar("invoice_number", 50)
    val issueDate = date("issue_date")
    val dueDate = date("due_date")

    // Amounts - CRITICAL: Use NUMERIC(12, 2)
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val paidAmount = decimal("paid_amount", 12, 2).default(BigDecimal.ZERO)

    // Status
    val status = varchar("status", 50)
    // 'draft', 'sent', 'viewed', 'paid', 'overdue', 'cancelled'

    // Peppol e-invoicing (Belgium 2026 requirement)
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolSentAt = datetime("peppol_sent_at").nullable()
    val peppolStatus = varchar("peppol_status", 50).nullable()

    // Payment integration
    val paymentLink = varchar("payment_link", 500).nullable()
    val paymentLinkExpiresAt = datetime("payment_link_expires_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    val paymentMethod = varchar("payment_method", 50).nullable()

    // Additional
    val currency = varchar("currency", 3).default("EUR")
    val notes = text("notes").nullable()
    val termsAndConditions = text("terms_and_conditions").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, invoiceNumber)  // Unique per tenant
        index(false, tenantId)
        index(false, clientId)
        index(false, status)
        index(false, issueDate)
        index(false, dueDate)
        index(false, tenantId, status, dueDate)  // Dashboard query optimization
    }
}
```

**Status Transitions:**
```
draft → sent → viewed → paid
  ↓       ↓       ↓
  └───────┴───────┴──→ cancelled

Auto: sent → overdue (when past due_date)
```

#### InvoiceItems

Line items on invoices.

**Exposed Definition:**
```kotlin
object InvoiceItems : UUIDTable("invoice_items") {
    val invoiceId = reference("invoice_id", Invoices, onDelete = ReferenceOption.CASCADE)

    val description = text("description")
    val quantity = decimal("quantity", 10, 2)
    val unitPrice = decimal("unit_price", 12, 2)
    val vatRate = decimal("vat_rate", 5, 2)  // e.g., 21.00 for 21%

    // Calculated fields
    val lineTotal = decimal("line_total", 12, 2)    // quantity × unitPrice
    val vatAmount = decimal("vat_amount", 12, 2)    // lineTotal × (vatRate/100)

    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, invoiceId)
    }
}
```

**Calculation Example:**
```
Quantity: 40 hours
Unit Price: €75.00/hour
VAT Rate: 21%

Line Total = 40 × €75.00 = €3,000.00
VAT Amount = €3,000.00 × 0.21 = €630.00
Total = €3,630.00
```

#### Expenses

Business costs for tax deductions.

**Exposed Definition:**
```kotlin
object Expenses : UUIDTable("expenses") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)

    val date = date("date")
    val merchant = varchar("merchant", 255)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 2).nullable()

    val category = varchar("category", 100)
    // 'software', 'hardware', 'travel', 'office', 'meals', 'marketing', 'other'

    val description = text("description").nullable()

    // Receipt storage
    val receiptUrl = varchar("receipt_url", 500).nullable()      // S3 path
    val receiptFilename = varchar("receipt_filename", 255).nullable()

    // Tax deductibility
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2)
        .default(BigDecimal("100.00"))

    val paymentMethod = varchar("payment_method", 50).nullable()
    val isRecurring = bool("is_recurring").default(false)
    val notes = text("notes").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, category)
        index(false, tenantId, date)      // Monthly reports
        index(false, tenantId, category)  // Category reports
    }
}
```

**Deductibility Examples:**
- Software: 100% deductible
- Hardware: 100% deductible
- Meals: 69% deductible (Belgian rule)
- Travel: 100% deductible (business trips)

#### Payments

Payment records linked to invoices.

**Exposed Definition:**
```kotlin
object Payments : UUIDTable("payments") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    val invoiceId = reference("invoice_id", Invoices)

    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 3).default("EUR")
    val paymentDate = date("payment_date")
    val paymentMethod = varchar("payment_method", 50)
    // 'bank_transfer', 'credit_card', 'stripe', 'mollie', 'cash'

    // Gateway information
    val gatewayId = varchar("gateway_id", 100).nullable()
    val gatewayStatus = varchar("gateway_status", 50).nullable()
    val gatewayResponse = text("gateway_response").nullable()

    // References
    val bankTransactionId = uuid("bank_transaction_id").nullable()
    val paymentLinkId = varchar("payment_link_id", 100).nullable()

    // Fees
    val processingFee = decimal("processing_fee", 10, 4).nullable()
    val netAmount = decimal("net_amount", 12, 2).nullable()

    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, invoiceId)
        index(false, tenantId, paymentDate)
        uniqueIndex(gatewayId)  // Prevent duplicate payment processing
    }
}
```

#### AuditLogs

Immutable audit trail for compliance.

**Exposed Definition:**
```kotlin
object AuditLogs : UUIDTable("audit_logs") {
    val tenantId = reference("tenant_id", Tenants)
    val userId = reference("user_id", Users).nullable()

    val action = varchar("action", 100)      // "invoice.created", "expense.updated"
    val entityType = varchar("entity_type", 50)  // "invoice", "expense"
    val entityId = uuid("entity_id")

    // Change tracking
    val oldValues = text("old_values").nullable()  // JSON before
    val newValues = text("new_values").nullable()  // JSON after

    // Request metadata
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, entityType, entityId)
        index(false, createdAt)  // For date range queries
        index(false, tenantId, action)
    }
}
```

**Important:** Audit logs are **never** updated or deleted (legal requirement).

---

## Exposed ORM Implementation

### Basic Repository Pattern

```kotlin
abstract class BaseRepository {
    protected suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO) {
            block()
        }
}

class InvoiceRepository : BaseRepository() {
    suspend fun create(
        tenantId: UUID,
        clientId: UUID,
        invoiceNumber: String,
        issueDate: LocalDate,
        dueDate: LocalDate,
        items: List<InvoiceItemData>
    ): UUID = dbQuery {
        // Calculate totals
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val vat = items.sumOf { it.vatAmount }
        val total = subtotal + vat

        // Insert invoice
        val invoiceId = Invoices.insertAndGetId {
            it[Invoices.tenantId] = tenantId
            it[Invoices.clientId] = clientId
            it[Invoices.invoiceNumber] = invoiceNumber
            it[Invoices.issueDate] = issueDate
            it[Invoices.dueDate] = dueDate
            it[subtotalAmount] = subtotal
            it[vatAmount] = vat
            it[totalAmount] = total
            it[status] = "draft"
        }.value

        // Insert items
        items.forEachIndexed { index, item ->
            InvoiceItems.insert {
                it[InvoiceItems.invoiceId] = invoiceId
                it[description] = item.description
                it[quantity] = item.quantity
                it[unitPrice] = item.unitPrice
                it[vatRate] = item.vatRate
                it[lineTotal] = item.lineTotal
                it[vatAmount] = item.vatAmount
                it[sortOrder] = index
            }
        }

        // Audit log
        auditLog(
            tenantId = tenantId,
            action = "invoice.created",
            entityType = "invoice",
            entityId = invoiceId,
            newValues = """{"invoiceNumber": "$invoiceNumber"}"""
        )

        invoiceId
    }

    suspend fun findById(id: UUID, tenantId: UUID): Invoice? = dbQuery {
        Invoices
            .select {
                (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
            }
            .singleOrNull()
            ?.let { mapToInvoice(it) }
    }

    suspend fun findByTenant(
        tenantId: UUID,
        status: String? = null,
        limit: Int = 100
    ): List<Invoice> = dbQuery {
        val query = Invoices.select { Invoices.tenantId eq tenantId }

        status?.let {
            query.andWhere { Invoices.status eq it }
        }

        query
            .orderBy(Invoices.issueDate to SortOrder.DESC)
            .limit(limit)
            .map { mapToInvoice(it) }
    }
}
```

---

## Indexing Strategy

### Indexing Rules

1. **Always index `tenant_id`** (for multi-tenant queries)
2. **Index foreign keys** (for joins)
3. **Index filter columns** (status, date ranges)
4. **Composite indexes** for common queries
5. **Unique indexes** for uniqueness constraints

### Common Index Patterns

```kotlin
// Single column
index(false, tenantId)

// Unique constraint
uniqueIndex(tenantId, invoiceNumber)

// Composite for queries like: WHERE tenant_id = ? AND status = ? AND due_date < ?
index(false, tenantId, status, dueDate)

// Partial index (PostgreSQL specific, use raw SQL)
// CREATE INDEX idx_active_invoices ON invoices(tenant_id, status) WHERE status != 'cancelled'
```

### Query Performance Tips

```kotlin
// ✅ GOOD - Uses indexes effectively
Invoices.select {
    (Invoices.tenantId eq tenantId) and
    (Invoices.status eq "sent") and
    (Invoices.dueDate less LocalDate.now())
}.limit(50)

// ❌ BAD - Full table scan, very slow
Invoices.selectAll()
    .filter { it[tenantId] == tenantId }
    .take(50)
```

---

## Migration Management

### Flyway Migration Structure

```
foundation/database/src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_peppol_fields.sql
├── V3__add_audit_logs.sql
└── V4__add_payment_links.sql
```

### Migration Example

**V1__initial_schema.sql:**
```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    plan VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    country VARCHAR(2) DEFAULT 'BE',
    vat_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_email ON tenants(email);
CREATE INDEX idx_tenants_status ON tenants(status);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
```

### Migration Best Practices

1. **Never modify existing migrations** (they're versioned)
2. **Always test on copy of production data**
3. **Write rollback scripts** for critical changes
4. **Backward compatible** changes when possible
5. **Use transactions** for data migrations

---

## Best Practices

### 1. Always Filter by tenant_id

```kotlin
// Every query MUST include tenant_id filter
suspend fun getInvoices(tenantId: UUID): List<Invoice> = dbQuery {
    Invoices.select { Invoices.tenantId eq tenantId }
        .map { mapToInvoice(it) }
}
```

### 2. Use Transactions for Multi-Step Operations

```kotlin
suspend fun createInvoiceWithItems(...) = dbQuery {
    // This entire block is a single transaction
    val invoiceId = Invoices.insertAndGetId { ... }.value
    items.forEach { item ->
        InvoiceItems.insert { ... }
    }
    auditLog(...)
    invoiceId
}
```

### 3. Validate Before Database Operations

```kotlin
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> =
    runCatching {
        // Validate first
        require(request.dueDate > request.issueDate) {
            "Due date must be after issue date"
        }
        require(request.items.isNotEmpty()) {
            "Invoice must have at least one item"
        }

        // Then persist
        repository.create(...)
    }
```

### 4. Use Proper Error Handling

```kotlin
suspend fun findInvoice(id: UUID, tenantId: UUID): Invoice? = dbQuery {
    try {
        Invoices.select {
            (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
        }.singleOrNull()?.let { mapToInvoice(it) }
    } catch (e: Exception) {
        logger.e(e) { "Failed to find invoice $id" }
        null
    }
}
```

### 5. Audit All Financial Operations

```kotlin
suspend fun updateInvoice(id: UUID, updates: InvoiceUpdate) = dbQuery {
    val old = Invoices.select { Invoices.id eq id }.single()

    Invoices.update({ Invoices.id eq id }) {
        // Apply updates...
    }

    auditLog(
        action = "invoice.updated",
        entityType = "invoice",
        entityId = id,
        oldValues = serializeToJson(old),
        newValues = serializeToJson(updates)
    )
}
```

---

## Related Documentation

- [Architecture](./ARCHITECTURE.md) - System architecture
- [API Reference](./API.md) - RPC API documentation
- [Security](./SECURITY.md) - Security best practices
- [Setup Guide](./SETUP.md) - Development setup

---

**Last Updated:** October 2025
