package ai.thepredict.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class PredictException(override val message: String) : Exception(message) {
    @Serializable
    data object UserAlreadyExists : PredictException("That user already exists!")

    @Serializable
    data object NonAuthenticated : PredictException("Wrong credentials!")

    @Serializable
    data class Unknown(@Contextual val throwable: Throwable?) : PredictException(throwable?.message ?: "Unknown error")

    @Serializable
    data class InternalError(val errorMessage: String) : PredictException(errorMessage)
}

val Throwable?.asPredictException: PredictException
    get() {
        return if (this is PredictException) this
        else PredictException.Unknown(this)
    }

val Result<*>.asPredictException: PredictException
    get() {
        return if (isSuccess) PredictException.InternalError("Result is success. You should not call asPredictException")
        else exceptionOrNull().asPredictException
    }