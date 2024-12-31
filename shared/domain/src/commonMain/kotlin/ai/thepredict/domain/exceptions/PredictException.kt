package ai.thepredict.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class PredictException(override val message: String?) : Exception(message) {
    @Serializable
    data object UserAlreadyExists : PredictException("That user already exists!")

    @Serializable
    data class Unknown(@Contextual val throwable: Throwable) : PredictException(throwable.message)
}

val Throwable.asPredictException: PredictException
    get() = PredictException.Unknown(this)