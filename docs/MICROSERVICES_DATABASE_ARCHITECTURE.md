# Microservices Database Architecture

**Last Updated:** November 2025
**Status:** Design Document
**Architecture Pattern:** Database-per-Service with KotlinX RPC

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Design Principles](#design-principles)
3. [Service Database Schemas](#service-database-schemas)
4. [Cross-Service Communication](#cross-service-communication)
5. [Data Consistency Strategies](#data-consistency-strategies)
6. [Shared Data Patterns](#shared-data-patterns)
7. [Referential Integrity](#referential-integrity)
8. [Audit Logging Strategy](#audit-logging-strategy)
9. [Migration Management](#migration-management)
10. [Distributed Transactions](#distributed-transactions)
11. [Performance Optimization](#performance-optimization)
12. [Best Practices](#best-practices)

---

## Architecture Overview

### Service Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│           (Web, Mobile, Desktop - Compose Multiplatform)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ KotlinX RPC
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        RPC Gateway Layer                         │
│              (Shared domain/rpc interfaces)                      │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Auth Service │────▶│Invoice Service│────▶│Payment Service│
│              │     │              │     │              │
│  auth_db     │     │ invoicing_db │     │  payment_db  │
└──────────────┘     └──────────────┘     └──────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│Expense Service│────▶│Reporting Svc │────▶│Banking Service│
│              │     │              │     │              │
│  expense_db  │     │ reporting_db │     │  banking_db  │
└──────────────┘     └──────────────┘     └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │ Audit Service│
                     │              │
                     │   audit_db   │
                     └──────────────┘
```

### Key Characteristics

- **7 Independent Microservices**: Each with its own PostgreSQL database
- **KotlinX RPC Communication**: Type-safe, coroutine-based inter-service calls
- **Multi-Tenant Architecture**: Every database includes tenant_id isolation
- **Event-Driven Updates**: Real-time data synchronization via Flow
- **Distributed Audit Trail**: Centralized audit service for compliance

---

## Design Principles

### 1. Database-per-Service Pattern

**Why Database-per-Service?**

✅ **Advantages:**
- Service independence (deploy, scale, upgrade independently)
- Technology flexibility (can use different DB types if needed)
- Failure isolation (one DB failure doesn't cascade)
- Clear ownership boundaries
- Easier to scale specific services

⚠️ **Trade-offs:**
- No cross-database JOINs
- Eventual consistency instead of ACID transactions
- Data duplication required
- More complex queries spanning services

**When to Use Shared Database Instead:**
- Small team (< 5 developers)
- Simple domain (< 10 tables total)
- Strong consistency required
- MVP/early stage

**Dokus Decision:** Database-per-service because:
- Clear domain boundaries (invoicing, expenses, payments, etc.)
- Need to scale services independently (reporting is heavy)
- Future flexibility (different teams can own services)
- Compliance isolation (audit logs separate)

### 2. Eventual Consistency Model

**Consistency Spectrum:**

```
Strong Consistency          Eventual Consistency
(Single DB)                (Microservices)
     │                            │
     ├─ ACID transactions         ├─ BASE (Basically Available, Soft state, Eventually consistent)
     ├─ Immediate consistency     ├─ Async replication
     ├─ JOINs work               ├─ RPC calls
     └─ Simple to reason         └─ Complex but scalable
```

**Dokus Approach:**
- **Synchronous RPC** for critical operations (e.g., create invoice)
- **Asynchronous events** for non-critical updates (e.g., analytics)
- **Read replicas** for local caching where needed
- **Idempotency** for all RPC operations

### 3. Multi-Tenant Isolation

**Critical Security Rule:** EVERY table in EVERY database MUST include `tenant_id`.

```kotlin
// ✅ CORRECT - Every service enforces tenant isolation
suspend fun getInvoice(id: InvoiceId, tenantId: TenantId): Result<Invoice> {
    return dbQuery {
        InvoicesTable.select {
            (InvoicesTable.id eq id.value) and
            (InvoicesTable.tenantId eq tenantId.value)
        }.singleOrNull()?.toInvoice()
    }.toResult()
}

// ❌ WRONG - Cross-tenant data leak vulnerability!
suspend fun getInvoice(id: InvoiceId): Result<Invoice> {
    return dbQuery {
        InvoicesTable.select { InvoicesTable.id eq id.value }
            .singleOrNull()?.toInvoice()
    }.toResult()
}
```

### 4. Financial Data Precision

**CRITICAL:** All services handling money MUST use `NUMERIC(12, 2)` - NEVER `FLOAT` or `DOUBLE`.

```kotlin
// In every service handling money
val amount = decimal("amount", 12, 2)
val vatAmount = decimal("vat_amount", 12, 2)
val totalAmount = decimal("total_amount", 12, 2)
```

**Why NUMERIC(12, 2)?**
- Exact decimal arithmetic (no floating-point errors)
- Up to €9,999,999,999.99 (sufficient for SMB invoices)
- 2 decimal places (cent precision, legally required)
- Tax-compliant calculations

---

## Service Database Schemas

### 1. Auth Service Database (`auth_db`)

**Responsibility:** User authentication, tenant management, authorization

**Tables:**

```kotlin
// TenantsTable - Root entity for multi-tenancy
object TenantsTable : UUIDTable("tenants") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val plan = dbEnumeration<TenantPlan>("plan")
    val status = dbEnumeration<TenantStatus>("status").default(TenantStatus.Active)
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()
    val country = varchar("country", 2).default("BE")
    val language = dbEnumeration<Language>("language").default(Language.En)
    val vatNumber = varchar("vat_number", 50).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, email)
        index(false, status)
    }
}

// UsersTable - People with access to tenant accounts
object UsersTable : UUIDTable("users") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val mfaSecret = varchar("mfa_secret", 255).nullable()
    val role = dbEnumeration<UserRole>("role")
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val emailVerified = bool("email_verified").default(false)
    val emailVerificationToken = varchar("email_verification_token", 255).nullable()
    val emailVerificationExpiry = datetime("email_verification_expiry").nullable()
    val isActive = bool("is_active").default(true)
    val lastLoginAt = datetime("last_login_at").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, email)
        index(false, tenantId, isActive)
    }
}

// RefreshTokensTable - JWT refresh token management
object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, userId)
        index(false, token)
        index(false, expiresAt)
    }
}

// PasswordResetTokensTable - Password reset flow
object PasswordResetTokensTable : UUIDTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isUsed = bool("is_used").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, userId)
        index(false, token)
    }
}

// TenantSettingsTable - Tenant-specific configuration (1:1 with Tenant)
object TenantSettingsTable : UUIDTable("tenant_settings") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()

    // Company information
    val companyName = varchar("company_name", 255).nullable()
    val companyNumber = varchar("company_number", 50).nullable()
    val vatNumber = varchar("vat_number", 50).nullable()
    val addressLine1 = varchar("address_line_1", 255).nullable()
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).default("BE")

    // Invoice settings
    val invoicePrefix = varchar("invoice_prefix", 10).nullable()
    val nextInvoiceNumber = integer("next_invoice_number").default(1)
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2).default(BigDecimal("21.00"))

    // Banking
    val bankName = varchar("bank_name", 255).nullable()
    val iban = varchar("iban", 50).nullable()
    val bic = varchar("bic", 20).nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
    }
}
```

**Why Tenants Live Here:**
- Auth service is the "system of record" for tenants
- All other services reference tenants via RPC, not direct FK
- Enables moving tenants between databases/shards later

---

### 2. Invoicing Service Database (`invoicing_db`)

**Responsibility:** Invoice management, client management, invoice items

**Tables:**

```kotlin
// ClientsTable - Customers who receive invoices
// NOTE: No FK to tenants - tenant_id is just UUID
object ClientsTable : UUIDTable("clients") {
    val tenantId = uuid("tenant_id")  // ⚠️ No FK to auth_db!

    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val vatNumber = varchar("vat_number", 50).nullable()

    val addressLine1 = varchar("address_line_1", 255).nullable()
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).nullable()

    val contactPerson = varchar("contact_person", 255).nullable()
    val phone = varchar("phone", 50).nullable()

    val companyNumber = varchar("company_number", 50).nullable()
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2).nullable()

    // Peppol e-invoicing (Belgium 2026)
    val peppolId = varchar("peppol_id", 100).nullable()
    val peppolEnabled = bool("peppol_enabled").default(false)

    val tags = varchar("tags", 500).nullable()
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, name)
        index(false, tenantId, isActive)
    }
}

// InvoicesTable - Core financial documents
object InvoicesTable : UUIDTable("invoices") {
    val tenantId = uuid("tenant_id")
    val clientId = reference("client_id", ClientsTable, onDelete = ReferenceOption.RESTRICT)

    val invoiceNumber = varchar("invoice_number", 50)
    val issueDate = date("issue_date")
    val dueDate = date("due_date")

    // Financial amounts - NUMERIC(12, 2) for precision
    val subtotalAmount = decimal("subtotal_amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)
    val totalAmount = decimal("total_amount", 12, 2)
    val paidAmount = decimal("paid_amount", 12, 2).default(BigDecimal.ZERO)

    val status = dbEnumeration<InvoiceStatus>("status")

    // Peppol e-invoicing
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolSentAt = datetime("peppol_sent_at").nullable()
    val peppolStatus = dbEnumeration<PeppolStatus>("peppol_status").nullable()

    // Payment tracking
    val paymentLink = varchar("payment_link", 500).nullable()
    val paymentLinkExpiresAt = datetime("payment_link_expires_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()

    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val notes = text("notes").nullable()
    val termsAndConditions = text("terms_and_conditions").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, invoiceNumber)
        index(false, tenantId)
        index(false, clientId)
        index(false, status)
        index(false, issueDate)
        index(false, dueDate)
        index(false, tenantId, status, dueDate) // Dashboard optimization
    }
}

// InvoiceItemsTable - Line items on invoices
object InvoiceItemsTable : UUIDTable("invoice_items") {
    val invoiceId = reference("invoice_id", InvoicesTable, onDelete = ReferenceOption.CASCADE)

    val description = text("description")
    val quantity = decimal("quantity", 10, 2)
    val unitPrice = decimal("unit_price", 12, 2)
    val vatRate = decimal("vat_rate", 5, 2)

    // Calculated fields (denormalized for performance)
    val lineTotal = decimal("line_total", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2)

    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, invoiceId)
    }
}
```

**Key Design Decisions:**
- `clientId` FK is within this database (OK - owned by this service)
- `tenantId` is NOT FK to auth_db (loose coupling)
- Validation that tenant exists happens via RPC call to Auth service

---

### 3. Expense Service Database (`expense_db`)

**Responsibility:** Expense tracking, receipt storage, tax deductibility

**Tables:**

```kotlin
// ExpensesTable - Business costs for tax deductions
object ExpensesTable : UUIDTable("expenses") {
    val tenantId = uuid("tenant_id")

    val date = date("date")
    val merchant = varchar("merchant", 255)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 2).nullable()

    val category = dbEnumeration<ExpenseCategory>("category")
    val description = text("description").nullable()

    // Receipt storage (S3/MinIO)
    val receiptUrl = varchar("receipt_url", 500).nullable()
    val receiptFilename = varchar("receipt_filename", 255).nullable()

    // Tax deductibility (Belgium-specific rules)
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2)
        .default(BigDecimal("100.00"))

    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method").nullable()
    val isRecurring = bool("is_recurring").default(false)
    val notes = text("notes").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, category)
        index(false, tenantId, date)
        index(false, tenantId, category)
    }
}

// ExpenseAttachmentsTable - Support for multiple receipts per expense
object ExpenseAttachmentsTable : UUIDTable("expense_attachments") {
    val expenseId = reference("expense_id", ExpensesTable, onDelete = ReferenceOption.CASCADE)

    val filename = varchar("filename", 255)
    val fileUrl = varchar("file_url", 500)
    val fileSize = long("file_size")
    val mimeType = varchar("mime_type", 100)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, expenseId)
    }
}
```

**Deductibility Rules (Belgium):**
- Software: 100%
- Hardware: 100%
- Meals: 69%
- Car expenses: 50-100% (depends on car type)
- Travel: 100% (business trips)

---

### 4. Payment Service Database (`payment_db`)

**Responsibility:** Payment tracking, gateway integration, reconciliation

**Tables:**

```kotlin
// PaymentsTable - Payment transactions against invoices
object PaymentsTable : UUIDTable("payments") {
    val tenantId = uuid("tenant_id")
    val invoiceId = uuid("invoice_id")  // ⚠️ No FK to invoicing_db!

    val amount = decimal("amount", 12, 2)
    val paymentDate = date("payment_date")
    val paymentMethod = dbEnumeration<PaymentMethod>("payment_method")

    // External gateway tracking
    val transactionId = varchar("transaction_id", 255).nullable()
    val gatewayProvider = varchar("gateway_provider", 50).nullable() // stripe, mollie, etc.
    val gatewayStatus = varchar("gateway_status", 50).nullable()
    val gatewayResponse = text("gateway_response").nullable()

    // Processing fees
    val processingFee = decimal("processing_fee", 10, 4).nullable()
    val netAmount = decimal("net_amount", 12, 2).nullable()

    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, invoiceId)
        index(false, paymentDate)
        index(false, tenantId, paymentDate)
        uniqueIndex(transactionId) // Prevent duplicate processing
    }
}

// PaymentWebhooksTable - Webhook event tracking for idempotency
object PaymentWebhooksTable : UUIDTable("payment_webhooks") {
    val provider = varchar("provider", 50)
    val eventId = varchar("event_id", 255).uniqueIndex()
    val eventType = varchar("event_type", 100)
    val payload = text("payload")
    val processedAt = datetime("processed_at").nullable()
    val status = varchar("status", 50).default("pending")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, eventId)
        index(false, status)
    }
}
```

**Cross-Service Coordination:**
When payment is recorded:
1. Payment service creates payment record
2. Payment service calls Invoice API: `updateInvoiceStatus(invoiceId, status)`
3. Invoice service updates invoice.paidAmount and invoice.status
4. Both services emit events for real-time UI updates

---

### 5. Reporting Service Database (`reporting_db`)

**Responsibility:** Aggregated analytics, cached reports, materialized views

**Tables:**

```kotlin
// VatReturnsTable - VAT filing reports
object VatReturnsTable : UUIDTable("vat_returns") {
    val tenantId = uuid("tenant_id")

    val quarter = integer("quarter") // 1, 2, 3, 4
    val year = integer("year")

    val salesVat = decimal("sales_vat", 12, 2) // VAT collected
    val purchasesVat = decimal("purchases_vat", 12, 2) // VAT paid
    val vatOwed = decimal("vat_owed", 12, 2) // Net VAT owed

    val status = varchar("status", 50).default("draft") // draft, filed, paid
    val filedAt = datetime("filed_at").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, year, quarter)
        index(false, tenantId)
        index(false, year, quarter)
    }
}

// FinancialSnapshotsTable - Cached monthly/quarterly reports
object FinancialSnapshotsTable : UUIDTable("financial_snapshots") {
    val tenantId = uuid("tenant_id")

    val periodType = varchar("period_type", 20) // monthly, quarterly, yearly
    val periodStart = date("period_start")
    val periodEnd = date("period_end")

    // Aggregated metrics (denormalized for performance)
    val totalRevenue = decimal("total_revenue", 12, 2)
    val totalExpenses = decimal("total_expenses", 12, 2)
    val netProfit = decimal("net_profit", 12, 2)
    val invoiceCount = integer("invoice_count")
    val expenseCount = integer("expense_count")
    val paymentCount = integer("payment_count")
    val outstandingAmount = decimal("outstanding_amount", 12, 2)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, periodType, periodStart, periodEnd)
        index(false, tenantId)
        index(false, periodStart)
    }
}

// ReportCacheTable - Generic report caching
object ReportCacheTable : UUIDTable("report_cache") {
    val tenantId = uuid("tenant_id")
    val reportType = varchar("report_type", 50)
    val cacheKey = varchar("cache_key", 255)
    val reportData = text("report_data") // JSON
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, reportType, cacheKey)
        index(false, tenantId)
        index(false, expiresAt)
    }
}
```

**Report Generation Strategy:**
1. **Real-time**: Simple queries (< 100 records)
2. **Cached**: Complex aggregations (refreshed every 15 min)
3. **Pre-computed**: Monthly snapshots (generated overnight)

---

### 6. Banking Service Database (`banking_db`)

**Responsibility:** Bank integrations, transaction sync, reconciliation

**Tables:**

```kotlin
// BankConnectionsTable - Bank account connections via Plaid/Tink
object BankConnectionsTable : UUIDTable("bank_connections") {
    val tenantId = uuid("tenant_id")

    val provider = dbEnumeration<BankProvider>("provider")
    val institutionId = varchar("institution_id", 255)
    val institutionName = varchar("institution_name", 255)
    val accountId = varchar("account_id", 255)
    val accountName = varchar("account_name", 255).nullable()
    val accountType = dbEnumeration<BankAccountType>("account_type").nullable()
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // ⚠️ CRITICAL: Must be encrypted at rest!
    val accessToken = text("access_token")

    val lastSyncedAt = datetime("last_synced_at").nullable()
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, isActive)
    }
}

// BankTransactionsTable - Synced bank transactions
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val bankConnectionId = reference("bank_connection_id", BankConnectionsTable)
    val tenantId = uuid("tenant_id")

    val transactionId = varchar("transaction_id", 255).uniqueIndex()
    val date = date("date")
    val description = varchar("description", 500)
    val amount = decimal("amount", 12, 2)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Reconciliation
    val isReconciled = bool("is_reconciled").default(false)
    val reconciledInvoiceId = uuid("reconciled_invoice_id").nullable()
    val reconciledExpenseId = uuid("reconciled_expense_id").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, bankConnectionId)
        index(false, date)
        index(false, isReconciled)
        index(false, tenantId, date)
    }
}
```

**Security:** `accessToken` MUST be encrypted using AES-256-GCM before storage.

---

### 7. Audit Service Database (`audit_db`)

**Responsibility:** Centralized audit trail, compliance logging

**Tables:**

```kotlin
// AuditLogsTable - Immutable audit trail
object AuditLogsTable : UUIDTable("audit_logs") {
    val tenantId = uuid("tenant_id")
    val userId = uuid("user_id").nullable()

    val action = dbEnumeration<AuditAction>("action")
    val entityType = dbEnumeration<EntityType>("entity_type")
    val entityId = varchar("entity_id", 255)
    val serviceName = varchar("service_name", 50) // NEW: which service logged this

    // Change tracking (JSON)
    val oldValues = text("old_values").nullable()
    val newValues = text("new_values").nullable()

    // Request metadata
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, createdAt)
        index(false, entityType, entityId)
        index(false, userId)
        index(false, action)
        index(false, serviceName)
    }
}

// ComplianceEventsTable - GDPR, tax law events
object ComplianceEventsTable : UUIDTable("compliance_events") {
    val tenantId = uuid("tenant_id")
    val userId = uuid("user_id").nullable()

    val eventType = varchar("event_type", 100) // data_export, data_deletion, consent_given
    val details = text("details") // JSON
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, eventType)
    }
}
```

**Retention Policy:**
- Audit logs: 7 years (Belgian tax law requirement)
- Never update or delete audit logs (immutable)
- Archive to cold storage after 2 years

---

## Cross-Service Communication

### RPC Communication Patterns

#### 1. Synchronous RPC Calls

**Use when:** Need immediate response, critical operations

```kotlin
// Example: Creating an invoice (Invoice service needs tenant validation)
class InvoiceServiceImpl(
    private val tenantApi: TenantApi,  // RPC client to Auth service
    private val auditApi: AuditApi      // RPC client to Audit service
) : InvoiceApi {

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
        // 1. Validate tenant exists (RPC to Auth service)
        val tenant = tenantApi.getTenant(request.tenantId).getOrElse {
            return Result.failure(TenantNotFound())
        }

        // 2. Validate client belongs to tenant
        val client = clientRepository.findById(request.clientId, request.tenantId)
            ?: return Result.failure(ClientNotFound())

        // 3. Create invoice in local database
        val invoice = invoiceRepository.create(request)

        // 4. Log to audit service (fire-and-forget)
        launch {
            auditApi.logEvent(
                tenantId = request.tenantId,
                action = AuditAction.InvoiceCreated,
                entityType = EntityType.Invoice,
                entityId = invoice.id.toString(),
                serviceName = "invoicing"
            )
        }

        return Result.success(invoice)
    }
}
```

#### 2. Asynchronous Event Streaming

**Use when:** Real-time updates, analytics, non-critical operations

```kotlin
// Payment service emits events when payments recorded
@Rpc
interface PaymentApi {
    fun watchPayments(tenantId: TenantId): Flow<PaymentEvent>
}

// Reporting service consumes events to update analytics
class ReportingServiceImpl(
    private val paymentApi: PaymentApi
) {
    fun startPaymentMonitoring() {
        scope.launch {
            paymentApi.watchPayments(tenantId)
                .collect { event ->
                    when (event) {
                        is PaymentEvent.PaymentRecorded -> {
                            updateFinancialSnapshot(event.payment)
                        }
                    }
                }
        }
    }
}
```

#### 3. Service Composition Pattern

**Use when:** Need data from multiple services

```kotlin
// Reporting service aggregates data from multiple services
class ReportingServiceImpl(
    private val invoiceApi: InvoiceApi,
    private val expenseApi: ExpenseApi,
    private val paymentApi: PaymentApi
) : ReportingApi {

    override suspend fun getFinancialSummary(
        tenantId: TenantId,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<FinancialSummary> = coroutineScope {

        // Parallel RPC calls to multiple services
        val invoicesDeferred = async {
            invoiceApi.listInvoices(tenantId, fromDate = startDate, toDate = endDate)
        }
        val expensesDeferred = async {
            expenseApi.listExpenses(tenantId, fromDate = startDate, toDate = endDate)
        }
        val paymentsDeferred = async {
            paymentApi.listPayments(tenantId, fromDate = startDate, toDate = endDate)
        }

        // Aggregate results
        val invoices = invoicesDeferred.await().getOrElse { emptyList() }
        val expenses = expensesDeferred.await().getOrElse { emptyList() }
        val payments = paymentsDeferred.await().getOrElse { emptyList() }

        val totalRevenue = invoices.sumOf { it.totalAmount.amount }
        val totalExpenses = expenses.sumOf { it.amount.amount }
        val netProfit = totalRevenue - totalExpenses

        Result.success(FinancialSummary(
            tenantId = tenantId.value.toString(),
            period = DateRange(startDate, endDate),
            totalRevenue = Money(totalRevenue),
            totalExpenses = Money(totalExpenses),
            netProfit = Money(netProfit),
            invoiceCount = invoices.size,
            expenseCount = expenses.size,
            paymentCount = payments.size,
            outstandingAmount = calculateOutstanding(invoices, payments)
        ))
    }
}
```

### RPC Error Handling

```kotlin
// Retry policy for transient failures
class RpcRetryPolicy {
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Duration = 100.milliseconds,
        maxDelay: Duration = 1.seconds,
        factor: Double = 2.0,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            val result = block()
            if (result.isSuccess) return result

            delay(currentDelay)
            currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
        }
        return block()
    }
}

// Usage
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
    return retryPolicy.withRetry {
        tenantApi.getTenant(request.tenantId)
    }.mapCatching { tenant ->
        invoiceRepository.create(request)
    }
}
```

---

## Data Consistency Strategies

### 1. Eventual Consistency with Event Sourcing

**Pattern:** Services publish events, other services consume and update local state

```kotlin
// Invoice service publishes events
sealed class InvoiceEvent {
    data class InvoiceCreated(val invoice: Invoice) : InvoiceEvent()
    data class InvoiceStatusChanged(val invoiceId: InvoiceId, val status: InvoiceStatus) : InvoiceEvent()
}

// Payment service updates invoice status
class PaymentServiceImpl(
    private val invoiceApi: InvoiceApi
) {
    suspend fun recordPayment(request: RecordPaymentRequest): Result<Payment> {
        // 1. Create payment locally
        val payment = paymentRepository.create(request)

        // 2. Update invoice in Invoice service
        val updatedStatus = calculateInvoiceStatus(request.invoiceId, payment.amount)
        invoiceApi.updateInvoiceStatus(request.invoiceId, updatedStatus)

        // 3. Emit event
        emitEvent(PaymentEvent.PaymentRecorded(payment))

        return Result.success(payment)
    }
}
```

### 2. Saga Pattern for Distributed Transactions

**Pattern:** Coordinate multi-step operations across services with compensating transactions

```kotlin
// Example: Deleting a tenant (cascade across all services)
class TenantDeletionSaga(
    private val authService: AuthService,
    private val invoiceService: InvoiceService,
    private val expenseService: ExpenseService,
    private val paymentService: PaymentService,
    private val auditService: AuditService
) {
    suspend fun deleteTenant(tenantId: TenantId): Result<Unit> = runCatching {
        val steps = mutableListOf<suspend () -> Unit>()

        // Step 1: Delete invoices
        invoiceService.deleteAllForTenant(tenantId).onSuccess {
            steps.add { invoiceService.restoreTenant(tenantId) }
        }.onFailure {
            rollback(steps)
            throw it
        }

        // Step 2: Delete expenses
        expenseService.deleteAllForTenant(tenantId).onSuccess {
            steps.add { expenseService.restoreTenant(tenantId) }
        }.onFailure {
            rollback(steps)
            throw it
        }

        // Step 3: Delete payments
        paymentService.deleteAllForTenant(tenantId).onSuccess {
            steps.add { paymentService.restoreTenant(tenantId) }
        }.onFailure {
            rollback(steps)
            throw it
        }

        // Step 4: Delete tenant
        authService.deleteTenant(tenantId).onFailure {
            rollback(steps)
            throw it
        }

        // Step 5: Log deletion
        auditService.logCompliance(
            tenantId = tenantId,
            eventType = "tenant_deletion",
            details = "GDPR data deletion completed"
        )
    }

    private suspend fun rollback(steps: List<suspend () -> Unit>) {
        steps.reversed().forEach { compensate ->
            runCatching { compensate() }
        }
    }
}
```

### 3. Read Replicas for Local Caching

**Pattern:** Cache frequently-accessed data from other services

```kotlin
// Invoice service caches client names for display
object InvoiceClientCacheTable : UUIDTable("invoice_client_cache") {
    val clientId = uuid("client_id").uniqueIndex()
    val clientName = varchar("client_name", 255)
    val lastUpdated = datetime("last_updated")
}

// Update cache when client name changes (event listener)
class ClientEventHandler {
    suspend fun onClientUpdated(event: ClientEvent.ClientUpdated) {
        dbQuery {
            InvoiceClientCacheTable.upsert {
                it[clientId] = event.client.id
                it[clientName] = event.client.name
                it[lastUpdated] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
    }
}
```

---

## Shared Data Patterns

### Pattern 1: No Shared Tables (Recommended)

**Decision:** Each service owns its data exclusively

**Implementation:**
- Tenants table lives ONLY in auth_db
- Other services store `tenantId` as UUID (no FK)
- Validation via RPC calls

**Advantages:**
- True service independence
- Can move services to different databases/clouds
- Clear ownership boundaries

**Disadvantages:**
- No database-enforced referential integrity
- Must validate via RPC
- Potential data inconsistency

### Pattern 2: Reference Data Replication (Alternative)

**Decision:** Replicate read-only reference data to each service

**Implementation:**

```kotlin
// Each service has a TenantCacheTable
object TenantCacheTable : UUIDTable("tenant_cache") {
    val tenantId = uuid("tenant_id").uniqueIndex()
    val name = varchar("name", 255)
    val status = varchar("status", 50)
    val lastSynced = datetime("last_synced")
}

// Background job syncs from Auth service
class TenantCacheSyncJob(
    private val tenantApi: TenantApi
) {
    @Scheduled(fixedRate = 5.minutes)
    suspend fun syncTenants() {
        val tenants = tenantApi.listAllTenants().getOrElse { return }

        dbQuery {
            tenants.forEach { tenant ->
                TenantCacheTable.upsert {
                    it[tenantId] = tenant.id.value
                    it[name] = tenant.name
                    it[status] = tenant.status.name
                    it[lastSynced] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                }
            }
        }
    }
}
```

**Advantages:**
- Faster reads (no RPC needed)
- Works during Auth service downtime
- Can join with local tables

**Disadvantages:**
- Data duplication
- Sync lag (eventual consistency)
- Storage overhead

### Dokus Recommendation: Pattern 1 (No Shared Tables)

**Rationale:**
- Services should be independently deployable
- Reference data (tenants) changes infrequently
- RPC validation is acceptable overhead
- Simpler to reason about

---

## Referential Integrity

### Problem: No Cross-Database Foreign Keys

PostgreSQL cannot enforce FK constraints across databases.

### Solution: Application-Level Enforcement

#### 1. Validation Before Insert

```kotlin
// Invoice service validates tenant and client before creating invoice
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
    // Validate tenant exists
    val tenant = tenantApi.getTenant(request.tenantId).getOrElse {
        return Result.failure(ValidationError("Tenant does not exist"))
    }

    // Validate client belongs to tenant
    val client = clientRepository.findById(request.clientId, request.tenantId)
        ?: return Result.failure(ValidationError("Client not found"))

    // Now safe to create invoice
    return invoiceRepository.create(request)
}
```

#### 2. Soft Deletes Instead of Hard Deletes

```kotlin
// Don't physically delete tenants - mark as deleted
suspend fun deleteTenant(tenantId: TenantId): Result<Unit> {
    return dbQuery {
        TenantsTable.update({ TenantsTable.id eq tenantId.value }) {
            it[status] = TenantStatus.Deleted
            it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }
    }.toResult()
}

// Invoices remain intact but tenant is "deleted"
// Can be purged later via background job
```

#### 3. Orphan Cleanup Jobs

```kotlin
// Background job to clean up orphaned records
@Scheduled(fixedRate = 1.hour)
suspend fun cleanupOrphanedInvoices() {
    val invoices = dbQuery {
        InvoicesTable.selectAll().map { it[InvoicesTable.tenantId] }
    }

    val orphans = invoices.filter { tenantId ->
        tenantApi.getTenant(TenantId(tenantId)).isFailure
    }

    if (orphans.isNotEmpty()) {
        logger.w { "Found ${orphans.size} orphaned invoices" }
        // Mark for manual review or auto-archive
    }
}
```

#### 4. Event-Driven Cascade Deletes

```kotlin
// Auth service publishes event when tenant deleted
sealed class TenantEvent {
    data class TenantDeleted(val tenantId: TenantId) : TenantEvent()
}

// Other services listen and cascade delete
class InvoiceEventHandler {
    suspend fun onTenantDeleted(event: TenantEvent.TenantDeleted) {
        // Delete all invoices for this tenant
        invoiceRepository.deleteAllForTenant(event.tenantId)

        // Log cascade delete
        auditApi.logEvent(
            action = AuditAction.CascadeDelete,
            entityType = EntityType.Invoice,
            details = "Deleted all invoices for tenant ${event.tenantId}"
        )
    }
}
```

---

## Audit Logging Strategy

### Centralized vs Per-Service Audit Logs

**Decision:** Centralized Audit Service

**Rationale:**
- Single source of truth for compliance
- Easier to query across all services
- Simplified retention policy management
- Better security (audit DB can have stricter access controls)

### Implementation

```kotlin
// Shared audit logging client (used by all services)
class AuditClient(
    private val auditApi: AuditApi
) {
    suspend fun log(
        tenantId: TenantId,
        userId: UserId?,
        action: AuditAction,
        entityType: EntityType,
        entityId: String,
        serviceName: String,
        oldValues: String? = null,
        newValues: String? = null
    ) {
        // Fire-and-forget to avoid blocking main operation
        scope.launch {
            auditApi.logEvent(
                tenantId = tenantId,
                userId = userId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                serviceName = serviceName,
                oldValues = oldValues,
                newValues = newValues,
                ipAddress = getClientIp(),
                userAgent = getUserAgent()
            )
        }
    }
}

// Usage in Invoice service
suspend fun updateInvoice(id: InvoiceId, updates: UpdateInvoiceRequest): Result<Invoice> {
    val old = invoiceRepository.findById(id) ?: return Result.failure(NotFound())
    val updated = invoiceRepository.update(id, updates)

    auditClient.log(
        tenantId = old.tenantId,
        userId = getCurrentUserId(),
        action = AuditAction.InvoiceUpdated,
        entityType = EntityType.Invoice,
        entityId = id.value.toString(),
        serviceName = "invoicing",
        oldValues = Json.encodeToString(old),
        newValues = Json.encodeToString(updated)
    )

    return Result.success(updated)
}
```

### Audit Log Querying

```kotlin
// Audit service provides cross-service audit trail
@Rpc
interface AuditApi {
    suspend fun getAuditTrail(
        tenantId: TenantId,
        entityType: EntityType? = null,
        entityId: String? = null,
        serviceName: String? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 100
    ): Result<List<AuditLog>>

    suspend fun getEntityHistory(
        entityType: EntityType,
        entityId: String
    ): Result<List<AuditLog>>
}
```

---

## Migration Management

### Strategy: Service-Specific Flyway Migrations

Each service manages its own migrations independently.

### Directory Structure

```
features/
├── auth/
│   └── backend/
│       └── src/main/resources/db/migration/
│           ├── V1__create_tenants.sql
│           ├── V2__create_users.sql
│           └── V3__create_refresh_tokens.sql
│
├── invoicing/
│   └── backend/
│       └── src/main/resources/db/migration/
│           ├── V1__create_clients.sql
│           ├── V2__create_invoices.sql
│           └── V3__create_invoice_items.sql
│
├── expense/
│   └── backend/
│       └── src/main/resources/db/migration/
│           ├── V1__create_expenses.sql
│           └── V2__add_attachments.sql
│
└── (other services...)
```

### Migration Best Practices

#### 1. Versioning Scheme

```
V{service_major}_{migration_number}__{description}.sql

Examples:
V1_001__create_tenants.sql
V1_002__add_email_verification.sql
V2_001__add_mfa_support.sql
```

#### 2. Schema Change Patterns

```sql
-- ✅ GOOD: Backward compatible change (add nullable column)
ALTER TABLE invoices ADD COLUMN peppol_id VARCHAR(255) NULL;

-- ⚠️ CAREFUL: Add non-null column (requires default or backfill)
ALTER TABLE invoices ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

-- ❌ AVOID: Breaking change (drop column)
-- Instead: Deprecate → Mark as unused → Drop in next major version
ALTER TABLE invoices DROP COLUMN old_field;
```

#### 3. Data Migrations

```sql
-- Separate schema from data migrations
-- V1_005__migrate_invoice_totals.sql
DO $$
DECLARE
    invoice_row RECORD;
BEGIN
    FOR invoice_row IN SELECT id FROM invoices WHERE paid_amount IS NULL LOOP
        UPDATE invoices
        SET paid_amount = 0.00
        WHERE id = invoice_row.id;
    END LOOP;
END $$;
```

#### 4. Cross-Service Coordination

**Problem:** Invoice service adds `clientName` field, but needs data from separate Client service

**Solution:**

```kotlin
// Migration #1: Add column (nullable initially)
ALTER TABLE invoices ADD COLUMN client_name_cache VARCHAR(255) NULL;

// Migration #2: Backfill via application code
class InvoiceClientCacheBackfillJob {
    @PostConstruct
    suspend fun backfillClientNames() {
        val invoices = dbQuery {
            InvoicesTable.select { InvoicesTable.clientNameCache.isNull() }
                .limit(1000)
        }

        invoices.forEach { invoice ->
            val client = clientApi.getClient(invoice.clientId).getOrNull()
            client?.let {
                dbQuery {
                    InvoicesTable.update({ InvoicesTable.id eq invoice.id }) {
                        it[clientNameCache] = client.name
                    }
                }
            }
        }
    }
}

// Migration #3 (later): Make NOT NULL
ALTER TABLE invoices ALTER COLUMN client_name_cache SET NOT NULL;
```

### Rolling Deployments

```
1. Deploy new code (handles both old and new schema)
2. Run migrations (schema changes)
3. Verify health checks
4. Deploy next service
```

**Zero-Downtime Pattern:**

```kotlin
// Code supports both old and new schema
fun readInvoice(row: ResultRow): Invoice {
    val clientName = row.getOrNull(InvoicesTable.clientNameCache)
        ?: fetchClientNameViaRPC(row[InvoicesTable.clientId])

    return Invoice(
        clientName = clientName,
        // ... other fields
    )
}
```

---

## Distributed Transactions

### Problem Statement

**Scenario:** User creates invoice AND records payment in single operation

**Challenge:** Invoice service and Payment service have separate databases - no 2PC (two-phase commit)

### Solution 1: Saga Pattern (Recommended)

```kotlin
class CreateInvoiceWithPaymentSaga(
    private val invoiceApi: InvoiceApi,
    private val paymentApi: PaymentApi,
    private val auditApi: AuditApi
) {
    suspend fun execute(request: CreateInvoiceWithPaymentRequest): Result<InvoiceWithPayment> {
        var invoiceId: InvoiceId? = null
        var paymentId: PaymentId? = null

        try {
            // Step 1: Create invoice
            val invoice = invoiceApi.createInvoice(request.invoiceRequest)
                .getOrElse { throw it }
            invoiceId = invoice.id

            // Step 2: Record payment
            val payment = paymentApi.recordPayment(
                RecordPaymentRequest(
                    invoiceId = invoiceId,
                    amount = request.paymentAmount,
                    paymentDate = request.paymentDate,
                    paymentMethod = request.paymentMethod
                )
            ).getOrElse {
                // Compensate: Delete invoice
                invoiceApi.deleteInvoice(invoiceId)
                throw it
            }
            paymentId = payment.id

            // Step 3: Update invoice status
            invoiceApi.updateInvoiceStatus(invoiceId, InvoiceStatus.Paid)
                .getOrElse {
                    // Compensate: Delete payment and invoice
                    paymentApi.deletePayment(paymentId)
                    invoiceApi.deleteInvoice(invoiceId)
                    throw it
                }

            return Result.success(InvoiceWithPayment(invoice, payment))

        } catch (e: Exception) {
            auditApi.logEvent(
                action = AuditAction.SagaFailed,
                details = "CreateInvoiceWithPayment failed: ${e.message}",
                metadata = mapOf(
                    "invoiceId" to invoiceId?.toString(),
                    "paymentId" to paymentId?.toString()
                )
            )
            return Result.failure(e)
        }
    }
}
```

### Solution 2: Outbox Pattern

**Pattern:** Write to local DB + outbox table, then publish events asynchronously

```kotlin
// Invoice service database
object OutboxTable : UUIDTable("outbox") {
    val eventType = varchar("event_type", 100)
    val aggregateId = uuid("aggregate_id")
    val payload = text("payload")
    val published = bool("published").default(false)
    val publishedAt = datetime("published_at").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, published)
    }
}

// Write to outbox atomically with main operation
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> = transaction {
    // Insert invoice
    val invoice = InvoicesTable.insertAndGetId { /* ... */ }

    // Insert outbox event
    OutboxTable.insert {
        it[eventType] = "InvoiceCreated"
        it[aggregateId] = invoice.value
        it[payload] = Json.encodeToString(invoice)
    }

    invoice
}

// Background job publishes events
@Scheduled(fixedRate = 5.seconds)
suspend fun publishOutboxEvents() {
    val events = dbQuery {
        OutboxTable.select { OutboxTable.published eq false }
            .limit(100)
    }

    events.forEach { event ->
        // Publish to event bus / call other services
        eventPublisher.publish(event)

        // Mark as published
        dbQuery {
            OutboxTable.update({ OutboxTable.id eq event.id }) {
                it[published] = true
                it[publishedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
    }
}
```

### Solution 3: Idempotency Keys

**Pattern:** Make operations idempotent to allow safe retries

```kotlin
// Payment service
object IdempotencyKeysTable : UUIDTable("idempotency_keys") {
    val idempotencyKey = varchar("idempotency_key", 255).uniqueIndex()
    val response = text("response") // Cached response
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

suspend fun recordPayment(
    request: RecordPaymentRequest,
    idempotencyKey: String
): Result<Payment> {
    // Check if already processed
    val cached = dbQuery {
        IdempotencyKeysTable.select {
            IdempotencyKeysTable.idempotencyKey eq idempotencyKey
        }.singleOrNull()
    }

    if (cached != null) {
        // Return cached response
        return Result.success(Json.decodeFromString(cached[IdempotencyKeysTable.response]))
    }

    // Process payment
    val payment = paymentRepository.create(request)

    // Cache result
    dbQuery {
        IdempotencyKeysTable.insert {
            it[this.idempotencyKey] = idempotencyKey
            it[response] = Json.encodeToString(payment)
            it[expiresAt] = Clock.System.now().plus(24.hours).toLocalDateTime(TimeZone.UTC)
        }
    }

    return Result.success(payment)
}
```

---

## Performance Optimization

### 1. Database Connection Pooling

**HikariCP Configuration per Service:**

```kotlin
// application.conf (each service)
database {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/invoicing_db"
    user = "dokus_invoicing"
    password = ${DB_PASSWORD}

    hikari {
        maximumPoolSize = 10        // Max connections
        minimumIdle = 2             // Keep warm connections
        connectionTimeout = 30000   // 30 seconds
        idleTimeout = 600000        // 10 minutes
        maxLifetime = 1800000       // 30 minutes
    }
}
```

### 2. Query Optimization

```kotlin
// ✅ GOOD: Use indexes effectively
suspend fun getOverdueInvoices(tenantId: TenantId): List<Invoice> = dbQuery {
    InvoicesTable.select {
        (InvoicesTable.tenantId eq tenantId.value) and
        (InvoicesTable.status eq InvoiceStatus.Sent) and
        (InvoicesTable.dueDate less Clock.System.today())
    }
    .limit(100)
    .map { it.toInvoice() }
}
// Uses index: (tenant_id, status, due_date)

// ❌ BAD: No index, full table scan
suspend fun getAllInvoices(): List<Invoice> = dbQuery {
    InvoicesTable.selectAll().map { it.toInvoice() }
}
```

### 3. Batch Operations

```kotlin
// ✅ GOOD: Batch insert
suspend fun createInvoiceItems(items: List<InvoiceItemData>) = dbQuery {
    InvoiceItemsTable.batchInsert(items) { item ->
        this[InvoiceItemsTable.invoiceId] = item.invoiceId
        this[InvoiceItemsTable.description] = item.description
        this[InvoiceItemsTable.quantity] = item.quantity
        this[InvoiceItemsTable.unitPrice] = item.unitPrice
    }
}

// ❌ BAD: N+1 queries
items.forEach { item ->
    InvoiceItemsTable.insert { /* ... */ }
}
```

### 4. Caching Strategies

```kotlin
// Service-level caching with Caffeine
class TenantCacheService(
    private val tenantApi: TenantApi
) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(5.minutes)
        .maximumSize(1000)
        .buildSuspending<TenantId, Tenant> { tenantId ->
            tenantApi.getTenant(tenantId).getOrThrow()
        }

    suspend fun getTenant(tenantId: TenantId): Tenant {
        return cache.get(tenantId)
    }
}
```

### 5. Read Replicas for Reporting

```kotlin
// Reporting service uses read replica
database {
    master {
        url = "jdbc:postgresql://master.db:5432/reporting_db"
    }
    replica {
        url = "jdbc:postgresql://replica.db:5432/reporting_db"
    }
}

// Route heavy queries to replica
suspend fun generateReport(tenantId: TenantId): Report = dbQuery(useReplica = true) {
    // Complex aggregation query on replica
    FinancialSnapshotsTable.select { /* ... */ }
}
```

---

## Best Practices

### 1. Always Filter by tenant_id

```kotlin
// ✅ Every query MUST include tenant filter
fun getInvoices(tenantId: TenantId) = dbQuery {
    InvoicesTable.select { InvoicesTable.tenantId eq tenantId.value }
}

// ❌ NEVER query without tenant filter (security vulnerability!)
fun getInvoices() = dbQuery {
    InvoicesTable.selectAll()
}
```

### 2. Validate Cross-Service References

```kotlin
// Before creating invoice, validate client exists
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
    val client = clientRepository.findById(request.clientId, request.tenantId)
        ?: return Result.failure(ClientNotFound("Client ${request.clientId} not found"))

    return invoiceRepository.create(request)
}
```

### 3. Use Transactions for Multi-Step Operations

```kotlin
// Atomic: Create invoice + items + audit log
suspend fun createInvoiceWithItems(request: CreateInvoiceRequest) = transaction {
    val invoiceId = InvoicesTable.insertAndGetId { /* ... */ }.value

    InvoiceItemsTable.batchInsert(request.items) { /* ... */ }

    AuditLogsTable.insert { /* ... */ }

    invoiceId
}
```

### 4. Handle RPC Failures Gracefully

```kotlin
// Don't fail entire operation if audit logging fails
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
    val invoice = invoiceRepository.create(request)

    // Fire-and-forget audit log (don't block on failure)
    scope.launch {
        runCatching {
            auditApi.logEvent(/* ... */)
        }.onFailure {
            logger.w(it) { "Failed to log audit event" }
        }
    }

    return Result.success(invoice)
}
```

### 5. Monitor Cross-Service Call Latency

```kotlin
// Add metrics to RPC calls
class InstrumentedInvoiceApi(
    private val delegate: InvoiceApi,
    private val metrics: Metrics
) : InvoiceApi {

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
        return measureTimedValue {
            delegate.createInvoice(request)
        }.also { (result, duration) ->
            metrics.recordRpcLatency("invoice.create", duration)
            if (result.isFailure) {
                metrics.incrementRpcErrors("invoice.create")
            }
        }.value
    }
}
```

### 6. Implement Circuit Breakers

```kotlin
// Prevent cascade failures
class CircuitBreakerRpcClient(
    private val delegate: InvoiceApi
) : InvoiceApi {
    private val circuitBreaker = CircuitBreaker.of(
        "invoice-api",
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build()
    )

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
        return runCatching {
            circuitBreaker.executeSuspendFunction {
                delegate.createInvoice(request).getOrThrow()
            }
        }
    }
}
```

### 7. Version RPC Interfaces

```kotlin
// Support multiple API versions during transition
@Rpc
interface InvoiceApiV1 {
    suspend fun createInvoice(request: CreateInvoiceRequestV1): Result<Invoice>
}

@Rpc
interface InvoiceApiV2 {
    suspend fun createInvoice(request: CreateInvoiceRequestV2): Result<Invoice>
}

// V2 adds new fields while V1 continues to work
@Serializable
data class CreateInvoiceRequestV2(
    // V1 fields
    val tenantId: TenantId,
    val clientId: ClientId,
    val items: List<InvoiceItem>,

    // V2 additions
    val peppolId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
```

---

## Summary

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Database Strategy** | Database-per-Service | Service independence, scalability |
| **Communication** | KotlinX RPC | Type-safe, coroutine-native |
| **Consistency** | Eventual Consistency | Necessary trade-off for microservices |
| **Referential Integrity** | Application-Level | No cross-DB foreign keys |
| **Audit Logging** | Centralized Service | Single source of truth for compliance |
| **Transactions** | Saga Pattern | Compensating transactions for multi-service operations |
| **Multi-Tenancy** | Row-Level Isolation | Every table has tenant_id |
| **Migrations** | Service-Specific Flyway | Each service manages its own schema |

### Migration Path

**Phase 1 (MVP):** Monolithic database (faster development)
**Phase 2 (Current):** Database-per-service (this architecture)
**Phase 3 (Future):** Sharding by tenant (if > 10,000 tenants)

### Performance Targets

- RPC call latency: < 50ms (p95)
- Database query time: < 100ms (p95)
- Cross-service query: < 200ms (p95)
- Report generation: < 1s (cached), < 5s (fresh)

---

**Related Documentation:**
- [Database Schema](./DATABASE.md) - Detailed schema reference
- [Security](./SECURITY.md) - Multi-tenant security
- [API Reference](./API.md) - RPC interface documentation

---

**Last Updated:** November 2025
