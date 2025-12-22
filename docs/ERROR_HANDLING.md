# Error Handling Reference

This document provides a comprehensive reference for error handling in the Dokus platform, covering all `DokusException` types, their HTTP status mappings, and handling patterns for both backend and frontend developers.

## Table of Contents

- [Overview](#overview)
- [DokusException Types by HTTP Status Code](#dokusexception-types-by-http-status-code)
  - [400 Bad Request](#400-bad-request)
  - [401 Unauthorized](#401-unauthorized)
  - [403 Forbidden](#403-forbidden)
  - [404 Not Found](#404-not-found)
  - [409 Conflict](#409-conflict)
  - [429 Too Many Requests](#429-too-many-requests)
  - [500 Internal Server Error](#500-internal-server-error)
  - [501 Not Implemented](#501-not-implemented)
  - [503 Service Unavailable](#503-service-unavailable)
- [Error Response Format](#error-response-format)
- [Exception Properties](#exception-properties)
- [Backend Error Handling Configuration](#backend-error-handling-configuration)
  - [Installation](#installation)
  - [StatusPages Plugin Configuration](#statuspages-plugin-configuration)
  - [Exception Handler Priority](#exception-handler-priority)
  - [Standard Exception Mappings](#standard-exception-mappings)
  - [Rate Limiting with Retry-After Header](#rate-limiting-with-retry-after-header)
  - [Logging Patterns](#logging-patterns)
  - [JSON Serialization](#json-serialization)

---

## Overview

All errors in the Dokus platform are represented by `DokusException`, a sealed class hierarchy that provides:

- **Type-safe error handling**: Each error type is a distinct class
- **HTTP status mapping**: Every exception maps to an appropriate HTTP status code
- **Unique error codes**: Machine-readable codes for client-side handling
- **Error tracking**: Auto-generated error IDs (`ERR-*`) for debugging
- **Serialization support**: Full Kotlinx Serialization support for API responses
- **Recovery hints**: `recoverable` flag indicates if retry is meaningful

### Location

```
foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusException.kt
```

---

## DokusException Types by HTTP Status Code

### 400 Bad Request

General client errors for invalid requests and validation failures.

#### BadRequest

| Property | Value |
|----------|-------|
| **Error Code** | `BAD_REQUEST` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Bad request" |

**Use Case**: Generic bad request when the client sends malformed or invalid data.

```kotlin
throw DokusException.BadRequest("Missing required field: name")
```

---

#### Validation Errors

All validation errors share HTTP 400 status and the base error code `VALIDATION_ERROR`. They are organized under the `DokusException.Validation` sealed class.

##### Validation.InvalidEmail

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid email address" |

**Use Case**: Email format validation failed.

---

##### Validation.WeakPassword

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Password does not meet security requirements" |

**Use Case**: Password doesn't meet strength requirements.

---

##### Validation.PasswordDoNotMatch

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Passwords do not match" |

**Use Case**: Password confirmation doesn't match the password.

---

##### Validation.InvalidFirstName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid first name" |

**Use Case**: First name validation failed (empty, too long, invalid characters).

---

##### Validation.InvalidLastName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid last name" |

**Use Case**: Last name validation failed.

---

##### Validation.InvalidTaxNumber

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid tax number" |

**Use Case**: Tax identification number format is invalid.

---

##### Validation.InvalidWorkspaceName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid workspace name" |

**Use Case**: Workspace/tenant name validation failed.

---

##### Validation.InvalidLegalName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid legal name" |

**Use Case**: Company legal name validation failed.

---

##### Validation.InvalidDisplayName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid display name" |

**Use Case**: Display name validation failed.

---

##### Validation.InvalidVatNumber

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid VAT number" |

**Use Case**: VAT number format or checksum validation failed.

---

##### Validation.InvalidIban

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid IBAN" |

**Use Case**: IBAN format or checksum validation failed.

---

##### Validation.InvalidBic

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid BIC/SWIFT code" |

**Use Case**: BIC/SWIFT code format validation failed.

---

##### Validation.InvalidPeppolId

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid Peppol ID" |

**Use Case**: Peppol participant ID format validation failed.

---

##### Validation.InvalidInvoiceNumber

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid invoice number" |

**Use Case**: Invoice number format validation failed.

---

##### Validation.InvalidMoney

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid monetary amount" |

**Use Case**: Monetary amount format or value validation failed (e.g., negative amounts).

---

##### Validation.InvalidVatRate

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid VAT rate" |

**Use Case**: VAT rate is not a valid percentage or not in allowed list.

---

##### Validation.InvalidPercentage

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid percentage value" |

**Use Case**: Percentage value out of valid range (0-100).

---

##### Validation.InvalidQuantity

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid quantity" |

**Use Case**: Quantity value validation failed (e.g., negative or zero).

---

##### Validation.InvalidStreetName

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid street name" |

**Use Case**: Street name in address validation failed.

---

##### Validation.InvalidCity

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid city" |

**Use Case**: City name validation failed.

---

##### Validation.InvalidPostalCode

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid postal code" |

**Use Case**: Postal/ZIP code format validation failed.

---

##### Validation.InvalidCountry

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid country" |

**Use Case**: Country code validation failed (ISO 3166-1 alpha-2).

---

##### Validation.ApiKeyRequired

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "API Key is required" |

**Use Case**: API key is missing in Peppol/external service configuration.

---

##### Validation.ApiSecretRequired

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "API Secret is required" |

**Use Case**: API secret is missing in Peppol/external service configuration.

---

##### Validation.InvalidApiCredentials

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Invalid API credentials" |

**Use Case**: API credentials failed validation with external service.

---

##### Validation.MissingVatNumber

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "VAT number is required" |

**Use Case**: VAT number is required for the operation (e.g., Peppol sending).

---

##### Validation.MissingCompanyAddress

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Default Message** | "Company address is required" |

**Use Case**: Company address is required for the operation.

---

##### Validation.Generic

| Property | Value |
|----------|-------|
| **Error Code** | `VALIDATION_ERROR` |
| **HTTP Status** | 400 |
| **Recoverable** | No |
| **Custom Message** | Provided via `errorMessage` parameter |

**Use Case**: Custom validation errors not covered by specific types.

```kotlin
throw DokusException.Validation.Generic("Invoice date cannot be in the future")
```

---

### 401 Unauthorized

Authentication failures. The client must authenticate or re-authenticate.

#### NotAuthenticated

| Property | Value |
|----------|-------|
| **Error Code** | `NOT_AUTHENTICATED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Not authenticated" |

**Use Case**: Request lacks valid authentication credentials.

---

#### InvalidCredentials

| Property | Value |
|----------|-------|
| **Error Code** | `INVALID_CREDENTIALS` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Invalid email or password" |

**Use Case**: Login failed due to wrong email or password.

---

#### TokenExpired

| Property | Value |
|----------|-------|
| **Error Code** | `TOKEN_EXPIRED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Authentication token has expired" |

**Use Case**: JWT access token has expired.

---

#### TokenInvalid

| Property | Value |
|----------|-------|
| **Error Code** | `TOKEN_INVALID` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Invalid authentication token" |

**Use Case**: JWT token signature verification failed or token is malformed.

---

#### RefreshTokenExpired

| Property | Value |
|----------|-------|
| **Error Code** | `REFRESH_TOKEN_EXPIRED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Refresh token has expired. Please log in again." |

**Use Case**: Refresh token has expired; user must re-login.

---

#### RefreshTokenRevoked

| Property | Value |
|----------|-------|
| **Error Code** | `REFRESH_TOKEN_REVOKED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Refresh token has been revoked. Please log in again." |

**Use Case**: Refresh token was explicitly revoked (e.g., logout, security action).

---

#### SessionExpired

| Property | Value |
|----------|-------|
| **Error Code** | `SESSION_EXPIRED` |
| **HTTP Status** | 401 |
| **Recoverable** | Yes |
| **Default Message** | "Your session has expired. Please log in again." |

**Use Case**: User session has expired. User should be redirected to login.

---

#### SessionInvalid

| Property | Value |
|----------|-------|
| **Error Code** | `SESSION_INVALID` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Invalid session. Please log in again." |

**Use Case**: Session ID is invalid or was revoked.

---

#### PasswordResetTokenExpired

| Property | Value |
|----------|-------|
| **Error Code** | `PASSWORD_RESET_TOKEN_EXPIRED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Password reset token has expired. Please request a new one." |

**Use Case**: Password reset link has expired.

---

#### PasswordResetTokenInvalid

| Property | Value |
|----------|-------|
| **Error Code** | `PASSWORD_RESET_TOKEN_INVALID` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Invalid password reset token. Please request a new one." |

**Use Case**: Password reset token is invalid or was already used.

---

#### EmailVerificationTokenExpired

| Property | Value |
|----------|-------|
| **Error Code** | `EMAIL_VERIFICATION_TOKEN_EXPIRED` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Email verification token has expired. Please request a new one." |

**Use Case**: Email verification link has expired.

---

#### EmailVerificationTokenInvalid

| Property | Value |
|----------|-------|
| **Error Code** | `EMAIL_VERIFICATION_TOKEN_INVALID` |
| **HTTP Status** | 401 |
| **Recoverable** | No |
| **Default Message** | "Invalid email verification token. Please request a new one." |

**Use Case**: Email verification token is invalid or was already used.

---

### 403 Forbidden

Authorization failures. The client is authenticated but lacks permission.

#### NotAuthorized

| Property | Value |
|----------|-------|
| **Error Code** | `NOT_AUTHORIZED` |
| **HTTP Status** | 403 |
| **Recoverable** | No |
| **Default Message** | "You do not have permission to access this resource" |

**Use Case**: User lacks required permissions for the requested operation.

---

#### AccountInactive

| Property | Value |
|----------|-------|
| **Error Code** | `ACCOUNT_INACTIVE` |
| **HTTP Status** | 403 |
| **Recoverable** | No |
| **Default Message** | "Your account is inactive. Please contact support." |

**Use Case**: User account has been deactivated.

---

#### AccountLocked

| Property | Value |
|----------|-------|
| **Error Code** | `ACCOUNT_LOCKED` |
| **HTTP Status** | 403 |
| **Recoverable** | No |
| **Default Message** | "Your account has been locked. Please contact support to unlock it." |

**Use Case**: User account has been locked (e.g., security violation).

---

#### EmailNotVerified

| Property | Value |
|----------|-------|
| **Error Code** | `EMAIL_NOT_VERIFIED` |
| **HTTP Status** | 403 |
| **Recoverable** | No |
| **Default Message** | "Please verify your email address to continue" |

**Use Case**: Action requires verified email but user hasn't verified yet.

---

#### EmailAlreadyVerified

| Property | Value |
|----------|-------|
| **Error Code** | `EMAIL_ALREADY_VERIFIED` |
| **HTTP Status** | 403 |
| **Recoverable** | No |
| **Default Message** | "Email address has already been verified" |

**Use Case**: User attempted to verify an already-verified email.

---

#### TooManySessions

| Property | Value |
|----------|-------|
| **Error Code** | `TOO_MANY_SESSIONS` |
| **HTTP Status** | 403 |
| **Recoverable** | Yes |
| **Default Message** | "Maximum number of concurrent sessions reached. Please log out from another device." |
| **Additional Fields** | `maxSessions: Int` (default: 5) |

**Use Case**: User has reached the maximum number of concurrent sessions.

---

### 404 Not Found

Requested resource does not exist.

#### NotFound

| Property | Value |
|----------|-------|
| **Error Code** | `RESOURCE_NOT_FOUND` |
| **HTTP Status** | 404 |
| **Recoverable** | No |
| **Default Message** | "Resource was not found" |

**Use Case**: Generic resource not found.

---

#### UserNotFound

| Property | Value |
|----------|-------|
| **Error Code** | `USER_NOT_FOUND` |
| **HTTP Status** | 404 |
| **Recoverable** | No |
| **Default Message** | "User not found" |

**Use Case**: Requested user does not exist.

---

### 409 Conflict

Request conflicts with current state of the resource.

#### UserAlreadyExists

| Property | Value |
|----------|-------|
| **Error Code** | `USER_ALREADY_EXISTS` |
| **HTTP Status** | 409 |
| **Recoverable** | No |
| **Default Message** | "A user with this email already exists" |

**Use Case**: Attempted to register with an email that's already taken.

---

### 429 Too Many Requests

Rate limiting and throttling.

#### TooManyLoginAttempts

| Property | Value |
|----------|-------|
| **Error Code** | `TOO_MANY_LOGIN_ATTEMPTS` |
| **HTTP Status** | 429 |
| **Recoverable** | Yes |
| **Default Message** | "Too many login attempts. Please try again later." |
| **Additional Fields** | `retryAfterSeconds: Int` (default: 60) |

**Use Case**: User exceeded maximum login attempts and is temporarily locked out.

**Special Handling**: The backend sets the `Retry-After` HTTP header with the lockout duration in seconds.

**Related Documentation**: See [RATE_LIMITING.md](../features/auth/backend/docs/RATE_LIMITING.md) for detailed rate limiting configuration.

---

### 500 Internal Server Error

Server-side errors indicating something went wrong.

#### InternalError

| Property | Value |
|----------|-------|
| **Error Code** | `INTERNAL_ERROR` |
| **HTTP Status** | 500 |
| **Recoverable** | Yes |
| **Custom Message** | Provided via `errorMessage` parameter |

**Use Case**: Unexpected server error; client may retry.

```kotlin
throw DokusException.InternalError("Database connection failed")
```

---

#### TenantCreationFailed

| Property | Value |
|----------|-------|
| **Error Code** | `TENANT_CREATION_FAILED` |
| **HTTP Status** | 500 |
| **Recoverable** | Yes |
| **Default Message** | "Failed to create tenant. Please try again." |

**Use Case**: Workspace/tenant creation failed due to server error.

---

#### Unknown

| Property | Value |
|----------|-------|
| **Error Code** | `UNKNOWN_ERROR` |
| **HTTP Status** | 500 |
| **Recoverable** | No |
| **Custom Message** | Derived from wrapped `throwable` |

**Use Case**: Wraps unexpected exceptions for consistent error response format.

```kotlin
throw DokusException.Unknown(originalException)
```

---

### 501 Not Implemented

Feature is not yet available.

#### NotImplemented

| Property | Value |
|----------|-------|
| **Error Code** | `NOT_IMPLEMENTED` |
| **HTTP Status** | 501 |
| **Recoverable** | No |
| **Default Message** | "This feature is not yet implemented." |

**Use Case**: Endpoint or feature exists but is not yet implemented.

---

### 503 Service Unavailable

Service is temporarily unavailable.

#### ConnectionError

| Property | Value |
|----------|-------|
| **Error Code** | `CONNECTION_ERROR` |
| **HTTP Status** | 503 |
| **Recoverable** | Yes |
| **Default Message** | "Connection error. Please try again later." |

**Use Case**: Failed to connect to downstream service (database, external API, etc.).

---

## Error Response Format

All errors are returned as JSON with the following structure:

```json
{
  "httpStatusCode": 400,
  "errorCode": "VALIDATION_ERROR",
  "recoverable": false,
  "errorId": "ERR-550e8400-e29b-41d4-a716-446655440000",
  "message": "Invalid email address"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `httpStatusCode` | `Int` | HTTP status code (matches response status) |
| `errorCode` | `String` | Machine-readable error code for client handling |
| `recoverable` | `Boolean` | Whether the client should offer a retry option |
| `errorId` | `String` | Unique error ID for debugging (`ERR-{UUID}`) |
| `message` | `String?` | Human-readable error message |

### Special Response Fields

Some exceptions include additional fields:

| Exception | Field | Description |
|-----------|-------|-------------|
| `TooManyLoginAttempts` | `retryAfterSeconds` | Seconds until retry is allowed |
| `TooManySessions` | `maxSessions` | Maximum allowed concurrent sessions |
| `Validation.Generic` | `errorMessage` | Custom validation error message |
| `InternalError` | `errorMessage` | Server error description |

---

## Exception Properties

All `DokusException` types share these base properties:

| Property | Type | Description |
|----------|------|-------------|
| `httpStatusCode` | `Int` | HTTP status code to return |
| `errorCode` | `String` | Machine-readable error identifier |
| `recoverable` | `Boolean` | Whether retry is meaningful |
| `errorId` | `String` | Auto-generated unique ID (`ERR-{UUID}`) |
| `message` | `String?` | Human-readable description |

### Error ID Format

Error IDs follow the pattern `ERR-{UUID}` and are automatically generated for each exception instance:

```
ERR-550e8400-e29b-41d4-a716-446655440000
```

Use this ID when investigating errors in logs or when users report issues.

---

## Backend Error Handling Configuration

Backend services use Ktor's [StatusPages](https://ktor.io/docs/status-pages.html) plugin to convert exceptions into consistent JSON error responses. This section documents how to configure error handling in your backend service.

### Location

```
foundation/ktor-common/src/main/kotlin/ai/dokus/foundation/ktor/configure/ErrorHandling.kt
```

### Installation

To enable error handling in your Ktor application, call `configureErrorHandling()` during application setup:

```kotlin
fun Application.module() {
    // Configure error handling first to catch all exceptions
    configureErrorHandling()

    // Other configuration...
    configureSerialization()
    configureRouting()
}
```

### StatusPages Plugin Configuration

The `configureErrorHandling()` function installs the StatusPages plugin with exception handlers in a specific priority order. The order matters because Ktor uses the most specific handler that matches the thrown exception.

```kotlin
fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandler")

    install(StatusPages) {
        // 1. Rate limiting with special Retry-After header
        exception<DokusException.TooManyLoginAttempts> { call, cause ->
            logger.warn("Rate limit exceeded: ${cause.message}")
            call.response.headers.append(HttpHeaders.RetryAfter, cause.retryAfterSeconds.toString())
            call.respond<DokusException>(HttpStatusCode.TooManyRequests, cause)
        }

        // 2. All other DokusException types
        exception<DokusException> { call, cause ->
            logger.warn("DokusException: ${cause.message}")
            call.respond<DokusException>(
                HttpStatusCode.fromValue(cause.httpStatusCode),
                cause,
            )
        }

        // 3. Map Java/Kotlin standard exceptions
        exception<IllegalArgumentException> { call, cause -> ... }
        exception<NoSuchElementException> { call, cause -> ... }

        // 4. Map network/connection exceptions
        exception<ConnectException> { call, cause -> ... }
        exception<SocketTimeoutException> { call, cause -> ... }

        // 5. Catch-all for unexpected exceptions
        exception<Throwable> { call, cause -> ... }
    }
}
```

### Exception Handler Priority

Handlers are matched in order of specificity. More specific exception types should be registered before their parent types:

| Priority | Exception Type | Why First? |
|----------|----------------|------------|
| 1 | `DokusException.TooManyLoginAttempts` | Adds special `Retry-After` header |
| 2 | `DokusException` | All custom application exceptions |
| 3 | `IllegalArgumentException` | Common validation failures from stdlib |
| 4 | `NoSuchElementException` | Collection access failures |
| 5 | `ConnectException` | Downstream service connection failures |
| 6 | `SocketTimeoutException` | Downstream service timeouts |
| 7 | `Throwable` | Catch-all for unexpected errors |

### Standard Exception Mappings

The error handler automatically maps common Java/Kotlin exceptions to appropriate DokusException types:

#### IllegalArgumentException → BadRequest (400)

When business logic throws `IllegalArgumentException` (e.g., from `require()` calls), it's mapped to a `BadRequest`:

```kotlin
// In your service code:
fun createInvoice(amount: BigDecimal) {
    require(amount > BigDecimal.ZERO) { "Amount must be positive" }
    // ...
}

// If requirement fails, client receives:
// HTTP 400
// {
//   "httpStatusCode": 400,
//   "errorCode": "BAD_REQUEST",
//   "recoverable": false,
//   "errorId": "ERR-...",
//   "message": "Amount must be positive"
// }
```

#### NoSuchElementException → NotFound (404)

When code throws `NoSuchElementException` (e.g., from `.first()`, `.single()`, or `Map.getValue()`), it's mapped to `NotFound`:

```kotlin
// In your repository code:
fun getById(id: UUID): Entity =
    entities.first { it.id == id }  // Throws NoSuchElementException if not found

// Client receives:
// HTTP 404
// {
//   "httpStatusCode": 404,
//   "errorCode": "RESOURCE_NOT_FOUND",
//   "recoverable": false,
//   "errorId": "ERR-...",
//   "message": "Resource was not found"
// }
```

#### ConnectException → ServiceUnavailable (503)

Connection failures to downstream services (database, external APIs) are mapped to `ConnectionError`:

```kotlin
// If database connection fails:
// HTTP 503
// {
//   "httpStatusCode": 503,
//   "errorCode": "CONNECTION_ERROR",
//   "recoverable": true,
//   "errorId": "ERR-...",
//   "message": "Downstream service is unavailable"
// }
```

#### SocketTimeoutException → GatewayTimeout (504)

Request timeouts to downstream services are mapped to a gateway timeout:

```kotlin
// If external API times out:
// HTTP 504
// {
//   "httpStatusCode": 503,
//   "errorCode": "CONNECTION_ERROR",
//   "recoverable": true,
//   "errorId": "ERR-...",
//   "message": "Downstream service timed out"
// }
```

> **Note**: The error code is `CONNECTION_ERROR` with HTTP 504, indicating a transient issue that may succeed on retry.

#### Throwable → InternalError (500)

Any unhandled exception is caught and wrapped in an `InternalError`:

```kotlin
// Any unexpected exception:
// HTTP 500
// {
//   "httpStatusCode": 500,
//   "errorCode": "INTERNAL_ERROR",
//   "recoverable": true,
//   "errorId": "ERR-...",
//   "message": "An unexpected error occurred"
// }
```

### Rate Limiting with Retry-After Header

The `TooManyLoginAttempts` exception receives special handling to include the HTTP `Retry-After` header:

```kotlin
exception<DokusException.TooManyLoginAttempts> { call, cause ->
    call.response.headers.append(HttpHeaders.RetryAfter, cause.retryAfterSeconds.toString())
    call.respond<DokusException>(HttpStatusCode.TooManyRequests, cause)
}
```

**Response Headers:**
```
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/json
```

**Response Body:**
```json
{
  "httpStatusCode": 429,
  "errorCode": "TOO_MANY_LOGIN_ATTEMPTS",
  "recoverable": true,
  "errorId": "ERR-...",
  "message": "Too many login attempts. Please try again later.",
  "retryAfterSeconds": 60
}
```

Clients should read the `Retry-After` header (or `retryAfterSeconds` field) to know when to retry.

### Logging Patterns

All exceptions are logged with appropriate severity levels:

| Log Level | Exception Types | Reason |
|-----------|-----------------|--------|
| `warn` | `DokusException`, `IllegalArgumentException`, `NoSuchElementException` | Expected client errors |
| `error` | `ConnectException`, `SocketTimeoutException`, `Throwable` | Infrastructure or unexpected errors |

Log messages include the error ID for correlation:

```kotlin
logger.warn("DokusException: ${cause.message}")  // Logs error ID in message
logger.error("Unhandled exception", cause)       // Includes full stack trace
```

### JSON Serialization

Responses use Kotlinx Serialization. Ensure your application has serialization configured:

```kotlin
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            encodeDefaults = true
        })
    }
}
```

The `DokusException` class is annotated with `@Serializable`, ensuring all subclasses serialize correctly with their additional fields (e.g., `retryAfterSeconds`, `maxSessions`).

---

## Quick Reference Table

| HTTP Status | Exception Type | Error Code | Recoverable |
|-------------|----------------|------------|-------------|
| 400 | `BadRequest` | `BAD_REQUEST` | No |
| 400 | `Validation.*` | `VALIDATION_ERROR` | No |
| 401 | `NotAuthenticated` | `NOT_AUTHENTICATED` | No |
| 401 | `InvalidCredentials` | `INVALID_CREDENTIALS` | No |
| 401 | `TokenExpired` | `TOKEN_EXPIRED` | No |
| 401 | `TokenInvalid` | `TOKEN_INVALID` | No |
| 401 | `RefreshTokenExpired` | `REFRESH_TOKEN_EXPIRED` | No |
| 401 | `RefreshTokenRevoked` | `REFRESH_TOKEN_REVOKED` | No |
| 401 | `SessionExpired` | `SESSION_EXPIRED` | Yes |
| 401 | `SessionInvalid` | `SESSION_INVALID` | No |
| 401 | `PasswordResetTokenExpired` | `PASSWORD_RESET_TOKEN_EXPIRED` | No |
| 401 | `PasswordResetTokenInvalid` | `PASSWORD_RESET_TOKEN_INVALID` | No |
| 401 | `EmailVerificationTokenExpired` | `EMAIL_VERIFICATION_TOKEN_EXPIRED` | No |
| 401 | `EmailVerificationTokenInvalid` | `EMAIL_VERIFICATION_TOKEN_INVALID` | No |
| 403 | `NotAuthorized` | `NOT_AUTHORIZED` | No |
| 403 | `AccountInactive` | `ACCOUNT_INACTIVE` | No |
| 403 | `AccountLocked` | `ACCOUNT_LOCKED` | No |
| 403 | `EmailNotVerified` | `EMAIL_NOT_VERIFIED` | No |
| 403 | `EmailAlreadyVerified` | `EMAIL_ALREADY_VERIFIED` | No |
| 403 | `TooManySessions` | `TOO_MANY_SESSIONS` | Yes |
| 404 | `NotFound` | `RESOURCE_NOT_FOUND` | No |
| 404 | `UserNotFound` | `USER_NOT_FOUND` | No |
| 409 | `UserAlreadyExists` | `USER_ALREADY_EXISTS` | No |
| 429 | `TooManyLoginAttempts` | `TOO_MANY_LOGIN_ATTEMPTS` | Yes |
| 500 | `InternalError` | `INTERNAL_ERROR` | Yes |
| 500 | `TenantCreationFailed` | `TENANT_CREATION_FAILED` | Yes |
| 500 | `Unknown` | `UNKNOWN_ERROR` | No |
| 501 | `NotImplemented` | `NOT_IMPLEMENTED` | No |
| 503 | `ConnectionError` | `CONNECTION_ERROR` | Yes |

---

## Related Documentation

- **Rate Limiting**: [features/auth/backend/docs/RATE_LIMITING.md](../features/auth/backend/docs/RATE_LIMITING.md)
- **DokusException Source**: [foundation/domain/.../DokusException.kt](../foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusException.kt)
- **Backend Error Handler**: [foundation/ktor-common/.../ErrorHandling.kt](../foundation/ktor-common/src/main/kotlin/ai/dokus/foundation/ktor/configure/ErrorHandling.kt)
- **Client Error Handler**: [foundation/app-common/.../HttpClientExtensions.kt](../foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClientExtensions.kt)
