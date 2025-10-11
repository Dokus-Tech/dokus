package ai.dokus.foundation.ui.extensions

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
        is DokusException.InvalidAddress -> when (this) {
            is DokusException.InvalidAddress.InvalidStreetName -> "Please enter the valid street name"
            is DokusException.InvalidAddress.InvalidCity -> "Please enter the valid city"
            is DokusException.InvalidAddress.InvalidPostalCode -> "Please enter the valid postal code"
            is DokusException.InvalidAddress.InvalidCountry -> "Please enter the valid country"
        }
    }