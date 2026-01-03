package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_confirm_missing_fields
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.foundation.app.state.DokusState

private const val MinConfidenceThreshold = 0.0
private const val PercentageMultiplier = 100

@Immutable
sealed interface DocumentReviewState : MVIState, DokusState<Nothing> {

    data object Loading : DocumentReviewState

    data class Content(
        val documentId: DocumentId,
        val document: DocumentRecordDto,
        val editableData: EditableExtractedData,
        val originalData: ExtractedDocumentData?,
        val hasUnsavedChanges: Boolean = false,
        val isSaving: Boolean = false,
        val isConfirming: Boolean = false,
        val selectedFieldPath: String? = null,
        val previewUrl: String? = null,
        val contactSuggestions: List<ContactSuggestion> = emptyList(),
        val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
        val selectedContactId: ContactId? = null,
        val selectedContactSnapshot: ContactSnapshot? = null,
        val contactSelectionState: ContactSelectionState = ContactSelectionState.NoContact,
        val isContactRequired: Boolean = false,
        val contactValidationError: DokusException? = null,
        val isBindingContact: Boolean = false,
        val isDocumentConfirmed: Boolean = false,
        val showPreviewSheet: Boolean = false,
    ) : DocumentReviewState {

        val canConfirm: Boolean
            get() {
                val baseValid = (
                    document.draft?.draftStatus == DraftStatus.NeedsReview ||
                        document.draft?.draftStatus == DraftStatus.Ready
                    ) &&
                    !isConfirming &&
                    !isSaving &&
                    !isBindingContact &&
                    editableData.isValid
                return baseValid
            }

        val confirmBlockedReason: StringResource?
            get() = when {
                !editableData.isValid -> Res.string.cashflow_confirm_missing_fields
                else -> null
            }

        val showConfidence: Boolean
            get() {
                val conf = document.latestIngestion?.confidence
                return conf != null && conf > MinConfidenceThreshold
            }

        val confidencePercent: Int
            get() = ((document.latestIngestion?.confidence ?: MinConfidenceThreshold) * PercentageMultiplier).toInt()
    }

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentReviewState, DokusState.Error<Nothing>
}
