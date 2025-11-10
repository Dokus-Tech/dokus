package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.Country

class ValidateAddressUseCase(
    private val country: Country,
    private val streetNameValidator: Validator<String>,
    private val cityValidator: Validator<String>,
    private val postalCodeValidator: Validator<String>,
    private val countryValidator: Validator<String>,
) : ValidatorThrowable<Address> {

    @Throws(DokusException::class)
    override fun invoke(value: Address) {
        if (value.streetName != null && !streetNameValidator(value.streetName)) throw DokusException.Validation.InvalidStreetName
        if (value.city != null && !cityValidator(value.city)) throw DokusException.Validation.InvalidCity
        if (value.postalCode != null && !postalCodeValidator(value.postalCode)) throw DokusException.Validation.InvalidPostalCode
        if (value.country != null && !countryValidator(value.country)) throw DokusException.Validation.InvalidCity
    }
}