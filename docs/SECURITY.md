# Security & Best Practices

**Last Updated:** October 2025
**Status:** Production Security Guidelines

---

## Table of Contents

1. [Security Overview](#security-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Multi-Tenant Isolation](#multi-tenant-isolation)
4. [Data Encryption](#data-encryption)
5. [Audit Logging](#audit-logging)
6. [Security Best Practices](#security-best-practices)
7. [Compliance](#compliance)
8. [Security Checklist](#security-checklist)

---

## Security Overview

Dokus handles sensitive financial data and must maintain the highest security standards. This document outlines our security architecture, best practices, and compliance requirements.

### Security Principles

1. **Defense in Depth**: Multiple layers of security
2. **Principle of Least Privilege**: Minimal access by default
3. **Zero Trust**: Verify every request
4. **Audit Everything**: Complete trail of all actions
5. **Encryption Everywhere**: Data at rest and in transit

### Threat Model

**Assets to Protect:**
- Financial data (invoices, expenses, payments)
- User credentials and authentication tokens
- Client personal information
- Business tax records

**Threats:**
- Unauthorized access to tenant data
- Data breaches via SQL injection
- Session hijacking
- Man-in-the-middle attacks
- Data loss or corruption

---

## Authentication & Authorization

### JWT-Based Authentication

**Token Strategy:**
- **Access Token**: 15-minute expiry, stored in memory
- **Refresh Token**: 7-day expiry, stored in secure storage
- **Rotation**: New refresh token on every refresh

```kotlin
data class AuthTokens(
    val accessToken: String,      // Short-lived, for API calls
    val refreshToken: String,      // Long-lived, for token refresh
    val expiresIn: Long           // Access token expiry in seconds
)
```

### Token Storage

**Client-Side Storage Rules:**

```kotlin
// ✅ CORRECT - Access token in memory only
class TokenManager {
    private var accessToken: String? = null  // In-memory only

    fun setAccessToken(token: String) {
        accessToken = token
    }
}

// ❌ WRONG - Never store in localStorage/SharedPreferences unencrypted
localStorage.setItem("accessToken", token)  // VULNERABLE!
```

**Secure Storage Implementation:**

```kotlin
// Platform-specific secure storage
expect class SecureStorage {
    suspend fun saveRefreshToken(token: String)
    suspend fun getRefreshToken(): String?
    suspend fun deleteRefreshToken()
}

// Android: EncryptedSharedPreferences
actual class SecureStorage {
    private val encryptedPrefs = EncryptedSharedPreferences.create(/*...*/)

    actual suspend fun saveRefreshToken(token: String) {
        encryptedPrefs.edit().putString("refresh_token", token).apply()
    }
}

// iOS: Keychain
actual class SecureStorage {
    actual suspend fun saveRefreshToken(token: String) {
        let query = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrAccount: "refresh_token",
            kSecValueData: token.data(using: .utf8)!
        ]
        SecItemAdd(query as CFDictionary, nil)
    }
}
```

### Password Security

**Hashing Algorithm:** Argon2id (recommended) or bcrypt

```kotlin
object PasswordHasher {
    private const val BCRYPT_ROUNDS = 12

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
}

// Password requirements
data class PasswordPolicy(
    val minLength: Int = 12,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true
)

fun validatePassword(password: String, policy: PasswordPolicy): Result<Unit> {
    return when {
        password.length < policy.minLength ->
            Result.failure(ValidationError("Password must be at least ${policy.minLength} characters"))

        policy.requireUppercase && !password.any { it.isUpperCase() } ->
            Result.failure(ValidationError("Password must contain uppercase letter"))

        policy.requireDigit && !password.any { it.isDigit() } ->
            Result.failure(ValidationError("Password must contain digit"))

        else -> Result.success(Unit)
    }
}
```

### Multi-Factor Authentication (MFA)

**TOTP-Based MFA:**

```kotlin
class MFAService {
    fun generateSecret(): String {
        return Base32.random(160)  // 160-bit secret
    }

    fun generateQRCode(email: String, secret: String): String {
        val issuer = "Dokus"
        val otpAuthUrl = "otpauth://totp/$issuer:$email?secret=$secret&issuer=$issuer"
        return QRCode.generate(otpAuthUrl)
    }

    fun verifyCode(secret: String, code: String): Boolean {
        val totp = TOTP(secret)
        return totp.verify(code, allowedWindow = 1)  // ±30 seconds
    }
}
```

### Role-Based Access Control (RBAC)

```kotlin
enum class UserRole {
    OWNER,      // Full access
    MEMBER,     // Create/edit invoices and expenses
    ACCOUNTANT, // View-only financial data
    VIEWER      // Dashboard only
}

enum class Permission {
    // Invoice permissions
    INVOICE_CREATE, INVOICE_READ, INVOICE_UPDATE, INVOICE_DELETE, INVOICE_SEND,

    // Expense permissions
    EXPENSE_CREATE, EXPENSE_READ, EXPENSE_UPDATE, EXPENSE_DELETE,

    // Client permissions
    CLIENT_CREATE, CLIENT_READ, CLIENT_UPDATE, CLIENT_DELETE,

    // Settings permissions
    SETTINGS_UPDATE, BILLING_UPDATE, USER_MANAGE
}

object PermissionMatrix {
    private val rolePermissions = mapOf(
        UserRole.OWNER to Permission.values().toSet(),

        UserRole.MEMBER to setOf(
            Permission.INVOICE_CREATE, Permission.INVOICE_READ, Permission.INVOICE_UPDATE,
            Permission.EXPENSE_CREATE, Permission.EXPENSE_READ, Permission.EXPENSE_UPDATE,
            Permission.CLIENT_CREATE, Permission.CLIENT_READ, Permission.CLIENT_UPDATE
        ),

        UserRole.ACCOUNTANT to setOf(
            Permission.INVOICE_READ, Permission.EXPENSE_READ, Permission.CLIENT_READ
        ),

        UserRole.VIEWER to setOf(
            Permission.INVOICE_READ
        )
    )

    fun hasPermission(role: UserRole, permission: Permission): Boolean {
        return rolePermissions[role]?.contains(permission) ?: false
    }
}

// Usage in service
suspend fun deleteInvoice(invoiceId: UUID): Result<Unit> {
    val user = getCurrentUser()

    if (!PermissionMatrix.hasPermission(user.role, Permission.INVOICE_DELETE)) {
        return Result.failure(InsufficientPermissions("Cannot delete invoices"))
    }

    // Proceed with deletion...
}
```

---

## Multi-Tenant Isolation

### Database-Level Isolation

**Critical Security Rule:** ALWAYS filter by `tenant_id` in every query.

```kotlin
// ✅ CORRECT - Tenant-safe query
suspend fun getInvoice(id: UUID, tenantId: UUID): Invoice? = dbQuery {
    Invoices.select {
        (Invoices.id eq id) and (Invoices.tenantId eq tenantId)
    }.singleOrNull()?.let { mapToInvoice(it) }
}

// ❌ WRONG - Security vulnerability! Cross-tenant data leak!
suspend fun getInvoice(id: UUID): Invoice? = dbQuery {
    Invoices.select { Invoices.id eq id }.singleOrNull()?.let { mapToInvoice(it) }
}
```

### Tenant Context

```kotlin
// Extract tenant ID from JWT
class TenantContextInterceptor : Interceptor {
    override suspend fun intercept(call: ApplicationCall, next: suspend () -> Unit) {
        val jwt = call.principal<JWTPrincipal>()
        val tenantId = jwt?.payload?.getClaim("tenant_id")?.asString()

        if (tenantId == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return
        }

        call.attributes.put(TenantIdKey, UUID.fromString(tenantId))
        next()
    }
}

// Use in service
class InvoiceService {
    suspend fun getInvoices(call: ApplicationCall): List<Invoice> {
        val tenantId = call.attributes[TenantIdKey]
        return invoiceRepository.findByTenant(tenantId)
    }
}
```

### Row-Level Security (RLS) - PostgreSQL

**Optional Additional Layer:**

```sql
-- Enable RLS on all tenant tables
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only access their tenant's data
CREATE POLICY tenant_isolation_policy ON invoices
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Set tenant context at connection level
SET app.current_tenant_id = 'tenant-uuid-here';
```

---

## Data Encryption

### Encryption at Rest

**Database Encryption:**
- PostgreSQL: Enable transparent data encryption (TDE)
- AWS RDS: Encryption enabled by default
- Encrypted backups

**Sensitive Field Encryption:**

```kotlin
object EncryptionService {
    private val cipher = "AES/GCM/NoPadding"

    fun encrypt(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(cipher)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray())

        // Combine IV + ciphertext
        val combined = iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encrypted: String, key: SecretKey): String {
        val combined = Base64.getDecoder().decode(encrypted)

        val iv = combined.sliceArray(0 until 12)  // GCM IV is 12 bytes
        val ciphertext = combined.sliceArray(12 until combined.size)

        val cipher = Cipher.getInstance(cipher)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext)
    }
}

// Encrypt sensitive fields
suspend fun saveBankConnection(connection: BankConnection) = dbQuery {
    val encryptedToken = EncryptionService.encrypt(
        connection.accessToken,
        getEncryptionKey()
    )

    BankConnections.insert {
        it[tenantId] = connection.tenantId
        it[accessToken] = encryptedToken  // Encrypted!
        it[refreshToken] = EncryptionService.encrypt(connection.refreshToken, getEncryptionKey())
    }
}
```

### Encryption in Transit

**TLS 1.3 Required:**

```kotlin
// Ktor server configuration
fun Application.configureSecurity() {
    install(HTTPS) {
        sslPort = 443
        tlsVersion = TLSVersion.TLS13
    }

    install(HSTS) {
        includeSubDomains = true
        preload = true
        maxAgeInSeconds = 31536000  // 1 year
    }
}
```

**Certificate Pinning (Mobile Apps):**

```kotlin
// Android
val certificatePinner = CertificatePinner.Builder()
    .add("api.dokus.ai", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val httpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

---

## Audit Logging

### What to Log

**Required for Compliance:**
- All financial operations (create, update, delete)
- User authentication events
- Permission changes
- Export/download actions
- Failed authentication attempts

### Audit Log Implementation

```kotlin
suspend fun auditLog(
    tenantId: UUID,
    userId: UUID?,
    action: String,
    entityType: String,
    entityId: UUID,
    oldValues: String? = null,
    newValues: String? = null,
    ipAddress: String? = null,
    userAgent: String? = null
) = dbQuery {
    AuditLogs.insert {
        it[AuditLogs.tenantId] = tenantId
        it[AuditLogs.userId] = userId
        it[AuditLogs.action] = action
        it[AuditLogs.entityType] = entityType
        it[AuditLogs.entityId] = entityId
        it[AuditLogs.oldValues] = oldValues
        it[AuditLogs.newValues] = newValues
        it[AuditLogs.ipAddress] = ipAddress
        it[AuditLogs.userAgent] = userAgent
    }
}

// Usage
suspend fun updateInvoice(id: UUID, updates: UpdateInvoiceRequest): Result<Invoice> {
    val old = invoiceRepository.findById(id) ?: return Result.failure(NotFound())

    val updated = invoiceRepository.update(id, updates)

    auditLog(
        tenantId = old.tenantId,
        userId = getCurrentUserId(),
        action = "invoice.updated",
        entityType = "invoice",
        entityId = id,
        oldValues = Json.encodeToString(old),
        newValues = Json.encodeToString(updated),
        ipAddress = getClientIp(),
        userAgent = getUserAgent()
    )

    return Result.success(updated)
}
```

**Audit Log Retention:** 7 years (legal requirement)

---

## Security Best Practices

### Input Validation

```kotlin
fun validateInvoiceNumber(invoiceNumber: String): Result<Unit> {
    return when {
        invoiceNumber.isBlank() ->
            Result.failure(ValidationError("Invoice number is required"))

        invoiceNumber.length > 50 ->
            Result.failure(ValidationError("Invoice number too long"))

        !invoiceNumber.matches(Regex("^[A-Z0-9-]+$")) ->
            Result.failure(ValidationError("Invalid invoice number format"))

        else -> Result.success(Unit)
    }
}
```

### SQL Injection Prevention

**Using Exposed ORM (Type-Safe):**

```kotlin
// ✅ SAFE - Parameterized query via Exposed
Invoices.select {
    (Invoices.tenantId eq tenantId) and
    (Invoices.invoiceNumber eq invoiceNumber)
}

// ❌ UNSAFE - Raw SQL with string concatenation
exec("SELECT * FROM invoices WHERE invoice_number = '$invoiceNumber'")
```

### Rate Limiting

```kotlin
fun Application.configureRateLimiting() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 60.seconds)
        }

        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = 60.seconds)
        }
    }

    routing {
        rateLimit(RateLimitName("auth")) {
            post("/auth/login") { /* ... */ }
        }
    }
}
```

### CORS Configuration

```kotlin
fun Application.configureCORS() {
    install(CORS) {
        allowHost("dokus.ai", schemes = listOf("https"))
        allowHost("*.dokus.ai", schemes = listOf("https"))

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)

        allowCredentials = true
        maxAgeInSeconds = 3600
    }
}
```

---

## Compliance

### GDPR Compliance

**Required Capabilities:**
- ✅ Data export (user can download all their data)
- ✅ Data deletion (user can request account deletion)
- ✅ Consent management
- ✅ Privacy policy
- ✅ Data processing agreement

```kotlin
// GDPR: Data export
suspend fun exportUserData(userId: UUID): ByteArray {
    return Json.encodeToString(mapOf(
        "user" to userRepository.findById(userId),
        "invoices" to invoiceRepository.findByUser(userId),
        "expenses" to expenseRepository.findByUser(userId),
        "clients" to clientRepository.findByUser(userId)
    )).toByteArray()
}

// GDPR: Account deletion
suspend fun deleteUserAccount(userId: UUID) {
    // Anonymize audit logs (can't delete for legal compliance)
    auditLogRepository.anonymize(userId)

    // Delete user data
    invoiceRepository.deleteByUser(userId)
    expenseRepository.deleteByUser(userId)
    userRepository.delete(userId)

    // Log deletion
    auditLog(
        action = "user.deleted_account",
        userId = userId,
        reason = "GDPR data deletion request"
    )
}
```

### Belgian Tax Law Compliance

**Requirements:**
- ✅ 7-year audit log retention
- ✅ Immutable financial records
- ✅ VAT calculation accuracy
- ✅ Peppol e-invoicing (from 2026)

---

## Security Checklist

### Pre-Production

- [ ] HTTPS enforced (TLS 1.3)
- [ ] HSTS headers configured
- [ ] Certificate pinning (mobile apps)
- [ ] JWT with short expiry (15 min access, 7 day refresh)
- [ ] Passwords hashed with bcrypt/Argon2id
- [ ] MFA available for all users
- [ ] Multi-tenant isolation verified
- [ ] SQL injection prevention (Exposed ORM)
- [ ] XSS prevention (input sanitization)
- [ ] CSRF tokens for state-changing operations
- [ ] Rate limiting on all endpoints
- [ ] CORS properly configured
- [ ] Secrets in environment variables (never in code)
- [ ] Audit logging for all financial operations
- [ ] Encrypted storage for sensitive fields
- [ ] Dependency scanning (Dependabot)
- [ ] Security headers (CSP, X-Frame-Options)
- [ ] PCI DSS compliance (via Stripe/Mollie)
- [ ] GDPR compliance (export, deletion)
- [ ] Penetration testing completed

### Ongoing

- [ ] Regular security audits
- [ ] Dependency updates
- [ ] Access log monitoring
- [ ] Failed login monitoring
- [ ] Anomaly detection
- [ ] Incident response plan
- [ ] Backup verification
- [ ] Disaster recovery testing

---

## Related Documentation

- [Architecture](./ARCHITECTURE.md) - System architecture
- [Database Schema](./DATABASE.md) - Data model
- [API Reference](./API.md) - API security
- [Deployment](./DEPLOYMENT.md) - Secure deployment

---

**Last Updated:** October 2025
