package ai.dokus.foundation.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class DokusException(val recoverable: Boolean = false) : Exception() {
    @Serializable
    data object UserAlreadyExists : DokusException()

    @Serializable
    data object NotAuthenticated : DokusException()

    @Serializable
    data object NotAuthorized : DokusException()

    @Serializable
    data class Unknown(@Contextual val throwable: Throwable?) : DokusException()

    @Serializable
    data class InternalError(val errorMessage: String) : DokusException(recoverable = true)

    @Serializable
    data object ConnectionError : DokusException(recoverable = true)

    @Serializable
    data object InvalidEmail : DokusException()

    @Serializable
    data object WeakPassword : DokusException()

    data object PasswordDoNotMatch : DokusException()

    @Serializable
    data object InvalidFirstName : DokusException()

    @Serializable
    data object InvalidLastName : DokusException()

    @Serializable
    data object InvalidTaxNumber : DokusException()

    @Serializable
    data object InvalidWorkspaceName : DokusException()

    sealed class InvalidAddress : DokusException() {
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

fun DokusException.Companion.fromRestStatus(statusCode: Int): DokusException? =
    when (statusCode) {
        401 -> DokusException.NotAuthenticated
        403 -> DokusException.NotAuthorized
        else -> null
    }

val Throwable?.asDokusException: DokusException
    get() = when (this) {
        is DokusException -> this
        null -> DokusException.Unknown(this)
        else -> {
            if (message?.contains("Failed to connect to") == true) DokusException.ConnectionError
            else if (message?.contains("Connection refused") == true) DokusException.ConnectionError
            else if (message?.contains("Could not connect to the server") == true) DokusException.ConnectionError
            else if (message?.contains("WebSocket connection") == true) DokusException.ConnectionError
            else DokusException.Unknown(this)
        }
    }

val Result<*>.asDokusException: DokusException
    get() {
        return if (isSuccess) DokusException.InternalError("Result is success. You should not call asPredictException")
        else exceptionOrNull().asDokusException
    }