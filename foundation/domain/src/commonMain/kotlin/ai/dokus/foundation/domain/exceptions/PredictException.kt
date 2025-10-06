package ai.dokus.foundation.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class PredictException(val recoverable: Boolean = false) : Exception() {
    @Serializable
    data object UserAlreadyExists : PredictException()

    @Serializable
    data object NotAuthenticated : PredictException()

    @Serializable
    data object NotAuthorized : PredictException()

    @Serializable
    data class Unknown(@Contextual val throwable: Throwable?) : PredictException()

    @Serializable
    data class InternalError(val errorMessage: String) : PredictException(recoverable = true)

    @Serializable
    data object ConnectionError : PredictException(recoverable = true)

    @Serializable
    data object InvalidEmail : PredictException()

    @Serializable
    data object WeakPassword : PredictException()

    data object PasswordDoNotMatch : PredictException()

    @Serializable
    data object InvalidFirstName : PredictException()

    @Serializable
    data object InvalidLastName : PredictException()

    @Serializable
    data object InvalidTaxNumber : PredictException()

    @Serializable
    data object InvalidWorkspaceName : PredictException()

    sealed class InvalidAddress : PredictException() {
        @Serializable
        data object InvalidStreetName : InvalidAddress()

        @Serializable
        data object InvalidCity : InvalidAddress()

        @Serializable
        data object InvalidPostalCode : InvalidAddress()

        @Serializable
        data object InvalidCountry : InvalidAddress()
    }
}

fun PredictException.Companion.fromRestStatus(statusCode: Int): PredictException? =
    when (statusCode) {
        401 -> PredictException.NotAuthenticated
        403 -> PredictException.NotAuthorized
        else -> null
    }

val Throwable?.asPredictException: PredictException
    get() = when (this) {
        is PredictException -> this
        null -> PredictException.Unknown(this)
        else -> {
            if (message?.contains("Failed to connect to") == true) PredictException.ConnectionError
            else if (message?.contains("Connection refused") == true) PredictException.ConnectionError
            else if (message?.contains("Could not connect to the server") == true) PredictException.ConnectionError
            else if (message?.contains("WebSocket connection") == true) PredictException.ConnectionError
            else PredictException.Unknown(this)
        }
    }

val Result<*>.asPredictException: PredictException
    get() {
        return if (isSuccess) PredictException.InternalError("Result is success. You should not call asPredictException")
        else exceptionOrNull().asPredictException
    }