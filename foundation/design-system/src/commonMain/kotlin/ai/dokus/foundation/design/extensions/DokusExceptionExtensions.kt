package ai.dokus.foundation.design.extensions

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.exception_account_inactive
import ai.dokus.app.resources.generated.exception_account_locked
import ai.dokus.app.resources.generated.exception_connection_error
import ai.dokus.app.resources.generated.exception_email_already_verified
import ai.dokus.app.resources.generated.exception_email_not_verified
import ai.dokus.app.resources.generated.exception_email_verification_token_expired
import ai.dokus.app.resources.generated.exception_email_verification_token_invalid
import ai.dokus.app.resources.generated.exception_invalid_bic
import ai.dokus.app.resources.generated.exception_invalid_city
import ai.dokus.app.resources.generated.exception_invalid_country
import ai.dokus.app.resources.generated.exception_invalid_credentials
import ai.dokus.app.resources.generated.exception_invalid_email
import ai.dokus.app.resources.generated.exception_invalid_first_name
import ai.dokus.app.resources.generated.exception_invalid_iban
import ai.dokus.app.resources.generated.exception_invalid_invoice_number
import ai.dokus.app.resources.generated.exception_invalid_last_name
import ai.dokus.app.resources.generated.exception_invalid_money
import ai.dokus.app.resources.generated.exception_invalid_peppol_id
import ai.dokus.app.resources.generated.exception_invalid_percentage
import ai.dokus.app.resources.generated.exception_invalid_postal_code
import ai.dokus.app.resources.generated.exception_invalid_quantity
import ai.dokus.app.resources.generated.exception_invalid_street_name
import ai.dokus.app.resources.generated.exception_invalid_tax_number
import ai.dokus.app.resources.generated.exception_invalid_vat_number
import ai.dokus.app.resources.generated.exception_invalid_vat_rate
import ai.dokus.app.resources.generated.exception_invalid_workspace_name
import ai.dokus.app.resources.generated.exception_not_authenticated
import ai.dokus.app.resources.generated.exception_not_authorized
import ai.dokus.app.resources.generated.exception_password_do_not_match
import ai.dokus.app.resources.generated.exception_password_reset_token_expired
import ai.dokus.app.resources.generated.exception_password_reset_token_invalid
import ai.dokus.app.resources.generated.exception_refresh_token_expired
import ai.dokus.app.resources.generated.exception_refresh_token_revoked
import ai.dokus.app.resources.generated.exception_session_expired
import ai.dokus.app.resources.generated.exception_session_invalid
import ai.dokus.app.resources.generated.exception_tenant_creation_failed
import ai.dokus.app.resources.generated.exception_token_expired
import ai.dokus.app.resources.generated.exception_token_invalid
import ai.dokus.app.resources.generated.exception_too_many_login_attempts
import ai.dokus.app.resources.generated.exception_too_many_sessions
import ai.dokus.app.resources.generated.exception_unknown
import ai.dokus.app.resources.generated.exception_user_already_exists
import ai.dokus.app.resources.generated.exception_validation_error
import ai.dokus.app.resources.generated.exception_weak_password
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

/**
 * Extension property to get a localized error message for a DokusException.
 *
 * This property uses Compose Multiplatform's string resources to provide
 * translations for all exception types. The appropriate translation is selected
 * based on the user's locale settings.
 *
 * Supported locales:
 * - English (default)
 * - French (France and Belgium)
 * - Dutch (Netherlands and Belgium)
 * - German
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun ErrorMessage(exception: DokusException) {
 *     Text(text = exception.localized)
 * }
 * ```
 */
val DokusException.localized: String
    @Composable get() = when (this) {
        // 400 Validation Errors
        is DokusException.Validation.InvalidEmail -> stringResource(Res.string.exception_invalid_email)
        is DokusException.Validation.WeakPassword -> stringResource(Res.string.exception_weak_password)
        is DokusException.Validation.PasswordDoNotMatch -> stringResource(Res.string.exception_password_do_not_match)
        is DokusException.Validation.InvalidFirstName -> stringResource(Res.string.exception_invalid_first_name)
        is DokusException.Validation.InvalidLastName -> stringResource(Res.string.exception_invalid_last_name)
        is DokusException.Validation.InvalidTaxNumber -> stringResource(Res.string.exception_invalid_tax_number)
        is DokusException.Validation.InvalidWorkspaceName -> stringResource(Res.string.exception_invalid_workspace_name)
        is DokusException.Validation.InvalidVatNumber -> stringResource(Res.string.exception_invalid_vat_number)
        is DokusException.Validation.InvalidIban -> stringResource(Res.string.exception_invalid_iban)
        is DokusException.Validation.InvalidBic -> stringResource(Res.string.exception_invalid_bic)
        is DokusException.Validation.InvalidPeppolId -> stringResource(Res.string.exception_invalid_peppol_id)
        is DokusException.Validation.InvalidInvoiceNumber -> stringResource(Res.string.exception_invalid_invoice_number)
        is DokusException.Validation.InvalidMoney -> stringResource(Res.string.exception_invalid_money)
        is DokusException.Validation.InvalidVatRate -> stringResource(Res.string.exception_invalid_vat_rate)
        is DokusException.Validation.InvalidPercentage -> stringResource(Res.string.exception_invalid_percentage)
        is DokusException.Validation.InvalidQuantity -> stringResource(Res.string.exception_invalid_quantity)
        is DokusException.Validation.InvalidStreetName -> stringResource(Res.string.exception_invalid_street_name)
        is DokusException.Validation.InvalidCity -> stringResource(Res.string.exception_invalid_city)
        is DokusException.Validation.InvalidPostalCode -> stringResource(Res.string.exception_invalid_postal_code)
        is DokusException.Validation.InvalidCountry -> stringResource(Res.string.exception_invalid_country)
        is DokusException.Validation -> stringResource(Res.string.exception_validation_error)
        is DokusException.BadRequest -> message

        // 401 Authentication Errors
        is DokusException.NotAuthenticated -> stringResource(Res.string.exception_not_authenticated)
        is DokusException.InvalidCredentials -> stringResource(Res.string.exception_invalid_credentials)
        is DokusException.TokenExpired -> stringResource(Res.string.exception_token_expired)
        is DokusException.TokenInvalid -> stringResource(Res.string.exception_token_invalid)
        is DokusException.RefreshTokenExpired -> stringResource(Res.string.exception_refresh_token_expired)
        is DokusException.RefreshTokenRevoked -> stringResource(Res.string.exception_refresh_token_revoked)
        is DokusException.SessionExpired -> stringResource(Res.string.exception_session_expired)
        is DokusException.SessionInvalid -> stringResource(Res.string.exception_session_invalid)
        is DokusException.PasswordResetTokenExpired -> stringResource(Res.string.exception_password_reset_token_expired)
        is DokusException.PasswordResetTokenInvalid -> stringResource(Res.string.exception_password_reset_token_invalid)
        is DokusException.EmailVerificationTokenExpired -> stringResource(Res.string.exception_email_verification_token_expired)
        is DokusException.EmailVerificationTokenInvalid -> stringResource(Res.string.exception_email_verification_token_invalid)

        // 403 Authorization Errors
        is DokusException.NotAuthorized -> stringResource(Res.string.exception_not_authorized)
        is DokusException.AccountInactive -> stringResource(Res.string.exception_account_inactive)
        is DokusException.AccountLocked -> stringResource(Res.string.exception_account_locked)
        is DokusException.EmailNotVerified -> stringResource(Res.string.exception_email_not_verified)
        is DokusException.EmailAlreadyVerified -> stringResource(Res.string.exception_email_already_verified)
        is DokusException.TooManySessions -> stringResource(Res.string.exception_too_many_sessions)

        // 409 Conflict Errors
        is DokusException.UserAlreadyExists -> stringResource(Res.string.exception_user_already_exists)

        // 429 Rate Limiting Errors
        is DokusException.TooManyLoginAttempts -> stringResource(Res.string.exception_too_many_login_attempts)

        // 500 Server Errors
        is DokusException.InternalError -> errorMessage
        is DokusException.TenantCreationFailed -> stringResource(Res.string.exception_tenant_creation_failed)
        is DokusException.Unknown -> message ?: stringResource(Res.string.exception_unknown)

        // 501 Not Implemented
        is DokusException.NotImplemented -> message ?: "This feature is not yet implemented."

        // 503 Service Unavailable
        is DokusException.ConnectionError -> stringResource(Res.string.exception_connection_error)
        is DokusException.NotFound -> "TODO"
        is DokusException.UserNotFound -> "TODO"
    }
