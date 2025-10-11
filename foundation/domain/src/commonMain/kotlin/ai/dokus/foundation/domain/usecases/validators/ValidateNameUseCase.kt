package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.Name

object ValidateNameUseCase : Validator<Name> {
    private const val MIN_LENGTH = 3

    override operator fun invoke(value: Name): Boolean {
        return value.value.length >= MIN_LENGTH
    }
}