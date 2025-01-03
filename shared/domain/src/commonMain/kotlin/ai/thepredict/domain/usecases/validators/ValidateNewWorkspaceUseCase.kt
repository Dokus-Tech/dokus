package ai.thepredict.domain.usecases.validators

import ai.thepredict.data.NewWorkspace
import ai.thepredict.domain.exceptions.PredictException

class ValidateNewWorkspaceUseCase(
    private val nameValidator: ValidateWorkspaceNameUseCase = ValidateWorkspaceNameUseCase(),
    private val taxNumberValidator: ValidateWorkspaceTaxNumberUseCase = ValidateWorkspaceTaxNumberUseCase(),
) : ValidatorThrowable<NewWorkspace> {

    override operator fun invoke(value: NewWorkspace) {
        if (!nameValidator(value.name)) throw PredictException.InvalidWorkspaceName
        if (!taxNumberValidator(value.taxNumber)) throw PredictException.InvalidTaxNumber
    }
}