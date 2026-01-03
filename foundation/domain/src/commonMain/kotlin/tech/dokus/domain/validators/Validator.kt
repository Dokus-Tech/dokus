package tech.dokus.domain.validators

interface Validator<T> {
    operator fun invoke(value: T): Boolean
}
