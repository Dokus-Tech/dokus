package ai.dokus.app.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState

internal class DocumentReviewProvenance {
    suspend fun DocumentReviewCtx.handleSelectFieldForProvenance(fieldPath: String?) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(selectedFieldPath = fieldPath) }
        }
    }
}
