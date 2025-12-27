package ai.dokus.foundation.domain.usecases.validators

import tech.dokus.domain.exceptions.DokusException

interface ValidatorThrowable<T> {
    @Throws(DokusException::class)
    operator fun invoke(value: T)
}