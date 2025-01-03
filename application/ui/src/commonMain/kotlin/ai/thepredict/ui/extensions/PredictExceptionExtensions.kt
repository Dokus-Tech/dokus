package ai.thepredict.ui.extensions

import ai.thepredict.domain.exceptions.PredictException
import androidx.compose.runtime.Composable

val PredictException.localized: String
    @Composable get() = when (this) {
        is PredictException.UserAlreadyExists -> "That user already exists!"
        is PredictException.NonAuthenticated -> "Authentication unsuccessful"
        is PredictException.Unknown -> message ?: "Unknown error happened"
        is PredictException.InternalError -> errorMessage
        is PredictException.ConnectionError -> "Unable to connect to the server"
        is PredictException.InvalidEmail -> "Please enter the valid email"
        is PredictException.WeakPassword -> "The password is too weak"
        is PredictException.InvalidName -> "Please enter the valid name"
        is PredictException.InvalidTaxNumber -> "Please enter the valid tax number"
        is PredictException.InvalidWorkspaceName -> "Please enter the valid workspace name"
    }