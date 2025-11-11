# AuthService Integration - Action Items

## Critical Issues Found

Based on the AuthService integration analysis, here are the prioritized action items.

---

## 🔴 BLOCKING - Must Fix Before Production

### 1. Implement Email Service Integration

**Status**: Missing ⚠️
**Impact**: Tokens are generated but never sent to users
**Files Affected**: 
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/EmailVerificationService.kt` (Line 29)
- `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/services/PasswordResetService.kt` (Line 45)

**Current Code**:
```kotlin
class EmailVerificationService(
    // TODO: private val emailService: EmailService
)
```

**Required Changes**:

1. Create EmailService interface:
```kotlin
// foundation/ktor/src/commonMain/kotlin/ai/dokus/foundation/ktor/services/EmailService.kt
interface EmailService {
    suspend fun sendVerificationEmail(email: String, token: String, verificationLink: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String, resetLink: String): Result<Unit>
}
```

2. Update EmailVerificationService:
```kotlin
class EmailVerificationService(
    private val emailService: EmailService
) {
    suspend fun sendVerificationEmail(userId: UserId, email: String): Result<Unit> {
        return try {
            val token = generateSecureToken()
            val expiry = now() + 24.hours
            
            dbQuery {
                // ... store token ...
            }
            
            // Send email
            val verificationLink = "https://app.dokus.ai/auth/verify-email?token=$token"
            emailService.sendVerificationEmail(email, token, verificationLink)
                .onFailure { error ->
                    logger.warn("Failed to send verification email", error)
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send verification email", e)
            Result.failure(DokusException.InternalError("Failed to send verification email"))
        }
    }
}
```

3. Update PasswordResetService similarly

4. Update DependencyInjection.kt:
```kotlin
single { EmailService() } // Your implementation
single { EmailVerificationService(get()) }
single { PasswordResetService(get(), get(), get()) } // Add emailService
```

**Estimated Effort**: 4-6 hours (depends on email provider choice: SendGrid, AWS SES, Resend, etc.)

---

## 🟡 HIGH PRIORITY - Implement Soon

### 2. Complete RPC Endpoint: resendVerificationEmail()

**Status**: Returns NotImplementedError ⚠️
**Location**: `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/rpc/AccountRemoteServiceImpl.kt` (Lines 162-172)
**Impact**: Users cannot request new verification email via RPC

**Current Code**:
```kotlin
override suspend fun resendVerificationEmail(): Result<Unit> {
    logger.debug("RPC: resendVerificationEmail called")
    
    return Result.failure<Unit>(
        NotImplementedError("Resend verification email requires authenticated user context")
    ).also {
        logger.warn("RPC: resendVerificationEmail not fully implemented - needs user context")
    }
}
```

**Required Fix**:
```kotlin
override suspend fun resendVerificationEmail(userId: UserId): Result<Unit> {
    logger.debug("RPC: resendVerificationEmail called for user: ${userId.value}")
    
    return authService.resendVerificationEmail(userId)
        .onSuccess {
            logger.info("RPC: Verification email resent for user: ${userId.value}")
        }
        .onFailure { error ->
            logger.error("RPC: Failed to resend verification email for user: ${userId.value}", error)
        }
}
```

**Estimated Effort**: 1-2 hours

---

### 3. Implement Account Deactivation

**Status**: Returns NotImplementedError ⚠️
**Location**: `/features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/rpc/AccountRemoteServiceImpl.kt` (Lines 132-140)
**Impact**: Users cannot deactivate accounts (compliance critical)

**Required Steps**:

1. Add method to AuthService:
```kotlin
suspend fun deactivateAccount(userId: UserId): Result<Unit> = try {
    logger.debug("Account deactivation requested for user: ${userId.value}")
    
    // Revoke all refresh tokens to force re-login everywhere
    refreshTokenService.revokeAllUserTokens(userId)
        .onFailure { error ->
            logger.warn("Failed to revoke tokens during deactivation", error)
            // Don't fail - continue with deactivation
        }
    
    // Deactivate user account
    userService.deactivate(userId)
    
    logger.info("Account deactivated for user: ${userId.value}")
    Result.success(Unit)
} catch (e: DokusException) {
    logger.error("Account deactivation failed: ${e.errorCode}", e)
    Result.failure(e)
} catch (e: Exception) {
    logger.error("Account deactivation error", e)
    Result.failure(DokusException.InternalError("Failed to deactivate account"))
}
```

2. Ensure UserService.deactivate() exists:
```kotlin
suspend fun deactivate(userId: UserId) = dbQuery {
    val userUuid = java.util.UUID.fromString(userId.value)
    UsersTable.update({ UsersTable.id eq userUuid }) {
        it[isActive] = false
    }
}
```

3. Update RPC endpoint:
```kotlin
override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
    logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")
    
    // Extract userId from authenticated context
    val userId = extractUserIdFromContext() // Your implementation
    
    return authService.deactivateAccount(userId)
        .onSuccess {
            logger.info("RPC: Account deactivated successfully")
        }
        .onFailure { error ->
            logger.error("RPC: Account deactivation failed", error)
        }
}
```

**Estimated Effort**: 3-4 hours

---

## 🟠 MEDIUM PRIORITY - Do Before First Release

### 4. Add Comprehensive Test Coverage

**Status**: Only RefreshTokenService has tests (15% coverage)
**Location**: `/features/auth/backend/src/test/kotlin/`

**Required Tests**:

#### 4a. AuthService Tests (High value)
```kotlin
class AuthServiceTest {
    
    @Test
    fun testLoginWithRateLimiting() {
        // Test: login fails after 5 attempts
        // Test: lockout lasts 15 minutes
        // Test: reset on successful login
    }
    
    @Test
    fun testRegistrationWithEmailVerification() {
        // Test: user created with unverified email
        // Test: verification email sent (mocked)
        // Test: registration succeeds even if email fails
    }
    
    @Test
    fun testPasswordReset() {
        // Test: request returns success for any email
        // Test: invalid token fails reset
        // Test: expired token fails reset
        // Test: token marked as used after reset
        // Test: all tokens revoked after reset
    }
    
    @Test
    fun testEmailVerification() {
        // Test: valid token verifies email
        // Test: expired token fails
        // Test: used token fails
        // Test: resend creates new token
    }
}
```

#### 4b. RateLimitService Tests
```kotlin
class RateLimitServiceTest {
    
    @Test
    fun testLoginAttemptTracking() {
        // Test: 5th attempt triggers lockout
        // Test: lockout prevents login
        // Test: lockout expires after 15 minutes
        // Test: cleanup removes expired entries
    }
}
```

#### 4c. Integration Tests
```kotlin
class AuthIntegrationTest {
    
    @Test
    fun testCompleteRegistrationFlow() {
        // register() -> verifyEmail() -> login()
    }
    
    @Test
    fun testCompletePasswordResetFlow() {
        // requestPasswordReset() -> resetPassword() -> login()
    }
    
    @Test
    fun testBruteForceProtection() {
        // 5 failed attempts -> locked -> exponential backoff
    }
}
```

**Estimated Effort**: 6-8 hours

---

## 🟢 LOW PRIORITY - Nice to Have

### 5. Upgrade Rate Limiting to Redis

**Current**: In-memory storage (good for single-instance)
**Issue**: Doesn't work for multi-instance deployments

**When**: After first release, if scaling to multiple instances

**Estimated Effort**: 4-6 hours

---

## 🟢 LOW PRIORITY - Documentation

### 6. Add Architecture Documentation

Create `/docs/AUTH_ARCHITECTURE.md`:
- Service interaction diagram
- Security considerations
- Token lifecycle diagrams
- Email flow diagrams
- Password reset flow diagrams
- Rate limiting algorithm explanation

**Estimated Effort**: 3-4 hours

---

## Summary Timeline

### Week 1 (Blocking)
- [ ] Implement EmailService integration (4-6 hours)
- [ ] Complete RPC resendVerificationEmail (1-2 hours)

### Week 2 (High Priority)
- [ ] Implement account deactivation (3-4 hours)
- [ ] Start test coverage (2-3 hours)

### Week 3 (Complete)
- [ ] Finish test coverage (4-5 hours)
- [ ] Code review and bug fixes

### Post-Release
- [ ] Redis upgrade for rate limiting
- [ ] Architecture documentation
- [ ] Performance optimization

---

## Verification Checklist

After completing these items:

- [ ] Email verification emails sent successfully
- [ ] Password reset emails sent successfully
- [ ] Rate limiting prevents brute force attacks
- [ ] Users can verify email via RPC
- [ ] Users can deactivate accounts
- [ ] All new services have unit tests
- [ ] Auth flows have integration tests
- [ ] No TODOs in core auth files
- [ ] All RPC endpoints implemented
- [ ] Production-ready for launch

---

## Notes

1. **Email Service Provider Options**:
   - SendGrid (recommended for EU compliance)
   - AWS SES
   - Resend
   - Mailgun

2. **Test Framework**: Use the same framework as RefreshTokenServiceImplTest

3. **Security Considerations**: 
   - Keep EmailService implementation separate (in foundation.ktor)
   - Don't expose email addresses in logs
   - Ensure GDPR compliance for email handling

4. **Performance**:
   - Rate limiting cleanup job runs hourly (optimal)
   - Email sending should be non-blocking (already implemented)
   - Token validation is O(1) database lookup

---

## Questions?

Refer to the full analysis at: `/docs/AUTH_SERVICE_INTEGRATION_ANALYSIS.md`
