# Database Schema - Complete Reference

**Last Updated:** October 2025  
**Database:** PostgreSQL 15+  
**ORM:** Exposed 0.44+  
**Status:** Production Design

---

## Navigation

🏠 [[Documentation-Index|Back to Index]]  
🔌 [[KotlinX-RPC-Integration|RPC Integration Guide]]  
🏗️ [[02-Technical-Architecture|Technical Architecture]]  
📋 [[Database-Best-Practices|Database Best Practices]]

---

## Table of Contents

1. [[#Overview]]
2. [[#Design Principles]]
3. [[#Schema Hierarchy]]
4. [[#Table Definitions]]
5. [[#Exposed ORM Implementation]]
6. [[#SQL Migration Scripts]]
7. [[#Usage Examples]]
8. [[#Indexing Strategy]]

---

## Overview

This document provides the complete database schema design for Dokus. The schema is designed for a **multi-tenant SaaS application** where each tenant (freelancer/company) has isolated data within a shared database.

### Key Characteristics

- **Multi-Tenancy:** Shared database, shared schema with `tenant_id` discriminator
- **Financial Precision:** NUMERIC types for all monetary values
- **Audit Compliance:** Complete audit trail for all operations
- **Type Safety:** UUID primary keys, proper foreign key relationships
- **Scalability:** Optimized indexes for common query patterns

### Technology Stack

```yaml
Database: PostgreSQL 15+
ORM: JetBrains Exposed 0.44+
Migration Tool: Flyway
Connection Pool: HikariCP
Communication: KotlinX RPC
```

---

## Design Principles

### 1. Multi-Tenancy with Row-Level Security

Every table (except lookup tables) includes a `tenant_id` column. This provides:

**Advantages:**
- ✅ Simple to implement and maintain
- ✅ Cost-effective (single database)
- ✅ Easy to backup and restore
- ✅ Good for MVP and early growth

**Critical Security Requirement:**
```kotlin
// ✅ CORRECT - Always filter by tenant_id
fun getInvoice(id: UUID, tenantId: UUID) = transaction {
    Invoices.select {
        (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
    }
}

// ❌ WRONG - Security vulnerability!
fun getInvoice(id: UUID) = transaction {
    Invoices.select { Invoices.id eq id }
}
```

**Migration Path:**
- 0-1,000 tenants: Shared database ✅ (MVP)
- 1,000-10,000 tenants: Add read replicas
- 10,000+ tenants: Consider database-per-tenant

### 2. Financial Data Precision

**Always use NUMERIC, never FLOAT/REAL:**

```sql
-- ✅ CORRECT - Exact decimal precision
total_amount NUMERIC(12, 2)  -- Up to €9,999,999,999.99

-- ❌ WRONG - Introduces rounding errors
total_amount FLOAT  -- Can lose cents in calculations!
```

**Why NUMERIC(12, 2)?**
- `12` total digits: Handles invoices up to 9.9 billion euros
- `2` decimal places: Cent precision for financial calculations
- No rounding errors in calculations
- Required for tax compliance

**Exposed Implementation:**
```kotlin
val totalAmount = decimal("total_amount", 12, 2)
```

### 3. Immutable Audit Logging

**Requirements:**
- Log ALL financial operations (create, update, delete)
- Store 7 years minimum (GDPR + Belgian law)
- Never update or delete audit logs
- Include: who, what, when, from where

**Audit Log Structure:**
```kotlin
object AuditLogs : UUIDTable("audit_logs") {
    val tenantId = reference("tenant_id", Tenants)
    val userId = reference("user_id", Users).nullable()
    val action = varchar("action", 100)        // "invoice.created"
    val entityType = varchar("entity_type", 50) // "invoice"
    val entityId = uuid("entity_id")
    val oldValues = text("old_values").nullable() // JSON before
    val newValues = text("new_values").nullable() // JSON after
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
```

### 4. Type Safety with UUIDs

**Benefits:**
- Non-sequential (security)
- Globally unique (no collisions)
- Can be generated client-side
- No database round-trip needed

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
- **1:Many** - Tenant has many Users, Clients, Invoices, Expenses
- **1:1** - Tenant has one TenantSettings
- **Many:Many** - BankTransactions can link to Invoices OR Expenses (reconciliation)

---

## Table Definitions

### Authentication & Multi-Tenancy

#### Tenants

The root entity representing each customer account.

**Purpose:** One tenant = one paying customer (freelancer or company)

**Fields:**
```kotlin
object Tenants : UUIDTable("tenants") {
    // Identity
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    
    // Subscription
    val plan = varchar("plan", 50)      // 'free', 'professional', 'business'
    val status = varchar("status", 50).default("active")
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()
    
    // Localization
    val country = varchar("country", 2).default("BE")  // ISO 3166-1 alpha-2
    val language = varchar("language", 5).default("en") // 'en', 'nl', 'fr'
    
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

People who can access a tenant's account.

**Purpose:** Support team members, accountants, viewers

**Fields:**
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
        index(false, tenantId, isActive)  // Composite for active users query
    }
}
```

**Role Permissions:**
- `owner` - Full access, billing, settings
- `member` - Create/edit invoices and expenses
- `accountant` - View-only financial data
- `viewer` - Dashboard only

#### RefreshTokens

JWT refresh token management for authentication.

**Purpose:** Enable long-lived sessions with short-lived access tokens

**Fields:**
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

**Purpose:** The freelancer's clients/customers

**Fields:**
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
    
    // Additional
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)
    
    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, name)      // Client search
        index(false, tenantId, isActive)  // Active clients list
    }
}
```

#### Invoices

Core financial documents sent to clients.

**Purpose:** Billable documents with Peppol e-invoicing support

**Fields:**
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
    val paidAmount = decimal("paid_amount", 12, 2).default(java.math.BigDecimal.ZERO)
    
    // Status
    val status = varchar("status", 50)
    // Values: 'draft', 'sent', 'viewed', 'paid', 'overdue', 'cancelled'
    
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
        index(false, tenantId, status, dueDate)  // Dashboard query
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

Line items on an invoice.

**Purpose:** Individual products/services with pricing and VAT

**Fields:**
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

Business costs tracked for tax deductions.

**Purpose:** Track deductible business expenses with receipts

**Fields:**
```kotlin
object Expenses : UUIDTable("expenses") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    
    val date = date("date")
    val merchant = varchar("merchant", 255)
    val amount = decimal("amount", 12, 2)
    val vatAmount = decimal("vat_amount", 12, 2).nullable()
    val vatRate = decimal("vat_rate", 5, 2).nullable()
    
    val category = varchar("category", 100)
    // Values: 'software', 'hardware', 'travel', 'office', 'meals', 'marketing', 'other'
    
    val description = text("description").nullable()
    
    // Receipt storage
    val receiptUrl = varchar("receipt_url", 500).nullable()      // S3 path
    val receiptFilename = varchar("receipt_filename", 255).nullable()
    
    // Tax deductibility
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2)
        .default(java.math.BigDecimal("100.00"))
    
    val paymentMethod = varchar("payment_method", 50).nullable()
    val isRecurring = bool("is_recurring").default(false)
    val notes = text("notes").nullable()
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, category)
        index(false, tenantId, date)      // Date range queries
        index(false, tenantId, category)  // Category filtering
    }
}
```

**Deductibility Rules (Belgium):**
- Software/Hardware: 100% deductible
- Meals: 69% deductible
- Car: Depends on CO2 emissions
- Travel: 100% deductible

#### Payments

Payment transactions against invoices.

**Purpose:** Track when and how invoices are paid

**Fields:**
```kotlin
object Payments : UUIDTable("payments") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    val invoiceId = reference("invoice_id", Invoices, onDelete = ReferenceOption.RESTRICT)
    
    val amount = decimal("amount", 12, 2)
    val paymentDate = date("payment_date")
    val paymentMethod = varchar("payment_method", 50)
    // Values: 'bank_transfer', 'stripe', 'mollie', 'cash', 'cheque'
    
    val transactionId = varchar("transaction_id", 255).nullable()  // External ID
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, invoiceId)
        index(false, paymentDate)
        index(false, tenantId, paymentDate)
    }
}
```

---

### Bank Integration

#### BankConnections

Connected bank accounts via Plaid/Tink/Nordigen.

**Purpose:** Enable automatic transaction import

**Fields:**
```kotlin
object BankConnections : UUIDTable("bank_connections") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    
    // Provider
    val provider = varchar("provider", 50)  // 'plaid', 'tink', 'nordigen'
    val institutionId = varchar("institution_id", 100)
    val institutionName = varchar("institution_name", 255)
    
    // Account
    val accountId = varchar("account_id", 255)
    val accountName = varchar("account_name", 255).nullable()
    val accountType = varchar("account_type", 50).nullable()
    val currency = varchar("currency", 3).default("EUR")
    
    // CRITICAL: Must be encrypted at rest
    val accessToken = text("access_token")  // AES-256-GCM encrypted
    
    val lastSyncedAt = datetime("last_synced_at").nullable()
    val isActive = bool("is_active").default(true)
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, isActive)
    }
}
```

**Security Note:**
```kotlin
// Encrypt before storing
val encrypted = AES256.encrypt(accessToken, encryptionKey)
BankConnections.insert {
    it[accessToken] = encrypted
}

// Decrypt after retrieving
val decrypted = AES256.decrypt(row[BankConnections.accessToken], encryptionKey)
```

#### BankTransactions

Imported bank transactions.

**Purpose:** Reconcile with invoices and expenses

**Fields:**
```kotlin
object BankTransactions : UUIDTable("bank_transactions") {
    val bankConnectionId = reference("bank_connection_id", BankConnections, 
        onDelete = ReferenceOption.CASCADE)
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    
    val externalId = varchar("external_id", 255).uniqueIndex()  // Bank's ID
    val date = date("date")
    val amount = decimal("amount", 12, 2)
    val description = varchar("description", 500)
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()
    val isPending = bool("is_pending").default(false)
    
    // Reconciliation
    val expenseId = reference("expense_id", Expenses, 
        onDelete = ReferenceOption.SET_NULL).nullable()
    val invoiceId = reference("invoice_id", Invoices, 
        onDelete = ReferenceOption.SET_NULL).nullable()
    val isReconciled = bool("is_reconciled").default(false)
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, isReconciled)
        index(false, tenantId, isReconciled)
        index(false, tenantId, date)
    }
}
```

**Reconciliation Logic:**
- Match by amount and date
- Link to expense OR invoice (not both)
- Mark as reconciled when matched
- Show unreconciled transactions on dashboard

---

### Tax & Compliance

#### VatReturns

Quarterly VAT filing records.

**Purpose:** Track VAT submissions to tax authorities

**Fields:**
```kotlin
object VatReturns : UUIDTable("vat_returns") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    
    val quarter = integer("quarter")  // 1, 2, 3, 4
    val year = integer("year")
    
    // VAT calculations
    val salesVat = decimal("sales_vat", 12, 2)      // Collected from invoices
    val purchaseVat = decimal("purchase_vat", 12, 2) // Paid on expenses
    val netVat = decimal("net_vat", 12, 2)          // To pay or reclaim
    
    val status = varchar("status", 50)  // 'draft', 'filed', 'paid'
    val filedAt = datetime("filed_at").nullable()
    val paidAt = datetime("paid_at").nullable()
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, year, quarter)
        index(false, tenantId)
        index(false, year, quarter)
    }
}
```

**Belgian VAT Rules:**
- Filed quarterly (due 20th of month after quarter)
- Q1: April 20, Q2: July 20, Q3: October 20, Q4: January 20
- Net VAT = Sales VAT - Purchase VAT
- If negative, you get a refund

#### AuditLogs

Immutable audit trail for compliance.

**Purpose:** GDPR compliance, financial auditing, debugging

**Fields:**
```kotlin
object AuditLogs : UUIDTable("audit_logs") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    
    val action = varchar("action", 100)
    // Format: "entity.action" e.g., "invoice.created", "expense.updated"
    
    val entityType = varchar("entity_type", 50)  // "invoice", "expense", "payment"
    val entityId = uuid("entity_id")
    
    // Change tracking (JSON)
    val oldValues = text("old_values").nullable()  // Before state
    val newValues = text("new_values").nullable()  // After state
    
    // Request context
    val ipAddress = varchar("ip_address", 45).nullable()  // IPv4/IPv6
    val userAgent = text("user_agent").nullable()
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, entityType, entityId)  // Entity history
        index(false, createdAt)             // Time-based queries
        index(false, userId)                // User activity
        index(false, action)                // Action filtering
    }
}
```

**Example Audit Log:**
```json
{
  "action": "invoice.status_changed",
  "entityType": "invoice",
  "entityId": "123e4567-e89b-12d3-a456-426614174000",
  "oldValues": "{\"status\": \"draft\"}",
  "newValues": "{\"status\": \"sent\", \"sent_at\": \"2025-10-05T14:30:00Z\"}",
  "userId": "user-uuid-here",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0..."
}
```

---

### Configuration

#### TenantSettings

Tenant-specific preferences and defaults.

**Purpose:** Per-tenant configuration

**Fields:**
```kotlin
object TenantSettings : UUIDTable("tenant_settings") {
    val tenantId = reference("tenant_id", Tenants, 
        onDelete = ReferenceOption.CASCADE).uniqueIndex()
    
    // Invoice defaults
    val invoicePrefix = varchar("invoice_prefix", 20).default("INV")
    val nextInvoiceNumber = integer("next_invoice_number").default(1)
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2)
        .default(java.math.BigDecimal("21.00"))
    
    // Company info (for invoices)
    val companyName = varchar("company_name", 255).nullable()
    val companyAddress = text("company_address").nullable()
    val companyVatNumber = varchar("company_vat_number", 50).nullable()
    val companyIban = varchar("company_iban", 34).nullable()
    val companyBic = varchar("company_bic", 11).nullable()
    val companyLogoUrl = varchar("company_logo_url", 500).nullable()
    
    // Notifications
    val emailInvoiceReminders = bool("email_invoice_reminders").default(true)
    val emailPaymentConfirmations = bool("email_payment_confirmations").default(true)
    val emailWeeklyReports = bool("email_weekly_reports").default(false)
    
    // Feature flags
    val enableBankSync = bool("enable_bank_sync").default(false)
    val enablePeppol = bool("enable_peppol").default(false)
    
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
```

**Auto-increment Invoice Numbers:**
```kotlin
// When creating invoice
val settings = TenantSettings.select { ... }.single()
val number = settings[nextInvoiceNumber]
val invoiceNumber = "${settings[invoicePrefix]}-${number.toString().padStart(4, '0')}"
// Result: "INV-0001", "INV-0002", etc.

// Then increment
TenantSettings.update { 
    it[nextInvoiceNumber] = nextInvoiceNumber + 1 
}
```

#### Attachments

File uploads metadata.

**Purpose:** Track receipts, invoices, documents

**Fields:**
```kotlin
object Attachments : UUIDTable("attachments") {
    val tenantId = reference("tenant_id", Tenants, onDelete = ReferenceOption.CASCADE)
    
    // Entity linking
    val entityType = varchar("entity_type", 50)  // 'invoice', 'expense', 'client'
    val entityId = uuid("entity_id")
    
    // File metadata
    val filename = varchar("filename", 255)
    val mimeType = varchar("mime_type", 100)
    val sizeBytes = long("size_bytes")
    
    // Storage (S3/MinIO/etc)
    val s3Key = varchar("s3_key", 500)
    val s3Bucket = varchar("s3_bucket", 255)
    
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, entityType, entityId)
    }
}
```

---

## Exposed ORM Implementation

### Helper Objects

```kotlin
/**
 * Current timestamp for default values
 */
object CurrentDateTime : Expression<LocalDateTime>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("CURRENT_TIMESTAMP")
    }
}
```

**Usage:**
```kotlin
val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
```

### Complete Table List

```kotlin
/**
 * All tables for schema creation and migrations
 */
val allTables = arrayOf(
    Tenants,
    Users,
    RefreshTokens,
    Clients,
    Invoices,
    InvoiceItems,
    Expenses,
    Payments,
    BankConnections,
    BankTransactions,
    VatReturns,
    AuditLogs,
    TenantSettings,
    Attachments
)
```

### Database Factory

```kotlin
package com.dokus.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DATABASE_URL") 
                ?: "jdbc:postgresql://localhost:5432/dokus"
            driverClassName = "org.postgresql.Driver"
            username = System.getenv("DATABASE_USER") ?: "dokus"
            password = System.getenv("DATABASE_PASSWORD") ?: "dokus_dev"
            
            // Connection pool settings
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 600000       // 10 minutes
            connectionTimeout = 30000  // 30 seconds
            maxLifetime = 1800000      // 30 minutes
            
            // Transaction settings
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            validate()
        }
        
        Database.connect(HikariDataSource(config))
        
        // Development only: create tables
        if (System.getenv("ENV") == "development") {
            transaction {
                SchemaUtils.create(*allTables)
            }
        }
    }
}
```

---

## SQL Migration Scripts

### V1__initial_schema.sql

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tenants
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    plan VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'active' NOT NULL,
    trial_ends_at TIMESTAMP,
    subscription_started_at TIMESTAMP,
    country VARCHAR(2) DEFAULT 'BE' NOT NULL,
    language VARCHAR(5) DEFAULT 'en' NOT NULL,
    vat_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_tenants_email ON tenants(email);
CREATE INDEX idx_tenants_status ON tenants(status);

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    mfa_secret VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_active ON users(tenant_id, is_active);

-- Continue with other tables...
```

*(Full migration script available in repository)*

---

## Usage Examples

### Creating an Invoice

```kotlin
suspend fun createInvoice(
    tenantId: UUID,
    clientId: UUID,
    items: List<InvoiceItemData>
): UUID = transaction {
    // Get settings
    val settings = TenantSettings
        .select { TenantSettings.tenantId eq tenantId }
        .single()
    
    val invoiceNumber = "${settings[TenantSettings.invoicePrefix]}-" +
        "${settings[TenantSettings.nextInvoiceNumber].toString().padStart(4, '0')}"
    
    // Calculate totals
    val subtotal = items.sumOf { it.lineTotal }
    val vatTotal = items.sumOf { it.vatAmount }
    val total = subtotal + vatTotal
    
    // Insert invoice
    val invoiceId = Invoices.insertAndGetId {
        it[Invoices.tenantId] = tenantId
        it[Invoices.clientId] = clientId
        it[Invoices.invoiceNumber] = invoiceNumber
        it[issueDate] = LocalDate.now()
        it[dueDate] = LocalDate.now().plusDays(30)
        it[subtotalAmount] = subtotal
        it[vatAmount] = vatTotal
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
            it[InvoiceItems.vatAmount] = item.vatAmount
            it[sortOrder] = index
        }
    }
    
    // Increment counter
    TenantSettings.update({ TenantSettings.tenantId eq tenantId }) {
        with(SqlExpressionBuilder) {
            it[nextInvoiceNumber] = nextInvoiceNumber + 1
        }
    }
    
    // Audit log
    AuditLogs.insert {
        it[AuditLogs.tenantId] = tenantId
        it[action] = "invoice.created"
        it[entityType] = "invoice"
        it[entityId] = invoiceId
        it[newValues] = """{"invoice_number": "$invoiceNumber", "total": "$total"}"""
    }
    
    invoiceId
}
```

### Querying Invoices

```kotlin
suspend fun getInvoices(
    tenantId: UUID,
    status: String? = null,
    fromDate: LocalDate? = null
): List<InvoiceData> = transaction {
    var query = Invoices
        .innerJoin(Clients, { Invoices.clientId }, { Clients.id })
        .select { Invoices.tenantId eq tenantId }
    
    // Apply filters
    if (status != null) {
        query = query.andWhere { Invoices.status eq status }
    }
    if (fromDate != null) {
        query = query.andWhere { Invoices.issueDate greaterEq fromDate }
    }
    
    query
        .orderBy(Invoices.issueDate to SortOrder.DESC)
        .map { row ->
            InvoiceData(
                id = row[Invoices.id].value,
                clientName = row[Clients.name],
                invoiceNumber = row[Invoices.invoiceNumber],
                issueDate = row[Invoices.issueDate],
                dueDate = row[Invoices.dueDate],
                totalAmount = row[Invoices.totalAmount],
                status = row[Invoices.status]
            )
        }
}
```

### Recording Payment

```kotlin
suspend fun recordPayment(
    invoiceId: UUID,
    tenantId: UUID,
    amount: BigDecimal,
    paymentDate: LocalDate,
    method: String,
    userId: UUID
): UUID = transaction {
    // Verify invoice belongs to tenant
    val invoice = Invoices
        .select { (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId) }
        .singleOrNull() ?: throw NotFoundException("Invoice not found")
    
    // Record payment
    val paymentId = Payments.insertAndGetId {
        it[Payments.tenantId] = tenantId
        it[Payments.invoiceId] = invoiceId
        it[Payments.amount] = amount
        it[Payments.paymentDate] = paymentDate
        it[paymentMethod] = method
    }.value
    
    // Update invoice
    val newPaidAmount = invoice[Invoices.paidAmount] + amount
    val newStatus = if (newPaidAmount >= invoice[Invoices.totalAmount]) "paid" else "partially_paid"
    
    Invoices.update({ Invoices.id eq invoiceId }) {
        it[paidAmount] = newPaidAmount
        it[status] = newStatus
        if (newStatus == "paid") {
            it[paidAt] = LocalDateTime.now()
        }
    }
    
    // Audit log
    AuditLogs.insert {
        it[AuditLogs.tenantId] = tenantId
        it[AuditLogs.userId] = userId
        it[action] = "payment.recorded"
        it[entityType] = "payment"
        it[entityId] = paymentId
        it[newValues] = """{"amount": "$amount", "invoice_id": "$invoiceId"}"""
    }
    
    paymentId
}
```

---

## Indexing Strategy

### Primary Indexes

Every table has:
1. **Primary key index** (automatic with UUID)
2. **tenant_id index** (for tenant isolation)

### Composite Indexes for Common Queries

```kotlin
// Dashboard: Get overdue invoices for tenant
index(false, tenantId, status, dueDate)

// Invoice search by client
index(false, tenantId, clientId, issueDate)

// Expense reports by category
index(false, tenantId, category, date)

// User activity tracking
index(false, userId, isActive)
```

### Unique Indexes for Business Rules

```kotlin
// Invoice numbers unique per tenant
uniqueIndex(tenantId, invoiceNumber)

// VAT returns: one per quarter per tenant
uniqueIndex(tenantId, year, quarter)

// User email globally unique
uniqueIndex(email)

// Bank transaction IDs unique
uniqueIndex(externalId)
```

---

## Next Steps

1. **Review** this schema design
2. **Implement** DatabaseFactory with connection pooling
3. **Create** Flyway migrations from SQL scripts
4. **Read** [[KotlinX-RPC-Integration|KotlinX RPC Integration Guide]]
5. **Build** repository layer following patterns in guide
6. **Test** with in-memory H2 database
7. **Deploy** to PostgreSQL

---

## See Also

- [[KotlinX-RPC-Integration|KotlinX RPC Integration]] - How to use this schema with RPC services
- [[Database-Best-Practices|Database Best Practices]] - Security, performance, optimization
- [[02-Technical-Architecture|Technical Architecture]] - Overall system design
- [[12-First-90-Days|Implementation Timeline]] - When to build what

---

**Questions?** Check the [[Documentation-Index|Documentation Index]] for more guides.
