package ai.dokus.foundation.domain.exceptions

/**
 * Extension functions for DokusException handling and conversion.
 */

/**
 * Creates a DokusException from an HTTP status code.
 *
 * @param statusCode The HTTP status code
 * @return A DokusException matching the status code, or null if no match is found
 */
fun fromRestStatus(statusCode: Int): DokusException? =
    when (statusCode) {
        401 -> DokusException.NotAuthenticated()
        403 -> DokusException.NotAuthorized()
        else -> null
    }

/**
 * Helper function to create a validation/bad request exception with a custom message.
 * Maps to HTTP 400 Bad Request.
 *
 * @param message The error message
 * @return A validation exception (returns Validation.Other)
 */
@Suppress("FunctionName")
fun DokusException.Companion.BadRequest(message: String = "Bad request"): DokusException =
    DokusException.Validation.Other

/**
 * Helper function to create a not found exception with a custom message.
 * Maps to HTTP 404 Not Found.
 *
 * Note: This creates an InternalError with 500 status code as there is no dedicated
 * 404 exception type in the current DokusException hierarchy. Consider adding a proper
 * NotFound exception type to the sealed class hierarchy.
 *
 * @param message The error message
 * @return An exception indicating resource not found
 */
@Suppress("FunctionName")
fun DokusException.Companion.NotFound(message: String = "Resource not found"): DokusException =
    DokusException.InternalError(message)

/**
 * Converts a Throwable to a DokusException.
 * If the Throwable is already a DokusException, it returns it as-is.
 * Otherwise, it attempts to match the error message to a known exception type,
 * or returns Unknown if no match is found.
 */
val Throwable?.asDokusException: DokusException
    get() = when (this) {
        is DokusException -> this
        null -> DokusException.Unknown(this)
        else -> {
            // Auth-specific error mappings based on message content
            when {
                message?.contains("Invalid credentials", ignoreCase = true) == true ->
                    DokusException.InvalidCredentials()

                message?.contains("Invalid email or password", ignoreCase = true) == true ->
                    DokusException.InvalidCredentials()

                message?.contains("User with email", ignoreCase = true) == true &&
                message?.contains("already exists", ignoreCase = true) == true ->
                    DokusException.UserAlreadyExists()

                message?.contains("Account is inactive", ignoreCase = true) == true ->
                    DokusException.AccountInactive()

                message?.contains("Account is locked", ignoreCase = true) == true ->
                    DokusException.AccountLocked()

                message?.contains("Token expired", ignoreCase = true) == true ->
                    DokusException.TokenExpired()

                message?.contains("Token invalid", ignoreCase = true) == true ->
                    DokusException.TokenInvalid()

                message?.contains("Refresh token expired", ignoreCase = true) == true ->
                    DokusException.RefreshTokenExpired()

                message?.contains("Refresh token revoked", ignoreCase = true) == true ->
                    DokusException.RefreshTokenRevoked()

                message?.contains("Too many login attempts", ignoreCase = true) == true ->
                    DokusException.TooManyLoginAttempts()

                message?.contains("Session expired", ignoreCase = true) == true ->
                    DokusException.SessionExpired()

                message?.contains("Session invalid", ignoreCase = true) == true ->
                    DokusException.SessionInvalid()

                message?.contains("Email already verified", ignoreCase = true) == true ->
                    DokusException.EmailAlreadyVerified()

                message?.contains("Email not verified", ignoreCase = true) == true ->
                    DokusException.EmailNotVerified()

                message?.contains("Password reset token expired", ignoreCase = true) == true ->
                    DokusException.PasswordResetTokenExpired()

                message?.contains("Password reset token invalid", ignoreCase = true) == true ->
                    DokusException.PasswordResetTokenInvalid()

                message?.contains("Email verification token expired", ignoreCase = true) == true ->
                    DokusException.EmailVerificationTokenExpired()

                message?.contains("Email verification token invalid", ignoreCase = true) == true ->
                    DokusException.EmailVerificationTokenInvalid()

                message?.contains("Tenant creation failed", ignoreCase = true) == true ->
                    DokusException.TenantCreationFailed()

                // Connection error mappings
                message?.contains("Failed to connect to", ignoreCase = true) == true ->
                    DokusException.ConnectionError()

                message?.contains("Connection refused", ignoreCase = true) == true ->
                    DokusException.ConnectionError()

                message?.contains("Could not connect to the server", ignoreCase = true) == true ->
                    DokusException.ConnectionError()

                message?.contains("WebSocket connection", ignoreCase = true) == true ->
                    DokusException.ConnectionError()

                else -> DokusException.Unknown(this)
            }
        }
    }

/**
 * Converts a Result to a DokusException.
 * If the Result is successful, it returns InternalError.
 * If the Result is a failure, it converts the exception using asDokusException.
 */
val Result<*>.asDokusException: DokusException
    get() {
        return if (isSuccess) {
            DokusException.InternalError("Result is success. You should not call asDokusException")
        } else {
            exceptionOrNull().asDokusException
        }
    }
