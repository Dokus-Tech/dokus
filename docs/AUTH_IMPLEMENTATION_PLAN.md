# Authentication System Implementation Plan

## Executive Summary

This document outlines the complete implementation of the authentication system for Dokus, a Kotlin Multiplatform financial management application. The system uses JWT tokens, KotlinX RPC for client-server communication, and follows clean architecture principles.

## Current Status Analysis

### ✅ Implemented Components

**Foundation Layer:**
- ✅ `TokenManager` interface with refresh mechanism
- ✅ `AuthManager` for authentication events
- ✅ JWT data models (`JwtClaims`, `LoginRequest`, `LoginResponse`, etc.)
- ✅ Token storage with platform-specific secure storage
- ✅ JWT decoder (claims extraction works)
- ✅ Token refresh with mutex protection (prevents race conditions)
- ✅ Auth event broadcasting

**Backend:**
- ✅ User database schema with multi-tenant support
- ✅ `UserService` with password verification (bcrypt)
- ✅ Refresh tokens table
- ✅ Password hashing and verification
- ✅ User CRUD operations
- ✅ Ktor server setup with KotlinX RPC

**UI/Navigation:**
- ✅ Bootstrap flow with auth checking
- ✅ Splash screen with navigation
- ✅ Login/Register screen skeletons

### ❌ Missing Critical Components

1. **JWT Token Validation** - `validateToken()` and `isExpired()` throw `NotImplementedError`
2. **Auth RPC API** - No RPC interface for authentication
3. **Auth Routes** - Backend routes are empty (`IdentityRoutes`, `UserRoutes`)
4. **Auth Repository** - Only has `isAuthenticated` flow, no actual methods
5. **Use Cases** - All empty stubs (Login, Logout, Register)
6. **ViewModels** - Login logic entirely commented out
7. **Backend Auth Config** - Authentication middleware not implemented
8. **Token Refresh Backend** - No backend endpoint for refresh
9. **Session Management** - No active session tracking

---

## Implementation Plan

## Phase 1: Fix JWT Token Validation (CRITICAL)

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/utils/JwtDecoder.kt`

**Problem:** Token validation throws `NotImplementedError` because it needs current time.

**Solution:**
```kotlin
import kotlinx.datetime.Clock

fun validateToken(token: String?): TokenStatus {
    if (token.isNullOrBlank()) return TokenStatus.INVALID

    val claims = decode(token) ?: return TokenStatus.INVALID
    val exp = claims.exp ?: return TokenStatus.INVALID

    // Use kotlinx-datetime for multiplatform time
    val currentTime = Clock.System.now().epochSeconds

    return when {
        exp < currentTime -> TokenStatus.EXPIRED
        exp - currentTime < REFRESH_THRESHOLD_SECONDS -> TokenStatus.REFRESH_NEEDED
        else -> TokenStatus.VALID
    }
}

fun isExpired(token: String?): Boolean {
    if (token.isNullOrBlank()) return true

    val claims = decode(token) ?: return true
    val exp = claims.exp ?: return true

    val currentTime = Clock.System.now().epochSeconds
    return exp < currentTime
}
```

**Dependencies:** `kotlinx-datetime` is already in the project.

---

## Phase 2: Create Auth RPC API

### File: `foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/rpc/AuthApi.kt`

**Create the RPC interface for authentication:**

```kotlin
package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.model.auth.*
import kotlinx.rpc.annotations.Rpc

@Rpc
interface AuthApi {
    /**
     * Authenticate user with email and password.
     * Returns JWT tokens on success.
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse>

    /**
     * Register a new user account.
     * Automatically logs in and returns tokens.
     */
    suspend fun register(request: RegisterRequest): Result<LoginResponse>

    /**
     * Refresh an expired access token using refresh token.
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse>

    /**
     * Logout user and revoke session.
     */
    suspend fun logout(request: LogoutRequest): Result<Unit>

    /**
     * Validate if current token is still valid.
     */
    suspend fun validateToken(): Result<Boolean>

    /**
     * Send password reset email.
     */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /**
     * Reset password with token from email.
     */
    suspend fun resetPassword(token: String, request: ResetPasswordRequest): Result<Unit>
}
```

---

## Phase 3: Implement Auth Repository

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/repository/AuthRepository.kt`

**Add complete authentication methods:**

```kotlin
package ai.dokus.app.auth.repository

import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.domain.rpc.AuthApi
import kotlinx.coroutines.flow.StateFlow
import ai.dokus.foundation.platform.Logger

class AuthRepository(
    private val tokenManager: TokenManagerMutable,
    private val authManager: AuthManagerMutable,
    private val authApi: AuthApi
) {
    private val logger = Logger.forClass<AuthRepository>()

    val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated

    init {
        // Set up token refresh callback
        tokenManager.onTokenRefreshNeeded = { refreshToken ->
            refreshTokenInternal(refreshToken)
        }
    }

    /**
     * Initialize auth repository - load stored tokens.
     */
    suspend fun initialize() {
        logger.d { "Initializing auth repository" }
        tokenManager.initialize()
    }

    /**
     * Login with email and password.
     */
    suspend fun login(email: String, password: String, rememberMe: Boolean = true): Result<Unit> {
        logger.d { "Login attempt for email: ${email.take(3)}***" }

        return try {
            val request = LoginRequest(
                email = email,
                password = password,
                rememberMe = rememberMe,
                deviceType = getDeviceType()
            )

            val result = authApi.login(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Login successful" }
                    tokenManager.saveTokens(response)
                    authManager.onLoginSuccess()
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.e(error) { "Login failed" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Login error" }
            Result.failure(e)
        }
    }

    /**
     * Register a new user account.
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        logger.d { "Registration attempt for email: ${email.take(3)}***" }

        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName
            )

            val result = authApi.register(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Registration successful, auto-logging in" }
                    tokenManager.saveTokens(response)
                    authManager.onLoginSuccess()
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.e(error) { "Registration failed" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Registration error" }
            Result.failure(e)
        }
    }

    /**
     * Logout current user.
     */
    suspend fun logout() {
        logger.d { "Logging out user" }

        try {
            val token = tokenManager.getValidAccessToken()
            if (token != null) {
                val request = LogoutRequest(sessionToken = token)
                authApi.logout(request)
            }
        } catch (e: Exception) {
            logger.w(e) { "Logout API call failed, clearing local tokens anyway" }
        }

        tokenManager.onAuthenticationFailed()
        authManager.onUserLogout()
    }

    /**
     * Request password reset email.
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        logger.d { "Password reset requested for: ${email.take(3)}***" }

        return try {
            authApi.requestPasswordReset(email)
        } catch (e: Exception) {
            logger.e(e) { "Password reset request failed" }
            Result.failure(e)
        }
    }

    /**
     * Reset password with token.
     */
    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> {
        logger.d { "Resetting password with token" }

        return try {
            val request = ResetPasswordRequest(newPassword = newPassword)
            authApi.resetPassword(resetToken, request)
        } catch (e: Exception) {
            logger.e(e) { "Password reset failed" }
            Result.failure(e)
        }
    }

    /**
     * Internal token refresh implementation.
     */
    private suspend fun refreshTokenInternal(refreshToken: String): LoginResponse? {
        logger.d { "Refreshing access token" }

        return try {
            val request = RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceType = getDeviceType()
            )

            val result = authApi.refreshToken(request)

            result.getOrNull().also {
                if (it != null) {
                    logger.i { "Token refreshed successfully" }
                } else {
                    logger.w { "Token refresh failed" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Token refresh error" }
            null
        }
    }

    /**
     * Get current device type (platform-specific).
     */
    private fun getDeviceType(): String {
        // This should be platform-specific, but for now:
        return "mobile" // TODO: Make platform-specific
    }
}
```

---

## Phase 4: Implement Use Cases

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/usecases/LoginUseCase.kt`

```kotlin
package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.platform.Logger

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase
) {
    private val logger = Logger.forClass<LoginUseCase>()

    suspend operator fun invoke(email: Email, password: Password): Result<Unit> {
        // Validate inputs
        if (!validateEmail(email)) {
            logger.w { "Invalid email format" }
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (!validatePassword(password)) {
            logger.w { "Invalid password format" }
            return Result.failure(IllegalArgumentException("Invalid password format"))
        }

        // Perform login
        return authRepository.login(
            email = email.value,
            password = password.value,
            rememberMe = true
        )
    }
}
```

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/usecases/RegisterUseCase.kt`

```kotlin
package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.platform.Logger

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase
) {
    private val logger = Logger.forClass<RegisterUseCase>()

    suspend operator fun invoke(
        email: Email,
        password: Password,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        // Validate inputs
        if (!validateEmail(email)) {
            logger.w { "Invalid email format" }
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (!validatePassword(password)) {
            logger.w { "Invalid password format" }
            return Result.failure(IllegalArgumentException("Invalid password format"))
        }

        if (firstName.isBlank()) {
            logger.w { "First name is required" }
            return Result.failure(IllegalArgumentException("First name is required"))
        }

        if (lastName.isBlank()) {
            logger.w { "Last name is required" }
            return Result.failure(IllegalArgumentException("Last name is required"))
        }

        // Perform registration
        return authRepository.register(
            email = email.value,
            password = password.value,
            firstName = firstName,
            lastName = lastName
        )
    }
}
```

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/usecases/LogoutUseCase.kt`

```kotlin
package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.platform.Logger

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    private val logger = Logger.forClass<LogoutUseCase>()

    suspend operator fun invoke() {
        logger.d { "Executing logout use case" }
        authRepository.logout()
    }
}
```

---

## Phase 5: Update LoginViewModel

### File: `features/auth/presentation/src/commonMain/kotlin/ai/dokus/app/auth/viewmodel/LoginViewModel.kt`

```kotlin
package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.usecases.LoginUseCase
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class LoginViewModel : BaseViewModel<LoginViewModel.State>(State.Idle), KoinComponent {

    private val logger = Logger.forClass<LoginViewModel>()
    private val loginUseCase: LoginUseCase by inject()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun login(emailValue: Email, passwordValue: Password) = scope.launch {
        logger.d { "Login attempt started" }
        mutableState.value = State.Loading

        val result = loginUseCase(emailValue, passwordValue)

        result.fold(
            onSuccess = {
                logger.i { "Login successful, navigating to home" }
                mutableState.value = State.Idle
                mutableEffect.emit(Effect.NavigateToHome)
            },
            onFailure = { error ->
                logger.e(error) { "Login failed" }
                val dokusException = when (error) {
                    is IllegalArgumentException -> DokusException.InvalidCredentials
                    else -> DokusException.NetworkError
                }
                mutableState.value = State.Error(dokusException)
            }
        )
    }

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Error(val exception: DokusException) : State
    }

    sealed interface Effect {
        data object NavigateToHome : Effect
    }
}
```

---

## Phase 6: Backend Implementation

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/rpc/AuthApiImpl.kt`

**Create RPC implementation:**

```kotlin
package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.services.UserServiceImpl
import ai.dokus.auth.backend.utils.JwtGenerator
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.auth.*
import ai.dokus.foundation.domain.rpc.AuthApi
import kotlinx.datetime.Clock

class AuthApiImpl(
    private val userService: UserServiceImpl,
    private val jwtGenerator: JwtGenerator
) : AuthApi {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            // Verify credentials
            val user = userService.verifyCredentials(request.email, request.password)
                ?: return Result.failure(IllegalArgumentException("Invalid credentials"))

            // Record login time
            userService.recordLogin(user.id, Clock.System.now())

            // Generate tokens
            val response = jwtGenerator.generateTokens(
                userId = user.id,
                email = user.email,
                fullName = "${user.firstName} ${user.lastName}",
                tenantId = user.tenantId,
                roles = setOf(user.role.name)
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        return try {
            // TODO: Get tenantId from context or create new tenant
            val tenantId = TenantId("default-tenant-id") // FIXME

            // Register user
            val user = userService.register(
                tenantId = tenantId,
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                role = UserRole.owner
            )

            // Generate tokens (auto-login)
            val response = jwtGenerator.generateTokens(
                userId = user.id,
                email = user.email,
                fullName = "${user.firstName} ${user.lastName}",
                tenantId = user.tenantId,
                roles = setOf(user.role.name)
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        return try {
            // TODO: Implement refresh token validation and storage
            // For now, return failure
            Result.failure(NotImplementedError("Refresh token not implemented yet"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        return try {
            // TODO: Revoke refresh token
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateToken(): Result<Boolean> {
        return try {
            // TODO: Implement token validation
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        return try {
            // TODO: Implement password reset email
            Result.failure(NotImplementedError("Password reset not implemented yet"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(token: String, request: ResetPasswordRequest): Result<Unit> {
        return try {
            // TODO: Implement password reset
            Result.failure(NotImplementedError("Password reset not implemented yet"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/utils/JwtGenerator.kt`

**Create JWT token generator:**

```kotlin
package ai.dokus.auth.backend.utils

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.model.auth.LoginResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

class JwtGenerator(
    private val secret: String,
    private val issuer: String = "dokus-auth"
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateTokens(
        userId: UserId,
        email: String,
        fullName: String,
        tenantId: TenantId,
        roles: Set<String>
    ): LoginResponse {
        val now = Clock.System.now()
        val accessExpiry = now + 1.hours
        val refreshExpiry = now + 30.days

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("email", email)
            .withClaim("name", fullName)
            .withClaim("tenant_id", tenantId.value)
            .withArrayClaim("groups", roles.toTypedArray())
            .withIssuedAt(now.toEpochMilliseconds())
            .withExpiresAt(accessExpiry.toEpochMilliseconds())
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.value)
            .withClaim("type", "refresh")
            .withIssuedAt(now.toEpochMilliseconds())
            .withExpiresAt(refreshExpiry.toEpochMilliseconds())
            .sign(algorithm)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600L // 1 hour in seconds
        )
    }
}
```

---

## Phase 7: Update DI Configuration

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/DiModule.kt`

**Add new use cases to DI:**

```kotlin
// Add to existing module
single { LoginUseCase(get(), get(), get()) }
single { RegisterUseCase(get(), get(), get()) }
single { LogoutUseCase(get()) }
```

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/config/DependencyInjection.kt`

**Add auth API to backend DI:**

```kotlin
// Add to KoinModule configuration
single { JwtGenerator(secret = "your-secret-key") } // TODO: Load from config
single { AuthApiImpl(get(), get()) as AuthApi }

// Register RPC API
rpc {
    registerService<AuthApi> { get<AuthApiImpl>() }
}
```

---

## Phase 8: Update Backend Routes (Optional)

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/routes/IdentityRoutes.kt`

**Add REST routes (if needed alongside RPC):**

```kotlin
package ai.dokus.auth.backend.routes

import ai.dokus.auth.backend.rpc.AuthApiImpl
import ai.dokus.foundation.domain.model.auth.LoginRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.identityRoutes() {
    val authApi: AuthApiImpl by inject()

    post("/api/auth/login") {
        val request = call.receive<LoginRequest>()
        val result = authApi.login(request)

        result.fold(
            onSuccess = { response ->
                call.respond(HttpStatusCode.OK, response)
            },
            onFailure = { error ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to error.message))
            }
        )
    }

    // Add more routes as needed
}
```

---

## Implementation Order & Priority

### Critical Path (Week 1)
1. **Day 1-2:** Fix JWT validation (Phase 1)
2. **Day 3-4:** Create Auth RPC API (Phase 2)
3. **Day 4-5:** Implement Auth Repository (Phase 3)
4. **Day 5:** Add Use Cases (Phase 4)

### High Priority (Week 2)
5. **Day 6-7:** Update LoginViewModel and UI (Phase 5)
6. **Day 8-10:** Backend implementation (Phase 6)
7. **Day 10:** DI configuration (Phase 7)

### Medium Priority (Week 3)
8. Session management
9. Password reset flow
10. Email verification
11. MFA implementation

### Low Priority (Week 4+)
12. QR code login
13. Social auth providers
14. Advanced session analytics

---

## Security Considerations

### CRITICAL Security Rules (From CLAUDE.md)

1. **Multi-Tenant Security:**
   ```kotlin
   // ❌ SECURITY VULNERABILITY
   fun getInvoice(invoiceId: UUID) = transaction {
       Invoices.select { Invoices.id eq invoiceId }
   }

   // ✅ CORRECT
   fun getInvoice(invoiceId: UUID, tenantId: UUID) = transaction {
       Invoices.select {
           (Invoices.id eq invoiceId) and (Invoices.tenantId eq tenantId)
       }
   }
   ```
   **ALWAYS filter by `tenant_id` in every query!**

2. **Password Storage:**
   - ✅ Already using bcrypt via `PasswordCryptoService`
   - ✅ Never log passwords
   - ✅ Use secure password hashing

3. **Token Security:**
   - ✅ Short-lived access tokens (1 hour)
   - ✅ Long-lived refresh tokens (30 days)
   - ✅ Secure storage using platform keychains
   - TODO: Refresh token rotation
   - TODO: Token revocation on logout

4. **Audit Logging:**
   ```kotlin
   suspend fun logAudit(
       tenantId: UUID,
       userId: UUID?,
       action: String,
       entityType: String,
       entityId: UUID
   )
   ```
   - TODO: Log all authentication events
   - TODO: 7-year retention for compliance

---

## Testing Strategy

### Unit Tests
- `JwtDecoder` validation logic
- `TokenManager` refresh mechanism
- Use case validation logic
- ViewModel state transitions

### Integration Tests
- Login flow end-to-end
- Token refresh flow
- Logout and cleanup
- Multi-tenant isolation

### UI Tests
- Login screen interactions
- Error state handling
- Navigation flows

---

## Dependencies Required

All dependencies are already in the project:
- ✅ `kotlinx-datetime` - For time handling
- ✅ `kotlinx-rpc` - For RPC communication
- ✅ `kotlinx-serialization` - For JSON serialization
- ✅ `ktor-client` - For HTTP communication
- ✅ `koin` - For dependency injection
- ✅ Backend needs: `com.auth0:java-jwt` for JWT generation

---

## Migration Notes

Since users don't exist yet, no migration needed. Fresh implementation.

---

## Monitoring & Observability

### Metrics to Track
1. Login success/failure rates
2. Token refresh frequency
3. Session duration
4. Authentication latency
5. Failed login attempts (security)

### Logging
- Use existing `Logger` wrapper around Kermit
- Log all authentication events
- Include correlation IDs for tracing
- Mask sensitive data (passwords, tokens)

---

## Open Questions

1. **Tenant Creation:** How are tenants created? During first user registration?
2. **Email Verification:** Required before first login?
3. **MFA:** When should it be enforced?
4. **Password Policy:** Min length, complexity requirements?
5. **Session Limits:** Max sessions per user?

---

## Conclusion

This plan provides a complete implementation roadmap for the authentication system. The critical path focuses on getting basic login/logout working within 1 week. All security considerations from CLAUDE.md are incorporated, especially multi-tenant filtering and proper password handling.

**Next Steps:**
1. Review this plan with the team
2. Answer open questions
3. Start with Phase 1 (JWT validation fix)
4. Implement phases sequentially
5. Add comprehensive tests at each phase
