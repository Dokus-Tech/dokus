package ai.thepredict.ui.extensions

import ai.thepredict.domain.exceptions.PredictException
import androidx.compose.runtime.Composable

val PredictException.localized: String
    @Composable get() = when (this) {
        is PredictException.UserAlreadyExists -> "That user already exists!"
        is PredictException.NotAuthenticated -> "You are not authenticated"
        is PredictException.NotAuthorized -> "You are not authorized to perform this action"
        is PredictException.Unknown -> message ?: "Unknown error happened"
        is PredictException.InternalError -> errorMessage
        is PredictException.ConnectionError -> "Unable to connect to the server"
        is PredictException.InvalidEmail -> "Please enter the valid email"
        is PredictException.WeakPassword -> "The password is too weak"
        is PredictException.PasswordDoNotMatch -> "Passwords do not match"
        is PredictException.InvalidFirstName -> "Please enter the valid first name"
        is PredictException.InvalidLastName -> "Please enter the valid last name"
        is PredictException.InvalidTaxNumber -> "Please enter the valid tax number"
        is PredictException.InvalidWorkspaceName -> "Please enter the valid workspace name"
        is PredictException.InvalidAddress -> when (this) {
            is PredictException.InvalidAddress.InvalidStreetName -> "Please enter the valid street name"
            is PredictException.InvalidAddress.InvalidCity -> "Please enter the valid city"
            is PredictException.InvalidAddress.InvalidPostalCode -> "Please enter the valid postal code"
            is PredictException.InvalidAddress.InvalidCountry -> "Please enter the valid country"
        }
    }