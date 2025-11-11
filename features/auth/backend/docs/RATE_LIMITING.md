# Rate Limiting Implementation

## Overview

The auth service implements rate limiting to prevent brute force attacks on the login endpoint. The implementation tracks failed login attempts per email address and temporarily locks accounts after too many failures.

## Features

- **Failed Attempt Tracking**: Tracks login failures per email address (case-insensitive)
- **Account Lockout**: Automatically locks accounts after 5 failed attempts
- **Automatic Unlock**: Locks expire after 15 minutes
- **Attempt Window**: Failed attempts are counted within a 15-minute rolling window
- **Thread-Safe**: Uses mutex for concurrent request safety
- **Memory Management**: Background cleanup job removes expired entries every hour

## Configuration

```kotlin
companion object {
    /** Maximum number of failed login attempts before account lockout */
    private const val MAX_ATTEMPTS = 5

    /** Time window for counting failed attempts (15 minutes) */
    private val ATTEMPT_WINDOW = 15.minutes

    /** Duration of account lockout after max attempts exceeded (15 minutes) */
    private val LOCKOUT_DURATION = 15.minutes
}
```

## Architecture

### Components

1. **RateLimitService** (`/services/RateLimitService.kt`)
   - Core service implementing rate limiting logic
   - Tracks attempts in-memory (upgradeable to Redis for multi-instance deployments)
   - Thread-safe with kotlinx.coroutines.sync.Mutex
   - Provides methods for checking, recording, and resetting attempts

2. **RateLimitCleanupJob** (`/jobs/RateLimitCleanupJob.kt`)
   - Background coroutine that runs every hour
   - Removes expired rate limit entries from memory
   - Prevents memory leaks in long-running applications

3. **AuthService Integration**
   - Rate limit check runs BEFORE credential verification
   - Failed login attempts are recorded AFTER failed verification
   - Successful login resets the attempt counter

## Flow Diagram

```
┌─────────────────┐
│  Login Request  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Check Rate Limit        │
│ (RateLimitService)      │
└─────────┬───────────────┘
          │
          ├─────► [Blocked] Return 429 Too Many Requests
          │
          ▼
┌─────────────────────────┐
│ Verify Credentials      │
│ (UserService)           │
└─────────┬───────────────┘
          │
          ├─────► [Invalid] Record Failed Attempt → Return 401
          │
          ▼
┌─────────────────────────┐
│ Reset Attempt Counter   │
│ (On Success)            │
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│ Generate Tokens         │
│ Return 200 OK           │
└─────────────────────────┘
```

## API Response

When rate limit is exceeded, the API returns:

```json
{
  "error": {
    "code": "TOO_MANY_LOGIN_ATTEMPTS",
    "message": "Too many login attempts. Please try again in 782 seconds.",
    "retryAfterSeconds": 782,
    "recoverable": true
  }
}
```

HTTP Status: `429 Too Many Requests`

## Usage

### Check Rate Limit

```kotlin
rateLimitService.checkLoginAttempts(email).getOrElse { error ->
    // Throws DokusException.TooManyLoginAttempts if blocked
    throw error
}
```

### Record Failed Login

```kotlin
rateLimitService.recordFailedLogin(email)
```

### Reset on Success

```kotlin
rateLimitService.resetLoginAttempts(email)
```

### Monitoring Methods

```kotlin
// Get current attempt count
val attempts = rateLimitService.getAttemptCount(email)

// Check if account is locked
val isLocked = rateLimitService.isLocked(email)
```

## Security Considerations

### Email Normalization

All email addresses are normalized to lowercase to prevent bypassing rate limits with case variations:

```kotlin
val normalizedEmail = email.lowercase()
```

### Timing Attack Protection

The service checks rate limits BEFORE verifying credentials, preventing attackers from using timing analysis to determine if an email exists.

### Retry-After Header

The exception includes `retryAfterSeconds` to inform clients when they can retry, promoting good client behavior.

## Deployment Considerations

### Single Instance (Current)

The current implementation uses in-memory storage, which works well for single-instance deployments.

### Multi-Instance (Future)

For multi-instance deployments, replace the in-memory map with Redis:

```kotlin
// Replace in RateLimitService
private val loginAttempts = mutableMapOf<String, LoginAttemptTracker>()

// With Redis client
private val redisClient: RedisClient
```

Benefits of Redis:
- Shared state across instances
- Built-in TTL support
- No need for cleanup job
- Persistence across restarts

### Load Balancer Configuration

For multi-instance deployments without Redis, ensure:
- Sticky sessions for login attempts
- Or implement distributed rate limiting with Redis

## Monitoring

### Metrics to Track

1. **Rate Limit Blocks**: Count of requests blocked by rate limiting
2. **Lockout Events**: Number of accounts locked due to failed attempts
3. **Cleanup Stats**: Entries removed during cleanup runs
4. **Average Attempts**: Average failed attempts before success/lockout

### Log Examples

```
// Attempt blocked
WARN  - Login attempt blocked for user@example.com - locked for 782 seconds

// Account locked
WARN  - Account locked for user@example.com after 5 failed attempts

// Successful cleanup
DEBUG - Cleaned up 42 expired rate limit entries
```

## Testing

### Manual Testing

```bash
# Test rate limiting
for i in {1..6}; do
  curl -X POST http://localhost:8000/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}'
  echo "\nAttempt $i\n"
done
```

Expected results:
- Attempts 1-5: Return 401 Unauthorized
- Attempt 6+: Return 429 Too Many Requests

### Integration Tests

See `/features/auth/backend/src/test/kotlin/ai/dokus/auth/backend/services/RateLimitServiceTest.kt` for comprehensive test coverage (when test dependencies are enabled).

## Future Enhancements

1. **Configurable Limits**: Allow per-tenant rate limit configuration
2. **IP-Based Limiting**: Track attempts by IP address in addition to email
3. **Progressive Delays**: Increase lockout duration for repeat offenders
4. **CAPTCHA Integration**: Require CAPTCHA after N failed attempts
5. **Admin Override**: Allow admins to manually unlock accounts
6. **Notification System**: Alert users when their account is locked
7. **Redis Backend**: For distributed deployments
8. **Metrics Dashboard**: Real-time monitoring of rate limit events

## Related Files

- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/RateLimitService.kt`
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/jobs/RateLimitCleanupJob.kt`
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/AuthService.kt`
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/config/DependencyInjection.kt`
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/Application.kt`
- `/foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusException.kt`
