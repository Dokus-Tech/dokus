package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.exceptions.PredictException

interface ValidatorThrowable<T> {
    @Throws(PredictException::class)
    operator fun invoke(value: T)
}