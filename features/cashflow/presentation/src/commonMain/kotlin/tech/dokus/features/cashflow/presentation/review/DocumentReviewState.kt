package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_confirm_select_contact
import tech.dokus.aura.resources.cashflow_confirm_missing_fields
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.Money
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
        val counterpartyIntent: CounterpartyIntent = CounterpartyIntent.None,
        val contactValidationError: DokusException? = null,
        val isBindingContact: Boolean = false,
        val isRejecting: Boolean = false,
        val isDocumentConfirmed: Boolean = false,
        val isDocumentRejected: Boolean = false,
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
                    !isRejecting &&
                    !isBindingContact &&
                    confirmBlockedReason == null
                return baseValid
            }

        val confirmBlockedReason: StringResource?
            get() = when {
                isDocumentConfirmed || isDocumentRejected -> null
                editableData.documentType.isUnknown -> Res.string.cashflow_confirm_missing_fields
                !editableData.hasRequiredDates -> Res.string.cashflow_confirm_missing_fields
                !editableData.hasCoherentAmounts -> Res.string.cashflow_confirm_missing_fields
                counterpartyIntent == CounterpartyIntent.Pending -> Res.string.cashflow_confirm_select_contact
                isContactRequired && selectedContactId == null -> Res.string.cashflow_confirm_select_contact
                !editableData.isValid -> Res.string.cashflow_confirm_missing_fields
                else -> null
            }

        val showConfidence: Boolean
            get() {
                val conf = document.latestIngestion?.confidence
                val status = document.draft?.draftStatus
                val statusAllowsConfidence = status != DraftStatus.NeedsReview && status != DraftStatus.Rejected
                return conf != null && conf > MinConfidenceThreshold && statusAllowsConfidence
            }

        val confidencePercent: Int
            get() = ((document.latestIngestion?.confidence ?: MinConfidenceThreshold) * PercentageMultiplier).toInt()
    }

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentReviewState, DokusState.Error<Nothing>
}

private val DocumentType.isUnknown: Boolean
    get() = this == DocumentType.Unknown

private val EditableExtractedData.hasRequiredDates: Boolean
    get() = when (documentType) {
        DocumentType.Invoice -> invoice?.issueDate != null
        DocumentType.Bill -> bill?.issueDate != null
        DocumentType.Expense -> expense?.date != null
        else -> false
    }

private val EditableExtractedData.hasCoherentAmounts: Boolean
    get() {
        return when (documentType) {
            DocumentType.Invoice -> {
                val invoiceData = invoice ?: return false
                val subtotal = Money.parse(invoiceData.subtotalAmount)
                val vat = Money.parse(invoiceData.vatAmount)
                val total = Money.parse(invoiceData.totalAmount)
                if (subtotal == null) return false
                if (total == null || vat == null) return true
                val expected = subtotal + vat
                kotlin.math.abs(expected.minor - total.minor) <= 1L
            }
            DocumentType.Bill -> {
                val billData = bill ?: return false
                Money.parse(billData.amount) != null
            }
            DocumentType.Expense -> {
                val expenseData = expense ?: return false
                Money.parse(expenseData.amount) != null
            }
            else -> false
        }
    }
