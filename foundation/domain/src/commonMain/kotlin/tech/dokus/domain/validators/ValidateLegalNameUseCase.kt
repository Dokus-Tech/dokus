package tech.dokus.domain.validators

import tech.dokus.domain.LegalName

object ValidateLegalNameUseCase : Validator<LegalName> {
    private const val MIN_LENGTH = 3

    override operator fun invoke(value: LegalName): Boolean {
        return value.value.length >= MIN_LENGTH
    }
}