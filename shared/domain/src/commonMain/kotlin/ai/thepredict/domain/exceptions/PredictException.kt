package ai.thepredict.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class PredictException(val recoverable: Boolean = false) : Exception() {
    @Serializable
    data object UserAlreadyExists : PredictException()

    @Serializable
    data object NonAuthenticated : PredictException()

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

    @Serializable
    data object InvalidName : PredictException()
}

val Throwable?.asPredictException: PredictException
    get() = when (this) {
        is PredictException -> this
        else -> when (this?.message) {
            "Connection refused" -> PredictException.ConnectionError
            else -> PredictException.Unknown(this)
        }
    }

val Result<*>.asPredictException: PredictException
    get() {
        return if (isSuccess) PredictException.InternalError("Result is success. You should not call asPredictException")
        else exceptionOrNull().asPredictException
    }