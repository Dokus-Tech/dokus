package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.CreateCompanyRequest

class ValidateNewWorkspaceUseCase(
    private val nameValidator: ValidateWorkspaceNameUseCase,
    private val taxNumberValidator: ValidateWorkspaceTaxNumberUseCase,
    private val addressValidator: ValidatorThrowable<Address>
) : ValidatorThrowable<CreateCompanyRequest> {

    @Throws(DokusException::class)
    override operator fun invoke(value: CreateCompanyRequest) {
        if (!nameValidator(value.name)) throw DokusException.Validation.InvalidWorkspaceName
        if (!taxNumberValidator(value.taxId)) throw DokusException.Validation.InvalidTaxNumber
        addressValidator(value.address)
    }
}