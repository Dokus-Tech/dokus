package ai.thepredict.domain.exceptions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class PredictException : Exception() {
    @Serializable
    data object UserAlreadyExists : PredictException()

    @Serializable
    data class Unknown(@Contextual val throwable: Throwable) : PredictException()
}

val Throwable.asPredictException: PredictException
    get() = PredictException.Unknown(this)