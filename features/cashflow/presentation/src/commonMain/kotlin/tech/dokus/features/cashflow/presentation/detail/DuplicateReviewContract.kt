package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.foundation.app.state.DokusState

/**
 * A single field-level diff between existing and incoming documents.
 */
@Immutable
data class DuplicateDiff(
    val field: String,
    val existingValue: String,
    val incomingValue: String,
)

/**
 * State for the duplicate document review surface.
 * Holds both the existing (confirmed/linked) document and the incoming (potential duplicate).
 */
@Immutable
data class DuplicateReviewState(
    val existingDoc: DokusState<DocumentDetailDto> = DokusState.idle(),
    val incomingDoc: DokusState<DocumentDetailDto> = DokusState.idle(),
    val existingPreview: DocumentPreviewState = DocumentPreviewState.Loading,
    val incomingPreview: DocumentPreviewState = DocumentPreviewState.Loading,
    val existingUiData: DocumentUiData? = null,
    val incomingUiData: DocumentUiData? = null,
    val reviewId: DocumentMatchReviewId? = null,
    val reasonType: ReviewReason? = null,
    val diffs: List<DuplicateDiff> = emptyList(),
    val isResolving: Boolean = false,
    val error: DokusException? = null,
) : MVIState {

    val isLoaded: Boolean
        get() = existingUiData != null && incomingUiData != null

    val hasDiffs: Boolean
        get() = diffs.isNotEmpty()
}

/**
 * Intents for the duplicate review flow.
 */
@Immutable
sealed interface DuplicateReviewIntent : MVIIntent {
    data object ResolveSame : DuplicateReviewIntent
    data object ResolveDifferent : DuplicateReviewIntent
}

/**
 * One-off actions emitted by the duplicate review container.
 */
@Immutable
sealed interface DuplicateReviewAction : MVIAction {
    data object Resolved : DuplicateReviewAction
}
