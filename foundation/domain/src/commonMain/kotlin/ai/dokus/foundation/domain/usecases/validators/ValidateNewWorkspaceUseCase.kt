package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.CreateCompanyRequest

class ValidateNewWorkspaceUseCase(
    private val nameValidator: ValidateWorkspaceNameUseCase,
    private val taxNumberValidator: ValidateWorkspaceTaxNumberUseCase,
    private val addressValidator: ValidatorThrowable<Address>
) : ValidatorThrowable<CreateCompanyRequest> {

    @Throws(PredictException::class)
    override operator fun invoke(value: CreateCompanyRequest) {
        if (!nameValidator(value.name)) throw PredictException.InvalidWorkspaceName
        if (!taxNumberValidator(value.taxId)) throw PredictException.InvalidTaxNumber
        addressValidator(value.address)
    }
}