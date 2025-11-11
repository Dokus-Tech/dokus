# Authentication System Implementation - Revised Architecture

## Architecture Clarification

Based on your architecture guidelines:
- **Feature-specific RPC interfaces** → `features/auth/domain/`
- **Feature-specific DTOs** → `features/auth/domain/`
- **Shared models only** → `foundation/domain/`

### Naming Convention
- Use `AccountRemoteService` (not AuthApi) - already exists
- Rename to match: `UserRemoteService` for consistency

---

## Updated Implementation Plan

## Phase 1: Fix JWT Validation (CRITICAL - 30 minutes)

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/utils/JwtDecoder.kt`

**Fix token validation to use current time:**

```kotlin
import kotlinx.datetime.Clock

fun validateToken(token: String?): TokenStatus {
    if (token.isNullOrBlank()) return TokenStatus.INVALID

    val claims = decode(token) ?: return TokenStatus.INVALID
    val exp = claims.exp ?: return TokenStatus.INVALID

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

---

## Phase 2: Create Auth DTOs in Feature Domain (1 hour)

### File: `features/auth/domain/src/commonMain/kotlin/ai/dokus/app/auth/domain/models/AuthModels.kt`

**Create feature-specific DTOs:**

```kotlin
package ai.dokus.app.auth.domain.models

import kotlinx.serialization.Serializable

// Login/Register DTOs
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = true,
    val deviceType: String = "mobile"
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long // seconds
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val message: String = "Registration successful"
)

// Token management
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceType: String = "mobile"
)

@Serializable
data class ValidateTokenRequest(
    val token: String
)

@Serializable
data class ValidateTokenResponse(
    val isValid: Boolean,
    val expiresIn: Long? = null
)

// Logout
@Serializable
data class LogoutRequest(
    val sessionToken: String
)

// Password reset
@Serializable
data class RequestPasswordResetRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val resetToken: String,
    val newPassword: String
)

// Account deactivation
@Serializable
data class DeactivateAccountRequest(
    val reason: String? = null
)
```

**Note:** Keep `JwtClaims` and `TokenStatus` in `foundation/domain/model/auth/` since they're used across features.

---

## Phase 3: Implement AccountRemoteService (1 hour)

### File: `features/auth/domain/src/commonMain/kotlin/ai/dokus/app/auth/domain/AccountRemoteService.kt`

**Implement the RPC service interface:**

```kotlin
package ai.dokus.app.auth.domain

import ai.dokus.app.auth.domain.models.*
import kotlinx.rpc.annotations.Rpc

/**
 * Remote service for account and authentication operations.
 * Uses KotlinX RPC for client-server communication.
 */
@Rpc
interface AccountRemoteService {

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
     * Returns new token pair.
     */
    suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse>

    /**
     * Logout user and revoke current session.
     */
    suspend fun logout(request: LogoutRequest): Result<Unit>

    /**
     * Validate if current token is still valid.
     */
    suspend fun validateToken(request: ValidateTokenRequest): Result<ValidateTokenResponse>

    /**
     * Request password reset email.
     */
    suspend fun requestPasswordReset(request: RequestPasswordResetRequest): Result<Unit>

    /**
     * Reset password with token from email.
     */
    suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit>

    /**
     * Deactivate current user account.
     */
    suspend fun deactivateAccount(request: DeactivateAccountRequest): Result<Unit>
}
```

---

## Phase 4: Implement Auth Repository (2 hours)

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/repository/AuthRepository.kt`

**Complete implementation using AccountRemoteService:**

```kotlin
package ai.dokus.app.auth.repository

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.domain.models.*
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for authentication operations.
 * Coordinates between TokenManager, AuthManager, and AccountRemoteService.
 */
class AuthRepository(
    private val tokenManager: TokenManagerMutable,
    private val authManager: AuthManagerMutable,
    private val accountService: AccountRemoteService
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
    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean = true
    ): Result<Unit> {
        logger.d { "Login attempt for email: ${email.take(3)}***" }

        return try {
            val request = LoginRequest(
                email = email,
                password = password,
                rememberMe = rememberMe,
                deviceType = getPlatformDeviceType()
            )

            val result = accountService.login(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Login successful" }
                    tokenManager.saveTokens(
                        ai.dokus.foundation.domain.model.auth.LoginResponse(
                            accessToken = response.accessToken,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
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

            val result = accountService.register(request)

            result.fold(
                onSuccess = { response ->
                    logger.i { "Registration successful, auto-logging in" }
                    tokenManager.saveTokens(
                        ai.dokus.foundation.domain.model.auth.LoginResponse(
                            accessToken = response.accessToken,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
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
                accountService.logout(request)
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
            val request = RequestPasswordResetRequest(email = email)
            accountService.requestPasswordReset(request)
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
            val request = ResetPasswordRequest(
                resetToken = resetToken,
                newPassword = newPassword
            )
            accountService.resetPassword(request)
        } catch (e: Exception) {
            logger.e(e) { "Password reset failed" }
            Result.failure(e)
        }
    }

    /**
     * Internal token refresh implementation.
     */
    private suspend fun refreshTokenInternal(
        refreshToken: String
    ): ai.dokus.foundation.domain.model.auth.LoginResponse? {
        logger.d { "Refreshing access token" }

        return try {
            val request = RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceType = getPlatformDeviceType()
            )

            val result = accountService.refreshToken(request)

            result.getOrNull()?.let { response ->
                logger.i { "Token refreshed successfully" }
                ai.dokus.foundation.domain.model.auth.LoginResponse(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresIn = response.expiresIn
                )
            } ?: run {
                logger.w { "Token refresh failed" }
                null
            }
        } catch (e: Exception) {
            logger.e(e) { "Token refresh error" }
            null
        }
    }

    /**
     * Get platform-specific device type.
     * TODO: Make this platform-specific with expect/actual
     */
    private fun getPlatformDeviceType(): String {
        return "mobile" // TODO: Implement platform detection
    }
}
```

---

## Phase 5: Update Use Cases (1 hour)

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

## Phase 6: Update LoginViewModel (1 hour)

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
                val dokusException = when {
                    error.message?.contains("Invalid") == true -> DokusException.InvalidCredentials
                    error.message?.contains("email") == true -> DokusException.InvalidEmail
                    error.message?.contains("password") == true -> DokusException.WeakPassword
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

## Phase 7: Backend Implementation (3 hours)

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/rpc/AccountRemoteServiceImpl.kt`

**Implement the RPC service:**

```kotlin
package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.domain.models.*
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.platform.Logger

class AccountRemoteServiceImpl(
    private val authService: AuthService
) : AccountRemoteService {

    private val logger = Logger.forClass<AccountRemoteServiceImpl>()

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        logger.d { "Login request for email: ${request.email.take(3)}***" }

        return try {
            authService.login(
                email = request.email,
                password = request.password,
                rememberMe = request.rememberMe,
                deviceType = request.deviceType
            )
        } catch (e: Exception) {
            logger.e(e) { "Login failed" }
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        logger.d { "Register request for email: ${request.email.take(3)}***" }

        return try {
            authService.register(
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName
            )
        } catch (e: Exception) {
            logger.e(e) { "Registration failed" }
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        logger.d { "Refresh token request" }

        return try {
            authService.refreshToken(request.refreshToken, request.deviceType)
        } catch (e: Exception) {
            logger.e(e) { "Token refresh failed" }
            Result.failure(e)
        }
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        logger.d { "Logout request" }

        return try {
            authService.logout(request.sessionToken)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.w(e) { "Logout failed (non-critical)" }
            Result.success(Unit) // Still return success since token is cleared client-side
        }
    }

    override suspend fun validateToken(request: ValidateTokenRequest): Result<ValidateTokenResponse> {
        return try {
            val result = authService.validateToken(request.token)
            Result.success(result)
        } catch (e: Exception) {
            logger.e(e) { "Token validation failed" }
            Result.failure(e)
        }
    }

    override suspend fun requestPasswordReset(request: RequestPasswordResetRequest): Result<Unit> {
        return try {
            authService.requestPasswordReset(request.email)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Password reset request failed" }
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit> {
        return try {
            authService.resetPassword(request.resetToken, request.newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Password reset failed" }
            Result.failure(e)
        }
    }

    override suspend fun deactivateAccount(request: DeactivateAccountRequest): Result<Unit> {
        return try {
            authService.deactivateAccount(request.reason)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Account deactivation failed" }
            Result.failure(e)
        }
    }
}
```

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/AuthService.kt`

**Create auth service (business logic layer):**

```kotlin
package ai.dokus.auth.backend.services

import ai.dokus.app.auth.domain.models.LoginResponse
import ai.dokus.app.auth.domain.models.ValidateTokenResponse
import ai.dokus.auth.backend.database.services.UserServiceImpl
import ai.dokus.auth.backend.utils.JwtGenerator
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import kotlinx.datetime.Clock

class AuthService(
    private val userService: UserServiceImpl,
    private val jwtGenerator: JwtGenerator
) {

    suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean,
        deviceType: String
    ): Result<LoginResponse> {
        // Verify credentials
        val user = userService.verifyCredentials(email, password)
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

        return Result.success(response)
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<LoginResponse> {
        // TODO: Get tenantId from context or create new tenant
        val tenantId = TenantId("default-tenant-id") // FIXME: Implement tenant creation

        // Register user
        val user = userService.register(
            tenantId = tenantId,
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName,
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

        return Result.success(response)
    }

    suspend fun refreshToken(refreshToken: String, deviceType: String): Result<LoginResponse> {
        // TODO: Implement refresh token validation and storage
        return Result.failure(NotImplementedError("Refresh token not implemented yet"))
    }

    suspend fun logout(sessionToken: String): Result<Unit> {
        // TODO: Revoke refresh token in database
        return Result.success(Unit)
    }

    suspend fun validateToken(token: String): ValidateTokenResponse {
        // TODO: Implement token validation
        return ValidateTokenResponse(isValid = true, expiresIn = 3600)
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> {
        // TODO: Generate reset token and send email
        return Result.failure(NotImplementedError("Password reset not implemented yet"))
    }

    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> {
        // TODO: Validate reset token and update password
        return Result.failure(NotImplementedError("Password reset not implemented yet"))
    }

    suspend fun deactivateAccount(reason: String?): Result<Unit> {
        // TODO: Deactivate user account
        return Result.failure(NotImplementedError("Account deactivation not implemented yet"))
    }
}
```

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/utils/JwtGenerator.kt`

**JWT token generator:**

```kotlin
package ai.dokus.auth.backend.utils

import ai.dokus.app.auth.domain.models.LoginResponse
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
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

## Phase 8: Update DI Configuration (1 hour)

### File: `features/auth/data/src/commonMain/kotlin/ai/dokus/app/auth/DiModule.kt`

**Update data module DI:**

```kotlin
// Add AccountRemoteService
single<AccountRemoteService> {
    // This will be provided by RPC client
    // Configuration happens in platform-specific modules
    TODO("Configure RPC client for AccountRemoteService")
}

// Add AuthRepository with AccountRemoteService
single {
    AuthRepository(
        tokenManager = get(),
        authManager = get(),
        accountService = get()
    )
}

// Add Use Cases
single { LoginUseCase(get(), get(), get()) }
single { RegisterUseCase(get(), get(), get()) }
single { LogoutUseCase(get()) }
```

### File: `features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/config/DependencyInjection.kt`

**Update backend DI:**

```kotlin
// Add AuthService
single {
    AuthService(
        userService = get(),
        jwtGenerator = get()
    )
}

// Add JwtGenerator
single {
    JwtGenerator(
        secret = environment.config.property("jwt.secret").getString()
    )
}

// Add AccountRemoteService implementation
single<AccountRemoteService> {
    AccountRemoteServiceImpl(
        authService = get()
    )
}

// Register RPC service
install(RPC) {
    rpcConfig {
        serialization {
            json()
        }
    }
}

// Register service in routing
routing {
    rpc("/api/rpc") {
        registerService<AccountRemoteService> { get() }
    }
}
```

---

## Summary of Architecture Changes

### Feature Domain Module Structure
```
features/auth/domain/
├── src/commonMain/kotlin/ai/dokus/app/auth/domain/
│   ├── AccountRemoteService.kt      # RPC interface
│   └── models/
│       └── AuthModels.kt            # Feature-specific DTOs
└── build.gradle.kts                 # With kotlinxRpcPlugin
```

### Shared Models (Stay in foundation)
```
foundation/domain/model/auth/
├── JwtModels.kt         # JwtClaims, TokenStatus (shared)
├── SessionDto.kt        # Session tracking (shared)
└── Identity.kt          # User identity (shared)
```

### Key Differences from Original Plan
1. ✅ Use `AccountRemoteService` instead of `AuthApi`
2. ✅ Put auth DTOs in `features/auth/domain/models/`
3. ✅ Keep only shared models in `foundation/domain/`
4. ✅ Feature domain module has RPC plugin enabled

---

## Implementation Checklist

- [ ] Phase 1: Fix JWT validation (30 min)
- [ ] Phase 2: Create auth DTOs in feature domain (1 hour)
- [ ] Phase 3: Implement AccountRemoteService interface (1 hour)
- [ ] Phase 4: Implement AuthRepository (2 hours)
- [ ] Phase 5: Implement Use Cases (1 hour)
- [ ] Phase 6: Update LoginViewModel (1 hour)
- [ ] Phase 7: Backend implementation (3 hours)
- [ ] Phase 8: DI configuration (1 hour)

**Total Estimated Time:** ~10 hours for basic login/logout flow

---

## Next Steps After Implementation

1. **Testing:** Write unit tests for each layer
2. **Refresh Token Storage:** Implement refresh token database table
3. **Password Reset:** Email service integration
4. **Session Management:** Track active sessions
5. **MFA:** Two-factor authentication
6. **Social Auth:** OAuth providers

---

## Notes

- All existing foundation models stay where they are
- Only auth-specific request/response DTOs move to feature domain
- RPC communication uses KotlinX RPC (already configured)
- Backend uses existing UserService for database operations
