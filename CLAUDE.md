# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dokus** is a Kotlin Multiplatform (KMP) financial management application targeting Android, iOS, Desktop (JVM), and Web (WASM). The codebase uses Compose Multiplatform for UI and follows a feature-modular architecture.

### Vision

**"Make financial compliance invisible for European developers and freelancers"**

Dokus is "anti-accounting software" - a system so automated and intelligent that developers never think about finances except when making business decisions. It focuses on Belgian IT freelancers with Belgium's 2026 Peppol e-invoicing mandate as the key market driver.

## Build Commands

### Running the Application
```bash
# Web development server with hot reload
./gradlew wasmJsBrowserRun -t

# Android debug build
./gradlew :composeApp:assembleDebug

# Desktop application (macOS/Windows/Linux)
./gradlew :composeApp:packageReleaseDmg  # macOS
./gradlew :composeApp:packageReleaseMsi  # Windows
./gradlew :composeApp:packageReleaseDeb  # Linux
```

### Testing
```bash
# Run all tests across platforms
./gradlew allTests

# Run specific platform tests
./gradlew testDebugUnitTest     # Android
./gradlew desktopTest           # Desktop/JVM
./gradlew iosSimulatorArm64Test # iOS Simulator (ARM)

# Full verification (build + test)
./gradlew check
```

### Development
```bash
# Clean build artifacts
./gradlew clean

# Build all modules
./gradlew build
```

## Architecture

### Module Structure
The project follows a feature-based modular architecture:

- **`/composeApp`**: Main application entry point with platform-specific configurations
- **`/application`**: Feature modules and core infrastructure
  - Feature modules: `onboarding`, `home`, `dashboard`, `contacts`, `cashflow`, `simulation`, `inventory`, `banking`, `profile`
  - Core modules: `core`, `repository`, `navigation`
- **`/foundation`**: Foundation modules shared across all features
  - Modules: `design-system`, `app-common`, `platform`, `navigation`, `domain`, `database`, `sstorage`, `ktor-common`
  - RPC interfaces: Located in `foundation/domain/rpc/` (KotlinX RPC service definitions)
- **`/features`**: Backend microservices (auth, invoicing, expense, payment, reporting)

### Key Architectural Patterns
- **Dependency Injection**: Koin throughout the application
- **Navigation**: JetBrains Compose Navigation for type-safe navigation
- **Logging**: Kermit multiplatform logging via Logger wrapper in `platform` module
- **Platform-specific code**: `expect`/`actual` declarations in the `platform` module
- **Source sets**: Each module has `commonMain`, `androidMain`, `iosMain`, `desktopMain`, and `wasmJsMain`

### Package Naming
- **Foundation modules**: `ai.dokus.foundation.{module}` (e.g., `ai.dokus.foundation.ui`, `ai.dokus.foundation.domain`)
- **Application modules**: `ai.dokus.app.{module}` (e.g., `ai.dokus.app.onboarding`, `ai.dokus.app.repository`)
- **Backend modules**: `ai.dokus.backend.{service}` (when enabled)

## Technology Stack

- **Kotlin**: 2.2.10
- **Compose Multiplatform**: 1.9.0
- **Ktor**: 3.3.0 (HTTP client/server)
- **Koin**: 4.0.0 (Dependency Injection)
- **Compose Navigation**: 2.9.0 (Navigation)
- **Kermit**: 2.0.4 (Logging)
- **BuildKonfig**: 0.15.2 (Build Configuration)
- **kotlinx.serialization**: 1.9.0

## Key Files & Entry Points

- Main application: `/composeApp/src/commonMain/kotlin/ai/dokus/app/app/App.kt`
- Version catalog: `/gradle/libs.versions.toml`
- Module configuration: `/settings.gradle.kts`
- Custom build plugins: `/build-logic/convention/`
- Server endpoints: `/foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/configuration/Constants.kt`
- Build configuration: `/foundation/platform/build.gradle.kts` (BuildKonfig setup)

## Development Guidelines

1. **Dependencies**: Always use the version catalog (`libs.*` references) instead of hardcoding versions
2. **Module references**: Use type-safe project accessors (e.g., `projects.application.core`)
3. **Platform code**: Place platform-specific implementations in appropriate source sets
4. **Testing**: Limited test coverage exists; server tests are in `/server/{module}/src/test/kotlin/`
5. **Server modules**: Currently commented out in settings.gradle.kts; uncomment if server work is needed

## Environment Configuration

The project has been simplified to support **two environments**:
- **Cloud** (production/cloud deployment)
- **Local** (local development)

### Frontend Configuration (BuildKonfig)

The client uses **BuildKonfig** to generate compile-time configuration for different environments. Configuration is set in `/foundation/platform/build.gradle.kts`.

**Cloud/Production (default):**
```bash
./gradlew build
# API_HOST: app.dokus.tech
# API_PORT: 443 (HTTPS)
# API_IS_LOCAL: false
```

**Local Development:**
```bash
./gradlew build -PENV=local
# API_HOST: 127.0.0.1
# API_PORT: 8000
# API_IS_LOCAL: true
```

**Android Emulator:**
```bash
./gradlew build -PENV=localAndroid
# API_HOST: 10.0.2.2 (emulator's localhost)
# API_PORT: 8000
# API_IS_LOCAL: true
```

**Custom Configuration:**
```bash
./gradlew build -PAPI_HOST=staging.example.com -PAPI_PORT=8080
# Or combine with debug logging:
./gradlew build -PENV=local -PDEBUG=true
```

### Backend Configuration (HOCON)

The backend services use **HOCON** (Typesafe Config) with a two-tier configuration:

**1. Base Configuration (`application.conf`):**
- Default values suitable for local development
- Memory caching (default)
- All values can be overridden via environment variables

**2. Cloud Configuration (`application-cloud.conf`):**
- Extends base configuration
- Production-optimized settings
- Redis caching (requires `CACHE_TYPE=redis`)
- Requires environment variables for sensitive data

**Configuration Files:**
- `/features/{service}/backend/src/main/resources/application.conf` - Base defaults
- `/features/auth/backend/src/main/resources/application-cloud.conf` - Cloud/production overrides
- `/deployment/.env.example` - Environment variable template
- `/deployment/docker-compose.yml` - Docker orchestration

### Backend Deployment

**Cloud/Production:**
```bash
cd deployment
cp .env.example .env
# Edit .env with production values
./dokus.sh
```

**Local Development:**
```bash
./dev.sh start
```

**CRITICAL Environment Variables (Cloud):**
- `CACHE_TYPE=redis` - Cache implementation (redis or memory)
- `DB_PASSWORD` - Database password
- `REDIS_PASSWORD` - Redis password
- `JWT_SECRET` - JWT signing secret (64+ characters)
- `CORS_ALLOWED_HOSTS` - Allowed CORS origins
- See `/deployment/.env.example` for complete list

For detailed configuration guide, see `/deployment/CONFIGURATION_GUIDE.md`.

### Accessing Configuration

Configuration is available via `BuildConfig` object in the `platform` module:
```kotlin
import ai.dokus.foundation.platform.BuildConfig

// Access values
val host = BuildConfig.API_HOST        // String: hostname/IP
val port = BuildConfig.API_PORT        // Int: port number (443 for production HTTPS)
val isLocal = BuildConfig.API_IS_LOCAL // Boolean: true for local dev
val isDebug = BuildConfig.DEBUG        // Boolean: enables debug logging
```

The `ServerEndpoint` object automatically uses these values.

## Common Workflows

### Adding a new feature module
1. Create module in `/application/{feature-name}`
2. Add to `settings.gradle.kts`
3. Follow existing module structure with platform source sets
4. Register in Koin DI modules

### Working with platform-specific code
1. Define interface with `expect` in `commonMain`
2. Provide `actual` implementations in platform source sets
3. Use the `platform` module for shared platform abstractions

### Debugging build issues
1. Check module inclusion in `settings.gradle.kts`
2. Verify dependencies in module's `build.gradle.kts`
3. Ensure version catalog entries exist for new dependencies
4. Run `./gradlew clean` before rebuilding

### Using Logging
The project uses Kermit for multiplatform logging, wrapped in a custom `Logger` class in the `platform` module.

**Creating a logger:**
```kotlin
// Using class name as tag
private val logger = Logger.forClass<MyViewModel>()

// Using custom tag
private val logger = Logger.withTag("CustomTag")
```

**Logging messages:**
```kotlin
logger.v { "Verbose message" }  // Verbose (only in debug builds)
logger.d { "Debug message" }    // Debug (only in debug builds)
logger.i { "Info message" }     // Info
logger.w { "Warning message" }  // Warning
logger.e { "Error message" }    // Error
logger.a { "Assert/WTF" }       // Assert (critical errors)

// With exceptions
logger.e(exception) { "Error occurred" }
```

**Build configuration:**
- Debug logging is controlled via BuildKonfig's `DEBUG` flag
- Production builds: `./gradlew build` (DEBUG=false, Info+ logs only)
- Development builds: `./gradlew build -PDEBUG=true` (DEBUG=true, all logs including Verbose/Debug)
- The DEBUG flag is generated at compile time via the BuildKonfig plugin

**Best practices:**
- Use lazy message evaluation (lambdas) to avoid string construction when logging is disabled
- Log levels are automatically configured: Verbose/Debug when DEBUG=true, Info+ when DEBUG=false
- HTTP requests/responses are automatically logged via the `LoggingPlugin` in the repository layer
- Each platform uses native logging: Logcat (Android), NSLog (iOS), Console (Desktop/Web)

**Example usage in ViewModels:**
```kotlin
class LoginViewModel : BaseViewModel<State>(State.Idle), KoinComponent {
    private val logger = Logger.forClass<LoginViewModel>()

    fun login(email: String, password: String) = scope.launch {
        logger.d { "Login attempt started" }
        // ... login logic
        logger.i { "Login successful" }
    }
}
```

## Critical Database & Security Rules

### Multi-Tenant Security (CRITICAL!)

**Rule #1: ALWAYS filter by `tenant_id` in every query**

This is the most critical security rule. Every database query MUST include tenant_id filtering.

```kotlin
// ❌ SECURITY VULNERABILITY - NEVER DO THIS
fun getInvoice(invoiceId: UUID) = transaction {
    Invoices.select { Invoices.id eq invoiceId }
}

// ✅ CORRECT - Always filter by tenant
fun getInvoice(invoiceId: UUID, tenantId: UUID) = transaction {
    Invoices.select {
        (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
    }
}
```

### Financial Data Handling

**Rule #2: Use NUMERIC, NEVER FLOAT for money**

```kotlin
// ❌ WRONG - Causes rounding errors
val price = 19.99f
val total = price * 3f  // 59.970005 instead of 59.97!

// ✅ CORRECT - Exact decimal arithmetic
val price = BigDecimal("19.99")
val total = price * BigDecimal("3")  // Exactly 59.97

// In Exposed schema
val totalAmount = decimal("total_amount", 12, 2)  // NUMERIC(12,2)
```

**Rounding Rules:**
```kotlin
import java.math.RoundingMode

val amount = BigDecimal("10.555")
val rounded = amount.setScale(2, RoundingMode.HALF_UP)  // 10.56
```

### RPC Service Design with KotlinX RPC

All RPC interfaces are marked with `@Rpc` annotation and located in `foundation/domain/rpc/`:

```kotlin
@Rpc
interface InvoiceApi {
    suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice>
    suspend fun getInvoice(id: InvoiceId): Result<Invoice>
    fun watchInvoices(tenantId: TenantId): Flow<Invoice>
}
```

**Sealed classes for events must include @SerialName:**
```kotlin
@Serializable
sealed class ClientEvent {
    @Serializable
    @SerialName("ClientEvent.ClientCreated")
    data class ClientCreated(val client: Client) : ClientEvent()
}
```

### Audit Logging

Every financial operation MUST be logged:

```kotlin
suspend fun logAudit(
    tenantId: UUID,
    userId: UUID?,
    action: String,
    entityType: String,
    entityId: UUID,
    oldValues: JsonObject?,
    newValues: JsonObject?
) {
    // Log to audit_logs table (immutable, 7-year retention)
}
```

### Transaction Management

```kotlin
// ✅ CORRECT - Entire operation is atomic
suspend fun createInvoice(...) = transaction {
    val invoiceId = Invoices.insertAndGetId { ... }
    items.forEach { InvoiceItems.insert { ... } }
    AuditLogs.insert { ... }
    invoiceId  // All succeed or all fail
}
```

### Performance Best Practices

1. **Avoid N+1 queries** - Use joins instead of loops
2. **Batch operations** - Use `batchInsert` for multiple rows
3. **Index properly** - Index all foreign keys and query filters
4. **Connection pooling** - Configure HikariCP with appropriate pool size

See `docs/DATABASE.md` and `docs/SECURITY.md` for complete details.

## Standard Workflow
1. First think through the problem, read the codebase for relevant files, and write a plan to tasks/todo.md.
2. The plan should have a list of todo items that you can
   check off as you complete them
3. Before you begin working, check in with me and I will verify the plan.
4. Then, begin working on the todo items, marking them as complete as you go.
5. Please every step of the way just give me a high level explanation of what changes you made
6. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
7. Finally, add a review section to the todo.md file with a summary of the changes you made and any other relevant information.