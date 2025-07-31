package ai.thepredict.domain.usecases.validators

import ai.thepredict.domain.model.TaxNumber


class ValidateWorkspaceTaxNumberUseCase : Validator<String?> {

    override fun invoke(value: String?): Boolean {
        if (value.isNullOrEmpty()) return true
        return TaxNumber.canBeUsed(value)
    }
}