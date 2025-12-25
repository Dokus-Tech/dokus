package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.City

object ValidateCityUseCase : Validator<City> {
    private const val MIN_LENGTH = 2

    override operator fun invoke(value: City): Boolean {
        return value.value.length >= MIN_LENGTH
    }
}
