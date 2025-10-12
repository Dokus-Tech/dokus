# Database Best Practices

**Last Updated:** October 2025  
**For:** Dokus Financial Management Platform  
**Database:** PostgreSQL 15+ with Exposed ORM

---

## Navigation

🏠 [[Documentation-Index|Back to Index]]  
💾 [[Database-Schema|Database Schema]]  
🔌 [[KotlinX-RPC-Integration|RPC Integration]]  
🏗️ [[02-Technical-Architecture|Technical Architecture]]

---

## Table of Contents

1. [[#Multi-Tenant Security]]
2. [[#Financial Data Handling]]
3. [[#Performance Optimization]]
4. [[#Transaction Management]]
5. [[#Error Handling]]
6. [[#Backup & Recovery]]
7. [[#Monitoring]]
8. [[#Common Pitfalls]]

---

## Multi-Tenant Security

### Rule #1: ALWAYS Filter by tenant_id

**This is CRITICAL** - Every query must include tenant_id in the WHERE clause.

```kotlin
// ❌ SECURITY VULNERABILITY!
fun getInvoice(invoiceId: UUID) = transaction {
    Invoices.select { Invoices.id eq invoiceId }
}
// Problem: User from Tenant A can access Tenant B's invoices!

// ✅ SECURE
fun getInvoice(invoiceId: UUID, tenantId: UUID) = transaction {
    Invoices.select {
        (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
    }
}
```

### Enforce in Repository Layer

```kotlin
abstract class BaseRepository {
    /**
     * Validate tenant access before operations
     */
    protected fun validateTenantAccess(
        entityTenantId: UUID,
        requestTenantId: UUID
    ) {
        if (entityTenantId != requestTenantId) {
            throw UnauthorizedException("Access denied to this resource")
        }
    }
    
    /**
     * Template for tenant-scoped queries
     */
    protected fun tenantQuery(
        tenantId: UUID,
        block: SqlExpressionBuilder.() -> Op<Boolean>
    ): Query {
        return /* table */.select {
            (/* table */.tenantId eq tenantId) and block()
        }
    }
}
```

### Testing Tenant Isolation

```kotlin
@Test
fun `should not access other tenant's data`() = runBlocking {
    val tenant1 = createTenant("Tenant 1")
    val tenant2 = createTenant("Tenant 2")
    
    val invoice1 = createInvoice(tenantId = tenant1)
    
    // Try to access from wrong tenant
    val result = repository.findById(invoice1, tenantId = tenant2)
    
    assertNull(result)  // Should not find
}
```

---

## Financial Data Handling

### Use NUMERIC, Never FLOAT/REAL

**Problem with FLOAT:**
```kotlin
// ❌ WRONG - Introduces rounding errors
val price = 19.99f
val quantity = 3f
val total = price * quantity  // 59.970005 instead of 59.97!
```

**Solution with NUMERIC:**
```kotlin
// ✅ CORRECT - Exact decimal arithmetic
val price = BigDecimal("19.99")
val quantity = BigDecimal("3")
val total = price * quantity  // Exactly 59.97

// In Exposed schema
val totalAmount = decimal("total_amount", 12, 2)
```

### NUMERIC Precision Guide

```kotlin
// For money (euros, dollars, etc.)
decimal("amount", 12, 2)
// 12 total digits, 2 after decimal
// Range: -9,999,999,999.99 to 9,999,999,999.99
// Perfect for invoices up to ~10 billion

// For VAT rates (percentages)
decimal("vat_rate", 5, 2)
// Range: -999.99 to 999.99
// Perfect for 0.00% to 100.00%

// For quantities
decimal("quantity", 10, 2)
// Range: -99,999,999.99 to 99,999,999.99
// Perfect for hours, units, etc.
```

### Calculation Best Practices

```kotlin
// ✅ Calculate in correct order
fun calculateInvoiceTotal(items: List<InvoiceItem>): InvoiceTotals {
    val subtotal = items.sumOf { item ->
        item.quantity * item.unitPrice  // Line total
    }
    
    val vatByRate = items.groupBy { it.vatRate }.mapValues { (rate, items) ->
        val totalForRate = items.sumOf { it.quantity * it.unitPrice }
        totalForRate * (rate / BigDecimal(100))
    }
    
    val totalVat = vatByRate.values.sumOf { it }
    val total = subtotal + totalVat
    
    return InvoiceTotals(
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP),
        vat = totalVat.setScale(2, RoundingMode.HALF_UP),
        total = total.setScale(2, RoundingMode.HALF_UP)
    )
}
```

### Rounding Rules

```kotlin
import java.math.RoundingMode

// Belgian tax law: round to nearest cent
val amount = BigDecimal("10.555")
val rounded = amount.setScale(2, RoundingMode.HALF_UP)  // 10.56

// Available modes:
// HALF_UP:   Round 5 up (10.555 -> 10.56) ✅ Use this
// HALF_DOWN: Round 5 down (10.555 -> 10.55)
// FLOOR:     Always down (10.56 -> 10.55)
// CEILING:   Always up (10.54 -> 10.55)
```

---

## Performance Optimization

### Index Strategy

**Rule: Index what you query**

```kotlin
object Invoices : UUIDTable("invoices") {
    val tenantId = reference("tenant_id", Tenants)
    val clientId = reference("client_id", Clients)
    val status = varchar("status", 50)
    val issueDate = date("issue_date")
    val dueDate = date("due_date")
    
    init {
        // Primary tenant isolation
        index(false, tenantId)
        
        // Dashboard query: "Show me overdue invoices"
        index(false, tenantId, status, dueDate)
        
        // Invoice search: "Find invoices for this client"
        index(false, tenantId, clientId, issueDate)
        
        // Unique constraint
        uniqueIndex(tenantId, invoiceNumber)
    }
}
```

**Composite Index Order:**
```sql
-- ✅ GOOD - Filter by tenant first, then others
CREATE INDEX idx_invoices_tenant_status 
ON invoices(tenant_id, status, due_date);

-- Query benefits from index:
SELECT * FROM invoices 
WHERE tenant_id = ? AND status = 'overdue' 
ORDER BY due_date;

-- ❌ BAD - Wrong order
CREATE INDEX idx_invoices_status_tenant 
ON invoices(status, tenant_id);
-- Less effective because tenant_id should come first
```

### Avoid N+1 Queries

**Problem:**
```kotlin
// ❌ N+1 queries - SLOW!
fun getInvoicesWithClients(tenantId: UUID): List<InvoiceWithClient> {
    val invoices = Invoices
        .select { Invoices.tenantId eq tenantId }
        .map { it }
    
    return invoices.map { invoice ->
        // This runs N times!
        val client = Clients
            .select { Clients.id eq invoice.clientId }
            .single()
        
        InvoiceWithClient(invoice, client)
    }
}
```

**Solution:**
```kotlin
// ✅ Single join query - FAST!
fun getInvoicesWithClients(tenantId: UUID): List<InvoiceWithClient> {
    return Invoices
        .innerJoin(Clients, { Invoices.clientId }, { Clients.id })
        .select { Invoices.tenantId eq tenantId }
        .map { row ->
            InvoiceWithClient(
                invoice = mapInvoice(row),
                client = mapClient(row)
            )
        }
}
```

### Batch Operations

**Insert Multiple Rows:**
```kotlin
// ❌ Slow - N inserts
items.forEach { item ->
    InvoiceItems.insert {
        it[invoiceId] = id
        it[description] = item.description
        // ...
    }
}

// ✅ Fast - Single batch insert
InvoiceItems.batchInsert(items) { item ->
    this[invoiceId] = id
    this[description] = item.description
    // ...
}
```

**Update Multiple Rows:**
```kotlin
// Mark invoices as overdue
Invoices.update({
    (Invoices.tenantId eq tenantId) and 
    (Invoices.dueDate less LocalDate.now()) and
    (Invoices.status eq "sent")
}) {
    it[status] = "overdue"
}
```

### Connection Pooling

```kotlin
val config = HikariConfig().apply {
    // Connection settings
    jdbcUrl = "jdbc:postgresql://localhost:5432/dokus"
    driverClassName = "org.postgresql.Driver"
    username = "dokus"
    password = "secure_password"
    
    // Pool size
    maximumPoolSize = 10          // Max connections
    minimumIdle = 2               // Keep warm
    
    // Timeouts
    connectionTimeout = 30000     // 30s to get connection
    idleTimeout = 600000          // 10min idle before close
    maxLifetime = 1800000         // 30min max connection age
    
    // Health check
    connectionTestQuery = "SELECT 1"
    
    // Leak detection (development only)
    leakDetectionThreshold = 60000  // Warn if held >60s
}
```

---

## Transaction Management

### Transaction Boundaries

```kotlin
// ✅ CORRECT - Entire operation is atomic
suspend fun createInvoice(...) = transaction {
    val invoiceId = Invoices.insertAndGetId { ... }
    
    items.forEach { item ->
        InvoiceItems.insert { ... }
    }
    
    TenantSettings.update { ... }
    AuditLogs.insert { ... }
    
    invoiceId  // All succeed or all fail
}
```

### Avoid Nested Transactions

```kotlin
// ❌ WRONG - Nested transactions
suspend fun createInvoiceAndClient(...) = transaction {
    val clientId = createClient(...)  // Also wrapped in transaction!
    val invoiceId = createInvoice(clientId, ...)
    invoiceId
}

// ✅ CORRECT - Share transaction or split operations
suspend fun createInvoiceAndClient(...) = transaction {
    // Both operations in same transaction
    val clientId = Clients.insertAndGetId { ... }
    val invoiceId = Invoices.insertAndGetId { ... }
    invoiceId
}
```

### Transaction Isolation

```kotlin
// Set isolation level in HikariCP
config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

// Available levels:
// READ_UNCOMMITTED - Dirty reads possible ❌
// READ_COMMITTED   - Default in PostgreSQL ⚠️
// REPEATABLE_READ  - Recommended for financial data ✅
// SERIALIZABLE     - Strictest, but slowest 🐌
```

### Deadlock Prevention

```kotlin
// ✅ Always lock tables in same order
transaction {
    // 1. Tenants
    // 2. Users
    // 3. Clients
    // 4. Invoices
    // 5. InvoiceItems
    
    // Never mix order to avoid deadlocks
}

// Set timeout
Database.connect(dataSource).apply {
    TransactionManager.defaultIsolationLevel = 
        Connection.TRANSACTION_REPEATABLE_READ
    TransactionManager.defaultRepetitionAttempts = 3
}
```

---

## Error Handling

### Exception Hierarchy

```kotlin
// Base exception
open class DokusException(message: String) : Exception(message)

// Specific exceptions
class NotFoundException(message: String) : DokusException(message)
class ValidationException(message: String) : DokusException(message)
class UnauthorizedException(message: String) : DokusException(message)
class ConflictException(message: String) : DokusException(message)
class DatabaseException(message: String) : DokusException(message)
```

### Error Handling in Repository

```kotlin
suspend fun createInvoice(...): UUID = try {
    transaction {
        // ... database operations
    }
} catch (e: ExposedSQLException) {
    when {
        e.message?.contains("unique constraint") == true ->
            throw ConflictException("Invoice number already exists")
        
        e.message?.contains("foreign key") == true ->
            throw ValidationException("Invalid client ID")
        
        else ->
            throw DatabaseException("Database error: ${e.message}")
    }
}
```

### Validation Before Database

```kotlin
// ✅ Validate early, fail fast
fun validateCreateRequest(request: CreateInvoiceRequest) {
    require(request.items.isNotEmpty()) {
        "Invoice must have at least one item"
    }
    
    require(request.items.all { it.quantity > BigDecimal.ZERO }) {
        "Quantity must be positive"
    }
    
    require(request.items.all { it.unitPrice >= BigDecimal.ZERO }) {
        "Price cannot be negative"
    }
    
    val issueDate = LocalDate.parse(request.issueDate)
    val dueDate = LocalDate.parse(request.dueDate)
    
    require(!dueDate.isBefore(issueDate)) {
        "Due date must be after issue date"
    }
}
```

---

## Backup & Recovery

### Backup Strategy

```bash
# Daily automated backups
0 2 * * * pg_dump -U dokus -F c dokus > /backups/dokus_$(date +\%Y\%m\%d).dump

# Weekly full backup
0 3 * * 0 pg_dumpall -U postgres > /backups/full_$(date +\%Y\%m\%d).sql

# Retention: 
# - Daily: Keep 30 days
# - Weekly: Keep 12 weeks
# - Monthly: Keep 12 months
```

### Point-in-Time Recovery

```sql
-- Enable WAL archiving in postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

### Test Restore

```bash
# Test restore monthly
pg_restore -U dokus -d dokus_test /backups/latest.dump

# Verify data
psql -U dokus -d dokus_test -c "SELECT COUNT(*) FROM invoices;"
```

---

## Monitoring

### Key Metrics to Track

**1. Query Performance:**
```sql
-- Enable slow query logging
ALTER SYSTEM SET log_min_duration_statement = 100;  -- Log queries >100ms

-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

**2. Connection Pool:**
```kotlin
// Monitor HikariCP metrics
val pool = dataSource as HikariDataSource
logger.info("Active connections: ${pool.hikariPoolMXBean.activeConnections}")
logger.info("Idle connections: ${pool.hikariPoolMXBean.idleConnections}")
logger.info("Total connections: ${pool.hikariPoolMXBean.totalConnections}")
logger.info("Threads waiting: ${pool.hikariPoolMXBean.threadsAwaitingConnection}")
```

**3. Table Sizes:**
```sql
-- Monitor table growth
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**4. Index Usage:**
```sql
-- Find unused indexes
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexname NOT LIKE '%_pkey';
```

### Alerts to Set Up

```yaml
alerts:
  - name: "Slow Queries"
    condition: query_time > 1000ms
    action: Log and notify
    
  - name: "Connection Pool Exhausted"
    condition: waiting_threads > 0
    action: Alert immediately
    
  - name: "Table Size Growth"
    condition: table_size > 10GB
    action: Review retention policy
    
  - name: "Replication Lag"
    condition: lag > 30s
    action: Alert DBA
```

---

## Common Pitfalls

### 1. Missing tenant_id Filter

```kotlin
// ❌ DANGER: Missing tenant filter
fun getAllInvoices() = transaction {
    Invoices.selectAll()  // Returns ALL tenants' data!
}

// ✅ ALWAYS filter by tenant
fun getAllInvoices(tenantId: UUID) = transaction {
    Invoices.select { Invoices.tenantId eq tenantId }
}
```

### 2. Using FLOAT for Money

```kotlin
// ❌ WRONG
val total = 19.99 * 1.21  // 24.1879... 

// ✅ CORRECT
val total = BigDecimal("19.99") * BigDecimal("1.21")  // Exactly 24.19
```

### 3. N+1 Query Problem

```kotlin
// ❌ BAD: N+1 queries
invoices.forEach { invoice ->
    val client = Clients.select { ... }.single()  // N queries
}

// ✅ GOOD: Join
Invoices.innerJoin(Clients).select { ... }  // 1 query
```

### 4. Not Using Transactions

```kotlin
// ❌ DANGEROUS: Partial updates possible
fun createInvoice(...) {
    val id = Invoices.insert { ... }
    InvoiceItems.insert { ... }  // Could fail, leaving orphan invoice!
}

// ✅ SAFE: All or nothing
fun createInvoice(...) = transaction {
    val id = Invoices.insert { ... }
    InvoiceItems.insert { ... }
}
```

### 5. Forgetting Audit Logs

```kotlin
// ❌ NO AUDIT TRAIL
fun deleteInvoice(id: UUID) = transaction {
    Invoices.deleteWhere { Invoices.id eq id }
}

// ✅ ALWAYS LOG
fun deleteInvoice(id: UUID, tenantId: UUID, userId: UUID) = transaction {
    Invoices.deleteWhere { 
        (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
    }
    
    AuditLogs.insert {
        it[AuditLogs.tenantId] = tenantId
        it[AuditLogs.userId] = userId
        it[action] = "invoice.deleted"
        it[entityType] = "invoice"
        it[entityId] = id
    }
}
```

### 6. Exposing Database Entities

```kotlin
// ❌ WRONG: Exposing internal structure
@Rpc
interface InvoiceService {
    suspend fun getInvoice(id: UUID): ResultRow  // Exposed type!
}

// ✅ CORRECT: Use DTOs
@Rpc
interface InvoiceService {
    suspend fun getInvoice(id: UUID): InvoiceDTO  // Clean API
}
```

### 7. Not Handling Unique Constraints

```kotlin
// ❌ Unhandled database error
fun createInvoice(...) = transaction {
    Invoices.insert { ... }  // Throws if invoice_number exists
}

// ✅ Handle gracefully
fun createInvoice(...) = try {
    transaction {
        Invoices.insert { ... }
    }
} catch (e: ExposedSQLException) {
    if (e.message?.contains("unique constraint") == true) {
        throw ConflictException("Invoice number already exists")
    }
    throw e
}
```

---

## Security Checklist

Before deploying to production:

- [ ] All queries filter by `tenant_id`
- [ ] Use NUMERIC for all monetary values
- [ ] All mutations wrapped in transactions
- [ ] Audit logging for all financial operations
- [ ] Sensitive fields encrypted (MFA secrets, bank tokens)
- [ ] Connection pooling configured
- [ ] Slow query logging enabled
- [ ] Automated backups scheduled
- [ ] Point-in-time recovery tested
- [ ] Indexes on all foreign keys
- [ ] Composite indexes for common queries
- [ ] No raw SQL (use Exposed DSL)
- [ ] Input validation before database
- [ ] Proper exception handling
- [ ] No database entities exposed in DTOs

---

## Performance Checklist

- [ ] N+1 queries eliminated (use joins)
- [ ] Batch operations for bulk inserts
- [ ] Appropriate indexes created
- [ ] Connection pool sized correctly
- [ ] Transaction isolation level set
- [ ] Query timeout configured
- [ ] Monitoring and alerting set up
- [ ] Regular VACUUM and ANALYZE
- [ ] Index usage tracked
- [ ] Slow queries logged and reviewed

---

## See Also

- [[Database-Schema|Database Schema]] - Complete table definitions
- [[KotlinX-RPC-Integration|RPC Integration]] - Service implementation
- [[02-Technical-Architecture|Technical Architecture]] - Overall design
- [[12-First-90-Days|Implementation Timeline]] - Build sequence

---

**Questions?** Check the [[Documentation-Index|Documentation Index]] for more guides.
