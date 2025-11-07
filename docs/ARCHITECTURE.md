# System Architecture

**Last Updated:** October 2025
**Status:** Production Architecture

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Technology Stack](#technology-stack)
4. [Module Structure](#module-structure)
5. [Client Architecture](#client-architecture)
6. [Backend Services Architecture](#backend-services-architecture)
7. [Data Flow](#data-flow)
8. [Communication Patterns](#communication-patterns)
9. [Security Architecture](#security-architecture)
10. [Scalability Strategy](#scalability-strategy)

---

## Overview

### Vision

**Make financial compliance invisible for European developers and freelancers.**

Dokus is not "another accounting tool"—it's **anti-accounting software**. A system so automated and intelligent that developers never think about finances except when making business decisions. This vision drives every architectural decision: automation over manual processes, intelligence over complexity, invisibility over interfaces.

### Architecture Goals

Dokus follows a **microservices architecture** with a **Kotlin Multiplatform client** that runs on Android, iOS, Desktop, and Web. The system is designed for:

- **Multi-tenancy**: Each customer (tenant) has isolated data
- **Type-safety**: Kotlin across all layers
- **Scalability**: Independent service scaling
- **Maintainability**: Clear separation of concerns
- **Performance**: Async operations, efficient caching
- **Automation**: Minimize manual intervention at every layer

###Architecture Principles

1. **API-First Design**: All functionality exposed via type-safe RPC
2. **Event-Driven**: Audit logging for all state changes
3. **Security by Default**: Multi-tenant isolation at every layer
4. **Fail-Safe**: Graceful degradation and comprehensive error handling
5. **Observable**: Metrics, logging, and tracing built-in

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS                            │
├───────────────┬──────────────┬──────────────┬────────────────────┤
│  Web (WASM)   │  iOS Native  │   Android    │   Desktop (JVM)    │
│ Compose MP    │ Compose MP   │  Compose MP  │    Compose MP      │
└───────┬───────┴──────┬───────┴──────┬───────┴────────┬───────────┘
        │              │              │                │
        └──────────────┼──────────────┼────────────────┘
                       │              │
                ┌──────▼──────────────▼──────┐
                │   KotlinX RPC Protocol     │
                │   (Type-Safe Communication)│
                └──────┬─────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌───▼─────┐  ┌───▼──────┐
    │  Auth   │   │Database │  │Invoicing │
    │ Service │   │ Service │  │ Service  │
    │ (Ktor)  │   │ (Ktor)  │  │ (Ktor)   │
    └────┬────┘   └────┬────┘  └────┬─────┘
         │             │             │
         │        ┌────▼──────┐  ┌──▼────────┐
         │        │ Expense   │  │  Payment  │
         │        │ Service   │  │  Service  │
         │        │ (Ktor)    │  │  (Ktor)   │
         │        └────┬──────┘  └──┬────────┘
         │             │             │
         └─────────────┼─────────────┘
                       │
           ┌───────────┼───────────┬──────────────┐
           │           │           │              │
      ┌────▼────┐ ┌───▼──────┐ ┌─▼──────┐  ┌───▼───────┐
      │PostgreSQL│ │  Redis   │ │  S3    │  │ Prometheus│
      │   17     │ │    8     │ │Storage │  │  Metrics  │
      └──────────┘ └──────────┘ └────────┘  └───────────┘
```

---

## Technology Stack

### Client Applications (Kotlin Multiplatform)

**Framework:** Kotlin Multiplatform 2.2.20
- Single codebase for all platforms
- Shared business logic and UI components
- Platform-specific optimizations where needed

**UI Framework:** Compose Multiplatform 1.9.1
- Declarative UI across platforms
- Material Design 3 support
- Consistent UX on all platforms

**Navigation:** Jetpack Compose Navigation 2.9.1
- Type-safe navigation
- Deep linking support
- Shared navigation logic

**Dependency Injection:** Koin 4.1.1
- Lightweight DI framework
- Multiplatform support
- Easy testing and mocking

**Logging:** Kermit 2.0.8
- Multiplatform logging
- Platform-native outputs
- Configurable log levels

### Backend Services

**Framework:** Ktor 3.3.1
- Asynchronous, non-blocking
- Lightweight and fast
- Kotlin-native with coroutines

**RPC Framework:** KotlinX RPC 0.10.1
- Type-safe client-server communication
- Shared API definitions
- Automatic serialization

**ORM:** Exposed 0.61.0
- Type-safe SQL DSL
- Kotlin-first design
- Prevents SQL injection

**Database:** PostgreSQL 17
- ACID compliance
- NUMERIC for exact calculations
- JSONB for flexible data
- Excellent performance

**Cache:** Redis 8
- Session storage
- Rate limiting
- Temporary data
- Message queues

**Migration Tool:** Flyway
- Versioned migrations
- Automatic execution
- Rollback support

**Connection Pool:** HikariCP
- High-performance
- Configurable pool size
- Connection leak detection

---

## Module Structure

### Project Organization

```
dokus/
├── composeApp/                 # Main KMP application
│   └── src/
│       ├── commonMain/         # Shared code (90%)
│       ├── androidMain/        # Android-specific
│       ├── iosMain/            # iOS-specific
│       ├── desktopMain/        # Desktop-specific
│       └── wasmJsMain/         # Web-specific
│
├── foundation/                 # Foundation modules (shared)
│   ├── design-system/          # UI components & theming
│   ├── app-common/             # ViewModels, state management
│   ├── platform/               # Platform abstractions
│   ├── navigation/             # Type-safe navigation
│   ├── domain/                 # Domain models & use cases
│   ├── apispec/                # RPC API specifications
│   ├── database/               # Database schemas & migrations
│   ├── ktor-common/            # Shared Ktor configuration
│   └── sstorage/               # Secure storage abstraction
│
├── features/                   # Feature modules
│   ├── auth/
│   │   ├── backend/            # Auth service (Ktor)
│   │   ├── presentation/       # Auth UI (Compose MP)
│   │   └── data/               # Auth data layer
│   ├── invoicing/backend/      # Invoicing service
│   ├── expense/backend/        # Expense service
│   ├── payment/backend/        # Payment service
│   └── reporting/backend/      # Reporting service
│
└── build-logic/                # Custom Gradle plugins
    └── convention/             # Build conventions
```

### Module Responsibilities

**composeApp**: Application entry point, platform-specific configuration

**foundation/design-system**: Reusable UI components, Material theming, icons

**foundation/app-common**: ViewModels, state management, common business logic

**foundation/platform**: Platform-specific implementations (logging, storage, etc.)

**foundation/navigation**: Navigation graphs, deep linking, type-safe destinations

**foundation/domain**: Domain models, use cases, business rules

**foundation/domain/rpc**: KotlinX RPC service definitions (shared contract)

**foundation/database**: Exposed table definitions, Flyway migrations

**foundation/sstorage**: Platform-agnostic secure storage (keychain, encrypted prefs)

**features/*/backend**: Microservices implementing business logic

**features/*/presentation**: Feature-specific UI screens and components

**features/*/data**: Data repositories, API clients

---

## Client Architecture

### Layers

```
┌─────────────────────────────────────┐
│          Presentation Layer         │
│  (Compose UI, ViewModels, State)    │
├─────────────────────────────────────┤
│           Domain Layer              │
│  (Use Cases, Business Rules)        │
├─────────────────────────────────────┤
│            Data Layer               │
│  (Repositories, RPC Clients, Cache) │
├─────────────────────────────────────┤
│         Platform Layer              │
│  (Secure Storage, Logging, Config)  │
└─────────────────────────────────────┘
```

### State Management

**Pattern:** Unidirectional Data Flow (UDF) with MVI

```kotlin
// State
data class InvoiceListState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: DokusException? = null
)

// ViewModel
class InvoiceListViewModel : ViewModel() {
    private val _state = MutableStateFlow(InvoiceListState())
    val state: StateFlow<InvoiceListState> = _state.asStateFlow()

    fun loadInvoices() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }

        runCatching {
            invoiceRepository.getInvoices()
        }.onSuccess { invoices ->
            _state.update { it.copy(invoices = invoices, isLoading = false) }
        }.onFailure { error ->
            _state.update { it.copy(error = error.asDokusException, isLoading = false) }
        }
    }
}

// UI
@Composable
fun InvoiceListScreen(viewModel: InvoiceListViewModel) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorMessage(state.error)
        else -> InvoiceList(state.invoices)
    }
}
```

### Navigation

**Type-Safe Navigation with Sealed Classes:**

```kotlin
// Destinations
sealed class CoreDestination {
    @Serializable
    data object Splash : CoreDestination()

    @Serializable
    data object Home : CoreDestination()
}

sealed class AuthDestination {
    @Serializable
    data object Login : AuthDestination()

    @Serializable
    data class Register(val inviteCode: String? = null) : AuthDestination()
}

// Navigation
navController.navigateTo(AuthDestination.Login)
navController.replace(CoreDestination.Home)
```

---

## Backend Services Architecture

### Service Pattern

Each backend service follows this structure:

```
service/
├── src/main/kotlin/
│   ├── Application.kt          # Service entry point
│   ├── plugins/                # Ktor plugins
│   │   ├── Security.kt         # JWT auth, CORS
│   │   ├── Monitoring.kt       # Metrics, health checks
│   │   └── Serialization.kt    # JSON serialization
│   ├── api/                    # RPC API implementations
│   │   └── InvoiceApiImpl.kt
│   ├── services/               # Business logic
│   │   └── InvoiceService.kt
│   └── repositories/           # Data access
│       └── InvoiceRepository.kt
└── src/main/resources/
    └── application.conf        # Ktor configuration
```

### Service Communication

**Synchronous:** KotlinX RPC over HTTP
```kotlin
// API Specification (shared)
interface InvoiceApi : RPC {
    suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice>
    suspend fun getInvoice(id: UUID): Result<Invoice>
}

// Service Implementation
class InvoiceApiImpl(
    private val context: RPCServiceContext,
    private val invoiceService: InvoiceService
) : InvoiceApi {
    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> {
        val tenantId = context.getTenantId()
        return runCatching {
            invoiceService.create(tenantId, request)
        }
    }
}

// Client Usage
val invoiceApi = rpcClient.withService<InvoiceApi>()
val result = invoiceApi.createInvoice(request)
```

**Asynchronous:** Redis pub/sub (for events)
```kotlin
// Publish event
redisPublisher.publish("invoices", InvoiceCreatedEvent(invoiceId))

// Subscribe to events
redisSubscriber.subscribe("invoices") { event ->
    when (event) {
        is InvoiceCreatedEvent -> handleInvoiceCreated(event)
    }
}
```

---

## Data Flow

### Create Invoice Flow

```
1. User fills invoice form (UI)
   ↓
2. ViewModel validates input
   ↓
3. ViewModel calls InvoiceRepository.create()
   ↓
4. Repository makes RPC call to Invoicing Service
   ↓
5. Invoicing Service validates business rules
   ↓
6. Service calls InvoiceRepository.save()
   ↓
7. Repository executes SQL INSERT (with tenant_id)
   ↓
8. Audit log entry created
   ↓
9. Success response returned to client
   ↓
10. UI updates with new invoice
```

### Authentication Flow

```
1. User enters email + password
   ↓
2. Client calls AuthApi.login()
   ↓
3. Auth Service validates credentials
   ↓
4. Generate JWT access token (15 min)
   ↓
5. Generate JWT refresh token (7 days)
   ↓
6. Store refresh token in Redis
   ↓
7. Return both tokens to client
   ↓
8. Client stores tokens in secure storage
   ↓
9. Client includes access token in subsequent requests
```

---

## Communication Patterns

### RPC (Request/Response)

Used for: Direct queries, CRUD operations

```kotlin
suspend fun getInvoice(id: UUID): Result<Invoice>
suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice>
```

### Event Streaming

Used for: Real-time updates, notifications

```kotlin
fun observeInvoices(tenantId: UUID): Flow<Invoice>
fun observePayments(invoiceId: UUID): Flow<Payment>
```

---

## Security Architecture

See [SECURITY.md](./SECURITY.md) for detailed security documentation.

**Key Points:**
- Multi-tenant isolation at database level
- JWT with short-lived access tokens
- Refresh token rotation
- All sensitive data encrypted at rest
- Comprehensive audit logging
- Rate limiting on all endpoints

---

## Scalability Strategy

### Current (0-1,000 Users)

- Single PostgreSQL instance
- 2-3 backend service containers
- Redis for caching/sessions
- **Cost:** ~€200/month
- **Capacity:** 10K requests/day

### Phase 1 (1,000-5,000 Users)

- PostgreSQL upgrade (more CPU/RAM)
- 5-10 backend containers
- Read replicas for reporting
- **Cost:** ~€500/month
- **Capacity:** 100K requests/day

### Phase 2 (5,000+ Users)

- Multi-region deployment
- Database sharding by tenant_id
- Kubernetes orchestration
- CDN for static assets
- **Cost:** ~€2,000/month
- **Capacity:** 1M+ requests/day

---

## Performance Targets

### Response Times

| Operation | Target | Maximum |
|-----------|--------|---------|
| GET invoice | <100ms | <300ms |
| POST invoice | <200ms | <500ms |
| Dashboard load | <150ms | <400ms |
| Peppol send | <2s | <5s |

### Database

- Query time: <50ms for simple SELECTs
- Connection pool: 50-100 connections
- Index all foreign keys and query filters

---

## Monitoring & Observability

### Metrics

- Request rate (requests/minute)
- Error rate (%)
- Response times (p50, p95, p99)
- Database query performance
- Cache hit rate

### Logging

- Structured logging (JSON)
- Log levels: DEBUG, INFO, WARN, ERROR
- Centralized logging via Kermit
- Platform-specific outputs (Logcat, NSLog, Console)

### Health Checks

```
GET /health       # Basic health check
GET /metrics      # Prometheus metrics
GET /health/ready # Readiness probe
GET /health/live  # Liveness probe
```

---

## Design Decisions

### Why Kotlin Multiplatform?

✅ Single codebase for all platforms
✅ Share business logic and UI
✅ Native performance
✅ Type-safety across the stack
✅ Modern, concise syntax

### Why Microservices?

✅ Independent scaling
✅ Technology flexibility
✅ Team autonomy
✅ Fault isolation
✅ Easier to maintain and deploy

### Why PostgreSQL?

✅ ACID compliance (critical for finance)
✅ NUMERIC type for exact calculations
✅ Excellent performance and scalability
✅ Rich feature set (JSONB, full-text search)
✅ Battle-tested reliability

### Why Ktor over Spring Boot?

✅ Lightweight (faster startup)
✅ Kotlin-native (better DSL)
✅ Async by default (coroutines)
✅ Smaller footprint
✅ Easier to reason about

---

## Related Documentation

- [Setup Guide](./SETUP.md) - Getting started
- [Database Schema](./DATABASE.md) - Database design
- [API Reference](./API.md) - RPC API documentation
- [Security](./SECURITY.md) - Security architecture
- [Deployment](./DEPLOYMENT.md) - Deployment strategies

---

**Last Updated:** October 2025
