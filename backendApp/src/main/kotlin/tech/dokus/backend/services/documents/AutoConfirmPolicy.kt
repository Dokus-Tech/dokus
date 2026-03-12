package tech.dokus.backend.services.documents

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.processing.DocumentProcessingConstants

class AutoConfirmPolicy {
    suspend fun canAutoConfirm(
        tenantId: TenantId,
        documentId: DocumentId,
        source: DocumentSource,
        documentType: DocumentType,
        draftData: DocumentDraftData,
        auditPassed: Boolean,
        confidence: Double,
        contactId: ContactId?,
        directionResolvedFromAiHintOnly: Boolean
    ): Boolean {
        if (source == DocumentSource.Manual) return false

        val draftType = draftData.toDocumentType()
        if (documentType == DocumentType.Unknown || draftType != documentType) return false
        if (draftData is InvoiceDraftData && contactId == null) return false
        if (draftData is CreditNoteDraftData && contactId == null) return false
        if (!hasRequiredFieldsForAutoConfirm(draftData)) return false
        if (directionResolvedFromAiHintOnly) return false
        if (!isDirectionValid(draftData)) return false
        if (!isAmountPositive(draftData)) return false
        if (!auditPassed) return false
        return when (source) {
            DocumentSource.Peppol -> true
            DocumentSource.Upload,
            DocumentSource.Email -> {
                val meetsConfidence = confidence >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
                val counterpartyKnown = contactId != null
                meetsConfidence && counterpartyKnown
            }
            DocumentSource.Manual -> false
        }
    }

    private fun isDirectionValid(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.direction != DocumentDirection.Unknown
            is ReceiptDraftData -> draftData.direction != DocumentDirection.Unknown
            is CreditNoteDraftData -> draftData.direction != DocumentDirection.Unknown
            is BankStatementDraftData -> draftData.direction == DocumentDirection.Neutral
        }
    }

    private fun isAmountPositive(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.totalAmount?.isPositive == true
            is ReceiptDraftData -> draftData.totalAmount?.isPositive == true
            is CreditNoteDraftData -> draftData.totalAmount?.isPositive == true
            is BankStatementDraftData -> false
        }
    }

    private fun hasRequiredFieldsForAutoConfirm(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> true
            is ReceiptDraftData -> {
                draftData.date != null &&
                    !draftData.merchantName.isNullOrBlank() &&
                    draftData.totalAmount != null
            }
            is CreditNoteDraftData -> {
                !draftData.creditNoteNumber.isNullOrBlank() &&
                    draftData.issueDate != null &&
                    draftData.subtotalAmount != null &&
                    draftData.vatAmount != null &&
                    draftData.totalAmount != null
            }
            is BankStatementDraftData -> draftData.transactions.isNotEmpty()
        }
    }
}
