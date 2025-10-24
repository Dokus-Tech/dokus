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
        is DokusException.InvalidEmail -> "Please enter the valid email"
        is DokusException.WeakPassword -> "The password is too weak"
        is DokusException.PasswordDoNotMatch -> "Passwords do not match"
        is DokusException.InvalidFirstName -> "Please enter the valid first name"
        is DokusException.InvalidLastName -> "Please enter the valid last name"
        is DokusException.InvalidTaxNumber -> "Please enter the valid tax number"
        is DokusException.InvalidWorkspaceName -> "Please enter the valid workspace name"
        is DokusException.InvalidVatNumber -> "Please enter a valid VAT number"
        is DokusException.InvalidIban -> "Please enter a valid IBAN"
        is DokusException.InvalidBic -> "Please enter a valid BIC"
        is DokusException.InvalidPeppolId -> "Please enter a valid Peppol ID"
        is DokusException.InvalidInvoiceNumber -> "Please enter a valid invoice number"
        is DokusException.InvalidMoney -> "Please enter a valid amount"
        is DokusException.InvalidVatRate -> "Please enter a valid VAT rate"
        is DokusException.InvalidPercentage -> "Please enter a valid percentage"
        is DokusException.InvalidQuantity -> "Please enter a valid quantity"
        is DokusException.InvalidAddress -> when (this) {
            is DokusException.InvalidAddress.InvalidStreetName -> "Please enter the valid street name"
            is DokusException.InvalidAddress.InvalidCity -> "Please enter the valid city"
            is DokusException.InvalidAddress.InvalidPostalCode -> "Please enter the valid postal code"
            is DokusException.InvalidAddress.InvalidCountry -> "Please enter the valid country"
        }
    }