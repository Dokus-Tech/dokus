package ai.dokus.foundation.domain.usecases.validators

interface Validator<T> {
    operator fun invoke(value: T): Boolean
}