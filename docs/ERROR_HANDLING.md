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
- [Client-Side Error Handling](#client-side-error-handling)
  - [HTTP Client Configuration](#http-client-configuration)
  - [Response Validation](#response-validation)
  - [Rate Limit Handling](#rate-limit-handling)
  - [Token Refresh on 401](#token-refresh-on-401)
  - [DokusState Error Handling Pattern](#dokusstate-error-handling-pattern)
  - [Retry Handler Pattern](#retry-handler-pattern)
- [Rate Limiting Integration](#rate-limiting-integration)
  - [Overview](#overview-1)
  - [Exception Details](#exception-details)
  - [Backend Configuration](#backend-configuration)
  - [Client Handling Patterns](#client-handling-patterns)
  - [Retry Strategies](#retry-strategies)
  - [UI Considerations](#ui-considerations)
  - [Testing Rate Limiting](#testing-rate-limiting)
  - [Best Practices](#best-practices)
- [Frontend Error Display Patterns](#frontend-error-display-patterns)
  - [Localized Error Messages](#localized-error-messages)
  - [Supported Locales](#supported-locales)
  - [Using the Localized Property](#using-the-localized-property)
  - [Localized Message Examples](#localized-message-examples)
  - [Adding New Translations](#adding-new-translations)
  - [Error UI Components](#error-ui-components)
- [Best Practices and Anti-Patterns](#best-practices-and-anti-patterns)
  - [Backend: Choosing the Right Exception Type](#backend-choosing-the-right-exception-type)
  - [Backend: Exception Chaining with asDokusException](#backend-exception-chaining-with-asdokusexception)
  - [Frontend: Error Handling Patterns](#frontend-error-handling-patterns)
  - [Anti-Patterns to Avoid](#anti-patterns-to-avoid)
- [Troubleshooting](#troubleshooting)
  - [Error ID Tracking](#error-id-tracking)
  - [Common Error Scenarios](#common-error-scenarios)
  - [Logging Patterns for Investigation](#logging-patterns-for-investigation)
  - [Debugging Checklist](#debugging-checklist)

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

## Client-Side Error Handling

The KMP frontend uses Ktor's HTTP client with custom plugins to handle errors consistently across all platforms (Android, iOS, Desktop, Web). This section documents how to configure and use client-side error handling.

### Location

```
foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClientExtensions.kt
foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClient.kt
```

### HTTP Client Configuration

Two HTTP client factory functions are provided:

#### Basic HTTP Client

For unauthenticated requests (login, registration, password reset):

```kotlin
val client = createDynamicBaseHttpClient(endpointProvider)
```

This client includes:
- JSON content negotiation
- Type-safe resources
- Dynamic endpoint configuration (self-hosted server support)
- Logging
- Response validation (error conversion)

#### Authenticated HTTP Client

For authenticated API requests:

```kotlin
val client = createDynamicAuthenticatedHttpClient(
    endpointProvider = endpointProvider,
    tokenManager = tokenManager,
    onAuthenticationFailed = {
        // Navigate to login screen
        navigator.navigateToLogin()
    }
)
```

This client adds:
- Dynamic bearer authentication (token attached per-request)
- Automatic token refresh on 401
- Callback for permanent authentication failure

### Response Validation

The `withResponseValidation()` extension configures an `HttpResponseValidator` that converts HTTP error responses into `DokusException` instances.

```kotlin
fun HttpClientConfig<*>.withResponseValidation() {
    HttpResponseValidator {
        validateResponse { response: HttpResponse ->
            if (response.status.isSuccess()) return@validateResponse

            // Try to parse the response body as DokusException
            runCatching { response.body<DokusException>() }.fold(
                onSuccess = { exception -> throw exception },
                onFailure = {
                    // Fallback for non-JSON responses
                    when (response.status) {
                        HttpStatusCode.Unauthorized -> throw DokusException.NotAuthenticated()
                        HttpStatusCode.Forbidden -> throw DokusException.NotAuthorized()
                        HttpStatusCode.NotFound -> throw DokusException.NotFound()
                        HttpStatusCode.TooManyRequests -> throw DokusException.TooManyLoginAttempts()
                        else -> throw DokusException.Unknown(it)
                    }
                }
            )
        }
    }
}
```

#### How It Works

1. **Success responses (2xx)**: Pass through without modification
2. **Error responses with JSON body**: Deserialize the body as `DokusException` and throw it
3. **Error responses without JSON body**: Map the HTTP status code to an appropriate `DokusException`:

| HTTP Status | Fallback Exception |
|-------------|-------------------|
| 401 | `NotAuthenticated` |
| 403 | `NotAuthorized` |
| 404 | `NotFound` |
| 429 | `TooManyLoginAttempts` |
| Other | `Unknown` |

### Rate Limit Handling

When a rate limit response (429) is received, the client extracts the `Retry-After` header to determine how long to wait before retrying.

```kotlin
// Special handling for TooManyLoginAttempts
if (exception is DokusException.TooManyLoginAttempts) {
    val retryAfter = parseRetryAfterHeader(response)
    throw DokusException.TooManyLoginAttempts(
        retryAfterSeconds = retryAfter ?: exception.retryAfterSeconds
    )
}
```

#### Retry-After Header Parsing

```kotlin
private fun parseRetryAfterHeader(response: HttpResponse): Int? {
    val retryAfter = response.headers[HttpHeaders.RetryAfter] ?: return null
    return retryAfter.toIntOrNull()
}
```

The header value is expected to be an integer representing seconds. If the header is missing or invalid, the default value from the exception (`60` seconds) is used.

#### UI Usage Example

```kotlin
when (val state = loginState.value) {
    is DokusState.Error -> {
        val exception = state.exception
        if (exception is DokusException.TooManyLoginAttempts) {
            Text("Too many attempts. Try again in ${exception.retryAfterSeconds} seconds.")
        } else {
            Text(exception.localized)
        }
    }
    // ...
}
```

### Token Refresh on 401

The `withUnauthorizedRefreshRetry()` plugin automatically handles 401 responses by refreshing the access token and retrying the request. This prevents users from being logged out when their access token expires during normal usage.

```kotlin
fun HttpClientConfig<*>.withUnauthorizedRefreshRetry(
    tokenManager: TokenManager,
    onAuthenticationFailed: suspend () -> Unit = {},
    maxRetries: Int = 1,
)
```

#### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `tokenManager` | `TokenManager` | Manages access and refresh tokens |
| `onAuthenticationFailed` | `suspend () -> Unit` | Called when retry fails (user must re-login) |
| `maxRetries` | `Int` | Maximum retry attempts (default: 1) |

#### Retry Flow

```
Request → 401 Response → Token Refresh → Retry Request
                              ↓
                         If refresh fails
                              ↓
                    onAuthenticationFailed()
                              ↓
                       Throw original exception
```

1. **Initial Request**: Make the API request with current access token
2. **401 Response**: Access token is invalid/expired
3. **Check Retries**: If retry limit reached, call `onAuthenticationFailed()` and throw
4. **Token Comparison**: Check if token has already been refreshed by another request
5. **Token Refresh**: Call `tokenManager.refreshToken(force = true)` to get new token
6. **Retry Request**: Retry the original request with the new token

#### Concurrent Request Handling

When multiple requests fail with 401 simultaneously, the plugin handles this efficiently:

```kotlin
val tokenUsedForFailedRequest = extractBearerToken(request.headers[HttpHeaders.Authorization])
val latestValidToken = tokenManager.getValidAccessToken()

val tokenForRetry = if (latestValidToken != tokenUsedForFailedRequest) {
    // Another request already refreshed the token
    latestValidToken
} else {
    // This request needs to trigger the refresh
    tokenManager.refreshToken(force = true)
}
```

This prevents multiple simultaneous refresh requests when several API calls fail at once.

#### Implementation Details

The plugin uses a custom Ktor plugin that intercepts the `HttpSend` pipeline:

```kotlin
private val UnauthorizedRefreshRetryPlugin = createClientPlugin(
    name = "UnauthorizedRefreshRetry",
    createConfiguration = ::UnauthorizedRefreshRetryConfig
) {
    client.plugin(HttpSend).intercept { request ->
        executeWithUnauthorizedRefreshRetry(
            request = request,
            tokenManager = tokenManager,
            maxRetries = maxRetries,
            onAuthenticationFailed = onAuthenticationFailed
        )
    }
}
```

### DokusState Error Handling Pattern

The KMP frontend uses `DokusState<T>` as a type-safe state machine for all async operations. Errors are wrapped in `DokusException` and paired with a `RetryHandler` for user-initiated retries.

#### Location

```
foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusState.kt
foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusStateSimple.kt
```

#### State Types

```kotlin
interface DokusState<DataType> {
    /** Initial state before any operation starts */
    interface Idle<DataType> : DokusState<DataType>

    /** Operation in progress */
    interface Loading<DataType> : DokusState<DataType>

    /** Operation completed successfully with data */
    interface Success<DataType> : DokusState<DataType> {
        val data: DataType
    }

    /** Operation failed with exception and retry capability */
    interface Error<DataType> : DokusState<DataType> {
        val exception: DokusException
        val retryHandler: RetryHandler
    }
}
```

#### Creating States

Use the companion object factory methods:

```kotlin
// Initial state
val idle = DokusState.idle<User>()

// Loading state
val loading = DokusState.loading<User>()

// Success state with data
val success = DokusState.success(user)

// Error state with exception and retry handler
val error = DokusState.error(exception) { retryOperation() }

// Error from any Throwable (auto-converts to DokusException)
val error = DokusState.error(throwable) { retryOperation() }
```

#### ViewModel Usage Pattern

```kotlin
class LoginViewModel : BaseViewModel<DokusState<Unit>>(DokusState.idle()) {

    fun login(email: Email, password: Password) {
        scope.launch {
            mutableState.emitLoading()

            val result = loginUseCase(email, password)

            result.fold(
                onSuccess = {
                    mutableState.emit(Unit)
                    // Navigate to next screen
                },
                onFailure = { error ->
                    // Emit error with retry handler
                    mutableState.emit(error) { login(email, password) }
                }
            )
        }
    }
}
```

#### Flow Extension Functions

Convenient extensions for emitting states to `MutableStateFlow`:

```kotlin
// Emit idle state
mutableState.emitIdle()

// Emit loading state
mutableState.emitLoading()

// Emit success state with data
mutableState.emit(data)

// Emit error state with throwable and retry handler
mutableState.emit(error) { retryOperation() }
```

#### State Checking Extensions

Use type-safe extension functions to check state:

```kotlin
val state: DokusState<User> = ...

if (state.isIdle()) { /* Initial state */ }
if (state.isLoading()) { /* Show loading indicator */ }
if (state.isSuccess()) { /* Use state.data */ }
if (state.isError()) { /* Show error with state.exception */ }

// Get exception if in error state
val exception: DokusException? = state.exceptionIfError()
```

### Retry Handler Pattern

The `RetryHandler` is a functional interface that encapsulates the retry action for failed operations:

```kotlin
fun interface RetryHandler {
    fun retry()
}
```

#### Why RetryHandler?

1. **Decoupling**: UI components don't need to know how to retry—they just call `retry()`
2. **State Encapsulation**: Retry logic captures the original parameters
3. **Consistency**: Standard retry pattern across all error states

#### UI Usage Example (Compose)

```kotlin
@Composable
fun ContentWithError(state: DokusState<Data>) {
    when (state) {
        is DokusState.Error -> {
            Column {
                DokusErrorText(state.exception)

                if (state.exception.recoverable) {
                    Button(onClick = { state.retryHandler.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }
        // ... other states
    }
}
```

#### Best Practices

1. **Always provide a retry handler**: Even for non-recoverable errors, provide a no-op handler
2. **Capture necessary state**: The retry lambda should capture all parameters needed to retry
3. **Check recoverable flag**: Only show retry UI for `exception.recoverable == true`
4. **Use localized messages**: Display `exception.localized` for user-facing messages

---

## Rate Limiting Integration

This section provides a comprehensive guide for handling `TooManyLoginAttempts` exceptions, including client retry strategies, UI patterns, and integration with the backend rate limiting system.

### Overview

The `TooManyLoginAttempts` exception (HTTP 429) is thrown when a user exceeds the maximum number of login attempts within a time window. Unlike most errors, this exception:

- Is **recoverable** after a waiting period
- Includes a `retryAfterSeconds` field indicating when to retry
- Has a corresponding `Retry-After` HTTP header for standard client handling
- Requires special UI treatment (countdown timer, disabled login button)

### Exception Details

| Property | Value |
|----------|-------|
| **Error Code** | `TOO_MANY_LOGIN_ATTEMPTS` |
| **HTTP Status** | 429 |
| **Recoverable** | Yes |
| **Default Message** | "Too many login attempts. Please try again later." |
| **Additional Fields** | `retryAfterSeconds: Int` (default: 60) |

### Backend Configuration

The auth service implements the following rate limiting defaults:

| Parameter | Value | Description |
|-----------|-------|-------------|
| Maximum Attempts | 5 | Failed attempts before lockout |
| Attempt Window | 15 minutes | Rolling window for counting failures |
| Lockout Duration | 15 minutes | How long the account is locked |

For detailed backend configuration and deployment considerations, see [RATE_LIMITING.md](../features/auth/backend/docs/RATE_LIMITING.md).

### Client Handling Patterns

#### Basic Error Detection

```kotlin
try {
    authService.login(email, password)
} catch (e: DokusException.TooManyLoginAttempts) {
    // Handle rate limit
    val seconds = e.retryAfterSeconds
    showRateLimitMessage(seconds)
}
```

#### Reading the Retry-After Header

The client automatically parses the `Retry-After` header when present:

```kotlin
// HttpClientExtensions handles this automatically
// The retryAfterSeconds value is populated from:
// 1. Retry-After header (if present)
// 2. Response body retryAfterSeconds field
// 3. Default value (60 seconds)
```

#### Complete Login Flow with Rate Limit Handling

```kotlin
class LoginViewModel : BaseViewModel<LoginState>(LoginState.Idle) {

    fun login(email: Email, password: Password) {
        viewModelScope.launch {
            mutableState.emitLoading()

            loginUseCase(email, password).fold(
                onSuccess = { tokens ->
                    mutableState.emit(LoginState.Success(tokens))
                },
                onFailure = { error ->
                    when (error) {
                        is DokusException.TooManyLoginAttempts -> {
                            // Set rate-limited state with countdown
                            mutableState.emit(
                                LoginState.RateLimited(
                                    retryAfterSeconds = error.retryAfterSeconds,
                                    lockedUntil = Clock.System.now() + error.retryAfterSeconds.seconds
                                )
                            )
                        }
                        else -> {
                            mutableState.emit(error) { login(email, password) }
                        }
                    }
                }
            )
        }
    }
}

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val tokens: Tokens) : LoginState
    data class RateLimited(
        val retryAfterSeconds: Int,
        val lockedUntil: Instant
    ) : LoginState
    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler
    ) : LoginState
}
```

### Retry Strategies

#### Simple Countdown Retry

Wait for the exact duration specified, then allow retry:

```kotlin
class RateLimitHandler(
    private val retryAfterSeconds: Int,
    private val onUnlocked: () -> Unit
) {
    private var job: Job? = null

    fun startCountdown(scope: CoroutineScope) {
        job = scope.launch {
            delay(retryAfterSeconds.seconds)
            onUnlocked()
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
```

#### Exponential Backoff for Repeated Rate Limits

If users repeatedly hit rate limits, consider implementing exponential backoff:

```kotlin
class ExponentialBackoffHandler {
    private var consecutiveRateLimits = 0
    private val baseDelay = 60.seconds
    private val maxDelay = 30.minutes

    fun handleRateLimit(serverRetryAfter: Int): Duration {
        consecutiveRateLimits++

        // Use server's retry-after as base, but increase for repeated violations
        val multiplier = 2.0.pow(consecutiveRateLimits - 1).toInt()
        val calculatedDelay = (serverRetryAfter * multiplier).seconds

        return minOf(calculatedDelay, maxDelay)
    }

    fun reset() {
        consecutiveRateLimits = 0
    }
}
```

#### Automatic Retry with Delay

For background operations, implement automatic retry:

```kotlin
suspend fun <T> retryWithRateLimit(
    maxRetries: Int = 3,
    block: suspend () -> T
): T {
    var lastException: DokusException.TooManyLoginAttempts? = null

    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: DokusException.TooManyLoginAttempts) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(e.retryAfterSeconds.seconds)
            }
        }
    }

    throw lastException!!
}
```

### UI Considerations

#### Countdown Timer Display

Show a live countdown timer when rate limited:

```kotlin
@Composable
fun RateLimitCountdown(
    lockedUntil: Instant,
    onUnlocked: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockedUntil) {
        while (true) {
            val now = Clock.System.now()
            val remaining = (lockedUntil - now).inWholeSeconds.toInt()

            if (remaining <= 0) {
                onUnlocked()
                break
            }

            remainingSeconds = remaining
            delay(1.seconds)
        }
    }

    Text(
        text = "Too many login attempts. Try again in ${formatDuration(remainingSeconds)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return when {
        minutes > 0 -> "$minutes min $remainingSeconds sec"
        else -> "$seconds seconds"
    }
}
```

#### Disabled Login Button

Disable the login button while rate limited:

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsState()

    val isRateLimited = state is LoginState.RateLimited
    val isLoading = state is LoginState.Loading

    Button(
        onClick = { viewModel.login(email, password) },
        enabled = !isRateLimited && !isLoading
    ) {
        Text(
            if (isRateLimited) "Please wait..." else "Login"
        )
    }

    if (state is LoginState.RateLimited) {
        val rateLimitState = state as LoginState.RateLimited
        RateLimitCountdown(
            lockedUntil = rateLimitState.lockedUntil,
            onUnlocked = { viewModel.clearRateLimitState() }
        )
    }
}
```

#### Progress Indicator

Optionally show remaining time as a progress indicator:

```kotlin
@Composable
fun RateLimitProgress(
    totalSeconds: Int,
    remainingSeconds: Int
) {
    val progress = 1f - (remainingSeconds.toFloat() / totalSeconds)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("$remainingSeconds seconds remaining")
    }
}
```

#### Error Message Localization

The rate limit error has localized messages in all supported languages:

| Locale | Message |
|--------|---------|
| English | "Too many login attempts. Please try again later." |
| French | "Trop de tentatives de connexion. Veuillez réessayer plus tard." |
| Dutch | "Te veel inlogpogingen. Probeer het later opnieuw." |
| German | "Zu viele Anmeldeversuche. Bitte versuchen Sie es später erneut." |

Use the standard localization pattern:

```kotlin
// Displays the localized message
Text(exception.localized)
```

### Testing Rate Limiting

#### Manual Testing

```bash
# Make 6 rapid login attempts with wrong password
for i in {1..6}; do
  curl -X POST http://localhost:8000/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}' \
    -w "\nHTTP Status: %{http_code}\n"
  echo "---"
done
```

Expected behavior:
- Attempts 1-5: HTTP 401 Unauthorized
- Attempt 6+: HTTP 429 Too Many Requests with `Retry-After` header

#### Unit Testing

```kotlin
@Test
fun `should handle rate limit exception with retry countdown`() = runTest {
    // Arrange
    val exception = DokusException.TooManyLoginAttempts(retryAfterSeconds = 120)
    whenever(loginUseCase(any(), any())).thenReturn(Result.failure(exception))

    // Act
    viewModel.login(email, password)

    // Assert
    val state = viewModel.state.value
    assertTrue(state is LoginState.RateLimited)
    assertEquals(120, (state as LoginState.RateLimited).retryAfterSeconds)
}
```

### Best Practices

1. **Always show remaining time**: Users should know exactly how long they need to wait
2. **Disable the form**: Prevent users from making additional requests while locked
3. **Preserve entered data**: Don't clear the email field—only the password
4. **Use server time**: Trust `retryAfterSeconds` from the response, not local calculations
5. **Handle clock skew**: Add a small buffer (5 seconds) when re-enabling the form
6. **Log for support**: Log rate limit events for customer support investigations
7. **Consider UX copy**: Use friendly language that doesn't blame the user

### Related Documentation

- **Backend Rate Limiting Details**: [features/auth/backend/docs/RATE_LIMITING.md](../features/auth/backend/docs/RATE_LIMITING.md)
- **Exception Definition**: [foundation/domain/.../DokusException.kt](../foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusException.kt) (see `TooManyLoginAttempts`)

---

## Frontend Error Display Patterns

This section documents how to display localized error messages in the Compose UI, including the `DokusException.localized` extension property and supported locales.

### Localized Error Messages

The `DokusException.localized` extension property provides user-friendly, localized error messages for all exception types. It uses Compose Multiplatform's string resources to automatically select the appropriate translation based on the user's device locale settings.

#### Location

```
foundation/design-system/src/commonMain/kotlin/ai/dokus/foundation/design/extensions/DokusExceptionExtensions.kt
```

#### How It Works

The `localized` property is a Composable extension that:

1. **Maps each exception type to a string resource**: Every `DokusException` subtype has a corresponding string resource key (e.g., `exception_invalid_email`, `exception_not_authenticated`)
2. **Automatically resolves the locale**: Uses Compose Multiplatform's `stringResource()` function which reads the device/system locale
3. **Falls back gracefully**: For exceptions with dynamic messages (like `BadRequest`, `InternalError`), uses the exception's `message` property directly

```kotlin
val DokusException.localized: String
    @Composable get() = when (this) {
        // Validation errors → localized string resources
        is DokusException.Validation.InvalidEmail -> stringResource(Res.string.exception_invalid_email)
        is DokusException.Validation.WeakPassword -> stringResource(Res.string.exception_weak_password)
        // ... other specific types

        // Dynamic messages → use the exception's message property
        is DokusException.BadRequest -> message
        is DokusException.InternalError -> errorMessage

        // Unknown errors → fallback to localized generic message
        is DokusException.Unknown -> message ?: stringResource(Res.string.exception_unknown)
    }
```

### Supported Locales

The platform currently supports error message translations for the following locales:

| Locale Code | Language | Region |
|-------------|----------|--------|
| `values/` (default) | English | — |
| `values-de/` | German | Germany |
| `values-es/` | Spanish | Spain |
| `values-fr/` | French | France |
| `values-fr-rBE/` | French | Belgium |
| `values-it/` | Italian | Italy |
| `values-nl/` | Dutch | Netherlands |
| `values-nl-rBE/` | Dutch | Belgium |
| `values-ru/` | Russian | Russia |

#### Resource File Structure

Exception strings are stored in separate `exceptions.xml` files within each locale directory:

```
foundation/design-system/src/commonMain/composeResources/
├── values/
│   └── exceptions.xml         # English (default)
├── values-de/
│   └── exceptions.xml         # German
├── values-fr/
│   └── exceptions.xml         # French
├── values-nl/
│   └── exceptions.xml         # Dutch
└── ...
```

### Using the Localized Property

#### Basic Usage in Composable

The simplest way to display an error message:

```kotlin
@Composable
fun ErrorMessage(exception: DokusException) {
    Text(
        text = exception.localized,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}
```

#### Usage with DokusState

When using the `DokusState` pattern:

```kotlin
@Composable
fun ContentScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is DokusState.Error -> {
            val errorState = state as DokusState.Error<MyData>

            Column {
                // Display localized error message
                Text(
                    text = errorState.exception.localized,
                    color = MaterialTheme.colorScheme.error
                )

                // Show retry button if error is recoverable
                if (errorState.exception.recoverable) {
                    Button(onClick = { errorState.retryHandler.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }
        is DokusState.Success -> { /* ... */ }
        is DokusState.Loading -> { /* ... */ }
        is DokusState.Idle -> { /* ... */ }
    }
}
```

#### Usage in Forms

For validation errors in forms:

```kotlin
@Composable
fun LoginForm(
    onLogin: (Email, Password) -> Unit,
    errorState: DokusException?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = errorState is DokusException.Validation.InvalidEmail,
            supportingText = {
                if (errorState is DokusException.Validation.InvalidEmail) {
                    Text(errorState.localized)
                }
            }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            isError = errorState is DokusException.InvalidCredentials,
            visualTransformation = PasswordVisualTransformation()
        )

        // Show general error message
        errorState?.let { error ->
            Text(
                text = error.localized,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = { onLogin(Email(email), Password(password)) }
        ) {
            Text("Login")
        }
    }
}
```

#### Usage with Snackbar

For transient error notifications:

```kotlin
@Composable
fun ScreenWithSnackbar(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when error occurs
    LaunchedEffect(state) {
        if (state is DokusState.Error) {
            val error = (state as DokusState.Error<*>).exception
            snackbarHostState.showSnackbar(
                message = error.localized,
                actionLabel = if (error.recoverable) "Retry" else null
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Screen content
    }
}
```

### Localized Message Examples

Here's a sample of how the same error appears in different languages:

#### Invalid Email (Validation.InvalidEmail)

| Locale | Message |
|--------|---------|
| English | "Invalid email address" |
| French | "Adresse e-mail invalide" |
| German | "Ungültige E-Mail-Adresse" |
| Dutch | "Ongeldig e-mailadres" |
| Spanish | "Dirección de correo electrónico inválida" |

#### Invalid Credentials (InvalidCredentials)

| Locale | Message |
|--------|---------|
| English | "Invalid email or password" |
| French | "E-mail ou mot de passe invalide" |
| German | "Ungültige E-Mail oder Passwort" |
| Dutch | "Ongeldige e-mail of wachtwoord" |
| Spanish | "Correo electrónico o contraseña inválidos" |

#### Too Many Login Attempts (TooManyLoginAttempts)

| Locale | Message |
|--------|---------|
| English | "Too many login attempts. Please try again later." |
| French | "Trop de tentatives de connexion. Veuillez réessayer plus tard." |
| German | "Zu viele Anmeldeversuche. Bitte versuchen Sie es später erneut." |
| Dutch | "Te veel inlogpogingen. Probeer het later opnieuw." |
| Spanish | "Demasiados intentos de inicio de sesión. Por favor, inténtelo más tarde." |

#### Session Expired (SessionExpired)

| Locale | Message |
|--------|---------|
| English | "Your session has expired. Please log in again." |
| French | "Votre session a expiré. Veuillez vous reconnecter." |
| German | "Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an." |
| Dutch | "Uw sessie is verlopen. Gelieve opnieuw in te loggen." |
| Spanish | "Su sesión ha expirado. Por favor, inicie sesión de nuevo." |

### Adding New Translations

To add translations for a new locale:

1. **Create the locale directory** under `composeResources/`:
   ```
   foundation/design-system/src/commonMain/composeResources/values-{locale}/
   ```

2. **Create `exceptions.xml`** with all exception strings:
   ```xml
   <resources>
       <!-- 400 Validation Errors -->
       <string name="exception_validation_error">Translated message</string>
       <string name="exception_invalid_email">Translated message</string>
       <!-- ... all other exception strings -->
   </resources>
   ```

3. **Copy all keys from the English `exceptions.xml`** and translate each value

4. **Test the translations** by changing your device/emulator locale

#### Exception String Keys Reference

All exception string keys follow the pattern `exception_{snake_case_name}`:

| Exception Type | String Key |
|---------------|------------|
| `Validation.InvalidEmail` | `exception_invalid_email` |
| `Validation.WeakPassword` | `exception_weak_password` |
| `Validation.PasswordDoNotMatch` | `exception_password_do_not_match` |
| `NotAuthenticated` | `exception_not_authenticated` |
| `InvalidCredentials` | `exception_invalid_credentials` |
| `TokenExpired` | `exception_token_expired` |
| `SessionExpired` | `exception_session_expired` |
| `NotAuthorized` | `exception_not_authorized` |
| `NotFound` | `exception_not_found` |
| `TooManyLoginAttempts` | `exception_too_many_login_attempts` |
| `ConnectionError` | `exception_connection_error` |
| `Unknown` | `exception_unknown` |

For the complete list, refer to `foundation/design-system/src/commonMain/composeResources/values/exceptions.xml`.

### Error UI Components

The design system provides ready-to-use error display components that handle localization, styling, and retry functionality automatically.

#### Location

```
foundation/design-system/src/commonMain/kotlin/ai/dokus/foundation/design/components/common/ErrorBox.kt
```

#### DokusErrorText

A simple text component for displaying error messages with proper error styling.

```kotlin
@Composable
fun DokusErrorText(
    text: String,
    modifier: Modifier = Modifier.padding(all = Constrains.Spacing.large)
)

// Convenience overload that uses localized message
@Composable
fun DokusErrorText(
    exception: DokusException,
    modifier: Modifier = Modifier.padding(all = Constrains.Spacing.large)
)
```

**Features:**
- Displays text with `MaterialTheme.colorScheme.error` color
- Center-aligned text
- Default padding for consistent spacing

**Usage Example:**

```kotlin
@Composable
fun LoginError(exception: DokusException) {
    // Option 1: Pass the exception directly (uses localized message)
    DokusErrorText(exception)

    // Option 2: Pass custom text
    DokusErrorText("An error occurred during login")
}
```

#### DokusErrorContent

A comprehensive error display component with icon, optional title, message, and retry button.

```kotlin
@Composable
fun DokusErrorContent(
    text: String,
    retryHandler: RetryHandler?,
    title: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
)

// Convenience overload for DokusException
@Composable
fun DokusErrorContent(
    exception: DokusException,
    retryHandler: RetryHandler?,
    title: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
)
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `text` / `exception` | `String` / `DokusException` | Error message to display (or exception for auto-localization) |
| `retryHandler` | `RetryHandler?` | Callback for retry button (hidden if null) |
| `title` | `String?` | Optional title displayed above the message |
| `compact` | `Boolean` | Use compact layout for inline display (default: false) |
| `modifier` | `Modifier` | Layout modifier (default: fillMaxWidth) |

**Full Mode (compact = false):**
- Large error icon (xxLarge size)
- Optional headline title in error color
- Body text for error message
- Retry button (if handler provided)
- Vertical layout with generous spacing
- Best for: Full-screen error states, empty state errors

**Compact Mode (compact = true):**
- Medium error icon
- Smaller body text
- Retry button (if handler provided)
- Minimal spacing
- Best for: Card-inline errors, section errors, constrained spaces

#### The Recoverable Flag and Retry Button

The `DokusErrorContent` component intelligently handles the retry button based on the exception's `recoverable` property:

```kotlin
// When using the DokusException overload:
DokusErrorContent(
    exception = exception,
    retryHandler = state.retryHandler,  // May be non-null
    compact = true
)

// Internally, the component does:
retryHandler = retryHandler.takeIf { exception.recoverable }
```

**How it works:**
1. You always pass the `retryHandler` from `DokusState.Error`
2. The component checks `exception.recoverable`
3. If `recoverable = false`, the retry button is hidden
4. If `recoverable = true`, the retry button is shown

**Recoverable Exceptions** (retry button shown):
- `ConnectionError` - Network may become available
- `InternalError` - Server issue may be transient
- `TenantCreationFailed` - May succeed on retry
- `TooManyLoginAttempts` - After waiting period
- `SessionExpired` - Re-authentication possible
- `TooManySessions` - After logging out elsewhere

**Non-Recoverable Exceptions** (no retry button):
- `InvalidCredentials` - Wrong password won't become right
- `UserAlreadyExists` - Email is taken
- `NotAuthorized` - Permission denied
- `Validation.*` - Invalid input needs correction
- `TokenInvalid` - Requires re-authentication

#### Usage Examples

##### Full-Screen Error State

```kotlin
@Composable
fun DataScreen(viewModel: DataViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is DokusState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler,
                    title = "Unable to Load Data"
                )
            }
        }
        // ... other states
    }
}
```

##### Card-Inline Error (Compact)

```kotlin
@Composable
fun VatSummaryCard(state: DokusState<VatSummaryData>) {
    Card {
        when (state) {
            is DokusState.Error -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler,
                        compact = true  // Use compact mode for cards
                    )
                }
            }
            is DokusState.Success -> {
                VatSummaryContent(state.data)
            }
            // ... other states
        }
    }
}
```

##### With DokusState Pattern

```kotlin
@Composable
fun WorkspaceSelection(state: DokusState<List<Workspace>>) {
    when (state) {
        is DokusState.Idle, is DokusState.Loading -> {
            CircularProgressIndicator()
        }
        is DokusState.Success -> {
            WorkspaceList(state.data)
        }
        is DokusState.Error -> {
            // Simple inline usage - component handles everything
            DokusErrorContent(state.exception, state.retryHandler)
        }
    }
}
```

##### Custom Error with Text-Only Display

```kotlin
@Composable
fun LoginForm(errorState: DokusException?) {
    Column {
        // Form fields...

        // Show error message below form (no retry button, no icon)
        if (errorState != null) {
            DokusErrorText(errorState)
        }

        // Login button
        Button(onClick = { /* submit */ }) {
            Text("Login")
        }
    }
}
```

#### Component Selection Guide

| Scenario | Component | Mode |
|----------|-----------|------|
| Full-screen error | `DokusErrorContent` | Full (default) |
| Error inside a card | `DokusErrorContent` | Compact |
| Form validation error | `DokusErrorText` | — |
| Inline error message | `DokusErrorText` | — |
| Error with retry option | `DokusErrorContent` | Full or Compact |
| Non-recoverable error | `DokusErrorContent` | Any (button auto-hidden) |

#### Best Practices

1. **Use the DokusException overload** when possible—it handles localization and recoverable flag automatically
2. **Match compact mode to context**: Full for dedicated error screens, compact for inline/card errors
3. **Always pass the retryHandler** from `DokusState.Error`—the component will hide it if not recoverable
4. **Add a title for clarity** when the error is shown in a prominent location
5. **Use DokusErrorText for forms** where you want just the message without the icon and button

---

## Best Practices and Anti-Patterns

This section provides practical guidance for developers on throwing exceptions in backend code and handling them in frontend code.

### Backend: Choosing the Right Exception Type

Selecting the correct exception type ensures consistent API responses and proper client handling. Use this guide when deciding which exception to throw.

#### Decision Tree

```
Is the request malformed or missing required fields?
├── Yes → BadRequest("specific message")
└── No ↓

Is a specific field invalid (email, password, IBAN, etc.)?
├── Yes → Validation.InvalidEmail, Validation.InvalidIban, etc.
└── No ↓

Is the user not authenticated (no token, expired token)?
├── Yes → NotAuthenticated, TokenExpired, or TokenInvalid
└── No ↓

Is the user authenticated but lacks permission?
├── Yes → NotAuthorized
└── No ↓

Does the requested resource not exist?
├── Yes → NotFound or UserNotFound
└── No ↓

Is there a conflict with existing data (duplicate email)?
├── Yes → UserAlreadyExists
└── No ↓

Is this a rate limiting issue?
├── Yes → TooManyLoginAttempts(retryAfterSeconds = ...)
└── No ↓

Is this an external service failure (database, API)?
├── Yes → ConnectionError or InternalError
└── No ↓

Is this feature not yet implemented?
├── Yes → NotImplemented
└── No → InternalError("specific message")
```

#### Exception Selection by Scenario

| Scenario | Exception | Example |
|----------|-----------|---------|
| Invalid request body | `BadRequest` | `throw DokusException.BadRequest("Missing 'amount' field")` |
| Email format invalid | `Validation.InvalidEmail` | `throw DokusException.Validation.InvalidEmail` |
| Password too weak | `Validation.WeakPassword` | `throw DokusException.Validation.WeakPassword` |
| Custom validation failed | `Validation.Generic` | `throw DokusException.Validation.Generic("Invoice date must be in the past")` |
| No auth token provided | `NotAuthenticated` | `throw DokusException.NotAuthenticated()` |
| Wrong email/password | `InvalidCredentials` | `throw DokusException.InvalidCredentials()` |
| Access token expired | `TokenExpired` | `throw DokusException.TokenExpired()` |
| Refresh token expired | `RefreshTokenExpired` | `throw DokusException.RefreshTokenExpired()` |
| User lacks permission | `NotAuthorized` | `throw DokusException.NotAuthorized()` |
| Account deactivated | `AccountInactive` | `throw DokusException.AccountInactive()` |
| Resource ID not found | `NotFound` | `throw DokusException.NotFound()` |
| User ID not found | `UserNotFound` | `throw DokusException.UserNotFound()` |
| Email already registered | `UserAlreadyExists` | `throw DokusException.UserAlreadyExists()` |
| Too many failed logins | `TooManyLoginAttempts` | `throw DokusException.TooManyLoginAttempts(retryAfterSeconds = 900)` |
| Database connection failed | `ConnectionError` | `throw DokusException.ConnectionError()` |
| Unexpected server error | `InternalError` | `throw DokusException.InternalError("Payment processor unavailable")` |
| Feature in development | `NotImplemented` | `throw DokusException.NotImplemented()` |

#### Using `require()` vs Throwing DokusException

Kotlin's `require()` function is automatically mapped to `BadRequest` by the error handler, but throwing specific `DokusException` types provides better client experience:

```kotlin
// ❌ Less specific - client sees generic "BAD_REQUEST"
fun createInvoice(amount: BigDecimal) {
    require(amount > BigDecimal.ZERO) { "Amount must be positive" }
}

// ✅ More specific - client can handle validation specifically
fun createInvoice(amount: BigDecimal) {
    if (amount <= BigDecimal.ZERO) {
        throw DokusException.Validation.InvalidMoney
    }
}

// ✅ For custom validation that doesn't fit existing types
fun createInvoice(date: LocalDate) {
    if (date.isAfter(LocalDate.now())) {
        throw DokusException.Validation.Generic("Invoice date cannot be in the future")
    }
}
```

**When to use `require()`:**
- Internal assertions that indicate programming errors
- Preconditions that should never fail if the API is used correctly

**When to throw `DokusException`:**
- User input validation
- Business rule violations
- Any error the client needs to handle specifically

### Backend: Exception Chaining with asDokusException

The `asDokusException` extension property converts any `Throwable` to an appropriate `DokusException`. This is essential for consistent error handling across the stack.

#### Location

```
foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusExceptionExtensions.kt
```

#### How It Works

```kotlin
val Throwable?.asDokusException: DokusException
    get() {
        return when (this) {
            // Already a DokusException - return as-is
            is DokusException -> this

            // Null - wrap in Unknown
            null -> DokusException.Unknown(this)

            // Try to match known error patterns
            else -> {
                val message = this.message?.lowercase()
                when {
                    message?.contains("failed to connect to") == true -> DokusException.ConnectionError(message)
                    message?.contains("connection refused") == true -> DokusException.ConnectionError(message)
                    message?.contains("connection reset") == true -> DokusException.ConnectionError(message)
                    message?.contains("network is unreachable") == true -> DokusException.ConnectionError(message)
                    // ... other patterns
                    else -> DokusException.Unknown(this)
                }
            }
        }
    }
```

#### Usage in ViewModel/UseCase

Use `asDokusException` when emitting error states to ensure consistent exception types:

```kotlin
class InvoiceViewModel : BaseViewModel<DokusState<Invoice>>(DokusState.idle()) {

    fun loadInvoice(id: String) {
        viewModelScope.launch {
            mutableState.emitLoading()

            invoiceRepository.getById(id).fold(
                onSuccess = { invoice ->
                    mutableState.emit(invoice)
                },
                onFailure = { error ->
                    // Convert any error to DokusException
                    mutableState.emit(error.asDokusException) { loadInvoice(id) }
                }
            )
        }
    }
}
```

#### Usage with Result

The extension also works on `Result<*>`:

```kotlin
val result: Result<Invoice> = invoiceRepository.getById(id)

if (result.isFailure) {
    val dokusException = result.asDokusException
    // dokusException is guaranteed to be a DokusException
}
```

#### Error Pattern Matching

The `asDokusException` extension automatically detects common network errors and converts them to `ConnectionError`:

| Error Message Pattern | Resulting Exception |
|----------------------|---------------------|
| "failed to connect to..." | `ConnectionError` |
| "connection refused" | `ConnectionError` |
| "connection reset" | `ConnectionError` |
| "could not connect to the server" | `ConnectionError` |
| "websocket connection" | `ConnectionError` |
| "network is unreachable" | `ConnectionError` |
| "io.ktor.serialization.jsonconvertexception" | `InternalError("Serialization error")` |
| (other) | `Unknown(originalThrowable)` |

#### Best Practices for Exception Chaining

1. **Always use `asDokusException` at the boundary**: Convert exceptions when they cross module boundaries (e.g., repository → use case → viewmodel)

2. **Preserve the original exception**: The `Unknown` wrapper preserves the original throwable for debugging:
   ```kotlin
   catch (e: Exception) {
       val dokus = e.asDokusException
       // dokus.throwable contains the original exception (if Unknown)
   }
   ```

3. **Don't double-wrap**: The extension handles `DokusException` inputs gracefully by returning them as-is

4. **Use in error state emission**: Always convert before emitting to `DokusState.Error`:
   ```kotlin
   mutableState.emit(error.asDokusException) { retry() }
   // NOT: mutableState.emit(error as? DokusException ?: DokusException.Unknown(error)) { retry() }
   ```

### Frontend: Error Handling Patterns

#### Structured Error Handling in ViewModels

Always handle errors in a structured way that allows the UI to display appropriate messages:

```kotlin
class ProfileViewModel : BaseViewModel<DokusState<Profile>>(DokusState.idle()) {

    fun loadProfile() {
        viewModelScope.launch {
            mutableState.emitLoading()

            profileUseCase().fold(
                onSuccess = { profile ->
                    mutableState.emit(profile)
                },
                onFailure = { error ->
                    // Always convert to DokusException
                    val exception = error.asDokusException

                    // Special handling for specific errors
                    when (exception) {
                        is DokusException.NotAuthenticated,
                        is DokusException.SessionExpired -> {
                            // Navigate to login
                            navigator.navigateToLogin()
                        }
                        else -> {
                            // Emit error state with retry handler
                            mutableState.emit(exception) { loadProfile() }
                        }
                    }
                }
            )
        }
    }
}
```

#### Handling Multiple Exception Types in UI

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is DokusState.Error -> {
            val errorState = state as DokusState.Error<Profile>
            val exception = errorState.exception

            when (exception) {
                is DokusException.ConnectionError -> {
                    // Show offline indicator with retry
                    OfflineContent(
                        message = exception.localized,
                        onRetry = { errorState.retryHandler.retry() }
                    )
                }
                is DokusException.NotAuthorized -> {
                    // Show permission denied without retry
                    PermissionDeniedContent(message = exception.localized)
                }
                else -> {
                    // Default error display
                    DokusErrorContent(
                        exception = exception,
                        retryHandler = errorState.retryHandler
                    )
                }
            }
        }
        // ... other states
    }
}
```

#### Form Validation Error Handling

For forms, display validation errors inline with the fields:

```kotlin
@Composable
fun RegistrationForm(
    onSubmit: (Email, Password) -> Unit,
    errorState: DokusException?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = errorState is DokusException.Validation.InvalidEmail ||
                      errorState is DokusException.UserAlreadyExists,
            supportingText = {
                when (errorState) {
                    is DokusException.Validation.InvalidEmail -> Text(errorState.localized)
                    is DokusException.UserAlreadyExists -> Text(errorState.localized)
                    else -> {}
                }
            }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            isError = errorState is DokusException.Validation.WeakPassword,
            supportingText = {
                if (errorState is DokusException.Validation.WeakPassword) {
                    Text(errorState.localized)
                }
            },
            visualTransformation = PasswordVisualTransformation()
        )

        // Show general errors that aren't field-specific
        errorState?.let { error ->
            if (error !is DokusException.Validation && error !is DokusException.UserAlreadyExists) {
                DokusErrorText(error)
            }
        }

        Button(onClick = { onSubmit(Email(email), Password(password)) }) {
            Text("Register")
        }
    }
}
```

#### Retry Handler Best Practices

1. **Always capture parameters in the retry lambda**:
   ```kotlin
   // ✅ Correct - captures the parameters
   fun loadData(id: String) {
       // ...
       mutableState.emit(error) { loadData(id) }
   }

   // ❌ Incorrect - references potentially stale state
   var currentId: String? = null
   fun loadData(id: String) {
       currentId = id
       // ...
       mutableState.emit(error) { loadData(currentId!!) }
   }
   ```

2. **Check the recoverable flag before showing retry UI**:
   ```kotlin
   if (exception.recoverable) {
       Button(onClick = { retryHandler.retry() }) {
           Text("Try Again")
       }
   }
   ```

3. **Provide feedback during retry**:
   ```kotlin
   Button(
       onClick = {
           viewModel.setRetrying(true)
           retryHandler.retry()
       },
       enabled = !isRetrying
   ) {
       if (isRetrying) {
           CircularProgressIndicator(Modifier.size(16.dp))
       } else {
           Text("Retry")
       }
   }
   ```

### Anti-Patterns to Avoid

#### 1. Catching and Suppressing Exceptions

```kotlin
// ❌ BAD: Exception is lost, no way to debug
try {
    apiService.fetchData()
} catch (e: Exception) {
    // Silently ignored
}

// ✅ GOOD: Exception is converted and handled properly
try {
    apiService.fetchData()
} catch (e: Exception) {
    mutableState.emit(e.asDokusException) { retry() }
}
```

#### 2. Logging Sensitive Information in Error Messages

```kotlin
// ❌ BAD: Password in error message
throw DokusException.BadRequest("Invalid password: $password")

// ✅ GOOD: No sensitive data
throw DokusException.InvalidCredentials()
```

#### 3. Using Generic Exceptions for Specific Errors

```kotlin
// ❌ BAD: Generic exception, client can't handle specifically
throw DokusException.BadRequest("Email is invalid")

// ✅ GOOD: Specific exception, client can show inline validation
throw DokusException.Validation.InvalidEmail
```

#### 4. Throwing Exceptions for Expected Business Logic

```kotlin
// ❌ BAD: Using exceptions for flow control
fun findUser(email: String): User {
    val user = userRepository.findByEmail(email)
    if (user == null) {
        throw DokusException.UserNotFound()  // Expensive stack trace
    }
    return user
}

// ✅ GOOD: Return nullable or Result for expected cases
fun findUser(email: String): User? {
    return userRepository.findByEmail(email)
}

// ✅ ALSO GOOD: Use Result<T> for operations that can fail
fun findUser(email: String): Result<User> {
    val user = userRepository.findByEmail(email)
    return if (user != null) Result.success(user)
           else Result.failure(DokusException.UserNotFound())
}
```

#### 5. Ignoring the Recoverable Flag

```kotlin
// ❌ BAD: Showing retry for non-recoverable errors
Button(onClick = { retry() }) {
    Text("Retry")  // Shows for InvalidCredentials, which won't help
}

// ✅ GOOD: Check recoverable flag
if (exception.recoverable) {
    Button(onClick = { retry() }) {
        Text("Retry")
    }
}
```

#### 6. Not Using Localized Messages

```kotlin
// ❌ BAD: Using raw exception message
Text(exception.message ?: "An error occurred")

// ✅ GOOD: Using localized message
Text(exception.localized)
```

#### 7. Creating DokusException on the Client Side

```kotlin
// ❌ BAD: Client fabricating server errors
throw DokusException.InternalError("Something went wrong")

// ✅ GOOD: Client uses asDokusException for unknown errors
val exception = error.asDokusException
// The server's exception type is preserved
```

#### 8. Double-Wrapping Exceptions

```kotlin
// ❌ BAD: Wrapping DokusException in Unknown
catch (e: Exception) {
    throw DokusException.Unknown(DokusException.Unknown(e))
}

// ✅ GOOD: Use asDokusException which handles this
catch (e: Exception) {
    throw e.asDokusException  // If already DokusException, returns as-is
}
```

#### 9. Not Providing Retry Handler

```kotlin
// ❌ BAD: Error state without retry capability
sealed class ScreenState {
    data class Error(val message: String) : ScreenState()  // No way to retry
}

// ✅ GOOD: Use DokusState which includes RetryHandler
DokusState.error(exception) { loadData() }
```

#### 10. Mixing Exception Handling Styles

```kotlin
// ❌ BAD: Inconsistent error handling
fun loadProfile(): Profile {
    try {
        return api.getProfile()
    } catch (e: IOException) {
        throw e  // Raw exception
    } catch (e: HttpException) {
        throw RuntimeException("HTTP error")  // Different wrapper
    }
}

// ✅ GOOD: Consistent DokusException handling
fun loadProfile(): Result<Profile> {
    return runCatching { api.getProfile() }
        .recoverCatching { e ->
            throw e.asDokusException  // All errors are DokusException
        }
}
```

---

## Troubleshooting

This section provides practical guidance for debugging errors in the Dokus platform, including how to use error IDs for tracking, common error scenarios, and logging patterns for investigation.

### Error ID Tracking

Every `DokusException` instance is automatically assigned a unique error ID in the format `ERR-{UUID}`. This ID is generated when the exception is created and persists through serialization, making it invaluable for tracking errors across the stack.

#### How Error IDs Are Generated

```kotlin
// In DokusException.kt
@OptIn(ExperimentalUuidApi::class)
fun generateErrorId(): String =
    "ERR-${Uuid.random()}"
```

Example error ID: `ERR-550e8400-e29b-41d4-a716-446655440000`

#### Using Error IDs for Support

When users report issues, ask them to provide the error ID displayed in the UI:

```kotlin
@Composable
fun ErrorDetails(exception: DokusException) {
    Column {
        Text(
            text = exception.localized,
            style = MaterialTheme.typography.bodyLarge
        )

        // Show error ID for support reference
        Text(
            text = "Error ID: ${exception.errorId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

#### Correlating Error IDs in Logs

To find an error in backend logs:

```bash
# Search for a specific error ID in logs
grep "ERR-550e8400-e29b-41d4-a716-446655440000" /var/log/dokus/*.log

# Search across all services with kubectl
kubectl logs -l app=dokus-auth --all-containers | grep "ERR-550e8400"

# Using structured logging query (if using JSON logs)
jq 'select(.errorId == "ERR-550e8400-e29b-41d4-a716-446655440000")' logs.json
```

### Common Error Scenarios

#### Scenario 1: "401 Unauthorized" After Login

**Symptoms**: User logs in successfully but subsequent requests return 401.

**Investigation Steps**:

1. **Check token storage**: Verify the access token was stored after login
   ```kotlin
   // Debug logging in TokenManager
   println("Stored access token: ${accessToken.take(20)}...")
   ```

2. **Verify token is being sent**: Enable HTTP logging
   ```kotlin
   withLogging(LogLevel.HEADERS)  // Shows Authorization header
   ```

3. **Check token expiry**: Access tokens may be expired
   ```kotlin
   // Log token expiry during validation
   logger.debug("Token expires at: ${tokenClaims.expiresAt}")
   ```

4. **Verify refresh logic**: Check if refresh token is valid
   ```kotlin
   // In TokenManager
   logger.debug("Attempting token refresh, current token valid: $isValid")
   ```

**Common Causes**:
- Token not being attached to requests (missing `withDynamicBearerAuth`)
- Clock skew between client and server
- Refresh token revoked on another device

---

#### Scenario 2: "ConnectionError" on Mobile

**Symptoms**: App shows "Connection error. Please try again later." intermittently.

**Investigation Steps**:

1. **Check network state**: Verify device connectivity
   ```kotlin
   // Before making request
   if (!connectivityManager.isNetworkAvailable()) {
       // Show offline UI instead of making request
   }
   ```

2. **Review error message details**: The `ConnectionError` message often contains useful info
   ```kotlin
   when (val ex = error.asDokusException) {
       is DokusException.ConnectionError -> {
           logger.debug("Connection error: ${ex.message}")
           // Message might be: "Failed to connect to api.dokus.ai:443"
       }
   }
   ```

3. **Check server health**: Verify the API is reachable
   ```bash
   curl -I https://api.dokus.ai/health
   ```

**Common Causes**:
- Poor network connectivity
- Server downtime or deployment in progress
- Firewall or proxy blocking requests
- DNS resolution issues

---

#### Scenario 3: Validation Errors Not Displaying

**Symptoms**: Form shows generic error instead of specific validation message.

**Investigation Steps**:

1. **Check exception type**: Ensure backend throws specific validation exception
   ```kotlin
   // Backend should throw:
   throw DokusException.Validation.InvalidEmail

   // NOT:
   throw DokusException.BadRequest("Email is invalid")
   ```

2. **Verify UI handles the type**: Check the when clause covers the exception
   ```kotlin
   when (exception) {
       is DokusException.Validation.InvalidEmail -> {
           // This should display inline with the email field
       }
       is DokusException.BadRequest -> {
           // This shows as generic error
       }
   }
   ```

3. **Check serialization**: Ensure response body deserializes correctly
   ```kotlin
   // Enable verbose logging
   withLogging(LogLevel.BODY)
   // Check: {"type":"DokusException.Validation.InvalidEmail",...}
   ```

**Common Causes**:
- Backend throwing generic `BadRequest` instead of specific validation type
- UI `when` clause missing the specific validation type
- Missing `@SerialName` annotation on exception class

---

#### Scenario 4: Rate Limiting Not Showing Countdown

**Symptoms**: User sees "Too many login attempts" but no countdown timer.

**Investigation Steps**:

1. **Check Retry-After header**: Verify backend sends the header
   ```bash
   curl -i -X POST https://api.dokus.ai/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@test.com","password":"wrong"}'
   # Look for: Retry-After: 120
   ```

2. **Verify header parsing**: Check client extracts the value
   ```kotlin
   // In HttpClientExtensions.kt
   val retryAfter = parseRetryAfterHeader(response)
   println("Retry-After header: $retryAfter")
   ```

3. **Check UI state handling**: Ensure view model stores the countdown value
   ```kotlin
   is DokusException.TooManyLoginAttempts -> {
       println("Rate limited for ${exception.retryAfterSeconds} seconds")
       // Update UI state with this value
   }
   ```

**Common Causes**:
- Missing `Retry-After` header in response
- Header value not being parsed (check for string vs int issues)
- UI not observing the rate limit state

---

#### Scenario 5: "Unknown Error" in Production

**Symptoms**: User sees "An unexpected error occurred" with an error ID.

**Investigation Steps**:

1. **Find the error in logs**: Use the error ID
   ```bash
   # The error ID is in the exception
   grep "ERR-abc123" /var/log/dokus/auth.log
   ```

2. **Check for stack trace**: Look for "Unhandled exception" in logs
   ```bash
   grep -A 50 "Unhandled exception" /var/log/dokus/auth.log
   ```

3. **Review the original exception**: The `Unknown` wrapper preserves it
   ```kotlin
   // The throwable field contains the original exception
   if (exception is DokusException.Unknown) {
       logger.error("Original exception", exception.throwable)
   }
   ```

**Common Causes**:
- Unhandled exception in business logic
- Third-party service returning unexpected response
- Database connection issues
- Null pointer or other runtime exceptions

### Logging Patterns for Investigation

#### Backend Logging Levels

The `ErrorHandling.kt` uses different log levels based on exception severity:

```kotlin
// WARN for expected/client errors (DokusException)
logger.warn("DokusException: ${cause.message}")

// ERROR for unexpected/server errors (Throwable)
logger.error("Unhandled exception", cause)
```

#### Structured Logging Best Practices

For better searchability, include the error ID and error code in structured log fields:

```kotlin
// Using SLF4J with MDC (Mapped Diagnostic Context)
MDC.put("errorId", exception.errorId)
MDC.put("errorCode", exception.errorCode)
MDC.put("httpStatus", exception.httpStatusCode.toString())

logger.warn("Request failed", exception)

MDC.clear()
```

This enables queries like:

```bash
# Find all authentication failures
jq 'select(.errorCode == "INVALID_CREDENTIALS")' logs.json

# Find all 5xx errors
jq 'select(.httpStatus | startswith("5"))' logs.json
```

#### Client-Side Error Logging

For debugging mobile/desktop apps, implement structured error logging:

```kotlin
class ErrorLogger(private val analytics: Analytics) {

    fun log(exception: DokusException, context: String) {
        val properties = mapOf(
            "error_id" to exception.errorId,
            "error_code" to exception.errorCode,
            "http_status" to exception.httpStatusCode,
            "recoverable" to exception.recoverable,
            "context" to context
        )

        analytics.track("error_occurred", properties)

        // Also log to console in debug builds
        if (BuildConfig.DEBUG) {
            println("[ERROR] $context: ${exception.errorCode} - ${exception.message}")
        }
    }
}
```

#### Request Correlation

For tracing errors across services, use request correlation IDs:

```kotlin
// Add correlation ID to all requests
install(DefaultRequest) {
    header("X-Correlation-ID", UUID.randomUUID().toString())
}

// In backend, log the correlation ID with errors
val correlationId = call.request.header("X-Correlation-ID")
logger.warn("Error [correlationId=$correlationId]: ${cause.message}")
```

### Debugging Checklist

Use this checklist when investigating production errors:

#### 1. Gather Information

- [ ] Error ID from user report: `ERR-________________`
- [ ] Error code: `________________` (e.g., `INVALID_CREDENTIALS`)
- [ ] HTTP status code: `___` (e.g., 401, 500)
- [ ] Approximate time of occurrence: `________________`
- [ ] User identifier (if available): `________________`
- [ ] Platform (iOS/Android/Web): `________________`

#### 2. Backend Investigation

- [ ] Search logs for error ID: `grep "ERR-..." logs`
- [ ] Check for stack traces near the error
- [ ] Verify exception type thrown matches what client received
- [ ] Check database connectivity at time of error
- [ ] Review recent deployments that might have introduced the issue

#### 3. Client Investigation

- [ ] Check if error is reproducible
- [ ] Verify network connectivity during error
- [ ] Check if token/session is valid
- [ ] Review HTTP response body and headers
- [ ] Check if `asDokusException` conversion preserved the original type

#### 4. Common Fixes

| Error Code | Quick Fix |
|------------|-----------|
| `NOT_AUTHENTICATED` | Check token is being sent; try logging in again |
| `NOT_AUTHORIZED` | Verify user has required permissions |
| `CONNECTION_ERROR` | Check network and server availability |
| `INTERNAL_ERROR` | Review backend logs for root cause |
| `TOO_MANY_LOGIN_ATTEMPTS` | Wait for `retryAfterSeconds` to elapse |
| `VALIDATION_ERROR` | Check input data format matches expected schema |
| `TOKEN_EXPIRED` | Trigger token refresh; check if refresh token is valid |

#### 5. Escalation Path

1. **Tier 1**: Check quick fixes above
2. **Tier 2**: Search logs, reproduce locally
3. **Tier 3**: Involve backend developer with error ID and logs
4. **Tier 4**: Production debugging with tracing enabled

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
- **Exception Chaining (asDokusException)**: [foundation/domain/.../DokusExceptionExtensions.kt](../foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/exceptions/DokusExceptionExtensions.kt)
- **Backend Error Handler**: [foundation/ktor-common/.../ErrorHandling.kt](../foundation/ktor-common/src/main/kotlin/ai/dokus/foundation/ktor/configure/ErrorHandling.kt)
- **Client Error Handler**: [foundation/app-common/.../HttpClientExtensions.kt](../foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClientExtensions.kt)
- **HTTP Client Factory**: [foundation/app-common/.../HttpClient.kt](../foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClient.kt)
- **DokusState**: [foundation/app-common/.../DokusState.kt](../foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusState.kt)
- **RetryHandler**: [foundation/domain/.../RetryHandler.kt](../foundation/domain/src/commonMain/kotlin/ai/dokus/foundation/domain/asbtractions/RetryHandler.kt)
- **Localized Error Messages**: [foundation/design-system/.../DokusExceptionExtensions.kt](../foundation/design-system/src/commonMain/kotlin/ai/dokus/foundation/design/extensions/DokusExceptionExtensions.kt)
- **Error UI Components**: [foundation/design-system/.../ErrorBox.kt](../foundation/design-system/src/commonMain/kotlin/ai/dokus/foundation/design/components/common/ErrorBox.kt)
- **Exception String Resources**: [foundation/design-system/.../composeResources/values/exceptions.xml](../foundation/design-system/src/commonMain/composeResources/values/exceptions.xml)
