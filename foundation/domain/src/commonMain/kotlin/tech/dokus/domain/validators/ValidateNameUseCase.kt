package tech.dokus.domain.validators

import tech.dokus.domain.Name

object ValidateNameUseCase : Validator<Name> {
    private const val MIN_LENGTH = 3

    override operator fun invoke(value: Name): Boolean {
        return value.value.length >= MIN_LENGTH
    }
}