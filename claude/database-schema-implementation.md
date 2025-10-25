# Database Schema Implementation Tasks

## Task 1: Complete Missing Table Implementations

### Location
`foundation/database/src/main/kotlin/ai/dokus/foundation/database/schema/`

### Tables to Implement

#### 1. ClientsTable.kt
```kotlin
package ai.dokus.foundation.database.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ClientsTable : UUIDTable("clients") {
    val tenantId = uuid("tenant_id").index()
    val name = varchar("name", 200)
    val email = varchar("email", 254).nullable()
    val phone = varchar("phone", 50).nullable()
    val vatNumber = varchar("vat_number", 20).nullable()
    val companyNumber = varchar("company_number", 50).nullable()
    
    // Address fields
    val addressLine1 = varchar("address_line1", 255).nullable()
    val addressLine2 = varchar("address_line2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).default("BE")
    
    // Financial
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2).nullable()
    
    // Peppol
    val peppoleId = varchar("peppol_id", 100).nullable()
    val peppolEnabled = bool("peppol_enabled").default(false)
    
    // Metadata
    val notes = text("notes").nullable()
    val tags = varchar("tags", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        uniqueIndex(tenantId, email)
        index(false, tenantId, isActive)
    }
}
```

#### 2. ExpensesTable.kt
```kotlin
package ai.dokus.foundation.database.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ExpensesTable : UUIDTable("expenses") {
    val tenantId = uuid("tenant_id").index()
    val supplierId = uuid("supplier_id").references(ClientsTable.id).nullable()
    
    // Core fields
    val expenseNumber = varchar("expense_number", 50)
    val description = varchar("description", 500)
    val category = varchar("category", 100).index()
    val date = date("date").index()
    
    // Financial
    val subtotalAmount = decimal("subtotal_amount", 19, 4)
    val vatAmount = decimal("vat_amount", 19, 4)
    val totalAmount = decimal("total_amount", 19, 4)
    val currency = varchar("currency", 3).default("EUR")
    
    // Payment
    val paymentMethod = varchar("payment_method", 50).nullable()
    val isPaid = bool("is_paid").default(false)
    val paidAt = timestamp("paid_at").nullable()
    
    // References
    val receiptUrl = varchar("receipt_url", 500).nullable()
    val bankTransactionId = uuid("bank_transaction_id").nullable()
    val attachmentId = uuid("attachment_id").nullable()
    
    // VAT
    val vatRate = decimal("vat_rate", 5, 2).nullable()
    val isDeductible = bool("is_deductible").default(true)
    val deductiblePercentage = decimal("deductible_percentage", 5, 2).default(100.00)
    
    // Metadata
    val notes = text("notes").nullable()
    val tags = varchar("tags", 500).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        uniqueIndex(tenantId, expenseNumber)
        index(false, tenantId, date)
        index(false, tenantId, category)
    }
}
```

#### 3. PaymentsTable.kt
```kotlin
package ai.dokus.foundation.database.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object PaymentsTable : UUIDTable("payments") {
    val tenantId = uuid("tenant_id").index()
    val invoiceId = uuid("invoice_id").references(InvoicesTable.id)
    
    // Payment details
    val amount = decimal("amount", 19, 4)
    val currency = varchar("currency", 3).default("EUR")
    val paymentDate = date("payment_date")
    val paymentMethod = varchar("payment_method", 50)
    
    // Gateway information
    val gatewayId = varchar("gateway_id", 100).nullable()
    val gatewayStatus = varchar("gateway_status", 50).nullable()
    val gatewayResponse = text("gateway_response").nullable()
    
    // References
    val bankTransactionId = uuid("bank_transaction_id").nullable()
    val paymentLinkId = varchar("payment_link_id", 100).nullable()
    
    // Fees
    val processingFee = decimal("processing_fee", 10, 4).nullable()
    val netAmount = decimal("net_amount", 19, 4).nullable()
    
    // Metadata
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    init {
        index(false, tenantId, invoiceId)
        index(false, tenantId, paymentDate)
        uniqueIndex(gatewayId)
    }
}
```

### Task 2: Create Repository Implementations

#### Location
`foundation/database/src/main/kotlin/ai/dokus/foundation/database/repositories/`

#### BaseRepository.kt
```kotlin
package ai.dokus.foundation.database.repositories

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

abstract class BaseRepository {
    
    protected suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO) {
            addLogger(StdOutSqlLogger)
            block()
        }
    
    protected fun <T> blockingDbQuery(block: Transaction.() -> T): T =
        transaction {
            addLogger(StdOutSqlLogger)
            block()
        }
    
    // Multi-tenant query helpers
    protected fun Query.filterByTenant(tenantId: UUID): Query = this
    
    protected fun Table.tenantFilter(tenantId: UUID): Op<Boolean> {
        return (this as UUIDTable).let {
            it.columns.find { col -> col.name == "tenant_id" }?.let { tenantCol ->
                (tenantCol as Column<UUID>) eq tenantId
            } ?: Op.TRUE
        }
    }
    
    // Audit logging helper
    protected suspend fun auditLog(
        tenantId: UUID,
        userId: UUID?,
        action: String,
        entityType: String,
        entityId: UUID,
        oldValues: String? = null,
        newValues: String? = null
    ) = dbQuery {
        AuditLogsTable.insert {
            it[AuditLogsTable.tenantId] = tenantId
            it[AuditLogsTable.userId] = userId
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.entityType] = entityType
            it[AuditLogsTable.entityId] = entityId
            it[AuditLogsTable.oldValues] = oldValues
            it[AuditLogsTable.newValues] = newValues
            it[AuditLogsTable.ipAddress] = null // Get from context
            it[AuditLogsTable.userAgent] = null // Get from context
        }
    }
}
```

#### ClientRepository.kt
```kotlin
package ai.dokus.foundation.database.repositories

import ai.dokus.foundation.database.schema.ClientsTable
import ai.dokus.foundation.domain.model.Client
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import java.util.UUID

class ClientRepository : BaseRepository() {
    
    suspend fun create(
        tenantId: UUID,
        name: String,
        email: String? = null,
        vatNumber: String? = null
    ): UUID = dbQuery {
        val clientId = ClientsTable.insertAndGetId {
            it[ClientsTable.tenantId] = tenantId
            it[ClientsTable.name] = name
            it[ClientsTable.email] = email
            it[ClientsTable.vatNumber] = vatNumber
            it[createdAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
        }.value
        
        auditLog(
            tenantId = tenantId,
            userId = null, // Get from context
            action = "client.created",
            entityType = "client",
            entityId = clientId,
            newValues = """{"name": "$name", "email": "$email"}"""
        )
        
        clientId
    }
    
    suspend fun findById(id: UUID, tenantId: UUID): Client? = dbQuery {
        ClientsTable
            .select { 
                (ClientsTable.id eq id) and 
                (ClientsTable.tenantId eq tenantId) 
            }
            .singleOrNull()
            ?.let { mapToClient(it) }
    }
    
    suspend fun findByTenant(
        tenantId: UUID,
        isActive: Boolean? = null,
        limit: Int = 100
    ): List<Client> = dbQuery {
        val query = ClientsTable.select { 
            ClientsTable.tenantId eq tenantId 
        }
        
        if (isActive != null) {
            query.andWhere { ClientsTable.isActive eq isActive }
        }
        
        query
            .orderBy(ClientsTable.name)
            .limit(limit)
            .map { mapToClient(it) }
    }
    
    suspend fun update(
        id: UUID,
        tenantId: UUID,
        updates: ClientUpdateData
    ): Boolean = dbQuery {
        val updated = ClientsTable.update({
            (ClientsTable.id eq id) and 
            (ClientsTable.tenantId eq tenantId)
        }) {
            updates.name?.let { name -> it[ClientsTable.name] = name }
            updates.email?.let { email -> it[ClientsTable.email] = email }
            updates.vatNumber?.let { vat -> it[vatNumber] = vat }
            it[updatedAt] = Clock.System.now()
        }
        
        if (updated > 0) {
            auditLog(
                tenantId = tenantId,
                userId = null,
                action = "client.updated",
                entityType = "client",
                entityId = id
            )
        }
        
        updated > 0
    }
    
    private fun mapToClient(row: ResultRow): Client {
        // Map to domain model
        TODO("Implement mapping")
    }
}

data class ClientUpdateData(
    val name: String? = null,
    val email: String? = null,
    val vatNumber: String? = null
)
```

### Task 3: Create Flyway Migrations

#### Location
`foundation/database/src/main/resources/db/migration/`

#### V1__initial_schema.sql
```sql
-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tenants table (organizations)
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    subdomain VARCHAR(100) UNIQUE,
    settings JSONB,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(254) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) DEFAULT 'user',
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    mfa_enabled BOOLEAN DEFAULT false,
    mfa_secret VARCHAR(255),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Continue with all other tables...
```

### Task 4: Service Implementation Pattern

#### Location
`features/*/backend/src/main/kotlin/ai/dokus/*/services/`

#### Template for New Services
```kotlin
package ai.dokus.[feature].backend.services

import ai.dokus.foundation.apispec.[Feature]Api
import ai.dokus.foundation.database.repositories.[Feature]Repository
import ai.dokus.foundation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RpcServiceContext

class [Feature]ApiImpl(
    private val context: RpcServiceContext,
    private val repository: [Feature]Repository = [Feature]Repository()
) : [Feature]Api {
    
    private fun getTenantId(): UUID {
        return context.get("tenantId")?.let { UUID.fromString(it) }
            ?: throw UnauthorizedException("No tenant context")
    }
    
    private fun getUserId(): UUID? {
        return context.get("userId")?.let { UUID.fromString(it) }
    }
    
    override suspend fun create(request: Create[Feature]Request): Result<[Feature]> = 
        runCatching {
            val tenantId = getTenantId()
            
            // Validation
            validateRequest(request)
            
            // Business logic
            val id = repository.create(
                tenantId = tenantId,
                // map request fields
            )
            
            // Return entity
            repository.findById(id, tenantId)
                ?: throw IllegalStateException("Failed to create")
        }
    
    private fun validateRequest(request: Create[Feature]Request) {
        // Implement validation rules
    }
}
```

## Implementation Checklist

### Database Layer
- [ ] Implement all 14 table definitions
- [ ] Create BaseRepository with tenant isolation
- [ ] Implement repository for each entity
- [ ] Create Flyway migration scripts
- [ ] Add proper indexes for performance
- [ ] Implement audit logging

### Service Layer
- [ ] Create service interfaces in apispec
- [ ] Implement service classes
- [ ] Add proper validation
- [ ] Implement business logic
- [ ] Add error handling
- [ ] Integrate with repositories

### Testing
- [ ] Unit tests for repositories
- [ ] Integration tests for services
- [ ] Multi-tenant isolation tests
- [ ] Performance tests
- [ ] Security tests

### Documentation
- [ ] Update API documentation
- [ ] Add code comments
- [ ] Create developer guide
- [ ] Document deployment process
