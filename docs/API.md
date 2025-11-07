# API Reference

**Last Updated:** October 2025
**Protocol:** KotlinX RPC 0.10.1 over HTTP
**Status:** Production API

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Services](#api-services)
4. [Error Handling](#error-handling)
5. [Client Setup](#client-setup)
6. [Common Patterns](#common-patterns)

---

## Overview

Dokus uses **KotlinX RPC** for type-safe communication between client applications and backend services. Unlike traditional REST APIs, KotlinX RPC provides:

✅ **Compile-time type safety** - No runtime errors from API changes
✅ **Shared contracts** - Same interface on client and server
✅ **Automatic serialization** - No manual JSON parsing
✅ **Real-time streaming** - Flow-based updates
✅ **Multiplatform support** - Works on Android, iOS, Desktop, and Web

### Communication Protocol

```
Client App (KMP)
      ↓
   RPC Call
      ↓
 HTTP/JSON Transport
      ↓
Backend Service (Ktor)
      ↓
   Database (PostgreSQL)
```

---

## Authentication

### JWT-Based Authentication

All API calls (except login/register) require a valid JWT access token.

**Token Types:**
- **Access Token**: Short-lived (15 minutes), included in RPC context
- **Refresh Token**: Long-lived (7 days), stored securely client-side

### Authentication Flow

```kotlin
// 1. Login
val result = authService.login(
    email = "user@example.com",
    password = "securePassword123"
)

when (result) {
    is Result.Success -> {
        val tokens = result.data
        // Store tokens securely
        secureStorage.saveAccessToken(tokens.accessToken)
        secureStorage.saveRefreshToken(tokens.refreshToken)
    }
    is Result.Failure -> {
        // Handle error
    }
}

// 2. Make authenticated calls
val rpcClient = RPC.Client {
    streamScoped {
        withRPCContext {
            put("Authorization", "Bearer ${secureStorage.getAccessToken()}")
        }
    }
}

// 3. Refresh token when access token expires
val refreshResult = authService.refreshToken(
    refreshToken = secureStorage.getRefreshToken()
)
```

---

## API Services

### AuthService

**Package:** `ai.dokus.foundation.apispec.AuthApi`

```kotlin
interface AuthApi : RPC {
    /**
     * Login with email and password
     * Returns access and refresh tokens
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<AuthTokens>

    /**
     * Register new user account
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<User>

    /**
     * Refresh access token
     */
    suspend fun refreshToken(
        refreshToken: String
    ): Result<AuthTokens>

    /**
     * Logout (revokes refresh token)
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get current user profile
     */
    suspend fun getCurrentUser(): Result<User>
}
```

**Data Models:**
```kotlin
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long  // seconds until access token expires
)

@Serializable
data class User(
    val id: UUID,
    val tenantId: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole
)

@Serializable
enum class UserRole {
    OWNER, MEMBER, ACCOUNTANT, VIEWER
}
```

---

### InvoiceService

**Package:** `ai.dokus.foundation.apispec.InvoiceApi`

```kotlin
interface InvoiceApi : RPC {
    /**
     * Get all invoices for current tenant
     */
    suspend fun getInvoices(
        status: InvoiceStatus? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Invoice>>

    /**
     * Get single invoice by ID
     */
    suspend fun getInvoice(id: UUID): Result<Invoice>

    /**
     * Create new invoice
     */
    suspend fun createInvoice(
        request: CreateInvoiceRequest
    ): Result<Invoice>

    /**
     * Update existing invoice
     */
    suspend fun updateInvoice(
        id: UUID,
        request: UpdateInvoiceRequest
    ): Result<Invoice>

    /**
     * Delete invoice (only drafts)
     */
    suspend fun deleteInvoice(id: UUID): Result<Unit>

    /**
     * Send invoice to client (via email/Peppol)
     */
    suspend fun sendInvoice(id: UUID): Result<Invoice>

    /**
     * Mark invoice as paid
     */
    suspend fun markAsPaid(
        id: UUID,
        paymentDate: LocalDate,
        paymentMethod: String
    ): Result<Invoice>

    /**
     * Stream invoice updates (real-time)
     */
    fun observeInvoices(): Flow<InvoiceEvent>
}
```

**Data Models:**
```kotlin
@Serializable
data class Invoice(
    val id: UUID,
    val invoiceNumber: String,
    val clientId: UUID,
    val clientName: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: BigDecimal,
    val vatAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val status: InvoiceStatus,
    val items: List<InvoiceItem>,
    val peppolId: String? = null,
    val paymentLink: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class InvoiceItem(
    val id: UUID,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal,
    val lineTotal: BigDecimal,
    val vatAmount: BigDecimal
)

@Serializable
enum class InvoiceStatus {
    DRAFT, SENT, VIEWED, PAID, OVERDUE, CANCELLED
}

@Serializable
data class CreateInvoiceRequest(
    val clientId: UUID,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val items: List<CreateInvoiceItemRequest>,
    val notes: String? = null,
    val termsAndConditions: String? = null
)

@Serializable
data class CreateInvoiceItemRequest(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal  // 21.00, 12.00, or 6.00 for Belgium
)
```

---

### ExpenseService

**Package:** `ai.dokus.foundation.apispec.ExpenseApi`

```kotlin
interface ExpenseApi : RPC {
    /**
     * Get all expenses for current tenant
     */
    suspend fun getExpenses(
        category: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        limit: Int = 100
    ): Result<List<Expense>>

    /**
     * Get single expense by ID
     */
    suspend fun getExpense(id: UUID): Result<Expense>

    /**
     * Create new expense
     */
    suspend fun createExpense(
        request: CreateExpenseRequest
    ): Result<Expense>

    /**
     * Update existing expense
     */
    suspend fun updateExpense(
        id: UUID,
        request: UpdateExpenseRequest
    ): Result<Expense>

    /**
     * Delete expense
     */
    suspend fun deleteExpense(id: UUID): Result<Unit>

    /**
     * Upload receipt for expense
     */
    suspend fun uploadReceipt(
        expenseId: UUID,
        receipt: ByteArray,
        filename: String
    ): Result<String>  // Returns receipt URL
}
```

**Data Models:**
```kotlin
@Serializable
data class Expense(
    val id: UUID,
    val date: LocalDate,
    val merchant: String,
    val amount: BigDecimal,
    val vatAmount: BigDecimal?,
    val category: ExpenseCategory,
    val description: String?,
    val receiptUrl: String?,
    val isDeductible: Boolean,
    val deductiblePercentage: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class ExpenseCategory {
    SOFTWARE, HARDWARE, TRAVEL, OFFICE,
    MEALS, MARKETING, PROFESSIONAL_SERVICES, OTHER
}

@Serializable
data class CreateExpenseRequest(
    val date: LocalDate,
    val merchant: String,
    val amount: BigDecimal,
    val vatAmount: BigDecimal? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val paymentMethod: String? = null
)
```

---

### ClientService

**Package:** `ai.dokus.foundation.apispec.ClientApi`

```kotlin
interface ClientApi : RPC {
    /**
     * Get all clients for current tenant
     */
    suspend fun getClients(
        isActive: Boolean? = null
    ): Result<List<Client>>

    /**
     * Get single client by ID
     */
    suspend fun getClient(id: UUID): Result<Client>

    /**
     * Create new client
     */
    suspend fun createClient(
        request: CreateClientRequest
    ): Result<Client>

    /**
     * Update existing client
     */
    suspend fun updateClient(
        id: UUID,
        request: UpdateClientRequest
    ): Result<Client>

    /**
     * Deactivate client (soft delete)
     */
    suspend fun deactivateClient(id: UUID): Result<Unit>
}
```

**Data Models:**
```kotlin
@Serializable
data class Client(
    val id: UUID,
    val name: String,
    val email: String?,
    val vatNumber: String?,
    val address: Address?,
    val contactPerson: String?,
    val phone: String?,
    val peppolId: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class Address(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val postalCode: String,
    val country: String  // ISO 3166-1 alpha-2
)

@Serializable
data class CreateClientRequest(
    val name: String,
    val email: String? = null,
    val vatNumber: String? = null,
    val address: Address? = null,
    val contactPerson: String? = null,
    val phone: String? = null
)
```

---

## Error Handling

### Result Type

All RPC methods return `Result<T>` which can be either `Success` or `Failure`.

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val exception: DokusException) : Result<Nothing>()
}
```

### Exception Types

```kotlin
sealed class DokusException : Exception() {
    // Authentication errors
    data class Unauthorized(
        override val message: String = "Unauthorized"
    ) : DokusException()

    data class InvalidCredentials(
        override val message: String = "Invalid email or password"
    ) : DokusException()

    // Validation errors
    data class ValidationError(
        override val message: String,
        val field: String? = null
    ) : DokusException()

    // Business logic errors
    data class InvoiceAlreadySent(
        override val message: String = "Invoice has already been sent"
    ) : DokusException()

    data class InsufficientPermissions(
        override val message: String = "Insufficient permissions"
    ) : DokusException()

    // System errors
    data class NetworkError(
        override val message: String = "Network error occurred"
    ) : DokusException()

    data class ServerError(
        override val message: String = "Server error occurred"
    ) : DokusException()
}
```

### Error Handling Example

```kotlin
val result = invoiceService.createInvoice(request)

when (result) {
    is Result.Success -> {
        val invoice = result.data
        // Handle success
        println("Invoice created: ${invoice.invoiceNumber}")
    }

    is Result.Failure -> {
        when (val error = result.exception) {
            is DokusException.ValidationError -> {
                // Show validation error to user
                println("Validation error: ${error.message}")
            }

            is DokusException.Unauthorized -> {
                // Redirect to login
                navigateToLogin()
            }

            is DokusException.NetworkError -> {
                // Show retry option
                showRetryDialog()
            }

            else -> {
                // Generic error handling
                showErrorMessage(error.message)
            }
        }
    }
}
```

---

## Client Setup

### Kotlin Multiplatform Client

```kotlin
// Create RPC client with authentication
class ApiClient(
    private val secureStorage: SecureStorage
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val rpcClient = RPC.Client {
        url("https://api.dokus.ai")

        httpClient(httpClient)

        streamScoped {
            withRPCContext {
                // Add authorization header
                val accessToken = secureStorage.getAccessToken()
                if (accessToken != null) {
                    put("Authorization", "Bearer $accessToken")
                }

                // Add tenant ID if available
                val tenantId = secureStorage.getTenantId()
                if (tenantId != null) {
                    put("X-Tenant-ID", tenantId.toString())
                }
            }
        }
    }

    // Service instances
    val authService: AuthApi by lazy {
        rpcClient.withService<AuthApi>()
    }

    val invoiceService: InvoiceApi by lazy {
        rpcClient.withService<InvoiceApi>()
    }

    val expenseService: ExpenseApi by lazy {
        rpcClient.withService<ExpenseApi>()
    }

    val clientService: ClientApi by lazy {
        rpcClient.withService<ClientApi>()
    }
}
```

### Usage in ViewModel

```kotlin
class InvoiceListViewModel(
    private val apiClient: ApiClient
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    fun loadInvoices() = viewModelScope.launch {
        _state.value = UiState.Loading

        val result = apiClient.invoiceService.getInvoices()

        _state.value = when (result) {
            is Result.Success -> UiState.Success(result.data)
            is Result.Failure -> UiState.Error(result.exception.message)
        }
    }
}
```

---

## Common Patterns

### Pagination

```kotlin
suspend fun loadInvoices(page: Int, pageSize: Int = 50) {
    val result = invoiceService.getInvoices(
        limit = pageSize,
        offset = page * pageSize
    )
    // Handle result
}
```

### Filtering

```kotlin
suspend fun loadPaidInvoices() {
    val result = invoiceService.getInvoices(
        status = InvoiceStatus.PAID
    )
    // Handle result
}
```

### Real-time Updates

```kotlin
fun observeInvoiceChanges() = viewModelScope.launch {
    invoiceService.observeInvoices()
        .collect { event ->
            when (event) {
                is InvoiceEvent.Created -> handleNewInvoice(event.invoice)
                is InvoiceEvent.Updated -> handleUpdatedInvoice(event.invoice)
                is InvoiceEvent.Deleted -> handleDeletedInvoice(event.invoiceId)
            }
        }
}
```

### Retry Logic

```kotlin
suspend fun <T> retryOnNetworkError(
    maxRetries: Int = 3,
    block: suspend () -> Result<T>
): Result<T> {
    repeat(maxRetries) { attempt ->
        val result = block()

        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.exception !is DokusException.NetworkError) {
                    return result
                }
                if (attempt == maxRetries - 1) {
                    return result
                }
                delay((attempt + 1) * 1000L)  // Exponential backoff
            }
        }
    }

    return Result.Failure(DokusException.NetworkError())
}

// Usage
val result = retryOnNetworkError {
    invoiceService.getInvoices()
}
```

---

## Related Documentation

- [Architecture](./ARCHITECTURE.md) - System architecture
- [Database Schema](./DATABASE.md) - Database design
- [Security](./SECURITY.md) - Authentication & authorization
- [Setup Guide](./SETUP.md) - Development setup

---

**Last Updated:** October 2025
