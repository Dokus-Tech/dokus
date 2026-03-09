package tech.dokus.features.cashflow.presentation.review

internal class DocumentReviewProvenance {
    suspend fun DocumentReviewCtx.handleSelectFieldForProvenance(fieldPath: String?) {
        updateState { copy(selectedFieldPath = fieldPath) }
    }
}
