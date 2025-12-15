package ai.dokus.foundation.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
sealed class DokusException(
    val httpStatusCode: Int,
    val errorCode: String,
    val recoverable: Boolean = false,
    val errorId: String = generateErrorId(),
) : Throwable() {

    @Serializable
    @SerialName("DokusException.BadRequest")
    data class BadRequest(
        override val message: String = "Bad request",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 400
            const val ERROR_CODE = "BAD_REQUEST"
        }
    }

    // 400 Bad Request - Validation Errors
    @Serializable
    @SerialName("DokusException.Validation")
    sealed class Validation(
        override val message: String? = "Validation error",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 400
            const val ERROR_CODE = "VALIDATION_ERROR"
        }

        @Serializable
        @SerialName("DokusException.Validation.InvalidEmail")
        data object InvalidEmail : Validation(
            message = "Invalid email address",
        )

        @Serializable
        @SerialName("DokusException.Validation.WeakPassword")
        data object WeakPassword : Validation(
            message = "Password does not meet security requirements",
        )

        @Serializable
        @SerialName("DokusException.Validation.PasswordDoNotMatch")
        data object PasswordDoNotMatch : Validation(
            message = "Passwords do not match",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidFirstName")
        data object InvalidFirstName : Validation(
            message = "Invalid first name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidLastName")
        data object InvalidLastName : Validation(
            message = "Invalid last name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidTaxNumber")
        data object InvalidTaxNumber : Validation(
            message = "Invalid tax number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidWorkspaceName")
        data object InvalidWorkspaceName : Validation(
            message = "Invalid workspace name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidLegalName")
        data object InvalidLegalName : Validation(
            message = "Invalid legal name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidDisplayName")
        data object InvalidDisplayName : Validation(
            message = "Invalid display name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidVatNumber")
        data object InvalidVatNumber : Validation(
            message = "Invalid VAT number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidIban")
        data object InvalidIban : Validation(
            message = "Invalid IBAN",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidBic")
        data object InvalidBic : Validation(
            message = "Invalid BIC/SWIFT code",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPeppolId")
        data object InvalidPeppolId : Validation(
            message = "Invalid Peppol ID",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidInvoiceNumber")
        data object InvalidInvoiceNumber : Validation(
            message = "Invalid invoice number",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidMoney")
        data object InvalidMoney : Validation(
            message = "Invalid monetary amount",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidVatRate")
        data object InvalidVatRate : Validation(
            message = "Invalid VAT rate",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPercentage")
        data object InvalidPercentage : Validation(
            message = "Invalid percentage value",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidQuantity")
        data object InvalidQuantity : Validation(
            message = "Invalid quantity",
        )

        // Address Validation Errors
        @Serializable
        @SerialName("DokusException.Validation.InvalidStreetName")
        data object InvalidStreetName : Validation(
            message = "Invalid street name",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidCity")
        data object InvalidCity : Validation(
            message = "Invalid city",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidPostalCode")
        data object InvalidPostalCode : Validation(
            message = "Invalid postal code",
        )

        @Serializable
        @SerialName("DokusException.Validation.InvalidCountry")
        data object InvalidCountry : Validation(
            message = "Invalid country",
        )

        @Serializable
        @SerialName("DokusException.Validation.Generic")
        data class Generic(
            val errorMessage: String,
        ) : Validation(message = errorMessage)
    }

    // 401 Unauthorized - Authentication Errors
    @Serializable
    @SerialName("DokusException.NotAuthenticated")
    data class NotAuthenticated(
        override val message: String? = "Not authenticated",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "NOT_AUTHENTICATED"
        }
    }

    @Serializable
    @SerialName("DokusException.InvalidCredentials")
    data class InvalidCredentials(
        override val message: String? = "Invalid email or password",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "INVALID_CREDENTIALS"
        }
    }

    @Serializable
    @SerialName("DokusException.TokenExpired")
    data class TokenExpired(
        override val message: String? = "Authentication token has expired",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.TokenInvalid")
    data class TokenInvalid(
        override val message: String? = "Invalid authentication token",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "TOKEN_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.RefreshTokenExpired")
    data class RefreshTokenExpired(
        override val message: String? = "Refresh token has expired. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "REFRESH_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.RefreshTokenRevoked")
    data class RefreshTokenRevoked(
        override val message: String? = "Refresh token has been revoked. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "REFRESH_TOKEN_REVOKED"
        }
    }

    @Serializable
    @SerialName("DokusException.SessionExpired")
    data class SessionExpired(
        override val message: String? = "Your session has expired. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "SESSION_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.SessionInvalid")
    data class SessionInvalid(
        override val message: String? = "Invalid session. Please log in again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "SESSION_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.PasswordResetTokenExpired")
    data class PasswordResetTokenExpired(
        override val message: String? = "Password reset token has expired. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "PASSWORD_RESET_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.PasswordResetTokenInvalid")
    data class PasswordResetTokenInvalid(
        override val message: String? = "Invalid password reset token. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "PASSWORD_RESET_TOKEN_INVALID"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailVerificationTokenExpired")
    data class EmailVerificationTokenExpired(
        override val message: String? = "Email verification token has expired. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "EMAIL_VERIFICATION_TOKEN_EXPIRED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailVerificationTokenInvalid")
    data class EmailVerificationTokenInvalid(
        override val message: String? = "Invalid email verification token. Please request a new one.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 401
            const val ERROR_CODE = "EMAIL_VERIFICATION_TOKEN_INVALID"
        }
    }

    // 403 Forbidden - Authorization Errors
    @Serializable
    @SerialName("DokusException.NotAuthorized")
    data class NotAuthorized(
        override val message: String? = "You do not have permission to access this resource",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "NOT_AUTHORIZED"
        }
    }

    @Serializable
    @SerialName("DokusException.AccountInactive")
    data class AccountInactive(
        override val message: String? = "Your account is inactive. Please contact support.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "ACCOUNT_INACTIVE"
        }
    }

    @Serializable
    @SerialName("DokusException.AccountLocked")
    data class AccountLocked(
        override val message: String? = "Your account has been locked. Please contact support to unlock it.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "ACCOUNT_LOCKED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailNotVerified")
    data class EmailNotVerified(
        override val message: String? = "Please verify your email address to continue",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "EMAIL_NOT_VERIFIED"
        }
    }

    @Serializable
    @SerialName("DokusException.EmailAlreadyVerified")
    data class EmailAlreadyVerified(
        override val message: String? = "Email address has already been verified",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 403
            const val ERROR_CODE = "EMAIL_ALREADY_VERIFIED"
        }
    }

    // 409 Conflict - Duplicate Resources
    @Serializable
    @SerialName("DokusException.UserAlreadyExists")
    data class UserAlreadyExists(
        override val message: String? = "A user with this email already exists",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
    ) {
        companion object {
            const val HTTP_STATUS = 409
            const val ERROR_CODE = "USER_ALREADY_EXISTS"
        }
    }

    // 404 Not Found
    @Serializable
    @SerialName("DokusException.UserNotFound")
    data class UserNotFound(
        override val message: String? = "User not found",
    ) : DokusException(
        httpStatusCode = 404,
        errorCode = "USER_NOT_FOUND",
    )

    @Serializable
    @SerialName("DokusException.NotFound")
    data class NotFound(
        override val message: String? = "Resource was not found",
    ) : DokusException(
        httpStatusCode = 404,
        errorCode = "RESOURCE_NOT_FOUND",
    )

    // 429 Too Many Requests
    @Serializable
    @SerialName("DokusException.TooManyLoginAttempts")
    data class TooManyLoginAttempts(
        override val message: String? = "Too many login attempts. Please try again later.",
        val retryAfterSeconds: Int = 60,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 429
            const val ERROR_CODE = "TOO_MANY_LOGIN_ATTEMPTS"
        }
    }

    // 500 Internal Server Error
    @Serializable
    @SerialName("DokusException.InternalError")
    data class InternalError(
        val errorMessage: String,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "INTERNAL_ERROR"
        }
    }

    @Serializable
    @SerialName("DokusException.TenantCreationFailed")
    data class TenantCreationFailed(
        override val message: String? = "Failed to create tenant. Please try again.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "TENANT_CREATION_FAILED"
        }
    }

    @Serializable
    @SerialName("DokusException.Unknown")
    data class Unknown(
        @Contextual val throwable: Throwable?,
        override val message: String? = throwable?.message,
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 500
            const val ERROR_CODE = "UNKNOWN_ERROR"
        }
    }

    // 501 Not Implemented
    @Serializable
    @SerialName("DokusException.NotImplemented")
    data class NotImplemented(
        override val message: String? = "This feature is not yet implemented.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = false,
    ) {
        companion object {
            const val HTTP_STATUS = 501
            const val ERROR_CODE = "NOT_IMPLEMENTED"
        }
    }

    // 503 Service Unavailable
    @Serializable
    @SerialName("DokusException.ConnectionError")
    data class ConnectionError(
        override val message: String? = "Connection error. Please try again later.",
    ) : DokusException(
        httpStatusCode = HTTP_STATUS,
        errorCode = ERROR_CODE,
        recoverable = true,
    ) {
        companion object {
            const val HTTP_STATUS = 503
            const val ERROR_CODE = "CONNECTION_ERROR"
        }
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generateErrorId(): String =
            "ERR-${Uuid.random()}"
    }
}
