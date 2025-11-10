package ai.dokus.foundation.design.extensions

import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.runtime.Composable

val DokusException.localized: String
    @Composable get() = when (this) {
        is DokusException.UserAlreadyExists -> "That user already exists!"
        is DokusException.NotAuthenticated -> "You are not authenticated"
        is DokusException.NotAuthorized -> "You are not authorized to perform this action"
        is DokusException.Unknown -> message ?: "Unknown error happened"
        is DokusException.InternalError -> errorMessage
        is DokusException.ConnectionError -> "Unable to connect to the server"
        is DokusException.Validation.InvalidEmail -> "Please enter the valid email"
        is DokusException.Validation.WeakPassword -> "The password is too weak"
        is DokusException.Validation.PasswordDoNotMatch -> "Passwords do not match"
        is DokusException.Validation.InvalidFirstName -> "Please enter the valid first name"
        is DokusException.Validation.InvalidLastName -> "Please enter the valid last name"
        is DokusException.Validation.InvalidTaxNumber -> "Please enter the valid tax number"
        is DokusException.Validation.InvalidWorkspaceName -> "Please enter the valid workspace name"
        is DokusException.Validation.InvalidVatNumber -> "Please enter a valid VAT number"
        is DokusException.Validation.InvalidIban -> "Please enter a valid IBAN"
        is DokusException.Validation.InvalidBic -> "Please enter a valid BIC"
        is DokusException.Validation.InvalidPeppolId -> "Please enter a valid Peppol ID"
        is DokusException.Validation.InvalidInvoiceNumber -> "Please enter a valid invoice number"
        is DokusException.Validation.InvalidMoney -> "Please enter a valid amount"
        is DokusException.Validation.InvalidVatRate -> "Please enter a valid VAT rate"
        is DokusException.Validation.InvalidPercentage -> "Please enter a valid percentage"
        is DokusException.Validation.InvalidQuantity -> "Please enter a valid quantity"
        is DokusException.Validation.InvalidAddress -> when (this) {
            is DokusException.Validation.InvalidAddress.InvalidStreetName -> "Please enter the valid street name"
            is DokusException.Validation.InvalidAddress.InvalidCity -> "Please enter the valid city"
            is DokusException.Validation.InvalidAddress.InvalidPostalCode -> "Please enter the valid postal code"
            is DokusException.Validation.InvalidAddress.InvalidCountry -> "Please enter the valid country"
        }
        is DokusException.TokenExpired -> "Your session has expired. Please log in again."
        is DokusException.TokenInvalid -> "Invalid authentication token"
        is DokusException.RefreshTokenExpired -> "Your session has expired. Please log in again."
        is DokusException.RefreshTokenRevoked -> "Your session has been revoked. Please log in again."
        is DokusException.SessionExpired -> "Your session has expired. Please log in again."
        is DokusException.SessionInvalid -> "Invalid session. Please log in again."
        is DokusException.PasswordResetTokenExpired -> "Password reset link has expired. Please request a new one."
        is DokusException.PasswordResetTokenInvalid -> "Invalid password reset link. Please request a new one."
        is DokusException.EmailVerificationTokenExpired -> "Email verification link has expired. Please request a new one."
        is DokusException.EmailVerificationTokenInvalid -> "Invalid email verification link. Please request a new one."
        is DokusException.AccountInactive -> "Your account is inactive. Please contact support."
        is DokusException.AccountLocked -> "Your account has been locked. Please contact support."
        is DokusException.EmailNotVerified -> "Please verify your email address to continue"
        is DokusException.EmailAlreadyVerified -> "Your email has already been verified"
        is DokusException.TooManyLoginAttempts -> "Too many login attempts. Please try again later."
        is DokusException.TenantCreationFailed -> "Failed to create workspace. Please try again."
        is DokusException.InvalidCredentials -> "Invalid email or password"
    }