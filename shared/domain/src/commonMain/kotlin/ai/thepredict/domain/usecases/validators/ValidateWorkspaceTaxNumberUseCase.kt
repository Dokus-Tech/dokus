package ai.thepredict.domain.usecases.validators

import ai.thepredict.data.TaxNumber

class ValidateWorkspaceTaxNumberUseCase : Validator<String?> {
    override fun invoke(value: String?): Boolean {
        if (value == null) return true
        return TaxNumber.canBeUsed(value)
    }
}