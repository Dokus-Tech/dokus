package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.model.NewWorkspace

class ValidateNewWorkspaceUseCase(
    private val nameValidator: ValidateWorkspaceNameUseCase = ValidateWorkspaceNameUseCase(),
    private val taxNumberValidator: ValidateWorkspaceTaxNumberUseCase = ValidateWorkspaceTaxNumberUseCase(),
) : ValidatorThrowable<NewWorkspace> {

    override operator fun invoke(value: NewWorkspace) {
        if (!nameValidator(value.name)) throw PredictException.InvalidWorkspaceName
        if (!taxNumberValidator(value.taxNumber)) throw PredictException.InvalidTaxNumber
    }
}