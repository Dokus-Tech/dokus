# RefreshTokenService Documentation

## Overview

The `RefreshTokenService` provides secure management of JWT refresh tokens with persistence, rotation, and revocation capabilities. It ensures that refresh tokens are properly validated, rotated on use, and cleaned up when expired or revoked.

## Architecture

### Components

1. **RefreshTokenService** - Interface defining token management operations
2. **RefreshTokenServiceImpl** - Implementation with database persistence using Exposed ORM
3. **RefreshTokensTable** - Database schema for storing tokens

### Security Features

- **Token Rotation**: Old tokens are automatically revoked when used to generate new tokens
- **Expiration Validation**: Expired tokens are rejected
- **Revocation Support**: Tokens can be revoked individually or in bulk
- **Secure Logging**: Token values are never logged; only hashes are recorded
- **Transactional Operations**: All database operations are atomic

## Database Schema

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

## API Reference

### saveRefreshToken

Saves a refresh token to the database.

```kotlin
suspend fun saveRefreshToken(
    userId: UserId,
    token: String,
    expiresAt: Instant
): Result<Unit>
```

**Parameters:**
- `userId`: The user this token belongs to
- `token`: The JWT refresh token string
- `expiresAt`: When this token expires

**Returns:** `Result<Unit>` indicating success or failure

**Example:**
```kotlin
val userId = UserId("550e8400-e29b-41d4-a716-446655440000")
val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
val expiresAt = Clock.System.now() + 30.days

refreshTokenService.saveRefreshToken(userId, token, expiresAt)
    .onSuccess { logger.info("Token saved successfully") }
    .onFailure { error -> logger.error("Failed to save token", error) }
```

### validateAndRotate

Validates a refresh token and marks it as revoked (token rotation pattern).

```kotlin
suspend fun validateAndRotate(oldToken: String): Result<UserId>
```

**Parameters:**
- `oldToken`: The current refresh token to validate

**Returns:** `Result<UserId>` containing the user ID if valid, or error if invalid

**Validation Checks:**
1. Token exists in database
2. Token is not revoked
3. Token is not expired
4. Marks old token as revoked

**Errors:**
- `IllegalArgumentException`: Token not found or expired
- `SecurityException`: Token has been revoked

**Example:**
```kotlin
refreshTokenService.validateAndRotate(oldRefreshToken)
    .onSuccess { userId ->
        // Generate new tokens
        val newAccessToken = jwtGenerator.generateAccessToken(userId, ...)
        val newRefreshToken = jwtGenerator.generateRefreshToken(userId, ...)

        // Save new refresh token
        refreshTokenService.saveRefreshToken(userId, newRefreshToken, ...)
    }
    .onFailure { error ->
        when (error) {
            is SecurityException -> logger.warn("Revoked token used")
            is IllegalArgumentException -> logger.warn("Invalid token")
            else -> logger.error("Token validation failed", error)
        }
    }
```

### revokeToken

Revokes a specific refresh token (used during logout).

```kotlin
suspend fun revokeToken(token: String): Result<Unit>
```

**Parameters:**
- `token`: The refresh token to revoke

**Returns:** `Result<Unit>` indicating success or failure

**Example:**
```kotlin
// During logout
refreshTokenService.revokeToken(userRefreshToken)
    .onSuccess { logger.info("User logged out successfully") }
    .onFailure { error -> logger.error("Failed to revoke token", error) }
```

### revokeAllUserTokens

Revokes all refresh tokens for a user (used for security purposes).

```kotlin
suspend fun revokeAllUserTokens(userId: UserId): Result<Unit>
```

**Parameters:**
- `userId`: The user whose tokens should be revoked

**Returns:** `Result<Unit>` indicating success or failure

**Use Cases:**
- Password reset
- Account compromise detection
- User deactivation
- Security policy changes

**Example:**
```kotlin
// After password reset
refreshTokenService.revokeAllUserTokens(userId)
    .onSuccess {
        logger.info("All user sessions invalidated")
        notifyUser("All devices have been logged out")
    }
    .onFailure { error -> logger.error("Failed to revoke tokens", error) }
```

### cleanupExpiredTokens

Removes expired and revoked tokens from the database.

```kotlin
suspend fun cleanupExpiredTokens(): Result<Int>
```

**Returns:** `Result<Int>` containing count of deleted tokens

**Cleanup Criteria:**
- `expiresAt < now` OR `isRevoked = true`

**Example:**
```kotlin
// Run as scheduled job (e.g., daily at 2 AM)
refreshTokenService.cleanupExpiredTokens()
    .onSuccess { count ->
        logger.info("Cleaned up $count expired/revoked tokens")
    }
    .onFailure { error -> logger.error("Cleanup failed", error) }
```

### getUserActiveTokens

Retrieves all active tokens for a user.

```kotlin
suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo>
```

**Parameters:**
- `userId`: The user to query

**Returns:** List of active token information (non-revoked, non-expired)

**RefreshTokenInfo:**
```kotlin
data class RefreshTokenInfo(
    val tokenId: String,      // Database ID
    val createdAt: Instant,   // When token was created
    val expiresAt: Instant,   // When token expires
    val isRevoked: Boolean    // Revocation status
)
```

**Example:**
```kotlin
// Display active sessions to user
val activeSessions = refreshTokenService.getUserActiveTokens(userId)

activeSessions.forEach { session ->
    println("Session from: ${session.createdAt}")
    println("Expires: ${session.expiresAt}")
    println("---")
}
```

## Integration Guide

### 1. Login Flow

```kotlin
suspend fun login(email: String, password: String): Result<LoginResponse> {
    // 1. Verify credentials
    val user = userService.verifyCredentials(email, password)
        ?: return Result.failure(InvalidCredentialsException())

    // 2. Generate tokens
    val loginResponse = jwtGenerator.generateLoginResponse(
        userId = user.id,
        email = user.email.value,
        name = "${user.firstName} ${user.lastName}",
        tenantId = user.tenantId,
        roles = listOf(user.role)
    ).getOrThrow()

    // 3. Save refresh token
    val expiresAt = Clock.System.now() + 30.days
    refreshTokenService.saveRefreshToken(
        userId = user.id,
        token = loginResponse.refreshToken,
        expiresAt = expiresAt
    ).getOrThrow()

    // 4. Record login
    userService.recordLogin(user.id, Clock.System.now())

    return Result.success(loginResponse)
}
```

### 2. Token Refresh Flow

```kotlin
suspend fun refreshAccessToken(refreshToken: String): Result<LoginResponse> {
    // 1. Validate and rotate refresh token
    val userId = refreshTokenService.validateAndRotate(refreshToken)
        .getOrElse { error ->
            return Result.failure(InvalidRefreshTokenException(error.message))
        }

    // 2. Get user details
    val user = userService.findById(userId)
        ?: return Result.failure(UserNotFoundException())

    // 3. Generate new tokens
    val loginResponse = jwtGenerator.generateLoginResponse(
        userId = user.id,
        email = user.email.value,
        name = "${user.firstName} ${user.lastName}",
        tenantId = user.tenantId,
        roles = listOf(user.role)
    ).getOrThrow()

    // 4. Save new refresh token
    val expiresAt = Clock.System.now() + 30.days
    refreshTokenService.saveRefreshToken(
        userId = user.id,
        token = loginResponse.refreshToken,
        expiresAt = expiresAt
    ).getOrThrow()

    return Result.success(loginResponse)
}
```

### 3. Logout Flow

```kotlin
suspend fun logout(refreshToken: String): Result<Unit> {
    return refreshTokenService.revokeToken(refreshToken)
        .onSuccess { logger.info("User logged out successfully") }
        .onFailure { error -> logger.error("Logout failed", error) }
}
```

### 4. Security Operations

```kotlin
// Revoke all sessions after password reset
suspend fun handlePasswordReset(userId: UserId) {
    refreshTokenService.revokeAllUserTokens(userId)
        .onSuccess {
            logger.info("Revoked all sessions for user after password reset")
            emailService.sendPasswordResetNotification(userId)
        }
}

// Scheduled cleanup job
@Scheduled(cron = "0 2 * * *") // Daily at 2 AM
suspend fun scheduledTokenCleanup() {
    refreshTokenService.cleanupExpiredTokens()
        .onSuccess { count ->
            logger.info("Daily cleanup: removed $count expired tokens")
        }
}
```

## Koin Dependency Injection

The service is registered in the Koin module:

```kotlin
// In features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/config/DependencyInjection.kt

private val appModule = module {
    // ... other services

    single<RefreshTokenService> { RefreshTokenServiceImpl() }

    // ... other services
}
```

**Usage:**
```kotlin
class MyService(
    private val refreshTokenService: RefreshTokenService  // Injected by Koin
) {
    suspend fun someMethod() {
        refreshTokenService.saveRefreshToken(...)
    }
}
```

## Security Best Practices

### 1. Token Storage

**Client-side:**
- Store refresh tokens in secure, HTTP-only cookies
- Never store in localStorage (XSS vulnerability)
- Use SameSite=Strict to prevent CSRF

**Server-side:**
- Store tokens in database with user reference
- Index token column for fast lookup
- Cascade delete when user is deleted

### 2. Token Rotation

- Always rotate refresh tokens on use
- Mark old token as revoked immediately
- Generate new token with same expiry duration
- Never reuse refresh tokens

### 3. Validation

- Check expiration before token revoked status
- Use database timestamps in UTC
- Reject expired tokens immediately
- Log suspicious activity (revoked token usage)

### 4. Logging

- NEVER log actual token values
- Use token hashes for correlation
- Log security events (revoked token usage)
- Track failed validation attempts

### 5. Cleanup

- Run cleanup job daily
- Delete both expired AND revoked tokens
- Monitor cleanup metrics
- Alert on unusual token counts

## Error Handling

```kotlin
refreshTokenService.validateAndRotate(token)
    .onSuccess { userId ->
        // Token is valid, proceed with refresh
    }
    .onFailure { error ->
        when (error) {
            is SecurityException -> {
                // Revoked token used - potential security issue
                logger.warn("Revoked token attempted", error)
                alertSecurityTeam(token)
                return@onFailure respondWith401("Token revoked")
            }
            is IllegalArgumentException -> {
                // Invalid/expired token - normal case
                logger.debug("Invalid token", error)
                return@onFailure respondWith401("Invalid token")
            }
            else -> {
                // Unexpected error - log and investigate
                logger.error("Token validation failed unexpectedly", error)
                return@onFailure respondWith500("Internal error")
            }
        }
    }
```

## Testing

Comprehensive tests are provided in `RefreshTokenServiceImplTest.kt`:

```bash
# Run tests
./gradlew :features:auth:backend:test --tests RefreshTokenServiceImplTest

# Run with coverage
./gradlew :features:auth:backend:test :features:auth:backend:jacocoTestReport
```

**Test Coverage:**
- Token persistence and retrieval
- Validation of valid tokens
- Rejection of expired tokens
- Rejection of revoked tokens
- Rejection of non-existent tokens
- Single token revocation
- Bulk token revocation
- Expired token cleanup
- Active token listing
- Concurrent operations

## Performance Considerations

1. **Database Indexes**: All foreign keys and query columns are indexed
2. **Connection Pooling**: Uses HikariCP for efficient connection management
3. **Batch Operations**: Cleanup operations use single queries
4. **Token Hashing**: SHA-256 hashing is fast and secure
5. **Query Optimization**: Uses Exposed ORM efficiently

## Migration Guide

If upgrading from a system without token persistence:

```sql
-- 1. Create refresh_tokens table (already done via schema)

-- 2. Clean up any existing in-memory tokens (restart server)

-- 3. All users will need to re-login (tokens regenerated)

-- 4. Monitor for issues in first 48 hours
```

## Troubleshooting

### Token Validation Fails

**Symptoms:** Users cannot refresh access tokens

**Checks:**
1. Verify token exists: `SELECT * FROM refresh_tokens WHERE token = ?`
2. Check if revoked: `is_revoked = true`
3. Check if expired: `expires_at < NOW()`
4. Verify user exists: `user_id` references valid user

### Cleanup Not Working

**Symptoms:** Database growing with old tokens

**Checks:**
1. Verify cleanup job is running
2. Check database timestamps are in UTC
3. Verify delete permissions
4. Monitor cleanup metrics

### Performance Issues

**Symptoms:** Slow token operations

**Checks:**
1. Verify indexes exist: `EXPLAIN SELECT * FROM refresh_tokens WHERE token = ?`
2. Check connection pool size
3. Monitor query execution times
4. Consider archiving old tokens

## Future Enhancements

1. **Device Tracking**: Store device info with tokens
2. **Geo-location**: Track login locations
3. **Rate Limiting**: Limit refresh attempts per user
4. **Token Families**: Implement refresh token families for better security
5. **Push Notifications**: Notify users of new logins
6. **Session Analytics**: Track session duration and patterns
