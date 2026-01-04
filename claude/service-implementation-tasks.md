# Service Implementation Tasks for Claude Code

## Overview
Complete implementation of all backend services using KotlinX RPC pattern established in the codebase.

## Task 1: Client Service Implementation

### Step 1: Define API Interface
**Location:** `foundation/apispec/src/commonMain/kotlin/ai/dokus/foundation/apispec/ClientApi.kt`

```kotlin
package tech.dokus.foundation.apispec

import tech.dokus.foundation.domain.*
import tech.dokus.foundation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ClientApi : RemoteService {
    
    suspend fun createClient(request: CreateClientRequest): Result<Client>
    
    suspend fun getClient(id: ClientId): Result<Client>
    
    suspend fun updateClient(id: ClientId, request: UpdateClientRequest): Result<Client>
    
    suspend fun deleteClient(id: ClientId): Result<Unit>
    
    suspend fun listClients(
        search: String? = null,
        isActive: Boolean? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Client>>
    
    suspend fun searchClients(query: String): Result<List<Client>>
    
    suspend fun getClientStatistics(id: ClientId): Result<ClientStatistics>
    
    // Real-time updates
    fun watchClients(tenantId: TenantId): Flow<Client>
}
```

### Step 2: Implement Service
**Location:** `features/clients/backend/src/main/kotlin/ai/dokus/clients/backend/services/ClientApiImpl.kt`

```kotlin
package tech.dokus.clients.backend.services

import tech.dokus.foundation.apispec.ClientApi
import tech.dokus.foundation.database.repositories.ClientRepository
import tech.dokus.foundation.domain.*
import tech.dokus.foundation.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.rpc.RpcServiceContext
import java.util.UUID

class ClientApiImpl(
    private val context: RpcServiceContext,
    private val repository: ClientRepository = ClientRepository()
) : ClientApi {
    
    private val clientUpdates = MutableSharedFlow<Client>()
    
    private fun getTenantId(): UUID = 
        context.get("tenantId")?.let { UUID.fromString(it) }
            ?: throw UnauthorizedException("No tenant context")
    
    override suspend fun createClient(request: CreateClientRequest): Result<Client> = 
        runCatching {
            val tenantId = getTenantId()
            
            // Validate
            require(request.name.isNotBlank()) { "Client name is required" }
            request.email?.let {
                require(it.contains("@")) { "Invalid email format" }
            }
            request.vatNumber?.let {
                require(validateVatNumber(it)) { "Invalid VAT number" }
            }
            
            // Create
            val clientId = repository.create(
                tenantId = tenantId,
                name = request.name,
                email = request.email,
                phone = request.phone,
                vatNumber = request.vatNumber,
                addressLine1 = request.addressLine1,
                city = request.city,
                postalCode = request.postalCode,
                country = request.country
            )
            
            // Get created client
            val client = repository.findById(clientId, tenantId)
                ?: throw IllegalStateException("Failed to create client")
            
            // Emit update
            clientUpdates.emit(client)
            
            client
        }
    
    override suspend fun updateClient(
        id: ClientId, 
        request: UpdateClientRequest
    ): Result<Client> = runCatching {
        val tenantId = getTenantId()
        
        // Update
        val updated = repository.update(
            id = id.value.toJavaUuid(),
            tenantId = tenantId,
            updates = ClientUpdateData(
                name = request.name,
                email = request.email,
                phone = request.phone,
                vatNumber = request.vatNumber
            )
        )
        
        if (!updated) {
            throw NotFoundException("Client not found: $id")
        }
        
        // Get updated client
        val client = repository.findById(id.value.toJavaUuid(), tenantId)!!
        
        // Emit update
        clientUpdates.emit(client)
        
        client
    }
    
    override suspend fun listClients(
        search: String?,
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<Client>> = runCatching {
        val tenantId = getTenantId()
        
        repository.findByTenant(
            tenantId = tenantId,
            search = search,
            isActive = isActive,
            limit = limit,
            offset = offset
        )
    }
    
    override fun watchClients(tenantId: TenantId): Flow<Client> {
        // Filter by tenant
        return clientUpdates
    }
    
    private fun validateVatNumber(vat: String): Boolean {
        // Belgian VAT format: BE0123456789
        val belgiumVat = Regex("^BE[0-9]{10}$")
        // Dutch VAT format: NL123456789B01
        val netherlandsVat = Regex("^NL[0-9]{9}B[0-9]{2}$")
        
        return belgiumVat.matches(vat) || netherlandsVat.matches(vat)
    }
}
```

## Task 2: Expense Service Implementation

### API Interface
**Location:** `foundation/apispec/src/commonMain/kotlin/ai/dokus/foundation/apispec/ExpenseApi.kt`

```kotlin
@Rpc
interface ExpenseApi : RemoteService {
    
    suspend fun createExpense(request: CreateExpenseRequest): Result<Expense>
    
    suspend fun getExpense(id: ExpenseId): Result<Expense>
    
    suspend fun listExpenses(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        category: String? = null,
        isPaid: Boolean? = null,
        limit: Int = 100
    ): Result<List<Expense>>
    
    suspend fun updateExpense(id: ExpenseId, request: UpdateExpenseRequest): Result<Expense>
    
    suspend fun deleteExpense(id: ExpenseId): Result<Unit>
    
    suspend fun attachReceipt(expenseId: ExpenseId, receipt: ByteArray): Result<String>
    
    suspend fun categorizeExpense(expenseId: ExpenseId, category: String): Result<Unit>
    
    suspend fun getExpenseStatistics(
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<ExpenseStatistics>
    
    suspend fun generateExpenseReport(
        fromDate: LocalDate,
        toDate: LocalDate,
        format: ReportFormat
    ): Result<ByteArray>
}
```

## Task 3: Payment Service Implementation

### API Interface
**Location:** `foundation/apispec/src/commonMain/kotlin/ai/dokus/foundation/apispec/PaymentApi.kt`

```kotlin
@Rpc
interface PaymentApi : RemoteService {
    
    suspend fun createPaymentLink(
        invoiceId: InvoiceId,
        expiresAt: Instant? = null
    ): Result<PaymentLink>
    
    suspend fun processPayment(request: ProcessPaymentRequest): Result<Payment>
    
    suspend fun recordManualPayment(request: ManualPaymentRequest): Result<Payment>
    
    suspend fun getPayment(id: PaymentId): Result<Payment>
    
    suspend fun listPayments(
        invoiceId: InvoiceId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<Payment>>
    
    suspend fun refundPayment(
        paymentId: PaymentId,
        amount: Money? = null,
        reason: String
    ): Result<Refund>
    
    suspend fun handleStripeWebhook(payload: String, signature: String): Result<Unit>
    
    suspend fun handleMollieWebhook(payload: String): Result<Unit>
    
    suspend fun getPaymentStatistics(
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<PaymentStatistics>
}
```

## Task 4: Peppol Integration Service

### Implementation
**Location:** `features/peppol/backend/src/main/kotlin/ai/dokus/peppol/backend/`

```kotlin
package tech.dokus.peppol.backend.services

import tech.dokus.foundation.domain.model.Invoice
import tech.dokus.peppol.backend.converters.UblConverter
import tech.dokus.peppol.backend.validators.SchematronValidator

class PeppolService(
    private val accessPointClient: AccessPointClient,
    private val ublConverter: UblConverter,
    private val validator: SchematronValidator
) {
    
    suspend fun sendInvoice(invoice: Invoice): PeppolTransmissionResult {
        // 1. Convert to UBL format
        val ublDocument = ublConverter.convertToUbl(invoice)
        
        // 2. Validate against Schematron
        val validationResult = validator.validate(ublDocument)
        if (!validationResult.isValid) {
            throw ValidationException(validationResult.errors)
        }
        
        // 3. Send to Access Point
        val transmission = accessPointClient.send(
            document = ublDocument,
            recipientId = invoice.client.peppolId,
            documentType = DocumentType.INVOICE
        )
        
        // 4. Store transmission details
        return PeppolTransmissionResult(
            transmissionId = transmission.id,
            timestamp = transmission.timestamp,
            status = transmission.status
        )
    }
    
    suspend fun getTransmissionStatus(transmissionId: String): TransmissionStatus {
        return accessPointClient.getStatus(transmissionId)
    }
    
    suspend fun registerEndpoint(
        participantId: String,
        documentTypes: List<DocumentType>
    ): RegistrationResult {
        return accessPointClient.registerEndpoint(
            participantId = participantId,
            documentTypes = documentTypes,
            transportProfile = TransportProfile.AS4
        )
    }
}
```

## Task 5: Complete Application Setup

### Main Application Module
**Location:** `features/*/backend/src/main/kotlin/ai/dokus/*/backend/Application.kt`

```kotlin
package tech.dokus.[feature].backend

import tech.dokus.foundation.database.DatabaseFactory
import tech.dokus.foundation.ktor.*
import tech.dokus.[feature].backend.services.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.rpc.krpc.ktor.server.*

fun main() {
    embeddedServer(
        Netty,
        port = AppConfig.load().ktor.deployment.port,
        host = AppConfig.load().ktor.deployment.host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init(AppConfig.load().database)
    
    // Configure server
    configureHTTP()
    configureSecurity()
    configureMonitoring()
    configureRPC()
}

fun Application.configureRPC() {
    install(Krpc)
    
    routing {
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json {
                        prettyPrint = false
                        ignoreUnknownKeys = true
                    }
                }
            }
            
            // Register all services
            registerService<ClientApi> { ctx ->
                ClientApiImpl(ctx)
            }
            
            registerService<ExpenseApi> { ctx ->
                ExpenseApiImpl(ctx)
            }
            
            registerService<PaymentApi> { ctx ->
                PaymentApiImpl(ctx)
            }
            
            registerService<InvoiceApi> { ctx ->
                InvoiceApiImpl(ctx)
            }
        }
    }
}
```

## Implementation Priority Order

### Week 1: Foundation
1. ✅ Complete database schema (all tables)
2. ✅ Implement BaseRepository pattern
3. ✅ Create all entity repositories
4. ✅ Set up Flyway migrations
5. ✅ Configure test database

### Week 2: Core Services
1. ⬜ Client Service (CRUD + search)
2. ⬜ Expense Service (tracking + receipts)
3. ⬜ Payment Service (recording + links)
4. ⬜ Enhanced Invoice Service (PDF + email)

### Week 3: Integrations
1. ⬜ Peppol integration
2. ⬜ Stripe webhook handling
3. ⬜ Mollie webhook handling
4. ⬜ Email service (SendGrid/AWS SES)
5. ⬜ S3 file storage

### Week 4: Production Ready
1. ⬜ Comprehensive testing
2. ⬜ Performance optimization
3. ⬜ Security audit
4. ⬜ Monitoring setup
5. ⬜ Deployment pipeline

## Testing Requirements

### Unit Tests
- Repository tests with H2
- Service logic tests
- Validation tests
- Multi-tenant isolation tests

### Integration Tests
- Full service flow tests
- Database transaction tests
- External API mock tests
- Error handling tests

### Performance Tests
- Load testing with K6
- Database query optimization
- Connection pool tuning
- Cache effectiveness

## Success Criteria
- [ ] All services accessible via KotlinX RPC
- [ ] 100% multi-tenant isolation
- [ ] <100ms p95 response time
- [ ] 80%+ test coverage
- [ ] Zero critical security issues
- [ ] Peppol certification achieved
- [ ] Production deployment ready
