# KotlinX RPC Integration Guide

**Last Updated:** October 2025  
**Status:** Architecture Design  
**Framework:** KotlinX RPC 0.9.1+

---

## Navigation

🏠 [[Documentation-Index|Back to Index]]  
💾 [[Database-Schema|Database Schema Reference]]  
🏗️ [[02-Technical-Architecture|Technical Architecture]]  
✅ [[Database-Best-Practices|Database Best Practices]]

---

## Table of Contents

1. [[#Overview]]
2. [[#Why KotlinX RPC]]
3. [[#Architecture]]
4. [[#Project Structure]]
5. [[#Service Interfaces]]
6. [[#DTOs Design]]
7. [[#Repository Pattern]]
8. [[#Service Implementation]]
9. [[#Server Setup]]
10. [[#Client Usage]]
11. [[#Best Practices]]
12. [[#Testing]]

---

## Overview

This guide explains how to integrate **KotlinX RPC** with the **Exposed ORM** database layer for Dokus microservices. KotlinX RPC provides type-safe remote procedure calls using pure Kotlin constructs, eliminating the need for REST boilerplate or gRPC proto files.

### What You'll Learn

- ✅ How to design RPC service interfaces
- ✅ Repository pattern for database access with tenant isolation
- ✅ DTO design for external APIs vs internal entities
- ✅ Server setup with Ktor + KotlinX RPC
- ✅ Client implementation for web and mobile
- ✅ Real-time updates with Flows
- ✅ Testing strategies

---

## Why KotlinX RPC?

### vs REST API

| Feature | REST | KotlinX RPC |
|---------|------|-------------|
| **Type Safety** | Manual DTOs, runtime errors | Compile-time verification |
| **Client Code** | Manual HTTP client | Auto-generated from interface |
| **Streaming** | SSE/WebSockets (complex) | Flow (native) |
| **Multiplatform** | Separate clients per platform | Shared interface |
| **Boilerplate** | High (routes, serialization, etc) | Minimal |

### REST Example

```kotlin
// Server
app.post("/api/invoices") {
    val request = call.receive<CreateInvoiceRequest>()
    // validation, auth, etc
    val invoice = service.create(request)
    call.respond(HttpStatusCode.Created, invoice)
}

// Client (manual)
val response = httpClient.post("/api/invoices") {
    contentType(ContentType.Application.Json)
    setBody(request)
}
val invoice = response.body<InvoiceDTO>()
```

### KotlinX RPC Example

```kotlin
// Shared interface
@Rpc
interface InvoiceService {
    suspend fun createInvoice(request: CreateInvoiceRequest): InvoiceDTO
}

// Server implementation
class InvoiceServiceImpl : InvoiceService {
    override suspend fun createInvoice(request: CreateInvoiceRequest): InvoiceDTO {
        // implementation
    }
}

// Client (automatic!)
val invoice = invoiceService.createInvoice(request)
```

**Winner:** KotlinX RPC for Kotlin-first projects ✨

### Why Perfect for Dokus?

1. **Kotlin Multiplatform** - Share code between backend, web, iOS, Android
2. **Type Safety** - Catch errors at compile time, not runtime
3. **Less Code** - 70% less boilerplate than REST
4. **Real-time** - Flow-based streaming for live updates
5. **Solo-Founder Friendly** - One person can maintain full stack

---

## Architecture

### Communication Flow

```
┌─────────────────────────────────────────────┐
│           CLIENT APPLICATIONS                │
├──────────────┬──────────────┬───────────────┤
│   Web (JS)   │  iOS (Swift) │ Android (Kt)  │
└──────┬───────┴──────┬───────┴───────┬───────┘
       │              │               │
       └──────────────┼───────────────┘
                      │ KotlinX RPC
            ┌─────────▼──────────┐
            │   Ktor Server      │
            │  (API Gateway)     │
            └─────────┬──────────┘
                      │
         ┌────────────┼────────────┐
         │            │            │
    ┌────▼───┐  ┌────▼────┐  ┌───▼────┐
    │Invoice │  │Expense  │  │  Auth  │
    │Service │  │Service  │  │Service │
    └────┬───┘  └────┬────┘  └───┬────┘
         │           │            │
         └───────────┼────────────┘
                     │
          ┌──────────▼──────────┐
          │   Repository Layer   │
          │   (Database Access)  │
          └──────────┬──────────┘
                     │
          ┌──────────▼──────────┐
          │   PostgreSQL + Exposed│
          └─────────────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────┐
│  RPC Interface (shared/)             │  ← Shared between client & server
│  - Service interfaces                │
│  - DTOs (Data Transfer Objects)     │
└─────────────────────────────────────┘
                 │
┌─────────────────────────────────────┐
│  Service Implementation (backend/)   │  ← Server-side only
│  - Business logic                    │
│  - Validation                        │
│  - Authorization                     │
└─────────────────────────────────────┘
                 │
┌─────────────────────────────────────┐
│  Repository Layer (backend/)         │  ← Database access
│  - CRUD operations                   │
│  - Tenant isolation                  │
│  - Transaction management            │
└─────────────────────────────────────┘
                 │
┌─────────────────────────────────────┐
│  Database (PostgreSQL + Exposed)     │
│  - Table definitions                 │
│  - Indexes                           │
│  - Constraints                       │
└─────────────────────────────────────┘
```

---

## Project Structure

```
dokus/
├── shared/                              # Shared between all platforms
│   └── src/commonMain/kotlin/
│       ├── com/dokus/api/              # RPC Service Interfaces
│       │   ├── InvoiceService.kt
│       │   ├── ExpenseService.kt
│       │   ├── ClientService.kt
│       │   └── AuthService.kt
│       └── com/dokus/dto/              # Data Transfer Objects
│           ├── InvoiceDTO.kt
│           ├── ExpenseDTO.kt
│           ├── ClientDTO.kt
│           └── Common.kt
│
├── backend/
│   ├── services/
│   │   ├── invoice-service/           # Invoice microservice
│   │   │   └── src/main/kotlin/
│   │   │       ├── InvoiceServiceImpl.kt    # RPC implementation
│   │   │       ├── InvoiceRepository.kt      # Database access
│   │   │       └── Application.kt            # Ktor setup
│   │   │
│   │   ├── expense-service/           # Expense microservice
│   │   └── auth-service/              # Authentication microservice
│   │
│   └── shared/                         # Backend shared code
│       └── src/main/kotlin/
│           ├── com/dokus/database/
│           │   ├── schema/
│           │   │   └── DatabaseSchemas.kt    # Exposed tables
│           │   ├── DatabaseFactory.kt        # Connection setup
│           │   └── repositories/
│           │       └── BaseRepository.kt     # Common patterns
│           └── com/dokus/auth/
│               └── AuthContext.kt            # JWT utilities
│
└── frontend/
    ├── web/                            # Kotlin/JS web app
    ├── android/                        # Android app
    └── ios/                            # iOS app (Swift + shared Kotlin)
```

---

## Service Interfaces

### Defining RPC Interfaces

Location: `shared/src/commonMain/kotlin/com/dokus/api/`

```kotlin
package com.dokus.api

import com.dokus.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Contextual
import java.util.UUID

/**
 * Invoice RPC Service Interface
 * 
 * This interface is shared between server and all clients.
 * Changes here automatically propagate everywhere.
 * 
 * Best Practices:
 * - Use suspend functions for async operations
 * - Use Flow for streaming/real-time updates
 * - Always validate on server, never trust client
 * - Include tenant context in every call (from auth)
 */
@Rpc
interface InvoiceService {
    
    /**
     * Create a new invoice
     * 
     * Tenant ID extracted from authentication context.
     * 
     * @param request Invoice data with items
     * @return Created invoice with generated ID and number
     * @throws ValidationException if data is invalid
     * @throws UnauthorizedException if not authenticated
     */
    suspend fun createInvoice(
        request: CreateInvoiceRequest
    ): InvoiceDTO
    
    /**
     * Get all invoices for authenticated tenant
     * 
     * @param filters Optional filters (status, date range, etc)
     * @return List of invoices (newest first)
     */
    suspend fun getInvoices(
        filters: InvoiceFiltersDTO? = null
    ): List<InvoiceDTO>
    
    /**
     * Get single invoice with all details
     * 
     * @param invoiceId Invoice UUID
     * @return Invoice with line items and client details
     * @throws NotFoundException if invoice doesn't exist
     * @throws UnauthorizedException if wrong tenant
     */
    suspend fun getInvoice(
        invoiceId: UUID
    ): InvoiceWithItemsDTO
    
    /**
     * Update invoice (only if status is 'draft')
     * 
     * @param invoiceId Invoice to update
     * @param request Update data
     * @return Updated invoice
     * @throws ValidationException if invoice is not draft
     */
    suspend fun updateInvoice(
        invoiceId: UUID,
        request: UpdateInvoiceRequest
    ): InvoiceDTO
    
    /**
     * Send invoice via email and/or Peppol
     * 
     * Changes status from 'draft' to 'sent'.
     * 
     * @param invoiceId Invoice to send
     * @param sendEmail Include email to client
     * @param sendPeppol Send via Peppol network
     * @return Updated invoice
     */
    suspend fun sendInvoice(
        invoiceId: UUID,
        sendEmail: Boolean = true,
        sendPeppol: Boolean = false
    ): InvoiceDTO
    
    /**
     * Delete invoice (only if status is 'draft')
     * 
     * @param invoiceId Invoice to delete
     * @throws ValidationException if invoice is not draft
     */
    suspend fun deleteInvoice(invoiceId: UUID)
    
    /**
     * Real-time invoice updates (server-side streaming)
     * 
     * Emits events when invoices are created, updated, or paid.
     * Client can show live notifications.
     * 
     * NOTE: Flow functions must be non-suspending and return Flow as top-level type
     * 
     * @return Flow of invoice update events
     */
    fun watchInvoices(): Flow<InvoiceUpdateEvent>
    
    /**
     * Generate PDF for invoice
     * 
     * @param invoiceId Invoice to render
     * @return Base64-encoded PDF bytes
     */
    suspend fun generatePdf(invoiceId: UUID): String
    
    /**
     * Calculate VAT for a quarter
     * 
     * @param year Year
     * @param quarter Quarter (1-4)
     * @return VAT calculation with itemized breakdown
     */
    suspend fun calculateVat(
        year: Int,
        quarter: Int
    ): VatCalculationDTO
}
```

### More Service Examples

```kotlin
@Rpc
interface ExpenseService {
    suspend fun createExpense(request: CreateExpenseRequest): ExpenseDTO
    suspend fun getExpenses(filters: ExpenseFiltersDTO? = null): List<ExpenseDTO>
    suspend fun updateExpense(expenseId: UUID, request: UpdateExpenseRequest): ExpenseDTO
    suspend fun deleteExpense(expenseId: UUID)
    suspend fun uploadReceipt(expenseId: UUID, file: ByteArray, filename: String): String
}

@Rpc
interface ClientService {
    suspend fun createClient(request: CreateClientRequest): ClientDTO
    suspend fun getClients(activeOnly: Boolean = true): List<ClientDTO>
    suspend fun getClient(clientId: UUID): ClientDTO
    suspend fun updateClient(clientId: UUID, request: UpdateClientRequest): ClientDTO
    suspend fun deleteClient(clientId: UUID)
}

@Rpc
interface AuthService {
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun refreshToken(refreshToken: String): AuthResponse
    suspend fun logout()
    suspend fun resetPassword(email: String)
}
```

---

## DTOs Design

### Data Transfer Objects

Location: `shared/src/commonMain/kotlin/com/dokus/dto/`

```kotlin
package com.dokus.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Invoice DTO - External API representation
 * 
 * Differences from database entity:
 * - No internal fields (created_at timestamps, etc)
 * - Denormalized for convenience (clientName)
 * - Computed fields (daysOverdue)
 * - Client-friendly types (strings for dates)
 */
@Serializable
data class InvoiceDTO(
    val id: UUID,
    val invoiceNumber: String,
    
    // Client info (denormalized)
    val clientId: UUID,
    val clientName: String,
    
    // Dates
    val issueDate: String,  // ISO 8601: "2025-10-05"
    val dueDate: String,
    
    // Amounts
    val subtotalAmount: BigDecimal,
    val vatAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    
    // Status
    val status: InvoiceStatus,
    val paymentLink: String? = null,
    
    // Computed fields
    val daysOverdue: Int? = null,  // Null if not overdue
    val isPaid: Boolean,
    val isOverdue: Boolean,
    
    // Metadata
    val currency: String = "EUR",
    val createdAt: String,
    val updatedAt: String
)

@Serializable
enum class InvoiceStatus {
    DRAFT,
    SENT,
    VIEWED,
    PAID,
    OVERDUE,
    CANCELLED
}

@Serializable
data class InvoiceItemDTO(
    val id: UUID,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal,
    val lineTotal: BigDecimal,
    val vatAmount: BigDecimal,
    val sortOrder: Int
)

/**
 * Full invoice with items and client
 */
@Serializable
data class InvoiceWithItemsDTO(
    val invoice: InvoiceDTO,
    val items: List<InvoiceItemDTO>,
    val client: ClientSummaryDTO
)

/**
 * Request to create invoice
 */
@Serializable
data class CreateInvoiceRequest(
    val clientId: UUID,
    val issueDate: String,      // ISO 8601
    val dueDate: String,
    val items: List<CreateInvoiceItemRequest>,
    val notes: String? = null,
    val termsAndConditions: String? = null
)

@Serializable
data class CreateInvoiceItemRequest(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal    // e.g., 21.00 for 21%
)

@Serializable
data class UpdateInvoiceRequest(
    val issueDate: String? = null,
    val dueDate: String? = null,
    val items: List<CreateInvoiceItemRequest>? = null,
    val notes: String? = null,
    val termsAndConditions: String? = null
)

/**
 * Filters for invoice queries
 */
@Serializable
data class InvoiceFiltersDTO(
    val status: InvoiceStatus? = null,
    val clientId: UUID? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val searchQuery: String? = null
)

/**
 * Real-time update event
 */
@Serializable
data class InvoiceUpdateEvent(
    val eventType: InvoiceEventType,
    val invoice: InvoiceDTO
)

@Serializable
enum class InvoiceEventType {
    CREATED,
    UPDATED,
    SENT,
    PAID,
    OVERDUE,
    CANCELLED
}

/**
 * Client summary for invoice details
 */
@Serializable
data class ClientSummaryDTO(
    val id: UUID,
    val name: String,
    val email: String?,
    val vatNumber: String?,
    val address: String?
)

/**
 * VAT calculation result
 */
@Serializable
data class VatCalculationDTO(
    val year: Int,
    val quarter: Int,
    val salesVat: BigDecimal,        // Collected from invoices
    val purchaseVat: BigDecimal,     // Paid on expenses
    val netVat: BigDecimal,          // To pay or reclaim
    val breakdown: List<VatItemDTO>
)

@Serializable
data class VatItemDTO(
    val description: String,
    val amount: BigDecimal,
    val vatRate: BigDecimal,
    val vatAmount: BigDecimal
)
```

### DTO Best Practices

**DO:**
- ✅ Use descriptive names
- ✅ Include validation in requests
- ✅ Denormalize for convenience
- ✅ Use enums for status fields
- ✅ Add computed fields (isPaid, daysOverdue)
- ✅ Use ISO 8601 strings for dates (easy to serialize)

**DON'T:**
- ❌ Expose database IDs or internal structure
- ❌ Include sensitive data (password hashes, tokens)
- ❌ Use nullable fields without defaults
- ❌ Return database entities directly

---

## Repository Pattern

### Base Repository

Location: `backend/shared/src/main/kotlin/com/dokus/database/repositories/`

```kotlin
package com.dokus.database.repositories

import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Base repository with common patterns
 */
abstract class BaseRepository {
    
    /**
     * Execute in transaction
     */
    protected suspend fun <T> dbQuery(block: () -> T): T = transaction {
        block()
    }
    
    /**
     * Validate tenant access
     */
    protected fun validateTenantAccess(
        entityTenantId: UUID,
        requestTenantId: UUID
    ) {
        if (entityTenantId != requestTenantId) {
            throw UnauthorizedException("Access denied to this resource")
        }
    }
}

class UnauthorizedException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message)
```

### Invoice Repository

```kotlin
package com.dokus.invoice

import com.dokus.database.repositories.*
import com.dokus.database.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Invoice Repository - Database access layer
 * 
 * CRITICAL SECURITY RULE:
 * Every query MUST filter by tenantId
 */
class InvoiceRepository : BaseRepository() {
    
    /**
     * Create invoice with items (atomic transaction)
     * 
     * Steps:
     * 1. Get next invoice number
     * 2. Insert invoice
     * 3. Insert all items
     * 4. Increment counter
     * 5. Create audit log
     * 
     * All in one transaction - rollback if any step fails!
     */
    suspend fun create(
        tenantId: UUID,
        clientId: UUID,
        issueDate: LocalDate,
        dueDate: LocalDate,
        items: List<InvoiceItemData>,
        notes: String? = null,
        termsAndConditions: String? = null
    ): UUID = dbQuery {
        // Validate
        require(items.isNotEmpty()) { "Invoice must have at least one item" }
        require(!dueDate.isBefore(issueDate)) { "Due date cannot be before issue date" }
        
        // Calculate totals
        val subtotal = items.sumOf { it.lineTotal }
        val vatTotal = items.sumOf { it.vatAmount }
        val total = subtotal + vatTotal
        
        // Get tenant settings
        val settings = TenantSettings
            .select { TenantSettings.tenantId eq tenantId }
            .singleOrNull() ?: throw NotFoundException("Tenant settings not found")
        
        val invoiceNumber = "${settings[TenantSettings.invoicePrefix]}-" +
            settings[TenantSettings.nextInvoiceNumber].toString().padStart(4, '0')
        
        // Insert invoice
        val invoiceId = Invoices.insertAndGetId {
            it[Invoices.tenantId] = tenantId
            it[Invoices.clientId] = clientId
            it[Invoices.invoiceNumber] = invoiceNumber
            it[Invoices.issueDate] = issueDate
            it[Invoices.dueDate] = dueDate
            it[subtotalAmount] = subtotal
            it[vatAmount] = vatTotal
            it[totalAmount] = total
            it[status] = "draft"
            it[Invoices.notes] = notes
            it[Invoices.termsAndConditions] = termsAndConditions
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
    
    /**
     * Get all invoices for tenant with optional filtering
     * 
     * SECURITY: ALWAYS filters by tenantId first
     */
    suspend fun findByTenant(
        tenantId: UUID,
        status: String? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): List<InvoiceEntity> = dbQuery {
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
        if (toDate != null) {
            query = query.andWhere { Invoices.issueDate lessEq toDate }
        }
        
        query
            .orderBy(Invoices.issueDate to SortOrder.DESC)
            .map { row -> mapToEntity(row) }
    }
    
    /**
     * Get invoice by ID with tenant validation
     * 
     * SECURITY: Verifies invoice belongs to tenant
     */
    suspend fun findById(
        invoiceId: UUID,
        tenantId: UUID
    ): InvoiceEntity? = dbQuery {
        Invoices
            .innerJoin(Clients, { Invoices.clientId }, { Clients.id })
            .select {
                (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
            }
            .singleOrNull()
            ?.let { mapToEntity(it) }
    }
    
    /**
     * Get invoice with all items
     */
    suspend fun findWithItems(
        invoiceId: UUID,
        tenantId: UUID
    ): InvoiceWithItems? = dbQuery {
        val invoice = findById(invoiceId, tenantId) ?: return@dbQuery null
        
        val items = InvoiceItems
            .select { InvoiceItems.invoiceId eq invoiceId }
            .orderBy(InvoiceItems.sortOrder)
            .map { row ->
                InvoiceItemEntity(
                    id = row[InvoiceItems.id].value,
                    description = row[InvoiceItems.description],
                    quantity = row[InvoiceItems.quantity],
                    unitPrice = row[InvoiceItems.unitPrice],
                    vatRate = row[InvoiceItems.vatRate],
                    lineTotal = row[InvoiceItems.lineTotal],
                    vatAmount = row[InvoiceItems.vatAmount],
                    sortOrder = row[InvoiceItems.sortOrder]
                )
            }
        
        InvoiceWithItems(invoice, items)
    }
    
    /**
     * Update invoice status
     */
    suspend fun updateStatus(
        invoiceId: UUID,
        tenantId: UUID,
        newStatus: String,
        userId: UUID?
    ): Boolean = dbQuery {
        val updated = Invoices.update({
            (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
        }) {
            it[status] = newStatus
            if (newStatus == "paid") {
                it[paidAt] = java.time.LocalDateTime.now()
                it[paidAmount] = Invoices.totalAmount
            }
        }
        
        if (updated > 0) {
            AuditLogs.insert {
                it[AuditLogs.tenantId] = tenantId
                it[AuditLogs.userId] = userId
                it[action] = "invoice.status_changed"
                it[entityType] = "invoice"
                it[entityId] = invoiceId
                it[newValues] = """{"status": "$newStatus"}"""
            }
        }
        
        updated > 0
    }
    
    /**
     * Delete invoice (only if draft)
     */
    suspend fun delete(
        invoiceId: UUID,
        tenantId: UUID,
        userId: UUID?
    ): Boolean = dbQuery {
        // Check status first
        val invoice = findById(invoiceId, tenantId) 
            ?: throw NotFoundException("Invoice not found")
        
        if (invoice.status != "draft") {
            throw ValidationException("Cannot delete non-draft invoice")
        }
        
        // Delete items first (cascade)
        InvoiceItems.deleteWhere {
            InvoiceItems.invoiceId eq invoiceId
        }
        
        // Delete invoice
        val deleted = Invoices.deleteWhere {
            (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
        }
        
        if (deleted > 0) {
            AuditLogs.insert {
                it[AuditLogs.tenantId] = tenantId
                it[AuditLogs.userId] = userId
                it[action] = "invoice.deleted"
                it[entityType] = "invoice"
                it[entityId] = invoiceId
            }
        }
        
        deleted > 0
    }
    
    // Private mapping functions
    
    private fun mapToEntity(row: ResultRow) = InvoiceEntity(
        id = row[Invoices.id].value,
        tenantId = row[Invoices.tenantId].value,
        clientId = row[Invoices.clientId].value,
        clientName = row[Clients.name],
        invoiceNumber = row[Invoices.invoiceNumber],
        issueDate = row[Invoices.issueDate],
        dueDate = row[Invoices.dueDate],
        subtotalAmount = row[Invoices.subtotalAmount],
        vatAmount = row[Invoices.vatAmount],
        totalAmount = row[Invoices.totalAmount],
        paidAmount = row[Invoices.paidAmount],
        status = row[Invoices.status],
        paymentLink = row[Invoices.paymentLink],
        createdAt = row[Invoices.createdAt],
        updatedAt = row[Invoices.updatedAt]
    )
}

// Domain entities (internal use only, not exposed via RPC)

data class InvoiceEntity(
    val id: UUID,
    val tenantId: UUID,
    val clientId: UUID,
    val clientName: String,
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: BigDecimal,
    val vatAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val status: String,
    val paymentLink: String?,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime
)

data class InvoiceItemEntity(
    val id: UUID,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal,
    val lineTotal: BigDecimal,
    val vatAmount: BigDecimal,
    val sortOrder: Int
)

data class InvoiceWithItems(
    val invoice: InvoiceEntity,
    val items: List<InvoiceItemEntity>
)

data class InvoiceItemData(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal,
    val lineTotal: BigDecimal,
    val vatAmount: BigDecimal
)
```

---

## Service Implementation

### Invoice Service Implementation

Location: `backend/services/invoice-service/src/main/kotlin/`

```kotlin
package com.dokus.invoice

import com.dokus.api.InvoiceService
import com.dokus.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.rpc.RpcServiceContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Invoice Service Implementation
 * 
 * Implements RPC interface with business logic.
 * All clients call these methods directly.
 */
class InvoiceServiceImpl(
    private val context: RpcServiceContext,
    private val repository: InvoiceRepository = InvoiceRepository()
) : InvoiceService {
    
    // Shared flow for real-time updates
    private val invoiceUpdates = MutableSharedFlow<InvoiceUpdateEvent>()
    
    override suspend fun createInvoice(
        request: CreateInvoiceRequest
    ): InvoiceDTO {
        // Get tenant from auth context
        val tenantId = context.getTenantId()
        
        // Validate request
        validateCreateRequest(request)
        
        // Calculate line items
        val items = request.items.map { item ->
            val lineTotal = item.quantity * item.unitPrice
            val vatAmount = lineTotal * (item.vatRate / BigDecimal(100))
            
            InvoiceItemData(
                description = item.description,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                vatRate = item.vatRate,
                lineTotal = lineTotal,
                vatAmount = vatAmount
            )
        }
        
        // Create in database
        val invoiceId = repository.create(
            tenantId = tenantId,
            clientId = request.clientId,
            issueDate = LocalDate.parse(request.issueDate),
            dueDate = LocalDate.parse(request.dueDate),
            items = items,
            notes = request.notes,
            termsAndConditions = request.termsAndConditions
        )
        
        // Fetch created invoice
        val created = repository.findById(invoiceId, tenantId)
            ?: throw IllegalStateException("Failed to create invoice")
        
        val dto = mapToDTO(created)
        
        // Emit real-time update
        invoiceUpdates.emit(InvoiceUpdateEvent(InvoiceEventType.CREATED, dto))
        
        return dto
    }
    
    override suspend fun getInvoices(
        filters: InvoiceFiltersDTO?
    ): List<InvoiceDTO> {
        val tenantId = context.getTenantId()
        
        val invoices = repository.findByTenant(
            tenantId = tenantId,
            status = filters?.status?.name?.lowercase(),
            fromDate = filters?.fromDate?.let { LocalDate.parse(it) },
            toDate = filters?.toDate?.let { LocalDate.parse(it) }
        )
        
        return invoices.map { mapToDTO(it) }
    }
    
    override suspend fun getInvoice(invoiceId: UUID): InvoiceWithItemsDTO {
        val tenantId = context.getTenantId()
        
        val invoiceWithItems = repository.findWithItems(invoiceId, tenantId)
            ?: throw NotFoundException("Invoice not found")
        
        return InvoiceWithItemsDTO(
            invoice = mapToDTO(invoiceWithItems.invoice),
            items = invoiceWithItems.items.map { mapItemToDTO(it) },
            client = getClientSummary(invoiceWithItems.invoice.clientId, tenantId)
        )
    }
    
    override suspend fun updateInvoice(
        invoiceId: UUID,
        request: UpdateInvoiceRequest
    ): InvoiceDTO {
        val tenantId = context.getTenantId()
        
        // TODO: Implement update logic
        // 1. Verify invoice is draft
        // 2. Update invoice fields
        // 3. Update items if provided
        // 4. Audit log
        // 5. Emit update event
        
        throw NotImplementedError("Update not yet implemented")
    }
    
    override suspend fun sendInvoice(
        invoiceId: UUID,
        sendEmail: Boolean,
        sendPeppol: Boolean
    ): InvoiceDTO {
        val tenantId = context.getTenantId()
        val userId = context.getUserId()
        
        // TODO: Send email and/or Peppol
        
        // Update status
        val updated = repository.updateStatus(invoiceId, tenantId, "sent", userId)
        if (!updated) {
            throw NotFoundException("Invoice not found")
        }
        
        val invoice = repository.findById(invoiceId, tenantId)!!
        val dto = mapToDTO(invoice)
        
        // Emit update
        invoiceUpdates.emit(InvoiceUpdateEvent(InvoiceEventType.SENT, dto))
        
        return dto
    }
    
    override suspend fun deleteInvoice(invoiceId: UUID) {
        val tenantId = context.getTenantId()
        val userId = context.getUserId()
        
        repository.delete(invoiceId, tenantId, userId)
    }
    
    override fun watchInvoices(): Flow<InvoiceUpdateEvent> {
        // Filter by tenant in Flow
        val tenantId = context.getTenantId()
        
        return invoiceUpdates
        // Could filter here: .filter { it.invoice.tenantId == tenantId }
    }
    
    override suspend fun generatePdf(invoiceId: UUID): String {
        val tenantId = context.getTenantId()
        
        val invoice = repository.findWithItems(invoiceId, tenantId)
            ?: throw NotFoundException("Invoice not found")
        
        // TODO: Generate PDF using iText or similar
        return "base64_pdf_content_here"
    }
    
    override suspend fun calculateVat(year: Int, quarter: Int): VatCalculationDTO {
        val tenantId = context.getTenantId()
        
        // TODO: Calculate VAT from invoices and expenses
        
        return VatCalculationDTO(
            year = year,
            quarter = quarter,
            salesVat = BigDecimal.ZERO,
            purchaseVat = BigDecimal.ZERO,
            netVat = BigDecimal.ZERO,
            breakdown = emptyList()
        )
    }
    
    // Helper functions
    
    private fun mapToDTO(entity: InvoiceEntity): InvoiceDTO {
        val today = LocalDate.now()
        val daysOverdue = if (entity.status == "overdue") {
            ChronoUnit.DAYS.between(entity.dueDate, today).toInt()
        } else null
        
        return InvoiceDTO(
            id = entity.id,
            invoiceNumber = entity.invoiceNumber,
            clientId = entity.clientId,
            clientName = entity.clientName,
            issueDate = entity.issueDate.toString(),
            dueDate = entity.dueDate.toString(),
            subtotalAmount = entity.subtotalAmount,
            vatAmount = entity.vatAmount,
            totalAmount = entity.totalAmount,
            paidAmount = entity.paidAmount,
            status = InvoiceStatus.valueOf(entity.status.uppercase()),
            paymentLink = entity.paymentLink,
            daysOverdue = daysOverdue,
            isPaid = entity.status == "paid",
            isOverdue = entity.status == "overdue",
            createdAt = entity.createdAt.toString(),
            updatedAt = entity.updatedAt.toString()
        )
    }
    
    private fun mapItemToDTO(entity: InvoiceItemEntity) = InvoiceItemDTO(
        id = entity.id,
        description = entity.description,
        quantity = entity.quantity,
        unitPrice = entity.unitPrice,
        vatRate = entity.vatRate,
        lineTotal = entity.lineTotal,
        vatAmount = entity.vatAmount,
        sortOrder = entity.sortOrder
    )
    
    private fun validateCreateRequest(request: CreateInvoiceRequest) {
        require(request.items.isNotEmpty()) {
            "Invoice must have at least one item"
        }
        
        val issueDate = LocalDate.parse(request.issueDate)
        val dueDate = LocalDate.parse(request.dueDate)
        
        require(!dueDate.isBefore(issueDate)) {
            "Due date cannot be before issue date"
        }
    }
    
    private suspend fun getClientSummary(
        clientId: UUID,
        tenantId: UUID
    ): ClientSummaryDTO {
        // TODO: Fetch from ClientRepository
        return ClientSummaryDTO(
            id = clientId,
            name = "Client Name",
            email = null,
            vatNumber = null,
            address = null
        )
    }
}

// Extension functions for auth context

fun RpcServiceContext.getTenantId(): UUID {
    return UUID.fromString(
        this.get("tenantId") ?: throw UnauthorizedException("Not authenticated")
    )
}

fun RpcServiceContext.getUserId(): UUID? {
    return this.get("userId")?.let { UUID.fromString(it) }
}

// Custom exceptions

class NotFoundException(message: String) : Exception(message)
class ValidationException(message: String) : Exception(message)
class UnauthorizedException(message: String) : Exception(message)
```

---

## Server Setup

### Ktor Application

Location: `backend/services/invoice-service/src/main/kotlin/Application.kt`

```kotlin
package com.dokus.invoice

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Install KotlinX RPC
    install(Krpc)
    
    routing {
        // Register RPC services at /rpc endpoint
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json {
                        prettyPrint = false
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                }
            }
            
            // Register services
            registerService<InvoiceService> { ctx ->
                InvoiceServiceImpl(ctx)
            }
            
            // Add more services
            // registerService<ExpenseService> { ctx -> ExpenseServiceImpl(ctx) }
            // registerService<ClientService> { ctx -> ClientServiceImpl(ctx) }
        }
    }
}

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlinx.rpc.plugin")
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    
    // KotlinX RPC
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.9.1")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Shared modules
    implementation(project(":shared"))
    implementation(project(":backend:shared"))
}
```

---

## Client Usage

### Web Client (Kotlin/JS)

```kotlin
package com.dokus.web

import com.dokus.api.InvoiceService
import com.dokus.dto.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.serialization.json.json

// Create HTTP client
val httpClient = HttpClient(Js) {
    installKrpc()
}

// Create RPC client
val invoiceService: InvoiceService = httpClient.rpc {
    url("${window.location.protocol}//${window.location.host}/rpc")
    
    rpcConfig {
        serialization {
            json()
        }
    }
}

// Usage in your app
fun main() {
    MainScope().launch {
        try {
            // Create invoice
            val invoice = invoiceService.createInvoice(
                CreateInvoiceRequest(
                    clientId = UUID.randomUUID(),
                    issueDate = "2025-10-05",
                    dueDate = "2025-11-04",
                    items = listOf(
                        CreateInvoiceItemRequest(
                            description = "Web Development",
                            quantity = BigDecimal("40"),
                            unitPrice = BigDecimal("75.00"),
                            vatRate = BigDecimal("21.00")
                        )
                    )
                )
            )
            
            console.log("Created invoice: ${invoice.invoiceNumber}")
            
            // Watch for updates
            watchInvoiceUpdates()
            
        } catch (e: Exception) {
            console.error("Error: ${e.message}")
        }
    }
}

fun CoroutineScope.watchInvoiceUpdates() {
    launch {
        invoiceService.watchInvoices().collect { event ->
            when (event.eventType) {
                InvoiceEventType.CREATED -> showNotification("New invoice created!")
                InvoiceEventType.PAID -> showNotification("Invoice paid!")
                else -> {}
            }
        }
    }
}

fun showNotification(message: String) {
    // Show browser notification or toast
    console.log("Notification: $message")
}
```

### Android Client (Kotlin/Android)

```kotlin
package com.dokus.android

import com.dokus.api.InvoiceService
import com.dokus.dto.*
import io.ktor.client.*
import io.ktor.client.engine.android.*
import kotlinx.coroutines.flow.collect
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.serialization.json.json

class InvoiceViewModel : ViewModel() {
    
    private val httpClient = HttpClient(Android) {
        installKrpc()
    }
    
    private val invoiceService: InvoiceService = httpClient.rpc {
        url("https://api.dokus.app/rpc")
        
        rpcConfig {
            serialization {
                json()
            }
        }
    }
    
    // State
    private val _invoices = MutableStateFlow<List<InvoiceDTO>>(emptyList())
    val invoices: StateFlow<List<InvoiceDTO>> = _invoices.asStateFlow()
    
    // Load invoices
    fun loadInvoices() {
        viewModelScope.launch {
            try {
                val result = invoiceService.getInvoices()
                _invoices.value = result
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Watch for updates
    init {
        viewModelScope.launch {
            invoiceService.watchInvoices().collect { event ->
                // Update UI
                loadInvoices()
            }
        }
    }
}
```

---

## Best Practices

### Security

**1. Always Validate Tenant Access**
```kotlin
// ❌ WRONG
suspend fun getInvoice(id: UUID) = repository.findById(id)

// ✅ CORRECT
suspend fun getInvoice(id: UUID): InvoiceDTO {
    val tenantId = context.getTenantId()
    val invoice = repository.findById(id, tenantId) 
        ?: throw NotFoundException()
    return mapToDTO(invoice)
}
```

**2. Never Trust Client Input**
```kotlin
// Always validate on server
fun validateCreateRequest(request: CreateInvoiceRequest) {
    require(request.items.isNotEmpty()) { "Must have items" }
    require(request.items.all { it.quantity > BigDecimal.ZERO }) {
        "Quantity must be positive"
    }
    // etc.
}
```

**3. Use Transactions**
```kotlin
// ✅ Atomic operations
suspend fun createInvoice(...) = transaction {
    val id = Invoices.insert { ... }
    InvoiceItems.batchInsert(items) { ... }
    AuditLogs.insert { ... }
    id  // All succeed or all fail
}
```

### Performance

**1. Avoid N+1 Queries**
```kotlin
// ❌ BAD - N+1 queries
fun getInvoices(tenantId: UUID): List<InvoiceDTO> {
    val invoices = Invoices.select { ... }.map { it }
    return invoices.map { invoice ->
        val client = Clients.select { ... }.single()  // N queries!
        mapToDTO(invoice, client)
    }
}

// ✅ GOOD - Single join
fun getInvoices(tenantId: UUID): List<InvoiceDTO> {
    return Invoices
        .innerJoin(Clients)
        .select { Invoices.tenantId eq tenantId }
        .map { row ->  // All data in one query
            mapToDTO(row)
        }
}
```

**2. Use Batch Operations**
```kotlin
// ✅ Batch insert
InvoiceItems.batchInsert(items) { item ->
    this[invoiceId] = id
    this[description] = item.description
}
```

**3. Index Foreign Keys**
```kotlin
object Invoices : UUIDTable("invoices") {
    // ...
    
    init {
        index(false, tenantId)           // Single column
        index(false, tenantId, status)   // Composite
        index(false, tenantId, status, dueDate)  // For dashboard queries
    }
}
```

---

## Testing

### Unit Test Repository

```kotlin
class InvoiceRepositoryTest {
    
    @Before
    fun setup() {
        // In-memory database for tests
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        
        transaction {
            SchemaUtils.create(*allTables)
        }
    }
    
    @Test
    fun `should create invoice with items`() = runBlocking {
        transaction {
            // Setup tenant
            val tenantId = Tenants.insertAndGetId {
                it[name] = "Test Corp"
                it[email] = "test@example.com"
                it[plan] = "professional"
            }.value
            
            TenantSettings.insert {
                it[TenantSettings.tenantId] = tenantId
                it[invoicePrefix] = "INV"
                it[nextInvoiceNumber] = 1
            }
            
            // Create invoice
            val repository = InvoiceRepository()
            val invoiceId = repository.create(
                tenantId = tenantId,
                clientId = UUID.randomUUID(),
                issueDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(30),
                items = listOf(
                    InvoiceItemData(
                        description = "Test Service",
                        quantity = BigDecimal("10"),
                        unitPrice = BigDecimal("100"),
                        vatRate = BigDecimal("21"),
                        lineTotal = BigDecimal("1000"),
                        vatAmount = BigDecimal("210")
                    )
                )
            )
            
            // Verify
            val invoice = repository.findById(invoiceId, tenantId)
            assertNotNull(invoice)
            assertEquals("INV-0001", invoice?.invoiceNumber)
            assertEquals(BigDecimal("1000.00"), invoice?.subtotalAmount)
        }
    }
}
```

### Integration Test RPC Service

```kotlin
class InvoiceServiceTest {
    
    private lateinit var testServer: TestApplicationEngine
    private lateinit var client: HttpClient
    private lateinit var service: InvoiceService
    
    @Before
    fun setup() {
        testServer = TestApplicationEngine()
        testServer.start()
        testServer.application.module()
        
        client = HttpClient {
            installKrpc()
        }
        
        service = client.rpc {
            url("http://localhost:8080/rpc")
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }
    
    @Test
    fun `should create and retrieve invoice`() = runTest {
        // Create
        val created = service.createInvoice(
            CreateInvoiceRequest(
                clientId = UUID.randomUUID(),
                issueDate = "2025-10-05",
                dueDate = "2025-11-04",
                items = listOf(/* ... */)
            )
        )
        
        // Retrieve
        val retrieved = service.getInvoice(created.id)
        
        assertEquals(created.id, retrieved.invoice.id)
        assertEquals(created.invoiceNumber, retrieved.invoice.invoiceNumber)
    }
}
```

---

## Migration from REST

If you have existing REST endpoints:

```kotlin
// OLD: REST approach
routing {
    post("/api/invoices") {
        val request = call.receive<CreateInvoiceRequest>()
        val invoice = invoiceService.create(request)
        call.respond(HttpStatusCode.Created, invoice)
    }
    
    get("/api/invoices") {
        val invoices = invoiceService.getAll()
        call.respond(invoices)
    }
}

// NEW: KotlinX RPC approach
rpc("/rpc") {
    registerService<InvoiceService> { ctx ->
        InvoiceServiceImpl(ctx)
    }
}
```

**Client Migration:**

```kotlin
// OLD: Manual HTTP
val response = httpClient.post("/api/invoices") {
    contentType(ContentType.Application.Json)
    setBody(request)
}
val invoice = response.body<InvoiceDTO>()

// NEW: Direct function call
val invoice = invoiceService.createInvoice(request)
```

---

## Next Steps

1. ✅ Read [[Database-Schema|Database Schema]]
2. ⬜ Set up monorepo project structure
3. ⬜ Implement shared DTOs and interfaces
4. ⬜ Create repository layer with tenant isolation
5. ⬜ Implement RPC services
6. ⬜ Set up Ktor server with KotlinX RPC
7. ⬜ Build client applications
8. ⬜ Add authentication middleware
9. ⬜ Write tests

---

## See Also

- [[Database-Schema|Database Schema]] - Complete table definitions
- [[Database-Best-Practices|Database Best Practices]] - Security and optimization
- [[02-Technical-Architecture|Technical Architecture]] - System overview
- [[12-First-90-Days|Implementation Timeline]] - What to build when

---

**Questions?** Check the [[Documentation-Index|Documentation Index]] for more guides.
