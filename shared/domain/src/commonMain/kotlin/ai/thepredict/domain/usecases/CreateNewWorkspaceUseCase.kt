package ai.thepredict.domain.usecases

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceNameUseCase
import ai.thepredict.domain.usecases.validators.ValidateWorkspaceTaxNumberUseCase

class CreateNewWorkspaceUseCase(
    private val validateWorkspaceNameUseCase: ValidateWorkspaceNameUseCase,
    private val validateWorkspaceTaxNumberUseCase: ValidateWorkspaceTaxNumberUseCase,
) {
    operator fun invoke(
        name: String,
        legalName: String?,
        taxNumber: String?,
    ): Result<Any> {
        if (!validateWorkspaceNameUseCase(name)) return Result.failure(PredictException.InvalidWorkspaceName)
        if (!validateWorkspaceTaxNumberUseCase(taxNumber)) return Result.failure(PredictException.InvalidTaxNumber)
//        val newWorkspace = NewWorkspace(
//            name = name,
//            legalName = legalName.takeIf { it?.isNotEmpty() == true },
//            taxNumber = taxNumber
//        )
        return Result.success(Any())
    }
}