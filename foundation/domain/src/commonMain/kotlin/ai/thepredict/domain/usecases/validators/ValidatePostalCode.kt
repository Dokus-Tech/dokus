package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.model.Country

class ValidatePostalCode(
    private val country: Country,
) : Validator<String> {

    private val Country.characters
        get() = when (this) {
            Country.BE -> 4
        }

    override fun invoke(value: String): Boolean {
        return value.length >= country.characters
    }
}