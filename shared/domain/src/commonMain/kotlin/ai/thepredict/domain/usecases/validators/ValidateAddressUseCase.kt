package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.model.Address
import ai.thepredict.domain.model.Country

class ValidateAddressUseCase(
    private val country: Country,
    private val streetNameValidator: Validator<String>,
    private val cityValidator: Validator<String>,
    private val postalCodeValidator: Validator<String>,
    private val countryValidator: Validator<String>,
) : ValidatorThrowable<Address> {

    @Throws(PredictException.InvalidAddress::class)
    override fun invoke(value: Address) {
        if (value.streetName != null && !streetNameValidator(value.streetName)) throw PredictException.InvalidAddress.InvalidStreetName
        if (value.city != null && !cityValidator(value.city)) throw PredictException.InvalidAddress.InvalidCity
        if (value.postalCode != null && !postalCodeValidator(value.postalCode)) throw PredictException.InvalidAddress.InvalidPostalCode
        if (value.country != null && !countryValidator(value.country)) throw PredictException.InvalidAddress.InvalidCity
    }
}