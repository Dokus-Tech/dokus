package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.exceptions.PredictException

interface ValidatorThrowable<T> {
    @Throws(PredictException::class)
    operator fun invoke(value: T)
}