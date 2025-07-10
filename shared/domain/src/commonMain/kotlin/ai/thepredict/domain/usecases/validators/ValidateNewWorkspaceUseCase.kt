package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.exceptions.PredictException

class ValidateNewWorkspaceUseCase(
    private val nameValidator: ValidateWorkspaceNameUseCase = ValidateWorkspaceNameUseCase(),
    private val taxNumberValidator: ValidateWorkspaceTaxNumberUseCase = ValidateWorkspaceTaxNumberUseCase(),
) : ValidatorThrowable<Any> {

    override operator fun invoke(value: Any) {
//        if (!nameValidator(value.name)) throw PredictException.InvalidWorkspaceName
//        if (!taxNumberValidator(value.taxNumber)) throw PredictException.InvalidTaxNumber
    }
}