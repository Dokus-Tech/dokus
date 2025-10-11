package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.Password

object ValidatePasswordUseCase : Validator<Password> {
    private const val MIN_LENGTH = 8

    override operator fun invoke(value: Password): Boolean {
        return value.value.length >= MIN_LENGTH
    }
}