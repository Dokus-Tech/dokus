package tech.dokus.features.cashflow.presentation.detail

internal class DocumentDetailProvenance {
    suspend fun DocumentDetailCtx.handleSelectFieldForProvenance(fieldPath: String?) {
        updateState { copy(selectedFieldPath = fieldPath) }
    }
}
