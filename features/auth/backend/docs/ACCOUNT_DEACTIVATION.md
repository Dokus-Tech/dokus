# Account Deactivation Feature

## Overview

The account deactivation feature allows users to deactivate (soft delete) their own accounts. This is a critical security and compliance feature that:

- Marks the user account as inactive
- Revokes all active refresh tokens (terminates all sessions)
- Logs the deactivation reason for audit trails
- Preserves all user data (soft delete, not hard delete)

## Architecture

### Components

1. **AuthService.deactivateAccount()** - Business logic layer
   - Location: `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/AuthService.kt`
   - Orchestrates the deactivation process
   - Validates user exists and is active
   - Coordinates with UserService and RefreshTokenService

2. **UserService.deactivate()** - Data persistence layer
   - Location: `/foundation/ktor-common/src/main/kotlin/ai/dokus/foundation/ktor/services/UserService.kt`
   - Interface definition with optional reason parameter
   - Implementation: `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/database/services/UserServiceImpl.kt`
   - Sets `isActive = false` in the database

3. **AccountRemoteServiceImpl.deactivateAccount()** - RPC endpoint
   - Location: `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/rpc/AccountRemoteServiceImpl.kt`
   - RPC interface for client-server communication
   - TODO: Extract userId from JWT authentication context

## Implementation Details

### 1. AuthService.deactivateAccount()

```kotlin
suspend fun deactivateAccount(userId: UserId, reason: String): Result<Unit>
```

**Process Flow:**

1. **Validate User Exists**
   - Queries UserService to find the user
   - Returns `DokusException.InvalidCredentials` if user not found

2. **Check User Status**
   - Verifies user is currently active
   - Returns `DokusException.AccountInactive` if already deactivated

3. **Deactivate Account**
   - Calls `userService.deactivate(userId, reason)`
   - Sets `isActive = false` in the database
   - Logs the deactivation reason

4. **Revoke All Tokens**
   - Calls `refreshTokenService.revokeAllUserTokens(userId)`
   - Marks all refresh tokens as revoked
   - Terminates all active sessions
   - Non-blocking: logs errors but doesn't fail if revocation fails

5. **Audit Logging**
   - All operations are logged with userId and reason
   - Enables audit trail for compliance

**Error Handling:**

- `DokusException.InvalidCredentials` - User not found
- `DokusException.AccountInactive` - User already deactivated
- `DokusException.InternalError` - Database or internal errors

### 2. UserService.deactivate()

```kotlin
suspend fun deactivate(userId: UserId, reason: String? = null)
```

**Implementation:**

```kotlin
override suspend fun deactivate(userId: UserId, reason: String?) = dbQuery {
    val javaUuid = java.util.UUID.fromString(userId.value)
    val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
        it[isActive] = false
    }

    if (updated == 0) {
        throw IllegalArgumentException("User not found: $userId")
    }

    val reasonLog = if (reason != null) " - Reason: $reason" else ""
    logger.info("Deactivated user $userId$reasonLog")
}
```

**Database Changes:**

- Updates `users.is_active` column to `false`
- No data is deleted (soft delete)
- User record remains in the database for audit purposes

### 3. RPC Endpoint

```kotlin
override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit>
```

**Implementation:**

```kotlin
override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
    logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")

    return try {
        // Extract userId from JWT authentication context
        val userId = requireAuthenticatedUserId()
        logger.debug("Deactivating account for authenticated user: ${userId.value}")

        authService.deactivateAccount(userId, request.reason)
            .onSuccess {
                logger.info("RPC: Account deactivated successfully for user: ${userId.value}")
            }
            .onFailure { error ->
                logger.error("RPC: Account deactivation failed for user: ${userId.value}", error)
            }
    } catch (e: IllegalStateException) {
        // No authentication context available
        logger.error("RPC: deactivateAccount called without authentication")
        Result.failure(DokusException.Unauthorized("Authentication required to deactivate account"))
    } catch (e: Exception) {
        logger.error("RPC: Unexpected error in deactivateAccount", e)
        Result.failure(DokusException.InternalError("Failed to deactivate account"))
    }
}
```

**Authentication Context:**

- Uses `requireAuthenticatedUserId()` to extract userId from JWT token in coroutine context
- Throws `IllegalStateException` if no authentication context is available
- Returns `DokusException.Unauthorized` if user is not authenticated
- Ensures user can only deactivate their own account (userId from JWT)

## Security Considerations

### Access Control

- **Authentication Required**: User must be authenticated (JWT token)
- **Authorization**: User can only deactivate their own account
- **No Privilege Escalation**: Cannot deactivate other users' accounts

### Session Management

- **Token Revocation**: All refresh tokens are revoked immediately
- **Session Termination**: User is logged out of all devices
- **Access Token**: Short-lived access tokens expire naturally (1 hour)

### Data Preservation

- **Soft Delete**: User record is not deleted, only marked inactive
- **Audit Trail**: All deactivation attempts are logged
- **Reversibility**: Admin can reactivate accounts if needed

### Compliance

- **GDPR**: Supports right to erasure (first step before data deletion)
- **Audit Logging**: Tracks who, when, and why accounts are deactivated
- **Data Retention**: User data preserved for legal/compliance requirements

## Usage Examples

### From Backend Service

```kotlin
// Inject AuthService
val authService: AuthService = get()

// Deactivate user account
val userId = UserId("550e8400-e29b-41d4-a716-446655440000")
val reason = "User requested account deletion"

val result = authService.deactivateAccount(userId, reason)

result.onSuccess {
    println("Account deactivated successfully")
}
result.onFailure { error ->
    println("Deactivation failed: ${error.message}")
}
```

### From RPC Client

```kotlin
// Create RPC client
val accountService: AccountRemoteService = getRpcClient()

// Deactivate account (requires authentication)
val request = DeactivateUserRequest(
    reason = "No longer need the service"
)

val result = accountService.deactivateAccount(request)

result.onSuccess {
    // Show success message to user
    // Navigate to logout/goodbye screen
}
result.onFailure { error ->
    // Show error message
}
```

## Testing

### Unit Tests

Location: `/features/auth/backend/src/test/kotlin/ai/dokus/auth/backend/services/AuthServiceDeactivateAccountTest.kt`

**Test Coverage:**

- ✅ Successful deactivation of active user
- ✅ User not found error handling
- ✅ Already inactive user error handling
- ✅ Token revocation during deactivation
- ✅ Token revocation failure handling (non-blocking)
- ✅ Reason propagation to user service
- ✅ Database error handling
- ✅ Operation ordering verification
- ✅ Empty reason string handling

**Running Tests:**

```bash
# Run all auth service tests
./gradlew :features:auth:backend:test --tests "*AuthServiceDeactivateAccountTest"

# Run specific test
./gradlew :features:auth:backend:test --tests "*AuthServiceDeactivateAccountTest.deactivateAccount should successfully deactivate active user"
```

### Integration Tests

**TODO: Add integration tests for:**

- End-to-end RPC flow with authentication
- Database transaction verification
- Token revocation across distributed systems
- Concurrent deactivation attempts

## Database Schema

### Users Table

```sql
-- Relevant columns for deactivation
CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    -- ... other columns
);

-- After deactivation
-- is_active = false
```

### Refresh Tokens Table

```sql
-- All tokens revoked after deactivation
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- After deactivation
-- is_revoked = true for all user's tokens
```

## API Reference

### DeactivateUserRequest

```kotlin
@Serializable
data class DeactivateUserRequest(
    val reason: String
)
```

**Fields:**

- `reason` (String): Human-readable reason for deactivation
  - Examples: "No longer need the service", "Privacy concerns", "Switching to competitor"
  - Used for audit logging and analytics
  - Can be empty but not null

### Response

```kotlin
Result<Unit>
```

**Success:**

- Returns `Result.success(Unit)` when account is deactivated
- All sessions are terminated
- User cannot log in with existing credentials

**Failure:**

- `DokusException.InvalidCredentials` - User not found
- `DokusException.AccountInactive` - Already deactivated
- `DokusException.InternalError` - System error
- `IllegalStateException` - Authentication context not implemented (RPC)

## Future Enhancements

### 1. Reactivation Flow

- [ ] Admin endpoint for account reactivation
- [ ] Email verification for reactivation
- [ ] Time-limited reactivation window
- [ ] Audit logging for reactivations

### 2. Data Deletion

- [ ] Scheduled job for permanent data deletion
- [ ] Grace period before hard delete (30 days)
- [ ] Compliance with GDPR "right to be forgotten"
- [ ] Export user data before deletion

### 3. Analytics

- [ ] Track deactivation reasons
- [ ] Measure retention impact
- [ ] User feedback collection
- [ ] Exit survey integration

### 4. Email Notifications

- [ ] Confirmation email on deactivation
- [ ] Reactivation instructions
- [ ] Data deletion schedule notice
- [ ] Account status updates

## Troubleshooting

### User Can't Deactivate Account

**Symptoms:**
- Deactivation fails with `Unauthorized` error
- RPC returns `DokusException.Unauthorized`

**Solution:**
- Ensure user is authenticated (valid JWT token in request)
- Check JWT token is not expired
- Verify authentication middleware is properly configured
- Check RPC client is sending authentication headers

### Account Already Deactivated

**Symptoms:**
- Deactivation fails with `AccountInactive` exception
- User sees "already deactivated" error

**Solution:**
- Check user status: `userService.findById(userId)`
- Admin can reactivate if needed: `userService.reactivate(userId)`
- Verify not attempting duplicate deactivation

### Tokens Not Revoked

**Symptoms:**
- User still logged in after deactivation
- Access tokens still work

**Solution:**
- Access tokens expire naturally (1 hour max)
- Refresh tokens are immediately revoked
- Check `refresh_tokens` table: all tokens should have `is_revoked = true`
- Verify `refreshTokenService.revokeAllUserTokens()` was called

## Related Documentation

- [Authentication Service Integration](./AUTHSERVICE_INTEGRATION_EXAMPLE.md)
- [Refresh Token Service](./REFRESH_TOKEN_SERVICE.md)
- [Database Schema](../../../../docs/DATABASE.md)
- [Security Guidelines](../../../../docs/SECURITY.md)

## Change Log

### 2025-11-10 - Initial Implementation

- ✅ Implemented `AuthService.deactivateAccount()`
- ✅ Updated `UserService.deactivate()` with reason parameter
- ✅ Implemented RPC endpoint with authentication context
- ✅ Added comprehensive unit tests (8 test cases)
- ✅ Documented implementation and usage
- ⏳ Integration tests pending
- ⏳ Email notifications pending
