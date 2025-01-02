package ai.thepredict.ui.extensions

import ai.thepredict.domain.exceptions.PredictException
import androidx.compose.runtime.Composable

val PredictException.localized: String
    @Composable get() = when (this) {
        is PredictException.UserAlreadyExists -> "That user already exists!"
        is PredictException.NonAuthenticated -> "Authentication unsuccessful"
        is PredictException.Unknown -> message ?: "Unknown error happened"
        is PredictException.InternalError -> errorMessage
        is PredictException.InvalidEmail -> "Invalid email"
        is PredictException.WeakPassword -> "The password is too weak"
        is PredictException.InvalidName -> "Please enter the valid name"
    }