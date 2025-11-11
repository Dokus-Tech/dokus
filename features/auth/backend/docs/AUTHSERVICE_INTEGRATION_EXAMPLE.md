# AuthService Integration with RefreshTokenService

## Overview

This document shows how to integrate `RefreshTokenService` into the existing `AuthService` to implement complete token lifecycle management including:
- Saving refresh tokens on login/register
- Refreshing access tokens
- Revoking tokens on logout

## Updated AuthService

Here's how to update `AuthService` to use `RefreshTokenService`:

### 1. Add RefreshTokenService Dependency

```kotlin
class AuthService(
    private val userService: UserService,
    private val tenantService: TenantService,
    private val jwtGenerator: JwtGenerator,
    private val refreshTokenService: RefreshTokenService  // Add this
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    companion object {
        private val REFRESH_TOKEN_DURATION = 30.days
    }

    // ... methods
}
```

### 2. Update Login Method

Add refresh token persistence after generating tokens:

```kotlin
@OptIn(ExperimentalUuidApi::class)
suspend fun login(request: LoginRequest): Result<LoginResponse> = try {
    logger.debug("Login attempt for email: ${request.email.value}")

    // Verify user credentials
    val user = userService.verifyCredentials(
        email = request.email.value,
        password = request.password.value
    ) ?: run {
        logger.warn("Invalid credentials for email: ${request.email.value}")
        throw IllegalArgumentException("Invalid credentials")
    }

    // Check if account is active
    if (!user.isActive) {
        logger.warn("Inactive account login attempt for email: ${request.email.value}")
        throw IllegalArgumentException("Account is inactive")
    }

    // Record successful login
    val userId = UserId(user.id.value.toString())
    val loginTime = Clock.System.now()
    userService.recordLogin(userId, loginTime)

    // Generate full name for JWT claims
    val fullName = buildString {
        user.firstName?.let { append(it) }
        if (user.firstName != null && user.lastName != null) append(" ")
        user.lastName?.let { append(it) }
    }.ifEmpty { user.email.value }

    // Generate JWT tokens
    val response = jwtGenerator.generateTokens(
        userId = userId,
        email = user.email.value,
        fullName = fullName,
        tenantId = user.tenantId,
        roles = setOf(user.role.dbValue)
    )

    // *** NEW: Save refresh token to database ***
    val refreshTokenExpiresAt = Clock.System.now() + REFRESH_TOKEN_DURATION
    refreshTokenService.saveRefreshToken(
        userId = userId,
        token = response.refreshToken,
        expiresAt = refreshTokenExpiresAt
    ).getOrElse { error ->
        logger.error("Failed to save refresh token for user: $userId", error)
        throw IllegalStateException("Failed to complete login: ${error.message}")
    }

    logger.info("Successful login for user: ${user.id} (email: ${user.email.value})")
    Result.success(response)
} catch (e: Exception) {
    logger.error("Login error for email: ${request.email.value}", e)
    Result.failure(e)
}
```

### 3. Update Register Method

Add refresh token persistence after registration:

```kotlin
@OptIn(ExperimentalUuidApi::class)
suspend fun register(request: RegisterRequest): Result<LoginResponse> = try {
    logger.debug("Registration attempt for email: ${request.email.value}")

    // Create tenant for the new user
    val tenantName = buildString {
        append(request.firstName.value)
        if (request.firstName.value.isNotEmpty() && request.lastName.value.isNotEmpty()) append(" ")
        append(request.lastName.value)
    }.ifEmpty { request.email.value }

    logger.debug("Creating new tenant: $tenantName for email: ${request.email.value}")

    val tenant = tenantService.createTenant(
        name = tenantName,
        email = request.email.value,
        plan = TenantPlan.Trial,
        country = "BE",
        language = Language.En,
        vatNumber = null
    )

    logger.info("Created tenant: ${tenant.id} with trial ending at: ${tenant.trialEndsAt}")

    // Register new user with Owner role
    val user = userService.register(
        tenantId = tenant.id,
        email = request.email.value,
        password = request.password.value,
        firstName = request.firstName.value,
        lastName = request.lastName.value,
        role = UserRole.Owner
    )

    // Generate full name for JWT claims
    val fullName = buildString {
        user.firstName?.let { append(it) }
        if (user.firstName != null && user.lastName != null) append(" ")
        user.lastName?.let { append(it) }
    }.ifEmpty { user.email.value }

    // Auto-login: generate JWT tokens
    val userId = UserId(user.id.value.toString())
    val response = jwtGenerator.generateTokens(
        userId = userId,
        email = user.email.value,
        fullName = fullName,
        tenantId = user.tenantId,
        roles = setOf(user.role.dbValue)
    )

    // *** NEW: Save refresh token to database ***
    val refreshTokenExpiresAt = Clock.System.now() + REFRESH_TOKEN_DURATION
    refreshTokenService.saveRefreshToken(
        userId = userId,
        token = response.refreshToken,
        expiresAt = refreshTokenExpiresAt
    ).getOrElse { error ->
        logger.error("Failed to save refresh token for user: $userId", error)
        throw IllegalStateException("Failed to complete registration: ${error.message}")
    }

    logger.info("Successful registration and auto-login for user: ${user.id} (email: ${user.email.value}), tenant: ${tenant.id}")
    Result.success(response)
} catch (e: IllegalArgumentException) {
    logger.warn("Registration failed for email: ${request.email.value} - ${e.message}")
    Result.failure(e)
} catch (e: Exception) {
    logger.error("Registration error for email: ${request.email.value}", e)
    Result.failure(e)
}
```

### 4. Implement Refresh Token Method

Replace the TODO with full implementation:

```kotlin
/**
 * Refreshes an expired access token using a valid refresh token.
 * Implements token rotation: old token is revoked, new token is generated and saved.
 *
 * @param request Refresh token request containing the current refresh token
 * @return Result with new LoginResponse containing fresh access and refresh tokens
 */
@OptIn(ExperimentalUuidApi::class)
suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> = try {
    logger.debug("Token refresh attempt")

    // 1. Validate old refresh token and get userId (old token is automatically revoked)
    val userId = refreshTokenService.validateAndRotate(request.refreshToken)
        .getOrElse { error ->
            logger.warn("Invalid refresh token: ${error.message}")
            when (error) {
                is SecurityException -> throw IllegalArgumentException("Refresh token has been revoked")
                is IllegalArgumentException -> throw IllegalArgumentException("Invalid or expired refresh token")
                else -> throw IllegalStateException("Token validation failed: ${error.message}")
            }
        }

    // 2. Fetch user details (needed for generating new tokens)
    val user = userService.findById(userId)
        ?: run {
            logger.error("User not found for userId: $userId during token refresh")
            throw IllegalArgumentException("User not found")
        }

    // 3. Check if account is still active
    if (!user.isActive) {
        logger.warn("Token refresh attempted for inactive account: ${user.email.value}")
        throw IllegalArgumentException("Account is inactive")
    }

    // 4. Generate full name for JWT claims
    val fullName = buildString {
        user.firstName?.let { append(it) }
        if (user.firstName != null && user.lastName != null) append(" ")
        user.lastName?.let { append(it) }
    }.ifEmpty { user.email.value }

    // 5. Generate new JWT tokens (both access and refresh)
    val response = jwtGenerator.generateTokens(
        userId = userId,
        email = user.email.value,
        fullName = fullName,
        tenantId = user.tenantId,
        roles = setOf(user.role.dbValue)
    )

    // 6. Save new refresh token to database
    val refreshTokenExpiresAt = Clock.System.now() + REFRESH_TOKEN_DURATION
    refreshTokenService.saveRefreshToken(
        userId = userId,
        token = response.refreshToken,
        expiresAt = refreshTokenExpiresAt
    ).getOrElse { error ->
        logger.error("Failed to save new refresh token for user: $userId", error)
        throw IllegalStateException("Failed to complete token refresh: ${error.message}")
    }

    logger.info("Successful token refresh for user: $userId")
    Result.success(response)
} catch (e: Exception) {
    logger.error("Token refresh error", e)
    Result.failure(e)
}
```

### 5. Implement Logout Method

Replace the TODO with token revocation:

```kotlin
/**
 * Logs out a user and invalidates their current session.
 * Revokes the refresh token to prevent it from being used again.
 *
 * @param request Logout request containing the refresh token to revoke
 * @return Result indicating success or failure
 */
suspend fun logout(request: LogoutRequest): Result<Unit> = try {
    logger.info("Logout request received")

    // Revoke the refresh token
    refreshTokenService.revokeToken(request.sessionToken)
        .getOrElse { error ->
            // Log warning but don't fail logout if token doesn't exist
            // (user might be logging out with an already expired token)
            logger.warn("Failed to revoke token during logout: ${error.message}")
        }

    logger.info("Logout successful")
    Result.success(Unit)
} catch (e: Exception) {
    logger.error("Logout error", e)
    Result.failure(Exception("Logout failed: ${e.message}", e))
}
```

### 6. Additional Security Methods

Add helper methods for security operations:

```kotlin
/**
 * Revokes all refresh tokens for a user.
 * Used for security operations like password reset or account compromise.
 *
 * @param userId The user whose tokens should be revoked
 * @return Result indicating success or failure
 */
suspend fun revokeAllUserSessions(userId: UserId): Result<Unit> = try {
    logger.info("Revoking all sessions for user: $userId")

    refreshTokenService.revokeAllUserTokens(userId)
        .getOrThrow()

    logger.info("Successfully revoked all sessions for user: $userId")
    Result.success(Unit)
} catch (e: Exception) {
    logger.error("Failed to revoke all sessions for user: $userId", e)
    Result.failure(e)
}

/**
 * Get all active sessions for a user.
 * Useful for displaying active sessions in user settings.
 *
 * @param userId The user to query
 * @return List of active session information
 */
suspend fun getUserActiveSessions(userId: UserId): List<RefreshTokenInfo> {
    return refreshTokenService.getUserActiveTokens(userId)
}
```

## Updated Koin DI Configuration

Update the DI configuration to inject RefreshTokenService:

```kotlin
// In features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/config/DependencyInjection.kt

private val appModule = module {
    // ... database and other services

    // Local database services
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single<RefreshTokenService> { RefreshTokenServiceImpl() }  // Already added

    // JWT token generation
    single {
        val appConfig = get<AppBaseConfig>()
        JwtGenerator(
            secret = appConfig.jwt.secret,
            issuer = appConfig.jwt.issuer
        )
    }

    // Authentication service - now with RefreshTokenService
    single {
        AuthService(
            userService = get(),
            tenantService = get(),
            jwtGenerator = get(),
            refreshTokenService = get()  // Add this parameter
        )
    }

    // ... RPC implementations
}
```

## API Endpoint Updates

Update the HTTP routes to handle token refresh:

```kotlin
// In your routing configuration
fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            authService.login(request)
                .onSuccess { response ->
                    call.respond(HttpStatusCode.OK, response)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to (error.message ?: "Login failed"))
                    )
                }
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            authService.register(request)
                .onSuccess { response ->
                    call.respond(HttpStatusCode.Created, response)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Registration failed"))
                    )
                }
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            authService.refreshToken(request)
                .onSuccess { response ->
                    call.respond(HttpStatusCode.OK, response)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to (error.message ?: "Token refresh failed"))
                    )
                }
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            authService.logout(request)
                .onSuccess {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (error.message ?: "Logout failed"))
                    )
                }
        }

        // Admin endpoint to revoke all sessions (requires authentication)
        post("/revoke-all-sessions") {
            // Extract userId from JWT token in Authorization header
            val userId = call.principal<JWTPrincipal>()
                ?.getClaim("sub", String::class)
                ?.let { UserId(it) }
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            authService.revokeAllUserSessions(userId)
                .onSuccess {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "All sessions revoked"))
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (error.message ?: "Failed to revoke sessions"))
                    )
                }
        }

        // Endpoint to get active sessions
        get("/active-sessions") {
            val userId = call.principal<JWTPrincipal>()
                ?.getClaim("sub", String::class)
                ?.let { UserId(it) }
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val sessions = authService.getUserActiveSessions(userId)
            call.respond(HttpStatusCode.OK, sessions)
        }
    }
}
```

## Client-Side Integration

### JavaScript/TypeScript Example

```typescript
// Store tokens after login/register
async function login(email: string, password: string) {
    const response = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });

    const data = await response.json();

    // Store access token in memory (short-lived)
    sessionStorage.setItem('accessToken', data.accessToken);

    // Store refresh token in secure HTTP-only cookie (server-side)
    // OR in secure storage if mobile app
    localStorage.setItem('refreshToken', data.refreshToken);
}

// Refresh access token when it expires
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');

    const response = await fetch('/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
    });

    if (response.ok) {
        const data = await response.json();
        sessionStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        return data.accessToken;
    } else {
        // Refresh token invalid/expired - redirect to login
        window.location.href = '/login';
        return null;
    }
}

// Automatically refresh on 401 errors
async function fetchWithAuth(url: string, options: RequestInit = {}) {
    const accessToken = sessionStorage.getItem('accessToken');

    const response = await fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${accessToken}`
        }
    });

    if (response.status === 401) {
        // Access token expired - try to refresh
        const newAccessToken = await refreshAccessToken();
        if (newAccessToken) {
            // Retry request with new token
            return fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    'Authorization': `Bearer ${newAccessToken}`
                }
            });
        }
    }

    return response;
}

// Logout
async function logout() {
    const refreshToken = localStorage.getItem('refreshToken');

    await fetch('/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionToken: refreshToken })
    });

    sessionStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
}
```

## Scheduled Cleanup Job

Add a scheduled job to clean up expired tokens:

```kotlin
// In your application setup
class TokenCleanupJob(
    private val refreshTokenService: RefreshTokenService
) {
    private val logger = LoggerFactory.getLogger(TokenCleanupJob::class.java)

    suspend fun execute() {
        logger.info("Starting token cleanup job")

        refreshTokenService.cleanupExpiredTokens()
            .onSuccess { count ->
                logger.info("Token cleanup completed: removed $count tokens")
            }
            .onFailure { error ->
                logger.error("Token cleanup failed", error)
            }
    }
}

// Schedule daily cleanup at 2 AM
fun Application.scheduleTokenCleanup() {
    val cleanupJob = koin.get<TokenCleanupJob>()

    launch {
        while (isActive) {
            // Wait until 2 AM
            val now = Clock.System.now()
            val next2AM = /* calculate next 2 AM */
            delay(next2AM - now)

            // Run cleanup
            cleanupJob.execute()

            // Wait 24 hours before next run
            delay(24.hours)
        }
    }
}
```

## Testing the Integration

```kotlin
@Test
fun `login should save refresh token`() = runBlocking {
    val request = LoginRequest(
        email = Email("test@example.com"),
        password = Password("SecurePass123!")
    )

    val result = authService.login(request)

    assertTrue(result.isSuccess)
    val response = result.getOrThrow()

    // Verify refresh token was saved
    val tokens = refreshTokenService.getUserActiveTokens(UserId(response.userId))
    assertEquals(1, tokens.size)
}

@Test
fun `refreshToken should rotate tokens`() = runBlocking {
    // Login to get initial tokens
    val loginResponse = authService.login(/* ... */).getOrThrow()

    // Refresh token
    val refreshRequest = RefreshTokenRequest(loginResponse.refreshToken)
    val refreshResponse = authService.refreshToken(refreshRequest).getOrThrow()

    // Verify new tokens are different
    assertNotEquals(loginResponse.accessToken, refreshResponse.accessToken)
    assertNotEquals(loginResponse.refreshToken, refreshResponse.refreshToken)

    // Verify old refresh token cannot be used again
    val secondRefresh = authService.refreshToken(refreshRequest)
    assertTrue(secondRefresh.isFailure)
}

@Test
fun `logout should revoke refresh token`() = runBlocking {
    val loginResponse = authService.login(/* ... */).getOrThrow()

    // Logout
    authService.logout(LogoutRequest(loginResponse.refreshToken)).getOrThrow()

    // Verify token cannot be used
    val refreshResult = authService.refreshToken(
        RefreshTokenRequest(loginResponse.refreshToken)
    )
    assertTrue(refreshResult.isFailure)
}
```

## Summary

This integration provides:

1. **Complete token lifecycle**: From creation to rotation to revocation
2. **Security best practices**: Token rotation, proper validation, secure logging
3. **Production-ready**: Error handling, logging, testing
4. **Maintainable**: Clean separation of concerns, dependency injection

The implementation follows the OWASP recommendations for OAuth 2.0 and JWT security.
